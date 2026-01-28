package com.aidoctor.diagnosis.service.cdp;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.exception.CDPNotFoundException;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * CDP管理器
 * 负责CDP的创建、更新、版本控制等核心功能
 * 
 * 参考文档：
 * - 《AI医生系统-数据模型设计.md》
 * - 《AI医生系统-技术架构设计-CDP数据与状态管理.md》
 */
@Slf4j
@Service
public class CDPManager {
    
    @Autowired
    private CDPRepository cdpRepository;
    
    @Autowired
    private CDPVersionService cdpVersionService;
    
    /**
     * 创建新的CDP
     * 
     * @param patientId 患者ID
     * @param sessionId 会话ID
     * @return 创建的CDP
     */
    @Transactional
    public CDP createCDP(String patientId, String sessionId) {
        log.info("创建CDP: patientId={}, sessionId={}", patientId, sessionId);
        
        String cdpId = generateCDPId();
        
        CDP cdp = CDP.builder()
            .id(cdpId)
            .patientId(patientId)
            .sessionId(sessionId)
            .version(1)
            .cdpStatus("initial")
            .build();
        
        // 使用setter初始化JSON字段（setter会自动转换为JSON字符串）
        cdp.setPatientState(new HashMap<>());
        cdp.setDdx(new ArrayList<>());
        cdp.setEvidenceGraph(new ArrayList<>());
        cdp.setWorkupPlan(new ArrayList<>());
        cdp.setManagementPlan(new ArrayList<>());
        cdp.setUncertainty(new HashMap<>());
        cdp.setAudit(new HashMap<>());
        
        CDP savedCDP = cdpRepository.save(cdp);
        
        // 创建初始版本
        cdpVersionService.createInitialVersion(savedCDP);
        
        log.info("CDP创建成功: cdpId={}", cdpId);
        return savedCDP;
    }
    
    /**
     * 更新CDP（使用悲观锁防止并发冲突）
     * 
     * 性能优化：使用SELECT FOR UPDATE确保同一时间只有一个线程能更新CDP
     * 解决execution-trace-service并发更新导致的CDPNotFoundException问题
     * 
     * 注意：如果CDP刚创建，事务可能还没提交，需要重试
     * 
     * @param cdpId CDP ID
     * @param updates 更新内容
     * @return 更新后的CDP
     */
    @Transactional
    public CDP updateCDP(String cdpId, Map<String, Object> updates) {
        log.info("更新CDP: cdpId={}", cdpId);
        
        // 使用悲观锁获取CDP，防止并发更新冲突
        // SELECT FOR UPDATE会锁定该行，其他事务必须等待
        // 如果CDP刚创建，事务可能还没提交，需要重试
        CDP existingCDP = null;
        int maxRetries = 3;
        int retryDelayMs = 200;
        
        for (int i = 0; i < maxRetries; i++) {
            Optional<CDP> cdpOpt = cdpRepository.findByIdWithLock(cdpId);
            if (cdpOpt.isPresent()) {
                existingCDP = cdpOpt.get();
                break;
            }
            
            // 如果找不到且不是最后一次重试，等待后重试（可能是事务还没提交）
            if (i < maxRetries - 1) {
                log.debug("CDP未找到，等待事务提交后重试: cdpId={}, retry={}/{}", cdpId, i + 1, maxRetries);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待CDP时被中断", e);
                }
            }
        }
        
        // 如果重试后还是找不到，抛出异常
        if (existingCDP == null) {
            throw new CDPNotFoundException("CDP not found: " + cdpId);
        }
        
        // 保存当前版本（写时复制）
        cdpVersionService.createVersion(existingCDP);
        
        // 应用更新
        applyUpdates(existingCDP, updates);
        
        // 版本号自增
        existingCDP.setVersion(existingCDP.getVersion() + 1);
        
        CDP updatedCDP = cdpRepository.save(existingCDP);
        
