package com.ruoyi.entity;

import lombok.Data;

/**
 * 传感器上报数据实体，映射 sensor_data 表
 */
@Data
public class SensorData {
    /** 主键id */
    private Long id;
    /** 设备编号 */
    private String deviceId;
    /** 障碍物距离（厘米或硬件约定单位） */
    private Double obstacleDistance;
    /** 是否跌倒：true=判定跌倒 */
    private Boolean isFall;
    /** 加速度 X（单位 g，静止约 1g 合量级） */
    private Double accelX;
    /** 加速度 Y */
    private Double accelY;
    /** 加速度 Z */
    private Double accelZ;
    /** 跌倒置信度，0.0-1.0 */
    private Double fallConfidence;
    /** 纬度（GCJ-02 / 设备坐标系） */
    private Double latitude;
    /** 经度 */
    private Double longitude;
    /** 温度（℃） */
    private Double temperature;
    /** 湿度（%） */
    private Double humidity;
    /**
     * 电量百分比 0-100（可选，不上 sensor_data 表；有值时回写 cane_device.battery_level）
     */
    private Integer batteryLevel;
    /** 设备侧采样时间 */
    private String dataTime;
    /** 入库时间 */
    private String createTime;
}
