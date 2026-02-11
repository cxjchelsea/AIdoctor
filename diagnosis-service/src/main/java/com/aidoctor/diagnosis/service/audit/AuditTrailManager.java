package com.aidoctor.diagnosis.service.audit;

import com.aidoctor.diagnosis.entity.AuditTrail;
import com.aidoctor.diagnosis.repository.AuditTrailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AuditTrail管理器
 * 负责审计轨迹的创建、查询等操作
 * 
 * 参考文档：
 * - 《6.数据模型设计/AuditTrail数据结构设计.md》
 * - 《2.架构设计/主Agent架构设计.md》
 */
@Slf4j
@Service
public class AuditTrailManager {
    
    @Autowired
    private AuditTrailRepository auditTrailRepository;
    
    /**
     * 记录工具调用事件
     * 
     * @param cdpId CDP ID
     * @param sessionId 会话ID
     * @param toolCallData 工具调用数据（包含tool_id、trace_id、input、output、evidence等）
     */
    @Transactional
    public void recordToolCall(String cdpId, String sessionId, Map<String, Object> toolCallData) {
        log.debug("记录工具调用事件: cdpId={}, sessionId={}, toolId={}", 
            cdpId, sessionId, toolCallData.get("tool_id"));
        
        AuditTrail auditTrail = AuditTrail.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .cdpId(cdpId)
            .sessionId(sessionId)
            .timestamp(LocalDateTime.now())
            .eventType("tool_call")
            .build();
        
        auditTrail.setToolCallMap(toolCallData);
        
        auditTrailRepository.save(auditTrail);
        log.debug("工具调用事件记录成功: auditTrailId={}", auditTrail.getId());
    }
    
    /**
     * 记录CDP更新事件
     * 
     * @param cdpId CDP ID
     * @param sessionId 会话ID
     * @param cdpUpdateData CDP更新数据（包含from_version、to_version、changed_fields、reason等）
     */
    @Transactional
    public void recordCDPUpdate(String cdpId, String sessionId, Map<String, Object> cdpUpdateData) {
        log.debug("记录CDP更新事件: cdpId={}, sessionId={}, fromVersion={}, toVersion={}", 
            cdpId, sessionId, cdpUpdateData.get("from_version"), cdpUpdateData.get("to_version"));
        
        AuditTrail auditTrail = AuditTrail.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .cdpId(cdpId)
            .sessionId(sessionId)
            .timestamp(LocalDateTime.now())
            .eventType("cdp_update")
            .build();
        
        auditTrail.setCdpUpdateMap(cdpUpdateData);
        
        auditTrailRepository.save(auditTrail);
        log.debug("CDP更新事件记录成功: auditTrailId={}", auditTrail.getId());
    }
    
    /**
     * 记录主Agent决策事件
     * 
     * @param cdpId CDP ID
     * @param sessionId 会话ID
     * @param agentDecisionData 主Agent决策数据（包含decision_type、reason、evidence_fusion等）
     */
    @Transactional
    public void recordAgentDecision(String cdpId, String sessionId, Map<String, Object> agentDecisionData) {
        log.debug("记录主Agent决策事件: cdpId={}, sessionId={}, decisionType={}", 
            cdpId, sessionId, agentDecisionData.get("decision_type"));
        
        AuditTrail auditTrail = AuditTrail.builder()
            .id(UUID.randomUUID().toString().replace("-", ""))
            .cdpId(cdpId)
            .sessionId(sessionId)
            .timestamp(LocalDateTime.now())
            .eventType("agent_decision")
            .build();
        
        auditTrail.setAgentDecisionMap(agentDecisionData);
        
        auditTrailRepository.save(auditTrail);
        log.debug("主Agent决策事件记录成功: auditTrailId={}", auditTrail.getId());
    }
    
    /**
     * 获取CDP的审计记录
     * 
     * @param cdpId CDP ID
     * @return 审计记录列表
     */
    public List<AuditTrail> getAuditTrailByCdpId(String cdpId) {
        return auditTrailRepository.findByCdpIdOrderByTimestampAsc(cdpId);
    }
    
    /**
     * 获取会话的审计记录
     * 
     * @param sessionId 会话ID
     * @return 审计记录列表
     */
    public List<AuditTrail> getAuditTrailBySessionId(String sessionId) {
        return auditTrailRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
    
    /**
     * 根据事件类型获取审计记录
     * 
     * @param eventType 事件类型（tool_call/cdp_update/agent_decision）
     * @return 审计记录列表
     */
    public List<AuditTrail> getAuditTrailByEventType(String eventType) {
        return auditTrailRepository.findByEventTypeOrderByTimestampAsc(eventType);
    }
    
    /**
     * 根据CDP ID和事件类型获取审计记录
     * 
     * @param cdpId CDP ID
     * @param eventType 事件类型
     * @return 审计记录列表
     */
    public List<AuditTrail> getAuditTrailByCdpIdAndEventType(String cdpId, String eventType) {
        return auditTrailRepository.findByCdpIdAndEventTypeOrderByTimestampAsc(cdpId, eventType);
    }
    
    /**
     * 根据时间范围获取审计记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计记录列表
     */
    public List<AuditTrail> getAuditTrailByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return auditTrailRepository.findByTimeRange(startTime, endTime);
    }
}

