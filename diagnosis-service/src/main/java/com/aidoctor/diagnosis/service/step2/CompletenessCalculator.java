package com.aidoctor.diagnosis.service.step2;

import com.aidoctor.diagnosis.dto.questionlist.StructuredQuestionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 完整度计算服务
 * Step 1：识别问题 - 基于结构化问题清单计算信息完整度（脑区B）
 */
@Slf4j
@Service
public class CompletenessCalculator {
    
    /**
     * 计算信息完整度（基于结构化问题清单）
     * 总信息项：14项
     * - 症状信息：4项（主诉、持续时间、严重程度、伴随症状）
     * - 健康档案：4项（年龄、性别、既往史、用药史）
     * - 生命体征：3项（血压、心率、体温）
     * - 检查结果：1项
     * - 家族史：1项
     * - 生活方式：1项
     * 
     * @param questionList 结构化问题清单
     * @return 信息完整度（0-1）
     */
    public double calculateCompleteness(StructuredQuestionList questionList) {
        log.debug("计算信息完整度");
        
        int totalItems = 14;
        int collectedItems = 0;
        
        // 1. 症状信息（4项）
        if (questionList.getChiefComplaint() != null 
                && questionList.getChiefComplaint().getName() != null 
                && !questionList.getChiefComplaint().getName().isEmpty()) {
            collectedItems++;  // 主诉
        }
        
        if (questionList.getChiefComplaint() != null 
                && questionList.getChiefComplaint().getDuration() != null 
                && !questionList.getChiefComplaint().getDuration().isEmpty()) {
            collectedItems++;  // 持续时间
        }
        
        if (questionList.getChiefComplaint() != null 
                && questionList.getChiefComplaint().getSeverity() != null) {
            collectedItems++;  // 严重程度
        }
        
        if (questionList.getAccompanyingSymptoms() != null 
                && !questionList.getAccompanyingSymptoms().isEmpty()) {
            collectedItems++;  // 伴随症状
        }
        
        // 2. 健康档案（4项）
        if (questionList.getKeyBackground() != null) {
            if (questionList.getKeyBackground().getAge() != null) {
                collectedItems++;  // 年龄
            }
            if (questionList.getKeyBackground().getGender() != null 
                    && !questionList.getKeyBackground().getGender().isEmpty()) {
                collectedItems++;  // 性别
            }
            if (questionList.getKeyBackground().getMedicalHistory() != null 
                    && !questionList.getKeyBackground().getMedicalHistory().isEmpty()) {
                collectedItems++;  // 既往史
            }
            if (questionList.getKeyBackground().getMedicationHistory() != null 
                    && !questionList.getKeyBackground().getMedicationHistory().isEmpty()) {
                collectedItems++;  // 用药史
            }
        }
        
        // 3. 生命体征（3项）
        if (questionList.getVitalSigns() != null) {
            if (questionList.getVitalSigns().getBloodPressure() != null) {
                collectedItems++;  // 血压
            }
            if (questionList.getVitalSigns().getHeartRate() != null) {
                collectedItems++;  // 心率
            }
            if (questionList.getVitalSigns().getTemperature() != null) {
                collectedItems++;  // 体温
            }
        }
        
        // 4. 检查结果（1项）
        if (questionList.getExaminationResults() != null 
                && !questionList.getExaminationResults().isEmpty()) {
            collectedItems++;
        }
        
        // 5. 家族史（1项）
        if (questionList.getKeyBackground() != null 
                && questionList.getKeyBackground().getFamilyHistory() != null 
                && !questionList.getKeyBackground().getFamilyHistory().isEmpty()) {
            collectedItems++;
        }
        
        // 6. 生活方式（1项）
        if (questionList.getKeyBackground() != null 
                && questionList.getKeyBackground().getLifestyle() != null 
                && !questionList.getKeyBackground().getLifestyle().isEmpty()) {
            collectedItems++;
        }
        
        double completeness = (double) collectedItems / totalItems;
        log.debug("信息完整度: {}/{} = {}", collectedItems, totalItems, completeness);
        
        return completeness;
    }
}

