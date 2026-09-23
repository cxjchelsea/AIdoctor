package com.aidoctor.diagnosis.runtime.u06;

import java.util.LinkedHashMap;
import java.util.Map;

public final class U06SyntheticRevalidationAuthority {
    public static final String REPLAY_CONFLICT = "REVALIDATION_REPLAY_CONFLICT";
    public static final String STALE_BEFORE_PUBLISH = "STALE_BEFORE_PUBLISH";

    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    public synchronized Result evaluate(U06ProfileBRequest request, U06SyntheticDecisionBundle decision,
                                        int currentStateVersionAtPublish) {
        String id = required(decision.getRevalidationRef(), "revalidationRef");
        String status = required(decision.getRevalidationStatus(), "revalidationStatus");
        String fingerprint = U06Ids.hash("u06revalidationfp",
                request.getConsultationId(),
                request.getSourceAuthorityType(),
                request.getSourceAuthorityRef(),
                String.valueOf(request.getAuthoritativeClinicalStateVersion()),
                request.getDependencyBindingType(),
                request.getDependencyBindingRef(),
                request.getF3OwnerPolicyRef(),
                request.getCanonicalEventRef(),
                request.getBusinessEventIdentity(),
                status);

        Entry existing = entries.get(id);
        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint)) {
                return Result.failed(id, fingerprint, REPLAY_CONFLICT, false);
            }
            return new Result(id, fingerprint, existing.status, null, true);
        }

        if (currentStateVersionAtPublish != request.getAuthoritativeClinicalStateVersion()) {
            return Result.failed(id, fingerprint, STALE_BEFORE_PUBLISH, false);
        }

        entries.put(id, new Entry(fingerprint, status));
        return new Result(id, fingerprint, status, null, false);
    }

    private static final class Entry {
        final String fingerprint;
        final String status;
        Entry(String fingerprint, String status) { this.fingerprint = fingerprint; this.status = status; }
    }

    public static final class Result {
        private final String revalidationId;
        private final String fingerprint;
        private final String status;
        private final String failureCode;
        private final boolean replay;

        Result(String id, String fingerprint, String status, String failureCode, boolean replay) {
            this.revalidationId = id;
            this.fingerprint = fingerprint;
            this.status = status;
            this.failureCode = failureCode;
            this.replay = replay;
        }

        static Result failed(String id, String fingerprint, String code, boolean replay) {
            return new Result(id, fingerprint, U06SyntheticDecisionBundle.FAILED, code, replay);
        }

        public String getRevalidationId() { return revalidationId; }
        public String getFingerprint() { return fingerprint; }
        public String getStatus() { return status; }
        public String getFailureCode() { return failureCode; }
        public boolean isReplay() { return replay; }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
