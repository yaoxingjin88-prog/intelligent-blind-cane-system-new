-- 插入测试数据脚本
-- 用于在数据库中创建测试设备和传感器数据

-- 1. 插入测试视障用户（如果不存在）
INSERT INTO visually_impaired_user (username, password, name, phone, id_card, address, create_time)
SELECT 'test001', '123456', '张三', '13800138001', '110101199001011234', '北京市朝阳区', NOW()
WHERE NOT EXISTS (SELECT 1 FROM visually_impaired_user WHERE username = 'test001');

-- 2. 插入测试监护人（如果不存在）
INSERT INTO guardian (user_id, name, phone, relationship, create_time)
SELECT 1, '李四', '13900139001', '父亲', NOW()
WHERE NOT EXISTS (SELECT 1 FROM guardian WHERE phone = '13900139001');

-- 3. 插入测试设备（如果不存在）
INSERT INTO cane_device (device_no, user_id, device_name, battery_level, status, last_heartbeat, create_time)
SELECT 'ESP32_001', 1, '张三的智能盲杖', 85, '在线', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM cane_device WHERE device_no = 'ESP32_001');

-- 4. 插入测试传感器数据（最新的位置数据）
-- 位置：北京天安门附近
INSERT INTO sensor_data (device_id, obstacle_distance, is_fall, latitude, longitude, temperature, humidity, data_time, create_time)
VALUES 
('ESP32_001', 120.50, FALSE, 39.9042, 116.4074, 25.5, 60.0, NOW(), NOW()),
('ESP32_001', 95.00, FALSE, 39.9050, 116.4080, 25.8, 58.0, DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
('ESP32_001', 150.00, FALSE, 39.9035, 116.4065, 26.0, 55.0, DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
('ESP32_001', 45.00, TRUE, 39.9055, 116.4090, 24.5, 65.0, DATE_SUB(NOW(), INTERVAL 15 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- 5. 插入测试报警记录
INSERT INTO alarm_record (device_id, alarm_type, alarm_time, status, longitude, latitude, handle_time, handler, create_time)
VALUES 
('ESP32_001', '摔倒报警', DATE_SUB(NOW(), INTERVAL 15 MINUTE), 0, 116.4090, 39.9055, NULL, NULL, DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- 查询验证
SELECT '设备数量' as info, COUNT(*) as count FROM cane_device;
SELECT '传感器数据数量' as info, COUNT(*) as count FROM sensor_data;
SELECT '报警记录数量' as info, COUNT(*) as count FROM alarm_record;
