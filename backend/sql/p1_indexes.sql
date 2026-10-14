-- 高频查询索引（在 smart_cane 库执行）
-- 用法: mysql -u smart_cane -p smart_cane < sql/p1_indexes.sql
-- 可重复执行：已存在的索引会跳过，不会报 1061

DELIMITER $$

DROP PROCEDURE IF EXISTS ensure_index $$
CREATE PROCEDURE ensure_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
        LIMIT 1
    ) THEN
        SET @sql = p_ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL ensure_index('sensor_data', 'idx_sensor_device_created',
    'CREATE INDEX idx_sensor_device_created ON sensor_data (device_id, created_at, id)');

CALL ensure_index('sensor_data', 'idx_sensor_created',
    'CREATE INDEX idx_sensor_created ON sensor_data (created_at)');

CALL ensure_index('alarm_record', 'idx_alarm_status_time',
    'CREATE INDEX idx_alarm_status_time ON alarm_record (status, alarm_time, id)');

CALL ensure_index('alarm_record', 'idx_alarm_device_time',
    'CREATE INDEX idx_alarm_device_time ON alarm_record (device_id, alarm_time, id)');

CALL ensure_index('cane_device', 'idx_device_device_id',
    'CREATE INDEX idx_device_device_id ON cane_device (device_id)');

CALL ensure_index('cane_device', 'idx_device_user_id',
    'CREATE INDEX idx_device_user_id ON cane_device (user_id)');

CALL ensure_index('electronic_fence', 'idx_fence_device_id',
    'CREATE INDEX idx_fence_device_id ON electronic_fence (device_id)');

DROP PROCEDURE IF EXISTS ensure_index;
