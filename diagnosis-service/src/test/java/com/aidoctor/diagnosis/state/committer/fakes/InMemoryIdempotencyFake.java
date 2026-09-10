package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.IdempotencyPort;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** Thread-safe mechanical fake for reservation-race and retry tests. */
public final class InMemoryIdempotencyFake implements IdempotencyPort {
    private final Map<String, Record> records = new HashMap<String, Record>();
    private int inspectCalls;
    private int reserveCalls;
    private int completeCalls;
    private int releaseCalls;
    private boolean failNextInspect;
    private boolean failNextReserve;
    private boolean failNextComplete;
    private boolean failNextRelease;
    private final List<String> callOrder;

    public InMemoryIdempotencyFake() {
        this(new ArrayList<String>());
    }

    public InMemoryIdempotencyFake(List<String> callOrder) {
        this.callOrder = callOrder;
    }

    public synchronized int lookupCalls() {
        return inspectCalls;
    }

    public synchronized int inspectCalls() {
        return inspectCalls;
    }

    public synchronized int reserveCalls() {
        return reserveCalls;
    }

    public synchronized int rememberCalls() {
        return completeCalls;
    }

    public synchronized int completeCalls() {
        return completeCalls;
    }

    public synchronized int releaseCalls() {
        return releaseCalls;
    }

    public synchronized void failNextInspect() {
        failNextInspect = true;
    }

    public synchronized void failNextReserve() {
        failNextReserve = true;
    }

    public synchronized void failNextRemember() {
        failNextComplete = true;
    }

    public synchronized void failNextComplete() {
        failNextComplete = true;
    }

    public synchronized void failNextRelease() {
        failNextRelease = true;
    }

    @Override
    public synchronized Decision inspect(String idempotencyKey, String canonicalFingerprint) {
        inspectCalls++;
        callOrder.add("idempotency.inspect");
        if (failNextInspect) {
            failNextInspect = false;
            throw new IllegalStateException("synthetic idempotency inspection failure");
        }
        return decide(records.get(idempotencyKey), canonicalFingerprint);
    }

    @Override
    public synchronized Decision reserve(String idempotencyKey, String canonicalFingerprint) {
        reserveCalls++;
        callOrder.add("idempotency.reserve");
        if (failNextReserve) {
            failNextReserve = false;
            throw new IllegalStateException("synthetic idempotency reservation failure");
        }
        Record existing = records.get(idempotencyKey);
        if (existing != null) {
            return decide(existing, canonicalFingerprint);
        }
        records.put(idempotencyKey, new Record(canonicalFingerprint, null));
        return Decision.acquired();
    }

    @Override
    public synchronized void complete(
            String idempotencyKey,
            String canonicalFingerprint,
            StateTypes.CommitResult originalResult
    ) {
        completeCalls++;
        callOrder.add("idempotency.complete");
        Record existing = records.get(idempotencyKey);
        if (existing == null || !canonicalFingerprint.equals(existing.fingerprint)) {
            throw new IllegalStateException("reservation ownership mismatch");
        }
        if (failNextComplete) {
            failNextComplete = false;
            throw new IllegalStateException("synthetic idempotency completion failure");
        }
        records.put(idempotencyKey, new Record(canonicalFingerprint, originalResult));
    }

    @Override
    public synchronized void release(String idempotencyKey, String canonicalFingerprint) {
        releaseCalls++;
        callOrder.add("idempotency.release");
        if (failNextRelease) {
            failNextRelease = false;
            throw new IllegalStateException("synthetic idempotency release failure");
        }
        Record existing = records.get(idempotencyKey);
        if (existing != null && existing.result == null
                && canonicalFingerprint.equals(existing.fingerprint)) {
            records.remove(idempotencyKey);
        }
    }

    private static Decision decide(Record record, String fingerprint) {
        if (record == null) {
            return Decision.absent();
        }
        if (!fingerprint.equals(record.fingerprint)) {
            return Decision.mismatch();
        }
        if (record.result != null) {
            return Decision.completedSameFingerprint(record.result);
        }
        return Decision.reservedSameFingerprint();
    }

    private static final class Record {
        final String fingerprint;
        final StateTypes.CommitResult result;

        Record(String fingerprint, StateTypes.CommitResult result) {
            this.fingerprint = fingerprint;
            this.result = result;
        }
    }
}
