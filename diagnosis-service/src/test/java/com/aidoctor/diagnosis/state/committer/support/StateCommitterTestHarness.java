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
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class StateCommitterTestHarness {
    public static final Clock CLOCK = Clock.fixed(Instant.parse(SyntheticStatePatchFactory.FIXED_AT), ZoneOffset.UTC);

    public final List<String> callOrder = new ArrayList<String>();
    public final MechanicalVersionRepositoryFake repository = new MechanicalVersionRepositoryFake(callOrder);
    public final SyntheticCapabilityPolicyFake capabilityPolicy = new SyntheticCapabilityPolicyFake();
    public final SyntheticFieldPermissionFake fieldPermission = new SyntheticFieldPermissionFake();
    public final SyntheticConsentPolicyFake consentPolicy = new SyntheticConsentPolicyFake();
    public final SyntheticSourceValidationFake sourceValidation = new SyntheticSourceValidationFake();
    public final InMemoryIdempotencyFake idempotency = new InMemoryIdempotencyFake(callOrder);
    public final SyntheticAuditPortFake audit = new SyntheticAuditPortFake(CLOCK, callOrder);
    public final RecordingCommitEventEvidenceFake events = new RecordingCommitEventEvidenceFake(callOrder);
    public final StateCommitter committer;

    public StateCommitterTestHarness() {
        repository.seed(SyntheticStatePatchFactory.CDP_ID, 0);
        committer = new StateCommitter(
                repository,
                orderedCapability(),
                orderedFieldPermission(),
                orderedConsent(),
                orderedSourceValidation(),
                idempotency,
                audit,
                events,
                CLOCK
        );
    }

    private CapabilityPolicyPort orderedCapability() {
        return new CapabilityPolicyPort() {
            @Override
            public CapabilityDecision evaluate(String capabilityId, String capabilityVersion) {
                callOrder.add("capability.evaluate");
                return capabilityPolicy.evaluate(capabilityId, capabilityVersion);
            }
        };
    }

    private ConsentPolicyPort orderedConsent() {
        return new ConsentPolicyPort() {
            @Override
            public ConsentDecision evaluate(String cdpId, String capabilityId) {
                callOrder.add("consent.evaluate");
                return consentPolicy.evaluate(cdpId, capabilityId);
            }
        };
    }

    private FieldPermissionPort orderedFieldPermission() {
        return new FieldPermissionPort() {
            @Override
            public FieldPermissionDecision evaluate(String path, String capabilityId) {
                callOrder.add("field.evaluate");
                return fieldPermission.evaluate(path, capabilityId);
            }
        };
    }

    private SourceValidationPort orderedSourceValidation() {
        return new SourceValidationPort() {
            @Override
            public SourceDecision evaluate(String source) {
                callOrder.add("source.evaluate");
                return sourceValidation.evaluate(source);
            }
        };
    }
}
