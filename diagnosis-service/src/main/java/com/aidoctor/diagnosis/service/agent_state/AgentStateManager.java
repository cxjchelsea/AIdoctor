package com.aidoctor.diagnosis.service.agent_state;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.repository.AgentStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AgentState管理器
 * 负责AgentState的创建、更新、查询等操作
 * 
 * 参考文档：
 * - 《6.数据模型设计/AgentState数据结构设计.md》
 * - 《2.架构设计/主Agent架构设计.md》
 */
@Slf4j
@Service
public class AgentStateManager {
    
    @Autowired
    private AgentStateRepository agentStateRepository;
    
    /**
     * 创建AgentState
     * 
     * @param sessionId 会话ID
     * @param cdpId CDP ID
     * @return AgentState
     */
    @Transactional
    public AgentState createAgentState(String sessionId, String cdpId) {
        log.info("创建AgentState: sessionId={}, cdpId={}", sessionId, cdpId);
        
        // 生成ID
        String id = UUID.randomUUID().toString().replace("-", "");
        
        // 创建AgentState，使用默认值
        AgentState agentState = AgentState.builder()
            .id(id)
            .sessionId(sessionId)
            .cdpId(cdpId)
            .currentStep(1)
            .workMode("clinical_mode")
            .build();
        
        // 设置默认阈值配置
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("confidence_threshold", 0.7);
        thresholds.put("evidence_count_threshold", 3);
        thresholds.put("information_gain_threshold", 0.5);
        agentState.setThresholdsMap(thresholds);
        
        // 设置默认预算配置
        Map<String, Object> budget = new HashMap<>();
        budget.put("max_tool_calls", 50);
        budget.put("max_time_seconds", 300);
        budget.put("max_cost", 100.0);
        budget.put("current_tool_calls", 0);
        budget.put("current_time_seconds", 0);
        budget.put("current_cost", 0.0);
        agentState.setBudgetMap(budget);
        
        // 设置默认失败回退策略
        Map<String, Object> failureBackoff = new HashMap<>();
        failureBackoff.put("max_retries", 3);
        failureBackoff.put("backoff_strategy", "exponential");
        agentState.setFailureBackoffMap(failureBackoff);
        
        // 设置默认已尝试工具列表（空列表）
        agentState.setTriedToolsList(new ArrayList<>());
        
        // 设置默认证据融合状态
        Map<String, Object> evidenceFusionState = new HashMap<>();
        evidenceFusionState.put("conflicts", new ArrayList<>());
        evidenceFusionState.put("resolution_strategy", "evidence_strength");
        agentState.setEvidenceFusionStateMap(evidenceFusionState);
        
        // 设置默认停止条件状态
        Map<String, Object> stopConditions = new HashMap<>();
        stopConditions.put("cdp_required_fields_complete", false);
        stopConditions.put("evidence_references_complete", false);
        stopConditions.put("risk_assessment_complete", false);
        stopConditions.put("diagnosis_conclusion_clear", false);
        stopConditions.put("workup_plan_complete", false);
        stopConditions.put("management_plan_complete", false);
        stopConditions.put("evidence_chain_complete", false);
        stopConditions.put("final_conclusion_generated", false);
        agentState.setStopConditionsMap(stopConditions);
        
        // 保存
        agentState = agentStateRepository.save(agentState);
        log.info("AgentState创建成功: id={}", agentState.getId());
        
        return agentState;
    }
    
    /**
     * 更新AgentState
     * 
     * @param agentState AgentState
     * @return 更新后的AgentState
     */
    @Transactional
    public AgentState updateAgentState(AgentState agentState) {
        log.debug("更新AgentState: id={}", agentState.getId());
        return agentStateRepository.save(agentState);
    }
    
    /**
     * 根据会话ID获取AgentState
     * 
     * @param sessionId 会话ID
     * @return AgentState
     */
    public Optional<AgentState> getAgentStateBySessionId(String sessionId) {
        return agentStateRepository.findBySessionId(sessionId);
    }
    
    /**
     * 根据CDP ID获取AgentState
     * 
     * @param cdpId CDP ID
     * @return AgentState
     */
    public Optional<AgentState> getAgentStateByCdpId(String cdpId) {
        return agentStateRepository.findByCdpId(cdpId);
    }
    
