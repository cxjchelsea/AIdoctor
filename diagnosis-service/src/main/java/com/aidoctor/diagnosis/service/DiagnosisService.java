package com.aidoctor.diagnosis.service;

import com.aidoctor.diagnosis.dto.request.DiagnosisRequest;
import com.aidoctor.diagnosis.dto.request.UserAnswer;
import com.aidoctor.diagnosis.dto.response.DiagnosisResponse;
import com.aidoctor.diagnosis.dto.response.QuestionResponse;
import com.aidoctor.diagnosis.entity.DiagnosisRecord;
import com.aidoctor.diagnosis.repository.DiagnosisRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 诊断服务
 */
@Slf4j
@Service
public class DiagnosisService {
    
    @Autowired
    private DiagnosisRecordRepository diagnosisRecordRepository;
    
    @Autowired
    private AdaptiveQuestioningService adaptiveQuestioningService;
    
    // 注意：当前使用简化版本的完整度计算（calculateSimpleCompleteness）
    // 如需使用 step2.CompletenessCalculator，需要先将 DiagnosisRecord 转换为 StructuredQuestionList
    
    /**
     * 开始诊断
     */
    @Transactional
    public DiagnosisResponse startDiagnosis(DiagnosisRequest request) {
        log.info("开始诊断: userId={}, type={}", request.getUserId(), request.getDiagnosisType());
        
        // 1. 创建诊断记录
        DiagnosisRecord.DiagnosisType diagnosisType = parseDiagnosisType(request.getDiagnosisType());
        DiagnosisRecord record = DiagnosisRecord.builder()
            .userId(request.getUserId())
            .diagnosisType(diagnosisType)
            .status(DiagnosisRecord.DiagnosisStatus.COLLECTING)
            .chiefComplaint(request.getSymptomInfo() != null ? request.getSymptomInfo().getChiefComplaint() : null)
            .symptomDuration(request.getSymptomInfo() != null ? request.getSymptomInfo().getDuration() : null)
            .symptomSeverity(request.getSymptomInfo() != null ? request.getSymptomInfo().getSeverity() : null)
            .symptomFrequency(request.getSymptomInfo() != null ? request.getSymptomInfo().getFrequency() : null)
            .symptomLocation(request.getSymptomInfo() != null ? request.getSymptomInfo().getLocation() : null)
            .accompanyingSymptoms(request.getSymptomInfo() != null && request.getSymptomInfo().getAccompanyingSymptoms() != null 
                ? String.join(",", request.getSymptomInfo().getAccompanyingSymptoms()) : null)
            .questioningCount(0)
            .build();
        
        record = diagnosisRecordRepository.save(record);
        log.info("创建诊断记录: id={}", record.getId());
        
        // 2. 计算初始完整度（简化版本）
        double completeness = calculateSimpleCompleteness(record);
        
        // 3. 生成第一个追问问题（如果完整度不足）
        QuestionResponse question = null;
        String status = "collecting";
        
        if (completeness < 0.6) {
            question = generateNextQuestion(record);
            status = "questioning";
        } else {
            status = "analyzing";
        }
        
        // 4. 构建响应
        return DiagnosisResponse.builder()
            .code(200)
            .message("success")
            .diagnosisId(String.valueOf(record.getId()))
            .status(status)
            .completeness(completeness * 100)
            .question(question)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 继续对话
     */
    @Transactional
    public DiagnosisResponse continueDialogue(UserAnswer answer) {
        // 兼容旧字段：优先使用cdpId，如果没有则使用已过时的diagnosisId
        String cdpId = answer.getCdpId();
        if (cdpId == null || cdpId.isEmpty()) {
            @SuppressWarnings("deprecation")
            String deprecatedId = answer.getDiagnosisId();
            cdpId = deprecatedId;
        }
        if (cdpId == null || cdpId.isEmpty()) {
            throw new IllegalArgumentException("CDP ID或Diagnosis ID不能为空");
        }
        log.info("继续对话: cdpId={}, answer={}", cdpId, answer.getAnswer());
        
        // 1. 获取诊断记录
        Long diagnosisId = Long.parseLong(cdpId);
        Optional<DiagnosisRecord> recordOpt = diagnosisRecordRepository.findById(diagnosisId);
        
        if (!recordOpt.isPresent()) {
            throw new RuntimeException("诊断记录不存在: " + diagnosisId);
        }
        
        DiagnosisRecord record = recordOpt.get();
        
        // 2. 更新诊断记录（根据answerType）
        updateRecordWithAnswer(record, answer);
        record.setQuestioningCount(record.getQuestioningCount() + 1);
        record = diagnosisRecordRepository.save(record);
        
        // 3. 计算完整度
        double completeness = calculateSimpleCompleteness(record);
        
        // 4. 判断下一步
        QuestionResponse question = null;
        String status = "questioning";
        
        if (completeness >= 0.6) {
            // 信息足够，可以开始分析
            record.setStatus(DiagnosisRecord.DiagnosisStatus.ANALYZING);
            status = "analyzing";
            diagnosisRecordRepository.save(record);
        } else {
            // 信息不足，继续追问
            question = generateNextQuestion(record);
            record.setStatus(DiagnosisRecord.DiagnosisStatus.QUESTIONING);
            diagnosisRecordRepository.save(record);
        }
        
        // 5. 构建响应
        return DiagnosisResponse.builder()
            .code(200)
            .message("success")
            .diagnosisId(cdpId)
            .status(status)
            .completeness(completeness * 100)
            .question(question)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 解析诊断类型
     */
    private DiagnosisRecord.DiagnosisType parseDiagnosisType(String type) {
        if (type == null) {
            return DiagnosisRecord.DiagnosisType.SYMPTOM;
        }
        switch (type.toLowerCase()) {
            case "symptom":
                return DiagnosisRecord.DiagnosisType.SYMPTOM;
            case "examination":
                return DiagnosisRecord.DiagnosisType.EXAMINATION;
            case "comprehensive":
                return DiagnosisRecord.DiagnosisType.COMPREHENSIVE;
            default:
                return DiagnosisRecord.DiagnosisType.SYMPTOM;
        }
    }
    
    /**
     * 根据答案更新诊断记录（简化版本）
     */
    private void updateRecordWithAnswer(DiagnosisRecord record, UserAnswer answer) {
        String answerText = answer.getAnswer();
        String answerType = answer.getAnswerType();
        
        if (answerType == null || answerType.isEmpty()) {
            // 如果没有指定类型，尝试从上下文推断
            answerType = inferAnswerType(record);
        }
        
        switch (answerType) {
            case "symptom":
            case "duration":
                record.setSymptomDuration(answerText);
                break;
            case "severity":
                try {
                    int severity = Integer.parseInt(answerText);
                    if (severity >= 0 && severity <= 10) {
                        record.setSymptomSeverity(severity);
                    }
                } catch (NumberFormatException e) {
                    // 尝试从文本中提取数字
                    String numStr = answerText.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        try {
                            int severity = Integer.parseInt(numStr);
                            if (severity >= 0 && severity <= 10) {
                                record.setSymptomSeverity(severity);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                break;
            case "frequency":
                record.setSymptomFrequency(answerText);
                break;
            case "location":
                record.setSymptomLocation(answerText);
                break;
            default:
                // 如果不知道类型，尝试更新主诉或持续时间
                if (record.getSymptomDuration() == null) {
                    record.setSymptomDuration(answerText);
                }
                break;
        }
    }
    
    /**
     * 推断答案类型（基于已收集的信息）
     */
    private String inferAnswerType(DiagnosisRecord record) {
        if (record.getSymptomDuration() == null) {
            return "duration";
        }
        if (record.getSymptomSeverity() == null) {
            return "severity";
        }
        if (record.getSymptomFrequency() == null) {
            return "frequency";
        }
        return "symptom";
    }
    
    /**
     * 计算简单完整度（基于已收集的症状信息）
     * 简化版本：只检查5个关键字段
     */
    private double calculateSimpleCompleteness(DiagnosisRecord record) {
        int totalFields = 5;
        int collectedFields = 0;
        
        if (record.getChiefComplaint() != null && !record.getChiefComplaint().isEmpty()) {
            collectedFields++;
        }
        if (record.getSymptomDuration() != null && !record.getSymptomDuration().isEmpty()) {
            collectedFields++;
        }
        if (record.getSymptomSeverity() != null) {
            collectedFields++;
        }
        if (record.getSymptomFrequency() != null && !record.getSymptomFrequency().isEmpty()) {
            collectedFields++;
        }
        if (record.getAccompanyingSymptoms() != null && !record.getAccompanyingSymptoms().isEmpty()) {
            collectedFields++;
        }
        
        return (double) collectedFields / totalFields;
    }
    
    /**
     * 生成下一个追问问题（简化版本）
     */
    private QuestionResponse generateNextQuestion(DiagnosisRecord record) {
        // 按优先级生成问题
        if (record.getSymptomDuration() == null || record.getSymptomDuration().isEmpty()) {
            return QuestionResponse.builder()
                .question("请描述一下这个症状持续了多长时间？")
                .questionType("duration")
                .missingInfoType("symptom")
                .priority("required")
                .required(true)
                .build();
        }
        
        if (record.getSymptomSeverity() == null) {
            return QuestionResponse.builder()
                .question("请用0-10分评价一下症状的严重程度，0表示不严重，10表示非常严重。")
                .questionType("severity")
                .missingInfoType("symptom")
                .options(Arrays.asList("1-3分（轻微）", "4-6分（中等）", "7-10分（严重）"))
                .priority("required")
                .required(true)
                .build();
        }
        
        if (record.getSymptomFrequency() == null || record.getSymptomFrequency().isEmpty()) {
            return QuestionResponse.builder()
                .question("请描述一下症状出现的频率？")
                .questionType("frequency")
                .missingInfoType("symptom")
                .options(Arrays.asList("持续存在", "每天多次", "每天一次", "偶尔出现", "很少出现"))
                .priority("important")
                .required(false)
                .build();
        }
        
        if (record.getSymptomLocation() == null || record.getSymptomLocation().isEmpty()) {
            return QuestionResponse.builder()
                .question("请描述一下症状的具体部位？")
                .questionType("location")
                .missingInfoType("symptom")
                .priority("important")
                .required(false)
                .build();
        }
        
        // 如果所有基本信息都有了，询问伴随症状
        return QuestionResponse.builder()
            .question("除了这个症状，还有没有其他不适的症状？")
            .questionType("accompanying")
            .missingInfoType("symptom")
            .priority("optional")
            .required(false)
            .build();
    }
}

