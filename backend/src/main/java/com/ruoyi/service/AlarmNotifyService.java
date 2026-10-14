package com.ruoyi.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.common.AlarmStatus;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.mapper.AlarmRecordMapper;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报警落库与实时推送：创建时通知管理端+家属；处理时广播结案结果。
 * 责任约定：监护人主责处理，管理端兜底。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmNotifyService {
    public static final String HANDLED_BY_ADMIN = "admin";
    public static final String HANDLED_BY_GUARDIAN = "guardian";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final AppCacheService appCacheService;

    /**
     * 异步创建报警并推送（传感入库边沿触发）
     */
    @Async
    public void createAlarmAndNotify(String deviceId, String alarmType) {
        try {
            createAlarmPersistAndPush(deviceId, alarmType);
        } catch (Exception e) {
            log.warn("异步创建报警失败 deviceId={} type={}", deviceId, alarmType, e);
        }
    }

    /**
     * 同步创建报警并推送管理端 + 家属端（SOS 等需要立刻拿回记录的场景）
     */
    public AlarmRecord createAlarmPersistAndPush(String deviceId, String alarmType) {
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setDeviceId(deviceId);
        alarmRecord.setAlarmType(alarmType);
        alarmRecord.setStatus(AlarmStatus.PENDING);
        alarmRecord.setAlarmTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        alarmRecordMapper.insert(alarmRecord);

        alarmWebSocketHandler.sendAlarmNotification(deviceId, alarmRecord);
        alarmWebSocketHandler.broadcastAlarm(alarmRecord);
        // 家属端主责：守护提醒与管理端报警同源
        String[] guardianMeta = mapGuardianAlert(alarmType);
        alarmWebSocketHandler.sendGuardianAlert(
                deviceId,
                guardianMeta[0],
                guardianMeta[1],
                "设备 " + deviceId + " 发生" + alarmType + "，请监护人尽快确认并处理。");
        appCacheService.evictDashboardCaches();
        log.info("alarm.created deviceId={} type={} alarmId={}", deviceId, alarmType, alarmRecord.getId());
        return alarmRecord;
    }

    /**
     * 报警已处理：通知管理端与家属端（避免一方结案另一方不知情）
     *
     * @param alarm     已更新后的报警实体
     * @param handledBy admin|guardian
     */
    public void notifyAlarmHandled(AlarmRecord alarm, String handledBy) {
        if (alarm == null || alarm.getDeviceId() == null) {
            return;
        }
        try {
            String byLabel = HANDLED_BY_ADMIN.equals(handledBy) ? "管理端（兜底）" : "监护人";
            String message = byLabel + "已处理「" + (alarm.getAlarmType() == null ? "报警" : alarm.getAlarmType()) + "」";
            alarmWebSocketHandler.broadcastAlarmHandled(alarm, handledBy, message);
            alarmWebSocketHandler.sendAlarmHandled(alarm.getDeviceId(), alarm, handledBy, message);
            appCacheService.evictDashboardCaches();
            log.info("alarm.handled id={} deviceId={} by={}", alarm.getId(), alarm.getDeviceId(), handledBy);
        } catch (Exception e) {
            log.warn("推送报警已处理失败 id={}", alarm.getId(), e);
        }
    }

    /** @return [alertType, level] */
    private static String[] mapGuardianAlert(String alarmType) {
        if (alarmType == null) {
            return new String[]{"ALARM", "warning"};
        }
        if (alarmType.contains("摔") || alarmType.contains("跌倒")) {
            return new String[]{"FALL", "danger"};
        }
        if (alarmType.contains("围栏") || alarmType.contains("越界")) {
            return new String[]{"FENCE", "danger"};
        }
        if (alarmType.toUpperCase().contains("SOS") || alarmType.contains("求助")) {
            return new String[]{"SOS", "danger"};
        }
        if (alarmType.contains("电") || alarmType.contains("电池")) {
            return new String[]{"LOW_BATTERY", "warning"};
        }
        return new String[]{"ALARM", "warning"};
    }
}
