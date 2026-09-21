# U05 Implementation Authorization Review v0.1

> Review type: U05 non-production implementation authorization eligibility review  
> Readiness basis: U05 Implementation Readiness Re-Evaluation v0.4 / PR #179  
> Reviewed readiness semantic head: `4dc2d3a78afd9e7fd576bf7c1cd64113de479756`  
> Current readiness status/provenance head: `a0f3af930940661e173b406686d7b4bfe2c7fbd1`  
> Exact re-frozen six-contract package: `7acbeba0e066c6a7755bb07affe4ec30d6a6f562`  
> Proposed authorization ID: `AUTH-U05-RUNTIME-IMPL-001`  
> Status: **REVISED / READY_FOR_TARGETED_AUTHORIZATION_REVIEW**  
> This document does not itself grant implementation authorization.

---

# 1. Review question

Whether the current U05 package is sufficiently ready and bounded to allow an explicit repository-owner decision authorizing a non-production implementation slice.

This review must not answer:

    "Should production U05 go live?"

It answers only:

    "May an owner explicitly authorize implementation of the current frozen non-production U05 slice?"

---

# 2. Preconditions

The following are confirmed inputs:

    BF-U05-RG-01 = CLOSED
    BF-U05-RG-02 = CLOSED
    BF-U05-RG-03 = CLOSED
    BF-U05-RG-04 = CLOSED
    BF-U05-RG-05 = CLOSED
    BF-U05-RG-06 = CLOSED

    BF-U05-AGR-01 = CLOSED_BY_REFREEZE
    BF-U05-AGR-02 = CLOSED_BY_REFREEZE
    BF-U05-AGR-03 = CLOSED_BY_REFREEZE

    BF-U05-AGR-AMEND-IR-01 = CLOSED

    U05 Definition / Business-Semantic Readiness = READY
    U05 Aggregate Contract Compatibility = READY
    U05 Implementation Readiness = READY

    U05 Implementation Authorization Review
    = PERMITTED_TO_BEGIN

The exact contract package is:

    RDP-01 = REFROZEN / V1 — PASS_FOR_READINESS
    RDP-02 = READY-POLICY REFROZEN / V1
    RDP-03 = REFROZEN / V1 — PASS_FOR_READINESS
    RDP-04 = FROZEN / PASS_FOR_READINESS
    RDP-05 = REFROZEN / V1 — CURRENT AUTHORITATIVE STATUS
    RDP-06 = REFROZEN / V1 — PASS_FOR_READINESS

No open readiness blocker remains.

---

# 3. Proposed authorization shape

If separately approved by the repository owner:

    AUTH-U05-RUNTIME-IMPL-001

shall mean:

    AUTHORIZED
    / NON_PRODUCTION_ONLY
    / FROZEN_RDP01_TO_RDP06_ONLY
    / NO_LIVE_UPSTREAM_CUTOVER
    / NO_LIVE_DOWNSTREAM_EXECUTION
    / NO_PRODUCTION_MUTATION
    / NO_SHARED_RUNTIME_SEMANTIC_CHANGE

This authorization would permit implementation of the current U05 slice only.

---

# 4. Exact implementation base

If authorized, implementation must branch from the reviewed readiness lineage at:

    base branch:
      review/u05-implementation-readiness-reevaluation-v04

    exact authorization base:
      a0f3af930940661e173b406686d7b4bfe2c7fbd1

Recommended implementation branch:

    impl/u05-nonprod-clinical-readiness-v1

If the implementation base changes materially before work begins:

    authorization validity must be rechecked.

If any RDP-01..RDP-06 semantic contract changes after authorization:

    authorization becomes stale
    -> stop implementation
    -> impact review / re-authorization required.

---

# 5. Allowed implementation scope — RDP-01

Implementation may add the U05 consumer-side non-production admission layer implementing the frozen RDP-01 semantics.

Allowed capabilities include:

    U05ConsumerInboundRequest

    U05AdmissionResult

    U05AdmittedInput

    U05ReadinessInputManifest
    reference-only assembly / validation

    route/gate/currentness checks

    A1 and continuation-context admission discrimination

    admission replay / idempotency

    RESTRICTED exact permission provenance continuity

    admission trace/provenance

Allowed result boundary:

    ADMITTED
    or frozen typed admission rejection/failure outcomes

RDP-01 implementation must not:

    recompute F1/F3/F5/F6 truth

    invent new applicability state

    invent a new upstream route

    bypass current U04 Gate/currentness

    activate external/live U04 -> U05 routing.

