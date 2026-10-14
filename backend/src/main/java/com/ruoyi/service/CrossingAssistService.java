package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CrossingAssistSnapshot;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路口辅助：缓存最新识别快照，并通过 WebSocket 推送给监护端/管理端
 */
@Service
@RequiredArgsConstructor
public class CrossingAssistService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MOCK_SCENE_COUNT = 5;

    private final Map<String, CrossingAssistSnapshot> latestSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastMockScenes = new ConcurrentHashMap<>();

    private final AlarmWebSocketHandler alarmWebSocketHandler;

    /**
     * 取设备最新路口辅助快照；无数据时返回默认「暂未识别」态
     *
     * @param deviceId 业务设备号
     * @return 快照
     */
    public CrossingAssistSnapshot getLatest(String deviceId) {
        CrossingAssistSnapshot snapshot = latestSnapshots.get(deviceId);
        if (snapshot != null) {
            return snapshot;
        }
        return buildDefaultSnapshot(deviceId);
    }

    /**
     * 写入/刷新路口辅助结果并推送
     * 业务步骤：1. 归一化缺省字段 2. 写入内存缓存 3. WebSocket 推送
     *
     * @param deviceId 业务设备号
     * @param snapshot 识别结果（可部分为空，由服务补全）
     * @return 归一化后的快照
     */
    public CrossingAssistSnapshot update(String deviceId, CrossingAssistSnapshot snapshot) {
        // 步骤1：归一化缺省字段
        CrossingAssistSnapshot normalized = normalize(deviceId, snapshot);
        // 步骤2：写入内存缓存
        latestSnapshots.put(deviceId, normalized);
        // 步骤3：WebSocket 推送
        alarmWebSocketHandler.sendCrossingAssist(deviceId, normalized);
        return normalized;
    }

    /**
     * 演示用：轮换预设场景并走同一套 update 推送链路
     *
     * @param deviceId 业务设备号
     * @return 推送后的快照
     */
    public CrossingAssistSnapshot mock(String deviceId) {
        CrossingAssistSnapshot snapshot = new CrossingAssistSnapshot();
        int scene = nextMockScene(deviceId);
        switch (scene) {
            case 0:
                snapshot.setTrafficLightStatus("RED");
                snapshot.setZebraCrossingDetected(true);
                snapshot.setZebraCrossingDirection("CENTER");
                snapshot.setVehicleApproaching(false);
                snapshot.setConfidence(0.94);
                snapshot.setRecommendation("WAIT");
                snapshot.setMessage("检测到红灯，斑马线居中，请原地等待");
                break;
            case 1:
                snapshot.setTrafficLightStatus("GREEN");
                snapshot.setZebraCrossingDetected(true);
                snapshot.setZebraCrossingDirection("LEFT");
                snapshot.setVehicleApproaching(false);
                snapshot.setConfidence(0.91);
                snapshot.setRecommendation("PROCEED_CAUTION");
                snapshot.setMessage("检测到绿灯，斑马线偏左，请略向左调整后谨慎通行");
                break;
            case 2:
                snapshot.setTrafficLightStatus("GREEN");
                snapshot.setZebraCrossingDetected(true);
                snapshot.setZebraCrossingDirection("CENTER");
                snapshot.setVehicleApproaching(false);
                snapshot.setConfidence(0.96);
                snapshot.setRecommendation("PROCEED_CAUTION");
                snapshot.setMessage("检测到绿灯，斑马线居中，可保持方向谨慎通行");
                break;
            case 3:
                snapshot.setTrafficLightStatus("YELLOW");
                snapshot.setZebraCrossingDetected(true);
                snapshot.setZebraCrossingDirection("RIGHT");
                snapshot.setVehicleApproaching(false);
                snapshot.setConfidence(0.88);
                snapshot.setRecommendation("WAIT");
                snapshot.setMessage("检测到黄灯，斑马线偏右，请先等待并稍向右对准");
                break;
            default:
                snapshot.setTrafficLightStatus("GREEN");
                snapshot.setZebraCrossingDetected(true);
                snapshot.setZebraCrossingDirection("CENTER");
                snapshot.setVehicleApproaching(true);
                snapshot.setConfidence(0.9);
                snapshot.setRecommendation("WAIT");
                snapshot.setMessage("检测到前方有车辆接近，请暂缓前进");
                break;
        }
        snapshot.setSource("mock-demo");
        return update(deviceId, snapshot);
    }

    private int nextMockScene(String deviceId) {
        Integer lastScene = lastMockScenes.get(deviceId);
        int scene = lastScene == null ? 0 : (lastScene + 1) % MOCK_SCENE_COUNT;
        lastMockScenes.put(deviceId, scene);
        return scene;
    }

    private CrossingAssistSnapshot normalize(String deviceId, CrossingAssistSnapshot snapshot) {
        CrossingAssistSnapshot normalized = snapshot == null ? new CrossingAssistSnapshot() : snapshot;
        normalized.setDeviceId(deviceId);
        if (normalized.getTrafficLightStatus() == null || normalized.getTrafficLightStatus().isBlank()) {
            normalized.setTrafficLightStatus("UNKNOWN");
        }
        if (normalized.getZebraCrossingDetected() == null) {
            normalized.setZebraCrossingDetected(Boolean.FALSE);
        }
        if (normalized.getZebraCrossingDirection() == null || normalized.getZebraCrossingDirection().isBlank()) {
            normalized.setZebraCrossingDirection("UNKNOWN");
        }
        if (normalized.getVehicleApproaching() == null) {
            normalized.setVehicleApproaching(Boolean.FALSE);
        }
        if (normalized.getSource() == null || normalized.getSource().isBlank()) {
            normalized.setSource("vision-module");
        }
        if (normalized.getRecommendation() == null || normalized.getRecommendation().isBlank()) {
            normalized.setRecommendation(buildRecommendation(normalized));
        }
        if (normalized.getMessage() == null || normalized.getMessage().isBlank()) {
            normalized.setMessage(buildMessage(normalized));
        }
        if (normalized.getConfidence() == null) {
            normalized.setConfidence(0.0);
        }
        normalized.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        return normalized;
    }

    private CrossingAssistSnapshot buildDefaultSnapshot(String deviceId) {
        CrossingAssistSnapshot snapshot = new CrossingAssistSnapshot();
        snapshot.setDeviceId(deviceId);
        snapshot.setTrafficLightStatus("UNKNOWN");
        snapshot.setZebraCrossingDetected(Boolean.FALSE);
        snapshot.setZebraCrossingDirection("UNKNOWN");
        snapshot.setVehicleApproaching(Boolean.FALSE);
        snapshot.setConfidence(0.0);
        snapshot.setSource("system");
        snapshot.setRecommendation("WAIT");
        snapshot.setMessage("暂未收到路口辅助识别结果");
        snapshot.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        return snapshot;
    }

    private String buildRecommendation(CrossingAssistSnapshot snapshot) {
        if (Boolean.TRUE.equals(snapshot.getVehicleApproaching())) {
            return "WAIT";
        }
        String light = snapshot.getTrafficLightStatus();
        if ("RED".equalsIgnoreCase(light) || "YELLOW".equalsIgnoreCase(light)) {
            return "WAIT";
        }
        if (Boolean.TRUE.equals(snapshot.getZebraCrossingDetected()) && "GREEN".equalsIgnoreCase(light)) {
            return "PROCEED_CAUTION";
        }
        if (Boolean.TRUE.equals(snapshot.getZebraCrossingDetected())) {
            return "ALIGN_FIRST";
        }
        return "SEARCH_ZEBRA";
    }

    private String buildMessage(CrossingAssistSnapshot snapshot) {
        if (Boolean.TRUE.equals(snapshot.getVehicleApproaching())) {
            return "检测到前方有车辆接近，请暂缓前进";
        }
        String light = snapshot.getTrafficLightStatus();
        String direction = toDirectionText(snapshot.getZebraCrossingDirection());
        if ("RED".equalsIgnoreCase(light)) {
            return "检测到红灯，请等待";
        }
        if ("YELLOW".equalsIgnoreCase(light)) {
            return "检测到黄灯，请先等待";
        }
        if (Boolean.TRUE.equals(snapshot.getZebraCrossingDetected()) && "GREEN".equalsIgnoreCase(light)) {
            if (direction.isEmpty()) {
                return "检测到绿灯，可谨慎通行";
            }
            return "检测到绿灯，斑马线" + direction + "，请调整方向后谨慎通行";
        }
        if (Boolean.TRUE.equals(snapshot.getZebraCrossingDetected())) {
            if (direction.isEmpty()) {
                return "已检测到斑马线，请继续确认通行条件";
            }
            return "已检测到斑马线" + direction + "，请先对准斑马线方向";
        }
        return "暂未识别到清晰斑马线，请缓慢调整方向";
    }

    private String toDirectionText(String direction) {
        if (direction == null) {
            return "";
        }
        switch (direction.toUpperCase()) {
            case "LEFT":
                return "偏左";
            case "RIGHT":
                return "偏右";
            case "CENTER":
                return "居中";
            default:
                return "";
        }
    }
}
