# U05 RDP-06 Authoritative Verifier Target Rebind Amendment v0.1

> Amendment type: exact-target rebind only  
> Parent verifier authorization: `AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001 = AUTHORIZED`  
> Parent authorized decision head: `c591909f81e4725d1cf0c5c32b51264ca91207fc`  
> Prior exact target: `1dc49c4097841523a9445dab078bc5a3d1ad1259`  
> Proposed new exact target: `261ee5525c8260e93db19173ffbde89a8af6810d`  
> Status: **PROPOSED / INDEPENDENT_TARGET_REBIND_REVIEW_PENDING**  
> This amendment changes no verifier semantics, no runtime scope and no production authorization.

---

# 1. Why rebind is required

The original verifier authorization freezes:

    U05_IMPLEMENTATION_SHA
    = 1dc49c4097841523a9445dab078bc5a3d1ad1259

and explicitly states:

    any target drift invalidates the authorization
    and requires a new review/authorization decision.

RDP-06 pre-execution review on that target found:

    BF-U05-RDP06-AV-01
    BF-U05-RDP06-AV-02
    BF-U05-RDP06-AV-03.

Those blockers were reviewed in PR #205 and remediated in PR #206.

Therefore the old exact target must remain historical and must not be silently rewritten.

---

# 2. New implementation candidate basis

New exact implementation candidate:

    261ee5525c8260e93db19173ffbde89a8af6810d

Implementation PR:

    #206

Reviewed remediation design / impact package:

    PR #205
    final package head:
      457f302e3f987843f1bdfd4e4236179980f87364

    targeted independent design re-review:
      PASS
      review_id = 5274983415

Exact-head implementation re-review:

    PR #206
    review_id = 5275319103
    verdict = PASS_FOR_REBIND

Accepted engineering smoke:

    PR #207
    run_id = 35700739442
    job_id = 106657871560

    compile = PASS
    structural authorization guards = PASS
    focused U05 = 30 / failures 0 / errors 0 / skipped 0

This engineering smoke remains non-authoritative.

---

# 3. Blocker disposition at the new target

At exact head:

    261ee5525c8260e93db19173ffbde89a8af6810d

the exact-head implementation review concluded:

    BF-U05-RDP06-AV-01
    = CLOSED_AS_IMPLEMENTATION_SURFACE_BLOCKER

    BF-U05-RDP06-AV-02
    = CLOSED_AS_IMPLEMENTATION_SURFACE_BLOCKER

    BF-U05-RDP06-AV-03
    = CLOSED_AS_IMPLEMENTATION_SURFACE_BLOCKER

This does not mean:

    RDP-06 PASS
    U05 Implementation Verification PASS
    merge authorized
    production authorized.

It means only that the previously missing required executable surfaces are now present and the verifier may be rebound for authoritative testing.

---

# 4. Exact rebind rule

If explicitly owner-authorized, the parent verifier authorization is rebound as:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = AUTHORIZED

with:

    parent authorization
    = AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-001

    U05_IMPLEMENTATION_SHA
    = 261ee5525c8260e93db19173ffbde89a8af6810d

The old target remains:

    1dc49c4097841523a9445dab078bc5a3d1ad1259
    = HISTORICAL / NOT_RUN / SUPERSEDED_FOR_FUTURE_RDP06_EXECUTION

No evidence from the old target may be relabeled as evidence for the new target.

---

# 5. Verifier semantics remain unchanged

The following remain exactly as reviewed in PR #203:

    verification-overlay topology
    exact RDP-01..06 authority binding
    referenced Phase 5/6/8/9 authority binding
    60 EV cases
    U05-HG-001
    U05-VG-001..006
    expectation oracle
    D03 precedence oracle
    fixture manifest
    typed effect-count evidence
    exact provenance-equality evidence
    G0..G15 workflow gates
    immutable full-SHA action pins
    toolchain/workflow provenance
    >=90-day durable evidence artifact
    independent evidence-only review
    no workflow self-declared final PASS.

No verifier path inventory is expanded by this amendment.

No expected business result is changed by this amendment.

---

# 6. Frozen contract identity applicability

The remediation implementation does not change:

    RDP-01
    RDP-02
    RDP-03
    RDP-04
    RDP-05
    RDP-06

or referenced:

    Phase 5
    Phase 6
    Phase 8
    Phase 9

frozen authority documents.

Therefore the contract authority-core identities frozen by the reviewed verifier design remain applicable.

Before authoritative execution, the verifier must still recompute and fail closed on any actual blob drift.

---

# 7. Oracle / precedence / fixture applicability

The verifier overlay/oracle/fixture implementation has not yet been committed/exact-head reviewed.

Therefore there are no previously accepted oracle/precedence/fixture digests to carry forward from the old implementation target.

Required sequence remains:

    implement verifier overlay against the rebound target
    -> independent overlay/oracle/precedence/fixture review
    -> freeze exact reviewed digests
    -> authoritative CI consumes those same digests.

No digest is grandfathered by this rebind.

---

# 8. Exact implementation-target delta bound by rebind review

Relative to prior implementation target:

    1dc49c4097841523a9445dab078bc5a3d1ad1259

the proposed new target:

    261ee5525c8260e93db19173ffbde89a8af6810d

has exactly six changed paths.

## 8.1 U05 production remediation — 3 files

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/U05AdmissionService.java
    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/U05ReadinessInput.java
    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/U05ReadinessInputManifest.java

## 8.2 U05 focused remediation tests — 1 file

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/U05NonProductionClinicalReadinessTest.java

