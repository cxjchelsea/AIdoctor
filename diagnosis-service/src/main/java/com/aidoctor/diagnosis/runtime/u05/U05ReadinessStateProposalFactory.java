package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds stable version-bound U05 Clinical Readiness proposals without mutating state. */
public final class U05ReadinessStateProposalFactory {
    public static final String READINESS_PATH = "/patient_state/clinical_readiness";
    public static final String EFFECT_CONTRACT_VERSION = "U05-RDP03-EFFECT-V1";

    public U05ReadinessStateProposal create(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision decision) {
        if (input == null || decision == null) throw new IllegalArgumentException("U05 proposal inputs are required");
        if (!decision.isDecided()) throw new IllegalStateException("only DECIDED D03 may create readiness proposal");
        validateBindings(input, decision);

        String effectId = U05Ids.hash(
                "u05-readiness-effect",
                input.getConsultationId(),
                input.getCdpId(),
                input.getAdmissionId(),
                decision.getDecisionId(),
                String.valueOf(input.getClinicalStateVersion()),
                input.getEvaluationContext(),
                input.getAcceptedReadinessInputSetIdentity(),
                decision.getClinicalReadiness(),
                U05ClinicalReadinessPolicy.POLICY_ID,
                decision.getPolicyVersion(),
                decision.getPolicyRuleRef(),
                input.getAcceptedU04GateRef(),
                input.getAcceptedRouteAuthorizationType(),
                input.getAcceptedRouteAuthorizationRef(),
                input.getAcceptedRestrictedContextRef(),
                EFFECT_CONTRACT_VERSION);

        String readinessRecordId = U05Ids.hash("u05-readiness-record", effectId);
        String proposalId = U05Ids.hash("u05-readiness-proposal", effectId);
        String expectedCurrentReadinessRecordRef = input.getSourceCurrentReadinessRecordRef();
        String mutationOperation =
                expectedCurrentReadinessRecordRef == null ? "ADD" : "REPLACE";

        List<String> dependencyRefs = new ArrayList<String>();
        dependencyRefs.add(input.getAcceptedU04GateRef());
        dependencyRefs.add(input.getAcceptedRouteAuthorizationRef());
        dependencyRefs.add(input.getAcceptedReadinessInputManifestRef());
        dependencyRefs.addAll(input.getAcceptedReadinessInputRefs());
        if (input.getAcceptedRestrictedContextRef() != null) dependencyRefs.add(input.getAcceptedRestrictedContextRef());
        if (input.getAcceptedRestrictedPermissionRef() != null) dependencyRefs.add(input.getAcceptedRestrictedPermissionRef());

        String payloadFingerprint = U05Ids.hash(
                "u05-readiness-payload",
                decision.getClinicalReadiness(),
                input.getConsultationId(),
                input.getCdpId(),
                String.valueOf(input.getClinicalStateVersion()),
                input.getEvaluationContext(),
                input.getAdmissionId(),
                decision.getDecisionId(),
                input.getAcceptedReadinessInputSetIdentity(),
                join(input.getAcceptedReadinessInputRefs()),
                input.getAcceptedU04GateRef(),
                input.getAcceptedRouteAuthorizationType(),
                input.getAcceptedRouteAuthorizationRef(),
                input.getAcceptedRestrictedContextRef(),
                U05ClinicalReadinessPolicy.POLICY_ID,
                decision.getPolicyVersion(),
                decision.getPolicyRuleRef(),
                join(decision.getRuleReleaseRefs()),
                join(decision.getKnowledgeReleaseRefs()),
                join(dependencyRefs),
                "CURRENT",
                effectId,
                mutationOperation,
                expectedCurrentReadinessRecordRef);

        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = proposalId;
        envelope.correlationId = input.getCorrelationId();
        envelope.traceId = input.getTraceId();
        envelope.createdAt = input.getAdmittedCreatedAt();
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = "P01";
        envelope.capabilityVersion = "1.0.0";

        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("readiness_record_id", readinessRecordId);
        value.put("clinical_readiness", decision.getClinicalReadiness());
        value.put("consultation_id", input.getConsultationId());
        value.put("cdp_id", input.getCdpId());
        value.put("derived_from_clinical_state_version", Integer.valueOf(input.getClinicalStateVersion()));
        value.put("evaluation_context", input.getEvaluationContext());
        value.put("source_admission_id", input.getAdmissionId());
        value.put("source_d03_decision_ref", decision.getDecisionId());
        value.put("source_d03_decision_status", decision.getDecisionStatus());
        value.put("source_readiness_input_manifest_ref", input.getAcceptedReadinessInputManifestRef());
        value.put("source_readiness_input_set_identity", input.getAcceptedReadinessInputSetIdentity());
        value.put("source_readiness_input_refs", input.getAcceptedReadinessInputRefs());
        value.put("source_u04_gate_ref", input.getAcceptedU04GateRef());
        value.put("source_route_authorization_type", input.getAcceptedRouteAuthorizationType());
        value.put("source_route_authorization_ref", input.getAcceptedRouteAuthorizationRef());
        value.put("source_restricted_context_ref", input.getAcceptedRestrictedContextRef());
        value.put("source_restricted_permission_ref", input.getAcceptedRestrictedPermissionRef());
        value.put("policy_id", U05ClinicalReadinessPolicy.POLICY_ID);
        value.put("policy_version", decision.getPolicyVersion());
        value.put("policy_rule_ref", decision.getPolicyRuleRef());
        value.put("rule_release_refs", decision.getRuleReleaseRefs());
        value.put("knowledge_release_refs", decision.getKnowledgeReleaseRefs());
        value.put("readiness_dependency_refs", dependencyRefs);
        value.put("state_validity", "CURRENT");
        value.put("effect_id", effectId);
        value.put("proposal_ref", proposalId);
        value.put("created_at", input.getAdmittedCreatedAt());
        value.put("canonical_payload_fingerprint", payloadFingerprint);

        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = mutationOperation;
        operation.path = READINESS_PATH;
        operation.value = value;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";

        List<String> evidenceRefs = new ArrayList<String>();
        evidenceRefs.add(input.getAdmissionId());
        evidenceRefs.add(decision.getDecisionId());
        evidenceRefs.add(input.getAcceptedReadinessInputManifestRef());
        evidenceRefs.addAll(input.getAcceptedReadinessInputRefs());
        evidenceRefs.add(input.getAcceptedU04GateRef());
        evidenceRefs.add(input.getAcceptedRouteAuthorizationRef());
        if (expectedCurrentReadinessRecordRef != null) evidenceRefs.add(expectedCurrentReadinessRecordRef);
        if (input.getAcceptedRestrictedContextRef() != null) evidenceRefs.add(input.getAcceptedRestrictedContextRef());
        if (input.getAcceptedRestrictedPermissionRef() != null) evidenceRefs.add(input.getAcceptedRestrictedPermissionRef());

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = input.getCdpId();
        patch.baseVersion = Integer.valueOf(input.getClinicalStateVersion());
        patch.patchId = proposalId;
        patch.idempotencyKey = "u05-readiness-effect-" + effectId;
        patch.operations = Arrays.asList(operation);
        patch.reasonCode = "U05_CLINICAL_READINESS_COMMIT";
        patch.evidenceRefs = evidenceRefs;
        patch.producer = "diagnosis-service";
        patch.createdAt = input.getAdmittedCreatedAt();

        return new U05ReadinessStateProposal(
                effectId,
                readinessRecordId,
                proposalId,
                payloadFingerprint,
                decision.getDecisionId(),
                mutationOperation,
                expectedCurrentReadinessRecordRef,
                patch);
    }

