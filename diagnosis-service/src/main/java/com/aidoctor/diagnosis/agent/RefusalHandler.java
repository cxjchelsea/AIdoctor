package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 拒答策略处理器
 * 处理拒答条件，执行拒答策略
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent运行循环设计.md》
 */
@Slf4j
@Component
public class RefusalHandler {
    
    /**
     * 评估拒答条件
     * 
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @return 是否满足拒答条件
     */
    public RefusalResult evaluateRefusal(CDP cdp, AgentState agentState) {
        log.debug("评估拒答条件: cdpId={}", cdp.getId());
        
        boolean shouldRefuse = false;
        String refusalReason = null;
        String refusalType = null;
        
        // 1. 检查超出能力范围
        // 简化实现：检查是否有明确的超出能力范围的标记
        Map<String, Object> uncertaintyMap = cdp.getUncertaintyMap();
        if (uncertaintyMap != null && Boolean.TRUE.equals(uncertaintyMap.get("out_of_scope"))) {
            shouldRefuse = true;
            refusalReason = "超出AI医生的能力范围";
            refusalType = "out_of_scope";
        }
        
        // 2. 检查信息不足
        if (uncertaintyMap != null) {
            Object missingInfoObj = uncertaintyMap.get("missing_critical_info");
            if (missingInfoObj instanceof java.util.List) {
                java.util.List<?> missingInfo = (java.util.List<?>) missingInfoObj;
                if (missingInfo != null && missingInfo.size() > 5) {
                    // 如果缺失的关键信息过多，可能无法做出任何判断
                    shouldRefuse = true;
                    refusalReason = "信息不足以做出任何判断（缺失关键信息过多）";
                    refusalType = "insufficient_information";
                }
            }
        }
        
        // 3. 检查安全风险
        Map<String, Object> triageMap = cdp.getTriageMap();
        if (triageMap != null) {
            String riskLevel = (String) triageMap.get("risk_level");
            if ("L1".equals(riskLevel)) {
                // L1风险等级可能涉及紧急情况，不应仅依赖AI建议
                shouldRefuse = true;
                refusalReason = "存在安全风险，不应仅依赖AI建议，建议立即就医";
                refusalType = "safety_risk";
            }
        }
        
        log.info("拒答条件评估完成: cdpId={}, shouldRefuse={}, reason={}", 
            cdp.getId(), shouldRefuse, refusalReason);
        
        return RefusalResult.builder()
            .shouldRefuse(shouldRefuse)
            .refusalReason(refusalReason)
            .refusalType(refusalType)
            .build();
    }
    
    /**
     * 执行拒答策略
     * 
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @param refusalResult 拒答评估结果
     * @return 拒答处理结果
     */
    public Map<String, Object> executeRefusal(CDP cdp, AgentState agentState, RefusalResult refusalResult) {
        log.info("执行拒答策略: cdpId={}, refusalType={}", cdp.getId(), refusalResult.getRefusalType());
        
        Map<String, Object> refusalResponse = new HashMap<>();
        refusalResponse.put("refused", true);
        refusalResponse.put("refusal_type", refusalResult.getRefusalType());
        refusalResponse.put("refusal_reason", refusalResult.getRefusalReason());
        refusalResponse.put("message", getRefusalMessage(refusalResult.getRefusalType()));
        
        return refusalResponse;
    }
    
    /**
     * 获取拒答消息
     */
    private String getRefusalMessage(String refusalType) {
        switch (refusalType) {
            case "out_of_scope":
                return "抱歉，您的问题超出了AI医生的能力范围，建议咨询专业医生。";
            case "insufficient_information":
                return "抱歉，提供的信息不足以做出任何判断，建议提供更多详细信息或咨询专业医生。";
            case "safety_risk":
                return "抱歉，根据您的情况，存在安全风险，建议立即就医，不应仅依赖AI建议。";
            default:
                return "抱歉，无法为您提供诊断建议，建议咨询专业医生。";
        }
    }
    
    /**
     * 拒答评估结果
     */
    @lombok.Data
    @lombok.Builder
    public static class RefusalResult {
        /**
         * 是否应该拒答
         */
        private boolean shouldRefuse;
        
        /**
         * 拒答原因
         */
        private String refusalReason;
        
        /**
         * 拒答类型
         */
        private String refusalType;
    }
}

