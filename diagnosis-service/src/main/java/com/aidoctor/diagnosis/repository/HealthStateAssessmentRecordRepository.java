package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.HealthStateAssessmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 健康状态判定记录数据访问接口
 */
@Repository
public interface HealthStateAssessmentRecordRepository extends JpaRepository<HealthStateAssessmentRecord, Long> {
}

