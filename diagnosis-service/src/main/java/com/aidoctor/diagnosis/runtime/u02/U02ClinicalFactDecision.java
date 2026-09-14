package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;

import java.util.Collections;
import java.util.List;

/** Business-owned interpretation result. Capability candidates are not Clinical Truth. */
public final class U02ClinicalFactDecision {
    private final String decisionId;
    private final String consultationId;
    private final String eventId;
    private final String businessOwner;
    private final List<C01U02CapabilityResponse.ObservationCandidate> acceptedObservations;
    private final boolean clarificationRequired;
    private final String reasonCode;

    public U02ClinicalFactDecision(
            String decisionId,
            String consultationId,
            String eventId,
            String businessOwner,
            List<C01U02CapabilityResponse.ObservationCandidate> acceptedObservations,
            boolean clarificationRequired,
            String reasonCode) {
        this.decisionId = decisionId;
        this.consultationId = consultationId;
        this.eventId = eventId;
        this.businessOwner = businessOwner;
        this.acceptedObservations = acceptedObservations == null
                ? Collections.<C01U02CapabilityResponse.ObservationCandidate>emptyList()
                : Collections.unmodifiableList(acceptedObservations);
        this.clarificationRequired = clarificationRequired;
        this.reasonCode = reasonCode;
    }

    public String getDecisionId() { return decisionId; }
    public String getConsultationId() { return consultationId; }
    public String getEventId() { return eventId; }
    public String getBusinessOwner() { return businessOwner; }
    public List<C01U02CapabilityResponse.ObservationCandidate> getAcceptedObservations() { return acceptedObservations; }
    public boolean isClarificationRequired() { return clarificationRequired; }
    public String getReasonCode() { return reasonCode; }
}
