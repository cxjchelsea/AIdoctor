package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 升级策略处理器
 * 处理升级条件，执行升级策略
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent运行循环设计.md》
 */
@Slf4j
@Component
public class EscalationHandler {
    
    /**
     * 评估升级条件
     * 
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @return 是否满足升级条件
     */
    @SuppressWarnings("unchecked")
    public EscalationResult evaluateEscalation(CDP cdp, AgentState agentState) {
        log.debug("评估升级条件: cdpId={}", cdp.getId());
        
        boolean shouldEscalate = false;
        String escalationReason = null;
        String escalationType = null;
        
        // 1. 检查高风险识别
        Map<String, Object> triageMap = cdp.getTriage();
        if (triageMap != null) {
            String riskLevel = (String) triageMap.get("risk_level");
            if ("L1".equals(riskLevel) || "L2".equals(riskLevel)) {
                shouldEscalate = true;
                escalationReason = "识别到高风险情况（风险等级：" + riskLevel + "）";
                escalationType = "high_risk";
            }
        }
        
        // 2. 检查红旗信号
        if (triageMap != null) {
            Object redFlagsObj = triageMap.get("red_flags");
            if (redFlagsObj instanceof java.util.List) {
                java.util.List<?> redFlags = (java.util.List<?>) redFlagsObj;
                if (redFlags != null && !redFlags.isEmpty()) {
                    shouldEscalate = true;
                    escalationReason = "识别到危险信号（红旗信号）";
                    escalationType = "red_flag";
                }
            }
        }
        
        // 3. 检查证据不足
        Map<String, Object> uncertaintyMap = cdp.getUncertainty();
        if (uncertaintyMap != null) {
            Object missingInfoObj = uncertaintyMap.get("missing_critical_info");
            if (missingInfoObj instanceof java.util.List) {
                java.util.List<?> missingInfo = (java.util.List<?>) missingInfoObj;
                if (missingInfo != null && !missingInfo.isEmpty()) {
                    // 检查是否有关键信息缺失
                    boolean hasCriticalMissing = missingInfo.stream()
                        .anyMatch(item -> {
                            if (item instanceof Map) {
                                Map<String, Object> itemMap = (Map<String, Object>) item;
                                return Boolean.TRUE.equals(itemMap.get("critical"));
                            }
                            return false;
                        });
                    
                    if (hasCriticalMissing) {
                        shouldEscalate = true;
                        escalationReason = "证据不足以做出诊断结论";
                        escalationType = "insufficient_evidence";
                    }
                }
            }
        }
        
        // 4. 检查工具失败
        java.util.List<Map<String, Object>> triedTools = agentState.getTriedToolsList();
        if (triedTools != null) {
            long failureCount = triedTools.stream()
                .filter(tool -> {
                    String lastResult = (String) tool.get("last_result");
                    return "failure".equals(lastResult) || "timeout".equals(lastResult);
                })
                .count();
            
            if (failureCount >= 3) {
                shouldEscalate = true;
                escalationReason = "关键工具执行失败次数过多（" + failureCount + "次）";
                escalationType = "tool_failure";
            }
        }
        
        // 5. 检查预算超限
        Map<String, Object> budget = agentState.getBudgetMap();
        if (budget != null) {
            Integer currentToolCalls = (Integer) budget.getOrDefault("current_tool_calls", 0);
            Integer maxToolCalls = (Integer) budget.getOrDefault("max_tool_calls", 50);
            
            if (currentToolCalls >= maxToolCalls) {
                shouldEscalate = true;
                escalationReason = "达到最大工具调用次数限制（" + maxToolCalls + "次）";
                escalationType = "budget_exceeded";
            }
        }
        
        log.info("升级条件评估完成: cdpId={}, shouldEscalate={}, reason={}", 
            cdp.getId(), shouldEscalate, escalationReason);
        
        return EscalationResult.builder()
            .shouldEscalate(shouldEscalate)
            .escalationReason(escalationReason)
            .escalationType(escalationType)
            .build();
    }
    
    /**
     * 执行升级策略
     * 
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @param escalationResult 升级评估结果
     * @return 升级处理结果
     */
    public Map<String, Object> executeEscalation(CDP cdp, AgentState agentState, EscalationResult escalationResult) {
        log.info("执行升级策略: cdpId={}, escalationType={}", cdp.getId(), escalationResult.getEscalationType());
        
        Map<String, Object> escalationResponse = new HashMap<>();
        escalationResponse.put("escalated", true);
        escalationResponse.put("escalation_type", escalationResult.getEscalationType());
        escalationResponse.put("escalation_reason", escalationResult.getEscalationReason());
        escalationResponse.put("recommendation", getEscalationRecommendation(escalationResult.getEscalationType()));
        
        return escalationResponse;
    }
    
    /**
     * 获取升级建议
     */
    private String getEscalationRecommendation(String escalationType) {
        switch (escalationType) {
            case "high_risk":
                return "建议立即就医，进行紧急处理";
            case "red_flag":
                return "建议立即就医，进行详细检查";
            case "insufficient_evidence":
                return "建议提供更多信息或进行进一步检查";
            case "tool_failure":
                return "系统异常，建议联系技术支持或人工介入";
            case "budget_exceeded":
                return "诊断流程超时，建议简化问题或重新开始";
            default:
                return "建议人工介入处理";
        }
    }
    
    /**
     * 升级评估结果
     */
    @lombok.Data
    @lombok.Builder
    public static class EscalationResult {
        /**
         * 是否应该升级
         */
        private boolean shouldEscalate;
        
        /**
         * 升级原因
         */
        private String escalationReason;
        
        /**
         * 升级类型
         */
        private String escalationType;
    }
}

