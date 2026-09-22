# U05 M2 Integration Baseline Verification Runner — Explicit Owner Authorization Decision v0.1

> Authorization ID: `AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001`  
> Decision type: **Repository Owner Explicit Verification-Runner Authorization**  
> Exact reviewed design head: `54d61f71f39abd651ba721428afb1ab4c71a78bf`  
> Design PR: #198  
> Targeted Independent Re-Review: PASS / review_id `5273800024`  
> Current status: **AUTHORIZED / OWNER_APPROVED**  
> This package itself grants no workflow modification authorization.

---

# 1. Decision question

Should the repository owner authorize implementation of:

    U05-M2-INTEGRATION-BASELINE-VERIFIER-01

under:

    AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001

for executable verification of the exact M2 integration baseline?

---

# 2. Exact verification target

    M2 target SHA
    = d620df6361cec2f757b01341918dfeef115a6475

    original U05 SHA
    = f98163e5a8dd038280cdfa3f66b9172abaef133d

    M2 PBNC merge SHA
    = 9c997fbefb58fa97664716bb6d5681557b01044f

    PBNC-02A closure SHA
    = 95a49831c29165b86300af9f6bdd8e76952177a7

    Effect Ledger closure SHA
    = 5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2

Changing any of these exact SHAs is outside this authorization.

---

# 3. Exact authorized repository-file scope if approved

Authorization permits adding exactly one file:

    .github/workflows/u05-m2-integration-baseline-verification.yml

No other file may be modified under the verifier implementation.

---

# 4. Exact verification topology if approved

Create:

    verify/u05-m2-integration-baseline-v1

from the exact Owner-authorized decision-record commit produced by this authorization.

Open a PR whose:

    head =
      verify/u05-m2-integration-baseline-v1

    base =
      decision/u05-m2-integration-baseline-verification-authorization

The workflow may trigger only for that PR topology.

Every job must fail unless:

    github.event_name == pull_request

    github.head_ref
    == verify/u05-m2-integration-baseline-v1

    github.base_ref
    == decision/u05-m2-integration-baseline-verification-authorization.

---

# 5. Immutable verifier identity if approved

The workflow must pin:

    VERIFIER_AUTHORIZATION_BASE_SHA
    = <exact Owner-authorized decision-record commit>

and log:

    VERIFICATION_WORKFLOW_HEAD_SHA
    = github.event.pull_request.head.sha

    VERIFICATION_EVENT_SHA
    = github.sha

    VERIFICATION_EVENT_NAME

    VERIFICATION_PR_NUMBER

    VERIFICATION_HEAD_REF

    VERIFICATION_BASE_REF

    VERIFIER_AUTHORIZATION_BASE_SHA

    M2_TARGET_SHA.

Before target checkout, it must checkout the exact PR head and prove:

    git rev-parse HEAD
    == VERIFICATION_WORKFLOW_HEAD_SHA

    VERIFIER_AUTHORIZATION_BASE_SHA
    is ancestor of VERIFICATION_WORKFLOW_HEAD_SHA

and exact verifier diff equals only:

    A .github/workflows/u05-m2-integration-baseline-verification.yml

Any second file:

    FAIL.

---

# 6. Required M2 order and ancestry proof

The workflow must checkout:

    d620df6361cec2f757b01341918dfeef115a6475

and prove:

    f98163e5a8dd038280cdfa3f66b9172abaef133d
    -> 9c997fbefb58fa97664716bb6d5681557b01044f

    95a49831c29165b86300af9f6bdd8e76952177a7
    -> 9c997fbefb58fa97664716bb6d5681557b01044f

    9c997fbefb58fa97664716bb6d5681557b01044f
    -> d620df6361cec2f757b01341918dfeef115a6475

    5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2
    -> d620df6361cec2f757b01341918dfeef115a6475

using fail-closed ancestry checks.

This binds:

    PBNC-02A first
    Effect Ledger second.

---

# 7. Required exact integration inventory

The workflow must require the exact 14-file name/status inventory frozen in the reviewed design.

Any:
    additional file
    missing file
    renamed file
    different status

must FAIL.

---

# 8. Required three-way content-equivalence proof

## Original U05

