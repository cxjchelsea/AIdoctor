package com.aidoctor.diagnosis.config;

import com.aidoctor.diagnosis.client.TraceServiceClient;
import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
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
    private TraceServiceClient traceServiceClient;
    
    @Bean
    public Decoder feignDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new TraceDecoder(new SpringDecoder(messageConverters));
    }
    
    /**
     * 带追踪功能的Decoder
     */
    private class TraceDecoder implements Decoder {
        private final Decoder delegate;
        
        public TraceDecoder(Decoder delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Object decode(Response response, Type type) throws IOException {
            String cdpId = TraceContext.getCdpId();
            if (traceServiceClient != null && cdpId != null) {
                // 获取traceId
                Collection<String> traceIdHeaders = response.request().headers().get("X-Trace-Id");
                String traceId = traceIdHeaders != null && !traceIdHeaders.isEmpty() 
                    ? traceIdHeaders.iterator().next() 
                    : UUID.randomUUID().toString();
                
                // 从请求头获取服务名和方法名（与开始事件保持一致）
                Collection<String> serviceHeaders = response.request().headers().get("X-Service-Name");
                String service = serviceHeaders != null && !serviceHeaders.isEmpty()
                    ? serviceHeaders.iterator().next()
                    : "unknown";
                
                Collection<String> methodHeaders = response.request().headers().get("X-Method-Name");
                String method = methodHeaders != null && !methodHeaders.isEmpty()
                    ? methodHeaders.iterator().next()
                    : "unknown";
                
                // 从请求头获取开始时间（注意：应该从请求头获取，不是响应头）
                Collection<String> startTimeHeaders = response.request().headers().get("X-Start-Time");
                long startTime = startTimeHeaders != null && !startTimeHeaders.isEmpty()
                    ? Long.parseLong(startTimeHeaders.iterator().next())
                    : System.currentTimeMillis();
                
                // 计算持续时间
                long duration = System.currentTimeMillis() - startTime;
                
                // 记录结束事件
                ExecutionTraceEvent endEvent = ExecutionTraceEvent.builder()
                    .cdpId(cdpId)
                    .traceId(traceId)
                    .type("FEIGN_CALL_END")
                    .service(service)  // 使用与开始事件相同的服务名
                    .module("default")
                    .method(method)  // 添加方法名
                    .url(response.request().url())
                    .status(response.status() >= 200 && response.status() < 300 ? "SUCCESS" : "ERROR")
                    .duration(duration)
                    .timestamp(System.currentTimeMillis())
                    .build();
                
                traceServiceClient.recordEvent(endEvent);
            }
            
            return delegate.decode(response, type);
        }
    }
}

