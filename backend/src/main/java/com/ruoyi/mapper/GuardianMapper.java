package com.ruoyi.mapper;

import com.ruoyi.entity.Guardian;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 监护人 Mapper，注解 SQL 访问 guardian
 */
public interface GuardianMapper {
    /** 查询全部监护人 */
    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian")
    List<Guardian> getAllGuardians();

    /** 按主键查询 */
    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian WHERE id = #{id}")
    Guardian getGuardianById(Long id);

    /** 插入监护人 */
    @Insert("INSERT INTO guardian (user_id, name, phone, relation) VALUES (#{userId}, #{name}, #{phone}, #{relation})")
    void insert(Guardian guardian);

    /** 按主键删除 */
    @Delete("DELETE FROM guardian WHERE id = #{id}")
    void delete(Long id);

    /** 更新监护人信息 */
    @Update("UPDATE guardian SET user_id = #{userId}, name = #{name}, phone = #{phone}, relation = #{relation} WHERE id = #{id}")
    void update(Guardian guardian);

    /** 按手机号查询 */
    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian WHERE phone = #{phone}")
    Guardian findByPhone(String phone);
}
