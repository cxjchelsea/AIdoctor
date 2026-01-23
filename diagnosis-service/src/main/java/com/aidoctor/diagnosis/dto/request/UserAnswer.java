package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 用户回答
 */
@Data
public class UserAnswer {
    
    @NotBlank(message = "CDP ID不能为空")
    private String cdpId;
    
    @NotBlank(message = "问题ID不能为空")
    private String questionId;
    
    @NotBlank(message = "回答内容不能为空")
    private String answer;
    
    private String answerType; // symptom, sign, history, other
    
    // 兼容旧字段
    @Deprecated
    private String diagnosisId;
}

