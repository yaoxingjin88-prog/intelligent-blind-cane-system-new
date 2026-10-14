package com.ruoyi.controller;

import lombok.RequiredArgsConstructor;
import com.ruoyi.dto.UserDeviceAssignRequest;
import com.ruoyi.entity.Result;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.VisuallyImpairedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视力障碍用户接口控制器
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
@Tag(name = "视力障碍用户管理", description = "视力障碍用户相关接口")
public class VisuallyImpairedUserController {

    private final VisuallyImpairedUserService userService;
    private final CaneDeviceService caneDeviceService;

    /**
     * 用户列表 GET /api/users
     *
     * @return 统一返回结果
     */
    @Operation(summary = "获取所有用户", description = "获取系统中所有视力障碍用户的信息")
    @GetMapping
    public Result<List<VisuallyImpairedUser>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    /**
     * 按主键查询 GET /api/users/{id}
     */
    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取视力障碍用户的详细信息")
    @GetMapping("/{id}")
    public Result<VisuallyImpairedUser> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 新增用户 POST /api/users
     *
     * @param user 用户实体
     * @return 统一返回结果
     */
    @Operation(summary = "添加用户", description = "添加新的视力障碍用户")
    @PostMapping
    public Result addUser(@RequestBody VisuallyImpairedUser user) {
        // 调用业务层入库
        userService.addUser(user);
        return Result.success();
    }

    /**
     * 删除用户 DELETE /api/users/{id}
     */
    @Operation(summary = "删除用户", description = "根据用户ID删除视力障碍用户")
    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * 更新用户 PUT /api/users
     *
     * @param user 用户实体
     * @return 统一返回结果
     */
    @Operation(summary = "更新用户", description = "更新视力障碍用户信息")
    @PutMapping
    public Result updateUser(@RequestBody VisuallyImpairedUser user) {
        // 调用业务层更新
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 为用户分配/更换/清空绑定设备 PUT /api/users/{id}/device
     * body.deviceId 为 cane_device 主键；传 null 表示解绑
     */
    @Operation(summary = "分配用户设备", description = "一人一台：绑定指定设备或清空绑定")
    @PutMapping("/{id}/device")
    public Result assignDevice(@PathVariable Long id, @RequestBody(required = false) UserDeviceAssignRequest request) {
        Long devicePk = request != null ? request.getDeviceId() : null;
        caneDeviceService.assignUserDevice(id, devicePk);
        return Result.success();
    }
}
