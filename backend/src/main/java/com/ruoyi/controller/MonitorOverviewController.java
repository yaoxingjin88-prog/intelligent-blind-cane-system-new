package com.ruoyi.controller;

import com.ruoyi.entity.Result;
import com.ruoyi.service.MonitorOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@Tag(name = "实时监控聚合", description = "监控中心聚合查询接口")
public class MonitorOverviewController {

    @Autowired
    private MonitorOverviewService monitorOverviewService;

    @Operation(summary = "监控中心总览", description = "一次返回设备、最新传感数据、最新告警与测试状态，避免前端 N+1 请求")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(monitorOverviewService.getOverview());
    }
}
