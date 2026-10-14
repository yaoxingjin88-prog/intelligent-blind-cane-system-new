package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 新增设备档案（可带初始归属；电量/状态由上报维护，入库给默认值） */
    public void addDevice(CaneDevice device) {
        validateDeviceCodeUnique(device, null);
        if (device.getUserId() != null && deviceMapper.countByUserId(device.getUserId()) > 0) {
            throw new IllegalArgumentException("该用户已绑定一台设备，每人仅可绑定一台");
        }
        if (device.getDeviceName() == null || device.getDeviceName().isBlank()) {
            device.setDeviceName(device.getDeviceId());
        }
        if (device.getBatteryLevel() == null) {
            device.setBatteryLevel(100);
        }
        if (device.getStatus() == null || device.getStatus().isBlank()) {
            device.setStatus("离线");
        }
        deviceMapper.insert(device);
    }

    /** 按主键删除设备 */
    public void deleteDevice(Long id) {
        deviceMapper.delete(id);
    }

    /**
     * 更新设备档案（仅编号/名称）。
     * 电量、在线状态由传感上报与心跳维护；归属请用 bind / unbind。
     */
    public void updateDevice(CaneDevice device) {
        if (device == null || device.getId() == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        CaneDevice existing = deviceMapper.getDeviceById(device.getId());
        if (existing == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        validateDeviceCodeUnique(device, device.getId());
        if (device.getDeviceName() == null || device.getDeviceName().isBlank()) {
            device.setDeviceName(existing.getDeviceName());
        }
        // 禁止管理端手改运行态字段
        device.setBatteryLevel(existing.getBatteryLevel());
        device.setStatus(existing.getStatus());
        deviceMapper.update(device);
    }

    /**
     * 将设备绑定到用户（设备须未绑定；用户须尚无设备）
     */
    @Transactional
    public void bindDeviceToUser(Long devicePk, Long userId) {
        if (devicePk == null || userId == null) {
            throw new IllegalArgumentException("设备与用户不能为空");
        }
        CaneDevice device = deviceMapper.getDeviceById(devicePk);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (device.getUserId() != null) {
            if (device.getUserId().equals(userId)) {
                return;
            }
            throw new IllegalArgumentException("该设备已绑定其他用户，请先解绑后再绑定");
        }
        if (deviceMapper.countByUserId(userId) > 0) {
            throw new IllegalArgumentException("该用户已绑定一台设备，每人仅可绑定一台");
        }
        deviceMapper.updateUserId(devicePk, userId);
    }

    /** 解除设备与用户的绑定 */
    @Transactional
    public void unbindDevice(Long devicePk) {
        if (devicePk == null) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        CaneDevice device = deviceMapper.getDeviceById(devicePk);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        deviceMapper.updateUserId(devicePk, null);
    }

    /**
     * 用户侧分配设备：先清空该用户旧绑定，再绑到指定设备（devicePk 为 null 则仅解绑）
     */
    @Transactional
    public void assignUserDevice(Long userId, Long devicePk) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        List<CaneDevice> owned = deviceMapper.getDevicesByUserId(userId);
        for (CaneDevice ownedDevice : owned) {
            if (devicePk == null || !ownedDevice.getId().equals(devicePk)) {
                deviceMapper.updateUserId(ownedDevice.getId(), null);
            }
        }
        if (devicePk == null) {
            return;
        }
        CaneDevice target = deviceMapper.getDeviceById(devicePk);
        if (target == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (target.getUserId() != null && !target.getUserId().equals(userId)) {
            throw new IllegalArgumentException("该设备已绑定其他用户，无法占用");
        }
        if (target.getUserId() == null) {
            deviceMapper.updateUserId(devicePk, userId);
        }
    }

    void validateDeviceCodeUnique(CaneDevice device, Long selfId) {
        if (device == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        if (device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            return;
        }
        CaneDevice sameCode = deviceMapper.getDeviceByDeviceId(device.getDeviceId().trim());
        if (sameCode != null && (selfId == null || !sameCode.getId().equals(selfId))) {
            throw new IllegalArgumentException("设备编号已被占用，无法重复使用");
        }
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
     * 主动离线（假硬件停止 / 设备关机上报）：落库离线并停掉页内测试模拟，
     * 列表心跳判定会忽略近 3 分钟残留传感，避免停止后仍显示在线。
     *
     * @param id 设备主键
     */
    public void markOffline(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (device.getDeviceId() != null) {
            deviceTestSimulationService.stopSimulation(device.getDeviceId());
            deviceMapper.updateStatusByDeviceId(device.getDeviceId(), "离线");
        }
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
            // 已停止测试（库内已标离线）→ 立即离线，不吃模拟残留心跳
            // 库非离线且近 3 分钟有入库 → 在线（真机上报会先把库写成「在线」）
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
        // 优先用库入库时间（服务器本地时区）；设备 dataTime 可能是 UTC，易导致误判离线
        LocalDateTime pointTime = parseDateTime(latest.getCreateTime());
        if (pointTime == null) {
            pointTime = parseDateTime(latest.getDataTime());
        }
        if (pointTime == null) {
            return false;
        }
        long minutes = Math.abs(ChronoUnit.MINUTES.between(pointTime, now));
        return minutes <= ONLINE_THRESHOLD_MINUTES;
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
