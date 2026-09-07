package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
    private boolean conflictNextCommit;
    private final List<String> callOrder;

    public MechanicalVersionRepositoryFake() {
        this(new ArrayList<String>());
    }

    public MechanicalVersionRepositoryFake(List<String> callOrder) {
        this.callOrder = callOrder;
    }

    public synchronized void seed(String cdpId, int version) {
        versions.put(cdpId, Integer.valueOf(version));
    }

    public synchronized int currentVersion(String cdpId) {
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

    public void conflictNextCommit() {
        conflictNextCommit = true;
    }

    @Override
    public synchronized int readCurrentVersion(String cdpId) {
        readCalls++;
        callOrder.add("repository.read");
        if (failNextRead) {
            failNextRead = false;
            throw new IllegalStateException("synthetic repository read failure");
        }
        return currentVersion(cdpId);
    }

    @Override
    public synchronized AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command) {
        commitCalls++;
        callOrder.add("repository.commit");
        if (throwOnCommit) {
            throwOnCommit = false;
            throw new IllegalStateException("synthetic repository commit exception");
        }
        if (failNextCommit) {
            failNextCommit = false;
            return AtomicCommitOutcome.failed("REPOSITORY_INTERNAL_FAILURE", "synthetic repository failure");
        }
        int current = currentVersion(command.cdpId);
        if (conflictNextCommit) {
            conflictNextCommit = false;
            return AtomicCommitOutcome.conflict(command.expectedCurrentVersion, current);
        }
        if (current != command.expectedCurrentVersion) {
            return AtomicCommitOutcome.conflict(command.expectedCurrentVersion, current);
        }
        int next = current + 1;
        versions.put(command.cdpId, Integer.valueOf(next));
        return AtomicCommitOutcome.committed(current);
    }
}
