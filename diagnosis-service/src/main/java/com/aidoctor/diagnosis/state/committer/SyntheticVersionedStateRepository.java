package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dormant PBNC-02 in-memory synthetic StateRepositoryPort implementation.
 *
 * <p>No Spring wiring, persistence, network, provider, CDP, or production
 * reachability is introduced here.
 */
public final class SyntheticVersionedStateRepository implements StateRepositoryPort {
    private final Map<String, Record> records = new LinkedHashMap<String, Record>();
    private final SyntheticJsonPointerApplier applier = new SyntheticJsonPointerApplier();
    private int mutationCount;

    public SyntheticVersionedStateRepository() {
    }

    public SyntheticVersionedStateRepository(Map<String, SyntheticStateSnapshot> initialStates) {
        if (initialStates == null) {
            return;
        }
        for (Map.Entry<String, SyntheticStateSnapshot> entry : initialStates.entrySet()) {
            SyntheticStateSnapshot snapshot = entry.getValue();
            records.put(entry.getKey(), new Record(snapshot.version(), snapshot.state()));
        }
    }

    @Override
    public synchronized int readCurrentVersion(String cdpId) {
        return record(cdpId).version;
    }

    @Override
    public synchronized AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command) {
        AtomicCommitOutcome invalid = validateCommand(command);
        if (invalid != null) {
            return invalid;
        }

        Record current = record(command.cdpId);
        if (current.version != command.expectedCurrentVersion) {
            return AtomicCommitOutcome.conflict(command.expectedCurrentVersion, current.version);
        }

        Map<String, Object> working = SyntheticStateSnapshot.copyMap(current.state);
        try {
            for (StateTypes.StatePatchOperation operation : command.patch.operations) {
                applier.apply(operation, working);
            }
            AtomicCommitOutcome outcome = AtomicCommitOutcome.committed(current.version);
            records.put(command.cdpId, new Record(outcome.committedVersion, working));
            mutationCount++;
            return outcome;
        } catch (SyntheticJsonPointerApplier.SyntheticApplicationException exception) {
            return AtomicCommitOutcome.failedNonRetryable(
                    CommitReasonCodes.STATE_OPERATION_APPLICATION_FAILED,
                    exception.getMessage());
        } catch (ArithmeticException exception) {
            return AtomicCommitOutcome.failedRetryable(
                    CommitReasonCodes.REPOSITORY_INTERNAL_FAILURE,
                    "Synthetic repository version transition overflow.");
        }
    }

    public synchronized SyntheticStateSnapshot snapshot(String cdpId) {
        Record record = record(cdpId);
        return new SyntheticStateSnapshot(record.version, record.state);
    }

    public synchronized int mutationCount() {
        return mutationCount;
    }

    private AtomicCommitOutcome validateCommand(AtomicCommitCommand command) {
        if (command == null || command.patch == null) {
            return invalidCommand("State repository command requires StatePatch.");
        }
        StateTypes.StatePatch patch = command.patch;
        if (!same(command.cdpId, patch.cdpId)
                || patch.baseVersion == null
                || command.expectedCurrentVersion != patch.baseVersion.intValue()
                || !same(command.patchId, patch.patchId)
                || !same(command.idempotencyKey, patch.idempotencyKey)) {
            return invalidCommand("State repository command metadata does not match StatePatch.");
        }
        return null;
    }

    private static AtomicCommitOutcome invalidCommand(String message) {
        return AtomicCommitOutcome.failedNonRetryable(
                CommitReasonCodes.STATE_REPOSITORY_COMMAND_INVALID,
                message);
    }

    private Record record(String cdpId) {
        Record record = records.get(cdpId);
        if (record == null) {
            return new Record(0, new LinkedHashMap<String, Object>());
        }
        return record;
    }

    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static final class Record {
        final int version;
        final Map<String, Object> state;

        Record(int version, Map<String, Object> state) {
            this.version = version;
            this.state = SyntheticStateSnapshot.copyMap(state);
        }
    }
}
