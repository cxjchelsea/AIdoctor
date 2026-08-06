# Phase A6.5-C Non-Clinical Contract and Observability Mapping Plan

Document status: **Draft planning**
Planning branch: `agent/phase-a6-5-c-non-clinical-mapping-plan`
Enterprise base: `356be01f1c86cccdfea87f856e152d858793e5a2`
Authorized tasks: `TASK-C01`, `TASK-C03` — **planning only**
Blocked: `TASK-C02` (`BLOCKED_BY_TASK_B04`), `TASK-B04` (`BLOCKED`)

## 1. Executive Summary

This plan defines how future Phase A6.5-C non-clinical work will:

- map Tool/CDP/AgentState/AuditTrail legacy structures to Shared Contracts `contracts/v1` at field level (TASK-C01);
- map AOP/Trace/Feign/Python decorator observability paths with failure isolation, PHI boundaries, and OTel candidate semantics (TASK-C03).

It does **not** authorize implementation, Contract mutation, Runtime wiring, OTel deployment, clinical extraction, or TASK-C02/B04.

## 2. Exact Enterprise Base

```text
Repository: cxjchelsea/AIdoctor
Enterprise branch: agent/enterprise-agent-refactoring-plan
Enterprise Head: 356be01f1c86cccdfea87f856e152d858793e5a2
```

If Enterprise moves, stop with `ENTERPRISE_TARGET_MOVED` and reassess. Do not rebase this plan automatically.

## 3. Prior Phase Evidence

```text
A6.5-A: MERGED_AND_VERIFIED
A6.5-B planning: MERGED_AND_VERIFIED
Gate-B0 / TASK-B01 / TASK-B02 / TASK-B03 / B-Core: MERGED_AND_VERIFIED
Independent Review PR #18 merge: 40ad473f3f218830f8592710ac79b8912991b140
Enterprise merge PR #17: 356be01f1c86cccdfea87f856e152d858793e5a2
```

Still retained open residuals:

```text
F-A65A-R09: OPEN / P2 / NON_BLOCKING
RISK-001..005: OPEN
RISK-008: OPEN
OD-001..OD-006: unresolved / fail-closed
OD-007: ACKNOWLEDGED
```

Historical backlog rows may still say `PLANNED` for C tasks. Those are historical snapshots; this plan is authoritative for A6.5-C non-clinical planning status.

## 4. Authorization Scope

```text
TASK-C01: AUTHORIZED_FOR_PLANNING_ONLY
TASK-C03: AUTHORIZED_FOR_PLANNING_ONLY
```

Allowed now:

- read Inventory/Backlog/Risk/Decision/Evidence;
- read static Java/Python/SQL/CSV/JSON Schema;
- read `contracts/v1/**` as mapping destination;
- define future Evidence schemas, enums, validation methods, and stop conditions;
- create this planning package and a Draft Planning PR.

## 5. Explicit Non-Authorization

```text
TASK-C01 implementation: NOT_AUTHORIZED
TASK-C03 implementation: NOT_AUTHORIZED
TASK-C02: BLOCKED_BY_TASK_B04
TASK-B04: BLOCKED
HUMAN_SUPERVISED_CLINICAL_READ: inactive
A7: NOT_STARTED
Runtime: NOT_IMPLEMENTED
Production: BLOCKED
```

Forbidden now:

- field-level formal Mapping Evidence files;
- modifying `contracts/v1/**`, Capability, Runtime, app source, AOP/Trace/Feign sources;
- adapters, runtime wiring, OTel install/collector/exporter;
- DB/Redis/Neo4j/trace-store/log/patient content access;
- clinical policy extraction or C02/B04 work.

## 6. Current Phase Status

```text
Lifecycle: DRAFT
Clinical review: REQUIRES_CLINICAL_REVIEW
Approved clinical rules/thresholds/hypotheses/sources: 0/0/0/0
A6.5-B Overall: CORE_COMPLETE_B04_BLOCKED
A6.5-C: planning Draft in progress
CI: NO_CI_CONFIGURED
```

## 7. Gate-C0

### 7.1 Status reconciliation

