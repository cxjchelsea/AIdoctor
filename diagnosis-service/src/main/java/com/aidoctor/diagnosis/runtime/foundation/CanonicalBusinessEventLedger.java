package com.aidoctor.diagnosis.runtime.foundation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Resolves transport retries to one canonical event identity.
 *
 * <p>This component deliberately does not decide ACCEPTED, DUPLICATE,
 * EXPIRED, REJECTED or APPLIED. Those are business decisions outside the
 * Foundation-0 event ledger.</p>
 */
@Service
public class CanonicalBusinessEventLedger {
    private final CanonicalBusinessEventRepository repository;
    private final Clock clock;

    public CanonicalBusinessEventLedger(CanonicalBusinessEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    CanonicalBusinessEventLedger(CanonicalBusinessEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CanonicalBusinessEventRecord resolveOrCreate(
            String eventId,
            String consultationId,
            String eventType,
            String idempotencyKey,
            String payloadDigest
    ) {
        Optional<CanonicalBusinessEventRecord> byEventId = repository.findById(eventId);
        if (byEventId.isPresent()) {
            return requireSame(byEventId.get(), consultationId, eventType, idempotencyKey, payloadDigest);
        }

        Optional<CanonicalBusinessEventRecord> byIdempotency = repository.findByIdempotencyKey(idempotencyKey);
        if (byIdempotency.isPresent()) {
            return requireSame(byIdempotency.get(), consultationId, eventType, idempotencyKey, payloadDigest);
        }

        CanonicalBusinessEventRecord created = new CanonicalBusinessEventRecord(
                eventId, consultationId, eventType, idempotencyKey, payloadDigest,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        try {
            return repository.save(created);
        } catch (DataIntegrityViolationException race) {
            // Concurrent transport retries may race the unique idempotency key.
            CanonicalBusinessEventRecord winner = repository.findById(eventId)
                    .orElseGet(() -> repository.findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() -> race));
            return requireSame(winner, consultationId, eventType, idempotencyKey, payloadDigest);
        }
    }

    private CanonicalBusinessEventRecord requireSame(
            CanonicalBusinessEventRecord existing,
            String consultationId,
            String eventType,
            String idempotencyKey,
            String payloadDigest
    ) {
        if (!existing.sameCanonicalInput(consultationId, eventType, idempotencyKey, payloadDigest)) {
            throw new IllegalStateException("Canonical event identity was reused with different semantics.");
        }
        return existing;
    }
}
