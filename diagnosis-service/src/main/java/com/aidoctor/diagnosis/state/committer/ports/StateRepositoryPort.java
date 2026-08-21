package com.aidoctor.diagnosis.state.committer.ports;

/**
 * Minimal repository abstraction for one mechanical commit attempt.
 * Not a clinical state store, Encounter model, or persistence engine.
 */
public interface StateRepositoryPort {

    int readCurrentVersion(String cdpId);

    AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command);

    final class AtomicCommitCommand {
        public final String cdpId;
        public final int expectedCurrentVersion;
        public final String patchId;
        public final String idempotencyKey;

        public AtomicCommitCommand(
                String cdpId,
                int expectedCurrentVersion,
                String patchId,
                String idempotencyKey
        ) {
            this.cdpId = cdpId;
            this.expectedCurrentVersion = expectedCurrentVersion;
            this.patchId = patchId;
            this.idempotencyKey = idempotencyKey;
        }
    }

    final class AtomicCommitOutcome {
        public enum Status {
            COMMITTED,
            CONFLICT,
            FAILED
        }

        public final Status status;
        public final Integer previousVersion;
        public final Integer committedVersion;
        public final Integer actualVersion;
        public final String failureCode;
        public final String failureMessage;

        private AtomicCommitOutcome(
                Status status,
                Integer previousVersion,
                Integer committedVersion,
                Integer actualVersion,
                String failureCode,
                String failureMessage
        ) {
            this.status = status;
            this.previousVersion = previousVersion;
            this.committedVersion = committedVersion;
            this.actualVersion = actualVersion;
            this.failureCode = failureCode;
            this.failureMessage = failureMessage;
        }

        public static AtomicCommitOutcome committed(int previousVersion, int committedVersion) {
            return new AtomicCommitOutcome(
                    Status.COMMITTED,
                    Integer.valueOf(previousVersion),
                    Integer.valueOf(committedVersion),
                    Integer.valueOf(committedVersion),
                    null,
                    null
            );
        }

        public static AtomicCommitOutcome conflict(int expectedVersion, int actualVersion) {
            return new AtomicCommitOutcome(
                    Status.CONFLICT,
                    Integer.valueOf(actualVersion),
                    null,
                    Integer.valueOf(actualVersion),
                    null,
                    null
            );
        }

        public static AtomicCommitOutcome failed(String failureCode, String failureMessage) {
            return new AtomicCommitOutcome(
                    Status.FAILED,
                    null,
                    null,
                    null,
                    failureCode,
                    failureMessage
            );
        }
    }
}
