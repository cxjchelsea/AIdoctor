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
        
        // 从URL中提取方法名（取路径的最后一部分）
        String method = extractMethodFromUrl(url);
        
        long startTime = System.currentTimeMillis();
        
        ExecutionTraceEvent startEvent = ExecutionTraceEvent.builder()
            .cdpId(cdpId)
            .traceId(traceId)
            .type("FEIGN_CALL_START")
            .service(service)
            .module("default")
            .method(method)
            .url(url)
            .timestamp(startTime)
            .status("IN_PROGRESS")
            .build();
        
        traceServiceClient.recordEvent(startEvent);
        
        // 将服务名和方法名添加到请求头，以便在响应拦截器中获取（确保开始和结束事件一致）
        template.header("X-Service-Name", service);
        template.header("X-Method-Name", method);
        // 将traceId和开始时间存储到请求头中，以便在响应拦截器中获取
        template.header("X-Trace-Id", traceId);
        template.header("X-Start-Time", String.valueOf(startTime));
    }
    
    /**
     * 从URL中提取方法名
     * 例如：/api/v1/wellness-screening/a1-demand-classification -> a1DemandClassification
     */
    private String extractMethodFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        
        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }
        
        // 获取路径的最后一部分
        String[] parts = url.split("/");
        if (parts.length == 0) {
            return "unknown";
        }
        
        String lastPart = parts[parts.length - 1];
        if (lastPart.isEmpty() && parts.length > 1) {
            lastPart = parts[parts.length - 2];
        }
        
        // 将 kebab-case 转换为 camelCase
        // 例如：a1-demand-classification -> a1DemandClassification
        if (lastPart.contains("-")) {
            String[] words = lastPart.split("-");
            StringBuilder camelCase = new StringBuilder(words[0]);
            for (int i = 1; i < words.length; i++) {
                if (!words[i].isEmpty()) {
                    camelCase.append(Character.toUpperCase(words[i].charAt(0)));
                    if (words[i].length() > 1) {
                        camelCase.append(words[i].substring(1));
                    }
                }
            }
            return camelCase.toString();
        }
        
        return lastPart.isEmpty() ? "unknown" : lastPart;
    }
}

