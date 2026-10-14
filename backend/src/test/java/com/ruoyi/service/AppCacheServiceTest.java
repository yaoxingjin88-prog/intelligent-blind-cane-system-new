package com.ruoyi.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link AppCacheService} 单测：无 Redis 时走本机 ConcurrentHashMap 回退
 */
class AppCacheServiceTest {

    /**
     * 构造「未启用 Redis」的缓存实例（getIfAvailable 恒为 null）
     *
     * @return 仅本地回退的 AppCacheService
     */
    @SuppressWarnings("unchecked")
    private AppCacheService newLocalCache() {
        ObjectProvider<StringRedisTemplate> redis = mock(ObjectProvider.class);
        ObjectProvider<AnalyticsService> analytics = mock(ObjectProvider.class);
        ObjectProvider<MonitorOverviewService> monitor = mock(ObjectProvider.class);
        when(redis.getIfAvailable()).thenReturn(null);
        when(analytics.getIfAvailable()).thenReturn(null);
        when(monitor.getIfAvailable()).thenReturn(null);
        return new AppCacheService(redis, analytics, monitor);
    }

    /** 写入后可读回同一 key 的 Map 字段 */
    @Test
    void putAndGetFromLocalFallback() {
        AppCacheService cache = newLocalCache();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCount", 6);
        cache.putMap(AppCacheService.KEY_DASHBOARD, data, Duration.ofSeconds(30));

        Optional<Map<String, Object>> hit = cache.getMap(AppCacheService.KEY_DASHBOARD);
        assertTrue(hit.isPresent());
        assertEquals(6, hit.get().get("deviceCount"));
    }

    /** evict 后同一 key 应 miss */
    @Test
    void evictRemovesLocalEntry() {
        AppCacheService cache = newLocalCache();
        cache.putMap(AppCacheService.KEY_MONITOR_OVERVIEW, Map.of("ok", true), Duration.ofSeconds(30));
        cache.evict(AppCacheService.KEY_MONITOR_OVERVIEW);
        assertTrue(cache.getMap(AppCacheService.KEY_MONITOR_OVERVIEW).isEmpty());
    }
}
