package com.aidoctor.diagnosis.runtime.governance;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Foundation1GovernanceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-11T06:00:00Z"), ZoneOffset.UTC);

    @Test
    void activeCompatibleBindingResolves() {
        CapabilityBindingRepository repository = mock(CapabilityBindingRepository.class);
        BindingReleaseResolver resolver = new BindingReleaseResolver(repository, CLOCK);
        CapabilityBindingRecord binding = binding(CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-11T00:00:00"), null);
        when(repository.findById("binding-1")).thenReturn(Optional.of(binding));

        CapabilityBindingRecord resolved = resolver.resolveCapabilityBinding(
                "binding-1", "C01",
                new CapabilityExecutionContext("scope-v1", "contracts-v1", "adult", "CN", "zh-CN", "WEB"));

        assertSame(binding, resolved);
    }

    @Test
    void disabledExpiredOrIncompatibleBindingFailsClosed() {
        CapabilityExecutionContext context = new CapabilityExecutionContext(
                "scope-v1", "contracts-v1", "adult", "CN", "zh-CN", "WEB");

        CapabilityBindingRepository disabledRepo = mock(CapabilityBindingRepository.class);
        when(disabledRepo.findById("binding-1")).thenReturn(Optional.of(binding(
                CapabilityBindingRecord.DISABLED, LocalDateTime.parse("2026-09-11T00:00:00"), null)));
        assertThrows(IllegalStateException.class, () ->
                new BindingReleaseResolver(disabledRepo, CLOCK).resolveCapabilityBinding("binding-1", "C01", context));

        CapabilityBindingRepository expiredRepo = mock(CapabilityBindingRepository.class);
        when(expiredRepo.findById("binding-1")).thenReturn(Optional.of(binding(
                CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-10T00:00:00"),
                LocalDateTime.parse("2026-09-11T05:00:00"))));
        assertThrows(IllegalStateException.class, () ->
                new BindingReleaseResolver(expiredRepo, CLOCK).resolveCapabilityBinding("binding-1", "C01", context));

        CapabilityBindingRepository incompatibleRepo = mock(CapabilityBindingRepository.class);
        CapabilityBindingRecord incompatible = new CapabilityBindingRecord(
                "binding-1", "C01", "c01-1.0.0", "caps-v1", "scope-v2", "contracts-v1",
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ACTIVE,
                LocalDateTime.parse("2026-09-11T00:00:00"), null, LocalDateTime.parse("2026-09-11T00:00:00"));
        when(incompatibleRepo.findById("binding-1")).thenReturn(Optional.of(incompatible));
        assertThrows(IllegalStateException.class, () ->
                new BindingReleaseResolver(incompatibleRepo, CLOCK).resolveCapabilityBinding("binding-1", "C01", context));
    }

    @Test
    void traceCorrelatesCapabilityResultDecisionProposalAndCommitWithoutBecomingClinicalState() {
        CapabilityCallTraceRepository repository = mock(CapabilityCallTraceRepository.class);
        CapabilityTraceService service = new CapabilityTraceService(repository, CLOCK);
        when(repository.existsById("call-1")).thenReturn(false);
        when(repository.save(any(CapabilityCallTraceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CapabilityCallTraceRecord started = service.start(
                "call-1", "consultation-1", "thread-1", "run-1", "event-1", "U02", "C01", "binding-1", 7);
        when(repository.findById("call-1")).thenReturn(Optional.of(started));

        CapabilityCallTraceRecord completed = service.succeed(
                "call-1", "result-1", "decision-1", "proposal-1", "commit-1", 8);

        assertEquals(CapabilityCallTraceRecord.SUCCEEDED, completed.getCallStatus());
        assertEquals("result-1", completed.getCapabilityResultRef());
        assertEquals("decision-1", completed.getDecisionRef());
        assertEquals("proposal-1", completed.getProposalRef());
        assertEquals("commit-1", completed.getCommitRef());
        assertEquals(7, completed.getClinicalStateVersionBefore().intValue());
        assertEquals(8, completed.getClinicalStateVersionAfter().intValue());
        assertThrows(IllegalStateException.class, () -> completed.fail("LATE_FAILURE", LocalDateTime.now()));
    }

    private CapabilityBindingRecord binding(String status, LocalDateTime effectiveFrom, LocalDateTime effectiveUntil) {
        return new CapabilityBindingRecord(
                "binding-1", "C01", "c01-1.0.0", "caps-v1", "scope-v1", "contracts-v1",
                CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY, CapabilityBindingRecord.ANY,
                CapabilityBindingRecord.ANY, status, effectiveFrom, effectiveUntil, LocalDateTime.parse("2026-09-11T00:00:00"));
    }
}
