# PBNC-02A Synthetic Object Value Support — Controlled Design v0.1

> Design ID: `PBNC-02A-SYNTHETIC-OBJECT-VALUES`  
> Proposed authorization ID: `AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001`  
> Trigger: U05 implementation blocker `BF-U05-IMPL-IR-05`  
> Impact review: PR #183 / PASS / review_id `5265060541`  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document authorizes no implementation.

---

# 1. Problem

PBNC-02 is already implemented and merged through PR #74.

Current shared non-production capability:

    StateCommitter
    -> StateRepositoryPort
    -> SyntheticVersionedStateRepository
    -> SyntheticJsonPointerApplier

The PBNC-02 repository can apply synthetic StatePatch mutations and expose:

    SyntheticStateSnapshot

for read-back.

However:

    StatePatchBoundaryValidator

already permits exactly one controlled structured object level:

    top-level Map<String,Object>

whose values may be:

    String
    Number
    Boolean
    null
    List of scalar values

while:

    SyntheticJsonPointerApplier

currently rejects every Map operation value.

Therefore the boundary validator accepts a StatePatch shape
that PBNC-02 cannot actually apply.

U05 exposes this mismatch because frozen RDP-03 writes:

    /patient_state/clinical_readiness

as one structured readiness record.

---

# 2. Design objective

PBNC-02A shall align:

    SyntheticJsonPointerApplier

with the already-existing:

    StatePatchBoundaryValidator controlled-value boundary.

PBNC-02A must NOT broaden Shared Contracts semantics.

This is an implementation-alignment fix inside the existing synthetic-only state boundary.

---

# 3. Exact allowed semantic scope

PBNC-02A may support operation.value where value is:

    String
    Number
    Boolean
    null
    List of scalar values

or exactly one:

    Map<String,Object>

with:

    1..32 keys

each key satisfying the same opaque identifier requirements already enforced by StatePatchBoundaryValidator

and each value limited to:

    String
    Number
    Boolean
    null
    List of scalar values.

PBNC-02A must implement exact parity with the existing
StatePatchBoundaryValidator controlled-value constraints.

Current exact constraints to preserve:

    top-level operation value:
      String <= 4000 characters
      Number abs(value) <= 9007199254740991
      Boolean
      List size <= 64
      one top-level Map size 1..32

    Map key:
      opaque-id syntax
      length 1..64

    Map child value:
      String <= 1000 characters
      Number abs(value) <= 9007199254740991
      Boolean
      null
      List size <= 64 containing scalar values only

    List item string:
      <= 1000 characters

    nested List:
      prohibited

    nested Map:
      prohibited

    Map inside List:
      prohibited

    arbitrary Java object:
      prohibited

    ADD / REPLACE top-level operation value:
      non-null required

    REMOVE:
      non-null value prohibited

The implementation must not create a second, broader value vocabulary.

Normative parity rule:

    SyntheticJsonPointerApplier accepts an operation value
    only if the same semantic value shape is accepted by the existing
    StatePatchBoundaryValidator operation-value boundary.

PBNC-02A does not change the validator; it mirrors the already-frozen boundary.

---

# 4. Existing semantics that remain frozen

PBNC-02A does not change:

    StatePatch schema

    ContractVersion

    StatePatchBoundaryValidator semantic authority

    StateRepositoryPort interface

    StateCommitter commit semantics

    idempotency semantics

    version conflict semantics

    field permission semantics

    source validation

    consent/capability policy

    Audit semantics

    production wiring.

The existing statement remains true:

    PBNC-02
    = synthetic engineering-only state integration
    != production Clinical State.

---

# 5. Exact file impact

Proposed implementation may modify only:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/
      SyntheticJsonPointerApplier.java

