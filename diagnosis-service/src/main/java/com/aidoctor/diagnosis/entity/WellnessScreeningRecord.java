package com.aidoctor.diagnosis.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 健康筛查记录实体类
 */
@Entity
@Table(name = "wellness_screening_record")
public class WellnessScreeningRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id")
    private Long patientId;
    
    @Column(name = "demand_type")
    private String demandType;
    
    @Column(name = "health_profile", columnDefinition = "LONGTEXT")
    private String healthProfile;
    
    @Column(name = "screening_result", columnDefinition = "LONGTEXT")
    private String screeningResult;
    
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
    
    public String getDemandType() {
        return demandType;
    }
    
    public void setDemandType(String demandType) {
        this.demandType = demandType;
    }
    
    public String getHealthProfile() {
        return healthProfile;
    }
    
    public void setHealthProfile(String healthProfile) {
        this.healthProfile = healthProfile;
    }
    
    public String getScreeningResult() {
        return screeningResult;
    }
    
    public void setScreeningResult(String screeningResult) {
        this.screeningResult = screeningResult;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

