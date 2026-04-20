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
 * 盲杖硬件端 → 后端的唤醒接口
 * 硬件按下 AI 按钮时，通过 HTTP 调用此接口
 * 后端再通过 WebSocket 通知绑定该设备的手机小程序
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Tag(name = "设备硬件交互", description = "盲杖硬件端调用的接口")
public class DeviceWakeController {

    private final AlarmWebSocketHandler alarmWebSocketHandler;

    /**
     * 盲杖按下 AI 按钮 → 唤醒手机端 AI 对话
     * 硬件调用示例：POST /api/device/ai-wake?deviceId=CANE001
     * 也支持 JSON body: { "deviceId": "CANE001" }
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
        alarmWebSocketHandler.sendAiWake(deviceId);
        return Result.success("已唤醒");
    }
}
