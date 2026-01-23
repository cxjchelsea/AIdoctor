package com.aidoctor.diagnosis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 执行追踪注解
 * 用于标记需要追踪的方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceExecution {
    
    /**
     * 服务名称
     */
    String service() default "";
    
    /**
     * 模块名称
     */
    String module() default "";
    
    /**
     * 是否记录输入数据
     */
    boolean traceInput() default true;
    
    /**
     * 是否记录输出数据
     */
    boolean traceOutput() default true;
}

