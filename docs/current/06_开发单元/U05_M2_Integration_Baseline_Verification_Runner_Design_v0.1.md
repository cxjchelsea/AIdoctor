# U05 M2 Integration Baseline Verification Runner Design v0.1

> Design ID: `U05-M2-INTEGRATION-BASELINE-VERIFIER-01`  
> Proposed authorization: `AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001`  
> Target integration SHA: `d620df6361cec2f757b01341918dfeef115a6475`  
> Original U05 SHA: `f98163e5a8dd038280cdfa3f66b9172abaef133d`  
> PBNC-02A closure commit: `95a49831c29165b86300af9f6bdd8e76952177a7`  
> Effect Ledger closure commit: `5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2`  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document authorizes no workflow modification.

---

# 1. Purpose

The M2 integration branch already exists and has passed static PMV:

    integration/u05-shared-capabilities-v1

    HEAD =
    d620df6361cec2f757b01341918dfeef115a6475

Static PMV review:

    PR #197
    review_id = 5273779463
    verdict = PASS / EXECUTABLE_EVIDENCE_PENDING

The missing evidence is executable integration-baseline verification.

This runner exists only to prove that the already-created M2 baseline:

    preserves the original U05 implementation

    contains the PMV-approved PBNC-02A closure

    contains the PMV-approved Runtime Effect Ledger closure

    compiles as one combined diagnosis-service tree

    passes all shared focused suites

    passes the U05 focused engineering suite

    passes full diagnosis-service regression.

---

# 2. What this runner does NOT prove

A green runner does NOT itself prove:

    BF-U05-IMPL-IR-05 = CLOSED

    BF-U05-IMPL-IR-06 = CLOSED

    U05 Implementation Verification = PASS

    RDP-06 authoritative verification = PASS

    U05 consumer integration = implemented

    merge to U05 implementation branch = authorized

    production/live traffic = authorized.

The runner only generates:

    AUDITABLE_M2_INTEGRATION_BASELINE_EXECUTABLE_EVIDENCE.

An independent evidence review remains mandatory.

---

# 3. Exact target pinning

The workflow must hard-pin:

    M2_TARGET_SHA
    = d620df6361cec2f757b01341918dfeef115a6475

    ORIGINAL_U05_SHA
    = f98163e5a8dd038280cdfa3f66b9172abaef133d

    PBNC02A_CLOSURE_SHA
    = 95a49831c29165b86300af9f6bdd8e76952177a7

    EFFECT_LEDGER_CLOSURE_SHA
    = 5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2

Evidence is valid only for this exact target SHA.

If the integration branch head changes:

    prior evidence
    = STALE_FOR_NEW_HEAD.

---

# 4. Auditable trigger topology

Because the available governed GitHub evidence interface exposes
pull_request-triggered workflow runs, the workflow must use a dedicated PR topology.

After Owner authorization:

    decision branch:
      decision/u05-m2-integration-baseline-verification-authorization

    verification branch:
      verify/u05-m2-integration-baseline-v1

The workflow may trigger only on:

    pull_request

whose base is:

    decision/u05-m2-integration-baseline-verification-authorization

and must assert:

    github.event_name == pull_request

    github.head_ref
    == verify/u05-m2-integration-baseline-v1

    github.base_ref
    == decision/u05-m2-integration-baseline-verification-authorization.

No arbitrary PR may use this workflow as accepted evidence.

---

# 5. Exact authorized workflow file if approved

Implementation may add exactly one file:

    .github/workflows/u05-m2-integration-baseline-verification.yml

No other file may be modified under this verifier authorization.

If implementation requires:

    source change
    test change
    pom/dependency change
    reusable script/action
    second workflow
    branch-protection change

then:

    STOP
    -> amendment review.

---

# 6. Verifier identity evidence

Every job must log:

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

Before target checkout, the workflow must explicitly checkout:

    github.event.pull_request.head.sha

with:

    fetch-depth: 0

and assert:

    git rev-parse HEAD
    == VERIFICATION_WORKFLOW_HEAD_SHA.

After Owner authorization, the workflow must pin the immutable
Owner authorization decision commit as:

    VERIFIER_AUTHORIZATION_BASE_SHA.

