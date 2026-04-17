package com.ruoyi.service;

import com.ruoyi.entity.Feedback;
import com.ruoyi.mapper.FeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FeedbackService {
    @Autowired
    private FeedbackMapper feedbackMapper;

    public void submitFeedback(Feedback feedback) {
        feedback.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        feedback.setStatus("0"); // 0表示待处理
        feedbackMapper.insert(feedback);
    }

    public List<Feedback> getAllFeedbacks() {
        return feedbackMapper.getAllFeedbacks();
    }

    public Feedback getFeedbackById(Long id) {
        return feedbackMapper.getFeedbackById(id);
    }

    public List<Feedback> getFeedbacksByUserId(Long userId) {
        return feedbackMapper.getFeedbacksByUserId(userId);
    }

    public void updateStatus(Long id, String status) {
        feedbackMapper.updateStatus(id, status);
    }
}
