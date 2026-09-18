package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

import java.util.Map;

/** Narrow non-production StateCommitter adapter for U04 Safety Gate proposals. */
public final class U04CommitService {
    private final StateCommitter stateCommitter;

    public U04CommitService(StateCommitter stateCommitter) {
        if (stateCommitter == null) throw new IllegalArgumentException("stateCommitter is required");
        this.stateCommitter = stateCommitter;
    }

    public StateTypes.CommitResult commitNonProduction(
            U04AdmittedInput input,
            U04SafetyGateDecision decision,
            U04StateProposal proposal) {
        if (input == null || decision == null || proposal == null) {
            throw new IllegalArgumentException("U04 commit inputs are required");
        }
        requireNonProduction(input.getEnvironmentId());
        validate(input, decision, proposal);
        return stateCommitter.commit(proposal.getStatePatch());
    }

    private static void validate(
            U04AdmittedInput input,
            U04SafetyGateDecision decision,
            U04StateProposal proposal) {
        U03OutboundHandoff handoff = input.getHandoff();
        StateTypes.StatePatch patch = proposal.getStatePatch();

        if (!decision.getDecisionId().equals(proposal.getSourceDecisionRef())) {
            throw new IllegalStateException("U04 proposal does not reference the Gate decision");
        }
        if (!decision.getPolicyRef().equals(proposal.getPolicyRef())
                || !U04ScopeContext.FROZEN_POLICY_REF.equals(proposal.getPolicyRef())) {
            throw new IllegalStateException("U04 proposal policy ref mismatch");
        }
        if (decision.getClinicalStateVersion() != input.getCurrentClinicalStateVersion()) {
            throw new IllegalStateException("U04 decision version mismatch");
        }
        if (patch.baseVersion == null
                || patch.baseVersion.intValue() != input.getCurrentClinicalStateVersion()) {
            throw new IllegalStateException("U04 proposal baseVersion mismatch");
        }
        if (!handoff.getCdpId().equals(patch.cdpId)) {
            throw new IllegalStateException("U04 proposal CDP identity mismatch");
        }
        if (patch.envelope == null
                || !handoff.getCorrelationId().equals(patch.envelope.correlationId)
                || !handoff.getTraceId().equals(patch.envelope.traceId)
                || !"P01".equals(patch.envelope.capabilityId)) {
            throw new IllegalStateException("U04 proposal correlation/P01 identity mismatch");
        }
        if (patch.operations == null
                || patch.operations.size() != 1
                || !U04StateProposalFactory.U04_GATE_PATH.equals(patch.operations.get(0).path)) {
            throw new IllegalStateException("unsupported U04 proposal operation set");
        }
        Object raw = patch.operations.get(0).value;
        if (!(raw instanceof Map<?, ?>)) {
            throw new IllegalStateException("U04 proposal payload is malformed");
        }
        Map<?, ?> value = (Map<?, ?>) raw;
        requireValue(value, "safety_gate", decision.getGate());
        requireValue(value, "reason_code", decision.getReasonCode());
        requireValue(
                value,
                "clinical_state_version",
                Integer.valueOf(input.getCurrentClinicalStateVersion()));
        requireValue(value, "source_u03_decision_ref", handoff.getDecisionId());
        requireValue(value, "u04_policy_ref", U04ScopeContext.FROZEN_POLICY_REF);
        if (!handoff.getGovernedReleaseRefs().equals(value.get("governed_release_refs"))) {
            throw new IllegalStateException("U04 proposal governed release refs mismatch");
        }
    }

    private static void requireValue(Map<?, ?> value, String field, Object expected) {
        if (!expected.equals(value.get(field))) {
            throw new IllegalStateException("U04 proposal " + field + " mismatch");
        }
    }

    private static void requireNonProduction(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("environmentId is required");
        }
        String normalized = value.trim().toLowerCase();
        if ("prod".equals(normalized)
                || "production".equals(normalized)
                || normalized.startsWith("prod-")
                || normalized.startsWith("production-")) {
            throw new IllegalStateException("production environment is not authorized for U04 runtime");
        }
    }
}
