package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.CDP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

