package com.aidoctor.trace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 追踪异步配置
 */
@Configuration
@EnableAsync
public class TraceAsyncConfig {

    @Value("${execution.trace.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${execution.trace.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${execution.trace.async.queue-capacity:1000}")
    private int queueCapacity;

    @Bean(name = "traceExecutor")
    public Executor traceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("TraceExecutor-");
        executor.initialize();
        return executor;
    }
}


