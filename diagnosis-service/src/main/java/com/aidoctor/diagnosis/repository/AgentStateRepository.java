package com.aidoctor.diagnosis.repository;

import com.aidoctor.diagnosis.entity.AgentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AgentState Repository
 * 主Agent策略状态数据访问层
 * 
 * 参考文档：
 * - 《6.数据模型设计/AgentState数据结构设计.md》
 */
@Repository
public interface AgentStateRepository extends JpaRepository<AgentState, String> {
    
    /**
     * 根据会话ID查找AgentState
     * 
     * @param sessionId 会话ID
     * @return AgentState
     */
    Optional<AgentState> findBySessionId(String sessionId);
    
    /**
     * 根据CDP ID查找AgentState
     * 
     * @param cdpId CDP ID
     * @return AgentState
     */
    Optional<AgentState> findByCdpId(String cdpId);
    
    /**
     * 根据会话ID删除AgentState
     * 
     * @param sessionId 会话ID
     */
    void deleteBySessionId(String sessionId);
}

