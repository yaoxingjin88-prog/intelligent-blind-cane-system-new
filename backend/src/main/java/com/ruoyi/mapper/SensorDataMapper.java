package com.ruoyi.mapper;

import com.ruoyi.entity.SensorData;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 传感器数据 Mapper，注解 SQL 访问 sensor_data
 */
public interface SensorDataMapper {
    /** 统计传感器数据总数 */
    @Select("SELECT COUNT(*) FROM sensor_data")
    int countAll();

    /** 查询全部传感器数据 */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime FROM sensor_data")
    List<SensorData> getAllSensorData();

    /** 按创建时间倒序查询最近数据 */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<SensorData> getRecentSensorData(@Param("limit") Integer limit);

    /**
     * 查询近 N 天数据，并限制返回条数。
     * @param days 回溯天数
     * @param limit 条数上限
     */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) ORDER BY created_at DESC LIMIT #{limit}")
    List<SensorData> getSensorDataSinceDaysLimited(@Param("days") Integer days, @Param("limit") Integer limit);

    /**
     * 统计近 N 天障碍物距离 ≤80 的风险事件数。
     * @param days 回溯天数
     */
    @Select("SELECT COUNT(*) FROM sensor_data WHERE obstacle_distance <= 80 AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countRiskEventsSinceDays(@Param("days") Integer days);

    /** 统计今日有上报的活跃设备数 */
    @Select("SELECT COUNT(DISTINCT device_id) FROM sensor_data WHERE created_at >= CURDATE()")
    int countActiveDevicesToday();

    /**
     * 查询近 N 天全部传感器数据。
     * @param days 回溯天数
     */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<SensorData> getSensorDataSinceDays(@Param("days") Integer days);

    /** 每设备取最新一条传感器数据 */
    @Select("""
            SELECT s.id, s.device_id as deviceId, s.obstacle_distance as obstacleDistance, s.is_fall as isFall,
                   s.accel_x as accelX, s.accel_y as accelY, s.accel_z as accelZ, s.fall_confidence as fallConfidence,
                   s.latitude, s.longitude, s.temperature, s.humidity, s.data_time as dataTime, s.created_at as createTime
            FROM sensor_data s
            INNER JOIN (
                SELECT device_id, MAX(id) AS max_id
                FROM sensor_data
                GROUP BY device_id
            ) latest ON s.id = latest.max_id
            """)
    List<SensorData> getLatestForAllDevices();

    /** 按主键查询 */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime FROM sensor_data WHERE id = #{id}")
    SensorData getSensorDataById(Long id);

    /** 插入传感器数据 */
    @Insert("INSERT INTO sensor_data (device_id, obstacle_distance, is_fall, accel_x, accel_y, accel_z, fall_confidence, latitude, longitude, temperature, humidity, data_time) VALUES (#{deviceId}, #{obstacleDistance}, IFNULL(#{isFall}, false), #{accelX}, #{accelY}, #{accelZ}, #{fallConfidence}, #{latitude}, #{longitude}, #{temperature}, #{humidity}, #{dataTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SensorData sensorData);

    /** 按主键删除 */
    @Delete("DELETE FROM sensor_data WHERE id = #{id}")
    void delete(Long id);

    /**
     * 按设备保留最近 N 条，清理更早记录。
     * @param keepPerDevice 每设备保留条数
     * @return 删除行数
     */
    @Delete("DELETE FROM sensor_data WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY created_at DESC, id DESC) AS rn FROM sensor_data) ranked WHERE ranked.rn > #{keepPerDevice})")
    int pruneOlderThanKeepPerDevice(@Param("keepPerDevice") Integer keepPerDevice);

    /** 查询指定设备最新一条传感器数据 */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} ORDER BY created_at DESC LIMIT 1")
    SensorData getLatestByDeviceId(String deviceId);

    /**
     * 查询近若干小时内的轨迹点（按时间升序）。
     * @param deviceId 设备号
     * @param hours 回溯小时数
     */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} AND created_at >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) ORDER BY created_at ASC")
    List<SensorData> getTrajectory(@Param("deviceId") String deviceId, @Param("hours") Integer hours);

    /**
     * 按时间区间查询轨迹点（按时间升序）。
     * @param deviceId 设备号
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    @Select("SELECT id, device_id as deviceId, obstacle_distance as obstacleDistance, is_fall as isFall, accel_x as accelX, accel_y as accelY, accel_z as accelZ, fall_confidence as fallConfidence, latitude, longitude, temperature, humidity, data_time as dataTime, created_at as createTime FROM sensor_data WHERE device_id = #{deviceId} AND created_at >= #{startTime} AND created_at <= #{endTime} ORDER BY created_at ASC")
    List<SensorData> getTrajectoryByTimeRange(@Param("deviceId") String deviceId, @Param("startTime") String startTime, @Param("endTime") String endTime);
}
