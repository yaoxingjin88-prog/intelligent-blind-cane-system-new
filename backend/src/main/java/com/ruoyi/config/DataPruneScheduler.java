package com.ruoyi.config;

import com.ruoyi.service.AlarmRecordService;
import com.ruoyi.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 历史数据定时裁剪
 * 按设备保留最近 N 条传感/报警，防止表无限膨胀
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPruneScheduler {

    private final SensorDataService sensorDataService;
    private final AlarmRecordService alarmRecordService;

    /** 是否启用定时清理，对应 app.prune.enabled */
    @Value("${app.prune.enabled:true}")
    private boolean enabled;

    /** 每设备保留传感条数 */
    @Value("${app.prune.sensor-keep-per-device:50}")
    private int sensorKeepPerDevice;

    /** 每设备保留报警条数 */
    @Value("${app.prune.alarm-keep-per-device:20}")
    private int alarmKeepPerDevice;

    /**
     * 定时清理任务（默认每天 03:15，cron 可配置）
     * 业务步骤：
     * 1. 开关关闭则直接返回
     * 2. 裁剪 sensor_data
     * 3. 裁剪 alarm_record
     */
    @Scheduled(cron = "${app.prune.cron:0 15 3 * * ?}")
    public void pruneHistory() {
        // 步骤1：开关
        if (!enabled) {
            return;
        }
        try {
            // 步骤2/3：按设备保留最近 N 条
            int sensorDeleted = sensorDataService.pruneOldSensorData(sensorKeepPerDevice);
            int alarmDeleted = alarmRecordService.pruneOldAlarmRecords(alarmKeepPerDevice);
            log.info("定时清理完成 sensorDeleted={} alarmDeleted={}", sensorDeleted, alarmDeleted);
        } catch (Exception e) {
            log.warn("定时清理失败", e);
        }
    }
}
