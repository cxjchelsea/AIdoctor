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
 * 检查方案实体
 */
@TypeDef(name = "json", typeClass = JsonType.class)
@Entity
@Table(name = "examination_plan", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_plan_type", columnList = "plan_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExaminationPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "plan_name", nullable = false, length = 255)
    private String planName;
    
    @Column(name = "plan_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PlanType planType;
    
    @Type(type = "json")
    @Column(name = "plan_items", columnDefinition = "json")
    private List<Map<String, Object>> planItems;
    
    @Type(type = "json")
    @Column(name = "target_conditions", columnDefinition = "json")
    private Map<String, Object> targetConditions;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum PlanType {
        ROUTINE,
        DIAGNOSTIC,
        FOLLOW_UP
    }
}

