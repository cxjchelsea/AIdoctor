# A6.5-D TASK-D01 Synthetic Regression Planning and Gate-DI0 Definition

## 1. Executive Summary

This document is **PLANNING_ONLY** for TASK-D01.

It locks six authoritative eval/workflow/prompt targets, defines synthetic-only non-clinical evaluation boundaries, suite taxonomy (STATIC / INTERACTIVE / TRAJECTORY), coverage/gap/fixture/oracle/determinism taxonomies, the future `legacy-eval-asset-inventory.csv` schema, Open Decisions, Risks, Planning Validation Matrix, and Gate-DI0 authorization conditions.

```text
Gate-DI0: NOT_AUTHORIZED
TASK-D01 implementation: NOT_STARTED
legacy-eval-asset-inventory.csv: ABSENT (must remain absent in this PR)
```

## 2. Exact Enterprise Base

| Field | Value |
|---|---|
| Repository | `cxjchelsea/AIdoctor` |
| Enterprise branch | `agent/enterprise-agent-refactoring-plan` |
| Exact Head | `5b7b143b979fb5f3bc81e62b95642b88b9b0a4cc` |
| Meaning | TASK-C03 MERGED_AND_VERIFIED |
| Parents | `1b559fe…` + `452fd56…` |
| Planning branch | `agent/phase-a6-5-d-d01-synthetic-regression-plan` |

## 3. Authoritative Backlog Definition

From `a6-5-implementation-backlog.csv`:

| Field | Value |
|---|---|
| TASK | TASK-D01 |
| Batch | A6.5-D |
| Title | Synthetic regression planning for non-clinical behaviors |
| Description | Plan fixtures for workflow/tool/trace behaviors; forbid PHI and clinical gold labels; produce legacy-eval-asset-inventory.csv covering static/interactive/trajectory suite gaps (Coverage Matrix A6.5-F eval obligations) |
| Dependency | TASK-C01 |
| Priority / Risk | P1 / P1 |
| Owner / Reviewer | evaluation / evaluation\|clinical-advisor |
| Future artifact | `legacy-eval-asset-inventory.csv` |
| Acceptance | synthetic-only policy enforced; missing suites located or declared absent |
| Stop condition | real patient cases proposed as fixtures |

## 4. Current Phase State

```text
TASK-C01: MERGED_AND_VERIFIED
TASK-C03: MERGED_AND_VERIFIED
TASK-C02: BLOCKED_BY_TASK_B04
TASK-B04: BLOCKED
HUMAN_SUPERVISED_CLINICAL_READ: inactive
A6.5-D: PLANNING_IN_PROGRESS (this PR)
Gate-DI0: NOT_AUTHORIZED
TASK-D01 implementation: NOT_STARTED
A7 / Runtime / Production: unchanged blocked/not-started
Approved clinical rules/thresholds/hypotheses/sources: 0/0/0/0
```

## 5. Dependency Graph

```text
TASK-C01 (MERGED_AND_VERIFIED)
    └── TASK-D01 (this planning)

TASK-C03 (MERGED_AND_VERIFIED) — REFERENCE_ONLY for D01, not authoritative dependency
TASK-C02 / TASK-B04 — NOT required; remain blocked; must not be touched
```

```text
D01 dependency prerequisite: SATISFIED
C02 required: no
B04 required: no
```

## 6. Non-Clinical Evaluation Boundary

D01 evaluates **non-clinical software behavior** only:

- workflow state transition mechanics
- tool invocation structure
- contract-shaped request/response behavior
- identifier propagation
- trace/correlation behavior
- pause/resume/retry mechanics
- error/fallback mechanics
- deterministic routing mechanics
- schema conformance / serialization
- synthetic trajectory completion
- observability failure boundaries (via C03 REFERENCE_ONLY)

Forbidden evaluation goals:

- diagnosis / triage / treatment / medical recommendation correctness
- clinical threshold or rule correctness
- patient risk / outcome scoring

