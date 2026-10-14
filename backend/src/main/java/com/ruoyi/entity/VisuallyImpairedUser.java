package com.ruoyi.entity;

import lombok.Data;

/**
 * 视障用户实体，映射 visually_impaired_user 表
 */
@Data
public class VisuallyImpairedUser {
    /** 主键id */
    private Long id;
    /** 登录用户名 */
    private String username;
    /** 密码（BCrypt 哈希，禁止明文） */
    private String password;
    /** 真实姓名 */
    private String name;
    /** 年龄 */
    private Integer age;
    /** 性别 */
    private String gender;
    /** 血型 */
    private String bloodType;
    /** 手机号 */
    private String phone;
    /** 身份证号 */
    private String idCard;
    /** 住址 */
    private String address;
    /** 紧急联系人姓名 */
    private String emergencyContact;
    /** 紧急联系人电话 */
    private String emergencyPhone;
    /** 病史备注 */
    private String medicalHistory;
}
