package com.aidoctor.diagnosis.runtime.u03;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable accepted-evidence input bound to one exact Clinical State Version.
 *
 * <p>This object carries evidence identity/provenance only. It does not declare
 * clinical truth, risk, safety, or mutation authority.</p>
 */
public final class U03AcceptedEvidenceBinding {
    private final String acceptanceRef;
    private final int clinicalStateVersion;
    private final List<String> evidenceRefs;
    private final List<String> sourceRefs;
    private final List<String> provenanceRefs;

    public U03AcceptedEvidenceBinding(
            String acceptanceRef,
            int clinicalStateVersion,
            List<String> evidenceRefs,
            List<String> sourceRefs,
            List<String> provenanceRefs) {
        this.acceptanceRef = required(acceptanceRef, "acceptanceRef");
        if (clinicalStateVersion < 0) {
            throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        }
        this.clinicalStateVersion = clinicalStateVersion;
        this.evidenceRefs = requiredList(evidenceRefs, "evidenceRefs");
        this.sourceRefs = immutable(sourceRefs);
        this.provenanceRefs = requiredList(provenanceRefs, "provenanceRefs");
    }

    public String getAcceptanceRef() { return acceptanceRef; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public List<String> getEvidenceRefs() { return evidenceRefs; }
    public List<String> getSourceRefs() { return sourceRefs; }
    public List<String> getProvenanceRefs() { return provenanceRefs; }

    private static List<String> requiredList(List<String> values, String name) {
        List<String> copy = immutable(values);
        if (copy.isEmpty()) throw new IllegalArgumentException(name + " is required");
        for (String value : copy) required(value, name + " entry");
        return copy;
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
