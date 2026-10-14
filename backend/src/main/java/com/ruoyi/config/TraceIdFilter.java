package com.ruoyi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路 TraceId 过滤器
 * 优先透传请求头 X-Trace-Id，否则生成；写入 MDC 供 logback 打印，并回写响应头
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    /** MDC / 日志 pattern 中的 key */
    public static final String TRACE_ID = "traceId";
    /** 请求/响应头名称 */
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 防止线程池复用时 MDC 泄漏
            MDC.remove(TRACE_ID);
        }
    }
}
