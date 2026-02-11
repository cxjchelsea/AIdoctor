package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.dto.tool.ToolResult;
import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 证据融合算法
 * 将多个工具返回的证据融合成统一的证据视图
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent核心算法设计.md》二、证据融合算法
 */
@Slf4j
@Component
public class EvidenceFusion {
    
    /**
     * 证据强度优先级
     */
    private static final Map<String, Integer> STRENGTH_PRIORITY = new HashMap<String, Integer>() {{
        put("strong", 3);
        put("medium", 2);
        put("weak", 1);
    }};
    
    /**
     * 证据来源优先级
     */
    private static final Map<String, Integer> SOURCE_PRIORITY = new HashMap<String, Integer>() {{
        put("knowledge_base", 4);
        put("kg_path", 3);
        put("rule", 2);
        put("llm", 1);
    }};
    
    /**
     * 融合证据
     * 
     * @param toolResults 工具返回结果列表
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @return 融合结果（包含融合后的证据视图和冲突列表）
     */
    public FusionResult fuseEvidence(List<ToolResult> toolResults, CDP cdp, AgentState agentState) {
        log.info("开始证据融合: toolResultsCount={}", toolResults.size());
        
        // 1. 收集所有工具的evidence
        List<ToolResult.Evidence> evidenceList = new ArrayList<>();
        for (ToolResult toolResult : toolResults) {
            if (toolResult.isSuccess() && toolResult.getEvidence() != null) {
                evidenceList.addAll(toolResult.getEvidence());
            }
        }
        
        // 2. 按证据来源分类
        Map<String, List<ToolResult.Evidence>> classifiedEvidence = evidenceList.stream()
            .collect(Collectors.groupingBy(ToolResult.Evidence::getSource));
        
        // 3. 同源证据合并
        Map<String, List<ToolResult.Evidence>> mergedEvidence = new HashMap<>();
        for (Map.Entry<String, List<ToolResult.Evidence>> entry : classifiedEvidence.entrySet()) {
            String source = entry.getKey();
            List<ToolResult.Evidence> evidences = entry.getValue();
            
            // 按内容分组（简化实现：按reference分组）
            Map<String, List<ToolResult.Evidence>> contentGroups = evidences.stream()
                .collect(Collectors.groupingBy(ToolResult.Evidence::getReference));
            
            // 合并相同内容的证据
            List<ToolResult.Evidence> merged = new ArrayList<>();
            for (List<ToolResult.Evidence> group : contentGroups.values()) {
                ToolResult.Evidence mergedEvidenceItem = mergeEvidences(group);
                merged.add(mergedEvidenceItem);
            }
            mergedEvidence.put(source, merged);
        }
        
        // 4. 异源证据叠加
        List<ToolResult.Evidence> fusedEvidenceList = new ArrayList<>();
        for (String source : Arrays.asList("knowledge_base", "kg_path", "rule", "llm")) {
            if (mergedEvidence.containsKey(source)) {
                fusedEvidenceList.addAll(mergedEvidence.get(source));
            }
        }
        
        // 5. 证据强度排序
        fusedEvidenceList.sort((e1, e2) -> {
            int strengthCompare = Integer.compare(
                STRENGTH_PRIORITY.getOrDefault(e2.getStrength(), 0),
                STRENGTH_PRIORITY.getOrDefault(e1.getStrength(), 0)
            );
            if (strengthCompare != 0) {
                return strengthCompare;
            }
            return Integer.compare(
                SOURCE_PRIORITY.getOrDefault(e2.getSource(), 0),
                SOURCE_PRIORITY.getOrDefault(e1.getSource(), 0)
            );
        });
        
        // 6. 证据去重
        List<ToolResult.Evidence> deduplicatedEvidence = deduplicateEvidence(fusedEvidenceList);
        
        // 7. 构建融合后的证据视图
        Map<String, Object> fusedEvidenceView = new HashMap<>();
        fusedEvidenceView.put("evidence_list", deduplicatedEvidence);
        fusedEvidenceView.put("evidence_count", deduplicatedEvidence.size());
        fusedEvidenceView.put("evidence_distribution", buildEvidenceDistribution(deduplicatedEvidence));
        
        // 8. 检测证据冲突
        List<Map<String, Object>> conflicts = detectConflicts(deduplicatedEvidence, cdp, toolResults);
        
        // 9. 更新evidence_fusion_state
        Map<String, Object> evidenceFusionState = agentState.getEvidenceFusionStateMap();
        evidenceFusionState.put("fused_evidence_view", fusedEvidenceView);
        evidenceFusionState.put("conflicts", conflicts);
        evidenceFusionState.put("fusion_timestamp", System.currentTimeMillis());
        agentState.setEvidenceFusionStateMap(evidenceFusionState);
        
        log.info("证据融合完成: evidenceCount={}, conflictsCount={}", 
            deduplicatedEvidence.size(), conflicts.size());
        
        return FusionResult.builder()
            .fusedEvidenceView(fusedEvidenceView)
            .conflicts(conflicts)
            .build();
    }
    
