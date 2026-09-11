package com.aidoctor.diagnosis.runtime.u01.capability;

import com.aidoctor.diagnosis.client.C01U01CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class C01U01CapabilityGatewayTest {

    @Test
    void capabilityExistsButDefaultOffBindingCannotBeInvoked() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, false);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("CAPABILITY_BINDING_INACTIVE", ex.getReasonCode());
        assertFalse(ex.isRetryable());
        verifyNoInteractions(client);
    }

    @Test
    void activeExactBindingAcceptsCandidateResult() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        when(client.interpret(any())).thenReturn(validResponse("SUCCESS"));
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, true);

        C01U01CapabilityResponse response = gateway.interpret(
                "user-1", "我头晕", "consult-1", null);

        assertEquals("SELF", response.getSubjectCandidate().getSubjectType());
        assertEquals("SYMPTOM", response.getScopeCandidate().getScope());
        verify(client, times(1)).interpret(any());
    }

    @Test
    void insufficientInformationIsAValidCandidateOutcomeNotDependencyFailure() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        C01U01CapabilityResponse response = validResponse("INSUFFICIENT_INFORMATION");
        response.getSubjectCandidate().setSubjectType("UNKNOWN");
        response.getScopeCandidate().setScope("UNKNOWN");
        when(client.interpret(any())).thenReturn(response);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, true);

        C01U01CapabilityResponse actual = gateway.interpret(
                "user-1", "你好", "consult-1", null);

        assertEquals("INSUFFICIENT_INFORMATION", actual.getBusinessStatus());
    }

    @Test
    void bindingMismatchFailsClosed() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        C01U01CapabilityResponse response = validResponse("SUCCESS");
        response.getBindingRef().setCapabilityVersion("unexpected");
        when(client.interpret(any())).thenReturn(response);
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, true);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("INVALID_OUTPUT", ex.getReasonCode());
        assertFalse(ex.isRetryable());
    }

    @Test
    void dependencyFailureDoesNotBecomeUnknownOrNegativeClinicalResult() {
        C01U01CapabilityClient client = mock(C01U01CapabilityClient.class);
        when(client.interpret(any())).thenThrow(new RuntimeException("down"));
        C01U01CapabilityGateway gateway = new C01U01CapabilityGateway(client, true);

        C01CapabilityException ex = assertThrows(C01CapabilityException.class,
                () -> gateway.interpret("user-1", "我头晕", "consult-1", null));

        assertEquals("DEPENDENCY_FAILURE", ex.getReasonCode());
        assertTrue(ex.isRetryable());
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
        binding.setBindingStatus("ACTIVE");
        binding.setCapabilityId(C01U01CapabilityGateway.CAPABILITY_ID);
        binding.setCapabilityVersion(C01U01CapabilityGateway.CAPABILITY_VERSION);
        binding.setCapabilitySetVersion(C01U01CapabilityGateway.CAPABILITY_SET_VERSION);
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
