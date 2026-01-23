package com.aidoctor.diagnosis.service;

import com.aidoctor.diagnosis.dto.response.QuestionResponse;
import com.aidoctor.diagnosis.entity.DiagnosisRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 智能追问服务
 */
@Slf4j
@Service
public class AdaptiveQuestioningService {
    
    /**
     * 生成追问问题
     */
    public QuestionResponse generateQuestion(DiagnosisRecord record, Object profile) {
        log.debug("生成追问问题: diagnosisId={}", record.getId());
        
        // TODO: 实现智能追问逻辑
        // 1. 识别缺失信息
        // 2. 选择优先级最高的缺失信息
        // 3. 根据缺失信息类型生成问题
        
        return QuestionResponse.builder()
            .question("您这个症状出现多久了？")
            .questionType("symptom_duration")
            .missingInfoType("duration")
            .required(true)
            .build();
    }
}

