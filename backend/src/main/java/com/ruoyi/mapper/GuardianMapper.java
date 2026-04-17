package com.ruoyi.mapper;

import com.ruoyi.entity.Guardian;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface GuardianMapper {
    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian")
    List<Guardian> getAllGuardians();

    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian WHERE id = #{id}")
    Guardian getGuardianById(Long id);

    @Insert("INSERT INTO guardian (user_id, name, phone, relation) VALUES (#{userId}, #{name}, #{phone}, #{relation})")
    void insert(Guardian guardian);

    @Delete("DELETE FROM guardian WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE guardian SET user_id = #{userId}, name = #{name}, phone = #{phone}, relation = #{relation} WHERE id = #{id}")
    void update(Guardian guardian);

    @Select("SELECT id, user_id as userId, name, phone, relation FROM guardian WHERE phone = #{phone}")
    Guardian findByPhone(String phone);
}
