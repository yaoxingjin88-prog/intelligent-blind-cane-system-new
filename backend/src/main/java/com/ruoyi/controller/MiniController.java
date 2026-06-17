package com.ruoyi.controller;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.CrossingAssistSnapshot;
import com.ruoyi.entity.ElectronicFence;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.Feedback;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.entity.SensorData;
import com.ruoyi.entity.VisuallyImpairedUser;
import com.ruoyi.service.AlarmRecordService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.CrossingAssistService;
import com.ruoyi.service.ElectronicFenceService;
import com.ruoyi.service.FeedbackService;
import com.ruoyi.service.GuardianService;
import com.ruoyi.service.VisuallyImpairedUserService;
import com.ruoyi.mapper.ElectronicFenceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import com.ruoyi.utils.JwtUtil;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口", description = "小程序相关接口")
public class MiniController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${demo.location.force:false}")
    private Boolean demoLocationForce;

    @Value("${demo.location.latitude:39.9042}")
    private Double demoLocationLatitude;

    @Value("${demo.location.longitude:116.4074}")
    private Double demoLocationLongitude;

    @Autowired
    private GuardianService guardianService;

    @Autowired
    private CaneDeviceService caneDeviceService;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ElectronicFenceService electronicFenceService;

    @Autowired
    private ElectronicFenceMapper electronicFenceMapper;

    @Autowired
    private SensorDataMapper sensorDataMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VisuallyImpairedUserService visuallyImpairedUserService;

    @Autowired
    private CrossingAssistService crossingAssistService;

    @Autowired
    private AlarmWebSocketHandler alarmWebSocketHandler;

    private String extractToken(String token) {
        if (token == null) {
            return null;
        }
        return token.replace("Bearer", "").trim();
    }

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
            String userId = jwtUtil.getUsernameFromToken(extractToken(token));
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
            String userId = jwtUtil.getUsernameFromToken(extractToken(token));
            guardian.setId(Long.parseLong(userId));
            guardianService.updateGuardian(guardian);
            return Result.success();
        } catch (Exception e) {
            return Result.error("更新用户信息失败");
        }
    }

    @Operation(summary = "获取盲人档案", description = "获取当前监护人关联的盲人档案信息")
    @GetMapping("/blind-profile")
    public Result getBlindProfile(@RequestHeader("Authorization") String token) {
        try {
            String guardianId = jwtUtil.getUsernameFromToken(extractToken(token));
            Guardian guardian = guardianService.getGuardianById(Long.parseLong(guardianId));
            if (guardian == null || guardian.getUserId() == null) {
                return Result.error("未找到关联的盲人档案");
            }
            VisuallyImpairedUser profile = visuallyImpairedUserService.getUserById(guardian.getUserId());
            if (profile == null) {
                return Result.error("未找到关联的盲人档案");
            }
            return Result.success(profile);
        } catch (Exception e) {
            return Result.error("获取盲人档案失败");
        }
    }

    @Operation(summary = "保存盲人档案", description = "保存当前监护人关联的盲人档案信息")
    @PutMapping("/blind-profile")
    public Result saveBlindProfile(@RequestBody VisuallyImpairedUser profile, @RequestHeader("Authorization") String token) {
        try {
            String guardianId = jwtUtil.getUsernameFromToken(extractToken(token));
            Guardian guardian = guardianService.getGuardianById(Long.parseLong(guardianId));
            if (guardian == null || guardian.getUserId() == null) {
                return Result.error("未找到关联的盲人档案");
            }
            VisuallyImpairedUser existingUser = visuallyImpairedUserService.getUserById(guardian.getUserId());
            if (existingUser == null) {
                return Result.error("未找到关联的盲人档案");
            }
            profile.setId(existingUser.getId());
            profile.setUsername(existingUser.getUsername());
            profile.setPassword(existingUser.getPassword());
            profile.setIdCard(profile.getIdCard() == null || profile.getIdCard().trim().isEmpty() ? existingUser.getIdCard() : profile.getIdCard());
            profile.setName(profile.getName() == null ? existingUser.getName() : profile.getName());
            profile.setPhone(profile.getPhone() == null ? existingUser.getPhone() : profile.getPhone());
            profile.setAddress(profile.getAddress() == null ? existingUser.getAddress() : profile.getAddress());
            VisuallyImpairedUser savedProfile = visuallyImpairedUserService.updateUserById(existingUser.getId(), profile);
            return Result.success(savedProfile);
        } catch (Exception e) {
            return Result.error("保存盲人档案失败");
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
            String cleanToken = extractToken(token);
            
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
            CaneDevice device = resolveDevice(id);
            if (device != null) {
                status.put("status", normalizeDeviceStatus(device.getStatus()));
                status.put("batteryLevel", device.getBatteryLevel() != null ? device.getBatteryLevel() : 0);
            } else {
                java.util.Random random = new java.util.Random();
                status.put("status", "online");
                status.put("batteryLevel", 60 + random.nextInt(40));
            }
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("获取设备状态失败");
        }
    }

    @Operation(summary = "获取设备位置", description = "获取设备实时位置（从sensor_data表读取最新GPS）")
    @GetMapping("/devices/{id}/location")
    public Result getDeviceLocation(@PathVariable String id) {
        try {
            Map<String, Object> location = new HashMap<>();
            location.put("deviceId", id);

            // 从 sensor_data 表读取该设备最新一条有 GPS 的记录
            SensorData latest = sensorDataMapper.getLatestByDeviceId(id);
            if (Boolean.TRUE.equals(demoLocationForce)) {
                location.put("latitude", demoLocationLatitude);
                location.put("longitude", demoLocationLongitude);
                location.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            } else if (latest != null && latest.getLatitude() != null && latest.getLongitude() != null) {
                location.put("latitude", latest.getLatitude());
                location.put("longitude", latest.getLongitude());
                location.put("updateTime", latest.getDataTime() != null ? latest.getDataTime() : latest.getCreateTime());
            } else {
                // 没有传感器数据时返回默认位置
                location.put("latitude", demoLocationLatitude);
                location.put("longitude", demoLocationLongitude);
                location.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            }
            location.put("address", "");
            return Result.success(location);
        } catch (Exception e) {
            return Result.error("获取设备位置失败");
        }
    }

    @Operation(summary = "获取传感器数据", description = "获取设备传感器数据（从sensor_data表读取）")
    @GetMapping("/devices/{deviceId}/sensor-data")
    public Result getSensorData(@PathVariable String deviceId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", deviceId);

            SensorData latest = sensorDataMapper.getLatestByDeviceId(deviceId);
            if (latest != null) {
                data.put("obstacleDistance", latest.getObstacleDistance() != null ? latest.getObstacleDistance() : 0);
                data.put("isFall", latest.getIsFall() != null ? latest.getIsFall() : false);
                data.put("temperature", latest.getTemperature() != null ? latest.getTemperature() : 0);
                data.put("humidity", latest.getHumidity() != null ? latest.getHumidity() : 0);
                data.put("latitude", latest.getLatitude());
                data.put("longitude", latest.getLongitude());
                data.put("accelX", latest.getAccelX());
                data.put("accelY", latest.getAccelY());
                data.put("accelZ", latest.getAccelZ());
                data.put("fallConfidence", latest.getFallConfidence());
                data.put("updateTime", latest.getDataTime() != null ? latest.getDataTime() : latest.getCreateTime());
                // 根据已有数据计算步数（简单估算：用总记录数 * 步幅系数）
                List<SensorData> todayData = sensorDataMapper.getTrajectory(deviceId, 24);
                data.put("stepCount", todayData != null ? todayData.size() * 150 : 0);
            } else {
                data.put("obstacleDistance", 0);
                data.put("isFall", false);
                data.put("temperature", 0);
                data.put("humidity", 0);
                data.put("stepCount", 0);
                data.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            }
            data.put("online", latest != null);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取传感器数据失败");
        }
    }

    @Operation(summary = "获取家属守护概览", description = "获取家属远程协同守护卡片所需的概览数据")
    @GetMapping("/devices/{deviceId}/guardian-care/overview")
    public Result getGuardianCareOverview(@PathVariable String deviceId,
                                          @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Guardian guardian = resolveGuardian(token);
            return Result.success(buildGuardianCareOverview(deviceId, guardian));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取家属守护概览失败");
        }
    }

    @Operation(summary = "发送家属安抚语音", description = "家属向设备端下发一条安抚文本，并由小程序端播放 TTS")
    @PostMapping("/devices/{deviceId}/guardian-care/comfort")
    public Result sendGuardianComfort(@PathVariable String deviceId, @RequestBody(required = false) Map<String, String> params) {
        try {
            String content = params == null ? null : params.get("content");
            if (content == null || content.trim().isEmpty()) {
                content = "别着急，家属正在关注你，请先在安全区域稍作等待。";
            }
            String normalized = content.trim();
            alarmWebSocketHandler.sendGuardianComfort(deviceId, normalized);
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("content", normalized);
            result.put("sentAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送安抚语音失败");
        }
    }

    @Operation(summary = "发送家属目的地", description = "家属向设备端发送一个文本目的地，用于演示远程导航协同")
    @PostMapping("/devices/{deviceId}/guardian-care/destination")
    public Result sendGuardianDestination(@PathVariable String deviceId, @RequestBody Map<String, String> params) {
        try {
            String destination = params == null ? null : params.get("destination");
            if (destination == null || destination.trim().isEmpty()) {
                return Result.error("目的地不能为空");
            }
            String normalized = destination.trim();
            String prompt = "家属已发送新的目的地：" + normalized + "。请按当前安全提醒继续前进。";
            alarmWebSocketHandler.sendGuardianDestination(deviceId, normalized, prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("destination", normalized);
            result.put("sentAt", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送目的地失败");
        }
    }

    @Operation(summary = "用户端发起SOS求助", description = "用户端点击一键求助后，创建一条 SOS 报警并推送给家属端")
    @PostMapping("/devices/{deviceId}/guardian-care/sos")
    public Result triggerGuardianSos(@PathVariable String deviceId, @RequestBody(required = false) Map<String, String> params) {
        try {
            String locationText = params == null ? null : params.get("locationText");
            String content = params == null ? null : params.get("content");
            String normalizedContent = content == null || content.trim().isEmpty()
                    ? "用户端已主动发起 SOS 求助，请立即查看最新位置并尽快联系。"
                    : content.trim();
            String normalizedLocation = locationText == null || locationText.trim().isEmpty()
                    ? "当前位置待确认"
                    : locationText.trim();

            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setDeviceId(deviceId);
            alarmRecord.setAlarmType("SOS求助");
            alarmRecord.setAlarmTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            alarmRecord.setStatus("0");
            alarmRecordService.addAlarmRecord(alarmRecord);

            String guardianMessage = normalizedContent;
            alarmWebSocketHandler.sendGuardianAlert(deviceId, "SOS", "danger", guardianMessage, normalizedLocation);
            alarmWebSocketHandler.sendAlarmNotification(deviceId, alarmRecord);

            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("alarmType", "SOS求助");
            result.put("message", guardianMessage);
            result.put("locationText", normalizedLocation);
            result.put("sentAt", alarmRecord.getAlarmTime());
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发起SOS求助失败");
        }
    }

    @Operation(summary = "获取路口辅助结果", description = "获取设备最近一次路口安全辅助识别结果")
    @GetMapping("/devices/{deviceId}/crossing-assist")
    public Result<CrossingAssistSnapshot> getCrossingAssist(@PathVariable String deviceId) {
        try {
            return Result.success(crossingAssistService.getLatest(deviceId));
        } catch (Exception e) {
            return Result.error("获取路口辅助结果失败");
        }
    }

    @Operation(summary = "上报路口辅助结果", description = "由视觉模块上报红绿灯、斑马线与车辆粗提醒结果")
    @PostMapping("/devices/{deviceId}/crossing-assist")
    public Result<CrossingAssistSnapshot> updateCrossingAssist(@PathVariable String deviceId,
                                                               @RequestBody CrossingAssistSnapshot snapshot) {
        try {
            return Result.success(crossingAssistService.update(deviceId, snapshot));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上报路口辅助结果失败");
        }
    }

    @Operation(summary = "生成路口辅助演示结果", description = "生成一条演示用的路口辅助识别结果并推送")
    @PostMapping("/devices/{deviceId}/crossing-assist/mock")
    public Result<CrossingAssistSnapshot> mockCrossingAssist(@PathVariable String deviceId) {
        try {
            return Result.success(crossingAssistService.mock(deviceId));
        } catch (Exception e) {
            return Result.error("生成路口辅助演示结果失败");
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

    // ==================== 设备绑定 ====================

    @Operation(summary = "绑定设备", description = "通过设备ID绑定设备")
    @PostMapping("/devices/bind")
    public Result bindDevice(@RequestBody Map<String, String> params, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            String deviceId = params.get("deviceId");
            String deviceName = params.get("deviceName");
            if (deviceId == null || deviceId.trim().isEmpty()) {
                return Result.error("设备ID不能为空");
            }

            // 获取当前用户
            Long userId = null;
            if (token != null && !token.trim().isEmpty()) {
                String cleanToken = extractToken(token);
                String userIdStr = jwtUtil.getUsernameFromToken(cleanToken);
                Guardian guardian = guardianService.getGuardianById(Long.parseLong(userIdStr));
                if (guardian != null && guardian.getUserId() != null) {
                    userId = guardian.getUserId();
                }
            }

            // 创建设备
            CaneDevice device = new CaneDevice();
            device.setDeviceId(deviceId.trim());
            device.setDeviceName(deviceName != null ? deviceName.trim() : "智能盲杖");
            device.setUserId(userId != null ? userId : 1L);
            device.setBatteryLevel(100);
            device.setStatus("在线");
            caneDeviceService.addDevice(device);

            return Result.success(device);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("绑定设备失败: " + e.getMessage());
        }
    }

    // ==================== 电子围栏 ====================

    @Operation(summary = "获取围栏列表", description = "根据设备ID获取围栏列表")
    @GetMapping("/fences")
    public Result getFenceList(@RequestParam(required = false) String deviceId) {
        try {
            List<ElectronicFence> fences;
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                fences = electronicFenceMapper.getListByDeviceId(deviceId);
            } else {
                fences = electronicFenceService.getAll();
            }

            // 转换为前端需要的格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (ElectronicFence fence : fences) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", fence.getId());
                item.put("name", fence.getFenceName());
                item.put("type", "circle");
                item.put("radius", fence.getRadiusMeters() != null ? fence.getRadiusMeters().intValue() : 300);
                item.put("isAlarmEnabled", Boolean.TRUE.equals(fence.getEnabled()));
                item.put("status", Boolean.TRUE.equals(fence.getEnabled()) ? "active" : "inactive");
                Map<String, Object> center = new HashMap<>();
                center.put("longitude", fence.getCenterLongitude() != null ? fence.getCenterLongitude() : 116.4074);
                center.put("latitude", fence.getCenterLatitude() != null ? fence.getCenterLatitude() : 39.9042);
                item.put("center", center);
                item.put("deviceId", fence.getDeviceId());
                result.add(item);
            }
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取围栏列表失败");
        }
    }

    @Operation(summary = "创建围栏", description = "创建电子围栏")
    @PostMapping("/fences")
    public Result createFence(@RequestBody Map<String, Object> params) {
        try {
            ElectronicFence fence = new ElectronicFence();
            fence.setFenceName((String) params.get("name"));
            fence.setDeviceId((String) params.get("deviceId"));
            fence.setEnabled(true);
            fence.setLastStatus("INSIDE");

            // 解析半径
            Object radiusObj = params.get("radius");
            if (radiusObj instanceof Number) {
                fence.setRadiusMeters(((Number) radiusObj).doubleValue());
            } else {
                fence.setRadiusMeters(500.0);
            }

            // 解析中心坐标
            Object centerObj = params.get("center");
            if (centerObj instanceof Map) {
                Map<String, Object> center = (Map<String, Object>) centerObj;
                Object lng = center.get("longitude");
                Object lat = center.get("latitude");
                if (lng instanceof Number) fence.setCenterLongitude(((Number) lng).doubleValue());
                if (lat instanceof Number) fence.setCenterLatitude(((Number) lat).doubleValue());
            }

            electronicFenceMapper.insert(fence);

            // 返回前端需要的格式
            Map<String, Object> result = new HashMap<>();
            result.put("id", fence.getId());
            result.put("name", fence.getFenceName());
            result.put("type", "circle");
            result.put("radius", fence.getRadiusMeters() != null ? fence.getRadiusMeters().intValue() : 500);
            result.put("isAlarmEnabled", true);
            result.put("status", "active");
            Map<String, Object> center = new HashMap<>();
            center.put("longitude", fence.getCenterLongitude());
            center.put("latitude", fence.getCenterLatitude());
            result.put("center", center);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建围栏失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新围栏", description = "更新电子围栏")
    @PutMapping("/fences/{id}")
    public Result updateFence(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            ElectronicFence fence = electronicFenceMapper.getById(id);
            if (fence == null) {
                return Result.error("围栏不存在");
            }

            if (params.containsKey("name")) {
                fence.setFenceName((String) params.get("name"));
            }
            if (params.containsKey("isAlarmEnabled")) {
                fence.setEnabled((Boolean) params.get("isAlarmEnabled"));
            }
            if (params.containsKey("radius")) {
                Object radiusObj = params.get("radius");
                if (radiusObj instanceof Number) {
                    fence.setRadiusMeters(((Number) radiusObj).doubleValue());
                }
            }

            electronicFenceMapper.update(fence);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新围栏失败");
        }
    }

    @Operation(summary = "删除围栏", description = "删除电子围栏")
    @DeleteMapping("/fences/{id}")
    public Result deleteFence(@PathVariable Long id) {
        try {
            electronicFenceMapper.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除围栏失败");
        }
    }

    @Operation(summary = "获取围栏状态", description = "获取围栏状态")
    @GetMapping("/fences/{id}/status")
    public Result getFenceStatus(@PathVariable Long id) {
        try {
            ElectronicFence fence = electronicFenceMapper.getById(id);
            if (fence == null) {
                return Result.error("围栏不存在");
            }
            Map<String, Object> status = new HashMap<>();
            status.put("id", fence.getId());
            status.put("enabled", fence.getEnabled());
            status.put("lastStatus", fence.getLastStatus());
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("获取围栏状态失败");
        }
    }

    // ==================== 轨迹 ====================

    @Operation(summary = "获取轨迹数据", description = "根据时间范围获取设备轨迹")
    @GetMapping("/devices/{deviceId}/trajectory")
    public Result getTrajectory(@PathVariable String deviceId,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime) {
        try {
            List<SensorData> points;
            if (startTime != null && endTime != null) {
                points = sensorDataMapper.getTrajectoryByTimeRange(deviceId, startTime, endTime);
            } else {
                // 默认获取最近24小时
                points = sensorDataMapper.getTrajectory(deviceId, 24);
            }

            // 转换为前端需要的格式 [{latitude, longitude, time}]
            List<Map<String, Object>> result = new ArrayList<>();
            for (SensorData point : points) {
                if (point.getLatitude() != null && point.getLongitude() != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("latitude", point.getLatitude());
                    item.put("longitude", point.getLongitude());
                    item.put("time", point.getDataTime() != null ? point.getDataTime() : point.getCreateTime());
                    result.add(item);
                }
            }

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取轨迹失败");
        }
    }

    @Operation(summary = "获取轨迹统计", description = "获取设备轨迹统计数据")
    @GetMapping("/devices/{deviceId}/statistics")
    public Result getTrajectoryStatistics(@PathVariable String deviceId,
                                          @RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime) {
        try {
            List<SensorData> points;
            if (startTime != null && endTime != null) {
                points = sensorDataMapper.getTrajectoryByTimeRange(deviceId, startTime, endTime);
            } else {
                points = sensorDataMapper.getTrajectory(deviceId, 24);
            }

            // 计算统计数据
            double totalDistance = 0;
            long totalDuration = 0;
            if (points.size() >= 2) {
                for (int i = 1; i < points.size(); i++) {
                    SensorData prev = points.get(i - 1);
                    SensorData curr = points.get(i);
                    if (prev.getLatitude() != null && prev.getLongitude() != null
                            && curr.getLatitude() != null && curr.getLongitude() != null) {
                        totalDistance += calculateDistance(
                                prev.getLatitude(), prev.getLongitude(),
                                curr.getLatitude(), curr.getLongitude());
                    }
                }
                // 粗算持续时间（秒）
                totalDuration = points.size() * 30L; // 假设每个点间隔30秒
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDistance", Math.round(totalDistance));
            stats.put("duration", totalDuration);
            stats.put("avgSpeed", totalDuration > 0 ? Math.round(totalDistance / totalDuration * 36) / 10.0 : 0);
            stats.put("pointCount", points.size());
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取轨迹统计失败");
        }
    }

    // ==================== 模拟移动 ====================

    @Operation(summary = "模拟设备移动", description = "生成模拟轨迹数据，用于演示和测试")
    @PostMapping("/devices/{deviceId}/simulate")
    public Result simulateMovement(@PathVariable String deviceId,
                                   @RequestParam(defaultValue = "39.9042") double startLat,
                                   @RequestParam(defaultValue = "116.4074") double startLng,
                                   @RequestParam(defaultValue = "20") int points) {
        try {
            java.util.Random random = new java.util.Random();
            double lat = startLat;
            double lng = startLng;
            java.time.LocalDateTime time = java.time.LocalDateTime.now().minusMinutes(points);
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<Map<String, Object>> trajectory = new ArrayList<>();

            for (int i = 0; i < points; i++) {
                // 模拟步行：每步约10-30米
                lat += (random.nextDouble() - 0.3) * 0.0003;
                lng += (random.nextDouble() - 0.3) * 0.0003;

                SensorData data = new SensorData();
                data.setDeviceId(deviceId);
                data.setLatitude(lat);
                data.setLongitude(lng);
                data.setObstacleDistance(50 + random.nextDouble() * 200);
                data.setIsFall(false);
                data.setAccelX(random.nextDouble() * 2 - 1);
                data.setAccelY(random.nextDouble() * 2 - 1);
                data.setAccelZ(9.8 + random.nextDouble() * 0.5);
                data.setFallConfidence(random.nextDouble() * 0.1);
                data.setTemperature(20 + random.nextDouble() * 5);
                data.setHumidity(40 + random.nextDouble() * 20);
                data.setDataTime(time.format(fmt));

                sensorDataMapper.insert(data);

                Map<String, Object> point = new HashMap<>();
                point.put("latitude", lat);
                point.put("longitude", lng);
                point.put("time", time.format(fmt));
                trajectory.add(point);

                time = time.plusMinutes(1);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("count", points);
            result.put("trajectory", trajectory);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("模拟移动失败: " + e.getMessage());
        }
    }

    private Guardian resolveGuardian(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String guardianId = jwtUtil.getUsernameFromToken(extractToken(token));
        if (guardianId == null || guardianId.trim().isEmpty()) {
            return null;
        }
        return guardianService.getGuardianById(Long.parseLong(guardianId));
    }

    private CaneDevice resolveDevice(String deviceRef) {
        CaneDevice device = caneDeviceService.getDeviceByDeviceId(deviceRef);
        if (device != null) {
            return device;
        }
        try {
            return caneDeviceService.getDeviceById(Long.parseLong(deviceRef));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> buildGuardianCareOverview(String deviceId, Guardian guardian) {
        CaneDevice device = resolveDevice(deviceId);
        SensorData latestSensor = sensorDataMapper.getLatestByDeviceId(deviceId);
        FenceEvaluationResult fenceEvaluationResult = latestSensor == null ? null : electronicFenceService.evaluate(deviceId, latestSensor);
        AlarmRecord latestAlarm = alarmRecordService.getLatestAlarm(deviceId);
        List<SensorData> recentPoints = sensorDataMapper.getTrajectory(deviceId, 1);
        Long stationaryMinutes = calculateStationaryMinutes(recentPoints);
        boolean isFall = latestSensor != null && Boolean.TRUE.equals(latestSensor.getIsFall());
        boolean isFenceTriggered = fenceEvaluationResult != null && Boolean.TRUE.equals(fenceEvaluationResult.getTriggered());
        boolean isLowBattery = device != null && device.getBatteryLevel() != null && device.getBatteryLevel() <= 20;
        boolean isInactive = stationaryMinutes != null && stationaryMinutes >= 20;
        boolean isOffline = device == null || "offline".equals(normalizeDeviceStatus(device.getStatus()));

        String statusKey = "normal";
        String statusLevel = "safe";
        String statusText = "守护中";
        String statusDescription = "当前位置与设备状态均正常，家属可随时远程发起关怀。";
        List<Map<String, Object>> tags = new ArrayList<>();

        if (isFall) {
            statusKey = "fall";
            statusLevel = "danger";
            statusText = "跌倒风险";
            statusDescription = "检测到跌倒异常，建议家属尽快联系并确认现场情况。";
            tags.add(buildGuardianTag("跌倒", "danger"));
        } else if (isFenceTriggered) {
            statusKey = "fence";
            statusLevel = "danger";
            statusText = "越界提醒";
            statusDescription = "设备已离开守护区域，请关注最新位置并及时联系。";
            tags.add(buildGuardianTag("越界", "danger"));
        } else if (isLowBattery) {
            statusKey = "low_battery";
            statusLevel = "warning";
            statusText = "低电量";
            statusDescription = "设备电量不足，建议尽快返程或安排充电。";
            tags.add(buildGuardianTag("低电量", "warning"));
        } else if (isInactive) {
            statusKey = "inactive";
            statusLevel = "warning";
            statusText = "静止偏久";
            statusDescription = "设备已较长时间未移动，建议主动发送安抚语音确认情况。";
            tags.add(buildGuardianTag("静止 " + stationaryMinutes + " 分钟", "warning"));
        } else if (isOffline) {
            statusKey = "offline";
            statusLevel = "warning";
            statusText = "连接中断";
            statusDescription = "设备当前离线，请检查网络或设备开机状态。";
            tags.add(buildGuardianTag("设备离线", "warning"));
        } else {
            tags.add(buildGuardianTag("守护正常", "safe"));
        }

        if (latestAlarm != null && latestAlarm.getAlarmType() != null && !latestAlarm.getAlarmType().trim().isEmpty()) {
            tags.add(buildGuardianTag(latestAlarm.getAlarmType(), ("0".equals(latestAlarm.getStatus()) || "pending".equals(latestAlarm.getStatus())) ? "danger" : "neutral"));
        }

        Map<String, Object> overview = new HashMap<>();
        overview.put("deviceId", deviceId);
        overview.put("deviceName", device != null && device.getDeviceName() != null ? device.getDeviceName() : "智能盲杖");
        overview.put("guardianName", guardian != null && guardian.getName() != null ? guardian.getName() : "家属");
        overview.put("relation", guardian != null && guardian.getRelation() != null ? guardian.getRelation() : "监护人");
        overview.put("elderName", device != null && device.getUserName() != null ? device.getUserName() : "老人");
        overview.put("batteryLevel", device != null && device.getBatteryLevel() != null ? device.getBatteryLevel() : 0);
        overview.put("online", !isOffline);
        overview.put("locationText", formatLocationText(latestSensor));
        overview.put("updateTime", latestSensor != null ? (latestSensor.getDataTime() != null ? latestSensor.getDataTime() : latestSensor.getCreateTime()) : "暂无更新");
        overview.put("statusKey", statusKey);
        overview.put("statusLevel", statusLevel);
        overview.put("statusText", statusText);
        overview.put("statusDescription", statusDescription);
        overview.put("stationaryMinutes", stationaryMinutes != null ? stationaryMinutes : 0L);
        overview.put("lastAlarmType", latestAlarm != null ? latestAlarm.getAlarmType() : "");
        overview.put("lastAlarmTime", latestAlarm != null ? latestAlarm.getAlarmTime() : "");
        overview.put("tags", tags);
        if (latestSensor != null) {
            overview.put("latitude", latestSensor.getLatitude());
            overview.put("longitude", latestSensor.getLongitude());
        }
        return overview;
    }

    private String normalizeDeviceStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "offline";
        }
        String trimmed = status.trim();
        if ("在线".equals(trimmed)) {
            return "online";
        }
        if ("离线".equals(trimmed)) {
            return "offline";
        }
        return trimmed.toLowerCase();
    }

    private Map<String, Object> buildGuardianTag(String label, String tone) {
        Map<String, Object> tag = new HashMap<>();
        tag.put("label", label);
        tag.put("tone", tone);
        return tag;
    }

    private String formatLocationText(SensorData latestSensor) {
        if (latestSensor == null || latestSensor.getLatitude() == null || latestSensor.getLongitude() == null) {
            return "暂无位置数据";
        }
        return String.format("%.4f, %.4f", latestSensor.getLatitude(), latestSensor.getLongitude());
    }

    private Long calculateStationaryMinutes(List<SensorData> points) {
        if (points == null || points.size() < 3) {
            return null;
        }
        SensorData first = points.get(0);
        SensorData last = points.get(points.size() - 1);
        LocalDateTime start = parseSensorTime(first.getDataTime() != null ? first.getDataTime() : first.getCreateTime());
        LocalDateTime end = parseSensorTime(last.getDataTime() != null ? last.getDataTime() : last.getCreateTime());
        if (start == null || end == null) {
            return null;
        }
        long minutes = Math.max(0, Duration.between(start, end).toMinutes());
        if (minutes < 20) {
            return null;
        }
        if (first.getLatitude() == null || first.getLongitude() == null) {
            return null;
        }
        for (SensorData point : points) {
            if (point.getLatitude() == null || point.getLongitude() == null) {
                return null;
            }
            if (calculateDistance(first.getLatitude(), first.getLongitude(), point.getLatitude(), point.getLongitude()) > 35) {
                return null;
            }
        }
        return minutes;
    }

    private LocalDateTime parseSensorTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
