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

import java.lang.reflect.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 执行追踪切面
 * 自动追踪标记了@TraceExecution的方法
 */
@Aspect
@Component
@ConditionalOnProperty(name = "execution.trace.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
@SuppressWarnings("unchecked")
public class ExecutionTraceAspect {
    
    @Autowired(required = false)
    private TraceServiceClient traceServiceClient;
    
    // 构造函数，用于确认切面是否被加载
    public ExecutionTraceAspect() {
        log.info("ExecutionTraceAspect 切面已加载");
    }
    
    @Around("@annotation(com.aidoctor.diagnosis.annotation.TraceExecution) || @within(com.aidoctor.diagnosis.annotation.TraceExecution)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        // 手动获取注解（先尝试方法上的注解，再尝试类上的注解）
        TraceExecution traceExecution = getTraceExecutionAnnotation(joinPoint);
        
        log.debug("追踪切面被触发: method={}, traceServiceClient={}, traceExecution={}", 
            joinPoint.getSignature().getName(), traceServiceClient != null, traceExecution != null);
        
        if (traceServiceClient == null || traceExecution == null) {
            log.warn("追踪切面跳过: traceServiceClient={}, traceExecution={}", 
                traceServiceClient != null, traceExecution != null);
            return joinPoint.proceed();
        }
        
        String cdpId = TraceContext.getCdpId();
        if (cdpId == null) {
            log.warn("追踪切面跳过: cdpId为null");
            return joinPoint.proceed();
        }
        
        log.info("开始追踪方法执行: method={}, cdpId={}", joinPoint.getSignature().getName(), cdpId);
        
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
     * 获取TraceExecution注解
     * 优先获取方法上的注解，如果没有则获取类上的注解
     */
    private TraceExecution getTraceExecutionAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // 先尝试获取方法上的注解
            TraceExecution annotation = method.getAnnotation(TraceExecution.class);
            if (annotation != null) {
                return annotation;
            }
            
            // 如果方法上没有，尝试获取类上的注解
            Class<?> targetClass = joinPoint.getTarget().getClass();
            annotation = targetClass.getAnnotation(TraceExecution.class);
            if (annotation != null) {
                return annotation;
            }
            
            // 如果类上也没有，尝试获取接口上的注解（如果方法是从接口继承的）
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> intf : interfaces) {
                try {
                    Method interfaceMethod = intf.getMethod(method.getName(), method.getParameterTypes());
                    annotation = interfaceMethod.getAnnotation(TraceExecution.class);
                    if (annotation != null) {
                        return annotation;
                    }
                } catch (NoSuchMethodException e) {
                    // 接口中没有这个方法，继续
                }
            }
            
        } catch (Exception e) {
            log.warn("获取TraceExecution注解失败: {}", e.getMessage());
        }
        
        return null;
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
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("argCount", args.length);
        result.put("argTypes", java.util.Arrays.stream(args)
            .map(arg -> arg != null ? arg.getClass().getSimpleName() : "null")
            .toArray());
        return result;
    }
    
    /**
     * 脱敏输出数据
     */
    private Object sanitizeOutput(Object result) {
        // 简单实现：只记录返回类型
        if (result == null) {
            return null;
        }
        
        java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
        resultMap.put("returnType", result.getClass().getSimpleName());
        resultMap.put("hasValue", true);
        return resultMap;
    }
}

