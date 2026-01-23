package com.aidoctor.diagnosis.service.step1;

import com.aidoctor.diagnosis.dto.questionlist.StructuredQuestionList;
import com.aidoctor.diagnosis.dto.questionlist.InformationGaps;
import com.aidoctor.diagnosis.entity.DiagnosisRecord;
import com.aidoctor.diagnosis.service.step2.InformationGapIdentifier;
import com.aidoctor.diagnosis.service.step2.CompletenessCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化问题清单构建器
 * Step 1：识别问题 - 构建结构化问题清单（脑区A）
 */
@Slf4j
@Service
public class StructuredQuestionListBuilder {
    
    @Autowired
    private ConceptNormalizationService conceptNormalizationService;
    
    @Autowired
    private InformationGapIdentifier informationGapIdentifier;
    
    @Autowired
    private CompletenessCalculator completenessCalculator;
    
    /**
     * 构建结构化问题清单
     * 从诊断记录中提取信息，构建结构化问题清单
     * 
     * @param record 诊断记录
     * @return 结构化问题清单
     */
    public StructuredQuestionList build(DiagnosisRecord record) {
        log.info("构建结构化问题清单，诊断ID: {}", record.getId());
        
        StructuredQuestionList.StructuredQuestionListBuilder builder = StructuredQuestionList.builder();
        
        // 1. 构建主要问题（主诉）
        StructuredQuestionList.ChiefComplaint chiefComplaint = buildChiefComplaint(record);
        builder.chiefComplaint(chiefComplaint);
        
        // 2. 构建伴随症状
        List<StructuredQuestionList.AccompanyingSymptom> accompanyingSymptoms = buildAccompanyingSymptoms(record);
        builder.accompanyingSymptoms(accompanyingSymptoms);
        
        // 3. 构建关键背景
        StructuredQuestionList.KeyBackground keyBackground = buildKeyBackground(record);
        builder.keyBackground(keyBackground);
        
        // 4. 构建生命体征
        StructuredQuestionList.VitalSigns vitalSigns = buildVitalSigns(record);
        builder.vitalSigns(vitalSigns);
        
        // 5. 构建检查结果（如果有）
        List<StructuredQuestionList.ExaminationResult> examinationResults = buildExaminationResults(record);
        builder.examinationResults(examinationResults);
        
        // 6. 构建历史诊断（如果有）
        List<StructuredQuestionList.HistoricalDiagnosis> historicalDiagnoses = buildHistoricalDiagnoses(record);
        builder.historicalDiagnoses(historicalDiagnoses);
        
        // 7. 标记信息缺口
        StructuredQuestionList questionList = builder.build();
        InformationGaps gaps = informationGapIdentifier.identifyGaps(questionList);
        builder.informationGaps(gaps);
        
        // 8. 计算信息完整度
        questionList = builder.build();
        double completeness = completenessCalculator.calculateCompleteness(questionList);
        builder.completeness(completeness);
        
        return builder.build();
    }
    
    /**
     * 构建主要问题（主诉）
     */
    private StructuredQuestionList.ChiefComplaint buildChiefComplaint(DiagnosisRecord record) {
        String originalText = record.getChiefComplaint();
        String normalizedName = conceptNormalizationService.normalizeChiefComplaint(originalText);
        
        return StructuredQuestionList.ChiefComplaint.builder()
            .name(normalizedName)
            .originalText(originalText)
            .duration(record.getSymptomDuration())
            .onsetMode(determineOnsetMode(record.getSymptomDuration()))
            .severity(record.getSymptomSeverity())
            .frequency(record.getSymptomFrequency())
            .location(record.getSymptomLocation())
            .features(record.getSymptomFeatures() != null ? record.getSymptomFeatures() : new HashMap<>())
            .triageLevel(null)  // TODO: 从诊断结果中提取分诊等级
            .build();
    }
    
