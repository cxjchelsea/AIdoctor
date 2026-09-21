package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

/** Post-P01 commit evidence. It is not part of the pre-commit readiness state value. */
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

    public static U05ClinicalReadinessCommitEvidence from(
            U05ReadinessStateProposal proposal,
            StateTypes.CommitResult result) {
        if (proposal == null || result == null) throw new IllegalArgumentException("commit evidence inputs are required");
        if (!"COMMITTED".equals(result.status)
                || result.previousVersion == null
                || result.committedVersion == null) {
            throw new IllegalStateException("authoritative readiness commit evidence requires COMMITTED result");
        }
        String audit = result.auditRef == null ? "audit-unavailable" : result.auditRef.auditId;
        String resultRef = U05Ids.hash(
                "u05-readiness-commit-result",
                result.patchId,
                result.cdpId,
                result.status,
                String.valueOf(result.previousVersion),
                String.valueOf(result.committedVersion),
                audit);
        String recordRef = "clinical-state:" + result.cdpId + "@"
                + result.committedVersion + U05ReadinessStateProposalFactory.READINESS_PATH;
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
