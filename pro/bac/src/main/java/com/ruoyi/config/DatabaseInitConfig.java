package com.ruoyi.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseInitConfig {
    @Bean
    public CommandLineRunner initElectronicFenceTable(JdbcTemplate jdbcTemplate) {
        return args -> {
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
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_x", "ALTER TABLE sensor_data ADD COLUMN accel_x DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_y", "ALTER TABLE sensor_data ADD COLUMN accel_y DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "accel_z", "ALTER TABLE sensor_data ADD COLUMN accel_z DOUBLE NULL DEFAULT NULL");
            addColumnIfMissing(jdbcTemplate, "sensor_data", "fall_confidence", "ALTER TABLE sensor_data ADD COLUMN fall_confidence DOUBLE NULL DEFAULT NULL");
            jdbcTemplate.execute("ALTER TABLE sensor_data CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            jdbcTemplate.execute("ALTER TABLE cane_device CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            jdbcTemplate.execute("ALTER TABLE alarm_record CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            jdbcTemplate.update("UPDATE electronic_fence SET fence_name = '默认安全区' WHERE fence_name IS NULL OR REPLACE(fence_name, '?', '') = ''");
            jdbcTemplate.update("UPDATE cane_device SET status = '在线' WHERE status = '??'");
        };
    }

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
