package com.aidoctor.examination.controller;

import com.aidoctor.examination.dto.response.ApiResponse;
import com.aidoctor.examination.entity.ExaminationRecord;
import com.aidoctor.examination.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examination")
@RequiredArgsConstructor
public class ExaminationController {
    
    private final ExaminationService examinationService;
    
    /**
     * 上传检查报告
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam String userId,
            @RequestParam String examinationType) {
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文件不能为空"));
            }
            
            // 验证文件大小（10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文件大小超过限制（10MB）"));
            }
            
            // 上传报告
            ExaminationRecord record = examinationService.uploadReport(
                userId, file, examinationType
            );
            
            // 构建响应
            Map<String, Object> data = new HashMap<>();
            data.put("recordId", record.getId());
            data.put("examinationType", record.getExaminationType().name());
            data.put("reportFilePath", record.getReportFilePath());
            data.put("ocrStatus", record.getOcrStatus());
            data.put("ocrResult", record.getReportOcrResult());
            
            return ResponseEntity.ok(ApiResponse.success(data));
            
        } catch (IOException e) {
            log.error("上传检查报告失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "上传检查报告失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("上传检查报告失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "上传检查报告失败: " + e.getMessage()));
        }
    }
    
    /**
     * OCR识别报告
     */
    @PostMapping("/ocr")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ocrRecognize(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long recordId) {
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "文件不能为空"));
            }
            
            // OCR识别
            Map<String, Object> ocrResult;
            if (recordId != null) {
                ocrResult = examinationService.ocrRecognize(recordId, file);
            } else {
                // 如果没有recordId，直接调用OCR服务
                // 这里简化处理，实际应该通过OCR服务客户端调用
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "recordId参数不能为空"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(ocrResult));
            
        } catch (Exception e) {
            log.error("OCR识别失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "OCR识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取检查历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        try {
            Page<ExaminationRecord> records = examinationService.getHistory(
                userId, page, pageSize
            );
            
            // 构建响应
            Map<String, Object> data = new HashMap<>();
            data.put("total", records.getTotalElements());
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("records", records.getContent());
            
            return ResponseEntity.ok(ApiResponse.success(data));
            
        } catch (Exception e) {
            log.error("获取检查历史失败: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "获取检查历史失败: " + e.getMessage()));
        }
    }
}

