package com.ruoyi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.config.AiConfig;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.SensorData;
import com.ruoyi.entity.VisuallyImpairedUser;
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

    public record AiContext(Guardian guardian, VisuallyImpairedUser blindUser, CaneDevice device, SensorData latestSensorData, String latestAddress) {
    }

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
        return chat(messages, null);
    }

    public String chat(List<Map<String, String>> messages, AiContext context) throws Exception {
        List<Map<String, String>> fullMessages = buildMessages(messages, context);
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
        chatStream(messages, null, emitter);
    }

    public void chatStream(List<Map<String, String>> messages, AiContext context, SseEmitter emitter) {
        Thread thread = new Thread(() -> {
            try {
                List<Map<String, String>> fullMessages = buildMessages(messages, context);
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
        return buildMessages(userMessages, null);
    }

    private List<Map<String, String>> buildMessages(List<Map<String, String>> userMessages, AiContext context) {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(Map.of("role", "system", "content", buildSystemPrompt(context)));
        if (userMessages != null) {
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

    private String buildSystemPrompt(AiContext context) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        prompt.append("\n\n你可以读取下面这份由系统提供的真实业务上下文。回答时必须严格以这些数据为准，不要编造系统中不存在的信息。");
        prompt.append("\n如果用户询问‘我是谁’‘我的家人是谁’‘我的设备怎么样’‘我现在在哪里’等问题，请优先结合这份上下文回答。");
        prompt.append("\n如果系统没有提供某项数据，要明确说‘系统暂未获取到该信息’，不要说你完全无法访问项目数据。");
        prompt.append("\n要特别区分：‘档案住址’只是家庭住址，不等于实时位置；‘最新定位’才代表当前或最近一次设备位置。回答位置时要注明是‘根据系统最新一次定位’。\n");

        if (context == null) {
            prompt.append("\n当前业务上下文：暂无可用登录态信息。\n");
            return prompt.toString();
        }

        Guardian guardian = context.guardian();
        VisuallyImpairedUser blindUser = context.blindUser();
        CaneDevice device = context.device();
        SensorData latestSensorData = context.latestSensorData();
        String latestAddress = context.latestAddress();

        prompt.append("\n当前业务上下文如下：\n");
        prompt.append("1. 监护人信息\n");
        prompt.append("- 姓名：").append(valueOrDefault(guardian == null ? null : guardian.getName())).append("\n");
        prompt.append("- 电话：").append(valueOrDefault(guardian == null ? null : guardian.getPhone())).append("\n");
        prompt.append("- 关系：").append(valueOrDefault(guardian == null ? null : guardian.getRelation())).append("\n");

        prompt.append("2. 视障用户档案\n");
        prompt.append("- 姓名：").append(valueOrDefault(blindUser == null ? null : blindUser.getName())).append("\n");
        prompt.append("- 年龄：").append(valueOrDefault(blindUser == null ? null : blindUser.getAge())).append("\n");
        prompt.append("- 性别：").append(valueOrDefault(blindUser == null ? null : blindUser.getGender())).append("\n");
        prompt.append("- 联系电话：").append(valueOrDefault(blindUser == null ? null : blindUser.getPhone())).append("\n");
        prompt.append("- 家庭住址：").append(valueOrDefault(blindUser == null ? null : blindUser.getAddress())).append("\n");
        prompt.append("- 紧急联系人：").append(valueOrDefault(blindUser == null ? null : blindUser.getEmergencyContact())).append("\n");
        prompt.append("- 紧急联系电话：").append(valueOrDefault(blindUser == null ? null : blindUser.getEmergencyPhone())).append("\n");
        prompt.append("- 病史：").append(valueOrDefault(blindUser == null ? null : blindUser.getMedicalHistory())).append("\n");

        prompt.append("3. 当前绑定设备\n");
        prompt.append("- 设备编号：").append(valueOrDefault(device == null ? null : device.getDeviceId())).append("\n");
        prompt.append("- 设备名称：").append(valueOrDefault(device == null ? null : device.getDeviceName())).append("\n");
        prompt.append("- 设备状态：").append(valueOrDefault(device == null ? null : device.getStatus())).append("\n");
        prompt.append("- 电池电量：").append(valueOrDefault(device == null ? null : device.getBatteryLevel())).append("\n");

        prompt.append("4. 最新设备定位与传感器数据\n");
        prompt.append("- 最新中文地址：").append(valueOrDefault(latestAddress)).append("\n");
        prompt.append("- 最新定位纬度：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getLatitude())).append("\n");
        prompt.append("- 最新定位经度：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getLongitude())).append("\n");
        prompt.append("- 定位采集时间：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getDataTime())).append("\n");
        prompt.append("- 记录入库时间：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getCreateTime())).append("\n");
        prompt.append("- 障碍物距离：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getObstacleDistance())).append("\n");
        prompt.append("- 是否检测跌倒：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getIsFall())).append("\n");
        prompt.append("- 温度：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getTemperature())).append("\n");
        prompt.append("- 湿度：").append(valueOrDefault(latestSensorData == null ? null : latestSensorData.getHumidity())).append("\n");

        prompt.append("\n回答规则补充：如果用户问当前在哪里，且系统提供了最新中文地址，就优先回答‘根据系统最新一次定位，你现在在XX附近/XX一带’；如果没有中文地址但有经纬度，就明确说明这是根据设备最新一次定位得到的经纬度，不要编造街道名称，并提醒家属在地图页查看更直观的位置。\n");
        return prompt.toString();
    }

    private String valueOrDefault(Object value) {
        if (value == null) {
            return "系统暂未获取到";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "系统暂未获取到" : text;
    }
}
