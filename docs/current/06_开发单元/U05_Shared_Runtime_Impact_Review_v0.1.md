# U05 Shared Runtime Impact Review v0.1

> Review type: implementation-blocking shared-runtime capability impact review  
> Finding source: PR #182 exact-head implementation review / review_id `5264992072`  
> U05 authorization: `AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED`  
> U05 authorization boundary: **NO_SHARED_RUNTIME_SEMANTIC_CHANGE**  
> Review base: `726d4be3d483889384276e73e7a98f1ff13875e4`  
> Status: **PROPOSED / INDEPENDENT_IMPACT_REVIEW_PENDING**  
> This document authorizes no shared-runtime modification.

---

# 1. Why this review exists

PR #182 implementation review found two blockers that cannot lawfully be fixed under the current U05 implementation authorization:

    BF-U05-IMPL-IR-05
    = MECHANICAL_COMMIT_PROMOTED_TO_AUTHORITATIVE_READINESS

    BF-U05-IMPL-IR-06
    = DURABLE_ADMISSION_ROUTE_REPLAY_BOUNDARY_NOT_IMPLEMENTED

AUTH-U05-RUNTIME-IMPL-001 explicitly prohibits:

    non-U05 production/shared-runtime source modification
    shared-runtime semantic change

Therefore U05 implementation must stop at the existing shared boundary and request a separate impact decision.

This review does not reopen U05 design/readiness.

---

# 2. Impact A — PBNC-02 synthetic structured state application/read-back

## 2.1 Existing implemented capability

PR #74:

    PBNC-02 Synthetic Versioned State Integration

is merged.

Current implemented shared capability includes:

    SyntheticVersionedStateRepository
    SyntheticStateSnapshot
    SyntheticJsonPointerApplier

The repository can:

    atomically apply a StatePatch
    advance synthetic Clinical State version
    expose a synthetic snapshot for read-back.

This is non-production/synthetic only.

## 2.2 Current limitation

Current SyntheticJsonPointerApplier accepts operation values only when they are:

    null
    String
    Number
    Boolean
    List of scalar values

It rejects:

    Map / structured JSON object values.

But frozen U05 RDP-03 readiness proposal writes a structured record at:

    /patient_state/clinical_readiness

containing at least:

    readiness_record_id
    clinical_readiness
    source admission / D03 refs
    readiness input-set refs
    Gate / route refs
    policy refs
    dependency refs
    state_validity
    effect/proposal refs
    canonical payload fingerprint
    invalidation metadata when stale.

Therefore:

    U05 StatePatch value = structured object / Map

and current PBNC-02 synthetic applier cannot represent the frozen record.

If U05 is switched from MechanicalVersionRepositoryFake to the actual PBNC-02 repository today:

    StateCommitter may pass mechanical admission
    -> SyntheticVersionedStateRepository attempts patch application
    -> SyntheticJsonPointerApplier rejects structured Map
    -> STATE_OPERATION_APPLICATION_FAILED.

## 2.3 Why U05 cannot work around this locally

Forbidden workarounds:

    serialize readiness object to opaque JSON string

because that changes state semantics and prevents governed field/read-back assertions.

    split the frozen whole-record ADD/REPLACE into an ad-hoc set of leaf patches

because the current RDP-03 proposal contract freezes a single current Clinical Readiness record and its canonical replay identity.

    create a second U05-only pseudo Clinical State store

because G2/P01 / StateRepositoryPort remains the authoritative non-production mutation boundary.

Therefore the minimum shared capability is generic JSON-compatible object support in PBNC-02 synthetic state application.

## 2.4 Minimal proposed extension

Candidate shared extension:

    PBNC-02A
    = SYNTHETIC_JSON_OBJECT_STATE_VALUE_SUPPORT

Allowed semantic scope:

    SyntheticJsonPointerApplier
    and PBNC-02 tests only

Generic behavior must exactly mirror the existing
StatePatchBoundaryValidator controlled-value boundary.

Allowed:

    one top-level Map<String,Object>

    Map values limited to:
      String
      Number
      Boolean
      null
      List of scalar values

    existing scalar/list values remain unchanged.

Still prohibited:

    nested Map
    list-of-Map
    arbitrary Java objects
    any value rejected by StatePatchBoundaryValidator
    domain inference
    medical interpretation
    production persistence
    Spring wiring
    network/provider/CDP integration
    PHI/real-patient content.

PBNC-02A must therefore align the synthetic applier
with the already-authorized StatePatch boundary;
it must not broaden Shared Contracts value semantics.

No Shared Contracts schema/validator semantic change is required.

## 2.5 Required U05 consumption after extension

Even after PBNC-02A exists:

    StateCommitter COMMITTED
    alone
    != authoritative readiness proof.

U05 must:

    commit via StateCommitter / PBNC-02

    then read:
      SyntheticVersionedStateRepository.snapshot(cdp_id)

    verify:
      committed version
      /patient_state/clinical_readiness exists
      readiness_record_id matches proposal
      effect_id matches proposal
      canonical_payload_fingerprint matches proposal
      state_validity matches expected

Only then may U05 construct:

    ClinicalReadinessCommitEvidence
    authoritativeReadinessRecordRef

and proceed to RDP-04.

For invalidation:

    read-back must prove:
      same readiness business/provenance record
      state_validity = STALE
      invalidation_effect_ref matches
      invalidation reason refs match.

---

# 3. Impact B — Runtime / Canonical Effect Ledger durability

## 3.1 Frozen requirement

RDP-01 crash semantics require:

    lookup U05_ADMISSION_ID
    -> reattach same authoritative admission record.

RDP-04 freezes:

    U05DownstreamRouteLedgerRecord

Owner:

    Runtime / Canonical Effect Ledger

