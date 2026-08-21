package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticFieldPermissionFake;
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
        assertEquals("STATE_COMMITTED", result.auditRef.auditType);
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
    void postCommitAuditFailureCannotDowngradeAuthoritativeResult() {
        StateCommitterTestHarness harness = new StateCommitterTestHarness();
        harness.audit.failNext();
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.valid(
                0,
                "synthetic-idem-post-commit-audit",
                "synthetic-patch-post-commit-audit"
        );

        StateTypes.CommitResult result = harness.committer.commit(patch);

        assertAuthoritativeCommitPreserved(result, harness);
        assertEquals("STATE_COMMITTED", result.auditRef.auditType);
        assertEquals("synthetic-audit-fallback", result.auditRef.auditId);
        assertEquals(1, harness.idempotency.rememberCalls());
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
    void allPostCommitFailuresStillReturnCommitted() {
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

        assertAuthoritativeCommitPreserved(result, harness);
        assertEquals("STATE_COMMITTED", result.auditRef.auditType);
        assertEquals(1, harness.idempotency.rememberCalls());
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
        assertEquals("STATE_COMMITTED", committed.auditRef.auditType);
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
}
