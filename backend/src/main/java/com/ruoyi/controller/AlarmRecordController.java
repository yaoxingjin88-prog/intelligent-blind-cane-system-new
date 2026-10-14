package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.Result;
import com.ruoyi.service.AlarmRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警记录接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/alarm-records")
@Tag(name = "告警记录管理", description = "告警记录相关接口")
public class AlarmRecordController {

    private final AlarmRecordService alarmRecordService;

    /**
     * 分页/限量查询告警 GET /api/alarm-records
     *
     * @param limit         条数上限，默认 200
     * @param all           保留参数（不再全表扫描）
     * @param unhandledOnly true 时仅返回未处理告警
     * @param deviceId      可选，按设备过滤
     * @return 统一返回结果
     */
    @Operation(summary = "获取告警记录", description = "默认返回最近若干条；可按 deviceId 过滤；unhandledOnly=true 时仅未处理")
    @GetMapping
    public Result<List<AlarmRecord>> getAllAlarmRecords(
            @RequestParam(required = false, defaultValue = "200") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean all,
            @RequestParam(required = false, defaultValue = "false") Boolean unhandledOnly,
            @RequestParam(required = false) String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            List<AlarmRecord> byDevice = alarmRecordService.getAlarmsByDeviceId(deviceId, limit);
            if (Boolean.TRUE.equals(unhandledOnly)) {
                byDevice = byDevice.stream()
                        .filter(a -> com.ruoyi.common.AlarmStatus.isPending(a.getStatus()))
                        .toList();
            }
            return Result.success(byDevice);
        }
        // all=true 不再全表扫描，统一走限量查询
        if (Boolean.TRUE.equals(unhandledOnly)) {
            return Result.success(alarmRecordService.getUnhandledAlarmRecords(limit));
        }
        return Result.success(alarmRecordService.getRecentAlarmRecords(limit));
    }

    /**
     * 告警记录总数 GET /api/alarm-records/count
     */
    @Operation(summary = "获取告警记录总数", description = "获取系统当前告警记录总条数")
    @GetMapping("/count")
    public Result<Integer> getAlarmRecordCount() {
        return Result.success(alarmRecordService.countAllAlarmRecords());
    }

    /**
     * 按主键查询 GET /api/alarm-records/{id}
     */
    @Operation(summary = "根据ID获取告警记录", description = "根据告警记录ID获取告警记录的详细信息")
    @GetMapping("/{id}")
    public Result<AlarmRecord> getAlarmRecordById(@PathVariable Long id) {
        return Result.success(alarmRecordService.getAlarmRecordById(id));
    }

    /**
     * 新增告警记录 POST /api/alarm-records
     *
     * @param alarmRecord 告警实体
     * @return 统一返回结果
     */
    @Operation(summary = "添加告警记录", description = "添加新的告警记录")
    @PostMapping
    public Result addAlarmRecord(@RequestBody AlarmRecord alarmRecord) {
        // 调用业务层入库
        alarmRecordService.addAlarmRecord(alarmRecord);
        return Result.success();
    }

    /**
     * 删除告警记录 DELETE /api/alarm-records/{id}
     */
    @Operation(summary = "删除告警记录", description = "根据告警记录ID删除告警记录")
    @DeleteMapping("/{id}")
    public Result deleteAlarmRecord(@PathVariable Long id) {
        alarmRecordService.deleteAlarmRecord(id);
        return Result.success();
    }

    /**
     * 按设备裁剪历史告警 DELETE /api/alarm-records/prune
     *
     * @param keepPerDevice 每设备保留最近 N 条
     * @return 删除条数
     */
    @Operation(summary = "裁剪历史告警记录", description = "按设备仅保留最近N条告警记录")
    @DeleteMapping("/prune")
    public Result<Integer> pruneAlarmRecords(@RequestParam(defaultValue = "20") Integer keepPerDevice) {
        return Result.success(alarmRecordService.pruneOldAlarmRecords(keepPerDevice));
    }

    /**
     * 更新告警状态 PUT /api/alarm-records/status/{id}
     *
     * @param id     告警主键
     * @param status 目标状态
     * @return 统一返回结果
     */
    @Operation(summary = "更新告警状态", description = "根据告警记录ID更新告警状态；标已处理视为管理端兜底结案并通知家属")
    @PutMapping("/status/{id}")
    public Result updateAlarmStatus(@PathVariable Long id, @RequestParam String status) {
        alarmRecordService.updateAlarmStatus(id, status);
        return Result.success();
    }

    /**
     * 设备最新一条告警 GET /api/alarm-records/latest（含已处理，供实时监控展示上下文）
     *
     * @param deviceId 设备编号
     */
    @Operation(summary = "获取设备最新报警", description = "按设备取最近一条报警记录（不限处理状态）")
    @GetMapping("/latest")
    public Result<AlarmRecord> getLatestAlarm(@RequestParam String deviceId) {
        return Result.success(alarmRecordService.getLatestAlarm(deviceId));
    }

    /**
     * 处理告警（标记已处理） PUT /api/alarm-records/{id}/handle
     */
    @Operation(summary = "处理报警", description = "管理端兜底结案：标已处理并通知家属端")
    @PutMapping("/{id}/handle")
    public Result handleAlarm(@PathVariable Long id) {
        return Result.success(alarmRecordService.handleAlarm(id, com.ruoyi.service.AlarmNotifyService.HANDLED_BY_ADMIN));
    }
}
