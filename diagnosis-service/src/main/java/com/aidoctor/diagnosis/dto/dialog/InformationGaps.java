package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 信息缺口分类
 * 对应 Python 端的 InformationGaps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationGaps {
    private List<InformationGapItem> required;
    private List<InformationGapItem> important;
    private List<InformationGapItem> optional;
}


