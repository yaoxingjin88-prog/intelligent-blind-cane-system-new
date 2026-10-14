package com.ruoyi.controller.mini;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.service.GuardianFamilyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序家庭监护圈：主监护人邀请/移除子监护人
 */
@RestController
@RequestMapping("/api/mini/family")
@Tag(name = "小程序接口-Family", description = "家庭监护成员管理")
@RequiredArgsConstructor
public class MiniFamilyController {

    private final MiniSupport support;
    private final GuardianFamilyService guardianFamilyService;

    @Operation(summary = "当前家庭成员")
    @GetMapping("/members")
    public Result<Map<String, Object>> members(@RequestHeader(value = "Authorization", required = false) String token) {
        Guardian me = support.requireGuardian(token);
        return Result.success(guardianFamilyService.toFamilyView(me));
    }

    @Operation(summary = "主监护人邀请子监护人")
    @PostMapping("/invite")
    public Result<Guardian> invite(@RequestHeader(value = "Authorization", required = false) String token,
                                   @RequestBody Map<String, String> body) {
        Guardian me = support.requireGuardian(token);
        Guardian member = guardianFamilyService.inviteMember(
                me,
                body.get("phone"),
                body.get("name"),
                body.get("relation")
        );
        return Result.success(member);
    }

    @Operation(summary = "主监护人移除子监护人")
    @DeleteMapping("/members/{id}")
    public Result<Void> remove(@RequestHeader(value = "Authorization", required = false) String token,
                               @PathVariable Long id) {
        Guardian me = support.requireGuardian(token);
        guardianFamilyService.removeMember(me, id);
        return Result.success();
    }
}
