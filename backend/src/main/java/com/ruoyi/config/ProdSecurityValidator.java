package com.ruoyi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import com.ruoyi.utils.CredentialMatchers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 生产环境启动时校验关键密钥是否已外置配置，避免带着默认弱口令上线。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProdSecurityValidator implements ApplicationRunner {

    private final Environment environment;
    private final SecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * 仅 prod profile 执行：管理员密码、JWT、测试短信码、设备上报密钥
     */
    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        if (!prod) {
            return;
        }
        String password = securityProperties.getAdminPassword();
        String jwtSecret = securityProperties.getJwtSecret();
        // 管理员口令：禁止默认弱口令；BCrypt 时禁止哈希对应 123456 等
        if (!StringUtils.hasText(password) || isWeakAdminPassword(password)) {
            throw new IllegalStateException("生产环境必须通过 ADMIN_PASSWORD / application-local.yml 配置强管理员密码（支持明文或 BCrypt）");
        }
        if (jwtSecret == null || jwtSecret.isBlank()
                || jwtSecret.contains("please-change")
                || jwtSecret.contains("change-me")
                || jwtSecret.length() < 32) {
            throw new IllegalStateException("生产环境必须配置长度≥32 的 JWT_SECRET，且勿使用示例默认值");
        }
        if (securityProperties.isAllowTestSmsCode()) {
            throw new IllegalStateException("生产环境禁止开启 allow-test-sms-code");
        }
        if (!StringUtils.hasText(securityProperties.getDeviceApiKey())) {
            throw new IllegalStateException("生产环境必须配置 DEVICE_API_KEY，供硬件上报 X-Device-Key 校验");
        }
        log.info("生产安全配置校验通过");
    }

    /** 判断配置口令是否过弱（明文长度/常见弱口令，或 BCrypt 命中弱明文） */
    private boolean isWeakAdminPassword(String password) {
        if ("123456".equals(password) || "admin".equalsIgnoreCase(password) || "password".equalsIgnoreCase(password)) {
            return true;
        }
        if (CredentialMatchers.isBcryptHash(password)) {
            return passwordEncoder.matches("123456", password)
                    || passwordEncoder.matches("admin", password)
                    || passwordEncoder.matches("password", password);
        }
        return password.length() < 8;
    }
}
