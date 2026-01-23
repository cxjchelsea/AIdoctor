package com.aidoctor.diagnosis.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 健康状态判定记录实体类
 */
@Entity
@Table(name = "health_state_assessment_record")
public class HealthStateAssessmentRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id")
    private Long patientId;
    
    @Column(name = "assessment_result", columnDefinition = "LONGTEXT")
    private String assessmentResult;
    
    @Column(name = "path_selection")
    private String pathSelection;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPatientId() {
        return patientId;
    }
    
    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }
    
    public String getAssessmentResult() {
        return assessmentResult;
    }
    
    public void setAssessmentResult(String assessmentResult) {
        this.assessmentResult = assessmentResult;
    }
    
    public String getPathSelection() {
        return pathSelection;
    }
    
    public void setPathSelection(String pathSelection) {
        this.pathSelection = pathSelection;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

