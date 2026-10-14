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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorOverviewServiceCacheTest {

    @Mock
    private CaneDeviceService caneDeviceService;
    @Mock
    private SensorDataService sensorDataService;
    @Mock
    private AlarmRecordService alarmRecordService;

    @InjectMocks
    private MonitorOverviewService monitorOverviewService;

    @BeforeEach
    void stub() {
        when(caneDeviceService.getAllDevices()).thenAnswer(invocation -> {
            Thread.sleep(50);
            return Collections.<CaneDevice>emptyList();
        });
        when(caneDeviceService.getTestingDeviceIds()).thenReturn(Collections.emptySet());
        when(sensorDataService.getLatestSensorDataForAllDevices()).thenReturn(Collections.<SensorData>emptyList());
        when(alarmRecordService.getLatestAlarmForAllDevices()).thenReturn(Collections.emptyList());
        when(alarmRecordService.getUnhandledAlarmRecords()).thenReturn(Collections.emptyList());
    }

    @Test
    void concurrentCallsShareSingleFlight() throws Exception {
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (monitorOverviewService.getOverview() != null) {
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

    @Test
    void secondCallWithinTtlUsesCache() {
        Map<String, Object> first = monitorOverviewService.getOverview();
        Map<String, Object> second = monitorOverviewService.getOverview();
        assertSame(first, second);
        verify(caneDeviceService, times(1)).getAllDevices();
    }
}
