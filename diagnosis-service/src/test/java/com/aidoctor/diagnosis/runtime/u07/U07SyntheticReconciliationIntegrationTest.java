package com.aidoctor.diagnosis.runtime.u07;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class U07SyntheticReconciliationIntegrationTest {
    private final U07SyntheticCanonicalEventLedger ledger = new U07SyntheticCanonicalEventLedger();
    private final U07SyntheticLedgerToF8Adapter adapter = new U07SyntheticLedgerToF8Adapter(ledger);
    private final U07SyntheticEventApplicationState applications =
            new U07SyntheticEventApplicationState();
    private final U07SyntheticReconciliationIntegration integration =
            new U07SyntheticReconciliationIntegration(applications);
    private final U07SyntheticEventApplicationState.EventKey key =
            new U07SyntheticEventApplicationState.EventKey("synthetic-event", "digest-1");

    private U07CanonicalEventSource.Snapshot source() {
        return new U07CanonicalEventSource.Snapshot("synthetic-version-1",
                U07CanonicalEventSource.Provenance.SYNTHETIC_TEST_ONLY,
                new U07SyntheticCanonicalEventLedger.Event("synthetic-event", "synthetic-consult",
                        "synthetic-question", "synthetic-wait", "digest-1",
                        U07SyntheticCanonicalEventLedger.EventType.USER_ANSWER));
    }

    private U07InboundAdmission.Input inbound() {
        return new U07InboundAdmission.Input(U07InboundAdmission.EventType.USER_ANSWER,
                "synthetic-event", "synthetic-consult", "synthetic-thread", "synthetic-run",
                null, "synthetic-wait", "synthetic-wait", 3, 3, true, true, true, true, true);
    }

    private U07SyntheticF8Decision.Input business() {
        return new U07SyntheticF8Decision.Input("synthetic-event", "synthetic-consult",
                "synthetic-question", "synthetic-question", "synthetic-wait", "synthetic-wait",
                U07SyntheticF8Decision.EventType.USER_ANSWER,
                U07SyntheticF8Decision.Lifecycle.WAITING_USER,
                U07SyntheticF8Decision.Question.CURRENT_DELIVERED,
                U07SyntheticF8Decision.Ledger.NEW_EVENT,
                U07SyntheticF8Decision.EvidenceScope.SYNTHETIC_VERIFIED, true, true);
    }

    private U07SyntheticLedgerToF8Adapter.Result replay() {
        adapter.evaluate(source(), inbound(), business());
        return adapter.evaluate(source(), inbound(), business());
    }

    @Test void replayWithMissingApplicationStateStaysUnresolved() {
        U07SyntheticReconciliationIntegration.Result r =
                integration.inspect(replay(), source(), key);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.APPLICATION_STATE_MISSING, r.resolution);
        assertFalse(r.mayResumeRuntime);
        assertFalse(r.mayHandoffU02);
    }

    @Test void replayWithReceivedApplicationRemainsPending() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        applications.receive(key, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.APPLICATION_STATE_PENDING,
                integration.inspect(r, source(), key).resolution);
    }

    @Test void acceptedWithoutAppliedReceiptRemainsPending() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        applications.receive(key, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN);
        applications.accept(key, 1);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.APPLICATION_STATE_PENDING,
                integration.inspect(r, source(), key).resolution);
    }

    @Test void failedApplicationRequiresReviewWithoutAutomaticRetry() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        applications.receive(key, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN);
        applications.fail(key, 1);
        U07SyntheticReconciliationIntegration.Result result = integration.inspect(r, source(), key);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.APPLICATION_STATE_FAILED_REVIEW,
                result.resolution);
        assertFalse(result.mayResumeRuntime);
    }

    @Test void syntheticAppliedReceiptDoesNotAuthorizeRuntimeOrU02() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        applications.receive(key, U07SyntheticCanonicalEventLedger.Status.FIRST_SEEN);
        applications.accept(key, 1);
        applications.apply(key, 2, "synthetic-only-receipt");
        U07SyntheticReconciliationIntegration.Result result = integration.inspect(r, source(), key);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.SYNTHETIC_APPLIED_RECEIPT_SEEN,
                result.resolution);
        assertEquals(3L, result.observedRevision);
        assertFalse(result.mayResumeRuntime);
        assertFalse(result.mayHandoffU02);
    }

    @Test void noReconciliationBarrierDoesNotReadAsApplied() {
        U07SyntheticLedgerToF8Adapter.Result first =
                adapter.evaluate(source(), inbound(), business());
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.NOT_A_RECONCILIATION_CASE,
                integration.inspect(first, source(), key).resolution);
    }

    @Test void mismatchedProtectedPayloadDigestFailsBeforeLookup() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        U07SyntheticEventApplicationState.EventKey mismatch =
                new U07SyntheticEventApplicationState.EventKey("synthetic-event", "different-digest");
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.INVALID_EVIDENCE,
                integration.inspect(r, source(), mismatch).resolution);
    }

    @Test void unverifiedSourceCannotSupplyReconciliationProof() {
        U07SyntheticLedgerToF8Adapter.Result r = replay();
        U07CanonicalEventSource.Snapshot forged =
                new U07CanonicalEventSource.Snapshot("synthetic-version-1",
                        U07CanonicalEventSource.Provenance.UNVERIFIED, source().event);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.INVALID_EVIDENCE,
                integration.inspect(r, forged, key).resolution);
    }

    @Test void nullAdapterAndSourceFailClosed() {
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.NOT_A_RECONCILIATION_CASE,
                integration.inspect(null, source(), key).resolution);
        assertEquals(U07SyntheticReconciliationIntegration.Resolution.INVALID_EVIDENCE,
                integration.inspect(replay(), null, key).resolution);
    }
}
