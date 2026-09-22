package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds version-bound U04 Safety Gate proposals without mutating canonical state. */
public final class U04StateProposalFactory {
    public static final String U04_GATE_PATH = "/patient_state/safety_gate";

    public U04StateProposal create(
            U04AdmittedInput input,
            U04SafetyGateDecision decision) {
        if (input == null || decision == null) {
            throw new IllegalArgumentException("U04 proposal inputs are required");
        }
        if (decision.getClinicalStateVersion() != input.getCurrentClinicalStateVersion()) {
            throw new IllegalStateException("U04 decision Clinical State Version mismatch");
        }
        if (!input.getScopeContext().getPolicyRef().equals(decision.getPolicyRef())) {
            throw new IllegalStateException("U04 decision policy ref mismatch");
        }

        U03OutboundHandoff handoff = input.getHandoff();
        String proposalId = "u04-proposal-" + safe(handoff.getEventId());
        String now = Instant.now().toString();

        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = proposalId;
        envelope.correlationId = handoff.getCorrelationId();
        envelope.traceId = handoff.getTraceId();
        envelope.createdAt = now;
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = "P01";
        envelope.capabilityVersion = "1.0.0";

        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("safety_gate", decision.getGate());
        value.put("reason_code", decision.getReasonCode());
        value.put("clinical_state_version", Integer.valueOf(input.getCurrentClinicalStateVersion()));
        value.put("source_u03_decision_ref", handoff.getDecisionId());
        value.put("source_u03_status", handoff.getDecisionStatus());
        value.put("source_u03_disposition", handoff.getDispositionCode());
        value.put("u04_policy_ref", decision.getPolicyRef());
        value.put("governed_release_refs", handoff.getGovernedReleaseRefs());
        value.put("acceptance_ref", handoff.getAcceptanceRef());

        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "ADD";
        operation.path = U04_GATE_PATH;
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";

        List<String> evidenceRefs = new ArrayList<String>();
        evidenceRefs.add(handoff.getEventId());
        evidenceRefs.add(handoff.getDecisionId());
        evidenceRefs.add(decision.getDecisionId());
        evidenceRefs.add(handoff.getAcceptanceRef());
        evidenceRefs.addAll(handoff.getAcceptedEvidenceRefs());
        evidenceRefs.addAll(handoff.getAcceptedProvenanceRefs());

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = handoff.getCdpId();
        patch.baseVersion = Integer.valueOf(input.getCurrentClinicalStateVersion());
        patch.patchId = proposalId;
        patch.idempotencyKey = "u04-event-" + safe(handoff.getEventId());
        patch.operations = Arrays.asList(operation);
        patch.reasonCode = "U04_CURRENT_VERSION_SAFETY_GATE";
        patch.evidenceRefs = evidenceRefs;
        patch.producer = "diagnosis-service";
        patch.createdAt = now;

        return new U04StateProposal(
                proposalId,
                decision.getDecisionId(),
                decision.getPolicyRef(),
                patch);
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("opaque identifier is required");
        }
        return value.replaceAll("[^A-Za-z0-9._:-]", "_");
    }
}
