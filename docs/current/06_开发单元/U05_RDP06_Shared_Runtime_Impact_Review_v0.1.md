# U05 RDP-06 Shared Runtime Impact Review v0.1

> Review trigger: `BF-U05-RDP06-AV-01..03`  
> Finding source: PR #201 / review_id `5274870345`  
> Exact reviewed implementation head: `1dc49c4097841523a9445dab078bc5a3d1ad1259`  
> U05 implementation authorization: `AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED`  
> Status: **PROPOSED / INDEPENDENT_IMPACT_REVIEW_PENDING**  
> This document authorizes no shared-runtime implementation.

---

# 1. Review question

Determine whether remediation of the three RDP-06 preflight blockers requires:

    non-U05 production/shared-runtime source modification
    generic Scheduler semantic change
    generic Runtime/Effect-Ledger semantic change
    Binding/Release governance semantic change
    new production wiring

or whether the blockers can be resolved entirely inside the already-authorized U05 implementation / verification boundary.

---

# 2. Existing authorization boundary

`AUTH-U05-RUNTIME-IMPL-001` permits:

    U05-owned production implementation
    U05 tests/fakes/spies
    U05 verification tooling
    read-only consumption of existing shared/runtime interfaces
    non-live test/fake Scheduler target-intent evidence.

It explicitly does not permit:

    non-U05 production/shared-runtime source modification
    generic Scheduler semantic change
    generic Runtime/Effect-Ledger semantic change
    live Scheduler dispatch
    live U06/U08/U10/U11/U14 execution.

Therefore any required shared production change must be separately identified and authorized.

---

# 3. Impact review — AV-01

Finding:

    BF-U05-RDP06-AV-01
    = INPUT_CONFLICT_REQUIRED_CASE_UNREPRESENTABLE.

Affected implementation is entirely U05-owned:

    U05ReadinessInputManifest
    U05ClinicalReadinessPolicy only if needed
    U05 focused tests.

The current defect is that a U05 manifest constructor eliminates a profile before the already-implemented D03 P1 branch can consume it.

No shared runtime component owns:

    manifest cardinality
    readiness input-set ordering
    D03 conflict interpretation.

Shared runtime impact:

    NONE.

Required shared source change:

    NO.

---

# 4. Impact review — AV-02

Finding:

    BF-U05-RDP06-AV-02
    = APPLICABILITY_EVIDENCE_MISSING_TYPED_ADMISSION_PATH_ABSENT.

Affected implementation is entirely U05-owned:

    U05ReadinessInput
    U05AdmissionService
    U05 focused tests.

The defect is boundary placement:

    constructor rejects too early
    while frozen RDP-01 requires a typed admission rejection.

No shared runtime component owns:

    RDP-01 admission reason code
    readiness applicability evidence validation
    U05 admitted-snapshot creation.

Shared runtime impact:

    NONE.

Required shared source change:

    NO.

---

# 5. Impact review — AV-03 initial concern

Finding source initially classified:

    BF-U05-RDP06-AV-03
    = OPEN / BLOCKING / CROSS_RUNTIME_IMPACT.

Reason:

    the exact repository target contains no generic production Scheduler
    consuming U05DownstreamEligibility.

The repository does contain existing shared governance/runtime capability including:

    Runtime Canonical Effect Ledger
    BindingReleaseResolver / Capability Binding governance.

But it does not contain a production U05 eligibility-consumption Scheduler path.

If RDP-06 required a production Scheduler SUT, a separate shared-runtime project would be mandatory.

That is not, however, the current authorized verification requirement.

---

# 6. Governing RDP-06 evidence boundary

RDP-06 requires EV cases to execute actual implementation/runtime/governance surfaces.

It also explicitly states that observed evidence may be extracted from:

    Scheduler target intent spy/fake.

Typed side-effect evidence may point to:

    Scheduler intent spy/fake.

The implementation authorization independently permits:

    non-live test/fake Scheduler target-intent evidence.

Therefore:

    verification-only Scheduler consumer fake/spy
    is an explicitly authorized executable evidence surface

provided:

    it consumes real U05DownstreamEligibility
    it does not generate expected authority
    observed facts come from returned/ledger objects
    no downstream Unit is invoked
    no production/shared Scheduler semantics are changed.

---

# 7. Existing shared capability sufficient for EV-058 durability

The repository already has the separately verified generic:

    CanonicalEffectLedger
    NonProductionFileCanonicalEffectLedger.

It is:

    generic
    non-clinical
    non-production
    opaque-payload based
    durable across reconstruction/process evidence.

A verification-only Scheduler fake may consume this existing interface to prove:

    same eligibility
    -> same test target-intent identity
    -> durable reattach
    -> no duplicate authoritative test intent.

This is read-only consumption of an existing shared boundary from the perspective of shared source semantics.

No new Effect Ledger feature is required.

---

# 8. Binding/Release impact for EV-057

Frozen EV-057 requires:

    target execution binding unavailable
    -> no target invocation
    -> failure-governance handoff
    -> no silent alternate clinical route.

The repository contains:

    BindingReleaseResolver

for governed capability-binding resolution.

