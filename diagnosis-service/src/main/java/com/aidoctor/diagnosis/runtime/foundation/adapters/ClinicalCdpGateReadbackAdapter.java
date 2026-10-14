package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u04.U04ScopeContext;
import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authoritative, read-only U04 gate evidence from the current JPA CDP row.
 *
 * <p>Opt-in and fail-closed. The CDP itself is NOT an A1/F3 authority issuer:
 * no A1 routing/barrier approval can be synthesized from its fields.
 */
@Component
@ConditionalOnProperty(prefix = "aidoctor.u07.persistence",
        name = "authority-readback-enabled", havingValue = "true", matchIfMissing = false)
public final class ClinicalCdpGateReadbackAdapter
        implements U04A1U05AuthorityBridge.GateReadbackPort {
    private final CDPRepository repository;

    public ClinicalCdpGateReadbackAdapter(CDPRepository repository) {
        if (repository == null) throw new IllegalArgumentException("CDP repository required");
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public U04A1U05AuthorityBridge.GateReadback loadCurrent(String cdpId) {
        if (cdpId == null || cdpId.trim().isEmpty())
            throw new IllegalArgumentException("U07_GATE_CDP_ID_REQUIRED");
        CDP cdp = repository.findById(cdpId)
                .orElseThrow(() -> new IllegalStateException("U07_GATE_CDP_NOT_FOUND"));
        Integer version = cdp.getVersion();
        if (version == null || version.intValue() < 0)
            throw new IllegalStateException("U07_GATE_VERSION_INVALID");
        Map<String, Object> state = cdp.getPatientState();
        if (state == null || !(state.get("safety_gate") instanceof Map<?, ?>))
            throw new IllegalStateException("U07_GATE_EVIDENCE_MISSING");
        Map<?, ?> gate = (Map<?, ?>) state.get("safety_gate");
        String gateValue = requiredString(gate.get("safety_gate"), "U07_GATE_VALUE_INVALID");
        if (!("ALLOW".equals(gateValue) || "RESTRICTED".equals(gateValue)
                || "BLOCK".equals(gateValue)))
            throw new IllegalStateException("U07_GATE_VALUE_INVALID");
        String sourceRef = requiredString(gate.get("source_u03_decision_ref"),
                "U07_GATE_PROVENANCE_MISSING");
        String policyRef = requiredString(gate.get("u04_policy_ref"),
                "U07_GATE_POLICY_MISSING");
        if (!U04ScopeContext.FROZEN_POLICY_REF.equals(policyRef))
            throw new IllegalStateException("U07_GATE_POLICY_UNTRUSTED");
        return new U04A1U05AuthorityBridge.GateReadback(
                version.intValue(), gateValue, sourceRef, policyRef);
    }

    private static String requiredString(Object raw, String failure) {
        if (!(raw instanceof String) || ((String) raw).trim().isEmpty())
            throw new IllegalStateException(failure);
        return (String) raw;
    }
}
