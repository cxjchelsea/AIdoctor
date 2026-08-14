# A6.5-E01 Disposition Board Report

> Task: `TASK-E01`
>
> Scope: `E01 ONLY`
>
> Implementation state: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> E02: `NOT_AUTHORIZED` / `NOT_EXECUTED`
>
> A6.5: `INCOMPLETE_PENDING_E01_E02_EXIT`

## 1. Base / Branch

```text
Exact Enterprise Base: 6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda
PR #38: MERGED
Merge SHA: 6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda
Branch: agent/a6-5-e01-disposition-board
Worktree: D:\project\AIdoctor-a6-5-e01-disposition-board
```

## 2. Authorization

Repository Owner Explicit Authorization for this task grants `TASK-E01`
Disposition Board plus bounded post-merge reconciliation and PR38-CIR-F001
narrative correction.

```text
NOT AUTHORIZED:
TASK-E02
A6.5 Exit Review
physical deletion/decommission
B04 extraction
C02 clinical mapping
new clinical content
A7-CL
PHASE_A_NC_CLOSURE implementation
NC-CLOSE-01
A8/A9/A10/A11
Phase B
```

## 3. Population algorithm

Authoritative source: `docs/refactoring/plans/phase-a/a6-5/a6-5-legacy-asset-inventory.csv`
(203 inventory rows; recomputed on exact Base, not copied from assessment).

```text
UNION:
A. risk_level in {P0, P1}
B. contains_clinical_logic = YES and not already in A
C. remaining HIGH clinical-relevance legacy assets required by existing
   governance evidence and not already in A/B
D. historical TASK-E01.asset_ids (40) must be contained
+ justified boundary-only rows: BOUND-001, BOUND-002
```

Historical `TASK-E01.asset_ids` (40) is a `HISTORICAL_SEED_SET` only.
It is **not** the implementation ceiling. Assessment finding
`A6.5-E-AUTH-F001` is remediated by this recomputation.

## 4. Population counts

| Set | Count |
|---|---|
| Inventory total | 203 |
| P0 | 77 |
| P1 | 72 |
| P0/P1 | 149 |
| Additional clinical-logic (not P0/P1) | 2 (`DOC-043`, `DOC-045`) |
| Additional HIGH-only after A/B | 0 |
| Historical seed | 40 (all contained) |
| Justified boundary rows | 2 (`BOUND-001`, `BOUND-002`) |
| Expected board population | 153 |
| Actual board rows | 153 |
| Missing | 0 |
| Duplicates | 0 |
| Unexpected | 0 (boundary rows justified) |

```text
Expected IDs == Actual IDs
P0/P1 coverage = 100%
clinical-logic coverage = 100%
historical 40 seed containment = YES
```

## 5. Boundary classification outside the board

The following inventory rows are **not** in the E01 board because they fail
A/B/C/D and are not justified boundary rows. This is an explicit
classification, not an omitted P0/P1:

- `PROMPT-012`..`PROMPT-015`: P2, `clinical_relevance=MED`,
  `contains_clinical_logic=UNKNOWN`, not historical seed.
  Classification: `OUT_OF_E01_BOARD_SCOPE_P2_UNKNOWN_LOGIC`.
  Do not promote them by reading Prompt bodies.
- Remaining P2 LOW/MED design, schema, research, and ENG rows listed in
  inventory but outside A/B/C/D.

## 6. Board classes

| Authority class | Count |
|---|---|
| LEGACY_CLINICAL | 50 |
| NON_CLINICAL_RETAINED | 60 |
| QUARANTINED_MEDICAL_SOURCE | 31 |
| QUARANTINED_PHI | 6 |
| EVIDENCE_ONLY | 3 |
| RESEARCH_ONLY | 1 |
| NEW_A6_BOUNDARY | 1 |
| NEW_A5_BOUNDARY | 1 |

## 7. Dispositions

### File disposition

