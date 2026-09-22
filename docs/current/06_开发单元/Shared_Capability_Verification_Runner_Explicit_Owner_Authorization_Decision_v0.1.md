# Shared Capability Verification Runner — Explicit Owner Authorization Decision v0.1

> Authorization ID: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-001`  
> Decision type: **Repository Owner Explicit Verification-Runner Authorization**  
> Exact reviewed design head: `ff81cbcfadbf87fe32b58971e76051165a44250f`  
> Design PR: #190  
> Targeted Independent Design Review: PASS / review_id `5273391778`  
> Current status: **OWNER_DECISION_PENDING**  
> This package itself grants no workflow modification authorization.

---

# 1. Decision question

Should the repository owner authorize implementation of the exact reviewed design:

    SHARED-CAP-EXACT-HEAD-VERIFIER-01

under authorization:

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-001

with the bounded scope below?

---

# 2. Exact verification targets

## PBNC-02A

    PR #188

    exact implementation SHA
    = 5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d

    owner-authorization base
    = 5ca69deaad861e6298856fa36d583c1d660bd831

## Runtime Effect Ledger NC-01

    PR #189

    exact implementation SHA
    = c0315bf11d8a96a6c2426ce38c8ef02f7bef298f

    owner-authorization base
    = 0aa1565110b32a0a0a7ed28badac6127af29879e

The runner may verify only these exact SHAs.

---

# 3. Exact authorized repository-file scope if approved

Authorization permits adding exactly one file:

    .github/workflows/shared-capability-exact-head-verification.yml

No other repository file may be modified under this authorization.

---

# 4. Exact workflow trigger if approved

The workflow may trigger only on:

    push

to:

    verify/shared-capabilities-exact-head-v1

No automatic trigger on:

    main
    production branches
    arbitrary pull requests.

---

# 5. Required GitHub permission boundary

Workflow permissions must be:

    contents: read

It must not request:

    contents: write
    pull-requests: write
    issues: write
    actions: write
    packages: write
    deployments: write
    id-token: write.

No secret/cloud/production environment access is authorized.

---

# 6. Required exact-head identity evidence

Every verification job must:

    emit VERIFICATION_WORKFLOW_COMMIT
    emit VERIFICATION_WORKFLOW_REF
    emit TARGET_IMPLEMENTATION_SHA
    emit AUTHORIZATION_BASE_SHA

    assert github.ref_name
      == verify/shared-capabilities-exact-head-v1

    checkout the target implementation SHA

    assert:
      git rev-parse HEAD
      == TARGET_IMPLEMENTATION_SHA.

Actions must be commit-SHA pinned.

---

# 7. Required lineage / exact-diff guard

## PBNC-02A

Must verify:

    5ca69deaad861e6298856fa36d583c1d660bd831
    is ancestor of
    5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d

and exact name-status inventory equals:

    M diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplier.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplierObjectValueParityTest.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/SyntheticStructuredObjectRepositoryIntegrationTest.java

## Runtime Effect Ledger

Must verify:

    0aa1565110b32a0a0a7ed28badac6127af29879e
    is ancestor of
    c0315bf11d8a96a6c2426ce38c8ef02f7bef298f

and exact name-status inventory equals:

    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedger.java

    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerDecision.java

    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerRecord.java

    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/NonProductionFileCanonicalEffectLedger.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerProcessProbe.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/NonProductionFileCanonicalEffectLedgerTest.java

Any mismatch:

    FAIL.

---

# 8. Required PBNC-02A executable verification

Workflow must:

    set up JDK 8

    install local Shared Contracts v1 Java binding

    compile diagnosis-service

    run focused tests:
      SyntheticJsonPointerApplierObjectValueParityTest
      SyntheticStructuredObjectRepositoryIntegrationTest
      SyntheticVersionedStateRepositoryTest
      StateCommitterArchitectureGuardTest
      StateCommitterContractBoundaryTest
      StateCommitterMechanicalCoreTest

    immediately validate focused Surefire XML:
      report exists
      errors = 0
      failures = 0
      skipped = 0

    run full diagnosis-service regression suite.

No failed/skipped focused case may be treated as PASS.

---

# 9. Required Runtime Effect Ledger executable verification

Workflow must:

    set up JDK 8

    install local Shared Contracts v1 Java binding

    compile diagnosis-service

    run:
      NonProductionFileCanonicalEffectLedgerTest

    immediately validate focused Surefire XML:
      report exists
      errors = 0
      failures = 0
      skipped = 0

    run full diagnosis-service regression suite.

This focused runtime suite must exercise the existing exact-head tests for:

    CREATED / REATTACHED

    canonical conflicts

    corruption

    unsupported format

    object reconstruction

    isolated JVM restart

    concurrency

    orphan temp non-authority

    symlink/root containment

    payload isolation/size bounds.

If the environment skips a critical durability test:

    workflow must FAIL.

---

# 10. Failure handling

The workflow must not use:

    continue-on-error

    || true

    dynamic test suppression

    test deletion/patching

    conditional success after command failure.

Missing focused Surefire report:

    FAIL.

Unparsable focused report:

    FAIL.

Unexpected exact diff:

    FAIL.

Wrong target SHA:

    FAIL.

---

# 11. Evidence semantics

A green run establishes only:

    exact-head executable evidence generated

for the pinned target SHA.

It does NOT itself establish:

    PBNC-02A VERIFIED

    Runtime Effect Ledger VERIFIED

    BF-U05-IMPL-IR-05 CLOSED

    BF-U05-IMPL-IR-06 CLOSED

    U05 Implementation Verification PASS

    RDP-06 PASS

    merge authorization

    production/live authorization.

After execution, an independent evidence review must inspect:

    workflow definition commit

    workflow run ID

    exact target SHA

    job IDs

    step results

    focused test reports/logs

    regression result.

Only that review may issue verification PASS/FAIL.

---

# 12. Evidence freshness

Evidence applies only to the pinned SHAs above.

If PR #188 or PR #189 head changes:

    prior runner evidence
    = STALE_FOR_NEW_HEAD.

A new runner pin/run/evidence review is required.

---

# 13. Required implementation lineage

If Owner approves:

    the verification workflow implementation branch
    MUST descend from the explicit owner-approved
    decision-record commit for this authorization.

Retrospective authorization is not allowed.

---

# 14. STOP conditions

Implementation must STOP if it requires:

    any source-code modification

    any test-code modification

    pom/dependency change

    reusable script/action file

    branch protection change

    token write permission

    secret/environment access

    production deployment

    changing either pinned implementation SHA without amendment/re-review

    adding another workflow file.

---

# 15. Owner options

Repository Owner must choose exactly one:

## AUTHORIZE

Record:

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-001
    = AUTHORIZED

Meaning:

    adding the one exact reviewed workflow file
    is allowed within Sections 2-14.

## REVISE

Meaning:

    authorization not granted

    package/design requires amendment and re-review.

## REJECT

Meaning:

    authorization not granted

    verification workflow implementation must not proceed.

---

# 16. Current state before Owner decision

    Shared Capability Verification Runner Design
    = PASS

    exact reviewed design head
    = ff81cbcfadbf87fe32b58971e76051165a44250f

    targeted design review
    = PASS
    review_id = 5273391778

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-001
    = NOT_GRANTED / OWNER_DECISION_PENDING

No workflow modification, shared-capability verification PASS,
U05 closure, merge, production, or live authorization is granted.
