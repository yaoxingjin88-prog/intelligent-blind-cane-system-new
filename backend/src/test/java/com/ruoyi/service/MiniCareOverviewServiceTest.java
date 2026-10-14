package com.ruoyi.service;

import com.ruoyi.entity.SensorData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link MiniCareOverviewService} 单测：静止时长（关怀概览用）计算
 */
@ExtendWith(MockitoExtension.class)
class MiniCareOverviewServiceTest {

    @Mock
    private CaneDeviceService caneDeviceService;
    @Mock
    private com.ruoyi.mapper.SensorDataMapper sensorDataMapper;
    @Mock
    private ElectronicFenceService electronicFenceService;
    @Mock
    private AlarmRecordService alarmRecordService;

    @InjectMocks
    private MiniCareOverviewService service;

    /** 轨迹点均在静止半径内时，返回首尾时间差（分钟） */
    @Test
    void stationaryMinutesWhenPointsStayWithinRadius() {
        List<SensorData> points = new ArrayList<>();
        points.add(point("2026-08-13 08:00:00", 36.3210, 120.3970));
        points.add(point("2026-08-13 08:10:00", 36.3210, 120.3971));
        points.add(point("2026-08-13 08:25:00", 36.3211, 120.3970));

        Long minutes = service.calculateStationaryMinutes(points);
        assertEquals(25L, minutes);
    }

    /** 中间发生明显位移时，不累计静止时长（返回 null） */
    @Test
    void stationaryMinutesNullWhenMovedFar() {
        List<SensorData> points = new ArrayList<>();
        points.add(point("2026-08-13 08:00:00", 36.3210, 120.3970));
        points.add(point("2026-08-13 08:10:00", 36.3210, 120.3971));
        points.add(point("2026-08-13 08:25:00", 36.40, 120.50));

        assertNull(service.calculateStationaryMinutes(points));
    }

    /** 构造带时间与经纬度的传感点 */
    private static SensorData point(String time, double lat, double lng) {
        SensorData data = new SensorData();
        data.setDataTime(time);
        data.setLatitude(lat);
        data.setLongitude(lng);
        return data;
    }
}
