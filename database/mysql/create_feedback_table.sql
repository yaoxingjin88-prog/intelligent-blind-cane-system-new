-- 创建意见反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '反馈类型：suggestion-功能建议，bug-Bug反馈，usage-使用问题，other-其他',
    content TEXT NOT NULL COMMENT '反馈内容',
    contact VARCHAR(100) COMMENT '联系方式',
    images TEXT COMMENT '图片URL，多个图片用逗号分隔',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    status VARCHAR(20) DEFAULT '0' COMMENT '状态：0-待处理，1-已处理',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见反馈表';
