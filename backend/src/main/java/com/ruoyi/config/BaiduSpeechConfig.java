package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.baidu")
public class BaiduSpeechConfig {
    private String apiKey;
    private String secretKey;
    private int ttsVoice = 0;
    private int ttsSpeed = 5;
    private int ttsPitch = 5;
    private int ttsVolume = 5;
    private int timeoutSeconds = 60;
}
