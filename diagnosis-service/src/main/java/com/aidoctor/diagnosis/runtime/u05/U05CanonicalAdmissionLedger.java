package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedger;
import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedgerDecision;

/**
 * U05 adapter over the generic durable Canonical Effect Ledger.
 *
 * <p>Business semantics remain in U05. The shared ledger stores opaque bytes
 * and exact-effect identity only.</p>
 */
public final class U05CanonicalAdmissionLedger implements U05AdmissionLedger {
    public static final String NAMESPACE = "U05_ADMISSION";

    private final CanonicalEffectLedger ledger;

    public U05CanonicalAdmissionLedger(CanonicalEffectLedger ledger) {
        if (ledger == null) throw new IllegalArgumentException("ledger is required");
        this.ledger = ledger;
    }

    @Override
    public Entry reconcile(
            String admissionId,
            String fingerprint,
            U05AdmittedInput candidate) {
        CanonicalEffectLedgerDecision decision = ledger.createIfAbsent(
                NAMESPACE,
                admissionId,
                fingerprint,
                U05CanonicalEffectPayloadCodec.ADMISSION_SCHEMA,
                U05CanonicalEffectPayloadCodec.admission(candidate));

        if (CanonicalEffectLedgerDecision.Status.CREATED.equals(decision.getStatus())) {
            return new Entry(fingerprint, candidate, false);
        }
        if (CanonicalEffectLedgerDecision.Status.REATTACHED.equals(decision.getStatus())) {
            return new Entry(fingerprint, candidate, true);
        }
        throw new IllegalStateException(
                "U05_ADMISSION_LEDGER_" + decision.getStatus()
                        + (decision.getReasonCode() == null ? "" : "_" + decision.getReasonCode()));
    }
}
