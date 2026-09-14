package com.aidoctor.diagnosis.runtime.governance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class CapabilityTraceService {
    private final CapabilityCallTraceRepository repository;
    private final Clock clock;

    public CapabilityTraceService(CapabilityCallTraceRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CapabilityTraceService(CapabilityCallTraceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CapabilityCallTraceRecord start(
            String capabilityCallId,
            String consultationId,
            String threadId,
            String runId,
            String eventId,
            String unitId,
            String capabilityId,
            String bindingId,
            Integer clinicalStateVersionBefore
    ) {
        if (repository.existsById(capabilityCallId)) {
            throw new IllegalStateException("Capability call trace already exists: " + capabilityCallId);
        }
        return repository.save(new CapabilityCallTraceRecord(
                capabilityCallId,
                consultationId,
                threadId,
                runId,
                eventId,
                unitId,
                capabilityId,
                bindingId,
                clinicalStateVersionBefore,
                now()));
    }

    @Transactional
    public CapabilityCallTraceRecord succeed(
            String capabilityCallId,
            String capabilityResultRef,
            String decisionRef,
            String proposalRef,
            String commitRef,
            Integer clinicalStateVersionAfter
    ) {
        CapabilityCallTraceRecord trace = require(capabilityCallId);
        trace.succeed(
                capabilityResultRef,
                decisionRef,
                proposalRef,
                commitRef,
                clinicalStateVersionAfter,
                now());
        return repository.save(trace);
    }

    @Transactional
    public CapabilityCallTraceRecord fail(String capabilityCallId, String reasonCode) {
        CapabilityCallTraceRecord trace = require(capabilityCallId);
        trace.fail(reasonCode, now());
        return repository.save(trace);
    }

    public CapabilityCallTraceRecord require(String capabilityCallId) {
        return repository.findById(capabilityCallId)
                .orElseThrow(() -> new IllegalStateException("Capability call trace not found: " + capabilityCallId));
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
