package com.ruoyi.service;

import com.ruoyi.entity.ElectronicFence;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.ElectronicFenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

 import java.util.List;
 import java.util.stream.Collectors;

@Service
public class ElectronicFenceService {
    private static final double DEFAULT_RADIUS_METERS = 300.0;

    @Autowired
    private ElectronicFenceMapper electronicFenceMapper;

    public List<ElectronicFence> getAll() {
        return electronicFenceMapper.getAll()
                .stream()
                .map(this::normalizeAndPersistIfNeeded)
                .collect(Collectors.toList());
    }

    public ElectronicFence getByDeviceId(String deviceId) {
        ElectronicFence fence = electronicFenceMapper.getByDeviceId(deviceId);
        return normalizeAndPersistIfNeeded(fence);
    }

    public ElectronicFence save(ElectronicFence fence) {
        normalizeFence(fence);
        ElectronicFence existing = electronicFenceMapper.getByDeviceId(fence.getDeviceId());
        if (existing == null) {
            if (fence.getLastStatus() == null || fence.getLastStatus().isBlank()) {
                fence.setLastStatus("INSIDE");
            }
            if (fence.getEnabled() == null) {
                fence.setEnabled(Boolean.TRUE);
            }
            electronicFenceMapper.insert(fence);
            return fence;
        }
        existing.setFenceName(fence.getFenceName());
        existing.setCenterLatitude(fence.getCenterLatitude());
        existing.setCenterLongitude(fence.getCenterLongitude());
        existing.setRadiusMeters(fence.getRadiusMeters());
        existing.setEnabled(fence.getEnabled());
        if (fence.getLastStatus() != null && !fence.getLastStatus().isBlank()) {
            existing.setLastStatus(fence.getLastStatus());
        }
        electronicFenceMapper.update(existing);
        return existing;
    }

    public FenceEvaluationResult evaluate(String deviceId, SensorData sensorData) {
        ElectronicFence fence = normalizeAndPersistIfNeeded(electronicFenceMapper.getByDeviceId(deviceId));
        if (fence == null || Boolean.FALSE.equals(fence.getEnabled())) {
            return null;
        }
        if (sensorData.getLatitude() == null || sensorData.getLongitude() == null || fence.getCenterLatitude() == null || fence.getCenterLongitude() == null || fence.getRadiusMeters() == null) {
            return null;
        }

        double distanceMeters = calculateDistanceMeters(
                fence.getCenterLatitude(),
                fence.getCenterLongitude(),
                sensorData.getLatitude(),
                sensorData.getLongitude()
        );
        boolean outside = distanceMeters > fence.getRadiusMeters();
        String newStatus = outside ? "OUTSIDE" : "INSIDE";
        boolean triggered = outside && !"OUTSIDE".equalsIgnoreCase(fence.getLastStatus());

        if (!newStatus.equalsIgnoreCase(fence.getLastStatus())) {
            fence.setLastStatus(newStatus);
            electronicFenceMapper.updateLastStatus(fence);
        }

        return new FenceEvaluationResult(fence, distanceMeters, outside, triggered);
    }

    private double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
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

    private ElectronicFence normalizeAndPersistIfNeeded(ElectronicFence fence) {
        if (fence == null) {
            return null;
        }
        boolean changed = normalizeFence(fence);
        if (changed) {
            electronicFenceMapper.update(fence);
        }
        return fence;
    }

    private boolean normalizeFence(ElectronicFence fence) {
        boolean changed = false;
        if (fence.getFenceName() == null || fence.getFenceName().isBlank() || "默认安全区".equals(fence.getFenceName())) {
            fence.setFenceName("安全活动区");
            changed = true;
        }
        if (fence.getRadiusMeters() == null || fence.getRadiusMeters() < 100) {
            fence.setRadiusMeters(DEFAULT_RADIUS_METERS);
            changed = true;
        }
        if (fence.getEnabled() == null) {
            fence.setEnabled(Boolean.TRUE);
            changed = true;
        }
        if (fence.getLastStatus() == null || fence.getLastStatus().isBlank()) {
            fence.setLastStatus("INSIDE");
            changed = true;
        }
        return changed;
    }
}
