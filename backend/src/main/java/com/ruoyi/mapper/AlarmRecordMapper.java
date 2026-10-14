package com.ruoyi.mapper;

import com.ruoyi.entity.AlarmRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 报警记录 Mapper，注解 SQL 访问 alarm_record
 */
public interface AlarmRecordMapper {
    /** 统计报警总数 */
    @Select("SELECT COUNT(*) FROM alarm_record")
    int countAll();

    /** 统计未处理报警数（status 为 0 或「未处理」） */
    @Select("SELECT COUNT(*) FROM alarm_record WHERE status = '0' OR status = '未处理'")
    int countUnhandled();

    /** 按设备汇总未处理报警数 */
    @Select("SELECT device_id as deviceId, COUNT(*) as unhandledCount FROM alarm_record WHERE status = '0' OR status = '未处理' GROUP BY device_id")
    List<Map<String, Object>> getUnhandledCountByDevice();

    /**
     * 按设备汇总报警数/未处理数/最近报警时间，按报警量降序取前 N。
     * @param limit 返回条数上限
     */
    @Select("SELECT device_id as deviceId, COUNT(*) as alarmCount, SUM(CASE WHEN status = '0' OR status = '未处理' THEN 1 ELSE 0 END) as unhandledCount, MAX(alarm_time) as latestAlarmTime FROM alarm_record GROUP BY device_id ORDER BY alarmCount DESC LIMIT #{limit}")
    List<Map<String, Object>> getDeviceAlarmStats(@Param("limit") int limit);

    /** 按报警类型分组统计分布 */
    @Select("SELECT COALESCE(NULLIF(TRIM(alarm_type), ''), '其他报警') as name, COUNT(*) as value FROM alarm_record GROUP BY COALESCE(NULLIF(TRIM(alarm_type), ''), '其他报警') ORDER BY value DESC")
    List<Map<String, Object>> getAlarmTypeDistribution();

    /** 按时间倒序查询报警列表 */
    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record ORDER BY alarm_time DESC, id DESC LIMIT #{limit}")
    List<AlarmRecord> getAllAlarmRecords(@Param("limit") Integer limit);

    /** 按时间倒序查询未处理报警 */
    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE status = '0' OR status = '未处理' ORDER BY alarm_time DESC, id DESC LIMIT #{limit}")
    List<AlarmRecord> getUnhandledAlarmRecords(@Param("limit") Integer limit);

    /** 查询最近报警记录 */
    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record ORDER BY alarm_time DESC, id DESC LIMIT #{limit}")
    List<AlarmRecord> getRecentAlarmRecords(@Param("limit") Integer limit);

    /** 每设备取最新一条报警 */
    @Select("""
            SELECT a.id, a.device_id as deviceId, a.alarm_type as alarmType, a.alarm_time as alarmTime, a.status
            FROM alarm_record a
            INNER JOIN (
                SELECT device_id, MAX(id) AS max_id
                FROM alarm_record
                GROUP BY device_id
            ) latest ON a.id = latest.max_id
            """)
    List<AlarmRecord> getLatestForAllDevices();

    /** 按主键查询 */
    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE id = #{id}")
    AlarmRecord getAlarmRecordById(Long id);

    /** 插入报警记录 */
    @Insert("INSERT INTO alarm_record (device_id, alarm_type, status, alarm_time) VALUES (#{deviceId}, #{alarmType}, #{status}, #{alarmTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AlarmRecord alarmRecord);

    /** 按主键删除 */
    @Delete("DELETE FROM alarm_record WHERE id = #{id}")
    void delete(Long id);

    /**
     * 按设备保留最近 N 条，清理更早记录。
     * @param keepPerDevice 每设备保留条数
     * @return 删除行数
     */
    @Delete("DELETE FROM alarm_record WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY alarm_time DESC, id DESC) AS rn FROM alarm_record) ranked WHERE ranked.rn > #{keepPerDevice})")
    int pruneOlderThanKeepPerDevice(@Param("keepPerDevice") Integer keepPerDevice);

    /** 更新报警处理状态 */
    @Update("UPDATE alarm_record SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 查询指定设备最新一条报警 */
    @Select("SELECT id, device_id as deviceId, alarm_type as alarmType, alarm_time as alarmTime, status FROM alarm_record WHERE device_id = #{deviceId} ORDER BY alarm_time DESC LIMIT 1")
    AlarmRecord getLatestByDeviceId(String deviceId);

    /**
     * 小程序报警统计。deviceId 为空时统计全部。
     * alarm_time 为字符串字段，按 yyyy-MM-dd 前缀过滤今日/本周。
     * @param deviceId 可选设备号；空则全量
     * @return total/pending/handled/today/thisWeek 及 fall/outOfBounds/lowBattery/sos 等分项
     */
    @Select("""
            SELECT
              COUNT(*) AS total,
              COALESCE(SUM(CASE WHEN status = '0' OR status = '未处理' THEN 1 ELSE 0 END), 0) AS pending,
              COALESCE(SUM(CASE WHEN status <> '0' AND status <> '未处理' THEN 1 ELSE 0 END), 0) AS handled,
              COALESCE(SUM(CASE WHEN alarm_time >= DATE_FORMAT(CURDATE(), '%Y-%m-%d') THEN 1 ELSE 0 END), 0) AS today,
              COALESCE(SUM(CASE WHEN alarm_time >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), '%Y-%m-%d') THEN 1 ELSE 0 END), 0) AS thisWeek,
              COALESCE(SUM(CASE WHEN alarm_type LIKE '%摔%' OR alarm_type LIKE '%跌%' OR LOWER(alarm_type) LIKE '%fall%' THEN 1 ELSE 0 END), 0) AS fall,
              COALESCE(SUM(CASE WHEN alarm_type LIKE '%围栏%' OR alarm_type LIKE '%越界%' THEN 1 ELSE 0 END), 0) AS outOfBounds,
              COALESCE(SUM(CASE WHEN alarm_type LIKE '%电%' OR LOWER(alarm_type) LIKE '%battery%' THEN 1 ELSE 0 END), 0) AS lowBattery,
              COALESCE(SUM(CASE WHEN LOWER(alarm_type) LIKE '%sos%' THEN 1 ELSE 0 END), 0) AS sos
            FROM alarm_record
            WHERE (#{deviceId} IS NULL OR #{deviceId} = '' OR device_id = #{deviceId})
            """)
    Map<String, Object> getMiniStatistics(@Param("deviceId") String deviceId);
}
