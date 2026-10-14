package com.ruoyi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.entity.SensorData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备运行时态：最新传感、监护告警去重、静止计时。
 * 有 Redis 时跨实例共享；未启用或失败时回退本机 ConcurrentHashMap。
 */
@Service
@Slf4j
public class DeviceRuntimeStore {

    private static final String PREFIX_LATEST = "runtime:latest:";
    private static final String PREFIX_ALERT = "runtime:alert:";
    private static final String PREFIX_STATIONARY = "runtime:stationary:";

    /** 最新传感保留时长（略长于心跳窗口，便于重启后短时命中） */
    private static final Duration TTL_LATEST = Duration.ofMinutes(30);
    /** 同类监护告警去重态 */
    private static final Duration TTL_ALERT = Duration.ofHours(6);
    /** 静止计时起点 */
    private static final Duration TTL_STATIONARY = Duration.ofHours(2);

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, SensorData> localLatest = new ConcurrentHashMap<>();
    private final Map<String, String> localAlertTypes = new ConcurrentHashMap<>();
    private final Map<String, StationaryState> localStationary = new ConcurrentHashMap<>();

    public DeviceRuntimeStore(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public void putLatest(String deviceId, SensorData data) {
        if (deviceId == null || data == null) {
            return;
        }
        localLatest.put(deviceId, data);
        writeJson(PREFIX_LATEST + deviceId, data, TTL_LATEST);
    }

    public SensorData getLatest(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        SensorData fromRedis = readJson(PREFIX_LATEST + deviceId, SensorData.class);
        if (fromRedis != null) {
            localLatest.put(deviceId, fromRedis);
            return fromRedis;
        }
        return localLatest.get(deviceId);
    }

    /** 本机镜像中的全部最新值（DB 不可用时的兜底；多实例以 DB 为准） */
    public List<SensorData> localLatestValues() {
        return new ArrayList<>(localLatest.values());
    }

    public String getLastAlertType(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        String fromRedis = readString(PREFIX_ALERT + deviceId);
        if (fromRedis != null) {
            localAlertTypes.put(deviceId, fromRedis);
            return fromRedis;
        }
        return localAlertTypes.get(deviceId);
    }

    public void putLastAlertType(String deviceId, String alertType) {
        if (deviceId == null || alertType == null) {
            return;
        }
        localAlertTypes.put(deviceId, alertType);
        writeString(PREFIX_ALERT + deviceId, alertType, TTL_ALERT);
    }

    public void clearLastAlertType(String deviceId) {
        if (deviceId == null) {
            return;
        }
        localAlertTypes.remove(deviceId);
        deleteKey(PREFIX_ALERT + deviceId);
    }

    public StationaryState getStationary(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        StationaryState fromRedis = readJson(PREFIX_STATIONARY + deviceId, StationaryState.class);
        if (fromRedis != null) {
            localStationary.put(deviceId, fromRedis);
            return fromRedis;
        }
        return localStationary.get(deviceId);
    }

    public void putStationary(String deviceId, StationaryState state) {
        if (deviceId == null || state == null) {
            return;
        }
        localStationary.put(deviceId, state);
        writeJson(PREFIX_STATIONARY + deviceId, state, TTL_STATIONARY);
    }

    public void clearStationary(String deviceId) {
        if (deviceId == null) {
            return;
        }
        localStationary.remove(deviceId);
        deleteKey(PREFIX_STATIONARY + deviceId);
    }

    private void writeJson(String key, Object value, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.debug("runtime.redis.put fail key={}: {}", key, e.getMessage());
        }
    }

    private <T> T readJson(String key, Class<T> type) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.debug("runtime.redis.get fail key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeString(String key, String value, Duration ttl) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.debug("runtime.redis.putStr fail key={}: {}", key, e.getMessage());
        }
    }

    private String readString(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("runtime.redis.getStr fail key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void deleteKey(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.debug("runtime.redis.del fail key={}: {}", key, e.getMessage());
        }
    }

    /** 静止计时起点（可 JSON 序列化） */
    @Data
    public static class StationaryState {
        private double lat;
        private double lng;
        /** yyyy-MM-dd HH:mm:ss */
        private String since;

        public static StationaryState of(double lat, double lng, LocalDateTime since, java.time.format.DateTimeFormatter fmt) {
            StationaryState state = new StationaryState();
            state.lat = lat;
            state.lng = lng;
            state.since = since.format(fmt);
            return state;
        }
    }
}
