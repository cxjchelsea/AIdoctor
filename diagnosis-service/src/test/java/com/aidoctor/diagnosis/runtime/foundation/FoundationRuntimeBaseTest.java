package com.aidoctor.diagnosis.runtime.foundation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoundationRuntimeBaseTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void bindingIsIdempotentButCannotSilentlySwitchRuntimeOrVersions() {
        RuntimeBindingRepository repository = mock(RuntimeBindingRepository.class);
        RuntimeBindingService service = new RuntimeBindingService(repository, CLOCK);
        RuntimeBindingRecord existing = new RuntimeBindingRecord(
                "consultation-1", "cdp-1", RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                "thread-1", "scope-v1", "caps-v1", "1.0.0", null);
        when(repository.findById("consultation-1")).thenReturn(Optional.of(existing));

        assertSame(existing, service.bind("consultation-1", "cdp-1", RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                "scope-v1", "caps-v1", "1.0.0"));
        verify(repository, never()).save(any(RuntimeBindingRecord.class));

        assertThrows(IllegalStateException.class, () ->
                service.bind("consultation-1", "cdp-1", RuntimeBindingRecord.LEGACY_FIXED_WORKFLOW,
                        "scope-v1", "caps-v1", "1.0.0"));
        assertThrows(IllegalStateException.class, () ->
                service.bind("consultation-1", "cdp-1", RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                        "scope-v2", "caps-v1", "1.0.0"));
    }

    @Test
    void sameEventTransportReplayReturnsExistingCanonicalEvent() {
        CanonicalBusinessEventRepository repository = mock(CanonicalBusinessEventRepository.class);
        CanonicalBusinessEventLedger ledger = new CanonicalBusinessEventLedger(repository, CLOCK);
        CanonicalBusinessEventRecord existing = new CanonicalBusinessEventRecord(
                "event-1", "consultation-1", "START_CONSULTATION", "idem-1", "sha256:abc", null);
        when(repository.findById("event-1")).thenReturn(Optional.of(existing));

        CanonicalBusinessEventRecord resolved = ledger.resolveOrCreate(
                "event-1", "consultation-1", "START_CONSULTATION", "idem-1", "sha256:abc");

        assertSame(existing, resolved);
        verify(repository, never()).save(any(CanonicalBusinessEventRecord.class));
    }

    @Test
    void reusedEventIdentityWithDifferentPayloadFailsClosed() {
        CanonicalBusinessEventRepository repository = mock(CanonicalBusinessEventRepository.class);
        CanonicalBusinessEventLedger ledger = new CanonicalBusinessEventLedger(repository, CLOCK);
        CanonicalBusinessEventRecord existing = new CanonicalBusinessEventRecord(
                "event-1", "consultation-1", "START_CONSULTATION", "idem-1", "sha256:abc", null);
        when(repository.findById("event-1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> ledger.resolveOrCreate(
                "event-1", "consultation-1", "START_CONSULTATION", "idem-1", "sha256:different"));
        verify(repository, never()).save(any(CanonicalBusinessEventRecord.class));
    }

    @Test
    void clinicalRunCannotOpenForLegacyBoundConsultation() {
        RuntimeBindingRepository bindingRepository = mock(RuntimeBindingRepository.class);
        RuntimeBindingService bindingService = new RuntimeBindingService(bindingRepository, CLOCK);
        CanonicalBusinessEventRepository eventRepository = mock(CanonicalBusinessEventRepository.class);
        ClinicalRunRepository runRepository = mock(ClinicalRunRepository.class);
        ClinicalRunCoordinator coordinator = new ClinicalRunCoordinator(bindingService, eventRepository, runRepository, CLOCK);

        RuntimeBindingRecord legacy = new RuntimeBindingRecord(
                "consultation-1", "cdp-1", RuntimeBindingRecord.LEGACY_FIXED_WORKFLOW,
                "thread-1", "scope-v1", "caps-v1", "1.0.0", null);
        when(bindingRepository.findById("consultation-1")).thenReturn(Optional.of(legacy));

        assertThrows(IllegalStateException.class, () -> coordinator.openRun("consultation-1", "event-1", 1));
        verify(eventRepository, never()).findById(any(String.class));
        verify(runRepository, never()).save(any(ClinicalRunRecord.class));
    }

    @Test
    void clinicalRunReferencesButDoesNotCopyClinicalTruth() {
        RuntimeBindingRepository bindingRepository = mock(RuntimeBindingRepository.class);
        RuntimeBindingService bindingService = new RuntimeBindingService(bindingRepository, CLOCK);
        CanonicalBusinessEventRepository eventRepository = mock(CanonicalBusinessEventRepository.class);
        ClinicalRunRepository runRepository = mock(ClinicalRunRepository.class);
        ClinicalRunCoordinator coordinator = new ClinicalRunCoordinator(bindingService, eventRepository, runRepository, CLOCK);

        RuntimeBindingRecord binding = new RuntimeBindingRecord(
                "consultation-1", "cdp-1", RuntimeBindingRecord.CLINICAL_RUNTIME_V1,
                "thread-1", "scope-v1", "caps-v1", "1.0.0", null);
        CanonicalBusinessEventRecord event = new CanonicalBusinessEventRecord(
                "event-1", "consultation-1", "START_CONSULTATION", "idem-1", "sha256:abc", null);
        when(bindingRepository.findById("consultation-1")).thenReturn(Optional.of(binding));
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(event));
        when(runRepository.save(any(ClinicalRunRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalRunRecord run = coordinator.openRun("consultation-1", "event-1", 7);

        assertEquals("thread-1", run.getThreadId());
        assertEquals("event-1", run.getEventId());
        assertEquals(7, run.getBasedOnClinicalStateVersion().intValue());
        assertEquals(ClinicalRunRecord.OPEN, run.getStatus());
    }
}