    private static void validateBindings(
            U05AdmittedInput input,
            U05ClinicalReadinessDecision decision) {
        if (!input.getAdmissionId().equals(decision.getSourceAdmissionRef())) {
            throw new IllegalStateException("D03 source_admission_ref mismatch");
        }
        if (!input.getAcceptedReadinessInputSetIdentity().equals(decision.getSourceReadinessInputSetIdentity())) {
            throw new IllegalStateException("D03 readiness_input_set_identity mismatch");
        }
        if (!input.getAcceptedU04GateRef().equals(decision.getSafetyGateRef())) {
            throw new IllegalStateException("D03 safety_gate_ref mismatch");
        }
        if (!equal(input.getAcceptedRestrictedContextRef(), decision.getRestrictedContextRef())) {
            throw new IllegalStateException("D03 restricted_context_ref mismatch");
        }
        if (!equal(input.getAcceptedRestrictedPermissionRef(), decision.getRestrictedPermissionRef())) {
            throw new IllegalStateException("D03 restricted_permission_ref mismatch");
        }
        if (input.getClinicalStateVersion() != decision.getInputClinicalStateVersion()) {
            throw new IllegalStateException("D03 input version mismatch");
        }
    }

    private static boolean equal(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) out.append(value == null ? "<null>" : value).append('|');
        }
        return out.toString();
    }
}
