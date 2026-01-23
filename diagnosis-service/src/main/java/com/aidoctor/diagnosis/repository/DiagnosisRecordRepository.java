package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.DiagnosisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 诊断记录Repository
 */
@Repository
public interface DiagnosisRecordRepository extends JpaRepository<DiagnosisRecord, Long> {
    
    List<DiagnosisRecord> findByUserId(String userId);
    
    Page<DiagnosisRecord> findByUserId(String userId, Pageable pageable);
    
    List<DiagnosisRecord> findByUserIdAndStatus(String userId, DiagnosisRecord.DiagnosisStatus status);
    
    Optional<DiagnosisRecord> findByIdAndUserId(Long id, String userId);
}

