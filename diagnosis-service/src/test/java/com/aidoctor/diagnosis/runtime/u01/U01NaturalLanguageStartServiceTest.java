package com.aidoctor.diagnosis.runtime.u01;

import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import com.aidoctor.diagnosis.runtime.u01.capability.C01U01CapabilityGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class U01NaturalLanguageStartServiceTest {

    @Test
    void firstRawStartUsesC01CandidateThenDelegatesFormalDecisionToU01() {
        ConsultationRepository repository = mock(ConsultationRepository.class);
        C01U01CapabilityGateway gateway = mock(C01U01CapabilityGateway.class);
        U01ConsultationService u01 = mock(U01ConsultationService.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(gateway.interpret(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(response("SELF", "持续咳嗽三天", "SYMPTOM", false));
        U01Result expected = mock(U01Result.class);
        when(u01.start(any(U01StartCommand.class))).thenReturn(expected);

        U01NaturalLanguageStartService service =
                new U01NaturalLanguageStartService(repository, gateway, u01);
        U01Result actual = service.start(new U01RawStartCommand(
                "evt-raw-1", "idem-raw-1", "user-1", "我持续咳嗽三天", null));

        assertSame(expected, actual);
        ArgumentCaptor<U01StartCommand> captor = ArgumentCaptor.forClass(U01StartCommand.class);
        verify(u01).start(captor.capture());
        U01StartCommand mapped = captor.getValue();
        assertEquals("SELF", mapped.getSubjectType());
        assertEquals("持续咳嗽三天", mapped.getProblemText());
        assertEquals("SYMPTOM", mapped.getScopeIntentCandidate());
        assertNull(mapped.getSubjectReferenceId());
        assertNotNull(mapped.getSourceInputFingerprint());
    }

    @Test
    void inferredOtherDoesNotForgeFormalSubjectReference() {
        ConsultationRepository repository = mock(ConsultationRepository.class);
        C01U01CapabilityGateway gateway = mock(C01U01CapabilityGateway.class);
        U01ConsultationService u01 = mock(U01ConsultationService.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(gateway.interpret(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(response("OTHER", "我妈头晕", "SYMPTOM", false));
        when(u01.start(any(U01StartCommand.class))).thenReturn(mock(U01Result.class));

        U01NaturalLanguageStartService service =
                new U01NaturalLanguageStartService(repository, gateway, u01);
        service.start(new U01RawStartCommand(
                "evt-other", "idem-other", "user-1", "我妈头晕", null));

        ArgumentCaptor<U01StartCommand> captor = ArgumentCaptor.forClass(U01StartCommand.class);
        verify(u01).start(captor.capture());
        assertEquals("OTHER", captor.getValue().getSubjectType());
        assertNull(captor.getValue().getSubjectReferenceId());
    }

    @Test
    void knownFormalSubjectReferenceCanBeCarriedOnlyForOtherCandidate() {
        ConsultationRepository repository = mock(ConsultationRepository.class);
        C01U01CapabilityGateway gateway = mock(C01U01CapabilityGateway.class);
        U01ConsultationService u01 = mock(U01ConsultationService.class);
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(gateway.interpret(anyString(), anyString(), anyString(), eq("subject-42")))
                .thenReturn(response("OTHER", "母亲头晕", "SYMPTOM", false));
        when(u01.start(any(U01StartCommand.class))).thenReturn(mock(U01Result.class));

        U01NaturalLanguageStartService service =
                new U01NaturalLanguageStartService(repository, gateway, u01);
        service.start(new U01RawStartCommand(
                "evt-other-known", "idem-other-known", "user-1", "母亲头晕", "subject-42"));

        ArgumentCaptor<U01StartCommand> captor = ArgumentCaptor.forClass(U01StartCommand.class);
        verify(u01).start(captor.capture());
        assertEquals("subject-42", captor.getValue().getSubjectReferenceId());
    }

    @Test
    void canonicalReplaySkipsCapabilityReinterpretation() {
        ConsultationRepository repository = mock(ConsultationRepository.class);
        C01U01CapabilityGateway gateway = mock(C01U01CapabilityGateway.class);
        U01ConsultationService u01 = mock(U01ConsultationService.class);
        when(repository.findById(anyString())).thenReturn(Optional.of(mock(ConsultationRecord.class)));
        U01Result expected = mock(U01Result.class);
        when(u01.start(any(U01StartCommand.class))).thenReturn(expected);

        U01NaturalLanguageStartService service =
                new U01NaturalLanguageStartService(repository, gateway, u01);
        U01Result actual = service.start(new U01RawStartCommand(
                "evt-replay", "idem-replay", "user-1", "我头晕", null));

        assertSame(expected, actual);
        verifyNoInteractions(gateway);
        ArgumentCaptor<U01StartCommand> captor = ArgumentCaptor.forClass(U01StartCommand.class);
        verify(u01).start(captor.capture());
        assertNull(captor.getValue().getSubjectType());
        assertNotNull(captor.getValue().getSourceInputFingerprint());
    }

    private C01U01CapabilityResponse response(
            String subjectType, String problemText, String scope, boolean safetyDetected) {
        C01U01CapabilityResponse response = new C01U01CapabilityResponse();
        response.setBusinessStatus("SUCCESS");
        response.setReasonCode("CANDIDATES_EXTRACTED");
        response.setRetryable(false);
        response.setSourceAttribution("USER_TEXT");
        response.setProvenance(Collections.singletonList("test"));

        C01U01CapabilityResponse.SubjectCandidate subject = new C01U01CapabilityResponse.SubjectCandidate();
        subject.setSubjectType(subjectType);
        response.setSubjectCandidate(subject);

        C01U01CapabilityResponse.ProblemCandidate problem = new C01U01CapabilityResponse.ProblemCandidate();
        problem.setText(problemText);
        response.setProblemCandidate(problem);

        C01U01CapabilityResponse.ScopeCandidate scopeCandidate = new C01U01CapabilityResponse.ScopeCandidate();
        scopeCandidate.setScope(scope);
        response.setScopeCandidate(scopeCandidate);

        C01U01CapabilityResponse.EarlySafetySignalCandidate safety =
                new C01U01CapabilityResponse.EarlySafetySignalCandidate();
        safety.setDetected(safetyDetected);
        safety.setClues(Collections.emptyList());
        safety.setEvidenceSpans(Collections.emptyList());
        response.setEarlySafetySignalCandidate(safety);
        return response;
    }
}
