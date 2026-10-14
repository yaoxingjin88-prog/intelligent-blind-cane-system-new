package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 盲杖设备档案：查询、增删改，以及联调测试模拟启停。
 * 「在线」按最近传感心跳判断，避免演示库里 status 长期写死为在线。
 */
@Service
@RequiredArgsConstructor
public class CaneDeviceService {
    /** 超过该分钟无上报则视为离线（联调模拟约 10 秒一条） */
    private static final long ONLINE_THRESHOLD_MINUTES = 3L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CaneDeviceMapper deviceMapper;
    private final SensorDataMapper sensorDataMapper;
    private final DeviceTestSimulationService deviceTestSimulationService;

    /** 查询全部设备（含关联用户名；状态按心跳刷新） */
    public List<CaneDevice> getAllDevices() {
        List<CaneDevice> devices = deviceMapper.getAllDevices();
        applyHeartbeatStatus(devices);
        return devices;
    }

    /**
     * 按主键查设备
     *
     * @param id 设备表主键
     * @return 设备，不存在则 null
     */
    public CaneDevice getDeviceById(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        applyHeartbeatStatus(device);
        return device;
    }

    /**
     * 按业务设备号查设备
     *
     * @param deviceId 业务 deviceId（如 DEVICE001）
     * @return 设备，不存在则 null
     */
    public CaneDevice getDeviceByDeviceId(String deviceId) {
        CaneDevice device = deviceMapper.getDeviceByDeviceId(deviceId);
        applyHeartbeatStatus(device);
        return device;
    }

    /**
     * 按视障用户 ID 取一台绑定设备（取最早一条）
     *
     * @param userId 视障用户主键
     * @return 设备，未绑定则 null
     */
    public CaneDevice getDeviceByUserId(Long userId) {
        CaneDevice device = deviceMapper.getDeviceByUserId(userId);
        applyHeartbeatStatus(device);
        return device;
    }

    /**
     * 按视障用户 ID 列出全部绑定设备
     *
     * @param userId 视障用户主键
     * @return 设备列表
     */
    public List<CaneDevice> getDevicesByUserId(Long userId) {
        List<CaneDevice> devices = deviceMapper.getDevicesByUserId(userId);
        applyHeartbeatStatus(devices);
        return devices;
    }

    /** 新增设备档案 */
    public void addDevice(CaneDevice device) {
        deviceMapper.insert(device);
    }

    /** 按主键删除设备 */
    public void deleteDevice(Long id) {
        deviceMapper.delete(id);
    }

    /** 更新设备档案（名称、电量、状态等） */
    public void updateDevice(CaneDevice device) {
        deviceMapper.update(device);
    }

    /**
     * 上报心跳：将设备标记为在线（硬件或联调模拟入库时调用）
     *
     * @param deviceId 业务设备号
     */
    public void markOnline(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        deviceMapper.updateStatusByDeviceId(deviceId, "在线");
    }

    /**
     * 启动设备联调模拟上报
     *
     * @param id 设备主键
     * @return 是否成功启动（设备不存在或已在跑则 false）
     */
    public boolean startDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        boolean started = deviceTestSimulationService.startSimulation(device);
        if (started && device != null) {
            markOnline(device.getDeviceId());
        }
        return started;
    }

    /**
     * 停止设备联调模拟上报
     *
     * @param id 设备主键
     * @return 是否成功停止
     */
    public boolean stopDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        if (device == null) {
            return false;
        }
        boolean stopped = deviceTestSimulationService.stopSimulation(device.getDeviceId());
        // 无论是否原本在跑，停止后一律落库离线，避免残留心跳被算成在线
        deviceMapper.updateStatusByDeviceId(device.getDeviceId(), "离线");
        return stopped;
    }

    /**
     * 当前正在模拟上报的设备号集合
     *
     * @return 业务 deviceId 集合
     */
    public Set<String> getTestingDeviceIds() {
        return deviceTestSimulationService.getRunningDeviceIds();
    }

    private void applyHeartbeatStatus(CaneDevice device) {
        if (device == null) {
            return;
        }
        applyHeartbeatStatus(List.of(device));
    }

    /**
     * 按最近传感时间 / 是否正在模拟，覆盖返回的 status（不批量写库）
     */
    private void applyHeartbeatStatus(List<CaneDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        Map<String, SensorData> latestByDevice = new HashMap<>();
        List<SensorData> latestList = sensorDataMapper.getLatestForAllDevices();
        if (latestList != null) {
            for (SensorData item : latestList) {
                if (item != null && item.getDeviceId() != null) {
                    latestByDevice.put(item.getDeviceId(), item);
                }
            }
        }
        Set<String> testingIds = deviceTestSimulationService.getRunningDeviceIds();
        LocalDateTime now = LocalDateTime.now();
        for (CaneDevice device : devices) {
            if (device == null || device.getDeviceId() == null) {
                continue;
            }
            // 正在联调测试 → 在线
            // 已停止测试（库内已标离线）→ 立即离线，不再吃「3 分钟内模拟残留」
            // 非离线且近 3 分钟有上报 → 在线（真硬件心跳）
            boolean testing = testingIds.contains(device.getDeviceId());
            boolean forcedOffline = isExplicitOffline(device.getStatus());
            boolean recent = !forcedOffline && isRecentHeartbeat(latestByDevice.get(device.getDeviceId()), now);
            device.setStatus(testing || recent ? "在线" : "离线");
        }
    }

    private boolean isExplicitOffline(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String trimmed = status.trim();
        return "离线".equals(trimmed)
                || "offline".equalsIgnoreCase(trimmed)
                || "OFFLINE".equals(trimmed);
    }

    private boolean isRecentHeartbeat(SensorData latest, LocalDateTime now) {
        if (latest == null) {
            return false;
        }
        LocalDateTime pointTime = parseDateTime(latest.getDataTime());
        if (pointTime == null) {
            pointTime = parseDateTime(latest.getCreateTime());
        }
        if (pointTime == null) {
            return false;
        }
        long minutes = ChronoUnit.MINUTES.between(pointTime, now);
        return minutes >= 0 && minutes <= ONLINE_THRESHOLD_MINUTES;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.trim().replace('T', ' ');
            if (normalized.length() > 19) {
                normalized = normalized.substring(0, 19);
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
