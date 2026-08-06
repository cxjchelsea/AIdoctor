# Phase A6.5-C Open Decisions

Document status: **Draft planning**
Planning branch: `agent/phase-a6-5-c-non-clinical-mapping-plan`
Enterprise base: `356be01f1c86cccdfea87f856e152d858793e5a2`
Scope: TASK-C01 and TASK-C03 planning only

This log does not close A6.5 residual decisions OD-001..OD-006 or alter OD-007.
Those remain:

```text
OD-001..OD-006: unresolved / fail-closed
OD-007: ACKNOWLEDGED
```

---

## C-OD-001 CDP / CDPVersion / AgentState boundary

| Field | Value |
| --- | --- |
| decision_id | C-OD-001 |
| question | How should CDP, CDPVersion, and AgentState map to ContractEnvelope, IdentifierSet, StatePatch, and CommitResult? |
| options | Exact multi-contract composition; Adapter-required composition; Partial map with SOURCE_ONLY gaps; Remain legacy-only until major version |
| default_fail_closed | No runtime wiring; no Contract mutation; gaps recorded as UNKNOWN_FAIL_CLOSED or ADAPTER_REQUIRED |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | runtime-compatible; production-compatible; field authorization via StatePatch |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| platform-contracts |
| unblock_evidence | C01 field mapping + gap register covering identifier/version/state semantics |
| notes | Exact Contract Version remains 1.0.0; legacy version fields are not Contract versions |

---

## C-OD-002 AuditTrail mapping destination

| Field | Value |
| --- | --- |
| decision_id | C-OD-002 |
| question | Should AuditTrail map to AuditRef, require an adapter, or remain legacy-only? |
| options | Map to AuditRef; Adapter-required; Legacy-only with documented SOURCE_ONLY |
| default_fail_closed | Do not treat AuditTrail as TraceRef; do not invent audit authorization |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | audit authorization; audit SoT replacement; TraceRef equivalence |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| audit \| security |
| unblock_evidence | C01 AuditTrail mapping rows + Trace/Audit separation checks |
| notes | Related to C-OD-006 and C-RISK-005 |

---

## C-OD-003 ToolContext / ToolResult exactness

| Field | Value |
| --- | --- |
| decision_id | C-OD-003 |
| question | Can ToolContext and ToolResult Exact Map to contracts/v1 ToolContext and ToolResult, or is an Adapter required? |
| options | EXACT; RENAMED/TYPE_COERCION; MANUAL_ADAPTER_REQUIRED; INSUFFICIENT_EVIDENCE |
| default_fail_closed | No Exact claim without field-level Evidence; no Contract mutation to force Exact |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | schema-compatible implies runtime-compatible |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| agent-platform |
| unblock_evidence | C01 field mapping for WF-015 and WF-016 |
| notes | Support contracts ContractConflict/EvidencePack may record gaps only |

---

## C-OD-004 CTR-001 DTO tree expansion granularity

| Field | Value |
| --- | --- |
| decision_id | C-OD-004 |
| question | What expansion granularity applies to the CTR-001 DTO tree for C01 mapping? |
| options | Top-level DTO inventory only; Consumer-referenced subset; Full recursive tree; Deferred beyond C01 |
| default_fail_closed | Do not extract clinical bodies; do not expand into C02 clinical packs |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | complete DTO coverage; clinical DTO approval |
| required_owner_role | architecture |
| required_reviewer_role | architecture |
| unblock_evidence | Consumer list + selected expansion Evidence under C01 |
| notes | C02 clinical pack mapping remains blocked by TASK-B04 |

---

## C-OD-005 CTR-009 vs Java legacy state conflicts

| Field | Value |
| --- | --- |
| decision_id | C-OD-005 |
| question | How should conflicts between CTR-009 prior CDP access Evidence and current Java legacy state be resolved? |
| options | Prefer Java static source; Prefer CTR-009; Dual-record gap; Insufficient evidence stop |
| default_fail_closed | Do not silently prefer either source; record gap_type UNKNOWN_FAIL_CLOSED or IDENTIFIER_SEMANTICS_MISMATCH |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | CTR-009 alone is authoritative for runtime identifiers |
| required_owner_role | architecture |
| required_reviewer_role | architecture |
| unblock_evidence | Side-by-side consistency Evidence in C01 gap register |
| notes | CTR-009 is prior Evidence, not a mutation license |

---

## C-OD-006 TraceRef vs AuditRef boundary

