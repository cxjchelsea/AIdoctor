# A6.5-A Discovery and Quarantine Report

## 1. Execution Scope

Executed Phase A6.5-A only:

- `TASK-A01` — metadata-level L0 discovery for all inventoried assets
- `TASK-A02` — path-level quarantine for PHI-capable and unconfirmed medical-source assets
- `TASK-A03` — clinical-policy candidate path location (no content extraction)

Not executed: A6.5-B/C/D/E, `TASK-B04`, live/staging access, clinical body extraction, contract/capability mapping, Runtime, A7.

## 2. Exact Enterprise Base

```text
7f64c99d0e009e0285c25a55314d608b078e9b46
```

Includes PR #10 (plan), PR #11 (independent review), PR #12 (whitespace format fix).

## 3. Branch and Worktree

```text
Branch: agent/phase-a6-5-a-discovery-quarantine
Worktree: D:\project\AIdoctor-a6-5-a-discovery-quarantine
Base HEAD at branch creation: 7f64c99d0e009e0285c25a55314d608b078e9b46
```

## 4. Authorization and OD-007 Acknowledgement

A6.5-A execution was explicitly authorized by the repository-owner task prompt.

`OD-007` updated to `ACKNOWLEDGED` with retention mapping confirmed:

```text
Static / Interactive / Trajectory evaluation-suite obligations
and legacy-eval-asset-inventory → A6.5-D

Legacy Asset Decommission Register and matrix write-back
→ A6.5-E
```

`OD-001` through `OD-006` remain fail-closed / unresolved (`NEEDS_HUMAN_DECISION`, `NEEDS_OWNER`, `NEEDS_EVIDENCE`, or `NEEDS_SECURITY_REVIEW` as previously recorded).

`TASK-B04` remains `BLOCKED`.

## 5. Safety Boundary

```text
Lifecycle: DRAFT
Clinical review: REQUIRES_CLINICAL_REVIEW
Approved clinical rules: 0
Approved thresholds: 0
Approved hypotheses: 0
Approved medical sources: 0
Runtime adoption: NOT_IMPLEMENTED
Production eligibility: BLOCKED
Overall evidence: PARTIALLY_VALIDATED

Tracked real patient records found: 0
PHI-capable paths: 18
Patient-data content inspected: no
Live/staging stores accessed: no
Clinical content extracted: no
Runtime implemented: no
A6.5-B started: no
A7 started: no
```

## 6. Commands Used

Path/metadata discovery only:

```text
git ls-files
git cat-file -e HEAD:<path>
git grep -l -F -- <token>    # filenames only; match lines not persisted
python path-existence checks against git-tracked set
python CSV parse/write for planning and evidence artifacts
```

Not used for patient/data assets: content reads via cat/head/tail/less/more/jq, DB queries, live/staging connections, model/API calls.

## 7. Inventory Before

```text
Assets: 203
P0/P1/P2/P3: 77/72/52/2
Validation matrix rows: 259
Risk themes: 8
Backlog tasks: 13
PHI-capable paths: 18
P0 theme-risk traceability: 77/77
```

Path-status before discovery:

```text
TRACKED_FILE: 187
TRACKED_DIRECTORY_SUMMARY: 7
RUNTIME_PATH_ONLY: 6
MISSING_EXPECTED_ASSET: 3
```

## 8. Discovery Method

For each of 203 assets:

1. Normalize inventory path (strip missing-suite annotations where present).
2. Classify against git-tracked file set and path prefixes.
3. For P0/P1 tracked files, collect consumer/reference **file paths** via `git grep -l` only.
4. For runtime-only PHI paths, confirm code/config reference tokens exist; do not open stores.
5. For missing eval suites, search for version-controlled suite datasets; keep `MISSING_EXPECTED_ASSET` when absent.
6. Record before/after path status, risk level, risk basis, and result enum in discovery CSV.

