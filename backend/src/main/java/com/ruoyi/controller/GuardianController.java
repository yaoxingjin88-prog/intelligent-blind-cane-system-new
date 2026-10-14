package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.service.GuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 监护人接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/guardians")
@Tag(name = "监护人管理", description = "监护人相关接口")
public class GuardianController {

    private final GuardianService guardianService;

    /**
     * 监护人列表 GET /api/guardians
     *
     * @return 统一返回结果
     */
    @Operation(summary = "获取所有监护人", description = "获取系统中所有监护人的信息")
    @GetMapping
    public Result<List<Guardian>> getAllGuardians() {
        return Result.success(guardianService.getAllGuardians());
    }

    /**
     * 按主键查询 GET /api/guardians/{id}
     */
    @Operation(summary = "根据ID获取监护人", description = "根据监护人ID获取监护人的详细信息")
    @GetMapping("/{id}")
    public Result<Guardian> getGuardianById(@PathVariable Long id) {
        return Result.success(guardianService.getGuardianById(id));
    }

    /**
     * 新增监护人 POST /api/guardians
     *
     * @param guardian 监护人实体
     * @return 统一返回结果
     */
    @Operation(summary = "添加监护人", description = "添加新的监护人")
    @PostMapping
    public Result<Void> addGuardian(@RequestBody Guardian guardian) {
        // 调用业务层入库
        guardianService.addGuardian(guardian);
        return Result.success();
    }

    /**
     * 删除监护人 DELETE /api/guardians/{id}
     */
    @Operation(summary = "删除监护人", description = "根据监护人ID删除监护人")
    @DeleteMapping("/{id}")
    public Result<Void> deleteGuardian(@PathVariable Long id) {
        guardianService.deleteGuardian(id);
        return Result.success();
    }

    /**
     * 更新监护人 PUT /api/guardians
     *
     * @param guardian 监护人实体
     * @return 统一返回结果
     */
    @Operation(summary = "更新监护人", description = "更新监护人信息")
    @PutMapping
    public Result<Void> updateGuardian(@RequestBody Guardian guardian) {
        // 调用业务层更新
        guardianService.updateGuardian(guardian);
        return Result.success();
    }
}
