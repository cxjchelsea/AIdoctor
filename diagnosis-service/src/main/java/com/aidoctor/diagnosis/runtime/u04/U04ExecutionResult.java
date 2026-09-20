package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.contracts.v1.StateTypes;

/** U04 component execution result. Contains eligibility only; no downstream owner execution. */
public final class U04ExecutionResult {
    public static final String ADMISSION_FAILED = "ADMISSION_FAILED";
    public static final String COMMIT_COMPLETE = "COMMIT_COMPLETE";
    public static final String COMMIT_NOT_COMPLETE = "COMMIT_NOT_COMPLETE";

    private final String status;
    private final U04AdmissionResult admission;
    private final U04SafetyGateDecision decision;
    private final U04StateProposal proposal;
    private final StateTypes.CommitResult commitResult;
    private final U04RoutingEligibility routingEligibility;

    public U04ExecutionResult(
            String status,
            U04AdmissionResult admission,
            U04SafetyGateDecision decision,
            U04StateProposal proposal,
            StateTypes.CommitResult commitResult,
            U04RoutingEligibility routingEligibility) {
        this.status = status;
        this.admission = admission;
        this.decision = decision;
        this.proposal = proposal;
        this.commitResult = commitResult;
        this.routingEligibility = routingEligibility;
    }

    public String getStatus() { return status; }
    public U04AdmissionResult getAdmission() { return admission; }
    public U04SafetyGateDecision getDecision() { return decision; }
    public U04StateProposal getProposal() { return proposal; }
    public StateTypes.CommitResult getCommitResult() { return commitResult; }
    public U04RoutingEligibility getRoutingEligibility() { return routingEligibility; }
}
