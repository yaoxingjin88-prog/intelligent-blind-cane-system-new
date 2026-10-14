package com.ruoyi.entity;

import lombok.Data;

/**
 * 电子围栏实体，映射 electronic_fence 表（圆形围栏）
 */
@Data
public class ElectronicFence {
    /** 主键id */
    private Long id;
    /** 绑定设备编号 */
    private String deviceId;
    /** 围栏名称 */
    private String fenceName;
    /** 圆心纬度 */
    private Double centerLatitude;
    /** 圆心经度 */
    private Double centerLongitude;
    /** 半径（米） */
    private Double radiusMeters;
    /** 是否启用：true启用 false禁用 */
    private Boolean enabled;
    /** 上次判定状态：INSIDE=围栏内，OUTSIDE=围栏外（用于边沿触发） */
    private String lastStatus;
    /** 创建时间 */
    private String createdAt;
    /** 更新时间 */
    private String updatedAt;
}
