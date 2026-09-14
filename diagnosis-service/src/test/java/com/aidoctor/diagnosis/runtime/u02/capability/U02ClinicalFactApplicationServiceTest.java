package com.aidoctor.diagnosis.runtime.u02.capability;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactBusinessOwner;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactCommitService;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactProposalFactory;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class U02ClinicalFactApplicationServiceTest {
    @Test
    void clarificationKeepsClinicalVersionAndDoesNotCreateProposalOrCommit() {
        C01U02CapabilityGateway gateway = mock(C01U02CapabilityGateway.class);
        U02ClinicalFactCommitService commitService = mock(U02ClinicalFactCommitService.class);
        CapabilityTraceService trace = mock(CapabilityTraceService.class);
        C01U02CapabilityResponse response = response(false);
        when(gateway.interpret(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new C01U02CapabilityGateway.GovernedResult(response, binding()));
        U02ClinicalFactApplicationService service = service(gateway, commitService, trace);

        U02ExecutionResult result = service.execute(command(3));

        assertEquals("CLARIFICATION_REQUIRED", result.status);
        assertNull(result.proposal);
        assertNull(result.commitResult);
        verify(commitService, never()).commit(any());
        verify(trace).start(anyString(), eq("consult-1"), eq("thread-1"), eq("run-1"),
                eq("event-1"), eq("U02"), eq("C01"), eq(C01U02CapabilityGateway.BINDING_ID), eq(Integer.valueOf(3)));
        verify(trace).succeed(anyString(), eq("c01-u02-result-event-1"),
                eq("u02-decision-event-1"), isNull(), isNull(), eq(Integer.valueOf(3)));
    }

    @Test
    void committedPathLinksCapabilityDecisionProposalCommitAndVersion() {
        C01U02CapabilityGateway gateway = mock(C01U02CapabilityGateway.class);
        U02ClinicalFactCommitService commitService = mock(U02ClinicalFactCommitService.class);
        CapabilityTraceService trace = mock(CapabilityTraceService.class);
        when(gateway.interpret(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new C01U02CapabilityGateway.GovernedResult(response(true), binding()));
        StateTypes.CommitResult committed = new StateTypes.CommitResult();
        committed.status = "COMMITTED";
        committed.patchId = "u02-proposal-event-1";
        committed.previousVersion = Integer.valueOf(3);
        committed.committedVersion = Integer.valueOf(4);
        committed.reasonCode = "STATE_COMMITTED";
        when(commitService.commit(any())).thenReturn(committed);
        U02ClinicalFactApplicationService service = service(gateway, commitService, trace);

        U02ExecutionResult result = service.execute(command(3));

        assertEquals("COMMITTED", result.status);
        assertEquals(Integer.valueOf(4), result.commitResult.committedVersion);
        verify(trace).succeed(anyString(), eq("c01-u02-result-event-1"),
                eq("u02-decision-event-1"), eq("u02-proposal-event-1"),
                eq("u02-proposal-event-1"), eq(Integer.valueOf(4)));
    }

    private U02ClinicalFactApplicationService service(
            C01U02CapabilityGateway gateway,
            U02ClinicalFactCommitService commitService,
            CapabilityTraceService trace) {
        return new U02ClinicalFactApplicationService(
                gateway,
                new U02ClinicalFactBusinessOwner(),
                new U02ClinicalFactProposalFactory(),
                commitService,
                new U02DependencyInvalidationHook(),
                trace);
    }

    private U02ExecutionCommand command(int baseVersion) {
        return new U02ExecutionCommand(
                "user-1", "consult-1", "thread-1", "run-1", "event-1", "cdp-1",
                "我头晕", "PATIENT_REPORTED", baseVersion, "corr-1", "trace-1",
                Collections.emptyList());
    }

    private C01U02CapabilityResponse response(boolean resolved) {
        C01U02CapabilityResponse.ObservationCandidate observation = new C01U02CapabilityResponse.ObservationCandidate();
        observation.setObservationId("obs-1");
        observation.setConceptId(resolved ? "CUI:C0012833" : null);
        observation.setConceptDisplay(resolved ? "头晕" : "UNRESOLVED_CLINICAL_EXPRESSION");
        observation.setRawTextRef("sha256:abc");
        observation.setValueSemantics(resolved ? "YES" : "UNKNOWN");
        observation.setSourceType("PATIENT_REPORTED");
        observation.setLifecycle(resolved ? "NORMALIZED" : "UNCERTAIN");
        observation.setConfidenceOrUncertainty(resolved ? 0.9d : 0.25d);
        observation.setProvenance(Collections.singletonList("candidate"));
        observation.setAmbiguityFlags(Collections.emptyList());
        observation.setContradictionRefs(Collections.emptyList());

        C01U02CapabilityResponse response = new C01U02CapabilityResponse();
        response.setBusinessStatus(resolved ? "SUCCESS" : "INSUFFICIENT_INFORMATION");
        response.setReasonCode(resolved ? "OBSERVATION_CANDIDATES_EXTRACTED" : "ONLY_UNCERTAIN_OBSERVATIONS");
        response.setRetryable(false);
        response.setObservationCandidates(Collections.singletonList(observation));
        response.setSourceAttribution("USER_TEXT");
        response.setProvenance(Collections.singletonList("test"));
        return response;
    }

    private CapabilityBindingRecord binding() {
        return new CapabilityBindingRecord(
                C01U02CapabilityGateway.BINDING_ID,
                C01U02CapabilityGateway.CAPABILITY_ID,
                "c01-u02-1.0.0",
                "aidoctor-v1-u02",
                C01U02CapabilityGateway.SCOPE_VERSION,
                C01U02CapabilityGateway.CONTRACT_VERSION,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-01T00:00:00"),
                null,
                LocalDateTime.parse("2026-09-01T00:00:00"));
    }
}
