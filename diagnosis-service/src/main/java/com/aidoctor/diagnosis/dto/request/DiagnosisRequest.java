package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 开始诊断请求
 */
@Data
public class DiagnosisRequest {
    
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    
    @NotNull(message = "诊断类型不能为空")
    private String diagnosisType; // symptom, examination, comprehensive
    
    private SymptomInfo symptomInfo;
    
    private Long examinationRecordId;
    
    // 健康状态判定相关字段（从前端传递）
    private String userInput; // 用户原始输入
    private Map<String, Object> basicInfo; // 基本信息
    private List<String> symptoms; // 症状列表
    private Map<String, Object> vitalSigns; // 生命体征
}

