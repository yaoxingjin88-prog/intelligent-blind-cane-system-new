package com.ruoyi.entity;

import lombok.Data;

@Data
public class ElectronicFence {
    private Long id;
    private String deviceId;
    private String fenceName;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radiusMeters;
    private Boolean enabled;
    private String lastStatus;
    private String createdAt;
    private String updatedAt;
}
