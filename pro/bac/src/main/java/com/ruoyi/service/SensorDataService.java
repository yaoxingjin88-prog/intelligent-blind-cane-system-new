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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SensorDataService {
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
    
    private void createAlarmAndNotify(SensorData sensorData, String alarmType) {
        try {
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setDeviceId(sensorData.getDeviceId());
            alarmRecord.setAlarmType(alarmType);
            alarmRecord.setStatus("0");
            alarmRecord.setAlarmTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
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
