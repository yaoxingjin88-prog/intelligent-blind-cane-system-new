package com.ruoyi.entity;

import lombok.Data;

/**
 * 报警记录实体，映射 alarm_record 表
 */
@Data
public class AlarmRecord {
    /** 主键id */
    private Long id;
    /** 设备编号 */
    private String deviceId;
    /** 报警类型（摔倒报警、电子围栏越界报警、SOS求助等） */
    private String alarmType;
    /** 报警时间，格式 yyyy-MM-dd HH:mm:ss */
    private String alarmTime;
    /** 处理状态：0/未处理=待处理，1/已处理=已处理（见 AlarmStatus） */
    private String status;
}
