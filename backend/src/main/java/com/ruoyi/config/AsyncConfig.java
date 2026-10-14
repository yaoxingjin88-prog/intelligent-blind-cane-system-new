package com.ruoyi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 * 供 {@code @Async} 使用（如报警落库推送），避免阻塞传感上报主线程
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 报警等异步任务执行器
     *
     * @return 业务线程池
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心/最大线程与队列：本地小流量够用，生产可按上报 QPS 调大
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-alarm-");
        executor.initialize();
        return executor;
    }
}
