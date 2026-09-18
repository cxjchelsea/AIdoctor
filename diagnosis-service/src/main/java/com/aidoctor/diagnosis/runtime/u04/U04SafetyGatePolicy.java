package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.runtime.u03.U03RiskAssessmentCandidate;

/** Deterministic implementation of the owner-approved RDP-02 policy. */
public final class U04SafetyGatePolicy {
    public U04SafetyGateDecision decide(U04AdmittedInput input) {
        if (input == null) throw new IllegalArgumentException("admitted input is required");
        U03OutboundHandoff handoff = input.getHandoff();
        U04ScopeContext scope = input.getScopeContext();
        scope.requireFrozenPolicy();

        if (U03RiskAssessmentCandidate.VALID.equals(handoff.getDecisionStatus())
                && "HIGH_RISK".equals(handoff.getDispositionCode())) {
            return decision(input, U04SafetyGateDecision.BLOCKED, "U04_HIGH_RISK_BLOCKED");
        }

        if (U03RiskAssessmentCandidate.FAILED.equals(handoff.getDecisionStatus())) {
            return decision(
                    input,
                    U04SafetyGateDecision.UNAVAILABLE,
                    "U04_RISK_ASSESSMENT_UNAVAILABLE");
        }

        // Current approved RDP-05 declares no additional Safety Capability.
        // Therefore no additional dependency branch may be introduced here.

        if (!scope.isEstablished() || !scope.isApplicable()) {
            return decision(input, U04SafetyGateDecision.UNAVAILABLE, "U04_SCOPE_UNAVAILABLE");
        }

        if (U03RiskAssessmentCandidate.VALID.equals(handoff.getDecisionStatus())
                && "CAUTION".equals(handoff.getDispositionCode())) {
            return decision(input, U04SafetyGateDecision.RESTRICTED, "U04_CAUTION_RESTRICTED");
        }

        if (U03RiskAssessmentCandidate.VALID.equals(handoff.getDecisionStatus())
                && "NO_HIGH_RISK_SIGNAL".equals(handoff.getDispositionCode())) {
            return decision(
                    input,
                    U04SafetyGateDecision.ALLOW,
                    "U04_NO_HIGH_RISK_SIGNAL_ALLOWED");
        }

        throw new IllegalStateException(
                "SAFETY_POLICY_EXPECTATION_GAP: frozen U04 policy has no mapping for the admitted input");
    }

    private static U04SafetyGateDecision decision(
            U04AdmittedInput input,
            String gate,
            String reasonCode) {
        U03OutboundHandoff handoff = input.getHandoff();
        return new U04SafetyGateDecision(
                "u04-gate-" + safe(handoff.getEventId()),
                gate,
                reasonCode,
                input.getCurrentClinicalStateVersion(),
                input.getScopeContext().getPolicyRef(),
                handoff.getDecisionId());
    }

    private static String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
