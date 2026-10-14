package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import com.ruoyi.entity.SensorData;
import com.ruoyi.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传感器数据接口控制器（管理端查询 + 硬件上报）
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sensor-data")
@Tag(name = "传感器数据管理", description = "传感器数据相关接口")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    /**
     * 分页/限量查询传感数据 GET /api/sensor-data
     *
     * @param limit 条数上限，默认 200
     * @param all   true 时按最大 1000 条返回（禁止无界全表）
     * @return 统一返回结果
     */
    @Operation(summary = "获取传感器数据")
    @GetMapping
    public Result<List<SensorData>> getAllSensorData(
            @RequestParam(required = false, defaultValue = "200") Integer limit,
            @RequestParam(required = false, defaultValue = "false") Boolean all) {
        if (Boolean.TRUE.equals(all)) {
            return Result.success(sensorDataService.getRecentSensorData(1000));
        }
        return Result.success(sensorDataService.getRecentSensorData(limit));
    }

    /**
     * 传感数据总数 GET /api/sensor-data/count
     */
    @Operation(summary = "获取传感器数据总数")
    @GetMapping("/count")
    public Result<Integer> getSensorDataCount() {
        return Result.success(sensorDataService.countAllSensorData());
    }

    /**
     * 按主键查询 GET /api/sensor-data/{id}
     */
    @Operation(summary = "根据ID获取传感器数据")
    @GetMapping("/{id}")
    public Result<SensorData> getSensorDataById(@PathVariable Long id) {
        return Result.success(sensorDataService.getSensorDataById(id));
    }

    /**
     * 硬件上报传感数据 POST /api/sensor-data
     * （鉴权见 JwtInterceptor：可配置 X-Device-Key）
     *
     * @param sensorData 上报体
     * @return 统一返回结果
     */
    @Operation(summary = "添加传感器数据")
    @PostMapping
    public Result addSensorData(@RequestBody SensorData sensorData) {
        // 调用业务层：入库 + 跌倒/围栏联动
        sensorDataService.addSensorData(sensorData);
        return Result.success();
    }

    /**
     * 删除单条传感数据 DELETE /api/sensor-data/{id}
     */
    @Operation(summary = "删除传感器数据")
    @DeleteMapping("/{id}")
    public Result deleteSensorData(@PathVariable Long id) {
        sensorDataService.deleteSensorData(id);
        return Result.success();
    }

    /**
     * 按设备裁剪历史 DELETE /api/sensor-data/prune
     *
     * @param keepPerDevice 每设备保留最近 N 条
     * @return 删除条数
     */
    @Operation(summary = "裁剪历史传感器数据")
    @DeleteMapping("/prune")
    public Result<Integer> pruneSensorData(@RequestParam(defaultValue = "50") Integer keepPerDevice) {
        return Result.success(sensorDataService.pruneOldSensorData(keepPerDevice));
    }

    /**
     * 设备最新一条 GET /api/sensor-data/latest
     */
    @Operation(summary = "获取设备最新传感器数据")
    @GetMapping("/latest")
    public Result<SensorData> getLatestSensorData(@RequestParam String deviceId) {
        return Result.success(sensorDataService.getLatestSensorData(deviceId));
    }

    /**
     * 设备轨迹 GET /api/sensor-data/trajectory
     *
     * @param deviceId 设备编号
     * @param hours    最近小时数，默认 1
     */
    @Operation(summary = "获取设备轨迹数据")
    @GetMapping("/trajectory")
    public Result<List<SensorData>> getTrajectory(
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "1") Integer hours) {
        return Result.success(sensorDataService.getTrajectory(deviceId, hours));
    }
}
