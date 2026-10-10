package com.aidoctor.diagnosis.runtime.u07;

final class U07SyntheticInputValidator {
    private U07SyntheticInputValidator() { }
    static void validate(U07SyntheticInput in) {
        if (in == null || !id(in.eventId, "synthetic-d3-")
                || !id(in.idempotencyKey, "synthetic-d3-")
                || !id(in.consultationId, "synthetic-") || !id(in.questionId, "synthetic-")
                || !id(in.waitEffectId, "synthetic-") || in.expectedConsultationVersion < 0
                || in.digest == null || in.digest.trim().isEmpty() || in.digest.length() > 128) {
            throw new IllegalArgumentException("INVALID_SYNTHETIC_INPUT");
        }
    }
    private static boolean id(String value, String prefix) {
        return value != null && value.length() <= 128 && value.startsWith(prefix)
                && value.matches("[A-Za-z0-9._:-]+");
    }
}
