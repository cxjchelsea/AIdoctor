package com.aidoctor.diagnosis.runtime.effects;

/** Typed storage/replay decision. No failure is silently converted to ABSENT. */
public final class CanonicalEffectLedgerDecision {
    public enum Status {
        ABSENT,
        CREATED,
        REATTACHED,
        CONFLICT,
        CORRUPT,
        UNAVAILABLE
    }

    private final Status status;
    private final CanonicalEffectLedgerRecord record;
    private final String reasonCode;

    private CanonicalEffectLedgerDecision(
            Status status,
            CanonicalEffectLedgerRecord record,
            String reasonCode) {
        this.status = status;
        this.record = record;
        this.reasonCode = reasonCode;
    }

    public static CanonicalEffectLedgerDecision absent() {
        return new CanonicalEffectLedgerDecision(Status.ABSENT, null, null);
    }

    public static CanonicalEffectLedgerDecision created(CanonicalEffectLedgerRecord record) {
        return new CanonicalEffectLedgerDecision(Status.CREATED, require(record), null);
    }

    public static CanonicalEffectLedgerDecision reattached(CanonicalEffectLedgerRecord record) {
        return new CanonicalEffectLedgerDecision(Status.REATTACHED, require(record), null);
    }

    public static CanonicalEffectLedgerDecision conflict(
            CanonicalEffectLedgerRecord record,
            String reasonCode) {
        return new CanonicalEffectLedgerDecision(Status.CONFLICT, record, required(reasonCode));
    }

    public static CanonicalEffectLedgerDecision corrupt(String reasonCode) {
        return new CanonicalEffectLedgerDecision(Status.CORRUPT, null, required(reasonCode));
    }

    public static CanonicalEffectLedgerDecision unavailable(String reasonCode) {
        return new CanonicalEffectLedgerDecision(Status.UNAVAILABLE, null, required(reasonCode));
    }

    public Status getStatus() { return status; }
    public CanonicalEffectLedgerRecord getRecord() { return record; }
    public String getReasonCode() { return reasonCode; }

    public boolean isSuccessfulRecord() {
        return Status.CREATED.equals(status) || Status.REATTACHED.equals(status);
    }

    private static CanonicalEffectLedgerRecord require(CanonicalEffectLedgerRecord record) {
        if (record == null) throw new IllegalArgumentException("record is required");
        return record;
    }

    private static String required(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        return value;
    }
}
