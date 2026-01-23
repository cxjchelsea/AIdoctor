package com.aidoctor.diagnosis.dto.evidence;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 证据分析
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceAnalysis {
    
    /**
     * 首要假设的证据
     */
    private DiseaseEvidence primaryHypothesisEvidence;
    
    /**
     * 主要备选诊断的证据
     */
    private List<DiseaseEvidence> alternativesEvidence;
    
    /**
     * 必须排除的高危诊断的证据
     */
    private DiseaseEvidence mustExcludeEvidence;
}