---

# 6. Allowed implementation scope — RDP-02

Implementation may add the deterministic U05 / D03 Clinical Readiness resolver.

Allowed:

    D03 DECIDED
    D03 INPUT_FAILURE
    D03 INPUT_CONFLICT

    six Clinical Readiness values only

    frozen P0-P7 precedence

    D03-POL-005
    D03-POL-011

    explicit:
      source_admission_ref
      source_readiness_input_set_identity
      restricted permission provenance

    frozen fail-closed behavior

The implementation must be deterministic over the admitted frozen input set.

It must not use:

    LLM
    Agent planning
    model inference
    external API
    tool call
    new clinical/safety capability

to decide Clinical Readiness.

POLICY_EXPECTATION_GAP remains:

    verification/design sentinel only

and must not become a runtime D03 status or seventh readiness value.

---

# 7. Allowed implementation scope — RDP-03

Implementation may add the U05 readiness mutation/provenance path.

Allowed:

    ClinicalReadinessStateValue

    ClinicalReadinessCommitEvidence

    CLINICAL_READINESS_EFFECT_ID

    canonical payload fingerprint

    deterministic/stable record/proposal identity

    K09 StateChangeProposal formation

    use of the existing G2/P01 commit boundary
    in the explicitly authorized non-production environment

    readiness-only commit

    readiness invalidation effect

    replay-first reconciliation

    conflict handling

    crash/recovery reconciliation

    P05 Trace/Audit evidence

    exact RESTRICTED permission provenance preservation

Actual non-production Clinical Readiness mutation is allowed only through:

    K09
    -> G2/P01
    -> authoritative non-production Clinical State

Direct state mutation remains prohibited.

---

# 8. Shared-runtime boundary

This authorization may consume existing:

    K09
    G2/P01
    Clinical State
    Trace/Audit
    Runtime/Effect-Ledger
    Safety/permission governance

interfaces as currently governed.

This authorization does NOT permit changing shared/core runtime semantics.

In particular, it does not authorize semantic modification of:

    K09 proposal rules
    G2/P01 commit semantics
    shared Clinical State ownership
    U04 Safety Gate policy
    shared permission policy
    canonical contract meaning
    generic Scheduler policy
    generic Runtime/Effect-Ledger semantics

If U05 implementation discovers that a shared/core runtime semantic change is necessary:

    STOP

    do not patch core semantics under AUTH-U05-RUNTIME-IMPL-001

    return for:
      shared-runtime impact review
      controlled contract/design amendment
      explicit additional authorization.

Local U05-side adapters or wrappers that only consume the existing shared boundary are allowed.

---

# 9. Allowed implementation scope — RDP-04

Implementation may add the U05 post-readiness routing projection up to the frozen non-production side-effect boundary.

Allowed:

    committed-readiness currentness validation

    deterministic readiness-to-target mapping

    U05DownstreamRoutingDecision

    DownstreamActionPermissionDecision
    using existing current Safety/permission authority

    U05_ROUTING_DECISION_ID

    routing-decision fingerprint

    eligible-only:
      U05_DOWNSTREAM_ROUTE_EFFECT_ID
      U05_DOWNSTREAM_ROUTE_AUTHORIZATION_ID
      U05DownstreamEligibility

    Runtime/Canonical Effect Ledger route evidence

    replay / stale / preemption / failure-governance handoff

    non-production test/fake Scheduler consumption evidence

The implementation slice may prove:

    one eligibility
    -> at most one Scheduler target intent

through a fake/spy/non-live harness.

It must NOT invoke real target Unit owner execution.

---

# 10. Explicit downstream boundary

The following live executions remain prohibited:

    U06 live question owner execution

    U08 live clinical-analysis / DDx execution

    U10 live offline-evidence action

    U11 live safe-exit/delivery execution

    U14 live/final recovery or business decision execution

For current U05 implementation:

    route eligibility
    != live downstream execution authorization

    Scheduler target intent evidence
    != target Unit invocation.

No implementation may silently route to a different target if the intended target binding is unavailable.

---

# 11. Upstream / external-entry boundary

The current authorization shape does not permit live external entry cutover.

Still prohibited:

    production U04 -> U05 route activation

    external /diagnosis endpoint cutover into U05

    live continuation router activation for real traffic

    real-patient request flow

Allowed:

    explicit test invocation

    synthetic fixture invocation

    non-production harness invocation

    manually controlled internal non-production integration test

