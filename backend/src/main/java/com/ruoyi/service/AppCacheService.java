package com.ruoyi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用级短缓存：有 Redis 则共享，否则回退本机 ConcurrentHashMap。
 * 用于看板 / 监控总览等可短时复用的聚合结果。
 */
@Service
@Slf4j
public class AppCacheService {

    public static final String KEY_DASHBOARD = "cache:dashboard";
    public static final String KEY_MONITOR_OVERVIEW = "cache:monitor:overview";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectProvider<AnalyticsService> analyticsProvider;
    private final ObjectProvider<MonitorOverviewService> monitorOverviewProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, LocalEntry> local = new ConcurrentHashMap<>();

    public AppCacheService(
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<AnalyticsService> analyticsProvider,
            ObjectProvider<MonitorOverviewService> monitorOverviewProvider) {
        this.redisProvider = redisProvider;
        this.analyticsProvider = analyticsProvider;
        this.monitorOverviewProvider = monitorOverviewProvider;
    }

    /** 是否已启用 Redis Bean（不等于一定能连上） */
    public boolean redisEnabled() {
        return redisProvider.getIfAvailable() != null;
    }

    public Optional<Map<String, Object>> getMap(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(key);
                if (json != null && !json.isBlank()) {
                    Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {});
                    return Optional.ofNullable(data);
                }
            } catch (Exception e) {
                log.debug("redis.get miss/fail key={}: {}", key, e.getMessage());
            }
        }
        LocalEntry entry = local.get(key);
        if (entry != null && entry.expiresAt > System.currentTimeMillis()) {
            return Optional.of(entry.data);
        }
        if (entry != null) {
            local.remove(key, entry);
        }
        return Optional.empty();
    }

    public void putMap(String key, Map<String, Object> data, Duration ttl) {
        if (data == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        local.put(key, new LocalEntry(data, expiresAt));

        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(data);
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.debug("redis.put fail key={}: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        local.remove(key);
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.debug("redis.del fail key={}: {}", key, e.getMessage());
        }
    }

    /** 传感/报警变更后失效看板相关缓存（含各 Service 本机 L1） */
    public void evictDashboardCaches() {
        evict(KEY_DASHBOARD);
        evict(KEY_MONITOR_OVERVIEW);
        AnalyticsService analytics = analyticsProvider.getIfAvailable();
        if (analytics != null) {
            analytics.invalidateLocalCache();
        }
        MonitorOverviewService monitor = monitorOverviewProvider.getIfAvailable();
        if (monitor != null) {
            monitor.invalidateLocalCache();
        }
    }

    private record LocalEntry(Map<String, Object> data, long expiresAt) {
    }
}