Directory summaries (`CTR-*`, `BOUND-*`, `DOC-KG-CLUSTER`) validated as tracked prefixes. No new independent P0/P1 file-level rows were added in this pass because no newly discovered standalone file met all expansion criteria (independent consumer/risk/owner/disposition need beyond the existing summary coverage).

## 9. Discovery Results

```text
Assets before: 203
Assets after: 203
Assets added: 0

Path-status after:
TRACKED_FILE: 187
TRACKED_DIRECTORY_SUMMARY: 8
RUNTIME_PATH_ONLY: 5
MISSING_EXPECTED_ASSET: 3

Discovery results:
VALIDATED_FOR_DISCOVERY: 195
RUNTIME_PATH_CONFIRMED: 5
MISSING_EXPECTED_ASSET: 3
```

Inventory change in this batch:

```text
PHI-006: RUNTIME_PATH_ONLY -> TRACKED_DIRECTORY_SUMMARY
Evidence: git-tracked prefix frontend/src/components/cdp-visualization/
Reason: path exists as version-controlled UI directory; remains PHI_CAPABLE_API_PATH and quarantined
```

Evidence file: `a6-5-a-discovery-results.csv` (203 rows).

## 10. P0/P1 Calibration

```text
P0 before/after: 77/77
P1 before/after: 72/72
P2 before/after: 52/52
P3 before/after: 2/2
```

No risk-level recalibration was required in this pass. All 77 P0 assets retain non-empty `risk_basis`. All 149 P0/P1 assets have non-empty `path_status`.

## 11. Runtime and External Paths

Runtime-only (store not accessed):

```text
PHI-001 diagnosis-service runtime DB entities
PHI-002 examination-service uploads/reports/{userId}/
PHI-003 dialog-service Redis key dialog:context:{cdp_id}
PHI-004 ocr-service input/output buffers
PHI-005 execution-trace-service ExecutionTrace store
```

Reclassified from runtime-only to tracked directory summary:

```text
PHI-006 frontend/src/components/cdp-visualization/
```

No `EXTERNAL_REFERENCE` or `HISTORICAL_REFERENCE` rows were present in the baseline inventory.

## 12. Missing Expected Assets

Confirmed still missing as version-controlled suite datasets:

```text
DATA-EV001 static_case_eval_suite
DATA-EV002 interactive_interview_eval_suite
DATA-EV003 trajectory_replay_eval_suite
```

Per OD-007, suite inventory obligations remain in A6.5-D (`TASK-D01`).

## 13. PHI-Capable Path Results

```text
PHI-capable paths: 18
Tracked real patient records found: 0
Patient-data content inspected: no
Live/staging stores accessed: no
Path-level quarantine only: yes
```

All 18 PHI-capable assets appear in `a6-5-a-quarantine-register.csv` with status `QUARANTINED_AT_PATH_LEVEL`.

## 14. Medical Source and License Quarantine

Fail-closed metadata quarantine applied for:

- RISK-003 related medical-source assets
- Assets declaring medical sources with `license_status=UNKNOWN`
- DR.KNOWS-related assets (`DOC-019`, `MODEL-008`, `DATA-DR001`..`DATA-DR006`)

```text
Quarantine register rows: 91
Unique quarantined assets: 85
Read-only exceptions (KEEP_READ_ONLY_EXCEPTION): 0
```

No claim of completed storage isolation, encryption, deletion, or license approval.

## 15. Clinical Candidate Path Register

Registered path metadata only for:

```text
CLIN-020 CLIN-021 CLIN-022 CLIN-025 CLIN-026
PROMPT-001 PROMPT-003 PROMPT-004 PROMPT-008
```

```text
Clinical content extracted: no
Clinical rules approved: 0
Thresholds approved: 0
Hypotheses approved: 0
Medical sources approved: 0
TASK-B04: BLOCKED
```

Evidence: `a6-5-a-clinical-policy-candidate-path-register.md`.

## 16. Quarantine Results

