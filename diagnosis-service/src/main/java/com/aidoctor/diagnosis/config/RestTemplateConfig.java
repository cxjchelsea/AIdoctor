package com.aidoctor.diagnosis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置（用于调用追踪服务）
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        log.info("RestTemplate Bean 创建成功（用于追踪服务）");
        return new RestTemplate();
    }
}

