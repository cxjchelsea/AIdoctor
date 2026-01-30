package com.aidoctor.examination.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检查记录实体
 */
@TypeDef(name = "json", typeClass = JsonType.class)
@Entity
@Table(name = "examination_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "family_id", length = 64)
    private String familyId;
    
    @Column(name = "examination_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private ExaminationType examinationType;
    
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "plan_name", length = 255)
    private String planName;
    
    @Type(type = "json")
    @Column(name = "plan_items", columnDefinition = "json")
    private List<Map<String, Object>> planItems;
    
    @Column(name = "report_type", length = 32)
    private String reportType;
    
    @Column(name = "report_file_path", length = 512)
    private String reportFilePath;
    
    @Type(type = "json")
    @Column(name = "report_ocr_result", columnDefinition = "json")
    private Map<String, Object> reportOcrResult;
    
    @Type(type = "json")
    @Column(name = "report_structured_data", columnDefinition = "json")
    private Map<String, Object> reportStructuredData;
    
    @Type(type = "json")
    @Column(name = "interpretation_result", columnDefinition = "json")
    private Map<String, Object> interpretationResult;
    
    @Column(name = "ocr_status", length = 32)
    private String ocrStatus;  // OCR识别状态：pending/completed/failed
    
    @Column(name = "examination_date")
    private LocalDate examinationDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum ExaminationType {
        BLOOD_TEST,
        IMAGING,
        PHYSICAL,
        COMPREHENSIVE
    }
}