```text
Enterprise Base: 356be01f1c86cccdfea87f856e152d858793e5a2
A6.5-B Core: MERGED_AND_VERIFIED
TASK-B01: MERGED_AND_VERIFIED
TASK-B04: BLOCKED
TASK-C01: AUTHORIZED_FOR_PLANNING_ONLY
TASK-C03: AUTHORIZED_FOR_PLANNING_ONLY
TASK-C02: BLOCKED_BY_TASK_B04
```

### 7.2 Gate-C0 acceptance

```text
Enterprise exact base: pass
B01 merged and verified: pass
B04 blocked: pass
C02 excluded: pass
C01 target count: 9
C03 target count: 5
Total task-target rows: 14
Unique target assets: 14
Runtime access required: no
Patient-data access required: no
Contract mutation required: no
```

Failure status: `GATE_C0_FAILED`.

## 8. Dependency Graph

```text
TASK-B01
   ↓
TASK-C01
   ↓
TASK-C03

TASK-B01 + TASK-B04
          ↓
       TASK-C02
```

Current eligibility:

```text
C01 planning: ELIGIBLE
C03 planning: ELIGIBLE_AS_DEPENDENT_PLAN
C02 planning/implementation: BLOCKED
B04: BLOCKED
```

Future implementation gates:

```text
Gate-CI0: C01 Implementation Authorization (separate)
  ↓ C01 impl + independent review + merge
Gate-CI1: C03 Implementation Authorization (separate)
```

One vague authorization must not unlock C01 and C03 together without preserving the C01 → C03 execution dependency.

## 9. TASK-C01 Scope

```text
Map Tool/CDP/AgentState/AuditTrail to Shared Contracts
```

Future implementation goal (not this PR):

- field-level mapping to `contracts/v1`;
- record gaps;
- no runtime wiring;
- no Contract mutation.

## 10. C01 Target Assets

| Asset ID | Path | Planning Role |
| --- | --- | --- |
| WF-004 | `diagnosis-service/.../entity/CDP.java` | Legacy state/entity source |
| WF-005 | `diagnosis-service/.../entity/CDPVersion.java` | Legacy version/state source |
| WF-006 | `diagnosis-service/.../entity/AgentState.java` | Legacy agent state source |
| WF-007 | `diagnosis-service/.../entity/AuditTrail.java` | Legacy audit source |
| WF-015 | `diagnosis-service/.../dto/tool/ToolContext.java` | Legacy tool input DTO |
| WF-016 | `diagnosis-service/.../dto/tool/ToolResult.java` | Legacy tool result DTO |
| CTR-001 | `diagnosis-service/.../dto` | DTO tree and consumer scope |
| CTR-009 | `docs/refactoring/evidence/phase-a/a2/python-cdp-access-inventory.csv` | Prior CDP access Evidence |
| BOUND-001 | `contracts/v1` | Read-only mapping destination |

Cardinality: 9 rows / 9 unique asset IDs / 0 missing Inventory IDs.

## 11. Shared Contract Manifest Boundary

Source of truth: `contracts/v1/manifest.json` at Enterprise Head.

```text
contract_version: 1.0.0
version_negotiation: EXACT
supported_versions: ["1.0.0"]
```

Manifest names used by this plan exist in the active contract set. No new Contract names may be invented.

## 12. C01 Candidate Contract Mapping

Primary candidates:

```text
ContractEnvelope
IdentifierSet
StatePatch
CommitResult
ToolContext
ToolResult
AuditRef
TraceRef
```

Support-only candidates:

```text
ContractConflict
EvidencePack
```

Out of current C01 scope (mark only as `OUT_OF_CURRENT_SCOPE` if mentioned):

```text
PatientDeliveryView
KnowledgeReleaseRef
SourceArtifact
```

Hard semantic boundaries:

```text
field-level mapping != runtime service migration
schema-compatible candidate != runtime-compatible implementation
StatePatch validation != field authorization
mapping destination != permission to modify contracts/v1
static Java entity field != patient record
```

Future C01 implementation default:

```text
contracts/v1/**: READ_ONLY
```

On Contract Gap, future implementation may only record:

```text
CONTRACT_EXTENSION_CANDIDATE
MAJOR_VERSION_REQUIRED
ADAPTER_REQUIRED
INSUFFICIENT_EVIDENCE
```

## 13. C01 Future Evidence Schema

### 13.1 Contract Field Mapping

