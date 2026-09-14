package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class U03StateProposalFactory {
    public U03StateProposal create(U03ExecutionCommand command, U03DecisionOutcome decision,
            U03GovernedCandidateGateway.GovernedResult governed) {
        if (command == null || decision == null || governed == null) throw new IllegalArgumentException("U03 inputs are required");
        String proposalId = "u03-proposal-" + command.eventId;
        String now = Instant.now().toString();

        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = proposalId;
        envelope.correlationId = command.correlationId;
        envelope.traceId = command.traceId;
        envelope.createdAt = now;
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = "P01";
        envelope.capabilityVersion = "1.0.0";

        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("assessment_status", decision.getStatus());
        value.put("outcome_code", decision.getOutcomeCode());
        value.put("reason_code", decision.getReasonCode());
        value.put("clinical_state_version", Integer.valueOf(command.clinicalStateVersion));
        value.put("source_decision_ref", decision.getDecisionId());
        value.put("capability_binding_ref", governed.getCapabilityBinding().getBindingId());
        value.put("rule_release_ref", governed.getReleaseBinding().getRuleReleaseId());
        value.put("knowledge_release_ref", governed.getReleaseBinding().getKnowledgeReleaseId());
        value.put("evidence_refs", decision.getEvidenceRefs());

        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "REPLACE";
        operation.path = "/patient_state/current_risk_assessment";
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = command.cdpId;
        patch.baseVersion = Integer.valueOf(command.clinicalStateVersion);
        patch.patchId = proposalId;
        patch.idempotencyKey = "u03-event-" + command.eventId.replaceAll("[^A-Za-z0-9._:-]", "_");
        patch.operations = Arrays.asList(operation);
        patch.reasonCode = "U03_CURRENT_VERSION_RISK_ASSESSMENT";
        patch.evidenceRefs = decision.getEvidenceRefs();
        patch.producer = "diagnosis-service";
        patch.createdAt = now;

        return new U03StateProposal(proposalId, decision.getDecisionId(),
                Arrays.asList(governed.getCapabilityBinding().getBindingId(),
                        governed.getReleaseBinding().getRuleReleaseId(),
                        governed.getReleaseBinding().getKnowledgeReleaseId()), patch);
    }
}
