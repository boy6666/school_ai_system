package com.eduagent.teacher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池：学情看板聚合（CompletableFuture + Semaphore 保护下游）。
 * caller-runs 拒绝策略保证班级人数瞬时超限时不会丢任务、而是退化为当前线程执行。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("analyticsExecutor")
    public Executor analyticsExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(2);
        e.setMaxPoolSize(8);
        e.setQueueCapacity(200);
        e.setThreadNamePrefix("analytics-");
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();
        return e;
    }
}
