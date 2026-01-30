package com.aidoctor.examination.service;

import com.aidoctor.examination.entity.ExaminationPlan;
import com.aidoctor.examination.entity.ExaminationRecord;
import com.aidoctor.examination.entity.ExaminationRecord.ExaminationType;
import com.aidoctor.examination.repository.ExaminationPlanRepository;
import com.aidoctor.examination.repository.ExaminationRecordRepository;
import com.aidoctor.examination.client.OcrServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

/**
 * 检查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExaminationService {
    
    private final ExaminationPlanRepository planRepository;
    private final ExaminationRecordRepository recordRepository;
    private final OcrServiceClient ocrServiceClient;
    
    // 文件存储路径
    private static final String UPLOAD_DIR = "uploads/reports/";
    
    /**
     * 设计检查方案
     */
    public ExaminationPlan designPlan(String userId, Map<String, Object> request) {
        log.info("设计检查方案，userId: {}", userId);
        
        // TODO: 实现检查方案设计逻辑
        // 1. 分析症状、年龄、性别、既往史
        // 2. 匹配检查项
        // 3. 生成检查方案
        
        List<String> symptoms = (List<String>) request.getOrDefault("symptoms", new ArrayList<>());
        Map<String, Object> basicInfo = (Map<String, Object>) request.getOrDefault("basicInfo", new HashMap<>());
        Map<String, Object> medicalHistory = (Map<String, Object>) request.getOrDefault("medicalHistory", new HashMap<>());
        
        // 生成检查方案（简化实现）
        List<Map<String, Object>> planItems = generatePlanItems(symptoms, basicInfo, medicalHistory);
        
        ExaminationPlan plan = ExaminationPlan.builder()
                .userId(userId)
                .planName(generatePlanName(symptoms))
                .planType(ExaminationPlan.PlanType.DIAGNOSTIC)
                .planItems(planItems)
                .targetConditions(request)
                .build();
        
        return planRepository.save(plan);
    }
    
    /**
     * 生成检查项列表
     */
    private List<Map<String, Object>> generatePlanItems(
            List<String> symptoms,
            Map<String, Object> basicInfo,
            Map<String, Object> medicalHistory) {
        
        List<Map<String, Object>> items = new ArrayList<>();
        
        // 根据症状生成检查项（简化实现）
        if (symptoms.contains("胸痛") || symptoms.contains("气短")) {
            items.add(createPlanItem("心电图", "ECG", "high", "排查心脏疾病"));
            items.add(createPlanItem("心肌酶谱", "CARDIAC_ENZYMES", "high", "确诊急性心肌梗死"));
        }
        
        if (symptoms.contains("发热") || symptoms.contains("咳嗽")) {
            items.add(createPlanItem("胸部X光", "XRAY_CHEST", "medium", "排查肺部疾病"));
        }
        
        // 基础检查
        items.add(createPlanItem("血常规", "BLOOD_ROUTINE", "medium", "基础检查"));
        
        return items;
    }
    
    /**
     * 创建检查项
     */
    private Map<String, Object> createPlanItem(String testName, String testCode, String priority, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("testName", testName);
        item.put("testCode", testCode);
        item.put("priority", priority);
        item.put("reason", reason);
        return item;
    }
    
    /**
     * 生成方案名称
     */
    private String generatePlanName(List<String> symptoms) {
        if (symptoms.isEmpty()) {
            return "常规检查方案";
        }
        return String.join("、", symptoms) + "相关检查方案";
    }
    
    /**
     * 上传检查报告
     */
    public ExaminationRecord uploadReport(
            String userId,
            MultipartFile file,
            String examinationType) throws IOException {
        
        log.info("上传检查报告，userId: {}, type: {}", userId, examinationType);
        
        // 1. 保存文件
        String filePath = saveFile(file, userId);
        
        // 2. 创建检查记录
        ExaminationRecord record = ExaminationRecord.builder()
                .userId(userId)
                .examinationType(ExaminationType.valueOf(examinationType))
                .reportType(getFileType(file.getOriginalFilename()))
                .reportFilePath(filePath)
                .examinationDate(LocalDate.now())
                .ocrStatus("pending")
                .build();
        
        record = recordRepository.save(record);
        
        // 3. 异步调用OCR服务（可选）
        // 这里简化处理，同步调用
        try {
            Map<String, Object> ocrResult = ocrServiceClient.recognize(file);
            record.setReportOcrResult((Map<String, Object>) ocrResult.get("data"));
            record.setOcrStatus("completed");
            
            // 提取结构化数据
            Map<String, Object> structuredData = (Map<String, Object>) 
                ((Map<String, Object>) ocrResult.get("data")).get("structured_data");
            record.setReportStructuredData(structuredData);
            
            record = recordRepository.save(record);
        } catch (Exception e) {
            log.error("OCR识别失败: {}", e.getMessage());
            record.setOcrStatus("failed");
            record = recordRepository.save(record);
        }
        
        return record;
    }
    
    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, String userId) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(UPLOAD_DIR + userId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : "";
        String filename = System.currentTimeMillis() + extension;
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
    
    /**
     * 获取文件类型
     */
    private String getFileType(String filename) {
        if (filename == null) {
            return "unknown";
        }
        if (filename.endsWith(".pdf")) {
            return "PDF";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "JPG";
        } else if (filename.endsWith(".png")) {
            return "PNG";
        }
        return "unknown";
    }
    
    /**
     * OCR识别报告
     */
    public Map<String, Object> ocrRecognize(Long recordId, MultipartFile file) {
        log.info("OCR识别报告，recordId: {}", recordId);
        
        try {
            // 调用OCR服务
            Map<String, Object> ocrResult = ocrServiceClient.recognize(file);
            
            // 更新检查记录
            ExaminationRecord record = recordRepository.findById(recordId)
                    .orElseThrow(() -> new RuntimeException("检查记录不存在"));
            
            record.setReportOcrResult((Map<String, Object>) ocrResult.get("data"));
            record.setOcrStatus("completed");
            
            // 提取结构化数据
            Map<String, Object> structuredData = (Map<String, Object>) 
                ((Map<String, Object>) ocrResult.get("data")).get("structured_data");
            record.setReportStructuredData(structuredData);
            
            recordRepository.save(record);
            
            return ocrResult;
            
        } catch (Exception e) {
            log.error("OCR识别失败: {}", e.getMessage());
            throw new RuntimeException("OCR识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取检查历史
     */
    public Page<ExaminationRecord> getHistory(String userId, int page, int pageSize) {
        log.info("获取检查历史，userId: {}, page: {}, pageSize: {}", userId, page, pageSize);
        
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        return recordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}