## 7. Synthetic-only Policy

```text
SYNTHETIC_ONLY: required
PHI: FORBIDDEN
REAL_PATIENT_CASES: FORBIDDEN
DEIDENTIFIED_PATIENT: FORBIDDEN
PSEUDONYMIZED_PATIENT: FORBIDDEN
PRODUCTION_LOG_DERIVED: FORBIDDEN
CLINICAL_CASE_COPY: FORBIDDEN
MEDICAL_CORPUS_EXCERPT: FORBIDDEN
```

Stop condition from backlog remains normative: real patient cases proposed as fixtures → stop.

## 8. Patient/PHI Prohibition

Static PHI-capable source paths (WF-001/WF-002/PROMPT-001) may be planned using:

```text
PATH_METADATA / FILE_TYPE / SYMBOL_REFERENCE / HASH
```

```text
static PHI-capable path
!=
patient data
```

No patient content, live/staging, Trace DB, clinical DB, Redis, or Neo4j access.

## 9. Clinical Gold-label Prohibition

```text
CLINICAL_GOLD_LABELS: FORBIDDEN
MEDICAL_CORRECTNESS_ORACLE: FORBIDDEN
PATIENT_OUTCOME_ORACLE: FORBIDDEN
DOCTOR_GOLD_LABEL: FORBIDDEN
REAL_PATIENT_EXPECTED_ANSWER: FORBIDDEN
```

If only clinical oracles could evaluate an asset → `NO_SAFE_ORACLE` + gap.

## 10. Authorized Target Inventory

| Asset | concrete_eval_asset_path | reference_document_path | Status |
|---|---|---|---|
| DATA-EV001 | **ABSENT** | `docs/AI医生/项目文档/12.性能与评估/评估验证体系.md` | PARTIALLY_PLANNABLE |
| DATA-EV002 | **ABSENT** | same documentation path | PARTIALLY_PLANNABLE |
| DATA-EV003 | **ABSENT** | same documentation path | PARTIALLY_PLANNABLE |
| WF-001 | `diagnosis-service/.../DiagnosisWorkflowOrchestrator.java` | n/a | READY_FOR_SYNTHETIC_PLANNING |
| WF-002 | `diagnosis-service/.../AgentLoop.java` | n/a | READY_FOR_SYNTHETIC_PLANNING |
| PROMPT-001 | `common/aidoctor_llm/prompt_manager.py` | n/a | PARTIALLY_PLANNABLE |

Path semantics (remediation for Independent Review Finding P01):

```text
reference_document_path
  = design/documentation citation only
  ≠ Evaluation Dataset
  ≠ concrete suite fixture

concrete_eval_asset_path
  = ABSENT for DATA-EV001..003
  = TRACKED_FILE path for WF-001 / WF-002 / PROMPT-001

resolved_path for DATA-EV
  = ABSENT_CONCRETE_SUITE;see_reference_document_path
  must NOT be treated as an existing suite path
```

```text
Target count: 6
Unique: 6
Missing inventory IDs: 0
Duplicates: 0
Unknown: 0
```

See `a6-5-d-d01-eval-target-register.csv`.

## 10a. Synthetic Provenance Proof (without content-first inspection)

`CONTENT_READ_BLOCKED_UNTIL_SYNTHETIC_PROVEN` must not create a circular open-file-first loop.

Safe provenance proof sources (any one or combination, recorded before content use):

```text
authoritative inventory/metadata declaring synthetic generation
fixture-generation provenance (generator id + seed/recipe)
explicit synthetic marker in trusted manifest
generator source under repo governance
trusted synthetic manifest signed/owned by evaluation
```

Unsafe / forbidden as first step:

```text
open candidate file and inspect patient-like content to decide if synthetic
```

If safe provenance cannot be established → treat as blocked / create new MINIMAL_SYNTHETIC fixtures instead of reading unknown content.

## 11. DATA-EV001 Analysis Boundary

