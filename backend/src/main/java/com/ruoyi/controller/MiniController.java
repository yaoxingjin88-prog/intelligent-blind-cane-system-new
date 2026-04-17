package com.ruoyi.controller;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Feedback;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.service.AlarmRecordService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.FeedbackService;
import com.ruoyi.service.GuardianService;
import com.ruoyi.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口", description = "小程序相关接口")
public class MiniController {

    @Autowired
    private GuardianService guardianService;

    @Autowired
    private CaneDeviceService caneDeviceService;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "小程序登录", description = "监护人通过手机号和验证码登录")
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String code = params.get("code");

        // 测试模式：验证码为123456时通过
        if (!"123456".equals(code)) {
            return Result.error("验证码错误");
        }

        // 根据手机号查找监护人
        Guardian guardian = guardianService.findByPhone(phone);
        if (guardian == null) {
            return Result.error("该手机号未注册为监护人");
        }

        // 生成JWT token
        String token = jwtUtil.generateToken(guardian.getId().toString());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", guardian);

        return Result.success(data);
    }

    @Operation(summary = "微信登录", description = "微信授权登录")
    @PostMapping("/wechat-login")
    public Result wechatLogin(@RequestBody Map<String, Object> params) {
        // TODO: 实现微信登录逻辑
        return Result.error("微信登录暂未实现");
    }

    @Operation(summary = "注册", description = "监护人注册")
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String code = params.get("code");

        // 测试模式：验证码为123456时通过
        if (!"123456".equals(code)) {
            return Result.error("验证码错误");
        }

        // 检查手机号是否已注册
        Guardian existingGuardian = guardianService.findByPhone(phone);
        if (existingGuardian != null) {
            return Result.error("该手机号已注册");
        }

        // 创建新监护人
        Guardian guardian = new Guardian();
        guardian.setPhone(phone);
        guardian.setName("新监护人");
        guardian.setRelation("未设置");

        guardianService.addGuardian(guardian);

        return Result.success("注册成功");
    }

    @Operation(summary = "退出登录", description = "退出登录")
    @PostMapping("/logout")
    public Result logout() {
        return Result.success();
    }

    @Operation(summary = "获取用户信息", description = "获取当前登录用户信息")
    @GetMapping("/user/info")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String userId = jwtUtil.getUsernameFromToken(token);
            Guardian guardian = guardianService.getGuardianById(Long.parseLong(userId));
            return Result.success(guardian);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }

    @Operation(summary = "更新用户信息", description = "更新当前登录用户信息")
    @PutMapping("/user/info")
    public Result updateUserInfo(@RequestBody Guardian guardian, @RequestHeader("Authorization") String token) {
        try {
            String userId = jwtUtil.getUsernameFromToken(token);
            guardian.setId(Long.parseLong(userId));
            guardianService.updateGuardian(guardian);
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新用户信息失败");
        }
    }

    @Operation(summary = "获取设备列表", description = "获取当前监护人的设备列表")
    @GetMapping("/devices")
    public Result<List<CaneDevice>> getDeviceList(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 如果没有token，返回空列表
            if (token == null || token.trim().isEmpty()) {
                return Result.success(new java.util.ArrayList<>());
            }
            
            // 移除 Bearer 前缀和空格
            String cleanToken = token.replace("Bearer", "").trim();
            
            // 根据监护人ID过滤设备
            String userId = jwtUtil.getUsernameFromToken(cleanToken);
            Guardian guardian = guardianService.getGuardianById(Long.parseLong(userId));
            
            // 如果监护人没有关联用户，返回空列表
            if (guardian == null || guardian.getUserId() == null) {
                return Result.success(new java.util.ArrayList<>());
            }
            
            // 获取所有设备，过滤出属于该监护人关联用户的设备
            List<CaneDevice> allDevices = caneDeviceService.getAllDevices();
            List<CaneDevice> userDevices = allDevices.stream()
                .filter(device -> device.getUserId() != null && device.getUserId().equals(guardian.getUserId()))
                .toList();
            
            return Result.success(userDevices);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(new java.util.ArrayList<>());
        }
    }

    @Operation(summary = "获取设备详情", description = "根据设备ID获取设备详情")
    @GetMapping("/devices/{id}")
    public Result<CaneDevice> getDeviceDetail(@PathVariable Long id) {
        try {
            CaneDevice device = caneDeviceService.getDeviceById(id);
            return Result.success(device);
        } catch (Exception e) {
            return Result.error("获取设备详情失败");
        }
    }

    @Operation(summary = "解绑设备", description = "解绑设备")
    @DeleteMapping("/devices/{id}")
    public Result unbindDevice(@PathVariable Long id) {
        try {
            caneDeviceService.deleteDevice(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("解绑设备失败");
        }
    }

    @Operation(summary = "获取设备状态", description = "获取设备状态")
    @GetMapping("/devices/{id}/status")
    public Result getDeviceStatus(@PathVariable String id) {
        try {
            Map<String, Object> status = new HashMap<>();
            CaneDevice device = null;
            try {
                device = caneDeviceService.getDeviceById(Long.parseLong(id));
            } catch (NumberFormatException ignored) {}
            if (device != null) {
                status.put("status", device.getStatus());
                status.put("batteryLevel", device.getBatteryLevel());
            } else {
                // 模拟数据
                java.util.Random random = new java.util.Random();
                status.put("status", "online");
                status.put("batteryLevel", 60 + random.nextInt(40));
            }
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("获取设备状态失败");
        }
    }

    @Operation(summary = "获取设备位置", description = "获取设备实时位置")
    @GetMapping("/devices/{id}/location")
    public Result getDeviceLocation(@PathVariable String id) {
        try {
            // 暂时返回固定位置，后续从传感器数据表获取
            java.util.Random random = new java.util.Random();
            Map<String, Object> location = new HashMap<>();
            location.put("deviceId", id);
            location.put("latitude", 39.9042 + (random.nextDouble() - 0.5) * 0.01);
            location.put("longitude", 116.4074 + (random.nextDouble() - 0.5) * 0.01);
            location.put("address", "北京市东城区东长安街");
            location.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            return Result.success(location);
        } catch (Exception e) {
            return Result.error("获取设备位置失败");
        }
    }

    @Operation(summary = "获取传感器数据", description = "获取设备传感器数据")
    @GetMapping("/devices/{deviceId}/sensor-data")
    public Result getSensorData(@PathVariable String deviceId) {
        try {
            // 模拟传感器数据（后续从传感器数据表获取）
            java.util.Random random = new java.util.Random();
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", deviceId);
            // 障碍物距离 (cm)
            data.put("obstacleDistance", Math.round((80 + random.nextDouble() * 120) * 10) / 10.0);
            // 是否跌倒
            data.put("isFall", false);
            // 温度 (°C)
            data.put("temperature", Math.round((22 + random.nextDouble() * 8) * 10) / 10.0);
            // 湿度 (%)
            data.put("humidity", Math.round((40 + random.nextDouble() * 30) * 10) / 10.0);
            // 电池电量 (%)
            data.put("battery", 60 + random.nextInt(40));
            // 信号强度
            data.put("signal", 3 + random.nextInt(3));
            // 心率 (bpm)
            data.put("heartRate", 65 + random.nextInt(25));
            // 步数
            data.put("stepCount", 3000 + random.nextInt(5000));
            // GPS位置
            data.put("latitude", 39.9042 + (random.nextDouble() - 0.5) * 0.01);
            data.put("longitude", 116.4074 + (random.nextDouble() - 0.5) * 0.01);
            // 在线状态
            data.put("online", true);
            // 更新时间
            data.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取传感器数据失败");
        }
    }

    @Operation(summary = "获取报警列表", description = "获取报警列表")
    @GetMapping("/alarms")
    public Result getAlarmList() {
        try {
            return Result.success(alarmRecordService.getAllAlarmRecords());
        } catch (Exception e) {
            return Result.error("获取报警列表失败");
        }
    }

    @Operation(summary = "获取报警统计", description = "获取报警统计数据")
    @GetMapping("/alarms/statistics")
    public Result getAlarmStatistics(@RequestParam(required = false) String deviceId) {
        try {
            java.util.Random random = new java.util.Random();
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", 12 + random.nextInt(20));
            stats.put("pending", random.nextInt(5));
            stats.put("handled", 8 + random.nextInt(15));
            stats.put("today", random.nextInt(4));
            stats.put("thisWeek", 3 + random.nextInt(8));
            stats.put("fall", random.nextInt(3));
            stats.put("outOfBounds", random.nextInt(4));
            stats.put("lowBattery", random.nextInt(3));
            stats.put("sos", random.nextInt(2));
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取报警统计失败");
        }
    }

    @Operation(summary = "处理报警", description = "处理报警")
    @PutMapping("/alarms/{id}/handle")
    public Result handleAlarm(@PathVariable Long id, @RequestBody Map<String, String> data) {
        try {
            String status = data.get("status");
            alarmRecordService.updateAlarmStatus(id, status);
            return Result.success();
        } catch (Exception e) {
            return Result.error("处理报警失败");
        }
    }

    @Operation(summary = "提交意见反馈", description = "提交意见反馈")
    @PostMapping("/feedback")
    public Result submitFeedback(@RequestBody Map<String, String> data) {
        try {
            Feedback feedback = new Feedback();
            feedback.setUserId(Long.parseLong(jwtUtil.getUsernameFromToken(data.get("token"))));
            feedback.setType(data.get("type"));
            feedback.setContent(data.get("content"));
            feedback.setContact(data.get("contact"));
            feedback.setImages(data.get("images"));
            feedbackService.submitFeedback(feedback);
            return Result.success();
        } catch (Exception e) {
            return Result.error("提交反馈失败");
        }
    }

    @Operation(summary = "获取消息列表", description = "获取消息列表")
    @GetMapping("/messages")
    public Result getMessageList() {
        try {
            // 返回模拟消息数据
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> msg1 = new HashMap<>();
            msg1.put("id", 1);
            msg1.put("type", "system");
            msg1.put("title", "系统通知");
            msg1.put("content", "欢迎使用智能盲杖系统！");
            msg1.put("createTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            msg1.put("read", false);
            messages.add(msg1);

            Map<String, Object> msg2 = new HashMap<>();
            msg2.put("id", 2);
            msg2.put("type", "alarm");
            msg2.put("title", "报警通知");
            msg2.put("content", "设备检测到跌倒报警，请及时处理。");
            msg2.put("createTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis() - 3600000)));
            msg2.put("read", true);
            messages.add(msg2);

            return Result.success(messages);
        } catch (Exception e) {
            return Result.error("获取消息列表失败");
        }
    }

    @Operation(summary = "标记消息已读", description = "标记消息已读")
    @PutMapping("/messages/{id}/read")
    public Result markMessageRead(@PathVariable Long id) {
        try {
            // 模拟标记已读
            return Result.success();
        } catch (Exception e) {
            return Result.error("标记消息已读失败");
        }
    }
}
