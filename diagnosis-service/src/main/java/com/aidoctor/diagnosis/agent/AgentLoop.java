package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.dto.tool.ToolContext;
import com.aidoctor.diagnosis.dto.tool.ToolResult;
import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.exception.ClinicalSemanticBlockerException;
import com.aidoctor.diagnosis.service.agent_state.AgentStateManager;
import com.aidoctor.diagnosis.service.audit.AuditTrailManager;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.util.CDPFieldWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 主Agent运行循环
 * 实现Observe→Plan→Act→Update→Evaluate循环
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent运行循环设计.md》
 * - 《2.架构设计/主Agent架构设计.md》
 */
@Slf4j
@Component
public class AgentLoop {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private AgentStateManager agentStateManager;
    
    @Autowired
    private AuditTrailManager auditTrailManager;
    
    @Autowired
    private ToolCaller toolCaller;
    
    @Autowired
    private EvidenceFusion evidenceFusion;
    
    @Autowired
    private ConflictResolution conflictResolution;
    
    @Autowired
    private StopConditionEvaluator stopConditionEvaluator;
    
    @Autowired
    private EscalationHandler escalationHandler;
    
    @Autowired
    private RefusalHandler refusalHandler;
    
    @Autowired
    private CDPFieldWriter cdpFieldWriter;
    
    /**
     * 运行Agent循环
     * 
     * @param cdp 初始CDP
     * @param agentState 初始AgentState
     * @return 运行结果
     */
    public LoopResult runLoop(CDP cdp, AgentState agentState) {
        log.info("开始运行Agent循环: cdpId={}, sessionId={}", cdp.getId(), agentState.getSessionId());
        
        int loopCount = 0;
        int maxLoops = 100; // 防止无限循环
        
        while (loopCount < maxLoops) {
            loopCount++;
            log.debug("Agent循环第{}次迭代: cdpId={}", loopCount, cdp.getId());
            
            // 1. Observe（观察）
            CDPState cdpState = observe(cdp, agentState);
            
            // 2. Plan（规划）
            ToolCallPlan plan = plan(cdpState, agentState);
            
            // 如果计划为空（预算超限等），触发升级或拒答
            if (plan.getToolList().isEmpty()) {
                if (plan.getAction() != null && "escalate".equals(plan.getAction())) {
                    EscalationHandler.EscalationResult escalationResult = escalationHandler.evaluateEscalation(cdp, agentState);
                    if (escalationResult.isShouldEscalate()) {
                        Map<String, Object> escalationResponse = escalationHandler.executeEscalation(cdp, agentState, escalationResult);
                        return LoopResult.builder()
                            .decisionType("escalate")
                            .result(escalationResponse)
                            .loopCount(loopCount)
                            .build();
                    }
                }
                continue;
            }
            
            // 3. Act（执行）
            List<ToolResult> toolResults = act(plan, cdp, agentState);
            
            // 4. Update（更新）
            update(cdp, agentState, toolResults);
            
            // 5. Evaluate（评估）
            Decision decision = evaluate(cdp, agentState);
            
            // 6. Stop/Escalate/Continue
            if ("stop".equals(decision.getType())) {
                log.info("Agent循环停止: cdpId={}, loopCount={}, reason={}", 
                    cdp.getId(), loopCount, decision.getReason());
                return LoopResult.builder()
                    .decisionType("stop")
                    .result(decision.getResult())
                    .loopCount(loopCount)
                    .build();
            } else if ("escalate".equals(decision.getType())) {
                log.info("Agent循环升级: cdpId={}, loopCount={}, reason={}", 
                    cdp.getId(), loopCount, decision.getReason());
                return LoopResult.builder()
                    .decisionType("escalate")
                    .result(decision.getResult())
                    .loopCount(loopCount)
                    .build();
            } else if ("refuse".equals(decision.getType())) {
                log.info("Agent循环拒答: cdpId={}, loopCount={}, reason={}", 
                    cdp.getId(), loopCount, decision.getReason());
                return LoopResult.builder()
                    .decisionType("refuse")
                    .result(decision.getResult())
                    .loopCount(loopCount)
                    .build();
            }
            
            // Continue：继续循环
            log.debug("Agent循环继续: cdpId={}, loopCount={}", cdp.getId(), loopCount);
        }
        
        // 达到最大循环次数
        log.warn("Agent循环达到最大循环次数: cdpId={}, maxLoops={}", cdp.getId(), maxLoops);
        return LoopResult.builder()
            .decisionType("escalate")
            .result(Collections.singletonMap("reason", "达到最大循环次数，触发升级"))
            .loopCount(loopCount)
            .build();
    }
    