with durable crash/replay support across:

    readiness commit -> route decision

    route decision -> eligibility

    eligibility -> Scheduler consumption.

RDP-06 requires replay/crash evidence and no duplicate route/target intent.

## 3.2 Current implementation state

PR #182 currently has:

    U05InMemoryAdmissionLedger
    U05InMemoryRouteLedger

These provide only:

    same-JVM retry idempotency.

After process restart:

    admission records are gone
    route decision records are gone
    eligibility reattachment evidence is gone.

Therefore they cannot satisfy the frozen crash/recovery semantics.

## 3.3 Current repository capability check

No implemented shared production/runtime source corresponding to:

    Runtime / Canonical Effect Ledger

was identified in the current repository lineage.

The term exists in frozen RDP-03/RDP-04/RDP-06 design artifacts,
but an implementation-grade shared durable effect-ledger port/adapter
is not currently available to U05.

This is a capability gap, not permission for U05 to invent a private competing shared ledger.

## 3.4 Minimal proposed shared capability

Candidate shared capability:

    RUNTIME-EFFECT-LEDGER-NC-01
    = NON_PRODUCTION_CANONICAL_EFFECT_LEDGER

It should be generic/non-clinical.

Minimum key/value behavior:

    lookup(namespace, effect_identity)

    create_if_absent(
      namespace,
      effect_identity,
      canonical_fingerprint,
      immutable_record
    )

    exact same identity + fingerprint
    -> reattach prior record

    same identity + different fingerprint
    -> typed replay conflict

    persistence survives service object reconstruction
    required by crash/restart verification harness.

Minimum U05 namespaces:

    U05_ADMISSION

    U05_ROUTING_DECISION

    U05_ROUTE_ELIGIBILITY

Future generic use is permitted,
but no U05-specific business semantics may be moved into the ledger.

Ledger record:

    non-clinical runtime/governance evidence only

    != Clinical State
    != Clinical Truth
    != Clinical Readiness
    != downstream Unit state.

## 3.5 Scope boundary

This impact review does not authorize:

    Scheduler implementation
    downstream Unit invocation
    target execution ledger
    production database/persistence
    distributed transaction design
    production HA
    checkpoint becoming Clinical State authority.

Only enough non-production durability to prove exact crash/replay semantics is in scope.

---

# 4. Relationship between the two shared capabilities

PBNC-02A and RUNTIME-EFFECT-LEDGER-NC-01 solve different problems.

PBNC-02A:

    authoritative synthetic Clinical State application/read-back
    for readiness state mutation.

Effect Ledger:

    non-clinical runtime effect/replay identity
    for admission/routing/eligibility.

They must not be collapsed.

Required separation:

    Clinical State truth
    != Runtime effect ledger
    != CommitResult
    != Trace.

---

# 5. U05-local findings unaffected by this review

The following remain U05-owned local fixes under AUTH-U05-RUNTIME-IMPL-001:

    BF-U05-IMPL-IR-01
    readiness input-set provenance

    BF-U05-IMPL-IR-02
    current-baseline admission routing

    BF-U05-IMPL-IR-03
    readiness invalidation effect/proposal semantics
    up to shared state application/read-back

    BF-U05-IMPL-IR-04
    route identity version-chasing.

They do not require shared-runtime semantic authorization.

---

# 6. Proposed decision split

Do not use one broad "modify shared runtime for U05" authorization.

Use two bounded decisions:

## Decision A

    AUTH-PBNC02A-SYNTHETIC-OBJECT-VALUES-001

Question:

    May PBNC-02 synthetic-only StateRepository support
    recursively JSON-compatible Map/object operation values
    without production wiring or domain semantics?

## Decision B

    AUTH-RUNTIME-EFFECT-LEDGER-NC-001

Question:

    May a generic non-production Canonical Effect Ledger boundary
    be implemented for durable exact-effect replay evidence?

Each requires:

    exact file/surface design
    independent impact/design review
    explicit owner authorization
    implementation
    independent verification

before U05 consumes it.

---

# 7. Current disposition

Current:

    BF-U05-IMPL-IR-05
    = OPEN / BLOCKING / SHARED_RUNTIME_IMPACT

    BF-U05-IMPL-IR-06
    = OPEN / BLOCKING / SHARED_RUNTIME_IMPACT

    U05 Implementation Review
    = REVISE_REQUIRED

    U05 RDP-06 Authoritative Verification
    = NOT_PERMITTED_YET

Recommended next governance sequence:

    1. finish U05-local targeted fixes

    2. independent review this shared-runtime impact package

    3. prepare bounded design/authorization for PBNC-02A

    4. prepare bounded design/authorization for non-production Canonical Effect Ledger

    5. implement/verify shared capabilities

    6. consume them from U05 without changing frozen U05 semantics

    7. repeat exact-head U05 implementation review.


---

# 8. Independent Impact Review Remediation

Independent review:

    PR #183
    review_id = 5265053120
    verdict = REVISE_REQUIRED

Finding:

    BF-U05-SR-IMPACT-IR-01
    = PBNC02A_OBJECT_SCOPE_BROADER_THAN_CURRENT_STATEPATCH_BOUNDARY

Remediation:

    PBNC-02A scope is narrowed to the existing
    StatePatchBoundaryValidator controlled-value semantics only.

    top-level Map = allowed
    scalar/list-of-scalar children = allowed

    nested Map = prohibited
    list-of-Map = prohibited

    Shared Contracts semantics remain unchanged.

Current:

    BF-U05-SR-IMPACT-IR-01
    = REMEDIATED / TARGETED_REVIEW_PENDING

    U05 Shared Runtime Impact Review
    = REVISED / READY_FOR_TARGETED_REVIEW
