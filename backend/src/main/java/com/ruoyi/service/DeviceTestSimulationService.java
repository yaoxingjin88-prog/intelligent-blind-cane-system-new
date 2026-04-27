package com.ruoyi.service;

import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class DeviceTestSimulationService {
    private static final int REPORT_INTERVAL_SECONDS = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SensorDataService sensorDataService;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("device-test-simulator");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

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

    public synchronized Set<String> getRunningDeviceIds() {
        runningTasks.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isCancelled() || entry.getValue().isDone());
        return new TreeSet<>(runningTasks.keySet());
    }

    private void pushSample(String deviceId) {
        try {
            double[] baseCoordinate = getBaseCoordinate(deviceId);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            boolean simulatedFall = random.nextDouble() < 0.02;

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
            sensorDataService.addSensorData(sensorData);
        } catch (Exception e) {
            System.err.println("设备测试模拟上报失败: " + e.getMessage());
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
