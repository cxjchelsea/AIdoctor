package com.aidoctor.diagnosis.runtime.u01.capability;

import com.aidoctor.diagnosis.client.C01U01CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityRequest;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal governed C01 invocation gateway for U01.
 *
 * <p>Capability existence is not capability authorization. The local feature flag remains
 * default-off, but an enabled composition still must pass the authoritative Foundation-1
 * CapabilityInvocationGuard before any clinical capability call is made. Capability output
 * is candidate-only and its returned binding reference must match the binding that was
 * actually authorized for this invocation.</p>
 */
@Component
public class C01U01CapabilityGateway {
    public static final String BINDING_ID = "c01-u01-v1-active";
    public static final String CAPABILITY_ID = "C01";
    public static final String SCOPE_VERSION = "aidoctor-v1-scope";
    public static final String CONTRACT_VERSION = "contracts-v1";

    private static final Set<String> ACCEPTED_BUSINESS_STATUSES =
            new HashSet<>(Arrays.asList("SUCCESS", "INSUFFICIENT_INFORMATION"));
    private static final Set<String> SUBJECT_TYPES =
            new HashSet<>(Arrays.asList("SELF", "OTHER", "UNKNOWN"));
    private static final Set<String> SCOPES =
            new HashSet<>(Arrays.asList(
                    "SYMPTOM", "EXAMINATION", "COMPREHENSIVE", "CLINICAL_CONSULTATION",
                    "OUTSIDE_V1_INTENT", "MIXED", "UNKNOWN"));

    private final C01U01CapabilityClient client;
    private final CapabilityInvocationGuard invocationGuard;
    private final boolean enabled;

    public C01U01CapabilityGateway(
            C01U01CapabilityClient client,
            CapabilityInvocationGuard invocationGuard,
            @Value("${aidoctor.capability.c01-u01.enabled:false}") boolean enabled) {
        this.client = client;
        this.invocationGuard = invocationGuard;
        this.enabled = enabled;
    }

    public C01U01CapabilityResponse interpret(
            String userId,
            String rawText,
            String consultationId,
            String knownSubjectReferenceId) {
        if (!enabled) {
            throw new C01CapabilityException(
                    "CAPABILITY_BINDING_INACTIVE", false,
                    "C01/U01 capability binding is not active for this runtime composition.");
        }

        final CapabilityBindingRecord authorizedBinding;
        try {
            authorizedBinding = invocationGuard.authorize(
                    BINDING_ID,
                    CAPABILITY_ID,
                    new CapabilityExecutionContext(
                            SCOPE_VERSION,
                            CONTRACT_VERSION,
                            CapabilityBindingRecord.ANY,
                            CapabilityBindingRecord.ANY,
                            CapabilityBindingRecord.ANY,
                            CapabilityBindingRecord.ANY));
        } catch (RuntimeException ex) {
            throw new C01CapabilityException(
                    "CAPABILITY_BINDING_NOT_AUTHORIZED", false,
                    "C01/U01 authoritative capability binding validation failed.", ex);
        }

        C01U01CapabilityRequest request = new C01U01CapabilityRequest(
                userId,
                rawText,
                consultationId,
                knownSubjectReferenceId,
                authorizedBinding.getBindingId(),
                authorizedBinding.getCapabilitySetVersion(),
                authorizedBinding.getScopeVersion(),
                authorizedBinding.getContractVersion());

        final C01U01CapabilityResponse response;
        try {
            response = client.interpret(request);
        } catch (RuntimeException ex) {
            throw new C01CapabilityException(
                    "DEPENDENCY_FAILURE", true,
                    "C01/U01 capability invocation failed.", ex);
        }

        validateResponse(response, authorizedBinding);
        return response;
    }

    private void validateResponse(
            C01U01CapabilityResponse response,
            CapabilityBindingRecord authorizedBinding) {
        if (response == null) {
            throw invalid("C01/U01 returned null response.");
        }
        if (!ACCEPTED_BUSINESS_STATUSES.contains(response.getBusinessStatus())) {
            throw new C01CapabilityException(
                    safeReason(response.getReasonCode(), "CAPABILITY_RESULT_NOT_USABLE"),
                    response.isRetryable(),
                    "C01/U01 returned non-usable business status: " + response.getBusinessStatus());
        }
        validateBinding(response.getBindingRef(), authorizedBinding);
        if (response.getSubjectCandidate() == null
                || response.getProblemCandidate() == null
                || response.getScopeCandidate() == null
                || response.getEarlySafetySignalCandidate() == null) {
            throw invalid("C01/U01 candidate envelope is incomplete.");
        }
        if (!SUBJECT_TYPES.contains(response.getSubjectCandidate().getSubjectType())) {
            throw invalid("Unsupported C01 subject candidate: "
                    + response.getSubjectCandidate().getSubjectType());
        }
        if (!SCOPES.contains(response.getScopeCandidate().getScope())) {
            throw invalid("Unsupported C01 scope candidate: "
                    + response.getScopeCandidate().getScope());
        }
        if (!"USER_TEXT".equals(response.getSourceAttribution())) {
            throw invalid("C01/U01 source attribution must remain USER_TEXT for this slice.");
        }
    }

    private void validateBinding(
            C01U01CapabilityResponse.BindingRef binding,
            CapabilityBindingRecord authorizedBinding) {
        if (binding == null
                || !authorizedBinding.getBindingId().equals(binding.getBindingId())
                || !authorizedBinding.getBindingStatus().equals(binding.getBindingStatus())
                || !authorizedBinding.getCapabilityId().equals(binding.getCapabilityId())
                || !authorizedBinding.getCapabilityVersion().equals(binding.getCapabilityVersion())
                || !authorizedBinding.getCapabilitySetVersion().equals(binding.getCapabilitySetVersion())
                || !authorizedBinding.getScopeVersion().equals(binding.getScopeVersion())
                || !authorizedBinding.getContractVersion().equals(binding.getContractVersion())) {
            throw invalid("C01/U01 CapabilityBindingRef does not match the authorized binding.");
        }
    }

    private C01CapabilityException invalid(String message) {
        return new C01CapabilityException("INVALID_OUTPUT", false, message);
    }

    private static String safeReason(String reason, String fallback) {
        return reason == null || reason.trim().isEmpty() ? fallback : reason.trim();
    }
}
