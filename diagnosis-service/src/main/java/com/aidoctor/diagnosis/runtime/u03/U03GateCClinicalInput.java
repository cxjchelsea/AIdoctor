package com.aidoctor.diagnosis.runtime.u03;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed non-production input consumed by the frozen Gate-C U03 rule evaluator.
 *
 * <p>This object does not declare clinical truth on its own. It carries the already
 * accepted clinical fact values needed to execute the exact Gate-C-frozen rule
 * semantics. Production sourcing is intentionally out of scope.</p>
 */
public final class U03GateCClinicalInput {
    public static final String PRESENT = "PRESENT";
    public static final String ABSENT = "ABSENT";

    private final String ageState;
    private final Integer ageYears;
    private final String pregnancyOrPuerperium;
    private final boolean pediatrics;
    private final boolean regionScopeOk;
    private final boolean channelScopeOk;
    private final String setting;
    private final String suspectedSepsis;
    private final String dyspnoeaContext;
    private final Map<String, String> evidenceStates;
    private final Map<String, Measurement> measurements;
    private final Map<String, String> ruleScopes;
    private final boolean invalidInput;
    private final boolean dependencyFailure;

    private U03GateCClinicalInput(Builder b) {
        this.ageState = required(b.ageState, "ageState");
        this.ageYears = b.ageYears;
        this.pregnancyOrPuerperium = required(b.pregnancyOrPuerperium, "pregnancyOrPuerperium");
        this.pediatrics = b.pediatrics;
        this.regionScopeOk = b.regionScopeOk;
        this.channelScopeOk = b.channelScopeOk;
        this.setting = required(b.setting, "setting");
        this.suspectedSepsis = required(b.suspectedSepsis, "suspectedSepsis");
        this.dyspnoeaContext = required(b.dyspnoeaContext, "dyspnoeaContext");
        this.evidenceStates = immutableStringMap(b.evidenceStates);
        this.measurements = Collections.unmodifiableMap(new LinkedHashMap<String, Measurement>(b.measurements));
        this.ruleScopes = immutableStringMap(b.ruleScopes);
        this.invalidInput = b.invalidInput;
        this.dependencyFailure = b.dependencyFailure;
    }

    public String getAgeState() { return ageState; }
    public Integer getAgeYears() { return ageYears; }
    public String getPregnancyOrPuerperium() { return pregnancyOrPuerperium; }
    public boolean isPediatrics() { return pediatrics; }
    public boolean isRegionScopeOk() { return regionScopeOk; }
    public boolean isChannelScopeOk() { return channelScopeOk; }
    public String getSetting() { return setting; }
    public String getSuspectedSepsis() { return suspectedSepsis; }
    public String getDyspnoeaContext() { return dyspnoeaContext; }
    public boolean isInvalidInput() { return invalidInput; }
    public boolean isDependencyFailure() { return dependencyFailure; }

    public String evidenceState(String evidenceRef) {
        String value = evidenceStates.get(evidenceRef);
        return value == null ? ABSENT : value;
    }

    public Measurement measurement(String key) {
        Measurement value = measurements.get(key);
        return value == null ? Measurement.state("UNMEASURED") : value;
    }

    public String ruleScope(String ruleId) {
        String value = ruleScopes.get(ruleId);
        return value == null ? "TRUE" : value;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String ageState = PRESENT;
        private Integer ageYears = Integer.valueOf(30);
        private String pregnancyOrPuerperium = "FALSE";
        private boolean pediatrics;
        private boolean regionScopeOk = true;
        private boolean channelScopeOk = true;
        private String setting = "SOURCE_SUPPORTED_COMMUNITY";
        private String suspectedSepsis = "FALSE";
        private String dyspnoeaContext = "FALSE";
        private final Map<String, String> evidenceStates = new LinkedHashMap<String, String>();
        private final Map<String, Measurement> measurements = new LinkedHashMap<String, Measurement>();
        private final Map<String, String> ruleScopes = new LinkedHashMap<String, String>();
        private boolean invalidInput;
        private boolean dependencyFailure;

        public Builder ageYears(int value) {
            this.ageState = PRESENT;
            this.ageYears = Integer.valueOf(value);
            return this;
        }

        public Builder ageState(String state) {
            this.ageState = required(state, "ageState");
            if (!PRESENT.equals(state)) this.ageYears = null;
            return this;
        }

        public Builder pregnancyOrPuerperium(String value) {
            this.pregnancyOrPuerperium = required(value, "pregnancyOrPuerperium");
            return this;
        }

        public Builder pediatrics(boolean value) { this.pediatrics = value; return this; }
        public Builder regionScopeOk(boolean value) { this.regionScopeOk = value; return this; }
        public Builder channelScopeOk(boolean value) { this.channelScopeOk = value; return this; }
        public Builder setting(String value) { this.setting = required(value, "setting"); return this; }
        public Builder suspectedSepsis(String value) { this.suspectedSepsis = required(value, "suspectedSepsis"); return this; }
        public Builder dyspnoeaContext(String value) { this.dyspnoeaContext = required(value, "dyspnoeaContext"); return this; }
        public Builder evidence(String ref, String state) {
            evidenceStates.put(required(ref, "evidence ref"), required(state, "evidence state"));
            return this;
        }
        public Builder measurement(String key, Measurement value) {
            if (value == null) throw new IllegalArgumentException("measurement value is required");
            measurements.put(required(key, "measurement key"), value);
            return this;
        }
        public Builder ruleScope(String ruleId, String state) {
            ruleScopes.put(required(ruleId, "ruleId"), required(state, "rule scope"));
            return this;
        }
        public Builder invalidInput(boolean value) { this.invalidInput = value; return this; }
        public Builder dependencyFailure(boolean value) { this.dependencyFailure = value; return this; }
        public U03GateCClinicalInput build() { return new U03GateCClinicalInput(this); }
    }

    public static final class Measurement {
        private final String state;
        private final Double value;

        private Measurement(String state, Double value) {
            this.state = required(state, "measurement state");
            this.value = value;
        }

        public static Measurement present(double value) {
            return new Measurement(PRESENT, Double.valueOf(value));
        }

        public static Measurement state(String state) {
            return new Measurement(state, null);
        }

        public String getState() { return state; }
        public Double getValue() { return value; }
    }

    private static Map<String, String> immutableStringMap(Map<String, String> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