Then it must prove:

    git merge-base --is-ancestor
      $VERIFIER_AUTHORIZATION_BASE_SHA
      $VERIFICATION_WORKFLOW_HEAD_SHA

and exact verifier diff:

    A .github/workflows/u05-m2-integration-baseline-verification.yml

with no second file.

---

# 7. M2 ancestry proof

After checkout of the target SHA:

    git rev-parse HEAD
    == d620df6361cec2f757b01341918dfeef115a6475

must PASS.

The runner must then prove all three ancestors:

    git merge-base --is-ancestor
      f98163e5a8dd038280cdfa3f66b9172abaef133d
      d620df6361cec2f757b01341918dfeef115a6475

    git merge-base --is-ancestor
      95a49831c29165b86300af9f6bdd8e76952177a7
      d620df6361cec2f757b01341918dfeef115a6475

    git merge-base --is-ancestor
      5ce8c106942d64e5fb5ba4e71698f0ab1d31ded2
      d620df6361cec2f757b01341918dfeef115a6475

All must PASS.

---

# 8. Exact integration diff inventory

The exact:

    git diff --name-status
      f98163e5a8dd038280cdfa3f66b9172abaef133d
      d620df6361cec2f757b01341918dfeef115a6475

must equal exactly:

    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedger.java
    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerDecision.java
    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerRecord.java
    A diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/NonProductionFileCanonicalEffectLedger.java

    M diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplier.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/CanonicalEffectLedgerProcessProbe.java
    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/NonProductionFileCanonicalEffectLedgerTest.java

    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/SyntheticJsonPointerApplierObjectValueParityTest.java
    A diagnosis-service/src/test/java/com/aidoctor/diagnosis/state/committer/SyntheticStructuredObjectRepositoryIntegrationTest.java

    A docs/current/06_开发单元/PBNC02A_Explicit_Owner_Authorization_Decision_v0.1.md
    A docs/current/06_开发单元/PBNC02A_Synthetic_Object_Value_Support_Design_v0.1.md
    A docs/current/06_开发单元/Runtime_Canonical_Effect_Ledger_NonProd_Design_v0.1.md
    A docs/current/06_开发单元/Runtime_Effect_Ledger_NC01_Explicit_Owner_Authorization_Decision_v0.1.md
    A docs/current/06_开发单元/U05_Shared_Runtime_Impact_Review_v0.1.md

Any additional, missing, renamed, or status-different file:

    FAIL.

---

# 9. Explicit U05 preservation guard

