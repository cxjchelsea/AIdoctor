package com.aidoctor.diagnosis.runtime.u07;

/**
 * Pure, non-authoritative preflight for a U07 answer/resume event.
 * A positive result means only that the event may be considered by F8.
 * It does not confer Business Resume ACCEPTED, checkpoint compatibility,
 * permission to mutate state, or any clinical/runtime action.
 */
public final class U07InboundAdmission {
    public enum EventType { USER_ANSWER, RESUME_REQUEST }
    public enum Status {
        ELIGIBLE_FOR_F8,
        INVALID_EVENT,
        SOURCE_NOT_CURRENT,
        WAIT_NOT_CURRENT,
        STATE_VERSION_MISMATCH,
        BINDING_MISMATCH
    }

    public static final class Input {
        public final EventType eventType;
        public final String eventId, consultationId, threadId, runId, checkpointId;
        public final String canonicalWaitEffectId, sourceWaitEffectId;
        public final long claimedStateVersion, authoritativeStateVersion;
        public final boolean sourceEventVerified, sourceEventCurrent, waitingUser;
        public final boolean questionBindingCurrent, consultationBindingCurrent;

        public Input(EventType eventType, String eventId, String consultationId, String threadId,
                     String runId, String checkpointId, String canonicalWaitEffectId,
                     String sourceWaitEffectId, long claimedStateVersion, long authoritativeStateVersion,
                     boolean sourceEventVerified, boolean sourceEventCurrent, boolean waitingUser,
                     boolean questionBindingCurrent, boolean consultationBindingCurrent) {
            this.eventType = eventType;
            this.eventId = eventId;
            this.consultationId = consultationId;
            this.threadId = threadId;
            this.runId = runId;
            this.checkpointId = checkpointId;
            this.canonicalWaitEffectId = canonicalWaitEffectId;
            this.sourceWaitEffectId = sourceWaitEffectId;
            this.claimedStateVersion = claimedStateVersion;
            this.authoritativeStateVersion = authoritativeStateVersion;
            this.sourceEventVerified = sourceEventVerified;
            this.sourceEventCurrent = sourceEventCurrent;
            this.waitingUser = waitingUser;
            this.questionBindingCurrent = questionBindingCurrent;
            this.consultationBindingCurrent = consultationBindingCurrent;
        }
    }

    public static final class Result {
        public final Status status;
        public final boolean mayEvaluateF8;
        private Result(Status status) {
            this.status = status;
            this.mayEvaluateF8 = status == Status.ELIGIBLE_FOR_F8;
        }
    }

    public Result evaluate(Input input) {
        if (input == null || input.eventType == null || blank(input.eventId)
                || blank(input.consultationId) || blank(input.threadId) || blank(input.runId)
                || blank(input.canonicalWaitEffectId) || blank(input.sourceWaitEffectId)
                || input.claimedStateVersion < 0 || input.authoritativeStateVersion < 0) {
            return new Result(Status.INVALID_EVENT);
        }
        // An answer may be business-valid even when runtime checkpoint reconstruction
        // is unavailable. Do not use checkpoint presence as an admission condition.
        if (!input.sourceEventVerified || !input.sourceEventCurrent) {
            return new Result(Status.SOURCE_NOT_CURRENT);
        }
        if (!input.waitingUser) {
            return new Result(Status.WAIT_NOT_CURRENT);
        }
        if (input.claimedStateVersion != input.authoritativeStateVersion) {
            return new Result(Status.STATE_VERSION_MISMATCH);
        }
        if (!input.questionBindingCurrent || !input.consultationBindingCurrent
                || !input.canonicalWaitEffectId.equals(input.sourceWaitEffectId)) {
            return new Result(Status.BINDING_MISMATCH);
        }
        return new Result(Status.ELIGIBLE_FOR_F8);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
