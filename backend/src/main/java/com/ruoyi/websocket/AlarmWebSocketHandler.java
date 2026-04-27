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

@Slf4j
@Component
public class AlarmWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String deviceId = getDeviceIdFromSession(session);
        if (deviceId != null) {
            sessions.computeIfAbsent(deviceId, key -> new ConcurrentHashMap<>()).put(session.getId(), session);
            log.info("WebSocket连接建立: deviceId={}, sessionId={}", deviceId, session.getId());
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "CONNECTED",
                "deviceId", deviceId,
                "message", "连接成功"
            ));
        }
    }

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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到WebSocket消息: {}", payload);
        
        // 可以处理前端发送的心跳消息等
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            
            if ("PING".equals(type)) {
                sendMessage(session, Map.of("type", "PONG", "time", System.currentTimeMillis()));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    /**
     * 发送报警通知给指定设备
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
     * 盲杖按下 AI 按键：通知手机端跳转到 AI 对话页并开始录音
     */
    public void sendAiWake(String deviceId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "AI_WAKE");
        message.put("deviceId", deviceId);
        message.put("message", "盲杖 AI 按键唤醒");
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

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

    public void sendGuardianAlert(String deviceId, String alertType, String level, String messageText) {
        sendGuardianAlert(deviceId, alertType, level, messageText, null);
    }

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

    public void sendGuardianComfort(String deviceId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "GUARDIAN_COMFORT");
        message.put("deviceId", deviceId);
        message.put("content", content);
        message.put("message", content);
        message.put("timestamp", System.currentTimeMillis());
        sendToDevice(deviceId, message);
    }

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
     * 广播报警通知给所有连接的客户端
     */
    public void broadcastAlarm(AlarmRecord alarmRecord) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ALARM");
        message.put("alarm", alarmRecord);
        message.put("message", "设备发生" + alarmRecord.getAlarmType() + "，请及时处理！");
        message.put("timestamp", System.currentTimeMillis());

        sessions.values().forEach(deviceSessions -> deviceSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("广播报警通知失败", e);
                }
            }
        }));
    }

    private String getDeviceIdFromSession(WebSocketSession session) {
        // 从URL参数中获取deviceId
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

    /** 当前所有已连接的 deviceId，便于排查问题 */
    public java.util.Set<String> getOnlineDeviceIds() {
        return sessions.keySet();
    }

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

    private Map<String, Object> buildFencePayload(FenceEvaluationResult fenceEvaluationResult) {
        Map<String, Object> fencePayload = new HashMap<>();
        fencePayload.put("fence", fenceEvaluationResult.getFence());
        fencePayload.put("distanceMeters", fenceEvaluationResult.getDistanceMeters());
        fencePayload.put("outside", fenceEvaluationResult.getOutside());
        fencePayload.put("triggered", fenceEvaluationResult.getTriggered());
        return fencePayload;
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }
}