provided the RDP-06 authorization profile is satisfied.

---

# 12. Non-production activation boundary

Implementation may use:

    explicit construction
    test wiring
    test/in-memory infrastructure
    dedicated non-production profile

It must not become automatically active in production/default runtime.

If Spring/runtime registration is used:

    activation must be explicitly non-production-profile gated

    production/default profile must remain inactive

    RDP-06 VG structural/runtime guard must prove this boundary.

---

# 13. Verification/evidence implementation scope — RDP-06

Authorization may include all implementation needed to execute the frozen RDP-06 verification plan:

    U05-EV-001..060 governed cases

    U05-HG-001 harness self-test

    U05-VG-001..006 verification gates

    pairwise D03 precedence matrix

    static independent expectation oracle

    reviewed synthetic fixture manifest

    precedence oracle

    focused tests

    replay/crash/conflict tests

    evidence harness

    expected/observed provenance equality evidence

    typed effect-count evidence

    exact-head verification workflow

    immutable full-SHA GitHub Action pins

    workflow/toolchain provenance

    SHA256/checksum bundle

    90-day raw CI artifact

    sanitized repository-governed accepted evidence snapshot support

Creating these artifacts does not itself mark verification PASS.

---

# 14. Expected implementation surface

The authorization review expects implementation to remain primarily in U05-owned surfaces, for example:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/**

    diagnosis-service/src/test/java/.../runtime/u05/**

    tools/u05_nonprod_verification/**

    .github/workflows/u05-nonprod-verification.yml

    U05 implementation verification/result/status governance documents

Equivalent repository paths are acceptable if existing module structure requires them.

The authorization does not grant broad permission to modify unrelated modules.

Under AUTH-U05-RUNTIME-IMPL-001:

    no non-U05 production/shared-runtime source file modification is authorized.

Allowed outside U05-owned production source is limited to:

    U05 tests
    test fakes/spies
    verification tools
    CI workflow
    U05 governance/evidence documents

and read-only consumption/import of existing shared runtime interfaces.

If implementation requires any shared production source modification:

    STOP

    do not include that change under AUTH-U05-RUNTIME-IMPL-001

    return for:
      shared-runtime impact review
      exact affected-file/surface analysis
      separate explicit authorization.

This rule is stricter than merely requiring the change to be contract-preserving.

---

# 15. Frozen semantics that implementation may not change

AUTH-U05-RUNTIME-IMPL-001 must not change:

    six Clinical Readiness values

    D03 statuses

    P0-P7 precedence

    D03-POL-005

    D03-POL-011

    RDP-05 applicability meanings

    F1/F3/F5/F6 ownership

    admission route contexts

    RESTRICTED permission provenance rules

    K09/P01 state ownership

    readiness effect/replay semantics

    readiness invalidation semantics

    readiness-to-target mapping

    downstream action-permission ownership

    U14 failure ownership

    non-production no-live-downstream boundary

    RDP-06 expected-authority model.

If code cannot be implemented without changing one of these:

    STOP
    -> return to controlled design amendment.

---

# 16. Clinical truth boundary

Implementation must preserve:

    Clinical Truth
    != Model Output
    != Capability Result
    != Runtime State
    != Trace

D03 does not create medical truth.

U05 must not:

    diagnose
    create Must-Exclude facts
    decide new offline examination medical need beyond frozen input semantics
    create Safety truth
    create delivery truth

It consumes frozen/authoritative upstream state and projects Clinical Readiness only.

---

# 17. Verification gate after implementation

Implementation is not complete merely because code exists or compiles.

Before implementation closure, all RDP-06 requirements must execute at one exact implementation head.

Required:

    compile PASS

    structural authorization guards PASS

    all 60 required EV cases PASS

    HG-001 PASS

    all 6 VG gates PASS

    all CONSTRUCTIBLE D03 precedence subcases PASS

    all NOT_CONSTRUCTIBLE pairs independently justified

    regression PASS

    policy_expectation_gap_count = 0
    for normal governed cases

    typed effect counts match

    exact provenance equalities match

    live downstream invocation count = 0

    external delivery count = 0

    unauthorized model/tool call count = 0

    production activation = false

    real patient traffic = false

    durable evidence bundle complete

    checksums valid.

---

# 18. Required post-implementation governance

After an implementation candidate exists, the mandatory order is:

    1. exact-head implementation/code-boundary review

    2. independent expectation-oracle review

    3. independent D03 precedence-oracle review

    4. independent fixture-manifest review

    5. freeze the reviewed oracle / precedence / fixture digests

    6. authoritative exact-head CI verification
       consuming exactly those reviewed digests

    7. independent evidence-only review

    8. combined implementation/evidence review

    9. explicit implementation closure record

The system under test must not generate its own expected oracle.

If expectation oracle, precedence oracle, or fixture manifest changes after its independent review:

    prior authoritative CI evidence becomes stale

    -> oracle/fixture re-review
    -> exact-head CI rerun
    -> evidence review rerun.

Only after all of those may the implementation slice become:

    VERIFIED / COMPLETE_FOR_AUTHORIZED_NONPRODUCTION_SLICE

That still does not authorize merge.

Merge requires a separate:

    Merge Authorization Review
    -> explicit repository-owner merge authorization
    -> standard merge commit only
    -> post-merge verification.

No squash/rebase merge is authorized.

---

# 19. Immediate prohibited actions

Even if AUTH-U05-RUNTIME-IMPL-001 is later explicitly authorized, the following remain prohibited:

    automatic merge

    squash merge

    rebase merge

    production Clinical Runtime enablement

    production Clinical State mutation

    production U04 -> U05 route activation

    external diagnosis entry cutover

    live U06/U08/U10/U11 execution

    external user delivery

    release activation

    real-patient traffic

    new clinical/safety policy invention.

---

# 20. Authorization invalidation conditions

AUTH-U05-RUNTIME-IMPL-001 eligibility becomes invalid if before or during implementation:

    any RDP-01..06 semantic contract changes

    readiness base is materially replaced

    a new open readiness blocker is found

    implementation requires a shared-runtime semantic change

    implementation requires a new clinical/safety truth dependency

    implementation requires live downstream execution

    implementation requires production routing/mutation

    implementation cannot satisfy RDP-06 without changing expected authority.

In those cases:

    STOP
    -> return to governance review.

---

# 21. Independent Authorization Review Remediation

Independent review:

    PR #180
    review_id = 5263950557
    verdict = REVISE_REQUIRED

Findings:

    BF-U05-IA-IR-01
    = SHARED_RUNTIME_SOURCE_CHANGE_SCOPE_TOO_BROAD

    BF-U05-IA-IR-02
    = ORACLE_FIXTURE_REVIEW_ORDER_NOT_EXPLICIT_IN_AUTHORIZATION_SEQUENCE

    BF-U05-IA-IR-03
    = U14_EXECUTION_EXCEPTION_LEAKS_AUTHORIZATION_SCOPE

Remediation:

    IR-01
    -> no non-U05 production/shared-runtime source modification
       is authorized under AUTH-U05-RUNTIME-IMPL-001

    IR-02
    -> oracle / precedence / fixture independent reviews
       are mandatory before authoritative CI
    -> reviewed digests must be the exact CI inputs

    IR-03
    -> U14 live/business execution is unconditionally outside
       the U05 authorization scope
    -> U05 may produce failure-governance handoff only

Current:

    BF-U05-IA-IR-01 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-IA-IR-02 = REMEDIATED / TARGETED_REVIEW_PENDING
    BF-U05-IA-IR-03 = REMEDIATED / TARGETED_REVIEW_PENDING

---

# 22. Candidate authorization review verdict

Based on the current exact frozen/refrozen package and the targeted fixes above:

    U05 Implementation Readiness
    = READY

    U05 Implementation Authorization Review
    = TARGETED_REVIEW_PENDING

Candidate result if targeted re-review passes:

    AUTH-U05-RUNTIME-IMPL-001
    = ELIGIBLE_FOR_EXPLICIT_REPOSITORY_OWNER_AUTHORIZATION

If targeted re-review passes and owner later approves, the exact authorization must remain:

    NON_PRODUCTION_ONLY
    FROZEN_RDP01_TO_RDP06_ONLY
    NO_LIVE_UPSTREAM_CUTOVER
    NO_LIVE_DOWNSTREAM_EXECUTION
    NO_PRODUCTION_MUTATION
    NO_SHARED_RUNTIME_SEMANTIC_CHANGE

This review itself does NOT grant implementation authorization.

Current:

    U05 Implementation Authorization
    = NOT_GRANTED

Next permitted governance step:

    targeted independent authorization re-review.

Only after PASS may an explicit repository-owner decision
for AUTH-U05-RUNTIME-IMPL-001 be opened.
