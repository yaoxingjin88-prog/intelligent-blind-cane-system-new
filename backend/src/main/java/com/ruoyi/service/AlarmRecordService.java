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

    public int countAllAlarmRecords() {
        return alarmRecordMapper.countAll();
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
