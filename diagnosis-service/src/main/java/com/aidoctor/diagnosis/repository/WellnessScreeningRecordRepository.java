package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.WellnessScreeningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 健康筛查记录数据访问接口
 */
@Repository
public interface WellnessScreeningRecordRepository extends JpaRepository<WellnessScreeningRecord, Long> {
}

