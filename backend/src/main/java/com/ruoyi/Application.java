package com.ruoyi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.ruoyi.mapper")
@EnableScheduling
@org.springframework.boot.context.properties.EnableConfigurationProperties(com.ruoyi.config.SecurityProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
