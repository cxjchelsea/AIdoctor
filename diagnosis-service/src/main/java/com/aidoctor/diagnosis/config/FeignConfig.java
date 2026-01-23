package com.aidoctor.diagnosis.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Feign配置
 */
@Configuration
public class FeignConfig {
    
    @Autowired(required = false)
    private FeignTraceInterceptor traceInterceptor;
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 可以在这里添加请求头，如Token等
                
                // 如果启用了追踪，应用追踪拦截器
                if (traceInterceptor != null) {
                    traceInterceptor.apply(template);
                }
            }
        };
    }
}

