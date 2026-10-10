package com.aidoctor.diagnosis.runtime.u07;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local synthetic diagnostic facts; never a clinical receipt or dispatch authority. */
public final class U07SyntheticResults {
    private U07SyntheticResults() { }
    public enum Operation { SUCCEEDED, REJECTED, FAILED }
    public enum Transaction { NOT_STARTED, ROLLED_BACK, COMMITTED, COMMIT_OUTCOME_UNKNOWN, ABORT_UNCONFIRMED }
    public enum Response { AVAILABLE, SUPPRESSED_AFTER_COMMIT }
    public enum Cleanup { NOT_REQUIRED, COMPLETE, FAILED }
    public enum Business { ACCEPTED, SAME_EVENT_REPLAY, REJECTED_CURRENTNESS }
    public enum Read { NOT_ATTEMPTED, COMPLETE, UNAVAILABLE }
    public enum Evidence { NO_DURABLE_EVENT, RECEIVED_ONLY_REQUIRES_REVIEW, HISTORICAL_ACCEPTED,
        PARTIAL_OR_INCONSISTENT, IDENTITY_CONFLICT }
    public static final class Observation {
        public final Evidence state;
        public final Instant observedAt;
        Observation(Evidence state) { this.state = state; observedAt = Instant.now(); }
    }
    public static final class WriteResult {
        public final Operation operationStatus;
        public final Transaction transactionStatus;
        public final Response responseAvailability;
        public final Cleanup cleanupStatus;
        public final Business businessOutcome;
        public final Throwable primaryFailure;
        public final List<Throwable> cleanupErrors;
        // execute never initiates recovery. There is no mutable observation slot.
        WriteResult(Operation op, Transaction tx, Response response, Cleanup cleanup,
                    Business business, Throwable failure, List<Throwable> errors) {
            operationStatus = op; transactionStatus = tx; responseAvailability = response;
            cleanupStatus = cleanup; businessOutcome = business; primaryFailure = failure;
            cleanupErrors = Collections.unmodifiableList(new ArrayList<>(errors));
        }
        public boolean isNormalSuccess() {
            return operationStatus == Operation.SUCCEEDED && transactionStatus == Transaction.COMMITTED
                    && responseAvailability == Response.AVAILABLE && cleanupStatus == Cleanup.COMPLETE;
        }
    }
    public static final class RecoveryResult {
        public final Operation operationStatus;
        public final Read readStatus;
        public final Observation observation;
        public final Cleanup cleanupStatus;
        public final Throwable primaryFailure;
        public final List<Throwable> cleanupErrors;
        RecoveryResult(Operation op, Read read, Observation observation, Cleanup cleanup,
                       Throwable failure, List<Throwable> errors) {
            operationStatus = op; readStatus = read; this.observation = observation;
            cleanupStatus = cleanup; primaryFailure = failure;
            cleanupErrors = Collections.unmodifiableList(new ArrayList<>(errors));
        }
    }
}
