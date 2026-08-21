package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal version-counter fake. Not a PBNC-02 synthetic state store.
 */
public final class MechanicalVersionRepositoryFake implements StateRepositoryPort {
    private final Map<String, Integer> versions = new HashMap<String, Integer>();
    private int readCalls;
    private int commitCalls;
    private boolean failNextRead;
    private boolean failNextCommit;
    private boolean throwOnCommit;

    public void seed(String cdpId, int version) {
        versions.put(cdpId, Integer.valueOf(version));
    }

    public int currentVersion(String cdpId) {
        Integer version = versions.get(cdpId);
        return version == null ? 0 : version.intValue();
    }

    public int readCalls() {
        return readCalls;
    }

    public int commitCalls() {
        return commitCalls;
    }

    public void failNextRead() {
        failNextRead = true;
    }

    public void failNextCommit() {
        failNextCommit = true;
    }

    public void throwOnCommit() {
        throwOnCommit = true;
    }

    @Override
    public int readCurrentVersion(String cdpId) {
        readCalls++;
        if (failNextRead) {
            failNextRead = false;
            throw new IllegalStateException("synthetic repository read failure");
        }
        return currentVersion(cdpId);
    }

    @Override
    public AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command) {
        commitCalls++;
        if (throwOnCommit) {
            throwOnCommit = false;
            throw new IllegalStateException("synthetic repository commit exception");
        }
        if (failNextCommit) {
            failNextCommit = false;
            return AtomicCommitOutcome.failed("REPOSITORY_INTERNAL_FAILURE", "synthetic repository failure");
        }
        int current = currentVersion(command.cdpId);
        if (current != command.expectedCurrentVersion) {
            return AtomicCommitOutcome.conflict(command.expectedCurrentVersion, current);
        }
        int next = current + 1;
        versions.put(command.cdpId, Integer.valueOf(next));
        return AtomicCommitOutcome.committed(current, next);
    }
}
