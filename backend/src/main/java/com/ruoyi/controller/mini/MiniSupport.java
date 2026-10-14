package com.ruoyi.controller.mini;

import lombok.RequiredArgsConstructor;
import com.ruoyi.common.UnauthorizedException;
import com.ruoyi.config.SecurityProperties;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.SensorData;
import com.ruoyi.service.AlarmNotifyService;
import com.ruoyi.service.AlarmRecordService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.CrossingAssistService;
import com.ruoyi.service.ElectronicFenceService;
import com.ruoyi.service.FeedbackService;
import com.ruoyi.service.GuardianService;
import com.ruoyi.service.MiniCareOverviewService;
import com.ruoyi.service.SensorDataService;
import com.ruoyi.service.VisuallyImpairedUserService;
import com.ruoyi.utils.JwtUtil;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 小程序 Controller 共享依赖与鉴权辅助
 */
@Component
@RequiredArgsConstructor
public class MiniSupport {

    @Value("${demo.location.force:false}")
    private Boolean demoLocationForce;

    @Value("${demo.location.latitude:36.321143}")
    private Double demoLocationLatitude;

    @Value("${demo.location.longitude:120.397571}")
    private Double demoLocationLongitude;

    private final GuardianService guardianService;
    private final CaneDeviceService caneDeviceService;
    private final AlarmRecordService alarmRecordService;
    private final AlarmNotifyService alarmNotifyService;
    private final FeedbackService feedbackService;
    private final ElectronicFenceService electronicFenceService;
    private final SensorDataService sensorDataService;
    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;
    private final VisuallyImpairedUserService visuallyImpairedUserService;
    private final CrossingAssistService crossingAssistService;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final MiniCareOverviewService miniCareOverviewService;

    public GuardianService guardianService() {
        return guardianService;
    }

    public CaneDeviceService caneDeviceService() {
        return caneDeviceService;
    }

    public AlarmRecordService alarmRecordService() {
        return alarmRecordService;
    }

    public AlarmNotifyService alarmNotifyService() {
        return alarmNotifyService;
    }

    public FeedbackService feedbackService() {
        return feedbackService;
    }

    public ElectronicFenceService electronicFenceService() {
        return electronicFenceService;
    }

    public SensorDataService sensorDataService() {
        return sensorDataService;
    }

    public JwtUtil jwtUtil() {
        return jwtUtil;
    }

    public SecurityProperties securityProperties() {
        return securityProperties;
    }

    public VisuallyImpairedUserService visuallyImpairedUserService() {
        return visuallyImpairedUserService;
    }

    public CrossingAssistService crossingAssistService() {
        return crossingAssistService;
    }

    public AlarmWebSocketHandler alarmWebSocketHandler() {
        return alarmWebSocketHandler;
    }

    public boolean isDemoLocationForce() {
        return Boolean.TRUE.equals(demoLocationForce);
    }

    public Double getDemoLocationLatitude() {
        return demoLocationLatitude;
    }

    public Double getDemoLocationLongitude() {
        return demoLocationLongitude;
    }

    /** 去掉 Bearer 前缀，得到纯 token */
    public String extractToken(String token) {
        if (token == null) {
            return null;
        }
        return token.replace("Bearer", "").trim();
    }

    /**
     * 按 Authorization 解析监护人，失败返回 null
     *
     * @param token Authorization 头
     * @return 监护人实体或 null
     */
    public Guardian resolveGuardian(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            String guardianId = jwtUtil.getUsernameFromToken(extractToken(token));
            if (guardianId == null || guardianId.trim().isEmpty()) {
                return null;
            }
            return guardianService.getGuardianById(Long.parseLong(guardianId));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 强制登录：未登录或令牌无效时抛 UnauthorizedException
     *
     * @param token Authorization 头
     * @return 监护人实体
     */
    public Guardian requireGuardian(String token) {
        Guardian guardian = resolveGuardian(token);
        if (guardian == null) {
            throw new UnauthorizedException("未登录或令牌无效");
        }
        return guardian;
    }

    /** 按主键或设备编号解析设备 */
    public CaneDevice resolveDevice(String deviceRef) {
        return miniCareOverviewService.resolveDevice(deviceRef);
    }

    /** 组装家属守护概览 */
    public Map<String, Object> buildGuardianCareOverview(String deviceId, Guardian guardian) {
        return miniCareOverviewService.buildGuardianCareOverview(deviceId, guardian);
    }

    /** 规范化设备在线状态文案 */
    public String normalizeDeviceStatus(String status) {
        return miniCareOverviewService.normalizeDeviceStatus(status);
    }

    /** 格式化最新位置展示文案 */
    public String formatLocationText(SensorData latestSensor) {
        return miniCareOverviewService.formatLocationText(latestSensor);
    }
}
