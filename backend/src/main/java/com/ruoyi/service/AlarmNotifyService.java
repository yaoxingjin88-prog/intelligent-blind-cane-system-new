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
 * 报警落库与实时推送
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmNotifyService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final AppCacheService appCacheService;

    /**
     * 异步创建报警并推送
     * 业务步骤：
     * 1. 组装报警记录（待处理状态）
     * 2. 写入 alarm_record
     * 3. 按设备推送 + 广播给管理端
     *
     * @param deviceId  设备编号
     * @param alarmType 报警类型文案
     */
    @Async
    public void createAlarmAndNotify(String deviceId, String alarmType) {
        try {
            // 步骤1：组装报警记录
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setDeviceId(deviceId);
            alarmRecord.setAlarmType(alarmType);
            alarmRecord.setStatus(AlarmStatus.PENDING);
            alarmRecord.setAlarmTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            // 步骤2：落库
            alarmRecordMapper.insert(alarmRecord);
            // 步骤3：WebSocket 推送
            alarmWebSocketHandler.sendAlarmNotification(deviceId, alarmRecord);
            alarmWebSocketHandler.broadcastAlarm(alarmRecord);
            appCacheService.evictDashboardCaches();
            log.info("alarm.created deviceId={} type={} alarmId={}", deviceId, alarmType, alarmRecord.getId());
        } catch (Exception e) {
            log.warn("异步创建报警失败 deviceId={} type={}", deviceId, alarmType, e);
        }
    }
}
