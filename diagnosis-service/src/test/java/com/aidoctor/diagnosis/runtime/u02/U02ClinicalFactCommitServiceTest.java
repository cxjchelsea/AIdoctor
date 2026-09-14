package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticCapabilityPolicyFake;
import com.aidoctor.diagnosis.state.committer.support.StateCommitterTestHarness;
import com.aidoctor.diagnosis.state.committer.support.SyntheticStatePatchFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class U02ClinicalFactCommitServiceTest {
    @Test
    void typedClinicalFactCrossesP01MechanicalCommitBoundaryExactlyOnce() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        U02ClinicalFactDecision decision = new U02ClinicalFactBusinessOwner()
                .decide("consult-1", "event-commit-1", response());
        U02ClinicalFactProposal proposal = new U02ClinicalFactProposalFactory().create(
                SyntheticStatePatchFactory.CDP_ID, 0, "trace-u02", "corr-u02", decision, binding());

        // The shared harness intentionally authorizes only synthetic defaults.
        // This test adds the exact U02 field and frozen K03 source permission
        // needed to verify StateCommitter mechanics without weakening production policy.
        proposal.getStatePatch().envelope.capabilityId = SyntheticCapabilityPolicyFake.AUTHORIZED_ID;
        proposal.getStatePatch().envelope.capabilityVersion = SyntheticCapabilityPolicyFake.AUTHORIZED_VERSION;
        harness.fieldPermission.authorize(proposal.getStatePatch().operations.get(0).path);
        harness.sourceValidation.authorize("PATIENT_REPORTED");

        U02ClinicalFactCommitService service = new U02ClinicalFactCommitService(harness.committer);
        StateTypes.CommitResult first = service.commit(proposal);
        StateTypes.CommitResult replay = service.commit(proposal);

        assertEquals("COMMITTED", first.status);
        assertEquals(Integer.valueOf(0), first.previousVersion);
        assertEquals(Integer.valueOf(1), first.committedVersion);
        assertEquals(first.committedVersion, replay.committedVersion);
        assertEquals(1, harness.repository.commitCalls());
    }

    private C01U02CapabilityResponse response() {
        C01U02CapabilityResponse.ObservationCandidate observation = new C01U02CapabilityResponse.ObservationCandidate();
        observation.setObservationId("obs-commit-1");
        observation.setConceptId("CUI:C0012833");
        observation.setConceptDisplay("头晕");
        observation.setRawTextRef("sha256:abc");
        observation.setValueSemantics("YES");
        observation.setSourceType("PATIENT_REPORTED");
        observation.setLifecycle("NORMALIZED");
        observation.setConfidenceOrUncertainty(0.9d);
        observation.setProvenance(Collections.singletonList("candidate"));
        observation.setAmbiguityFlags(Collections.emptyList());
        observation.setContradictionRefs(Collections.emptyList());

        C01U02CapabilityResponse response = new C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setObservationCandidates(Collections.singletonList(observation));
        return response;
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
