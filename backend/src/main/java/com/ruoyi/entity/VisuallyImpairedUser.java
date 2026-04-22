package com.ruoyi.entity;

import lombok.Data;

@Data
public class VisuallyImpairedUser {
    private Long id;
    private String username;
    private String password;
    private String name;
    private Integer age;
    private String gender;
    private String bloodType;
    private String phone;
    private String idCard;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;
    private String medicalHistory;
}
