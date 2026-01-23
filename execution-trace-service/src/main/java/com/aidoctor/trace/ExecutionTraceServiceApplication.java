package com.aidoctor.trace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 执行追踪服务启动类
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class ExecutionTraceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExecutionTraceServiceApplication.class, args);
    }
}


