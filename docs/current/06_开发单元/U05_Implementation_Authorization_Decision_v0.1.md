# U05 Implementation Authorization Decision v0.1

> Decision ID: `AUTH-U05-RUNTIME-IMPL-001`  
> Decision status: **NOT_DECIDED / TARGETED_GATE_REVIEW_PENDING**  
> Readiness basis: U05 Implementation Readiness Re-Evaluation v0.4 / PR #179  
> Authorization Review: PR #180  
> Reviewed semantic authorization head: `33001ae6301d250c6485df1284e5e78d8faf64ea`  
> Authorization Review status/provenance head: `8cbf2ed81d25fda5944bc19dcf88587fdd94a7fd`  
> Targeted Independent Authorization Re-Review: **PASS** / review_id `5263958905`  
> Status-sync verification: **PASS** / review_id `5263963867`  
> This package does not itself grant implementation authorization.

---

# 1. Owner decision question

Whether to authorize implementation of the current frozen U05 non-production Clinical Readiness slice under:

    AUTH-U05-RUNTIME-IMPL-001

with the exact bounded shape defined below.

This is not a production authorization decision.

This is not a merge authorization decision.

This is not a live downstream-routing authorization decision.

---

# 2. Preconditions

Confirmed:

    U05 Definition / Business-Semantic Readiness
    = READY

    U05 Aggregate Contract Compatibility
    = READY

    U05 Implementation Readiness
    = READY

    U05 Implementation Authorization Review
    = PASS

    BF-U05-RG-01..06
    = CLOSED

    BF-U05-AGR-01..03
    = CLOSED_BY_REFREEZE

    BF-U05-IA-IR-01..03
    = CLOSED

Therefore:

    AUTH-U05-RUNTIME-IMPL-001
    = ELIGIBLE_FOR_EXPLICIT_REPOSITORY_OWNER_AUTHORIZATION

but:

    U05 Implementation Authorization
    = NOT_GRANTED
    until owner explicitly chooses AUTHORIZE.

---

# 3. Exact authorization shape

If owner chooses AUTHORIZE:

    AUTH-U05-RUNTIME-IMPL-001
    = AUTHORIZED
    / NON_PRODUCTION_ONLY
    / FROZEN_RDP01_TO_RDP06_ONLY
    / NO_LIVE_UPSTREAM_CUTOVER
    / NO_LIVE_DOWNSTREAM_EXECUTION
    / NO_PRODUCTION_MUTATION
    / NO_SHARED_RUNTIME_SEMANTIC_CHANGE

No broader interpretation is allowed.

---

# 4. Exact implementation lineage

Reviewed implementation semantic/governance base:

    review/u05-implementation-authorization-review

    exact reviewed base:
      8cbf2ed81d25fda5944bc19dcf88587fdd94a7fd

However implementation must NOT branch directly from that pre-owner-decision head.

If owner chooses AUTHORIZE:

    1. the owner decision must first be recorded in this decision package;

    2. that decision-record commit must be a descendant of:
         8cbf2ed81d25fda5944bc19dcf88587fdd94a7fd

    3. the decision record must explicitly contain:
         AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED
         exact authorization shape
         reviewed authorization head
         owner decision provenance

    4. the actual implementation branch point is:
         the final owner-authorized decision-record head.

Recommended implementation branch:

    impl/u05-nonprod-clinical-readiness-v1

Therefore:

    reviewed semantic/governance base
    != actual implementation branch point

The implementation lineage must contain the explicit owner authorization record.

If implementation branches from a commit that does not contain:

    AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED

then:

    AUTHORIZATION_LINEAGE_CHECK = FAIL
    implementation must not begin.

If the reviewed base or authorization scope changes materially before implementation begins:

    STOP
    -> revalidate authorization applicability.

If any U05 RDP semantic contract changes:

    authorization becomes stale
    -> STOP
    -> impact review / re-authorization.

---

# 5. Authorized implementation — RDP-01

Allowed:

    U05ConsumerInboundRequest

    U05AdmissionResult

    U05AdmittedInput

    U05ReadinessInputManifest
    reference-only assembly / validation

    route / Gate / currentness checks

    A1 vs continuation admission discrimination

    admission replay/idempotency

    exact RESTRICTED permission provenance continuity

    admission trace/provenance.

Not allowed:

    recomputing F1/F3/F5/F6 truth

    inventing applicability

    inventing route authorization

    bypassing U04 Safety/currentness

    live U04 -> U05 activation.

---

# 6. Authorized implementation — RDP-02

Allowed:

    deterministic D03 resolver

    DECIDED
    INPUT_FAILURE
    INPUT_CONFLICT

    six frozen Clinical Readiness values

    P0-P7 precedence

    D03-POL-005

    D03-POL-011

    source_admission_ref

    source_readiness_input_set_identity

    exact admitted RESTRICTED permission provenance

    frozen fail-closed semantics.

