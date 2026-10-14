package com.ruoyi.entity;

import lombok.Data;

/**
 * 监护人实体，映射 guardian 表
 */
@Data
public class Guardian {
    /** 主键id（小程序 JWT subject） */
    private Long id;
    /** 关联视障用户id */
    private Long userId;
    /** 监护人姓名 */
    private String name;
    /** 手机号（登录账号） */
    private String phone;
    /** 与视障用户关系（如子女、配偶） */
    private String relation;
}
