package com.aidoctor.diagnosis.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 诊断记录实体
 */
@TypeDef(name = "json", typeClass = JsonType.class)
@Entity
@Table(name = "diagnosis_record", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_family_id", columnList = "family_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRecord {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    /**
     * 家庭ID（可选）
     */
    @Column(name = "family_id", length = 64)
    private String familyId;
    
    /**
     * CDP ID（关联CDP）
     * 新增字段：关联临床决策包
     */
    @Column(name = "cdp_id", length = 64)
    private String cdpId;
    
    /**
     * 工作态
     * wellness_mode: 健康管理态
     * clinical_mode: 临床诊疗态
     */
    @Column(name = "work_mode", length = 32)
    private String workMode;
    
    /**
     * 诊断类型
     */
    @Column(name = "diagnosis_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DiagnosisType diagnosisType;
    
    /**
     * 诊断状态
     */
    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private DiagnosisStatus status;
    
    // ========== 症状信息 ==========
    
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "symptom_duration", length = 64)
    private String symptomDuration;
    
    @Column(name = "symptom_severity")
    private Integer symptomSeverity;
    
    @Column(name = "symptom_frequency", length = 32)
    private String symptomFrequency;
    
    @Column(name = "symptom_location", columnDefinition = "TEXT")
    private String symptomLocation;
    
    @Column(name = "accompanying_symptoms", columnDefinition = "TEXT")
    private String accompanyingSymptoms;
    
    @Type(type = "json")
    @Column(name = "symptom_features", columnDefinition = "json")
    private Map<String, Object> symptomFeatures;
    
    // ========== 体征数据 ==========
    
    @Type(type = "json")
    @Column(name = "vital_signs", columnDefinition = "json")
    private Map<String, Object> vitalSigns;
    
    @Type(type = "json")
    @Column(name = "physical_exam", columnDefinition = "json")
    private Map<String, Object> physicalExam;
    
    // ========== 检查结果 ==========
    
    @Type(type = "json")
    @Column(name = "examination_results", columnDefinition = "json")
    private List<Map<String, Object>> examinationResults;
    
    // ========== 诊断结果 ==========
    
    @Type(type = "json")
    @Column(name = "diagnosis_result", columnDefinition = "json")
    private Map<String, Object> diagnosisResult;
    
    // ========== 对话记录 ==========
    
    @Type(type = "json")
    @Column(name = "dialogue_history", columnDefinition = "json")
    private List<Map<String, Object>> dialogueHistory;
    
    @Column(name = "questioning_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer questioningCount;
    
    // ========== 元数据 ==========
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 诊断类型枚举
     */
    public enum DiagnosisType {
        SYMPTOM,        // 症状诊断
        EXAMINATION,    // 检查诊断
        COMPREHENSIVE   // 综合诊断
    }
    
    /**
     * 诊断状态枚举
     */
    public enum DiagnosisStatus {
        COLLECTING,     // 信息收集中
        QUESTIONING,    // 追问中
        ANALYZING,      // 分析中
        COMPLETED,      // 已完成
        CANCELLED       // 已取消
    }
}

