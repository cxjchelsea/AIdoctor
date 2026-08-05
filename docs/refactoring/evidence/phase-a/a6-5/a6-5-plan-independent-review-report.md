# Phase A6.5 Plan Independent Review Report

## 1. Review Scope

Independent review and narrow remediation of Draft PR #10 planning artifacts only.

Does **not** start A6.5-A, merge PR #10, approve clinical content, inspect patient-data content, or implement Runtime/A7.

## 2. Exact Base and Head

| Item | SHA / ref |
| --- | --- |
| Enterprise base | `b4a1714506ac7b05ce2d8c7b6ecae3bd7bb2e836` |
| Reviewed planning Head (PR #10) | `0cc13f29c564731257569ee293359ae37b5fbce9` |
| Review branch | `agent/phase-a6-5-plan-independent-review` |
| Review worktree | `D:\project\AIdoctor-a6-5-plan-review` |

## 3. Files Reviewed

Planning:

- `docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-validation-plan.md`
- `docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv`
- `docs/refactoring/plans/phase-a/a6-5/a6-5-validation-matrix.csv`
- `docs/refactoring/plans/phase-a/a6-5/a6-5-risk-register.csv`
- `docs/refactoring/plans/phase-a/a6-5/a6-5-decision-log.md`
- `docs/refactoring/plans/phase-a/a6-5/a6-5-implementation-backlog.csv`

Authority cross-check (read-only):

- `docs/refactoring/可执行实施路线.md` (A6.5)
- `docs/refactoring/原设计资产保留、改造与目标架构映射.md` §15
- Coverage / Migration / Engineering decommission sections
- A5/A6 boundary artifacts

## 4. Authoritative Source Review

| Topic | Result |
| --- | --- |
| A6 → A6.5 → A7 sequence | Confirmed NORMATIVE |
| Official name | **Legacy Design Asset Validation**; planning title alias documented in D-001 |
| A6.5-F folding | Eval suites + decommission register retained via D/E table (OD-007) |
| Disposition / evidence enums | Mapping table retained; conflicts logged |
| A7 leakage into A6.5 | No Runtime registry/model-load tasks found; Model assets mapped only |
| Decommission / rollback evidence | Retained in TASK-E01 / normative deliverable list |

Unresolved authority issue: OD-007 still needs human ack of folding table (fail-closed keeps deliverables).

## 5. Inventory Integrity

| Check | Result |
| --- | --- |
| Assets | 203 (unchanged count) |
| Duplicate `asset_id` | none |
| Category sum | 203 |
| Path classification | `path_status` added |
| Runtime-only paths | 6 |
| Missing expected assets | 3 (eval suites) |
| Tracked files/dirs | 187 file + 7 directory summaries |

Added fields: `path_status`, `risk_basis`, `phi_path_class`.

## 6. P0/P1 Risk Calibration

| Metric | Before | After |
| --- | ---: | ---: |
| P0 | 84 | 77 |
| P1 | 65 | 72 |
| P2 | 52 | 52 |
| P3 | 2 | 2 |

P0 after calibration by `risk_basis`:

- `LICENSE_PROVENANCE_BLOCKER`: 32
- `CLINICAL_LOGIC_ACTIVE_PATH`: 19
- `PHI_CAPABLE_PATH`: 18
- `RUNTIME_COUPLING_BLOCKER`: 8

No `CONFIRMED_*` bases used (no confirmed incident evidence).

Design documents are no longer automatic P0.

## 7. Potential Patient-Data Semantics

```text
Tracked real patient records found: 0
PHI-capable or potentially patient-bearing paths: 18
Patient-data content inspected: no
Live/staging stores accessed: no
```

`phi_path_class` values used for the 18 paths include database/storage/log/API/code-path capability classes only.

## 8. Validation Matrix Review

| Check | Result |
| --- | --- |
| Rows | 259 |
| Duplicate validation IDs | none |
| Broken asset refs | none |
| P0 without validation | 0 |
| Status values | all `PLANNED` |
| L4 overclaim | none (safety strengthened) |
| L5 overclaim | none (explicit NOT production/runtime ready) |

## 9. Risk Register Review

Reworked from 18 single-asset rows to **8 theme risks** with:

- `related_asset_ids`
- `blocking_batches`
- `unblock_evidence`
- `containment`

Binding P0 groups for privacy, clinical logic, license/provenance, path-injection, state/PHI, eval gap, scope, duplication.

## 10. Open Decision Review

OD-001..OD-007 expanded with owner/reviewer roles, blocking batches, default fail-closed decision, required evidence, phase gate, and impact.

Defaults:

- OD-001 quarantine / not eligible for knowledge release
- OD-002 archive_or_quarantine / no product use
- OD-003 reject_or_keep_read_only / no runtime registration
- OD-004 block disposition while UNASSIGNED
- OD-005 designed_but_missing + synthetic-only
- OD-006 no content inspection
- OD-007 keep D/E retention table

## 11. Batch Boundary Review

A6.5-A reduced to discovery/quarantine/path marking.

`TASK-A03` converted to path-location only.

Content-level clinical-policy extraction moved to `TASK-B04` in A6.5-B with status `BLOCKED`.

A6.5-F deliverables retained in OD-007 table (D for eval inventory; E for decommission register).

## 12. Backlog Review

| Check | Result |
| --- | --- |
| Tasks | 13 (added TASK-B04) |
| Statuses | PLANNED / BLOCKED only |
| Dependency cycles | none detected |
| Out-of-scope Runtime/A7 files | none in expected_files |
| Asset refs | all resolve |

## 13. Cross-File Integrity Checks

Executed locally after remediation:

- unique IDs for assets/validations/risks/tasks/findings
- Validation/Risk/Backlog → Inventory refs OK
- category/risk totals consistent with inventory
- plan statistics updated to post-calibration numbers

## 14. Findings

| Severity | Count |
| --- | ---: |
| P0 | 3 |
| P1 | 4 |
| P2 | 2 |
| P3 | 0 |

All P0/P1 findings: `resolution=FIXED`, `merge_blocking=false`, `merge_blocking_at_discovery=true`.

Details: `a6-5-plan-independent-review-findings.csv`.

## 15. Remediation Applied

1. Inventory schema + risk recalibration
2. PHI path classes + mandatory semantics
3. OD structured fail-closed fields + A6.5-F retention table
4. A6.5-A / TASK-A03 / TASK-B04 batch fix
5. Theme risk register with unblock evidence
6. L4/L5 safety boundary hardening
7. Plan statistics synchronization

## 16. Remaining Open Decisions

OD-001..OD-007 remain human decisions (not silently closed).

## 17. Safety Boundary

```text
Lifecycle: DRAFT
Clinical review: REQUIRES_CLINICAL_REVIEW
Approved clinical rules: 0
Approved thresholds: 0
Approved hypotheses: 0
Approved medical sources: 0
Runtime implemented: no
Production eligibility: BLOCKED
Tracked real patient records found: 0
PHI-capable or potentially patient-bearing paths: 18
Patient-data content inspected: no
Live/staging stores accessed: no
A6.5-A started: no
A7 started: no
```

## 18. Explicit Non-Claims

This review does not claim:

- legacy assets validated or approved
- clinical correctness
- medical-source approval
- Runtime / production readiness
- PR #10 Ready-to-merge
- A6.5 implementation start
- CI PASS

## 19. Recommendation

`READY_FOR_A6_5_PLAN_REMEDIATION_MERGE`

Meaning only:

- this independent review remediation PR may be considered for merge into the planning branch;
- PR #10 remains Draft;
- A6.5-A remains NOT_STARTED.
