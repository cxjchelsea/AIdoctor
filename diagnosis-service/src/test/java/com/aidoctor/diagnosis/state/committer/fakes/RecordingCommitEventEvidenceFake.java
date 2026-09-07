package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.InternalCommitEventEvidence;
import com.aidoctor.diagnosis.state.committer.ports.CommitEventEvidencePort;

import java.util.ArrayList;
import java.util.List;

public final class RecordingCommitEventEvidenceFake implements CommitEventEvidencePort {
    private final List<InternalCommitEventEvidence> events = new ArrayList<InternalCommitEventEvidence>();
    private boolean failNext;
    private final List<String> callOrder;

    public RecordingCommitEventEvidenceFake() {
        this(new ArrayList<String>());
    }

    public RecordingCommitEventEvidenceFake(List<String> callOrder) {
        this.callOrder = callOrder;
    }

    public List<InternalCommitEventEvidence> events() {
        return events;
    }

    public void failNext() {
        failNext = true;
    }

    @Override
    public void record(InternalCommitEventEvidence evidence) {
        callOrder.add("event.record");
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("synthetic event evidence failure");
        }
        events.add(evidence);
    }
}