The runner must separately prove no diff between the original U05 head
and M2 target under:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/**

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/**

    .github/workflows/u05-engineering-smoke.yml

Required command semantics:

    git diff --quiet
      $ORIGINAL_U05_SHA
      $M2_TARGET_SHA
      -- <all U05-owned paths above>

must PASS.

This prevents a green regression suite from masking an accidental U05 baseline mutation.

---

# 10. Runtime/toolchain setup

The runner must:

    use ubuntu-latest

    set up Temurin JDK 8

    use commit-SHA-pinned:
      actions/checkout
      actions/setup-java

    install:
      contracts/v1/bindings/java/pom.xml
      with -DskipTests

    compile:
      diagnosis-service/pom.xml
      with -DskipTests.

No external production service is required.

---

# 11. Focused PBNC-02A verification

Run:

    SyntheticJsonPointerApplierObjectValueParityTest

    SyntheticStructuredObjectRepositoryIntegrationTest

    SyntheticVersionedStateRepositoryTest

    StateCommitterArchitectureGuardTest

    StateCommitterContractBoundaryTest

    StateCommitterMechanicalCoreTest

Immediately after the focused command, require all corresponding
Surefire XML reports to:

    exist
    be readable
    errors = 0
    failures = 0
    skipped = 0.

Missing/unreadable/skipped focused evidence:

    FAIL.

---

# 12. Focused Runtime Effect Ledger verification

Run:

    NonProductionFileCanonicalEffectLedgerTest

Immediately inspect its Surefire XML and require:

    errors = 0
    failures = 0
    skipped = 0.

This preserves the previously verified restart/concurrency/corruption/symlink
runtime evidence inside the combined M2 tree.

---

# 13. Focused U05 engineering verification

Run:

    U05NonProductionClinicalReadinessTest

Immediately inspect:

    TEST-com.aidoctor.diagnosis.runtime.u05.U05NonProductionClinicalReadinessTest.xml

and require:

    errors = 0
    failures = 0
    skipped = 0.

This verifies that adding both shared capability closure lineages
did not regress the currently authorized U05 implementation slice.

It is still:

    NON_AUTHORITATIVE_ENGINEERING_EVIDENCE

and not RDP-06 authoritative U05 verification.

---

# 14. Full diagnosis-service regression

After all focused suites pass:

    mvn -f diagnosis-service/pom.xml test

must return:

    BUILD SUCCESS.

Existing non-target skips may be reported and must be recorded in later evidence review.

A full-regression skip does not automatically fail this runner unless:

    a focused required report is skipped

or a new unexpected skip/failure indicates regression.

The workflow itself must not suppress failures.

---

# 15. Failure suppression prohibited

The workflow must not use:

    continue-on-error

    || true

    dynamic source/test patching

    dynamic test deletion

    test-ignore flags that weaken the required suites

    conditional conversion of failed Maven commands to success.

Any required command failure:

    job FAIL.

---

# 16. GitHub security boundary

Workflow permissions:

    contents: read

No:

    contents: write
    pull-requests: write
    issues: write
    actions: write
    packages: write
    deployments: write
    id-token: write.

No secrets, cloud credentials, deployment environment,
production profile, or live traffic.

---

# 17. Evidence boundary declaration

A successful job must explicitly print:

    M2_INTEGRATION_BASELINE_EXECUTABLE_EVIDENCE_GENERATED=YES

    M2_INTEGRATION_BASELINE_VERIFICATION_PASS=NOT_DECIDED_BY_WORKFLOW

    U05_IR05_CLOSED=NO

    U05_IR06_CLOSED=NO

    RDP06_AUTHORITATIVE_VERIFICATION=NOT_RUN.

The workflow cannot self-authorize its own verification verdict.

---

# 18. Required independent evidence review

After a run, independent review must inspect:

    workflow-definition exact head

    immutable verifier authorization base

    PR number

    run ID

    job ID

    target SHA

    ancestry checks

    exact 14-file integration inventory

    explicit U05 preservation guard

    compile result

    PBNC focused result + zero skips

    Effect Ledger focused result + zero skips

    U05 focused result + zero skips

    full diagnosis-service regression result

    any non-target skips.

Only independent evidence review may decide:

    U05 M2 Integration Baseline Verification
    = PASS / FAIL.

---

# 19. Post-PASS boundary

If independent evidence review later returns PASS:

    M2 Integration Baseline
    = VERIFIED_FOR_U05_CONSUMPTION_WORK

then U05-owned consumer implementation work may begin under the already-authorized
U05 implementation boundary, provided it stays within U05-owned source/tests
and does not require new shared-runtime semantic changes.

A PASS still does NOT close:

    BF-U05-IMPL-IR-05
    BF-U05-IMPL-IR-06.

Those require actual U05 consumption implementation and a new exact-head review.

---

# 20. Proposed authorization shape

    AUTH-U05-M2-INTEGRATION-BASELINE-VERIFIER-001

If approved:

    VERIFICATION_ONLY
    / ONE_WORKFLOW_FILE_ONLY
    / EXACT_M2_TARGET_SHA
    / EXACT_THREE-ANCESTOR_GUARD
    / EXACT_14_FILE_INVENTORY
    / U05_TREE_PRESERVATION_GUARD
    / PBNC_FOCUSED_ZERO_SKIP
    / EFFECT_LEDGER_FOCUSED_ZERO_SKIP
    / U05_FOCUSED_ZERO_SKIP
    / FULL_DIAGNOSIS_REGRESSION
    / READ_ONLY_GITHUB_PERMISSIONS
    / NO_SOURCE_TEST_CHANGE
    / NO_MERGE
    / NO_PRODUCTION.
