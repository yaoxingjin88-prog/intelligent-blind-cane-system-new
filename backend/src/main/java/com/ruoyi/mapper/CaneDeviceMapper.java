package com.ruoyi.mapper;

import com.ruoyi.entity.CaneDevice;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 盲杖设备 Mapper，注解 SQL 访问 cane_device
 */
public interface CaneDeviceMapper {
    /** 查询全部设备（含绑定用户姓名） */
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id")
    List<CaneDevice> getAllDevices();

    /** 按主键查询 */
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id WHERE d.id = #{id}")
    CaneDevice getDeviceById(Long id);

    /** 按设备号查询 */
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id WHERE d.device_id = #{deviceId} LIMIT 1")
    CaneDevice getDeviceByDeviceId(String deviceId);

    /** 按用户取绑定的第一台设备 */
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id WHERE d.user_id = #{userId} ORDER BY d.id ASC LIMIT 1")
    CaneDevice getDeviceByUserId(Long userId);

    /** 按用户查询全部绑定设备 */
    @Select("SELECT d.id, d.device_id as deviceId, d.device_name as deviceName, d.user_id as userId, u.name as userName, d.battery_level as batteryLevel, d.status FROM cane_device d LEFT JOIN visually_impaired_user u ON d.user_id = u.id WHERE d.user_id = #{userId} ORDER BY d.id ASC")
    List<CaneDevice> getDevicesByUserId(Long userId);

    /** 插入设备 */
    @Insert("INSERT INTO cane_device (device_id, device_name, user_id, battery_level, status) VALUES (#{deviceId}, #{deviceName}, #{userId}, #{batteryLevel}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CaneDevice device);

    /** 按主键删除 */
    @Delete("DELETE FROM cane_device WHERE id = #{id}")
    void delete(Long id);

    /** 更新设备信息 */
    @Update("UPDATE cane_device SET device_id = #{deviceId}, user_id = #{userId}, battery_level = #{batteryLevel}, status = #{status} WHERE id = #{id}")
    void update(CaneDevice device);
}
