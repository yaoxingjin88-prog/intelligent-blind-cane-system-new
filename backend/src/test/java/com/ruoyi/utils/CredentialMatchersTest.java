package com.ruoyi.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialMatchersTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void matchesPlaintextPassword() {
        assertTrue(CredentialMatchers.matchesPassword(encoder, "123456", "123456"));
        assertFalse(CredentialMatchers.matchesPassword(encoder, "123456", "wrong"));
    }

    @Test
    void matchesBcryptPassword() {
        String hash = encoder.encode("secret-pass");
        assertTrue(CredentialMatchers.isBcryptHash(hash));
        assertTrue(CredentialMatchers.matchesPassword(encoder, hash, "secret-pass"));
        assertFalse(CredentialMatchers.matchesPassword(encoder, hash, "123456"));
    }

    @Test
    void deviceKeyBlankMeansOpen() {
        assertTrue(CredentialMatchers.matchesDeviceKey("", "anything"));
        assertTrue(CredentialMatchers.matchesDeviceKey(null, null));
    }

    @Test
    void deviceKeyMustMatchWhenConfigured() {
        assertTrue(CredentialMatchers.matchesDeviceKey("hw-key", "hw-key"));
        assertFalse(CredentialMatchers.matchesDeviceKey("hw-key", "wrong"));
        assertFalse(CredentialMatchers.matchesDeviceKey("hw-key", null));
    }
}
