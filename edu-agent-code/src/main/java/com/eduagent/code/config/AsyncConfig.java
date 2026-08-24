package com.eduagent.code.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 判分异步执行器（方案 a：@Async 线程池 + 启动补偿，配合 {@link com.eduagent.code.service.worker.JudgeWorker}）。
 * CallerRuns 饱和策略：队列满时由提交线程原地判分（背压，不丢任务）。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("judge-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
