package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolved Gate-C release metadata for one controlled non-production U03 execution.
 *
 * <p>This object deliberately does not model publication or production activation.
 * It only proves that the exact frozen refs were resolved as one compatible set for
 * the current execution.</p>
 */
public final class U03ResolvedNonProductionReleaseSet {
    private final U03ExplicitNonProductionReleaseRefs refs;
    private final String capabilityBindingId;

    U03ResolvedNonProductionReleaseSet(
            U03ExplicitNonProductionReleaseRefs refs,
            String capabilityBindingId) {
        if (refs == null) throw new IllegalArgumentException("refs are required");
        if (capabilityBindingId == null || capabilityBindingId.trim().isEmpty()) {
            throw new IllegalArgumentException("capabilityBindingId is required");
        }
        refs.requireGateCFrozenSet();
        this.refs = refs;
        this.capabilityBindingId = capabilityBindingId;
    }

    public U03ExplicitNonProductionReleaseRefs getRefs() { return refs; }
    public String getCapabilityBindingId() { return capabilityBindingId; }

    /**
     * Adapter for the existing C02 provider contract. The returned metadata remains
     * non-production-only; active=false is intentional and must not be used with
     * requireActive().
     */
    public U03ReleaseBinding asCandidateReleaseBinding() {
        List<String> provenance = Collections.unmodifiableList(Arrays.asList(
                refs.getCoverageContractRef(),
                refs.getPolicyReleaseRef(),
                refs.getPolicyPairRef()));
        return new U03ReleaseBinding(
                capabilityBindingId,
                refs.getRuleReleaseRef(),
                "0.2.1-candidate",
                refs.getKnowledgeReleaseRef(),
                "0.1.0-candidate",
                U03ReleaseBinding.ANY,
                U03ReleaseBinding.ANY,
                U03ReleaseBinding.ANY,
                U03ReleaseBinding.ANY,
                java.time.LocalDateTime.MIN,
                null,
                provenance,
                false);
    }

    public boolean isCompatibleWith(CapabilityBindingRecord capabilityBinding) {
        return capabilityBinding != null
                && capabilityBindingId.equals(capabilityBinding.getBindingId())
                && asCandidateReleaseBinding().isCompatibleWith(capabilityBinding);
    }
}
