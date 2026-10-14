package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fail-closed, non-production Gate-C input mapping from persisted U02 facts.
 * Neither missing facts nor UNKNOWN/UNMEASURED become negative evidence.
 * Scope, demographics and supported measurements require a separate,
 * host-authorized source, not inferred patient defaults.
 */
public final class U03CdpGateCClinicalInputPort implements U03GateCClinicalInputPort {
    public interface ScopeAuthority {
        U03GateCClinicalInput.Builder requireVerifiedScope(
                U03ExecutionCommand command, U03AcceptedEvidenceBinding accepted);
    }

    private static final java.util.List<String> RULE_EVIDENCE = java.util.Collections.unmodifiableList(
            Arrays.asList("EV-RF-RESP-001", "EV-MNM-NEURO-001", "EV-MNM-NEURO-002",
                    "EV-MNM-CARD-001", "EV-RF-ALLERGY-001", "EV-RF-APPEAR-001",
                    "EV-RF-NEURO-001", "EV-VS-SEPSIS-001", "EV-VS-SEPSIS-002",
                    "EV-VS-SEPSIS-003", "EV-RF-SEPSIS-001"));

    private final CDPRepository cdps;
    private final ScopeAuthority scopeAuthority;

    public U03CdpGateCClinicalInputPort(CDPRepository cdps, ScopeAuthority scopeAuthority) {
        if (cdps == null || scopeAuthority == null) throw new IllegalArgumentException("Trusted ports required");
        this.cdps = cdps;
        this.scopeAuthority = scopeAuthority;
    }

    @Override
    public U03GateCClinicalInput load(U03ExecutionCommand command, U03AcceptedEvidenceBinding accepted) {
        if (command == null || accepted == null
                || command.clinicalStateVersion != accepted.getClinicalStateVersion())
            throw blocked("ACCEPTED_VERSION_MISMATCH");
        CDP cdp = cdps.findById(command.cdpId).orElseThrow(() -> blocked("CDP_NOT_FOUND"));
        if (cdp.getVersion() == null || cdp.getVersion().intValue() != command.clinicalStateVersion)
            throw blocked("STALE_CLINICAL_STATE");

        U03GateCClinicalInput.Builder b = scopeAuthority.requireVerifiedScope(command, accepted);
        if (b == null) throw blocked("SCOPE_UNVERIFIED");

        Map<String,String> byEvidence = new LinkedHashMap<String,String>();
        Map<String,Object> state = cdp.getPatientState();
        if (state == null) throw blocked("PATIENT_STATE_NOT_FOUND");
        for (Map.Entry<String,Object> entry : state.entrySet()) {
            if (!entry.getKey().startsWith("clinical_fact_")) continue;
            if (!(entry.getValue() instanceof Map<?, ?>)) throw blocked("MALFORMED_FACT");
            Map<?,?> fact = (Map<?,?>)entry.getValue();
            Object concept = fact.get("concept_id");
            if (!(concept instanceof String) || !RULE_EVIDENCE.contains(concept)) continue;
            String evidenceRef = (String)concept;
            if (!accepted.getEvidenceRefs().contains(evidenceRef)) continue;
            if (!"PATIENT_REPORTED".equals(fact.get("source_type"))
                    || !(fact.get("raw_text_ref") instanceof String)
                    || !accepted.getSourceRefs().contains(fact.get("raw_text_ref"))
                    || !(fact.get("source_decision_ref") instanceof String)
                    || !accepted.getProvenanceRefs().contains(fact.get("source_decision_ref")))
                throw blocked("EVIDENCE_PROVENANCE_MISMATCH");
            String value = semantic(fact.get("value_semantics"));
            if (byEvidence.put(evidenceRef, value) != null)
                throw blocked("DUPLICATE_EVIDENCE");
        }
        // Every missing frozen Gate-C evidence is explicitly UNKNOWN.
        // Gate-C must decide INPUT_INSUFFICIENT, never silently NO_MATCH.
        for (String ref : RULE_EVIDENCE) {
            b.evidence(ref, byEvidence.containsKey(ref) ? byEvidence.get(ref) : "UNKNOWN");
        }
        return b.build();
    }

    private static String semantic(Object value) {
        if ("YES".equals(value)) return "PRESENT";
        if ("NO".equals(value)) return "ABSENT";
        if ("UNKNOWN".equals(value) || "UNMEASURED".equals(value)
                || "NOT_ASKED".equals(value) || "AMBIGUOUS".equals(value)
                || "CONFLICTING".equals(value) || "NOT_ESTABLISHED".equals(value))
            return "UNKNOWN";
        throw blocked("UNMAPPED_VALUE_SEMANTICS");
    }

    private static IllegalStateException blocked(String why) {
        return new IllegalStateException("U03_CDP_GATE_C_MAPPING_BLOCKED:" + why);
    }
}
