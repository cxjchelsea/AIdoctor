package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07SyntheticResumePipelineTest {
    private final U07SyntheticResumePipeline pipeline = new U07SyntheticResumePipeline();

    private U07InboundAdmission.Input inbound(U07InboundAdmission.EventType type,
                                                String event, boolean sourceCurrent,
                                                boolean waiting, long version) {
        return new U07InboundAdmission.Input(type, event, "synthetic-consult",
                "synthetic-thread", "synthetic-run", null, "synthetic-wait", "synthetic-wait",
                version, 3, true, sourceCurrent, waiting, true, true);
    }

    private U07SyntheticF8Decision.Input business(U07SyntheticF8Decision.EventType type,
                                                   String event, String wait,
                                                   U07SyntheticF8Decision.Ledger ledger,
                                                   U07SyntheticF8Decision.EvidenceScope scope) {
        return new U07SyntheticF8Decision.Input(event, "synthetic-consult",
                "synthetic-question", "synthetic-question", wait, "synthetic-wait",
                type, U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                ledger, scope, true, true);
    }

    @Test void userAnswerPassesPreflightThenF8Accepted() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.F8_CLASSIFIED, r.stage);
        assertEquals(U07InboundAdmission.Status.ELIGIBLE_FOR_F8, r.admissionStatus);
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, r.f8Outcome);
    }

    @Test void resumeRequestUsesSameCrossLayerIdentity() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.RESUME_REQUEST, "synthetic-resume", true, true, 3),
                business(U07SyntheticF8Decision.EventType.RESUME_REQUEST, "synthetic-resume",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, r.f8Outcome);
    }

    @Test void failedAdmissionNeverExecutesF8() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", false, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.BLOCKED_BY_ADMISSION, r.stage);
        assertEquals(U07InboundAdmission.Status.SOURCE_NOT_CURRENT, r.admissionStatus);
        assertEquals(null, r.f8Outcome);
    }

    @Test void staleVersionOrNotWaitingBlocksF8() {
        for (U07InboundAdmission.Input candidate : new U07InboundAdmission.Input[] {
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 2),
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, false, 3)}) {
            U07SyntheticResumePipeline.Result r = pipeline.evaluate(candidate,
                    business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                            "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                            U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
            assertEquals(U07SyntheticResumePipeline.Stage.BLOCKED_BY_ADMISSION, r.stage);
            assertEquals(null, r.f8Outcome);
        }
    }

    @Test void replayStatusFlowsFromF8WithoutEffects() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.ALREADY_APPLIED_SAME_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticF8Decision.Outcome.DUPLICATE, r.f8Outcome);
    }

    @Test void conflictingEventIdentityAndTypeFailBeforeF8() {
        U07SyntheticResumePipeline.Result mismatchedEvent = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "event-a", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "event-b",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION, mismatchedEvent.stage);
        assertEquals(null, mismatchedEvent.f8Outcome);
        U07SyntheticResumePipeline.Result mismatchedType = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "event-a", true, true, 3),
                business(U07SyntheticF8Decision.EventType.RESUME_REQUEST, "event-a",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION, mismatchedType.stage);
    }

    @Test void crossLayerWaitMismatchOrUnverifiedScopeFailsClosed() {
        U07SyntheticResumePipeline.Result badWait = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "different-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION, badWait.stage);
        U07SyntheticResumePipeline.Result notSynthetic = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.NEW_EVENT,
                        U07SyntheticF8Decision.EvidenceScope.UNVERIFIED_OR_REAL));
        assertEquals(U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION, notSynthetic.stage);
    }

    @Test void nullInputsNeverYieldF8Result() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(null, null);
        assertEquals(U07SyntheticResumePipeline.Stage.INVALID_COMPOSITION, r.stage);
        assertEquals(null, r.f8Outcome);
    }

    @Test void unknownLedgerNeverBecomesBusinessAcceptance() {
        U07SyntheticResumePipeline.Result r = pipeline.evaluate(
                inbound(U07InboundAdmission.EventType.USER_ANSWER, "synthetic-event", true, true, 3),
                business(U07SyntheticF8Decision.EventType.USER_ANSWER, "synthetic-event",
                        "synthetic-wait", U07SyntheticF8Decision.Ledger.UNKNOWN,
                        U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED));
        assertEquals(U07SyntheticResumePipeline.Stage.F8_CLASSIFIED, r.stage);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, r.f8Outcome);
    }
}
