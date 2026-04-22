package com.ruoyi.mapper;

import com.ruoyi.entity.VisuallyImpairedUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface VisuallyImpairedUserMapper {
    @Select("SELECT id, username, password, name, age, gender, blood_type as bloodType, phone, id_card as idCard, address, emergency_contact as emergencyContact, emergency_phone as emergencyPhone, medical_history as medicalHistory FROM visually_impaired_user")
    List<VisuallyImpairedUser> getAllUsers();

    @Select("SELECT id, username, password, name, age, gender, blood_type as bloodType, phone, id_card as idCard, address, emergency_contact as emergencyContact, emergency_phone as emergencyPhone, medical_history as medicalHistory FROM visually_impaired_user WHERE id = #{id}")
    VisuallyImpairedUser getUserById(Long id);

    @Insert("INSERT INTO visually_impaired_user (username, password, name, age, gender, blood_type, phone, id_card, address, emergency_contact, emergency_phone, medical_history) VALUES (#{username}, #{password}, #{name}, #{age}, #{gender}, #{bloodType}, #{phone}, #{idCard}, #{address}, #{emergencyContact}, #{emergencyPhone}, #{medicalHistory})")
    void insert(VisuallyImpairedUser user);

    @Delete("DELETE FROM visually_impaired_user WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE visually_impaired_user SET username = #{username}, password = #{password}, name = #{name}, age = #{age}, gender = #{gender}, blood_type = #{bloodType}, phone = #{phone}, id_card = #{idCard}, address = #{address}, emergency_contact = #{emergencyContact}, emergency_phone = #{emergencyPhone}, medical_history = #{medicalHistory} WHERE id = #{id}")
    void update(VisuallyImpairedUser user);
}
