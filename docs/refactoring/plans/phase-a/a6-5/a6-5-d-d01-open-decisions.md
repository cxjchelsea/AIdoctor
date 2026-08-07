# A6.5-D TASK-D01 Open Decisions

Default posture: `UNRESOLVED / FAIL_CLOSED` unless noted `ACKNOWLEDGED`.

Findings or planning notes ≠ Decision closure.

---

## A65D-OD-001 A6.5-F Coverage Matrix authoritative reference

| Field | Value |
|---|---|
| decision_id | A65D-OD-001 |
| status | ACKNOWLEDGED |
| question | What is the authoritative Coverage Matrix reference for A6.5-F eval obligations? |
| current_evidence | `docs/refactoring/目标能力与旧设计资产覆盖矩阵.md` (Draft v2.6 Coverage Matrix) contains DESIGNED rows for 静态病例集 / 交互问诊集 / 轨迹回放集 tagged A6.5-F; `a6-5-decision-log.md` OD-007 retains A6.5-F deliverables via D/E mapping |
| fail_closed_rule | D01 must not claim A6.5-F completion or matrix write-back |
| notes | Inventory will be capable of future write-back; write-back itself is not authorized here |

---

## A65D-OD-002 DATA-EV001..003 synthetic provenance/content-read boundary

| Field | Value |
|---|---|
| decision_id | A65D-OD-002 |
| status | UNRESOLVED / FAIL_CLOSED |
| question | When may DATA-EV suite contents be read for D01? |
| current_evidence | Inventory marks concrete suites `MISSING_EXPECTED_ASSET`; documentation path exists; synthetic/PHI/gold status of any future located datasets unknown |
| fail_closed_rule | `CONTENT_READ_BLOCKED_UNTIL_SYNTHETIC_PROVEN`; forbid real/deidentified patient fixtures |
| notes | Planning continues with gap rows; Implementation may invent new synthetic fixtures without reading forbidden content |

---

## A65D-OD-003 PROMPT-001 evaluation without prompt-body access

| Field | Value |
|---|---|
| decision_id | A65D-OD-003 |
| status | ACKNOWLEDGED |
| question | Can D01 plan PROMPT-001 evaluation without reading prompt body? |
| current_evidence | Target is `common/aidoctor_llm/prompt_manager.py`; clinical prompt extraction blocked by B04/C02 posture |
| fail_closed_rule | Path/type/symbol/hash/consumer relationship only; `PROMPT_BODY_NOT_AUTHORIZED`; clinical semantics → `NO_SAFE_ORACLE` / `GAP_BLOCKED_CLINICAL_GOLD` |
| notes | Target inclusion ≠ body-read authorization |

---

## A65D-OD-004 STATIC vs INTERACTIVE vs TRAJECTORY suite ownership

| Field | Value |
|---|---|
| decision_id | A65D-OD-004 |
| status | ACKNOWLEDGED |
| question | How are suite classes owned for D01? |
| current_evidence | Backlog requires covering static/interactive/trajectory suite gaps; Coverage Matrix maps three eval suite types |
| fail_closed_rule | Every target/suite pair explicitly classified; N/A allowed with rationale; static pass ≠ trajectory verified |
| notes | Ownership remains evaluation; harness ownership may later split engineering |

---

## A65D-OD-005 Safe oracle boundary for behavior-only evaluation

| Field | Value |
|---|---|
| decision_id | A65D-OD-005 |
| status | ACKNOWLEDGED |
| question | Which oracles are safe for non-clinical behavior evaluation? |
| current_evidence | D01 acceptance: synthetic-only; forbid clinical gold |
| fail_closed_rule | Only SCHEMA/EXACT_VALUE/STATE_TRANSITION/INVARIANT/EVENT_SEQUENCE/ERROR_CLASS/IDENTIFIER_PROPAGATION/TRACE_LINKAGE/SNAPSHOT_STRUCTURE/MANUAL_NON_CLINICAL_REVIEW/NO_SAFE_ORACLE |
| notes | Clinical diagnosis/outcome oracles forbidden |

