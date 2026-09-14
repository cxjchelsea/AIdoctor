package com.aidoctor.diagnosis.runtime.governance;

import lombok.Getter;

/** Execution context used to validate a capability binding before invocation. */
@Getter
public class CapabilityExecutionContext {
    private final String scopeVersion;
    private final String contractVersion;
    private final String population;
    private final String region;
    private final String language;
    private final String channel;

    public CapabilityExecutionContext(
            String scopeVersion,
            String contractVersion,
            String population,
            String region,
            String language,
            String channel
    ) {
        this.scopeVersion = required(scopeVersion, "scopeVersion");
        this.contractVersion = required(contractVersion, "contractVersion");
        this.population = defaultAny(population);
        this.region = defaultAny(region);
        this.language = defaultAny(language);
        this.channel = defaultAny(channel);
    }

    private static String defaultAny(String value) {
        return value == null || value.trim().isEmpty() ? CapabilityBindingRecord.ANY : value;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
