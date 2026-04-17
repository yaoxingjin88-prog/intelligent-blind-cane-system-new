package com.ruoyi.controller;

import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.service.GuardianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guardians")
@Tag(name = "监护人管理", description = "监护人相关接口")
public class GuardianController {

    @Autowired
    private GuardianService guardianService;

    @Operation(summary = "获取所有监护人", description = "获取系统中所有监护人的信息")
    @GetMapping
    public List<Guardian> getAllGuardians() {
        return guardianService.getAllGuardians();
    }

    @Operation(summary = "根据ID获取监护人", description = "根据监护人ID获取监护人的详细信息")
    @GetMapping("/{id}")
    public Guardian getGuardianById(@PathVariable Long id) {
        return guardianService.getGuardianById(id);
    }

    @Operation(summary = "添加监护人", description = "添加新的监护人")
    @PostMapping
    public Result addGuardian(@RequestBody Guardian guardian) {
        guardianService.addGuardian(guardian);
        return Result.success();
    }

    @Operation(summary = "删除监护人", description = "根据监护人ID删除监护人")
    @DeleteMapping("/{id}")
    public Result deleteGuardian(@PathVariable Long id) {
        guardianService.deleteGuardian(id);
        return Result.success();
    }

    @Operation(summary = "更新监护人", description = "更新监护人信息")
    @PutMapping
    public Result updateGuardian(@RequestBody Guardian guardian) {
        guardianService.updateGuardian(guardian);
        return Result.success();
    }
}
