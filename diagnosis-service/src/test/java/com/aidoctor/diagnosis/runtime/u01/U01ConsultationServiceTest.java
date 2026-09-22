package com.aidoctor.diagnosis.runtime.u01;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.runtime.foundation.CanonicalBusinessEventLedger;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunCoordinator;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class U01ConsultationServiceTest {
    private ConsultationRepository consultationRepository;
    private CanonicalBusinessEventLedger eventLedger;
    private RuntimeBindingService bindingService;
    private ClinicalRunCoordinator runCoordinator;
    private CDPManager cdpManager;
    private U01ConsultationService service;

    @BeforeEach
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        eventLedger = mock(CanonicalBusinessEventLedger.class);
        bindingService = mock(RuntimeBindingService.class);
        runCoordinator = mock(ClinicalRunCoordinator.class);
        cdpManager = mock(CDPManager.class);
        service = new U01ConsultationService(
                consultationRepository,
                new U01SemanticPolicy(),
                eventLedger,
                bindingService,
                runCoordinator,
                cdpManager);
    }

    @Test
    void establishesOneActiveConsultationBoundToClinicalRuntime() {
        U01StartCommand command = command("evt-1", "idem-1");
        String consultationId = U01ConsultationService.consultationIdFor(command.getEventId());
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.empty());
        CDP cdp = CDP.builder().id("cdp-1").version(1).build();
        when(cdpManager.createCDP(eq("user-1"), anyString())).thenReturn(cdp);
        when(consultationRepository.save(any(ConsultationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(runCoordinator.openRun(consultationId, "evt-1", 1))
                .thenReturn(new ClinicalRunRecord("run-1", "thread-1", consultationId, "evt-1", 1, LocalDateTime.now()));

        U01Result result = service.start(command);

        assertEquals(ConsultationRecord.ACTIVE, result.getLifecycleStatus());
        assertEquals(U01SemanticDecision.SUBJECT_RESOLVED, result.getSubjectStatus());
        assertEquals(U01SemanticDecision.PROBLEM_FRAMED, result.getProblemStatus());
        assertEquals(U01SemanticDecision.IN_SCOPE, result.getScopeDecision());
        assertEquals("U02", result.getNextUnit());
        assertEquals("run-1", result.getRunId());
        verify(eventLedger).resolveOrCreate(eq("evt-1"), eq(consultationId), eq("START_CONSULTATION"), eq("idem-1"), anyString());
        verify(bindingService).bind(eq(consultationId), eq("cdp-1"), eq(RuntimeBindingRecord.CLINICAL_RUNTIME_V1),
                eq(U01ConsultationService.SCOPE_VERSION), eq(U01ConsultationService.CAPABILITY_SET_VERSION), eq(U01ConsultationService.CONTRACT_VERSION));
        verify(runCoordinator).openRun(consultationId, "evt-1", 1);
    }

    @Test
    void canonicalReplayReturnsExistingOutcomeWithoutSecondCdpOrRun() {
        U01StartCommand command = command("evt-replay", "idem-replay");
        String consultationId = U01ConsultationService.consultationIdFor(command.getEventId());
        U01SemanticDecision decision = new U01SemanticPolicy().decide(command);
        ConsultationRecord existing = new ConsultationRecord(
                consultationId, "cdp-existing", "user-1", command, decision, LocalDateTime.now());
        ClinicalRunRecord originalRun = new ClinicalRunRecord(
                "run-original", "thread-original", consultationId, "evt-replay", 1, LocalDateTime.now());
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(existing));
        when(runCoordinator.requireOriginalRun(consultationId, "evt-replay")).thenReturn(originalRun);

        U01Result result = service.start(command);

        assertEquals("cdp-existing", result.getCdpId());
        assertEquals("run-original", result.getRunId());
        assertEquals("U02", result.getNextUnit());
        verifyNoInteractions(cdpManager);
        verifyNoInteractions(bindingService);
        verify(runCoordinator, never()).openRun(anyString(), anyString(), anyInt());
        verify(consultationRepository, never()).save(any());
    }

    @Test
    void clarificationDoesNotCreateWaitingUserState() {
        U01StartCommand command = new U01StartCommand(
                "evt-clarify", "idem-clarify", "user-1", null, null,
                "头晕", "SYMPTOM", false);
        String consultationId = U01ConsultationService.consultationIdFor(command.getEventId());
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.empty());
        CDP cdp = CDP.builder().id("cdp-2").version(1).build();
        when(cdpManager.createCDP(eq("user-1"), anyString())).thenReturn(cdp);
        when(consultationRepository.save(any(ConsultationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(runCoordinator.openRun(consultationId, "evt-clarify", 1))
                .thenReturn(new ClinicalRunRecord("run-2", "thread-2", consultationId, "evt-clarify", 1, LocalDateTime.now()));

        U01Result result = service.start(command);

        assertEquals(ConsultationRecord.ACTIVE, result.getLifecycleStatus());
        assertEquals("U06", result.getNextUnit());
        assertNotEquals("WAITING_USER", result.getLifecycleStatus());
    }

    private U01StartCommand command(String eventId, String idempotencyKey) {
        return new U01StartCommand(eventId, idempotencyKey, "user-1", "SELF", null,
                "持续咳嗽三天", "SYMPTOM", false);
    }
}
