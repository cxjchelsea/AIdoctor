package com.aidoctor.diagnosis.service.step2;

import com.aidoctor.diagnosis.dto.questionlist.InformationGaps;
import com.aidoctor.diagnosis.dto.questionlist.StructuredQuestionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息缺口识别与分级服务
 * Step 1：识别问题 - 识别信息缺口并按等级分类（脑区B）
 */
@Slf4j
@Service
public class InformationGapIdentifier {
    
    /**
     * 识别信息缺口并按等级分类
     * 
     * @param questionList 结构化问题清单
     * @return 信息缺口
     */
    public InformationGaps identifyGaps(StructuredQuestionList questionList) {
        log.info("识别信息缺口");
        
        InformationGaps.InformationGapsBuilder builder = InformationGaps.builder();
        List<String> requiredGaps = new ArrayList<>();
        List<String> importantGaps = new ArrayList<>();
        List<String> optionalGaps = new ArrayList<>();
        
        // 1. 检查必填缺口（缺失则不能进入 Step 2）
        // 主诉持续时间
        if (questionList.getChiefComplaint() == null 
                || questionList.getChiefComplaint().getDuration() == null 
                || questionList.getChiefComplaint().getDuration().isEmpty()) {
            requiredGaps.add("duration");
        }
        
        // 主诉严重程度
        if (questionList.getChiefComplaint() == null 
                || questionList.getChiefComplaint().getSeverity() == null) {
            requiredGaps.add("severity");
        }
        
        // 主诉本身
        if (questionList.getChiefComplaint() == null 
                || questionList.getChiefComplaint().getName() == null 
                || questionList.getChiefComplaint().getName().isEmpty()) {
            requiredGaps.add("chief_complaint");
        }
        
        // 生命体征（如果症状严重）
        if (questionList.getChiefComplaint() != null 
                && questionList.getChiefComplaint().getSeverity() != null 
                && questionList.getChiefComplaint().getSeverity() >= 7 
                && questionList.getVitalSigns() == null) {
            requiredGaps.add("vital_signs");
        }
        
        // 2. 检查重要缺口（可进入但必须提示不确定性与风险）
        // 伴随症状
        if (questionList.getAccompanyingSymptoms() == null 
                || questionList.getAccompanyingSymptoms().isEmpty()) {
            importantGaps.add("accompanying_symptoms");
        }
        
        // 既往史
        if (questionList.getKeyBackground() == null 
                || questionList.getKeyBackground().getMedicalHistory() == null 
                || questionList.getKeyBackground().getMedicalHistory().isEmpty()) {
            importantGaps.add("medical_history");
        }
        
        // 用药史
        if (questionList.getKeyBackground() == null 
                || questionList.getKeyBackground().getMedicationHistory() == null 
                || questionList.getKeyBackground().getMedicationHistory().isEmpty()) {
            importantGaps.add("medication_history");
        }
        
        // 家族史
        if (questionList.getKeyBackground() == null 
                || questionList.getKeyBackground().getFamilyHistory() == null 
                || questionList.getKeyBackground().getFamilyHistory().isEmpty()) {
            importantGaps.add("family_history");
        }
        
        // 3. 检查可选缺口（后续补充即可）
        // 生活方式
        if (questionList.getKeyBackground() == null 
                || questionList.getKeyBackground().getLifestyle() == null 
                || questionList.getKeyBackground().getLifestyle().isEmpty()) {
            optionalGaps.add("lifestyle");
        }
        
        // 近期事件
        if (questionList.getKeyBackground() == null 
                || questionList.getKeyBackground().getRecentEvents() == null 
                || questionList.getKeyBackground().getRecentEvents().isEmpty()) {
            optionalGaps.add("recent_events");
        }
        
        return builder
            .requiredGaps(requiredGaps)
            .importantGaps(importantGaps)
            .optionalGaps(optionalGaps)
            .build();
    }
}

