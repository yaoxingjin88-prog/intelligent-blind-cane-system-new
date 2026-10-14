package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek 大模型配置，绑定 {@code ai.deepseek.*}
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiConfig {
    /** API Key，勿提交仓库 */
    private String apiKey;
    /** 接口根地址 */
    private String baseUrl = "https://api.deepseek.com";
    /** 模型名，如 deepseek-chat */
    private String model = "deepseek-chat";
    /** HTTP 超时（秒） */
    private int timeoutSeconds = 30;
}
