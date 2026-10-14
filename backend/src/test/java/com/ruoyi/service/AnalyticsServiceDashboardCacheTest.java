package com.ruoyi.service;

import com.ruoyi.entity.CaneDevice;
import com.ruoyi.entity.SensorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AnalyticsService} 看板缓存单测：单飞合并与 TTL 内复用
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceDashboardCacheTest {

    @Mock
    private CaneDeviceService caneDeviceService;
    @Mock
    private SensorDataService sensorDataService;
    @Mock
    private AlarmRecordService alarmRecordService;
    @Mock
    private AppCacheService appCacheService;

    @InjectMocks
    private AnalyticsService analyticsService;

    /** 共享缓存 miss；构建路径依赖打桩并人为延迟，便于观察单飞 */
    @BeforeEach
    void stubSlowBuild() {
        when(appCacheService.getMap(anyString())).thenReturn(Optional.empty());
        when(caneDeviceService.getAllDevices()).thenAnswer(invocation -> {
            Thread.sleep(80);
            return Collections.<CaneDevice>emptyList();
        });
        when(sensorDataService.getSensorDataSinceDaysLimited(anyInt(), anyInt()))
                .thenReturn(Collections.<SensorData>emptyList());
        when(sensorDataService.countAllSensorData()).thenReturn(0);
        when(sensorDataService.countRiskEventsSinceDays(anyInt())).thenReturn(0);
        when(sensorDataService.countActiveDevicesToday()).thenReturn(0);
        when(sensorDataService.getLatestSensorDataForAllDevices()).thenReturn(Collections.emptyList());
        when(alarmRecordService.countAllAlarmRecords()).thenReturn(0);
        when(alarmRecordService.countUnhandledAlarmRecords()).thenReturn(0);
        when(alarmRecordService.getUnhandledCountByDevice()).thenReturn(Collections.emptyList());
        when(alarmRecordService.getDeviceAlarmStats(anyInt())).thenReturn(Collections.emptyList());
        when(alarmRecordService.getAlarmTypeDistribution()).thenReturn(Collections.emptyList());
    }

    /** 并发请求应合并为一次 build（getAllDevices 只调 1 次） */
    @Test
    void concurrentCallsShareSingleFlightBuild() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Map<String, Object> data = analyticsService.getDashboardData();
                    if (data != null) {
                        ok.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threads, ok.get());
        verify(caneDeviceService, atMost(2)).getAllDevices();
        verify(caneDeviceService, times(1)).getAllDevices();
    }

    /** TTL 内第二次调用直接命中本机 L1，返回同一引用 */
    @Test
    void secondCallWithinTtlUsesCache() {
        Map<String, Object> first = analyticsService.getDashboardData();
        Map<String, Object> second = analyticsService.getDashboardData();
        assertSame(first, second);
        verify(caneDeviceService, times(1)).getAllDevices();
    }
}