No change is permitted between original U05 and M2 target under:

    runtime/u05/**
    test/runtime/u05/**
    .github/workflows/u05-engineering-smoke.yml

Required result:
    git diff --quiet = PASS.

## PBNC-02A

Verified PBNC implementation/test paths at:

    95a49831c29165b86300af9f6bdd8e76952177a7

must be content-equivalent at M2 target:

    d620df6361cec2f757b01341918dfeef115a6475.

Required result:
    git diff --quiet = PASS.

## Runtime Effect Ledger

Verified runtime/effects production/test paths at:

    5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2

must be content-equivalent at M2 target.

Required result:
    git diff --quiet = PASS.

Any content difference:
    FAIL.

---

# 9. Required executable verification

The workflow must:

    use ubuntu-latest

    set up Temurin JDK 8

    install Shared Contracts v1 Java binding

    compile diagnosis-service

    run PBNC-02A focused suite

    verify PBNC focused Surefire reports:
      errors = 0
      failures = 0
      skipped = 0

    run Runtime Effect Ledger focused suite

    verify Effect Ledger focused Surefire report:
      errors = 0
      failures = 0
      skipped = 0

    run U05NonProductionClinicalReadinessTest

    verify U05 focused Surefire report:
      errors = 0
      failures = 0
      skipped = 0

    run full diagnosis-service regression

    require Maven BUILD SUCCESS.

---

# 10. Failure suppression is forbidden

No:

    continue-on-error

    || true

    dynamic source patching

    dynamic test patching/deletion

    ignore-failure conversion

    skip flags weakening the required focused suites.

Missing/unreadable focused report:
    FAIL.

---

# 11. GitHub permission boundary

Workflow permissions:

    contents: read

No write permissions.

No:
    secrets
    deployment
    release
    package publish
    production environment
    cloud credentials
    live traffic.

Actions must be commit-SHA pinned.

---

# 12. Evidence semantics

A successful workflow means only:

    AUDITABLE_M2_INTEGRATION_BASELINE_EXECUTABLE_EVIDENCE_GENERATED.

It does NOT itself establish:

    M2 Integration Baseline Verification = PASS

    BF-U05-IMPL-IR-05 = CLOSED

    BF-U05-IMPL-IR-06 = CLOSED

    U05 Implementation Verification = PASS

    RDP-06 = PASS

    production/live authorization.

Independent evidence review remains mandatory.

---

# 13. Required workflow boundary declaration

A successful workflow must print:

    M2_INTEGRATION_BASELINE_EXECUTABLE_EVIDENCE_GENERATED=YES

    M2_INTEGRATION_BASELINE_VERIFICATION_PASS=NOT_DECIDED_BY_WORKFLOW

    U05_IR05_CLOSED=NO

    U05_IR06_CLOSED=NO

    RDP06_AUTHORITATIVE_VERIFICATION=NOT_RUN.

---

# 14. Evidence freshness

Evidence applies only to:

    d620df6361cec2f757b01341918dfeef115a6475

If integration target changes:

    prior evidence
    = STALE_FOR_NEW_HEAD

and a new pin/run/review is required.

---

# 15. STOP conditions

Implementation must STOP if it requires:

    source/test modification

    pom/dependency modification

    another workflow/script/reusable action

    broader GitHub permissions

    target SHA change

    test-list weakening

    different PR topology

    merge

    production/deployment access.

Any such need requires amendment + re-review.

---

# 16. Owner options

## AUTHORIZE

Record:

    AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001
    = AUTHORIZED

Meaning:
    the one exact reviewed verification workflow may be implemented and run.

## REVISE

Authorization not granted; package/design must be amended.

## REJECT

Authorization not granted; runner implementation must not proceed.

---

# 17. Current state before Owner decision

    U05 M2 Integration Baseline Verification Runner Design
    = PASS

    exact reviewed design head
    = 54d61f71f39abd651ba721428afb1ab4c71a78bf

    targeted independent review
    = PASS
    review_id = 5273800024

    AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001
    = AUTHORIZED / OWNER_APPROVED

No workflow modification, executable verification PASS,
U05 blocker closure, merge, production, or live authorization is granted.


---

# 18. Owner Authorization Record

Owner command:

    AUTHORIZE

Authorization:

    AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001
    = AUTHORIZED

Exact authorization shape:

    VERIFICATION_ONLY
    / ONE_WORKFLOW_FILE_ONLY
    / EXACT_M2_TARGET_SHA
    / EXACT_M2_ORDER_ANCESTRY
    / EXACT_14_FILE_INVENTORY
    / U05_TREE_PRESERVATION
    / PBNC_CONTENT_EQUIVALENCE
    / EFFECT_LEDGER_CONTENT_EQUIVALENCE
    / PBNC_FOCUSED_ZERO_SKIP
    / EFFECT_LEDGER_FOCUSED_ZERO_SKIP
    / U05_FOCUSED_ZERO_SKIP
    / FULL_DIAGNOSIS_REGRESSION
    / READ_ONLY_GITHUB_PERMISSIONS
    / NO_SOURCE_TEST_CHANGE
    / NO_MERGE
    / NO_PRODUCTION

Reviewed basis:

    design PR #198
    exact reviewed design head
    = 54d61f71f39abd651ba721428afb1ab4c71a78bf

    targeted independent review
    = PASS
    review_id = 5273800024

    authorization package PR #199
    package review
    = PASS
    review_id = 5273807369

Implementation lineage rule:

    verify/u05-m2-integration-baseline-v1
    MUST descend from this owner-authorized decision-record commit.

This authorization does NOT grant:

    M2 Integration Baseline Verification PASS
    BF-U05-IMPL-IR-05 closure
    BF-U05-IMPL-IR-06 closure
    U05 Implementation Verification PASS
    RDP-06 PASS
    merge
    production/live traffic
    real-patient use.
