# 数据库设计

## 数据库概述

- **数据库类型**: MySQL 8.0
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **时区**: Asia/Shanghai

## 表结构设计

### 1. admin (管理员表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 管理员ID | PK, AUTO_INCREMENT |
| username | varchar(50) | 用户名 | UNIQUE, NOT NULL |
| password | varchar(100) | 密码 | NOT NULL |
| name | varchar(50) | 姓名 | |
| phone | varchar(20) | 电话 | |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

### 2. visually_impaired_user (视障用户表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 用户ID | PK, AUTO_INCREMENT |
| name | varchar(50) | 姓名 | NOT NULL |
| age | int | 年龄 | |
| gender | varchar(10) | 性别 | |
| phone | varchar(20) | 联系电话 | |
| photo | varchar(255) | 照片URL | |
| address | varchar(255) | 家庭住址 | |
| emergency_contact | varchar(50) | 紧急联系人 | |
| emergency_phone | varchar(20) | 紧急联系电话 | |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

### 3. guardian (监护人表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 监护人ID | PK, AUTO_INCREMENT |
| user_id | bigint | 关联用户ID | FK |
| name | varchar(50) | 姓名 | NOT NULL |
| phone | varchar(20) | 手机号 | UNIQUE |
| relationship | varchar(20) | 关系 | |
| is_primary | tinyint | 是否主要监护人 | DEFAULT 0 |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

### 4. cane_device (盲杖设备表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 设备ID | PK, AUTO_INCREMENT |
| device_id | varchar(50) | 设备编号 | UNIQUE, NOT NULL |
| device_name | varchar(50) | 设备名称 | |
| user_id | bigint | 所属用户ID | FK |
| battery_level | int | 电池电量(0-100) | DEFAULT 100 |
| status | varchar(20) | 在线状态 | DEFAULT 'offline' |
| health_score | int | 健康度评分(0-100) | |
| last_data_time | datetime | 最后数据时间 | |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

### 5. sensor_data (传感器数据表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 数据ID | PK, AUTO_INCREMENT |
| device_id | varchar(50) | 设备ID | FK, NOT NULL |
| obstacle_distance | double | 障碍物距离(米) | |
| is_fall | tinyint | 是否跌倒 | DEFAULT 0 |
| accel_x | double | X轴加速度 | |
| accel_y | double | Y轴加速度 | |
| accel_z | double | Z轴加速度 | |
| fall_confidence | double | 跌倒置信度 | |
| latitude | double | 纬度 | |
| longitude | double | 经度 | |
| temperature | double | 温度(℃) | |
| humidity | double | 湿度(%) | |
| data_time | datetime | 数据时间 | |
| created_at | timestamp | 创建时间 | |

### 6. alarm_record (报警记录表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 报警ID | PK, AUTO_INCREMENT |
| device_id | varchar(50) | 设备ID | FK, NOT NULL |
| alarm_type | varchar(50) | 报警类型 | NOT NULL |
| alarm_time | datetime | 报警时间 | NOT NULL |
| latitude | double | 纬度 | |
| longitude | double | 经度 | |
| address | varchar(255) | 地址 | |
| status | varchar(20) | 状态 | DEFAULT 'pending' |
| handle_time | datetime | 处理时间 | |
| handler | varchar(50) | 处理人 | |
| description | text | 描述 | |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

### 7. electronic_fence (电子围栏表)

| 字段名 | 类型 | 说明 | 约束 |
|--------|------|------|------|
| id | bigint | 围栏ID | PK, AUTO_INCREMENT |
| fence_name | varchar(50) | 围栏名称 | NOT NULL |
| user_id | bigint | 所属用户ID | FK |
| fence_type | varchar(20) | 围栏类型 | NOT NULL |
| coordinates | json | 坐标点 | |
| radius | double | 半径(米) | |
| is_alarm_enabled | tinyint | 是否启用报警 | DEFAULT 1 |
| start_time | time | 生效开始时间 | |
| end_time | time | 生效结束时间 | |
| status | varchar(20) | 状态 | DEFAULT 'active' |
| created_at | timestamp | 创建时间 | |
| updated_at | timestamp | 更新时间 | |

## 索引设计

### cane_device表
- `idx_device_id`: device_id
- `idx_user_id`: user_id
- `idx_status`: status

### sensor_data表
- `idx_device_id`: device_id
- `idx_data_time`: data_time
- `idx_created_at`: created_at

### alarm_record表
- `idx_device_id`: device_id
- `idx_alarm_time`: alarm_time
- `idx_status`: status
- `idx_alarm_type`: alarm_type

### electronic_fence表
- `idx_user_id`: user_id
- `idx_status`: status

## 数据分区

### sensor_data表
- 按月分区，保留最近12个月数据
- 历史数据归档到历史表

### alarm_record表
- 按月分区，保留最近24个月数据

## 数据清理策略

### sensor_data表
- 保留最近30天详细数据
- 超过30天的数据按小时聚合
- 超过90天的数据按天聚合

### alarm_record表
- 保留所有已处理的报警记录
- 已忽略的报警保留6个月

## 备份策略

### 备份频率
- 全量备份：每天凌晨2点
- 增量备份：每4小时一次
- 日志备份：每天一次

### 保留周期
- 全量备份：保留7天
- 增量备份：保留30天
- 日志备份：保留90天