    /**
     * 合并相同内容的证据
     */
    private ToolResult.Evidence mergeEvidences(List<ToolResult.Evidence> evidences) {
        if (evidences.isEmpty()) {
            return null;
        }
        
        // 选择强度最高的证据
        ToolResult.Evidence highestStrength = evidences.stream()
            .max(Comparator.comparing(e -> STRENGTH_PRIORITY.getOrDefault(e.getStrength(), 0)))
            .orElse(evidences.get(0));
        
        // 合并引用（简化实现：使用第一个证据的引用）
        return highestStrength;
    }
    
    /**
     * 证据去重
     */
    private List<ToolResult.Evidence> deduplicateEvidence(List<ToolResult.Evidence> evidenceList) {
        Map<String, ToolResult.Evidence> seenDirections = new HashMap<>();
        List<ToolResult.Evidence> deduplicated = new ArrayList<>();
        
        for (ToolResult.Evidence evidence : evidenceList) {
            String key = evidence.getReference() + "_" + evidence.getSource();
            if (!seenDirections.containsKey(key)) {
                seenDirections.put(key, evidence);
                deduplicated.add(evidence);
            } else {
                // 如果新证据强度更高，替换旧证据
                ToolResult.Evidence existing = seenDirections.get(key);
                if (STRENGTH_PRIORITY.getOrDefault(evidence.getStrength(), 0) > 
                    STRENGTH_PRIORITY.getOrDefault(existing.getStrength(), 0)) {
                    deduplicated.remove(existing);
                    seenDirections.put(key, evidence);
                    deduplicated.add(evidence);
                }
            }
        }
        
        return deduplicated;
    }
    
    /**
     * 构建证据分布统计
     */
    private Map<String, Object> buildEvidenceDistribution(List<ToolResult.Evidence> evidenceList) {
        Map<String, Long> bySource = evidenceList.stream()
            .collect(Collectors.groupingBy(ToolResult.Evidence::getSource, Collectors.counting()));
        
        Map<String, Long> byStrength = evidenceList.stream()
            .collect(Collectors.groupingBy(ToolResult.Evidence::getStrength, Collectors.counting()));
        
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("by_source", bySource);
        distribution.put("by_strength", byStrength);
        
        return distribution;
    }
    
    /**
     * 检测证据冲突
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> detectConflicts(List<ToolResult.Evidence> evidenceList, 
                                                      CDP cdp, 
                                                      List<ToolResult> toolResults) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        
        // 简化实现：检测诊断概率冲突
        // 从tool_3的结果中检测诊断概率差异
        for (ToolResult toolResult : toolResults) {
            if ("tool_3".equals(toolResult.getToolId()) && toolResult.isSuccess()) {
                Map<String, Object> payload = toolResult.getPayload();
                if (payload != null && payload.containsKey("ddx_rank_list")) {
                    List<Map<String, Object>> ddxList = (List<Map<String, Object>>) payload.get("ddx_rank_list");
                    if (ddxList != null && ddxList.size() > 1) {
                        // 检测概率差异
                        for (int i = 0; i < ddxList.size() - 1; i++) {
                            Map<String, Object> ddx1 = ddxList.get(i);
                            Map<String, Object> ddx2 = ddxList.get(i + 1);
                            
                            Double prob1 = ((Number) ddx1.getOrDefault("probability", 0.0)).doubleValue();
                            Double prob2 = ((Number) ddx2.getOrDefault("probability", 0.0)).doubleValue();
                            
                            if (Math.abs(prob1 - prob2) > 0.3) {
                                Map<String, Object> conflict = new HashMap<>();
                                conflict.put("conflict_type", "diagnosis_probability");
                                conflict.put("conflicting_values", Arrays.asList(
                                    Map.of("tool_id", "tool_3", "value", prob1),
                                    Map.of("tool_id", "tool_3", "value", prob2)
                                ));
                                conflict.put("severity", Math.abs(prob1 - prob2) > 0.5 ? "high" : "medium");
                                conflicts.add(conflict);
                            }
                        }
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * 融合结果
     */
    @lombok.Data
    @lombok.Builder
    public static class FusionResult {
        /**
         * 融合后的证据视图
         */
        private Map<String, Object> fusedEvidenceView;
        
        /**
         * 证据冲突列表
         */
        private List<Map<String, Object>> conflicts;
    }
}

