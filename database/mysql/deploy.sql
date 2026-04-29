-- ========================================================
-- 智能盲杖管理系统 - 数据库部署脚本
-- ========================================================
-- 使用方法:
-- 1. 创建数据库: CREATE DATABASE smart_cane CHARACTER SET utf8 COLLATE utf8mb4_general_ci;
-- 2. 导入脚本: mysql -u root -p smart_cane < deploy.sql
-- ========================================================

SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================================
-- 1. 管理员表
-- ========================================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` bigint NOT NULL COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电话',
  `created_at` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = DYNAMIC;

-- 默认管理员账号: admin / 123456
INSERT INTO `admin` VALUES (1, 'admin', '123456', '管理员', '13800138000', '2026-04-09 22:27:51', '2026-04-09 22:27:51');

-- ========================================================
-- 2. 视障用户表
-- ========================================================
DROP TABLE IF EXISTS `visually_impaired_user`;
CREATE TABLE `visually_impaired_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `blood_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '血型',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电话',
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `emergency_contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急联系电话',
  `medical_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '病史与注意事项',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视力障碍用户表' ROW_FORMAT = Dynamic;

-- 插入测试用户数据
INSERT INTO `visually_impaired_user` VALUES 
(1, 'user1', '123456', '测试用户', 62, '男', 'A型', '13900139000', '110101199001011234', '北京市海淀区', '张三', '13800138000', '高血压，外出时需语音提醒', '2026-04-11 09:46:12'),
(2, 'zhangweiming', '123456', '张明', 58, '男', 'O型', '13811223344', '110101198506154321', '北京市西城区', '李小明', '13988776655', '无', '2026-04-11 12:23:11'),
(3, 'lihua', '123456', '刘华', 55, '女', 'B型', '13788990011', '320104197809234567', '南京市玄武区', '王建国', '13877665544', '糖尿病，需要规律提醒', '2026-04-11 13:44:25'),
(4, 'wanghong', '123456', '王红', 60, '女', 'AB型', '13677889900', '420106198205123456', '武汉市江汉区', '程小丽', '13766554433', '行动较慢，需注意路口提醒', '2026-04-11 13:58:02'),
(5, 'chenglin', '123456', '程林', 50, '男', 'A型', '13566778899', '350203198511304567', '厦门市思明区', '李小明', '13988776655', '无', '2026-04-11 13:51:29');

-- ========================================================
-- 3. 监护人表
-- ========================================================
DROP TABLE IF EXISTS `guardian`;
CREATE TABLE `guardian` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '监护人ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '监护人姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '监护人电话',
  `relation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关系',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `guardian_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `visually_impaired_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '监护人表' ROW_FORMAT = DYNAMIC;

-- 插入监护人数据
INSERT INTO `guardian` VALUES 
(2, '张三', '13800138000', '亲属', 1, '2026-04-11 11:26:11', '2026-04-11 11:26:11'),
(3, '李小明', '13988776655', '子女', 2, '2026-04-11 14:11:12', '2026-04-11 14:11:12'),
(4, '王建国', '13877665544', '配偶', 3, '2026-04-11 14:11:28', '2026-04-11 14:11:28'),
(7, '程小丽', '13766554433', '子女', 4, '2026-04-11 14:11:36', '2026-04-11 14:11:36');

-- ========================================================
-- 4. 盲杖设备表
-- ========================================================
DROP TABLE IF EXISTS `cane_device`;
CREATE TABLE `cane_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一标识',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `device_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备状态',
  `battery_level` int NULL DEFAULT 100 COMMENT '电池电量',
  `last_online_time` timestamp NULL DEFAULT NULL COMMENT '最后在线时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `device_id`(`device_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `cane_device_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `visually_impaired_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '盲杖设备表' ROW_FORMAT = DYNAMIC;