- Documented static case eval suite; concrete files **MISSING_EXPECTED_ASSET** / `concrete_eval_asset_path=ABSENT`.
- Content read: blocked until synthetic proven via safe provenance (not content-first inspection).
- Primary planned suite: STATIC synthetic schema/invariant fixtures.
- INTERACTIVE / TRAJECTORY: explicit `NOT_APPLICABLE` by **asset role** (`static_case_eval_suite`), not merely because fixture is currently missing.

## 12. DATA-EV002 Analysis Boundary

- Interactive interview suite missing.
- Primary planned suite: INTERACTIVE synthetic bounded multi-turn (non-clinical).
- STATIC envelope + optional TRAJECTORY linkage also classified.

## 13. DATA-EV003 Analysis Boundary

- Trajectory replay suite missing.
- Primary planned suite: TRAJECTORY synthetic multi-step path (ordering, identifiers, termination).
- No patient-outcome oracle.

## 14. WF-001 Analysis Boundary

- Path exists (`DiagnosisWorkflowOrchestrator.java`, blob `4405be7c…`).
- Plan STATIC symbol/reference integrity, INTERACTIVE mocked tool/state-patch, TRAJECTORY multi-step orchestration.
- No clinical correctness.
- `runtime_required=no_for_authorized_scope` means authorized D01 Evidence may not require Runtime Store — **not** that local harness independence is already proven (`harness_feasibility=INSUFFICIENT_EVIDENCE` until Implementation).
- If only Runtime works → `GAP_RUNTIME_DEPENDENCY` / inventory `BLOCKED_RUNTIME_DEPENDENCY`.

## 15. WF-002 Analysis Boundary

- Path exists (`AgentLoop.java`, blob `3f5dcd0a…`).
- Plan STATIC structure, INTERACTIVE pause/resume/retry with mocks, TRAJECTORY seeded/mocked paths.
- INTERACTIVE/TRAJECTORY Evidence rows require an explicit **mock seam** or local substitute before claiming local evaluation.
- External LLM without seam → `NONDETERMINISTIC_BLOCKED` (do not expand authorization).
- `external_dependency_required=no_for_authorized_scope` is an authorization boundary, not a proof that AgentLoop is model-independent.

## 16. PROMPT-001 Analysis Boundary

- Path exists (`prompt_manager.py`, blob `dd1c1d89…`).
- Authorized: path, file type, symbol/reference, structural role, consumer relationship, hash, synthetic-test relevance.
- Structural scan may detect `INLINE_PROMPT_CONTENT_PRESENT` (long literals in manager). Record presence only; **do not** copy/extract clinical prompt text.
- **Not authorized:** prompt text dump, clinical instruction extraction, medical recommendation extraction, embedded-literal body read.
- Clinical prompt semantics: `GAP_BLOCKED_CLINICAL_GOLD` / `NO_SAFE_ORACLE`.
- If body were required to proceed → `PROMPT_BODY_REQUIRED_FOR_D01` + BLOCKED (not claimed here).

### 16a. `clinical_gold_required` field semantics (Interpretation A — locked)

```text
clinical_gold_required=no
  = authorized D01 execution must not require clinical gold

GAP_BLOCKED_CLINICAL_GOLD
  = desired clinical-semantic coverage cannot be safely evaluated
    without a separately authorized clinical oracle
```

These are compatible. Interpretation B (“obligation intrinsically needs no gold” vs blocked-gold status) is **rejected** as a reading of the coverage-plan fields.

## 17. Static Suite Definition

STATIC = deterministic checks without Runtime interaction.

Allowed: schema validation, fixture shape, contract compatibility, required fields, enum/domain, static workflow topology, asset existence, reference integrity, identifier format, deterministic pure-function behavior.

Forbidden claim:

```text
static fixture passes
!=
runtime workflow verified
```

## 18. Interactive Suite Definition

INTERACTIVE = single-step or bounded multi-turn synthetic interaction.

Constraints: synthetic input; mock/stub/local deterministic dependency; no patient data; no external model/API.

