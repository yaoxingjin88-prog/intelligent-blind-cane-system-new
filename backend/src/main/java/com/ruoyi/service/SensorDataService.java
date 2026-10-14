package com.ruoyi.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import com.ruoyi.utils.GeoUtils;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传感器数据业务：入库、跌倒判定、围栏联动、监护推送
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SensorDataService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double STATIONARY_RADIUS_METERS = 35.0;
    private static final long STATIONARY_THRESHOLD_MINUTES = 20L;

    /** 已确认存在的设备，避免每次上报都查 cane_device（本机优化即可） */
    private final Set<String> knownDeviceIds = ConcurrentHashMap.newKeySet();

    private final SensorDataMapper sensorDataMapper;
    private final CaneDeviceMapper caneDeviceMapper;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final ElectronicFenceService electronicFenceService;
    private final AlarmNotifyService alarmNotifyService;
    private final AppCacheService appCacheService;
    private final DeviceRuntimeStore deviceRuntimeStore;

    public List<SensorData> getAllSensorData() {
        return getRecentSensorData(1000);
    }

    public List<SensorData> getRecentSensorData(Integer limit) {
        int normalized = limit == null ? 200 : Math.max(1, Math.min(limit, 1000));
        return sensorDataMapper.getRecentSensorData(normalized);
    }

    public List<SensorData> getSensorDataSinceDays(Integer days) {
        int normalized = days == null ? 7 : Math.max(1, Math.min(days, 90));
        return sensorDataMapper.getSensorDataSinceDays(normalized);
    }

    public List<SensorData> getSensorDataSinceDaysLimited(Integer days, Integer limit) {
        int normalizedDays = days == null ? 7 : Math.max(1, Math.min(days, 90));
        int normalizedLimit = limit == null ? 2000 : Math.max(100, Math.min(limit, 5000));
        return sensorDataMapper.getSensorDataSinceDaysLimited(normalizedDays, normalizedLimit);
    }

    public int countRiskEventsSinceDays(Integer days) {
        int normalized = days == null ? 7 : Math.max(1, Math.min(days, 90));
        return sensorDataMapper.countRiskEventsSinceDays(normalized);
    }

    public int countActiveDevicesToday() {
        return sensorDataMapper.countActiveDevicesToday();
    }

    public List<SensorData> getLatestSensorDataForAllDevices() {
        List<SensorData> fromDb = sensorDataMapper.getLatestForAllDevices();
        if (fromDb != null) {
            for (SensorData item : fromDb) {
                if (item != null && item.getDeviceId() != null) {
                    deviceRuntimeStore.putLatest(item.getDeviceId(), item);
                }
            }
            return fromDb;
        }
        return deviceRuntimeStore.localLatestValues();
    }

    public int countAllSensorData() {
        return sensorDataMapper.countAll();
    }

    public SensorData getSensorDataById(Long id) {
        return sensorDataMapper.getSensorDataById(id);
    }

    /**
     * 硬件上报传感器数据（核心入库链路）
     * 业务步骤：
     * 1. 归一化加速度并判定跌倒
     * 2. 读取上一条，用于跌倒边沿去重
     * 3. 设备不存在则自动注册
     * 4. 写入 sensor_data 并刷新运行时最新态（Redis/本机）
     * 5. 电子围栏判定 + WebSocket 推送
     * 6. 跌倒/越界边沿触发报警
     *
     * @param sensorData 硬件上报体
     */
    public void addSensorData(SensorData sensorData) {
        // 步骤1：跌倒归一化
        normalizeSensorData(sensorData);
        String deviceId = sensorData.getDeviceId();

        // 步骤2：取上一条，供边沿判断
        SensorData previousLatest = deviceId == null ? null : deviceRuntimeStore.getLatest(deviceId);
        if (previousLatest == null && deviceId != null) {
            previousLatest = sensorDataMapper.getLatestByDeviceId(deviceId);
        }

        // 步骤3：确保设备档案存在，并刷新心跳在线 / 可选电量
        ensureDeviceExists(deviceId);
        if (deviceId != null && !deviceId.isBlank()) {
            caneDeviceMapper.updateStatusByDeviceId(deviceId, "在线");
            Integer battery = sensorData.getBatteryLevel();
            if (battery != null) {
                int clamped = Math.max(0, Math.min(100, battery));
                sensorData.setBatteryLevel(clamped);
                caneDeviceMapper.updateBatteryLevelByDeviceId(deviceId, clamped);
            }
        }

        // 步骤4：落库 + 写穿运行时态
        sensorDataMapper.insert(sensorData);
        if (sensorData.getCreateTime() == null || sensorData.getCreateTime().isBlank()) {
            sensorData.setCreateTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        }
        if (deviceId != null) {
            deviceRuntimeStore.putLatest(deviceId, sensorData);
        }

        // 步骤5：围栏判定与实时推送（复用入参，避免再查 latest）
        SensorData latestSensorData = sensorData;
        FenceEvaluationResult fenceEvaluationResult = electronicFenceService.evaluate(deviceId, latestSensorData);
        alarmWebSocketHandler.sendSensorData(deviceId, latestSensorData, fenceEvaluationResult);
        if (fenceEvaluationResult != null) {
            alarmWebSocketHandler.sendFenceStatus(deviceId, fenceEvaluationResult);
        }
        pushGuardianAlertIfNeeded(latestSensorData, fenceEvaluationResult);

        // 步骤6：边沿告警（持续跌倒/持续在外不重复刷）
        boolean fallTriggered = Boolean.TRUE.equals(latestSensorData.getIsFall())
                && (previousLatest == null || !Boolean.TRUE.equals(previousLatest.getIsFall()));
        if (fallTriggered) {
            alarmNotifyService.createAlarmAndNotify(deviceId, "摔倒报警");
        }

        boolean fenceTriggered = fenceEvaluationResult != null
                && Boolean.TRUE.equals(fenceEvaluationResult.getTriggered());
        if (fenceTriggered) {
            alarmNotifyService.createAlarmAndNotify(deviceId, "电子围栏越界报警");
        }

        appCacheService.evictDashboardCaches();
        log.info("sensor.ingest deviceId={} fall={} fallTriggered={} fenceTriggered={}",
                deviceId,
                Boolean.TRUE.equals(latestSensorData.getIsFall()),
                fallTriggered,
                fenceTriggered);
    }

    private void ensureDeviceExists(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        if (knownDeviceIds.contains(deviceId)) {
            return;
        }
        CaneDevice existing = caneDeviceMapper.getDeviceByDeviceId(deviceId);
        if (existing != null) {
            knownDeviceIds.add(deviceId);
            return;
        }
        try {
            CaneDevice device = new CaneDevice();
            device.setDeviceId(deviceId);
            device.setUserId(1L);
            device.setBatteryLevel(100);
            device.setStatus("在线");
            caneDeviceMapper.insert(device);
            knownDeviceIds.add(deviceId);
        } catch (Exception e) {
            knownDeviceIds.add(deviceId);
            log.debug("设备可能已存在 deviceId={}: {}", deviceId, e.getMessage());
        }
    }

    private void normalizeSensorData(SensorData sensorData) {
        Double accelX = sensorData.getAccelX();
        Double accelY = sensorData.getAccelY();
        Double accelZ = sensorData.getAccelZ();
        if (accelX == null || accelY == null || accelZ == null) {
            if (sensorData.getFallConfidence() == null) {
                sensorData.setFallConfidence(Boolean.TRUE.equals(sensorData.getIsFall()) ? 1.0 : 0.0);
            }
            return;
        }

        // 加速度按 g 归一：静止约 1g。偏离过大视为冲击/失重；阈值来自样机联调经验，非医学标准
        double magnitude = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
        double deviation = Math.abs(magnitude - 1.0);
        double confidence = clamp((deviation - 0.35) / 1.35, 0.0, 1.0);
        boolean fallByAcceleration = magnitude < 0.45 || magnitude > 2.35 || confidence >= 0.82;

        if (Boolean.TRUE.equals(sensorData.getIsFall())) {
            confidence = Math.max(confidence, 0.98);
        }

        sensorData.setFallConfidence(confidence);
        sensorData.setIsFall(Boolean.TRUE.equals(sensorData.getIsFall()) || fallByAcceleration);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void pushGuardianAlertIfNeeded(SensorData latestSensorData, FenceEvaluationResult fenceEvaluationResult) {
        String deviceId = latestSensorData.getDeviceId();
        Long stationaryMinutes = updateAndGetStationaryMinutes(latestSensorData);

        // 跌倒/越界的 GUARDIAN_ALERT 由 AlarmNotifyService 与落库同源推送，此处不再重复

        CaneDevice device = caneDeviceMapper.getDeviceByDeviceId(deviceId);
        if (device != null && device.getBatteryLevel() != null && device.getBatteryLevel() <= 20) {
            publishGuardianAlertOnce(deviceId, "LOW_BATTERY", "warning",
                    "盲杖电量偏低，当前剩余约 " + device.getBatteryLevel() + "% ，建议尽快返程或安排充电。");
            return;
        }
        if (stationaryMinutes != null && stationaryMinutes >= STATIONARY_THRESHOLD_MINUTES) {
            publishGuardianAlertOnce(deviceId, "INACTIVE", "warning",
                    "设备已连续静止约 " + stationaryMinutes + " 分钟，建议家属主动发起关怀确认。");
            return;
        }
        deviceRuntimeStore.clearLastAlertType(deviceId);
    }

    private void publishGuardianAlertOnce(String deviceId, String alertType, String level, String message) {
        String previousType = deviceRuntimeStore.getLastAlertType(deviceId);
        if (alertType.equals(previousType)) {
            return;
        }
        deviceRuntimeStore.putLastAlertType(deviceId, alertType);
        alarmWebSocketHandler.sendGuardianAlert(deviceId, alertType, level, message);
    }

    /**
     * 用当前点相对静止起点的位移判断，避免每次拉 1 小时轨迹。
     */
    private Long updateAndGetStationaryMinutes(SensorData current) {
        String deviceId = current.getDeviceId();
        if (deviceId == null || current.getLatitude() == null || current.getLongitude() == null) {
            return null;
        }
        LocalDateTime now = parseSensorTime(current);
        if (now == null) {
            now = LocalDateTime.now();
        }

        DeviceRuntimeStore.StationaryState tracker = deviceRuntimeStore.getStationary(deviceId);
        if (tracker == null
                || GeoUtils.haversineMeters(tracker.getLat(), tracker.getLng(), current.getLatitude(), current.getLongitude())
                > STATIONARY_RADIUS_METERS) {
            deviceRuntimeStore.putStationary(deviceId,
                    DeviceRuntimeStore.StationaryState.of(
                            current.getLatitude(), current.getLongitude(), now, DATE_TIME_FORMATTER));
            return null;
        }

        LocalDateTime since;
        try {
            since = LocalDateTime.parse(tracker.getSince(), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            deviceRuntimeStore.putStationary(deviceId,
                    DeviceRuntimeStore.StationaryState.of(
                            current.getLatitude(), current.getLongitude(), now, DATE_TIME_FORMATTER));
            return null;
        }
        long minutes = Math.max(0, Duration.between(since, now).toMinutes());
        return minutes >= STATIONARY_THRESHOLD_MINUTES ? minutes : null;
    }

    private LocalDateTime parseSensorTime(SensorData sensorData) {
        String value = sensorData.getDataTime() != null && !sensorData.getDataTime().isBlank()
                ? sensorData.getDataTime()
                : sensorData.getCreateTime();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void deleteSensorData(Long id) {
        sensorDataMapper.delete(id);
    }

    public int pruneOldSensorData(Integer keepPerDevice) {
        int normalizedKeep = keepPerDevice == null ? 50 : Math.max(1, keepPerDevice);
        return sensorDataMapper.pruneOlderThanKeepPerDevice(normalizedKeep);
    }

    public SensorData getLatestSensorData(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        SensorData cached = deviceRuntimeStore.getLatest(deviceId);
        if (cached != null) {
            return cached;
        }
        SensorData fromDb = sensorDataMapper.getLatestByDeviceId(deviceId);
        if (fromDb != null) {
            deviceRuntimeStore.putLatest(deviceId, fromDb);
        }
        return fromDb;
    }

    public List<SensorData> getTrajectory(String deviceId, Integer hours) {
        return sensorDataMapper.getTrajectory(deviceId, hours);
    }

    public List<SensorData> getTrajectoryByTimeRange(String deviceId, String startTime, String endTime) {
        return sensorDataMapper.getTrajectoryByTimeRange(deviceId, startTime, endTime);
    }

    public List<Map<String, Object>> getTrajectoryPoints(String deviceId, String startTime, String endTime) {
        List<SensorData> points = resolveTrajectory(deviceId, startTime, endTime);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SensorData point : points) {
            if (point.getLatitude() == null || point.getLongitude() == null) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("latitude", point.getLatitude());
            item.put("longitude", point.getLongitude());
            item.put("time", point.getDataTime() != null ? point.getDataTime() : point.getCreateTime());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getTrajectoryStatistics(String deviceId, String startTime, String endTime) {
        List<SensorData> points = resolveTrajectory(deviceId, startTime, endTime);
        double totalDistance = 0;
        long totalDuration = 0;
        if (points.size() >= 2) {
            for (int i = 1; i < points.size(); i++) {
                SensorData prev = points.get(i - 1);
                SensorData curr = points.get(i);
                if (prev.getLatitude() != null && prev.getLongitude() != null
                        && curr.getLatitude() != null && curr.getLongitude() != null) {
                    totalDistance += GeoUtils.haversineMeters(
                            prev.getLatitude(), prev.getLongitude(),
                            curr.getLatitude(), curr.getLongitude());
                }
            }
            // 无精确时间戳间隔时，按约 30 秒/点粗算时长（演示统计用）
            totalDuration = points.size() * 30L;
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDistance", Math.round(totalDistance));
        stats.put("duration", totalDuration);
        stats.put("avgSpeed", totalDuration > 0 ? Math.round(totalDistance / totalDuration * 36) / 10.0 : 0);
        stats.put("pointCount", points.size());
        return stats;
    }

    public Map<String, Object> simulateMovement(String deviceId, double startLat, double startLng, int points) {
        int count = Math.max(1, Math.min(points, 200));
        Random random = new Random();
        double lat = startLat;
        double lng = startLng;
        LocalDateTime time = LocalDateTime.now().minusMinutes(count);
        List<Map<String, Object>> trajectory = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            lat += (random.nextDouble() - 0.3) * 0.0003;
            lng += (random.nextDouble() - 0.3) * 0.0003;

            SensorData data = new SensorData();
            data.setDeviceId(deviceId);
            data.setLatitude(lat);
            data.setLongitude(lng);
            data.setObstacleDistance(50 + random.nextDouble() * 200);
            data.setIsFall(false);
            data.setAccelX(random.nextDouble() * 2 - 1);
            data.setAccelY(random.nextDouble() * 2 - 1);
            data.setAccelZ(9.8 + random.nextDouble() * 0.5);
            data.setFallConfidence(random.nextDouble() * 0.1);
            data.setTemperature(20 + random.nextDouble() * 5);
            data.setHumidity(40 + random.nextDouble() * 20);
            data.setDataTime(time.format(DATE_TIME_FORMATTER));
            sensorDataMapper.insert(data);

            Map<String, Object> point = new HashMap<>();
            point.put("latitude", lat);
            point.put("longitude", lng);
            point.put("time", time.format(DATE_TIME_FORMATTER));
            trajectory.add(point);
            time = time.plusMinutes(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("trajectory", trajectory);
        log.info("sensor.simulate deviceId={} points={}", deviceId, count);
        return result;
    }

    private List<SensorData> resolveTrajectory(String deviceId, String startTime, String endTime) {
        if (startTime != null && endTime != null) {
            return sensorDataMapper.getTrajectoryByTimeRange(deviceId, startTime, endTime);
        }
        return sensorDataMapper.getTrajectory(deviceId, 24);
    }
}
