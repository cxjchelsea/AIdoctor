package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.diagnosis.state.committer.InternalCommitEventEvidence;

/**
 * Non-authoritative internal event evidence sink.
 * Not a canonical contracts/v1 event type.
 * Sink failure cannot alter an authoritative commit or replay result.
 */
public interface CommitEventEvidencePort {
    void record(InternalCommitEventEvidence evidence);
}
