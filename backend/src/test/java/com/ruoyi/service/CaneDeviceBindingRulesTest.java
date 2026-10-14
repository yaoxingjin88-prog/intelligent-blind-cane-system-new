package com.ruoyi.service;

import com.ruoyi.entity.CaneDevice;
import com.ruoyi.mapper.CaneDeviceMapper;
import com.ruoyi.mapper.SensorDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 一人一台、专用 bind/unbind、普通更新不改归属与运行态
 */
@ExtendWith(MockitoExtension.class)
class CaneDeviceBindingRulesTest {

    @Mock
    private CaneDeviceMapper deviceMapper;
    @Mock
    private SensorDataMapper sensorDataMapper;
    @Mock
    private DeviceTestSimulationService deviceTestSimulationService;

    @InjectMocks
    private CaneDeviceService caneDeviceService;

    @Test
    void rejectWhenUserAlreadyHasDeviceOnAdd() {
        CaneDevice device = new CaneDevice();
        device.setDeviceId("NEW_001");
        device.setUserId(1L);
        when(deviceMapper.getDeviceByDeviceId("NEW_001")).thenReturn(null);
        when(deviceMapper.countByUserId(1L)).thenReturn(1);

        assertThrows(IllegalArgumentException.class, () -> caneDeviceService.addDevice(device));
        verify(deviceMapper, never()).insert(any());
    }

    @Test
    void updateDeviceKeepsOwnershipAndRuntimeFields() {
        CaneDevice existing = new CaneDevice();
        existing.setId(9L);
        existing.setDeviceId("DEV_A");
        existing.setUserId(1L);
        existing.setDeviceName("盲杖");
        existing.setBatteryLevel(55);
        existing.setStatus("在线");

        CaneDevice update = new CaneDevice();
        update.setId(9L);
        update.setDeviceId("DEV_A");
        update.setDeviceName("新名称");
        update.setUserId(2L);
        update.setBatteryLevel(80);
        update.setStatus("离线");

        when(deviceMapper.getDeviceById(9L)).thenReturn(existing);
        when(deviceMapper.getDeviceByDeviceId("DEV_A")).thenReturn(existing);

        caneDeviceService.updateDevice(update);

        ArgumentCaptor<CaneDevice> captor = ArgumentCaptor.forClass(CaneDevice.class);
        verify(deviceMapper).update(captor.capture());
        CaneDevice saved = captor.getValue();
        assertEquals("新名称", saved.getDeviceName());
        assertEquals(55, saved.getBatteryLevel());
        assertEquals("在线", saved.getStatus());
        verify(deviceMapper, never()).updateUserId(any(), any());
    }

    @Test
    void rejectBindOccupiedDevice() {
        CaneDevice existing = new CaneDevice();
        existing.setId(9L);
        existing.setUserId(1L);
        when(deviceMapper.getDeviceById(9L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> caneDeviceService.bindDeviceToUser(9L, 2L));
        verify(deviceMapper, never()).updateUserId(any(), any());
    }

    @Test
    void assignUserDeviceReplacesBinding() {
        CaneDevice old = new CaneDevice();
        old.setId(1L);
        old.setUserId(10L);
        CaneDevice target = new CaneDevice();
        target.setId(2L);
        target.setUserId(null);

        when(deviceMapper.getDevicesByUserId(10L)).thenReturn(List.of(old));
        when(deviceMapper.getDeviceById(2L)).thenReturn(target);

        caneDeviceService.assignUserDevice(10L, 2L);

        verify(deviceMapper).updateUserId(1L, null);
        verify(deviceMapper).updateUserId(2L, 10L);
    }

    @Test
    void allowBindUnboundDeviceToFreeUser() {
        CaneDevice device = new CaneDevice();
        device.setDeviceId("FREE_001");
        device.setUserId(3L);
        when(deviceMapper.getDeviceByDeviceId("FREE_001")).thenReturn(null);
        when(deviceMapper.countByUserId(3L)).thenReturn(0);

        caneDeviceService.addDevice(device);
        verify(deviceMapper).insert(device);
    }
}