---

## A65D-OD-006 Local harness vs runtime dependency boundary

| Field | Value |
|---|---|
| decision_id | A65D-OD-006 |
| status | UNRESOLVED / FAIL_CLOSED |
| question | Which behaviors require only local harness vs true runtime? |
| current_evidence | WF-001/WF-002 are runtime-coupled assets; D01 default forbids prod/staging/DB/Redis/Neo4j/trace backends |
| fail_closed_rule | Prefer local unit / mocked / in-memory; if only runtime works → `GAP_RUNTIME_DEPENDENCY` (not authorization expansion) |
| notes | Gate-DI0 requires runtime store not required for authorized Evidence Implementation |

---

## A65D-OD-007 Existing fixture reuse vs new synthetic fixture creation

| Field | Value |
|---|---|
| decision_id | A65D-OD-007 |
| status | UNRESOLVED / FAIL_CLOSED |
| question | Reuse existing fixtures or create new synthetic ones? |
| current_evidence | DATA-EV suites missing; A6 capabilities eval cases are synthetic structural-only but not D01 inventory |
| fail_closed_rule | Reuse only if proven synthetic + non-PHI + non-clinical-gold; otherwise create new MINIMAL/BOUNDARY/STATE_SEQUENCE synthetics |
| notes | Capabilities eval fixtures are REFERENCE_ONLY candidates, not auto-imported |

---

## A65D-OD-008 Trace/observability scenarios reuse from C03

| Field | Value |
|---|---|
| decision_id | A65D-OD-008 |
| status | ACKNOWLEDGED |
| question | How may D01 reuse C03 Evidence? |
| current_evidence | TASK-C03 MERGED_AND_VERIFIED with PLANNED synthetic observability test matrix |
| fail_closed_rule | REFERENCE_ONLY; must not claim C03 scenarios Runtime-executed; must not modify C03 Evidence; must not deploy OTel |
| notes | Useful for TRACE_LINKAGE / failure-isolation planning inputs |

---

## A65D-OD-009 Determinism policy for Agent/workflow evaluation

| Field | Value |
|---|---|
| decision_id | A65D-OD-009 |
| status | ACKNOWLEDGED |
| question | What determinism classes are allowed? |
| current_evidence | AgentLoop may call models; external LLM nondeterministic |
| fail_closed_rule | Prefer DETERMINISTIC / SEEDED_DETERMINISTIC / MOCKED_DETERMINISTIC; `NONDETERMINISTIC_BLOCKED` if external model required |
| notes | No external LLM for D01 |

---

## A65D-OD-010 Future A6.5-F Coverage Matrix write-back semantics

| Field | Value |
|---|---|
| decision_id | A65D-OD-010 |
| status | UNRESOLVED / FAIL_CLOSED |
| question | When and how may D01 inventory write back Coverage Matrix statuses? |
| current_evidence | TASK-E02 owns matrix write-back; D01 produces inventory capable of future write-back |
| fail_closed_rule | D01 Planning/Implementation must not mark Coverage Matrix PRODUCTION_VALIDATED or claim A6.5-F done |
| notes | Write-back requires separate authorization (likely E02) |

---

## Summary

| Decision | Status | Blocking for Planning? |
|---|---|---|
| A65D-OD-001 | ACKNOWLEDGED | no |
| A65D-OD-002 | UNRESOLVED / FAIL_CLOSED | no (gaps allowed) |
| A65D-OD-003 | ACKNOWLEDGED | no |
| A65D-OD-004 | ACKNOWLEDGED | no |
| A65D-OD-005 | ACKNOWLEDGED | no |
| A65D-OD-006 | UNRESOLVED / FAIL_CLOSED | no for planning; yes for runtime expansion |
| A65D-OD-007 | UNRESOLVED / FAIL_CLOSED | no |
| A65D-OD-008 | ACKNOWLEDGED | no |
| A65D-OD-009 | ACKNOWLEDGED | no |
| A65D-OD-010 | UNRESOLVED / FAIL_CLOSED | no for planning |

Closed by this planning task: **0**
