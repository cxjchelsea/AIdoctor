package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

class U07SyntheticCanonicalEventLedgerTest {
    private final U07SyntheticCanonicalEventLedger ledger = new U07SyntheticCanonicalEventLedger();

    private U07CanonicalEventSource.Snapshot snapshot(String id, String digest,
            U07SyntheticCanonicalEventLedger.EventType type,
            U07CanonicalEventSource.Provenance provenance) {
        return new U07CanonicalEventSource.Snapshot("synthetic-version-1", provenance,
                new U07SyntheticCanonicalEventLedger.Event(id, "synthetic-consult",
                        "synthetic-question", "synthetic-wait", digest, type));
    }

    @Test void firstCanonicalEventIsRecordedOnlyOnce() {
        assertEquals(U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN,
                ledger.register(snapshot("event-1", "digest-1",
                        U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY)).status);
        assertEquals(1, ledger.syntheticEntryCount());
    }

    @Test void exactTransportReplayKeepsOneEventIdentity() {
        U07CanonicalEventSource.Snapshot event = snapshot("event-1", "digest-1",
                U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY);
        ledger.register(event);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY, ledger.register(event).status);
        assertEquals(1, ledger.syntheticEntryCount());
    }

    @Test void changedPayloadUnderProtectedEventIdIsConflict() {
        ledger.register(snapshot("event-1", "digest-A", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY));
        assertEquals(U07SyntheticCanonicalEventLedger.Status.PROTECTED_IDENTITY_CONFLICT,
                ledger.register(snapshot("event-1", "digest-B", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY)).status);
        assertEquals(1, ledger.syntheticEntryCount());
    }

    @Test void changedTypeUnderSameIdIsConflict() {
        ledger.register(snapshot("event-1", "digest-A", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY));
        assertEquals(U07SyntheticCanonicalEventLedger.Status.PROTECTED_IDENTITY_CONFLICT,
                ledger.register(snapshot("event-1", "digest-A", U07SyntheticCanonicalEventLedger.EventType.RESUME_REQUEST,
                        U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY)).status);
    }

    @Test void unverifiedAndPretendAuthenticatedSourceDoNotWrite() {
        assertEquals(U07SyntheticCanonicalEventLedger.Status.SOURCE_UNVERIFIED,
                ledger.register(snapshot("event-1", "digest-1", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.UNVERIFIED)).status);
        assertEquals(U07SyntheticCanonicalEventLedger.Status.SOURCE_UNVERIFIED,
                ledger.register(snapshot("event-1", "digest-1", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.AUTHENTICATED_SOURCE)).status);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void missingIdentityIsInvalidAndDoesNotWrite() {
        assertEquals(U07SyntheticCanonicalEventLedger.Status.INVALID_EVENT,
                ledger.register(snapshot(" ", "digest-1", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY)).status);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void missingSourceSnapshotFailsClosed() {
        assertEquals(U07SyntheticCanonicalEventLedger.Status.SOURCE_UNVERIFIED, ledger.register(null).status);
        assertEquals(0, ledger.syntheticEntryCount());
    }

    @Test void differentCanonicalEventIdsRemainSeparatePendingSemanticPolicy() {
        ledger.register(snapshot("event-1", "digest-1", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY));
        assertEquals(U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN,
                ledger.register(snapshot("event-2", "digest-1", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER,
                        U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY)).status);
        assertEquals(2, ledger.syntheticEntryCount());
    }

    @Test void competingIdenticalRegistrationsOnlyOneFirstSeen() throws InterruptedException {
        final U07SyntheticCanonicalEventLedger.Event event = new U07SyntheticCanonicalEventLedger.Event(
                "race-event", "synthetic-consult", "synthetic-question", "synthetic-wait",
                "synthetic-digest", U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER);
        final U07CanonicalEventSource.Snapshot snap = new U07CanonicalEventSource.Snapshot(
                "synthetic-version-1", U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY, event);
        final AtomicInteger first = new AtomicInteger();
        final AtomicInteger replay = new AtomicInteger();
        final CountDownLatch ready = new CountDownLatch(12);
        final CountDownLatch start = new CountDownLatch(1);
        Thread[] workers = new Thread[12];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(new Runnable() {
                public void run() {
                    ready.countDown();
                    try {
                        start.await();
                        U07SyntheticCanonicalEventLedger.Status status = ledger.register(snap).status;
                        if (status == U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN) first.incrementAndGet();
                        if (status == U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY) replay.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            workers[i].start();
        }
        ready.await();
        start.countDown();
        for (Thread worker : workers) worker.join();
        assertEquals(1, first.get());
        assertEquals(11, replay.get());
        assertEquals(1, ledger.syntheticEntryCount());
    }
}
