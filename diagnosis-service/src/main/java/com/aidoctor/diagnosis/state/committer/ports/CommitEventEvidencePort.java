package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.diagnosis.state.committer.InternalCommitEventEvidence;

/**
 * Non-authoritative internal event evidence sink.
 * Not a canonical contracts/v1 event type.
 */
public interface CommitEventEvidencePort {
    void record(InternalCommitEventEvidence evidence);
}
