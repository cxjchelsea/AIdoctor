package com.aidoctor.trace.repository;

import com.aidoctor.trace.entity.ExecutionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行追踪Repository
 */
@Repository
public interface ExecutionTraceRepository extends JpaRepository<ExecutionTrace, Long> {
    
    List<ExecutionTrace> findByCdpIdOrderByTimestampAsc(String cdpId);
    
    List<ExecutionTrace> findByCdpIdAndTimestampBetweenOrderByTimestampAsc(
        String cdpId, LocalDateTime startTime, LocalDateTime endTime);
    
    long countByCdpId(String cdpId);
    
    long countByCdpIdAndStatus(String cdpId, String status);
    
    List<ExecutionTrace> findByTimestampBefore(LocalDateTime cutoff);
}


