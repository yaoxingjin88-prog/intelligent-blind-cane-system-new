from pathlib import Path

root = Path(r'c:\Users\Lenovo\Desktop\个人项目\mangzhang\intelligent-blind-cane-system\backend\src\main\java\com\ruoyi')
src_path = root / 'controller' / 'MiniController.java'
src = src_path.read_text(encoding='utf-8')
out_dir = root / 'controller' / 'mini'
out_dir.mkdir(parents=True, exist_ok=True)

helper_start = src.index('    private Guardian resolveGuardian')
helper_end = src.rindex('}')
helpers = src[helper_start:helper_end].replace('    private ', '    public ')
# DATE_TIME_FORMATTER references in helpers should use MiniSupport.DATE_TIME_FORMATTER
helpers = helpers.replace('DATE_TIME_FORMATTER', 'DATE_TIME_FORMATTER')

extract = '''    public String extractToken(String token) {
        if (token == null) {
            return null;
        }
        return token.replace("Bearer", "").trim();
    }
'''

support = f'''package com.ruoyi.controller.mini;

import com.ruoyi.config.SecurityProperties;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.ElectronicFenceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import com.ruoyi.service.AlarmRecordService;
import com.ruoyi.service.CaneDeviceService;
import com.ruoyi.service.CrossingAssistService;
import com.ruoyi.service.ElectronicFenceService;
import com.ruoyi.service.FeedbackService;
import com.ruoyi.service.GuardianService;
import com.ruoyi.service.VisuallyImpairedUserService;
import com.ruoyi.utils.JwtUtil;
import com.ruoyi.websocket.AlarmWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MiniSupport {{
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${{demo.location.force:false}}")
    public Boolean demoLocationForce;

    @Value("${{demo.location.latitude:36.321143}}")
    public Double demoLocationLatitude;

    @Value("${{demo.location.longitude:120.397571}}")
    public Double demoLocationLongitude;

    @Autowired public GuardianService guardianService;
    @Autowired public CaneDeviceService caneDeviceService;
    @Autowired public AlarmRecordService alarmRecordService;
    @Autowired public FeedbackService feedbackService;
    @Autowired public ElectronicFenceService electronicFenceService;
    @Autowired public ElectronicFenceMapper electronicFenceMapper;
    @Autowired public SensorDataMapper sensorDataMapper;
    @Autowired public JwtUtil jwtUtil;
    @Autowired public SecurityProperties securityProperties;
    @Autowired public VisuallyImpairedUserService visuallyImpairedUserService;
    @Autowired public CrossingAssistService crossingAssistService;
    @Autowired public AlarmWebSocketHandler alarmWebSocketHandler;

{extract}
{helpers}
}}
'''
(out_dir / 'MiniSupport.java').write_text(support, encoding='utf-8')

# Build slim MiniController that uses MiniSupport for helpers and fields via support.
# Keep all endpoint methods but replace field access and helper calls.

body_start = src.index('@Operation(summary = "小程序登录"')
body_end = helper_start
endpoints = src[body_start:body_end]

# Rewrite endpoint body to use support.
replacements = [
    ('securityProperties.', 'support.securityProperties.'),
    ('guardianService.', 'support.guardianService.'),
    ('caneDeviceService.', 'support.caneDeviceService.'),
    ('alarmRecordService.', 'support.alarmRecordService.'),
    ('feedbackService.', 'support.feedbackService.'),
    ('electronicFenceService.', 'support.electronicFenceService.'),
    ('electronicFenceMapper.', 'support.electronicFenceMapper.'),
    ('sensorDataMapper.', 'support.sensorDataMapper.'),
    ('jwtUtil.', 'support.jwtUtil.'),
    ('visuallyImpairedUserService.', 'support.visuallyImpairedUserService.'),
    ('crossingAssistService.', 'support.crossingAssistService.'),
    ('alarmWebSocketHandler.', 'support.alarmWebSocketHandler.'),
    ('demoLocationForce', 'support.demoLocationForce'),
    ('demoLocationLatitude', 'support.demoLocationLatitude'),
    ('demoLocationLongitude', 'support.demoLocationLongitude'),
    ('DATE_TIME_FORMATTER', 'MiniSupport.DATE_TIME_FORMATTER'),
    ('extractToken(', 'support.extractToken('),
    ('resolveGuardian(', 'support.resolveGuardian('),
    ('resolveDevice(', 'support.resolveDevice('),
    ('buildGuardianCareOverview(', 'support.buildGuardianCareOverview('),
    ('normalizeDeviceStatus(', 'support.normalizeDeviceStatus('),
    ('calculateDistance(', 'support.calculateDistance('),
]
for a, b in replacements:
    endpoints = endpoints.replace(a, b)

# Split into files by section markers
sections = {
    'MiniAuthController.java': (
        'Auth',
        '@Operation(summary = "小程序登录"',
        '@Operation(summary = "获取设备列表"',
    ),
    'MiniDeviceController.java': (
        'Device',
        '@Operation(summary = "获取设备列表"',
        '@Operation(summary = "获取家属守护概览"',
    ),
    'MiniCareController.java': (
        'Care',
        '@Operation(summary = "获取家属守护概览"',
        '@Operation(summary = "获取报警列表"',
    ),
    'MiniAlarmController.java': (
        'Alarm',
        '@Operation(summary = "获取报警列表"',
        '// ==================== 设备绑定 ====================',
    ),
    'MiniFenceController.java': (
        'Fence',
        '// ==================== 设备绑定 ====================',
        None,
    ),
}

header_tpl = '''package com.ruoyi.controller.mini;

import com.ruoyi.dto.MiniSmsLoginRequest;
import com.ruoyi.entity.*;
import com.ruoyi.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-{name}", description = "小程序{name}相关接口")
public class {cls} {{

    @Autowired
    private MiniSupport support;

'''

# Find section slices in endpoints string
def slice_from(text, start_marker, end_marker):
    s = text.index(start_marker)
    e = len(text) if end_marker is None else text.index(end_marker)
    return text[s:e]

for filename, (name, start, end) in sections.items():
    cls = filename.replace('.java', '')
    chunk = slice_from(endpoints, start, end)
    # Auth login method: replace Map params with DTO for login/register
    if cls == 'MiniAuthController':
        chunk = chunk.replace(
            'public Result login(@RequestBody Map<String, String> params) {\n        String phone = params.get("phone");\n        String code = params.get("code");',
            'public Result login(@Valid @RequestBody MiniSmsLoginRequest params) {\n        String phone = params.getPhone();\n        String code = params.getCode();'
        )
        chunk = chunk.replace(
            'public Result register(@RequestBody Map<String, String> params) {\n        String phone = params.get("phone");\n        String code = params.get("code");',
            'public Result register(@Valid @RequestBody MiniSmsLoginRequest params) {\n        String phone = params.getPhone();\n        String code = params.getCode();'
        )
    content = header_tpl.format(name=name, cls=cls) + chunk + '\n}\n'
    (out_dir / filename).write_text(content, encoding='utf-8')
    print('wrote', filename, 'lines', len(content.splitlines()))

# Remove old MiniController
src_path.unlink()
print('removed MiniController.java')
print('done')
