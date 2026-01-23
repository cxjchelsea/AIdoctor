package com.aidoctor.diagnosis.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 随访计划实体类
 */
@Entity
@Table(name = "follow_up_plan")
public class FollowUpPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id")
    private Long patientId;
    
    @Column(name = "plan_content", columnDefinition = "LONGTEXT")
    private String planContent;
    
    @Column(name = "next_follow_up_time")
    private LocalDateTime nextFollowUpTime;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
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
    
    public String getPlanContent() {
        return planContent;
    }
    
    public void setPlanContent(String planContent) {
        this.planContent = planContent;
    }
    
    public LocalDateTime getNextFollowUpTime() {
        return nextFollowUpTime;
    }
    
    public void setNextFollowUpTime(LocalDateTime nextFollowUpTime) {
        this.nextFollowUpTime = nextFollowUpTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

