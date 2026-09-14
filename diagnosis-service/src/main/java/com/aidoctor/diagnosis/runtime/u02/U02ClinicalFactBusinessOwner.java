package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic U02 owner boundary. It may accept candidate facts, but it does
 * not commit state and it does not reinterpret capability failure as a negative fact.
 */
@Component
public class U02ClinicalFactBusinessOwner {
    public static final String OWNER = "U02_CLINICAL_FACT_OWNER";
    private static final Set<String> ACCEPTABLE_VALUES = new HashSet<String>();

    static {
        ACCEPTABLE_VALUES.add("YES");
        ACCEPTABLE_VALUES.add("NO");
        ACCEPTABLE_VALUES.add("UNKNOWN");
        ACCEPTABLE_VALUES.add("UNMEASURED");
        ACCEPTABLE_VALUES.add("NOT_ASKED");
        ACCEPTABLE_VALUES.add("NOT_APPLICABLE");
    }

    public U02ClinicalFactDecision decide(
            String consultationId,
            String eventId,
            C01U02CapabilityResponse capabilityResult) {
        if (capabilityResult == null) {
            throw new IllegalArgumentException("capabilityResult is required");
        }
        List<C01U02CapabilityResponse.ObservationCandidate> candidates = capabilityResult.getObservationCandidates();
        List<C01U02CapabilityResponse.ObservationCandidate> accepted = new ArrayList<C01U02CapabilityResponse.ObservationCandidate>();
        if (candidates != null) {
            for (C01U02CapabilityResponse.ObservationCandidate candidate : candidates) {
                if (acceptable(candidate)) {
                    accepted.add(candidate);
                }
            }
        }
        boolean clarification = accepted.isEmpty();
        return new U02ClinicalFactDecision(
                "u02-decision-" + eventId,
                consultationId,
                eventId,
                OWNER,
                accepted,
                clarification,
                clarification ? "CLINICAL_FACTS_REQUIRE_CLARIFICATION" : "CLINICAL_FACTS_ACCEPTED");
    }

    private boolean acceptable(C01U02CapabilityResponse.ObservationCandidate candidate) {
        if (candidate == null
                || blank(candidate.getObservationId())
                || blank(candidate.getConceptId())
                || blank(candidate.getConceptDisplay())
                || blank(candidate.getRawTextRef())
                || !candidate.getRawTextRef().startsWith("sha256:")
                || !ACCEPTABLE_VALUES.contains(candidate.getValueSemantics())
                || blank(candidate.getSourceType())) {
            return false;
        }
        // UNCERTAIN can still be an explicit UNKNOWN/UNMEASURED fact; unresolved concepts cannot.
        if ("UNCERTAIN".equals(candidate.getLifecycle())) {
            return "UNKNOWN".equals(candidate.getValueSemantics())
                    || "UNMEASURED".equals(candidate.getValueSemantics());
        }
        return "NORMALIZED".equals(candidate.getLifecycle()) || "CONFIRMED".equals(candidate.getLifecycle());
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
