package com.ruoyi.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 盲杖设备实体，映射 cane_device 表
 */
@Data
@Alias("CaneDevice")
public class CaneDevice {
    /** 主键id */
    private Long id;
    /** 设备编号（硬件侧唯一标识，如 ESP32_001） */
    private String deviceId;
    /** 设备名称 */
    private String deviceName;
    /** 所属视障用户id */
    private Long userId;
    /** 所属用户姓名（联表展示用，非必落库） */
    private String userName;
    /** 电池电量，0-100 */
    private Integer batteryLevel;
    /** 在线状态：在线 / 离线 */
    private String status;
}
