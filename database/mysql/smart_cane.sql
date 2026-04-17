/*
 Navicat Premium Dump SQL

 Source Server         : gha2507
 Source Server Type    : MySQL
 Source Server Version : 80027 (8.0.27)
 Source Host           : localhost:3306
 Source Schema         : smart_cane

 Target Server Type    : MySQL
 Target Server Version : 80027 (8.0.27)
 File Encoding         : 65001

 Date: 15/04/2026 20:46:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
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

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', '123456', '管理员', '13800138000', '2026-04-09 22:27:51', '2026-04-09 22:27:51');

-- ----------------------------
-- Table structure for alarm_record
-- ----------------------------
DROP TABLE IF EXISTS `alarm_record`;
CREATE TABLE `alarm_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  `device_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备ID',
  `alarm_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '告警类型',
  `alarm_time` datetime NOT NULL COMMENT '告警时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '告警状态',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of alarm_record
-- ----------------------------
INSERT INTO `alarm_record` VALUES (1, 'DEVICE001', '摔倒', '2026-04-11 11:45:48', '已处理', '2026-04-11 11:45:48');
INSERT INTO `alarm_record` VALUES (2, 'DEVICE001', '障碍物', '2026-04-11 14:22:59', '已处理', '2026-04-11 14:22:59');
INSERT INTO `alarm_record` VALUES (3, 'DEVICE002', '电池', '2026-04-11 14:31:48', '未处理', '2026-04-11 14:31:48');
INSERT INTO `alarm_record` VALUES (4, 'DEVICE003', '摔倒', '2026-04-12 10:15:30', '已处理', '2026-04-12 10:15:30');
INSERT INTO `alarm_record` VALUES (5, 'DEVICE001', '障碍物', '2026-04-12 14:45:20', '未处理', '2026-04-12 14:45:20');
INSERT INTO `alarm_record` VALUES (6, 'DEVICE002', '摔倒', '2026-04-13 09:30:15', '已处理', '2026-04-13 09:30:15');

-- ----------------------------
-- Table structure for cane_device
-- ----------------------------
DROP TABLE IF EXISTS `cane_device`;
CREATE TABLE `cane_device`  (
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

-- ----------------------------
-- Records of cane_device
-- ----------------------------
INSERT INTO `cane_device` VALUES (1, 'DEVICE001', 1, '智能盲杖', '在线', 100, '2026-04-11 14:23:20', '2026-04-11 11:37:48', '2026-04-12 22:09:51');
INSERT INTO `cane_device` VALUES (2, 'DEVICE002', 2, '智能盲杖2', '在线', 89, '2026-04-11 14:31:48', '2026-04-11 14:01:31', '2026-04-12 22:09:57');
INSERT INTO `cane_device` VALUES (4, 'DEVICE003', 3, '智能盲杖3', '在线', 90, NULL, '2026-04-11 14:05:30', '2026-04-11 14:05:30');
INSERT INTO `cane_device` VALUES (10, 'DEVICE004', 1, NULL, '??', 85, NULL, '2026-04-12 22:19:13', '2026-04-12 22:19:13');
INSERT INTO `cane_device` VALUES (14, '5', 5, NULL, '在线', 5, NULL, '2026-04-13 00:04:54', '2026-04-13 00:04:54');
INSERT INTO `cane_device` VALUES (15, '1', 1, NULL, '在线', 100, NULL, '2026-04-13 00:28:59', '2026-04-13 00:28:59');

-- ----------------------------
-- Table structure for guardian
-- ----------------------------
DROP TABLE IF EXISTS `guardian`;
CREATE TABLE `guardian`  (
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

-- ----------------------------
-- Records of guardian
-- ----------------------------
INSERT INTO `guardian` VALUES (2, '张三', '13800138000', '亲属', 1, '2026-04-11 11:26:11', '2026-04-11 11:26:11');
INSERT INTO `guardian` VALUES (3, '李小明', '13988776655', '子女', 2, '2026-04-11 14:11:12', '2026-04-11 14:11:12');
INSERT INTO `guardian` VALUES (4, '王建国', '13877665544', '配偶', 3, '2026-04-11 14:11:28', '2026-04-11 14:11:28');
INSERT INTO `guardian` VALUES (7, '程小丽', '13766554433', '子女', 4, '2026-04-11 14:11:36', '2026-04-11 14:11:36');

-- ----------------------------
-- Table structure for history_track
-- ----------------------------
DROP TABLE IF EXISTS `history_track`;
CREATE TABLE `history_track`  (
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

-- ----------------------------
-- Records of history_track
-- ----------------------------
INSERT INTO `history_track` VALUES (1, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 11:41:08');
INSERT INTO `history_track` VALUES (2, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 11:45:48');
INSERT INTO `history_track` VALUES (3, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:16:15');
INSERT INTO `history_track` VALUES (4, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:16:37');
INSERT INTO `history_track` VALUES (5, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:19:52');
INSERT INTO `history_track` VALUES (6, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:20:10');
INSERT INTO `history_track` VALUES (7, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:20:19');
INSERT INTO `history_track` VALUES (8, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:21:13');
INSERT INTO `history_track` VALUES (9, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:22:59');
INSERT INTO `history_track` VALUES (10, 'DEVICE001', 1, 39.9042, 116.4074, '2026-04-11 14:23:20');
INSERT INTO `history_track` VALUES (11, 'DEVICE002', 2, 30.5928, 114.3055, '2026-04-11 14:31:48');

-- ----------------------------
-- Table structure for sensor_data
-- ----------------------------
DROP TABLE IF EXISTS `sensor_data`;
CREATE TABLE `sensor_data`  (
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

-- ----------------------------
-- Records of sensor_data
-- ----------------------------
INSERT INTO `sensor_data` VALUES (5, 'DEVICE001', 1.5, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 11:41:08', '2026-04-11 11:41:08');
INSERT INTO `sensor_data` VALUES (6, 'DEVICE001', 2, 1, 39.9042, 116.4074, 26, 46, '2026-04-11 11:45:48', '2026-04-11 11:45:48');
INSERT INTO `sensor_data` VALUES (7, 'DEVICE001', 2.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-11 14:16:15', '2026-04-11 14:16:15');
INSERT INTO `sensor_data` VALUES (8, 'DEVICE001', 1.8, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:16:37', '2026-04-11 14:16:37');
INSERT INTO `sensor_data` VALUES (9, 'DEVICE001', 2.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-11 14:19:52', '2026-04-11 14:19:52');
INSERT INTO `sensor_data` VALUES (10, 'DEVICE001', 3, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:20:10', '2026-04-11 14:20:10');
INSERT INTO `sensor_data` VALUES (11, 'DEVICE001', 2.2, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:20:19', '2026-04-11 14:20:19');
INSERT INTO `sensor_data` VALUES (12, 'DEVICE001', 1.9, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:21:13', '2026-04-11 14:21:13');
INSERT INTO `sensor_data` VALUES (13, 'DEVICE001', 3.2, 0, 39.9042, 116.4074, 27, 50, '2026-04-11 14:22:59', '2026-04-11 14:22:59');
INSERT INTO `sensor_data` VALUES (14, 'DEVICE001', 2.8, 0, 39.9042, 116.4074, 25.5, 45, '2026-04-11 14:23:20', '2026-04-11 14:23:20');
INSERT INTO `sensor_data` VALUES (15, 'DEVICE002', 1.8, 0, 30.5928, 114.3055, 25.8, 52, '2026-04-11 14:31:48', '2026-04-11 14:31:48');
INSERT INTO `sensor_data` VALUES (23, 'DEVICE001', 2.1, 0, 39.9042, 116.4074, 26.2, 47, '2026-04-12 09:15:30', '2026-04-12 09:15:30');
INSERT INTO `sensor_data` VALUES (24, 'DEVICE001', 1.7, 0, 39.9042, 116.4074, 26.5, 49, '2026-04-12 10:20:15', '2026-04-12 10:20:15');
INSERT INTO `sensor_data` VALUES (25, 'DEVICE001', 2.9, 0, 39.9042, 116.4074, 25.8, 46, '2026-04-12 11:30:45', '2026-04-12 11:30:45');
INSERT INTO `sensor_data` VALUES (26, 'DEVICE001', 3.5, 0, 39.9042, 116.4074, 26, 48, '2026-04-12 14:45:20', '2026-04-12 14:45:20');
INSERT INTO `sensor_data` VALUES (27, 'DEVICE002', 2.2, 0, 30.5928, 114.3055, 25.9, 53, '2026-04-12 15:15:10', '2026-04-12 15:15:10');
INSERT INTO `sensor_data` VALUES (28, 'DEVICE003', 1.9, 0, 31.2304, 121.4737, 24.5, 50, '2026-04-12 16:30:25', '2026-04-12 16:30:25');
INSERT INTO `sensor_data` VALUES (29, 'DEVICE001', 2.4, 0, 39.9042, 116.4074, 26.3, 47, '2026-04-13 08:45:15', '2026-04-13 08:45:15');
INSERT INTO `sensor_data` VALUES (30, 'DEVICE001', 1.6, 0, 39.9042, 116.4074, 26.1, 48, '2026-04-13 09:30:15', '2026-04-13 09:30:15');
INSERT INTO `sensor_data` VALUES (31, 'DEVICE002', 2, 0, 30.5928, 114.3055, 25.7, 52, '2026-04-13 10:15:30', '2026-04-13 10:15:30');
INSERT INTO `sensor_data` VALUES (32, 'DEVICE003', 2.7, 0, 31.2304, 121.4737, 24.8, 51, '2026-04-13 11:20:45', '2026-04-13 11:20:45');
INSERT INTO `sensor_data` VALUES (33, 'DEVICE001', 120.5, 0, 39.9042, 116.4074, 25.5, 60, '2026-04-15 20:40:18', '2026-04-15 20:40:18');
INSERT INTO `sensor_data` VALUES (34, 'DEVICE001', 110, 0, 39.9038, 116.408, 25.6, 58, '2026-04-15 20:39:18', '2026-04-15 20:39:18');
INSERT INTO `sensor_data` VALUES (35, 'DEVICE001', 95.5, 0, 39.9035, 116.4085, 25.8, 55, '2026-04-15 20:38:18', '2026-04-15 20:38:18');
INSERT INTO `sensor_data` VALUES (36, 'DEVICE001', 80, 0, 39.903, 116.409, 26, 52, '2026-04-15 20:37:18', '2026-04-15 20:37:18');
INSERT INTO `sensor_data` VALUES (37, 'DEVICE001', 65.5, 0, 39.9025, 116.4095, 26.2, 50, '2026-04-15 20:36:18', '2026-04-15 20:36:18');
INSERT INTO `sensor_data` VALUES (38, 'DEVICE001', 50, 0, 39.902, 116.41, 26.5, 48, '2026-04-15 20:35:18', '2026-04-15 20:35:18');
INSERT INTO `sensor_data` VALUES (39, 'DEVICE001', 45, 1, 39.9015, 116.4105, 26.8, 45, '2026-04-15 20:34:18', '2026-04-15 20:34:18');

-- ----------------------------
-- Table structure for system_log
-- ----------------------------
DROP TABLE IF EXISTS `system_log`;
CREATE TABLE `system_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of system_log
-- ----------------------------

-- ----------------------------
-- Table structure for visually_impaired_user
-- ----------------------------
DROP TABLE IF EXISTS `visually_impaired_user`;
CREATE TABLE `visually_impaired_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `id_card` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地址',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `id_card`(`id_card` ASC) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '视力障碍用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of visually_impaired_user
-- ----------------------------
INSERT INTO `visually_impaired_user` VALUES (1, 'user1', '123456', '测试用户', '13900139000000', '110101199001011234', '北京市海淀区', '2026-04-11 09:44:41', '2026-04-12 22:52:48');
INSERT INTO `visually_impaired_user` VALUES (2, 'zhangweiming', '123456', '张维明', '13811223344', '110102198506154321', '北京市西城区西长安街12号', '2026-04-11 12:34:44', '2026-04-13 16:50:30');
INSERT INTO `visually_impaired_user` VALUES (3, 'liuhua', '123456', '刘华', '13788990011', '320104197809234567', '南京市玄武区中山路1号', '2026-04-11 13:51:11', '2026-04-11 13:51:11');
INSERT INTO `visually_impaired_user` VALUES (4, 'wanghong', '123456', '王红', '13677889900', '420106198205123456', '武汉市武昌区珞喻路1037号', '2026-04-11 13:51:23', '2026-04-11 13:51:23');
INSERT INTO `visually_impaired_user` VALUES (5, 'chenglin', '123456', '程林', '13566778899111', '350203198511304567', '厦门市思明区湖滨南路88号', '2026-04-11 13:51:29', '2026-04-12 00:46:05');
INSERT INTO `visually_impaired_user` VALUES (9, '弱者', '123456', '王少', '13395316449', '', '山东潍坊', '2026-04-12 14:41:49', '2026-04-12 14:41:49');
INSERT INTO `visually_impaired_user` VALUES (10, 'scs', 'sd', '闻所未闻', '166666666', '464661', '是的是的1', '2026-04-12 22:53:33', '2026-04-12 23:14:02');
INSERT INTO `visually_impaired_user` VALUES (12, '放大', 'f', '的', '发顺丰', '发顺丰？？', '是否是1', '2026-04-12 23:06:47', '2026-04-13 00:20:10');
INSERT INTO `visually_impaired_user` VALUES (15, '123', '333', '刚刚', '888', '555', '他依然', '2026-04-13 16:01:41', '2026-04-13 16:01:41');

SET FOREIGN_KEY_CHECKS = 1;