Future file (must not be created in this planning PR):

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-c-contract-field-mapping.csv
```

Fields:

```text
mapping_id, execution_id, task_id, source_asset_id, source_path,
source_symbol, source_field_path, source_type, source_requiredness,
source_nullability, source_enum_or_domain, target_contract_name,
target_schema_path, target_json_pointer, target_type,
target_requiredness, target_nullability, mapping_class,
transform_class, compatibility_class, phi_risk,
runtime_wiring_required, contract_mutation_required,
evidence_reference, reviewer_role, status, notes
```

Deterministic ID:

```text
A65C-C01-<ASSET_ID>-<NORMALIZED_SOURCE_FIELD_HASH>
```

### 13.2 Mapping Class

```text
EXACT
RENAMED
TYPE_COERCION_REQUIRED
ENUM_TRANSLATION_REQUIRED
WRAPPER_REQUIRED
SPLIT_REQUIRED
MERGE_REQUIRED
SOURCE_ONLY
TARGET_ONLY
BLOCKED_PHI_REVIEW
INSUFFICIENT_EVIDENCE
NOT_APPLICABLE
```

### 13.3 Transform Class

```text
NONE
STRING_NORMALIZATION
NUMERIC_TO_DECIMAL_STRING
TIMESTAMP_TO_RFC3339
ENUM_LOOKUP
NULLABILITY_ADAPTER
OBJECT_WRAPPER
ARRAY_NORMALIZATION
IDENTIFIER_REBINDING
MANUAL_ADAPTER_REQUIRED
UNKNOWN_FAIL_CLOSED
```

### 13.4 Compatibility Class

```text
COMPATIBLE_AS_IS
ADAPTER_REQUIRED
CONTRACT_EXTENSION_CANDIDATE
MAJOR_VERSION_REQUIRED
DOCUMENTED_ONLY
UNKNOWN_FAIL_CLOSED
```

Forbidden without independent Runtime Evidence:

```text
RUNTIME_COMPATIBLE
PRODUCTION_COMPATIBLE
CLINICALLY_APPROVED
```

## 14. C01 Gap and Conflict Model

Future file:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-c-contract-gap-register.csv
```

Fields:

```text
gap_id, execution_id, task_id, source_asset_id, source_path,
source_field_path, candidate_contract, gap_type,
compatibility_impact, runtime_impact, patient_safety_impact,
phi_risk, proposed_resolution_class, required_owner_role,
required_evidence, status, notes
```

Allowed `gap_type`:

```text
NO_TARGET_FIELD
TYPE_MISMATCH
REQUIREDNESS_MISMATCH
NULLABILITY_MISMATCH
ENUM_MISMATCH
IDENTIFIER_SEMANTICS_MISMATCH
VERSIONING_MISMATCH
AUDIT_SEMANTICS_MISMATCH
TRACE_SEMANTICS_MISMATCH
UNKNOWN_FAIL_CLOSED
```

## 15. C01 Validation Strategy

Future implementation must plan:

1. Java static field extraction;
2. Contract JSON Schema Pointer enumeration;
3. Required/Optional comparison;
4. Nullability comparison;
5. Enum/Domain comparison;
6. Numeric ID → Decimal String risk;
7. Timestamp → RFC 3339 risk;
8. Unknown Fields Fail-Closed;
9. Exact Contract Version `1.0.0`;
10. DTO tree consumer list;
11. CTR-009 vs current Java source consistency;
12. no Contract mutation;
13. no Runtime Adapter creation under unauthorized work;
14. no Runtime Store access;
15. no patient-data content access.

This planning PR does **not** execute complete field mapping.

## 16. TASK-C03 Scope

```text
AOP/Trace and Observability Migration Mapping
```

Future goal (not this PR): specialized validation plan for Trace failure isolation, PHI boundaries, Feign propagation, and OTel candidate mapping — with no production cutover.

C03 planning and future implementation depend on C01 Identifier/Envelope/AuditRef/TraceRef mapping rules.

## 17. C03 Target Assets

