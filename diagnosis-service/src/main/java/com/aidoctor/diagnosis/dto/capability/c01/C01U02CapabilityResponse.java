package com.aidoctor.diagnosis.dto.capability.c01;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Candidate-only C01/U02 result. It is neither Clinical Truth nor a K09 proposal. */
@Data
@NoArgsConstructor
public class C01U02CapabilityResponse {
    private String businessStatus;
    private String reasonCode;
    private boolean retryable;
    private BindingRef bindingRef;
    private List<ObservationCandidate> observationCandidates;
    private String sourceAttribution;
    private List<String> provenance;

    @Data
    @NoArgsConstructor
    public static class BindingRef {
        private String bindingId;
        private String bindingStatus;
        private String capabilityId;
        private String capabilityVersion;
        private String capabilitySetVersion;
        private String scopeVersion;
        private String contractVersion;
    }

    @Data
    @NoArgsConstructor
    public static class ObservationCandidate {
        private String observationId;
        private String conceptId;
        private String conceptDisplay;
        private String rawTextRef;
        private String normalizedValue;
        private String valueSemantics;
        private String unit;
        private boolean negation;
        private String temporality;
        private String severityOrDegree;
        private String sourceType;
        private String lifecycle;
        private double confidenceOrUncertainty;
        private List<String> provenance;
        private List<String> ambiguityFlags;
        private List<String> contradictionRefs;
    }
}
