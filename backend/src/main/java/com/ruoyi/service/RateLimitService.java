package com.ruoyi.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内滑动窗口限流（单机有效；多实例需换 Redis）
 */
@Service
public class RateLimitService {
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

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
