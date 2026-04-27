package com.ruoyi.service;

import com.ruoyi.entity.CrossingAssistSnapshot;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CrossingAssistService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MOCK_SCENE_COUNT = 5;

    private final Map<String, CrossingAssistSnapshot> latestSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastMockScenes = new ConcurrentHashMap<>();

    @Autowired
    private AlarmWebSocketHandler alarmWebSocketHandler;

    public CrossingAssistSnapshot getLatest(String deviceId) {
        CrossingAssistSnapshot snapshot = latestSnapshots.get(deviceId);
        if (snapshot != null) {
            return snapshot;
        }
        return buildDefaultSnapshot(deviceId);
    }

    public CrossingAssistSnapshot update(String deviceId, CrossingAssistSnapshot snapshot) {
        CrossingAssistSnapshot normalized = normalize(deviceId, snapshot);
        latestSnapshots.put(deviceId, normalized);
        alarmWebSocketHandler.sendCrossingAssist(deviceId, normalized);
        return normalized;
    }

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
