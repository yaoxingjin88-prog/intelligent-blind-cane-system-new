package com.ruoyi.entity;

import lombok.Data;

@Data
public class Guardian {
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String relation;
}
