package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public final class ClinicalTypes {
    private ClinicalTypes() { }

    public static class Encounter {
        @JsonProperty("contract_version") public String contractVersion;
        @JsonProperty("envelope") public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("encounter_id") public String encounterId;
        @JsonProperty("subject_ref") public String subjectRef;
        @JsonProperty("session_ref") public String sessionRef;
        @JsonProperty("lifecycle_status") public String lifecycleStatus;
        @JsonProperty("started_at") public String startedAt;
        @JsonProperty("updated_at") public String updatedAt;
        @JsonProperty("closed_at") public String closedAt;
        @JsonProperty("current_state_version") public Long currentStateVersion;
    }

    public static class ClinicalStateSnapshot {
        @JsonProperty("contract_version") public String contractVersion;
        @JsonProperty("envelope") public FoundationTypes.ContractEnvelope envelope;
        @JsonProperty("encounter_id") public String encounterId;
        @JsonProperty("state_version") public Long stateVersion;
        @JsonProperty("committed_at") public String committedAt;
        @JsonProperty("observations") public Map<String, ClinicalObservation> observations;
        @JsonProperty("audit_ref") public FoundationTypes.AuditRef auditRef;
        @JsonProperty("trace_ref") public FoundationTypes.TraceRef traceRef;
        @JsonProperty("evidence_refs") public List<String> evidenceRefs;
    }

    public static class ClinicalObservation {
        @JsonProperty("contract_version") public String contractVersion;
        @JsonProperty("observation_id") public String observationId;
        @JsonProperty("encounter_id") public String encounterId;
        @JsonProperty("subject_ref") public String subjectRef;
        @JsonProperty("value") public Object value;
        @JsonProperty("recorded_time") public String recordedTime;
        @JsonProperty("effective_time") public String effectiveTime;
        @JsonProperty("status") public String status;
        @JsonProperty("sensitivity") public String sensitivity;
        @JsonProperty("provenance_refs") public List<String> provenanceRefs;
        @JsonProperty("evidence_refs") public List<String> evidenceRefs;
    }
}
