# Phase A6 Independent Review and Remediation Report

## 1. Review object

- Repository: `cxjchelsea/AIdoctor`
- Target PR: [#8](https://github.com/cxjchelsea/AIdoctor/pull/8)
- Remediation PR: [#9](https://github.com/cxjchelsea/AIdoctor/pull/9) (existing; updated in place)
- Target Base: `agent/enterprise-agent-refactoring-plan` @ `f406fec3d6c3159f1f229bbb41113117a34a31e3`
- A6 Head reviewed: `agent/phase-a6-adult-respiratory-capability-skeleton` @ `fbab53f8099728df752a336d0ffb0f74443b411a`
- Round-1 remediation tip: `5980ccec75fccbcc5290ecaf4cbf5f000075c522`
- Round-2 branch: `agent/phase-a6-independent-review-remediation`
- Worktree: `D:\project\AIdoctor-a6-independent-review`

## 2. Status history

| Round | Verdict | Reason |
|---|---|---|
| Round 1 | temporarily `READY_FOR_A6_READY_TRANSITION` | closed empty-array ACTIVE bypasses only |
| Round 1 retracted | `BLOCKED_FOR_A6_REMEDIATION` | runtime/knowledge/child authorization consistency still bypassable |
| Round 2 | `READY_FOR_A6_READY_TRANSITION` | A6-IR-009/010/011 fixed and re-verified |

## 3. Remote state for round 2 start

- PR #9: OPEN / Draft / MERGEABLE
- PR #9 Head: `5980ccec75fccbcc5290ecaf4cbf5f000075c522` (unchanged; no TARGET_MOVED)
- PR #8: OPEN / Draft
- Reviews/CI: none (`NO_CI_CONFIGURED`)

## 4. Independent environment

```text
OS: Windows win32
Python: 3.13.5
Virtualenv: D:\project\a6-review-venv
Lock: capabilities/requirements-test.lock.txt
pip check: No broken requirements found
Key versions: pytest 8.4.1; PyYAML 6.0.2; jsonschema 4.25.1; referencing 0.37.0
```

## 5. Round-2 verification

```text
python capabilities/validator/validate_capability.py
# PASS: 11 schemas, 25 assets, 5 eval cases, 0 issues

python -m pytest capabilities/tests/test_capability.py -v
# collected 59; passed 59; failed 0; errors 0; skipped 0
```

Independent authorization probes (temporary non-committed script):

| Probe | Result |
|---|---|
| IMPLEMENTED_NOT_ENABLED | rejected (`a6-stage`) |
| ENABLED + one reference | rejected (`a6-stage`) |
| production ELIGIBLE | rejected (`a6-stage`) |
| PENDING source counted approved | rejected (`knowledge` predicate) |
| unapproved Safety child under APPROVED pack | rejected (`safety-child`) |
| unapproved Hypothesis child under APPROVED pack | rejected (`hypothesis-child`) |
| ACTIVE + governance.prohibited_runtime_use true | rejected (`governance`) |

## 6. Findings

| ID | Severity | Current merge_blocking | Resolution |
|---|---|---|---|
| A6-IR-001..005 | P0/P1 | false | FIXED in round 1 |
| A6-IR-006..008 | P2 | false | DEFERRED/ACCEPTED |
| A6-IR-009 | P0 | false | FIXED in round 2 |
| A6-IR-010 | P0 | false | FIXED in round 2 |
| A6-IR-011 | P0 | false | FIXED in round 2 |

Details: [a6-independent-review-findings.csv](./a6-independent-review-findings.csv)

### A6-IR-009

A6 package `1.0.0` now hard-locks:

- `runtime_adoption == NOT_IMPLEMENTED`
- `production_eligibility == BLOCKED`

Allowlist padding cannot promote Runtime Adoption. Fine-grained empty-reference and blocked-pack checks remain as defense in depth.

### A6-IR-010

`approved_source_count` must equal the number of sources satisfying the approved-source predicate:

- license APPROVED
- clinical APPROVED
- withdrawal AVAILABLE
- retrieval ELIGIBLE
- freshness not STALE
- non-placeholder `release_binding`

Incomplete region/population/freshness policies force approved count `0`. Policy and source manifest must stay consistent. No real medical sources were added.

### A6-IR-011

Pack-level APPROVED / production-ELIGIBLE / runtime-stage claims cannot mask:

- unapproved Safety child rules
- Safety children still prohibiting runtime use
- unapproved Hypothesis children
- PENDING runtime references
- ACTIVE with `governance.prohibited_runtime_use: true`

## 7. Remediation files

- `capabilities/validator/validate_capability.py`
- `capabilities/tests/test_capability.py` (43 → 59 collected tests; prior tests retained)
- A6 evidence files under `docs/refactoring/evidence/phase-a/a6/`

## 8. Clinical and safety boundary

- approved clinical rules: 0
- approved thresholds: 0
- approved hypotheses: 0
- approved knowledge sources: 0
- runtime adoption: NOT_IMPLEMENTED
- production eligibility: BLOCKED
- real patient data: none

## 9. Explicit non-claims

This review does not prove clinical correctness, red-flag recall, triage accuracy,
model quality, runtime availability, patient workflow readiness, CI success,
production readiness, A6.5 completion, A7 completion, or Frozen Baseline.

## 10. Final recommendation

`READY_FOR_A6_READY_TRANSITION`

Meaning only:

- no unresolved P0/P1 inside A6 skeleton authorization scope after round 2;
- clean-env install/validator/pytest/probes succeeded;
- PR #9 remains Draft for merge into the A6 branch;
- PR #8 remains Draft and is not clinically/runtime/production ready.
