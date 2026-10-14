package com.ruoyi.interceptor;

import com.ruoyi.config.SecurityProperties;
import com.ruoyi.utils.CredentialMatchers;
import com.ruoyi.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT / 设备密钥鉴权拦截：公开路径放行，传感上报校验 X-Device-Key，其余校验 Bearer Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String DEVICE_KEY_HEADER = "X-Device-Key";

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;

    /**
     * 鉴权入口：关闭鉴权或 OPTIONS/公开路径直接放行；传感 POST 走设备密钥；其余校验 JWT
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!securityProperties.isAuthEnabled()) {
            return true;
        }

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (!context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        if (isPublic(path, request.getMethod())) {
            return true;
        }

        // 硬件上报：不走 JWT，改校验共享设备密钥（未配置密钥时放行，便于本地联调）
        if (path.equals("/api/sensor-data") && HttpMethod.POST.matches(request.getMethod())) {
            return allowSensorIngest(request, response, path);
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
            if (jwtUtil.validateToken(token)) {
                return true;
            }
            log.warn("authz.reject path={} reason=invalid", path);
        } else {
            log.warn("authz.reject path={} reason=missing", path);
        }

        writeUnauthorized(response, "未登录或令牌无效");
        return false;
    }

    /** 传感入库：比对配置中的设备共享密钥 */
    private boolean allowSensorIngest(HttpServletRequest request, HttpServletResponse response, String path)
            throws Exception {
        String expected = securityProperties.getDeviceApiKey();
        String provided = request.getHeader(DEVICE_KEY_HEADER);
        if (CredentialMatchers.matchesDeviceKey(expected, provided)) {
            return true;
        }
        log.warn("authz.reject path={} reason=bad_device_key", path);
        writeUnauthorized(response, "设备密钥无效");
        return false;
    }

    /** 写出 401 JSON 响应体 */
    private static void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }

    /** 登录/健康检查/文档等无需 Token 的路径 */
    private boolean isPublic(String path, String method) {
        if (path.equals("/api/login") && HttpMethod.POST.matches(method)) {
            return true;
        }
        if (path.equals("/api/health") || path.startsWith("/api/health/")) {
            return true;
        }
        if (path.equals("/api/mini/login") || path.equals("/api/mini/register") || path.equals("/api/mini/wechat-login")) {
            return true;
        }
        if (path.equals("/api/device/ai-wake") && HttpMethod.POST.matches(method)) {
            return true;
        }
        // sensor-data POST 在 preHandle 中单独校验设备密钥，不在此放行
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/doc.html")
                || path.startsWith("/webjars")) {
            return true;
        }
        return false;
    }
}
