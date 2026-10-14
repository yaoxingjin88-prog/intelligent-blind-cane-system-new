package com.ruoyi.mapper;

import com.ruoyi.entity.Guardian;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 监护人 Mapper，注解 SQL 访问 guardian
 */
public interface GuardianMapper {
    /** 查询全部监护人 */
    @Select("SELECT id, user_id as userId, name, phone, relation, role FROM guardian")
    List<Guardian> getAllGuardians();

    /** 按主键查询 */
    @Select("SELECT id, user_id as userId, name, phone, relation, role FROM guardian WHERE id = #{id}")
    Guardian getGuardianById(Long id);

    /** 按持杖用户查询家庭监护人（主监护人优先） */
    @Select("SELECT id, user_id as userId, name, phone, relation, role FROM guardian WHERE user_id = #{userId} ORDER BY CASE WHEN role = 'PRIMARY' THEN 0 ELSE 1 END, id ASC")
    List<Guardian> getGuardiansByUserId(Long userId);

    /** 插入监护人 */
    @Insert("INSERT INTO guardian (user_id, name, phone, relation, role) VALUES (#{userId}, #{name}, #{phone}, #{relation}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Guardian guardian);

    /** 按主键删除 */
    @Delete("DELETE FROM guardian WHERE id = #{id}")
    void delete(Long id);

    /** 更新监护人信息 */
    @Update("UPDATE guardian SET user_id = #{userId}, name = #{name}, phone = #{phone}, relation = #{relation}, role = #{role} WHERE id = #{id}")
    void update(Guardian guardian);

    /** 按手机号查询 */
    @Select("SELECT id, user_id as userId, name, phone, relation, role FROM guardian WHERE phone = #{phone}")
    Guardian findByPhone(String phone);

    /** 统计某持杖用户下主监护人数量 */
    @Select("SELECT COUNT(1) FROM guardian WHERE user_id = #{userId} AND role = 'PRIMARY'")
    int countPrimaryByUserId(Long userId);
}
