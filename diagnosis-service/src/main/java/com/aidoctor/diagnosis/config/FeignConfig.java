package com.aidoctor.diagnosis.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign配置
 */
@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 可以在这里添加请求头，如Token等
            }
        };
    }
}

