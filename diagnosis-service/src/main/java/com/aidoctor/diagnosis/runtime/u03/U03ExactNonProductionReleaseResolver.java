package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;

/**
 * Resolves only the Gate-C-frozen U03 release set for controlled non-production use.
 *
 * <p>No latest/current fallback, publication, or production activation is performed.</p>
 */
public final class U03ExactNonProductionReleaseResolver {
    public U03ResolvedNonProductionReleaseSet resolve(
            U03NonProductionExecutionContext context,
            CapabilityBindingRecord capabilityBinding) {
        if (context == null) throw new IllegalArgumentException("context is required");
        if (capabilityBinding == null) throw new IllegalArgumentException("capabilityBinding is required");
        if (!U03NonProductionExecutionContext.BINDING_MODE.equals(context.getBindingMode())) {
            throw new IllegalStateException("unsupported U03 runtime binding mode");
        }

        U03ExplicitNonProductionReleaseRefs refs = context.getReleaseRefs();
        refs.requireGateCFrozenSet();

        U03ResolvedNonProductionReleaseSet resolved =
                new U03ResolvedNonProductionReleaseSet(refs, capabilityBinding.getBindingId());
        if (!resolved.isCompatibleWith(capabilityBinding)) {
            throw new IllegalStateException(
                    "U03 exact release set is not compatible with authorized capability binding");
        }
        return resolved;
    }
}
