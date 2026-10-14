package com.ruoyi.service;

import lombok.RequiredArgsConstructor;
import com.ruoyi.entity.Feedback;
import com.ruoyi.mapper.FeedbackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户反馈：提交入库、查询与处理状态更新
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackMapper feedbackMapper;

    /**
     * 提交反馈：补齐创建时间，初始状态为待处理
     *
     * @param feedback 反馈内容（类型、正文、联系方式等）
     */
    public void submitFeedback(Feedback feedback) {
        feedback.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        feedback.setStatus("0"); // 0表示待处理
        feedbackMapper.insert(feedback);
    }

    /** 查询全部反馈（按时间倒序） */
    public List<Feedback> getAllFeedbacks() {
        return feedbackMapper.getAllFeedbacks();
    }

    /**
     * 按主键查反馈
     *
     * @param id 反馈主键
     * @return 反馈，不存在则 null
     */
    public Feedback getFeedbackById(Long id) {
        return feedbackMapper.getFeedbackById(id);
    }

    /**
     * 按用户查其反馈列表
     *
     * @param userId 用户主键
     * @return 反馈列表
     */
    public List<Feedback> getFeedbacksByUserId(Long userId) {
        return feedbackMapper.getFeedbacksByUserId(userId);
    }

    /**
     * 更新反馈处理状态
     *
     * @param id     反馈主键
     * @param status 状态码（如 0 待处理 / 1 已处理）
     */
    public void updateStatus(Long id, String status) {
        feedbackMapper.updateStatus(id, status);
    }
}
