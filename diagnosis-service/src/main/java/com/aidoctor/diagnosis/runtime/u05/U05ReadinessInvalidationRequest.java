package com.aidoctor.diagnosis.runtime.u05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative dependency/currentness signal requesting governed readiness
 * invalidation. It carries prior readiness as read-only provenance; it does not
 * create a new Clinical Readiness business value.
 */
public final class U05ReadinessInvalidationRequest {
    private final String consultationId;
    private final String cdpId;
    private final int baseClinicalStateVersion;
    private final String priorReadinessRecordRef;
    private final String priorReadinessEffectId;
    private final Map<String, Object> priorReadinessPayload;
    private final String triggeringAuthoritativeChangeRef;
    private final List<String> affectedDependencyRefs;
    private final String invalidationReasonCode;
    private final String sourceEventOrDecisionRef;
    private final String correlationId;
    private final String traceId;
    private final String environmentId;
    private final String createdAt;

    public U05ReadinessInvalidationRequest(
            String consultationId,
            String cdpId,
            int baseClinicalStateVersion,
            String priorReadinessRecordRef,
            String priorReadinessEffectId,
            Map<String, Object> priorReadinessPayload,
            String triggeringAuthoritativeChangeRef,
            List<String> affectedDependencyRefs,
            String invalidationReasonCode,
            String sourceEventOrDecisionRef,
            String correlationId,
            String traceId,
            String environmentId,
            String createdAt) {
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        if (baseClinicalStateVersion < 0) throw new IllegalArgumentException("baseClinicalStateVersion must be non-negative");
        this.baseClinicalStateVersion = baseClinicalStateVersion;
        this.priorReadinessRecordRef = required(priorReadinessRecordRef, "priorReadinessRecordRef");
        this.priorReadinessEffectId = required(priorReadinessEffectId, "priorReadinessEffectId");
        if (priorReadinessPayload == null || priorReadinessPayload.isEmpty()) {
            throw new IllegalArgumentException("priorReadinessPayload is required");
        }
        this.priorReadinessPayload = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(priorReadinessPayload));
        this.triggeringAuthoritativeChangeRef = required(triggeringAuthoritativeChangeRef, "triggeringAuthoritativeChangeRef");
        if (affectedDependencyRefs == null || affectedDependencyRefs.isEmpty()) {
            throw new IllegalArgumentException("affectedDependencyRefs are required");
        }
        this.affectedDependencyRefs = Collections.unmodifiableList(new ArrayList<String>(affectedDependencyRefs));
        this.invalidationReasonCode = required(invalidationReasonCode, "invalidationReasonCode");
        this.sourceEventOrDecisionRef = required(sourceEventOrDecisionRef, "sourceEventOrDecisionRef");
        this.correlationId = required(correlationId, "correlationId");
        this.traceId = required(traceId, "traceId");
        this.environmentId = required(environmentId, "environmentId");
        this.createdAt = required(createdAt, "createdAt");

        requirePayload("readiness_record_id", priorReadinessRecordRef);
        requirePayload("effect_id", priorReadinessEffectId);
        requirePayload("state_validity", "CURRENT");
    }

    public String getConsultationId() { return consultationId; }
    public String getCdpId() { return cdpId; }
    public int getBaseClinicalStateVersion() { return baseClinicalStateVersion; }
    public String getPriorReadinessRecordRef() { return priorReadinessRecordRef; }
    public String getPriorReadinessEffectId() { return priorReadinessEffectId; }
    public Map<String, Object> getPriorReadinessPayload() { return new LinkedHashMap<String, Object>(priorReadinessPayload); }
    public String getTriggeringAuthoritativeChangeRef() { return triggeringAuthoritativeChangeRef; }
    public List<String> getAffectedDependencyRefs() { return affectedDependencyRefs; }
    public String getInvalidationReasonCode() { return invalidationReasonCode; }
    public String getSourceEventOrDecisionRef() { return sourceEventOrDecisionRef; }
    public String getCorrelationId() { return correlationId; }
    public String getTraceId() { return traceId; }
    public String getEnvironmentId() { return environmentId; }
    public String getCreatedAt() { return createdAt; }

    private void requirePayload(String field, String expected) {
        Object actual = priorReadinessPayload.get(field);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("prior readiness payload " + field + " mismatch");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