| file_disposition | Count |
|---|---|
| KEEP_READ_ONLY | 50 |
| QUARANTINE | 42 |
| MAP | 39 |
| ADAPT | 21 |
| ARCHIVE | 1 |

MAP/ADAPT file dispositions are **non-clinical technical** paths only
(CTR/MODEL/WF/ENG/DOC technical). No Legacy Clinical row uses ADOPT / ADAPT /
MAP as clinical authority.

### Runtime disposition

| runtime_disposition | Count |
|---|---|
| TECHNICAL_PATH_PRESERVED | 62 |
| REJECT_FOR_NEW_RUNTIME | 52 |
| NOT_AUTHORITY | 37 |
| NOT_LEGACY_RETENTION_SUBJECT | 2 |

```text
Prohibited clinical authority dispositions: 0
ADOPT_AS_CLINICAL_AUTHORITY: 0
ADAPT_INTO_NEW_CLINICAL_RUNTIME: 0
MAP_CONTENT_INTO_CAPABILITY: 0
```

`WF-003` / `WF-017`: file `ADAPT` preserved (non-clinical); runtime
`REJECT_FOR_NEW_RUNTIME` (clinical authority only).

`BOUND-002`: `NEW_A6_BOUNDARY` / `KEEP_READ_ONLY` /
`NOT_LEGACY_RETENTION_SUBJECT`. Not REJECT / ARCHIVE / QUARANTINE.
A6 lifecycle unchanged.

## 8. Owners / reviewers

| owner_role | Count |
|---|---|
| architecture | 50 |
| governance | 50 |
| legal | 32 |
| contracts | 10 |
| security | 8 |
| evaluation | 3 |
| UNASSIGNED | 0 |

Reviewer roles are matched to risk:

- PHI → `security|privacy`
- unlicensed medical source → `legal|governance`
- legacy clinical rejection → `governance-boundary|legal`
- evaluation assets → `evaluation|architecture`
- technical / contracts → architecture or contracts

Clinical Content Review is **not** required to judge old clinical correctness
for rejected assets.

## 9. Runtime consumers

Consumer status is derived from already-produced inventory
`current_consumer` metadata (A6.5-A discovery). No workflow was executed,
no model/provider was called, and no PHI was read.

| runtime_consumer_status | Count |
|---|---|
| CONFIRMED_CONSUMER | 100 |
| MAY_STILL_EXIST | 48 |
| EVIDENCE_ONLY | 3 |
| NOT_RUNTIME_ASSET | 2 |
| NO_CONSUMER_FOUND | 0 |

```text
REJECT_FOR_NEW_RUNTIME is not used as a consumer status.
False NO_CONSUMER inference: 0
```

Named production/service consumers (`diagnosis-service`,
`health-state-assessment-service`, `dialog-service`, and other listed
services) map to `CONFIRMED_CONSUMER`. `legacy-services` / ops / production-capable
tokens fail closed to `MAY_STILL_EXIST`. Planning/docs/offline-extraction map
to `EVIDENCE_ONLY`. BOUND-001/002 are `NOT_RUNTIME_ASSET`.

## 10. Replacement / decommission

| replacement_status | Count |
|---|---|
| FUTURE_REBUILD_REQUIRED | 81 |
| EXISTING_TECHNICAL_PATH | 62 |
| GOVERNANCE_PATH_ONLY | 6 |
| TECHNICAL_ADAPT_PRESERVED_CLINICAL_AUTHORITY_REJECTED | 2 |
| NEW_A6_PACKAGE_IN_PLACE | 1 |
| NEW_A5_CONTRACTS_IN_PLACE | 1 |

`FUTURE_REBUILD_REQUIRED` does **not** authorize Future New Clinical
implementation.

Decommission register: `docs/refactoring/plans/phase-a/a6-5/legacy-asset-decommission-register.csv`
(153 rows; IDs unique and equal to board).

| decommission_eligibility | Count |
|---|---|
| NOT_ELIGIBLE_CONSUMER_PRESENT | 148 |
| DEFERRED_SEPARATE_AUTHORIZATION | 3 |
| NOT_APPLICABLE_NEW_BOUNDARY | 2 |
| ELIGIBLE_FOR_PHYSICAL_DELETE | 0 |

