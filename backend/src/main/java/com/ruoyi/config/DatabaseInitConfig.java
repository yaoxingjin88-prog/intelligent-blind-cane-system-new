package com.ruoyi.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 启动时轻量库表补齐
 * 用于演示/升级场景：建围栏表、补传感加速度列、统一 utf8mb4，避免手工跑脚本
 */
@Configuration
public class DatabaseInitConfig {

    /**
     * 应用启动后执行一次 DDL/修复 SQL
     * 业务步骤：
     * 1. 确保 electronic_fence 存在
     * 2. sensor_data 补加速度/置信度列（若缺失）
     * 3. 关键表字符集转 utf8mb4
     * 4. 修复历史乱码占位数据
     *
     * @param jdbcTemplate JDBC 模板
     * @return 启动回调
     */
    @Bean
    public CommandLineRunner initElectronicFenceTable(JdbcTemplate jdbcTemplate) {
        return args -> {
            // 步骤1：围栏表
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS electronic_fence (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "device_id VARCHAR(50) NOT NULL UNIQUE," +
                            "fence_name VARCHAR(100) DEFAULT NULL," +
                            "center_latitude DOUBLE DEFAULT NULL," +
                            "center_longitude DOUBLE DEFAULT NULL," +
                            "radius_meters DOUBLE DEFAULT NULL," +
                            "enabled TINYINT(1) NOT NULL DEFAULT 1," +
                            "last_status VARCHAR(20) NOT NULL DEFAULT 'INSIDE'," +
                            "created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci"
            );
            jdbcTemplate.execute("ALTER TABLE electronic_fence CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            // 步骤2：传感表增量列
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_x", "ALTER TABLE sensor_data ADD COLUMN accel_x DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_y", "ALTER TABLE sensor_data ADD COLUMN accel_y DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_z", "ALTER TABLE sensor_data ADD COLUMN accel_z DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "fall_confidence", "ALTER TABLE sensor_data ADD COLUMN fall_confidence DOUBLE NULL DEFAULT NULL");
            // 步骤3：字符集
            jdbcTemplate.execute("ALTER TABLE sensor_data CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            jdbcTemplate.execute("ALTER TABLE cane_device CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            jdbcTemplate.execute("ALTER TABLE alarm_record CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            // 步骤4：脏数据修复
            jdbcTemplate.update("UPDATE electronic_fence SET fence_name = '默认安全区' WHERE fence_name IS NULL OR REPLACE(fence_name, '?', '') = ''");
            jdbcTemplate.update("UPDATE cane_device SET status = '在线' WHERE status = '??'");
            // 步骤5：监护人角色（主监护人 / 子监护人）
            addColumnIfMissing(jdbcTemplate, "guardian", "role",
                    "ALTER TABLE guardian ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PRIMARY' COMMENT 'PRIMARY主监护人 MEMBER子监护人' AFTER relation");
            // 同一持杖用户：id 最小者为主监护人，其余为子监护人（幂等）
            jdbcTemplate.update(
                    "UPDATE guardian g " +
                            "INNER JOIN (SELECT user_id, MIN(id) AS primary_id FROM guardian WHERE user_id IS NOT NULL GROUP BY user_id) t " +
                            "ON g.user_id = t.user_id " +
                            "SET g.role = CASE WHEN g.id = t.primary_id THEN 'PRIMARY' ELSE 'MEMBER' END"
            );
            // 步骤6：设备可解绑（user_id 可空）+ 历史一人多绑收敛为只保留最早一台
            try {
                jdbcTemplate.execute("ALTER TABLE cane_device MODIFY COLUMN user_id BIGINT NULL COMMENT '持杖用户ID，可空表示未绑定'");
            } catch (Exception ignored) {
                // 已是可空或无权限时跳过
            }
            jdbcTemplate.update(
                    "UPDATE cane_device d " +
                            "INNER JOIN ( " +
                            "  SELECT user_id, MIN(id) AS keep_id FROM cane_device " +
                            "  WHERE user_id IS NOT NULL GROUP BY user_id HAVING COUNT(*) > 1 " +
                            ") t ON d.user_id = t.user_id AND d.id <> t.keep_id " +
                            "SET d.user_id = NULL"
            );
        };
    }

    /** 列不存在时再执行 ALTER，保证幂等可重复启动 */
    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
