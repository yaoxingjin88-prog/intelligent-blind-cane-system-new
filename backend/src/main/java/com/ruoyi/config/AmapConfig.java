package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    private String webKey;
    private String baseUrl = "https://restapi.amap.com";
    private int timeoutSeconds = 8;
}
