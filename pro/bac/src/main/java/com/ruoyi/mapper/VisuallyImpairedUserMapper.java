package com.ruoyi.mapper;

import com.ruoyi.entity.VisuallyImpairedUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface VisuallyImpairedUserMapper {
    @Select("SELECT id, username, password, name, phone, id_card as idCard, address FROM visually_impaired_user")
    List<VisuallyImpairedUser> getAllUsers();

    @Select("SELECT id, username, password, name, phone, id_card as idCard, address FROM visually_impaired_user WHERE id = #{id}")
    VisuallyImpairedUser getUserById(Long id);

    @Insert("INSERT INTO visually_impaired_user (username, password, name, phone, id_card, address) VALUES (#{username}, #{password}, #{name}, #{phone}, #{idCard}, #{address})")
    void insert(VisuallyImpairedUser user);

    @Delete("DELETE FROM visually_impaired_user WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE visually_impaired_user SET username = #{username}, password = #{password}, name = #{name}, phone = #{phone}, id_card = #{idCard}, address = #{address} WHERE id = #{id}")
    void update(VisuallyImpairedUser user);
}
