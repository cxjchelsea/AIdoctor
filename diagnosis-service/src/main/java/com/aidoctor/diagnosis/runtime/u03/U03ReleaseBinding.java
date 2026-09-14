package com.aidoctor.diagnosis.runtime.u03;

/** Minimal U03 release-binding metadata carried with one governed assessment. */
public final class U03ReleaseBinding {
    private final String capabilityBindingId;
    private final String ruleReleaseId;
    private final String knowledgeReleaseId;
    private final boolean active;

    public U03ReleaseBinding(String capabilityBindingId, String ruleReleaseId, String knowledgeReleaseId, boolean active) {
        this.capabilityBindingId = required(capabilityBindingId, "capabilityBindingId");
        this.ruleReleaseId = required(ruleReleaseId, "ruleReleaseId");
        this.knowledgeReleaseId = required(knowledgeReleaseId, "knowledgeReleaseId");
        this.active = active;
    }

    public String getCapabilityBindingId() { return capabilityBindingId; }
    public String getRuleReleaseId() { return ruleReleaseId; }
    public String getKnowledgeReleaseId() { return knowledgeReleaseId; }
    public boolean isActive() { return active; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
