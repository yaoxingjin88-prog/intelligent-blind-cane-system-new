package com.ruoyi.entity;

import lombok.Data;

@Data
public class Feedback {
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String contact;
    private String images;
    private String createTime;
    private String status;
}
