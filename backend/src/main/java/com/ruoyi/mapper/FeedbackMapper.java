package com.ruoyi.mapper;

import com.ruoyi.entity.Feedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户反馈 Mapper，注解 SQL 访问 feedback
 */
@Mapper
public interface FeedbackMapper {

    /** 插入反馈 */
    @Insert("INSERT INTO feedback (user_id, type, content, contact, images, create_time, status) VALUES (#{userId}, #{type}, #{content}, #{contact}, #{images}, #{createTime}, #{status})")
    void insert(Feedback feedback);

    /** 查询全部反馈（按创建时间倒序） */
    @Select("SELECT * FROM feedback ORDER BY create_time DESC")
    List<Feedback> getAllFeedbacks();

    /** 按主键查询 */
    @Select("SELECT * FROM feedback WHERE id = #{id}")
    Feedback getFeedbackById(Long id);

    /** 按用户查询反馈列表 */
    @Select("SELECT * FROM feedback WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Feedback> getFeedbacksByUserId(Long userId);

    /** 更新反馈处理状态 */
    @Update("UPDATE feedback SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, String status);
}
