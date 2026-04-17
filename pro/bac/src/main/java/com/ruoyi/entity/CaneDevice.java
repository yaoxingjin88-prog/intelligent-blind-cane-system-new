package com.ruoyi.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("CaneDevice")
public class CaneDevice {
    private Long id;
    private String deviceId;      // 设备编号（如 ESP32_001）
    private String deviceName;    // 设备名称
    private Long userId;          // 所属用户ID
    private String userName;      // 所属用户姓名
    private Integer batteryLevel; // 电池电量
    private String status;        // 在线状态
}
