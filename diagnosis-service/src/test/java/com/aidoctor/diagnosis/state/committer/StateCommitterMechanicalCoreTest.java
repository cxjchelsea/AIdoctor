package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticFieldPermissionFake;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.IdempotencyPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import com.aidoctor.diagnosis.state.committer.support.CommitResultSchemaAssertions;
import com.aidoctor.diagnosis.state.committer.support.StateCommitterTestHarness;
import com.aidoctor.diagnosis.state.committer.support.SyntheticStatePatchFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateCommitterMechanicalCoreTest {

    @Test
    void validSyntheticPatchCommits() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-001", "synthetic-patch-001");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("COMMITTED", result.status);
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
        assertEquals(Boolean.FALSE, result.auditRef.phiCapable);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(1, harness.events.events().size());
        assertEquals(InternalCommitEventEvidence.KIND_COMMITTED, harness.events.events().get(0).eventKind);
        assertEquals(false, harness.events.events().get(0).authoritative);
    }

    @Test
    void successfulCommitIncrementsVersionExactlyOnce() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 4);
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(4, "synthetic-idem-002", "synthetic-patch-002");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("COMMITTED", result.status);
        assertEquals(Integer.valueOf(4), result.previousVersion);
        assertEquals(Integer.valueOf(5), result.committedVersion);
        assertEquals(5, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
    }

    @Test
    void sameIdempotencyKeyAndSamePatchReplaysOriginalResult() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 4);
        StateTypes.StatePatch first = SyntheticStatePatchFactory.valid(4, "synthetic-idem-replay", "synthetic-patch-replay-a");
        StateTypes.CommitResult original = harness.committer.commit(first);

        StateTypes.StatePatch replay = SyntheticStatePatchFactory.valid(4, "synthetic-idem-replay", "synthetic-patch-replay-b");
        StateTypes.CommitResult replayed = harness.committer.commit(replay);

        assertEquals("COMMITTED", original.status);
        assertEquals(Integer.valueOf(4), original.previousVersion);
        assertEquals(Integer.valueOf(5), original.committedVersion);
        assertEquals(original.status, replayed.status);
        assertEquals(original.previousVersion, replayed.previousVersion);
        assertEquals(original.committedVersion, replayed.committedVersion);
        assertEquals(original.reasonCode, replayed.reasonCode);
        assertEquals(original.auditRef.auditId, replayed.auditRef.auditId);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(5, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        assertEquals(InternalCommitEventEvidence.KIND_REPLAYED, harness.events.events().get(1).eventKind);
        assertNotEquals("NO_OP", replayed.status);
    }

    @Test
    void sameIdempotencyKeyWithDifferentPatchConflicts() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch first = SyntheticStatePatchFactory.valid(0, "synthetic-idem-mismatch", "synthetic-patch-mismatch-a");
        harness.committer.commit(first);

        StateTypes.StatePatch different = SyntheticStatePatchFactory.valid(0, "synthetic-idem-mismatch", "synthetic-patch-mismatch-b");
        different.operations.get(0).value = "beta";
        StateTypes.CommitResult result = harness.committer.commit(different);

        assertEquals("CONFLICT", result.status);
        assertEquals(CommitReasonCodes.IDEMPOTENCY_MISMATCH, result.reasonCode);
        assertEquals("IDEMPOTENCY_MISMATCH", result.conflicts.get(0).type);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(1, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
    }

    @Test
    void staleBaseVersionConflicts() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 7);
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(4, "synthetic-idem-stale", "synthetic-patch-stale");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("CONFLICT", result.status);
        assertEquals(CommitReasonCodes.VERSION_MISMATCH, result.reasonCode);
        assertEquals("VERSION_MISMATCH", result.conflicts.get(0).type);
        assertEquals(Integer.valueOf(4), result.conflicts.get(0).expectedVersion);
        assertEquals(Integer.valueOf(7), result.conflicts.get(0).actualVersion);
        assertEquals(Integer.valueOf(7), result.previousVersion);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(7, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
    }

    @Test
    void unauthorizedFieldIsRejectedBeforeMutation() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.withPath("/patient_state/unauthorized_field", 0);

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.FIELD_NOT_AUTHORIZED, result.reasonCode);
        assertEquals(Integer.valueOf(0), result.rejectedOperations.get(0).operationIndex);
        assertEquals(0, harness.repository.readCalls());
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void invalidCapabilityIsRejectedBeforeMutation() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.withCapability("unknown-capability-id", 0);

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.CAPABILITY_NOT_AUTHORIZED, result.reasonCode);
        assertEquals(1, result.rejectedOperations.size());
        assertEquals(0, harness.repository.readCalls());
        assertEquals(0, harness.repository.commitCalls());
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void missingConsentIsRejected() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.consentPolicy.missing(SyntheticStatePatchFactory.CDP_ID);
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-consent-missing", "synthetic-patch-consent-missing");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertRejectedConsent(result, harness);
    }

    @Test
    void deniedConsentIsRejected() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.consentPolicy.deny(SyntheticStatePatchFactory.CDP_ID);
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-consent-denied", "synthetic-patch-consent-denied");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertRejectedConsent(result, harness);
    }

    @Test
    void invalidSyntheticConsentIsRejected() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.consentPolicy.invalid(SyntheticStatePatchFactory.CDP_ID);
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-consent-invalid", "synthetic-patch-consent-invalid");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertRejectedConsent(result, harness);
    }

    @Test
    void invalidSourceIsRejectedBeforeMutation() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.withSource("PATIENT_FACT", 0);

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.SOURCE_NOT_AUTHORIZED, result.reasonCode);
        assertEquals(0, harness.repository.readCalls());
        assertEquals(0, harness.repository.commitCalls());
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void multiOperationValidationFailureDoesNotCallRepositoryCommit() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.multiOperation(0, "/patient_state/unauthorized_field");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("REJECTED", result.status);
        assertEquals(1, result.rejectedOperations.size());
        assertEquals(Integer.valueOf(1), result.rejectedOperations.get(0).operationIndex);
        assertEquals(CommitReasonCodes.FIELD_NOT_AUTHORIZED, result.rejectedOperations.get(0).reasonCode);
        assertEquals(0, harness.repository.readCalls());
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void repositoryAtomicFailureLeavesVersionUnchanged() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 4);
        harness.repository.failNextCommit();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(4, "synthetic-idem-repo-fail", "synthetic-patch-repo-fail");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE, result.reasonCode);
        assertEquals(null, result.committedVersion);
        assertEquals(null, result.committedAt);
        assertTrue(result.errors.size() >= 1);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(4, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
    }

    @Test
    void preCommitAuditFailurePreventsAuthoritativeMutation() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.audit.failNext();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-post-commit-audit",
                "synthetic-patch-post-commit-audit"
        );

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.AUDIT_INFRASTRUCTURE_FAILURE, result.reasonCode);
        assertEquals("synthetic-audit-fallback", result.auditRef.auditId);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        assertEquals(0, harness.idempotency.rememberCalls());
        assertEquals(1, harness.events.events().size());
    }

    @Test
    void postCommitIdempotencyFailureCannotDowngradeAuthoritativeResult() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.idempotency.failNextRemember();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-post-commit-idempotency",
                "synthetic-patch-post-commit-idempotency"
        );

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertAuthoritativeCommitPreserved(result, harness);
        assertEquals(1, harness.idempotency.rememberCalls());
        assertEquals(1, harness.events.events().size());
    }

    @Test
    void postCommitEventFailureCannotDowngradeAuthoritativeResult() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.events.failNext();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-post-commit-event",
                "synthetic-patch-post-commit-event"
        );

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertAuthoritativeCommitPreserved(result, harness);
        assertEquals(1, harness.idempotency.rememberCalls());
        assertEquals(0, harness.events.events().size());
    }

    @Test
    void replayEventFailureCannotDowngradeOriginalAuthoritativeResult() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.StatePatch originalPatch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-replay-event-failure",
                "synthetic-patch-replay-event-original"
        );
        StateTypes.CommitResult original = harness.committer.commit(originalPatch);
        harness.events.failNext();
        StateTypes.StatePatch replayPatch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-replay-event-failure",
                "synthetic-patch-replay-event-retry"
        );

        StateTypes.CommitResult replayed = harness.committer.commit(replayPatch);

        assertEquals("COMMITTED", original.status);
        assertEquals(original.status, replayed.status);
        assertEquals(original.previousVersion, replayed.previousVersion);
        assertEquals(original.committedVersion, replayed.committedVersion);
        assertEquals(original.auditRef.auditId, replayed.auditRef.auditId);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(1, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        assertEquals(1, harness.events.events().size());
        CommitResultSchemaAssertions.assertValid(replayed);
    }

    @Test
    void auditGateWinsBeforeAnyPostCommitFailureProbe() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.audit.failNext();
        harness.idempotency.failNextRemember();
        harness.events.failNext();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-post-commit-all",
                "synthetic-patch-post-commit-all"
        );

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.AUDIT_INFRASTRUCTURE_FAILURE, result.reasonCode);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.idempotency.rememberCalls());
        assertEquals(0, harness.events.events().size());
    }

    @Test
    void auditInfrastructureFailureIsFailedWithoutMutation() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.consentPolicy.deny(SyntheticStatePatchFactory.CDP_ID);
        harness.audit.failNext();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(0, "synthetic-idem-audit-fail", "synthetic-patch-audit-fail");

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.AUDIT_INFRASTRUCTURE_FAILURE, result.reasonCode);
        assertEquals(0, harness.repository.commitCalls());
        assertEquals(0, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
        assertEquals(Boolean.FALSE, result.auditRef.phiCapable);
    }

    @Test
    void committedRejectedConflictAndFailedAllCarryValidAuditRef() {
        StateCommitterTestHarness committedHarness = new StateCommitterTestHarness();
        StateTypes.CommitResult committed = committedHarness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-audit-committed", "synthetic-patch-audit-committed")
        );
        assertEquals("COMMITTED", committed.status);
        assertEquals("STATE_PATCH_REQUESTED", committed.auditRef.auditType);
        CommitResultSchemaAssertions.assertValid(committed);

        StateCommitterTestHarness rejectedHarness = new StateCommitterTestHarness();
        StateTypes.CommitResult rejected = rejectedHarness.committer.commit(
                SyntheticStatePatchFactory.withPath("/patient_state/unauthorized_field", 0)
        );
        assertEquals("REJECTED", rejected.status);
        assertEquals("STATE_PATCH_REQUESTED", rejected.auditRef.auditType);
        CommitResultSchemaAssertions.assertValid(rejected);

        StateCommitterTestHarness conflictHarness = new StateCommitterTestHarness();
        conflictHarness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 3);
        StateTypes.CommitResult conflict = conflictHarness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-audit-conflict", "synthetic-patch-audit-conflict")
        );
        assertEquals("CONFLICT", conflict.status);
        assertEquals("STATE_PATCH_REQUESTED", conflict.auditRef.auditType);
        CommitResultSchemaAssertions.assertValid(conflict);

        StateCommitterTestHarness failedHarness = new StateCommitterTestHarness();
        failedHarness.repository.failNextCommit();
        StateTypes.CommitResult failed = failedHarness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-audit-failed", "synthetic-patch-audit-failed")
        );
        assertEquals("FAILED", failed.status);
        assertEquals("STATE_PATCH_REQUESTED", failed.auditRef.auditType);
        CommitResultSchemaAssertions.assertValid(failed);
    }

    @Test
    void repositoryAtomicConflictAndExceptionLeaveVersionUnchanged() {
        StateCommitterTestHarness conflictHarness = new StateCommitterTestHarness();
        conflictHarness.repository.conflictNextCommit();
        StateTypes.CommitResult conflict = conflictHarness.committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-atomic-conflict", "synthetic-patch-atomic-conflict"));
        assertEquals("CONFLICT", conflict.status);
        assertEquals(0, conflictHarness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        assertEquals(1, conflictHarness.repository.commitCalls());

        StateCommitterTestHarness exceptionHarness = new StateCommitterTestHarness();
        exceptionHarness.repository.throwOnCommit();
        StateTypes.CommitResult failed = exceptionHarness.committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-atomic-exception", "synthetic-patch-atomic-exception"));
        assertEquals("FAILED", failed.status);
        assertEquals(0, exceptionHarness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        assertEquals(1, exceptionHarness.repository.commitCalls());
    }

    @Test
    void malformedCommittedVersionOutcomeIsNotRepresentable() throws Exception {
        java.lang.reflect.Method committed =
                com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort.AtomicCommitOutcome.class
                        .getDeclaredMethod("committed", int.class);
        assertEquals(1,
                ((com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort.AtomicCommitOutcome)
                        committed.invoke(null, Integer.valueOf(5))).committedVersion - 5);
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchMethodException.class, () ->
                com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort.AtomicCommitOutcome.class
                        .getDeclaredMethod("committed", int.class, int.class));
    }

    @Test
    void actualAuditPrecedesCommitAndCommittedCarriesThatReference() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        StateTypes.CommitResult result = harness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-order", "synthetic-patch-order"));

        assertEquals("COMMITTED", result.status);
        assertEquals(java.util.Arrays.asList(
                "idempotency.inspect",
                "capability.evaluate",
                "consent.evaluate",
                "field.evaluate",
                "source.evaluate",
                "repository.read",
                "idempotency.reserve",
                "audit.record",
                "repository.commit",
                "idempotency.complete",
                "event.record"
        ), harness.callOrder);
        assertEquals(harness.audit.lastAuditRef().auditId, result.auditRef.auditId);
        assertNotEquals("synthetic-audit-fallback", result.auditRef.auditId);
    }

    @Test
    void nullAndMalformedAuditReferencesPreventCommit() {
        StateCommitterTestHarness nullHarness = new StateCommitterTestHarness();
        nullHarness.audit.returnNullNext();
        StateTypes.CommitResult nullResult = nullHarness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-null-audit", "synthetic-patch-null-audit"));
        assertEquals("FAILED", nullResult.status);
        assertEquals(0, nullHarness.repository.commitCalls());

        StateCommitterTestHarness malformedHarness = new StateCommitterTestHarness();
        malformedHarness.audit.returnMalformedNext();
        StateTypes.CommitResult malformedResult = malformedHarness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-bad-audit", "synthetic-patch-bad-audit"));
        assertEquals("FAILED", malformedResult.status);
        assertEquals(0, malformedHarness.repository.commitCalls());
    }

    @Test
    void testAndUnknownOperationsAreRejectedWithoutCommit() {
        StateCommitterTestHarness testHarness = new StateCommitterTestHarness();
        StateTypes.CommitResult testResult = testHarness.committer.commit(
                SyntheticStatePatchFactory.withOperation("TEST", 0));
        assertEquals("REJECTED", testResult.status);
        assertEquals(CommitReasonCodes.OPERATION_NOT_AUTHORIZED, testResult.reasonCode);
        assertEquals(0, testHarness.repository.commitCalls());

        StateCommitterTestHarness unknownHarness = new StateCommitterTestHarness();
        StateTypes.CommitResult unknownResult = unknownHarness.committer.commit(
                SyntheticStatePatchFactory.withOperation("MOVE", 0));
        assertEquals("REJECTED", unknownResult.status);
        assertEquals(0, unknownHarness.repository.commitCalls());
    }

    @Test
    void reservationFailurePreventsCommit() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.idempotency.failNextReserve();

        StateTypes.CommitResult result = harness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-reserve-fail", "synthetic-patch-reserve-fail"));

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.IDEMPOTENCY_INFRASTRUCTURE_FAILURE, result.reasonCode);
        assertEquals(Boolean.TRUE, result.retryable);
        assertEquals(0, harness.repository.commitCalls());
    }

    @Test
    void completionFailureLeavesReservationAndRetryCannotDoubleCommit() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.idempotency.failNextComplete();
        StateTypes.CommitResult original = harness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-incomplete", "synthetic-patch-incomplete-a"));

        StateTypes.CommitResult retry = harness.committer.commit(
                SyntheticStatePatchFactory.valid(0, "synthetic-idem-incomplete", "synthetic-patch-incomplete-b"));

        assertEquals("COMMITTED", original.status);
        assertEquals("FAILED", retry.status);
        assertEquals(CommitReasonCodes.IDEMPOTENCY_RESULT_UNAVAILABLE, retry.reasonCode);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(1, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
    }

    @Test
    void changedBaseAfterCompletionFailureStillCannotDoubleCommit() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.idempotency.failNextComplete();
        harness.committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-incomplete-base", "synthetic-patch-incomplete-base-a"));

        StateTypes.CommitResult retry = harness.committer.commit(SyntheticStatePatchFactory.valid(
                1, "synthetic-idem-incomplete-base", "synthetic-patch-incomplete-base-b"));

        assertEquals("CONFLICT", retry.status);
        assertEquals(CommitReasonCodes.IDEMPOTENCY_MISMATCH, retry.reasonCode);
        assertEquals(1, harness.repository.commitCalls());
    }

    @Test
    void simulatedDoubleReservationHasOneWinner() {
        com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake fake =
                new com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake();
        IdempotencyPort.Decision first = fake.reserve("synthetic-idem-race", "fingerprint");
        IdempotencyPort.Decision second = fake.reserve("synthetic-idem-race", "fingerprint");

        assertEquals(IdempotencyPort.Decision.Status.ACQUIRED, first.status);
        assertEquals(IdempotencyPort.Decision.Status.RESERVED_SAME_FINGERPRINT, second.status);
    }

    @Test
    void eventFailureCannotAlterRejectedConflictOrFailedOutcomes() {
        StateCommitterTestHarness rejectedHarness = new StateCommitterTestHarness();
        rejectedHarness.events.failNext();
        assertEquals("REJECTED", rejectedHarness.committer.commit(
                SyntheticStatePatchFactory.withPath("/patient_state/unauthorized_field", 0)).status);

        StateCommitterTestHarness conflictHarness = new StateCommitterTestHarness();
        conflictHarness.repository.seed(SyntheticStatePatchFactory.CDP_ID, 2);
        conflictHarness.events.failNext();
        assertEquals("CONFLICT", conflictHarness.committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-event-conflict", "synthetic-patch-event-conflict")).status);

        StateCommitterTestHarness failedHarness = new StateCommitterTestHarness();
        failedHarness.repository.failNextCommit();
        failedHarness.events.failNext();
        assertEquals("FAILED", failedHarness.committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-event-failed", "synthetic-patch-event-failed")).status);
    }

    @Test
    void fixedDependenciesProduceDeterministicCommitResultIdentity() {
        StateTypes.StatePatch firstPatch = SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-deterministic", "synthetic-patch-deterministic");
        StateTypes.StatePatch secondPatch = SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-deterministic", "synthetic-patch-deterministic");
        StateTypes.CommitResult first = new StateCommitterTestHarness().committer.commit(firstPatch);
        StateTypes.CommitResult second = new StateCommitterTestHarness().committer.commit(secondPatch);

        assertEquals(first.envelope.messageId, second.envelope.messageId);
        assertEquals(first.auditRef.auditId, second.auditRef.auditId);
        assertEquals(first.status, second.status);
        assertEquals(first.committedVersion, second.committedVersion);
    }

    @Test
    void everyPolicyPortExceptionFailsClosed() {
        StateCommitterTestHarness capabilityHarness = new StateCommitterTestHarness();
        CapabilityPolicyPort badCapability = new CapabilityPolicyPort() {
            @Override public CapabilityDecision evaluate(String id, String version) {
                throw new IllegalStateException("capability failure");
            }
        };
        assertPolicyFailure(new StateCommitter(
                capabilityHarness.repository, badCapability, capabilityHarness.fieldPermission,
                capabilityHarness.consentPolicy, capabilityHarness.sourceValidation,
                capabilityHarness.idempotency, capabilityHarness.audit, capabilityHarness.events,
                StateCommitterTestHarness.CLOCK), capabilityHarness, "capability");

        StateCommitterTestHarness consentHarness = new StateCommitterTestHarness();
        ConsentPolicyPort badConsent = new ConsentPolicyPort() {
            @Override public ConsentDecision evaluate(String cdpId, String capabilityId) {
                throw new IllegalStateException("consent failure");
            }
        };
        assertPolicyFailure(new StateCommitter(
                consentHarness.repository, consentHarness.capabilityPolicy, consentHarness.fieldPermission,
                badConsent, consentHarness.sourceValidation, consentHarness.idempotency,
                consentHarness.audit, consentHarness.events, StateCommitterTestHarness.CLOCK),
                consentHarness, "consent");

        StateCommitterTestHarness fieldHarness = new StateCommitterTestHarness();
        FieldPermissionPort badField = new FieldPermissionPort() {
            @Override public FieldPermissionDecision evaluate(String path, String capabilityId) {
                throw new IllegalStateException("field failure");
            }
        };
        assertPolicyFailure(new StateCommitter(
                fieldHarness.repository, fieldHarness.capabilityPolicy, badField,
                fieldHarness.consentPolicy, fieldHarness.sourceValidation, fieldHarness.idempotency,
                fieldHarness.audit, fieldHarness.events, StateCommitterTestHarness.CLOCK),
                fieldHarness, "field");

        StateCommitterTestHarness sourceHarness = new StateCommitterTestHarness();
        SourceValidationPort badSource = new SourceValidationPort() {
            @Override public SourceDecision evaluate(String source) {
                throw new IllegalStateException("source failure");
            }
        };
        assertPolicyFailure(new StateCommitter(
                sourceHarness.repository, sourceHarness.capabilityPolicy, sourceHarness.fieldPermission,
                sourceHarness.consentPolicy, badSource, sourceHarness.idempotency,
                sourceHarness.audit, sourceHarness.events, StateCommitterTestHarness.CLOCK),
                sourceHarness, "source");
    }

    @Test
    void authorizedPathIsTheOnlySyntheticFieldUsed() {
        assertEquals("/patient_state/synthetic_test_value", SyntheticFieldPermissionFake.AUTHORIZED_PATH);
        assertNotEquals("/patient_state/chief_complaint", SyntheticFieldPermissionFake.AUTHORIZED_PATH);
    }

    private static void assertAuthoritativeCommitPreserved(
            StateTypes.CommitResult result,
            StateCommitterTestHarness harness
    ) {
        assertEquals("COMMITTED", result.status);
        assertEquals(CommitReasonCodes.PATCH_COMMITTED, result.reasonCode);
        assertEquals(Integer.valueOf(0), result.previousVersion);
        assertEquals(Integer.valueOf(1), result.committedVersion);
        assertEquals(1, harness.repository.commitCalls());
        assertEquals(1, harness.repository.currentVersion(SyntheticStatePatchFactory.CDP_ID));
        CommitResultSchemaAssertions.assertValid(result);
    }

    private static void assertRejectedConsent(StateTypes.CommitResult result, StateCommitterTestHarness harness) {
        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.CONSENT_NOT_AUTHORIZED, result.reasonCode);
        assertEquals(Boolean.FALSE, result.retryable);
        assertEquals(0, result.conflicts.size());
        assertEquals(0, result.errors.size());
        assertEquals(1, result.rejectedOperations.size());
        assertEquals(CommitReasonCodes.CONSENT_NOT_AUTHORIZED, result.rejectedOperations.get(0).reasonCode);
        assertEquals(0, harness.repository.readCalls());
        assertEquals(0, harness.repository.commitCalls());
        CommitResultSchemaAssertions.assertValid(result);
        assertEquals("STATE_PATCH_REQUESTED", result.auditRef.auditType);
    }

    private static void assertPolicyFailure(
            StateCommitter committer,
            StateCommitterTestHarness harness,
            String suffix
    ) {
        StateTypes.CommitResult result = committer.commit(SyntheticStatePatchFactory.valid(
                0, "synthetic-idem-policy-" + suffix, "synthetic-patch-policy-" + suffix));
        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.INVARIANT_FAILURE, result.reasonCode);
        assertEquals(Boolean.FALSE, result.retryable);
        assertEquals(0, harness.repository.commitCalls());
    }
}
