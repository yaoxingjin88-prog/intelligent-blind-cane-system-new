package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.SensorDataMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序「关怀概览」领域逻辑（从 MiniSupport 拆出）
 */
@Service
@RequiredArgsConstructor
public class MiniCareOverviewService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CaneDeviceService caneDeviceService;
    private final SensorDataMapper sensorDataMapper;
    private final ElectronicFenceService electronicFenceService;
    private final AlarmRecordService alarmRecordService;

    /**
     * 组装小程序「关怀概览」卡片数据（状态文案、标签、位置与电量等）
     * 业务步骤：1. 拉设备/传感/围栏/报警 2. 判定风险优先级 3. 组装概览字段与标签
     *
     * @param deviceId 业务设备号
     * @param guardian 当前登录监护人（可空，仅影响展示名）
     * @return 概览 Map，供小程序直接渲染
     */
    public Map<String, Object> buildGuardianCareOverview(String deviceId, Guardian guardian) {
        // 步骤1：拉设备/传感/围栏/报警
        CaneDevice device = resolveDevice(deviceId);
        SensorData latestSensor = sensorDataMapper.getLatestByDeviceId(deviceId);
        FenceEvaluationResult fenceEvaluationResult = latestSensor == null
                ? null
                : electronicFenceService.evaluate(deviceId, latestSensor);
        AlarmRecord latestAlarm = alarmRecordService.getLatestAlarm(deviceId);
        List<SensorData> recentPoints = sensorDataMapper.getTrajectory(deviceId, 1);
        Long stationaryMinutes = calculateStationaryMinutes(recentPoints);
        boolean isFall = latestSensor != null && Boolean.TRUE.equals(latestSensor.getIsFall());
        boolean isFenceTriggered = fenceEvaluationResult != null && Boolean.TRUE.equals(fenceEvaluationResult.getTriggered());
        boolean isLowBattery = device != null && device.getBatteryLevel() != null && device.getBatteryLevel() <= 20;
        boolean isInactive = stationaryMinutes != null && stationaryMinutes >= 20;
        boolean isOffline = device == null || "offline".equals(normalizeDeviceStatus(device.getStatus()));

        // 步骤2：判定风险优先级（跌倒 > 越界 > 低电 > 静止 > 离线）
        String statusKey = "normal";
        String statusLevel = "safe";
        String statusText = "守护中";
        String statusDescription = "当前位置与设备状态均正常，家属可随时远程发起关怀。";
        List<Map<String, Object>> tags = new ArrayList<>();

        if (isFall) {
            statusKey = "fall";
            statusLevel = "danger";
            statusText = "跌倒风险";
            statusDescription = "检测到跌倒异常，建议家属尽快联系并确认现场情况。";
            tags.add(buildGuardianTag("跌倒", "danger"));
        } else if (isFenceTriggered) {
            statusKey = "fence";
            statusLevel = "danger";
            statusText = "越界提醒";
            statusDescription = "设备已离开守护区域，请关注最新位置并及时联系。";
            tags.add(buildGuardianTag("越界", "danger"));
        } else if (isLowBattery) {
            statusKey = "low_battery";
            statusLevel = "warning";
            statusText = "低电量";
            statusDescription = "设备电量不足，建议尽快返程或安排充电。";
            tags.add(buildGuardianTag("低电量", "warning"));
        } else if (isInactive) {
            statusKey = "inactive";
            statusLevel = "warning";
            statusText = "静止偏久";
            statusDescription = "设备已较长时间未移动，建议主动发送安抚语音确认情况。";
            tags.add(buildGuardianTag("静止 " + stationaryMinutes + " 分钟", "warning"));
        } else if (isOffline) {
            statusKey = "offline";
            statusLevel = "warning";
            statusText = "连接中断";
            statusDescription = "设备当前离线，请检查网络或设备开机状态。";
            tags.add(buildGuardianTag("设备离线", "warning"));
        } else {
            tags.add(buildGuardianTag("守护正常", "safe"));
        }

        if (latestAlarm != null && latestAlarm.getAlarmType() != null && !latestAlarm.getAlarmType().trim().isEmpty()) {
            tags.add(buildGuardianTag(latestAlarm.getAlarmType(),
                    ("0".equals(latestAlarm.getStatus()) || "pending".equals(latestAlarm.getStatus())) ? "danger" : "neutral"));
        }

        // 步骤3：组装概览字段与标签
        Map<String, Object> overview = new HashMap<>();
        overview.put("deviceId", deviceId);
        overview.put("deviceName", device != null && device.getDeviceName() != null ? device.getDeviceName() : "智能盲杖");
        overview.put("guardianName", guardian != null && guardian.getName() != null ? guardian.getName() : "家属");
        overview.put("relation", guardian != null && guardian.getRelation() != null ? guardian.getRelation() : "监护人");
        overview.put("elderName", device != null && device.getUserName() != null ? device.getUserName() : "老人");
        overview.put("batteryLevel", device != null && device.getBatteryLevel() != null ? device.getBatteryLevel() : 0);
        overview.put("online", !isOffline);
        overview.put("locationText", formatLocationText(latestSensor));
        overview.put("updateTime", latestSensor != null
                ? (latestSensor.getDataTime() != null ? latestSensor.getDataTime() : latestSensor.getCreateTime())
                : "暂无更新");
        overview.put("statusKey", statusKey);
        overview.put("statusLevel", statusLevel);
        overview.put("statusText", statusText);
        overview.put("statusDescription", statusDescription);
        overview.put("stationaryMinutes", stationaryMinutes != null ? stationaryMinutes : 0L);
        overview.put("lastAlarmType", latestAlarm != null ? latestAlarm.getAlarmType() : "");
        overview.put("lastAlarmTime", latestAlarm != null ? latestAlarm.getAlarmTime() : "");
        overview.put("tags", tags);
        if (latestSensor != null) {
            overview.put("latitude", latestSensor.getLatitude());
            overview.put("longitude", latestSensor.getLongitude());
        }
        return overview;
    }

    /**
     * 解析设备：先按业务 deviceId，再尝试按主键数字
     *
     * @param deviceRef 设备号或主键字符串
     * @return 设备，解析失败则 null
     */
    public CaneDevice resolveDevice(String deviceRef) {
        CaneDevice device = caneDeviceService.getDeviceByDeviceId(deviceRef);
        if (device != null) {
            return device;
        }
        try {
            return caneDeviceService.getDeviceById(Long.parseLong(deviceRef));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 设备状态归一化为 online/offline（兼容中英文）
     *
     * @param status 原始状态文案
     * @return 归一化状态
     */
    public String normalizeDeviceStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "offline";
        }
        String trimmed = status.trim();
        if ("在线".equals(trimmed)) {
            return "online";
        }
        if ("离线".equals(trimmed)) {
            return "offline";
        }
        return trimmed.toLowerCase();
    }

    /** 构建关怀标签（文案 + 语气色） */
    public Map<String, Object> buildGuardianTag(String label, String tone) {
        Map<String, Object> tag = new HashMap<>();
        tag.put("label", label);
        tag.put("tone", tone);
        return tag;
    }

    /** 格式化经纬度展示文案；无坐标时返回「暂无位置数据」 */
    public String formatLocationText(SensorData latestSensor) {
        if (latestSensor == null || latestSensor.getLatitude() == null || latestSensor.getLongitude() == null) {
            return "暂无位置数据";
        }
        return String.format("%.4f, %.4f", latestSensor.getLatitude(), latestSensor.getLongitude());
    }

    /**
     * 估算静止时长：轨迹点均在起点 35m 内且跨度 ≥20 分钟才返回分钟数
     * 业务步骤：1. 校验点数与时间跨度 2. 逐点距离检查 3. 返回静止分钟
     *
     * @param points 近期轨迹点（通常约 1 小时内）
     * @return 静止分钟；不满足条件则 null
     */
    public Long calculateStationaryMinutes(List<SensorData> points) {
        // 步骤1：校验点数与时间跨度
        if (points == null || points.size() < 3) {
            return null;
        }
        SensorData first = points.get(0);
        SensorData last = points.get(points.size() - 1);
        LocalDateTime start = parseSensorTime(first.getDataTime() != null ? first.getDataTime() : first.getCreateTime());
        LocalDateTime end = parseSensorTime(last.getDataTime() != null ? last.getDataTime() : last.getCreateTime());
        if (start == null || end == null) {
            return null;
        }
        long minutes = Math.max(0, Duration.between(start, end).toMinutes());
        if (minutes < 20) {
            return null;
        }
        if (first.getLatitude() == null || first.getLongitude() == null) {
            return null;
        }
        // 步骤2：逐点距离检查（相对起点 >35m 则不算静止）
        for (SensorData point : points) {
            if (point.getLatitude() == null || point.getLongitude() == null) {
                return null;
            }
            if (calculateDistance(first.getLatitude(), first.getLongitude(), point.getLatitude(), point.getLongitude()) > 35) {
                return null;
            }
        }
        // 步骤3：返回静止分钟
        return minutes;
    }

    public LocalDateTime parseSensorTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        return com.ruoyi.utils.GeoUtils.haversineMeters(lat1, lng1, lat2, lng2);
    }
}
