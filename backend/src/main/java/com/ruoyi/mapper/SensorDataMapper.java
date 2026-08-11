package com.ruoyi.mapper;

import com.ruoyi.entity.SensorData;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface SensorDataMapper {
    @Select("SELECT COUNT(*) FROM sensor_data")
    int countAll();

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime FROM sensor_data")
    List<SensorData> getAllSensorData();

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<SensorData> getRecentSensorData(@Param("limit") Integer limit);

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) ORDER BY created_at DESC LIMIT #{limit}")
    List<SensorData> getSensorDataSinceDaysLimited(@Param("days") Integer days, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM sensor_data WHERE obstacle_distance <= 80 AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countRiskEventsSinceDays(@Param("days") Integer days);

    @Select("SELECT COUNT(DISTINCT device_id) FROM sensor_data WHERE DATE(created_at) = CURDATE()")
    int countActiveDevicesToday();

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<SensorData> getSensorDataSinceDays(@Param("days") Integer days);

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM (SELECT id, device_id, obstacle_distance, is_fall, accel_x, accel_y, accel_z, fall_confidence, latitude, longitude, temperature, humidity, data_time, created_at, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY created_at DESC, id DESC) AS rn FROM sensor_data) ranked WHERE ranked.rn = 1")
    List<SensorData> getLatestForAllDevices();

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime FROM sensor_data WHERE id = #{id}")
    SensorData getSensorDataById(Long id);

    @Insert("INSERT INTO sensor_data (device_id, obstacle_distance, is_fall, accel_x, accel_y, accel_z, fall_confidence, latitude, longitude, temperature, humidity, data_time) VALUES (#{deviceId}, #{obstacleDistance}, IFNULL(#{isFall}, false), #{accelX}, #{accelY}, #{accelZ}, #{fallConfidence}, #{latitude}, #{longitude}, #{temperature}, #{humidity}, #{dataTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SensorData sensorData);

    @Delete("DELETE FROM sensor_data WHERE id = #{id}")
    void delete(Long id);

    @Delete("DELETE FROM sensor_data WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY created_at DESC, id DESC) AS rn FROM sensor_data) ranked WHERE ranked.rn > #{keepPerDevice})")
    int pruneOlderThanKeepPerDevice(@Param("keepPerDevice") Integer keepPerDevice);

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} ORDER BY created_at DESC LIMIT 1")
    SensorData getLatestByDeviceId(String deviceId);

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) ORDER BY created_at ASC")
    List<SensorData> getTrajectory(@Param("deviceId") String deviceId, @Param("hours") Integer hours);

    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} AND created_at >= #{startTime} AND created_at <= #{endTime} ORDER BY created_at ASC")
    List<SensorData> getTrajectoryByTimeRange(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime);
}