Not allowed:

    LLM/model/Agent ownership of D03

    external API/tool/model call to determine readiness

    seventh readiness value

    POLICY_EXPECTATION_GAP as runtime D03 status

    new clinical/business truth.

---

# 7. Authorized implementation — RDP-03

Allowed:

    ClinicalReadinessStateValue

    ClinicalReadinessCommitEvidence

    CLINICAL_READINESS_EFFECT_ID

    canonical payload fingerprint

    stable record/proposal identity

    K09 StateChangeProposal

    existing G2/P01 non-production commit boundary usage

    readiness-only commit

    readiness invalidation effect

    replay-first reconciliation

    conflict handling

    crash recovery

    P05 trace/audit

    exact permission/admission/input-set provenance equality.

Actual non-production Clinical Readiness mutation is allowed only through:

    K09
    -> existing G2/P01
    -> authoritative non-production Clinical State.

Direct state mutation is prohibited.

---

# 8. Shared/core runtime boundary

AUTH-U05-RUNTIME-IMPL-001 may only consume existing shared/runtime interfaces.

Allowed:

    read-only imports/calls to current:
      K09
      G2/P01
      Clinical State
      Trace/Audit
      Runtime/Effect-Ledger
      Safety/permission governance

    U05-owned adapters/wrappers

    U05 tests/fakes/spies

    U05 verification tooling/workflow/docs.

Not authorized:

    any non-U05 production/shared-runtime source modification

    K09 semantic change

    G2/P01 semantic change

    shared Clinical State ownership change

    U04 Safety policy change

    shared permission policy change

    generic Scheduler semantic change

    generic Runtime/Effect-Ledger semantic change

    canonical frozen contract semantic change.

If implementation discovers any required non-U05 production/shared source modification:

    STOP

    -> shared-runtime impact review
    -> separate explicit authorization.

Do not include it under this authorization.

---

# 9. Authorized implementation — RDP-04

Allowed:

    committed-readiness currentness validation

    deterministic readiness-to-target projection

    U05DownstreamRoutingDecision

    route-time DownstreamActionPermissionDecision
    using existing Safety/permission authority

    U05_ROUTING_DECISION_ID

    route fingerprints

    eligible-only:
      U05_DOWNSTREAM_ROUTE_EFFECT_ID
      U05_DOWNSTREAM_ROUTE_AUTHORIZATION_ID
      U05DownstreamEligibility

    Runtime/Canonical Effect Ledger route evidence

    replay/stale/preemption/failure-handoff semantics

    non-live test/fake Scheduler target-intent evidence.

Not authorized:

    live Scheduler dispatch to target owner

    U06 live execution

    U08 live execution

    U10 live execution

    U11 live execution

    U14 live/final/business execution.

U05 may produce only typed failure-governance handoff evidence.

---

# 10. Upstream / external-entry boundary

Not authorized:

    production U04 -> U05 activation

    external /diagnosis cutover to U05

    live continuation-router activation for real traffic

    real-patient flow.

Allowed:

    synthetic fixture invocation

    explicit test invocation

    non-production harness invocation

    manually controlled internal non-production integration tests

provided no live route activation or real-patient source is used.

---

# 11. Runtime activation boundary

Allowed:

    explicit construction

    test wiring

    in-memory infrastructure

    dedicated non-production profile.

If Spring/runtime registration is used:

    it must be explicitly non-production-profile gated

    production/default runtime must remain inactive

    RDP-06 VG guards must prove the boundary.

No automatic production/default activation is authorized.

---

# 12. Authorized verification/evidence implementation — RDP-06

Allowed implementation includes:

    U05-EV-001..060

    U05-HG-001

    U05-VG-001..006

    pairwise D03 precedence matrix

    expectation oracle

    precedence oracle

    reviewed synthetic fixture manifest

    focused tests

    replay/crash/conflict tests

    evidence harness

    provenance equality evidence

    typed effect-count evidence

    exact-head CI workflow

    immutable full-SHA third-party Action pins

    workflow/toolchain provenance

    SHA256 evidence bundle

    raw CI artifact retention

    sanitized accepted evidence snapshot support.

No test/evidence artifact may fabricate expected business truth from the system under test.

---

# 13. Mandatory post-implementation evidence order

If authorized and an implementation candidate exists:

    1. exact-head implementation/code-boundary review

    2. independent expectation-oracle review

    3. independent D03 precedence-oracle review

    4. independent fixture-manifest review

    5. freeze reviewed oracle/precedence/fixture digests

    6. authoritative exact-head CI
       consumes exactly those reviewed digests

    7. independent evidence-only review

    8. combined implementation/evidence review

    9. explicit implementation closure record.

If oracle / precedence oracle / fixture manifest changes after review:

    prior authoritative evidence is stale

    -> re-review
    -> exact-head CI rerun
    -> evidence review rerun.

