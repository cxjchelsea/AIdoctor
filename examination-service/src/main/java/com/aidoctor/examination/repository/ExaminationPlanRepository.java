package com.aidoctor.examination.repository;

import com.aidoctor.examination.entity.ExaminationPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 检查方案Repository
 */
@Repository
public interface ExaminationPlanRepository extends JpaRepository<ExaminationPlan, Long> {
    
    /**
     * 根据用户ID查询检查方案
     */
    List<ExaminationPlan> findByUserIdOrderByCreatedAtDesc(String userId);
}

