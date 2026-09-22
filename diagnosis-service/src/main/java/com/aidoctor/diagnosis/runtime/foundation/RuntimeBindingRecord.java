package com.aidoctor.diagnosis.runtime.foundation;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Durable orchestration/version binding for one consultation.
 *
 * <p>This is runtime/governance metadata, not Clinical State. Once created it
 * must not silently switch authority or semantic versions for an in-flight
 * consultation.</p>
 */
@Entity
@Table(name = "clinical_runtime_binding")
@Getter
@NoArgsConstructor
public class RuntimeBindingRecord {
    public static final String LEGACY_FIXED_WORKFLOW = "LEGACY_FIXED_WORKFLOW";
    public static final String CLINICAL_RUNTIME_V1 = "CLINICAL_RUNTIME_V1";

    @Id
    @Column(name = "consultation_id", length = 128, nullable = false)
    private String consultationId;

    @Column(name = "cdp_id", length = 128, nullable = false)
    private String cdpId;

    @Column(name = "runtime_authority", length = 64, nullable = false)
    private String runtimeAuthority;

    @Column(name = "thread_id", length = 128, nullable = false)
    private String threadId;

    @Column(name = "scope_version", length = 64, nullable = false)
    private String scopeVersion;

    @Column(name = "capability_set_version", length = 64, nullable = false)
    private String capabilitySetVersion;

    @Column(name = "contract_version", length = 64, nullable = false)
    private String contractVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RuntimeBindingRecord(
            String consultationId,
            String cdpId,
            String runtimeAuthority,
            String threadId,
            String scopeVersion,
            String capabilitySetVersion,
            String contractVersion,
            LocalDateTime createdAt
    ) {
        this.consultationId = required(consultationId, "consultationId");
        this.cdpId = required(cdpId, "cdpId");
        this.runtimeAuthority = authority(runtimeAuthority);
        this.threadId = required(threadId, "threadId");
        this.scopeVersion = required(scopeVersion, "scopeVersion");
        this.capabilitySetVersion = required(capabilitySetVersion, "capabilitySetVersion");
        this.contractVersion = required(contractVersion, "contractVersion");
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public boolean sameBinding(
            String cdpId,
            String runtimeAuthority,
            String scopeVersion,
            String capabilitySetVersion,
            String contractVersion
    ) {
        return this.cdpId.equals(cdpId)
                && this.runtimeAuthority.equals(runtimeAuthority)
                && this.scopeVersion.equals(scopeVersion)
                && this.capabilitySetVersion.equals(capabilitySetVersion)
                && this.contractVersion.equals(contractVersion);
    }

    private static String authority(String value) {
        String normalized = required(value, "runtimeAuthority");
        if (!LEGACY_FIXED_WORKFLOW.equals(normalized) && !CLINICAL_RUNTIME_V1.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported runtime authority: " + normalized);
        }
        return normalized;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
