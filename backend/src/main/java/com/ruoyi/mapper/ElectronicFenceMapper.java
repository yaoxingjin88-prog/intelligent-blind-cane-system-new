package com.ruoyi.mapper;

import com.ruoyi.entity.ElectronicFence;
import org.apache.ibatis.annotations.*;

 import java.util.List;

@Mapper
public interface ElectronicFenceMapper {
    @Select("SELECT id, device_id as deviceId, fence_name as fenceName, center_latitude as centerLatitude, center_longitude as centerLongitude, radius_meters as radiusMeters, enabled, last_status as lastStatus, created_at as createdAt, updated_at as updatedAt FROM electronic_fence ORDER BY updated_at DESC, id DESC")
    List<ElectronicFence> getAll();

    @Select("SELECT id, device_id as deviceId, fence_name as fenceName, center_latitude as centerLatitude, center_longitude as centerLongitude, radius_meters as radiusMeters, enabled, last_status as lastStatus, created_at as createdAt, updated_at as updatedAt FROM electronic_fence WHERE device_id = #{deviceId} LIMIT 1")
    ElectronicFence getByDeviceId(String deviceId);

    @Insert("INSERT INTO electronic_fence (device_id, fence_name, center_latitude, center_longitude, radius_meters, enabled, last_status) VALUES (#{deviceId}, #{fenceName}, #{centerLatitude}, #{centerLongitude}, #{radiusMeters}, #{enabled}, #{lastStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ElectronicFence fence);

    @Update("UPDATE electronic_fence SET fence_name = #{fenceName}, center_latitude = #{centerLatitude}, center_longitude = #{centerLongitude}, radius_meters = #{radiusMeters}, enabled = #{enabled}, last_status = #{lastStatus}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void update(ElectronicFence fence);

    @Update("UPDATE electronic_fence SET last_status = #{lastStatus}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void updateLastStatus(ElectronicFence fence);

    @Select("SELECT id, device_id as deviceId, fence_name as fenceName, center_latitude as centerLatitude, center_longitude as centerLongitude, radius_meters as radiusMeters, enabled, last_status as lastStatus, created_at as createdAt, updated_at as updatedAt FROM electronic_fence WHERE id = #{id}")
    ElectronicFence getById(Long id);

    @Select("SELECT id, device_id as deviceId, fence_name as fenceName, center_latitude as centerLatitude, center_longitude as centerLongitude, radius_meters as radiusMeters, enabled, last_status as lastStatus, created_at as createdAt, updated_at as updatedAt FROM electronic_fence WHERE device_id = #{deviceId} ORDER BY updated_at DESC")
    List<ElectronicFence> getListByDeviceId(String deviceId);

    @Delete("DELETE FROM electronic_fence WHERE id = #{id}")
    void deleteById(Long id);
}
