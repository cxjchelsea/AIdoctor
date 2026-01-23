package com.aidoctor.diagnosis.dto.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 执行追踪事件
 * 用于在服务间传递追踪信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTraceEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * CDP ID
     */
    private String cdpId;
    
    /**
     * 追踪ID（用于关联开始和结束事件）
     */
    private String traceId;
    
    /**
     * 事件类型
     */
    private String type;
    
    /**
     * 服务名称
     */
    private String service;
    
    /**
     * 模块名称
     */
    private String module;
    
    /**
     * 方法名称
     */
    private String method;
    
    /**
     * 步骤名称
     */
    private String step;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 执行时长（毫秒）
     */
    private Long duration;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 输入数据
     */
    private Object input;
    
    /**
     * 输出数据
     */
    private Object output;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;
}

