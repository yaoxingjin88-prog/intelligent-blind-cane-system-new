package com.ruoyi.mapper;

import com.ruoyi.entity.AlarmRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface AlarmRecordMapper {
    @Select("SELECT COUNT(*) FROM alarm_record")
    int countAll();

    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record")
    List<AlarmRecord> getAllAlarmRecords();

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