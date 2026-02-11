package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 冲突解决算法
 * 解决证据融合过程中检测到的冲突
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent核心算法设计.md》三、冲突解决算法
 */
@Slf4j
@Component
public class ConflictResolution {
    
    /**
     * 工具优先级
     */
    private static final Map<String, Integer> TOOL_PRIORITY = new HashMap<String, Integer>() {{
        put("tool_6", 8);  // 风险评估工具优先级最高
        put("tool_4", 7);  // 检查建议工具
        put("tool_3", 6);  // 鉴别诊断工具
        put("tool_2", 5);  // 主动问诊工具
        put("tool_1", 4);  // 病例理解工具
        put("tool_0", 3);  // 健康状态判定工具
        put("tool_5", 2);  // 治疗建议工具
        put("tool_7", 1);  // 证据链工具
    }};
    
    /**
     * 证据强度优先级
     */
    private static final Map<String, Integer> STRENGTH_PRIORITY = new HashMap<String, Integer>() {{
        put("strong", 3);
        put("medium", 2);
        put("weak", 1);
    }};
    
    /**
     * 风险等级优先级
     */
    private static final Map<String, Integer> RISK_PRIORITY = new HashMap<String, Integer>() {{
        put("L1", 4);
        put("L2", 3);
        put("L3", 2);
        put("L4", 1);
    }};
    
    /**
     * 解决冲突
     * 
     * @param conflicts 冲突列表
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @return 冲突解决结果
     */
    @SuppressWarnings("unchecked")
    public ResolutionResult resolveConflicts(List<Map<String, Object>> conflicts, 
                                            CDP cdp, 
                                            AgentState agentState) {
        log.info("开始冲突解决: conflictsCount={}", conflicts.size());
        
        List<Map<String, Object>> resolutionResults = new ArrayList<>();
        
        for (Map<String, Object> conflict : conflicts) {
            String conflictType = (String) conflict.get("conflict_type");
            String strategy = selectResolutionStrategy(conflict, cdp, agentState);
            
            Map<String, Object> resolution;
            switch (strategy) {
                case "priority_rule":
                    resolution = resolveByPriorityRule(conflict, cdp, agentState);
                    break;
                case "evidence_strength_priority":
                    resolution = resolveByEvidenceStrength(conflict, cdp, agentState);
                    break;
                case "expert_voting":
                    resolution = resolveByExpertVoting(conflict, cdp, agentState);
                    break;
                case "risk_priority":
                    resolution = resolveByRiskPriority(conflict, cdp, agentState);
                    break;
                default:
                    resolution = resolveByExpertVoting(conflict, cdp, agentState);
            }
            
            resolution.put("conflict_id", conflict.getOrDefault("conflict_id", UUID.randomUUID().toString()));
            resolution.put("strategy", strategy);
            resolutionResults.add(resolution);
        }
        
        // 更新AgentState
        Map<String, Object> evidenceFusionState = agentState.getEvidenceFusionStateMap();
        evidenceFusionState.put("conflict_resolution", resolutionResults);
        agentState.setEvidenceFusionStateMap(evidenceFusionState);
        
        log.info("冲突解决完成: resolvedCount={}", resolutionResults.size());
        
        return ResolutionResult.builder()
            .resolutionResults(resolutionResults)
            .build();
    }
    
