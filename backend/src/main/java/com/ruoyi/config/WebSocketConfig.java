package com.ruoyi.config;

import com.ruoyi.websocket.AlarmWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 注册报警/传感实时通道 {@code /ws/alarm}，跨域与 HTTP CORS 同源配置对齐
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlarmWebSocketHandler alarmWebSocketHandler;

    private final SecurityProperties securityProperties;

    /**
     * 注册实时推送 Handler
     *
     * @param registry WebSocket 注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = securityProperties.allowedOriginArray();
        var registration = registry.addHandler(alarmWebSocketHandler, "/ws/alarm");
        if (origins.length == 0) {
            registration.setAllowedOriginPatterns("https://intelligent.shinegoldyao.store");
        } else {
            registration.setAllowedOriginPatterns(origins);
        }
    }
}
