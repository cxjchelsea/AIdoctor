package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问题响应
 * 对应 Python 端的 QuestionResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private String question;
    private String questionType;
    private List<MissingInfoItem> missingInfo;  // 修改为对象列表，匹配 Python 端格式
    private Double completeness;
    private InformationGaps informationGaps;  // 新增字段，匹配 Python 端格式
}

