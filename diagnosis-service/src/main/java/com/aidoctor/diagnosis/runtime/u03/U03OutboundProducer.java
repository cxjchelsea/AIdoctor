package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * S14 producer for the frozen U03 -> future-U04 interface boundary.
 *
 * <p>This class performs no U04 invocation or routing. It only constructs a typed,
 * provenance-bound U03 handoff after validating that the source execution/result is
 * internally consistent. A VALID result that would reference canonical Clinical
 * State requires a successful governed commit. Stale/conflicted/uncommitted valid
 * paths fail closed and do not produce an outbound payload.</p>
 */
public final class U03OutboundProducer {
    private static final String COMMITTED = "COMMITTED";

    /**
     * Produces an outbound boundary for a completed governed U03 result.
     *
     * <p>For FAILED C02/D09 execution, proposal and commitResult must both be null;
     * the failure facts are preserved without inventing a disposition. For VALID
     * execution, both are required and the commit must be COMMITTED.</p>
     */
    public U03OutboundHandoff produce(
            U03NonProductionExecutionContext context,
            U03GovernedCandidateGateway.GovernedResult governed,
            U03DecisionOutcome decision,
            U03StateProposal proposal,
            StateTypes.CommitResult commitResult) {
        if (context == null || governed == null || decision == null) {
            throw new IllegalArgumentException("U03 outbound inputs are required");
        }

        U03ExecutionCommand command = context.getCommand();
        U03AcceptedEvidenceBinding accepted = context.requireAcceptedEvidenceBinding();
        U03ResolvedNonProductionReleaseSet resolved = governed.getResolvedNonProductionReleaseSet();
        if (resolved == null) {
            throw new IllegalStateException("S14 outbound requires the resolved non-production release set");
        }
        resolved.getRefs().requireGateCFrozenSet();
        if (governed.getCapabilityBinding() == null
                || !resolved.getCapabilityBindingId().equals(governed.getCapabilityBinding().getBindingId())) {
            throw new IllegalStateException("S14 outbound capability/release binding mismatch");
        }

        U03RiskAssessmentCandidate candidate = governed.getCandidate();
        if (candidate == null) {
            throw new IllegalStateException("S14 outbound requires the governed C02 result");
        }

        verifyDecisionIdentity(candidate, decision);
        List<String> governedRefs = expectedGovernedRefs(governed, resolved);

        if (candidate.isFailed()) {
            if (proposal != null || commitResult != null) {
                throw new IllegalStateException(
                        "FAILED U03 execution must not be mislabeled as a committed outbound state");
            }
            return build(
                    context,
                    accepted,
                    candidate,
                    decision,
                    governed.getCapabilityBinding().getBindingId(),
                    governedRefs,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        verifyValidExecution(context, accepted, candidate, decision);
        if (proposal == null || commitResult == null) {
            throw new IllegalStateException(
                    "VALID U03 outbound requires the governed K09 proposal and StateCommitter result");
        }
        verifyProposal(context, governedRefs, decision, proposal);
        verifyCommittedBoundary(command, commitResult);

        String auditId = commitResult.auditRef == null ? null : commitResult.auditRef.auditId;
        if (auditId == null || auditId.trim().isEmpty()) {
            throw new IllegalStateException("COMMITTED U03 outbound requires commit audit identity");
        }

        return build(
                context,
                accepted,
                candidate,
                decision,
                governed.getCapabilityBinding().getBindingId(),
                governedRefs,
                proposal.getProposalId(),
                commitResult.status,
                commitResult.reasonCode,
                commitResult.committedVersion,
                auditId);
    }

    private static void verifyDecisionIdentity(
            U03RiskAssessmentCandidate candidate,
            U03DecisionOutcome decision) {
        if (!candidate.getStatus().equals(decision.getStatus())) {
            throw new IllegalStateException("S14 outbound C02/D09 status mismatch");
        }
        if (candidate.isFailed()) {
            if (decision.getOutcomeCode() != null) {
                throw new IllegalStateException("FAILED U03 outbound must not carry a normal disposition");
            }
            if (!candidate.getFailureReasonCode().equals(decision.getReasonCode())) {
                throw new IllegalStateException("FAILED U03 outbound failure reason mismatch");
            }
        }
    }

    private static void verifyValidExecution(
            U03NonProductionExecutionContext context,
            U03AcceptedEvidenceBinding accepted,
            U03RiskAssessmentCandidate candidate,
            U03DecisionOutcome decision) {
        U03ExecutionCommand command = context.getCommand();
        if (!U03RiskAssessmentCandidate.VALID.equals(candidate.getStatus())) {
            throw new IllegalStateException("Unsupported U03 outbound execution status");
        }
        if (candidate.getClinicalStateVersion() == null
                || candidate.getClinicalStateVersion().intValue() != command.clinicalStateVersion) {
            throw new IllegalStateException("S14 outbound C02 Clinical State Version mismatch");
        }
        if (!accepted.getEvidenceRefs().equals(candidate.getEvidenceRefs())) {
            throw new IllegalStateException("S14 outbound C02 evidence does not match accepted evidence");
        }
        if (!candidate.getEvidenceRefs().equals(decision.getEvidenceRefs())) {
            throw new IllegalStateException("S14 outbound D09 evidence does not match C02 evidence");
        }
        if (!candidate.getSourceRefs().containsAll(accepted.getSourceRefs())) {
            throw new IllegalStateException("S14 outbound lost accepted evidence source refs");
        }
        if (!candidate.getProvenance().containsAll(accepted.getProvenanceRefs())) {
            throw new IllegalStateException("S14 outbound lost accepted evidence provenance refs");
        }
    }

    private static void verifyProposal(
            U03NonProductionExecutionContext context,
            List<String> governedRefs,
            U03DecisionOutcome decision,
            U03StateProposal proposal) {
        U03ExecutionCommand command = context.getCommand();
        if (!decision.getDecisionId().equals(proposal.getSourceDecisionRef())) {
            throw new IllegalStateException("S14 outbound K09 proposal does not reference the D09 decision");
        }
        if (proposal.getStatePatch() == null
                || proposal.getStatePatch().baseVersion == null
                || proposal.getStatePatch().baseVersion.intValue() != command.clinicalStateVersion) {
            throw new IllegalStateException("S14 outbound K09 baseVersion mismatch");
        }
        if (!command.cdpId.equals(proposal.getStatePatch().cdpId)) {
            throw new IllegalStateException("S14 outbound K09 CDP identity mismatch");
        }
        if (proposal.getStatePatch().envelope == null
                || !command.correlationId.equals(proposal.getStatePatch().envelope.correlationId)
                || !command.traceId.equals(proposal.getStatePatch().envelope.traceId)) {
            throw new IllegalStateException("S14 outbound K09 correlation identity mismatch");
        }
        if (!proposal.isFullReleaseEvidenceRequired()
                || proposal.getReleaseRefs().size() != governedRefs.size()
                || !proposal.getReleaseRefs().containsAll(governedRefs)
                || !governedRefs.containsAll(proposal.getReleaseRefs())) {
            throw new IllegalStateException("S14 outbound K09 proposal lacks the exact governed release set");
        }
    }

    private static void verifyCommittedBoundary(
            U03ExecutionCommand command,
            StateTypes.CommitResult commitResult) {
        if (!COMMITTED.equals(commitResult.status)) {
            throw new IllegalStateException(
                    "S14 outbound valid path requires COMMITTED canonical Clinical State");
        }
        if (commitResult.previousVersion == null
                || commitResult.previousVersion.intValue() != command.clinicalStateVersion) {
            throw new IllegalStateException("S14 outbound commit previousVersion mismatch");
        }
        if (commitResult.committedVersion == null
                || commitResult.committedVersion.intValue() <= command.clinicalStateVersion) {
            throw new IllegalStateException("S14 outbound requires a newer committed Clinical State Version");
        }
    }

    private static U03OutboundHandoff build(
            U03NonProductionExecutionContext context,
            U03AcceptedEvidenceBinding accepted,
            U03RiskAssessmentCandidate candidate,
            U03DecisionOutcome decision,
            String capabilityBindingId,
            List<String> governedRefs,
            String proposalId,
            String commitStatus,
            String commitReasonCode,
            Integer committedVersion,
            String commitAuditId) {
        U03ExecutionCommand command = context.getCommand();
        return new U03OutboundHandoff(
                command.consultationId,
                command.cdpId,
                command.clinicalStateVersion,
                committedVersion,
                command.threadId,
                command.runId,
                command.eventId,
                command.correlationId,
                command.traceId,
                context.getEnvironmentId(),
                context.getBindingMode(),
                candidate.getStatus(),
                candidate.getFailureReasonCode(),
                candidate.getLimitations(),
                candidate.getUncertainty(),
                decision.getDecisionId(),
                decision.getStatus(),
                decision.getOutcomeCode(),
                decision.getReasonCode(),
                capabilityBindingId,
                governedRefs,
                accepted.getAcceptanceRef(),
                accepted.getEvidenceRefs(),
                accepted.getSourceRefs(),
                accepted.getProvenanceRefs(),
                proposalId,
                commitStatus,
                commitReasonCode,
                commitAuditId);
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
