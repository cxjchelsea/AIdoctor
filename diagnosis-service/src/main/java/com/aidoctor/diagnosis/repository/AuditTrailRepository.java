package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuditTrail Repository
 * 审计轨迹数据访问层
 * 
 * 参考文档：
 * - 《6.数据模型设计/AuditTrail数据结构设计.md》
 */
@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, String> {
    
    /**
     * 根据CDP ID查找审计记录（按时间排序）
     * 
     * @param cdpId CDP ID
     * @return 审计记录列表
     */
    List<AuditTrail> findByCdpIdOrderByTimestampAsc(String cdpId);
    
    /**
     * 根据会话ID查找审计记录（按时间排序）
     * 
     * @param sessionId 会话ID
     * @return 审计记录列表
     */
    List<AuditTrail> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    /**
     * 根据事件类型查找审计记录（按时间排序）
     * 
     * @param eventType 事件类型（tool_call/cdp_update/agent_decision）
     * @return 审计记录列表
     */
    List<AuditTrail> findByEventTypeOrderByTimestampAsc(String eventType);
    
    /**
     * 根据CDP ID和事件类型查找审计记录（按时间排序）
     * 
     * @param cdpId CDP ID
     * @param eventType 事件类型
     * @return 审计记录列表
     */
    List<AuditTrail> findByCdpIdAndEventTypeOrderByTimestampAsc(String cdpId, String eventType);
    
    /**
     * 根据时间范围查找审计记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计记录列表
     */
    @Query("SELECT a FROM AuditTrail a WHERE a.timestamp >= :startTime AND a.timestamp <= :endTime ORDER BY a.timestamp ASC")
    List<AuditTrail> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据工具ID查找工具调用记录（通过tool_call JSON字段中的tool_id）
     * 注意：此方法需要数据库支持JSON查询，如果数据库不支持，需要在应用层过滤
     * 
     * @param toolId 工具ID
     * @return 审计记录列表
     */
    @Query("SELECT a FROM AuditTrail a WHERE a.eventType = 'tool_call' AND a.toolCall LIKE %:toolId% ORDER BY a.timestamp ASC")
    List<AuditTrail> findByToolId(@Param("toolId") String toolId);
}

