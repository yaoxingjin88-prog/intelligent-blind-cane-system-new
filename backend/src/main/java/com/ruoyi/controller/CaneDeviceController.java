package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Result;
import com.ruoyi.service.CaneDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 盲杖设备接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/devices")
@Tag(name = "盲杖设备管理", description = "盲杖设备相关接口")
public class CaneDeviceController {

    private final CaneDeviceService deviceService;

    /**
     * 设备列表 GET /api/devices
     *
     * @return 统一返回结果
     */
    @Operation(summary = "获取所有设备", description = "获取系统中所有盲杖设备的信息")
    @GetMapping
    public Result<List<CaneDevice>> getAllDevices() {
        return Result.success(deviceService.getAllDevices());
    }

    /**
     * 按主键查询 GET /api/devices/{id}
     */
    @Operation(summary = "根据ID获取设备", description = "根据设备ID获取盲杖设备的详细信息")
    @GetMapping("/{id}")
    public Result<CaneDevice> getDeviceById(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    /**
     * 新增设备 POST /api/devices
     *
     * @param device 设备实体
     * @return 统一返回结果
     */
    @Operation(summary = "添加设备", description = "添加新的盲杖设备")
    @PostMapping
    public Result addDevice(@RequestBody CaneDevice device) {
        // 调用业务层入库
        deviceService.addDevice(device);
        return Result.success();
    }

    /**
     * 删除设备 DELETE /api/devices/{id}
     */
    @Operation(summary = "删除设备", description = "根据设备ID删除盲杖设备")
    @DeleteMapping("/{id}")
    public Result deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success();
    }

    /**
     * 更新设备 PUT /api/devices
     *
     * @param device 设备实体
     * @return 统一返回结果
     */
    @Operation(summary = "更新设备", description = "更新盲杖设备信息")
    @PutMapping
    public Result updateDevice(@RequestBody CaneDevice device) {
        // 调用业务层更新
        deviceService.updateDevice(device);
        return Result.success();
    }

    /**
     * 启动设备测试模拟 POST /api/devices/{id}/test/start
     */
    @Operation(summary = "启动设备测试", description = "启动指定设备的低频测试数据模拟")
    @PostMapping("/{id}/test/start")
    public Result<Boolean> startDeviceTest(@PathVariable Long id) {
        return Result.success(deviceService.startDeviceTest(id));
    }

    /**
     * 停止设备测试模拟 POST /api/devices/{id}/test/stop
     */
    @Operation(summary = "停止设备测试", description = "停止指定设备的低频测试数据模拟")
    @PostMapping("/{id}/test/stop")
    public Result<Boolean> stopDeviceTest(@PathVariable Long id) {
        return Result.success(deviceService.stopDeviceTest(id));
    }

    /**
     * 当前测试中的设备编号列表 GET /api/devices/test/running
     */
    @Operation(summary = "获取测试中的设备", description = "返回当前已启动测试模拟的设备ID列表")
    @GetMapping("/test/running")
    public Result<Set<String>> getRunningDeviceTests() {
        return Result.success(deviceService.getTestingDeviceIds());
    }
}
