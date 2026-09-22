package com.aidoctor.diagnosis.runtime.u03;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Component-level U03 release registry. Production registration remains a separate authorization concern. */
public final class U03ReleaseRegistry {
    private final Map<String, U03ReleaseBinding> byCapabilityBinding = new ConcurrentHashMap<String, U03ReleaseBinding>();
    private final Clock clock;

    public U03ReleaseRegistry() {
        this(Clock.systemUTC());
    }

    U03ReleaseRegistry(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock is required");
        this.clock = clock;
    }

    public void register(U03ReleaseBinding binding) {
        if (binding == null) throw new IllegalArgumentException("binding is required");
        U03ReleaseBinding existing = byCapabilityBinding.putIfAbsent(binding.getCapabilityBindingId(), binding);
        if (existing != null) {
            throw new IllegalStateException("U03 release binding already registered: " + binding.getCapabilityBindingId());
        }
    }

    public U03ReleaseBinding requireActive(String capabilityBindingId) {
        U03ReleaseBinding binding = byCapabilityBinding.get(capabilityBindingId);
        if (binding == null) throw new IllegalStateException("U03 release binding not found: " + capabilityBindingId);
        if (!binding.isActive()) throw new IllegalStateException("U03 release binding is inactive: " + capabilityBindingId);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!binding.isEffectiveAt(now)) throw new IllegalStateException("U03 release binding is not effective: " + capabilityBindingId);
        return binding;
    }
}