Validates: tool invocation, state patch, pause/resume, retry/fallback, error propagation, trace/correlation, contract-shaped interaction.

Does **not** validate clinical correctness.

## 19. Trajectory Suite Definition

TRAJECTORY = multi-step synthetic workflow path:

```text
initial state → synthetic action → transition → tool result → subsequent transition → terminal/non-terminal
```

Validates: ordering, state continuity, identifier continuity, termination mechanics, rollback/fallback, synthetic trace linkage.

Forbidden: clinical success outcome, diagnosis accuracy, medical gold-label comparison.

## 20. Coverage Obligation Model

Every formal target must have explicit dispositions for STATIC, INTERACTIVE, and TRAJECTORY (applicable or `NOT_APPLICABLE`).

Minimum: `6 × 3 = 18` suite disposition decisions.

This planning produces **21** coverage rows (triad + extra behavior obligations). See `a6-5-d-d01-suite-coverage-plan.csv`.

## 21. Coverage Status Taxonomy

Allowed:

```text
COVERED_EXISTING_SYNTHETIC
PARTIALLY_COVERED
GAP_NO_FIXTURE
GAP_NO_ORACLE
GAP_NO_HARNESS
GAP_BLOCKED_PHI
GAP_BLOCKED_CLINICAL_GOLD
GAP_RUNTIME_DEPENDENCY
NOT_APPLICABLE
INSUFFICIENT_EVIDENCE
```

Semantics:

```text
COVERED_EXISTING_SYNTHETIC
  = existing safe synthetic artifact + proven synthetic_provenance
  ≠ planning intent
  ≠ reference document existence

PARTIALLY_COVERED
  = some relevant non-executed planning/reference inputs exist
  ≠ COVERED_EXISTING_SYNTHETIC
  ≠ TEST_PASS / RUNTIME_PASS

INSUFFICIENT_EVIDENCE
  = path/metadata/reference only; no D01 fixture/harness yet
```

Runtime gap mapping:

```text
coverage_status / gap_type: GAP_RUNTIME_DEPENDENCY
inventory status: BLOCKED_RUNTIME_DEPENDENCY
```

Forbidden:

```text
PRODUCTION_VALIDATED
CLINICALLY_VALIDATED
PATIENT_VALIDATED
```

## 22. Gap Taxonomy

Gaps record missing fixtures, oracles, harnesses, PHI/clinical blocks, runtime dependency blocks, insufficient evidence, or N/A suite applicability. Missing DATA-EV suites are recorded as `GAP_NO_FIXTURE` (or role-based `NOT_APPLICABLE`), not fabricated datasets. Documentation path existence must never upgrade a missing suite to EXISTING / COVERED_EXISTING_SYNTHETIC.

## 23. Fixture Taxonomy

Allowed future fixtures:

```text
MINIMAL_SYNTHETIC
BOUNDARY_SYNTHETIC
INVALID_SYNTHETIC
ERROR_INJECTION_SYNTHETIC
STATE_SEQUENCE_SYNTHETIC
CONTRACT_FIXTURE
TRACE_CONTEXT_SYNTHETIC
MOCK_TOOL_RESULT
```

Forbidden:

```text
REAL_PATIENT
DEIDENTIFIED_PATIENT
PSEUDONYMIZED_PATIENT
PRODUCTION_LOG_DERIVED
CLINICAL_CASE_COPY
MEDICAL_CORPUS_EXCERPT
```

## 24. Oracle Taxonomy

Allowed:

```text
SCHEMA_ORACLE
EXACT_VALUE_ORACLE
STATE_TRANSITION_ORACLE
INVARIANT_ORACLE
EVENT_SEQUENCE_ORACLE
ERROR_CLASS_ORACLE
IDENTIFIER_PROPAGATION_ORACLE
TRACE_LINKAGE_ORACLE
SNAPSHOT_STRUCTURE_ORACLE
MANUAL_NON_CLINICAL_REVIEW
NO_SAFE_ORACLE
```

