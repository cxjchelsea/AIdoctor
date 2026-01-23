package com.aidoctor.diagnosis.dto.threeLayer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 必须排除的高危诊断（0-1个）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MustExcludeDiagnosis {
    private String disease;
    private Double score;
    private String layer;
    private String reason;  // 为什么必须排除
}

