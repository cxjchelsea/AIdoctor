package com.aidoctor.diagnosis.runtime.u01.capability;

import com.aidoctor.diagnosis.client.C01U01CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityRequest;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal governed C01 invocation gateway for U01.
 *
 * Capability existence is not capability authorization. This gateway is default-off,
 * validates the frozen U01 binding, and accepts candidate results only.
 */
@Component
public class C01U01CapabilityGateway {
    public static final String BINDING_ID = "c01-u01-v1-active";
    public static final String CAPABILITY_ID = "C01";
    public static final String CAPABILITY_VERSION = "c01-u01-1.0.0";
    public static final String CAPABILITY_SET_VERSION = "aidoctor-v1-u01";
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
    private final boolean enabled;

    public C01U01CapabilityGateway(
            C01U01CapabilityClient client,
            @Value("${aidoctor.capability.c01-u01.enabled:false}") boolean enabled) {
        this.client = client;
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

        C01U01CapabilityRequest request = new C01U01CapabilityRequest(
                userId,
                rawText,
                consultationId,
                knownSubjectReferenceId,
                BINDING_ID,
                CAPABILITY_SET_VERSION,
                SCOPE_VERSION,
                CONTRACT_VERSION);

        final C01U01CapabilityResponse response;
        try {
            response = client.interpret(request);
        } catch (RuntimeException ex) {
            throw new C01CapabilityException(
                    "DEPENDENCY_FAILURE", true,
                    "C01/U01 capability invocation failed.", ex);
        }

        validateResponse(response);
        return response;
    }

    private void validateResponse(C01U01CapabilityResponse response) {
        if (response == null) {
            throw invalid("C01/U01 returned null response.");
        }
        if (!ACCEPTED_BUSINESS_STATUSES.contains(response.getBusinessStatus())) {
            throw new C01CapabilityException(
                    safeReason(response.getReasonCode(), "CAPABILITY_RESULT_NOT_USABLE"),
                    response.isRetryable(),
                    "C01/U01 returned non-usable business status: " + response.getBusinessStatus());
        }
        validateBinding(response.getBindingRef());
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

    private void validateBinding(C01U01CapabilityResponse.BindingRef binding) {
        if (binding == null
                || !BINDING_ID.equals(binding.getBindingId())
                || !"ACTIVE".equals(binding.getBindingStatus())
                || !CAPABILITY_ID.equals(binding.getCapabilityId())
                || !CAPABILITY_VERSION.equals(binding.getCapabilityVersion())
                || !CAPABILITY_SET_VERSION.equals(binding.getCapabilitySetVersion())
                || !SCOPE_VERSION.equals(binding.getScopeVersion())
                || !CONTRACT_VERSION.equals(binding.getContractVersion())) {
            throw invalid("C01/U01 CapabilityBindingRef mismatch.");
        }
    }

    private C01CapabilityException invalid(String message) {
        return new C01CapabilityException("INVALID_OUTPUT", false, message);
    }

    private static String safeReason(String reason, String fallback) {
        return reason == null || reason.trim().isEmpty() ? fallback : reason.trim();
    }
}
