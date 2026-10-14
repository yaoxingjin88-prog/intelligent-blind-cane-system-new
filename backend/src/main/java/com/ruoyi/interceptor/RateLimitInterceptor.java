package com.ruoyi.interceptor;

import com.ruoyi.common.TooManyRequestsException;
import com.ruoyi.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 接口限流拦截：AI 按客户端 IP，传感上报按设备号/IP，超限抛 429
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.ai-per-minute:20}")
    private int aiPerMinute;

    @Value("${app.rate-limit.sensor-per-minute:120}")
    private int sensorPerMinute;

    /**
     * 对 /api/ai/** 与传感 POST 做滑动窗口限流
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (!context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        String client = clientKey(request);

        if (path.startsWith("/api/ai/")) {
            String key = "ai:" + client;
            if (!rateLimitService.tryAcquire(key, aiPerMinute, 60_000L)) {
                throw new TooManyRequestsException("AI 接口调用过于频繁，请稍后再试");
            }
        }

        if (HttpMethod.POST.matches(request.getMethod()) && "/api/sensor-data".equals(path)) {
            String deviceHint = request.getHeader("X-Device-Id");
            if (deviceHint == null || deviceHint.isBlank()) {
                deviceHint = client;
            }
            String key = "sensor:" + deviceHint;
            if (!rateLimitService.tryAcquire(key, sensorPerMinute, 60_000L)) {
                throw new TooManyRequestsException("传感器上报过于频繁，请稍后再试");
            }
        }

        return true;
    }

    /** 取客户端标识：优先 X-Forwarded-For 首段，否则 remoteAddr */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
