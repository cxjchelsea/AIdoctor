package com.aidoctor.diagnosis.runtime.u06;

public final class U06SyntheticPostF3SafetyBarrier {
    public static final String ALLOWED = "ALLOWED";
    public static final String BLOCKED = "BLOCKED";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    public Evaluation evaluate(String consultationId, String f3EffectId, String commitStatus,
                               int authoritativeReadBackVersion, Evidence evidence) {
        String status = evidence == null ? UNAVAILABLE : evidence.status;
        String evidenceRef = evidence == null ? "synthetic-safety-unavailable" : evidence.evidenceRef;
        if (!ALLOWED.equals(status) && !BLOCKED.equals(status) && !UNAVAILABLE.equals(status)) {
            throw new IllegalArgumentException("unsupported synthetic safety status");
        }
        String id = U06Ids.hash("u06safety", consultationId, f3EffectId, commitStatus,
                String.valueOf(authoritativeReadBackVersion), status, evidenceRef, "1");
        String fingerprint = U06Ids.hash("u06safetyfp", id, consultationId, f3EffectId, commitStatus,
                String.valueOf(authoritativeReadBackVersion), status, evidenceRef);
        return new Evaluation(id, fingerprint, status, evidenceRef, f3EffectId, authoritativeReadBackVersion);
    }

    public static final class Evidence {
        private final String status;
        private final String evidenceRef;

        public Evidence(String status, String evidenceRef) {
            this.status = required(status, "status");
            this.evidenceRef = required(evidenceRef, "evidenceRef");
        }

        public String getStatus() { return status; }
        public String getEvidenceRef() { return evidenceRef; }
    }

    public static final class Evaluation {
        private final String evaluationId;
        private final String fingerprint;
        private final String status;
        private final String evidenceRef;
        private final String f3EffectId;
        private final int stateVersion;

        Evaluation(String evaluationId, String fingerprint, String status, String evidenceRef,
                   String f3EffectId, int stateVersion) {
            this.evaluationId = evaluationId;
            this.fingerprint = fingerprint;
            this.status = status;
            this.evidenceRef = evidenceRef;
            this.f3EffectId = f3EffectId;
            this.stateVersion = stateVersion;
        }

        public String getEvaluationId() { return evaluationId; }
        public String getFingerprint() { return fingerprint; }
        public String getStatus() { return status; }
        public String getEvidenceRef() { return evidenceRef; }
        public String getF3EffectId() { return f3EffectId; }
        public int getStateVersion() { return stateVersion; }
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
