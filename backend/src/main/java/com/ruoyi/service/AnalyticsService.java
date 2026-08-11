package com.ruoyi.service;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CaneDeviceService caneDeviceService;
    @Autowired
    private SensorDataService sensorDataService;
    @Autowired
    private AlarmRecordService alarmRecordService;

    public Map<String, Object> getDashboardData() {
        List<CaneDevice> devices = safeList(caneDeviceService.getAllDevices());
        // 仅取近 7 天传感器数据，避免全表扫描拖垮服务
        List<SensorData> sensorDataList = safeList(sensorDataService.getSensorDataSinceDays(7));
        List<AlarmRecord> alarmRecords = safeList(alarmRecordService.getAllAlarmRecords());
        int sensorTotalCount = sensorDataService.countAllSensorData();

        Map<String, CaneDevice> deviceMap = devices.stream()
                .filter(device -> device.getDeviceId() != null)
                .collect(Collectors.toMap(CaneDevice::getDeviceId, device -> device, (left, right) -> left, LinkedHashMap::new));

        Map<String, SensorData> latestSensorByDevice = new HashMap<>();
        for (SensorData sensorData : safeList(sensorDataService.getLatestSensorDataForAllDevices())) {
            if (sensorData.getDeviceId() != null) {
                latestSensorByDevice.put(sensorData.getDeviceId(), sensorData);
            }
        }

        Map<String, List<AlarmRecord>> alarmsByDevice = alarmRecords.stream()
                .filter(alarm -> alarm.getDeviceId() != null)
                .collect(Collectors.groupingBy(AlarmRecord::getDeviceId));

        Map<String, Object> overview = buildOverview(devices, sensorDataList, alarmRecords);
        overview.put("sensorCount", sensorTotalCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        result.put("activityTrend", buildActivityTrend(sensorDataList));
        result.put("batteryTrend", buildBatteryTrend(devices));
        result.put("alarmDistribution", buildAlarmDistribution(alarmRecords));
        result.put("deviceRanking", buildDeviceRanking(devices, alarmsByDevice));
        result.put("deviceHealth", buildDeviceHealth(devices, latestSensorByDevice, alarmsByDevice));
        result.put("heatmapPoints", buildHeatmapPoints(sensorDataList, deviceMap));
        return result;
    }

    private Map<String, Object> buildOverview(List<CaneDevice> devices, List<SensorData> sensorDataList, List<AlarmRecord> alarmRecords) {
        long onlineDevices = devices.stream().filter(device -> isOnline(device.getStatus())).count();
        long lowBatteryDevices = devices.stream()
                .filter(device -> device.getBatteryLevel() != null && device.getBatteryLevel() <= 20)
                .count();
        long unhandledAlarms = alarmRecords.stream().filter(this::isUnhandledAlarm).count();
        long riskEvents = sensorDataList.stream()
                .filter(sensor -> sensor.getObstacleDistance() != null && sensor.getObstacleDistance() <= 80)
                .count();
        Set<String> activeDevicesToday = sensorDataList.stream()
                .filter(sensor -> extractSensorTime(sensor) != null)
                .filter(sensor -> LocalDate.now().equals(extractSensorTime(sensor).toLocalDate()))
                .map(SensorData::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("deviceCount", devices.size());
        overview.put("onlineDevices", onlineDevices);
        overview.put("lowBatteryDevices", lowBatteryDevices);
        overview.put("alarmCount", alarmRecords.size());
        overview.put("unhandledAlarms", unhandledAlarms);
        overview.put("sensorCount", sensorDataList.size());
        overview.put("riskEvents", riskEvents);
        overview.put("activeDevicesToday", activeDevicesToday.size());
        return overview;
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

    private List<Map<String, Object>> buildAlarmDistribution(List<AlarmRecord> alarmRecords) {
        Map<String, Long> grouped = alarmRecords.stream()
                .collect(Collectors.groupingBy(alarm -> normalizeAlarmType(alarm.getAlarmType()), LinkedHashMap::new, Collectors.counting()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDeviceRanking(List<CaneDevice> devices, Map<String, List<AlarmRecord>> alarmsByDevice) {
        Map<String, CaneDevice> deviceMap = devices.stream()
                .filter(device -> device.getDeviceId() != null)
                .collect(Collectors.toMap(CaneDevice::getDeviceId, device -> device, (left, right) -> left));

        return alarmsByDevice.entrySet().stream()
                .map(entry -> {
                    List<AlarmRecord> alarms = entry.getValue();
                    long unhandledCount = alarms.stream().filter(this::isUnhandledAlarm).count();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("deviceId", entry.getKey());
                    item.put("deviceName", normalizeDeviceName(deviceMap.get(entry.getKey())));
                    item.put("alarmCount", alarms.size());
                    item.put("unhandledCount", unhandledCount);
                    item.put("latestAlarmTime", alarms.stream()
                            .map(this::extractAlarmTime)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .map(DATE_TIME_FORMATTER::format)
                            .orElse("-"));
                    return item;
                })
                .sorted((left, right) -> Integer.compare(((Number) right.get("alarmCount")).intValue(), ((Number) left.get("alarmCount")).intValue()))
                .limit(6)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDeviceHealth(List<CaneDevice> devices, Map<String, SensorData> latestSensorByDevice, Map<String, List<AlarmRecord>> alarmsByDevice) {
        List<Map<String, Object>> panel = new ArrayList<>();
        for (CaneDevice device : devices) {
            SensorData latestSensor = latestSensorByDevice.get(device.getDeviceId());
            List<AlarmRecord> alarms = alarmsByDevice.getOrDefault(device.getDeviceId(), Collections.emptyList());
            long unhandledCount = alarms.stream().filter(this::isUnhandledAlarm).count();
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

    private boolean isUnhandledAlarm(AlarmRecord alarm) {
        if (alarm == null || alarm.getStatus() == null) {
            return false;
        }
        String status = alarm.getStatus().trim();
        return "0".equals(status) || "未处理".equals(status);
    }

    private List<Map<String, Object>> buildHeatmapPoints(List<SensorData> sensorDataList, Map<String, CaneDevice> deviceMap) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (SensorData sensorData : sensorDataList) {
            if (sensorData.getLongitude() == null || sensorData.getLatitude() == null) {
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

    private String normalizeAlarmType(String alarmType) {
        if (alarmType == null || alarmType.isBlank()) {
            return "其他报警";
        }
        return alarmType;
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

    private LocalDateTime extractAlarmTime(AlarmRecord alarmRecord) {
        if (alarmRecord == null) {
            return null;
        }
        return parseDateTime(alarmRecord.getAlarmTime());
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

    private boolean isAfter(LocalDateTime current, LocalDateTime existing) {
        if (current == null) {
            return false;
        }
        if (existing == null) {
            return true;
        }
        return current.isAfter(existing);
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }
}
