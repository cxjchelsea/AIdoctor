package com.aidoctor.diagnosis.state.committer.support;

import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.MechanicalVersionRepositoryFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticCapabilityPolicyFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticConsentPolicyFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticFieldPermissionFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticSourceValidationFake;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class StateCommitterTestHarness {
    public static final Clock CLOCK = Clock.fixed(Instant.parse(SyntheticStatePatchFactory.FIXED_AT), ZoneOffset.UTC);

    public final MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake();
    public final SyntheticCapabilityPolicyFake capabilityPolicy = new SyntheticCapabilityPolicyFake();
    public final SyntheticFieldPermissionFake fieldPermission = new SyntheticFieldPermissionFake();
    public final SyntheticConsentPolicyFake consentPolicy = new SyntheticConsentPolicyFake();
    public final SyntheticSourceValidationFake sourceValidation = new SyntheticSourceValidationFake();
    public final InMemoryIdempotencyFake idempotency = new InMemoryIdempotencyFake();
    public final SyntheticAuditPortFake audit = new SyntheticAuditPortFake(CLOCK);
    public final RecordingCommitEventEvidenceFake events = new RecordingCommitEventEvidenceFake();
    public final StateCommitter committer;

    public StateCommitterTestHarness() {
        repository.seed(SyntheticStatePatchFactory.CDP_ID, 0);
        committer = new StateCommitter(
                repository,
                capabilityPolicy,
                fieldPermission,
                consentPolicy,
                sourceValidation,
                idempotency,
                audit,
                events,
                CLOCK
        );
    }
}
