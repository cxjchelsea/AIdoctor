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
 * CDP（Clinical Decision Package）实体
 * 临床决策包 - 系统的核心数据结构
 * 
 * 参考文档：
 * - 《AI医生系统-数据模型设计.md》
 * - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
 * 
 * 注意：JSON字段使用String + CLOB存储，在应用层使用Jackson进行序列化/反序列化
 * getter方法返回Map/List格式，setter方法接受Map/List格式并自动转换为JSON字符串
 */
@Entity
@Table(name = "cdp", indexes = {
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_version", columnList = "version_no"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CDP {
    
    /**
     * CDP ID（主键）
     */
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    
    /**
     * 患者ID
     */
    @Column(name = "patient_id", length = 64, nullable = false)
    private String patientId;
    
    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;
    
    /**
     * 版本号（支持版本控制）
     */
    @Column(name = "version_no", nullable = false)
    private Integer version;

    /**
     * CDP状态（与《AI医生系统-技术架构设计-CDP数据与状态管理.md》7.3对齐）
     *
     * - initial：CDP刚创建，等待健康状态判定
     * - wellness_mode：健康管理态（A路径）
     * - clinical_mode_collecting：临床诊疗态-信息收集
     * - clinical_mode_diagnosing：临床诊疗态-诊断中
     * - clinical_mode_managing：临床诊疗态-处置中
     * - completed：诊断流程完成（已输出终点结论包）
     * - follow_up：随访状态
     */
    @Column(name = "cdp_status", length = 64)
    private String cdpStatus;
    
    /**
     * 健康状态判定结果（JSON格式，存储为CLOB字符串）
     * tool_0的输出
     * 包含：工作态判定（wellness_mode/clinical_mode）、风险等级、入口判定流程结果等
     * 对应功能设计文档：2.0节 健康状态判定
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "health_state_assessment", columnDefinition = "CLOB")
    private String healthStateAssessment;
    
    /**
     * 健康管理计划（JSON格式，存储为CLOB字符串）
     * 健康管理态使用
     * 包含：风险管理、生活方式建议、随访计划、健康筛查路径（A路径）执行结果等
     * 对应功能设计文档：3.2节 健康管理态详细流程
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "wellness_plan", columnDefinition = "CLOB")
    private String wellnessPlan;
    
    /**
     * 患者状态（JSON格式，存储为CLOB字符串）
     * 包含：症状、体征、背景信息等
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "patient_state", columnDefinition = "CLOB")
    private String patientState;
    
    /**
     * 鉴别诊断列表（JSON格式，存储为CLOB字符串）
     * 三层排序：首要假设/主要备选/必须排除
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "ddx", columnDefinition = "CLOB")
    private String ddx;
    
    /**
     * 证据图（JSON格式，存储为CLOB字符串）
     * 包含：支持证据、反对证据、证据链等
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "evidence_graph", columnDefinition = "CLOB")
    private String evidenceGraph;
    
    /**
     * 检查计划（JSON格式，存储为CLOB字符串）
     * tool_4的输出
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "workup_plan", columnDefinition = "CLOB")
    private String workupPlan;
    
    /**
     * 治疗计划（JSON格式，存储为CLOB字符串）
     * tool_5的输出
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "management_plan", columnDefinition = "CLOB")
    private String managementPlan;
    
    /**
     * 风险评估（JSON格式，存储为CLOB字符串）
     * tool_6的输出
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "triage", columnDefinition = "CLOB")
    private String triage;
    
    /**
     * 不确定性信息（JSON格式，存储为CLOB字符串）
     * 包含：缺失信息、冲突证据、不确定性来源等
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "uncertainty", columnDefinition = "CLOB")
    private String uncertainty;
    
    /**
     * 审计信息（JSON格式，存储为CLOB字符串）
     * 包含：创建者、修改者、操作记录等
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "audit_info", columnDefinition = "CLOB")
    private String audit;
    
    /**
     * 执行追踪摘要（JSON格式，存储为CLOB字符串）
     * 存储执行路径的摘要信息，用于快速查询和可视化
     * 包含：执行步骤、调用的服务列表、执行时间线等
     * 注意：在应用层使用Jackson进行序列化/反序列化
     */
    @Lob
    @Column(name = "execution_trace", columnDefinition = "CLOB")
    private String executionTrace;
    
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
    
    // ========== JSON字段的getter/setter：自动处理序列化/反序列化 ==========
    // 这些方法覆盖Lombok生成的getter/setter，提供Map/List接口，内部自动转换为JSON字符串
    
    /**
     * 获取健康状态判定结果（Map格式）
     * 内部自动将JSON字符串转换为Map
     */
    public Map<String, Object> getHealthStateAssessment() {
        return JsonUtil.jsonToMap(healthStateAssessment);
    }
    
    /**
     * 设置健康状态判定结果
     * 自动将Map转换为JSON字符串存储
     */
    public void setHealthStateAssessment(Map<String, Object> map) {
        this.healthStateAssessment = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取健康管理计划（Map格式）
     */
    public Map<String, Object> getWellnessPlan() {
        return JsonUtil.jsonToMap(wellnessPlan);
    }
    
    /**
     * 设置健康管理计划
     */
    public void setWellnessPlan(Map<String, Object> map) {
        this.wellnessPlan = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取患者状态（Map格式）
     */
    public Map<String, Object> getPatientState() {
        return JsonUtil.jsonToMap(patientState);
    }
    
    /**
     * 设置患者状态
     */
    public void setPatientState(Map<String, Object> map) {
        this.patientState = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取鉴别诊断列表（List格式）
     */
    public List<Map<String, Object>> getDdx() {
        return JsonUtil.jsonToList(ddx);
    }
    
    /**
     * 设置鉴别诊断列表
     */
    public void setDdx(List<Map<String, Object>> list) {
        this.ddx = JsonUtil.listToJson(list);
    }
    
    /**
     * 获取证据图（List格式）
     */
    public List<Map<String, Object>> getEvidenceGraph() {
        return JsonUtil.jsonToList(evidenceGraph);
    }
    
    /**
     * 设置证据图
     */
    public void setEvidenceGraph(List<Map<String, Object>> list) {
        this.evidenceGraph = JsonUtil.listToJson(list);
    }
    
    /**
     * 获取检查计划（List格式）
     */
    public List<Map<String, Object>> getWorkupPlan() {
        return JsonUtil.jsonToList(workupPlan);
    }
    
    /**
     * 设置检查计划
     */
    public void setWorkupPlan(List<Map<String, Object>> list) {
        this.workupPlan = JsonUtil.listToJson(list);
    }
    
    /**
     * 获取治疗计划（List格式）
     */
    public List<Map<String, Object>> getManagementPlan() {
        return JsonUtil.jsonToList(managementPlan);
    }
    
    /**
     * 设置治疗计划
     */
    public void setManagementPlan(List<Map<String, Object>> list) {
        this.managementPlan = JsonUtil.listToJson(list);
    }
    
    /**
     * 获取风险评估（Map格式）
     */
    public Map<String, Object> getTriage() {
        return JsonUtil.jsonToMap(triage);
    }
    
    /**
     * 设置风险评估
     */
    public void setTriage(Map<String, Object> map) {
        this.triage = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取不确定性信息（Map格式）
     */
    public Map<String, Object> getUncertainty() {
        return JsonUtil.jsonToMap(uncertainty);
    }
    
    /**
     * 设置不确定性信息
     */
    public void setUncertainty(Map<String, Object> map) {
        this.uncertainty = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取审计信息（Map格式）
     */
    public Map<String, Object> getAudit() {
        return JsonUtil.jsonToMap(audit);
    }
    
    /**
     * 设置审计信息
     */
    public void setAudit(Map<String, Object> map) {
        this.audit = JsonUtil.mapToJson(map);
    }
    
    /**
     * 获取执行追踪摘要（Map格式）
     */
    public Map<String, Object> getExecutionTrace() {
        return JsonUtil.jsonToMap(executionTrace);
    }
    
    /**
     * 设置执行追踪摘要
     */
    public void setExecutionTrace(Map<String, Object> map) {
        this.executionTrace = JsonUtil.mapToJson(map);
    }
}

