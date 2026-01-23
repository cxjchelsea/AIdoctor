package com.aidoctor.diagnosis.aspect;

import com.aidoctor.diagnosis.annotation.TraceExecution;
import com.aidoctor.diagnosis.client.TraceServiceClient;
import com.aidoctor.diagnosis.dto.trace.ExecutionTraceEvent;
import com.aidoctor.diagnosis.util.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 执行追踪切面
 * 自动追踪标记了@TraceExecution的方法
 */
@Aspect
@Component
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ExecutionTraceAspect {
    
    @Autowired(required = false)
    private TraceServiceClient traceServiceClient;
    
    @Around("@annotation(traceExecution) || @within(traceExecution)")
    public Object trace(ProceedingJoinPoint joinPoint, TraceExecution traceExecution) throws Throwable {
        if (traceServiceClient == null || traceExecution == null) {
            return joinPoint.proceed();
        }
        
        String cdpId = TraceContext.getCdpId();
        if (cdpId == null) {
            return joinPoint.proceed();
        }
        
        // 获取服务、模块、方法信息
        String service = traceExecution.service();
        String module = traceExecution.module();
        if (service.isEmpty()) {
            service = joinPoint.getSignature().getDeclaringType().getSimpleName();
        }
        if (module.isEmpty()) {
            module = joinPoint.getSignature().getDeclaringType().getSimpleName();
        }
        
        String method = joinPoint.getSignature().getName();
        String traceId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();
        Object result = null;
        
        try {
            // 记录开始事件
            ExecutionTraceEvent startEvent = ExecutionTraceEvent.builder()
                .cdpId(cdpId)
                .traceId(traceId)
                .type("SERVICE_CALL_START")
                .service(service)
                .module(module)
                .method(method)
                .input(traceExecution.traceInput() ? sanitizeInput(args) : null)
                .timestamp(startTime)
                .status("IN_PROGRESS")
                .build();
            
            traceServiceClient.recordEvent(startEvent);
            
            // 执行方法
            result = joinPoint.proceed();
            
            // 记录成功事件
            long duration = System.currentTimeMillis() - startTime;
            ExecutionTraceEvent endEvent = ExecutionTraceEvent.builder()
                .cdpId(cdpId)
                .traceId(traceId)
                .type("SERVICE_CALL_END")
                .service(service)
                .module(module)
                .method(method)
                .output(traceExecution.traceOutput() ? sanitizeOutput(result) : null)
                .duration(duration)
                .status("SUCCESS")
                .timestamp(System.currentTimeMillis())
                .build();
            
            traceServiceClient.recordEvent(endEvent);
            
            return result;
            
        } catch (Throwable e) {
            // 记录失败事件
            long duration = System.currentTimeMillis() - startTime;
            ExecutionTraceEvent errorEvent = ExecutionTraceEvent.builder()
                .cdpId(cdpId)
                .traceId(traceId)
                .type("SERVICE_CALL_ERROR")
                .service(service)
                .module(module)
                .method(method)
                .duration(duration)
                .status("ERROR")
                .errorMessage(e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
            
            traceServiceClient.recordEvent(errorEvent);
            
            throw e;
        }
    }
    
    /**
     * 脱敏输入数据
     */
    private Object sanitizeInput(Object[] args) {
        // 简单实现：只记录参数类型，不记录具体值（避免敏感数据泄露）
        // 可以根据需要扩展脱敏逻辑
        if (args == null || args.length == 0) {
            return null;
        }
        
        // 只记录参数类型和数量
        return Map.of(
            "argCount", args.length,
            "argTypes", java.util.Arrays.stream(args)
                .map(arg -> arg != null ? arg.getClass().getSimpleName() : "null")
                .toArray()
        );
    }
    
    /**
     * 脱敏输出数据
     */
    private Object sanitizeOutput(Object result) {
        // 简单实现：只记录返回类型
        if (result == null) {
            return null;
        }
        
        return Map.of(
            "returnType", result.getClass().getSimpleName(),
            "hasValue", true
        );
    }
}

