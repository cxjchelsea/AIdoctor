package com.aidoctor.diagnosis.runtime.governance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Minimal authoritative write boundary for P06 capability bindings.
 *
 * <p>Registration is idempotent only when the full binding definition is the
 * same. Existing binding identities cannot be silently repointed to another
 * capability/version/scope. Status changes are one-way in this minimal
 * foundation; reactivation requires a new binding identity.</p>
 */
@Service
public class CapabilityBindingRegistry {
    private final CapabilityBindingRepository repository;

    public CapabilityBindingRegistry(CapabilityBindingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CapabilityBindingRecord register(CapabilityBindingRecord candidate) {
        Optional<CapabilityBindingRecord> existing = repository.findById(candidate.getBindingId());
        if (existing.isPresent()) {
            if (!existing.get().sameDefinition(candidate)) {
                throw new IllegalStateException(
                        "Capability binding identity is immutable once registered: " + candidate.getBindingId());
            }
            return existing.get();
        }
        return repository.save(candidate);
    }

    @Transactional
    public CapabilityBindingRecord disable(String bindingId) {
        CapabilityBindingRecord binding = require(bindingId);
        binding.disable();
        return repository.save(binding);
    }

    @Transactional
    public CapabilityBindingRecord expire(String bindingId) {
        CapabilityBindingRecord binding = require(bindingId);
        binding.expire();
        return repository.save(binding);
    }

    public CapabilityBindingRecord require(String bindingId) {
        return repository.findById(bindingId)
                .orElseThrow(() -> new IllegalStateException("Capability binding not found: " + bindingId));
    }
}
