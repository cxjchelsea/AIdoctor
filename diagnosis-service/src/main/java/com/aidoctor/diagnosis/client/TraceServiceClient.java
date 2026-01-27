package com.aidoctor.diagnosis.client;

import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 追踪服务客户端
 * 用于调用独立的追踪服务
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
public class TraceServiceClient {
    
    private final RestTemplate restTemplate;
    private final String traceServiceUrl;
    
    public TraceServiceClient(
        RestTemplate restTemplate,
        @Value("${trace.service-url:http://localhost:8093}") String traceServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.traceServiceUrl = traceServiceUrl;
        log.info("TraceServiceClient 初始化成功: traceServiceUrl={}", traceServiceUrl);
    }
    
    /**
     * 发送追踪事件到追踪服务
     */
    public void recordEvent(ExecutionTraceEvent event) {
        try {
            log.info("发送追踪事件: cdpId={}, type={}, service={}", 
                event.getCdpId(), event.getType(), event.getService());
            restTemplate.postForObject(
                traceServiceUrl + "/api/v1/trace/events",
                event,
                Void.class
            );
            log.info("追踪事件发送成功: cdpId={}, type={}", event.getCdpId(), event.getType());
        } catch (Exception e) {
            log.error("发送追踪事件失败: cdpId={}, type={}, url={}", 
                event.getCdpId(), event.getType(), traceServiceUrl + "/api/v1/trace/events", e);
            // 不影响主业务流程
        }
    }
}