| Asset ID | Path | Planning Role |
| --- | --- | --- |
| WF-010 | `.../aspect/ExecutionTraceAspect.java` | Java AOP trace capture |
| WF-011 | `.../annotation/TraceExecution.java` | Trace annotation contract |
| WF-012 | `.../config/FeignTraceInterceptor.java` | Cross-service trace propagation |
| WF-013 | `execution-trace-service/.../ExecutionTrace.java` | Trace persistence entity |
| ENG-006 | `common/aidoctor_trace/trace_decorator.py` | Python trace decorator |

Cardinality: 5 rows / 5 unique asset IDs / 0 missing Inventory IDs.

PHI-capable static code read is allowed for WF-010..013 and ENG-006. That is **not** patient-data content access.

## 18. Trace vs Audit Boundary

Primary candidates:

```text
TraceRef
AuditRef
ContractEnvelope
IdentifierSet
```

Support:

```text
ContractConflict
EvidencePack
```

Normative planning statements:

```text
TraceRef: execution/correlation reference
AuditRef: governance/audit reference
trace propagation != audit authorization
trace collection != permission to persist payload
OTel mapping plan != OTel production deployment
trace availability != business workflow dependency
```

## 19. Failure Isolation Planning

Future Evidence must classify:

```text
FAIL_ISOLATED
FAIL_OPEN_OBSERVABILITY_ONLY
FAIL_CLOSED_WORKFLOW
UNKNOWN_REQUIRES_TEST
```

`FAIL_OPEN_OBSERVABILITY_ONLY` means observability failure must not silently alter clinical/business workflow results. It does **not** authorize patient-safety Fail-Open.

Planned failure scenarios include exporter/service unavailability, decorator/AOP/interceptor exceptions, serialization failures, and malformed trace context.

## 20. Feign Propagation Planning

Planned scenarios:

```text
trace ID propagation
correlation ID propagation
parent context propagation
duplicate header handling
caller-provided header handling
malformed header handling
retry propagation
```

Header names must be extracted and verified from static code. They must not be assumed in planning conclusions.

Static observation note (non-normative until C03 Evidence): `FeignTraceInterceptor` currently sets candidate headers including `X-Trace-Id` and `X-CDP-Id`. Final normative names remain `C-OD-007`.

## 21. PHI and Redaction Planning

Synthetic data only. Planned checks:

```text
raw request body not persisted
raw response body not persisted
prompt/provider response not persisted
patient identifier token redacted or excluded
exception message sanitization
structured metadata allowlist
```

Forbidden: real patient records as fixtures; live/staging/runtime store inspection.

Allowed `phi_capture_risk` for future rows:

```text
NONE_STATIC_ONLY
IDENTIFIER_ONLY
METADATA_CAPABLE
PAYLOAD_CAPABLE
UNKNOWN_HIGH_RISK
```

Forbidden statuses: `PHI_APPROVED`, `PRODUCTION_READY`, `OTEL_MIGRATED`.

## 22. OTel Candidate Mapping

Planning-only concepts:

```text
trace_id, span_id, parent_span_id, trace_flags,
baggage boundary, service name, operation name,
error status, attributes allowlist
```

Forbidden in this phase and unauthorized future work without a separate gate:

- install OTel;
- modify deployment;
- start Collector;
- configure Exporter;
- production cutover.

## 23. Legacy Coexistence and Rollback

Must plan:

```text
legacy trace path retained
new path shadow-only by default
no dual-write without explicit authorization
no old-path deletion
rollback entry
observability-disabled mode
```

## 24. Future Evidence Artifacts

C01 (not created now):

```text
a6-5-c-contract-field-mapping.csv
a6-5-c-contract-gap-register.csv
a6-5-c-contract-validation-evidence.csv
legacy-contract-extraction.md
```

C03 (not created now):

```text
a6-5-c-observability-mapping.csv
a6-5-c-observability-test-matrix.csv
a6-5-c-observability-validation-evidence.csv
legacy-observability-migration.md
```

Report (not created now):

```text
a6-5-c-non-clinical-mapping-report.md
```

Creating any of the above in this planning task is `A6_5_C_IMPLEMENTATION_STARTED_WITHOUT_AUTHORIZATION`.

## 25. Future Validation Artifacts

See `a6-5-c-plan-validation-matrix.csv` for Gate-C0, C01 Plan, C03 Plan, and Global checks.

Future observability mapping ID pattern:

```text
A65C-C03-<ASSET_ID>
```

