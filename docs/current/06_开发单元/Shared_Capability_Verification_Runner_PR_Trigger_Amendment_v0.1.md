# Shared Capability Verification Runner — PR Trigger Amendment v0.1

> Amendment ID: `SHARED-CAP-EXACT-HEAD-VERIFIER-01A`  
> Proposed authorization: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001`  
> Parent authorization: `AUTH-SHARED-CAP-VERIFICATION-RUNNER-001 = AUTHORIZED`  
> Parent authorized decision head: `5e982aca544f98b2385cf9f4713ece0fb835774d`  
> Existing workflow implementation head: `2b9df5555c716f5b84b7f93b5fd529e6c0c2bb4d`  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document authorizes no workflow amendment.

---

# 1. Why this amendment exists

The authorized v1 runner is push-only:

    verify/shared-capabilities-exact-head-v1

The workflow file was added at:

    2b9df5555c716f5b84b7f93b5fd529e6c0c2bb4d

The available GitHub connector can inspect workflow runs associated with a commit
only when they are:

    pull_request-triggered runs.

Therefore push-only execution cannot be retrieved with the available
governed evidence interface, even if GitHub Actions executed it.

This is an observability/evidence-collection limitation.

It is NOT evidence that:
    the push run passed
    the push run failed
    or the workflow did not execute.

The current v1 push evidence is therefore:

    NOT_AUDITABLE_WITH_AVAILABLE_CONNECTOR

and cannot establish capability verification.

---

# 2. Amendment objective

Add a second, narrowly constrained trigger mode that produces a
pull-request-triggered run visible to the governed GitHub evidence interface.

No target implementation SHA, test command, diff inventory, permission,
or verification semantic may change.

---

# 3. Exact target SHAs remain frozen

PBNC-02A:

    implementation SHA
    = 5e9f05f4b45d82cec4e67a6a24bc3a4d8507b02d

    authorization base
    = 5ca69deaad861e6298856fa36d583c1d660bd831

Runtime Effect Ledger:

    implementation SHA
    = c0315bf11d8a96a6c2426ce38c8ef02f7bef298f

    authorization base
    = 0aa1565110b32a0a0a7ed28badac6127af29879e

Changing either target SHA is outside this amendment.

---

# 4. Exact amended trigger design

Create a new verification branch after owner authorization:

    verify/shared-capabilities-exact-head-v2

The workflow file remains:

    .github/workflows/shared-capability-exact-head-verification.yml

The v2 workflow may trigger on:

    pull_request

only when:

    base branch
    = decision/shared-capability-verification-runner-pr-trigger-authorization

and the PR head branch is asserted at runtime as:

    verify/shared-capabilities-exact-head-v2

The verification PR itself must contain only:

    .github/workflows/shared-capability-exact-head-verification.yml

relative to its authorization-decision base.

No arbitrary PR is eligible.

---

# 5. Required PR identity guard

Every job must assert before target checkout:

    github.event_name == pull_request

    github.head_ref
    == verify/shared-capabilities-exact-head-v2

    github.base_ref
    == decision/shared-capability-verification-runner-pr-trigger-authorization

and emit:

    VERIFICATION_WORKFLOW_COMMIT
    VERIFICATION_EVENT_NAME
    VERIFICATION_PR_NUMBER
    VERIFICATION_HEAD_REF
    VERIFICATION_BASE_REF

    TARGET_IMPLEMENTATION_SHA
    AUTHORIZATION_BASE_SHA.

The workflow definition commit from the PR head is part of later evidence review.

---

# 6. Verification PR exact-diff guard

Before target checkout, the workflow must prove the verification PR itself
contains only the one authorized workflow modification.

The eventual amendment authorization decision commit is the base.

Required:

    git diff --name-status
      <PR_TRIGGER_AUTHORIZED_DECISION_HEAD>
      <VERIFICATION_WORKFLOW_IMPLEMENTATION_HEAD>

must equal exactly:

    M .github/workflows/shared-capability-exact-head-verification.yml

Reason:

    this amendment design branch descends from the v1 workflow implementation
    head 2b9df5555c716f5b84b7f93b5fd529e6c0c2bb4d;

    the amendment authorization decision branch will descend from this reviewed
    amendment design head;

    therefore the authorized decision base already contains the v1 workflow,
    and v2 is strictly one-file modification of that workflow.

No added/deleted/renamed/second file is permitted.

No other file may differ.

---

# 7. Existing verification semantics remain unchanged

The following remain exactly as approved in
AUTH-SHARED-CAP-VERIFICATION-RUNNER-001:

    exact target SHA checkout

    authorization-base ancestry checks

    exact implementation name-status inventories

    JDK 8

    local Shared Contracts Java binding install

    diagnosis-service compile

    focused PBNC-02A tests

    focused Effect Ledger tests

    focused Surefire:
      report exists
      errors = 0
      failures = 0
      skipped = 0

    full diagnosis-service regression

    no continue-on-error

    no failure suppression

    read-only GitHub permissions

    green run
    != verification decision.

No test/source command may be weakened by this amendment.

---

# 8. GitHub permission boundary remains unchanged

Workflow permissions remain exactly:

    contents: read

No:
    pull-requests: write
    issues: write
    actions: write
    packages: write
    deployments: write
    id-token: write.

No secrets, deployment environment, or production credentials.

---

# 9. v1 evidence disposition

The existing v1 workflow commit:

    2b9df5555c716f5b84b7f93b5fd529e6c0c2bb4d

is retained as historical authorized implementation.

Its push-run result is not used as capability evidence because the available
connector cannot retrieve the corresponding push run.

Status:

    V1_RUN_EVIDENCE
    = NOT_AUDITABLE_WITH_AVAILABLE_CONNECTOR
    = NOT_ACCEPTED_FOR_VERIFICATION.

This does not revoke the original authorization.

---

# 10. Evidence semantics for v2

A green PR-triggered run means only:

    AUDITABLE_EXECUTABLE_EVIDENCE_GENERATED

for the same pinned implementation SHAs.

Independent evidence review must inspect:

    workflow implementation head
    PR number
    workflow run ID
    job IDs
    job steps/logs
    exact target SHAs
    exact diff guards
    focused Surefire zero-skip checks
    full regression results.

Only independent evidence review may establish:

    PBNC-02A Executable Verification = PASS/FAIL

    Runtime Effect Ledger Executable Verification = PASS/FAIL.

---

# 11. Exact authorized file scope if approved

The amendment may modify/add only:

    .github/workflows/shared-capability-exact-head-verification.yml

on the new v2 verification branch.

No document/source/test/pom/script change is authorized in the implementation branch.

---

# 12. STOP conditions

STOP if the amendment requires:

    target implementation SHA change
    test command change
    focused evidence requirement weakening
    source/test/pom modification
    another workflow file
    reusable script/action
    broader GitHub permissions
    secrets
    deployment/production access
    merge automation.

Any such need requires separate impact/amendment review.

---

# 13. Proposed authorization

    AUTH-SHARED-CAP-VERIFICATION-RUNNER-PR-TRIGGER-001

If approved:

    PR_TRIGGER_OBSERVABILITY_ONLY
    / SAME_PINNED_TARGETS
    / SAME_TESTS
    / SAME_EXACT_DIFF_GUARDS
    / SAME_ZERO_SKIP_POLICY
    / SAME_READ_ONLY_PERMISSIONS
    / ONE_WORKFLOW_FILE_ONLY
    / NO_SOURCE_TEST_CHANGE
    / NO_MERGE
    / NO_PRODUCTION.
