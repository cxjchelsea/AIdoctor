package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.contracts.v1.StateTypes;

/**
 * Mechanical pre-commit idempotency reservation lifecycle.
 *
 * <p>{@link #reserve(String, String)} is atomic: only the caller receiving
 * {@code ACQUIRED} may attempt repository mutation. A completion failure must
 * leave the key reserved (or completed), so a retry cannot commit twice.
 */
public interface IdempotencyPort {

    Decision inspect(String idempotencyKey, String canonicalFingerprint);

    Decision reserve(String idempotencyKey, String canonicalFingerprint);

    void complete(
            String idempotencyKey,
            String canonicalFingerprint,
            StateTypes.CommitResult originalResult
    );

    void release(String idempotencyKey, String canonicalFingerprint);

    final class Decision {
        public enum Status {
            ABSENT,
            ACQUIRED,
            RESERVED_SAME_FINGERPRINT,
            COMPLETED_SAME_FINGERPRINT,
            MISMATCH
        }

        public final Status status;
        public final StateTypes.CommitResult originalResult;

        private Decision(Status status, StateTypes.CommitResult originalResult) {
            this.status = status;
            this.originalResult = originalResult;
        }

        public static Decision absent() {
            return new Decision(Status.ABSENT, null);
        }

        public static Decision acquired() {
            return new Decision(Status.ACQUIRED, null);
        }

        public static Decision reservedSameFingerprint() {
            return new Decision(Status.RESERVED_SAME_FINGERPRINT, null);
        }

        public static Decision completedSameFingerprint(StateTypes.CommitResult originalResult) {
            if (originalResult == null) {
                throw new IllegalArgumentException("originalResult is required");
            }
            return new Decision(Status.COMPLETED_SAME_FINGERPRINT, originalResult);
        }

        public static Decision mismatch() {
            return new Decision(Status.MISMATCH, null);
        }
    }
}
