package com.ruoyi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * DeepSeek 大模型对话服务
 * 为视障用户提供 AI 助手：导航、设备操作、紧急处理等答疑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiConfig aiConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 面向智能盲杖视障用户的 system prompt
     */
    private static final String SYSTEM_PROMPT = """
            你是"明眼助手"，一款为视障用户（使用智能盲杖）设计的 AI 助手。
            回答要求：
            1. 回答要具体、详细、可操作，给出分步骤的建议，而不是笼统的一句话。
            2. 使用"首先、其次、然后、最后"这类顺序词，把步骤讲清楚。
            3. 通常回答 200-400 字较合适；若是简单寒暄则简短自然回应即可。
            4. 用词通俗易懂，避免专业术语；老人也能听懂。
            5. 涉及紧急情况（跌倒、迷路、身体不适、交通事故）：先安抚情绪，再明确指引（比如"请立刻按盲杖上的 SOS 键/拨打 120/联系家属"），并给出在等待期间的注意事项。
            6. 关于盲杖使用、设备操作、出行路线、无障碍设施、健康科普等问题要优先、详尽解答，可以列举场景和小技巧。
            7. 回答涉及方向/方位时，使用"左手边、右手边、前方、身后"这类视障用户易理解的方式，避免"东南西北"。
            8. 不回答政治、色情、暴力等话题，委婉拒绝并引导回到助手能帮的范围。
            9. 语气温和、耐心、亲切，像一个体贴的家人。
            10. 不要使用 Markdown 符号（如 **、#、- 等）；因为回答会被朗读，请直接用中文标点和自然段落。
            """;

    /**
     * 一次性对话（非流式）
     * @param messages 用户历史消息 [{role:"user",content:"..."},{role:"assistant",content:"..."}]
     * @return AI 回复文字
     */
    public String chat(List<Map<String, String>> messages) throws Exception {
        List<Map<String, String>> fullMessages = buildMessages(messages);
        Map<String, Object> body = new HashMap<>();
        body.put("model", aiConfig.getModel());
        body.put("messages", fullMessages);
        body.put("stream", false);
        body.put("temperature", 0.8);
        body.put("max_tokens", 1500);

        String json = mapper.writeValueAsString(body);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiConfig.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            log.error("DeepSeek 接口错误: status={}, body={}", resp.statusCode(), resp.body());
            throw new RuntimeException("AI 服务异常: " + resp.statusCode());
        }
        JsonNode root = mapper.readTree(resp.body());
        return root.path("choices").get(0).path("message").path("content").asText("");
    }

    /**
     * 流式对话（SSE）
     */
    public void chatStream(List<Map<String, String>> messages, SseEmitter emitter) {
        Thread thread = new Thread(() -> {
            try {
                List<Map<String, String>> fullMessages = buildMessages(messages);
                Map<String, Object> body = new HashMap<>();
                body.put("model", aiConfig.getModel());
                body.put("messages", fullMessages);
                body.put("stream", true);
                body.put("temperature", 0.8);
                body.put("max_tokens", 1500);

                String json = mapper.writeValueAsString(body);
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                        .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + aiConfig.getApiKey())
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<java.io.InputStream> resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 400) {
                    emitter.send(SseEmitter.event().name("error").data("AI 接口错误: " + resp.statusCode()));
                    emitter.complete();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder fullText = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonNode node = mapper.readTree(data);
                            String delta = node.path("choices").get(0).path("delta").path("content").asText("");
                            if (!delta.isEmpty()) {
                                fullText.append(delta);
                                emitter.send(SseEmitter.event().name("delta").data(delta));
                            }
                        } catch (Exception ex) {
                            log.warn("解析流数据失败: {}", data);
                        }
                    }
                    emitter.send(SseEmitter.event().name("done").data(fullText.toString()));
                    emitter.complete();
                }
            } catch (IOException | InterruptedException e) {
                log.error("AI 流式对话失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private List<Map<String, String>> buildMessages(List<Map<String, String>> userMessages) {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (userMessages != null) {
            // 最多保留最近 10 轮历史，避免 token 爆炸
            int start = Math.max(0, userMessages.size() - 10);
            for (int i = start; i < userMessages.size(); i++) {
                Map<String, String> m = userMessages.get(i);
                String role = m.getOrDefault("role", "user");
                String content = m.getOrDefault("content", "");
                if (!content.isBlank()) {
                    list.add(Map.of("role", role, "content", content));
                }
            }
        }
        return list;
    }
}
