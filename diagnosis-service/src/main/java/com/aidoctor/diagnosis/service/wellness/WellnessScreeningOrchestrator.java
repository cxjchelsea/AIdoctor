package com.aidoctor.diagnosis.service.wellness;

import com.aidoctor.diagnosis.annotation.TraceExecution;
import com.aidoctor.diagnosis.client.HealthStateAssessmentClient;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.aidoctor.diagnosis.util.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康筛查流程编排服务
 * 负责编排健康筛查流程（A路径，A1-A5）
 * 
 * 参考文档：
 * - 《AI医生系统-业务逻辑详细设计.md》第3.2节
 * - 《AI医生系统-技术架构设计.md》
 */
@Slf4j
@Service
public class WellnessScreeningOrchestrator {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private HealthStateAssessmentClient healthStateAssessmentClient;
    
    @Autowired(required = false)
    private com.aidoctor.diagnosis.client.TraceServiceClient traceServiceClient;
    
    /**
     * 执行健康筛查流程（A路径）
     * 
     * @param cdp CDP对象
     * @return 更新后的CDP
     */
    @TraceExecution(service = "diagnosis-service", module = "wellness-screening")
    @Transactional
    public CDP executeWellnessScreening(CDP cdp) {
        // 设置追踪上下文
        TraceContext.setCdpId(cdp.getId());
        
        log.info("开始执行健康筛查流程: cdpId={}", cdp.getId());
        
        try {
            // A1: 需求分类
            cdp = a1DemandClassification(cdp);
            
            // A2: 收集健康画像
            cdp = a2HealthProfileCollection(cdp);
            
            // A3: 执行分支
            cdp = a3BranchExecution(cdp);
            
            // A4: 生成统一结果
            cdp = a4UnifiedResultGeneration(cdp);
            
            // A5: 设置随访
            cdp = a5FollowUpSetup(cdp);
            
            // 生成执行追踪摘要
            Map<String, Object> executionTrace = buildExecutionTraceSummary(cdp);
            
            // 更新状态为完成，并保存执行追踪摘要
            Map<String, Object> updates = new HashMap<>();
            updates.put("cdpStatus", "completed");
            updates.put("executionTrace", executionTrace);
            cdp = cdpManager.updateCDP(cdp.getId(), updates);
            
            log.info("健康筛查流程执行完成: cdpId={}", cdp.getId());
            return cdp;
            
        } catch (Exception e) {
            log.error("健康筛查流程执行失败: cdpId={}", cdp.getId(), e);
            throw e;
        } finally {
            // 清理追踪上下文
            TraceContext.clear();
        }
    }
    
