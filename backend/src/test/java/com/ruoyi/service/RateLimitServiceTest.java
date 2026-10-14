package com.ruoyi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void allowsUntilLimitThenBlocks() {
        RateLimitService service = new RateLimitService();
        String key = "test:ai";
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertFalse(service.tryAcquire(key, 3, 60_000L));
    }

    @Test
    void disabledWhenLimitNonPositive() {
        RateLimitService service = new RateLimitService();
        assertTrue(service.tryAcquire("k", 0, 60_000L));
        assertTrue(service.tryAcquire("k", -1, 60_000L));
    }
}
