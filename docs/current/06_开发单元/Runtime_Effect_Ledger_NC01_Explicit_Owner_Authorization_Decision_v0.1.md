# Runtime Canonical Effect Ledger NC-01 Explicit Owner Authorization Decision v0.1

> Authorization ID: `AUTH-RUNTIME-EFFECT-LEDGER-NC-001`  
> Decision type: **Repository Owner Explicit Implementation Authorization**  
> Exact reviewed design head: `0a275d461ac6001d6cede9e4d46fb36d6a7f50de`  
> Design PR: #185  
> Independent targeted design review: PASS / review_id `5273185469`  
> Impact review: PR #183 / PASS / review_id `5265060541`  
> Current status: **OWNER_DECISION_PENDING**  
> This package itself grants no implementation authorization.

---

# 1. Decision question

Should the repository owner authorize implementation of the exact reviewed design:

    RUNTIME-EFFECT-LEDGER-NC-01

under authorization:

    AUTH-RUNTIME-EFFECT-LEDGER-NC-001

with the bounded scope below?

---

# 2. Why this authorization is needed

U05 implementation review confirmed:

    BF-U05-IMPL-IR-06
    = DURABLE_ADMISSION_ROUTE_REPLAY_BOUNDARY_NOT_IMPLEMENTED
    = OPEN / BLOCKING / SHARED_RUNTIME_IMPACT.

Current U05:

    U05InMemoryAdmissionLedger
    U05InMemoryRouteLedger

only provide same-process retry behavior.

Frozen RDP-01/RDP-04 require durable crash/restart reattachment for:

    admission records

    routing-decision records

    eligible-route/eligibility records.

Current repository does not expose an implemented shared
Runtime / Canonical Effect Ledger capable of those semantics.

---

# 3. Exact authorized production-source scope if approved