Future observability mapping fields and enums are defined in this plan package and must be used unchanged unless a later planning revision is authorized.

## 26. Access and Safety Boundary

```text
Patient-data content accessed: no
Live/staging/runtime stores accessed: no
Clinical policy content extracted: no
Runtime wiring created: no
Application source modified: no
Shared Contracts modified: no
Capability modified: no
External model/API called: no
Approved clinical rules/thresholds/hypotheses/sources: 0/0/0/0
```

PHI-capable static source read:

```text
WF-004 WF-005 WF-006 WF-007 WF-010 WF-011 WF-012 WF-013 ENG-006
static_source_read=yes
runtime_content_access=no
```

BOUND-001:

```text
planning_role=MAPPING_DESTINATION
allowed_planning_operation=READ_SCHEMA_AND_MANIFEST_ONLY
forbidden_operation=MODIFY_CONTRACT
```

## 27. Risk Register

See `a6-5-c-risk-register.csv`.

Required open risks:

```text
C-RISK-001..C-RISK-012: OPEN
```

This planning phase must not close them.

## 28. Open Decisions

See `a6-5-c-open-decisions.md`.

```text
C-OD-001..C-OD-010: UNRESOLVED / FAIL_CLOSED
C-OD-011: ACKNOWLEDGED_BLOCKED (C02 remains BLOCKED_BY_TASK_B04)
```

## 29. Independent Review Requirements

Independent planning review must verify:

- exact Enterprise base;
- 9/5/14 cardinalities;
- C02/B04 exclusion;
- Contract manifest name fidelity;
- fail-closed enums;
- Trace/Audit separation;
- no Evidence/implementation artifacts created;
- A6 + Shared Contracts regressions;
- Draft-only PR state.

## 30. Implementation Authorization Gate

```text
Gate-CI0: C01 Implementation Authorization
Gate-CI1: C03 Implementation Authorization after C01 merge + review
```

Recommended future order:

```text
C01 implementation
→ C01 independent review
→ C01 merge
→ C03 implementation
→ C03 independent review
→ C03 merge
```

## 31. Acceptance Criteria

```text
C01 task-target rows: 9
C03 task-target rows: 5
Total rows: 14
Unique planning_target_id: 14
Duplicate task/asset keys: 0
Unknown Inventory IDs: 0
C02 targets: 0
B04 targets: 0
Clinical content extraction: 0
Runtime/patient/external access: 0
Application/Contract/Capability/Runtime changes: 0
```

Plan completeness:

```text
C01 field mapping schema: defined
C01 gap schema: defined
C01 validation model: defined
C03 observability mapping schema: defined
C03 test matrix: defined
Failure isolation / Feign / PHI / OTel / coexistence: defined
```

Status after planning PR:

```text
TASK-C01: PLANNED_PENDING_INDEPENDENT_REVIEW
TASK-C03: PLANNED_PENDING_INDEPENDENT_REVIEW
TASK-C02: BLOCKED_BY_TASK_B04
TASK-B04: BLOCKED
```

## 32. Stop Conditions

```text
ENTERPRISE_TARGET_MOVED
A6_5_C_PLAN_ALREADY_STARTED
WORKTREE_NOT_SAFE
A6_5_C_PLAN_BASELINE_FAILED
CONTRACT_TEST_ENV_UNAVAILABLE
GATE_C0_FAILED
A6_5_C_PLAN_CARDINALITY_FAILED
A6_5_C_DEPENDENCY_MODEL_FAILED
A6_5_C_CONTRACT_MANIFEST_MISMATCH
A6_5_C_SCOPE_VIOLATION
C02_OR_B04_AUTHORIZATION_VIOLATION
A6_5_C_IMPLEMENTATION_STARTED_WITHOUT_AUTHORIZATION
CONTRACT_MUTATION_VIOLATION
RUNTIME_WIRING_VIOLATION
PATIENT_DATA_ACCESS_VIOLATION
CLINICAL_CONTENT_ACCESS_VIOLATION
```

## 33. Final Recommendation

```text
READY_FOR_A6_5_C_NON_CLINICAL_PLAN_INDEPENDENT_REVIEW
```

This Draft Planning PR must not be merged before independent planning review.
It does not authorize C01/C03 implementation, C02, B04, A7, Runtime, or Production.
