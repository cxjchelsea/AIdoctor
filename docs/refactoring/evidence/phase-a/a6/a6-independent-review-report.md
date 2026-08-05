# Phase A6 Independent Review and Remediation Report

## 1. Review object

- Repository: `cxjchelsea/AIdoctor`
- Target PR: [#8](https://github.com/cxjchelsea/AIdoctor/pull/8) `phase-a6: establish adult respiratory capability skeleton`
- Target Base: `agent/enterprise-agent-refactoring-plan` @ `f406fec3d6c3159f1f229bbb41113117a34a31e3`
- Expected Target Head: `agent/phase-a6-adult-respiratory-capability-skeleton` @ `fbab53f8099728df752a336d0ffb0f74443b411a`
- Actual Target Head at review start: `fbab53f8099728df752a336d0ffb0f74443b411a`
- Remediation branch: `agent/phase-a6-independent-review-remediation`
- Remediation worktree: `D:\project\AIdoctor-a6-independent-review`

## 2. Remote state at review start

- PR #8: OPEN, Draft, MERGEABLE, mergeStateStatus CLEAN
- Reviews: none
- Review comments/threads: none observed
- Workflow runs for A6 head branch: none (`NO_CI_CONFIGURED`)
- Changed files on PR #8: 53 (+2638 / -15)
- No TARGET_MOVED condition

## 3. Worktree and branch

- Main worktree `D:\project\AIdoctor`: clean on enterprise base
- A6 worktree: clean at exact target SHA
- Review branch created from exact SHA; no reuse of dirty trees
- No in-progress merge/rebase/cherry-pick

## 4. Change scope of PR #8

Independent local diff against enterprise base:

- Changed files: 53
- Capability YAML: 21
- JSONL: 5
- Schemas: 11
- Manifest assets: 25
- Evidence files before remediation: 4
- Out-of-scope service/DB/runtime files: none
- `git diff --check`: clean

## 5. Independent environment

```text
OS: Windows 10 / win32
Python: 3.13.5
pip: 26.2.1 (venv)
Virtualenv: D:\project\a6-review-venv (fresh; not reused)
Lock: capabilities/requirements-test.lock.txt
Install: success
pip check: No broken requirements found
Key versions: pytest 8.4.1; PyYAML 6.0.2; jsonschema 4.25.1; referencing 0.37.0
```

Commands:

```text
python -m venv D:\project\a6-review-venv
D:\project\a6-review-venv\Scripts\python.exe -m pip install --upgrade pip
D:\project\a6-review-venv\Scripts\pip.exe install -r capabilities/requirements-test.lock.txt
D:\project\a6-review-venv\Scripts\pip.exe check
D:\project\a6-review-venv\Scripts\python.exe capabilities/validator/validate_capability.py
D:\project\a6-review-venv\Scripts\python.exe -m pytest capabilities/tests/test_capability.py -v
```

## 6. Independent verification

### Before remediation

- Validator: PASS (`11 schemas, 25 assets, 5 eval cases, 0 issues`) exit 0
- Pytest: collected 37; passed 37; failed 0; errors 0; skipped 0; exit 0
- Independent checksum script: all 25 manifest sha256 values matched after LF normalization
- Independent inventory: yaml=21 jsonl=5 schemas=11 gaps=10 test_defs=37
- CSV parsing: asset 40 / gap 10 / validation 15 data rows
- Lifecycle probes found bypasses (see Findings)

### After remediation

- Validator: PASS exit 0
- Pytest: collected 43; passed 43; failed 0; errors 0; skipped 0; exit 0
- Lifecycle probes: BYPASSES `[]`
- `git diff --check`: clean for remediation changes

Temporary non-committed scripts used:

- `_tmp_independent_verify.py` — inventory/checksum/CSV/clinical emptiness
- `_tmp_lifecycle_probes.py` — ACTIVE/ENABLED/owner bypass probes

## 7. Schema review

All 11 schemas use Draft 2020-12, unique `$id` under
`https://schemas.aidoctor.dev/capabilities/1.0.0/`, closed objects, bounded
strings/arrays, and safe-integer ceilings where integers exist. Descriptions do
not claim clinical validity. Local `$ref` resolution via referencing Registry;
no remote schema fetch observed. No Schema relaxation was performed in
remediation.

## 8. Lifecycle and fail-closed review

Confirmed already blocked before remediation:

- ACTIVE + empty Safety
- production ELIGIBLE + empty Safety
- empty sources + retrieval ELIGIBLE
- path absolute / `..` / encoded traversal
- synthetic/real-patient eval constraints
- Patient/Medical mixed index
- RUNTIME_VERIFIED claim
- `latest` token in manifest serialization

Confirmed bypasses before remediation (now fixed):

- ACTIVE + empty hypotheses with APPROVED pack status
- ACTIVE + zero knowledge sources with APPROVED pack status
- runtime ENABLED + empty allowlists outside DRAFT
- clinical APPROVED + owner UNASSIGNED

## 9. Path and checksum review

Independent SHA-256 recomputation with CRLF→LF normalization matched all
manifest digests. Absolute drive paths, UNC prefixes, `..`, and `%2e%2e`
traversal are rejected. Backslashes fail the controlled path pattern. No
symlink escape was present in the package. Unexpected-file scanning remains a
non-blocking P2 hardening item.

## 10. Test quality review

Original 37 tests were real collected tests with assertions and useful coverage
for many gates, but they did not cover the four bypasses above. Remediation
added six negative tests that failed against the pre-fix probe behavior and
pass after the validator gates. Local pytest success is not CI success.

## 11. Clinical boundary review

Current package still has:

- approved clinical rules: 0
- approved safety thresholds: 0
- approved hypotheses: 0
- approved knowledge sources: 0
- adult numeric age boundary: null / REQUIRES_CLINICAL_REVIEW
- no real patient data
- no automatic diagnosis/treatment/prescription path
- runtime adoption: NOT_IMPLEMENTED
- production eligibility: BLOCKED

These gaps remain intentionally blocked.

## 12. Patient Evidence / Citation contract review

Delivery policy binds `patient_delivery_contract_version: 1.0.0`, requires
clinician review, prohibits PROMPT / PROVIDER_RESPONSE / PRIVATE_REASONING /
UNAPPROVED_HYPOTHESIS / OTHER_PATIENT_DATA, requires citation-or-limitation,
keeps free-text safety as `POLICY_ENFORCED_LATER`, and remains
runtime/production blocked. No Patient API/UI was implemented. Shared Contract
language bindings remain absent and are not claimed.

## 13. Evidence consistency review

PR #8 file counts and local inventory match. Pre-remediation evidence correctly
stated 37 tests; post-remediation suite is 43 and is recorded here. Overall
evidence remains `PARTIALLY_VALIDATED`. No RUNTIME_VERIFIED / clinical /
production claim is accepted.

## 14. Findings summary

| ID | Severity | Status |
|---|---|---|
| A6-IR-001 | P0 | FIXED |
| A6-IR-002 | P0 | FIXED |
| A6-IR-003 | P1 | FIXED |
| A6-IR-004 | P1 | FIXED |
| A6-IR-005 | P1 | FIXED |
| A6-IR-006 | P2 | DEFERRED |
| A6-IR-007 | P2 | ACCEPTED/clarified |
| A6-IR-008 | P2 | DEFERRED |

Details: [a6-independent-review-findings.csv](./a6-independent-review-findings.csv)

## 15. Remediation performed

- Strengthened lifecycle/runtime gates in `validate_capability.py`
- Added six negative tests in `test_capability.py`
- Added this independent review report and findings CSV
- Updated validation matrix rows for the new gates

No clinical content was added. No A6.5/A7/runtime/service/contract binding work
was performed.

## 16. Explicit non-claims

This review does not prove clinical correctness, red-flag recall, triage
accuracy, model quality, runtime availability, patient workflow readiness, CI
success, production readiness, A6.5 completion, A7 completion, or Frozen
Baseline completion.

## 17. Final recommendation

`READY_FOR_A6_READY_TRANSITION`

Meaning only:

- no unresolved P0/P1 inside A6 skeleton scope after remediation;
- clean-env install/validator/pytest/checksum succeeded;
- remediation Draft PR may be reviewed for merge into the A6 branch;
- PR #8 itself remains Draft and is not merged by this task;
- clinical/runtime/production remain BLOCKED.
