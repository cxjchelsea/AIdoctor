package com.aidoctor.trace.repository;

import com.aidoctor.trace.entity.ExecutionTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    
    /**
     * 查询所有唯一的 CDP ID 列表，按最新时间戳排序
     * 使用 GROUP BY 和子查询来避免 DISTINCT + ORDER BY 的兼容性问题
     */
    @Query(value = "SELECT e.cdp_id FROM execution_trace e " +
            "WHERE e.cdp_id IS NOT NULL " +
            "GROUP BY e.cdp_id " +
            "ORDER BY MAX(e.event_timestamp) DESC", nativeQuery = true)
    List<String> findAllDistinctCdpIds();
    
    /**
     * 查询指定 CDP ID 的最新追踪记录时间戳
     */
    @Query("SELECT MAX(e.timestamp) FROM ExecutionTrace e WHERE e.cdpId = ?1")
    LocalDateTime findLatestTimestampByCdpId(String cdpId);
    
    /**
     * 查询所有追踪记录，按ID升序排列
     * 用于数据库管理工具查看时保证顺序
     */
    List<ExecutionTrace> findAllByOrderByIdAsc();
    
    /**
     * 查询所有追踪记录，按ID降序排列
     * 用于查看最新插入的记录
     */
    List<ExecutionTrace> findAllByOrderByIdDesc();
    
    /**
     * 根据CDP ID查找所有追踪记录，按ID升序排列
     */
    List<ExecutionTrace> findByCdpIdOrderByIdAsc(String cdpId);
}


