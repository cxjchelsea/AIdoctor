package com.aidoctor.diagnosis.dto.evidence;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 支持证据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportingEvidence {
    private String item;
    private String type;
    private String strength;  // strong / medium / weak
}

