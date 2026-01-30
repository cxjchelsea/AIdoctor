package com.aidoctor.examination.repository;

import com.aidoctor.examination.entity.ExaminationRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 检查记录Repository
 */
@Repository
public interface ExaminationRecordRepository extends JpaRepository<ExaminationRecord, Long> {
    
    /**
     * 根据用户ID查询检查记录（按创建时间倒序）
     */
    Page<ExaminationRecord> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * 根据用户ID和检查类型查询检查记录
     */
    Page<ExaminationRecord> findByUserIdAndExaminationTypeOrderByCreatedAtDesc(
            String userId, 
            ExaminationRecord.ExaminationType examinationType, 
            Pageable pageable);
}

