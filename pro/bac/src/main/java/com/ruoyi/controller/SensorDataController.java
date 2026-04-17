package com.ruoyi.controller;

import com.ruoyi.entity.Result;
import com.ruoyi.entity.SensorData;
import com.ruoyi.service.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-data")
@Tag(name = "传感器数据管理", description = "传感器数据相关接口")
public class SensorDataController {

    @Autowired
    private SensorDataService sensorDataService;

    @Operation(summary = "获取所有传感器数据", description = "获取系统中所有传感器数据的信息")
    @GetMapping
    public Result<List<SensorData>> getAllSensorData() {
        return Result.success(sensorDataService.getAllSensorData());
    }

    @Operation(summary = "获取传感器数据总数", description = "获取系统当前传感器数据总条数")
    @GetMapping("/count")
    public Result<Integer> getSensorDataCount() {
        return Result.success(sensorDataService.countAllSensorData());
    }

    @Operation(summary = "根据ID获取传感器数据", description = "根据传感器数据ID获取传感器数据的详细信息")
    @GetMapping("/{id}")
    public Result<SensorData> getSensorDataById(@PathVariable Long id) {
        return Result.success(sensorDataService.getSensorDataById(id));
    }

    @Operation(summary = "添加传感器数据", description = "添加新的传感器数据")
    @PostMapping
    public Result addSensorData(@RequestBody SensorData sensorData) {
        sensorDataService.addSensorData(sensorData);
        return Result.success();
    }

    @Operation(summary = "删除传感器数据", description = "根据传感器数据ID删除传感器数据")
    @DeleteMapping("/{id}")
    public Result deleteSensorData(@PathVariable Long id) {
        sensorDataService.deleteSensorData(id);
        return Result.success();
    }

    @Operation(summary = "裁剪历史传感器数据", description = "按设备仅保留最近N条传感器数据")
    @DeleteMapping("/prune")
    public Result<Integer> pruneSensorData(@RequestParam(defaultValue = "50") Integer keepPerDevice) {
        return Result.success(sensorDataService.pruneOldSensorData(keepPerDevice));
    }

    @Operation(summary = "获取设备最新传感器数据", description = "根据设备ID获取最新的传感器数据")
    @GetMapping("/latest")
    public Result<SensorData> getLatestSensorData(@RequestParam String deviceId) {
        return Result.success(sensorDataService.getLatestSensorData(deviceId));
    }

    @Operation(summary = "获取设备轨迹数据", description = "根据设备ID和时间范围获取轨迹数据")
    @GetMapping("/trajectory")
    public Result<List<SensorData>> getTrajectory(
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "1") Integer hours) {
        return Result.success(sensorDataService.getTrajectory(deviceId, hours));
    }
}
