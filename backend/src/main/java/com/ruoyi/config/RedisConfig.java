package com.ruoyi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 可选 Redis：仅当 {@code app.redis.enabled=true} 时装配。
 * 默认关闭，避免本地未装 Redis 时启动失败；
 * 启用后：限流、看板/监控缓存、设备运行时态（最新传感 / 告警去重 / 静止计时）。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database) {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration(host, port);
        conf.setDatabase(database);
        if (password != null && !password.isBlank()) {
            conf.setPassword(password);
        }
        return new LettuceConnectionFactory(conf);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
