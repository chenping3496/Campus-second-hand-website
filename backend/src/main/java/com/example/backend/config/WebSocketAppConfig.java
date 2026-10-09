package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * WebSocket应用配置
 * 配置线程池、定时任务等
 */
@Configuration
@EnableScheduling
public class WebSocketAppConfig {

    /**
     * 配置WebSocket心跳线程池
     */
    @Bean(name = "webSocketTaskScheduler")
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-scheduler-");
        scheduler.setDaemon(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    /**
     * 库存同步定时任务线程池
     * 用于定期同步Redis库存到数据库
     */
    @Bean(name = "inventorySyncTaskScheduler")
    public ThreadPoolTaskScheduler inventorySyncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("inventory-sync-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    /**
     * 过期预扣清理线程池
     * 用于清理过期的库存预扣记录
     */
    @Bean(name = "reserveCleanupTaskScheduler")
    public ThreadPoolTaskScheduler reserveCleanupTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("reserve-cleanup-");
        scheduler.setDaemon(true);
        return scheduler;
    }
}