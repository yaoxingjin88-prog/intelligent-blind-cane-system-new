package com.ruoyi.controller.mini;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.common.AlarmStatus;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.Feedback;
import com.ruoyi.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序告警与消息接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Alarm", description = "报警与消息")
@Slf4j
@RequiredArgsConstructor
public class MiniAlarmController {

    private final MiniSupport support;

    /**
     * 告警列表 GET /api/mini/alarms
     *
     * @param limit 条数上限，默认 100
     * @return 统一返回结果
     */
    @Operation(summary = "获取报警列表")
    @GetMapping("/alarms")
    public Result getAlarmList(@RequestParam(required = false, defaultValue = "100") Integer limit) {
        return Result.success(support.alarmRecordService().getRecentAlarmRecords(limit));
    }

    /**
     * 告警统计 GET /api/mini/alarms/statistics
     *
     * @param deviceId 可选设备过滤
     * @return 统一返回结果
     */
    @Operation(summary = "获取报警统计")
    @GetMapping("/alarms/statistics")
    public Result getAlarmStatistics(@RequestParam(required = false) String deviceId) {
        return Result.success(support.alarmRecordService().getMiniStatistics(deviceId));
    }

    /**
     * 处理告警 PUT /api/mini/alarms/{id}/handle
     *
     * @param id   告警主键
     * @param data 含 status
     * @return 统一返回结果
     */
    @Operation(summary = "处理报警")
    @PutMapping("/alarms/{id}/handle")
    public Result handleAlarm(@PathVariable Long id, @RequestBody Map<String, String> data) {
        String status = data.get("status");
        // 调用业务层更新状态
        support.alarmRecordService().updateAlarmStatus(id, status);
        log.info("alarm.handle id={} status={}", id, status);
        return Result.success();
    }

    /**
     * 提交意见反馈 POST /api/mini/feedback
     *
     * @param data 含 token/type/content 等
     * @return 统一返回结果
     */
    @Operation(summary = "提交意见反馈")
    @PostMapping("/feedback")
    public Result submitFeedback(@RequestBody Map<String, String> data) {
        Feedback feedback = new Feedback();
        feedback.setUserId(Long.parseLong(support.jwtUtil().getUsernameFromToken(data.get("token"))));
        feedback.setType(data.get("type"));
        feedback.setContent(data.get("content"));
        feedback.setContact(data.get("contact"));
        feedback.setImages(data.get("images"));
        // 调用业务层提交反馈
        support.feedbackService().submitFeedback(feedback);
        return Result.success();
    }

    /**
     * 消息列表（由最近告警映射） GET /api/mini/messages
     *
     * @param limit 条数上限，默认 50
     * @return 统一返回结果
     */
    @Operation(summary = "获取消息列表", description = "基于最近报警记录")
    @GetMapping("/messages")
    public Result getMessageList(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        List<AlarmRecord> alarms = support.alarmRecordService().getRecentAlarmRecords(limit);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (AlarmRecord alarm : alarms) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", alarm.getId());
            msg.put("type", "alarm");
            msg.put("title", alarm.getAlarmType() == null ? "报警通知" : alarm.getAlarmType());
            msg.put("content", buildAlarmMessage(alarm));
            msg.put("createTime", alarm.getAlarmTime());
            msg.put("read", AlarmStatus.isHandled(alarm.getStatus()));
            messages.add(msg);
        }
        return Result.success(messages);
    }

    /**
     * 标记消息已读 PUT /api/mini/messages/{id}/read
     */
    @Operation(summary = "标记消息已读")
    @PutMapping("/messages/{id}/read")
    public Result markMessageRead(@PathVariable Long id) {
        // 调用业务层将对应告警标记已处理
        support.alarmRecordService().handleAlarm(id);
        log.info("alarm.messageRead id={}", id);
        return Result.success();
    }

    /** 组装告警消息文案 */
    private static String buildAlarmMessage(AlarmRecord alarm) {
        String type = alarm.getAlarmType() == null ? "未知类型" : alarm.getAlarmType();
        String device = alarm.getDeviceId() == null ? "-" : alarm.getDeviceId();
        return "设备 " + device + " 触发" + type + "，请及时处理。";
    }
}
