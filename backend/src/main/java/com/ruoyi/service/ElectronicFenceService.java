package com.ruoyi.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.ElectronicFence;
import com.ruoyi.entity.FenceEvaluationResult;
import com.ruoyi.entity.SensorData;
import com.ruoyi.mapper.ElectronicFenceMapper;
import com.ruoyi.utils.GeoUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 电子围栏业务：配置缓存、越界边沿判定、小程序视图组装
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElectronicFenceService {
    private static final double DEFAULT_RADIUS_METERS = 300.0;
    private static final long CACHE_TTL_MS = 30_000L;
    /** 小程序列表缺坐标时的占位中心（仅展示，不参与越界判定） */
    private static final double FALLBACK_CENTER_LAT = 39.9042;
    private static final double FALLBACK_CENTER_LNG = 116.4074;

    private final Map<String, FenceCacheEntry> fenceByDevice = new ConcurrentHashMap<>();

    private final ElectronicFenceMapper electronicFenceMapper;

    public List<ElectronicFence> getAll() {
        return electronicFenceMapper.getAll()
                .stream()
                .map(this::normalizeAndPersistIfNeeded)
                .collect(Collectors.toList());
    }

    public ElectronicFence getByDeviceId(String deviceId) {
        return getCachedOrLoad(deviceId);
    }

    public List<ElectronicFence> getListByDeviceId(String deviceId) {
        return electronicFenceMapper.getListByDeviceId(deviceId)
                .stream()
                .map(this::normalizeAndPersistIfNeeded)
                .collect(Collectors.toList());
    }

    public ElectronicFence save(ElectronicFence fence) {
        normalizeFence(fence);
        ElectronicFence existing = null;
        if (fence.getId() != null) {
            existing = electronicFenceMapper.getById(fence.getId());
        }
        if (existing == null) {
            existing = electronicFenceMapper.getByDeviceId(fence.getDeviceId());
        }
        if (existing == null) {
            if (fence.getLastStatus() == null || fence.getLastStatus().isBlank()) {
                fence.setLastStatus("INSIDE");
            }
            if (fence.getEnabled() == null) {
                fence.setEnabled(Boolean.TRUE);
            }
            electronicFenceMapper.insert(fence);
            putCache(fence);
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
        putCache(existing);
        return existing;
    }

    /**
     * 根据最新定位判定是否越界
     * 业务步骤：
     * 1. 加载启用中的围栏（带短 TTL 缓存）
     * 2. 校验坐标完整性
     * 3. Haversine 计算距离并比较半径
     * 4. 仅 INSIDE→OUTSIDE 记为 triggered，并回写 lastStatus
     *
     * @param deviceId   设备编号
     * @param sensorData 含经纬度的传感数据
     * @return 判定结果；无围栏/未启用/缺坐标时返回 null
     */
    public FenceEvaluationResult evaluate(String deviceId, SensorData sensorData) {
        // 步骤1：取启用围栏
        ElectronicFence fence = getCachedOrLoad(deviceId);
        if (fence == null || Boolean.FALSE.equals(fence.getEnabled())) {
            return null;
        }
        // 步骤2：坐标完整性
        if (sensorData.getLatitude() == null || sensorData.getLongitude() == null
                || fence.getCenterLatitude() == null || fence.getCenterLongitude() == null
                || fence.getRadiusMeters() == null) {
            return null;
        }

        // 步骤3：距离与内外状态
        double distanceMeters = GeoUtils.haversineMeters(
                fence.getCenterLatitude(),
                fence.getCenterLongitude(),
                sensorData.getLatitude(),
                sensorData.getLongitude()
        );
        boolean outside = distanceMeters > fence.getRadiusMeters();
        String newStatus = outside ? "OUTSIDE" : "INSIDE";
        String previousStatus = fence.getLastStatus();
        // 步骤4：边沿触发，避免围栏外持续上报重复刷报警
        boolean triggered = outside && !"OUTSIDE".equalsIgnoreCase(previousStatus);

        if (!newStatus.equalsIgnoreCase(previousStatus)) {
            fence.setLastStatus(newStatus);
            electronicFenceMapper.updateLastStatus(fence);
            putCache(fence);
            log.info("fence.transition deviceId={} {}->{} distanceMeters={} triggered={}",
                    deviceId, previousStatus, newStatus, Math.round(distanceMeters), triggered);
        }

        return new FenceEvaluationResult(fence, distanceMeters, outside, triggered);
    }

    public void invalidate(String deviceId) {
        if (deviceId != null) {
            fenceByDevice.remove(deviceId);
        }
    }

    public ElectronicFence getById(Long id) {
        return normalizeAndPersistIfNeeded(electronicFenceMapper.getById(id));
    }

    public void deleteById(Long id) {
        ElectronicFence fence = electronicFenceMapper.getById(id);
        electronicFenceMapper.deleteById(id);
        if (fence != null) {
            invalidate(fence.getDeviceId());
        }
    }

    public List<Map<String, Object>> listForMini(String deviceId) {
        List<ElectronicFence> fences = (deviceId != null && !deviceId.isBlank())
                ? getListByDeviceId(deviceId)
                : getAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ElectronicFence fence : fences) {
            result.add(toMiniView(fence));
        }
        return result;
    }

    public Map<String, Object> createForMini(Map<String, Object> params) {
        ElectronicFence fence = new ElectronicFence();
        fence.setFenceName((String) params.get("name"));
        fence.setDeviceId((String) params.get("deviceId"));
        fence.setEnabled(true);
        fence.setLastStatus("INSIDE");

        Object radiusObj = params.get("radius");
        if (radiusObj instanceof Number number) {
            fence.setRadiusMeters(number.doubleValue());
        } else {
            fence.setRadiusMeters(500.0);
        }

        Object centerObj = params.get("center");
        if (centerObj instanceof Map<?, ?> center) {
            Object lng = center.get("longitude");
            Object lat = center.get("latitude");
            if (lng instanceof Number number) {
                fence.setCenterLongitude(number.doubleValue());
            }
            if (lat instanceof Number number) {
                fence.setCenterLatitude(number.doubleValue());
            }
        }

        normalizeFence(fence);
        if (fence.getLastStatus() == null || fence.getLastStatus().isBlank()) {
            fence.setLastStatus("INSIDE");
        }
        if (fence.getEnabled() == null) {
            fence.setEnabled(Boolean.TRUE);
        }
        electronicFenceMapper.insert(fence);
        putCache(fence);
        return toMiniView(fence);
    }

    public void updateForMini(Long id, Map<String, Object> params) {
        ElectronicFence fence = getById(id);
        if (fence == null) {
            throw new IllegalArgumentException("围栏不存在");
        }
        if (params.containsKey("name")) {
            fence.setFenceName((String) params.get("name"));
        }
        if (params.containsKey("isAlarmEnabled")) {
            fence.setEnabled((Boolean) params.get("isAlarmEnabled"));
        }
        if (params.containsKey("radius")) {
            Object radiusObj = params.get("radius");
            if (radiusObj instanceof Number number) {
                fence.setRadiusMeters(number.doubleValue());
            }
        }
        normalizeFence(fence);
        electronicFenceMapper.update(fence);
        putCache(fence);
    }

    public Map<String, Object> statusForMini(Long id) {
        ElectronicFence fence = getById(id);
        if (fence == null) {
            throw new IllegalArgumentException("围栏不存在");
        }
        Map<String, Object> status = new HashMap<>();
        status.put("id", fence.getId());
        status.put("enabled", fence.getEnabled());
        status.put("lastStatus", fence.getLastStatus());
        return status;
    }

    public Map<String, Object> toMiniView(ElectronicFence fence) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", fence.getId());
        item.put("name", fence.getFenceName());
        item.put("type", "circle");
        item.put("radius", fence.getRadiusMeters() != null ? fence.getRadiusMeters().intValue() : (int) DEFAULT_RADIUS_METERS);
        item.put("isAlarmEnabled", Boolean.TRUE.equals(fence.getEnabled()));
        item.put("status", Boolean.TRUE.equals(fence.getEnabled()) ? "active" : "inactive");
        Map<String, Object> center = new HashMap<>();
        center.put("longitude", fence.getCenterLongitude() != null ? fence.getCenterLongitude() : FALLBACK_CENTER_LNG);
        center.put("latitude", fence.getCenterLatitude() != null ? fence.getCenterLatitude() : FALLBACK_CENTER_LAT);
        item.put("center", center);
        item.put("deviceId", fence.getDeviceId());
        return item;
    }

    private ElectronicFence getCachedOrLoad(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        FenceCacheEntry entry = fenceByDevice.get(deviceId);
        if (entry != null && entry.expiresAt > now) {
            return entry.fence;
        }
        ElectronicFence fence = normalizeAndPersistIfNeeded(electronicFenceMapper.getByDeviceId(deviceId));
        if (fence != null) {
            putCache(fence);
        } else {
            fenceByDevice.remove(deviceId);
        }
        return fence;
    }

    private void putCache(ElectronicFence fence) {
        if (fence == null || fence.getDeviceId() == null) {
            return;
        }
        fenceByDevice.put(fence.getDeviceId(), new FenceCacheEntry(fence, System.currentTimeMillis() + CACHE_TTL_MS));
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

    private static final class FenceCacheEntry {
        private final ElectronicFence fence;
        private final long expiresAt;

        private FenceCacheEntry(ElectronicFence fence, long expiresAt) {
            this.fence = fence;
            this.expiresAt = expiresAt;
        }
    }
}
