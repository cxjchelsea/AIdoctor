package com.aidoctor.diagnosis.runtime.u03;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * U03 business-owner boundary. Failure handling is deterministic here; valid clinical
 * outcome selection is delegated to the separately governed D09 implementation.
 */
public final class U03DecisionService {
    private static final Set<String> VALID_OUTCOMES = new HashSet<String>(Arrays.asList(
            "NO_HIGH_RISK_SIGNAL", "CAUTION", "HIGH_RISK"));

    private final U03DecisionPort validDecisionPort;

    public U03DecisionService(U03DecisionPort validDecisionPort) {
        if (validDecisionPort == null) throw new IllegalArgumentException("validDecisionPort is required");
        this.validDecisionPort = validDecisionPort;
    }

    public U03DecisionOutcome decide(U03ExecutionCommand command, U03RiskAssessmentCandidate candidate,
            U03ReleaseBinding releaseBinding) {
        if (command == null || candidate == null) throw new IllegalArgumentException("U03 decision inputs are required");
        if (candidate.isFailed()) {
            return new U03DecisionOutcome(
                    "u03-decision-" + command.eventId,
                    U03RiskAssessmentCandidate.FAILED,
                    null,
                    candidate.getFailureReasonCode(),
                    candidate.getEvidenceRefs());
        }
        if (releaseBinding == null || !releaseBinding.isActive()) {
            throw new IllegalStateException("VALID U03 decision requires an active release binding");
        }
        U03DecisionOutcome decision = validDecisionPort.decide(command, candidate, releaseBinding);
        if (decision == null) throw new IllegalStateException("D09 returned null decision");
        if (!U03RiskAssessmentCandidate.VALID.equals(decision.getStatus())) {
            throw new IllegalStateException("D09 valid path must return VALID status");
        }
        if (!VALID_OUTCOMES.contains(decision.getOutcomeCode())) {
            throw new IllegalStateException("D09 returned unsupported outcome: " + decision.getOutcomeCode());
        }
        return decision;
    }
}
