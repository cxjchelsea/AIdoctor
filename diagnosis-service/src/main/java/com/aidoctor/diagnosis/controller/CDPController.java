package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.dto.response.ApiResponse;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.exception.CDPNotFoundException;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CDP管理控制器
 * 提供CDP相关的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diagnosis/cdp")
public class CDPController {
    
    @Autowired
    private CDPManager cdpManager;
    
    /**
     * 获取CDP数据
     * GET /api/v1/diagnosis/cdp/{cdpId}
     * 
     * 此接口供其他服务（如dialog-service）调用，用于获取CDP数据
     */
    @GetMapping("/{cdpId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCDP(@PathVariable String cdpId) {
        log.info("获取CDP数据: cdpId={}", cdpId);
        
        try {
            Optional<CDP> cdpOpt = cdpManager.getCDPById(cdpId);
            if (!cdpOpt.isPresent()) {
                log.warn("CDP不存在: cdpId={}", cdpId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "CDP不存在: " + cdpId));
            }
            
            CDP cdp = cdpOpt.get();
            Map<String, Object> cdpData = convertCDPToMap(cdp);
            
            return ResponseEntity.ok(ApiResponse.success(cdpData));
            
        } catch (Exception e) {
            log.error("获取CDP数据失败: cdpId={}", cdpId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "获取CDP数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 将CDP实体转换为Map格式
     */
    private Map<String, Object> convertCDPToMap(CDP cdp) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", cdp.getId());
        map.put("patientId", cdp.getPatientId());
        map.put("sessionId", cdp.getSessionId());
        map.put("version", cdp.getVersion());
        map.put("cdpStatus", cdp.getCdpStatus());
        map.put("healthStateAssessment", cdp.getHealthStateAssessment());
        map.put("wellnessPlan", cdp.getWellnessPlan());
        map.put("patientState", cdp.getPatientState());
        map.put("ddx", cdp.getDdx());
        map.put("evidenceGraph", cdp.getEvidenceGraph());
        map.put("workupPlan", cdp.getWorkupPlan());
        map.put("managementPlan", cdp.getManagementPlan());
        map.put("triage", cdp.getTriage());
        map.put("uncertainty", cdp.getUncertainty());
        map.put("audit", cdp.getAudit());
        map.put("executionTrace", cdp.getExecutionTrace());
        map.put("createdAt", cdp.getCreatedAt());
        map.put("updatedAt", cdp.getUpdatedAt());
        return map;
    }
    
    /**
     * 更新CDP数据
     * POST /api/v1/diagnosis/cdp/{cdpId}/update
     * 
     * 此接口供其他服务（如dialog-service）调用，用于更新CDP数据
     */
    @PostMapping("/{cdpId}/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCDP(
            @PathVariable String cdpId,
            @RequestBody Map<String, Object> request) {
        log.info("更新CDP数据: cdpId={}", cdpId);
        
        try {
            // 获取patient_state更新数据
            Map<String, Object> patientState = (Map<String, Object>) request.get("patient_state");
            if (patientState == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "patient_state不能为空"));
            }
            
            // 获取当前CDP
            Optional<CDP> cdpOpt = cdpManager.getCDPById(cdpId);
            if (!cdpOpt.isPresent()) {
                log.warn("CDP不存在: cdpId={}", cdpId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "CDP不存在: " + cdpId));
            }
            
            CDP cdp = cdpOpt.get();
            Map<String, Object> currentPatientState = cdp.getPatientState();
            if (currentPatientState == null) {
                currentPatientState = new HashMap<>();
            }
            
            // 合并patient_state更新（特别是将extracted_info合并到symptoms数组中）
            mergePatientState(currentPatientState, patientState);
            
            // 更新CDP
            Map<String, Object> updates = new HashMap<>();
            updates.put("patientState", currentPatientState);
            cdp = cdpManager.updateCDP(cdpId, updates);
            
