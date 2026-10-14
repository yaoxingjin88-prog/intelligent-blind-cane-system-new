package com.ruoyi.service;

import com.ruoyi.entity.SensorData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DeviceRuntimeStore}：无 Redis 时本机回退
 */
class DeviceRuntimeStoreTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @SuppressWarnings("unchecked")
    private DeviceRuntimeStore newLocalStore() {
        ObjectProvider<StringRedisTemplate> redis = mock(ObjectProvider.class);
        when(redis.getIfAvailable()).thenReturn(null);
        return new DeviceRuntimeStore(redis);
    }

    @Test
    void latestRoundTripOnLocalFallback() {
        DeviceRuntimeStore store = newLocalStore();
        SensorData data = new SensorData();
        data.setDeviceId("DEVICE001");
        data.setLatitude(36.3);
        data.setLongitude(120.4);
        store.putLatest("DEVICE001", data);

        SensorData hit = store.getLatest("DEVICE001");
        assertEquals(36.3, hit.getLatitude());
        assertEquals(1, store.localLatestValues().size());
    }

    @Test
    void alertTypeDedupState() {
        DeviceRuntimeStore store = newLocalStore();
        assertNull(store.getLastAlertType("DEVICE001"));
        store.putLastAlertType("DEVICE001", "LOW_BATTERY");
        assertEquals("LOW_BATTERY", store.getLastAlertType("DEVICE001"));
        store.clearLastAlertType("DEVICE001");
        assertNull(store.getLastAlertType("DEVICE001"));
    }

    @Test
    void stationaryStateRoundTrip() {
        DeviceRuntimeStore store = newLocalStore();
        LocalDateTime since = LocalDateTime.of(2026, 10, 14, 12, 0, 0);
        store.putStationary("DEVICE001",
                DeviceRuntimeStore.StationaryState.of(36.3, 120.4, since, FMT));

        DeviceRuntimeStore.StationaryState hit = store.getStationary("DEVICE001");
        assertEquals(36.3, hit.getLat());
        assertEquals("2026-10-14 12:00:00", hit.getSince());
    }
}
