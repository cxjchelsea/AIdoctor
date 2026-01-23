package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.FollowUpPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 随访计划数据访问接口
 */
@Repository
public interface FollowUpPlanRepository extends JpaRepository<FollowUpPlan, Long> {
}

