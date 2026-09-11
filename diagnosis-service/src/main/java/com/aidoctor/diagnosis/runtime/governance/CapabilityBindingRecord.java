package com.aidoctor.diagnosis.runtime.governance;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Durable P06 capability binding metadata.
 *
 * <p>This record governs whether a capability version may be invoked for a
 * concrete execution context. It is governance/runtime metadata and never
 * Clinical Truth.</p>
 */
@Entity
@Table(name = "clinical_capability_binding")
@Getter
@NoArgsConstructor
public class CapabilityBindingRecord {
    public static final String ACTIVE = "ACTIVE";
    public static final String DISABLED = "DISABLED";
    public static final String EXPIRED = "EXPIRED";
    public static final String ANY = "*";

    @Id
    @Column(name = "binding_id", length = 128, nullable = false)
    private String bindingId;

    @Column(name = "capability_id", length = 64, nullable = false)
    private String capabilityId;

    @Column(name = "capability_version", length = 64, nullable = false)
    private String capabilityVersion;

    @Column(name = "capability_set_version", length = 64, nullable = false)
    private String capabilitySetVersion;

    @Column(name = "scope_version", length = 64, nullable = false)
    private String scopeVersion;

    @Column(name = "contract_version", length = 64, nullable = false)
    private String contractVersion;

    @Column(name = "population_scope", length = 64, nullable = false)
    private String populationScope;

    @Column(name = "region_scope", length = 64, nullable = false)
    private String regionScope;

    @Column(name = "language_scope", length = 32, nullable = false)
    private String languageScope;

    @Column(name = "channel_scope", length = 32, nullable = false)
    private String channelScope;

    @Column(name = "binding_status", length = 32, nullable = false)
    private String bindingStatus;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CapabilityBindingRecord(
            String bindingId,
            String capabilityId,
            String capabilityVersion,
            String capabilitySetVersion,
            String scopeVersion,
            String contractVersion,
            String populationScope,
            String regionScope,
            String languageScope,
            String channelScope,
            String bindingStatus,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveUntil,
            LocalDateTime createdAt
    ) {
        this.bindingId = required(bindingId, "bindingId");
        this.capabilityId = required(capabilityId, "capabilityId");
        this.capabilityVersion = required(capabilityVersion, "capabilityVersion");
        this.capabilitySetVersion = required(capabilitySetVersion, "capabilitySetVersion");
        this.scopeVersion = required(scopeVersion, "scopeVersion");
        this.contractVersion = required(contractVersion, "contractVersion");
        this.populationScope = defaultAny(populationScope);
        this.regionScope = defaultAny(regionScope);
        this.languageScope = defaultAny(languageScope);
        this.channelScope = defaultAny(channelScope);
        this.bindingStatus = status(bindingStatus);
        this.effectiveFrom = effectiveFrom == null ? LocalDateTime.now() : effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        if (this.effectiveUntil != null && this.effectiveUntil.isBefore(this.effectiveFrom)) {
            throw new IllegalArgumentException("effectiveUntil cannot be before effectiveFrom");
        }
    }

    public boolean isEffectiveAt(LocalDateTime now) {
        return !now.isBefore(effectiveFrom)
                && (effectiveUntil == null || now.isBefore(effectiveUntil));
    }

    public boolean matches(CapabilityExecutionContext context) {
        return scopeVersion.equals(context.getScopeVersion())
                && contractVersion.equals(context.getContractVersion())
                && matchesScope(populationScope, context.getPopulation())
                && matchesScope(regionScope, context.getRegion())
                && matchesScope(languageScope, context.getLanguage())
                && matchesScope(channelScope, context.getChannel());
    }

    private static boolean matchesScope(String configured, String actual) {
        return ANY.equals(configured) || configured.equals(actual);
    }

    private static String defaultAny(String value) {
        return value == null || value.trim().isEmpty() ? ANY : value;
    }

    private static String status(String value) {
        String normalized = required(value, "bindingStatus");
        if (!ACTIVE.equals(normalized) && !DISABLED.equals(normalized) && !EXPIRED.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported binding status: " + normalized);
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
