# Shared Capability Exact-Head Verification Runner Design v0.1

> Design ID: `SHARED-CAP-EXACT-HEAD-VERIFIER-01`  
> Proposed authorization: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-001`  
> Targets:
> - PBNC-02A PR #188 exact head `5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d`
> - Runtime Effect Ledger PR #189 exact head `c0315bf11d8a96a6c2426ce38c8ef02f7bef298f`
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**
>
> This design authorizes no workflow/source modification.

---

# 1. Problem

PR #188 and PR #189 have both passed exact-head static implementation code review, but:

    executable verification
    = NOT_PASSED / RUNNER_EVIDENCE_PENDING

The repository has mature verification workflow patterns, but no existing workflow currently triggers for these two implementation branches.

Existing implementation authorizations do not permit editing:

    .github/workflows/**

Therefore a separate, narrow verification-runner authorization is required.

---

# 2. Objective

Add one verification-only workflow:

    .github/workflows/shared-capability-exact-head-verification.yml

whose sole purpose is to execute reproducible engineering verification against two pinned implementation SHAs.

The workflow does not modify:
    source code
    contracts
    runtime policy
    production configuration.

It only generates exact-head execution evidence.

---

# 3. Trigger model

Workflow trigger:

    push

restricted to one verification branch:

    verify/shared-capabilities-exact-head-v1

No automatic trigger on:
    main
    production branches
    arbitrary pull requests.

Reason:

    verification must remain tied to explicitly reviewed exact implementation heads.

---

# 4. Exact target pinning

The workflow must hard-pin:

## PBNC-02A

    implementation SHA
    = 5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d

    authorization base
    = 5ca69deaad861e6298856fa36d583c1d660bd831

## Runtime Effect Ledger NC-01

    implementation SHA
    = c0315bf11d8a96a6c2426ce38c8ef02f7bef298f

    authorization base
    = 0aa1565110b32a0a0a7ed28badac6127af29879e

The runner must fail if checkout HEAD differs from the pinned SHA.

If either implementation head changes later:

    existing runner evidence becomes stale

and a new controlled workflow amendment / rerun must pin the new exact SHA.

---

# 5. Job A — PBNC-02A exact-head verification

Required sequence:

    checkout exact PBNC-02A SHA
    verify git HEAD
    set up JDK 8
    install Shared Contracts v1 Java binding
    compile diagnosis-service

    verify diff scope against authorization base

    run focused tests:
      SyntheticJsonPointerApplierObjectValueParityTest
      SyntheticStructuredObjectRepositoryIntegrationTest
      SyntheticVersionedStateRepositoryTest
      StateCommitterArchitectureGuardTest
      StateCommitterContractBoundaryTest
      StateCommitterMechanicalCoreTest

    run diagnosis-service regression suite

Required scope guard:

Allowed production-source change:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/
      SyntheticJsonPointerApplier.java

Allowed tests:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/**

Any other changed file:

    FAIL.

---

# 6. Job B — Runtime Effect Ledger exact-head verification

Required sequence:

    checkout exact Effect Ledger SHA
    verify git HEAD
    set up JDK 8
    install Shared Contracts v1 Java binding
    compile diagnosis-service

    verify diff scope against authorization base

    run focused tests:
      NonProductionFileCanonicalEffectLedgerTest

    run diagnosis-service regression suite

Required scope guard:

Allowed added files only under:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/**
    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/**

Any modified pre-existing production/shared file:

    FAIL.

---

# 7. Effect Ledger runtime evidence requirements

The focused suite must exercise at runtime:

    create -> CREATED

    exact replay -> REATTACHED

    identity/fingerprint/schema/payload conflicts

    immutable payload copying

    corruption detection

    unsupported format detection

    namespace isolation

    service object reconstruction

    isolated JVM restart reattachment

    concurrent exact creators
      -> one CREATED
      -> others REATTACHED

    concurrent conflicting creators
      -> one CREATED
      -> one CONFLICT

    orphan temp non-authority

    symlink/root containment

    payload size bound.

If runner/filesystem does not support the frozen behavior:

    job must fail or test must explicitly report unsupported environment

and no PASS may be inferred.

A skipped critical durability test is not equivalent to PASS.

---

# 8. Workflow permission / security boundary

Workflow permissions:

    contents: read

No:
    write token permission
    PR merge
    issue write
    package publish
    release publish
    deployment
    secret access
    cloud credential
    production environment.

Actions must remain commit-SHA pinned.

---

# 9. Evidence semantics

A green workflow means only:

    exact pinned SHA checked out

    authorized diff scope respected

    Java compile passed

    focused executable tests passed

    diagnosis-service regression passed.

A green workflow does NOT itself mean:

    PBNC-02A VERIFIED

    Runtime Effect Ledger VERIFIED

    BF-U05-IMPL-IR-05 CLOSED

    BF-U05-IMPL-IR-06 CLOSED

    U05 Implementation Verification PASS

    merge authorized

    production/live authorized.

After the run:

    workflow run ID
    exact job IDs
    exact SHA
    step results/logs

must be independently reviewed.

Only that evidence review may produce:

    shared capability executable verification PASS/FAIL.

---

# 10. Exact authorized file scope if approved

Implementation authorization may add exactly one file:

    .github/workflows/shared-capability-exact-head-verification.yml

No other repository file may be modified under this authorization.

If runner implementation requires:
    pom change
    source/test change
    script file
    reusable action
    dependency file
    branch protection change

then:

    STOP
    -> impact/amendment review.

---

# 11. No test invention inside workflow

The workflow may only execute tests already present on the pinned implementation heads.

It may not:
    generate tests dynamically
    patch source
    patch test code
    suppress failed tests
    use continue-on-error
    use `|| true`
    selectively ignore failure.

---

# 12. Evidence freshness

Evidence is valid only for the pinned exact SHA.

If PR #188 or #189 head changes:

    prior evidence = STALE_FOR_NEW_HEAD.

The new head requires:
    new pin
    new workflow run
    new evidence review.

---

# 13. Owner decision shape

Proposed authorization:

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-001

If approved:

    VERIFICATION_ONLY
    / ONE_WORKFLOW_FILE_ONLY
    / PINNED_EXACT_HEADS_ONLY
    / READ_ONLY_GITHUB_PERMISSIONS
    / NO_SOURCE_MODIFICATION
    / NO_TEST_MODIFICATION
    / NO_MERGE
    / NO_PRODUCTION

---

# 14. Required sequence

    design
    -> independent design review
    -> explicit Owner Authorization
    -> add workflow from authorized decision lineage
    -> exact-head workflow run
    -> independent evidence review
    -> shared capability verification decisions
    -> only then return to U05 IR-05 / IR-06 closure work.
