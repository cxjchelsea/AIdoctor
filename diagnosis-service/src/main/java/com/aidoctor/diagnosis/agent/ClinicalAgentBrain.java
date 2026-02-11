package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.agent_state.AgentStateManager;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.service.audit.AuditTrailManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 主Agent主类（Clinical Agent Brain）
 * 协调运行循环和各个组件
 * 
 * 参考文档：
 * - 《2.架构设计/主Agent架构设计.md》
 * - 《5.主agent设计/主Agent运行循环设计.md》
 */
@Slf4j
@Service
public class ClinicalAgentBrain {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private AgentStateManager agentStateManager;
    
    @Autowired
    private AuditTrailManager auditTrailManager;
    
    @Autowired
    private AgentLoop agentLoop;
    
    /**
     * 运行主Agent
     * 
     * @param cdpId CDP ID
     * @return 运行结果
     */
    public Map<String, Object> runAgent(String cdpId) {
        log.info("启动主Agent: cdpId={}", cdpId);
        
        // 获取CDP和AgentState
        CDP cdp = cdpManager.getCDP(cdpId)
            .orElseThrow(() -> new RuntimeException("CDP不存在: " + cdpId));
        
        AgentState agentState = agentStateManager.getAgentStateByCdpId(cdpId)
            .orElseGet(() -> {
                log.warn("AgentState不存在，创建新的AgentState: cdpId={}", cdpId);
                return agentStateManager.createAgentState(cdp.getSessionId(), cdpId);
            });
        
        // 记录主Agent决策（开始）
        recordAgentDecision(cdpId, cdp.getSessionId(), "start", "主Agent开始运行");
        
        // 运行Agent循环
        AgentLoop.LoopResult loopResult = agentLoop.runLoop(cdp, agentState);
        
        // 记录主Agent决策（结束）
        recordAgentDecision(cdpId, cdp.getSessionId(), loopResult.getDecisionType(), 
            "主Agent运行完成: " + loopResult.getDecisionType());
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("decision_type", loopResult.getDecisionType());
        result.put("result", loopResult.getResult());
        result.put("loop_count", loopResult.getLoopCount());
        result.put("cdp_id", cdpId);
        result.put("cdp_version", cdp.getVersion());
        
        log.info("主Agent运行完成: cdpId={}, decisionType={}, loopCount={}", 
            cdpId, loopResult.getDecisionType(), loopResult.getLoopCount());
        
        return result;
    }
    
    /**
     * 记录主Agent决策
     */
    private void recordAgentDecision(String cdpId, String sessionId, String decisionType, String reason) {
        try {
            Map<String, Object> agentDecisionData = new HashMap<>();
            agentDecisionData.put("decision_type", decisionType);
            agentDecisionData.put("reason", reason);
            agentDecisionData.put("timestamp", System.currentTimeMillis());
            
            auditTrailManager.recordAgentDecision(cdpId, sessionId, agentDecisionData);
        } catch (Exception e) {
            log.error("记录主Agent决策失败: cdpId={}, decisionType={}, error={}", 
                cdpId, decisionType, e.getMessage(), e);
        }
    }
}

