package com.ruoyi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序短信登录/注册入参 DTO
 * 只接收手机号与验证码，不暴露监护人其它档案字段
 */
@Data
public class MiniSmsLoginRequest {
    // 非空校验：手机号不能为空
    @NotBlank(message = "手机号不能为空")
    private String phone;

    // 非空校验：验证码不能为空
    @NotBlank(message = "验证码不能为空")
    private String code;
}
