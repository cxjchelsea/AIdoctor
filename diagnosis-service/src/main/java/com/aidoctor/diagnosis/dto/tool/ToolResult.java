package com.aidoctor.diagnosis.dto.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ToolResult（工具返回结果）
 * 工具执行后返回给主Agent的结果
 * 
 * 参考文档：
 * - 《7.接口规范/工具调用协议.md》
 * - 《2.架构设计/主Agent架构设计.md》
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    
    /**
     * 追踪ID（与ToolContext中的trace_id一致）
     */
    private String traceId;
    
    /**
     * 工具ID
     */
    private String toolId;
    
    /**
     * 执行状态（success/partial_success/failure/timeout）
     */
    private String status;
    
    /**
     * 输出payload（工具特定结构）
     */
    private Map<String, Object> payload;
    
    /**
     * 证据引用
     */
    @Builder.Default
    private List<Evidence> evidence = new ArrayList<>();
    
    /**
     * 质量指标
     */
    private Quality quality;
    
    /**
     * 建议写回CDP的字段路径
     */
    @Builder.Default
    private List<SuggestedWrite> suggestedWrites = new ArrayList<>();
    
    /**
     * 错误信息
     */
    @Builder.Default
    private List<ErrorInfo> errors = new ArrayList<>();
    
    /**
     * 执行时间（毫秒）
     */
    private Long durationMs;
    
    /**
     * 元数据（工具特定）
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 证据引用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Evidence {
        /**
         * 证据来源（knowledge_base/kg_path/rule/llm）
         */
        private String source;
        
        /**
         * 证据引用（CUI/路径ID/规则ID/LLM prompt）
         */
        private String reference;
        
        /**
         * 证据强度（strong/medium/weak）
         */
        private String strength;
    }
    
    /**
     * 质量指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quality {
        /**
         * 置信度（0.0-1.0）
         */
        private Double confidence;
        
        /**
         * 完整度（0.0-1.0）
         */
        private Double completeness;
        
        /**
         * 准确度（0.0-1.0，如可评估）
         */
        private Double accuracy;
    }
    
    /**
     * 建议写回CDP的字段路径
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedWrite {
        /**
         * 字段路径（如cdp.ddx）
         */
        private String fieldPath;
        
        /**
         * 字段值
         */
        private Object value;
        
        /**
         * 写回原因
         */
        private String reason;
    }
    
    /**
     * 错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        /**
         * 错误类型（timeout/validation_error/runtime_error）
         */
        private String errorType;
        
        /**
         * 错误消息
         */
        private String errorMessage;
        
        /**
         * 错误详情
         */
        @Builder.Default
        private Map<String, Object> errorDetails = new HashMap<>();
    }
    
    /**
     * 判断是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "success".equals(status);
    }
    
    /**
     * 判断是否部分成功
     * 
     * @return 是否部分成功
     */
    public boolean isPartialSuccess() {
        return "partial_success".equals(status);
    }
    
    /**
     * 判断是否失败
     * 
     * @return 是否失败
     */
    public boolean isFailure() {
        return "failure".equals(status) || "timeout".equals(status);
    }
}

