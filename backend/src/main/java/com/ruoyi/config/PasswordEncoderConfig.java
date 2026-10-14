package com.ruoyi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置
 * 仅引入 BCrypt，不启用完整 Spring Security 过滤器链
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 提供 PasswordEncoder Bean（用户密码 / 管理端 BCrypt 口令校验）
     *
     * @return BCrypt 实现
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
