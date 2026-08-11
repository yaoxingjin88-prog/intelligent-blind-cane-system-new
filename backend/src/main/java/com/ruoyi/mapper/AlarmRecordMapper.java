package com.ruoyi.mapper;

import com.ruoyi.entity.AlarmRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface AlarmRecordMapper {
    @Select("SELECT COUNT(*) FROM alarm_record")
    int countAll();

    @Select("SELECT COUNT(*) FROM alarm_record WHERE status = '0' OR status = '未处理'")
    int countUnhandled();

    @Select("SELECT device_id as deviceId, COUNT(*) as unhandledCount FROM alarm_record WHERE status = '0' OR status = '未处理' GROUP BY device_id")
    List<Map<String, Object>> getUnhandledCountByDevice();

    @Select("SELECT device_id as deviceId, COUNT(*) as alarmCount, SUM(CASE WHEN status = '0' OR status = '未处理' THEN 1 ELSE 0 END) as unhandledCount, MAX(alarm_time) as latestAlarmTime FROM alarm_record GROUP BY device_id ORDER BY alarmCount DESC LIMIT #{limit}")
    List<Map<String, Object>> getDeviceAlarmStats(@Param("limit") int limit);

    @Select("SELECT COALESCE(NULLIF(TRIM(alarm_type), ''), '其他报警') as name, COUNT(*) as value FROM alarm_record GROUP BY COALESCE(NULLIF(TRIM(alarm_type), ''), '其他报警') ORDER BY value DESC")
    List<Map<String, Object>> getAlarmTypeDistribution();

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record")
    List<AlarmRecord> getAllAlarmRecords();

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE status = '0' OR status = '未处理' ORDER BY alarm_time DESC")
    List<AlarmRecord> getUnhandledAlarmRecords();

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record ORDER BY alarm_time DESC, id DESC LIMIT #{limit}")
    List<AlarmRecord> getRecentAlarmRecords(@Param("limit") Integer limit);

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM (SELECT id, device_id, alarm_type, alarm_time, status, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY alarm_time DESC, id DESC) AS rn FROM alarm_record) ranked WHERE ranked.rn = 1")
    List<AlarmRecord> getLatestForAllDevices();

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE id = #{id}")
    AlarmRecord getAlarmRecordById(Long id);

    @Insert("INSERT INTO alarm_record (device_id, alarm_type, status, alarm_time) VALUES (#{deviceId}, #{alarmType}, #{status}, #{alarmTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AlarmRecord alarmRecord);

    @Delete("DELETE FROM alarm_record WHERE id = #{id}")
    void delete(Long id);

    @Delete("DELETE FROM alarm_record WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY alarm_time DESC, id DESC) AS rn FROM alarm_record) ranked WHERE ranked.rn > #{keepPerDevice})")
    int pruneOlderThanKeepPerDevice(@Param("keepPerDevice") Integer keepPerDevice);

    @Update("UPDATE alarm_record SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE device_id = #{deviceId} ORDER BY alarm_time DESC LIMIT 1")
    AlarmRecord getLatestByDeviceId(String deviceId);
}