package com.aidoctor.diagnosis.config;

import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
import com.aidoctor.diagnosis.service.trace.ExecutionTraceService;
import com.aidoctor.diagnosis.util.TraceContext;
import feign.Response;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.UUID;

/**
 * Feign响应拦截器
 * 记录Feign调用的结束事件
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
public class FeignTraceResponseInterceptor {
    
    @Autowired(required = false)
    private ExecutionTraceService traceService;
    
    @Bean
    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new TraceDecoder(new SpringDecoder(messageConverters));
    }
    
    /**
     * 带追踪功能的Decoder
     */
    private class TraceDecoder implements Decoder {
        private final Decoder delegate;
        private long startTime;
        
        public TraceDecoder(Decoder delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Object decode(Response response, Type type) throws IOException {
            String cdpId = TraceContext.getCdpId();
            if (traceService != null && cdpId != null) {
                // 获取traceId
                Collection<String> traceIdHeaders = response.headers().get("X-Trace-Id");
                String traceId = traceIdHeaders != null && !traceIdHeaders.isEmpty() 
                    ? traceIdHeaders.iterator().next() 
                    : UUID.randomUUID().toString();
                
                // 获取服务名称
                String service = response.request().url();
                if (service.contains("/")) {
                    service = service.substring(0, service.indexOf("/"));
                }
                
                // 记录结束事件
                ExecutionTraceEvent endEvent = ExecutionTraceEvent.builder()
                    .cdpId(cdpId)
                    .traceId(traceId)
                    .type("FEIGN_CALL_END")
                    .service(service)
                    .url(response.request().url())
                    .status(response.status() >= 200 && response.status() < 300 ? "SUCCESS" : "ERROR")
                    .duration(System.currentTimeMillis() - startTime)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                traceService.recordEvent(endEvent);
            }
            
            return delegate.decode(response, type);
        }
    }
}

