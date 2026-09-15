package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds version-bound K09 proposals for both VALID and FAILED U03 outcomes. */
public final class U03StateProposalFactory {
    public U03StateProposal createValid(U03ExecutionCommand command, U03DecisionOutcome decision,
            U03GovernedCandidateGateway.GovernedResult governed) {
        if (command == null || decision == null || governed == null) throw new IllegalArgumentException("U03 inputs are required");
        if (!U03RiskAssessmentCandidate.VALID.equals(decision.getStatus())) {
            throw new IllegalArgumentException("createValid requires VALID decision status");
        }
        return build(command, decision,
                Arrays.asList(governed.getCapabilityBinding().getBindingId(),
                        governed.getReleaseBinding().getRuleReleaseId(),
                        governed.getReleaseBinding().getKnowledgeReleaseId()),
                true,
                governed.getCapabilityBinding().getBindingId(),
                governed.getReleaseBinding().getRuleReleaseId(),
                governed.getReleaseBinding().getKnowledgeReleaseId());
    }

    public U03StateProposal createFailed(U03ExecutionCommand command, U03DecisionOutcome decision,
            String attemptedBindingId) {
        if (command == null || decision == null) throw new IllegalArgumentException("U03 inputs are required");
        if (!U03RiskAssessmentCandidate.FAILED.equals(decision.getStatus())) {
            throw new IllegalArgumentException("createFailed requires FAILED decision status");
        }
        String bindingRef = required(attemptedBindingId, "attemptedBindingId");
        return build(command, decision, Arrays.asList(bindingRef), false, bindingRef, null, null);
    }

    public U03StateProposal createFailed(U03ExecutionCommand command, U03DecisionOutcome decision,
            U03GovernedCandidateGateway.GovernedResult governed) {
        if (command == null || decision == null || governed == null) throw new IllegalArgumentException("U03 inputs are required");
        if (!U03RiskAssessmentCandidate.FAILED.equals(decision.getStatus())) {
            throw new IllegalArgumentException("createFailed requires FAILED decision status");
        }
        return build(command, decision,
                Arrays.asList(governed.getCapabilityBinding().getBindingId(),
                        governed.getReleaseBinding().getRuleReleaseId(),
                        governed.getReleaseBinding().getKnowledgeReleaseId()),
                false,
                governed.getCapabilityBinding().getBindingId(),
                governed.getReleaseBinding().getRuleReleaseId(),
                governed.getReleaseBinding().getKnowledgeReleaseId());
    }

    private U03StateProposal build(U03ExecutionCommand command, U03DecisionOutcome decision,
            List<String> releaseRefs, boolean fullReleaseEvidenceRequired,
            String capabilityBindingRef, String ruleReleaseRef, String knowledgeReleaseRef) {
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
        value.put("capability_binding_ref", capabilityBindingRef);
        value.put("rule_release_ref", ruleReleaseRef);
        value.put("knowledge_release_ref", knowledgeReleaseRef);
        value.put("evidence_refs", decision.getEvidenceRefs());

        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "ADD";
        operation.path = "/patient_state/current_risk_assessment";
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";

        List<String> patchEvidence = new ArrayList<String>();
        patchEvidence.add(command.eventId);
        patchEvidence.add(decision.getDecisionId());
        patchEvidence.addAll(decision.getEvidenceRefs());

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = command.cdpId;
        patch.baseVersion = Integer.valueOf(command.clinicalStateVersion);
        patch.patchId = proposalId;
        patch.idempotencyKey = "u03-event-" + safeToken(command.eventId);
        patch.operations = Arrays.asList(operation);
        patch.reasonCode = "U03_CURRENT_VERSION_RISK_ASSESSMENT";
        patch.evidenceRefs = patchEvidence;
        patch.producer = "diagnosis-service";
        patch.createdAt = now;

        return new U03StateProposal(proposalId, decision.getDecisionId(), releaseRefs,
                fullReleaseEvidenceRequired, patch);
    }

    private static String safeToken(String value) {
        return required(value, "opaque identifier").replaceAll("[^A-Za-z0-9._:-]", "_");
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
