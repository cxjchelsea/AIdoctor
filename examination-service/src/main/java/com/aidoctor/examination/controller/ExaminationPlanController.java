package com.aidoctor.examination.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 检查方案控制器
 */
@RestController
@RequestMapping("/api/v1/examination/plan")
public class ExaminationPlanController {
    
    /**
     * 设计检查方案
     */
    @PostMapping
    public ResponseEntity<Object> designPlan(@RequestBody Object request) {
        // TODO: 实现检查方案设计逻辑
        return ResponseEntity.ok(null);
    }
}

