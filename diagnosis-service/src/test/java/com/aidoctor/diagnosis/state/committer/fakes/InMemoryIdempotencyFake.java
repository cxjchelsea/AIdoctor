package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.ports.IdempotencyPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryIdempotencyFake implements IdempotencyPort {
    private final Map<String, IdempotencyRecord> records = new HashMap<String, IdempotencyRecord>();
    private int lookupCalls;
    private int rememberCalls;
    private boolean failNextRemember;

    public int lookupCalls() {
        return lookupCalls;
    }

    public int rememberCalls() {
        return rememberCalls;
    }

    public void failNextRemember() {
        failNextRemember = true;
    }

    @Override
    public Optional<IdempotencyRecord> lookup(String idempotencyKey) {
        lookupCalls++;
        return Optional.ofNullable(records.get(idempotencyKey));
    }

    @Override
    public void remember(String idempotencyKey, String canonicalFingerprint, StateTypes.CommitResult originalResult) {
        rememberCalls++;
        if (failNextRemember) {
            failNextRemember = false;
            throw new IllegalStateException("synthetic idempotency remember failure");
        }
        records.put(idempotencyKey, new IdempotencyRecord(canonicalFingerprint, originalResult));
    }
}
