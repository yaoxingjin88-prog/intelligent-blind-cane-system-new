package com.ruoyi.entity;

import lombok.Data;

@Data
public class SensorData {
    private Long id;
    private String deviceId;
    private Double obstacleDistance;
    private Boolean isFall;
    private Double accelX;
    private Double accelY;
    private Double accelZ;
    private Double fallConfidence;
    private Double latitude;
    private Double longitude;
    private Double temperature;
    private Double humidity;
    private String dataTime;
    private String createTime;
}
