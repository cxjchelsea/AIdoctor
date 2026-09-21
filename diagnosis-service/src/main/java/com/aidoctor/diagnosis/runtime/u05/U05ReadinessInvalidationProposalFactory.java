package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implements the frozen RDP-03 dedicated readiness invalidation effect. */
public final class U05ReadinessInvalidationProposalFactory {
    public static final String INVALIDATION_CONTRACT_VERSION = "U05-RDP03-INVALIDATION-V1";

    public U05ReadinessInvalidationProposal create(U05ReadinessInvalidationRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");

        String affected = join(request.getAffectedDependencyRefs());
        String effectId = U05Ids.hash(
                "u05-readiness-invalidation-effect",
                request.getConsultationId(),
                request.getCdpId(),
                request.getPriorReadinessRecordRef(),
                request.getPriorReadinessEffectId(),
                request.getTriggeringAuthoritativeChangeRef(),
                affected,
                request.getInvalidationReasonCode(),
                request.getSourceEventOrDecisionRef(),
                INVALIDATION_CONTRACT_VERSION);
        String proposalId = U05Ids.hash("u05-readiness-invalidation-proposal", effectId);

        Map<String, Object> stalePayload =
                new LinkedHashMap<String, Object>(request.getPriorReadinessPayload());
        stalePayload.put("state_validity", "STALE");
        stalePayload.put("invalidation_effect_ref", effectId);
        stalePayload.put("invalidation_reason_refs",
                Arrays.asList(request.getInvalidationReasonCode(), request.getSourceEventOrDecisionRef()));

        String fingerprint = U05Ids.hash(
                "u05-readiness-invalidation-payload",
                request.getPriorReadinessRecordRef(),
                request.getPriorReadinessEffectId(),
                request.getTriggeringAuthoritativeChangeRef(),
                affected,
                request.getInvalidationReasonCode(),
                request.getSourceEventOrDecisionRef(),
                "STALE",
                INVALIDATION_CONTRACT_VERSION);

        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = proposalId;
        envelope.correlationId = request.getCorrelationId();
        envelope.traceId = request.getTraceId();
        envelope.createdAt = request.getCreatedAt();
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = "P01";
        envelope.capabilityVersion = "1.0.0";

        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "REPLACE";
        operation.path = U05ReadinessStateProposalFactory.READINESS_PATH;
        operation.value = stalePayload;
        operation.expectedCurrentValue = null;
        operation.source = "RULE_DERIVED";
        operation.sensitivity = "PHI";

        List<String> evidenceRefs = new ArrayList<String>();
        evidenceRefs.add(request.getPriorReadinessRecordRef());
        evidenceRefs.add(request.getPriorReadinessEffectId());
        evidenceRefs.add(request.getTriggeringAuthoritativeChangeRef());
        evidenceRefs.addAll(request.getAffectedDependencyRefs());
        evidenceRefs.add(request.getSourceEventOrDecisionRef());

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = request.getCdpId();
        patch.baseVersion = Integer.valueOf(request.getBaseClinicalStateVersion());
        patch.patchId = proposalId;
        patch.idempotencyKey = "u05-readiness-invalidation-" + effectId;
        patch.operations = Arrays.asList(operation);
        patch.reasonCode = "U05_CLINICAL_READINESS_INVALIDATION";
        patch.evidenceRefs = evidenceRefs;
        patch.producer = "diagnosis-service";
        patch.createdAt = request.getCreatedAt();

        return new U05ReadinessInvalidationProposal(
                effectId, proposalId, fingerprint, patch);
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) out.append(value).append('|');
        return out.toString();
    }
}
