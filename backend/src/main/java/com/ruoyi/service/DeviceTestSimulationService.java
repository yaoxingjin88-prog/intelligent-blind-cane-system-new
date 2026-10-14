package com.ruoyi.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 设备联调模拟：定时生成传感样本并走正式入库链路，便于无硬件时联调
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTestSimulationService {
    private static final int REPORT_INTERVAL_SECONDS = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SensorDataService sensorDataService;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("device-test-simulator");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 启动指定设备的定时模拟上报（已在跑则失败）
     *
     * @param device 设备档案
     * @return 是否成功启动
     */
    public synchronized boolean startSimulation(CaneDevice device) {
        if (device == null || device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            return false;
        }
        String deviceId = device.getDeviceId();
        ScheduledFuture<?> existing = runningTasks.get(deviceId);
        if (existing != null && !existing.isCancelled() && !existing.isDone()) {
            return false;
        }
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() -> pushSample(deviceId), 0, REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        runningTasks.put(deviceId, future);
        return true;
    }

    /**
     * 停止指定设备的模拟上报
     *
     * @param deviceId 业务设备号
     * @return 是否成功停止
     */
    public synchronized boolean stopSimulation(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        ScheduledFuture<?> future = runningTasks.remove(deviceId);
        if (future == null) {
            return false;
        }
        future.cancel(true);
        return true;
    }

    /** 当前仍在模拟中的设备号集合（顺带清理已结束任务） */
    public synchronized Set<String> getRunningDeviceIds() {
        runningTasks.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isCancelled() || entry.getValue().isDone());
        return new TreeSet<>(runningTasks.keySet());
    }

    /**
     * 生成一条模拟传感样本并入库（偶发大力加速度，由正式链路判跌倒）
     * 业务步骤：1. 取基准坐标并随机扰动 2. 填充传感字段 3. 走 addSensorData 正式链路
     *
     * @param deviceId 业务设备号
     */
    private void pushSample(String deviceId) {
        try {
            // 步骤1：取基准坐标并随机扰动
            double[] baseCoordinate = getBaseCoordinate(deviceId);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            boolean simulatedFall = random.nextDouble() < 0.02;

            // 步骤2：填充传感字段
            SensorData sensorData = new SensorData();
            sensorData.setDeviceId(deviceId);
            sensorData.setObstacleDistance(round(35 + random.nextDouble() * 95));
            sensorData.setAccelX(round(simulatedFall ? randomSigned(2.4, 3.1) : randomSigned(0.75, 1.15)));
            sensorData.setAccelY(round(simulatedFall ? randomSigned(2.2, 2.9) : randomSigned(0.75, 1.10)));
            sensorData.setAccelZ(round(simulatedFall ? randomSigned(0.05, 0.35) : randomSigned(0.85, 1.20)));
            sensorData.setIsFall(false);
            sensorData.setLatitude(round(baseCoordinate[0] + (random.nextDouble() - 0.5) * 0.006, 6));
            sensorData.setLongitude(round(baseCoordinate[1] + (random.nextDouble() - 0.5) * 0.006, 6));
            sensorData.setTemperature(round(20 + random.nextDouble() * 10));
            sensorData.setHumidity(round(40 + random.nextDouble() * 20));
            sensorData.setDataTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            // 步骤3：走 addSensorData 正式链路
            sensorDataService.addSensorData(sensorData);
        } catch (Exception e) {
            log.warn("设备测试模拟上报失败: {}", e.getMessage());
        }
    }

    private double[] getBaseCoordinate(String deviceId) {
        return switch (deviceId) {
            case "DEVICE002" -> new double[]{39.9060, 116.4108};
            case "DEVICE003" -> new double[]{39.9015, 116.4036};
            case "DEVICE004" -> new double[]{39.9083, 116.4142};
            default -> new double[]{39.9042, 116.4074};
        };
    }

    private double randomSigned(double minAbs, double maxAbs) {
        double value = minAbs + ThreadLocalRandom.current().nextDouble() * (maxAbs - minAbs);
        return ThreadLocalRandom.current().nextBoolean() ? value : -value;
    }

    private double round(double value) {
        return round(value, 1);
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    @PreDestroy
    public void shutdown() {
        runningTasks.values().forEach(task -> {
            if (task != null) {
                task.cancel(true);
            }
        });
        runningTasks.clear();
        executorService.shutdownNow();
    }
}
