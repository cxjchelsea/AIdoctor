package com.aidoctor.examination.controller;

import com.aidoctor.examination.dto.response.ApiResponse;
import com.aidoctor.examination.entity.ExaminationPlan;
import com.aidoctor.examination.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 检查方案控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examination/plan")
@RequiredArgsConstructor
public class ExaminationPlanController {
    
    private final ExaminationService examinationService;
    
    /**
     * 设计检查方案
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExaminationPlan>> designPlan(
            @RequestParam String userId,
            @RequestBody Map<String, Object> request) {
        
        try {
            ExaminationPlan plan = examinationService.designPlan(userId, request);
            return ResponseEntity.ok(ApiResponse.success(plan));
            
        } catch (Exception e) {
            log.error("设计检查方案失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "设计检查方案失败: " + e.getMessage()));
        }
    }
}

