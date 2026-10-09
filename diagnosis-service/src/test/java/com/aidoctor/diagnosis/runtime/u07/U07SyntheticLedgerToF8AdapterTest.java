package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07SyntheticLedgerToF8AdapterTest {
    private final U07SyntheticCanonicalEventLedger ledger = new U07SyntheticCanonicalEventLedger();
    private final U07SyntheticLedgerToF8Adapter adapter = new U07SyntheticLedgerToF8Adapter(ledger);

    private U07CanonicalEventSource.Snapshot source(
            String id, String digest, U07SyntheticCanonicalEventLedger.EventType type) {
        return new U07CanonicalEventSource.Snapshot("synthetic-version-1",
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY,
                new U07SyntheticCanonicalEventLedger.Event(id, "synthetic-consult",
                        "synthetic-question", "synthetic-wait", digest, type));
    }

    private U07InboundAdmission.Input inbound(String id, boolean current, String wait) {
        return new U07InboundAdmission.Input(U07InboundAdmission.EventType.USER_ANSWER,
                id, "synthetic-consult", "synthetic-thread", "synthetic-run", null,
                "synthetic-wait", wait, 3, 3, true, current, true, true, true);
    }

    private U07SyntheticF8Decision.Input business(String id,
            U07SyntheticF8Decision.Ledger ledgerStatus) {
        return new U07SyntheticF8Decision.Input(id, "synthetic-consult",
                "synthetic-question", "synthetic-question", "synthetic-wait",
                "synthetic-wait", U07SyntheticF8Decision.EventType.USER_ANSWER,
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED, ledgerStatus,
                U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED, true, true);
    }

    @Test void firstSeenEvaluatesAdmissionAndF8ButNotApply() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.F8_EVALUATED, r.stage);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN, r.registrationStatus);
        assertEquals(U07SyntheticF8Decision.Outcome.ACCEPTED, r.f8Outcome);
        assertEquals(1, ledger.syntheticEntryCount());
    }

    @Test void firstSeenCannotInventAppliedHistoryFromCallerSuppliedLedgerFlag() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.ALREADY_APPLIED_SAME_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.RECONCILIATION_REQUIRED, r.stage);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN, r.registrationStatus);
        assertEquals(null, r.f8Outcome);
    }

    @Test void replayIsReconciliationNotBusinessDuplicateEvenIfClaimedApplied() {
        U07CanonicalEventSource.Snapshot s = source("event-1", "digest-a",
                U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER);
        adapter.evaluate(s, inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(s,
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.ALREADY_APPLIED_SAME_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.RECONCILIATION_REQUIRED, r.stage);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY, r.registrationStatus);
        assertEquals(null, r.f8Outcome);
        assertEquals(1, ledger.syntheticEntryCount());
    }

    @Test void sameIdDifferentPayloadIsProtectedIdentityConflict() {
        U07CanonicalEventSource.Snapshot original = source("event-1", "digest-a",
                U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER);
        adapter.evaluate(original, inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-b", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.LEDGER_BLOCKED, r.stage);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.PROTECTED_IDENTITY_CONFLICT,
                r.registrationStatus);
        assertEquals(null, r.f8Outcome);
    }

    @Test void unknownBusinessLedgerStateNeverBecomesAccepted() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.UNKNOWN));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.F8_EVALUATED, r.stage);
        assertEquals(U07SyntheticF8Decision.Outcome.INSUFFICIENT_EVIDENCE, r.f8Outcome);
    }

    @Test void staleAdmissionBlocksF8AfterRegistration() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", false, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.ADMISSION_BLOCKED, r.stage);
        assertEquals(U07InboundAdmission.Status.SOURCE_NOT_CURRENT, r.admissionStatus);
        assertEquals(null, r.f8Outcome);
    }

    @Test void mismatchedSourceEventIdCannotRegister() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-source", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-inbound", true, "synthetic-wait"),
                business("event-inbound", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.INVALID_CROSS_INPUT, r.stage);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void mismatchedWaitCannotRegister() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "other-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.INVALID_CROSS_INPUT, r.stage);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void nullAndUnverifiedSnapshotCannotApplyAnything() {
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.INVALID_CROSS_INPUT,
                adapter.evaluate(null, inbound("event-1", true, "synthetic-wait"),
                        business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT)).stage);
        U07CanonicalEventSource.Snapshot forged = new U07CanonicalEventSource.Snapshot(
                "untrusted-version", U07CanonicalEventSource.Provenance.UNVERIFIED,
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER).event);
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.INVALID_CROSS_INPUT,
                adapter.evaluate(forged, inbound("event-1", true, "synthetic-wait"),
                        business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT)).stage);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void businessEventTypeMismatchCannotRegister() {
        U07SyntheticLedgerToF8Adapter.Result r = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.RESUME_REQUEST),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.INVALID_CROSS_INPUT, r.stage);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void separateEventIdsDoNotClaimSemanticDeduplication() {
        U07SyntheticLedgerToF8Adapter.Result one = adapter.evaluate(
                source("event-1", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-1", true, "synthetic-wait"),
                business("event-1", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        U07SyntheticLedgerToF8Adapter.Result two = adapter.evaluate(
                source("event-2", "digest-a", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER),
                inbound("event-2", true, "synthetic-wait"),
                business("event-2", U07SyntheticF8Decision.Ledger.NEW_EVENT));
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.F8_EVALUATED, one.stage);
        assertEquals(U07SyntheticLedgerToF8Adapter.Stage.F8_EVALUATED, two.stage);
        assertEquals(2, ledger.syntheticEntryCount());
    }
}
