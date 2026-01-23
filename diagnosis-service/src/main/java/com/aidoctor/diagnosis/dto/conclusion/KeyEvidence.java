package com.aidoctor.diagnosis.dto.conclusion;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 关键依据（至少三条证据）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyEvidence {
    private String item;
    private String type;  // symptom / sign / examination / medical_history
    private String strength;  // strong / medium / weak
    private String role;  // supporting / opposing
}

