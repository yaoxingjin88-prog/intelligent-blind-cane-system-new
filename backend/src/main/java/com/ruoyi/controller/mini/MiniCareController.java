package com.ruoyi.controller.mini;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CrossingAssistSnapshot;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.service.MiniCareOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序家属守护与路口辅助接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Care", description = "家属守护与路口辅助")
@RequiredArgsConstructor
public class MiniCareController {

    private final MiniSupport support;

    /**
     * 家属守护概览 GET /api/mini/devices/{deviceId}/guardian-care/overview
     *
     * @param deviceId 设备编号
     * @param token    Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "获取家属守护概览")
    @GetMapping("/devices/{deviceId}/guardian-care/overview")
    public Result getGuardianCareOverview(@PathVariable String deviceId,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        Guardian guardian = support.requireGuardian(token);
        // 调用业务层组装守护概览
        return Result.success(support.buildGuardianCareOverview(deviceId, guardian));
    }

    /**
     * 发送家属安抚语音 POST /api/mini/devices/{deviceId}/guardian-care/comfort
     *
     * @param deviceId 设备编号
     * @param params   可选 content
     * @return 统一返回结果
     */
    @Operation(summary = "发送家属安抚语音")
    @PostMapping("/devices/{deviceId}/guardian-care/comfort")
    public Result sendGuardianComfort(@PathVariable String deviceId, @RequestBody(required = false) Map<String, String> params) {
        String content = params == null ? null : params.get("content");
        if (content == null || content.trim().isEmpty()) {
            content = "别着急，家属正在关注你，请先在安全区域稍作等待。";
        }
        String normalized = content.trim();
        // 调用 WebSocket 下发安抚
        support.alarmWebSocketHandler().sendGuardianComfort(deviceId, normalized);
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("content", normalized);
        result.put("sentAt", LocalDateTime.now().format(MiniCareOverviewService.DATE_TIME_FORMATTER));
        return Result.success(result);
    }

    /**
     * 发送家属目的地 POST /api/mini/devices/{deviceId}/guardian-care/destination
     *
     * @param deviceId 设备编号
     * @param params   含 destination
     * @return 统一返回结果
     */
    @Operation(summary = "发送家属目的地")
    @PostMapping("/devices/{deviceId}/guardian-care/destination")
    public Result sendGuardianDestination(@PathVariable String deviceId, @RequestBody Map<String, String> params) {
        String destination = params == null ? null : params.get("destination");
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("目的地不能为空");
        }
        String normalized = destination.trim();
        String prompt = "家属已发送新的目的地：" + normalized + "。请按当前安全提醒继续前进。";
        // 调用 WebSocket 下发目的地
        support.alarmWebSocketHandler().sendGuardianDestination(deviceId, normalized, prompt);
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("destination", normalized);
        result.put("sentAt", LocalDateTime.now().format(MiniCareOverviewService.DATE_TIME_FORMATTER));
        return Result.success(result);
    }

    /**
     * 用户端发起 SOS POST /api/mini/devices/{deviceId}/guardian-care/sos
     *
     * @param deviceId 设备编号
     * @param params   可选 locationText/content
     * @return 统一返回结果
     */
    @Operation(summary = "用户端发起SOS求助")
    @PostMapping("/devices/{deviceId}/guardian-care/sos")
    public Result triggerGuardianSos(@PathVariable String deviceId, @RequestBody(required = false) Map<String, String> params) {
        String locationText = params == null ? null : params.get("locationText");
        String content = params == null ? null : params.get("content");
        String normalizedContent = content == null || content.trim().isEmpty()
                ? "用户端已主动发起 SOS 求助，请立即查看最新位置并尽快联系。"
                : content.trim();
        String normalizedLocation = locationText == null || locationText.trim().isEmpty()
                ? "当前位置待确认"
                : locationText.trim();

        // 与摔倒/越界统一：落库 + 管理端广播 + 家属守护提醒
        AlarmRecord alarmRecord = support.alarmNotifyService().createAlarmPersistAndPush(deviceId, "SOS求助");
        // 补充位置描述推送（create 文案偏通用，SOS 需要更明确）
        support.alarmWebSocketHandler().sendGuardianAlert(
                deviceId, "SOS", "danger", normalizedContent, normalizedLocation);

        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("alarmType", "SOS求助");
        result.put("message", normalizedContent);
        result.put("locationText", normalizedLocation);
        result.put("sentAt", alarmRecord.getAlarmTime());
        result.put("alarmId", alarmRecord.getId());
        return Result.success(result);
    }

    /**
     * 路口辅助最新结果 GET /api/mini/devices/{deviceId}/crossing-assist
     */
    @Operation(summary = "获取路口辅助结果")
    @GetMapping("/devices/{deviceId}/crossing-assist")
    public Result<CrossingAssistSnapshot> getCrossingAssist(@PathVariable String deviceId) {
        return Result.success(support.crossingAssistService().getLatest(deviceId));
    }

    /**
     * 上报路口辅助结果 POST /api/mini/devices/{deviceId}/crossing-assist
     *
     * @param deviceId 设备编号
     * @param snapshot 路口辅助快照
     * @return 统一返回结果
     */
    @Operation(summary = "上报路口辅助结果")
    @PostMapping("/devices/{deviceId}/crossing-assist")
    public Result<CrossingAssistSnapshot> updateCrossingAssist(@PathVariable String deviceId,
                                                               @RequestBody CrossingAssistSnapshot snapshot) {
        // 调用业务层更新路口辅助
        return Result.success(support.crossingAssistService().update(deviceId, snapshot));
    }

    /**
     * 生成路口辅助演示数据 POST /api/mini/devices/{deviceId}/crossing-assist/mock
     */
    @Operation(summary = "生成路口辅助演示结果")
    @PostMapping("/devices/{deviceId}/crossing-assist/mock")
    public Result<CrossingAssistSnapshot> mockCrossingAssist(@PathVariable String deviceId) {
        return Result.success(support.crossingAssistService().mock(deviceId));
    }
}