-- 插入设备数据
INSERT INTO `cane_device` VALUES 
(1, 'DEVICE001', 1, '智能盲杖', '在线', 100, '2026-04-11 14:23:20', '2026-04-11 11:37:48', '2026-04-12 22:09:51'),
(2, 'DEVICE002', 2, '智能盲杖2', '在线', 89, '2026-04-11 14:31:48', '2026-04-11 14:01:31', '2026-04-12 22:09:57'),
(4, 'DEVICE003', 3, '智能盲杖3', '在线', 90, NULL, '2026-04-11 14:05:30', '2026-04-11 14:05:30');

-- ========================================================
-- 5. 传感器数据表
-- ========================================================
DROP TABLE IF EXISTS `sensor_data`;
CREATE TABLE `sensor_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据ID',
  `device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备ID',
  `obstacle_distance` double NULL DEFAULT NULL COMMENT '障碍物距离',
  `is_fall` tinyint(1) NULL DEFAULT 0 COMMENT '是否跌倒',
  `latitude` double NULL DEFAULT NULL COMMENT '纬度',
  `longitude` double NULL DEFAULT NULL COMMENT '经度',
  `temperature` double NULL DEFAULT NULL COMMENT '温度',
  `humidity` double NULL DEFAULT NULL COMMENT '湿度',
  `data_time` datetime NULL DEFAULT NULL COMMENT '数据时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '传感器数据表' ROW_FORMAT = DYNAMIC;

