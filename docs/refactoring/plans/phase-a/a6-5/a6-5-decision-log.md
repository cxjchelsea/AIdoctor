# Phase A6.5 Decision Log

Planning base: `agent/enterprise-agent-refactoring-plan` @ `b4a1714506ac7b05ce2d8c7b6ecae3bd7bb2e836`  
Status: planning only — no implementation decisions executed

## 1. Confirmed decisions (planning scope)

| ID | Decision | Basis | Binding |
| --- | --- | --- | --- |
| D-001 | A6.5 is **Legacy Design Asset Validation**, inserted between A6 and A7 | `可执行实施路线.md` A6.5; `原设计资产保留、改造与目标架构映射.md` §15 | NORMATIVE |
| D-002 | Primary historical design evidence root is `docs/AI医生/项目文档` (49 Markdown files), but scan is **not** limited to that directory | Roadmap requires mapping to code/config/data/tests; A2/A4 already proved broader evidence | GOVERNING |
| D-003 | A5 `contracts/v1` and A6 `capabilities/adult_respiratory_v1` are **mapping targets / boundaries**, not legacy assets to validate for retention | A5/A6 phase evidence; A6 lifecycle remains DRAFT | NORMATIVE |
| D-004 | A6.5 must not mutate A6 lifecycle, clinical approval, runtime adoption, or production eligibility | A6 package governance; user planning constraints | NORMATIVE |
| D-005 | Static repo scan found **no tracked real patient records**; runtime paths remain PHI-capable and are inventoried as `potential_patient_data=YES` without content inspection | A4 baseline + path analysis | GOVERNING |
| D-006 | Implementation batches use A–E from this plan; Coverage Matrix `A6.5-F` (eval / decommission register) is folded into **A6.5-D** (eval planning) and **A6.5-E** (disposition / decommission register) | Avoid inventing a sixth execution wave before plan approval; preserve Coverage Matrix deliverables | GOVERNING |
| D-007 | Disposition vocabulary in inventory uses planning enums (`ADOPT/ADAPT/MAP/WRAP/KEEP_READ_ONLY/DEPRECATE/ARCHIVE/REJECT/QUARANTINE/NEEDS_DECISION`) and maps to roadmap `KEEP/ADAPT/EXTRACT/SPLIT/EVALUATE/ARCHIVE/REMOVE` in implementation | Roadmap + mapping doc + migration matrix richer set | GOVERNING |
| D-008 | Evidence levels during implementation must use the full mapping-doc set and treat roadmap `DOCUMENTED` as compatible with design-document evidence | Mapping doc vs roadmap wording difference | GOVERNING |

## 2. Document conflicts

| Conflict | Sources | Resolution | Status |
| --- | --- | --- | --- |
| Evidence enum completeness | Roadmap lists 5; mapping doc adds `DESIGN_CONFIRMED` / `UNKNOWN` | Use mapping-doc full set; map roadmap terms explicitly in implementation | RESOLVED (planning) |
| Retention decision enum | Roadmap 7; matrices also use `MERGE/WRAP/REWRITE/DEFER` | Inventory keeps planning dispositions; implementation mapping table required | RESOLVED (planning) |
| Batch naming `A6.5-F` | Coverage Matrix uses A6.5-F for eval/decommission | Fold into D/E; do not drop deliverables | RESOLVED (planning) |
| README narrative lag vs phase evidence | README Freeze Candidate language vs A1–A6 evidence | Prefer phase evidence files for baseline facts | RESOLVED (planning) |
| File-name navigation drift | Some task docs reference English filenames that do not exist | Prefer current Chinese filenames in `docs/refactoring/` | RESOLVED (planning) |

## 3. Open decisions requiring humans

| ID | Question | Why blocked | Needed roles |
| --- | --- | --- | --- |
| OD-001 | Which medical source corpora are legally usable for product Knowledge Release? | License/provenance unknown for HPO/ICPC/ICD/Bates/UMLS-derived assets | Legal + Evidence |
| OD-002 | Are DR.KNOWS assets evaluable for product graph enhancement or archive-only? | Coverage Matrix BLOCKED; license/repro incomplete | Legal + Architecture + Evidence |
| OD-003 | Should path-injection LLM remain EVALUATE indefinitely or be rejected for adult respiratory v1? | High clinical coupling; currently BLOCKED in Coverage Matrix | Clinical + Architecture + Model |
| OD-004 | Owner assignment for every P0 clinical rule / prompt / state asset | Inventory currently `UNASSIGNED` | Org owners |
| OD-005 | Where are the three designed evaluation suites, or should they be declared absent and replaced by synthetic planning only? | Datasets not found in repo scan | Evaluation + Architecture |
| OD-006 | May any live/staging store be inspected for PHI inventory beyond path-level quarantine? | Privacy authorization required; content inspection forbidden in this planning task | Security/Privacy |
| OD-007 | Confirm batch folding of Coverage Matrix A6.5-F into D/E before implementation starts | Naming consistency across matrices | Architecture |

## 4. Scope trade-offs

- **Included now:** discovery inventory, validation model, risks, batches, backlog, authoritative source analysis.
- **Deferred to implementation PRs:** code evidence CSVs, contract extraction docs, clinical policy extraction body, observability migration doc, decommission register population, matrix write-back.
- **Explicitly excluded:** Runtime enablement, model download/calls, clinical approvals, Patient API/UI, DB migration, A7, Frozen Baseline, deleting/moving legacy assets.

## 5. Unresolved dependencies

- Clinical review board availability for red-flag/triage/prompt extraction review.
- Legal review queue for medical corpora and DRKnows LICENSE.
- Privacy authorization if live-store confirmation is required beyond path quarantine.
- A7 still required for PromptSpec/ModelSpec; A6.5 only maps candidates.

## 6. Decision basis for planning recommendation

Recommendation: `READY_FOR_A6_5_PLAN_REVIEW`

Because:

1. Normative A6.5 definition located and cited;
2. No pre-existing A6.5 implementation branch/PR;
3. Inventory covers design, clinical, prompt, model, data, workflow, contract, engineering, and PHI-capable runtime paths;
4. Validation levels and fail-closed non-claims are explicit;
5. No runtime/clinical/production mutation performed.
