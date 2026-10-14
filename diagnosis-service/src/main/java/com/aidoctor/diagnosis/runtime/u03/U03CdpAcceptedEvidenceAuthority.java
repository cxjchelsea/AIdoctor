package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Non-production adapter that derives evidence identity ONLY from committed CDP
 * clinical facts. A host-owned attestation must independently accept the exact
 * source/version/call tuple; this adapter never mints acceptance authority.
 *
 * A missing Gate-C fact is not an ABSENT/NO result. Downstream clinical-input
 * mapping must preserve UNKNOWN/UNMEASURED and separately verify coverage.
 */
public final class U03CdpAcceptedEvidenceAuthority
        implements U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority {
    public interface ClinicalEvidenceAttestation {
        String requireAcceptanceRef(String consultationId, String cdpId, int version,
                String u02CallId, List<String> evidence, List<String> sources,
                List<String> provenance);
    }

    private final CDPRepository cdps;
    private final ClinicalEvidenceAttestation attestation;

    public U03CdpAcceptedEvidenceAuthority(CDPRepository cdps,
            ClinicalEvidenceAttestation attestation) {
        if (cdps == null || attestation == null)
            throw new IllegalArgumentException("CDP and independent attestation required");
        this.cdps = cdps;
        this.attestation = attestation;
    }

    @Override
    public U03AcceptedEvidenceBinding requireAccepted(String consultationId,
            String cdpId, int version, String u02CallId) {
        required(consultationId); required(cdpId); required(u02CallId);
        CDP cdp = cdps.findById(cdpId)
                .orElseThrow(() -> blocked("CDP_MISSING"));
        if (cdp.getVersion() == null || cdp.getVersion().intValue() != version)
            throw blocked("CDP_STALE");
        Map<String, Object> state = cdp.getPatientState();
        if (state == null) throw blocked("PATIENT_STATE_MISSING");
        Set<String> ids = new LinkedHashSet<String>();
        Set<String> sources = new LinkedHashSet<String>();
        Set<String> provenance = new LinkedHashSet<String>();
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            if (!entry.getKey().startsWith("clinical_fact_")) continue;
            if (!(entry.getValue() instanceof Map<?, ?>))
                throw blocked("FACT_MALFORMED");
            Map<?, ?> fact = (Map<?, ?>) entry.getValue();
            String ref = strict(fact.get("concept_id"), "CONCEPT_ID");
            // Only actual, mapped Gate-C evidence identifiers can be accepted.
            if (!ref.startsWith("EV-")) continue;
            if (!"PATIENT_REPORTED".equals(fact.get("source_type")))
                throw blocked("EVIDENCE_SOURCE_NOT_ACCEPTED");
            String semantic = strict(fact.get("value_semantics"), "VALUE_SEMANTICS");
            if (!("YES".equals(semantic) || "NO".equals(semantic)
                    || "UNKNOWN".equals(semantic) || "UNMEASURED".equals(semantic)
                    || "NOT_ASKED".equals(semantic)))
                throw blocked("EVIDENCE_SEMANTICS_UNMAPPED");
            String raw = strict(fact.get("raw_text_ref"), "RAW_TEXT_REF");
            String decision = strict(fact.get("source_decision_ref"), "DECISION_REF");
            if (!ids.add(ref)) throw blocked("DUPLICATE_GATE_C_EVIDENCE");
            sources.add(raw);
            provenance.add(decision);
        }
        if (ids.isEmpty()) throw blocked("NO_ACCEPTABLE_GATE_C_EVIDENCE");
        List<String> evidence = new ArrayList<String>(ids);
        List<String> sourceList = new ArrayList<String>(sources);
        List<String> provenanceList = new ArrayList<String>(provenance);
        String acceptanceRef = attestation.requireAcceptanceRef(
                consultationId, cdpId, version, u02CallId, evidence, sourceList, provenanceList);
        required(acceptanceRef);
        return new U03AcceptedEvidenceBinding(acceptanceRef, version, evidence,
                sourceList, provenanceList);
    }

    private static String strict(Object value, String field) {
        if (!(value instanceof String) || ((String) value).trim().isEmpty())
            throw blocked(field + "_MISSING");
        return (String) value;
    }
    private static void required(String value) {
        if (value == null || value.trim().isEmpty())
            throw blocked("AUTHORITY_ID_REQUIRED");
    }
    private static IllegalStateException blocked(String reason) {
        return new IllegalStateException("U03_CDP_EVIDENCE_BLOCKED:" + reason);
    }
}