Forbidden:

```text
CLINICAL_DIAGNOSIS_ORACLE
MEDICAL_CORRECTNESS_ORACLE
PATIENT_OUTCOME_ORACLE
DOCTOR_GOLD_LABEL
REAL_PATIENT_EXPECTED_ANSWER
```

## 25. Determinism Taxonomy

```text
DETERMINISTIC
SEEDED_DETERMINISTIC
MOCKED_DETERMINISTIC
NONDETERMINISTIC_BLOCKED
UNKNOWN
```

D01 prefers the first three. Must not attach external LLM to gain “coverage”.

## 26. Runtime / External Dependency Boundary

Coverage-plan fields:

```text
runtime_required=no_for_authorized_scope
external_dependency_required=no_for_authorized_scope
```

mean **PLANNED AUTHORIZATION BOUNDARY**, not **PROVEN IMPLEMENTATION FEASIBILITY**.

Future D01 Evidence Implementation must not require:

production; staging patient traffic; patient/clinical DB; Redis/Neo4j runtime stores; production tracing backend; external LLM/model API; third-party evaluation SaaS.

Allowed to plan: local unit-level; local synthetic integration; mocked service; in-memory harness; static validator.

Harness feasibility remains `INSUFFICIENT_EVIDENCE` until Implementation proves a local seam.

If coverage only possible with true Runtime → record `GAP_RUNTIME_DEPENDENCY`; do not expand authorization.

If AgentLoop control flow requires external model without mock seam → `NONDETERMINISTIC_BLOCKED`.

## 27. C03 Evidence Reuse Boundary

TASK-C03 is MERGED_AND_VERIFIED.

D01 may REFERENCE_ONLY:

- failure isolation classifications
- trace identifier / Feign propagation static observations
- synthetic observability scenario definitions

D01 must not:

- modify C03 Evidence
- claim C03 Test Matrix Runtime-executed
- implement observability runtime fixes
- deploy OTel

## 28. C02/B04 Isolation

```text
TASK-C02: BLOCKED_BY_TASK_B04
TASK-B04: BLOCKED
```

Correct D01 posture:

```text
non-clinical synthetic coverage proceeds
clinical-dependent coverage remains GAP/BLOCKED
```

Forbidden: extract clinical policy; read prompt bodies for clinical content; approve rules; create clinical gold labels.

## 29. A6.5-F Coverage Matrix Relationship

Located authoritative document:

```text
docs/refactoring/目标能力与旧设计资产覆盖矩阵.md
```

Contains DESIGNED A6.5-F rows for static / interactive / trajectory eval suites.

`a6-5-decision-log.md` OD-007 retains A6.5-F deliverables via D/E mapping (eval inventory in D; decommission in E).

```text
A65D-OD-001: ACKNOWLEDGED (reference located)
Write-back authorized: no
A6.5-F completion claimed: no
```

D01 will produce an inventory **capable of future** Coverage Matrix write-back; write-back itself is out of scope (see A65D-OD-010).

## 30. Future `legacy-eval-asset-inventory.csv` Schema

Future Implementation file (NOT created in this planning PR):

```text
docs/refactoring/evidence/phase-a/a6-5/legacy-eval-asset-inventory.csv
```

Locked header:

```text
eval_inventory_id
execution_id
task_id
source_asset_id
source_path
behavior_domain
suite_class
fixture_reference
fixture_class
fixture_source
fixture_generator
oracle_class
oracle_execution_state
determinism_class
synthetic_only
synthetic_provenance
content_read_state
phi_present
clinical_gold_present
runtime_dependency
external_dependency
coverage_status
gap_type
planned_or_existing
validation_level
evidence_reference
owner_role
reviewer_role
status
notes
```

Field rules (Independent Review remediation):

