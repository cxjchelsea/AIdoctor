package com.aidoctor.diagnosis.config;

import com.aidoctor.diagnosis.client.TraceServiceClient;
import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
import com.aidoctor.diagnosis.util.TraceContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Feign追踪拦截器
 * 自动追踪Feign服务调用
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
public class FeignTraceInterceptor implements RequestInterceptor {
    
    @Autowired(required = false)
    private TraceServiceClient traceServiceClient;
    
    @Override
    public void apply(RequestTemplate template) {
        if (traceServiceClient == null) {
            return;
        }
        
        String cdpId = TraceContext.getCdpId();
        if (cdpId == null) {
            return;
        }
        
        // 在请求头中添加CDP ID，以便Python服务也能获取
        template.header("X-CDP-Id", cdpId);
        
        // 记录Feign调用开始事件
        String service = template.feignTarget().name();
        String url = template.url();
        String traceId = UUID.randomUUID().toString();
        
        ExecutionTraceEvent startEvent = ExecutionTraceEvent.builder()
            .cdpId(cdpId)
            .traceId(traceId)
            .type("FEIGN_CALL_START")
            .service(service)
            .url(url)
            .timestamp(System.currentTimeMillis())
            .status("IN_PROGRESS")
            .build();
        
        traceServiceClient.recordEvent(startEvent);
        
        // 将traceId存储到请求属性中，以便在响应拦截器中获取
        template.header("X-Trace-Id", traceId);
    }
}

