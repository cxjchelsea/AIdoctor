package com.aidoctor.diagnosis.dto.threeLayer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 首要假设（1个）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrimaryHypothesis {
    private String disease;
    private Double score;
    private String layer;
    private String evidence;  // 入选依据
}

