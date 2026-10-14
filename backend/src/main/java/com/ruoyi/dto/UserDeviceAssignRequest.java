package com.ruoyi.dto;

import lombok.Data;

/**
 * 用户侧分配设备：一人一台，传设备表主键；传 null 表示解绑
 */
@Data
public class UserDeviceAssignRequest {
    /** cane_device 主键；null 表示清空该用户绑定 */
    private Long deviceId;
}
