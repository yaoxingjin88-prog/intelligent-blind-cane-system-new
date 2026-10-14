package com.ruoyi.entity;

import lombok.Data;

/**
 * 用户意见反馈实体，映射 feedback 表
 */
@Data
public class Feedback {
    /** 主键id */
    private Long id;
    /** 提交人用户id */
    private Long userId;
    /** 反馈类型（功能建议、故障等） */
    private String type;
    /** 反馈正文 */
    private String content;
    /** 联系方式 */
    private String contact;
    /** 图片地址（多图可用逗号分隔） */
    private String images;
    /** 创建时间 */
    private String createTime;
    /** 处理状态：0待处理 1已处理 */
    private String status;
}
