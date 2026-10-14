package com.ruoyi.utils;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 配置口令比对：支持明文（开发）与 BCrypt 哈希（生产）。
 */
public final class CredentialMatchers {

    private CredentialMatchers() {
    }

    /**
     * 判断配置值是否为 BCrypt 哈希（$2a$ / $2b$ / $2y$）
     *
     * @param value 配置中的口令或哈希
     * @return 是 BCrypt 则 true
     */
    public static boolean isBcryptHash(String value) {
        return value != null
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /**
     * 比对管理员密码：哈希走 encoder，否则明文 equals
     *
     * @param passwordEncoder Spring BCrypt encoder
     * @param configured      配置中的密码或哈希
     * @param rawPassword     用户输入明文
     * @return 匹配 true
     */
    public static boolean matchesPassword(PasswordEncoder passwordEncoder, String configured, String rawPassword) {
        if (configured == null || rawPassword == null) {
            return false;
        }
        if (isBcryptHash(configured)) {
            return passwordEncoder.matches(rawPassword, configured);
        }
        return configured.equals(rawPassword);
    }

    /**
     * 硬件上报设备密钥比对；expected 为空则放行（本地联调）
     *
     * @param expected 配置中的 deviceApiKey
     * @param provided 请求头 X-Device-Key
     * @return 允许通过 true
     */
    public static boolean matchesDeviceKey(String expected, String provided) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equals(provided);
    }
}
