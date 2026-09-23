package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.contracts.v1.StateTypes;

import java.util.List;
import java.util.Map;

/** Exact synthetic read-back evidence for the dedicated readiness invalidation effect. */
public final class U05ReadinessInvalidationEvidence {
    private final StateTypes.CommitResult commitResult;
    private final String invalidationEffectId;
    private final String authoritativeReadinessRecordRef;
    private final int committedClinicalStateVersion;

    private U05ReadinessInvalidationEvidence(
            StateTypes.CommitResult commitResult,
            String invalidationEffectId,
            String authoritativeReadinessRecordRef,
            int committedClinicalStateVersion) {
        this.commitResult = commitResult;
        this.invalidationEffectId = invalidationEffectId;
        this.authoritativeReadinessRecordRef = authoritativeReadinessRecordRef;
        this.committedClinicalStateVersion = committedClinicalStateVersion;
    }

    @SuppressWarnings("unchecked")
    public static U05ReadinessInvalidationEvidence fromVerifiedSnapshot(
            U05ReadinessInvalidationRequest request,
            U05ReadinessInvalidationProposal proposal,
            StateTypes.CommitResult result,
            U05ClinicalReadinessSnapshot snapshot) {
        if (request == null || proposal == null || result == null || snapshot == null) {
            throw new IllegalArgumentException("invalidation evidence inputs are required");
        }
        if (!"COMMITTED".equals(result.status)
                || result.committedVersion == null
                || snapshot.getVersion() != result.committedVersion.intValue()) {
            throw new IllegalStateException("U05_INVALIDATION_READBACK_COMMIT_MISMATCH");
        }

        Object rawExpected = proposal.getStatePatch().operations.get(0).value;
        if (!(rawExpected instanceof Map<?, ?>)) {
            throw new IllegalStateException("U05_INVALIDATION_EXPECTED_PAYLOAD_MALFORMED");
        }
        Map<String, Object> expected = (Map<String, Object>) rawExpected;
        Map<String, Object> actual = snapshot.getReadinessPayload();

        if (!expected.equals(actual)) {
            throw new IllegalStateException("U05_INVALIDATION_READBACK_PAYLOAD_MISMATCH");
        }
        require(actual, "readiness_record_id", request.getPriorReadinessRecordRef());
        require(actual, "effect_id", request.getPriorReadinessEffectId());
        require(actual, "state_validity", "STALE");
        require(actual, "invalidation_effect_ref", proposal.getInvalidationEffectId());

        Object reasons = actual.get("invalidation_reason_refs");
        if (!(reasons instanceof List<?>)
                || !((List<Object>) reasons).contains(request.getInvalidationReasonCode())
                || !((List<Object>) reasons).contains(request.getSourceEventOrDecisionRef())) {
            throw new IllegalStateException("U05_INVALIDATION_READBACK_REASON_MISMATCH");
        }

        String recordRef = "synthetic-state:" + request.getCdpId()
                + "@" + snapshot.getVersion()
                + U05ReadinessStateProposalFactory.READINESS_PATH
                + "#" + request.getPriorReadinessRecordRef();

        return new U05ReadinessInvalidationEvidence(
                result,
                proposal.getInvalidationEffectId(),
                recordRef,
                snapshot.getVersion());
    }

    private static void require(
            Map<String, Object> payload,
            String key,
            Object expected) {
        if (!expected.equals(payload.get(key))) {
            throw new IllegalStateException("U05 invalidation read-back " + key + " mismatch");
        }
    }

    public StateTypes.CommitResult getCommitResult() { return commitResult; }
    public String getInvalidationEffectId() { return invalidationEffectId; }
    public String getAuthoritativeReadinessRecordRef() { return authoritativeReadinessRecordRef; }
    public int getCommittedClinicalStateVersion() { return committedClinicalStateVersion; }
}
