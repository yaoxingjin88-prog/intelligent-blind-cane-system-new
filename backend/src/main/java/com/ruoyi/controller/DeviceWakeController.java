package com.ruoyi.controller;

import com.ruoyi.entity.Result;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 硬件 AI 唤醒接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Tag(name = "设备硬件交互", description = "盲杖硬件端调用的接口")
public class DeviceWakeController {

    private final AlarmWebSocketHandler alarmWebSocketHandler;

    /**
     * 盲杖 AI 按键唤醒手机端 POST /api/device/ai-wake
     * 支持 query 或 JSON body 传 deviceId
     *
     * @param deviceIdParam 设备编号（query）
     * @param body          可选 JSON，含 deviceId
     * @return 统一返回结果
     */
    @Operation(summary = "AI 唤醒", description = "盲杖按下 AI 按键触发手机 AI 对话")
    @PostMapping("/ai-wake")
    public Result<?> aiWake(
            @RequestParam(value = "deviceId", required = false) String deviceIdParam,
            @RequestBody(required = false) Map<String, String> body) {
        String deviceId = deviceIdParam;
        if ((deviceId == null || deviceId.isBlank()) && body != null) {
            deviceId = body.get("deviceId");
        }
        if (deviceId == null || deviceId.isBlank()) {
            return Result.error("deviceId 不能为空");
        }
        log.info("收到盲杖 AI 唤醒: deviceId={}", deviceId);
        // 调用 WebSocket 通知绑定小程序
        alarmWebSocketHandler.sendAiWake(deviceId);
        return Result.success("已唤醒");
    }
}
