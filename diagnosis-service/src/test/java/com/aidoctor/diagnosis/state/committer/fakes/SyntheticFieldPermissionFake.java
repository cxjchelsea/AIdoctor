package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;

import java.util.HashSet;
import java.util.Set;

public final class SyntheticFieldPermissionFake implements FieldPermissionPort {
    public static final String AUTHORIZED_PATH = "/patient_state/synthetic_test_value";

    private final Set<String> authorizedPaths = new HashSet<String>();
    private int evaluateCalls;

    public SyntheticFieldPermissionFake() {
        authorizedPaths.add(AUTHORIZED_PATH);
    }

    public void authorize(String path) {
        authorizedPaths.add(path);
    }

    public int evaluateCalls() {
        return evaluateCalls;
    }

    @Override
    public FieldPermissionDecision evaluate(String path, String capabilityId) {
        evaluateCalls++;
        if (path != null && authorizedPaths.contains(path)) {
            return FieldPermissionDecision.authorized();
        }
        return FieldPermissionDecision.denied();
    }
}
