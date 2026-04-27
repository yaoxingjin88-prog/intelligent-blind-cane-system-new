package com.ruoyi.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("CrossingAssistSnapshot")
public class CrossingAssistSnapshot {
    private String deviceId;
    private String trafficLightStatus;
    private Boolean zebraCrossingDetected;
    private String zebraCrossingDirection;
    private Boolean vehicleApproaching;
    private String recommendation;
    private String message;
    private Double confidence;
    private String source;
    private String updateTime;
}
