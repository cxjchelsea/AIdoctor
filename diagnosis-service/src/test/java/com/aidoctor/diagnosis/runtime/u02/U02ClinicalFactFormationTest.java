package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.state.committer.StatePatchBoundaryValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class U02ClinicalFactFormationTest {
    @Test
    void unknownWithResolvedConceptRemainsExplicitFactNotNegative() {
        U02ClinicalFactBusinessOwner owner = new U02ClinicalFactBusinessOwner();
        C01U02CapabilityResponse response = response(observation("UNKNOWN", "UNCERTAIN", "PATIENT_REPORTED"));

        U02ClinicalFactDecision decision = owner.decide("consult-1", "event-1", response);

        assertFalse(decision.isClarificationRequired());
        assertEquals("UNKNOWN", decision.getAcceptedObservations().get(0).getValueSemantics());
        assertNotEquals("NO", decision.getAcceptedObservations().get(0).getValueSemantics());
    }

    @Test
    void unresolvedConceptRequiresClarificationAndCannotBecomeProposal() {
        C01U02CapabilityResponse.ObservationCandidate unresolved = observation("UNKNOWN", "UNCERTAIN", "PATIENT_REPORTED");
        unresolved.setConceptId(null);
        U02ClinicalFactDecision decision = new U02ClinicalFactBusinessOwner()
                .decide("consult-1", "event-1", response(unresolved));

        assertTrue(decision.isClarificationRequired());
        assertTrue(decision.getAcceptedObservations().isEmpty());
    }

    @Test
    void typedPatientFactKeepsStructureSourceAndPassesStatePatchBoundary() {
        U02ClinicalFactDecision decision = new U02ClinicalFactBusinessOwner()
                .decide("consult-1", "event-1", response(observation("UNMEASURED", "UNCERTAIN", "PATIENT_REPORTED")));
        U02ClinicalFactProposal proposal = new U02ClinicalFactProposalFactory().create(
                "cdp-1", 3, "trace-1", "corr-1", decision, binding());

        StateTypes.StatePatch patch = proposal.getStatePatch();
        assertEquals(Integer.valueOf(3), patch.baseVersion);
        assertEquals("P01", patch.envelope.capabilityId);
        assertEquals(1, patch.operations.size());
        assertTrue(patch.operations.get(0).path.startsWith("/patient_state/clinical_fact_"));
        assertEquals("PATIENT_REPORTED", patch.operations.get(0).source);
        assertTrue(patch.operations.get(0).value instanceof Map<?, ?>);
        Map<?, ?> fact = (Map<?, ?>) patch.operations.get(0).value;
        assertEquals("UNMEASURED", fact.get("value_semantics"));
        assertEquals("PATIENT_REPORTED", fact.get("source_type"));
        assertEquals("U02_CLINICAL_FACT_OWNER", proposal.getBusinessOwner());
        assertEquals(Collections.singletonList("c01-u02-v1-active"), proposal.getCapabilityBindingRefs());
        assertTrue(new StatePatchBoundaryValidator().validate(patch).valid);
    }

    @Test
    void modelInferenceBecomesDerivedAssertionNotPatientFact() {
        U02ClinicalFactDecision decision = new U02ClinicalFactBusinessOwner()
                .decide("consult-1", "event-model", response(observation("YES", "NORMALIZED", "MODEL_INFERRED")));
        U02ClinicalFactProposal proposal = new U02ClinicalFactProposalFactory().create(
                "cdp-1", 3, "trace-model", "corr-model", decision, binding());

        StateTypes.StatePatchOperation operation = proposal.getStatePatch().operations.get(0);
        assertTrue(operation.path.startsWith("/patient_state/derived_clinical_assertion_"));
        assertEquals("MODEL_INFERRED", operation.source);
        Map<?, ?> value = (Map<?, ?>) operation.value;
        assertEquals("MODEL_INFERRED", value.get("source_type"));
        assertTrue(new StatePatchBoundaryValidator().validate(proposal.getStatePatch()).valid);
    }

    @Test
    void d05IsSafeNoOpWithoutDownstreamArtifacts() {
        U02DependencyInvalidationHook hook = new U02DependencyInvalidationHook();
        assertTrue(hook.invalidate("consult-1", "event-1", "obs-1", 4, Collections.emptyList()).isEmpty());
    }

    @Test
    void d05MapsOnlyRegisteredDependentArtifactsToInvalidationEffects() {
        U02DependencyInvalidationHook hook = new U02DependencyInvalidationHook();
        List<U02DependencyInvalidationHook.InvalidationRecord> records = hook.invalidate(
                "consult-1", "event-1", "obs-1", 4,
                Arrays.asList(
                        new U02DependencyInvalidationHook.DependentArtifact("RISK", "risk-1"),
                        new U02DependencyInvalidationHook.DependentArtifact("DDX", "ddx-1"),
                        new U02DependencyInvalidationHook.DependentArtifact("DELIVERY", "delivery-1")));

        assertEquals("STALE", records.get(0).targetStatus);
        assertEquals("INVALIDATED", records.get(1).targetStatus);
        assertEquals("SUPERSEDED", records.get(2).targetStatus);
        assertEquals(4, records.get(0).targetClinicalStateVersion);
    }

    @Test
    void d05RejectsUnsupportedArtifactTypeInsteadOfInventingStateEffect() {
        U02DependencyInvalidationHook hook = new U02DependencyInvalidationHook();

        assertThrows(IllegalArgumentException.class,
                () -> hook.invalidate(
                        "consult-1", "event-1", "obs-1", 4,
                        Collections.singletonList(
                                new U02DependencyInvalidationHook.DependentArtifact("UNKNOWN_MODULE", "artifact-1"))));
    }

    private C01U02CapabilityResponse response(C01U02CapabilityResponse.ObservationCandidate observation) {
        C01U02CapabilityResponse response = new C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setObservationCandidates(Collections.singletonList(observation));
        return response;
    }

    private C01U02CapabilityResponse.ObservationCandidate observation(String value, String lifecycle, String sourceType) {
        C01U02CapabilityResponse.ObservationCandidate observation = new C01U02CapabilityResponse.ObservationCandidate();
        observation.setObservationId("obs-1");
        observation.setConceptId("CUI:C0012833");
        observation.setConceptDisplay("头晕");
        observation.setRawTextRef("sha256:abc");
        observation.setValueSemantics(value);
        observation.setSourceType(sourceType);
        observation.setLifecycle(lifecycle);
        observation.setConfidenceOrUncertainty(0.8d);
        observation.setProvenance(Collections.singletonList("candidate"));
        observation.setAmbiguityFlags(Collections.emptyList());
        observation.setContradictionRefs(Collections.emptyList());
        return observation;
    }

    private CapabilityBindingRecord binding() {
        return new CapabilityBindingRecord(
                "c01-u02-v1-active", "C01", "c01-u02-1.0.0", "aidoctor-v1-u02",
                "aidoctor-v1-scope", "contracts-v1",
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-01T00:00:00"), null,
                LocalDateTime.parse("2026-09-01T00:00:00"));
    }
}
