package com.aidoctor.diagnosis.dto.capability.c01;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Candidate-only C01 result. This DTO is not Clinical Truth and not a K09 proposal. */
@Data
@NoArgsConstructor
public class C01U01CapabilityResponse {
    private String businessStatus;
    private String reasonCode;
    private boolean retryable;
    private BindingRef bindingRef;
    private SubjectCandidate subjectCandidate;
    private ProblemCandidate problemCandidate;
    private ScopeCandidate scopeCandidate;
    private EarlySafetySignalCandidate earlySafetySignalCandidate;
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
    public static class EvidenceSpan {
        private String text;
        private int start;
        private int end;
        private String source;
    }

    @Data
    @NoArgsConstructor
    public static class SubjectCandidate {
        private String subjectType;
        private String relationText;
        private double confidence;
        private boolean uncertain;
        private List<EvidenceSpan> evidenceSpans;
    }

    @Data
    @NoArgsConstructor
    public static class ProblemCandidate {
        private String text;
        private double confidence;
        private boolean uncertain;
        private List<EvidenceSpan> evidenceSpans;
    }

    @Data
    @NoArgsConstructor
    public static class ScopeCandidate {
        private String scope;
        private double confidence;
        private boolean uncertain;
        private List<EvidenceSpan> evidenceSpans;
    }

    @Data
    @NoArgsConstructor
    public static class EarlySafetySignalCandidate {
        private boolean detected;
        private List<String> clues;
        private List<EvidenceSpan> evidenceSpans;
    }
}