Authorization permits ADDING only a new generic package:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/**

and focused tests:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/**

No modification of existing production/shared source is authorized.

---

# 4. Exact semantic scope if approved

Implementation may create a generic non-production durable canonical-effect ledger that owns only:

    namespace partitioning

    effect identity association

    canonical fingerprint association

    record schema-version association

    immutable opaque payload bytes

    payload/full-record integrity digests

    assigned-once created_at

    create-if-absent semantics

    exact reattach

    conflict

    corruption/unavailable fail-closed behavior.

Required exact reattachment equality:

    same namespace
    same effect_identity
    same canonical_fingerprint
    same record_schema_version
    same payload SHA-256
    valid record integrity.

Any canonical material mismatch under the same effect identity:

    -> CONFLICT
    -> no overwrite.

---

# 5. Non-production durable adapter if approved

Implementation may add:

    NonProductionFileCanonicalEffectLedger

or an equivalent class under the new generic package.

It must:

    require explicit caller-provided non-production root

    have no default production path

    use bounded JDK filesystem primitives only

    persist immutable canonical records across:
      service reconstruction
      JVM/process restart
      reopening the same dedicated root.

It does not claim:

    production HA
    distributed consensus
    network-filesystem correctness
    disaster recovery
    multi-host coordination.

---

# 6. Atomic / crash / path rules that are part of authorization

Implementation must preserve the exact reviewed rules:

    canonical target
    = only canonical record existence authority

    temp files
    = staging only

    lock/coordination files
    = coordination only

    one concurrent creator wins

    no overwrite
    no last-writer-wins
    no truncate/rewrite
    no unsafe copy fallback.

If safe atomic non-overwriting publication cannot be guaranteed:

    -> UNAVAILABLE
    -> no CREATED claim.

Orphan temp after crash:

    != canonical record.

Symlink/root containment violation:

    -> UNAVAILABLE
    -> no write outside fixed resolved root.

Corrupt canonical record:

    -> CORRUPT
    -> no delete/replace during normal operation.

---

# 7. Generic business-independence boundary

Shared package must not contain:

    U05 admission rules

    D03 policy

    Clinical Readiness mapping

    U04 Gate business logic

    route target mapping

    downstream permission logic

    Scheduler policy

    Clinical State mutation

    target Unit execution.

Shared ledger stores opaque bytes only.

Business record codec remains consumer-owned.

Therefore:

    ledger record
    != Clinical State
    != Clinical Truth
    != Clinical Readiness
    != downstream effect.

---

# 8. Explicitly NOT authorized

Approval does NOT authorize modification to:

    StateCommitter

    StateRepositoryPort

    SyntheticVersionedStateRepository

    StatePatchBoundaryValidator

    contracts/v1/**

    Scheduler

    U01-U05 business source

    Spring/configuration

    DB/JPA/Redis

    HTTP/network/cloud storage.

It also does not authorize:

    production persistence

    PHI persistence

    live U06/U08/U10/U11/U14 execution

    route consumption / target execution intent

    merge

    U05 RDP-06 verification PASS.

---

# 9. Required implementation lineage

If Owner approves:

    implementation branch MUST descend from the explicit
    authorization decision-record lineage.

Retrospective authorization is not allowed.

---

# 10. Required shared capability verification

At minimum exact-head verification must prove:

    create -> CREATED

    exact same replay -> REATTACHED

    same identity / different fingerprint -> CONFLICT

    same identity/fingerprint / different schema -> CONFLICT

    same identity/fingerprint/schema / different payload -> CONFLICT

    canonical record immutable

    first created_at retained

    service-object reconstruction reattach

    isolated process/JVM restart reattach

    corrupt/truncated/unsupported-format -> CORRUPT

    inaccessible/unsafe root -> UNAVAILABLE

    namespace isolation

    traversal/symlink containment

    concurrent creators:
      exactly one CREATED
      exact losers REATTACHED
      conflicting loser CONFLICT

    caller/returned payload copy isolation

    >256 KiB payload fail-closed

    crash before canonical publication:
      orphan temp not authoritative

    unsupported atomic publication:
      UNAVAILABLE
      no fallback overwrite

    no imports/business policy from runtime.u05 or clinical business packages

    no Spring/default production activation

    no DB/network dependency.

---

# 11. U05 relationship after shared capability implementation

Even after this ledger passes its own verification:

    BF-U05-IMPL-IR-06
    != automatically CLOSED.

U05 must separately:

    implement deterministic U05 admission codec

    implement deterministic U05 routing/eligibility codecs

    replace authoritative verification path usage of:
      U05InMemoryAdmissionLedger
      U05InMemoryRouteLedger

    consume durable namespaces:
      U05_ADMISSION
      U05_ROUTING_DECISION
      U05_ROUTE_ELIGIBILITY

    prove isolated restart reattachment

    prove conflict fail-closed

    prove no duplicate eligibility

    revalidate currentness after reattachment.

Then U05 requires exact-head targeted implementation re-review.

---

# 12. STOP conditions

Implementation must STOP and return for impact/authorization amendment if it requires:

    modifying any existing production/shared source file

    changing Shared Contracts

    adding DB/Redis/network/cloud persistence

    adding production/default wiring

    introducing U05-specific business semantics in shared ledger

    implementing Scheduler or route consumption

    storing PHI/real patient content

    broadening scope beyond generic non-production durable effect reconciliation.

---

# 13. Owner options

Repository Owner must choose exactly one:

## AUTHORIZE

Record:

    AUTH-RUNTIME-EFFECT-LEDGER-NC-001
    = AUTHORIZED

Meaning:

    implementation of the exact reviewed design is allowed
    within Sections 3-8 and STOP conditions above.

## REVISE

Meaning:

    authorization not granted

    terms require amendment and re-review.

## REJECT

Meaning:

    authorization not granted

    implementation must not proceed.

---

# 14. Current state before Owner decision

    RUNTIME-EFFECT-LEDGER-NC-01 Design
    = PASS

    Exact reviewed design head
    = 0a275d461ac6001d6cede9e4d46fb36d6a7f50de

    Targeted Independent Design Review
    = PASS
    review_id = 5273185469

    AUTH-RUNTIME-EFFECT-LEDGER-NC-001
    = NOT_GRANTED / OWNER_DECISION_PENDING

    BF-U05-IMPL-IR-06
    = OPEN / BLOCKING

No implementation or merge is authorized by this document.
