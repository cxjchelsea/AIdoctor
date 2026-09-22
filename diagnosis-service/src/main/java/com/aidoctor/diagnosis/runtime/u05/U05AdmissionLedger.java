package com.aidoctor.diagnosis.runtime.u05;

/** U05-owned replay ledger boundary for stable admission identity. */
public interface U05AdmissionLedger {

    Entry reconcile(String admissionId, String fingerprint, U05AdmittedInput candidate);

    final class Entry {
        private final String fingerprint;
        private final U05AdmittedInput admittedInput;
        private final boolean reattached;

        public Entry(
                String fingerprint,
                U05AdmittedInput admittedInput,
                boolean reattached) {
            if (fingerprint == null || fingerprint.trim().isEmpty()) {
                throw new IllegalArgumentException("fingerprint is required");
            }
            if (admittedInput == null) {
                throw new IllegalArgumentException("admittedInput is required");
            }
            this.fingerprint = fingerprint;
            this.admittedInput = admittedInput;
            this.reattached = reattached;
        }

        public String getFingerprint() { return fingerprint; }
        public U05AdmittedInput getAdmittedInput() { return admittedInput; }
        public boolean isReattached() { return reattached; }
    }
}
