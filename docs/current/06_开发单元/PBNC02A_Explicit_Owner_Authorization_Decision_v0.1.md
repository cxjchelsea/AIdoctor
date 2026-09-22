# PBNC-02A Explicit Owner Authorization Decision v0.1

> Authorization ID: `AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001`  
> Decision type: **Repository Owner Explicit Implementation Authorization**  
> Exact reviewed design head: `997e8cf3dea4c18901c1609fa77b9896b33e9237`  
> Design PR: #184  
> Independent design review: PASS / review_id `5265120581`  
> Impact review: PR #183 / PASS / review_id `5265060541`  
> Current status: **OWNER_DECISION_PENDING**  
> This package itself grants no implementation authorization.

---

# 1. Decision question

Should the repository owner authorize implementation of the exact reviewed design:

    PBNC-02A-SYNTHETIC-OBJECT-VALUES

under authorization:

    AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001

with the bounded scope below?

---

# 2. Why this authorization is needed

U05 implementation review confirmed:

    BF-U05-IMPL-IR-05
    = MECHANICAL_COMMIT_PROMOTED_TO_AUTHORITATIVE_READINESS
    = OPEN / BLOCKING / SHARED_RUNTIME_IMPACT.

Current PBNC-02:

    SyntheticVersionedStateRepository

can apply synthetic StatePatch operations and expose read-back snapshots,
but current:

    SyntheticJsonPointerApplier

rejects structured Map/object operation values.

Current:

    StatePatchBoundaryValidator

already permits exactly one controlled structured-object level.

Frozen U05 RDP-03 Clinical Readiness is represented as such a controlled structured record.

Therefore PBNC-02A is needed only to align the synthetic applier
with semantics that the existing StatePatch boundary already accepts.

---

# 3. Exact authorized production-source scope if approved

Authorization permits modification only to:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/
      SyntheticJsonPointerApplier.java

and focused PBNC-02 tests under:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/**

No other production/shared source file is authorized.

---

# 4. Exact semantic scope if approved

Implementation may support exactly the current
StatePatchBoundaryValidator controlled-value set.

It may align SyntheticJsonPointerApplier to accept:

    scalar values already supported

    one top-level Map<String,Object>

with the same existing limits for:

    map size
    key syntax/length
    string lengths
    list size
    safe numeric range
    scalar/list-of-scalar child values.

It must continue to reject:

    nested Map
    nested List
    list-of-Map
    arbitrary Java object
    unsupported numeric range
    malformed keys
    values outside existing validator limits.

Normative rule:

    PBNC-02A accepted value semantics
    == current StatePatchBoundaryValidator accepted value semantics.

---

# 5. Explicitly NOT authorized

Approval does NOT authorize changes to:

    contracts/v1/**

    StatePatchBoundaryValidator.java

    StateRepositoryPort.java

    StateCommitter.java

    SyntheticVersionedStateRepository.java
    except test consumption already permitted by existing interface

    production Spring/configuration

    persistence/database/network/provider integrations

    U01-U05 business policy source

    Clinical Readiness policy semantics

    production Clinical State.

It does not authorize:

    production wiring
    real patient/PHI use
    merge
    release activation
    U05 implementation verification PASS.

---

# 6. Required implementation lineage

If Owner approves:

    implementation branch MUST descend from this explicit
    authorized decision-record lineage.

It must not start from a pre-authorization branch and later
claim retrospective authorization.

---

# 7. Required verification after implementation

At minimum implementation verification must prove:

    existing scalar semantics unchanged

    one-level controlled structured ADD works

    one-level controlled structured REPLACE works

    deep-copy semantics hold

    map/key/list/string/number boundary parity

    nested structures fail closed

    direct repository bypass cannot accept broader values

    existing conflict/idempotency behavior remains valid

    full relevant diagnosis-service regression is green

    no Shared Contracts drift

    no production/default wiring appears.

Implementation existence or green tests alone do not authorize U05 consumption.

---

# 8. U05 relationship after PBNC-02A implementation

Even after this capability passes its own verification:

    StateCommitter COMMITTED
    != authoritative U05 readiness evidence.

U05 must still:

    commit through PBNC-02

    read SyntheticStateSnapshot

    verify exact committed readiness record

    only then construct U05ClinicalReadinessCommitEvidence.

For invalidation, U05 must read back and prove:

    same readiness business value/provenance
    state_validity = STALE
    correct invalidation effect/reason refs.

Therefore:

    PBNC-02A implementation PASS
    != BF-U05-IMPL-IR-05 automatically CLOSED.

U05 exact-head targeted re-review is still required.

---

# 9. STOP conditions

Implementation must STOP and return for impact/authorization amendment if any of these is required:

    Shared Contracts semantic change

    StatePatchBoundaryValidator change

    StateRepositoryPort change

    StateCommitter change

    broader nested JSON support

    production adapter/wiring

    database/network persistence

    domain-specific Clinical Readiness logic in shared PBNC-02 code

    any additional production/shared source file.

---

# 10. Owner options

Repository Owner must choose exactly one:

## AUTHORIZE

Record:

    AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001
    = AUTHORIZED

Meaning:

    implementation of the exact reviewed design is allowed
    within Sections 3-5 and STOP conditions above.

## REVISE

Meaning:

    authorization not granted

    design/authorization terms require amendment
    and re-review before implementation.

## REJECT

Meaning:

    authorization not granted

    PBNC-02A implementation must not proceed.

---

# 11. Current state before Owner decision

    PBNC-02A Design
    = PASS

    Exact reviewed design head
    = 997e8cf3dea4c18901c1609fa77b9896b33e9237

    Independent Design Review
    = PASS
    review_id = 5265120581

    AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001
    = NOT_GRANTED / OWNER_DECISION_PENDING

    BF-U05-IMPL-IR-05
    = OPEN / BLOCKING

No implementation or merge is authorized by this document.
