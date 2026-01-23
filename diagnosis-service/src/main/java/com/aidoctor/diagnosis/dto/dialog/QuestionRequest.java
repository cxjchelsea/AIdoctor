package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 生成问题请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    private String diagnosisId;
    private Map<String, Object> context;
}