and focused PBNC-02 tests under:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/**

No modification is authorized to:

    contracts/v1/**

    StatePatchBoundaryValidator.java

    StateRepositoryPort.java

    StateCommitter.java

    production Spring/config/persistence/network files

    U01-U05 business policy source.

If implementation proves another shared production source must change:

    STOP
    -> impact review
    -> authorization amendment.

---

# 6. Required implementation rule

SyntheticJsonPointerApplier may not infer domain meaning.

It performs only:

    validate supported synthetic value shape

    deep copy through existing SyntheticStateSnapshot semantics

    ADD / REPLACE / REMOVE mechanics.

For one-level structured Map:

    ADD
    -> target absent required

    REPLACE
    -> target present required

No business-field validation occurs inside PBNC-02A.

---

# 7. Read-back semantics

PBNC-02A does not itself create U05-specific read-back APIs.

Existing:

    SyntheticVersionedStateRepository.snapshot(cdp_id)

remains sufficient.

After PBNC-02A implementation,
a U05-owned non-production adapter may:

    commit through StateCommitter

    read snapshot

    navigate:
      /patient_state/clinical_readiness

    compare exact governed fields.

That U05 adapter remains outside PBNC-02A.

---

# 8. U05 authoritative-evidence condition

After PBNC-02A exists, U05 still must not treat:

    CommitResult.status = COMMITTED

as sufficient readiness truth.

Required U05 evidence sequence:

    StatePatch proposal

    -> StateCommitter

    -> PBNC-02 synthetic application

    -> COMMITTED

    -> exact SyntheticStateSnapshot read-back

    -> verify:
         committed Clinical State version
         readiness_record_id
         effect_id
         canonical_payload_fingerprint
         clinical_readiness
         state_validity
         required provenance refs

    -> only then create
         U05ClinicalReadinessCommitEvidence.

For invalidation:

    read-back must verify:
      same readiness_record_id
      same prior effect_id
      same readiness business value
      state_validity = STALE
      invalidation_effect_ref
      invalidation reason refs.

---

# 9. Security / PHI boundary

PBNC-02A remains synthetic-only.

Tests must use:

    synthetic identifiers
    synthetic state
    no PHI
    no provider integration
    no real patient data.

No:

    database
    JPA
    Redis
    HTTP
    Feign
    production profile
    provider API

may be introduced.

---

# 10. Required focused verification

At minimum add tests proving:

## A. Existing scalar semantics unchanged

    scalar ADD / REPLACE / REMOVE still pass.

## B. One-level structured object ADD

Initial synthetic state:

    patient_state = {}

Patch:

    ADD /patient_state/clinical_readiness
    value = one-level controlled Map

Expected:

    COMMITTED
    version +1
    snapshot contains deep-equal object.

## C. One-level structured object REPLACE

Existing object present.

Patch:

    REPLACE exact path
    value = different one-level controlled Map

Expected:

    COMMITTED
    prior historical snapshot unchanged
    new snapshot contains replacement.

## D. Deep copy

Mutating caller-owned source Map/List after commit:

    must not mutate synthetic stored state.

## E. Nested Map rejected

    StatePatch/Synthetic application fails closed.

## F. list-of-Map rejected

    fails closed.

## G. direct repository bypass guard

Even if caller bypasses StateCommitter boundary validation,
SyntheticJsonPointerApplier itself must not accept values broader
than the frozen controlled-value shape.

Required parity rejection tests:

    top-level Map size = 33
    -> reject

    malformed / >64-char Map key
    -> reject

    List size = 65
    -> reject

    child String length = 1001
    -> reject

    top-level String length = 4001
    -> reject

    Number abs(value) > 9007199254740991
    -> reject

    nested List
    -> reject

    nested Map
    -> reject

    list-of-Map
    -> reject

    ADD/REPLACE with top-level null
    -> reject

Boundary acceptance tests:

    Map size = 32
    valid opaque keys
    child String length = 1000
    List size = 64
    safe-integer boundary value
    -> accepted when all other StatePatch requirements are valid.

## H. conflict/idempotency regression

Existing PBNC-01/PBNC-02 tests remain PASS.

---

# 11. Acceptance criteria

PBNC-02A design/implementation passes only if:

    structured one-level Map application works

    no nested-structure semantic expansion occurs

    existing scalar semantics remain unchanged

    StatePatchBoundaryValidator is unchanged

    Shared Contracts are unchanged

    StateRepositoryPort is unchanged

    no production wiring appears

    full diagnosis-service regression remains green.

---

# 12. Authorization boundary

Before explicit owner authorization:

    AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001
    = NOT_GRANTED

Allowed now:

    design
    independent review
    authorization-decision preparation.

Not allowed:

    modify SyntheticJsonPointerApplier
    modify shared runtime source
    claim U05 IR-05 closed.

After implementation + independent verification:

    U05 may consume PBNC-02A

but U05 IR-05 closes only after:

    exact U05 readiness/invalidation synthetic read-back
    is implemented and re-reviewed.

---

# 13. Proposed next gate

Required sequence:

    PBNC-02A design
    -> Independent Design Review
    -> explicit Owner Authorization
    -> implementation
    -> exact-head verification
    -> independent verification
    -> U05 consumption
    -> U05 targeted implementation re-review.


---

# 14. Independent Design Review Remediation

Initial Independent Design Review:

    PR #184
    verdict = REVISE_REQUIRED
    review_id = 5265098433

Finding:

    BF-PBNC02A-IR-01
    = CONTROLLED_VALUE_PARITY_LIMITS_UNDERDEFINED

Remediation:

    exact current StatePatch controlled-value limits are now frozen
    into PBNC-02A design and focused parity tests.

    PBNC-02A accepts no value that the current
    StatePatchBoundaryValidator would reject.

    StatePatchBoundaryValidator remains unchanged.

Current:

    BF-PBNC02A-IR-01
    = REMEDIATED / TARGETED_REVIEW_PENDING

    PBNC-02A Design
    = REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW
