package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticCapabilityPolicyFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticConsentPolicyFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticFieldPermissionFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticSourceValidationFake;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import com.aidoctor.diagnosis.state.committer.support.CommitResultSchemaAssertions;
import com.aidoctor.diagnosis.state.committer.support.StateCommitterTestHarness;
import com.aidoctor.diagnosis.state.committer.support.SyntheticStatePatchFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyntheticVersionedStateRepositoryTest {

    @Test
    void initialVersionIsDeterministicZero() {
        SyntheticVersionedStateRepository repository = new SyntheticVersionedStateRepository();

        assertEquals(0, repository.readCurrentVersion("test.subject.001"));
        assertEquals(0, repository.snapshot("test.subject.001").version());
    }

    @Test
    void initialSnapshotIsDeterministic() {
        SyntheticVersionedStateRepository repository = new SyntheticVersionedStateRepository();

        assertEquals(Collections.emptyMap(), repository.snapshot("test.subject.001").state());
    }

    @Test
    void negativeSnapshotVersionIsRejected() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() {
                new SyntheticStateSnapshot(-1, Collections.<String, Object>emptyMap());
            }
        });
    }

    @Test
    void zeroSnapshotVersionIsAccepted() {
        SyntheticStateSnapshot snapshot = new SyntheticStateSnapshot(0, Collections.<String, Object>emptyMap());

        assertEquals(0, snapshot.version());
    }

    @Test
    void integerMaxSnapshotVersionIsAccepted() {
        SyntheticStateSnapshot snapshot = new SyntheticStateSnapshot(
                Integer.MAX_VALUE, Collections.<String, Object>emptyMap());

        assertEquals(Integer.MAX_VALUE, snapshot.version());
    }

    @Test
    void addMutatesWorkingStateAndCommitsOnce() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, Collections.<String, Object>emptyMap());
        StateTypes.StatePatch patch = patch("ADD", "/patient_state/test_value", "alpha", 0, "synthetic-idem-add", "synthetic-patch-add");

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals(1, repository.mutationCount());
        assertEquals("alpha", patientState(repository).get("test_value"));
        assertEquals(1, repository.snapshot(SyntheticStatePatchFactory.CDP_ID).version());
    }

    @Test
    void replaceExistingValueCommitsOnce() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0, "synthetic-idem-replace", "synthetic-patch-replace");

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals(1, repository.mutationCount());
        assertEquals("beta", patientState(repository).get("test_value"));
    }

    @Test
    void removeExistingValueCommitsOnce() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REMOVE", "/patient_state/test_value", null, 0, "synthetic-idem-remove", "synthetic-patch-remove");

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals(1, repository.mutationCount());
        assertFalse(patientState(repository).containsKey("test_value"));
    }

    @Test
    void addExistingTargetFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("ADD", "/patient_state/test_value", "beta", 0, "synthetic-idem-add-existing", "synthetic-patch-add-existing")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void replaceMissingTargetFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, Collections.<String, Object>emptyMap());
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("REPLACE", "/patient_state/test_value", "beta", 0, "synthetic-idem-replace-missing", "synthetic-patch-replace-missing")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void removeMissingTargetFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, Collections.<String, Object>emptyMap());
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("REMOVE", "/patient_state/test_value", null, 0, "synthetic-idem-remove-missing", "synthetic-patch-remove-missing")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void missingParentFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = new SyntheticVersionedStateRepository();
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("ADD", "/patient_state/test_value", "alpha", 0, "synthetic-idem-missing-parent", "synthetic-patch-missing-parent")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void multiOperationPatchCommitsAtomically() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0, "synthetic-idem-multi-ok", "synthetic-patch-multi-ok");
        patch.operations.add(operation("ADD", "/patient_state/test_flag", Boolean.TRUE));

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals("beta", patientState(repository).get("test_value"));
        assertEquals(Boolean.TRUE, patientState(repository).get("test_flag"));
        assertEquals(1, repository.mutationCount());
    }

    @Test
    void laterOperationFailureDiscardsEarlierWorkingCopyChanges() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0, "synthetic-idem-multi-fail", "synthetic-patch-multi-fail");
        patch.operations.add(operation("REMOVE", "/patient_state/missing_value", null));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void staleBaseVersionConflictsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(2, singleton("test_value", "alpha"));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("REPLACE", "/patient_state/test_value", "beta", 0, "synthetic-idem-stale", "synthetic-patch-stale")));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT, outcome.status);
        assertUnchanged(before, repository);
    }

    @Test
    void nearIntegerMaxCommitsExactlyOnce() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(Integer.MAX_VALUE - 1, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", Integer.MAX_VALUE - 1,
                "synthetic-idem-near-max-state", "synthetic-patch-near-max-state");

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals(Integer.MAX_VALUE, outcome.committedVersion);
        assertEquals(1, repository.mutationCount());
        assertEquals("beta", patientState(repository).get("test_value"));
    }

    @Test
    void integerMaxFailsBeforeStateMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(Integer.MAX_VALUE, singleton("test_value", "alpha"));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                command(patch("REPLACE", "/patient_state/test_value", "beta", Integer.MAX_VALUE,
                        "synthetic-idem-max-state", "synthetic-patch-max-state")));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, outcome.status);
        assertEquals(CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE, outcome.failureCode);
        assertEquals(true, outcome.retryable);
        assertUnchanged(before, repository);
    }

    @Test
    void returnedSnapshotCannotMutateRepository() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        Map<String, Object> callerState = repository.snapshot(SyntheticStatePatchFactory.CDP_ID).state();
        patientState(callerState).put("test_value", "beta");

        assertEquals("alpha", patientState(repository).get("test_value"));
    }

    @Test
    void flatArrayLeafIsDeepCopied() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_values", Arrays.<Object>asList("alpha", "beta")));
        Map<String, Object> callerState = repository.snapshot(SyntheticStatePatchFactory.CDP_ID).state();
        listAt(patientState(callerState), "test_values").add("gamma");

        assertEquals(Arrays.<Object>asList("alpha", "beta"), listAt(patientState(repository), "test_values"));
    }

    @Test
    void jsonPointerEscapesAreDecodedDeterministically() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("test/value~one", "alpha");
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, nested);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(
                patch("REPLACE", "/patient_state/test~1value~0one", "beta", 0,
                        "synthetic-idem-escaped", "synthetic-patch-escaped")));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED, outcome.status);
        assertEquals("beta", patientState(repository).get("test/value~one"));
    }

    @Test
    void arrayIndexPathsFailDeterministicallyWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_values", Arrays.<Object>asList("alpha", "beta")));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(
                patch("REPLACE", "/patient_state/test_values/0", "gamma", 0,
                        "synthetic-idem-array-path", "synthetic-patch-array-path")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void commandMetadataMismatchFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-command", "synthetic-patch-command");
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(
                new StateRepositoryPort.AtomicCommitCommand(
                        patch.cdpId, 9, patch.patchId, patch.idempotencyKey, patch));

        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, outcome.status);
        assertEquals(CommitReasonCodes.STATE_REPOSITORY_COMMAND_INVALID, outcome.failureCode);
        assertEquals(false, outcome.retryable);
        assertUnchanged(before, repository);
    }

    @Test
    void nonNullExpectedCurrentValueFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-expected", "synthetic-patch-expected");
        patch.operations.get(0).expectedCurrentValue = "alpha";
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(patch));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void topLevelNullValueFailsWithoutMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateRepositoryPort.AtomicCommitOutcome outcome = repository.attemptAtomicCommit(command(
                patch("REPLACE", "/patient_state/test_value", null, 0,
                        "synthetic-idem-null", "synthetic-patch-null")));

        assertNonRetryableApplicationFailure(outcome);
        assertUnchanged(before, repository);
    }

    @Test
    void testOnlyBackendFailedOutcomeLeavesStateUnchanged() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        TestOnlyFailingRepository failing = TestOnlyFailingRepository.returnFailed(repository);
        StateCommitter committer = committer(failing, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateTypes.CommitResult result = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-infra-fail", "synthetic-patch-infra-fail"));

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE, result.reasonCode);
        assertEquals(Boolean.TRUE, result.retryable);
        assertEquals(0, repository.mutationCount());
        assertUnchanged(before, repository);
    }

    @Test
    void testOnlyBackendExceptionLeavesStateUnchanged() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        TestOnlyFailingRepository throwing = TestOnlyFailingRepository.throwing(repository);
        StateCommitter committer = committer(throwing, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateTypes.CommitResult result = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-infra-throw", "synthetic-patch-infra-throw"));

        assertEquals("FAILED", result.status);
        assertEquals(CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE, result.reasonCode);
        assertEquals(Boolean.TRUE, result.retryable);
        assertEquals(0, repository.mutationCount());
        assertUnchanged(before, repository);
    }

    @Test
    void directConcurrentSameBaseRaceHasOneWinnerAndOneConflict() throws Exception {
        final SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<StateRepositoryPort.AtomicCommitOutcome> first = new AtomicReference<StateRepositoryPort.AtomicCommitOutcome>();
        final AtomicReference<StateRepositoryPort.AtomicCommitOutcome> second = new AtomicReference<StateRepositoryPort.AtomicCommitOutcome>();
        Thread left = racer(repository, patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-race-left", "synthetic-patch-race-left"), ready, start, first);
        Thread right = racer(repository, patch("REPLACE", "/patient_state/test_value", "gamma", 0,
                "synthetic-idem-race-right", "synthetic-patch-race-right"), ready, start, second);

        left.start();
        right.start();
        ready.await();
        start.countDown();
        left.join();
        right.join();

        List<StateRepositoryPort.AtomicCommitOutcome.Status> statuses = Arrays.asList(first.get().status, second.get().status);
        assertEquals(1, count(statuses, StateRepositoryPort.AtomicCommitOutcome.Status.COMMITTED));
        assertEquals(1, count(statuses, StateRepositoryPort.AtomicCommitOutcome.Status.CONFLICT));
        assertEquals(1, repository.mutationCount());
        assertEquals(1, repository.snapshot(SyntheticStatePatchFactory.CDP_ID).version());
        Object finalValue = patientState(repository).get("test_value");
        assertEquals(true, "beta".equals(finalValue) || "gamma".equals(finalValue));
    }

    @Test
    void stateCommitterAddReplaceAndRemoveMutateSyntheticSnapshot() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, Collections.<String, Object>emptyMap());
        StateCommitter committer = committer(repository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());

        StateTypes.CommitResult add = committer.commit(patch("ADD", "/patient_state/test_value", "alpha", 0,
                "synthetic-idem-commit-add", "synthetic-patch-commit-add"));
        StateTypes.CommitResult replace = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 1,
                "synthetic-idem-commit-replace", "synthetic-patch-commit-replace"));
        StateTypes.CommitResult remove = committer.commit(patch("REMOVE", "/patient_state/test_value", null, 2,
                "synthetic-idem-commit-remove", "synthetic-patch-commit-remove"));

        assertCommitted(add, 0, 1);
        assertCommitted(replace, 1, 2);
        assertCommitted(remove, 2, 3);
        assertFalse(patientState(repository).containsKey("test_value"));
        assertEquals(3, repository.snapshot(SyntheticStatePatchFactory.CDP_ID).version());
    }

    @Test
    void stateCommitterUnsupportedSemanticsDoNotMutateRepository() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateCommitter committer = committer(repository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());
        StateTypes.StatePatch patch = patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-commit-expected", "synthetic-patch-commit-expected");
        patch.operations.get(0).expectedCurrentValue = "alpha";
        SyntheticStateSnapshot before = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);

        StateTypes.CommitResult result = committer.commit(patch);

        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.STATE_OPERATION_SEMANTICS_UNSUPPORTED, result.reasonCode);
        assertEquals(Boolean.FALSE, result.retryable);
        assertEquals(0, repository.mutationCount());
        assertUnchanged(before, repository);
        CommitResultSchemaAssertions.assertValid(result);
    }

    @Test
    void stateCommitterTestOperationRemainsRejectedBeforeRepositoryMutation() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateCommitter committer = committer(repository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());

        StateTypes.CommitResult result = committer.commit(patch("TEST", "/patient_state/test_value", "alpha", 0,
                "synthetic-idem-test-op", "synthetic-patch-test-op"));

        assertEquals("REJECTED", result.status);
        assertEquals(CommitReasonCodes.OPERATION_NOT_AUTHORIZED, result.reasonCode);
        assertEquals(0, repository.mutationCount());
    }

    @Test
    void stateCommitterIdempotencyReplayDoesNotSecondMutateState() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateCommitter committer = committer(repository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());

        StateTypes.CommitResult first = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-state-replay", "synthetic-patch-state-replay-a"));
        StateTypes.CommitResult replay = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-state-replay", "synthetic-patch-state-replay-b"));

        assertEquals("COMMITTED", first.status);
        assertEquals("COMMITTED", replay.status);
        assertEquals(1, repository.mutationCount());
        assertEquals("beta", patientState(repository).get("test_value"));
    }

    @Test
    void stateCommitterCompletionFailureCannotSecondMutateState() {
        SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        InMemoryIdempotencyFake idempotency = new InMemoryIdempotencyFake();
        idempotency.failNextComplete();
        StateCommitter committer = committer(repository, idempotency, new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());

        StateTypes.CommitResult first = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-state-incomplete", "synthetic-patch-state-incomplete-a"));
        StateTypes.CommitResult retry = committer.commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-state-incomplete", "synthetic-patch-state-incomplete-b"));

        assertEquals("COMMITTED", first.status);
        assertEquals("FAILED", retry.status);
        assertEquals(1, repository.mutationCount());
        assertEquals("beta", patientState(repository).get("test_value"));
    }

    @Test
    void stateCommitterAuditReservationPolicyConsentAndSourceFailuresDoNotMutateState() {
        SyntheticVersionedStateRepository auditRepository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        SyntheticAuditPortFake audit = new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>());
        audit.failNext();
        StateTypes.CommitResult auditResult = committer(auditRepository, new InMemoryIdempotencyFake(), audit, new RecordingCommitEventEvidenceFake())
                .commit(patch("REPLACE", "/patient_state/test_value", "beta", 0,
                        "synthetic-idem-audit-state", "synthetic-patch-audit-state"));
        assertEquals("FAILED", auditResult.status);
        assertEquals(0, auditRepository.mutationCount());

        SyntheticVersionedStateRepository sourceRepository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        StateTypes.StatePatch badSource = patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-source-state", "synthetic-patch-source-state");
        badSource.operations.get(0).source = "PATIENT_FACT";
        StateTypes.CommitResult sourceResult = committer(sourceRepository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake()).commit(badSource);
        assertEquals("REJECTED", sourceResult.status);
        assertEquals(0, sourceRepository.mutationCount());
    }

    @Test
    void stateCommitterSameBaseRaceHasOneCommittedAndOneConflict() throws Exception {
        final SyntheticVersionedStateRepository repository = repositoryWithPatientState(0, singleton("test_value", "alpha"));
        final StateCommitter committer = committer(repository, new InMemoryIdempotencyFake(), new SyntheticAuditPortFake(StateCommitterTestHarness.CLOCK, new ArrayList<String>()),
                new RecordingCommitEventEvidenceFake());
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<StateTypes.CommitResult> first = new AtomicReference<StateTypes.CommitResult>();
        final AtomicReference<StateTypes.CommitResult> second = new AtomicReference<StateTypes.CommitResult>();
        Thread left = committerRacer(committer, patch("REPLACE", "/patient_state/test_value", "beta", 0,
                "synthetic-idem-commit-race-left", "synthetic-patch-commit-race-left"), ready, start, first);
        Thread right = committerRacer(committer, patch("REPLACE", "/patient_state/test_value", "gamma", 0,
                "synthetic-idem-commit-race-right", "synthetic-patch-commit-race-right"), ready, start, second);

        left.start();
        right.start();
        ready.await();
        start.countDown();
        left.join();
        right.join();

        List<String> statuses = Arrays.asList(first.get().status, second.get().status);
        assertEquals(1, countStrings(statuses, "COMMITTED"));
        assertEquals(1, countStrings(statuses, "CONFLICT"));
        assertEquals(1, repository.mutationCount());
        assertEquals(1, repository.snapshot(SyntheticStatePatchFactory.CDP_ID).version());
    }

    private static StateCommitter committer(
            StateRepositoryPort repository,
            InMemoryIdempotencyFake idempotency,
            SyntheticAuditPortFake audit,
            RecordingCommitEventEvidenceFake events
    ) {
        SyntheticFieldPermissionFake fieldPermission = new SyntheticFieldPermissionFake();
        fieldPermission.authorize("/patient_state/test_value");
        fieldPermission.authorize("/patient_state/test_flag");
        fieldPermission.authorize("/patient_state/test_values");
        return new StateCommitter(
                repository,
                new SyntheticCapabilityPolicyFake(),
                fieldPermission,
                new SyntheticConsentPolicyFake(),
                new SyntheticSourceValidationFake(),
                idempotency,
                audit,
                events,
                StateCommitterTestHarness.CLOCK);
    }

    private static final class TestOnlyFailingRepository implements StateRepositoryPort {
        private enum Mode {
            RETURN_FAILED,
            THROW
        }

        private final SyntheticVersionedStateRepository delegate;
        private final Mode mode;

        private TestOnlyFailingRepository(SyntheticVersionedStateRepository delegate, Mode mode) {
            this.delegate = delegate;
            this.mode = mode;
        }

        static TestOnlyFailingRepository returnFailed(SyntheticVersionedStateRepository delegate) {
            return new TestOnlyFailingRepository(delegate, Mode.RETURN_FAILED);
        }

        static TestOnlyFailingRepository throwing(SyntheticVersionedStateRepository delegate) {
            return new TestOnlyFailingRepository(delegate, Mode.THROW);
        }

        @Override
        public int readCurrentVersion(String cdpId) {
            return delegate.readCurrentVersion(cdpId);
        }

        @Override
        public AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command) {
            if (mode == Mode.THROW) {
                throw new IllegalStateException("test-only repository infrastructure exception");
            }
            return AtomicCommitOutcome.failedRetryable(
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Test-only repository infrastructure failure.");
        }
    }

    private static Thread racer(
            final SyntheticVersionedStateRepository repository,
            final StateTypes.StatePatch patch,
            final CountDownLatch ready,
            final CountDownLatch start,
            final AtomicReference<StateRepositoryPort.AtomicCommitOutcome> result
    ) {
        return new Thread(new Runnable() {
            @Override public void run() {
                try {
                    ready.countDown();
                    start.await();
                    result.set(repository.attemptAtomicCommit(command(patch)));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
        });
    }

    private static Thread committerRacer(
            final StateCommitter committer,
            final StateTypes.StatePatch patch,
            final CountDownLatch ready,
            final CountDownLatch start,
            final AtomicReference<StateTypes.CommitResult> result
    ) {
        return new Thread(new Runnable() {
            @Override public void run() {
                try {
                    ready.countDown();
                    start.await();
                    result.set(committer.commit(patch));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
        });
    }

    private static StateRepositoryPort.AtomicCommitCommand command(StateTypes.StatePatch patch) {
        return new StateRepositoryPort.AtomicCommitCommand(
                patch.cdpId, patch.baseVersion.intValue(), patch.patchId, patch.idempotencyKey, patch);
    }

    private static StateTypes.StatePatch patch(
            String op,
            String path,
            Object value,
            int baseVersion,
            String idempotencyKey,
            String patchId
    ) {
        StateTypes.StatePatch patch = SyntheticStatePatchFactory.patch(
                SyntheticStatePatchFactory.CDP_ID,
                baseVersion,
                idempotencyKey,
                patchId,
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                new ArrayList<StateTypes.StatePatchOperation>());
        patch.operations.add(operation(op, path, value));
        return patch;
    }

    private static StateTypes.StatePatchOperation operation(String op, String path, Object value) {
        StateTypes.StatePatchOperation operation = SyntheticStatePatchFactory.operation(path, "TOOL_OUTPUT");
        operation.op = op;
        operation.value = value;
        return operation;
    }

    private static SyntheticVersionedStateRepository repositoryWithPatientState(int version, Map<String, Object> patientState) {
        Map<String, Object> state = new LinkedHashMap<String, Object>();
        state.put("patient_state", patientState);
        Map<String, SyntheticStateSnapshot> snapshots = new LinkedHashMap<String, SyntheticStateSnapshot>();
        snapshots.put(SyntheticStatePatchFactory.CDP_ID, new SyntheticStateSnapshot(version, state));
        return new SyntheticVersionedStateRepository(snapshots);
    }

    private static Map<String, Object> singleton(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> patientState(SyntheticVersionedStateRepository repository) {
        return patientState(repository.snapshot(SyntheticStatePatchFactory.CDP_ID).state());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> patientState(Map<String, Object> state) {
        return (Map<String, Object>) state.get("patient_state");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listAt(Map<String, Object> state, String key) {
        return (List<Object>) state.get(key);
    }

    private static void assertNonRetryableApplicationFailure(StateRepositoryPort.AtomicCommitOutcome outcome) {
        assertEquals(StateRepositoryPort.AtomicCommitOutcome.Status.FAILED, outcome.status);
        assertEquals(CommitReasonCodes.STATE_OPERATION_APPLICATION_FAILED, outcome.failureCode);
        assertEquals(false, outcome.retryable);
    }

    private static void assertUnchanged(SyntheticStateSnapshot before, SyntheticVersionedStateRepository repository) {
        SyntheticStateSnapshot after = repository.snapshot(SyntheticStatePatchFactory.CDP_ID);
        assertEquals(before.version(), after.version());
        assertEquals(before.state(), after.state());
        assertEquals(0, repository.mutationCount());
    }

    private static void assertCommitted(StateTypes.CommitResult result, int previous, int committed) {
        assertEquals("COMMITTED", result.status);
        assertEquals(Integer.valueOf(previous), result.previousVersion);
        assertEquals(Integer.valueOf(committed), result.committedVersion);
        CommitResultSchemaAssertions.assertValid(result);
    }

    private static int count(List<StateRepositoryPort.AtomicCommitOutcome.Status> statuses, StateRepositoryPort.AtomicCommitOutcome.Status status) {
        int total = 0;
        for (StateRepositoryPort.AtomicCommitOutcome.Status item : statuses) {
            if (item == status) {
                total++;
            }
        }
        return total;
    }

    private static int countStrings(List<String> statuses, String status) {
        int total = 0;
        for (String item : statuses) {
            if (status.equals(item)) {
                total++;
            }
        }
        return total;
    }
}
