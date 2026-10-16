package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class U03CdpGateCClinicalInputPortTest {
    private CDPRepository cdps;
    private U03CdpGateCClinicalInputPort.ScopeAuthority scope;
    private U03CdpGateCClinicalInputPort port;
    private CDP cdp;
    private U03ExecutionCommand command;
    private U03AcceptedEvidenceBinding accepted;

    @BeforeEach void setup() {
        cdps = mock(CDPRepository.class);
        scope = mock(U03CdpGateCClinicalInputPort.ScopeAuthority.class);
        port = new U03CdpGateCClinicalInputPort(cdps, scope);
        cdp = CDP.builder().id("cdp").patientId("synthetic").version(2).build();
        when(cdps.findById("cdp")).thenReturn(Optional.of(cdp));
        command = new U03ExecutionCommand("consult","thread","run","event","cdp",2,"corr","trace");
        accepted = new U03AcceptedEvidenceBinding("accepted",2,
                Arrays.asList("EV-RF-RESP-001"), Collections.singletonList("sha256:source"),
                Collections.singletonList("decision"));
        when(scope.requireVerifiedScope(command,accepted)).thenAnswer(inv -> U03GateCClinicalInput.builder());
    }

    @Test void noEvidenceNeverInventsNegativeFacts() {
        cdp.setPatientState(Collections.<String,Object>emptyMap());
        U03GateCClinicalInput input = port.load(command,accepted);
        assertEquals("UNKNOWN", input.evidenceState("EV-RF-RESP-001"));
        assertEquals("UNKNOWN", input.evidenceState("EV-MNM-NEURO-001"));
        assertEquals("UNMEASURED", input.measurement("respiratory_rate_bpm").getState());
    }

    @Test void unknownU02FactIsNotNo() {
        cdp.setPatientState(Collections.<String,Object>singletonMap(
                "clinical_fact_one", fact("EV-RF-RESP-001", "UNKNOWN","PATIENT_REPORTED")));
        U03GateCClinicalInput input=port.load(command,accepted);
        assertEquals("UNKNOWN",input.evidenceState("EV-RF-RESP-001"));
    }

    @Test void yesAndNoRemainDistinct() {
        cdp.setPatientState(Collections.<String,Object>singletonMap(
                "clinical_fact_one", fact("EV-RF-RESP-001", "YES","PATIENT_REPORTED")));
        assertEquals("PRESENT",port.load(command,accepted).evidenceState("EV-RF-RESP-001"));
        cdp.setPatientState(Collections.<String,Object>singletonMap(
                "clinical_fact_one", fact("EV-RF-RESP-001", "NO","PATIENT_REPORTED")));
        assertEquals("ABSENT",port.load(command,accepted).evidenceState("EV-RF-RESP-001"));
    }

    @Test void missingScopeIsNotAuthorizedByBuilderDefaults() {
        when(scope.requireVerifiedScope(command,accepted)).thenReturn(null);
        cdp.setPatientState(Collections.<String,Object>emptyMap());
        assertTrue(assertThrows(IllegalStateException.class,()->port.load(command,accepted))
                .getMessage().contains("SCOPE_UNVERIFIED"));
    }

    @Test void noAttestedSourceCannotBeRead() {
        cdp.setPatientState(Collections.<String,Object>singletonMap(
                "clinical_fact_one",fact("EV-RF-RESP-001","YES","MODEL_INFERRED")));
        assertTrue(assertThrows(IllegalStateException.class,()->port.load(command,accepted))
                .getMessage().contains("EVIDENCE_PROVENANCE_MISMATCH"));
    }

    @Test void staleVersionBlocksBeforeScopeAuthorization() {
        cdp.setVersion(3);
        assertTrue(assertThrows(IllegalStateException.class,()->port.load(command,accepted))
                .getMessage().contains("STALE_CLINICAL_STATE"));
        verifyNoInteractions(scope);
    }

    @Test void conflictingMultiTurnFactsRequireGovernedSupersession() {
        Map<String,Object> state = new LinkedHashMap<String,Object>();
        state.put("clinical_fact_round_one",
                fact("EV-RF-RESP-001", "NO", "PATIENT_REPORTED"));
        state.put("clinical_fact_round_two",
                fact("EV-RF-RESP-001", "YES", "PATIENT_REPORTED"));
        cdp.setPatientState(state);
        // Neither a last-write-wins heuristic nor an implicit YES override
        // is an authorized clinical truth rule. Current mapper fails closed
        // until supersession and evidence acceptance are explicitly governed.
        assertTrue(assertThrows(IllegalStateException.class, () -> port.load(command, accepted))
                .getMessage().contains("DUPLICATE_EVIDENCE"));
    }

    private static Map<String,Object> fact(String ref,String semantic,String source) {
        Map<String,Object> m=new LinkedHashMap<String,Object>();
        m.put("concept_id",ref);m.put("value_semantics",semantic);
        m.put("source_type",source);m.put("raw_text_ref","sha256:source");
        m.put("source_decision_ref","decision");
        return m;
    }
}