```text
synthetic_only=yes
  ≠ sufficient provenance by itself

synthetic_provenance
  = required when synthetic_only=yes and fixture is EXISTING or content will be read
  = one of: AUTHORITATIVE_METADATA | GENERATOR_PROVENANCE | EXPLICIT_MARKER |
            GENERATOR_SOURCE | TRUSTED_MANIFEST | NEW_MINIMAL_SYNTHETIC_CREATED

content_read_state
  = NOT_READ | READ_AFTER_PROVENANCE | BLOCKED_UNPROVEN

oracle_execution_state
  = PROPOSED | EXECUTABLE_LOCAL | NOT_EXECUTABLE

planned_or_existing
  PLANNED = inventory/planning row only
  EXISTING = artifact actually exists AND safe provenance established
  DATA-EV missing concrete suites must not be EXISTING

status
  INVENTORIED = inventory row exists
  ≠ fixture implemented
  ≠ test executed
  ≠ PASS

  Also allowed: GAP_RECORDED | BLOCKED_PHI | BLOCKED_CLINICAL_GOLD |
                BLOCKED_RUNTIME_DEPENDENCY | INSUFFICIENT_EVIDENCE | NOT_APPLICABLE
```

## 31. Future ID Rules

```text
A65D-D01-EVAL-<ASSET_ID>-<SUITE>-<NN>
```

Requirements: deterministic, stable, unique. Example: `A65D-D01-EVAL-WF-001-STATIC-01`.

## 32. Future Validation Strategy

D01 Implementation (after Gate-DI0) is primarily:

```text
eval asset inventory / planning evidence
```

not Runtime PASS theater.

Validation should prove: synthetic-only; forbidden fixture/oracle absence; suite dispositions; inventory integrity; A6/Contracts regression; git scope; C02/B04/C03 immutability.

Must not invent `TEST_PASS` / `RUNTIME_PASS` / `PRODUCTION_READY` / `CLINICALLY_VALIDATED` inventory statuses.

## 33. Open Decisions

See `a6-5-d-d01-open-decisions.md` (A65D-OD-001..010).

Closed by planning: **0**.

## 34. Risks

See `a6-5-d-d01-risk-register.csv` (A65D-RISK-001..012).

All remain **OPEN**.

## 35. Gate-DI0

### Name

```text
Gate-DI0
TASK-D01 Synthetic Regression Evidence Implementation Authorization
```

### Current state

```text
Gate-DI0: DEFINED
Gate-DI0 authorization: NOT_AUTHORIZED
```

### Required checks (23)

