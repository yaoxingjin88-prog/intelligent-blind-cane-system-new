package com.ruoyi.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CredentialMatchers} 单测：管理端口令（明文/BCrypt）与设备密钥比对
 */
class CredentialMatchersTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 配置为明文时 equals 比对 */
    @Test
    void matchesPlaintextPassword() {
        assertTrue(CredentialMatchers.matchesPassword(encoder, "123456", "123456"));
        assertFalse(CredentialMatchers.matchesPassword(encoder, "123456", "wrong"));
    }

    /** 配置为 BCrypt 哈希时走 encoder.matches */
    @Test
    void matchesBcryptPassword() {
        String hash = encoder.encode("secret-pass");
        assertTrue(CredentialMatchers.isBcryptHash(hash));
        assertTrue(CredentialMatchers.matchesPassword(encoder, hash, "secret-pass"));
        assertFalse(CredentialMatchers.matchesPassword(encoder, hash, "123456"));
    }

    /** 设备密钥未配置（空/null）时放行，便于本地联调 */
    @Test
    void deviceKeyBlankMeansOpen() {
        assertTrue(CredentialMatchers.matchesDeviceKey("", "anything"));
        assertTrue(CredentialMatchers.matchesDeviceKey(null, null));
    }

    /** 已配置设备密钥时必须与请求头一致 */
    @Test
    void deviceKeyMustMatchWhenConfigured() {
        assertTrue(CredentialMatchers.matchesDeviceKey("hw-key", "hw-key"));
        assertFalse(CredentialMatchers.matchesDeviceKey("hw-key", "wrong"));
        assertFalse(CredentialMatchers.matchesDeviceKey("hw-key", null));
    }
}