    /**
     * 选择冲突解决策略
     */
    @SuppressWarnings("unchecked")
    private String selectResolutionStrategy(Map<String, Object> conflict, CDP cdp, AgentState agentState) {
        String conflictType = (String) conflict.get("conflict_type");
        
        // 如果冲突涉及高危诊断，使用风险优先策略
        List<String> affectedDirections = (List<String>) conflict.get("affected_directions");
        if (affectedDirections != null) {
            Map<String, Object> ddxMap = cdp.getDdxMap();
            if (ddxMap != null) {
                List<Map<String, Object>> rankList = (List<Map<String, Object>>) ddxMap.get("rank_list");
                if (rankList != null) {
                    for (String direction : affectedDirections) {
                        for (Map<String, Object> ddx : rankList) {
                            if (direction.equals(ddx.get("disease_name"))) {
                                String riskLevel = (String) ddx.get("risk_level");
                                if ("L1".equals(riskLevel) || "L2".equals(riskLevel)) {
                                    return "risk_priority";
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 根据冲突类型选择策略
        if ("diagnosis_probability".equals(conflictType)) {
            List<Map<String, Object>> conflictingValues = (List<Map<String, Object>>) conflict.get("conflicting_values");
            if (conflictingValues != null) {
                for (Map<String, Object> value : conflictingValues) {
                    String toolId = (String) value.get("tool_id");
                    if ("tool_4".equals(toolId)) {
                        return "priority_rule";
                    }
                }
            }
            return "expert_voting";
        } else if ("evidence_direction".equals(conflictType)) {
            return "evidence_strength_priority";
        } else if ("diagnosis_direction".equals(conflictType)) {
            return "expert_voting";
        }
        
        return "expert_voting";
    }
    
    /**
     * 优先级规则解决冲突
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveByPriorityRule(Map<String, Object> conflict, 
                                                      CDP cdp, 
                                                      AgentState agentState) {
        List<Map<String, Object>> conflictingValues = (List<Map<String, Object>>) conflict.get("conflicting_values");
        
        Map<String, Object> highestPriorityValue = null;
        int highestPriority = 0;
        
        for (Map<String, Object> value : conflictingValues) {
            String toolId = (String) value.get("tool_id");
            int priority = TOOL_PRIORITY.getOrDefault(toolId, 0);
            if (priority > highestPriority) {
                highestPriority = priority;
                highestPriorityValue = value;
            }
        }
        
        Map<String, Object> resolution = new HashMap<>();
        resolution.put("resolved_value", highestPriorityValue != null ? highestPriorityValue.get("value") : null);
        resolution.put("resolution_reason", "使用优先级规则，选择优先级最高的工具的值");
        resolution.put("confidence", 0.8);
        
        return resolution;
    }
    
    /**
     * 证据强度优先解决冲突
     */
    private Map<String, Object> resolveByEvidenceStrength(Map<String, Object> conflict, 
                                                          CDP cdp, 
                                                          AgentState agentState) {
        // 简化实现
        Map<String, Object> resolution = new HashMap<>();
        resolution.put("resolved_value", null);
        resolution.put("resolution_reason", "使用证据强度优先规则");
        resolution.put("confidence", 0.85);
        
        return resolution;
    }
    
    /**
     * 专家投票解决冲突
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveByExpertVoting(Map<String, Object> conflict, 
                                                       CDP cdp, 
                                                       AgentState agentState) {
        // 工具权重
        Map<String, Double> toolWeights = new HashMap<String, Double>() {{
            put("tool_6", 0.4);
            put("tool_4", 0.3);
            put("tool_3", 0.2);
            put("tool_2", 0.05);
            put("tool_1", 0.03);
            put("tool_0", 0.01);
            put("tool_5", 0.005);
            put("tool_7", 0.005);
        }};
        
        List<Map<String, Object>> conflictingValues = (List<Map<String, Object>>) conflict.get("conflicting_values");
        
        Map<Object, Double> voteCounts = new HashMap<>();
        double totalVotes = 0.0;
        
        for (Map<String, Object> value : conflictingValues) {
            String toolId = (String) value.get("tool_id");
            Object voteValue = value.get("value");
            double weight = toolWeights.getOrDefault(toolId, 0.01);
            
            voteCounts.put(voteValue, voteCounts.getOrDefault(voteValue, 0.0) + weight);
            totalVotes += weight;
        }
        
        Object resolvedValue = voteCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        double confidence = resolvedValue != null ? 
            voteCounts.get(resolvedValue) / totalVotes : 0.0;
        
        Map<String, Object> resolution = new HashMap<>();
        resolution.put("resolved_value", resolvedValue);
        resolution.put("resolution_reason", "使用专家投票规则");
        resolution.put("confidence", confidence);
        
        return resolution;
    }
    
    /**
     * 风险优先解决冲突
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveByRiskPriority(Map<String, Object> conflict, 
                                                       CDP cdp, 
                                                       AgentState agentState) {
        List<String> affectedDirections = (List<String>) conflict.get("affected_directions");
        Map<String, Object> ddxMap = cdp.getDdxMap();
        
        String highestRiskDirection = null;
        int highestRisk = 0;
        
        if (ddxMap != null && affectedDirections != null) {
            List<Map<String, Object>> rankList = (List<Map<String, Object>>) ddxMap.get("rank_list");
            if (rankList != null) {
                for (String direction : affectedDirections) {
                    for (Map<String, Object> ddx : rankList) {
                        if (direction.equals(ddx.get("disease_name"))) {
                            String riskLevel = (String) ddx.get("risk_level");
                            int risk = RISK_PRIORITY.getOrDefault(riskLevel, 0);
                            if (risk > highestRisk) {
                                highestRisk = risk;
                                highestRiskDirection = direction;
                            }
                        }
                    }
                }
            }
        }
        
        Map<String, Object> resolution = new HashMap<>();
        resolution.put("resolved_value", highestRiskDirection);
        resolution.put("resolution_reason", "使用风险优先规则，选择风险等级最高的诊断");
        resolution.put("confidence", 0.9);
        
        return resolution;
    }
    
    /**
     * 冲突解决结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ResolutionResult {
        /**
         * 冲突解决结果列表
         */
        private List<Map<String, Object>> resolutionResults;
    }
}