---

# 14. Required implementation verification

Implementation closure requires at minimum:

    compile PASS

    structural authorization guards PASS

    all 60 EV cases PASS

    HG-001 PASS

    all 6 VG gates PASS

    all CONSTRUCTIBLE precedence subcases PASS

    all NOT_CONSTRUCTIBLE pairs independently justified

    regression PASS

    normal-case policy_expectation_gap_count = 0

    exact provenance equalities PASS

    typed effect counts match

    live downstream invocation count = 0

    external delivery count = 0

    unauthorized model/tool call count = 0

    production activation = false

    production mutation = false

    real-patient traffic = false

    durable evidence complete

    checksums valid.

---

# 15. Expected repository surface

Implementation is expected to remain primarily in:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/**

    diagnosis-service/src/test/java/.../runtime/u05/**

    tools/u05_nonprod_verification/**

    .github/workflows/u05-nonprod-verification.yml

    U05 verification/evidence governance documents.

Equivalent U05-owned paths are acceptable.

No non-U05 production/shared-runtime source modification is included in this authorization.

---

# 16. Authorization does not imply completion

Even after AUTHORIZE:

    CODE EXISTS
    != IMPLEMENTED
    != WIRED
    != VERIFIED
    != BUSINESS LOOP CLOSED
    != CLINICALLY EVALUATED
    != PRODUCTION AUTHORIZED.

Implementation must still pass the full RDP-06 and independent-review chain.

---

# 17. Merge boundary

AUTH-U05-RUNTIME-IMPL-001 does not authorize merge.

After implementation verification/closure:

    separate Merge Authorization Review

must PASS.

Then a repository owner must explicitly authorize merge.

Only:

    STANDARD MERGE COMMIT

is allowed.

Not allowed:

    automatic merge
    squash
    rebase.

Post-merge verification remains mandatory.

---

# 18. Production / live boundary

Still prohibited even after AUTHORIZE:

    production Clinical Runtime enablement

    production Clinical State mutation

    production U04 -> U05 route activation

    external diagnosis entry cutover

    live U06/U08/U10/U11/U14 execution

    external user delivery

    release activation

    clinical production authorization

    real-patient traffic.

These require separate future governance.

---

# 19. Authorization invalidation

Authorization becomes stale and implementation must STOP if:

    any RDP-01..06 semantic contract changes

    implementation base materially changes before work starts

    a new readiness blocker appears

    shared production source modification becomes necessary

    new clinical/safety dependency becomes necessary

    live downstream execution becomes necessary

    production mutation/routing becomes necessary

    RDP-06 expected authority would need modification to make implementation pass.

No “small exception” is implied.

---

# 20. Owner options

Owner may choose exactly one:

    AUTHORIZE

    REVISE

    REJECT

## AUTHORIZE

Sets:

    AUTH-U05-RUNTIME-IMPL-001
    = AUTHORIZED

with the exact bounded shape:

    NON_PRODUCTION_ONLY
    FROZEN_RDP01_TO_RDP06_ONLY
    NO_LIVE_UPSTREAM_CUTOVER
    NO_LIVE_DOWNSTREAM_EXECUTION
    NO_PRODUCTION_MUTATION
    NO_SHARED_RUNTIME_SEMANTIC_CHANGE

Then implementation may begin from the exact authorized base.

## REVISE

No implementation begins.

The authorization package must be changed and independently re-reviewed.

## REJECT

No implementation begins.

U05 remains implementation-ready but unauthorized.

---

# 21. Independent Gate Review Remediation

Independent Gate Review:

    PR #181
    review_id = 5263974730
    verdict = REVISE_REQUIRED

Finding:

    BF-U05-IA-DG-01
    = AUTHORIZATION_RECORD_NOT_REQUIRED_IN_IMPLEMENTATION_LINEAGE

Remediation:

    reviewed semantic/governance base remains:
      8cbf2ed81d25fda5944bc19dcf88587fdd94a7fd

    actual implementation branch point is now:
      final owner-authorized decision-record head

    implementation branch must inherit:
      AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED
      in its Git lineage.

Current:

    BF-U05-IA-DG-01
    = REMEDIATED / TARGETED_GATE_REVIEW_PENDING

    Decision Package
    = REVISED / READY_FOR_TARGETED_GATE_REVIEW

---

# 22. Current state

Until explicit owner decision:

    AUTH-U05-RUNTIME-IMPL-001
    = NOT_DECIDED

    U05 Implementation Readiness
    = READY

    U05 Implementation Authorization Review
    = PASS

    U05 Implementation Authorization
    = NOT_GRANTED

    U05 Runtime/Code Implementation
    = NOT_AUTHORIZED

    Merge Authorization
    = NOT_GRANTED

    Production Authorization
    = BLOCKED

    Real-patient traffic
    = NOT_AUTHORIZED
