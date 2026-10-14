package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.CaneDevice;
import com.ruoyi.mapper.CaneDeviceMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 盲杖设备档案：查询、增删改，以及联调测试模拟启停
 */
@Service
@RequiredArgsConstructor
public class CaneDeviceService {
    private final CaneDeviceMapper deviceMapper;
    private final DeviceTestSimulationService deviceTestSimulationService;

    /** 查询全部设备（含关联用户名） */
    public List<CaneDevice> getAllDevices() {
        return deviceMapper.getAllDevices();
    }

    /**
     * 按主键查设备
     *
     * @param id 设备表主键
     * @return 设备，不存在则 null
     */
    public CaneDevice getDeviceById(Long id) {
        return deviceMapper.getDeviceById(id);
    }

    /**
     * 按业务设备号查设备
     *
     * @param deviceId 业务 deviceId（如 DEVICE001）
     * @return 设备，不存在则 null
     */
    public CaneDevice getDeviceByDeviceId(String deviceId) {
        return deviceMapper.getDeviceByDeviceId(deviceId);
    }

    /**
     * 按视障用户 ID 取一台绑定设备（取最早一条）
     *
     * @param userId 视障用户主键
     * @return 设备，未绑定则 null
     */
    public CaneDevice getDeviceByUserId(Long userId) {
        return deviceMapper.getDeviceByUserId(userId);
    }

    /**
     * 按视障用户 ID 列出全部绑定设备
     *
     * @param userId 视障用户主键
     * @return 设备列表
     */
    public List<CaneDevice> getDevicesByUserId(Long userId) {
        return deviceMapper.getDevicesByUserId(userId);
    }

    /** 新增设备档案 */
    public void addDevice(CaneDevice device) {
        deviceMapper.insert(device);
    }

    /** 按主键删除设备 */
    public void deleteDevice(Long id) {
        deviceMapper.delete(id);
    }

    /** 更新设备档案（名称、电量、状态等） */
    public void updateDevice(CaneDevice device) {
        deviceMapper.update(device);
    }

    /**
     * 启动设备联调模拟上报
     *
     * @param id 设备主键
     * @return 是否成功启动（设备不存在或已在跑则 false）
     */
    public boolean startDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        return deviceTestSimulationService.startSimulation(device);
    }

    /**
     * 停止设备联调模拟上报
     *
     * @param id 设备主键
     * @return 是否成功停止
     */
    public boolean stopDeviceTest(Long id) {
        CaneDevice device = deviceMapper.getDeviceById(id);
        if (device == null) {
            return false;
        }
        return deviceTestSimulationService.stopSimulation(device.getDeviceId());
    }

    /**
     * 当前正在模拟上报的设备号集合
     *
     * @return 业务 deviceId 集合
     */
    public Set<String> getTestingDeviceIds() {
        return deviceTestSimulationService.getRunningDeviceIds();
    }
}
