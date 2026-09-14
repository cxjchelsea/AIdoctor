package com.aidoctor.diagnosis.runtime.u02.capability;

import com.aidoctor.diagnosis.client.C01U02CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class C01U02CapabilityGatewayTest {
    private static final String CAPABILITY_VERSION = "c01-u02-1.0.0";
    private static final String CAPABILITY_SET_VERSION = "aidoctor-v1-u02";

    @Test
    void defaultOffDoesNotInvokeCapabilityOrGovernance() {
        C01U02CapabilityClient client = mock(C01U02CapabilityClient.class);
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        C01U02CapabilityGateway gateway = new C01U02CapabilityGateway(client, guard, false);

        C01U02CapabilityException ex = assertThrows(C01U02CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", "event-1", "PATIENT_REPORTED"));

        assertEquals("CAPABILITY_BINDING_INACTIVE", ex.getReasonCode());
        verifyNoInteractions(client, guard);
    }

    @Test
    void authorizedBindingReturnsCandidateOnlyResult() {
        C01U02CapabilityClient client = mock(C01U02CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        when(client.interpret(any())).thenReturn(validResponse("UNKNOWN", "UNCERTAIN"));
        C01U02CapabilityGateway gateway = new C01U02CapabilityGateway(client, guard, true);

        C01U02CapabilityGateway.GovernedResult result = gateway.interpret(
                "user-1", "不确定有没有头晕", "consult-1", "event-1", "PATIENT_REPORTED");

        assertEquals("UNKNOWN", result.getResponse().getObservationCandidates().get(0).getValueSemantics());
        assertEquals(C01U02CapabilityGateway.BINDING_ID, result.getAuthorizedBinding().getBindingId());
        verify(guard).authorize(eq(C01U02CapabilityGateway.BINDING_ID),
                eq(C01U02CapabilityGateway.CAPABILITY_ID), any(CapabilityExecutionContext.class));
    }

    @Test
    void governanceFailureFailsBeforeRemoteCall() {
        C01U02CapabilityClient client = mock(C01U02CapabilityClient.class);
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        when(guard.authorize(anyString(), anyString(), any())).thenThrow(new IllegalStateException("disabled"));
        C01U02CapabilityGateway gateway = new C01U02CapabilityGateway(client, guard, true);

        C01U02CapabilityException ex = assertThrows(C01U02CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", "event-1", "PATIENT_REPORTED"));

        assertEquals("CAPABILITY_BINDING_NOT_AUTHORIZED", ex.getReasonCode());
        verifyNoInteractions(client);
    }

    @Test
    void bindingMismatchFailsClosed() {
        C01U02CapabilityClient client = mock(C01U02CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        C01U02CapabilityResponse response = validResponse("YES", "NORMALIZED");
        response.getBindingRef().setCapabilityVersion("unexpected");
        when(client.interpret(any())).thenReturn(response);
        C01U02CapabilityGateway gateway = new C01U02CapabilityGateway(client, guard, true);

        C01U02CapabilityException ex = assertThrows(C01U02CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", "event-1", "PATIENT_REPORTED"));
        assertEquals("INVALID_OUTPUT", ex.getReasonCode());
    }

    private CapabilityInvocationGuard authorizedGuard() {
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        when(guard.authorize(eq(C01U02CapabilityGateway.BINDING_ID),
                eq(C01U02CapabilityGateway.CAPABILITY_ID), any(CapabilityExecutionContext.class))).thenReturn(binding());
        return guard;
    }

    private CapabilityBindingRecord binding() {
        return new CapabilityBindingRecord(
                C01U02CapabilityGateway.BINDING_ID,
                C01U02CapabilityGateway.CAPABILITY_ID,
                CAPABILITY_VERSION,
                CAPABILITY_SET_VERSION,
                C01U02CapabilityGateway.SCOPE_VERSION,
                C01U02CapabilityGateway.CONTRACT_VERSION,
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-01T00:00:00"), null,
                LocalDateTime.parse("2026-09-01T00:00:00"));
    }

    private C01U02CapabilityResponse validResponse(String valueSemantics, String lifecycle) {
        C01U02CapabilityResponse response = new C01U02CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setReasonCode("OBSERVATION_CANDIDATES_EXTRACTED");
        response.setRetryable(false);
        response.setSourceAttribution("USER_TEXT");
        response.setProvenance(Collections.singletonList("test"));

        C01U02CapabilityResponse.BindingRef ref = new C01U02CapabilityResponse.BindingRef();
        ref.setBindingId(C01U02CapabilityGateway.BINDING_ID);
        ref.setBindingStatus(CapabilityBindingRecord.ACTIVE);
        ref.setCapabilityId(C01U02CapabilityGateway.CAPABILITY_ID);
        ref.setCapabilityVersion(CAPABILITY_VERSION);
        ref.setCapabilitySetVersion(CAPABILITY_SET_VERSION);
        ref.setScopeVersion(C01U02CapabilityGateway.SCOPE_VERSION);
        ref.setContractVersion(C01U02CapabilityGateway.CONTRACT_VERSION);
        response.setBindingRef(ref);

        C01U02CapabilityResponse.ObservationCandidate observation = new C01U02CapabilityResponse.ObservationCandidate();
        observation.setObservationId("obs-1");
        observation.setConceptId("CUI:C0012833");
        observation.setConceptDisplay("头晕");
        observation.setRawTextRef("sha256:abc");
        observation.setValueSemantics(valueSemantics);
        observation.setSourceType("PATIENT_REPORTED");
        observation.setLifecycle(lifecycle);
        observation.setConfidenceOrUncertainty(0.8d);
        observation.setProvenance(Collections.singletonList("test"));
        observation.setAmbiguityFlags(Collections.emptyList());
        observation.setContradictionRefs(Collections.emptyList());
        response.setObservationCandidates(Collections.singletonList(observation));
        return response;
    }
}
