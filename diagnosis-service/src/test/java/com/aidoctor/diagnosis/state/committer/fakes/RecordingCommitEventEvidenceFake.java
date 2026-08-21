package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.InternalCommitEventEvidence;
import com.aidoctor.diagnosis.state.committer.ports.CommitEventEvidencePort;

import java.util.ArrayList;
import java.util.List;

public final class RecordingCommitEventEvidenceFake implements CommitEventEvidencePort {
    private final List<InternalCommitEventEvidence> events = new ArrayList<InternalCommitEventEvidence>();

    public List<InternalCommitEventEvidence> events() {
        return events;
    }

    @Override
    public void record(InternalCommitEventEvidence evidence) {
        events.add(evidence);
    }
}
