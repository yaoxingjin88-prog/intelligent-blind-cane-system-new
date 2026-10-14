package com.ruoyi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流：优先 Redis 固定窗口（多实例一致）；未启用 Redis 时回退进程内滑动窗口。
 */
@Service
@Slf4j
public class RateLimitService {
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    /**
     * 尝试获取令牌：窗口内请求数未超限则记录并放行
     *
     * @param key      限流键（如 ai:ip、sensor:deviceId）
     * @param limit    窗口内最大次数；≤0 表示不限制
     * @param windowMs 窗口毫秒；≤0 表示不限制
     * @return true 放行，false 超限
     */
    public boolean tryAcquire(String key, int limit, long windowMs) {
        if (limit <= 0 || windowMs <= 0) {
            return true;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                return tryAcquireRedis(redis, key, limit, windowMs);
            } catch (Exception e) {
                log.warn("redis rate-limit fallback to local key={}: {}", key, e.getMessage());
            }
        }
        return tryAcquireLocal(key, limit, windowMs);
    }

    private boolean tryAcquireRedis(StringRedisTemplate redis, String key, int limit, long windowMs) {
        String redisKey = "rl:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, Duration.ofMillis(windowMs));
        }
        return count != null && count <= limit;
    }

    private boolean tryAcquireLocal(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }
}
