package com.aidoctor.diagnosis.runtime.u02.capability;

import com.aidoctor.diagnosis.client.C01U02CapabilityClient;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityRequest;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityBindingRecord;
import com.aidoctor.diagnosis.runtime.governance.CapabilityExecutionContext;
import com.aidoctor.diagnosis.runtime.governance.CapabilityInvocationGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Governed C01 invocation gateway for U02.
 * Capability output remains candidate-only; authorization comes from P06/Foundation-1.
 */
@Component
public class C01U02CapabilityGateway {
    public static final String BINDING_ID = "c01-u02-v1-active";
    public static final String CAPABILITY_ID = "C01";
    public static final String SCOPE_VERSION = "aidoctor-v1-scope";
    public static final String CONTRACT_VERSION = "contracts-v1";

    private static final Set<String> ACCEPTED_BUSINESS_STATUSES =
            new HashSet<String>(Arrays.asList("SUCCESS", "INSUFFICIENT_INFORMATION"));
    private static final Set<String> VALUE_SEMANTICS =
            new HashSet<String>(Arrays.asList("YES", "NO", "UNKNOWN", "UNMEASURED", "NOT_ASKED", "NOT_APPLICABLE"));
    private static final Set<String> LIFECYCLES =
            new HashSet<String>(Arrays.asList("EXTRACTED", "NORMALIZED", "CONFIRMED", "UNCERTAIN", "CONTRADICTED", "INVALIDATED"));

    private final C01U02CapabilityClient client;
    private final CapabilityInvocationGuard invocationGuard;
    private final boolean enabled;

    public C01U02CapabilityGateway(
            C01U02CapabilityClient client,
            CapabilityInvocationGuard invocationGuard,
            @Value("${aidoctor.capability.c01-u02.enabled:false}") boolean enabled) {
        this.client = client;
        this.invocationGuard = invocationGuard;
        this.enabled = enabled;
    }

    public GovernedResult interpret(
            String userId,
            String rawText,
            String consultationId,
            String eventId,
            String sourceType) {
        if (!enabled) {
            throw new C01U02CapabilityException(
                    "CAPABILITY_BINDING_INACTIVE", false,
                    "C01/U02 capability binding is not active for this runtime composition.");
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
            throw new C01U02CapabilityException(
                    "CAPABILITY_BINDING_NOT_AUTHORIZED", false,
                    "C01/U02 authoritative capability binding validation failed.", ex);
        }

        C01U02CapabilityRequest request = new C01U02CapabilityRequest(
                userId,
                rawText,
                consultationId,
                eventId,
                sourceType,
                authorizedBinding.getBindingId(),
                authorizedBinding.getCapabilitySetVersion(),
                authorizedBinding.getScopeVersion(),
                authorizedBinding.getContractVersion());

        final C01U02CapabilityResponse response;
        try {
            response = client.interpret(request);
        } catch (RuntimeException ex) {
            throw new C01U02CapabilityException(
                    "DEPENDENCY_FAILURE", true,
                    "C01/U02 capability invocation failed.", ex);
        }

        validateResponse(response, authorizedBinding);
        return new GovernedResult(response, authorizedBinding);
    }

    private void validateResponse(
            C01U02CapabilityResponse response,
            CapabilityBindingRecord authorizedBinding) {
        if (response == null) {
            throw invalid("C01/U02 returned null response.");
        }
        if (!ACCEPTED_BUSINESS_STATUSES.contains(response.getBusinessStatus())) {
            throw new C01U02CapabilityException(
                    safeReason(response.getReasonCode(), "CAPABILITY_RESULT_NOT_USABLE"),
                    response.isRetryable(),
                    "C01/U02 returned non-usable business status: " + response.getBusinessStatus());
        }
        validateBinding(response.getBindingRef(), authorizedBinding);
        if (!"USER_TEXT".equals(response.getSourceAttribution())) {
            throw invalid("C01/U02 source attribution must remain USER_TEXT for this slice.");
        }
        List<C01U02CapabilityResponse.ObservationCandidate> observations = response.getObservationCandidates();
        if (observations == null) {
            throw invalid("C01/U02 observationCandidates is required.");
        }
        if ("SUCCESS".equals(response.getBusinessStatus()) && observations.isEmpty()) {
            throw invalid("C01/U02 SUCCESS must contain at least one Observation Candidate.");
        }
        for (C01U02CapabilityResponse.ObservationCandidate observation : observations) {
            validateObservation(observation);
        }
    }

    private void validateObservation(C01U02CapabilityResponse.ObservationCandidate observation) {
        if (observation == null
                || blank(observation.getObservationId())
                || blank(observation.getConceptDisplay())
                || blank(observation.getRawTextRef())
                || !observation.getRawTextRef().startsWith("sha256:")
                || !VALUE_SEMANTICS.contains(observation.getValueSemantics())
                || !LIFECYCLES.contains(observation.getLifecycle())
                || blank(observation.getSourceType())
                || observation.getConfidenceOrUncertainty() < 0.0d
                || observation.getConfidenceOrUncertainty() > 1.0d) {
            throw invalid("C01/U02 returned malformed Observation Candidate.");
        }
    }

    private void validateBinding(
            C01U02CapabilityResponse.BindingRef binding,
            CapabilityBindingRecord authorizedBinding) {
        if (binding == null
                || !authorizedBinding.getBindingId().equals(binding.getBindingId())
                || !authorizedBinding.getBindingStatus().equals(binding.getBindingStatus())
                || !authorizedBinding.getCapabilityId().equals(binding.getCapabilityId())
                || !authorizedBinding.getCapabilityVersion().equals(binding.getCapabilityVersion())
                || !authorizedBinding.getCapabilitySetVersion().equals(binding.getCapabilitySetVersion())
                || !authorizedBinding.getScopeVersion().equals(binding.getScopeVersion())
                || !authorizedBinding.getContractVersion().equals(binding.getContractVersion())) {
            throw invalid("C01/U02 CapabilityBindingRef does not match the authorized binding.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private C01U02CapabilityException invalid(String message) {
        return new C01U02CapabilityException("INVALID_OUTPUT", false, message);
    }

    private static String safeReason(String reason, String fallback) {
        return blank(reason) ? fallback : reason.trim();
    }

    public static final class GovernedResult {
        private final C01U02CapabilityResponse response;
        private final CapabilityBindingRecord authorizedBinding;

        GovernedResult(C01U02CapabilityResponse response, CapabilityBindingRecord authorizedBinding) {
            this.response = response;
            this.authorizedBinding = authorizedBinding;
        }

        public C01U02CapabilityResponse getResponse() {
            return response;
        }

        public CapabilityBindingRecord getAuthorizedBinding() {
            return authorizedBinding;
        }
    }
}