But current U05 RDP-06 is not a conformance verification of production P06 binding resolution.

The verification fixture may inject:

    target binding availability = AVAILABLE / UNAVAILABLE

through a test-only binding availability fake/probe.

This input is an environmental/governance condition for the non-live Scheduler consumer.

Expected EV-057 authority remains the independently reviewed RDP-04/RDP-06 oracle.

Therefore the test fake does not become business truth.

No change to:

    BindingReleaseResolver
    CapabilityBindingRegistry
    CapabilityBindingRecord
    production repository wiring

is required.

If a future verification goal changes to:

    prove real production BindingReleaseResolver integration
    or
    run a real generic Scheduler

that is a different scope and must receive a separate impact review and authorization.

---

# 9. Why no generic Scheduler should be added now

Adding a production generic Scheduler merely to satisfy current U05 verification would be broader than necessary.

It would introduce new questions not required for current non-production U05 closure:

    generic target-intent contract
    generic scheduling policy
    multi-Unit fairness/priority
    execution ownership
    failure retry semantics
    production persistence
    runtime activation
    downstream dispatch wiring.

Those are architecture/runtime responsibilities beyond:

    U05 Clinical Readiness verification.

The existing authorization intentionally stops before live dispatch.

Therefore minimum-change governance requires:

    no production Scheduler addition in this remediation.

---

# 10. AV-03 revised impact classification

Proposed final classification:

    BF-U05-RDP06-AV-03
    = U05_VERIFICATION_ONLY_SCHEDULER_CONSUMER_SURFACE_GAP

    Shared Runtime Semantic Change
    = NOT_REQUIRED

    Shared Production Source Modification
    = NOT_REQUIRED

    Existing Shared Capability Consumption
    = PERMITTED / READ_ONLY

The finding remains blocking until the verification-only executable surface exists.

This review changes classification only.

It does not close the finding.

---

# 11. Allowed AV-03 implementation surface

Allowed:

    diagnosis-service/src/test/java/.../runtime/u05/**
    tools/u05_nonprod_verification/**
    U05 verification fixtures/oracle/evidence tooling

and read-only use of:

    CanonicalEffectLedger
    NonProductionFileCanonicalEffectLedger
    existing currentness/governance objects.

Potential test-only objects:

    U05NonLiveSchedulerConsumerFake
    U05SchedulerConsumptionObservation
    U05TargetBindingAvailabilityFake
    U05SchedulerTargetInvocationSpy.

No shared production package modification is required.

---

# 12. Prohibited shared-runtime changes under this review

This review does not authorize:

    new production Scheduler implementation
    generic Scheduler policy
    production target dispatch
    generic target execution intent persistence
    generic failure router implementation
    changes to BindingReleaseResolver
    changes to CapabilityBindingRegistry
    changes to CanonicalEffectLedger semantics
    changes to NonProductionFileCanonicalEffectLedger semantics
    changes to RuntimeBindingService
    production Spring activation
    U14 execution.

If any of those become necessary:

    STOP
    -> new shared-runtime impact review
    -> bounded design
    -> independent review
    -> explicit owner authorization.

---

# 13. Exact shared files expected to remain unchanged

At minimum, remediation should not modify:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/**
    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/**
    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/governance/**

unless a new impact review is opened.

A targeted exact-head review must check this file boundary.

---

# 14. Authorization decision requirement

For AV-01 and AV-02:

    existing AUTH-U05-RUNTIME-IMPL-001
    is sufficient
    if implementation remains contract-preserving.

For AV-03:

    existing AUTH-U05-RUNTIME-IMPL-001
    is sufficient for the verification-only fake/spy surface.

Therefore this impact review proposes:

    no new shared-runtime implementation authorization ID.

However the RDP-06 verifier itself is exact-target pinned.

After U05 remediation changes the implementation SHA:

    verifier target rebind
    is required before authoritative CI.

That is a verification authorization-target issue,
not shared-runtime implementation authorization.

---

# 15. Required shared-runtime guard evidence after remediation

Exact-head targeted review must prove:

    git diff contains no shared production source modification

    CanonicalEffectLedger behavior unchanged

    BindingReleaseResolver behavior unchanged

    no generic Scheduler production source added

    no Spring/default-profile activation added

    no downstream Unit production import/invocation added.

RDP-06 VG gates must continue to prove:

    live downstream invocation count = 0
    production activation = false
    external delivery count = 0
    unauthorized model/tool call count = 0.

---

# 16. Impact verdict

Proposed verdict:

    AV-01 Shared Runtime Impact
    = NONE

    AV-02 Shared Runtime Impact
    = NONE

    AV-03 Shared Runtime Impact
    = NO_SHARED_PRODUCTION_CHANGE_REQUIRED
      / VERIFICATION_ONLY_CONSUMER_SURFACE_REQUIRED

    New Shared Runtime Authorization
    = NOT_REQUIRED

    U05 Targeted Remediation
    = ELIGIBLE_TO_PROCEED_UNDER_EXISTING_AUTHORIZATION
      after independent review of this package.

This verdict does not itself authorize implementation beyond the existing owner-approved U05 scope.