-- 插入传感器测试数据
INSERT INTO `sensor_data` VALUES 
(5, 'DEVICE001', 1.5, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 11:41:08', '2026-04-11 11:41:08'),
(6, 'DEVICE001', 2, 1, 39.9042, 116.4074, 26, 46, '2026-04-11 11:45:48', '2026-04-11 11:45:48'),
(7, 'DEVICE001', 2.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-11 14:16:15', '2026-04-11 14:16:15'),
(8, 'DEVICE001', 1.8, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:16:37', '2026-04-11 14:16:37'),
(9, 'DEVICE001', 2.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-11 14:19:52', '2026-04-11 14:19:52'),
(10, 'DEVICE001', 3, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:20:10', '2026-04-11 14:20:10'),
(11, 'DEVICE001', 2.2, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:20:19', '2026-04-11 14:20:19'),
(12, 'DEVICE001', 1.9, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:21:13', '2026-04-11 14:21:13'),
(13, 'DEVICE001', 3.2, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:22:59', '2026-04-11 14:22:59'),
(14, 'DEVICE001', 2.8, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:23:20', '2026-04-11 14:23:20'),
(15, 'DEVICE002', 1.8, 0, 30.5928, 114.3055, 25.8, 52, '2026-04-11 14:31:48', '2026-04-11 14:31:48'),
(23, 'DEVICE001', 2.1, 0, 39.9042, 116.4074, 26.2, 47, '2026-04-12 09:15:30', '2026-04-12 09:15:30'),
(24, 'DEVICE001', 1.7, 0, 39.9042, 116.4074, 26.5, 49, '2026-04-12 10:20:15', '2026-04-12 10:20:15'),
(25, 'DEVICE001', 2.9, 0, 39.9042, 116.4074, 25.8, 46, '2026-04-12 11:30:45', '2026-04-12 11:30:45'),
(26, 'DEVICE001', 3.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-12 14:45:20', '2026-04-12 14:45:20'),
(27, 'DEVICE002', 2.2, 0, 30.5928, 114.3055, 25.9, 53, '2026-04-12 15:15:10', '2026-04-12 15:15:10'),
(28, 'DEVICE003', 1.9, 0, 31.2304, 121.4737, 24.5, 50, '2026-04-12 16:30:25', '2026-04-12 16:30:25'),
(29, 'DEVICE001', 2.4, 0, 39.9042, 116.4074, 26.3, 47, '2026-04-13 08:45:15', '2026-04-13 08:45:15'),
(30, 'DEVICE001', 1.6, 0, 39.9042, 116.4074, 26.1, 48, '2026-04-13 09:30:15', '2026-04-13 09:30:15'),
(31, 'DEVICE002', 2, 0, 30.5928, 114.3055, 25.7, 52, '2026-04-13 10:15:30', '2026-04-13 10:15:30'),
(32, 'DEVICE003', 2.7, 0, 31.2304, 121.4737, 24.8, 51, '2026-04-13 11:20:45', '2026-04-13 11:20:45'),
(33, 'DEVICE001', 120.5, 0, 39.9042, 116.4074, 25.5, 60, '2026-04-15 20:40:18', '2026-04-15 20:40:18'),
(34, 'DEVICE001', 110, 0, 39.9038, 116.408, 25.6, 58, '2026-04-15 20:39:18', '2026-04-15 20:39:18'),
(35, 'DEVICE001', 95.5, 0, 39.9035, 116.4085, 25.8, 55, '2026-04-15 20:38:18', '2026-04-15 20:38:18'),
(36, 'DEVICE001', 80, 0, 39.903, 116.409, 26, 52, '2026-04-15 20:37:18', '2026-04-15 20:37:18'),
(37, 'DEVICE001', 65.5, 0, 39.9025, 116.4095, 26.2, 50, '2026-04-15 20:36:18', '2026-04-15 20:36:18'),
(38, 'DEVICE001', 50, 0, 39.902, 116.41, 26.5, 48, '2026-04-15 20:35:18', '2026-04-15 20:35:18'),
(39, 'DEVICE001', 45, 1, 39.9015, 116.4105, 26.8, 45, '2026-04-15 20:34:18', '2026-04-15 20:34:18');

-- ========================================================
-- 6. 告警记录表
-- ========================================================
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  `device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备ID',
  `alarm_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '告警类型',
  `alarm_time` datetime NOT NULL COMMENT '告警时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '告警状态',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警记录表' ROW_FORMAT = DYNAMIC;

-- 插入告警记录
INSERT INTO `alarm_record` VALUES 
(1, 'DEVICE001', '摔倒', '2026-04-11 11:45:48', '已处理', '2026-04-11 11:45:48'),
(2, 'DEVICE001', '障碍物', '2026-04-11 14:22:59', '已处理', '2026-04-11 14:22:59'),
(3, 'DEVICE002', '电池', '2026-04-11 14:31:48', '未处理', '2026-04-11 14:31:48'),
(4, 'DEVICE003', '摔倒', '2026-04-12 10:15:30', '已处理', '2026-04-12 10:15:30'),
(5, 'DEVICE001', '障碍物', '2026-04-12 14:45:20', '未处理', '2026-04-12 14:45:20'),
(6, 'DEVICE002', '摔倒', '2026-04-13 09:30:15', '已处理', '2026-04-13 09:30:15');

-- ========================================================
-- 7. 历史轨迹表
-- ========================================================
DROP TABLE IF EXISTS `history_track`;
CREATE TABLE `history_track` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
  `device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `latitude` double NOT NULL COMMENT '纬度',
  `longitude` double NOT NULL COMMENT '经度',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史轨迹表' ROW_FORMAT = DYNAMIC;

-- 插入轨迹数据
INSERT INTO `history_track` VALUES 
(1, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 11:41:08'),
(2, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 11:45:48'),
(3, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:16:15'),
(4, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:16:37'),
(5, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:19:52'),
(6, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:20:10'),
(7, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:20:19'),
(8, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:21:13'),
(9, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:22:59'),
(10, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:23:20'),
(11, 'DEVICE002', 2, 30.5928, 114.3055, '2026-04-11 14:31:48');

-- ========================================================
-- 8. 意见反馈表
-- ========================================================
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '反馈类型：suggestion-功能建议，bug-Bug反馈，usage-使用问题，other-其他',
    `content` TEXT NOT NULL COMMENT '反馈内容',
    `contact` VARCHAR(100) COMMENT '联系方式',
    `images` TEXT COMMENT '图片URL，多个图片用逗号分隔',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `status` VARCHAR(20) DEFAULT '0' COMMENT '状态：0-待处理，1-已处理',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见反馈表';

-- ========================================================
-- 9. 系统日志表
-- ========================================================
DROP TABLE IF EXISTS `system_log`;
CREATE TABLE `system_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统日志表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
