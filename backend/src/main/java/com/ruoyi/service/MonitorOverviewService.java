package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * 管理端监控总览：设备行 + 未处理报警，带短时缓存与单飞加载
 */
@Service
@RequiredArgsConstructor
public class MonitorOverviewService {
    private static final long CACHE_MS = 10_000L;

    private volatile OverviewCache cache;
    private volatile CompletableFuture<Map<String, Object>> loadingFuture;

    private final CaneDeviceService caneDeviceService;
    private final SensorDataService sensorDataService;
    private final AlarmRecordService alarmRecordService;

    /**
     * 获取监控总览（10s 缓存；并发请求合并为一次构建）
     * 业务步骤：1. 命中缓存则直接返回 2. 单飞异步构建并回填缓存 3. 等待构建结果
     *
     * @return devices / alarms / testingDeviceIds
     */
    public Map<String, Object> getOverview() {
        // 步骤1：命中缓存则直接返回
        long now = System.currentTimeMillis();
        OverviewCache cached = cache;
        if (cached != null && cached.expiresAt > now) {
            return cached.data;
        }

        // 步骤2：单飞异步构建并回填缓存
        CompletableFuture<Map<String, Object>> inFlight = loadingFuture;
        if (inFlight == null || inFlight.isCompletedExceptionally()) {
            synchronized (this) {
                now = System.currentTimeMillis();
                cached = cache;
                if (cached != null && cached.expiresAt > now) {
                    return cached.data;
                }
                inFlight = loadingFuture;
                if (inFlight == null || inFlight.isCompletedExceptionally()) {
                    final CompletableFuture<Map<String, Object>> created =
                            CompletableFuture.supplyAsync(this::buildOverview);
                    loadingFuture = created;
                    inFlight = created;
                    created.whenComplete((data, err) -> {
                        if (err == null && data != null) {
                            cache = new OverviewCache(data, System.currentTimeMillis() + CACHE_MS);
                        }
                        if (loadingFuture == created) {
                            loadingFuture = null;
                        }
                    });
                }
            }
        }

        // 步骤3：等待构建结果
        try {
            return inFlight.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("监控总览加载失败", cause);
        }
    }

    /**
     * 组装总览：设备列表挂最新传感/报警，并附未处理报警与联调中设备
     */
    private Map<String, Object> buildOverview() {
        List<CaneDevice> devices = caneDeviceService.getAllDevices();
        if (devices == null) {
            devices = List.of();
        }

        Set<String> testingIds = caneDeviceService.getTestingDeviceIds();
        Map<String, SensorData> latestSensorByDevice = toLatestSensorMap(sensorDataService.getLatestSensorDataForAllDevices());
        Map<String, AlarmRecord> latestAlarmByDevice = toLatestAlarmMap(alarmRecordService.getLatestAlarmForAllDevices());
        List<AlarmRecord> unhandledAlarms = alarmRecordService.getUnhandledAlarmRecords();

        List<Map<String, Object>> deviceRows = new ArrayList<>();
        for (CaneDevice device : devices) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", device.getId());
            row.put("deviceId", device.getDeviceId());
            row.put("deviceName", device.getDeviceName());
            row.put("userId", device.getUserId());
            row.put("userName", device.getUserName());
            row.put("batteryLevel", device.getBatteryLevel());
            row.put("status", device.getStatus());
            row.put("latestData", latestSensorByDevice.get(device.getDeviceId()));
            row.put("latestAlarm", latestAlarmByDevice.get(device.getDeviceId()));
            row.put("testing", device.getDeviceId() != null && testingIds.contains(device.getDeviceId()));
            deviceRows.add(row);
        }

        List<AlarmRecord> sortedUnhandled = unhandledAlarms == null ? List.of() : unhandledAlarms.stream()
                .sorted(Comparator.comparing(AlarmRecord::getAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("devices", deviceRows);
        result.put("alarms", sortedUnhandled);
        result.put("testingDeviceIds", testingIds);
        return result;
    }

    private Map<String, SensorData> toLatestSensorMap(List<SensorData> list) {
        Map<String, SensorData> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (SensorData item : list) {
            if (item != null && item.getDeviceId() != null) {
                map.put(item.getDeviceId(), item);
            }
        }
        return map;
    }

    private Map<String, AlarmRecord> toLatestAlarmMap(List<AlarmRecord> list) {
        Map<String, AlarmRecord> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (AlarmRecord item : list) {
            if (item != null && item.getDeviceId() != null) {
                map.put(item.getDeviceId(), item);
            }
        }
        return map;
    }

    private static final class OverviewCache {
        private final Map<String, Object> data;
        private final long expiresAt;

        private OverviewCache(Map<String, Object> data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }
}
