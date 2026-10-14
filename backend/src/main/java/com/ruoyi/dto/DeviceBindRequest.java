package com.ruoyi.dto;

import lombok.Data;

/**
 * 设备绑定请求：将设备挂到指定用户
 */
@Data
public class DeviceBindRequest {
    /** 持杖用户主键 */
    private Long userId;
}
