package com.ruoyi.controller.mini;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序电子围栏接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Fence", description = "电子围栏")
@RequiredArgsConstructor
public class MiniFenceController {

    private final MiniSupport support;

    /**
     * 围栏列表 GET /api/mini/fences
     *
     * @param deviceId 可选设备过滤
     * @return 统一返回结果（小程序视图结构）
     */
    @Operation(summary = "获取围栏列表")
    @GetMapping("/fences")
    public Result getFenceList(@RequestParam(required = false) String deviceId) {
        return Result.success(support.electronicFenceService().listForMini(deviceId));
    }

    /**
     * 创建围栏 POST /api/mini/fences
     *
     * @param params 前端传入 name/deviceId/radius/center
     * @return 统一返回结果
     */
    @Operation(summary = "创建围栏")
    @PostMapping("/fences")
    public Result createFence(@RequestBody Map<String, Object> params) {
        // 调用业务层创建并转成小程序视图
        return Result.success(support.electronicFenceService().createForMini(params));
    }

    /**
     * 更新围栏 PUT /api/mini/fences/{id}
     *
     * @param id     围栏主键
     * @param params 更新字段
     * @return 统一返回结果
     */
    @Operation(summary = "更新围栏")
    @PutMapping("/fences/{id}")
    public Result updateFence(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        // 调用业务层更新
        support.electronicFenceService().updateForMini(id, params);
        return Result.success();
    }

    /**
     * 删除围栏 DELETE /api/mini/fences/{id}
     */
    @Operation(summary = "删除围栏")
    @DeleteMapping("/fences/{id}")
    public Result deleteFence(@PathVariable Long id) {
        support.electronicFenceService().deleteById(id);
        return Result.success();
    }

    /**
     * 围栏状态 GET /api/mini/fences/{id}/status
     *
     * @param id 围栏主键
     * @return 统一返回结果
     */
    @Operation(summary = "获取围栏状态")
    @GetMapping("/fences/{id}/status")
    public Result getFenceStatus(@PathVariable Long id) {
        return Result.success(support.electronicFenceService().statusForMini(id));
    }
}
