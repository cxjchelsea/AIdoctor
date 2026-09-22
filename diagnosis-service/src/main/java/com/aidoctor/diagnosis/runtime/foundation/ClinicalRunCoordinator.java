package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Minimal bounded-run coordinator for the new clinical runtime authority.
 *
 * <p>Foundation-0 only opens execution context. It deliberately does not
 * select a business Unit, mutate Clinical State, or infer Consultation
 * lifecycle values.</p>
 */
@Service
public class ClinicalRunCoordinator {
    private final RuntimeBindingService bindingService;
    private final CanonicalBusinessEventRepository eventRepository;
    private final ClinicalRunRepository runRepository;
    private final Clock clock;

    public ClinicalRunCoordinator(
            RuntimeBindingService bindingService,
            CanonicalBusinessEventRepository eventRepository,
            ClinicalRunRepository runRepository
    ) {
        this(bindingService, eventRepository, runRepository, Clock.systemUTC());
    }

    ClinicalRunCoordinator(
            RuntimeBindingService bindingService,
            CanonicalBusinessEventRepository eventRepository,
            ClinicalRunRepository runRepository,
            Clock clock
    ) {
        this.bindingService = bindingService;
        this.eventRepository = eventRepository;
        this.runRepository = runRepository;
        this.clock = clock;
    }

    @Transactional
    public ClinicalRunRecord openRun(String consultationId, String eventId, int clinicalStateVersion) {
        RuntimeBindingRecord binding = bindingService.requireBinding(consultationId);
        if (!RuntimeBindingRecord.CLINICAL_RUNTIME_V1.equals(binding.getRuntimeAuthority())) {
            throw new IllegalStateException("Legacy-bound consultation cannot be driven by Clinical Runtime V1.");
        }

        CanonicalBusinessEventRecord event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Canonical event not found: " + eventId));
        if (!consultationId.equals(event.getConsultationId())) {
            throw new IllegalStateException("Canonical event belongs to a different consultation.");
        }

        ClinicalRunRecord run = new ClinicalRunRecord(
                "run_" + UUID.randomUUID().toString().replace("-", ""),
                binding.getThreadId(),
                consultationId,
                eventId,
                clinicalStateVersion,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return runRepository.save(run);
    }

    public ClinicalRunRecord requireOriginalRun(String consultationId, String eventId) {
        return runRepository.findFirstByConsultationIdAndEventIdOrderByCreatedAtAsc(consultationId, eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Original run not found for canonical event: consultationId=" + consultationId + ", eventId=" + eventId));
    }
}
