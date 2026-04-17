package com.ruoyi.controller;

import com.ruoyi.entity.Result;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.service.VisuallyImpairedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "视力障碍用户管理", description = "视力障碍用户相关接口")
public class VisuallyImpairedUserController {

    @Autowired
    private VisuallyImpairedUserService userService;

    @Operation(summary = "获取所有用户", description = "获取系统中所有视力障碍用户的信息")
    @GetMapping
    public Result<List<VisuallyImpairedUser>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取视力障碍用户的详细信息")
    @GetMapping("/{id}")
    public Result<VisuallyImpairedUser> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "添加用户", description = "添加新的视力障碍用户")
    @PostMapping
    public Result addUser(@RequestBody VisuallyImpairedUser user) {
        userService.addUser(user);
        return Result.success();
    }

    @Operation(summary = "删除用户", description = "根据用户ID删除视力障碍用户")
    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "更新用户", description = "更新视力障碍用户信息")
    @PutMapping
    public Result updateUser(@RequestBody VisuallyImpairedUser user) {
        userService.updateUser(user);
        return Result.success();
    }
}
