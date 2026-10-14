package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.dto.DeviceBindRequest;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Result;
import com.ruoyi.service.AnalyticsService;
import com.ruoyi.service.AppCacheService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.MonitorOverviewService;
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
    private final AppCacheService appCacheService;
    private final AnalyticsService analyticsService;
    private final MonitorOverviewService monitorOverviewService;

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
     * 仅更新档案字段；归属请用 bind/unbind
     */
    @Operation(summary = "更新设备", description = "仅更新设备编号/名称；电量与在线状态由上报维护，归属走 bind/unbind")
    @PutMapping
    public Result updateDevice(@RequestBody CaneDevice device) {
        deviceService.updateDevice(device);
        return Result.success();
    }

    /**
     * 绑定设备到用户 POST /api/devices/{id}/bind
     */
    @Operation(summary = "绑定设备", description = "将未绑定设备挂到指定用户，一人一台")
    @PostMapping("/{id}/bind")
    public Result bindDevice(@PathVariable Long id, @RequestBody DeviceBindRequest request) {
        Long userId = request != null ? request.getUserId() : null;
        deviceService.bindDeviceToUser(id, userId);
        return Result.success();
    }

    /**
     * 解绑设备 POST /api/devices/{id}/unbind
     */
    @Operation(summary = "解绑设备", description = "清除设备上的用户归属")
    @PostMapping("/{id}/unbind")
    public Result unbindDevice(@PathVariable Long id) {
        deviceService.unbindDevice(id);
        return Result.success();
    }

    /**
     * 标记设备离线 POST /api/devices/{id}/offline
     * 假硬件停止上报或设备关机时调用，立即离线（不吃近 3 分钟残留心跳）
     */
    @Operation(summary = "标记设备离线", description = "落库离线并停止页内测试模拟；列表不再因残留传感显示在线")
    @PostMapping("/{id}/offline")
    public Result markOffline(@PathVariable Long id) {
        deviceService.markOffline(id);
        appCacheService.evictDashboardCaches();
        analyticsService.invalidateLocalCache();
        monitorOverviewService.invalidateLocalCache();
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
