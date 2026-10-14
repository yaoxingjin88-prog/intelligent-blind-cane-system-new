package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import com.ruoyi.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据分析看板接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "数据分析看板", description = "数据看板与可视化分析接口")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    /**
     * 数据看板总览 GET /api/analytics/dashboard
     *
     * @return 统一返回结果（首页看板聚合数据）
     */
    @Operation(summary = "获取数据看板总览", description = "返回首页看板、热力图、设备健康和报警分布所需数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardData() {
        // 调用业务层聚合看板数据
        return Result.success(analyticsService.getDashboardData());
    }
}
