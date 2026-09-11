package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/** Enforces one immutable orchestration/version binding per consultation. */
@Service
public class RuntimeBindingService {
    private final RuntimeBindingRepository repository;
    private final Clock clock;

    public RuntimeBindingService(RuntimeBindingRepository repository) {
        this(repository, Clock.systemUTC());
    }

    RuntimeBindingService(RuntimeBindingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public RuntimeBindingRecord bind(
            String consultationId,
            String cdpId,
            String runtimeAuthority,
            String scopeVersion,
            String capabilitySetVersion,
            String contractVersion
    ) {
        Optional<RuntimeBindingRecord> existing = repository.findById(consultationId);
        if (existing.isPresent()) {
            RuntimeBindingRecord binding = existing.get();
            if (!binding.sameBinding(cdpId, runtimeAuthority, scopeVersion, capabilitySetVersion, contractVersion)) {
                throw new IllegalStateException("Consultation runtime/version binding is immutable once established.");
            }
            return binding;
        }

        RuntimeBindingRecord created = new RuntimeBindingRecord(
                consultationId,
                cdpId,
                runtimeAuthority,
                "thread_" + UUID.randomUUID().toString().replace("-", ""),
                scopeVersion,
                capabilitySetVersion,
                contractVersion,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return repository.save(created);
    }

    public RuntimeBindingRecord requireBinding(String consultationId) {
        return repository.findById(consultationId)
                .orElseThrow(() -> new IllegalStateException("Runtime binding not found: " + consultationId));
    }
}