    /**
     * A1: 需求分类
     */
    private CDP a1DemandClassification(CDP cdp) {
        log.info("A1: 需求分类 - cdpId={}", cdp.getId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("cdpId", cdp.getId());
        request.put("userInput", extractUserInput(cdp));
        
        try {
            Object response = healthStateAssessmentClient.performDemandClassification(request);
            // 解析响应并更新wellnessPlan
            Map<String, Object> a1Result = parseA1Response(response);
            if (a1Result != null && !a1Result.isEmpty()) {
                Map<String, Object> wellnessPlan = cdp.getWellnessPlan();
                if (wellnessPlan == null) {
                    wellnessPlan = new HashMap<>();
                }
                wellnessPlan.put("demand_type", a1Result.get("demand_type"));
                wellnessPlan.put("a1_result", a1Result);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("wellnessPlan", wellnessPlan);
                cdp = cdpManager.updateCDP(cdp.getId(), updates);
            }
        } catch (Exception e) {
            log.error("A1需求分类失败", e);
            throw new RuntimeException("A1需求分类失败", e);
        }
        
        return cdp;
    }
    
    /**
     * A2: 收集健康画像
     */
    private CDP a2HealthProfileCollection(CDP cdp) {
        log.info("A2: 收集健康画像 - cdpId={}", cdp.getId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("cdpId", cdp.getId());
        request.put("patientState", cdp.getPatientState());
        
        try {
            Object response = healthStateAssessmentClient.collectHealthProfile(request);
            // 解析响应并更新patientState和wellnessPlan
            Map<String, Object> a2Result = parseA2Response(response);
            if (a2Result != null && !a2Result.isEmpty()) {
                Map<String, Object> wellnessPlan = cdp.getWellnessPlan();
                if (wellnessPlan == null) {
                    wellnessPlan = new HashMap<>();
                }
                wellnessPlan.put("profile", a2Result.get("profile"));
                wellnessPlan.put("completeness", a2Result.get("completeness"));
                wellnessPlan.put("a2_result", a2Result);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("wellnessPlan", wellnessPlan);
                // 如果profile中有patientState信息，也更新patientState
                if (a2Result.get("profile") instanceof Map) {
                    Map<String, Object> profile = (Map<String, Object>) a2Result.get("profile");
                    Map<String, Object> patientState = cdp.getPatientState();
                    if (patientState == null) {
                        patientState = new HashMap<>();
                    }
                    patientState.putAll(profile);
                    updates.put("patientState", patientState);
                }
                cdp = cdpManager.updateCDP(cdp.getId(), updates);
            }
        } catch (Exception e) {
            log.error("A2收集健康画像失败", e);
            throw new RuntimeException("A2收集健康画像失败", e);
        }
        
        return cdp;
    }
    
    /**
     * A3: 执行分支
     */
    private CDP a3BranchExecution(CDP cdp) {
        log.info("A3: 执行分支 - cdpId={}", cdp.getId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("cdpId", cdp.getId());
        request.put("wellnessPlan", cdp.getWellnessPlan());
        
        try {
            Object response = healthStateAssessmentClient.executeBranch(request);
            // 解析响应并更新wellnessPlan
            Map<String, Object> a3Result = parseA3Response(response);
            if (a3Result != null && !a3Result.isEmpty()) {
                Map<String, Object> wellnessPlan = cdp.getWellnessPlan();
                if (wellnessPlan == null) {
                    wellnessPlan = new HashMap<>();
                }
                wellnessPlan.put("branch", a3Result.get("branch"));
                wellnessPlan.put("branch_result", a3Result.get("branch_result"));
                wellnessPlan.put("a3_result", a3Result);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("wellnessPlan", wellnessPlan);
                cdp = cdpManager.updateCDP(cdp.getId(), updates);
            }
        } catch (Exception e) {
            log.error("A3执行分支失败", e);
            throw new RuntimeException("A3执行分支失败", e);
        }
        
        return cdp;
    }
    
    /**
     * A4: 生成统一结果
     */
    private CDP a4UnifiedResultGeneration(CDP cdp) {
        log.info("A4: 生成统一结果 - cdpId={}", cdp.getId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("cdpId", cdp.getId());
        request.put("wellnessPlan", cdp.getWellnessPlan());
        
        try {
            Object response = healthStateAssessmentClient.generateUnifiedResult(request);
            // 解析响应并更新wellnessPlan
            Map<String, Object> a4Result = parseA4Response(response);
            if (a4Result != null && !a4Result.isEmpty()) {
                Map<String, Object> wellnessPlan = cdp.getWellnessPlan();
                if (wellnessPlan == null) {
                    wellnessPlan = new HashMap<>();
                }
                wellnessPlan.put("unified_result", a4Result.get("result"));
                wellnessPlan.put("summary", a4Result.get("summary"));
                wellnessPlan.put("a4_result", a4Result);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("wellnessPlan", wellnessPlan);
                cdp = cdpManager.updateCDP(cdp.getId(), updates);
            }
        } catch (Exception e) {
            log.error("A4生成统一结果失败", e);
            throw new RuntimeException("A4生成统一结果失败", e);
        }
        
        return cdp;
    }
    
    /**
     * A5: 设置随访
     */
    private CDP a5FollowUpSetup(CDP cdp) {
        log.info("A5: 设置随访 - cdpId={}", cdp.getId());
        
        Map<String, Object> request = new HashMap<>();
        request.put("cdpId", cdp.getId());
        request.put("wellnessPlan", cdp.getWellnessPlan());
        
        try {
            Object response = healthStateAssessmentClient.setupFollowUp(request);
            // 解析响应并更新wellnessPlan
            Map<String, Object> a5Result = parseA5Response(response);
            if (a5Result != null && !a5Result.isEmpty()) {
                Map<String, Object> wellnessPlan = cdp.getWellnessPlan();
                if (wellnessPlan == null) {
                    wellnessPlan = new HashMap<>();
                }
                wellnessPlan.put("followup_plan", a5Result.get("followup_plan"));
                wellnessPlan.put("next_review_date", a5Result.get("next_review_date"));
                wellnessPlan.put("a5_result", a5Result);
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("wellnessPlan", wellnessPlan);
                cdp = cdpManager.updateCDP(cdp.getId(), updates);
            }
        } catch (Exception e) {
            log.error("A5设置随访失败", e);
            throw new RuntimeException("A5设置随访失败", e);
        }
        
        return cdp;
    }
    
    private String extractUserInput(CDP cdp) {
        if (cdp.getHealthStateAssessment() != null) {
            Map<String, Object> entryAssessment = (Map<String, Object>) 
                cdp.getHealthStateAssessment().get("entryAssessment");
            if (entryAssessment != null) {
                return (String) entryAssessment.get("userInput");
            }
        }
        return "";
    }
    
    // ========== 响应解析方法 ==========
    
    /**
     * 解析A1需求分类响应
     * 响应格式：{"code": 200, "message": "success", "data": {...}, "timestamp": ...}
     */
    private Map<String, Object> parseA1Response(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            log.error("解析A1响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * 解析A2收集健康画像响应
     */
    private Map<String, Object> parseA2Response(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            log.error("解析A2响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * 解析A3执行分支响应
     */
    private Map<String, Object> parseA3Response(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            log.error("解析A3响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * 解析A4生成统一结果响应
     */
    private Map<String, Object> parseA4Response(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            log.error("解析A4响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * 解析A5设置随访响应
     */
    private Map<String, Object> parseA5Response(Object response) {
        if (response == null) {
            return new HashMap<>();
        }
        
        try {
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            }
        } catch (Exception e) {
            log.error("解析A5响应失败", e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * 构建执行追踪摘要
     * 从追踪服务获取摘要信息，如果追踪服务不可用，则返回基本摘要
     */
    private Map<String, Object> buildExecutionTraceSummary(CDP cdp) {
        Map<String, Object> summary = new HashMap<>();
        
        // 如果追踪服务可用，尝试获取详细摘要
        if (traceServiceClient != null) {
            try {
                Map<String, Object> traceSummary = traceServiceClient.getTraceSummary(cdp.getId());
                if (traceSummary != null && !traceSummary.isEmpty()) {
                    summary.putAll(traceSummary);
                    log.debug("从追踪服务获取摘要成功: cdpId={}", cdp.getId());
                    return summary;
                }
            } catch (Exception e) {
                log.warn("从追踪服务获取摘要失败，使用基本摘要: cdpId={}", cdp.getId(), e);
            }
        }
        
        // 如果追踪服务不可用或获取失败，构建基本摘要
        summary.put("cdpId", cdp.getId());
        summary.put("cdpStatus", cdp.getCdpStatus());
        summary.put("version", cdp.getVersion());
        summary.put("workMode", "wellness_mode");
        summary.put("steps", java.util.Arrays.asList("A1", "A2", "A3", "A4", "A5"));
        summary.put("completed", true);
        
        log.debug("构建基本追踪摘要: cdpId={}", cdp.getId());
        return summary;
    }
}

