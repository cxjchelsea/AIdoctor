package com.aidoctor.diagnosis.runtime.u01.capability;

import com.aidoctor.diagnosis.client.C01U01CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class C01U01CapabilityGatewayTest {
    private static final String CAPABILITY_VERSION = "c01-u01-1.0.0";
    private static final String CAPABILITY_SET_VERSION = "aidoctor-v1-u01";

    @Test
    void capabilityExistsButDefaultOffBindingCannotBeInvoked() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, false);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("CAPABILITY_BINDING_INACTIVE", ex.getReasonCode());
        assertFalse(ex.isRetryable());
        verifyNoInteractions(client, guard);
    }

    @Test
    void activeAuthoritativeBindingAcceptsCandidateResult() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        when(client.interpret(any())).thenReturn(validResponse("SUCCESS"));
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, true);

        C01U01CapabilityResponse response = gateway.interpret(
                "user-1", "我头晕", "consult-1", null);

        assertEquals("SELF", response.getSubjectCandidate().getSubjectType());
        assertEquals("SYMPTOM", response.getScopeCandidate().getScope());
        verify(guard).authorize(eq(C01U01CapabilityGateway.BINDING_ID),
                eq(C01U01CapabilityGateway.CAPABILITY_ID), any(CapabilityExecutionContext.class));
        verify(client, times(1)).interpret(any());
    }

    @Test
    void authoritativeBindingFailureFailsClosedBeforeClientInvocation() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        when(guard.authorize(any(), any(), any()))
                .thenThrow(new IllegalStateException("disabled"));
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, true);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("CAPABILITY_BINDING_NOT_AUTHORIZED", ex.getReasonCode());
        assertFalse(ex.isRetryable());
        verifyNoInteractions(client);
    }

    @Test
    void insufficientInformationIsAValidCandidateOutcomeNotDependencyFailure() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        C01U01CapabilityResponse response = validResponse("INSUFFICIENT_INFORMATION");
        response.getSubjectCandidate().setSubjectType("UNKNOWN");
        response.getScopeCandidate().setScope("UNKNOWN");
        when(client.interpret(any())).thenReturn(response);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, true);

        C01U01CapabilityResponse actual = gateway.interpret(
                "user-1", "你好", "consult-1", null);

        assertEquals("INSUFFICIENT_INFORMATION", actual.getBusinessStatus());
    }

    @Test
    void returnedBindingMismatchFailsClosed() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        C01U01CapabilityResponse response = validResponse("SUCCESS");
        response.getBindingRef().setCapabilityVersion("unexpected");
        when(client.interpret(any())).thenReturn(response);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, true);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("INVALID_OUTPUT", ex.getReasonCode());
        assertFalse(ex.isRetryable());
    }

    @Test
    void dependencyFailureDoesNotBecomeUnknownOrNegativeClinicalResult() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        CapabilityInvocationGuard guard = authorizedGuard();
        when(client.interpret(any())).thenThrow(new RuntimeException("down"));
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, guard, true);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("DEPENDENCY_FAILURE", ex.getReasonCode());
        assertTrue(ex.isRetryable());
    }

    private CapabilityInvocationGuard authorizedGuard() {
        CapabilityInvocationGuard guard = mock(CapabilityInvocationGuard.class);
        when(guard.authorize(eq(C01U01CapabilityGateway.BINDING_ID),
                eq(C01U01CapabilityGateway.CAPABILITY_ID), any(CapabilityExecutionContext.class)))
                .thenReturn(binding());
        return guard;
    }

    private CapabilityBindingRecord binding() {
        return new CapabilityBindingRecord(
                C01U01CapabilityGateway.BINDING_ID,
                C01U01CapabilityGateway.CAPABILITY_ID,
                CAPABILITY_VERSION,
                CAPABILITY_SET_VERSION,
                C01U01CapabilityGateway.SCOPE_VERSION,
                C01U01CapabilityGateway.CONTRACT_VERSION,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-01T00:00:00"),
                null,
                LocalDateTime.parse("2026-09-01T00:00:00"));
    }

    private C01U01CapabilityResponse validResponse(String status) {
        C01U01CapabilityResponse response = new C01U01CapabilityResponse();
        response.setBusinessStatus(status);
        response.setReasonCode("CANDIDATES_EXTRACTED");
        response.setRetryable(false);
        response.setSourceAttribution("USER_TEXT");
        response.setProvenance(Collections.singletonList("test"));

        C01U01CapabilityResponse.BindingRef binding = new C01U01CapabilityResponse.BindingRef();
        binding.setBindingId(C01U01CapabilityGateway.BINDING_ID);
        binding.setBindingStatus(CapabilityBindingRecord.ACTIVE);
        binding.setCapabilityId(C01U01CapabilityGateway.CAPABILITY_ID);
        binding.setCapabilityVersion(CAPABILITY_VERSION);
        binding.setCapabilitySetVersion(CAPABILITY_SET_VERSION);
        binding.setScopeVersion(C01U01CapabilityGateway.SCOPE_VERSION);
        binding.setContractVersion(C01U01CapabilityGateway.CONTRACT_VERSION);
        response.setBindingRef(binding);

        C01U01CapabilityResponse.SubjectCandidate subject = new C01U01CapabilityResponse.SubjectCandidate();
        subject.setSubjectType("SELF");
        subject.setConfidence(0.9);
        subject.setUncertain(false);
        subject.setEvidenceSpans(Collections.emptyList());
        response.setSubjectCandidate(subject);

        C01U01CapabilityResponse.ProblemCandidate problem = new C01U01CapabilityResponse.ProblemCandidate();
        problem.setText("我头晕");
        problem.setConfidence(0.9);
        problem.setUncertain(false);
        problem.setEvidenceSpans(Collections.emptyList());
        response.setProblemCandidate(problem);

        C01U01CapabilityResponse.ScopeCandidate scope = new C01U01CapabilityResponse.ScopeCandidate();
        scope.setScope("SYMPTOM");
        scope.setConfidence(0.9);
        scope.setUncertain(false);
        scope.setEvidenceSpans(Collections.emptyList());
        response.setScopeCandidate(scope);

        C01U01CapabilityResponse.EarlySafetySignalCandidate safety =
                new C01U01CapabilityResponse.EarlySafetySignalCandidate();
        safety.setDetected(false);
        safety.setClues(Collections.emptyList());
        safety.setEvidenceSpans(Collections.emptyList());
        response.setEarlySafetySignalCandidate(safety);
        return response;
    }
}