    /**
     * 构建伴随症状
     */
    private List<StructuredQuestionList.AccompanyingSymptom> buildAccompanyingSymptoms(DiagnosisRecord record) {
        List<StructuredQuestionList.AccompanyingSymptom> symptoms = new ArrayList<>();
        
        if (record.getAccompanyingSymptoms() != null && !record.getAccompanyingSymptoms().isEmpty()) {
            String[] symptomArray = record.getAccompanyingSymptoms().split(",");
            for (String symptom : symptomArray) {
                StructuredQuestionList.AccompanyingSymptom acc = StructuredQuestionList.AccompanyingSymptom.builder()
                    .name(symptom.trim())
                    .startTime(null)  // TODO: 从记录中提取
                    .severity(null)   // TODO: 从记录中提取
                    .build();
                symptoms.add(acc);
            }
        }
        
        return symptoms;
    }
    
    /**
     * 构建关键背景
     */
    private StructuredQuestionList.KeyBackground buildKeyBackground(DiagnosisRecord record) {
        // TODO: 从健康档案服务获取完整的用户背景信息
        return StructuredQuestionList.KeyBackground.builder()
            .age(null)  // TODO: 从健康档案获取
            .gender(null)  // TODO: 从健康档案获取
            .medicalHistory(new ArrayList<>())  // TODO: 从健康档案获取
            .medicationHistory(new ArrayList<>())  // TODO: 从健康档案获取
            .familyHistory(new ArrayList<>())  // TODO: 从健康档案获取
            .lifestyle(new HashMap<>())  // TODO: 从健康档案获取
            .recentEvents(new ArrayList<>())  // TODO: 从健康档案获取
            .build();
    }
    
    /**
     * 构建生命体征
     */
    private StructuredQuestionList.VitalSigns buildVitalSigns(DiagnosisRecord record) {
        if (record.getVitalSigns() == null || record.getVitalSigns().isEmpty()) {
            return null;
        }
        
        Map<String, Object> vitalSignsMap = record.getVitalSigns();
        
        StructuredQuestionList.VitalSigns.BloodPressure bp = null;
        if (vitalSignsMap.containsKey("bp")) {
            Map<String, Object> bpMap = (Map<String, Object>) vitalSignsMap.get("bp");
            bp = StructuredQuestionList.VitalSigns.BloodPressure.builder()
                .systolic((Integer) bpMap.get("systolic"))
                .diastolic((Integer) bpMap.get("diastolic"))
                .build();
        }
        
        return StructuredQuestionList.VitalSigns.builder()
            .bloodPressure(bp)
            .heartRate((Integer) vitalSignsMap.get("heart_rate"))
            .temperature((Double) vitalSignsMap.get("temperature"))
            .oxygenSaturation((Integer) vitalSignsMap.get("oxygen_saturation"))
            .respiratoryRate((Integer) vitalSignsMap.get("respiratory_rate"))
            .build();
    }
    
    /**
     * 构建检查结果
     */
    private List<StructuredQuestionList.ExaminationResult> buildExaminationResults(DiagnosisRecord record) {
        List<StructuredQuestionList.ExaminationResult> results = new ArrayList<>();
        
        if (record.getExaminationResults() != null) {
            for (Map<String, Object> examMap : record.getExaminationResults()) {
                StructuredQuestionList.ExaminationResult exam = StructuredQuestionList.ExaminationResult.builder()
                    .type((String) examMap.get("type"))
                    .name((String) examMap.get("name"))
                    .date((String) examMap.get("date"))
                    .data(examMap)
                    .build();
                results.add(exam);
            }
        }
        
        return results;
    }
    
    /**
     * 构建历史诊断
     */
    private List<StructuredQuestionList.HistoricalDiagnosis> buildHistoricalDiagnoses(DiagnosisRecord record) {
        // TODO: 从历史诊断记录中获取
        return new ArrayList<>();
    }
    
    /**
     * 确定起病方式
     */
    private String determineOnsetMode(String duration) {
        // TODO: 根据持续时间判断起病方式（sudden/gradual）
        return "gradual";
    }
}