| ID | Check | Method | Evidence source | Pass criterion |
|---|---|---|---|---|
| GDI0-01 | Enterprise exact planning base verified | GIT_REV_PARSE | enterprise branch | equals authorized Enterprise Head for the Gate assessment |
| GDI0-02 | D01 planning merged and verified | PR/merge audit | Planning PR + Enterprise | planning review-integrated and Enterprise-merged as required by Gate assessment prompt |
| GDI0-03 | D01 independent planning review complete | PR audit | Review PR | Independent Review merged/integrated; blocking findings 0 |
| GDI0-04 | six target assets reconciled | COUNT/SET | backlog+inventory | exactly DATA-EV001..003,WF-001,WF-002,PROMPT-001 |
| GDI0-05 | no unresolved blocking target path conflict | PATH_RECONCILE | Target Register vs inventory | DATA-EV concrete_eval_asset_path=ABSENT or proven; WF/PROMPT TRACKED_FILE paths exist; no doc-path-as-suite |
| GDI0-06 | synthetic-only policy defined | DOC | Plan §7 | SYNTHETIC_ONLY required; forbidden classes listed |
| GDI0-07 | real/deidentified patient fixture prohibition defined | DOC+ENUM | Plan fixture taxonomy | REAL/DEIDENTIFIED/PSEUDONYMIZED/PRODUCTION_LOG_DERIVED forbidden |
| GDI0-08 | clinical gold-label prohibition defined | DOC+ENUM | Plan oracle taxonomy | clinical oracles forbidden; Interpretation A locked |
| GDI0-09 | safe oracle taxonomy defined | DOC | Plan §24 | allowed/forbidden oracle lists present |
| GDI0-10 | fixture taxonomy defined | DOC | Plan §23 | allowed/forbidden fixture lists present |
| GDI0-11 | suite taxonomy defined | DOC | Plan §17-19 | STATIC≠INTERACTIVE≠TRAJECTORY |
| GDI0-12 | all 6×3 suite dispositions planned | SET | coverage CSV | 18/18 (asset,suite) pairs present; missing=0 |
| GDI0-13 | future inventory schema defined | DOC | Plan §30 | schema includes synthetic_provenance + content_read_state |
| GDI0-14 | PROMPT-001 body access not required | BOUNDARY | Target Register + coverage | structural obligations only; body/embedded literals not required |
| GDI0-15 | runtime store not required | BOUNDARY | coverage runtime_required | authorized scope forbids Runtime Store; gaps use GAP_RUNTIME_DEPENDENCY |
| GDI0-16 | external model/API not required | BOUNDARY | coverage external_dependency | authorized scope forbids external model; else NONDETERMINISTIC_BLOCKED |
| GDI0-17 | C02/B04 remain untouched | SCOPE | git/phase state | no C02/B04 authorization or clinical extraction |
| GDI0-18 | approved counts remain zero | COUNT | phase state | 0/0/0/0 |
| GDI0-19 | baseline A6 regression passes | LOCAL_TEST | validator+pytest | 11/25/5/0 and 59 passed (local ≠ CI PASS) |
| GDI0-20 | baseline Contracts regression passes | LOCAL_TEST | validator+pytest | 13/13/33 and 110 passed |
| GDI0-21 | git scope clean | GIT_DIFF | Evidence PR scope | out-of-scope=0; source/contracts untouched |
| GDI0-22 | synthetic provenance proof defined | DOC | Plan §10a + schema | safe provenance sources listed; open-file-first forbidden |
| GDI0-23 | missing DATA-EV assets cannot be marked EXISTING | SCHEMA_RULE | Plan §30 planned_or_existing | ABSENT concrete suites ⇒ PLANNED/GAP only; never EXISTING/COVERED_EXISTING_SYNTHETIC without artifact+provenance |

This planning package defines Gate-DI0; it does **not** authorize it.

## 36. Independent Planning Review Requirements

A separate Independent Planning Review must verify:

- target cardinality and path reconciliation
- synthetic-only / PHI / gold / prompt-body boundaries
- suite/coverage completeness (≥18 dispositions)
- future inventory schema completeness
- Gate-DI0 completeness
- no implementation artifact created
- no source/contract/runtime mutation

Only after review integration + Gate-DI0 assessment may D01 Evidence Implementation be authorized.

## 37. Planning Validation Summary

See `a6-5-d-d01-plan-validation-matrix.csv`.

```text
Required planning definition checks: 48 (post Independent Review remediation)
Status values: DEFINED (not PASS)
False implementation PASS claims: 0
```

## 38. Explicit Non-Authorization

This PR does not authorize:

- D01 Evidence Implementation
- creation of `legacy-eval-asset-inventory.csv`
- Gate-DI0
- Runtime harness execution against live services
- OTel / dual-write / observability runtime changes
- C02 / B04 / clinical extraction / prompt body read
- A7 / Production
- closing Open Decisions or Risks

## 39. Exit Criteria

Planning is complete for Independent Planning Review when:

- six planning artifacts exist
- six targets reconciled
- ≥18 suite dispositions present
- Gate-DI0 defined with 23 checks (method/evidence/pass criteria + provenance/EXISTING controls)
- baseline A6/Contracts local verification green
- planning remediation scoped; implementation artifact absent

## 40. Recommendation

```text
READY_FOR_A6_5_D_D01_PLANNING_REVIEW_INTEGRATION
```

(Planning Review Integration is a separate authorized step. Gate-DI0 remains NOT_AUTHORIZED.)
