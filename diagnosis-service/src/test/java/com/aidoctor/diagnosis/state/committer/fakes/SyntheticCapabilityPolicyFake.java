package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;

import java.util.HashSet;
import java.util.Set;

public final class SyntheticCapabilityPolicyFake implements CapabilityPolicyPort {
    public static final String AUTHORIZED_ID = "synthetic-capability-v1";
    public static final String AUTHORIZED_VERSION = "1.0.0";

    private final Set<String> deniedIds = new HashSet<String>();
    private int evaluateCalls;

    public void deny(String capabilityId) {
        deniedIds.add(capabilityId);
    }

    public int evaluateCalls() {
        return evaluateCalls;
    }

    @Override
    public CapabilityDecision evaluate(String capabilityId, String capabilityVersion) {
        evaluateCalls++;
        if (deniedIds.contains(capabilityId)) {
            return CapabilityDecision.denied();
        }
        if (AUTHORIZED_ID.equals(capabilityId) && AUTHORIZED_VERSION.equals(capabilityVersion)) {
            return CapabilityDecision.authorized();
        }
        return CapabilityDecision.invalid();
    }
}
