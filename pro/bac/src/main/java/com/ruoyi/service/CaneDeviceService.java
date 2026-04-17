package com.ruoyi.service;

import com.ruoyi.entity.CaneDevice;
import com.ruoyi.mapper.CaneDeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CaneDeviceService {
    @Autowired
    private CaneDeviceMapper deviceMapper;
    @Autowired
    private DeviceTestSimulationService deviceTestSimulationService;

    public List<CaneDevice> getAllDevices() {
        return deviceMapper.getAllDevices();
    }

    public CaneDevice getDeviceById(Long id) {
        return deviceMapper.getDeviceById(id);
    }

    public void addDevice(CaneDevice device) {
        deviceMapper.insert(device);
    }

    public void deleteDevice(Long id) {
        deviceMapper.delete(id);
    }

    public void updateDevice(CaneDevice device) {
        deviceMapper.update(device);
    }

    public boolean startDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        return deviceTestSimulationService.startSimulation(device);
    }

    public boolean stopDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        if (device == null) {
            return false;
        }
        return deviceTestSimulationService.stopSimulation(device.getDeviceId());
    }

    public Set<String> getTestingDeviceIds() {
        return deviceTestSimulationService.getRunningDeviceIds();
    }
}
