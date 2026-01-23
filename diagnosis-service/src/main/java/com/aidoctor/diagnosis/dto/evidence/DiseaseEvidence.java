package com.aidoctor.diagnosis.dto.evidence;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 疾病证据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseEvidence {
    private String disease;
    private List<SupportingEvidence> supportingSymptoms;
    private List<SupportingEvidence> supportingSigns;
    private List<SupportingEvidence> supportingExaminations;
    private ProfileMatch profileMatch;
    private List<OpposingEvidence> opposingEvidence;
    private EvidenceStrength strength;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileMatch {
        private Boolean matched;
        private String reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceStrength {
        private List<String> strong;
        private List<String> medium;
        private List<String> weak;
    }
}

