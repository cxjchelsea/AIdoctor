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
    private static final Set<String> GOVERNED_FAILURE_REASONS = new HashSet<String>(Arrays.asList(
            "RELEASE_MISMATCH",
            "STALE_INPUT",
            "INVALID_INPUT",
            "DEPENDENCY_FAILURE",
            "OVERALL_POLICY_SCOPE_MISMATCH",
            "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED",
            "INSUFFICIENT_INFORMATION",
            "UNRESOLVABLE_CONFLICT"));

    private final U03DecisionPort validDecisionPort;
    private final U03EvidenceAcceptanceService evidenceAcceptanceService;

    public U03DecisionService(U03DecisionPort validDecisionPort) {
        if (validDecisionPort == null) throw new IllegalArgumentException("validDecisionPort is required");
        this.validDecisionPort = validDecisionPort;
        this.evidenceAcceptanceService = new U03EvidenceAcceptanceService();
    }

    /** Historical component-level decision path. */
    public U03DecisionOutcome decide(U03ExecutionCommand command, U03RiskAssessmentCandidate candidate,
            U03ReleaseBinding releaseBinding) {
        if (command == null || candidate == null) throw new IllegalArgumentException("U03 decision inputs are required");
        if (candidate.isFailed()) return failedOutcome(command, candidate);
        if (releaseBinding == null || !releaseBinding.isActive()) {
            throw new IllegalStateException("VALID U03 decision requires an active release binding");
        }
        U03DecisionOutcome decision = validDecisionPort.decide(command, candidate, releaseBinding);
        return validateValidDecision(candidate, decision);
    }

    /**
     * Authorized CD-07 non-production D09 path.
     *
     * <p>Only a C02 result accepted against the exact execution identity, accepted
     * evidence, capability binding, and Gate-C-frozen release set may enter D09.
     * The D09 implementation must explicitly support this governed non-production
     * contract; the historical port cannot silently ignore coverage/policy refs.</p>
     */
    public U03DecisionOutcome decide(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governedResult) {
        if (context == null || governedResult == null) {
            throw new IllegalArgumentException("CD-07 D09 inputs are required");
        }

        U03RiskAssessmentCandidate accepted = evidenceAcceptanceService.accept(context, governedResult);
        if (accepted.isFailed()) return failedOutcome(context.getCommand(), accepted);

        if (!(validDecisionPort instanceof U03NonProductionDecisionPort)) {
            throw new IllegalStateException(
                    "CD-07 explicit runtime requires a governed non-production D09 decision port");
        }

        U03ResolvedNonProductionReleaseSet resolved =
                governedResult.getResolvedNonProductionReleaseSet();
        if (resolved == null) {
            throw new IllegalStateException("CD-07 D09 requires the resolved non-production release set");
        }
        resolved.getRefs().requireGateCFrozenSet();

        U03DecisionOutcome decision = ((U03NonProductionDecisionPort) validDecisionPort).decide(
                context,
                accepted,
                resolved);
        U03DecisionOutcome validated = validateGovernedDecision(accepted, decision);
        if (!accepted.getEvidenceRefs().equals(validated.getEvidenceRefs())) {
            throw new IllegalStateException("D09 decision did not preserve accepted C02 evidence refs");
        }
        return validated;
    }

    private static U03DecisionOutcome failedOutcome(
            U03ExecutionCommand command,
            U03RiskAssessmentCandidate candidate) {
        return new U03DecisionOutcome(
                "u03-decision-" + command.eventId,
                U03RiskAssessmentCandidate.FAILED,
                null,
                candidate.getFailureReasonCode(),
                candidate.getEvidenceRefs());
    }

    private static U03DecisionOutcome validateGovernedDecision(
            U03RiskAssessmentCandidate candidate,
            U03DecisionOutcome decision) {
        if (decision == null) throw new IllegalStateException("D09 returned null decision");
        if (U03RiskAssessmentCandidate.VALID.equals(decision.getStatus())) {
            return validateValidDecision(candidate, decision);
        }
        if (!U03RiskAssessmentCandidate.FAILED.equals(decision.getStatus())) {
            throw new IllegalStateException("D09 returned unsupported status: " + decision.getStatus());
        }
        if (decision.getOutcomeCode() != null) {
            throw new IllegalStateException("D09 FAILED outcome must not carry a normal disposition");
        }
        if (!GOVERNED_FAILURE_REASONS.contains(decision.getReasonCode())) {
            throw new IllegalStateException("D09 returned unsupported failure reason: " + decision.getReasonCode());
        }
        return decision;
    }

    private static U03DecisionOutcome validateValidDecision(
            U03RiskAssessmentCandidate candidate,
            U03DecisionOutcome decision) {
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
