package com.ruoyi.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimitService} 单测：无 Redis 时验证进程内滑动窗口限流
 */
class RateLimitServiceTest {

    /**
     * 构造仅本地限流的服务（不注入 StringRedisTemplate）
     *
     * @return RateLimitService
     */
    @SuppressWarnings("unchecked")
    private RateLimitService newLocalService() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new RateLimitService(provider);
    }

    /** 未超限放行，达到 limit 后第 N+1 次拒绝 */
    @Test
    void allowsUntilLimitThenBlocks() {
        RateLimitService service = newLocalService();
        String key = "test:ai";
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertTrue(service.tryAcquire(key, 3, 60_000L));
        assertFalse(service.tryAcquire(key, 3, 60_000L));
    }

    /** limit ≤ 0 时视为不限流，始终放行 */
    @Test
    void disabledWhenLimitNonPositive() {
        RateLimitService service = newLocalService();
        assertTrue(service.tryAcquire("k", 0, 60_000L));
        assertTrue(service.tryAcquire("k", -1, 60_000L));
    }
}
