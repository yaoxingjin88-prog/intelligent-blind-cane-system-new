package com.ruoyi.mapper;

import com.ruoyi.entity.VisuallyImpairedUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 视障用户 Mapper，注解 SQL 访问 visually_impaired_user
 */
public interface VisuallyImpairedUserMapper {
    /** 查询全部用户 */
    @Select("SELECT id, username, password, name, age, gender, blood_type as bloodType, phone, id_card as idCard, address, emergency_contact as emergencyContact, emergency_phone as emergencyPhone, medical_history as medicalHistory FROM visually_impaired_user")
    List<VisuallyImpairedUser> getAllUsers();

    /** 按主键查询 */
    @Select("SELECT id, username, password, name, age, gender, blood_type as bloodType, phone, id_card as idCard, address, emergency_contact as emergencyContact, emergency_phone as emergencyPhone, medical_history as medicalHistory FROM visually_impaired_user WHERE id = #{id}")
    VisuallyImpairedUser getUserById(Long id);

    /** 插入用户 */
    @Insert("INSERT INTO visually_impaired_user (username, password, name, age, gender, blood_type, phone, id_card, address, emergency_contact, emergency_phone, medical_history) VALUES (#{username}, #{password}, #{name}, #{age}, #{gender}, #{bloodType}, #{phone}, #{idCard}, #{address}, #{emergencyContact}, #{emergencyPhone}, #{medicalHistory})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(VisuallyImpairedUser user);

    /** 按用户名查询（家庭注册占位去重） */
    @Select("SELECT id, username, password, name, age, gender, blood_type as bloodType, phone, id_card as idCard, address, emergency_contact as emergencyContact, emergency_phone as emergencyPhone, medical_history as medicalHistory FROM visually_impaired_user WHERE username = #{username} LIMIT 1")
    VisuallyImpairedUser findByUsername(String username);

    /** 按主键删除 */
    @Delete("DELETE FROM visually_impaired_user WHERE id = #{id}")
    void delete(Long id);

    /** 更新用户信息 */
    @Update("UPDATE visually_impaired_user SET username = #{username}, password = #{password}, name = #{name}, age = #{age}, gender = #{gender}, blood_type = #{bloodType}, phone = #{phone}, id_card = #{idCard}, address = #{address}, emergency_contact = #{emergencyContact}, emergency_phone = #{emergencyPhone}, medical_history = #{medicalHistory} WHERE id = #{id}")
    void update(VisuallyImpairedUser user);
}
