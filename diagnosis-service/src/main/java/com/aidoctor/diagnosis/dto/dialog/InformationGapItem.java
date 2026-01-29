package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信息缺口项
 * 对应 Python 端的 InformationGapItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationGapItem {
    private String field;
    private String description;
    private String reason;
}