    /**
     * 更新当前步骤
     * 
     * @param agentState AgentState
     * @param currentStep 当前步骤（1-5）
     */
    @Transactional
    public void updateCurrentStep(AgentState agentState, Integer currentStep) {
        log.debug("更新当前步骤: agentStateId={}, currentStep={}", agentState.getId(), currentStep);
        agentState.setCurrentStep(currentStep);
        agentStateRepository.save(agentState);
    }
    
    /**
     * 更新工作态
     * 
     * @param agentState AgentState
     * @param workMode 工作态（wellness_mode/clinical_mode）
     */
    @Transactional
    public void updateWorkMode(AgentState agentState, String workMode) {
        log.debug("更新工作态: agentStateId={}, workMode={}", agentState.getId(), workMode);
        agentState.setWorkMode(workMode);
        agentStateRepository.save(agentState);
    }
    
    /**
     * 记录工具调用（更新tried_tools）
     * 
     * @param agentState AgentState
     * @param toolId 工具ID
     * @param result 调用结果（success/failure/timeout）
     */
    @Transactional
    public void recordToolCall(AgentState agentState, String toolId, String result) {
        log.debug("记录工具调用: agentStateId={}, toolId={}, result={}", agentState.getId(), toolId, result);
        
        List<Map<String, Object>> triedTools = agentState.getTriedToolsList();
        
        // 查找是否已有该工具的记录
        Map<String, Object> toolRecord = null;
        for (Map<String, Object> record : triedTools) {
            if (toolId.equals(record.get("tool_id"))) {
                toolRecord = record;
                break;
            }
        }
        
        // 如果已有记录，更新
        if (toolRecord != null) {
            Integer callCount = (Integer) toolRecord.getOrDefault("call_count", 0);
            toolRecord.put("call_count", callCount + 1);
            toolRecord.put("last_result", result);
            toolRecord.put("last_call_time", LocalDateTime.now().toString());
        } else {
            // 如果没有记录，创建新记录
            Map<String, Object> newRecord = new HashMap<>();
            newRecord.put("tool_id", toolId);
            newRecord.put("call_count", 1);
            newRecord.put("last_result", result);
            newRecord.put("last_call_time", LocalDateTime.now().toString());
            triedTools.add(newRecord);
        }
        
        agentState.setTriedToolsList(triedTools);
        
        // 更新预算：增加工具调用次数
        Map<String, Object> budget = agentState.getBudgetMap();
        Integer currentToolCalls = (Integer) budget.getOrDefault("current_tool_calls", 0);
        budget.put("current_tool_calls", currentToolCalls + 1);
        agentState.setBudgetMap(budget);
        
        agentStateRepository.save(agentState);
    }
    
    /**
     * 更新预算（更新当前资源使用）
     * 
     * @param agentState AgentState
     * @param currentTimeSeconds 当前执行时间（秒）
     * @param currentCost 当前成本
     */
    @Transactional
    public void updateBudget(AgentState agentState, Integer currentTimeSeconds, Double currentCost) {
        log.debug("更新预算: agentStateId={}, currentTimeSeconds={}, currentCost={}", 
            agentState.getId(), currentTimeSeconds, currentCost);
        
        Map<String, Object> budget = agentState.getBudgetMap();
        budget.put("current_time_seconds", currentTimeSeconds);
        budget.put("current_cost", currentCost);
        agentState.setBudgetMap(budget);
        
        agentStateRepository.save(agentState);
    }
    
    /**
     * 检查预算是否超限
     * 
     * @param agentState AgentState
     * @return 是否超限
     */
    public boolean isBudgetExceeded(AgentState agentState) {
        Map<String, Object> budget = agentState.getBudgetMap();
        
        Integer maxToolCalls = (Integer) budget.getOrDefault("max_tool_calls", 50);
        Integer currentToolCalls = (Integer) budget.getOrDefault("current_tool_calls", 0);
        
        Integer maxTimeSeconds = (Integer) budget.getOrDefault("max_time_seconds", 300);
        Integer currentTimeSeconds = (Integer) budget.getOrDefault("current_time_seconds", 0);
        
        Double maxCost = ((Number) budget.getOrDefault("max_cost", 100.0)).doubleValue();
        Double currentCost = ((Number) budget.getOrDefault("current_cost", 0.0)).doubleValue();
        
        return currentToolCalls >= maxToolCalls 
            || currentTimeSeconds >= maxTimeSeconds 
            || currentCost >= maxCost;
    }
}

