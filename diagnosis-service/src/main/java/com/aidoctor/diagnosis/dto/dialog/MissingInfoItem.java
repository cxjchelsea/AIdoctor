package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缺失信息项
 * 对应 Python 端的 MissingInfoItem
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissingInfoItem {
    private String field;
    private String level;  // required/important/optional
    private String description;
}


