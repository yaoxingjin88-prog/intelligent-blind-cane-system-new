package com.ruoyi.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CrossingAssistSnapshot;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报警 / 围栏 / 过街辅助等实时推送 WebSocket 处理器
 * <p>
 * 连接约定：{@code /ws/alarm?deviceId=xxx}，按设备维度维护会话。
 * 下行消息 {@code type}：ALARM、SENSOR_DATA、FENCE_STATUS、CROSSING_ASSIST、
 * AI_WAKE、GUARDIAN_ALERT、GUARDIAN_COMFORT、GUARDIAN_DESTINATION、CONNECTED、PONG。
 * 上行仅处理心跳 PING → PONG。
 */
@Slf4j
@Component
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    /** deviceId → (sessionId → session)，同一设备可多端在线 */
    private static final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立：登记会话并回 CONNECTED
     *
     * @param session 新建会话（URI 需带 deviceId）
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String deviceId = getDeviceIdFromSession(session);
        if (deviceId != null) {
            sessions.computeIfAbsent(deviceId, key -> new ConcurrentHashMap<>()).put(session.getId(), session);
            log.info("WebSocket连接建立: deviceId={}, sessionId={}", deviceId, session.getId());
            sendMessage(session, Map.of(
                "type", "CONNECTED",
                "deviceId", deviceId,
                "message", "连接成功"
            ));
        }
    }

    /**
     * 连接关闭：从设备会话表移除；该设备无连接时清理外层 key
     *
     * @param session 关闭的会话
     * @param status  关闭状态码
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String deviceId = getDeviceIdFromSession(session);
        if (deviceId != null) {
            Map<String, WebSocketSession> deviceSessions = sessions.get(deviceId);
            if (deviceSessions != null) {
                deviceSessions.remove(session.getId());
                if (deviceSessions.isEmpty()) {
                    sessions.remove(deviceId);
                }
            }
            log.info("WebSocket连接关闭: deviceId={}, sessionId={}", deviceId, session.getId());
        }
    }

    /**
     * 处理客户端文本消息（当前仅心跳）
     *
     * @param session 来源会话
     * @param message 文本帧
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息 length={}", payload == null ? 0 : payload.length());

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            // 心跳：PING → PONG
            if ("PING".equals(type)) {
                sendMessage(session, Map.of("type", "PONG", "time", System.currentTimeMillis()));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    /**
     * 向指定设备推送报警（type=ALARM）
     *
     * @param deviceId    设备号
     * @param alarmRecord 报警实体
     */
    public void sendAlarmNotification(String deviceId, AlarmRecord alarmRecord) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ALARM");
        message.put("deviceId", deviceId);
        message.put("alarm", alarmRecord);
        message.put("message", "设备发生" + alarmRecord.getAlarmType() + "，请及时处理！");
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 推送最新传感器数据；可选附带围栏评估结果（type=SENSOR_DATA）
     *
     * @param deviceId              设备号
     * @param sensorData            传感器点位
     * @param fenceEvaluationResult 围栏评估，可为 null
     */
    public void sendSensorData(String deviceId, SensorData sensorData, FenceEvaluationResult fenceEvaluationResult) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SENSOR_DATA");
        message.put("deviceId", deviceId);
        message.put("sensorData", sensorData);
        message.put("timestamp", System.currentTimeMillis());
        if (fenceEvaluationResult != null) {
            message.put("fence", buildFencePayload(fenceEvaluationResult));
        }
        sendToDevice(deviceId, message);
    }

    /**
     * 盲杖 AI 按键：通知手机端跳转 AI 对话并开始录音（type=AI_WAKE）
     *
     * @param deviceId 设备号
     */
    public void sendAiWake(String deviceId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "AI_WAKE");
        message.put("deviceId", deviceId);
        message.put("message", "盲杖 AI 按键唤醒");
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 单独推送围栏状态（type=FENCE_STATUS）
     *
     * @param deviceId              设备号
     * @param fenceEvaluationResult 评估结果，null 则忽略
     */
    public void sendFenceStatus(String deviceId, FenceEvaluationResult fenceEvaluationResult) {
        if (fenceEvaluationResult == null) {
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("type", "FENCE_STATUS");
        message.put("deviceId", deviceId);
        message.put("fence", buildFencePayload(fenceEvaluationResult));
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 推送过街辅助快照（type=CROSSING_ASSIST）
     *
     * @param deviceId 设备号
     * @param snapshot 过街提示快照，null 则忽略
     */
    public void sendCrossingAssist(String deviceId, CrossingAssistSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CROSSING_ASSIST");
        message.put("deviceId", deviceId);
        message.put("crossingAssist", snapshot);
        message.put("message", snapshot.getMessage());
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 监护人告警（无位置文案）
     *
     * @param deviceId    设备号
     * @param alertType   告警类型
     * @param level       级别
     * @param messageText 展示文案
     */
    public void sendGuardianAlert(String deviceId, String alertType, String level, String messageText) {
        sendGuardianAlert(deviceId, alertType, level, messageText, null);
    }

    /**
     * 监护人告警（type=GUARDIAN_ALERT），可选带位置描述
     *
     * @param deviceId     设备号
     * @param alertType    告警类型
     * @param level        级别
     * @param messageText  展示文案
     * @param locationText 位置描述，可空
     */
    public void sendGuardianAlert(String deviceId, String alertType, String level, String messageText, String locationText) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "GUARDIAN_ALERT");
        message.put("deviceId", deviceId);
        message.put("alertType", alertType);
        message.put("level", level);
        message.put("message", messageText);
        if (locationText != null && !locationText.trim().isEmpty()) {
            message.put("locationText", locationText);
        }
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 监护人安抚消息（type=GUARDIAN_COMFORT）
     *
     * @param deviceId 设备号
     * @param content  安抚内容
     */
    public void sendGuardianComfort(String deviceId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "GUARDIAN_COMFORT");
        message.put("deviceId", deviceId);
        message.put("content", content);
        message.put("message", content);
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 监护人下发目的地（type=GUARDIAN_DESTINATION）
     *
     * @param deviceId    设备号
     * @param destination 目的地文本
     * @param messageText 提示文案
     */
    public void sendGuardianDestination(String deviceId, String destination, String messageText) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "GUARDIAN_DESTINATION");
        message.put("deviceId", deviceId);
        message.put("destination", destination);
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

    /**
     * 向所有在线会话广播报警（管理端大屏等）
     *
     * @param alarmRecord 报警实体
     */
    public void broadcastAlarm(AlarmRecord alarmRecord) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ALARM");
        message.put("alarm", alarmRecord);
        message.put("message", "设备发生" + alarmRecord.getAlarmType() + "，请及时处理！");
        message.put("timestamp", System.currentTimeMillis());
        broadcastToAll(message);
    }

    /**
     * 向指定设备会话推送「报警已处理」（家属端）
     */
    public void sendAlarmHandled(String deviceId, AlarmRecord alarmRecord, String handledBy, String messageText) {
        Map<String, Object> message = buildAlarmHandledPayload(alarmRecord, handledBy, messageText);
        sendToDevice(deviceId, message);
    }

    /**
     * 广播「报警已处理」（管理端铃铛/监控页）
     */
    public void broadcastAlarmHandled(AlarmRecord alarmRecord, String handledBy, String messageText) {
        broadcastToAll(buildAlarmHandledPayload(alarmRecord, handledBy, messageText));
    }

    private Map<String, Object> buildAlarmHandledPayload(AlarmRecord alarmRecord, String handledBy, String messageText) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ALARM_HANDLED");
        message.put("alarm", alarmRecord);
        message.put("deviceId", alarmRecord == null ? null : alarmRecord.getDeviceId());
        message.put("handledBy", handledBy);
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }

    private void broadcastToAll(Map<String, Object> message) {
        sessions.values().forEach(deviceSessions -> deviceSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("广播 WebSocket 消息失败 type={}", message.get("type"), e);
                }
            }
        }));
    }

    /**
     * 从 URI query 解析 deviceId（形如 {@code deviceId=xxx}）
     *
     * @param session WebSocket 会话
     * @return 设备号，解析失败则 null
     */
    private String getDeviceIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "deviceId".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }

    /**
     * 当前所有已连接的 deviceId，便于排查问题
     *
     * @return 在线设备号集合（只读视图来自 keySet）
     */
    public java.util.Set<String> getOnlineDeviceIds() {
        return sessions.keySet();
    }

    /**
     * 向指定设备的全部打开会话发送消息；无人在线则打 warn
     *
     * @param deviceId 设备号
     * @param message  已组好的下行载荷
     */
    private void sendToDevice(String deviceId, Map<String, Object> message) {
        Map<String, WebSocketSession> deviceSessions = sessions.get(deviceId);
        if (deviceSessions == null || deviceSessions.isEmpty()) {
            log.warn("设备未连接WebSocket: deviceId={}, 当前在线设备={}", deviceId, sessions.keySet());
            return;
        }
        deviceSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("发送WebSocket消息失败: deviceId={}, sessionId={}", deviceId, session.getId(), e);
                }
            }
        });
    }

    /**
     * 围栏评估结果转前端 payload（fence / distance / outside / triggered）
     *
     * @param fenceEvaluationResult 评估结果
     * @return 嵌套 Map
     */
    private Map<String, Object> buildFencePayload(FenceEvaluationResult fenceEvaluationResult) {
        Map<String, Object> fencePayload = new HashMap<>();
        fencePayload.put("fence", fenceEvaluationResult.getFence());
        fencePayload.put("distanceMeters", fenceEvaluationResult.getDistanceMeters());
        fencePayload.put("outside", fenceEvaluationResult.getOutside());
        fencePayload.put("triggered", fenceEvaluationResult.getTriggered());
        return fencePayload;
    }

    /**
     * 序列化为 JSON 文本帧写出
     *
     * @param session 目标会话
     * @param message 载荷 Map
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
}
