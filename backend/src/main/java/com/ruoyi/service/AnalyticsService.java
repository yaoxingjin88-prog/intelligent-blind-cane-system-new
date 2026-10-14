package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * 管理端数据分析看板：概览指标、趋势、报警分布、设备健康与热力点（带短时缓存）
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long DASHBOARD_CACHE_MS = 30_000L;

    private volatile DashboardCache dashboardCache;
    private volatile CompletableFuture<Map<String, Object>> loadingFuture;

    private final CaneDeviceService caneDeviceService;
    private final SensorDataService sensorDataService;
    private final AlarmRecordService alarmRecordService;
    private final AppCacheService appCacheService;

    /**
     * 获取看板聚合数据（30s 缓存；并发请求合并为一次构建）
     * 业务步骤：1. 命中本机/Redis 缓存 2. 单飞异步构建并回填 3. 等待构建结果
     *
     * @return overview / activityTrend / batteryTrend / alarmDistribution 等
     */
    public Map<String, Object> getDashboardData() {
        // 步骤1：命中本机 L1
        long now = System.currentTimeMillis();
        DashboardCache cached = dashboardCache;
        if (cached != null && cached.expiresAt > now) {
            return cached.data;
        }
        // 步骤1b：共享 L2（Redis 或 AppCache 本地）
        Optional<Map<String, Object>> shared = appCacheService.getMap(AppCacheService.KEY_DASHBOARD);
        if (shared.isPresent()) {
            Map<String, Object> data = shared.get();
            dashboardCache = new DashboardCache(data, now + DASHBOARD_CACHE_MS);
            return data;
        }

        // 步骤2：单飞异步构建并回填缓存
        CompletableFuture<Map<String, Object>> inFlight = loadingFuture;
        if (inFlight == null || inFlight.isCompletedExceptionally()) {
            synchronized (this) {
                now = System.currentTimeMillis();
                cached = dashboardCache;
                if (cached != null && cached.expiresAt > now) {
                    return cached.data;
                }
                shared = appCacheService.getMap(AppCacheService.KEY_DASHBOARD);
                if (shared.isPresent()) {
                    Map<String, Object> data = shared.get();
                    dashboardCache = new DashboardCache(data, now + DASHBOARD_CACHE_MS);
                    return data;
                }
                inFlight = loadingFuture;
                if (inFlight == null || inFlight.isCompletedExceptionally()) {
                    final CompletableFuture<Map<String, Object>> created =
                            CompletableFuture.supplyAsync(this::buildDashboardData);
                    loadingFuture = created;
                    inFlight = created;
                    created.whenComplete((data, err) -> {
                        if (err == null && data != null) {
                            long expireAt = System.currentTimeMillis() + DASHBOARD_CACHE_MS;
                            dashboardCache = new DashboardCache(data, expireAt);
                            appCacheService.putMap(AppCacheService.KEY_DASHBOARD, data, Duration.ofMillis(DASHBOARD_CACHE_MS));
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
            throw new IllegalStateException("看板数据加载失败", cause);
        }
    }

    /** 拉取设备/传感/报警统计并组装各看板模块 */
    private Map<String, Object> buildDashboardData() {
        List<CaneDevice> devices = safeList(caneDeviceService.getAllDevices());
        List<SensorData> sensorDataList = safeList(sensorDataService.getSensorDataSinceDaysLimited(7, 2000));
        int sensorTotalCount = sensorDataService.countAllSensorData();
        int alarmTotalCount = alarmRecordService.countAllAlarmRecords();
        int unhandledAlarmCount = alarmRecordService.countUnhandledAlarmRecords();
        int riskEventCount = sensorDataService.countRiskEventsSinceDays(7);
        int activeDevicesToday = sensorDataService.countActiveDevicesToday();

        Map<String, CaneDevice> deviceMap = devices.stream()
                .filter(device -> device.getDeviceId() != null)
                .collect(Collectors.toMap(CaneDevice::getDeviceId, device -> device, (left, right) -> left, LinkedHashMap::new));

        Map<String, SensorData> latestSensorByDevice = new HashMap<>();
        for (SensorData sensorData : safeList(sensorDataService.getLatestSensorDataForAllDevices())) {
            if (sensorData.getDeviceId() != null) {
                latestSensorByDevice.put(sensorData.getDeviceId(), sensorData);
            }
        }

        Map<String, Long> unhandledByDevice = alarmRecordService.getUnhandledCountByDevice().stream()
                .filter(row -> row.get("deviceId") != null)
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("deviceId")),
                        row -> ((Number) row.getOrDefault("unhandledCount", 0)).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        Map<String, Object> overview = buildOverview(devices, unhandledAlarmCount, alarmTotalCount, riskEventCount, activeDevicesToday, sensorTotalCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        result.put("activityTrend", buildActivityTrend(sensorDataList));
        result.put("batteryTrend", buildBatteryTrend(devices));
        result.put("alarmDistribution", buildAlarmDistributionFromStats(alarmRecordService.getAlarmTypeDistribution()));
        result.put("deviceRanking", buildDeviceRankingFromStats(alarmRecordService.getDeviceAlarmStats(6), deviceMap));
        result.put("deviceHealth", buildDeviceHealth(devices, latestSensorByDevice, unhandledByDevice));
        result.put("heatmapPoints", buildHeatmapPoints(sensorDataList, deviceMap));
        return result;
    }

    private Map<String, Object> buildOverview(
            List<CaneDevice> devices,
            long unhandledAlarms,
            long alarmCount,
            long riskEvents,
            long activeDevicesToday,
            long sensorCount) {
        long onlineDevices = devices.stream().filter(device -> isOnline(device.getStatus())).count();
        long lowBatteryDevices = devices.stream()
                .filter(device -> device.getBatteryLevel() != null && device.getBatteryLevel() <= 20)
                .count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("deviceCount", devices.size());
        overview.put("onlineDevices", onlineDevices);
        overview.put("lowBatteryDevices", lowBatteryDevices);
        overview.put("alarmCount", alarmCount);
        overview.put("unhandledAlarms", unhandledAlarms);
        overview.put("sensorCount", sensorCount);
        overview.put("riskEvents", riskEvents);
        overview.put("activeDevicesToday", activeDevicesToday);
        return overview;
    }

    private List<Map<String, Object>> buildAlarmDistributionFromStats(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.getOrDefault("name", "其他报警"));
            item.put("value", ((Number) row.getOrDefault("value", 0)).longValue());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDeviceRankingFromStats(List<Map<String, Object>> rows, Map<String, CaneDevice> deviceMap) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            String deviceId = String.valueOf(row.get("deviceId"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", deviceId);
            item.put("deviceName", normalizeDeviceName(deviceMap.get(deviceId)));
            item.put("alarmCount", ((Number) row.getOrDefault("alarmCount", 0)).intValue());
            item.put("unhandledCount", ((Number) row.getOrDefault("unhandledCount", 0)).intValue());
            item.put("latestAlarmTime", row.getOrDefault("latestAlarmTime", "-"));
            return item;
        }).collect(Collectors.toList());
    }

    private static final class DashboardCache {
        private final Map<String, Object> data;
        private final long expiresAt;

        private DashboardCache(Map<String, Object> data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }

    private List<Map<String, Object>> buildActivityTrend(List<SensorData> sensorDataList) {
        Map<String, List<LocalDateTime>> groupedTimes = new HashMap<>();
        for (SensorData sensorData : sensorDataList) {
            LocalDateTime time = extractSensorTime(sensorData);
            if (time == null || sensorData.getDeviceId() == null) {
                continue;
            }
            String key = time.toLocalDate() + "|" + sensorData.getDeviceId();
            groupedTimes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(time);
        }

        Map<LocalDate, Long> activeMinutesByDate = new HashMap<>();
        Map<LocalDate, Set<String>> activeDevicesByDate = new HashMap<>();

        for (Map.Entry<String, List<LocalDateTime>> entry : groupedTimes.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            LocalDate date = LocalDate.parse(parts[0]);
            String deviceId = parts[1];
            List<LocalDateTime> times = entry.getValue();
            times.sort(LocalDateTime::compareTo);
            long minutes = 1;
            if (times.size() > 1) {
                minutes = Math.max(1, Duration.between(times.get(0), times.get(times.size() - 1)).toMinutes() + 1);
            }
            minutes = Math.min(minutes, 12 * 60L);
            activeMinutesByDate.merge(date, minutes, Long::sum);
            activeDevicesByDate.computeIfAbsent(date, ignored -> new TreeSet<>()).add(deviceId);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("activeMinutes", activeMinutesByDate.getOrDefault(date, 0L));
            item.put("activeDevices", activeDevicesByDate.getOrDefault(date, Collections.emptySet()).size());
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildBatteryTrend(List<CaneDevice> devices) {
        return devices.stream()
                .sorted(Comparator.comparing(device -> device.getBatteryLevel() == null ? 101 : device.getBatteryLevel()))
                .map(device -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("deviceId", device.getDeviceId());
                    item.put("deviceName", normalizeDeviceName(device));
                    item.put("batteryLevel", device.getBatteryLevel() == null ? 0 : device.getBatteryLevel());
                    item.put("status", device.getStatus());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDeviceHealth(
            List<CaneDevice> devices,
            Map<String, SensorData> latestSensorByDevice,
            Map<String, Long> unhandledByDevice) {
        List<Map<String, Object>> panel = new ArrayList<>();
        for (CaneDevice device : devices) {
            SensorData latestSensor = latestSensorByDevice.get(device.getDeviceId());
            long unhandledCount = unhandledByDevice.getOrDefault(device.getDeviceId(), 0L);
            LocalDateTime latestTime = extractSensorTime(latestSensor);
            long freshnessMinutes = latestTime == null ? 999 : Math.max(0, Duration.between(latestTime, LocalDateTime.now()).toMinutes());
            int score = 100;
            int batteryLevel = device.getBatteryLevel() == null ? 0 : device.getBatteryLevel();
            if (batteryLevel < 60) {
                score -= (60 - batteryLevel) / 2;
            }
            if (!isOnline(device.getStatus())) {
                score -= 25;
            }
            if (freshnessMinutes > 15) {
                score -= 15;
            }
            score -= Math.min((int) unhandledCount * 8, 24);
            score = Math.max(0, Math.min(100, score));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("deviceId", device.getDeviceId());
            item.put("deviceName", normalizeDeviceName(device));
            item.put("status", device.getStatus());
            item.put("batteryLevel", batteryLevel);
            item.put("healthScore", score);
            item.put("latestDataTime", latestTime == null ? "-" : DATE_TIME_FORMATTER.format(latestTime));
            item.put("freshnessMinutes", latestTime == null ? null : freshnessMinutes);
            item.put("latestObstacleDistance", latestSensor == null ? null : latestSensor.getObstacleDistance());
            item.put("unhandledCount", unhandledCount);
            panel.add(item);
        }

        panel.sort((left, right) -> Integer.compare(((Number) left.get("healthScore")).intValue(), ((Number) right.get("healthScore")).intValue()));
        return panel;
    }

    private boolean isValidMapCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        // 排除 0,0 及明显无效点（常见未定位默认值）
        if (Math.abs(latitude) < 0.01 && Math.abs(longitude) < 0.01) {
            return false;
        }
        // 大致限制在中国及周边，避免脏数据把地图拉到大洋
        return latitude >= 3 && latitude <= 54 && longitude >= 73 && longitude <= 136;
    }

    private List<Map<String, Object>> buildHeatmapPoints(List<SensorData> sensorDataList, Map<String, CaneDevice> deviceMap) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (SensorData sensorData : sensorDataList) {
            if (!isValidMapCoordinate(sensorData.getLatitude(), sensorData.getLongitude())) {
                continue;
            }
            if (sensorData.getObstacleDistance() == null || sensorData.getObstacleDistance() > 80) {
                continue;
            }
            double lat = round(sensorData.getLatitude(), 4);
            double lng = round(sensorData.getLongitude(), 4);
            String key = lat + "|" + lng;
            Map<String, Object> point = grouped.computeIfAbsent(key, ignored -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("lat", lat);
                item.put("lng", lng);
                item.put("count", 0);
                item.put("value", 0);
                item.put("minObstacleDistance", sensorData.getObstacleDistance());
                item.put("deviceId", sensorData.getDeviceId());
                item.put("deviceName", normalizeDeviceName(deviceMap.get(sensorData.getDeviceId())));
                return item;
            });
            int count = ((Number) point.get("count")).intValue() + 1;
            double minDistance = Math.min(((Number) point.get("minObstacleDistance")).doubleValue(), sensorData.getObstacleDistance());
            int weight = (int) Math.round(count * 15 + Math.max(0, 80 - minDistance));
            point.put("count", count);
            point.put("minObstacleDistance", minDistance);
            point.put("value", weight);
        }

        return grouped.values().stream()
                .sorted((left, right) -> Integer.compare(((Number) right.get("value")).intValue(), ((Number) left.get("value")).intValue()))
                .limit(200)
                .collect(Collectors.toList());
    }

    private boolean isOnline(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("在线") || normalized.equals("active") || normalized.equals("online");
    }

    private String normalizeDeviceName(CaneDevice device) {
        if (device == null) {
            return "未命名设备";
        }
        if (device.getDeviceName() != null && !device.getDeviceName().isBlank()) {
            return device.getDeviceName();
        }
        return device.getDeviceId() == null ? "未命名设备" : device.getDeviceId();
    }

    private LocalDateTime extractSensorTime(SensorData sensorData) {
        if (sensorData == null) {
            return null;
        }
        LocalDateTime dataTime = parseDateTime(sensorData.getDataTime());
        if (dataTime != null) {
            return dataTime;
        }
        return parseDateTime(sensorData.getCreateTime());
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
