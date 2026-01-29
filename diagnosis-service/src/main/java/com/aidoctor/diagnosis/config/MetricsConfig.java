package com.aidoctor.diagnosis.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Metrics配置类
 * 用于配置和注册自定义指标
 */
@Slf4j
@Configuration
public class MetricsConfig implements MeterBinder {

    /**
     * 注册JVM内存指标
     * 这会自动暴露 jvm_memory_* 相关指标
     */
    @Bean
    public MeterBinder jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * 注册自定义指标
     * 添加 process_resident_memory_bytes 指标
     */
    @Override
    public void bindTo(MeterRegistry registry) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        // 注册进程常驻内存指标（使用JVM committed memory作为近似值）
        // 注意：在Windows上，无法直接获取真正的RSS，这里使用committed作为近似值
        Gauge.builder("process_resident_memory_bytes", memoryMXBean, bean -> {
            long heapCommitted = bean.getHeapMemoryUsage().getCommitted();
            long nonHeapCommitted = bean.getNonHeapMemoryUsage().getCommitted();
            return heapCommitted + nonHeapCommitted;
        })
        .description("Resident memory size in bytes (approximated using JVM committed memory)")
        .register(registry);
        
        log.info("自定义指标已注册: process_resident_memory_bytes");
    }
}

