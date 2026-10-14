package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    /**
     * 应用与数据库探活 GET /api/health
     *
     * @return 统一返回结果（status / db）
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("db", pingDb() ? "UP" : "DOWN");
        if ("DOWN".equals(body.get("db"))) {
            body.put("status", "DOWN");
            Result<Map<String, Object>> result = Result.error("数据库不可用");
            result.setCode(503);
            result.setData(body);
            return result;
        }
        return Result.success(body);
    }

    /** 探测数据源是否可用 */
    private boolean pingDb() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ignored) {
            return false;
        }
    }
}
