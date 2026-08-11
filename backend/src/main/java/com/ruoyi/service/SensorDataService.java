package com.ruoyi.service;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.SensorData;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.mapper.AlarmRecordMapper;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import com.ruoyi.mapper.VisuallyImpairedUserMapper;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SensorDataService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> lastGuardianAlertTypes = new ConcurrentHashMap<>();

    @Autowired
    private SensorDataMapper sensorDataMapper;
    @Autowired
    private CaneDeviceMapper caneDeviceMapper;
    @Autowired
    private VisuallyImpairedUserMapper userMapper;
    @Autowired
    private AlarmRecordMapper alarmRecordMapper;
    @Autowired
    private AlarmWebSocketHandler alarmWebSocketHandler;
    @Autowired
    private ElectronicFenceService electronicFenceService;

    public List<SensorData> getAllSensorData() {
        return sensorDataMapper.getAllSensorData();
    }

    public List<SensorData> getRecentSensorData(Integer limit) {
        int normalized = limit == null ? 200 : Math.max(1, Math.min(limit, 1000));
        return sensorDataMapper.getRecentSensorData(normalized);
    }

    public List<SensorData> getSensorDataSinceDays(Integer days) {
        int normalized = days == null ? 7 : Math.max(1, Math.min(days, 90));
        return sensorDataMapper.getSensorDataSinceDays(normalized);
    }

    public List<SensorData> getLatestSensorDataForAllDevices() {
        return sensorDataMapper.getLatestForAllDevices();
    }

    public int countAllSensorData() {
        return sensorDataMapper.countAll();
    }

    public SensorData getSensorDataById(Long id) {
        return sensorDataMapper.getSensorDataById(id);
    }

    public void addSensorData(SensorData sensorData) {
        normalizeSensorData(sensorData);
        SensorData previousLatestSensorData = sensorDataMapper.getLatestByDeviceId(sensorData.getDeviceId());
        // 检查用户是否存在，如果不存在则创建默认用户
        try {
            VisuallyImpairedUser user = new VisuallyImpairedUser();
            user.setUsername("default");
            user.setPassword("123456");
            user.setName("默认用户");
            user.setPhone("13800138000");
            user.setIdCard("110101199001011234");
            user.setAddress("默认地址");
            userMapper.insert(user);
        } catch (Exception e) {
            // 用户已存在，无需处理
        }
        
        // 检查设备是否存在，如果不存在则创建默认设备
        try {
            CaneDevice device = new CaneDevice();
            device.setDeviceId(sensorData.getDeviceId());
            device.setUserId(1L); // 默认用户ID
            device.setBatteryLevel(100); // 默认电池电量
            device.setStatus("在线"); // 默认状态
            caneDeviceMapper.insert(device);
        } catch (Exception e) {
            // 设备已存在，无需处理
        }
        
        sensorDataMapper.insert(sensorData);
        SensorData latestSensorData = sensorDataMapper.getLatestByDeviceId(sensorData.getDeviceId());
        FenceEvaluationResult fenceEvaluationResult = electronicFenceService.evaluate(sensorData.getDeviceId(), latestSensorData);
        alarmWebSocketHandler.sendSensorData(sensorData.getDeviceId(), latestSensorData, fenceEvaluationResult);
        if (fenceEvaluationResult != null) {
            alarmWebSocketHandler.sendFenceStatus(sensorData.getDeviceId(), fenceEvaluationResult);
        }
        pushGuardianAlertIfNeeded(latestSensorData, fenceEvaluationResult);
        
        boolean fallTriggered = Boolean.TRUE.equals(latestSensorData.getIsFall())
                && (previousLatestSensorData == null || !Boolean.TRUE.equals(previousLatestSensorData.getIsFall()));
        if (fallTriggered) {
            createAlarmAndNotify(latestSensorData, "摔倒报警");
        }

        if (fenceEvaluationResult != null && Boolean.TRUE.equals(fenceEvaluationResult.getTriggered())) {
            createAlarmAndNotify(latestSensorData, "电子围栏越界报警");
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
        String alertType = resolveGuardianAlertType(deviceId, latestSensorData, fenceEvaluationResult);
        if ("NORMAL".equals(alertType)) {
            lastGuardianAlertTypes.remove(deviceId);
            return;
        }
        String previousType = lastGuardianAlertTypes.get(deviceId);
        if (alertType.equals(previousType)) {
            return;
        }
        lastGuardianAlertTypes.put(deviceId, alertType);
        if ("FALL".equals(alertType)) {
            alarmWebSocketHandler.sendGuardianAlert(deviceId, "FALL", "danger", "检测到跌倒异常，请家属尽快确认盲杖持有人当前状态。");
            return;
        }
        if ("FENCE".equals(alertType)) {
            alarmWebSocketHandler.sendGuardianAlert(deviceId, "FENCE", "danger", "设备已离开设定守护区域，请及时联系并确认当前位置。");
            return;
        }
        if ("LOW_BATTERY".equals(alertType)) {
            CaneDevice device = caneDeviceMapper.getDeviceByDeviceId(deviceId);
            int batteryLevel = device != null && device.getBatteryLevel() != null ? device.getBatteryLevel() : 0;
            alarmWebSocketHandler.sendGuardianAlert(deviceId, "LOW_BATTERY", "warning", "盲杖电量偏低，当前剩余约 " + batteryLevel + "% ，建议尽快返程或安排充电。");
            return;
        }
        if ("INACTIVE".equals(alertType)) {
            Long inactiveMinutes = calculateStationaryMinutes(deviceId);
            long minutes = inactiveMinutes == null ? 0L : inactiveMinutes;
            alarmWebSocketHandler.sendGuardianAlert(deviceId, "INACTIVE", "warning", "设备已连续静止约 " + minutes + " 分钟，建议家属主动发起关怀确认。");
        }
    }

    private String resolveGuardianAlertType(String deviceId, SensorData latestSensorData, FenceEvaluationResult fenceEvaluationResult) {
        if (Boolean.TRUE.equals(latestSensorData.getIsFall())) {
            return "FALL";
        }
        if (fenceEvaluationResult != null && Boolean.TRUE.equals(fenceEvaluationResult.getTriggered())) {
            return "FENCE";
        }
        CaneDevice device = caneDeviceMapper.getDeviceByDeviceId(deviceId);
        if (device != null && device.getBatteryLevel() != null && device.getBatteryLevel() <= 20) {
            return "LOW_BATTERY";
        }
        Long stationaryMinutes = calculateStationaryMinutes(deviceId);
        if (stationaryMinutes != null && stationaryMinutes >= 20) {
            return "INACTIVE";
        }
        return "NORMAL";
    }

    private Long calculateStationaryMinutes(String deviceId) {
        List<SensorData> points = sensorDataMapper.getTrajectory(deviceId, 1);
        if (points == null || points.size() < 3) {
            return null;
        }
        SensorData first = points.get(0);
        SensorData last = points.get(points.size() - 1);
        LocalDateTime start = parseSensorTime(first);
        LocalDateTime end = parseSensorTime(last);
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
        for (SensorData point : points) {
            if (point.getLatitude() == null || point.getLongitude() == null) {
                return null;
            }
            if (calculateDistance(first.getLatitude(), first.getLongitude(), point.getLatitude(), point.getLongitude()) > 35) {
                return null;
            }
        }
        return minutes;
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

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    
    private void createAlarmAndNotify(SensorData sensorData, String alarmType) {
        try {
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setDeviceId(sensorData.getDeviceId());
            alarmRecord.setAlarmType(alarmType);
            alarmRecord.setStatus("0");
            alarmRecord.setAlarmTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            
            alarmRecordMapper.insert(alarmRecord);
            
            alarmWebSocketHandler.sendAlarmNotification(sensorData.getDeviceId(), alarmRecord);
            alarmWebSocketHandler.broadcastAlarm(alarmRecord);
            
        } catch (Exception e) {
            System.err.println("创建报警记录失败: " + e.getMessage());
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
        return sensorDataMapper.getLatestByDeviceId(deviceId);
    }

    public List<SensorData> getTrajectory(String deviceId, Integer hours) {
        return sensorDataMapper.getTrajectory(deviceId, hours);
    }
}
