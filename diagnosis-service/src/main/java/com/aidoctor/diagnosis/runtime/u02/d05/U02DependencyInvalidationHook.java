package com.aidoctor.diagnosis.runtime.u02.d05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimum D05 hook for U02. It does not own Risk/DDx/Workup/Delivery state;
 * it only converts already-registered dependencies into traceable invalidation records.
 */
public final class U02DependencyInvalidationHook {
    public List<InvalidationRecord> invalidate(
            String consultationId,
            String sourceEventId,
            String sourceFactRef,
            int targetClinicalStateVersion,
            List<DependentArtifact> registeredDependencies) {
        if (registeredDependencies == null || registeredDependencies.isEmpty()) {
            return Collections.emptyList();
        }
        List<InvalidationRecord> result = new ArrayList<InvalidationRecord>();
        for (DependentArtifact dependency : registeredDependencies) {
            if (dependency == null) continue;
            result.add(new InvalidationRecord(
                    consultationId,
                    sourceEventId,
                    sourceFactRef,
                    dependency.artifactRef,
                    statusFor(dependency.artifactType),
                    "UPSTREAM_CLINICAL_FACT_CHANGED",
                    targetClinicalStateVersion));
        }
        return Collections.unmodifiableList(result);
    }

    private static String statusFor(String artifactType) {
        if ("RISK".equals(artifactType)) return "STALE";
        if ("DDX".equals(artifactType)) return "INVALIDATED";
        if ("WORKUP".equals(artifactType) || "DELIVERY".equals(artifactType)) return "SUPERSEDED";
        throw new IllegalArgumentException("Unsupported U02/D05 dependent artifact type: " + artifactType);
    }

    public static final class DependentArtifact {
        public final String artifactType;
        public final String artifactRef;

        public DependentArtifact(String artifactType, String artifactRef) {
            this.artifactType = artifactType;
            this.artifactRef = artifactRef;
        }
    }

    public static final class InvalidationRecord {
        public final String consultationId;
        public final String sourceEventId;
        public final String sourceFactRef;
        public final String targetArtifactRef;
        public final String targetStatus;
        public final String reasonCode;
        public final int targetClinicalStateVersion;

        InvalidationRecord(
                String consultationId,
                String sourceEventId,
                String sourceFactRef,
                String targetArtifactRef,
                String targetStatus,
                String reasonCode,
                int targetClinicalStateVersion) {
            this.consultationId = consultationId;
            this.sourceEventId = sourceEventId;
            this.sourceFactRef = sourceFactRef;
            this.targetArtifactRef = targetArtifactRef;
            this.targetStatus = targetStatus;
            this.reasonCode = reasonCode;
            this.targetClinicalStateVersion = targetClinicalStateVersion;
        }
    }
}
