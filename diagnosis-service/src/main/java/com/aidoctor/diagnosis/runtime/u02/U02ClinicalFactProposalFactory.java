package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts an accepted U02 business decision into a typed K09/P01 proposal. */
@Component
public class U02ClinicalFactProposalFactory {
    public U02ClinicalFactProposal create(
            String cdpId,
            int baseClinicalStateVersion,
            String traceId,
            String correlationId,
            U02ClinicalFactDecision decision,
            CapabilityBindingRecord binding) {
        if (decision == null || binding == null || decision.getAcceptedObservations().isEmpty()) {
            throw new IllegalArgumentException("Accepted decision and authorized binding are required.");
        }
        String proposalId = "u02-proposal-" + decision.getEventId();
        String now = Instant.now().toString();

        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = proposalId;
        envelope.correlationId = correlationId;
        envelope.traceId = traceId;
        envelope.createdAt = now;
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = "P01";
        envelope.capabilityVersion = "1.0.0";

        List<StateTypes.StatePatchOperation> operations = new ArrayList<StateTypes.StatePatchOperation>();
        List<String> evidenceRefs = new ArrayList<String>();
        evidenceRefs.add(decision.getEventId());
        evidenceRefs.add(decision.getDecisionId());
        for (C01U02CapabilityResponse.ObservationCandidate observation : decision.getAcceptedObservations()) {
            StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
            operation.op = "ADD";
            operation.path = "/patient_state/clinical_fact_" + safeToken(observation.getObservationId());
            operation.value = factValue(observation, decision, binding);
            operation.expectedCurrentValue = null;
            operation.source = "PATIENT_FACT";
            operation.sensitivity = "PHI";
            operations.add(operation);
            evidenceRefs.add(safeEvidence(observation.getObservationId()));
        }

        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope;
        patch.cdpId = cdpId;
        patch.baseVersion = Integer.valueOf(baseClinicalStateVersion);
        patch.patchId = proposalId;
        patch.idempotencyKey = "u02-event-" + safeToken(decision.getEventId());
        patch.operations = operations;
        patch.reasonCode = "U02_CLINICAL_FACT_FORMATION";
        patch.evidenceRefs = evidenceRefs;
        patch.producer = "diagnosis-service";
        patch.createdAt = now;

        return new U02ClinicalFactProposal(
                proposalId,
                decision.getConsultationId(),
                decision.getBusinessOwner(),
                decision.getDecisionId(),
                Arrays.asList(binding.getBindingId()),
                patch);
    }

    private static Map<String, Object> factValue(
            C01U02CapabilityResponse.ObservationCandidate observation,
            U02ClinicalFactDecision decision,
            CapabilityBindingRecord binding) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("observation_id", observation.getObservationId());
        value.put("concept_id", observation.getConceptId());
        value.put("concept_display", observation.getConceptDisplay());
        value.put("raw_text_ref", observation.getRawTextRef());
        value.put("normalized_value", observation.getNormalizedValue());
        value.put("value_semantics", observation.getValueSemantics());
        value.put("unit", observation.getUnit());
        value.put("negation", Boolean.valueOf(observation.isNegation()));
        value.put("temporality", observation.getTemporality());
        value.put("severity_or_degree", observation.getSeverityOrDegree());
        value.put("source_type", observation.getSourceType());
        value.put("lifecycle", observation.getLifecycle());
        value.put("confidence", Double.valueOf(observation.getConfidenceOrUncertainty()));
        value.put("provenance", observation.getProvenance());
        value.put("ambiguity_flags", observation.getAmbiguityFlags());
        value.put("contradiction_refs", observation.getContradictionRefs());
        value.put("source_decision_ref", decision.getDecisionId());
        value.put("capability_binding_ref", binding.getBindingId());
        return value;
    }

    private static String safeToken(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("opaque identifier is required");
        return value.replaceAll("[^A-Za-z0-9._:-]", "_");
    }

    private static String safeEvidence(String value) {
        return safeToken(value);
    }
}
