package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07SyntheticEventApplicationStateTest {
    private final U07SyntheticEventApplicationState store = new U07SyntheticEventApplicationState();
    private final U07SyntheticEventApplicationState.EventKey key =
            new U07SyntheticEventApplicationState.EventKey("synthetic-event-1", "synthetic-digest-1");

    private U07SyntheticEventApplicationState.Write receive() {
        return store.receive(key, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN);
    }

    @Test void receivesButNeverPretendsApplied() {
        U07SyntheticEventApplicationState.Write result = receive();
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CREATED, result.status);
        assertEquals(U07SyntheticEventApplicationState.Phase.RECEIVED, result.snapshot.phase);
        assertEquals(U07SyntheticEventApplicationState.Recovery.NEEDS_RECONCILIATION,
                store.reconcile(key).recovery);
    }

    @Test void duplicateRegistrationCannotCreateApplicationEvidence() {
        receive();
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.INVALID,
                store.receive(key, U07SyntheticCanonicalEventLedger.Status.SAME_EVENT_REPLAY).status);
        assertEquals(U07SyntheticEventApplicationState.Phase.RECEIVED, store.reconcile(key).snapshot.phase);
        assertEquals(1, store.syntheticEntryCount());
    }

    @Test void normalSyntheticSequenceRequiresExpectedRevisionsAndReceipt() {
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CREATED, receive().status);
        U07SyntheticEventApplicationState.Write accepted = store.accept(key, 1);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.ADVANCED, accepted.status);
        assertEquals(U07SyntheticEventApplicationState.Phase.ACCEPTED, accepted.snapshot.phase);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.apply(key, 1, "synthetic-receipt").status);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.apply(key, 2, " ").status);
        U07SyntheticEventApplicationState.Write applied = store.apply(key, 2, "synthetic-receipt");
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.ADVANCED, applied.status);
        assertEquals(U07SyntheticEventApplicationState.Phase.APPLIED, applied.snapshot.phase);
        assertEquals(U07SyntheticEventApplicationState.Recovery.APPLIED_SYNTHETIC_RECEIPT_PRESENT,
                store.reconcile(key).recovery);
    }

    @Test void cannotApplyBeforeAcceptanceOrOverwriteApplied() {
        receive();
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.apply(key, 1, "receipt").status);
        store.accept(key, 1);
        store.apply(key, 2, "receipt");
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.apply(key, 3, "second-receipt").status);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.fail(key, 3).status);
    }

    @Test void failedEventNeedsReviewAndCannotBeImplicitlyRetried() {
        receive();
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.ADVANCED,
                store.fail(key, 1).status);
        assertEquals(U07SyntheticEventApplicationState.Recovery.FAILED_REQUIRES_REVIEW,
                store.reconcile(key).recovery);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.accept(key, 2).status);
    }

    @Test void eventDigestMismatchAndMalformedRegistrationFailClosed() {
        receive();
        U07SyntheticEventApplicationState.EventKey altered =
                new U07SyntheticEventApplicationState.EventKey(key.eventId, "different-digest");
        assertEquals(U07SyntheticEventApplicationState.Recovery.CONFLICT,
                store.reconcile(altered).recovery);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.accept(altered, 1).status);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.INVALID,
                store.receive(new U07SyntheticEventApplicationState.EventKey(" ", "digest"),
                        U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN).status);
    }

    @Test void absentEventDoesNotGenerateFakeState() {
        assertEquals(U07SyntheticEventApplicationState.Recovery.NOT_FOUND,
                store.reconcile(key).recovery);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.NOT_FOUND,
                store.accept(key, 1).status);
        assertEquals(0, store.syntheticEntryCount());
    }

    @Test void staleRevisionCannotApplyOrFail() {
        receive();
        store.accept(key, 1);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.fail(key, 1).status);
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.apply(key, 1, "synthetic-receipt").status);
        assertEquals(U07SyntheticEventApplicationState.Phase.ACCEPTED,
                store.reconcile(key).snapshot.phase);
    }

    @Test void protectedIdentityReRegistrationConflictsAndDoesNotReplaceDigest() {
        receive();
        U07SyntheticEventApplicationState.EventKey altered =
                new U07SyntheticEventApplicationState.EventKey(key.eventId, "changed");
        assertEquals(U07SyntheticEventApplicationState.WriteStatus.CONFLICT,
                store.receive(altered, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN).status);
        assertEquals(key.payloadDigest, store.reconcile(key).snapshot.payloadDigest);
    }
}