            Map<String, Object> cdpData = convertCDPToMap(cdp);
            log.info("CDP更新成功: cdpId={}", cdpId);
            return ResponseEntity.ok(ApiResponse.success(cdpData));
            
        } catch (CDPNotFoundException e) {
            log.warn("更新CDP失败: CDP不存在: cdpId={}", cdpId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "CDP不存在: " + e.getMessage()));
        } catch (Exception e) {
            log.error("更新CDP失败: cdpId={}", cdpId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 合并patient_state更新
     * 将dialog-service的extracted_info合并到symptoms数组中
     */
    private void mergePatientState(Map<String, Object> currentPatientState, Map<String, Object> updates) {
        // 合并基本字段
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过symptom_*字段，这些需要合并到symptoms数组中
            if (key.startsWith("symptom_")) {
                continue;
            }
            
            // 合并其他字段
            currentPatientState.put(key, value);
        }
        
        // 处理symptom_*字段，合并到symptoms数组中
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> symptoms = (java.util.List<Map<String, Object>>) 
            currentPatientState.getOrDefault("symptoms", new java.util.ArrayList<>());
        
        if (symptoms.isEmpty()) {
            // 如果没有symptoms，创建一个新的
            Map<String, Object> newSymptom = new HashMap<>();
            newSymptom.put("name", currentPatientState.getOrDefault("chiefComplaint", ""));
            symptoms.add(newSymptom);
        }
        
        // 更新第一个symptom（通常是最主要的症状）
        Map<String, Object> firstSymptom = symptoms.get(0);
        
        // 合并extracted_info到symptom
        if (updates.containsKey("symptom_duration")) {
            firstSymptom.put("duration", updates.get("symptom_duration"));
        }
        if (updates.containsKey("symptom_severity")) {
            Object severity = updates.get("symptom_severity");
            // 如果是数字，直接使用；如果是字符串，尝试转换
            if (severity instanceof Number) {
                firstSymptom.put("severity", ((Number) severity).intValue());
            } else if (severity instanceof String) {
                try {
                    firstSymptom.put("severity", Integer.parseInt((String) severity));
                } catch (NumberFormatException e) {
                    // 如果不是数字，保持原值
                    firstSymptom.put("severity", severity);
                }
            } else {
                firstSymptom.put("severity", severity);
            }
        }
        if (updates.containsKey("symptom_location")) {
            firstSymptom.put("location", updates.get("symptom_location"));
        }
        if (updates.containsKey("symptom_trigger")) {
            firstSymptom.put("trigger", updates.get("symptom_trigger"));
        }
        if (updates.containsKey("symptom_frequency")) {
            firstSymptom.put("frequency", updates.get("symptom_frequency"));
        }
        if (updates.containsKey("symptom_relief")) {
            firstSymptom.put("relief", updates.get("symptom_relief"));
        }
        if (updates.containsKey("accompanying_symptoms")) {
            firstSymptom.put("accompanyingSymptoms", updates.get("accompanying_symptoms"));
        }
        
        currentPatientState.put("symptoms", symptoms);
    }
    
    /**
     * 更新CDP的执行追踪摘要
     * POST /api/v1/diagnosis/cdp/update-trace-summary
     * 
     * 此接口供execution-trace-service调用，用于更新CDP中的执行追踪摘要
     */
    @PostMapping("/update-trace-summary")
    public ResponseEntity<ApiResponse<Void>> updateTraceSummary(
            @RequestBody Map<String, Object> request) {
        String cdpId = (String) request.get("cdpId");
        Map<String, Object> executionTrace = (Map<String, Object>) request.get("executionTrace");
        
        log.info("更新CDP追踪摘要: cdpId={}", cdpId);
        
        if (cdpId == null || executionTrace == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "cdpId和executionTrace不能为空"));
        }
        
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("executionTrace", executionTrace);
            cdpManager.updateCDP(cdpId, updates);
            
            log.info("CDP追踪摘要更新成功: cdpId={}", cdpId);
            return ResponseEntity.ok(ApiResponse.success(null));
            
        } catch (CDPNotFoundException e) {
            // CDP不存在是正常情况（可能已被删除），返回404而不是500
            log.warn("更新CDP追踪摘要失败: CDP不存在: cdpId={}", cdpId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "CDP不存在: " + e.getMessage()));
        } catch (Exception e) {
            // 检查异常消息中是否包含"CDP不存在"或"CDP not found"
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("CDP不存在") || errorMessage.contains("CDP not found"))) {
                // CDP不存在是正常情况（可能已被删除或尚未创建），返回404而不是500
                log.warn("更新CDP追踪摘要失败: CDP不存在: cdpId={}", cdpId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "CDP不存在: " + errorMessage));
            }
            log.error("更新CDP追踪摘要失败: cdpId={}", cdpId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "更新失败: " + errorMessage));
        }
    }
}

