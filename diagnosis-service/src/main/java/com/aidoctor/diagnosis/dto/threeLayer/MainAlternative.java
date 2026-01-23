package com.aidoctor.diagnosis.dto.threeLayer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 主要备选诊断（1-2个）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MainAlternative {
    private String disease;
    private Double score;
    private String layer;
    private String evidence;  // 入选依据
}

