package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

/** End-to-end result for the authorized U05 non-production application slice. */
public final class U05ExecutionResult {
    public static final String ADMISSION_REJECTED = "ADMISSION_REJECTED";
    public static final String D03_INPUT_FAILURE = "D03_INPUT_FAILURE";
    public static final String D03_INPUT_CONFLICT = "D03_INPUT_CONFLICT";
    public static final String POLICY_EXPECTATION_GAP = "POLICY_EXPECTATION_GAP";
    public static final String COMMIT_NOT_COMPLETE = "COMMIT_NOT_COMPLETE";
    public static final String ROUTING_COMPLETE = "ROUTING_COMPLETE";

    private final String status;
    private final U05AdmissionResult admission;
    private final U05ClinicalReadinessDecision decision;
    private final U05ReadinessStateProposal proposal;
    private final StateTypes.CommitResult commitResult;
    private final U05ClinicalReadinessCommitEvidence commitEvidence;
    private final U05DownstreamRoutingDecision routingDecision;
    private final String sentinelCode;

    public U05ExecutionResult(
            String status,
            U05AdmissionResult admission,
            U05ClinicalReadinessDecision decision,
            U05ReadinessStateProposal proposal,
            StateTypes.CommitResult commitResult,
            U05ClinicalReadinessCommitEvidence commitEvidence,
            U05DownstreamRoutingDecision routingDecision,
            String sentinelCode) {
        this.status = status;
        this.admission = admission;
        this.decision = decision;
        this.proposal = proposal;
        this.commitResult = commitResult;
        this.commitEvidence = commitEvidence;
        this.routingDecision = routingDecision;
        this.sentinelCode = sentinelCode;
    }

    public String getStatus() { return status; }
    public U05AdmissionResult getAdmission() { return admission; }
    public U05ClinicalReadinessDecision getDecision() { return decision; }
    public U05ReadinessStateProposal getProposal() { return proposal; }
    public StateTypes.CommitResult getCommitResult() { return commitResult; }
    public U05ClinicalReadinessCommitEvidence getCommitEvidence() { return commitEvidence; }
    public U05DownstreamRoutingDecision getRoutingDecision() { return routingDecision; }
    public String getSentinelCode() { return sentinelCode; }
}