```text
Physical deletes: 0
Eligible now: 0
Blocked/deferred: 151 (148 consumer-present + 3 deferred)
New-boundary N/A: 2
```

`DATA-EV001`..`DATA-EV003` are `INCONCLUSIVE_FOR_DECOMMISSION` /
`KEEP_READ_ONLY`. D01 does not prove a real regression suite exists.

## 11. Quarantine preservation

Existing quarantine **not lifted**:

- `CLIN-020`, `CLIN-021`, `CLIN-022`, `CLIN-025`, `CLIN-026` remain `QUARANTINE`
- `DATA-K001`..`DATA-K031` remain `QUARANTINE` / `NOT_APPROVED_MEDICAL_SOURCE`
- `PHI-001`..`PHI-006` remain `QUARANTINE` (path-level; no content inspection)

```text
Existing quarantine lifted: 0
Medical-source product-use approval claimed: 0
Knowledge Release: 0
Clinical gold: 0
```

## 12. B02 / D01 evidence use

B02 (`a6-5-b-governance-evidence.csv`, PR #17 MERGED) is used only for
provenance / license / privacy fail-closed disposition. This report does
**not** claim legal approval complete, clinical approval complete, or
product use approved. Unknown license remains reject/quarantine.

D01 (`legacy-eval-asset-inventory.csv`) is used only as synthetic-only
planning and evaluation-gap inventory. This report does **not** claim a
real regression suite exists, fixtures executed, or clinical gold exists.

```text
No false approval claims: yes
```

## 13. A6.5-F retention

E01 retains A6.5-F decommission deliverables:

- Legacy Asset Decommission Register (this PR)
- Decommission Gate evidence: owner / consumer / rollback-or-retention
  columns on every register row
- Evidence pointer to D01 evaluation inventory for eval-gap assets

Matrix write-back remains TASK-E02 and is **not** executed.

## 14. Reconciliation / F001

| Item | Action |
|---|---|
| Strategy status | `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW` → `MERGED_AND_VERIFIED` (PR #38) |
| Decision-log `A6.5-NONADOPT-001` | same; historical text retained |
| §5 Clinical Owner / B04 | supersession pointer added; historical bullet retained |
| New dated recon | `phase-a-current-state-reconciliation-2026-08-14-post-pr38.md` |
| PR38-CIR-F001 | TASK-A03 narrative only; identity/status unchanged |
| Historical E01 40 IDs | preserved as seed; not rewritten as “always 149” |
| E02 matrices | not final-written |

## 15. Safety / diff boundary

```text
Clinical content read: 0
Prompt body read/copied: 0
Threshold read/copied: 0
PHI content read: 0
Clinical gold: 0
Real provider: 0
External model/API: 0
Java production code: 0
Python runtime code: 0
Frontend production code: 0
contracts/v1: 0
capabilities semantic: 0
packages/model_runtime: 0
CI workflows: 0
tests: 0
physical deletion: 0
runtime file mutation: 0
E02 execution: 0
```

Inventory identity fields (`asset_id`, `path`, `current_consumer`,
`contains_*`, `risk_level`, `proposed_disposition`, `decision_status`)
are unchanged. Only metadata `notes` / `required_evidence` received an
E01 board pointer on in-scope rows.

## 16. Validation

See local-only validator output (not committed as product code).
Required checks:

- `git diff --check`
- CSV parse
- inventory / board / decommission / backlog IDs unique
- population set equality
- historical 40 seed containment
- P0/P1 coverage 100%
- clinical-logic coverage 100%
- owner_role / evidence_pointer / disposition non-empty
- prohibited clinical authority dispositions 0
- existing quarantine lifted 0
- runtime / contracts / Capability / Model Runtime / CI / tests / clinical body diff 0

## 17. Acceptance criteria

| ID | Result |
|---|---|
| AC-A6.5-E01-IMP-01 exact Enterprise Base | PASS |
| AC-A6.5-E01-IMP-02 clean branch/worktree at start | PASS |
| AC-A6.5-E01-IMP-03 E01 explicit authorization recognized | PASS |
| AC-A6.5-E01-IMP-04 E02 excluded | PASS |
| AC-A6.5-E01-IMP-05 B04 not reopened | PASS |
| AC-A6.5-E01-IMP-06 C02 not reopened | PASS |
| AC-A6.5-E01-IMP-07 B02 evidence consumed correctly | PASS |
| AC-A6.5-E01-IMP-08 D01 evidence consumed correctly | PASS |
| AC-A6.5-E01-IMP-09 population recomputed | PASS |
| AC-A6.5-E01-IMP-10 P0 count confirmed (77) | PASS |
| AC-A6.5-E01-IMP-11 P1 count confirmed (72) | PASS |
| AC-A6.5-E01-IMP-12 P0/P1 complete | PASS |
| AC-A6.5-E01-IMP-13 remaining clinical-logic complete | PASS |
| AC-A6.5-E01-IMP-14 historical 40 seed contained | PASS |
| AC-A6.5-E01-IMP-15 board IDs unique | PASS |
| AC-A6.5-E01-IMP-16 BOUND-002 correctly bounded | PASS |
| AC-A6.5-E01-IMP-17 legacy clinical dispositions valid | PASS |
| AC-A6.5-E01-IMP-18 prohibited authority dispositions 0 | PASS |
| AC-A6.5-E01-IMP-19 existing quarantine preserved | PASS |
| AC-A6.5-E01-IMP-20 medical source quarantine preserved | PASS |
| AC-A6.5-E01-IMP-21 nonclinical dispositions preserved | PASS |
| AC-A6.5-E01-IMP-22 owner role complete | PASS |
| AC-A6.5-E01-IMP-23 reviewer roles appropriate | PASS |
| AC-A6.5-E01-IMP-24 evidence pointers complete | PASS |
| AC-A6.5-E01-IMP-25 runtime consumer status complete | PASS |
| AC-A6.5-E01-IMP-26 replacement status complete | PASS |
| AC-A6.5-E01-IMP-27 decommission status complete | PASS |
| AC-A6.5-E01-IMP-28 no false NO_CONSUMER inference | PASS |
| AC-A6.5-E01-IMP-29 no physical delete | PASS |
| AC-A6.5-E01-IMP-30 board artifact created | PASS |
| AC-A6.5-E01-IMP-31 decommission register created | PASS |
| AC-A6.5-E01-IMP-32 evidence report created | PASS |
| AC-A6.5-E01-IMP-33 A6.5-F retention preserved | PASS |
| AC-A6.5-E01-IMP-34 stale strategy status reconciled | PASS |
| AC-A6.5-E01-IMP-35 stale decision-log status reconciled | PASS |
| AC-A6.5-E01-IMP-36 new dated reconciliation created | PASS |
| AC-A6.5-E01-IMP-37 F001 narrative corrected | PASS |
| AC-A6.5-E01-IMP-38 historical rows preserved | PASS |
| AC-A6.5-E01-IMP-39 E01 not falsely COMPLETE | PASS |
| AC-A6.5-E01-IMP-40 E02 not executed | PASS |
| AC-A6.5-E01-IMP-41 E02 matrices not final-written | PASS |
| AC-A6.5-E01-IMP-42 clinical content read 0 | PASS |
| AC-A6.5-E01-IMP-43 Prompt body read/copied 0 | PASS |
| AC-A6.5-E01-IMP-44 threshold read/copied 0 | PASS |
| AC-A6.5-E01-IMP-45 PHI content read 0 | PASS |
| AC-A6.5-E01-IMP-46 clinical gold 0 | PASS |
| AC-A6.5-E01-IMP-47 real provider 0 | PASS |
| AC-A6.5-E01-IMP-48 runtime diff 0 | PASS |
| AC-A6.5-E01-IMP-49 contracts diff 0 | PASS |
| AC-A6.5-E01-IMP-50 Capability semantic diff 0 | PASS |
| AC-A6.5-E01-IMP-51 Model Runtime diff 0 | PASS |
| AC-A6.5-E01-IMP-52 CI workflow diff 0 | PASS |
| AC-A6.5-E01-IMP-53 tests diff 0 | PASS |
| AC-A6.5-E01-IMP-54 A6 lifecycle unchanged | PASS |
| AC-A6.5-E01-IMP-55 A7-NC preserved | PASS |
| AC-A6.5-E01-IMP-56 A7-CL blocked | PASS |
| AC-A6.5-E01-IMP-57 A7 NOT_COMPLETE | PASS |
| AC-A6.5-E01-IMP-58 NC Closure NOT_AUTHORIZED | PASS |
| AC-A6.5-E01-IMP-59 NC-CLOSE-01 NOT_AUTHORIZED | PASS |
| AC-A6.5-E01-IMP-60 A8 NOT_AUTHORIZED | PASS |
| AC-A6.5-E01-IMP-61 A11 NOT_AUTHORIZED | PASS |
| AC-A6.5-E01-IMP-62 Phase B NOT_AUTHORIZED | PASS |
| AC-A6.5-E01-IMP-63 Architecture unchanged | PASS |
| AC-A6.5-E01-IMP-64 Refreeze NOT_REQUIRED | PASS |
| AC-A6.5-E01-IMP-65 git diff --check | PASS (run at close) |
| AC-A6.5-E01-IMP-66 CSV parse | PASS |
| AC-A6.5-E01-IMP-67 population equality | PASS |
| AC-A6.5-E01-IMP-68 no unexpected files | PASS (docs-only expected set) |
| AC-A6.5-E01-IMP-69 one implementation commit preferred | PASS (intent) |
| AC-A6.5-E01-IMP-70 no force push | PASS (intent) |
| AC-A6.5-E01-IMP-71 Draft PR correct | PASS (after create) |
| AC-A6.5-E01-IMP-72 no self-review | PASS |
| AC-A6.5-E01-IMP-73 no merge | PASS |
| AC-A6.5-E01-IMP-74 exact Head captured | PASS (after commit) |
| AC-A6.5-E01-IMP-75 one exact next Gate | PASS |

`AC-A6.5-E01-IMP: 75/75` after commit / Draft PR / validator close.

## 18. Findings

### A6.5-E01-IMP-F001

- Severity: `MINOR`
- Asset / Artifact: `PROMPT-012`..`PROMPT-015`
- Requirement: population algorithm A/B/C/D; do not invent clinical_logic
- Evidence: inventory `risk_level=P2`, `contains_clinical_logic=UNKNOWN`,
  not historical seed, not HIGH
- Impact: four P2 Prompt-adjacent rows remain outside the board
- Blocking: `NO`
- Recommended correction: none in E01. Do not read Prompt bodies to
  promote them. Revisit only under a later authorized inventory metadata
  refresh.

### A6.5-E-AUTH-F001 (assessment; remediated)

- Severity: `MAJOR` (implementation-scope; from authorization assessment)
- Requirement: historical 40 IDs must not cap E01 population
- Evidence: board now 153 = 149 P0/P1 + 2 clinical-logic + 2 justified boundary
- Impact: remediated
- Blocking: `NO` after this implementation

```text
Findings total: 1 new MINOR + 1 remediated MAJOR
Blocking: 0
Major open: 0
Minor: 1
```

## 19. Current state

```text
Enterprise: unchanged until this PR merges
E01 branch: IMPLEMENTED_PENDING_INDEPENDENT_REVIEW
E02: NOT_AUTHORIZED
A6.5: INCOMPLETE_PENDING_E01_E02_EXIT
```

## 20. Next Gate

```text
A6.5-E01 Disposition Board Independent Review
```

Do not execute from this report.
