package com.ruoyi.entity;

import lombok.Data;

@Data
public class AlarmRecord {
    private Long id;
    private String deviceId;
    private String alarmType;
    private String alarmTime;
    private String status;
}