| Field | Value |
| --- | --- |
| decision_id | C-OD-006 |
| question | What is the normative boundary between TraceRef and AuditRef for C01/C03? |
| options | Strict separation; Shared identifier subset with distinct semantics; Deferred architecture decision |
| default_fail_closed | TraceRef != AuditRef; propagation != authorization; collection != persist-payload permission |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | interchangeable refs; audit implied by trace |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| audit \| security |
| unblock_evidence | C01/C03 separation Evidence and validation rows |
| notes | Required before any C03 implementation authorization (Gate-CI1) |

---

## C-OD-007 Feign Trace Header names and conflict strategy

| Field | Value |
| --- | --- |
| decision_id | C-OD-007 |
| question | What are the normative Feign Trace Header names and conflict/overwrite policies? |
| options | Adopt current static headers after Evidence; Introduce adapter headers; Dual-write headers with coexistence rules |
| default_fail_closed | Do not assume header names; extract from static code; malformed/duplicate handling required |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | production header standard; OTel baggage equivalence |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| security |
| unblock_evidence | Static header extraction + propagation test matrix results |
| notes | Static code currently shows candidate headers such as `X-Trace-Id` and `X-CDP-Id`; these are observation candidates only until C03 Evidence |

---

## C-OD-008 Trace Failure Isolation target behavior

| Field | Value |
| --- | --- |
| decision_id | C-OD-008 |
| question | What is the target failure isolation behavior when trace/AOP/decorator/interceptor paths fail? |
| options | FAIL_ISOLATED; FAIL_OPEN_OBSERVABILITY_ONLY; FAIL_CLOSED_WORKFLOW for selected critical paths; UNKNOWN_REQUIRES_TEST |
| default_fail_closed | No production cutover; no claim that current behavior is approved without synthetic Evidence |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | patient-safety fail-open; production-ready observability |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| security |
| unblock_evidence | C03 failure isolation test Evidence |
| notes | `FAIL_OPEN_OBSERVABILITY_ONLY` means observability failure must not silently alter business workflow results; it is not a patient-safety Fail-Open |

---

## C-OD-009 OTel and legacy Trace path coexistence

| Field | Value |
| --- | --- |
| decision_id | C-OD-009 |
| question | How should OTel candidate mapping coexist with the legacy Trace path? |
| options | Shadow-only new path; Dual-write with explicit authorization; Deferred OTel; Legacy-only until fallback Evidence |
| default_fail_closed | Legacy path retained; no deletion; no dual-write without explicit authorization; no production cutover |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | OTEL_MIGRATED; PRODUCTION_READY |
| required_owner_role | architecture |
| required_reviewer_role | architecture \| security |
| unblock_evidence | Coexistence/rollback Evidence under C03 |
| notes | OTel mapping in this plan is planning-only; no collector/exporter/install |

---

## C-OD-010 PHI Redaction, Retention, and Sampling owners

| Field | Value |
| --- | --- |
| decision_id | C-OD-010 |
| question | Who owns PHI redaction allowlists, retention, and sampling for Trace/Observability mapping? |
| options | security+privacy joint; privacy primary; architecture interim with privacy gate |
| default_fail_closed | Payload-capable capture remains NEEDS_PRIVACY_REVIEW; synthetic-only fixtures |
| current_status | UNRESOLVED / FAIL_CLOSED |
| blocked_claims | PHI_APPROVED; production retention approved |
| required_owner_role | privacy |
| required_reviewer_role | privacy \| security |
| unblock_evidence | Named owner assignment + redaction matrix Evidence |
| notes | Related to C-RISK-008; does not authorize patient-data access |

---

## C-OD-011 TASK-C02 without TASK-B04

| Field | Value |
| --- | --- |
| decision_id | C-OD-011 |
| question | Can TASK-C02 be split into pure Schema Planning without TASK-B04? |
| options | Yes split; No keep blocked; Deferred |
| decision_this_phase | NO |
| current_status | ACKNOWLEDGED_BLOCKED |
| binding_result | TASK-C02 = BLOCKED_BY_TASK_B04; TASK-B04 = BLOCKED; HUMAN_SUPERVISED_CLINICAL_READ = inactive |
| default_fail_closed | No C02 planning or implementation under A6.5-C non-clinical scope |
| required_owner_role | architecture |
| required_reviewer_role | clinical \| architecture \| security |
| unblock_evidence | Separate B04 authorization and C02 authorization |
| notes | This planning PR must include zero C02/B04 targets |

---

## Residual A6.5 decisions (not closed here)

```text
F-A65A-R09: OPEN / P2 / NON_BLOCKING
RISK-001..005: OPEN
RISK-008: OPEN
OD-001..OD-006: unresolved / fail-closed
OD-007: ACKNOWLEDGED
```
