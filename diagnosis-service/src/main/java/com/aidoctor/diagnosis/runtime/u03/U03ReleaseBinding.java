package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minimal immutable P04/P06 release metadata required by the U03 first consumer. */
public final class U03ReleaseBinding {
    public static final String ANY = "*";

    private final String capabilityBindingId;
    private final String ruleReleaseId;
    private final String ruleReleaseVersion;
    private final String knowledgeReleaseId;
    private final String knowledgeReleaseVersion;
    private final String populationScope;
    private final String regionScope;
    private final String languageScope;
    private final String channelScope;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveUntil;
    private final List<String> provenanceRefs;
    private final boolean active;

    public U03ReleaseBinding(String capabilityBindingId, String ruleReleaseId, String knowledgeReleaseId, boolean active) {
        this(capabilityBindingId, ruleReleaseId, "1.0.0", knowledgeReleaseId, "1.0.0",
                ANY, ANY, ANY, ANY, LocalDateTime.MIN, null,
                Collections.<String>emptyList(), active);
    }

    public U03ReleaseBinding(
            String capabilityBindingId,
            String ruleReleaseId,
            String ruleReleaseVersion,
            String knowledgeReleaseId,
            String knowledgeReleaseVersion,
            String populationScope,
            String regionScope,
            String languageScope,
            String channelScope,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveUntil,
            List<String> provenanceRefs,
            boolean active) {
        this.capabilityBindingId = required(capabilityBindingId, "capabilityBindingId");
        this.ruleReleaseId = required(ruleReleaseId, "ruleReleaseId");
        this.ruleReleaseVersion = required(ruleReleaseVersion, "ruleReleaseVersion");
        this.knowledgeReleaseId = required(knowledgeReleaseId, "knowledgeReleaseId");
        this.knowledgeReleaseVersion = required(knowledgeReleaseVersion, "knowledgeReleaseVersion");
        this.populationScope = defaultAny(populationScope);
        this.regionScope = defaultAny(regionScope);
        this.languageScope = defaultAny(languageScope);
        this.channelScope = defaultAny(channelScope);
        if (effectiveFrom == null) throw new IllegalArgumentException("effectiveFrom is required");
        if (effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveUntil cannot be before effectiveFrom");
        }
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.provenanceRefs = provenanceRefs == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(provenanceRefs));
        this.active = active;
    }

    public String getCapabilityBindingId() { return capabilityBindingId; }
    public String getRuleReleaseId() { return ruleReleaseId; }
    public String getRuleReleaseVersion() { return ruleReleaseVersion; }
    public String getKnowledgeReleaseId() { return knowledgeReleaseId; }
    public String getKnowledgeReleaseVersion() { return knowledgeReleaseVersion; }
    public String getPopulationScope() { return populationScope; }
    public String getRegionScope() { return regionScope; }
    public String getLanguageScope() { return languageScope; }
    public String getChannelScope() { return channelScope; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public List<String> getProvenanceRefs() { return provenanceRefs; }
    public boolean isActive() { return active; }

    public boolean isEffectiveAt(LocalDateTime now) {
        return now != null && !now.isBefore(effectiveFrom)
                && (effectiveUntil == null || now.isBefore(effectiveUntil));
    }

    public boolean isCompatibleWith(CapabilityBindingRecord capabilityBinding) {
        return capabilityBinding != null
                && capabilityBindingId.equals(capabilityBinding.getBindingId())
                && scopeCompatible(populationScope, capabilityBinding.getPopulationScope())
                && scopeCompatible(regionScope, capabilityBinding.getRegionScope())
                && scopeCompatible(languageScope, capabilityBinding.getLanguageScope())
                && scopeCompatible(channelScope, capabilityBinding.getChannelScope());
    }

    private static boolean scopeCompatible(String releaseScope, String capabilityScope) {
        if (ANY.equals(releaseScope)) return true;
        if (CapabilityBindingRecord.ANY.equals(capabilityScope)) return false;
        return releaseScope.equals(capabilityScope);
    }

    private static String defaultAny(String value) {
        return value == null || value.trim().isEmpty() ? ANY : value;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
