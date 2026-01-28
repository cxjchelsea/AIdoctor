package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.dto.response.ApiResponse;
import com.aidoctor.diagnosis.exception.CDPNotFoundException;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CDP管理控制器
 * 提供CDP相关的管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cdp")
public class CDPController {
    
    @Autowired
    private CDPManager cdpManager;
    
    /**
     * 更新CDP的执行追踪摘要
     * POST /api/v1/cdp/update-trace-summary
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

