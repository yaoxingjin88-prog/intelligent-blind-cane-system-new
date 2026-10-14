package com.ruoyi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端登录入参 DTO
 * 只接收前端需要传的字段，不暴露配置中的密钥细节
 */
@Data
public class AdminLoginRequest {
    // 非空校验：用户名不能为空
    @NotBlank(message = "用户名不能为空")
    private String username;

    // 非空校验：密码不能为空
    @NotBlank(message = "密码不能为空")
    private String password;
}
