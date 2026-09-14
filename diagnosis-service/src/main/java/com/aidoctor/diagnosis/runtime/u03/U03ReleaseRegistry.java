package com.aidoctor.diagnosis.runtime.u03;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Component-level U03 release registry. Production registration remains a separate authorization concern. */
public final class U03ReleaseRegistry {
    private final Map<String, U03ReleaseBinding> byCapabilityBinding = new ConcurrentHashMap<String, U03ReleaseBinding>();

    public void register(U03ReleaseBinding binding) {
        if (binding == null) throw new IllegalArgumentException("binding is required");
        byCapabilityBinding.put(binding.getCapabilityBindingId(), binding);
    }

    public U03ReleaseBinding requireActive(String capabilityBindingId) {
        U03ReleaseBinding binding = byCapabilityBinding.get(capabilityBindingId);
        if (binding == null) throw new IllegalStateException("U03 release binding not found: " + capabilityBindingId);
        if (!binding.isActive()) throw new IllegalStateException("U03 release binding is inactive: " + capabilityBindingId);
        return binding;
    }
}
