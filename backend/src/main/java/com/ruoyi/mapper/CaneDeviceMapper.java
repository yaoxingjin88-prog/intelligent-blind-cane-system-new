package com.ruoyi.mapper;

import com.ruoyi.entity.CaneDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CaneDeviceMapper {
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id")
    List<CaneDevice> getAllDevices();

    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id WHERE d.id = #{id}")
    CaneDevice getDeviceById(Long id);

    @Insert("INSERT INTO cane_device (device_id, device_name, user_id, battery_level, status) VALUES (#{deviceId}, #{deviceName}, #{userId}, #{batteryLevel}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CaneDevice device);

    @Delete("DELETE FROM cane_device WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE cane_device SET device_id = #{deviceId}, user_id = #{userId}, battery_level = #{batteryLevel}, status = #{status} WHERE id = #{id}")
    void update(CaneDevice device);
}
