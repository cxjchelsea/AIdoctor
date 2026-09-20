package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Narrow P01 adapter for U03 typed proposals. */
public final class U03CommitService {
    private static final String U03_RISK_PATH = "/patient_state/current_risk_assessment";

    private final StateCommitter stateCommitter;

    public U03CommitService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    /** Historical component-level P01 path retained for compatibility. */
    public StateTypes.CommitResult commit(U03StateProposal proposal) {
        validateCommonProposal(proposal);
        return stateCommitter.commit(proposal.getStatePatch());
    }

    /**
     * Authorized CD-07 non-production P01 path.
     *
     * <p>This method is an admission gate only. Canonical mutation authority remains
     * exclusively inside the existing StateCommitter. It does not publish releases,
     * enable production traffic, or bypass expected-version / idempotency checks.</p>
     */
    public StateTypes.CommitResult commitNonProduction(
            U03NonProductionExecutionContext context,
            U03StateProposal proposal) {
        if (context == null) throw new IllegalArgumentException("CD-07 execution context is required");
        validateCommonProposal(proposal);
        validateNonProductionProposal(context, proposal);
        return stateCommitter.commit(proposal.getStatePatch());
    }

    private static void validateCommonProposal(U03StateProposal proposal) {
        if (proposal == null || proposal.getStatePatch() == null) {
            throw new IllegalArgumentException("U03 proposal is required");
        }
        if (proposal.getSourceDecisionRef() == null || proposal.getSourceDecisionRef().trim().isEmpty()) {
            throw new IllegalArgumentException("U03 proposal must reference its source decision");
        }
        if (proposal.getReleaseRefs().isEmpty()) {
            throw new IllegalArgumentException("U03 proposal must retain at least the attempted capability binding ref");
        }
        if (proposal.isFullReleaseEvidenceRequired() && proposal.getReleaseRefs().size() < 3) {
            throw new IllegalArgumentException("VALID U03 proposal must bind capability, rule and knowledge releases");
        }
    }

    private static void validateNonProductionProposal(
            U03NonProductionExecutionContext context,
            U03StateProposal proposal) {
        if (!U03NonProductionExecutionContext.BINDING_MODE.equals(context.getBindingMode())) {
            throw new IllegalStateException("unsupported U03 runtime binding mode");
        }
        context.getReleaseRefs().requireGateCFrozenSet();

        StateTypes.StatePatch patch = proposal.getStatePatch();
        U03ExecutionCommand command = context.getCommand();
        if (patch.baseVersion == null || patch.baseVersion.intValue() != command.clinicalStateVersion) {
            throw new IllegalStateException("U03 proposal baseVersion does not match execution Clinical State Version");
        }
        if (!command.cdpId.equals(patch.cdpId)) {
            throw new IllegalStateException("U03 proposal CDP identity does not match execution context");
        }
        if (patch.envelope == null
                || !command.correlationId.equals(patch.envelope.correlationId)
                || !command.traceId.equals(patch.envelope.traceId)) {
            throw new IllegalStateException("U03 proposal correlation identity does not match execution context");
        }
        if (!"P01".equals(patch.envelope.capabilityId)) {
            throw new IllegalStateException("CD-07 proposal must enter canonical mutation through P01");
        }
        if (patch.operations == null || patch.operations.size() != 1
                || !U03_RISK_PATH.equals(patch.operations.get(0).path)) {
            throw new IllegalStateException("unsupported U03 proposal operation set");
        }
        if (!proposal.isFullReleaseEvidenceRequired()) {
            throw new IllegalStateException("CD-07 valid commit path requires full governed release evidence");
        }

        U03ExplicitNonProductionReleaseRefs refs = context.getReleaseRefs();
        List<String> expectedReleaseRefs = Arrays.asList(
                U03GovernedCandidateGateway.BINDING_ID,
                refs.getKnowledgeReleaseRef(),
                refs.getRuleReleaseRef(),
                refs.getCoverageContractRef(),
                refs.getPolicyReleaseRef(),
                refs.getPolicyPairRef());
        if (!expectedReleaseRefs.equals(proposal.getReleaseRefs())) {
            throw new IllegalStateException("U03 proposal release evidence does not match the exact frozen release set");
        }

        Object rawValue = patch.operations.get(0).value;
        if (!(rawValue instanceof Map<?, ?>)) {
            throw new IllegalStateException("U03 proposal payload is malformed");
        }
        Map<?, ?> value = (Map<?, ?>) rawValue;
        requirePayloadRef(value, "capability_binding_ref", U03GovernedCandidateGateway.BINDING_ID);
        requirePayloadRef(value, "knowledge_release_ref", refs.getKnowledgeReleaseRef());
        requirePayloadRef(value, "rule_release_ref", refs.getRuleReleaseRef());
        requirePayloadRef(value, "coverage_contract_ref", refs.getCoverageContractRef());
        requirePayloadRef(value, "policy_release_ref", refs.getPolicyReleaseRef());
        requirePayloadRef(value, "policy_pair_ref", refs.getPolicyPairRef());
        if (!Integer.valueOf(command.clinicalStateVersion).equals(value.get("clinical_state_version"))) {
            throw new IllegalStateException("U03 proposal payload Clinical State Version mismatch");
        }
    }

    private static void requirePayloadRef(Map<?, ?> value, String field, String expected) {
        if (!expected.equals(value.get(field))) {
            throw new IllegalStateException("U03 proposal payload " + field + " mismatch");
        }
    }
}
