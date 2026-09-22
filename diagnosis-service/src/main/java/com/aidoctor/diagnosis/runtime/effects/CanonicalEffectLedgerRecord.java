package com.aidoctor.diagnosis.runtime.effects;

import java.util.Arrays;

/** Immutable shared-ledger record. Business payload remains opaque bytes. */
public final class CanonicalEffectLedgerRecord {
    private final String namespace;
    private final String effectIdentity;
    private final String canonicalFingerprint;
    private final String recordSchemaVersion;
    private final byte[] payload;
    private final String payloadSha256;
    private final long createdAtEpochMillis;

    CanonicalEffectLedgerRecord(
            String namespace,
            String effectIdentity,
            String canonicalFingerprint,
            String recordSchemaVersion,
            byte[] payload,
            String payloadSha256,
            long createdAtEpochMillis) {
        this.namespace = namespace;
        this.effectIdentity = effectIdentity;
        this.canonicalFingerprint = canonicalFingerprint;
        this.recordSchemaVersion = recordSchemaVersion;
        this.payload = Arrays.copyOf(payload, payload.length);
        this.payloadSha256 = payloadSha256;
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    public String getNamespace() { return namespace; }
    public String getEffectIdentity() { return effectIdentity; }
    public String getCanonicalFingerprint() { return canonicalFingerprint; }
    public String getRecordSchemaVersion() { return recordSchemaVersion; }
    public byte[] getPayload() { return Arrays.copyOf(payload, payload.length); }
    public String getPayloadSha256() { return payloadSha256; }
    public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }

    boolean canonicalEquals(
            String expectedNamespace,
            String expectedEffectIdentity,
            String expectedFingerprint,
            String expectedSchemaVersion,
            String expectedPayloadSha256) {
        return namespace.equals(expectedNamespace)
                && effectIdentity.equals(expectedEffectIdentity)
                && canonicalFingerprint.equals(expectedFingerprint)
                && recordSchemaVersion.equals(expectedSchemaVersion)
                && payloadSha256.equals(expectedPayloadSha256);
    }
}
