package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.dto.request.DiagnosisRequest;
import com.aidoctor.diagnosis.dto.request.UserAnswer;
import com.aidoctor.diagnosis.dto.response.ApiResponse;
import com.aidoctor.diagnosis.dto.response.DiagnosisResponse;
import com.aidoctor.diagnosis.service.DiagnosisOrchestrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 诊断控制器
 * 
 * 参考文档：
 * - 《AI医生系统-API接口规范.md》
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/diagnosis")
@Validated
public class DiagnosisController {
    
    @Autowired
    private DiagnosisOrchestrationService diagnosisOrchestrationService;
    
    /**
     * 启动诊断流程
     * POST /api/v1/diagnosis/start
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> startDiagnosis(
            @Valid @RequestBody DiagnosisRequest request) {
        log.info("启动诊断流程请求: userId={}", request.getUserId());
        
        DiagnosisResponse response = diagnosisOrchestrationService.startDiagnosis(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 继续诊断流程（提供用户回答）
     * POST /api/v1/diagnosis/continue
     */
    @PostMapping("/continue")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> continueDiagnosis(
            @Valid @RequestBody UserAnswer answer) {
        log.info("继续诊断流程: cdpId={}", answer.getCdpId());
        
        DiagnosisResponse response = diagnosisOrchestrationService.continueDiagnosis(answer);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取诊断状态
     * GET /api/v1/diagnosis/{cdpId}/status
     */
    @GetMapping("/{cdpId}/status")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> getDiagnosisStatus(
            @PathVariable String cdpId) {
        log.info("获取诊断状态: cdpId={}", cdpId);
        
        DiagnosisResponse response = diagnosisOrchestrationService.getDiagnosisStatus(cdpId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取诊断结果
     * GET /api/v1/diagnosis/{cdpId}/result
     */
    @GetMapping("/{cdpId}/result")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> getDiagnosisResult(
            @PathVariable String cdpId) {
        log.info("获取诊断结果: cdpId={}", cdpId);
        
        DiagnosisResponse response = diagnosisOrchestrationService.getDiagnosisResult(cdpId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 回答追问问题（兼容旧接口）
     * POST /api/v1/diagnosis/{diagnosisId}/answer
     */
    @PostMapping("/{diagnosisId}/answer")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> answerQuestion(
            @PathVariable String diagnosisId,
            @Valid @RequestBody UserAnswer answer) {
        log.info("回答追问: diagnosisId={}", diagnosisId);
        
        // 兼容旧接口：将diagnosisId设置为cdpId
        if (answer.getCdpId() == null) {
            answer.setCdpId(diagnosisId);
        }
        
        DiagnosisResponse response = diagnosisOrchestrationService.continueDiagnosis(answer);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

