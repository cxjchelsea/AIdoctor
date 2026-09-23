package com.aidoctor.diagnosis.runtime.effects;

/**
 * Generic non-clinical durable exact-effect ledger.
 *
 * <p>The ledger owns immutable runtime/governance effect records only. It is
 * not Clinical State, business policy, Scheduler state, or downstream effect
 * execution authority.</p>
 */
public interface CanonicalEffectLedger {

    CanonicalEffectLedgerDecision inspect(
            String namespace,
            String effectIdentity,
            String expectedCanonicalFingerprint);

    CanonicalEffectLedgerDecision createIfAbsent(
            String namespace,
            String effectIdentity,
            String canonicalFingerprint,
            String recordSchemaVersion,
            byte[] immutableRecordBytes);
}
