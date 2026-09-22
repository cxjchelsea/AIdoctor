# Shared Capability Verification Runner PR-Trigger — Explicit Owner Authorization Decision v0.1

> Authorization ID: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001`  
> Decision type: **Repository Owner Explicit Verification-Trigger Amendment Authorization**  
> Parent authorization: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-001 = AUTHORIZED`  
> Exact reviewed amendment head: `3b34475a2d15c8c0089fe821eb87f766b2422250`  
> Amendment PR: #192  
> Targeted Independent Re-Review: PASS / review_id `5273507238`  
> Current status: **OWNER_DECISION_PENDING**  
> This package itself grants no workflow amendment authorization.

---

# 1. Decision question

Should the repository owner authorize implementation of the exact reviewed amendment:

    SHARED-CAP-EXACT-HEAD-VERIFIER-01A

under authorization:

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001

for PR-trigger observability only?

---

# 2. Why this amendment is needed

The already-authorized v1 workflow:

    .github/workflows/shared-capability-exact-head-verification.yml

at:

    2b9df5555c716f5b84b7f93b5fd529e6c0c2bb4d

uses a push trigger.

The available governed GitHub evidence interface can retrieve
workflow runs associated with a commit only for:

    pull_request-triggered runs.

Therefore v1 push evidence is:

    NOT_AUDITABLE_WITH_AVAILABLE_CONNECTOR

and cannot support independent evidence review.

This does NOT mean the v1 run:
    passed
    failed
    or did not execute.

---

# 3. Exact authorized repository-file scope if approved

Authorization permits modifying exactly one file:

    .github/workflows/shared-capability-exact-head-verification.yml

No other repository file may be changed under this amendment implementation.

---

# 4. Exact v2 implementation branch if approved

Implementation branch:

    verify/shared-capabilities-exact-head-v2

must descend from the exact owner-authorized decision-record commit
created by this authorization.

Retrospective authorization is prohibited.

---

# 5. Exact PR trigger if approved

The v2 workflow may add:

    pull_request

only for PRs whose base branch is:

    decision/shared-capability-verification-runner-pr-trigger-authorization

and must fail unless:

    github.event_name == pull_request

    github.head_ref
    == verify/shared-capabilities-exact-head-v2

    github.base_ref
    == decision/shared-capability-verification-runner-pr-trigger-authorization.

No arbitrary PR verification is authorized.

---

# 6. Exact verification-workflow identity if approved

Every job must emit:

    VERIFICATION_WORKFLOW_HEAD_SHA
    = github.event.pull_request.head.sha

    VERIFICATION_EVENT_SHA
    = github.sha

    VERIFICATION_EVENT_NAME

    VERIFICATION_PR_NUMBER

    VERIFICATION_HEAD_REF

    VERIFICATION_BASE_REF

    TARGET_IMPLEMENTATION_SHA

    AUTHORIZATION_BASE_SHA.

Before verification-PR diff inspection, job must checkout:

    github.event.pull_request.head.sha

with:

    fetch-depth: 0

and assert:

    git rev-parse HEAD
    == github.event.pull_request.head.sha.

---

# 7. Immutable verifier authorization base

After Owner approval, the v2 workflow must pin:

    VERIFIER_AUTHORIZATION_BASE_SHA
    = <this exact owner-authorized decision-record commit>

and prove:

    git merge-base --is-ancestor
      $VERIFIER_AUTHORIZATION_BASE_SHA
      $VERIFICATION_WORKFLOW_HEAD_SHA

and exact:

    git diff --name-status
      $VERIFIER_AUTHORIZATION_BASE_SHA
      $VERIFICATION_WORKFLOW_HEAD_SHA

equals:

    M .github/workflows/shared-capability-exact-head-verification.yml

Any other changed file/status:

    FAIL.

---

# 8. Shared capability target SHAs remain unchanged

PBNC-02A:

    implementation SHA
    = 5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d

    authorization base
    = 5ca69deaad861e6298856fa36d583c1d660bd831

Runtime Effect Ledger:

    implementation SHA
    = c0315bf11d8a96a6c2426ce38c8ef02f7bef298f

    authorization base
    = 0aa1565110b32a0a7ed28badac6127af29879e

Changing either exact target SHA is NOT authorized by this amendment.

---

# 9. Verification commands remain unchanged

This amendment must not change:

    exact target checkout

    target authorization-base ancestry checks

    exact implementation name-status inventory

    JDK 8

    Shared Contracts Java binding install

    diagnosis-service compile

    PBNC-02A focused tests

    Effect Ledger focused tests

    focused Surefire:
      report exists
      errors = 0
      failures = 0
      skipped = 0

    full diagnosis-service regression.

No test weakening or suppression is allowed.

---

# 10. GitHub permission boundary remains unchanged

Workflow permissions:

    contents: read

No write permissions.

No:
    secrets
    deployment environment
    release
    package publish
    cloud credential
    production credential.

---

# 11. Explicitly NOT authorized

Approval does NOT authorize:

    source-code modification

    test-code modification

    pom/dependency change

    script/reusable-action addition

    another workflow file

    branch protection change

    target implementation SHA change

    broader permissions

    merge automation

    PBNC-02A verification PASS

    Runtime Effect Ledger verification PASS

    BF-U05-IMPL-IR-05 closure

    BF-U05-IMPL-IR-06 closure

    U05 RDP-06 verification

    production/live traffic.

---

# 12. Evidence semantics

A green v2 PR-triggered run means only:

    AUDITABLE_EXECUTABLE_EVIDENCE_GENERATED.

Independent evidence review remains mandatory.

Only that independent review may establish:

    PBNC-02A Executable Verification = PASS/FAIL

    Runtime Effect Ledger Executable Verification = PASS/FAIL.

---

# 13. STOP conditions

Implementation must STOP if it requires:

    any second file change

    any target SHA change

    source/test/pom change

    weakening focused/zero-skip checks

    broader GitHub permission

    secrets

    deployment/production access

    different PR base/head topology

    merge automation.

Any such need requires separate amendment/re-review.

---

# 14. Owner options

## AUTHORIZE

Record:

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001
    = AUTHORIZED

Meaning:

    PR-trigger observability amendment may be implemented
    exactly within Sections 3-13.

## REVISE

Authorization not granted; package/design requires amendment.

## REJECT

Authorization not granted; v2 PR-trigger amendment must not proceed.

---

# 15. Current state before Owner decision

    PR-Trigger Amendment Design
    = PASS

    exact reviewed amendment head
    = 3b34475a2d15c8c0089fe821eb87f766b2422250

    targeted independent review
    = PASS
    review_id = 5273507238

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001
    = NOT_GRANTED / OWNER_DECISION_PENDING

No workflow amendment, executable verification PASS,
U05 closure, merge, production, or live authorization is granted.
