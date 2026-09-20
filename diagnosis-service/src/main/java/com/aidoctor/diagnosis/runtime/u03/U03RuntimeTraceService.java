package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * S11/P05 correlation service for the authorized CD-07 non-production runtime.
 *
 * <p>Trace is written after runtime facts exist. A trace sink failure is returned as
 * an operational failure only; this service never rewrites a D09/K09/commit result
 * and has no canonical Clinical State mutation authority.</p>
 */
public final class U03RuntimeTraceService {
    private final U03RuntimeTracePort tracePort;

    public U03RuntimeTraceService(U03RuntimeTracePort tracePort) {
        if (tracePort == null) throw new IllegalArgumentException("tracePort is required");
        this.tracePort = tracePort;
    }

    public U03RuntimeTraceWriteOutcome record(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governed,
            U03DecisionOutcome decision,
            U03StateProposal proposal,
            StateTypes.CommitResult commitResult) {
        U03RuntimeTraceRecord record = buildRecord(context, governed, decision, proposal, commitResult);
        try {
            tracePort.record(record);
            return U03RuntimeTraceWriteOutcome.recorded();
        } catch (RuntimeException ignored) {
            return U03RuntimeTraceWriteOutcome.failed();
        }
    }

    U03RuntimeTraceRecord buildRecord(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governed,
            U03DecisionOutcome decision,
            U03StateProposal proposal,
            StateTypes.CommitResult commitResult) {
        if (context == null || governed == null || decision == null || proposal == null || commitResult == null) {
            throw new IllegalArgumentException("U03 trace correlation inputs are required");
        }

        U03ExecutionCommand command = context.getCommand();
        U03AcceptedEvidenceBinding accepted = context.requireAcceptedEvidenceBinding();
        U03ResolvedNonProductionReleaseSet resolved = governed.getResolvedNonProductionReleaseSet();
        if (resolved == null) {
            throw new IllegalStateException("P05 trace requires the resolved CD-07 non-production release set");
        }
        resolved.getRefs().requireGateCFrozenSet();

        U03RiskAssessmentCandidate candidate = governed.getCandidate();
        if (candidate == null) throw new IllegalStateException("P05 trace requires the C02 candidate/result");
        if (!accepted.getEvidenceRefs().equals(candidate.getEvidenceRefs())) {
            throw new IllegalStateException("P05 trace input C02 evidence does not match accepted evidence");
        }
        if (!candidate.getEvidenceRefs().equals(decision.getEvidenceRefs())) {
            throw new IllegalStateException("P05 trace input D09 evidence does not match C02 evidence");
        }
        if (!decision.getDecisionId().equals(proposal.getSourceDecisionRef())) {
            throw new IllegalStateException("P05 trace input K09 proposal does not reference the D09 decision");
        }
        if (proposal.getStatePatch() == null || proposal.getStatePatch().baseVersion == null
                || proposal.getStatePatch().baseVersion.intValue() != command.clinicalStateVersion) {
            throw new IllegalStateException("P05 trace input K09 baseVersion does not match execution state version");
        }
        if (!command.cdpId.equals(proposal.getStatePatch().cdpId)) {
            throw new IllegalStateException("P05 trace input K09 proposal does not match execution CDP");
        }
        if (proposal.getStatePatch().envelope == null
                || !command.correlationId.equals(proposal.getStatePatch().envelope.correlationId)
                || !command.traceId.equals(proposal.getStatePatch().envelope.traceId)) {
            throw new IllegalStateException("P05 trace input K09 correlation identity mismatch");
        }

        List<String> expectedRefs = expectedGovernedRefs(governed, resolved);
        if (!proposal.getReleaseRefs().containsAll(expectedRefs)) {
            throw new IllegalStateException("P05 trace input K09 proposal does not retain the full governed release set");
        }

        String auditId = commitResult.auditRef == null ? null : commitResult.auditRef.auditId;
        return new U03RuntimeTraceRecord(
                command.consultationId,
                command.threadId,
                command.runId,
                command.eventId,
                command.cdpId,
                command.clinicalStateVersion,
                command.correlationId,
                command.traceId,
                context.getEnvironmentId(),
                context.getBindingMode(),
                accepted.getAcceptanceRef(),
                accepted.getEvidenceRefs(),
                accepted.getSourceRefs(),
                accepted.getProvenanceRefs(),
                governed.getCapabilityBinding().getBindingId(),
                expectedRefs,
                candidate.getStatus(),
                candidate.getFailureReasonCode(),
                candidate.getRuleReleaseId(),
                candidate.getKnowledgeReleaseId(),
                decision.getDecisionId(),
                decision.getStatus(),
                decision.getOutcomeCode(),
                decision.getReasonCode(),
                proposal.getProposalId(),
                proposal.getSourceDecisionRef(),
                proposal.getStatePatch().patchId,
                proposal.getStatePatch().idempotencyKey,
                proposal.getStatePatch().baseVersion.intValue(),
                commitResult.status,
                commitResult.reasonCode,
                commitResult.previousVersion,
                commitResult.committedVersion,
                auditId);
    }

    private static List<String> expectedGovernedRefs(
            U03GovernedCandidateGateway.GovernedResult governed,
            U03ResolvedNonProductionReleaseSet resolved) {
        U03ExplicitNonProductionReleaseRefs refs = resolved.getRefs();
        return new ArrayList<String>(Arrays.asList(
                governed.getCapabilityBinding().getBindingId(),
                refs.getKnowledgeReleaseRef(),
                refs.getRuleReleaseRef(),
                refs.getCoverageContractRef(),
                refs.getPolicyReleaseRef(),
                refs.getPolicyPairRef()));
    }
}
