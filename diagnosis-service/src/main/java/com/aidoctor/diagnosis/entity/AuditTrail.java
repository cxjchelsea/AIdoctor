package com.aidoctor.diagnosis.entity;

import com.aidoctor.diagnosis.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * AuditTrail实体
 * 审计轨迹 - 记录所有工具调用、CDP更新、主Agent决策的完整信息
 * 
 * 参考文档：
 * - 《6.数据模型设计/AuditTrail数据结构设计.md》
 * - 《2.架构设计/主Agent架构设计.md》
 * 
 * 注意：JSON字段使用String + CLOB存储，在应用层使用Jackson进行序列化/反序列化
 */
@Entity
@Table(name = "audit_trail", indexes = {
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {
    
    /**
     * AuditTrail ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * CDP ID
     */
    @Column(name = "cdp_id", length = 64, nullable = false)
    private String cdpId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * 时间戳
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * 事件类型（tool_call/cdp_update/agent_decision）
     */
    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;
    
    /**
     * 工具调用记录（JSON格式，存储为CLOB字符串）
     * 当event_type=tool_call时使用
     */
    @Lob
    @Column(name = "tool_call", columnDefinition = "CLOB")
    private String toolCall;
    
    /**
     * CDP更新记录（JSON格式，存储为CLOB字符串）
     * 当event_type=cdp_update时使用
     */
    @Lob
    @Column(name = "cdp_update", columnDefinition = "CLOB")
    private String cdpUpdate;
    
    /**
     * 主Agent决策记录（JSON格式，存储为CLOB字符串）
     * 当event_type=agent_decision时使用
     */
    @Lob
    @Column(name = "agent_decision", columnDefinition = "CLOB")
    private String agentDecision;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // ========== 便捷方法：JSON字段的getter/setter ==========
    
    /**
     * 获取工具调用记录（Map格式）
     */
    public Map<String, Object> getToolCallMap() {
        return JsonUtil.jsonToMap(toolCall);
    }
    
    /**
     * 设置工具调用记录（Map格式）
     */
    public void setToolCallMap(Map<String, Object> toolCallMap) {
        this.toolCall = JsonUtil.mapToJson(toolCallMap);
    }
    
    /**
     * 获取CDP更新记录（Map格式）
     */
    public Map<String, Object> getCdpUpdateMap() {
        return JsonUtil.jsonToMap(cdpUpdate);
    }
    
    /**
     * 设置CDP更新记录（Map格式）
     */
    public void setCdpUpdateMap(Map<String, Object> cdpUpdateMap) {
        this.cdpUpdate = JsonUtil.mapToJson(cdpUpdateMap);
    }
    
    /**
     * 获取主Agent决策记录（Map格式）
     */
    public Map<String, Object> getAgentDecisionMap() {
        return JsonUtil.jsonToMap(agentDecision);
    }
    
    /**
     * 设置主Agent决策记录（Map格式）
     */
    public void setAgentDecisionMap(Map<String, Object> agentDecisionMap) {
        this.agentDecision = JsonUtil.mapToJson(agentDecisionMap);
    }
}

