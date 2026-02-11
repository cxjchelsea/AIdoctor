package com.aidoctor.diagnosis.entity;

import com.aidoctor.diagnosis.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AgentState实体
 * 主Agent策略状态 - 存储主Agent的策略状态，包括阈值、预算、失败回退、已尝试工具等
 * 
 * 参考文档：
 * - 《6.数据模型设计/AgentState数据结构设计.md》
 * - 《2.架构设计/主Agent架构设计.md》
 * 
 * 注意：JSON字段使用String + CLOB存储，在应用层使用Jackson进行序列化/反序列化
 */
@Entity
@Table(name = "agent_state", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    
    /**
     * AgentState ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 会话ID（与CDP关联）
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * CDP ID
     */
    @Column(name = "cdp_id", length = 64, nullable = false)
    private String cdpId;
    
    /**
     * 当前诊断步骤（1-5）
     */
    @Column(name = "current_step")
    private Integer currentStep;
    
    /**
     * 工作态（wellness_mode/clinical_mode）
     */
    @Column(name = "work_mode", length = 32)
    private String workMode;
    
    /**
     * 阈值配置（JSON格式，存储为CLOB字符串）
     * 包含：confidence_threshold、evidence_count_threshold、information_gain_threshold
     */
    @Lob
    @Column(name = "thresholds", columnDefinition = "CLOB")
    private String thresholds;
    
    /**
     * 预算配置（JSON格式，存储为CLOB字符串）
     * 包含：max_tool_calls、max_time_seconds、max_cost、current_tool_calls、current_time_seconds、current_cost
     */
    @Lob
    @Column(name = "budget", columnDefinition = "CLOB")
    private String budget;
    
    /**
     * 失败回退策略（JSON格式，存储为CLOB字符串）
     * 包含：max_retries、backoff_strategy
     */
    @Lob
    @Column(name = "failure_backoff", columnDefinition = "CLOB")
    private String failureBackoff;
    
    /**
     * 已尝试工具列表（JSON格式，存储为CLOB字符串）
     * 包含：tool_id、call_count、last_result、last_call_time
     */
    @Lob
    @Column(name = "tried_tools", columnDefinition = "CLOB")
    private String triedTools;
    
    /**
     * 证据融合状态（JSON格式，存储为CLOB字符串）
     * 包含：conflicts、resolution_strategy
     */
    @Lob
    @Column(name = "evidence_fusion_state", columnDefinition = "CLOB")
    private String evidenceFusionState;
    
    /**
     * 停止条件状态（JSON格式，存储为CLOB字符串）
     * 包含：cdp_required_fields_complete、evidence_references_complete、risk_assessment_complete等
     */
    @Lob
    @Column(name = "stop_conditions", columnDefinition = "CLOB")
    private String stopConditions;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ========== 便捷方法：JSON字段的getter/setter ==========
    
    /**
     * 获取阈值配置（Map格式）
     */
    public Map<String, Object> getThresholdsMap() {
        return JsonUtil.jsonToMap(thresholds);
    }
    
    /**
     * 设置阈值配置（Map格式）
     */
    public void setThresholdsMap(Map<String, Object> thresholdsMap) {
        this.thresholds = JsonUtil.mapToJson(thresholdsMap);
    }
    
    /**
     * 获取预算配置（Map格式）
     */
    public Map<String, Object> getBudgetMap() {
        return JsonUtil.jsonToMap(budget);
    }
    
    /**
     * 设置预算配置（Map格式）
     */
    public void setBudgetMap(Map<String, Object> budgetMap) {
        this.budget = JsonUtil.mapToJson(budgetMap);
    }
    
    /**
     * 获取失败回退策略（Map格式）
     */
    public Map<String, Object> getFailureBackoffMap() {
        return JsonUtil.jsonToMap(failureBackoff);
    }
    
    /**
     * 设置失败回退策略（Map格式）
     */
    public void setFailureBackoffMap(Map<String, Object> failureBackoffMap) {
        this.failureBackoff = JsonUtil.mapToJson(failureBackoffMap);
    }
    
    /**
     * 获取已尝试工具列表（List格式）
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTriedToolsList() {
        if (triedTools == null || triedTools.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return JsonUtil.jsonToList(triedTools);
    }
    
    /**
     * 设置已尝试工具列表（List格式）
     */
    public void setTriedToolsList(List<Map<String, Object>> triedToolsList) {
        this.triedTools = JsonUtil.listToJson(triedToolsList);
    }
    
    /**
     * 获取证据融合状态（Map格式）
     */
    public Map<String, Object> getEvidenceFusionStateMap() {
        return JsonUtil.jsonToMap(evidenceFusionState);
    }
    
    /**
     * 设置证据融合状态（Map格式）
     */
    public void setEvidenceFusionStateMap(Map<String, Object> evidenceFusionStateMap) {
        this.evidenceFusionState = JsonUtil.mapToJson(evidenceFusionStateMap);
    }
    
    /**
     * 获取停止条件状态（Map格式）
     */
    public Map<String, Object> getStopConditionsMap() {
        return JsonUtil.jsonToMap(stopConditions);
    }
    
    /**
     * 设置停止条件状态（Map格式）
     */
    public void setStopConditionsMap(Map<String, Object> stopConditionsMap) {
        this.stopConditions = JsonUtil.mapToJson(stopConditionsMap);
    }
    
    // ========== 便捷方法：AgentState摘要（提供给工具） ==========
    
    /**
     * 获取AgentState摘要（提供给工具，通过ToolContext）
     * 只包含工具需要的信息：current_step、work_mode、constraints
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("current_step", currentStep);
        summary.put("work_mode", workMode);
        
        // 从budget中提取constraints
        Map<String, Object> budgetMap = getBudgetMap();
        Map<String, Object> constraints = new HashMap<>();
        if (budgetMap != null) {
            constraints.put("max_time_seconds", budgetMap.get("max_time_seconds"));
            constraints.put("max_cost", budgetMap.get("max_cost"));
        }
        summary.put("constraints", constraints);
        
        return summary;
    }
}

