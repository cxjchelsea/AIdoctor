# U05 RDP-06 Targeted Remediation Design v0.1

> Remediation scope: `BF-U05-RDP06-AV-01..03`  
> Finding source: PR #201 / review_id `5274870345`  
> Exact implementation target reviewed: `1dc49c4097841523a9445dab078bc5a3d1ad1259`  
> Existing U05 implementation authorization: `AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED`  
> Existing verifier authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001 = AUTHORIZED`  
> Frozen authority: RDP-01..RDP-06 current frozen/refrozen package at the exact target lineage.  
> Status: **REVISED / TARGETED_INDEPENDENT_RE_REVIEW_PENDING**  
> This document does not modify runtime code and grants no merge, production, live downstream, release, or real-patient authorization.

---

# 1. Purpose

The pre-execution RDP-06 contract-to-surface review found:

    BF-U05-RDP06-AV-01
    = INPUT_CONFLICT_REQUIRED_CASE_UNREPRESENTABLE

    BF-U05-RDP06-AV-02
    = APPLICABILITY_EVIDENCE_MISSING_TYPED_ADMISSION_PATH_ABSENT

    BF-U05-RDP06-AV-03
    = SCHEDULER_CONSUMPTION_AND_TARGET_BINDING_FAILURE_SURFACE_MISSING

The remediation objective is not to weaken RDP-06.

It is:

    restore an executable implementation/verification surface
    for the already-frozen required semantics

while preserving:

    Clinical Truth
    != Model Output
    != Capability Result
    != Runtime State
    != Trace

and preserving:

    no live U06/U08/U10/U11/U14 execution
    no production mutation
    no automatic production activation
    no shared-runtime semantic change under AUTH-U05-RUNTIME-IMPL-001.

---

# 2. Governing interpretation

The three findings do not require any change to the frozen business result vocabulary.

No change is proposed to:

    RDP-01 admission statuses
    RDP-01 frozen rejection reason family
    RDP-02 D03 statuses
    six Clinical Readiness values
    P0-P7 precedence
    POL-005
    POL-011
    RDP-03 mutation semantics
    RDP-04 readiness-to-target mapping
    RDP-05 applicability vocabulary
    RDP-06 required EV identities.

Therefore this package is an implementation-surface remediation only.

If implementation later proves that a frozen semantic change is necessary:

    STOP
    -> controlled contract amendment
    -> independent review
    -> explicit re-authorization as applicable.

---

# 3. AV-01 root cause

Frozen RDP-02 requires:

    P1 mutually exclusive input conflict
    -> INPUT_CONFLICT
    -> no Clinical Readiness
    -> no readiness commit.

RDP-06 requires at least:

    U05-EV-024
    U05-EV-031
    constructible P1 precedence cases.

Current exact-head implementation contains a D03 conflict detector:

    U05ClinicalReadinessPolicy.hasConflict()

which detects:

    same readiness_input_id + different semantic fingerprint

or:

    multiple PRESENT business signals
    in the same source_domain.

But current `U05ReadinessInputManifest` prevents those profiles before D03 by:

    requireExactlyOneDomain(F1/F3/F5/F6)

and:

    reject every duplicate readiness_input_id.

Therefore the D03 P1 branch exists in code but is unreachable from a lawful admitted snapshot.

This is an implementation-shape mismatch, not a missing business rule.

---

# 4. AV-01 targeted remediation

## 4.1 Required-domain existence remains structural

For required manifest domains:

    F1
    F3
    F5
    F6

change only the cardinality guard from:

    exactly one record

to:

    at least one record.

This allows the manifest to preserve authoritative conflicting producer records so that D03, not RDP-01, resolves the frozen P1 business conflict boundary.

F2_CLARIFICATION remains optional and retains its existing bounded cardinality unless a separately frozen case requires otherwise.

## 4.2 RDP-01 must not resolve business conflict

RDP-01 Section 17 explicitly excludes:

    input conflict business resolution

from admission validation.

Therefore the manifest/admission layer must not:

    pick one conflicting signal
    discard a conflicting authoritative record
    apply D03 precedence
    convert conflict into an admission rejection.

The complete admitted snapshot must reach D03 when all structural/currentness/admission conditions are otherwise lawful.

## 4.3 Duplicate readiness_input_id handling

Frozen RDP-02 explicitly includes:

    same readiness_input_id
    maps to conflicting business signals
    -> INPUT_CONFLICT.

Therefore a blanket pre-D03 duplicate-ID rejection must be removed.

Required normalization rule:

    exact duplicate semantic record
    -> MUST be deterministically collapsed as redundant transport duplication

    same readiness_input_id
    + different semantic fingerprint
    -> both records must be preserved
    -> D03 P1 detects INPUT_CONFLICT.

No duplicate form may be silently converted into a different business signal.

## 4.4 Deterministic manifest identity

Because multiple records may now share:

    source_domain
    readiness_input_id

manifest canonical ordering must use a deterministic final tie-breaker.

Required order:

    source_domain
    readiness_input_id
    semantic_fingerprint

before:

    semantic input-set identity derivation
    authoritative record-ref list derivation.

This prevents caller list order from changing `READINESS_INPUT_SET_IDENTITY`.

## 4.4.1 Single canonicalization source of truth

Implementation MUST define one deterministic canonicalization routine, equivalent to:

    normalizeCanonicalInputs(inputs)

Its required semantics are:

    1. sort by:
       source_domain
       readiness_input_id
       semantic_fingerprint

    2. collapse only exact semantic duplicates:
       same readiness_input_id
       + same semantic_fingerprint

    3. preserve conflicting duplicate identities:
       same readiness_input_id
       + different semantic_fingerprint

    4. preserve distinct records from the same source_domain
       when they are not exact semantic duplicates.

The exact same normalized sequence MUST be consumed by:

    constructor-stored manifest inputs
    semanticSetIdentity(...)
    computedSemanticIdentity()
    authoritativeRecordRefs()
    admission request/ref comparison.

No second sorting/deduplication implementation is permitted.

This is required so that:

    same semantic input set
    -> same READINESS_INPUT_SET_IDENTITY

even when caller order or exact transport duplication differs.

## 4.5 D03 policy change

No D03 business-semantic rewrite is required.

The existing P0-before-P1 sequence remains:

    inputFailureReason()
    -> P0 INPUT_FAILURE

    then hasConflict()
    -> P1 INPUT_CONFLICT.

The existing conflict detector may be retained if the newly admitted profile makes its frozen branches executable.

A change to `U05ClinicalReadinessPolicy` is permitted only if required to preserve the same frozen P1 meaning; it must not add a new conflict category not supported by frozen authority.

---

# 5. AV-01 exact expected implementation surface

Primary production file:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/
      U05ReadinessInputManifest.java

Potential production file only if exact-head implementation proves necessary:

    U05ClinicalReadinessPolicy.java

Required focused tests/evidence:

    conflict: F1 OUT_OF_SCOPE + F1 FRAMED_IN_SCOPE
    -> D03 INPUT_CONFLICT

    same readiness_input_id + different semantic fingerprint
    -> D03 INPUT_CONFLICT

    P1 conflict + lower-priority lawful candidate
    -> P1 still wins after P0 is absent

    INPUT_CONFLICT
    -> no proposal
    -> no state commit
    -> no ordinary route

    same semantic records in different caller order
    -> same READINESS_INPUT_SET_IDENTITY.

Required RDP-06 identities unblocked:

    U05-EV-024
    U05-EV-031
    constructible P1 precedence subcases.

---

# 6. AV-02 root cause

Frozen RDP-01 requires for every non-PRESENT slot:

    authoritative applicability evidence.

RDP-01 freezes:

    U05_ADMISSION_APPLICABILITY_EVIDENCE_MISSING

and RDP-06 requires:

    U05-EV-026
    non-PRESENT required slot lacks authoritative applicability evidence
    -> RDP-01 REJECTED
    -> exact reason
    -> no D03.

Current `U05ReadinessInput` constructor requires a non-blank:

    applicabilityEvidenceRef

for every object.

Therefore the invalid inbound state cannot be represented long enough for RDP-01 to return the required typed rejection.

Current `U05AdmissionService` also lacks the frozen reason constant/check.

This places validation at the wrong boundary.

---

# 7. AV-02 targeted remediation

## 7.1 Preserve PRESENT strictness

For:

    applicability_status = PRESENT

retain the existing constructor requirement that the current authoritative record is fully identified.

No weakening of normal current-present records is required.

## 7.2 Permit representation of an incomplete non-PRESENT inbound slot

For:

    ABSENT_BY_DESIGN
    NOT_YET_APPLICABLE
    STALE
    FAILED
    UNAVAILABLE

the transport/admission object must be able to carry:

    applicabilityEvidenceRef = null / blank-normalized-null

long enough for RDP-01 to evaluate it.

This does not make the record admissible.

It only moves the frozen fail-closed check to the correct owner boundary.

## 7.3 Add exact RDP-01 typed rejection

Add to `U05AdmissionService`:

    U05_ADMISSION_APPLICABILITY_EVIDENCE_MISSING

Admission A11 manifest validation must inspect every non-PRESENT accepted slot.

If any such slot lacks authoritative applicability evidence:

    REJECTED
    reason = U05_ADMISSION_APPLICABILITY_EVIDENCE_MISSING

and:

    no U05AdmittedInput
    no D03
    no proposal
    no commit
    no route.

## 7.4 Validation order

Required order inside A11 remains:

    manifest/request identity binding
    authoritative ref identity binding
    per-record consultation/CDP identity
    applicability-evidence presence

then:

    A12 unresolved owner-recomputation guard.

This ensures:

    malformed/missing applicability authority
    != lawful admitted FAILED/UNAVAILABLE input.

A lawful FAILED/UNAVAILABLE input with valid applicability evidence may still reach D03 P0 according to RDP-01/RDP-02.

## 7.5 Identity behavior

The existing `U05Ids.hash` already represents null deterministically as:

    <null>

Therefore a missing evidence ref can participate in the malformed manifest semantic identity without inventing a substitute ref.

No hash algorithm change is required.

---

# 8. AV-02 exact expected implementation surface

Primary production files:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/
      U05ReadinessInput.java
      U05AdmissionService.java

No shared-runtime source is required.

Required focused tests/evidence:

    F5 NOT_YET_APPLICABLE + missing applicability evidence
    -> typed RDP-01 rejection

    F6 ABSENT_BY_DESIGN + missing applicability evidence
    -> typed RDP-01 rejection

    FAILED/UNAVAILABLE + valid applicability evidence
    -> admission may remain lawful when other route conditions are lawful
    -> D03 P0 INPUT_FAILURE

    missing applicability evidence
    -> zero D03 / proposal / commit / route effects.

Required RDP-06 identity unblocked:

    U05-EV-026.

---

# 9. AV-03 reclassification after contract review

Initial preflight classification was:

    OPEN / BLOCKING / CROSS_RUNTIME_IMPACT.

Targeted review of the already-authorized U05/RDP-06 scope found additional governing text:

`AUTH-U05-RUNTIME-IMPL-001` explicitly permits:

    non-live test/fake Scheduler target-intent evidence

and RDP-06 Section 22 explicitly permits observed evidence from:

    Scheduler target intent spy/fake.

RDP-06 Section 23 also permits side-effect evidence refs to point to:

    Scheduler intent spy/fake.

Therefore authoritative U05 verification does not require creation of a live/shared production Scheduler merely to execute EV-056/057/058.

Reclassification proposed:

    BF-U05-RDP06-AV-03
    from:
      SHARED_RUNTIME_IMPLEMENTATION_BLOCKER

    to:
      U05_VERIFICATION_ONLY_SCHEDULER_CONSUMER_SURFACE_GAP.

This does not make the case optional.

The surface must still exist and execute.

---

# 10. AV-03 verification-only remediation

## 10.1 Boundary

Add a verification-only non-live Scheduler consumer fake/spy under U05 test/evidence tooling.

It must consume the real implementation object:

    U05DownstreamEligibility

produced by the exact U05 implementation.

It must not construct a copied eligibility from expected-oracle fields.

## 10.2 Allowed behavior

The non-live Scheduler consumer may:

    accept one real U05DownstreamEligibility

    consume authoritative synthetic currentness fixture state

    evaluate an injected target-binding availability probe

    form a test-only Scheduler target-intent record

    reconcile that target intent through the already-existing
    generic non-production CanonicalEffectLedger

    expose typed observed fields:
      route_consumption_id
      scheduler_intent_ref
      failure_handoff_ref
      replay disposition / ledger decision
      target invocation count
      alternate-route count.

It must not:

    invoke U06/U08/U10/U11
    invoke U14 business recovery
    send a user message
    write Clinical State
    create DDx/F6/Safe-Exit business effects
    call an external model/tool/service
    remap a frozen U05 consequence.

## 10.3 EV-056 stale eligibility

Fixture:

    U05 eligibility exists
    then a bound dependency/currentness condition becomes stale
    before consumption.

Required observed result:

    scheduler_target_intent_count = 0
    downstream_unit_invocation_count = 0
    no alternate route.

The observed stale decision must come from the non-live consumer execution, not from oracle copying.

## 10.4 EV-057 target binding unavailable

Fixture:

    current eligibility exists
    exact mapped target is known
    target execution binding availability probe = UNAVAILABLE.

Required observed result:

    no target invocation
    no alternate clinical route
    failure_handoff_ref exists
    failure boundary = FAILURE_REQUIRED
    U14 business decision is not executed.

This fake/probe represents the external target-binding condition only.

It does not assert or verify production P06/BindingReleaseResolver conformance.

The observed failure handoff must be independently derivable from runtime facts, not from the expected oracle.

Required failure observation:

    failure_handoff_ref
    = deterministic identity over at least:
        eligibility_id
        route_effect_id
        target_unit_id
        observed binding-failure reason
        scheduler-consumption contract version

    failure_handoff_count = 1

    side_effect_evidence_refs[failure_handoff]
    -> returned Scheduler-consumption observation / durable test record

    downstream_unit_invocation_count = 0
    alternate_route_effect_count = 0

The fake must not create:

    U14 retry decision
    U14 degraded-safe-exit decision
    U14 terminal decision
    any final business outcome.

## 10.5 EV-058 exact eligibility replay

The verification-only Scheduler consumer MUST freeze one test-only semantic consumption contract:

    U05_TEST_SCHEDULER_CONSUMPTION_V1

The Scheduler target-intent semantic identity must bind at least:

    eligibility_id
    route_effect_id
    target_unit_id
    current execution-governance / target-binding fixture identity
    scheduler-consumption contract version.

The route_consumption_id must deterministically bind:

    eligibility_id
    route_effect_id
    target_unit_id
    scheduler_target_intent_identity
    scheduler-consumption contract version.

The canonical Scheduler-intent fingerprint must bind the same governed semantic inputs required to distinguish a different lawful consumption payload.

The following attempt-local metadata MUST be excluded from both identity and canonical fingerprint:

    retry/attempt number
    timestamp
    trace-span attempt id
    process/JVM identity
    temporary filesystem path
    transport/message attempt identity.

Required replay semantics:

    same semantic Scheduler intent identity
    + same canonical fingerprint
    -> CanonicalEffectLedger REATTACHED
    -> no second authoritative test target intent

    same semantic Scheduler intent identity
    + different canonical fingerprint
    -> fail closed as replay conflict
    -> no second target intent.

Use the existing verified:

    CanonicalEffectLedger
    NonProductionFileCanonicalEffectLedger

as the durable non-clinical record boundary for the test Scheduler intent.

Required:

    same eligibility semantic identity
    + same target/currentness/binding fixture
    -> same authoritative test target-intent identity
    -> reattach
    -> total authoritative scheduler_target_intent_count = 1.

A service-object reconstruction must be included so the evidence is not same-object-memory-only.

## 10.6 Why this is not mocked business truth

The fake/spy must not generate the expected result.

Expected EV results remain in the independently reviewed RDP-06 oracle.

The fake is only the executable non-live consumer surface whose returned/ledger objects are used as:

    observed_result
    observed_effect_counts
    side_effect_evidence_refs.

Thus:

    expected authority
    != fake implementation.

---

# 11. AV-03 expected repository surface

Verification/test only, for example:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/
      U05NonLiveSchedulerConsumerFake.java
      U05SchedulerConsumptionObservation.java
      U05TargetBindingAvailabilityFake.java
      U05SchedulerTargetInvocationSpy.java

or equivalent test-support names.

The authoritative verifier/evidence builder may consume those objects from:

    tools/u05_nonprod_verification/**

or focused U05 test support.

No new file is required under:

    diagnosis-service/src/main/java/.../runtime/foundation/**
    diagnosis-service/src/main/java/.../runtime/governance/**
    generic Scheduler production source.

No live Spring registration is permitted.

---

# 12. Exact blocker disposition if implemented as designed

After implementation and before authoritative RDP-06 run:

    BF-U05-RDP06-AV-01
    = REMEDIATED / EXACT_HEAD_RE_REVIEW_PENDING

    BF-U05-RDP06-AV-02
    = REMEDIATED / EXACT_HEAD_RE_REVIEW_PENDING

    BF-U05-RDP06-AV-03
    = REMEDIATED_AS_VERIFICATION_SURFACE
      / EXACT_HEAD_RE_REVIEW_PENDING

Not yet allowed at that point:

    U05 Implementation Verification = PASS

because the complete RDP-06 run and evidence review still remain mandatory.

---

# 13. Authorization assessment

## AV-01

Falls within existing authorized U05 production surface:

    U05ReadinessInputManifest
    D03 INPUT_CONFLICT
    RDP-01/RDP-02 frozen implementation.

No new authorization is required if the remediation remains semantically contract-preserving.

## AV-02

Falls within existing authorized U05 production surface:

    U05ReadinessInput
    U05AdmissionService
    frozen typed RDP-01 rejection.

No new authorization is required if the remediation remains semantically contract-preserving.

## AV-03

Falls within existing authorized RDP-04/RDP-06 verification support:

    non-live test/fake Scheduler target-intent evidence
    U05 tests/fakes/spies
    existing shared-runtime interfaces consumed read-only.

No shared-runtime semantic authorization is required if no shared production source is modified.

---

# 14. RDP-06 verifier exact-target consequence

The existing verifier authorization is pinned to:

    1dc49c4097841523a9445dab078bc5a3d1ad1259.

AV-01/02 remediation changes the U05 implementation head.

Therefore even if:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001
    remains semantically authorized,

the old exact-target binding cannot be reused for the new implementation head.

Required after remediation:

    exact-head code/boundary re-review
    +
    verifier target-rebind amendment
    +
    independent target-rebind review
    +
    exact oracle/fixture digest applicability check

before authoritative CI executes the new head.

The old exact-target run remains:

    NOT_RUN
    and must not be relabeled.

---

# 15. Required implementation order

    Step 1
    implement AV-01 local U05 remediation

    Step 2
    implement AV-02 local U05 remediation

    Step 3
    implement AV-03 verification-only consumer fake/spy

    Step 4
    run focused engineering smoke only
    no RDP-06 PASS claim

    Step 5
    Targeted Exact-Head Implementation Re-Review

    Step 6
    verify:
      no frozen RDP semantics changed
      no shared production runtime source changed
      AV-01/02/03 exact surfaces exist

    Step 7
    create verifier exact-target rebind amendment

    Step 8
    independent oracle / precedence / fixture review
    or confirm existing reviewed digests remain applicable

    Step 9
    authoritative RDP-06 exact-head CI

    Step 10
    independent evidence-only review
    combined closure review.

---

# 16. Mandatory remediation tests before verifier rebind

Minimum local engineering tests:

    AV01-01
    multi-record same-domain conflict reaches D03 P1

    AV01-02
    same readiness_input_id conflicting fingerprint reaches D03 P1

    AV01-03
    input-set identity is caller-order independent under conflict candidates

    AV01-04
    INPUT_CONFLICT creates no proposal/commit/route

    AV02-01
    non-PRESENT missing applicability evidence is representable pre-admission

    AV02-02
    admission returns exact frozen evidence-missing reason

    AV02-03
    evidence-missing case produces no D03/effect

    AV02-04
    lawful FAILED/UNAVAILABLE with evidence remains a D03 P0 case

    AV03-01
    stale eligibility -> zero target intent

    AV03-02
    unavailable target binding -> FAILURE_REQUIRED handoff / zero invocation

    AV03-03
    same eligibility replay -> one durable test target intent

    AV03-04
    no alternate U06/U08/U10/U11 route is invented

    AV03-05
    downstream invocation / delivery / external tool-model counts remain zero.

These are remediation engineering tests.

They do not replace the frozen EV identities.

---

# 17. Prohibited shortcuts

Do not remediate AV-01 by:

    hard-coding EV-024
    calling hasConflict() directly on an object that admission can never produce
    reflection-mutating a manifest after construction
    bypassing RDP-01.

Do not remediate AV-02 by:

    changing EV-026 expected reason
    treating constructor exception as equivalent to RDP-01 rejection
    inserting fake applicability evidence.

Do not remediate AV-03 by:

    adding live downstream Unit invocation
    implementing a production Scheduler under U05 authorization
    copying expected result into scheduler observation
    silently routing unavailable U08 to U10/U11
    invoking U14 final/business outcome.

---

# 18. Proposed review verdict

If independent review confirms this package:

    U05 RDP-06 Targeted Remediation Design
    = PASS_FOR_IMPLEMENTATION_UNDER_EXISTING_U05_AUTHORIZATION

    Shared Production Runtime Change
    = NOT_REQUIRED

    RDP-06 Authoritative Run
    = STILL_BLOCKED_UNTIL_REMEDIATION_EXACT_HEAD_REVIEW_AND_TARGET_REBIND

No closure is claimed by this design alone.


---

# 19. Independent Review Remediation Provenance

Initial Independent Review:

    PR #205
    review_id = 5274973422
    verdict = REVISE_REQUIRED

Findings:

    BF-U05-RDP06-TR-IR-01
    = MANIFEST_CANONICALIZATION_NOT_SINGLE_SOURCE_OF_TRUTH

    BF-U05-RDP06-TR-IR-02
    = TEST_SCHEDULER_INTENT_IDENTITY_NOT_FROZEN_ENOUGH_FOR_EV058

    RQ-U05-RDP06-TR-IR-03
    = FAILURE_HANDOFF_OBSERVATION_PROVENANCE_MUST_BE_EXPLICIT

Remediation applied:

    IR-01
    -> exact semantic duplicate collapse is mandatory
    -> one normalizeCanonicalInputs routine is the sole canonical source
    -> constructor/set identity/computed identity/ref ordering/admission comparison
       all consume the same normalized sequence
    -> conflicting duplicate IDs are preserved for D03 P1

    IR-02
    -> U05_TEST_SCHEDULER_CONSUMPTION_V1 frozen
    -> minimum Scheduler intent identity / route consumption identity /
       canonical fingerprint inputs frozen
    -> attempt-local exclusions frozen
    -> exact replay reattaches through existing CanonicalEffectLedger
    -> fingerprint mismatch fails closed

    IR-03
    -> EV-057 failure_handoff_ref derives from observed runtime facts
    -> typed failure_handoff_count/evidence ref required
    -> zero target invocation / zero alternate route
    -> no U14 business/final outcome generated.

Current:

    BF-U05-RDP06-TR-IR-01
    = REMEDIATED / TARGETED_RE_REVIEW_PENDING

    BF-U05-RDP06-TR-IR-02
    = REMEDIATED / TARGETED_RE_REVIEW_PENDING

    RQ-U05-RDP06-TR-IR-03
    = REMEDIATED / TARGETED_RE_REVIEW_PENDING

    U05 RDP-06 Targeted Remediation Design
    = REVISED / READY_FOR_TARGETED_INDEPENDENT_RE_REVIEW
