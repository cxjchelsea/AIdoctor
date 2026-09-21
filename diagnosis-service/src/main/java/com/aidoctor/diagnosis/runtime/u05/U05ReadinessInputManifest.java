package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable reference-only admission snapshot over authoritative RDP-05 records. */
public final class U05ReadinessInputManifest {
    private final String manifestRef;
    private final String setIdentity;
    private final String consultationId;
    private final String cdpId;
    private final int clinicalStateVersion;
    private final String evaluationContext;
    private final List<U05ReadinessInput> inputs;

    public U05ReadinessInputManifest(
            String manifestRef,
            String setIdentity,
            String consultationId,
            String cdpId,
            int clinicalStateVersion,
            String evaluationContext,
            List<U05ReadinessInput> inputs) {
        this.manifestRef = required(manifestRef, "manifestRef");
        this.setIdentity = required(setIdentity, "setIdentity");
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        this.clinicalStateVersion = clinicalStateVersion;
        this.evaluationContext = required(evaluationContext, "evaluationContext");
        if (inputs == null || inputs.isEmpty()) throw new IllegalArgumentException("inputs are required");

        List<U05ReadinessInput> copy = new ArrayList<U05ReadinessInput>(inputs);
        Collections.sort(copy, new Comparator<U05ReadinessInput>() {
            @Override
            public int compare(U05ReadinessInput left, U05ReadinessInput right) {
                int domain = left.getSourceDomain().compareTo(right.getSourceDomain());
                return domain != 0 ? domain : left.getReadinessInputId().compareTo(right.getReadinessInputId());
            }
        });
        this.inputs = Collections.unmodifiableList(copy);
        requireDomain(U05ReadinessInput.F1);
        requireDomain(U05ReadinessInput.F3);
        requireDomain(U05ReadinessInput.F5);
        requireDomain(U05ReadinessInput.F6);
    }

    public String getManifestRef() { return manifestRef; }
    public String getSetIdentity() { return setIdentity; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getEvaluationContext() { return evaluationContext; }
    public List<U05ReadinessInput> getInputs() { return inputs; }

    public List<U05ReadinessInput> getInputs(String domain) {
        List<U05ReadinessInput> result = new ArrayList<U05ReadinessInput>();
        for (U05ReadinessInput input : inputs) {
            if (domain.equals(input.getSourceDomain())) result.add(input);
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> presentInputRefs() {
        List<String> refs = new ArrayList<String>();
        for (U05ReadinessInput input : inputs) {
            if (input.isPresent()) refs.add(input.getReadinessInputId());
        }
        return Collections.unmodifiableList(refs);
    }

    public boolean hasStatus(String domain, String status) {
        for (U05ReadinessInput input : inputs) {
            if (domain.equals(input.getSourceDomain()) && status.equals(input.getApplicabilityStatus())) return true;
        }
        return false;
    }

    public boolean hasCurrentPresent(String domain) {
        for (U05ReadinessInput input : inputs) {
            if (domain.equals(input.getSourceDomain())
                    && input.isCurrentPresentAt(clinicalStateVersion)) return true;
        }
        return false;
    }

    public String computedSemanticIdentity() {
        List<String> parts = new ArrayList<String>();
        parts.add(consultationId);
        parts.add(cdpId);
        parts.add(String.valueOf(clinicalStateVersion));
        parts.add(evaluationContext);
        for (U05ReadinessInput input : inputs) parts.add(input.semanticFingerprint());
        return U05Ids.hash("u05-input-set", parts.toArray(new String[parts.size()]));
    }

    public void validateNoDuplicateInputIdConflict() {
        Set<String> seen = new LinkedHashSet<String>();
        for (U05ReadinessInput input : inputs) {
            if (!seen.add(input.getReadinessInputId())) {
                throw new IllegalStateException("duplicate readiness_input_id in manifest");
            }
        }
    }

    private void requireDomain(String domain) {
        if (getInputs(domain).isEmpty()) throw new IllegalArgumentException("manifest missing required domain " + domain);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
