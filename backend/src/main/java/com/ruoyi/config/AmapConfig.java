package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 高德 Web 服务配置，绑定 {@code amap.*}（逆地理等）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    /** 高德 Web Key */
    private String webKey;
    /** 接口根地址 */
    private String baseUrl = "https://restapi.amap.com";
    /** HTTP 超时（秒） */
    private int timeoutSeconds = 8;
}
