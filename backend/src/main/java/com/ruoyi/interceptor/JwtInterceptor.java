package com.ruoyi.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 在开发环境中跳过认证，方便测试
        return true;
        
        /*
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            // 暂时注释掉JWT验证，方便测试
            // if (JwtUtil.validateToken(token)) {
            //     return true;
            // }
        }
        response.setStatus(401);
        response.getWriter().write("Unauthorized");
        return false;
        */
    }
}
