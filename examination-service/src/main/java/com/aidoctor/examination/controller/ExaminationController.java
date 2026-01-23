package com.aidoctor.examination.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 检查控制器
 */
@RestController
@RequestMapping("/api/v1/examination")
public class ExaminationController {
    
    /**
     * 上传检查报告
     */
    @PostMapping("/upload")
    public ResponseEntity<Object> uploadReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam String userId,
            @RequestParam String examinationType) {
        // TODO: 实现上传逻辑
        return ResponseEntity.ok(null);
    }
    
    /**
     * OCR识别报告
     */
    @PostMapping("/ocr")
    public ResponseEntity<Object> ocrRecognize(@RequestPart("file") MultipartFile file) {
        // TODO: 实现OCR识别逻辑
        return ResponseEntity.ok(null);
    }
    
    /**
     * 获取检查历史
     */
    @GetMapping("/history")
    public ResponseEntity<Object> getHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        // TODO: 实现获取历史逻辑
        return ResponseEntity.ok(null);
    }
}