```text
Status used: QUARANTINED_AT_PATH_LEVEL
KEEP_READ_ONLY_EXCEPTION: 0
Files moved/deleted/permission-changed: 0
DB connected: no
```

RISK-001 .. RISK-005 remain `OPEN` with containment notes referencing A6.5-A path-level quarantine evidence. Quarantine does not close these risks.

## 17. Risk Traceability

```text
Risk themes: 8
P0 theme-risk traceability: 77/77
DATA-KG004 / DATA-KG006 remain linked via RISK-004
```

## 18. Cross-File Integrity

Checked with a local one-shot script (not committed):

```text
Duplicate asset/discovery/quarantine/execution/risk IDs: 0
Broken Discovery → Inventory refs: 0
Broken Quarantine → Inventory refs: 0
Broken Validation Evidence → Inventory refs: 0
Broken Risk related_asset_ids → Inventory refs: 0
Clinical candidate register → Inventory: 9/9
P0/P1 without path_status: 0
P0 without risk_basis: 0
PHI-capable without quarantine entry: 0
RISK-003 assets without quarantine entry: 0
```

## 19. A6 Regression Results

Pre-change and post-change local verification:

```text
Python: 3.13.5
pip check: No broken requirements found
Validator: 11 schemas / 25 assets / 5 eval cases / 0 issues
Pytest: 59 passed
git diff --check: clean (post-commit expectation)
```

`NO_CI_CONFIGURED` — local verification is not CI PASS.

## 20. Findings

| ID | Severity | Finding | Disposition |
| --- | --- | --- | --- |
| F-A01 | P2 | PHI-006 was labeled RUNTIME_PATH_ONLY but is a tracked frontend directory | Reclassified to TRACKED_DIRECTORY_SUMMARY; remains quarantined |
| F-A02 | P1 | Three eval suites remain MISSING_EXPECTED_ASSET | Deferred to A6.5-D per OD-007 |
| F-A03 | P0 | 18 PHI-capable paths require continued fail-closed handling | Path-level quarantine recorded; risks remain OPEN |
| F-A04 | P0 | Medical-source license/provenance largely UNKNOWN | Path-level quarantine recorded; OD-001/OD-002 unresolved |

No confirmed version-controlled real patient records were identified.

## 21. Stop Conditions Evaluated

| Stop condition | Triggered |
| --- | --- |
| CONFIRMED_PATIENT_DATA_FOUND | no |
| UNAUTHORIZED_CONTENT_ACCESS_REQUIRED | no |
| UNLICENSED_ACTIVE_PRODUCTION_USE_FOUND | no (UNKNOWN license alone is insufficient) |
| A6_5_A_SCOPE_CONFLICT | no |
| A6_5_A_INVENTORY_INTEGRITY_FAILED | no |
| A6_5_A_REGRESSION_FAILED | no |

## 22. Remaining Open Decisions

```text
OD-001: NEEDS_HUMAN_DECISION (fail-closed)
OD-002: NEEDS_HUMAN_DECISION (fail-closed)
OD-003: NEEDS_HUMAN_DECISION (fail-closed)
OD-004: NEEDS_OWNER (fail-closed)
OD-005: NEEDS_EVIDENCE (fail-closed)
OD-006: NEEDS_SECURITY_REVIEW (fail-closed)
OD-007: ACKNOWLEDGED
TASK-B04: BLOCKED
```

## 23. Explicit Non-Claims

This batch does **not** claim:

- legacy assets are validated beyond L0 path/metadata discovery
- clinical correctness or clinical approval
- medical-source license clearance
- Runtime readiness or production readiness
- completed storage quarantine / encryption / deletion
- A6.5-A merge into Enterprise
- A6.5-B authorization
- A7 start
- CI PASS

## 24. Exit Recommendation

```text
READY_FOR_A6_5_A_INDEPENDENT_REVIEW
```

Meaning only: the A6.5-A implementation branch and Draft PR are ready for independent review. Do not merge automatically. Do not start A6.5-B.
