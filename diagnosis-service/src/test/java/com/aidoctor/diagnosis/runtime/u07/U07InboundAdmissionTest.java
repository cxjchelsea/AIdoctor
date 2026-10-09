package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07InboundAdmissionTest {
    private final U07InboundAdmission preflight = new U07InboundAdmission();

    private U07InboundAdmission.Input input(U07InboundAdmission.EventType type,
            String eventId, String checkpointId, String sourceWait,
            long claimed, long current, boolean verified, boolean sourceCurrent,
            boolean waiting, boolean questionCurrent, boolean consultationCurrent) {
        return new U07InboundAdmission.Input(type, eventId, "synthetic-consult",
                "synthetic-thread", "synthetic-run", checkpointId, "synthetic-wait",
                sourceWait, claimed, current, verified, sourceCurrent,
                waiting, questionCurrent, consultationCurrent);
    }

    private U07InboundAdmission.Input good(U07InboundAdmission.EventType type) {
        return input(type, "synthetic-event", "synthetic-checkpoint", "synthetic-wait",
                3, 3, true, true, true, true, true);
    }

    @Test void currentAnswerIsOnlyEligibleForF8NotAccepted() {
        U07InboundAdmission.Result result = preflight.evaluate(good(U07InboundAdmission.EventType.USER_ANSWER));
        assertEquals(U07InboundAdmission.Status.ELIGIBLE_FOR_F8, result.status);
        assertTrue(result.mayEvaluateF8);
    }

    @Test void resumeRequestAlsoRequiresSamePreflight() {
        assertTrue(preflight.evaluate(good(U07InboundAdmission.EventType.RESUME_REQUEST)).mayEvaluateF8);
    }

    @Test void missingCheckpointDoesNotInventBusinessRejection() {
        U07InboundAdmission.Input req = input(U07InboundAdmission.EventType.USER_ANSWER,
                "synthetic-event", null, "synthetic-wait", 3, 3, true, true, true, true, true);
        assertEquals(U07InboundAdmission.Status.ELIGIBLE_FOR_F8, preflight.evaluate(req).status);
    }

    @Test void absentIdentityAndEventTypeFailClosed() {
        assertEquals(U07InboundAdmission.Status.INVALID_EVENT, preflight.evaluate(null).status);
        assertEquals(U07InboundAdmission.Status.INVALID_EVENT,
                preflight.evaluate(input(null, "synthetic-event", null, "synthetic-wait",
                        3, 3, true, true, true, true, true)).status);
        assertEquals(U07InboundAdmission.Status.INVALID_EVENT,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, " ",
                        null, "synthetic-wait", 3, 3, true, true, true, true, true)).status);
    }

    @Test void missingOrStaleSourceEvidenceDoesNotAdmit() {
        assertEquals(U07InboundAdmission.Status.SOURCE_NOT_CURRENT,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 3, 3, false, true, true, true, true)).status);
        assertEquals(U07InboundAdmission.Status.SOURCE_NOT_CURRENT,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 3, 3, true, false, true, true, true)).status);
    }

    @Test void closedWaitAndStaleStateFailClosed() {
        assertEquals(U07InboundAdmission.Status.WAIT_NOT_CURRENT,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 3, 3, true, true, false, true, true)).status);
        assertEquals(U07InboundAdmission.Status.STATE_VERSION_MISMATCH,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 2, 3, true, true, true, true, true)).status);
    }

    @Test void mismatchedWaitOrBindingFailsClosed() {
        assertEquals(U07InboundAdmission.Status.BINDING_MISMATCH,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "other-wait", 3, 3, true, true, true, true, true)).status);
        assertEquals(U07InboundAdmission.Status.BINDING_MISMATCH,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 3, 3, true, true, true, false, true)).status);
        assertEquals(U07InboundAdmission.Status.BINDING_MISMATCH,
                preflight.evaluate(input(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event",
                        null, "synthetic-wait", 3, 3, true, true, true, true, false)).status);
    }
}
