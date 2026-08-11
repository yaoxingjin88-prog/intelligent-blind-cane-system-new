package com.ruoyi.service;

import com.ruoyi.entity.AlarmRecord;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MonitorOverviewService {

    @Autowired
    private CaneDeviceService caneDeviceService;
    @Autowired
    private SensorDataService sensorDataService;
    @Autowired
    private AlarmRecordService alarmRecordService;

    public Map<String, Object> getOverview() {
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
}
