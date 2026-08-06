# Phase A6.5 Legacy Asset Validation Plan

Document status: **Draft planning**
Planning branch: `agent/phase-a6-5-legacy-asset-validation-plan`
Enterprise base: `b4a1714506ac7b05ce2d8c7b6ecae3bd7bb2e836` (PR #8 merge commit)
A6 status: `PHASE_A6_MERGED_AND_VERIFIED`
A6.5 status: `NOT_STARTED` (this PR plans only)

## 1. Executive Summary

Phase A6.5 is the mandatory gate between the A6 Capability Package skeleton and A7 Model Runtime. It exists to discover, classify, validate (structurally / governance-wise / mapping-wise), and disposition legacy design and system assets **without** migrating them, approving clinical content, enabling Runtime, or changing production eligibility.

This plan delivers:

- authoritative scope analysis;
- a 203-row legacy asset inventory;
- L0–L5 validation model and 259 planned validation matrix rows;
- risk register (18) and decision log;
- implementation backlog (12 tasks) across batches A–E;
- explicit non-claims and stop conditions.

It does **not** implement A6.5-A through A6.5-E.

## 2. Current Baseline

| Item | Value |
| --- | --- |
| Repository | `cxjchelsea/AIdoctor` |
| Enterprise branch | `agent/enterprise-agent-refactoring-plan` |
| Enterprise Head | `b4a1714506ac7b05ce2d8c7b6ecae3bd7bb2e836` |
| A6 package | `capabilities/adult_respiratory_v1` |
| Lifecycle | `DRAFT` |
| Clinical review | `REQUIRES_CLINICAL_REVIEW` |
| Runtime adoption | `NOT_IMPLEMENTED` |
| Production eligibility | `BLOCKED` |
| Approved clinical rules / thresholds / hypotheses / sources | `0 / 0 / 0 / 0` |
| A6 local verification baseline | Python 3.13.5; validator 11/25/5/0; pytest 59 passed; not CI PASS |
| Pre-existing A6.5 branch/PR | none found |

## 3. Authoritative Sources

| Source | Section | A6.5 requirement | Binding strength | Interpretation |
| --- | --- | --- | --- | --- |
| `docs/refactoring/可执行实施路线.md` | A6.5 Legacy Design Asset Validation | Scan principles/protocols/state/rules/AOP/eval/knowledge; map to code/config/data/tests; evidence levels; KEEP/ADAPT/...; exit gate before A7 | NORMATIVE | Primary executable definition and exit gate |
| `docs/refactoring/原设计资产保留、改造与目标架构映射.md` | §15 Phase A6.5 | Scan `docs/AI医生/项目文档`; inventory fields; deliverables; no delete before validation | NORMATIVE | Inventory schema + design-doc root |
| `docs/refactoring/工程、发布、回滚与下线.md` | §3.4 / §10 Legacy Asset Retention Gate | Required A6.5 deliverables; KEEP/ADAPT/EVALUATE/ARCHIVE gates | NORMATIVE | Retention / decommission gates |
| `docs/refactoring/目标能力与旧设计资产覆盖矩阵.md` | §10 Legacy Design Asset Coverage | Every legacy asset needs Evidence/Decision/Owner/Contract/Validation; DESIGNED→VALIDATED only after A6.5 evidence | GOVERNING | Coverage obligations; lists A6.5-C/D/F labels |
| `docs/refactoring/代码、数据与设计资产迁移矩阵.md` | Legacy / knowledge rows | No discard of rules/prompts/evals/AOP/graph without inventory decisions | GOVERNING | Migration constraints |
| `docs/refactoring/README.md` | Phase sequence | A6 → A6.5 → A7 → … → Frozen Baseline | GOVERNING | Sequencing; Freeze Candidate narrative may lag evidence |
| `docs/refactoring/架构冻结基线.md` | Frozen Baseline gate | A6.5 required before Frozen Baseline | GOVERNING | Downstream gate only |
| `docs/refactoring/evidence/phase-a/a4/*` | Data inventory | Prior DATA_VERIFIED structural facts for 13 files; PHI-capable stores | INFORMATIVE | Reuse, do not re-claim as clinical validation |
| `docs/refactoring/evidence/phase-a/a2/*` | Prompt / CDP inventories | Prompt variants and CDP access paths already listed | INFORMATIVE | Seed for A6.5 mapping |
| `docs/refactoring/evidence/phase-a/a6/*` | A6 skeleton evidence | New package is DRAFT/blocked; not legacy | INFORMATIVE | Boundary for non-legacy assets |
| `docs/AI医生/项目文档/*` | Historical design corpus | Design principles and intended behaviors | HISTORICAL | Candidate evidence, not production SoT |

Conflicts and resolutions are recorded in `a6-5-decision-log.md`.

## 4. A6.5 Objective

Produce evidence-backed retention and mapping decisions for P0/P1 legacy assets so that:

1. valuable design and engineering assets are not deleted prematurely;
2. unlicensed / unproven clinical and medical assets are not silently promoted into A6 or production;
3. Shared Contracts and Capability Package receive **mapping candidates**, not approvals;
4. A7 can start with a bounded Prompt/Model/legacy-adapter backlog.

## 5. In Scope

- Discover and inventory legacy design docs, clinical rules/terminology, prompts, model adapters, knowledge corpora, workflows/state, DTOs/APIs, engineering/deploy assets, and PHI-capable runtime paths.
- Define validation levels L0–L5 and planned validations.
- Propose dispositions and implementation batches.
- Plan future A6.5 deliverables named by normative docs:
  - `legacy-design-asset-inventory.csv` (seeded here as `a6-5-legacy-asset-inventory.csv`)
  - `legacy-design-code-evidence.csv`
  - `legacy-contract-extraction.md`
  - `legacy-clinical-policy-extraction.md`
  - `legacy-eval-asset-inventory.csv`
  - `legacy-observability-migration.md`
  - `legacy-asset-decommission-register.csv`

## 6. Out of Scope

- Implementing migrations, adapters, or service changes
- Approving clinical rules, thresholds, hypotheses, or medical sources
- Changing A6 manifest lifecycle / runtime / production fields
- Runtime model loading, provider calls, weight downloads
- Patient API/UI work
- Database migrations or live PHI content inspection without authorization
- A7 Model Runtime implementation
- Frozen Baseline promotion
- Deleting, moving, or renaming legacy assets

## 7. Repository Findings

Scanned roots (excluding `.git`, venvs, `node_modules`, `target`, `dist`, `__pycache__`, IDE caches):

- Microservices: `diagnosis-service`, `clinical-parsing-service`, `health-state-assessment-service`, `risk-assessment-service`, `dialog-service`, `explanation-service`, `diagnosis-engine-service`, `knowledge-management-service`, `examination-service`, `ocr-service`, `execution-trace-service`, `workup-planner-service`, `treatment-engine-service`
- Shared: `common/`, `frontend/`, `frontend-admin/`, `monitoring/`, `scripts/`, `docker-compose.yml`
- Research: `DRKnows-main/`
- Design/knowledge: `docs/AI医生/项目文档/` (49), `docs/AI医生/知识内容提取/`
- Non-legacy targets: `contracts/v1`, `capabilities/adult_respiratory_v1`

Key findings:

1. **49** historical design specs exist and remain the primary design evidence corpus.
2. Clinical logic is widely embedded in Python rules, YAML configs, prompts, and engines without clinical approval evidence.
3. Medical source/derived corpora exist with incomplete license/provenance; A4 structurally verified only a subset.
4. Designed eval suites (static / interactive / trajectory) are referenced in matrices but **concrete datasets were not found**.
5. Neo4j/DR.KNOWS/path-injection remain BLOCKED/EVALUATE candidates.
6. Runtime PHI-capable paths exist even though tracked static files showed no patient rows.
7. A6/A5 artifacts are present and must not be treated as legacy inheritance inputs.

## 8. Legacy Asset Taxonomy

| Category | Examples | Typical disposition |
| --- | --- | --- |
| DESIGN_DOCUMENT | `docs/AI医生/项目文档` | MAP / KEEP_READ_ONLY |
| CLINICAL_KNOWLEDGE | red flags, triage, vocabularies, KG code | QUARANTINE / NEEDS_DECISION / MAP |
| PROMPT | jinja2/py prompt managers | NEEDS_DECISION → A7 mapping |
| MODEL_INFERENCE | LLM client, NER, OCR, compose env | MAP to A7 |
| DATA_EVALUATION | sources, derived corpora, missing eval suites, PHI paths | QUARANTINE / NEEDS_DECISION |
| WORKFLOW_STATE | AgentLoop, CDP, AgentState, AuditTrail, Trace | ADAPT / MAP |
| API_CONTRACT | Java DTO, Pydantic, frontend types | MAP to `contracts/v1` |
| ENGINEERING | compose, monitoring, scripts, loggers | ADAPT |

## 9. Inventory Summary

Source: `a6-5-legacy-asset-inventory.csv`

| Metric | Count |
| --- | --- |
| Assets inventoried | 203 |
| Design documents | 50 |
| Clinical knowledge | 27 |
| Prompt | 15 |
| Model/inference | 9 |
| Data/evaluation (incl. sources, scripts, PHI paths, research) | 68 |
| Workflow/state | 17 |
| API/contract | 10 |
| Engineering | 7 |
| P0 / P1 / P2 / P3 | 77 / 72 / 52 / 2 |
| `potential_patient_data=YES` | 18 |
| Boundary / non-legacy markers | 2 (`BOUND-001`, `BOUND-002`) |
| Inventory fields added by independent review | `path_status`, `risk_basis`, `phi_path_class` |

### Patient-data semantics (mandatory)

```text
Tracked real patient records found: 0
PHI-capable or potentially patient-bearing paths: 18
Content inspection performed: no
Privacy authorization required for live-store inspection: yes
```

`PHI_CAPABLE_PATH` / `potential_patient_data=YES` means capability or path risk only. It does **not** mean confirmed real patient data was found.

### P0 / P1 definitions (after calibration)

| Level | Meaning |
| --- | --- |
| P0 | Explicit stop/safety blocker with `risk_basis` in {PHI_CAPABLE_PATH, CLINICAL_LOGIC_ACTIVE_PATH, LICENSE_PROVENANCE_BLOCKER, RUNTIME_COUPLING_BLOCKER, SECURITY_PRIVACY_BLOCKER} |
| P1 | Required for correct mapping/governance but not an immediate safety stop by itself |
| P2 | Legacy debt / informative conflict |
| P3 | Boundary or non-blocking markers |

`UNASSIGNED` owner alone does not create P0. Pure historical design documents are not automatic P0.

Every row has `asset_id`, proposed disposition, risk level, validation level, and batch. Directory cluster rows are limited; critical clinical/prompt/state/source files are file-level.

## 10. Validation Levels

| Level | Name | Proves | Does not prove |
| --- | --- | --- | --- |
| L0 | Discovery | exists, typed, initially referenced | correctness |
| L1 | Structural | parseable, encoding, refs | business/clinical truth |
| L2 | Provenance & governance | owner/license/privacy/clinical status fields | clinical validity |
| L3 | Contract compatibility | mappable to A5/A6 schemas with gaps listed | integration/runtime readiness |
| L4 | Behavioral regression (synthetic) | repeatable non-clinical behavior fixtures | clinical gold standard |
| L5 | Integration readiness | target/transform/deps/rollback/evidence clear | actually integrated or production-ready |

## 11. Validation Methods

Used in `a6-5-validation-matrix.csv`:

`STATIC_SCAN`, `FORMAT_PARSE`, `SCHEMA_VALIDATE` (planned in implementation), `REFERENCE_CHECK`, `DEPENDENCY_TRACE`, `DUPLICATION_CHECK`, `PROVENANCE_REVIEW`, `LICENSE_REVIEW`, `PRIVACY_REVIEW`, `CONTRACT_MAPPING`, `SYNTHETIC_REGRESSION`, `MANUAL_ARCHITECTURE_REVIEW`, `MANUAL_CLINICAL_REVIEW`.

Matrix status values in this planning PR are only `PLANNED` / `BLOCKED` / `NEEDS_DECISION` / `NOT_APPLICABLE` (currently `PLANNED`).

## 12. Disposition Model

Planning dispositions:

`ADOPT`, `ADAPT`, `MAP`, `WRAP`, `KEEP_READ_ONLY`, `DEPRECATE`, `ARCHIVE`, `REJECT`, `QUARANTINE`, `NEEDS_DECISION`

Roadmap mapping (implementation):

| Planning | Roadmap-oriented meaning |
| --- | --- |
| ADOPT / ADAPT / MAP / WRAP | KEEP / ADAPT / EXTRACT / SPLIT / WRAP |
| NEEDS_DECISION | EVALUATE / DEFER pending evidence |
| KEEP_READ_ONLY | KEEP without mutation |
| DEPRECATE / ARCHIVE / REJECT / QUARANTINE | ARCHIVE / REMOVE candidates with gates |

Judgement outputs per asset (implementation):

`VALIDATED_FOR_DISCOVERY`, `VALIDATED_STRUCTURALLY`, `VALIDATED_FOR_MAPPING`, `READY_FOR_FUTURE_ADAPTATION`, `KEEP_READ_ONLY`, `QUARANTINED`, `REJECTED`, `BLOCKED_PENDING_REVIEW`, `INSUFFICIENT_EVIDENCE`, `OUT_OF_SCOPE`

## 13. Safety and Privacy Boundaries

```text
Tracked real patient records found: 0
PHI-capable or potentially patient-bearing paths: 18
Content inspection performed: no
Privacy authorization required for live-store inspection: yes
```

- No real patient content is copied into planning docs.
- PHI-capable runtime paths are quarantined at path level.
- Secret/credential values are not reproduced.
- Local structural checks ≠ CI PASS.
- No external medical download, model weight fetch, or paid API use in A6.5 planning or default implementation batches.

## 14. Clinical Boundary

A6.5 may extract **candidates** for clinical review. It must not:

- set any A6 safety/hypothesis/knowledge child to approved;
- increase approved rule/threshold/hypothesis/source counts above 0;
- treat legacy engine output as clinical truth labels.

## 15. Runtime and A7 Boundary

| Content | A6.5 | A7 | Clinical review | Production |
| --- | ---: | ---: | ---: | ---: |
| Asset discovery | yes | no | no | no |
| Format/schema validation | yes | no | no | no |
| Source/license checks | yes | no | maybe | no |
| Capability/contract mapping | yes | no | no | no |
| Runtime registry / model load / inference | no | yes | no | no |
| Clinical/medical-source approval | no | no | yes(/legal) | no |
| Patient API/UI | no | later | no | later |
| Production enablement | no | no | no | yes |

A6.5 **cannot** change:

- `runtime_adoption` away from `NOT_IMPLEMENTED`
- `production_eligibility` away from `BLOCKED`
- Capability `lifecycle` away from `DRAFT`

## 16. Implementation Batches

### A6.5-A — Discovery and Quarantine

- **Goal:** path discovery, classification correction, P0/P1 calibration, PHI-capable path marking, missing Owner/License/Provenance flags, quarantine suggestions, stop-condition checks.
- **Inputs:** this inventory; A2/A4 evidence.
- **Included:** asset path discovery; category/`path_status`/`risk_basis` corrections; PHI-capable path marking; high-risk quarantine suggestions; clinical-policy **candidate path location** only (TASK-A03).
- **Excluded:** content-level clinical rule extraction; medical rule approval; data-file content inspection; database access; contract mapping; runtime mapping; synthetic regression; migration/rewrite; asset deletion.
- **Dependencies:** plan PR approval + OD-007 ack recommended.
- **Methods:** static scan, reference grep, privacy checklist (no content dump; no cat/head/tail of data files).
- **Deliverables:** updated inventory; quarantine list; clinical-policy-candidate-path-register.md.
- **Acceptance:** every P0/P1 has path_status; PHI/medical sources quarantined or exception-owned; no clinical rule bodies extracted.
- **Failure/stop:** confirmed patient-data sample; unauthorized live-store access; content-level extraction attempted in A.
- **Rollback/containment:** no asset deletion; quarantine flags only.
- **Reviewers:** architecture, security.
- **Exit:** `A6_5_A_COMPLETE` or blocked status.

Content-level `legacy-clinical-policy-extraction.md` is **TASK-B04** in A6.5-B and remains `BLOCKED` until a clinical owner role is available.

### A6.5-B — Structural and Provenance Validation

- **Goal:** L1 parse + L2 governance fields.
- **Dependencies:** A6.5-A.
- **Deliverables:** code-evidence CSV partial; governance checklist.
- **Stop:** external paid API/model required; production use of unlicensed corpus discovered.

### A6.5-C — Capability and Contract Mapping

- **Goal:** map Tool/CDP/AgentState/AuditTrail/prompts/rules to A5/A6 schemas; observability migration plan.
- **Excluded:** runtime wiring, State Committer implementation, A7 registries.
- **Stop:** any APPROVED/ACTIVE/ELIGIBLE mutation proposal without clinical gate.

### A6.5-D — Synthetic Regression Planning

- **Goal:** locate or declare absent the three eval suites; plan synthetic non-clinical fixtures.
- **Forbidden:** real patient fixtures; legacy clinical gold labels.
- **Note:** absorbs Coverage Matrix “A6.5-F eval” planning obligations.

### A6.5-E — Disposition Review

- **Goal:** board decisions; decommission register; matrix write-back plan.
- **Note:** absorbs Coverage Matrix “A6.5-F decommission register”.
- **Exit Gate alignment:** P0/P1 unique records; KEEP/ADAPT have test plans; EVALUATE has experiment plan; ARCHIVE/REMOVE have dependency/rollback checks; no unverified deletion.

## 17. Dependency Graph

```text
Plan approval
→ A6.5-A Discovery/Quarantine
→ A6.5-B Structural/Provenance
→ A6.5-C Contract/Capability Mapping
→ A6.5-D Synthetic Regression Planning
→ A6.5-E Disposition Review
→ (future) A7 Model Runtime skeleton
```

Blocked until humans decide: OD-001..OD-007 in decision log (license, DRKnows, path-injection, owners, eval suites, live PHI inspection, batch naming confirmation).

## 18. Acceptance Criteria (plan completeness)

- [x] A6.5 authoritative definition cited
- [x] Major asset classes scanned
- [x] Unique `asset_id` per inventory row
- [x] Disposition + risk per asset
- [x] Potential patient-data paths marked
- [x] No auto clinical/source approval
- [x] Validation levels + matrix defined
- [x] Batches + backlog defined
- [x] A6.5 vs A7 boundary explicit
- [x] Open decisions listed
- [x] No runtime code / A6 status mutation in this PR

## 19. Stop Conditions

Stop implementation and escalate if:

- real or highly suspected patient data samples must be handled;
- unlicensed medical data is found in active production use;
- another A6.5 implementation branch/PR appears;
- Enterprise Head introduces conflicting A6.5/A7 scope changes;
- authority for A6.5 becomes unresolved;
- worktree/branch safety checks fail.

## 20. Evidence Model

Every future validation conclusion must bind:

- command or review checklist;
- output summary;
- asset path;
- schema/test identifiers where applicable;
- human reviewer role;
- missing evidence list.

Planning artifacts themselves are evidence of scope, not of validation PASS.

## 21. Required Human Reviews

| Review | When | Assets |
| --- | --- | --- |
| Architecture | plan approval; A/C/E | workflow, contracts, batches |
| Clinical | A/B/C/E | rules, prompts with clinical logic |
| Security/Privacy | A/B/E | PHI paths, logs/trace, compose secrets |
| Legal/License | A/B/E | medical corpora, DRKnows |
| Evaluation | D/E | eval suites / synthetic fixtures |

## 22. Risks

See `a6-5-risk-register.csv` (8 theme rows; related_asset_ids bind calibrated P0 groups). Highest themes:

- PHI-capable runtime stores
- unapproved clinical rules/prompts
- unlicensed medical sources
- path-injection / KG coupling
- CDP multi-writer state risk
- missing eval datasets
- A6.5/A7/A6 scope confusion

## 23. Open Decisions

See `a6-5-decision-log.md` §3 (OD-001..OD-007).

## 24. Suggested Branch and PR Strategy

| Item | Value |
| --- | --- |
| Planning branch | `agent/phase-a6-5-legacy-asset-validation-plan` |
| Planning PR | Draft against Enterprise plan branch |
| Implementation | separate branches per batch after plan approval, e.g. `agent/phase-a6-5-a-discovery-quarantine` |
| Evidence path | `docs/refactoring/evidence/phase-a/a6-5/` (created in implementation, not this PR) |
| Merge rule | no batch merge without Expected Head SHA and exit checklist |

## 25. Definition of Done (this planning PR)

- Planning documents committed and Draft PR opened
- Inventory/matrix/risk/backlog internally consistent
- Enterprise/A6 safety boundaries unchanged
- Recommendation set to `READY_FOR_A6_5_PLAN_REVIEW` (planning PR #10); independent remediation aims `READY_FOR_A6_5_PLAN_REMEDIATION_MERGE`
- No A6.5-A execution started

## 26. Explicit Non-Claims

This plan does **not** claim:

- legacy assets are validated;
- legacy assets are approved;
- clinical correctness;
- medical-source approval;
- Runtime readiness;
- production readiness;
- A6.5 implementation completion;
- A7 completion;
- Frozen Baseline completion;
- CI PASS.

## 27. Recommendation

`READY_FOR_A6_5_PLAN_REVIEW` (planning PR #10); independent remediation aims `READY_FOR_A6_5_PLAN_REMEDIATION_MERGE`

Human reviewers should approve or amend asset scope, risk grades, batch folding of Coverage Matrix A6.5-F into D/E, and open decisions before any implementation branch is created.
