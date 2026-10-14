package com.ruoyi.controller;

import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.SensorData;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.service.AmapGeocodingService;
import com.ruoyi.service.AiChatService;
import com.ruoyi.service.BaiduSpeechService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.GuardianService;
import com.ruoyi.service.SensorDataService;
import com.ruoyi.service.VisuallyImpairedUserService;
import com.ruoyi.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话与语音接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 对话助手", description = "基于 DeepSeek 的智能盲杖语音/文字 AI 助手")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AmapGeocodingService amapGeocodingService;
    private final BaiduSpeechService baiduSpeechService;
    private final GuardianService guardianService;
    private final VisuallyImpairedUserService visuallyImpairedUserService;
    private final CaneDeviceService caneDeviceService;
    private final SensorDataService sensorDataService;
    private final JwtUtil jwtUtil;

    /** 从 Authorization 头提取纯 token */
    private String extractToken(String token) {
        if (token == null) {
            return null;
        }
        return token.replace("Bearer", "").trim();
    }

    /** 按 JWT 组装监护人/用户/设备/位置上下文 */
    private AiChatService.AiContext buildAiContext(String authorization) {
        try {
            String token = extractToken(authorization);
            if (token == null || token.isBlank()) {
                return null;
            }
            String guardianId = jwtUtil.getUsernameFromToken(token);
            if (guardianId == null || guardianId.isBlank()) {
                return null;
            }
            Guardian guardian = guardianService.getGuardianById(Long.parseLong(guardianId));
            if (guardian == null) {
                return null;
            }
            VisuallyImpairedUser blindUser = null;
            CaneDevice device = null;
            SensorData latestSensorData = null;
            String latestAddress = null;
            if (guardian.getUserId() != null) {
                blindUser = visuallyImpairedUserService.getUserById(guardian.getUserId());
                device = caneDeviceService.getDeviceByUserId(guardian.getUserId());
                if (device != null && device.getDeviceId() != null && !device.getDeviceId().isBlank()) {
                    latestSensorData = sensorDataService.getLatestSensorData(device.getDeviceId());
                    if (latestSensorData != null) {
                        latestAddress = amapGeocodingService.reverseGeocode(latestSensorData.getLatitude(), latestSensorData.getLongitude());
                    }
                }
            }
            return new AiChatService.AiContext(guardian, blindUser, device, latestSensorData, latestAddress);
        } catch (Exception e) {
            log.warn("构建 AI 上下文失败", e);
            return null;
        }
    }

    /**
     * 一次性对话（非流式） POST /api/ai/chat
     *
     * @param req           消息列表
     * @param authorization 可选 JWT，用于注入监护人上下文
     * @return 业务 Map（code / data）
     */
    @Operation(summary = "AI 对话（非流式）")
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest req,
                                    @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> resp = new HashMap<>();
        try {
            // 调用业务层完成对话
            String answer = aiChatService.chat(req.getMessages(), buildAiContext(authorization));
            resp.put("code", 200);
            resp.put("data", Map.of("content", answer));
        } catch (Exception e) {
            log.error("AI 对话失败", e);
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        }
        return resp;
    }

    /**
     * 流式对话（SSE） POST /api/ai/chat/stream
     *
     * @param req           消息列表
     * @param authorization 可选 JWT
     * @return SSE 发射器
     */
    @Operation(summary = "AI 对话（流式 SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest req,
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        SseEmitter emitter = new SseEmitter(60_000L);
        // 调用业务层推送流式结果
        aiChatService.chatStream(req.getMessages(), buildAiContext(authorization), emitter);
        return emitter;
    }

    /**
     * 语音识别 STT POST /api/ai/stt
     *
     * @param file   录音文件
     * @param format 音频格式，默认 mp3
     * @param rate   采样率，默认 16000
     * @return 业务 Map（识别文本）
     */
    @Operation(summary = "语音识别 STT")
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> stt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "format", defaultValue = "mp3") String format,
            @RequestParam(value = "rate", defaultValue = "16000") int rate) {
        Map<String, Object> resp = new HashMap<>();
        try {
            byte[] audioBytes = file.getBytes();
            // 调用业务层做语音识别
            String text = baiduSpeechService.recognize(audioBytes, format, rate);
            resp.put("code", 200);
            resp.put("data", Map.of("text", text));
        } catch (Exception e) {
            log.error("语音识别失败", e);
            resp.put("code", 500);
            resp.put("msg", e.getMessage());
        }
        return resp;
    }

    /**
     * 语音合成 TTS POST /api/ai/tts
     *
     * @param req 待合成文本
     * @return MP3 二进制响应
     */
    @Operation(summary = "语音合成 TTS")
    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest req) {
        try {
            // 调用业务层合成语音
            byte[] mp3 = baiduSpeechService.synthesize(req.getText());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(mp3.length);
            return new ResponseEntity<>(mp3, headers, 200);
        } catch (Exception e) {
            log.error("语音合成失败", e);
            return ResponseEntity.status(500).body(("{\"msg\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    /** 文字对话入参 */
    @Data
    public static class ChatRequest {
        private List<Map<String, String>> messages;
    }

    /** TTS 入参 */
    @Data
    public static class TtsRequest {
        private String text;
    }
}
