package com.aidoctor.contracts.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

public final class ClinicalTypes {
    private ClinicalTypes() {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TextObservationValue.class, name = "TEXT"),
        @JsonSubTypes.Type(value = NumberObservationValue.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = BooleanObservationValue.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = CodedObservationValue.class, name = "CODED"),
        @JsonSubTypes.Type(value = QuantityObservationValue.class, name = "QUANTITY"),
        @JsonSubTypes.Type(value = ReferenceObservationValue.class, name = "REFERENCE")
    })
    public abstract static class ObservationValue {
        @JsonProperty("kind")
        public String kind;
    }

    public static class TextObservationValue extends ObservationValue {
        @JsonProperty("value")
        public String value;
    }

    public static class NumberObservationValue extends ObservationValue {
        @JsonProperty("value")
        public Double value;
    }

    public static class BooleanObservationValue extends ObservationValue {
        @JsonProperty("value")
        public Boolean value;
    }

    public static class CodedObservationValue extends ObservationValue {
        @JsonProperty("code")
        public String code;
        @JsonProperty("system")
        public String system;
        @JsonProperty("display")
        public String display;
    }

    public static class QuantityObservationValue extends ObservationValue {
        @JsonProperty("value")
        public Double value;
        @JsonProperty("unit")
        public String unit;
    }

    public static class ReferenceObservationValue extends ObservationValue {
        @JsonProperty("reference_type")
        public String referenceType;
        @JsonProperty("reference_id")
        public String referenceId;
    }

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
        @JsonProperty("value") public ObservationValue value;
        @JsonProperty("recorded_time") public String recordedTime;
        @JsonProperty("effective_time") public String effectiveTime;
        @JsonProperty("status") public String status;
        @JsonProperty("sensitivity") public String sensitivity;
        @JsonProperty("provenance_refs") public List<String> provenanceRefs;
        @JsonProperty("evidence_refs") public List<String> evidenceRefs;
    }
}
