package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.ExaminationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 检查记录Repository
 */
@Repository
public interface ExaminationRecordRepository extends JpaRepository<ExaminationRecord, Long> {
    
    List<ExaminationRecord> findByUserId(String userId);
    
    Page<ExaminationRecord> findByUserId(String userId, Pageable pageable);
    
    List<ExaminationRecord> findByUserIdAndExaminationType(String userId, ExaminationRecord.ExaminationType type);
}

