package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;

import java.util.HashMap;
import java.util.Map;

public final class SyntheticConsentPolicyFake implements ConsentPolicyPort {
    private final Map<String, ConsentDecision.Status> statuses = new HashMap<String, ConsentDecision.Status>();
    private int evaluateCalls;

    public SyntheticConsentPolicyFake() {
        statuses.put("synthetic-cdp-001", ConsentDecision.Status.AUTHORIZED);
    }

    public void authorize(String cdpId) {
        statuses.put(cdpId, ConsentDecision.Status.AUTHORIZED);
    }

    public void missing(String cdpId) {
        statuses.put(cdpId, ConsentDecision.Status.MISSING);
    }

    public void deny(String cdpId) {
        statuses.put(cdpId, ConsentDecision.Status.DENIED);
    }

    public void invalid(String cdpId) {
        statuses.put(cdpId, ConsentDecision.Status.INVALID);
    }

    public int evaluateCalls() {
        return evaluateCalls;
    }

    @Override
    public ConsentDecision evaluate(String cdpId, String capabilityId) {
        evaluateCalls++;
        ConsentDecision.Status status = statuses.get(cdpId);
        if (status == null || status == ConsentDecision.Status.MISSING) {
            return ConsentDecision.missing();
        }
        if (status == ConsentDecision.Status.DENIED) {
            return ConsentDecision.denied();
        }
        if (status == ConsentDecision.Status.INVALID) {
            return ConsentDecision.invalid();
        }
        return ConsentDecision.authorized();
    }
}
