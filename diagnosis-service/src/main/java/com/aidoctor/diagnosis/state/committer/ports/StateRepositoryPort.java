package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.contracts.v1.StateTypes;

/**
 * Minimal authoritative mechanical mutation boundary.
 *
 * <p>{@code COMMITTED} means the mutation was atomically admitted and the
 * version advanced exactly once. {@code CONFLICT}, {@code FAILED}, and an
 * exception mean no mutation. An adapter must establish that the committed
 * transition is valid and representable before performing its authoritative
 * mutation. This does not assert that patch operations were applied to a
 * clinical record, and is not a clinical state store.
 */
public interface StateRepositoryPort {

    int readCurrentVersion(String cdpId);

    AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command);

    final class AtomicCommitCommand {
        public final String cdpId;
        public final int expectedCurrentVersion;
        public final String patchId;
        public final String idempotencyKey;
        public final StateTypes.StatePatch patch;

        public AtomicCommitCommand(
                String cdpId,
                int expectedCurrentVersion,
                String patchId,
                String idempotencyKey,
                StateTypes.StatePatch patch
        ) {
            this.cdpId = cdpId;
            this.expectedCurrentVersion = expectedCurrentVersion;
            this.patchId = patchId;
            this.idempotencyKey = idempotencyKey;
            this.patch = patch;
        }
    }

    final class AtomicCommitOutcome {
        public enum Status {
            COMMITTED,
            CONFLICT,
            FAILED
        }

        public final Status status;
        public final int previousVersion;
        public final int committedVersion;
        public final int actualVersion;
        public final String failureCode;
        public final String failureMessage;
        public final boolean retryable;

        private AtomicCommitOutcome(
                Status status,
                int previousVersion,
                int committedVersion,
                int actualVersion,
                String failureCode,
                String failureMessage,
                boolean retryable
        ) {
            this.status = status;
            this.previousVersion = previousVersion;
            this.committedVersion = committedVersion;
            this.actualVersion = actualVersion;
            this.failureCode = failureCode;
            this.failureMessage = failureMessage;
            this.retryable = retryable;
        }

        /** Makes an invalid committed version transition unrepresentable. */
        public static AtomicCommitOutcome committed(int previousVersion) {
            int committedVersion = Math.addExact(previousVersion, 1);
            return new AtomicCommitOutcome(
                    Status.COMMITTED,
                    previousVersion,
                    committedVersion,
                    committedVersion,
                    null,
                    null,
                    false
            );
        }

        public static AtomicCommitOutcome conflict(int expectedVersion, int actualVersion) {
            return new AtomicCommitOutcome(
                    Status.CONFLICT,
                    actualVersion,
                    0,
                    actualVersion,
                    null,
                    null,
                    true
            );
        }

        public static AtomicCommitOutcome failed(String failureCode, String failureMessage) {
            return failedRetryable(failureCode, failureMessage);
        }

        public static AtomicCommitOutcome failedRetryable(String failureCode, String failureMessage) {
            return new AtomicCommitOutcome(
                    Status.FAILED,
                    0,
                    0,
                    0,
                    failureCode,
                    failureMessage,
                    true
            );
        }

        public static AtomicCommitOutcome failedNonRetryable(String failureCode, String failureMessage) {
            return new AtomicCommitOutcome(
                    Status.FAILED,
                    0,
                    0,
                    0,
                    failureCode,
                    failureMessage,
                    false
            );
        }
    }
}
