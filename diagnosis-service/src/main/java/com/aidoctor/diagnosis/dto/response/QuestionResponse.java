package com.aidoctor.diagnosis.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 追问问题响应
 */
@Data
@Builder
public class QuestionResponse {
    
    private String question;
    private String questionType;
    private String missingInfoType;
    private List<String> options;
    private Boolean required;
    
    /**
     * 优先级（更新版，基于信息缺口分级）
     * required: 必填缺口
     * important: 重要缺口
     * optional: 可选缺口
     */
    private String priority;
}

