package com.aidoctor.diagnosis.runtime.u05;

/** U05-owned replay ledger boundary for stable admission identity. */
public interface U05AdmissionLedger {
    Entry find(String admissionId);
    void store(String admissionId, String fingerprint, U05AdmittedInput input);

    final class Entry {
        private final String fingerprint;
        private final U05AdmittedInput admittedInput;

        public Entry(String fingerprint, U05AdmittedInput admittedInput) {
            this.fingerprint = fingerprint;
            this.admittedInput = admittedInput;
        }

        public String getFingerprint() { return fingerprint; }
        public U05AdmittedInput getAdmittedInput() { return admittedInput; }
    }
}
