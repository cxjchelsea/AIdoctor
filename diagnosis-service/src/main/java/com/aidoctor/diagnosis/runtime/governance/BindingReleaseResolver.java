package com.aidoctor.diagnosis.runtime.governance;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Phase-9 Binding & Release Resolver core.
 *
 * <p>Foundation-1 intentionally resolves only the capability binding and
 * contract/scope compatibility required by current consumers. Knowledge and
 * rule release resolution remain explicit extension points for their first
 * real consumers.</p>
 */
@Service
public class BindingReleaseResolver {
    private final CapabilityBindingRepository repository;
    private final Clock clock;

    public BindingReleaseResolver(CapabilityBindingRepository repository) {
        this(repository, Clock.systemUTC());
    }

    BindingReleaseResolver(CapabilityBindingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CapabilityBindingRecord resolveCapabilityBinding(
            String bindingId,
            String expectedCapabilityId,
            CapabilityExecutionContext context
    ) {
        CapabilityBindingRecord binding = repository.findById(required(bindingId, "bindingId"))
                .orElseThrow(() -> new IllegalStateException("Capability binding not found: " + bindingId));

        if (!CapabilityBindingRecord.ACTIVE.equals(binding.getBindingStatus())) {
            throw new IllegalStateException("Capability binding is not active: " + bindingId);
        }
        if (!binding.getCapabilityId().equals(required(expectedCapabilityId, "expectedCapabilityId"))) {
            throw new IllegalStateException("Capability binding does not match requested capability: " + bindingId);
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!binding.isEffectiveAt(now)) {
            throw new IllegalStateException("Capability binding is outside its effective window: " + bindingId);
        }
        if (!binding.matches(context)) {
            throw new IllegalStateException("Capability binding is incompatible with execution context: " + bindingId);
        }
        return binding;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
