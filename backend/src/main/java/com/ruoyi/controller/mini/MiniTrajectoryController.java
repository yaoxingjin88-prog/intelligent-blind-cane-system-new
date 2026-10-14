package com.ruoyi.controller.mini;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序轨迹与演示模拟接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Trajectory", description = "轨迹与演示模拟")
@Slf4j
@RequiredArgsConstructor
public class MiniTrajectoryController {

    private final MiniSupport support;
    private final Environment environment;

    /**
     * 轨迹点列表 GET /api/mini/devices/{deviceId}/trajectory
     *
     * @param deviceId  设备编号
     * @param startTime 可选起始时间
     * @param endTime   可选结束时间
     * @return 统一返回结果
     */
    @Operation(summary = "获取轨迹数据")
    @GetMapping("/devices/{deviceId}/trajectory")
    public Result getTrajectory(@PathVariable String deviceId,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime) {
        return Result.success(support.sensorDataService().getTrajectoryPoints(deviceId, startTime, endTime));
    }

    /**
     * 轨迹统计 GET /api/mini/devices/{deviceId}/statistics
     *
     * @param deviceId  设备编号
     * @param startTime 可选起始时间
     * @param endTime   可选结束时间
     * @return 统一返回结果
     */
    @Operation(summary = "获取轨迹统计")
    @GetMapping("/devices/{deviceId}/statistics")
    public Result getTrajectoryStatistics(@PathVariable String deviceId,
                                          @RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime) {
        return Result.success(support.sensorDataService().getTrajectoryStatistics(deviceId, startTime, endTime));
    }

    /**
     * 模拟设备移动（非 prod） POST /api/mini/devices/{deviceId}/simulate
     *
     * @param deviceId 设备编号
     * @param startLat 起点纬度
     * @param startLng 起点经度
     * @param points   生成点数
     * @return 统一返回结果
     */
    @Operation(summary = "模拟设备移动", description = "仅非 prod 可用")
    @PostMapping("/devices/{deviceId}/simulate")
    public Result simulateMovement(@PathVariable String deviceId,
                                   @RequestParam(defaultValue = "39.9042") double startLat,
                                   @RequestParam(defaultValue = "116.4074") double startLng,
                                   @RequestParam(defaultValue = "20") int points) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("simulate.reject deviceId={} reason=prod_disabled", deviceId);
            return Result.error(403, "生产环境禁用模拟接口");
        }
        // 调用业务层生成模拟轨迹点
        return Result.success(support.sensorDataService().simulateMovement(deviceId, startLat, startLng, points));
    }
}
