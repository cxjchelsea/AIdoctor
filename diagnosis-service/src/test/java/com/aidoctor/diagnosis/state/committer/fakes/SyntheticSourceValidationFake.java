package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;

import java.util.HashSet;
import java.util.Set;

public final class SyntheticSourceValidationFake implements SourceValidationPort {
    public static final String AUTHORIZED_SOURCE = "TOOL_OUTPUT";

    private final Set<String> authorizedSources = new HashSet<String>();
    private int evaluateCalls;

    public SyntheticSourceValidationFake() {
        authorizedSources.add(AUTHORIZED_SOURCE);
    }

    public void authorize(String source) {
        authorizedSources.add(source);
    }

    public int evaluateCalls() {
        return evaluateCalls;
    }

    @Override
    public SourceDecision evaluate(String source) {
        evaluateCalls++;
        if (source != null && authorizedSources.contains(source)) {
            return SourceDecision.authorized();
        }
        return SourceDecision.denied();
    }
}
