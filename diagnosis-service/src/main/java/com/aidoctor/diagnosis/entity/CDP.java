package com.aidoctor.diagnosis.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CDP（Clinical Decision Package）实体
 * 临床决策包 - 系统的核心数据结构
 * 
 * 参考文档：
 * - 《AI医生系统-数据模型设计.md》
 * - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
 */
@Entity
@Table(name = "cdp", indexes = {
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_version", columnList = "version"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
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
    @Column(name = "version", nullable = false)
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
     * 健康状态判定结果（JSON格式）
     * 脑区0的输出
     * 包含：工作态判定（wellness_mode/clinical_mode）、风险等级、入口判定流程结果等
     * 对应功能设计文档：2.0节 健康状态判定
     */
    @Type(type = "json")
    @Column(name = "health_state_assessment", columnDefinition = "json")
    private Map<String, Object> healthStateAssessment;
    
    /**
     * 健康管理计划（JSON格式）
     * 健康管理态使用
     * 包含：风险管理、生活方式建议、随访计划、健康筛查路径（A路径）执行结果等
     * 对应功能设计文档：3.2节 健康管理态详细流程
     */
    @Type(type = "json")
    @Column(name = "wellness_plan", columnDefinition = "json")
    private Map<String, Object> wellnessPlan;
    
    /**
     * 患者状态（JSON格式）
     * 包含：症状、体征、背景信息等
     */
    @Type(type = "json")
    @Column(name = "patient_state", columnDefinition = "json")
    private Map<String, Object> patientState;
    
    /**
     * 鉴别诊断列表（三层排序：首要假设/主要备选/必须排除）
     */
    @Type(type = "json")
    @Column(name = "ddx", columnDefinition = "json")
    private List<Map<String, Object>> ddx;
    
    /**
     * 证据图（JSON格式）
     * 包含：支持证据、反对证据、证据链等
     */
    @Type(type = "json")
    @Column(name = "evidence_graph", columnDefinition = "json")
    private List<Map<String, Object>> evidenceGraph;
    
    /**
     * 检查计划（JSON格式）
     * 脑区D的输出
     */
    @Type(type = "json")
    @Column(name = "workup_plan", columnDefinition = "json")
    private List<Map<String, Object>> workupPlan;
    
    /**
     * 治疗计划（JSON格式）
     * 脑区E的输出
     */
    @Type(type = "json")
    @Column(name = "management_plan", columnDefinition = "json")
    private List<Map<String, Object>> managementPlan;
    
    /**
     * 风险评估（JSON格式）
     * 脑区F的输出
     */
    @Type(type = "json")
    @Column(name = "triage", columnDefinition = "json")
    private Map<String, Object> triage;
    
    /**
     * 不确定性信息（JSON格式）
     * 包含：缺失信息、冲突证据、不确定性来源等
     */
    @Type(type = "json")
    @Column(name = "uncertainty", columnDefinition = "json")
    private Map<String, Object> uncertainty;
    
    /**
     * 审计信息（JSON格式）
     * 包含：创建者、修改者、操作记录等
     */
    @Type(type = "json")
    @Column(name = "audit", columnDefinition = "json")
    private Map<String, Object> audit;
    
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
}

