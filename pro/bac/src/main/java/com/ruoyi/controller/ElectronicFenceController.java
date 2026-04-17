package com.ruoyi.controller;

import com.ruoyi.entity.ElectronicFence;
import com.ruoyi.entity.Result;
import com.ruoyi.service.ElectronicFenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

 import java.util.List;

@RestController
@RequestMapping("/api/fences")
@Tag(name = "电子围栏管理", description = "电子围栏相关接口")
public class ElectronicFenceController {
    @Autowired
    private ElectronicFenceService electronicFenceService;

    @Operation(summary = "获取所有围栏", description = "返回系统中所有电子围栏设置")
    @GetMapping("/all")
    public Result<List<ElectronicFence>> getAll() {
        return Result.success(electronicFenceService.getAll());
    }

    @Operation(summary = "根据设备编号获取围栏", description = "根据设备编号获取电子围栏设置")
    @GetMapping
    public Result<ElectronicFence> getByDeviceId(@RequestParam String deviceId) {
        return Result.success(electronicFenceService.getByDeviceId(deviceId));
    }

    @Operation(summary = "保存电子围栏", description = "新增或更新设备电子围栏设置")
    @PutMapping
    public Result<ElectronicFence> save(@RequestBody ElectronicFence fence) {
        return Result.success(electronicFenceService.save(fence));
    }
}
