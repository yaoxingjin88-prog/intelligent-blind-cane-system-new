package com.ruoyi.controller;

import com.ruoyi.config.SecurityProperties;
import com.ruoyi.dto.AdminLoginRequest;
import com.ruoyi.entity.Result;
import com.ruoyi.utils.CredentialMatchers;
import com.ruoyi.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端认证接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "认证", description = "管理端登录认证")
@RequiredArgsConstructor
public class AuthController {

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 管理端登录接口 POST /api/login
     *
     * @param body 前端传入参数，@Valid 开启参数校验
     * @return 统一返回结果（含 JWT 与用户信息）
     */
    @Operation(summary = "管理端登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest body) {
        String username = body.getUsername();
        String password = body.getPassword();

        // 校验账号密码（配置支持明文或 BCrypt）
        boolean ok = username.equals(securityProperties.getAdminUsername())
                && CredentialMatchers.matchesPassword(passwordEncoder, securityProperties.getAdminPassword(), password);
        if (!ok) {
            log.warn("auth.login fail username={}", username);
            return Result.error(401, "用户名或密码错误");
        }

        // 签发 JWT 并返回
        String token = jwtUtil.generateToken("admin:" + username);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", Map.of("username", username, "role", "admin"));
        log.info("auth.login success username={}", username);
        return Result.success(data);
    }
}
