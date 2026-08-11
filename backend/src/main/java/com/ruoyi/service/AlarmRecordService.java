package com.ruoyi.service;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.mapper.AlarmRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmRecordService {
    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    public List<AlarmRecord> getAllAlarmRecords() {
        return alarmRecordMapper.getAllAlarmRecords();
    }

    public List<AlarmRecord> getUnhandledAlarmRecords() {
        return alarmRecordMapper.getUnhandledAlarmRecords();
    }

    public List<AlarmRecord> getRecentAlarmRecords(Integer limit) {
        int normalized = limit == null ? 200 : Math.max(1, Math.min(limit, 1000));
        return alarmRecordMapper.getRecentAlarmRecords(normalized);
    }

    public List<AlarmRecord> getLatestAlarmForAllDevices() {
        return alarmRecordMapper.getLatestForAllDevices();
    }

    public int countAllAlarmRecords() {
        return alarmRecordMapper.countAll();
    }

    public int countUnhandledAlarmRecords() {
        return alarmRecordMapper.countUnhandled();
    }

    public List<java.util.Map<String, Object>> getUnhandledCountByDevice() {
        return alarmRecordMapper.getUnhandledCountByDevice();
    }

    public List<java.util.Map<String, Object>> getDeviceAlarmStats(Integer limit) {
        int normalized = limit == null ? 6 : Math.max(1, Math.min(limit, 20));
        return alarmRecordMapper.getDeviceAlarmStats(normalized);
    }

    public List<java.util.Map<String, Object>> getAlarmTypeDistribution() {
        return alarmRecordMapper.getAlarmTypeDistribution();
    }

    public AlarmRecord getAlarmRecordById(Long id) {
        return alarmRecordMapper.getAlarmRecordById(id);
    }

    public void addAlarmRecord(AlarmRecord alarmRecord) {
        alarmRecordMapper.insert(alarmRecord);
    }

    public void deleteAlarmRecord(Long id) {
        alarmRecordMapper.delete(id);
    }

    public int pruneOldAlarmRecords(Integer keepPerDevice) {
        int normalizedKeep = keepPerDevice == null ? 20 : Math.max(1, keepPerDevice);
        return alarmRecordMapper.pruneOlderThanKeepPerDevice(normalizedKeep);
    }

    public void updateAlarmStatus(Long id, String status) {
        alarmRecordMapper.updateStatus(id, status);
    }

    public AlarmRecord getLatestAlarm(String deviceId) {
        return alarmRecordMapper.getLatestByDeviceId(deviceId);
    }

    public void handleAlarm(Long id) {
        alarmRecordMapper.updateStatus(id, "1"); // 1表示已处理
    }
}