        log.info("CDP更新成功: cdpId={}, version={}", cdpId, updatedCDP.getVersion());
        return updatedCDP;
    }
    
    /**
     * 根据ID获取CDP
     * 
     * @param cdpId CDP ID
     * @return CDP
     */
    public Optional<CDP> getCDPById(String cdpId) {
        return cdpRepository.findById(cdpId);
    }
    
    /**
     * 获取所有CDP
     * 
     * @return CDP列表
     */
    public List<CDP> getAllCDPs() {
        return cdpRepository.findAll();
    }
    
    /**
     * 根据患者ID获取CDP列表
     * 
     * @param patientId 患者ID
     * @return CDP列表
     */
    public List<CDP> getCDPsByPatientId(String patientId) {
        return cdpRepository.findByPatientId(patientId);
    }
    
    /**
     * 根据会话ID获取CDP
     * 
     * @param sessionId 会话ID
     * @return CDP
     */
    public Optional<CDP> getCDPBySessionId(String sessionId) {
        return cdpRepository.findBySessionId(sessionId);
    }
    
    /**
     * 删除CDP
     * 
     * @param cdpId CDP ID
     */
    @Transactional
    public void deleteCDP(String cdpId) {
        log.info("删除CDP: cdpId={}", cdpId);
        cdpRepository.deleteById(cdpId);
    }
    
    /**
     * 生成CDP ID
     * 
     * @return CDP ID
     */
    private String generateCDPId() {
        return "cdp_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 应用更新到CDP
     * 
     * @param cdp CDP对象
     * @param updates 更新内容
     */
    @SuppressWarnings("unchecked")
    private void applyUpdates(CDP cdp, Map<String, Object> updates) {
        if (updates.containsKey("healthStateAssessment")) {
            Object value = updates.get("healthStateAssessment");
            if (value instanceof Map) {
                cdp.setHealthStateAssessment((Map<String, Object>) value);
            }
        }
        if (updates.containsKey("wellnessPlan")) {
            Object value = updates.get("wellnessPlan");
            if (value instanceof Map) {
                cdp.setWellnessPlan((Map<String, Object>) value);
            }
        }
        if (updates.containsKey("patientState")) {
            Object value = updates.get("patientState");
            if (value instanceof Map) {
                cdp.setPatientState((Map<String, Object>) value);
            }
        }
        if (updates.containsKey("ddx")) {
            Object value = updates.get("ddx");
            if (value instanceof List) {
                cdp.setDdx((List<Map<String, Object>>) value);
            }
        }
        if (updates.containsKey("evidenceGraph")) {
            Object value = updates.get("evidenceGraph");
            if (value instanceof List) {
                cdp.setEvidenceGraph((List<Map<String, Object>>) value);
            }
        }
        if (updates.containsKey("workupPlan")) {
            Object value = updates.get("workupPlan");
            if (value instanceof List) {
                cdp.setWorkupPlan((List<Map<String, Object>>) value);
            }
        }
        if (updates.containsKey("managementPlan")) {
            Object value = updates.get("managementPlan");
            if (value instanceof List) {
                cdp.setManagementPlan((List<Map<String, Object>>) value);
            }
        }
        if (updates.containsKey("triage")) {
            Object value = updates.get("triage");
            if (value instanceof Map) {
                cdp.setTriage((Map<String, Object>) value);
            }
        }
        if (updates.containsKey("uncertainty")) {
            Object value = updates.get("uncertainty");
            if (value instanceof Map) {
                cdp.setUncertainty((Map<String, Object>) value);
            }
        }
        if (updates.containsKey("cdpStatus")) {
            cdp.setCdpStatus((String) updates.get("cdpStatus"));
        }
        if (updates.containsKey("executionTrace")) {
            Object value = updates.get("executionTrace");
            if (value instanceof Map) {
                cdp.setExecutionTrace((Map<String, Object>) value);
            }
        }
    }
}

