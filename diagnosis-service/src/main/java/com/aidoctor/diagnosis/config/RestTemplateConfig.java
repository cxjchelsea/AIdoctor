package com.aidoctor.diagnosis.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置（用于调用追踪服务）
 */
@Configuration
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

