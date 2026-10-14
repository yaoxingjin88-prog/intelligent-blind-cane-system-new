package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.common.AlarmStatus;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.mapper.AlarmRecordMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报警记录查询与状态更新
 */
@Service
@RequiredArgsConstructor
public class AlarmRecordService {
    private final AlarmRecordMapper alarmRecordMapper;

    public List<AlarmRecord> getAllAlarmRecords() {
        return getRecentAlarmRecords(200);
    }

    public List<AlarmRecord> getAllAlarmRecords(Integer limit) {
        return getRecentAlarmRecords(limit);
    }

    public List<AlarmRecord> getUnhandledAlarmRecords() {
        return getUnhandledAlarmRecords(200);
    }

    /**
     * 查询未处理报警
     *
     * @param limit 条数上限，默认 200，最大 1000
     * @return 未处理报警列表
     */
    public List<AlarmRecord> getUnhandledAlarmRecords(Integer limit) {
        int normalized = limit == null ? 200 : Math.max(1, Math.min(limit, 1000));
        return alarmRecordMapper.getUnhandledAlarmRecords(normalized);
    }

    /**
     * 查询最近报警（含已处理）
     *
     * @param limit 条数上限，默认 200，最大 1000
     * @return 报警列表，按时间倒序
     */
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

    /**
     * 将报警标记为已处理
     *
     * @param id 报警主键
     */
    public void handleAlarm(Long id) {
        alarmRecordMapper.updateStatus(id, AlarmStatus.HANDLED);
    }

    /**
     * 小程序报警统计卡片数据
     *
     * @param deviceId 可选设备过滤；空则统计全部
     * @return total/pending/fall 等聚合字段
     */
    public Map<String, Object> getMiniStatistics(String deviceId) {
        Map<String, Object> raw = alarmRecordMapper.getMiniStatistics(deviceId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", toLong(raw, "total"));
        stats.put("pending", toLong(raw, "pending"));
        stats.put("handled", toLong(raw, "handled"));
        stats.put("today", toLong(raw, "today"));
        stats.put("thisWeek", toLong(raw, "thisWeek"));
        stats.put("fall", toLong(raw, "fall"));
        stats.put("outOfBounds", toLong(raw, "outOfBounds"));
        stats.put("lowBattery", toLong(raw, "lowBattery"));
        stats.put("sos", toLong(raw, "sos"));
        return stats;
    }

    private static long toLong(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0L;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
