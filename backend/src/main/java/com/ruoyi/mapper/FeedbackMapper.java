package com.ruoyi.mapper;

import com.ruoyi.entity.Feedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FeedbackMapper {

    @Insert("INSERT INTO feedback (user_id, type, content, contact, images, create_time, status) VALUES (#{userId}, #{type}, #{content}, #{contact}, #{images}, #{createTime}, #{status})")
    void insert(Feedback feedback);

    @Select("SELECT * FROM feedback ORDER BY create_time DESC")
    List<Feedback> getAllFeedbacks();

    @Select("SELECT * FROM feedback WHERE id = #{id}")
    Feedback getFeedbackById(Long id);

    @Select("SELECT * FROM feedback WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Feedback> getFeedbacksByUserId(Long userId);

    @Update("UPDATE feedback SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, String status);
}