## 8.3 Reviewed governance/design lineage — 2 files

    docs/current/06_开发单元/U05_RDP06_Targeted_Remediation_Design_v0.1.md
    docs/current/06_开发单元/U05_RDP06_Shared_Runtime_Impact_Review_v0.1.md

These two documents are the reviewed PR #205 remediation/impact package and contain no runtime implementation.

Therefore the exact target-delta inventory is:

    6 files total
    = 3 U05 production
    + 1 U05 focused test
    + 2 reviewed governance/design docs.

No shared production source is modified.

If comparison of the prior target and proposed target yields any seventh path, deleted path, renamed path, or different production/shared-runtime path:

    TARGET_REBIND_DELTA_GUARD = FAIL
    -> stop
    -> new impact/rebind review required.

---

# 8A. Exact target-authority precedence

The parent verifier design and parent authorization remain immutable historical records and still literally contain:

    U05_IMPLEMENTATION_SHA
    = 1dc49c4097841523a9445dab078bc5a3d1ad1259

This rebind does not edit those historical records.

For the implementation-target identity field only, authority precedence is frozen as:

    latest explicitly owner-authorized
    U05 RDP-06 verifier target-rebind record

    >

    parent verifier authorization target field.

This override applies only to:

    U05_IMPLEMENTATION_SHA
    exact implementation target identity.

It does not override:

    verifier scope
    EV/HG/VG inventory
    frozen contract identities
    oracle derivation rules
    evidence schemas
    workflow gates
    artifact requirements
    production/live prohibitions.

Verifier implementation/workflow must fail closed unless it resolves exactly one active authorized target.

Required resolution guard:

    parent authorization exists
    + parent target is historical after approved remediation
    + exactly one active owner-authorized rebind exists
    + rebind parent_authorization_id matches
    + workflow-pinned U05_IMPLEMENTATION_SHA equals rebind target SHA
    -> target identity accepted.

Failure cases:

    zero active authorized rebind records
    -> FAIL

    more than one active authorized rebind record
    -> FAIL

    rebind parent authorization mismatch
    -> FAIL

    workflow-pinned SHA != authorized rebind SHA
    -> FAIL.

No implicit "latest commit on branch" resolution is permitted.


---

# 9. Authorization scope after rebind

If owner authorizes the rebind, the permitted scope remains:

    VERIFICATION_ONLY
    PINNED_TARGET_SHA_ONLY
    FROZEN_RDP06_ONLY
    SYNTHETIC_NON_PHI_ONLY
    NO_PRODUCTION_RUNTIME_SOURCE_CHANGE
    NO_SHARED_RUNTIME_SEMANTIC_CHANGE
    NO_LIVE_UPSTREAM_CUTOVER
    NO_LIVE_DOWNSTREAM_EXECUTION
    NO_EXTERNAL_DELIVERY
    NO_REAL_PATIENT_TRAFFIC
    NO_MERGE.

The rebind does not authorize implementation changes to the U05 candidate.

Any further candidate-head drift after rebind:

    invalidates this rebind
    -> new exact-head review
    -> new target rebind decision.

---

# 10. Mandatory post-rebind sequence

After explicit owner rebind authorization:

    1. implement the already-reviewed verification-only overlay
       against exact target 261ee552...

    2. independent exact-head overlay review

    3. independent expectation-oracle review

    4. independent D03 precedence-oracle review

    5. independent fixture-manifest review

    6. freeze reviewed digests

    7. authoritative exact-target CI

    8. durable evidence generation

    9. independent evidence-only review

    10. combined implementation/evidence review

    11. explicit U05 implementation-verification closure record.

A green workflow remains:

    PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW

until independent evidence acceptance.

---

# 11. Owner decision requirement

Because the parent authorization explicitly says target drift requires a new review/authorization decision, this amendment does not self-authorize.

Proposed authorization:

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001

Current:

    NOT_GRANTED

Owner options after independent rebind review:

    AUTHORIZE
    REVISE
    REJECT.

No authoritative RDP-06 run may begin against the new target until this exact rebind is explicitly authorized.


---

# 12. Independent Rebind Review Remediation

Initial Independent Review:

    PR #208
    review_id = 5275336344
    verdict = REVISE_REQUIRED

Findings:

    BF-U05-RDP06-RB-IR-01
    = EXACT_TARGET_DELTA_INVENTORY_INCOMPLETE

    BF-U05-RDP06-RB-IR-02
    = TARGET_AUTHORITY_PRECEDENCE_NOT_EXPLICIT

Remediation:

    RB-IR-01
    -> exact prior-target -> new-target delta frozen as six paths:
       3 U05 production
       1 focused U05 test
       2 reviewed PR #205 governance/design docs
    -> any additional path invalidates the rebind basis.

    RB-IR-02
    -> target-only authority precedence frozen:
       latest explicit owner-authorized rebind
       > historical parent target field
    -> exactly-one-active-rebind resolution and workflow SHA equality
       are mandatory fail-closed guards.

Current:

    BF-U05-RDP06-RB-IR-01
    = REMEDIATED / TARGETED_RE_REVIEW_PENDING

    BF-U05-RDP06-RB-IR-02
    = REMEDIATED / TARGETED_RE_REVIEW_PENDING

    U05 RDP-06 Verifier Exact-Target Rebind
    = REVISED / READY_FOR_TARGETED_INDEPENDENT_RE_REVIEW

    AUTH-U05-RDP06-AUTHORITATIVE-VERIFIER-REBIND-001
    = NOT_GRANTED
