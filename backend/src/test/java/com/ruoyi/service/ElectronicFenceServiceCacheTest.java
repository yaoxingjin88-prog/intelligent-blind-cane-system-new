package com.ruoyi.service;

import com.ruoyi.entity.ElectronicFence;
import com.ruoyi.mapper.ElectronicFenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElectronicFenceServiceCacheTest {

    @Mock
    private ElectronicFenceMapper electronicFenceMapper;

    @InjectMocks
    private ElectronicFenceService electronicFenceService;

    @Test
    void getByDeviceIdUsesShortTtlCache() {
        ElectronicFence fence = new ElectronicFence();
        fence.setDeviceId("DEVICE001");
        fence.setFenceName("安全活动区");
        fence.setRadiusMeters(300.0);
        fence.setEnabled(true);
        fence.setLastStatus("INSIDE");
        fence.setCenterLatitude(36.3);
        fence.setCenterLongitude(120.3);

        when(electronicFenceMapper.getByDeviceId("DEVICE001")).thenReturn(fence);

        electronicFenceService.getByDeviceId("DEVICE001");
        electronicFenceService.getByDeviceId("DEVICE001");

        verify(electronicFenceMapper, times(1)).getByDeviceId("DEVICE001");
    }
}
