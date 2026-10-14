package com.ruoyi.entity;

import lombok.Data;

/**
 * 监护人实体，映射 guardian 表
 */
@Data
public class Guardian {
    /** 主监护人：可绑设备、邀请/移除子监护人 */
    public static final String ROLE_PRIMARY = "PRIMARY";
    /** 子监护人：可监视，不可改绑设备与管理家庭成员 */
    public static final String ROLE_MEMBER = "MEMBER";

    /** 主键id（小程序 JWT subject） */
    private Long id;
    /** 关联视障用户（持杖人）id */
    private Long userId;
    /** 监护人姓名 */
    private String name;
    /** 手机号（登录账号） */
    private String phone;
    /** 与持杖人关系（如子女、配偶） */
    private String relation;
    /** 角色：PRIMARY / MEMBER */
    private String role;

    public boolean isPrimary() {
        return ROLE_PRIMARY.equalsIgnoreCase(role);
    }
}