    /**
     * Observe（观察）
     */
    private CDPState observe(CDP cdp, AgentState agentState) {
        log.debug("Observe步骤: cdpId={}", cdp.getId());
        
        // 读取CDP状态
        Map<String, Object> cdpStateData = new HashMap<>();
        cdpStateData.put("cdp_id", cdp.getId());
        cdpStateData.put("cdp_version", cdp.getVersion());
        cdpStateData.put("cdp_status", cdp.getCdpStatus());
        
        // 识别信息缺口（详细分析）
        Map<String, Object> uncertaintyMap = cdp.getUncertainty();
        List<Object> missingInfo = new ArrayList<>();
        List<Object> criticalMissingInfo = new ArrayList<>();
        if (uncertaintyMap != null) {
            Object missingInfoObj = uncertaintyMap.get("missing_critical_info");
            if (missingInfoObj instanceof List) {
                missingInfo = (List<Object>) missingInfoObj;
                // 分析关键信息缺失
                for (Object item : missingInfo) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (Boolean.TRUE.equals(itemMap.get("critical"))) {
                            criticalMissingInfo.add(item);
                        }
                    }
                }
            }
        }
        
        // 识别证据冲突
        Map<String, Object> evidenceFusionState = agentState.getEvidenceFusionStateMap();
        List<Map<String, Object>> conflicts = new ArrayList<>();
        if (evidenceFusionState != null) {
            Object conflictsObj = evidenceFusionState.get("conflicts");
            if (conflictsObj instanceof List) {
                conflicts = (List<Map<String, Object>>) conflictsObj;
            }
        }
        
        // 识别风险信号
        Map<String, Object> triageMap = cdp.getTriage();
        List<Object> redFlags = new ArrayList<>();
        if (triageMap != null) {
            Object redFlagsObj = triageMap.get("red_flags");
            if (redFlagsObj instanceof List) {
                redFlags = (List<Object>) redFlagsObj;
            }
        }
        
        return CDPState.builder()
            .cdpData(cdpStateData)
            .missingInfo(missingInfo)
            .criticalMissingInfo(criticalMissingInfo)
            .conflicts(conflicts)
            .redFlags(redFlags)
            .build();
    }
    
    /**
     * Plan（规划）
     */
    private ToolCallPlan plan(CDPState cdpState, AgentState agentState) {
        log.debug("Plan步骤: cdpId={}", cdpState.getCdpData().get("cdp_id"));
        
        // 1. 检查预算和约束
        if (agentStateManager.isBudgetExceeded(agentState)) {
            log.warn("预算超限，无法继续调用工具");
            return ToolCallPlan.builder()
                .toolList(new ArrayList<>())
                .action("escalate")
                .reason("预算超限")
                .build();
        }
        
        // 2. 检查动态插入策略（优先级高于默认路径）
        // 2.1 红旗优先（Red Flag Priority）
        ToolCallPlan redFlagPlan = checkRedFlagPriority(cdpState, agentState);
        if (redFlagPlan != null && !redFlagPlan.getToolList().isEmpty()) {
            log.info("触发红旗优先策略");
            return redFlagPlan;
        }
        
        // 2.2 冲突复核（Conflict Review）
        ToolCallPlan conflictPlan = checkConflictReview(cdpState, agentState);
        if (conflictPlan != null && !conflictPlan.getToolList().isEmpty()) {
            log.info("触发冲突复核策略");
            return conflictPlan;
        }
        
        // 2.3 证据不足触发检索（Insufficient Evidence Trigger Retrieval）
        ToolCallPlan evidencePlan = checkInsufficientEvidence(cdpState, agentState);
        if (evidencePlan != null && !evidencePlan.getToolList().isEmpty()) {
            log.info("触发证据不足检索策略");
            return evidencePlan;
        }
        
        // 3. 根据当前步骤和默认路径决定工具调用
        Integer currentStep = agentState.getCurrentStep();
        List<ToolCallItem> toolList = new ArrayList<>();
        
        if (currentStep == null || currentStep == 1) {
            // Step 1: 识别问题
            toolList.add(ToolCallItem.builder()
                .toolId("tool_1")
                .priority("high")
                .build());
            
            if (!cdpState.getMissingInfo().isEmpty()) {
                toolList.add(ToolCallItem.builder()
                    .toolId("tool_2")
                    .priority("medium")
                    .build());
            }
        } else if (currentStep == 2) {
            // Step 2: 构建鉴别诊断候选集
            toolList.add(ToolCallItem.builder()
                .toolId("tool_3")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_6")
                .priority("high")
                .build());
        } else if (currentStep == 3) {
            // Step 3: 组织候选集
            toolList.add(ToolCallItem.builder()
                .toolId("tool_3")
                .priority("high")
                .callParams(Collections.singletonMap("mode", "reasoning_subgroup_organization"))
                .build());
        } else if (currentStep == 4) {
            // Step 4: 采集关键证据
            toolList.add(ToolCallItem.builder()
                .toolId("tool_2")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_3")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_4")
                .priority("high")
                .build());
        } else if (currentStep == 5) {
            // Step 5: 回填证据并输出结论
            toolList.add(ToolCallItem.builder()
                .toolId("tool_4")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_3")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_5")
                .priority("high")
                .build());
            toolList.add(ToolCallItem.builder()
                .toolId("tool_7")
                .priority("medium")
                .build());
        }
        
        return ToolCallPlan.builder()
            .toolList(toolList)
            .callOrder("sequential")
            .reason("基于Step " + currentStep + "的默认诊断路径")
            .build();
    }

    ToolCallPlan checkRedFlagPriority(CDPState cdpState, AgentState agentState) {
        throw new ClinicalSemanticBlockerException(
            "AgentLoop red-flag planning policy has no deterministic implementation");
    }

    ToolCallPlan checkConflictReview(CDPState cdpState, AgentState agentState) {
        throw new ClinicalSemanticBlockerException(
            "AgentLoop conflict-review policy has no deterministic implementation");
    }

    ToolCallPlan checkInsufficientEvidence(CDPState cdpState, AgentState agentState) {
        throw new ClinicalSemanticBlockerException(
            "AgentLoop insufficient-evidence policy has no deterministic implementation");
    }
    
    /**
     * Act（执行）
     */
    private List<ToolResult> act(ToolCallPlan plan, CDP cdp, AgentState agentState) {
        log.debug("Act步骤: cdpId={}, toolCount={}", cdp.getId(), plan.getToolList().size());
        
        List<ToolResult> toolResults = new ArrayList<>();
        
        for (ToolCallItem toolItem : plan.getToolList()) {
            // 生成ToolContext
            ToolContext toolContext = ToolContext.createDefault(
                UUID.randomUUID().toString().replace("-", ""),
                cdp.getId(),
                cdp.getVersion(),
                getReadFieldsForTool(toolItem.getToolId()),
                agentState.getCurrentStep(),
                agentState.getWorkMode()
            );
            
            if (toolItem.getCallParams() != null) {
                toolContext.setCallParams(toolItem.getCallParams());
            }
            
            // 调用工具
            ToolResult toolResult = toolCaller.callTool(toolContext, toolItem.getToolId());
            toolResults.add(toolResult);
            
            // 记录工具调用
            agentStateManager.recordToolCall(agentState, toolItem.getToolId(), toolResult.getStatus());
        }
        
        return toolResults;
    }
    
    /**
     * Update（更新）
     */
    private void update(CDP cdp, AgentState agentState, List<ToolResult> toolResults) {
        log.debug("Update步骤: cdpId={}, toolResultsCount={}", cdp.getId(), toolResults.size());
        
        // 证据融合
        EvidenceFusion.FusionResult fusionResult = evidenceFusion.fuseEvidence(toolResults, cdp, agentState);
        
        // 冲突解决
        if (!fusionResult.getConflicts().isEmpty()) {
            ConflictResolution.ResolutionResult resolutionResult = 
                conflictResolution.resolveConflicts(fusionResult.getConflicts(), cdp, agentState);
            log.info("冲突解决完成: resolvedCount={}", resolutionResult.getResolutionResults().size());
        }
        
        // 决定是否写回CDP（根据suggestedWrites）
        List<String> changedFields = new ArrayList<>();
        for (ToolResult toolResult : toolResults) {
            if (toolResult.isSuccess() && toolResult.getSuggestedWrites() != null) {
                for (ToolResult.SuggestedWrite suggestedWrite : toolResult.getSuggestedWrites()) {
                    String fieldPath = suggestedWrite.getFieldPath();
                    Object value = suggestedWrite.getValue();
                    String reason = suggestedWrite.getReason();
                    
                    log.debug("写回CDP字段: fieldPath={}, reason={}", fieldPath, reason);
                    
                    // 使用CDPFieldWriter写回字段
                    boolean success = cdpFieldWriter.writeField(cdp, fieldPath, value);
                    if (success) {
                        changedFields.add(fieldPath);
                    } else {
                        log.warn("CDP字段写回失败: fieldPath={}, reason={}", fieldPath, reason);
                    }
                }
            }
        }
        
        // 如果有CDP更新，记录到AuditTrail并保存CDP
        if (!changedFields.isEmpty()) {
            String sessionId = agentState.getSessionId();
            int fromVersion = cdp.getVersion();
            
            // 保存CDP（CDP字段已经通过CDPFieldWriter直接更新到cdp对象中）
            // 注意：这里需要重新保存CDP以触发版本控制
            // 但由于CDP是JPA实体，直接修改后保存即可
            // 实际保存会在事务提交时进行，这里先记录AuditTrail
            Map<String, Object> cdpUpdateData = new HashMap<>();
            cdpUpdateData.put("from_version", fromVersion);
            cdpUpdateData.put("to_version", fromVersion + 1); // 版本会在保存时自增
            cdpUpdateData.put("changed_fields", changedFields);
            auditTrailManager.recordCDPUpdate(cdp.getId(), sessionId, cdpUpdateData);
            
            // 注意：CDP的保存应该由调用方（如ClinicalAgentBrain）在事务中统一处理
            // 这里只负责字段写回和AuditTrail记录
        }
        
        // 更新AgentState
        agentStateManager.updateAgentState(agentState);
    }
    
    /**
     * Evaluate（评估）
     */
    private Decision evaluate(CDP cdp, AgentState agentState) {
        log.debug("Evaluate步骤: cdpId={}", cdp.getId());
        
        // 评估停止条件
        StopConditionEvaluator.StopConditionResult stopResult = 
            stopConditionEvaluator.evaluate(cdp, agentState);
        
        if (stopResult.isSatisfied()) {
            return Decision.builder()
                .type("stop")
                .reason(stopResult.getEvaluationReason())
                .result(Collections.singletonMap("stop_condition_result", stopResult))
                .build();
        }
        
        // 评估升级条件
        EscalationHandler.EscalationResult escalationResult = 
            escalationHandler.evaluateEscalation(cdp, agentState);
        
        if (escalationResult.isShouldEscalate()) {
            Map<String, Object> escalationResponse = escalationHandler.executeEscalation(cdp, agentState, escalationResult);
            return Decision.builder()
                .type("escalate")
                .reason(escalationResult.getEscalationReason())
                .result(escalationResponse)
                .build();
        }
        
        // 评估拒答条件
        RefusalHandler.RefusalResult refusalResult = 
            refusalHandler.evaluateRefusal(cdp, agentState);
        
        if (refusalResult.isShouldRefuse()) {
            Map<String, Object> refusalResponse = refusalHandler.executeRefusal(cdp, agentState, refusalResult);
            return Decision.builder()
                .type("refuse")
                .reason(refusalResult.getRefusalReason())
                .result(refusalResponse)
                .build();
        }
        
        // Continue
        return Decision.builder()
            .type("continue")
            .reason("继续运行循环")
            .result(new HashMap<>())
            .build();
    }
    
    /**
     * 获取工具需要读取的CDP字段
     */
    private List<String> getReadFieldsForTool(String toolId) {
        switch (toolId) {
            case "tool_1":
                return Arrays.asList("cdp.patient_state");
            case "tool_2":
                return Arrays.asList("cdp.patient_state", "cdp.ddx", "cdp.uncertainty.missing_critical_info");
            case "tool_3":
                return Arrays.asList("cdp.patient_state", "cdp.ddx");
            case "tool_4":
                return Arrays.asList("cdp.ddx", "cdp.triage");
            case "tool_5":
                return Arrays.asList("cdp.ddx", "cdp.triage");
            case "tool_6":
                return Arrays.asList("cdp.patient_state", "cdp.ddx");
            case "tool_7":
                return Arrays.asList("cdp.ddx", "cdp.evidence_graph", "cdp.workup_plan", "cdp.management_plan");
            default:
                return new ArrayList<>();
        }
    }
    
    // ========== 内部类 ==========
    
    @lombok.Data
    @lombok.Builder
    public static class CDPState {
        private Map<String, Object> cdpData;
        private List<Object> missingInfo;
        private List<Object> criticalMissingInfo; // 关键信息缺失列表
        private List<Map<String, Object>> conflicts;
        private List<Object> redFlags;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ToolCallPlan {
        private List<ToolCallItem> toolList;
        private String callOrder;
        private String reason;
        private String action; // escalate/refuse等
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ToolCallItem {
        private String toolId;
        private String priority;
        private Map<String, Object> callParams;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class Decision {
        private String type; // stop/escalate/refuse/continue
        private String reason;
        private Map<String, Object> result;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LoopResult {
        private String decisionType;
        private Map<String, Object> result;
        private int loopCount;
    }
}

