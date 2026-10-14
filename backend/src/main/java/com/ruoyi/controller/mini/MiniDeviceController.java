package com.ruoyi.controller.mini;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.Guardian;
import com.ruoyi.entity.Result;
import com.ruoyi.entity.SensorData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序设备绑定与状态接口控制器
 */
@RestController
@RequestMapping("/api/mini")
@Tag(name = "小程序接口-Device", description = "设备绑定与状态")
@RequiredArgsConstructor
public class MiniDeviceController {

    private final MiniSupport support;

    /**
     * 绑定设备 POST /api/mini/devices/bind
     *
     * @param params 含 deviceId/deviceName
     * @param token  可选 Authorization，用于关联监护人用户
     * @return 统一返回结果
     */
    @Operation(summary = "绑定设备")
    @PostMapping("/devices/bind")
    public Result bindDevice(@RequestBody Map<String, String> params,
                             @RequestHeader(value = "Authorization", required = false) String token) {
        String deviceId = params.get("deviceId");
        String deviceName = params.get("deviceName");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }

        Long userId = null;
        if (token != null && !token.trim().isEmpty()) {
            Guardian guardian = support.resolveGuardian(token);
            if (guardian != null && guardian.getUserId() != null) {
                userId = guardian.getUserId();
            }
        }

        CaneDevice device = new CaneDevice();
        device.setDeviceId(deviceId.trim());
        device.setDeviceName(deviceName != null ? deviceName.trim() : "智能盲杖");
        device.setUserId(userId != null ? userId : 1L);
        device.setBatteryLevel(100);
        device.setStatus("在线");
        // 调用业务层完成绑定入库
        support.caneDeviceService().addDevice(device);
        return Result.success(device);
    }

    /**
     * 当前监护人设备列表 GET /api/mini/devices
     *
     * @param token Authorization
     * @return 统一返回结果
     */
    @Operation(summary = "获取设备列表")
    @GetMapping("/devices")
    public Result<List<CaneDevice>> getDeviceList(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        Guardian guardian = support.requireGuardian(token);
        if (guardian.getUserId() == null) {
            return Result.success(new ArrayList<>());
        }
        List<CaneDevice> userDevices = support.caneDeviceService().getDevicesByUserId(guardian.getUserId());
        return Result.success(userDevices == null ? new ArrayList<>() : userDevices);
    }

    /**
     * 设备详情 GET /api/mini/devices/{id}
     */
    @Operation(summary = "获取设备详情")
    @GetMapping("/devices/{id}")
    public Result<CaneDevice> getDeviceDetail(@PathVariable Long id) {
        return Result.success(support.caneDeviceService().getDeviceById(id));
    }

    /**
     * 解绑设备 DELETE /api/mini/devices/{id}
     */
    @Operation(summary = "解绑设备")
    @DeleteMapping("/devices/{id}")
    public Result unbindDevice(@PathVariable Long id) {
        // 调用业务层删除设备
        support.caneDeviceService().deleteDevice(id);
        return Result.success();
    }

    /**
     * 设备在线状态与电量 GET /api/mini/devices/{id}/status
     *
     * @param id 设备主键或设备编号
     * @return 统一返回结果
     */
    @Operation(summary = "获取设备状态")
    @GetMapping("/devices/{id}/status")
    public Result getDeviceStatus(@PathVariable String id) {
        Map<String, Object> status = new HashMap<>();
        CaneDevice device = support.resolveDevice(id);
        if (device != null) {
            status.put("status", support.normalizeDeviceStatus(device.getStatus()));
            status.put("batteryLevel", device.getBatteryLevel() != null ? device.getBatteryLevel() : 0);
        } else {
            status.put("status", "offline");
            status.put("batteryLevel", 0);
        }
        return Result.success(status);
    }

    /**
     * 设备最新位置 GET /api/mini/devices/{id}/location
     *
     * @param id 设备编号
     * @return 统一返回结果
     */
    @Operation(summary = "获取设备位置")
    @GetMapping("/devices/{id}/location")
    public Result getDeviceLocation(@PathVariable String id) {
        Map<String, Object> location = new HashMap<>();
        location.put("deviceId", id);

        SensorData latest = support.sensorDataService().getLatestSensorData(id);
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (support.isDemoLocationForce()) {
            location.put("latitude", support.getDemoLocationLatitude());
            location.put("longitude", support.getDemoLocationLongitude());
            location.put("updateTime", now);
        } else if (latest != null && latest.getLatitude() != null && latest.getLongitude() != null) {
            location.put("latitude", latest.getLatitude());
            location.put("longitude", latest.getLongitude());
            location.put("updateTime", latest.getDataTime() != null ? latest.getDataTime() : latest.getCreateTime());
        } else {
            location.put("latitude", support.getDemoLocationLatitude());
            location.put("longitude", support.getDemoLocationLongitude());
            location.put("updateTime", now);
        }
        location.put("address", "");
        return Result.success(location);
    }

    /**
     * 设备最新传感摘要 GET /api/mini/devices/{deviceId}/sensor-data
     *
     * @param deviceId 设备编号
     * @return 统一返回结果
     */
    @Operation(summary = "获取传感器数据")
    @GetMapping("/devices/{deviceId}/sensor-data")
    public Result getSensorData(@PathVariable String deviceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);

        SensorData latest = support.sensorDataService().getLatestSensorData(deviceId);
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
            List<SensorData> todayData = support.sensorDataService().getTrajectory(deviceId, 24);
            data.put("stepCount", todayData != null ? todayData.size() * 150 : 0);
        } else {
            data.put("obstacleDistance", 0);
            data.put("isFall", false);
            data.put("temperature", 0);
            data.put("humidity", 0);
            data.put("stepCount", 0);
            data.put("updateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        data.put("online", latest != null);
        return Result.success(data);
    }
}
