package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import com.ruoyi.service.MonitorOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 实时监控聚合接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/monitor")
@Tag(name = "实时监控聚合", description = "监控中心聚合查询接口")
public class MonitorOverviewController {

    private final MonitorOverviewService monitorOverviewService;

    /**
     * 监控中心总览 GET /api/monitor/overview
     *
     * @return 统一返回结果（设备/传感/告警/测试状态）
     */
    @Operation(summary = "监控中心总览", description = "一次返回设备、最新传感数据、最新告警与测试状态，避免前端 N+1 请求")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        // 调用业务层聚合监控数据
        return Result.success(monitorOverviewService.getOverview());
    }
}
