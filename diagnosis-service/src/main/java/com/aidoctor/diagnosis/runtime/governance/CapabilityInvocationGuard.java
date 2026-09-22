package com.aidoctor.diagnosis.runtime.governance;

import org.springframework.stereotype.Component;

/**
 * Common fail-closed validation boundary for clinical capability invocation.
 * Actual transport/model/tool execution remains owned by the concrete gateway.
 */
@Component
public class CapabilityInvocationGuard {
    private final BindingReleaseResolver resolver;

    public CapabilityInvocationGuard(BindingReleaseResolver resolver) {
        this.resolver = resolver;
    }

    public CapabilityBindingRecord authorize(
            String bindingId,
            String expectedCapabilityId,
            CapabilityExecutionContext context
    ) {
        return resolver.resolveCapabilityBinding(bindingId, expectedCapabilityId, context);
    }
}
