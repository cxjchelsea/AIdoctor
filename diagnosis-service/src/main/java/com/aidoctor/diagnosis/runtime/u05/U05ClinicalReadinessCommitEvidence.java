package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.Map;

/**
 * Post-P01 commit evidence proven by exact synthetic-state read-back.
 *
 * <p>Mechanical COMMITTED alone is insufficient. Evidence exists only after
 * the exact structured readiness payload is observed at the committed version.</p>
 */
public final class U05ClinicalReadinessCommitEvidence {
    private final String readinessRecordId;
    private final String effectId;
    private final String proposalRef;
    private final String commitStatus;
    private final String commitResultRef;
    private final int previousClinicalStateVersion;
    private final int committedClinicalStateVersion;
    private final String auditRef;
    private final String authoritativeReadinessRecordRef;
    private final String committedAt;

    private U05ClinicalReadinessCommitEvidence(
            String readinessRecordId,
            String effectId,
            String proposalRef,
            String commitStatus,
            String commitResultRef,
            int previousClinicalStateVersion,
            int committedClinicalStateVersion,
            String auditRef,
            String authoritativeReadinessRecordRef,
            String committedAt) {
        this.readinessRecordId = readinessRecordId;
        this.effectId = effectId;
        this.proposalRef = proposalRef;
        this.commitStatus = commitStatus;
        this.commitResultRef = commitResultRef;
        this.previousClinicalStateVersion = previousClinicalStateVersion;
        this.committedClinicalStateVersion = committedClinicalStateVersion;
        this.auditRef = auditRef;
        this.authoritativeReadinessRecordRef = authoritativeReadinessRecordRef;
        this.committedAt = committedAt;
    }

    @SuppressWarnings("unchecked")
    public static U05ClinicalReadinessCommitEvidence fromVerifiedSnapshot(
            U05ReadinessStateProposal proposal,
            StateTypes.CommitResult result,
            U05ClinicalReadinessSnapshot snapshot) {
        if (proposal == null || result == null || snapshot == null) {
            throw new IllegalArgumentException("commit evidence inputs are required");
        }
        if (!"COMMITTED".equals(result.status)
                || result.previousVersion == null
                || result.committedVersion == null) {
            throw new IllegalStateException("readiness evidence requires COMMITTED result");
        }
        if (snapshot.getVersion() != result.committedVersion.intValue()) {
            throw new IllegalStateException("U05_READBACK_COMMITTED_VERSION_MISMATCH");
        }
        if (result.auditRef == null
                || result.auditRef.auditId == null
                || result.auditRef.auditId.trim().isEmpty()) {
            throw new IllegalStateException("COMMITTED readiness result requires audit_ref");
        }

        Object rawExpected = proposal.getStatePatch().operations.get(0).value;
        if (!(rawExpected instanceof Map<?, ?>)) {
            throw new IllegalStateException("U05 expected readiness payload malformed");
        }
        Map<String, Object> expected = (Map<String, Object>) rawExpected;
        Map<String, Object> actual = snapshot.getReadinessPayload();
        if (!expected.equals(actual)) {
            throw new IllegalStateException("U05_READBACK_PAYLOAD_MISMATCH");
        }

        require(actual, "readiness_record_id", proposal.getReadinessRecordId());
        require(actual, "effect_id", proposal.getEffectId());
        require(actual, "proposal_ref", proposal.getProposalId());
        require(actual, "canonical_payload_fingerprint", proposal.getCanonicalPayloadFingerprint());
        require(actual, "state_validity", "CURRENT");

        String audit = result.auditRef.auditId;
        String resultRef = U05Ids.hash(
                "u05-readiness-commit-result",
                result.patchId,
                result.cdpId,
                result.status,
                String.valueOf(result.previousVersion),
                String.valueOf(result.committedVersion),
                audit);

        String recordRef = "synthetic-state:" + result.cdpId + "@"
                + result.committedVersion
                + U05ReadinessStateProposalFactory.READINESS_PATH
                + "#" + proposal.getReadinessRecordId();

        return new U05ClinicalReadinessCommitEvidence(
                proposal.getReadinessRecordId(),
                proposal.getEffectId(),
                proposal.getProposalId(),
                result.status,
                resultRef,
                result.previousVersion.intValue(),
                result.committedVersion.intValue(),
                audit,
                recordRef,
                result.committedAt);
    }

    private static void require(
            Map<String, Object> payload,
            String key,
            Object expected) {
        if (!expected.equals(payload.get(key))) {
            throw new IllegalStateException("U05 read-back " + key + " mismatch");
        }
    }

    public String getReadinessRecordId() { return readinessRecordId; }
    public String getEffectId() { return effectId; }
    public String getProposalRef() { return proposalRef; }
    public String getCommitStatus() { return commitStatus; }
    public String getCommitResultRef() { return commitResultRef; }
    public int getPreviousClinicalStateVersion() { return previousClinicalStateVersion; }
    public int getCommittedClinicalStateVersion() { return committedClinicalStateVersion; }
    public String getAuditRef() { return auditRef; }
    public String getAuthoritativeReadinessRecordRef() { return authoritativeReadinessRecordRef; }
    public String getCommittedAt() { return committedAt; }
}
