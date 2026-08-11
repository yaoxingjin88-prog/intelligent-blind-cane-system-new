package com.ruoyi.controller;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.Result;
import com.ruoyi.service.AlarmRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alarm-records")
@Tag(name = "告警记录管理", description = "告警记录相关接口")
public class AlarmRecordController {

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Operation(summary = "获取所有告警记录", description = "默认返回最近若干条；header=true 时仅返回未处理告警")
    @GetMapping
    public Result<List<AlarmRecord>> getAllAlarmRecords(
            @RequestParam(required = false, defaultValue = "200") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean all,
            @RequestParam(required = false, defaultValue = "false") Boolean unhandledOnly) {
        if (Boolean.TRUE.equals(unhandledOnly)) {
            return Result.success(alarmRecordService.getUnhandledAlarmRecords());
        }
        if (Boolean.TRUE.equals(all)) {
            return Result.success(alarmRecordService.getAllAlarmRecords());
        }
        return Result.success(alarmRecordService.getRecentAlarmRecords(limit));
    }

    @Operation(summary = "获取告警记录总数", description = "获取系统当前告警记录总条数")
    @GetMapping("/count")
    public Result<Integer> getAlarmRecordCount() {
        return Result.success(alarmRecordService.countAllAlarmRecords());
    }

    @Operation(summary = "根据ID获取告警记录", description = "根据告警记录ID获取告警记录的详细信息")
    @GetMapping("/{id}")
    public Result<AlarmRecord> getAlarmRecordById(@PathVariable Long id) {
        return Result.success(alarmRecordService.getAlarmRecordById(id));
    }

    @Operation(summary = "添加告警记录", description = "添加新的告警记录")
    @PostMapping
    public Result addAlarmRecord(@RequestBody AlarmRecord alarmRecord) {
        alarmRecordService.addAlarmRecord(alarmRecord);
        return Result.success();
    }

    @Operation(summary = "删除告警记录", description = "根据告警记录ID删除告警记录")
    @DeleteMapping("/{id}")
    public Result deleteAlarmRecord(@PathVariable Long id) {
        alarmRecordService.deleteAlarmRecord(id);
        return Result.success();
    }

    @Operation(summary = "裁剪历史告警记录", description = "按设备仅保留最近N条告警记录")
    @DeleteMapping("/prune")
    public Result<Integer> pruneAlarmRecords(@RequestParam(defaultValue = "20") Integer keepPerDevice) {
        return Result.success(alarmRecordService.pruneOldAlarmRecords(keepPerDevice));
    }

    @Operation(summary = "更新告警状态", description = "根据告警记录ID更新告警状态")
    @PutMapping("/status/{id}")
    public Result updateAlarmStatus(@PathVariable Long id, @RequestParam String status) {
        alarmRecordService.updateAlarmStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取设备最新报警", description = "根据设备ID获取最新的未处理报警")
    @GetMapping("/latest")
    public Result<AlarmRecord> getLatestAlarm(@RequestParam String deviceId) {
        return Result.success(alarmRecordService.getLatestAlarm(deviceId));
    }

    @Operation(summary = "处理报警", description = "处理报警记录，将状态改为已处理")
    @PutMapping("/{id}/handle")
    public Result handleAlarm(@PathVariable Long id) {
        alarmRecordService.handleAlarm(id);
        return Result.success();
    }
}
