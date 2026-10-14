package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 百度语音（ASR/TTS）配置，绑定 {@code ai.baidu.*}
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.baidu")
public class BaiduSpeechConfig {
    /** 百度 API Key */
    private String apiKey;
    /** 百度 Secret Key */
    private String secretKey;
    /** TTS 发音人编号，默认 0 */
    private int ttsVoice = 0;
    /** TTS 语速，0-15，默认 5 */
    private int ttsSpeed = 5;
    /** TTS 音调，0-15，默认 5 */
    private int ttsPitch = 5;
    /** TTS 音量，0-15，默认 5 */
    private int ttsVolume = 5;
    /** HTTP 超时（秒） */
    private int timeoutSeconds = 60;
}
