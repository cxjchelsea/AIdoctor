package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.CDP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * CDP数据访问接口
 */
@Repository
public interface CDPRepository extends JpaRepository<CDP, String> {
    
    /**
     * 根据患者ID查询CDP列表
     * 
     * @param patientId 患者ID
     * @return CDP列表
     */
    List<CDP> findByPatientId(String patientId);
    
    /**
     * 根据会话ID查询CDP
     * 
     * @param sessionId 会话ID
     * @return CDP
     */
    Optional<CDP> findBySessionId(String sessionId);
    
    /**
     * 使用悲观锁查询CDP（用于并发更新场景）
     * SELECT FOR UPDATE，确保同一时间只有一个事务能更新CDP
     * 
     * @param cdpId CDP ID
     * @return CDP
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CDP c WHERE c.id = :cdpId")
    Optional<CDP> findByIdWithLock(@Param("cdpId") String cdpId);
    
    /**
     * 查询所有CDP，按创建时间升序排列
     * 用于数据库管理工具查看时保证顺序
     */
    List<CDP> findAllByOrderByCreatedAtAsc();
    
    /**
     * 查询所有CDP，按创建时间降序排列
     * 用于查看最新创建的CDP
     */
    List<CDP> findAllByOrderByCreatedAtDesc();
    
    /**
     * 根据患者ID查询CDP列表，按创建时间降序排列
     */
    List<CDP> findByPatientIdOrderByCreatedAtDesc(String patientId);
}

