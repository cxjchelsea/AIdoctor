package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable reference-only admission snapshot over authoritative RDP-05 records. */
public final class U05ReadinessInputManifest {
    public static final String RDP05_CONTRACT_VERSION = "U05-RDP05-REFROZEN-V1";
    private final String manifestRef;
    private final String setIdentity;
    private final String consultationId;
    private final String cdpId;
    private final int clinicalStateVersion;
    private final String evaluationContext;
    private final String rdp05ContractVersion;
    private final List<U05ReadinessInput> inputs;

    public U05ReadinessInputManifest(
            String manifestRef,
            String setIdentity,
            String consultationId,
            String cdpId,
            int clinicalStateVersion,
            String evaluationContext,
            String rdp05ContractVersion,
            List<U05ReadinessInput> inputs) {
        this.manifestRef = required(manifestRef, "manifestRef");
        String declaredSetIdentity = required(setIdentity, "setIdentity");
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (clinicalStateVersion < 0) throw new IllegalArgumentException("clinicalStateVersion must be non-negative");
        this.clinicalStateVersion = clinicalStateVersion;
        this.evaluationContext = required(evaluationContext, "evaluationContext");
        this.rdp05ContractVersion = required(rdp05ContractVersion, "rdp05ContractVersion");
        if (!RDP05_CONTRACT_VERSION.equals(this.rdp05ContractVersion)) {
            throw new IllegalArgumentException("unsupported RDP-05 contract version");
        }
        if (inputs == null || inputs.isEmpty()) throw new IllegalArgumentException("inputs are required");

        List<U05ReadinessInput> normalized = normalizeCanonicalInputs(inputs);
        this.inputs = Collections.unmodifiableList(normalized);
        this.setIdentity = semanticSetIdentity(
                this.consultationId,
                this.cdpId,
                this.clinicalStateVersion,
                this.evaluationContext,
                this.rdp05ContractVersion,
                this.inputs);
        if (!this.setIdentity.equals(declaredSetIdentity)) {
            throw new IllegalArgumentException("declared readiness input set identity does not match manifest content");
        }
        requireAtLeastOneDomain(U05ReadinessInput.F1);
        requireAtMostOneDomain(U05ReadinessInput.F2_CLARIFICATION);
        requireAtLeastOneDomain(U05ReadinessInput.F3);
        requireAtLeastOneDomain(U05ReadinessInput.F5);
        requireAtLeastOneDomain(U05ReadinessInput.F6);
    }

    public String getManifestRef() { return manifestRef; }
    public String getSetIdentity() { return setIdentity; }
    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getClinicalStateVersion() { return clinicalStateVersion; }
    public String getEvaluationContext() { return evaluationContext; }
    public String getRdp05ContractVersion() { return rdp05ContractVersion; }
    public List<U05ReadinessInput> getInputs() { return inputs; }

    public List<U05ReadinessInput> getInputs(String domain) {
        List<U05ReadinessInput> result = new ArrayList<U05ReadinessInput>();
        for (U05ReadinessInput input : inputs) {
            if (domain.equals(input.getSourceDomain())) result.add(input);
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> authoritativeRecordRefs() {
        List<String> refs = new ArrayList<String>();
        for (U05ReadinessInput input : inputs) {
            refs.add(input.getReadinessInputId());
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
        return semanticSetIdentity(
                consultationId, cdpId, clinicalStateVersion, evaluationContext, rdp05ContractVersion, inputs);
    }

    public static String semanticSetIdentity(
            String consultationId,
            String cdpId,
            int clinicalStateVersion,
            String evaluationContext,
            String rdp05ContractVersion,
            List<U05ReadinessInput> inputs) {
        List<U05ReadinessInput> normalized = normalizeCanonicalInputs(inputs);
        List<String> parts = new ArrayList<String>();
        parts.add(consultationId);
        parts.add(cdpId);
        parts.add(String.valueOf(clinicalStateVersion));
        parts.add(evaluationContext);
        parts.add(rdp05ContractVersion);
        for (U05ReadinessInput input : normalized) parts.add(input.semanticFingerprint());
        return U05Ids.hash("u05-input-set", parts.toArray(new String[parts.size()]));
    }

    private static List<U05ReadinessInput> normalizeCanonicalInputs(List<U05ReadinessInput> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("inputs are required");
        }
        List<U05ReadinessInput> sorted = new ArrayList<U05ReadinessInput>(source);
        Collections.sort(sorted, new Comparator<U05ReadinessInput>() {
            @Override
            public int compare(U05ReadinessInput left, U05ReadinessInput right) {
                int domain = left.getSourceDomain().compareTo(right.getSourceDomain());
                if (domain != 0) return domain;
                int inputId = left.getReadinessInputId().compareTo(right.getReadinessInputId());
                if (inputId != 0) return inputId;
                return left.semanticFingerprint().compareTo(right.semanticFingerprint());
            }
        });

        List<U05ReadinessInput> normalized = new ArrayList<U05ReadinessInput>();
        Set<String> exactSemanticRecords = new LinkedHashSet<String>();
        for (U05ReadinessInput input : sorted) {
            String fingerprint = input.semanticFingerprint();
            String semanticKey = input.getReadinessInputId() + "\u0000" + fingerprint;
            if (exactSemanticRecords.add(semanticKey)) {
                normalized.add(input);
            }
        }
        return normalized;
    }

    private void requireAtLeastOneDomain(String domain) {
        if (getInputs(domain).isEmpty()) {
            throw new IllegalArgumentException("manifest requires at least one slot for " + domain);
        }
    }

    private void requireAtMostOneDomain(String domain) {
        if (getInputs(domain).size() > 1) {
            throw new IllegalArgumentException("manifest permits at most one slot for " + domain);
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
