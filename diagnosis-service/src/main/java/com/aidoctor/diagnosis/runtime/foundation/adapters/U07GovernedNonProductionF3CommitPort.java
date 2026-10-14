package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.state.committer.StateCommitter;

/**
 * NON-PRODUCTION F3 physical host binding. Commit authority is exclusively
 * the existing governed StateCommitter (capability, field, source, consent,
 * idempotency and audit). No direct mechanical repository write is exposed.
 * The host must supply real, non-permissive governance ports.
 */
public final class U07GovernedNonProductionF3CommitPort
        implements U07NonProductionPhysicalF3Owner.F3CommitPort {
    private final CDPRepository repository;
    private final StateCommitter committer;

    public U07GovernedNonProductionF3CommitPort(
            CDPRepository repository, StateCommitter committer) {
        if (repository == null || committer == null)
            throw new IllegalArgumentException("U07_F3_GOVERNED_COMMIT_DEPENDENCIES_REQUIRED");
        this.repository = repository;
        this.committer = committer;
    }

    @Override
    public int currentVersion(String cdpId) {
        Integer version = repository.findById(cdpId)
                .orElseThrow(() -> new IllegalStateException("U07_F3_CDP_NOT_FOUND"))
                .getVersion();
        if (version == null || version.intValue() < 0)
            throw new IllegalStateException("U07_F3_CDP_VERSION_INVALID");
        return version.intValue();
    }

    @Override
    public int commitAuthorized(String cdpId, int expectedVersion,
            U07NonProductionPhysicalF3Owner.PreparedF3 prepared) {
        if (prepared == null || prepared.patch == null
                || prepared.patch.envelope == null
                || !cdpId.equals(prepared.patch.cdpId)
                || prepared.patch.baseVersion == null
                || prepared.patch.baseVersion.intValue() != expectedVersion
                || !prepared.patchId.equals(prepared.patch.patchId)
                || !prepared.idempotencyKey.equals(prepared.patch.idempotencyKey)
                || !"P01".equals(prepared.patch.envelope.capabilityId))
            throw new IllegalStateException("U07_F3_GOVERNED_PATCH_BINDING_INVALID");
        StateTypes.CommitResult committed = committer.commit(prepared.patch);
        if (committed == null || !"COMMITTED".equals(committed.status)
                || committed.committedVersion == null
                || committed.committedVersion.intValue() != expectedVersion + 1)
            throw new IllegalStateException("U07_F3_GOVERNED_COMMIT_NOT_AUTHORIZED");
        return committed.committedVersion.intValue();
    }
}
