package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.Optional;

/**
 * Lookup / remember of the original authoritative {@link StateTypes.CommitResult}.
 */
public interface IdempotencyPort {

    Optional<IdempotencyRecord> lookup(String idempotencyKey);

    void remember(String idempotencyKey, String canonicalFingerprint, StateTypes.CommitResult originalResult);

    final class IdempotencyRecord {
        public final String canonicalFingerprint;
        public final StateTypes.CommitResult originalResult;

        public IdempotencyRecord(String canonicalFingerprint, StateTypes.CommitResult originalResult) {
            this.canonicalFingerprint = canonicalFingerprint;
            this.originalResult = originalResult;
        }
    }
}
