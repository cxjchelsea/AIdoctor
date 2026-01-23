package com.aidoctor.diagnosis.dto.evidence;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 反对证据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpposingEvidence {
    private String item;
    private String type;
    private String strength;  // strong / medium / weak
}

