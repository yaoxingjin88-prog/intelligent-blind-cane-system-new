package com.ruoyi.config;

import com.ruoyi.interceptor.JwtInterceptor;
import com.ruoyi.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：CORS + 鉴权/限流拦截器注册
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final SecurityProperties securityProperties;

    /**
     * 跨域：允许管理端/小程序 H5 访问，来源取自 app.security.allowed-origins
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = securityProperties.allowedOriginArray();
        var registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // Origin Patterns 支持本地任意端口（如 Vite :3000）
        if (origins.length == 0) {
            registration.allowedOriginPatterns("https://intelligent.shinegoldyao.store");
        } else {
            registration.allowedOriginPatterns(origins);
        }
    }

    /**
     * 拦截器：AI/传感限流；全量 /api/** JWT（公开路径在拦截器内放行）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/**", "/api/sensor-data");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
    }
}
