package com.ruoyi.controller.mini;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.dto.MiniSmsLoginRequest;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.entity.VisuallyImpairedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序认证与监护人档案接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Auth", description = "登录注册与档案")
@Slf4j
@RequiredArgsConstructor
public class MiniAuthController {

    private final MiniSupport support;

    /**
     * 小程序登录接口 POST /api/mini/login
     * 业务步骤：
     * 1. 校验短信验证码（开发环境可开测试码）
     * 2. 按手机号查找监护人
     * 3. 签发 JWT 并返回用户信息
     *
     * @param params 前端传入手机号/验证码，@Valid 开启校验
     * @return 统一返回结果（token + userInfo）
     */
    @Operation(summary = "小程序登录")
    @PostMapping("/login")
    public Result login(@Valid @RequestBody MiniSmsLoginRequest params) {
        String phone = params.getPhone();
        String code = params.getCode();

        // 步骤1：验证码校验
        if (!support.securityProperties().isAllowTestSmsCode() || !"123456".equals(code)) {
            log.warn("auth.mini.login fail phone={} reason=bad_code", maskPhone(phone));
            return Result.error(401, support.securityProperties().isAllowTestSmsCode()
                    ? "验证码错误"
                    : "短信验证未配置，请联系管理员");
        }

        // 步骤2：查找监护人
        Guardian guardian = support.guardianService().findByPhone(phone);
        if (guardian == null) {
            log.warn("auth.mini.login fail phone={} reason=not_registered", maskPhone(phone));
            return Result.error(401, "该手机号未注册为监护人");
        }

        // 步骤3：签发 token
        String token = support.jwtUtil().generateToken(guardian.getId().toString());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", guardian);
        log.info("auth.mini.login success phone={} guardianId={}", maskPhone(phone), guardian.getId());
        return Result.success(data);
    }

    /**
     * 微信登录占位 POST /api/mini/wechat-login
     *
     * @return 统一返回结果（当前未开放）
     */
    @Operation(summary = "微信登录")
    @PostMapping("/wechat-login")
    public Result wechatLogin(@RequestBody Map<String, Object> params) {
        log.info("auth.mini.wechat-login rejected: not implemented");
        return Result.error(501, "微信登录暂未开放");
    }

    /**
     * 监护人注册 POST /api/mini/register
     *
     * @param params 手机号/验证码
     * @return 统一返回结果
     */
    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result register(@Valid @RequestBody MiniSmsLoginRequest params) {
        String phone = params.getPhone();
        String code = params.getCode();

        if (!support.securityProperties().isAllowTestSmsCode() || !"123456".equals(code)) {
            log.warn("auth.mini.register fail phone={} reason=bad_code", maskPhone(phone));
            return Result.error(400, support.securityProperties().isAllowTestSmsCode()
                    ? "验证码错误"
                    : "短信验证未配置，请联系管理员");
        }

        Guardian existingGuardian = support.guardianService().findByPhone(phone);
        if (existingGuardian != null) {
            log.warn("auth.mini.register fail phone={} reason=exists", maskPhone(phone));
            return Result.error(400, "该手机号已注册");
        }

        Guardian guardian = new Guardian();
        guardian.setPhone(phone);
        guardian.setName("新监护人");
        guardian.setRelation("未设置");
        // 调用业务层创建监护人
        support.guardianService().addGuardian(guardian);
        log.info("auth.mini.register success phone={}", maskPhone(phone));
        return Result.success("注册成功");
    }

    /**
     * 退出登录 POST /api/mini/logout
     * 无状态 JWT，客户端清除 token 即可
     *
     * @return 统一返回结果
     */
    @Operation(summary = "退出登录", description = "无状态 JWT，客户端清 token 即可")
    @PostMapping("/logout")
    public Result logout() {
        log.info("auth.mini.logout");
        return Result.success();
    }

    /**
     * 当前监护人信息 GET /api/mini/user/info
     *
     * @param token Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/user/info")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        return Result.success(support.requireGuardian(token));
    }

    /**
     * 更新监护人信息 PUT /api/mini/user/info
     *
     * @param guardian 待更新字段
     * @param token    Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/user/info")
    public Result updateUserInfo(@RequestBody Guardian guardian, @RequestHeader("Authorization") String token) {
        Guardian current = support.requireGuardian(token);
        guardian.setId(current.getId());
        // 调用业务层更新监护人
        support.guardianService().updateGuardian(guardian);
        return Result.success();
    }

    /**
     * 关联盲人档案 GET /api/mini/blind-profile
     *
     * @param token Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "获取盲人档案")
    @GetMapping("/blind-profile")
    public Result getBlindProfile(@RequestHeader("Authorization") String token) {
        Guardian guardian = support.requireGuardian(token);
        if (guardian.getUserId() == null) {
            return Result.error(404, "未找到关联的盲人档案");
        }
        VisuallyImpairedUser profile = support.visuallyImpairedUserService().getUserById(guardian.getUserId());
        if (profile == null) {
            return Result.error(404, "未找到关联的盲人档案");
        }
        return Result.success(profile);
    }

    /**
     * 保存盲人档案 PUT /api/mini/blind-profile
     *
     * @param profile 档案字段
     * @param token   Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "保存盲人档案")
    @PutMapping("/blind-profile")
    public Result saveBlindProfile(@RequestBody VisuallyImpairedUser profile, @RequestHeader("Authorization") String token) {
        Guardian guardian = support.requireGuardian(token);
        if (guardian.getUserId() == null) {
            return Result.error(404, "未找到关联的盲人档案");
        }
        VisuallyImpairedUser existingUser = support.visuallyImpairedUserService().getUserById(guardian.getUserId());
        if (existingUser == null) {
            return Result.error(404, "未找到关联的盲人档案");
        }
        profile.setId(existingUser.getId());
        profile.setUsername(existingUser.getUsername());
        profile.setPassword(existingUser.getPassword());
        profile.setIdCard(profile.getIdCard() == null || profile.getIdCard().trim().isEmpty()
                ? existingUser.getIdCard() : profile.getIdCard());
        profile.setName(profile.getName() == null ? existingUser.getName() : profile.getName());
        profile.setPhone(profile.getPhone() == null ? existingUser.getPhone() : profile.getPhone());
        profile.setAddress(profile.getAddress() == null ? existingUser.getAddress() : profile.getAddress());
        // 调用业务层按主键更新档案
        VisuallyImpairedUser savedProfile = support.visuallyImpairedUserService().updateUserById(existingUser.getId(), profile);
        return Result.success(savedProfile);
    }

    /** 日志用手机号脱敏 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
