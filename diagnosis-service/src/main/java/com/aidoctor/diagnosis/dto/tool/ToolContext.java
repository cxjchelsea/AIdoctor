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
 * ToolContext（工具调用上下文）
 * 主Agent调用工具时传递的上下文信息
 * 
 * 参考文档：
 * - 《7.接口规范/工具调用协议.md》
 * - 《2.架构设计/主Agent架构设计.md》
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolContext {
    
    /**
     * 追踪ID（用于审计和调试）
     */
    private String traceId;
    
    /**
     * CDP引用
     */
    private CDPReference cdpReference;
    
    /**
     * AgentState摘要
     */
    private AgentStateSummary agentStateSummary;
    
    /**
     * 约束（成本/时间/风险）
     */
    private Constraints constraints;
    
    /**
     * 工具调用参数（工具特定）
     */
    private Map<String, Object> callParams;
    
    /**
     * CDP引用
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CDPReference {
        /**
         * CDP ID
         */
        private String cdpId;
        
        /**
         * CDP版本号
         */
        private Integer version;
        
        /**
         * 工具需要读取的CDP字段路径
         */
        @Builder.Default
        private List<String> readFields = new ArrayList<>();
    }
    
    /**
     * AgentState摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStateSummary {
        /**
         * 当前诊断步骤（1-5）
         */
        private Integer currentStep;
        
        /**
         * 工作态（wellness_mode/clinical_mode）
         */
        private String workMode;
    }
    
    /**
     * 约束（成本/时间/风险）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Constraints {
        /**
         * 最大执行时间（秒）
         */
        private Integer maxTimeSeconds;
        
        /**
         * 最大成本
         */
        private Double maxCost;
        
        /**
         * 风险等级限制（L1/L2/L3/L4）
         */
        private String riskLevelLimit;
    }
    
    /**
     * 创建默认的ToolContext
     * 
     * @param traceId 追踪ID
     * @param cdpId CDP ID
     * @param cdpVersion CDP版本号
     * @param readFields 需要读取的CDP字段路径
     * @param currentStep 当前诊断步骤
     * @param workMode 工作态
     * @return ToolContext
     */
    public static ToolContext createDefault(
            String traceId,
            String cdpId,
            Integer cdpVersion,
            List<String> readFields,
            Integer currentStep,
            String workMode) {
        
        return ToolContext.builder()
            .traceId(traceId)
            .cdpReference(CDPReference.builder()
                .cdpId(cdpId)
                .version(cdpVersion)
                .readFields(readFields != null ? readFields : new ArrayList<>())
                .build())
            .agentStateSummary(AgentStateSummary.builder()
                .currentStep(currentStep)
                .workMode(workMode)
                .build())
            .constraints(Constraints.builder()
                .maxTimeSeconds(30)
                .maxCost(10.0)
                .build())
            .callParams(new HashMap<>())
            .build();
    }
}

