package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class U03CdpAcceptedEvidenceAuthorityTest {
    private CDPRepository cdps;
    private U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation attestation;
    private U03CdpAcceptedEvidenceAuthority resolver;
    private CDP cdp;

    @BeforeEach
    void setUp() {
        cdps = mock(CDPRepository.class);
        attestation = mock(U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation.class);
        resolver = new U03CdpAcceptedEvidenceAuthority(cdps, attestation);
        cdp = CDP.builder().id("cdp-1").patientId("synthetic").version(2).build();
        when(cdps.findById("cdp-1")).thenReturn(Optional.of(cdp));
    }

    @Test void unrelatedU02FactCannotFabricateGateCEvidence() {
        cdp.setPatientState(Collections.<String,Object>singletonMap("clinical_fact_observation",
                fact("TEST:HEADACHE", "UNKNOWN", "PATIENT_REPORTED")));
        assertTrue(assertThrows(IllegalStateException.class, () ->
                resolver.requireAccepted("consult", "cdp-1", 2, "call"))
                .getMessage().contains("NO_ACCEPTABLE_GATE_C_EVIDENCE"));
        verifyNoInteractions(attestation);
    }

    @Test void acceptedEvidenceKeepsUnknownAndRealProvenance() {
        cdp.setPatientState(Collections.<String,Object>singletonMap("clinical_fact_risk",
                fact("EV-RF-RESP-001", "UNKNOWN", "PATIENT_REPORTED")));
        when(attestation.requireAcceptanceRef(anyString(), anyString(), eq(2), anyString(),
                anyList(), anyList(), anyList())).thenReturn("host-acceptance-1");
        U03AcceptedEvidenceBinding result =
                resolver.requireAccepted("consult", "cdp-1", 2, "call");
        assertEquals(Collections.singletonList("EV-RF-RESP-001"), result.getEvidenceRefs());
        assertEquals(Collections.singletonList("sha256:source"), result.getSourceRefs());
        assertEquals(Collections.singletonList("decision-1"), result.getProvenanceRefs());
        assertEquals("host-acceptance-1", result.getAcceptanceRef());
    }

    @Test void missingAttestationAlwaysBlocks() {
        cdp.setPatientState(Collections.<String,Object>singletonMap("clinical_fact_risk",
                fact("EV-RF-RESP-001", "NO", "PATIENT_REPORTED")));
        assertThrows(IllegalStateException.class, () ->
                resolver.requireAccepted("consult", "cdp-1", 2, "call"));
    }

    @Test void staleVersionBlocksBeforeAttestation() {
        cdp.setVersion(3);
        assertTrue(assertThrows(IllegalStateException.class, () ->
                resolver.requireAccepted("consult", "cdp-1", 2, "call"))
                .getMessage().contains("CDP_STALE"));
        verifyNoInteractions(attestation);
    }

    @Test void derivedFactCannotMasqueradeAsPatientReported() {
        cdp.setPatientState(Collections.<String,Object>singletonMap("clinical_fact_risk",
                fact("EV-RF-RESP-001", "NO", "MODEL_INFERRED")));
        assertThrows(IllegalStateException.class, () ->
                resolver.requireAccepted("consult", "cdp-1", 2, "call"));
        verifyNoInteractions(attestation);
    }

    private static Map<String,Object> fact(String concept, String semantic, String source) {
        Map<String,Object> item = new LinkedHashMap<String,Object>();
        item.put("concept_id", concept);
        item.put("value_semantics", semantic);
        item.put("source_type", source);
        item.put("raw_text_ref", "sha256:source");
        item.put("source_decision_ref", "decision-1");
        return item;
    }
}
