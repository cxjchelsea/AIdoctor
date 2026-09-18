package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs;
import com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway;
import com.aidoctor.diagnosis.runtime.u03.U03NonProductionExecutionContext;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.u03.U03RiskAssessmentCandidate;

import java.util.Arrays;
import java.util.List;

/** Frozen RDP-01 consumer-side admission boundary. */
public final class U04AdmissionService {
    public static final String STALE_U03_HANDOFF = "STALE_U03_HANDOFF";
    public static final String MALFORMED_U03_HANDOFF = "MALFORMED_U03_HANDOFF";
    public static final String UNTRUSTED_U03_RELEASE_SET = "UNTRUSTED_U03_RELEASE_SET";
    public static final String UNTRUSTED_U04_POLICY_REF = "UNTRUSTED_U04_POLICY_REF";
    public static final String ENVIRONMENT_MISMATCH = "ENVIRONMENT_MISMATCH";

    public U04AdmissionResult admit(
            U03OutboundHandoff handoff,
            int currentClinicalStateVersion,
            U04ScopeContext scopeContext,
            String environmentId) {
        requireNonProduction(environmentId);
        if (handoff == null || scopeContext == null || currentClinicalStateVersion < 0) {
            return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
        }
        if (!environmentId.equals(handoff.getEnvironmentId())
                || !U03NonProductionExecutionContext.BINDING_MODE.equals(handoff.getBindingMode())) {
            return U04AdmissionResult.failed(ENVIRONMENT_MISMATCH);
        }
        try {
            scopeContext.requireFrozenPolicy();
        } catch (RuntimeException exception) {
            return U04AdmissionResult.failed(UNTRUSTED_U04_POLICY_REF);
        }
        if (!U03GovernedCandidateGateway.BINDING_ID.equals(handoff.getCapabilityBindingId())
                || !expectedGovernedRefs().equals(handoff.getGovernedReleaseRefs())) {
            return U04AdmissionResult.failed(UNTRUSTED_U03_RELEASE_SET);
        }
        if (blank(handoff.getConsultationId())
                || blank(handoff.getCdpId())
                || blank(handoff.getThreadId())
                || blank(handoff.getRunId())
                || blank(handoff.getEventId())
                || blank(handoff.getCorrelationId())
                || blank(handoff.getTraceId())
                || blank(handoff.getDecisionId())
                || blank(handoff.getDecisionStatus())
                || blank(handoff.getDecisionReasonCode())
                || blank(handoff.getAcceptanceRef())
                || handoff.getAcceptedEvidenceRefs().isEmpty()
                || handoff.getAcceptedSourceRefs().isEmpty()
                || handoff.getAcceptedProvenanceRefs().isEmpty()) {
            return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
        }

        if (!handoff.getExecutionStatus().equals(handoff.getDecisionStatus())) {
            return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
        }

        if (U03RiskAssessmentCandidate.VALID.equals(handoff.getDecisionStatus())) {
            if (handoff.getExecutionFailureReasonCode() != null
                    || !validDisposition(handoff.getDispositionCode())
                    || handoff.getCommittedClinicalStateVersion() == null
                    || handoff.getCommittedClinicalStateVersion().intValue()
                            <= handoff.getSourceClinicalStateVersion()
                    || !"COMMITTED".equals(handoff.getCommitStatus())
                    || blank(handoff.getProposalId())
                    || blank(handoff.getCommitAuditId())) {
                return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
            }
            if (currentClinicalStateVersion
                    != handoff.getCommittedClinicalStateVersion().intValue()) {
                return U04AdmissionResult.failed(STALE_U03_HANDOFF);
            }
        } else if (U03RiskAssessmentCandidate.FAILED.equals(handoff.getDecisionStatus())) {
            if (blank(handoff.getExecutionFailureReasonCode())
                    || !handoff.getExecutionFailureReasonCode().equals(handoff.getDecisionReasonCode())
                    || handoff.getDispositionCode() != null
                    || handoff.getCommittedClinicalStateVersion() != null
                    || handoff.getProposalId() != null
                    || handoff.getCommitStatus() != null
                    || handoff.getCommitAuditId() != null) {
                return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
            }
            if (currentClinicalStateVersion != handoff.getSourceClinicalStateVersion()) {
                return U04AdmissionResult.failed(STALE_U03_HANDOFF);
            }
        } else {
            return U04AdmissionResult.failed(MALFORMED_U03_HANDOFF);
        }

        return U04AdmissionResult.accepted(
                new U04AdmittedInput(
                        handoff,
                        currentClinicalStateVersion,
                        scopeContext,
                        environmentId));
    }

    private static List<String> expectedGovernedRefs() {
        return Arrays.asList(
                U03GovernedCandidateGateway.BINDING_ID,
                U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);
    }

    private static boolean validDisposition(String value) {
        return "NO_HIGH_RISK_SIGNAL".equals(value)
                || "CAUTION".equals(value)
                || "HIGH_RISK".equals(value);
    }

    private static void requireNonProduction(String value) {
        if (blank(value)) throw new IllegalArgumentException("environmentId is required");
        String normalized = value.trim().toLowerCase();
        if ("prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-")) {
            throw new IllegalStateException("production environment is not authorized for U04 runtime");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
