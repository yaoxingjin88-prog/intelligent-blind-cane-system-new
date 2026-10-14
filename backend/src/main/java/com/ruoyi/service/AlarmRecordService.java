package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.common.AlarmStatus;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.mapper.AlarmRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报警记录查询与状态更新。
 * 处理约定：监护人主责，管理端兜底；结案后通知对端。
 */
@Service
@RequiredArgsConstructor
public class AlarmRecordService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmNotifyService alarmNotifyService;

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

    /**
     * 裸更新状态：若改为已处理则按管理端兜底结案并通知
     */
    public void updateAlarmStatus(Long id, String status) {
        if (AlarmStatus.isHandled(status)) {
            handleAlarm(id, AlarmNotifyService.HANDLED_BY_ADMIN);
            return;
        }
        alarmRecordMapper.updateStatus(id, status);
    }

    public AlarmRecord getLatestAlarm(String deviceId) {
        return alarmRecordMapper.getLatestByDeviceId(deviceId);
    }

    /**
     * 按设备查询报警历史
     *
     * @param deviceId 设备编号
     * @param limit    条数上限，默认 50，最大 200
     */
    public List<AlarmRecord> getAlarmsByDeviceId(String deviceId, Integer limit) {
        if (deviceId == null || deviceId.isBlank()) {
            return List.of();
        }
        int normalized = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        return alarmRecordMapper.getByDeviceId(deviceId.trim(), normalized);
    }

    /**
     * 将报警标记为已处理（默认视为管理端兜底）
     *
     * @param id 报警主键
     */
    public void handleAlarm(Long id) {
        handleAlarm(id, AlarmNotifyService.HANDLED_BY_ADMIN);
    }

    /**
     * 结案并通知对端
     *
     * @param id        报警主键
     * @param handledBy admin|guardian
     * @return 更新后的记录；已处理或不存在则返回当前库中记录
     */
    public AlarmRecord handleAlarm(Long id, String handledBy) {
        AlarmRecord existing = alarmRecordMapper.getAlarmRecordById(id);
        if (existing == null) {
            throw new IllegalArgumentException("报警不存在");
        }
        if (AlarmStatus.isHandled(existing.getStatus())) {
            return existing;
        }
        String by = (handledBy == null || handledBy.isBlank())
                ? AlarmNotifyService.HANDLED_BY_ADMIN
                : handledBy.trim();
        String handledAt = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        alarmRecordMapper.updateHandled(id, AlarmStatus.HANDLED, by, handledAt);
        existing.setStatus(AlarmStatus.HANDLED);
        existing.setHandledBy(by);
        existing.setHandledAt(handledAt);
        alarmNotifyService.notifyAlarmHandled(existing, by);
        return existing;
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
