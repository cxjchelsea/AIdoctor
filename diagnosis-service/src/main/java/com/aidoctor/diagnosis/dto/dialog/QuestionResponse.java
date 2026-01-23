package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问题响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private String question;
    private String questionType;
    private List<String> missingInfo;
    private Double completeness;
}

