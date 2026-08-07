# Legacy Observability Migration Report (TASK-C03)

## 1. Execution Metadata

| Field | Value |
|---|---|
| Execution ID | `A65C-C03-20260807-1B559FE` |
| Task | `TASK-C03` |
| Title | AOP/Trace and observability migration mapping |
| Implementation class | `EVIDENCE_IMPLEMENTATION` (not `RUNTIME_CODE_IMPLEMENTATION`) |
| Executor role | architecture |
| Executed at | 2026-08-07T03:10:00Z |

## 2. Exact Enterprise Base

| Field | Value |
|---|---|
| Repository | `cxjchelsea/AIdoctor` |
| Enterprise branch | `agent/enterprise-agent-refactoring-plan` |
| Exact base SHA | `1b559fe99c1bdd8c7cbc080568392c52eec75221` |
| Base meaning | TASK-C01 Enterprise Merge + Post-Merge Verification (PR #21) |
| Implementation branch | `agent/phase-a6-5-c-c03-observability-mapping` |

## 3. Gate-CI1 Authorization

```text
Gate-CI1:
AUTHORIZED

TASK-C03 implementation:
AUTHORIZED_FOR_EVIDENCE_IMPLEMENTATION_ONLY
```

Authorized work in this PR:

- static source extraction for five targets
- observability mapping Evidence
- synthetic-only test matrix definition
- observability validation Evidence
- legacy observability migration documentation

## 4. Explicit Non-Authorization

This Evidence Implementation does **not** authorize:

- Runtime source modification (AOP / annotation / Feign / entity / Python decorator)
- Shared Contract mutation or version change
- Adapter implementation or Runtime wiring
- OTel SDK install, Collector, Exporter, Agent, dual-write, or production cutover
- Legacy trace path deletion
- Patient / live / staging / Trace Store / DB / Redis / Neo4j / APM access
- TASK-C02, TASK-B04, HUMAN_SUPERVISED_CLINICAL_READ, A7, Production

```text
OTel candidate mapping
!=
OTel implementation

Migration mapping
!=
Migration execution
```

## 5. Authorized C03 Target Inventory

| Asset | Role | Path |
|---|---|---|
| WF-010 | JAVA_AOP_TRACE_CAPTURE | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/aspect/ExecutionTraceAspect.java` |
| WF-011 | TRACE_ANNOTATION_CONTRACT | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/annotation/TraceExecution.java` |
| WF-012 | CROSS_SERVICE_TRACE_PROPAGATION | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/config/FeignTraceInterceptor.java` |
| WF-013 | TRACE_PERSISTENCE_ENTITY | `execution-trace-service/src/main/java/com/aidoctor/trace/entity/ExecutionTrace.java` |
| ENG-006 | PYTHON_TRACE_DECORATOR | `common/aidoctor_trace/trace_decorator.py` |

```text
Target count: 5
Missing: 0
Unknown: 0
Duplicate: 0
```

## 6. Source Hash Evidence

<a id="source-hash"></a>

### Target blobs

| Asset | git blob SHA |
|---|---|
| WF-010 | `a94e9ef866a8c43d97f64aa5605663f97bc2add0` |
| WF-011 | `4dd1f00c7341290045c8395783e27df07f94b888` |
| WF-012 | `3d7f501267660d7ec3952570d141ffedfec4c529` |
| WF-013 | `d204e10eb2e45dff601b00b83f419da92513e537` |
| ENG-006 | `88971899099a25ebd642fa4046a1a5af847e8a92` |

### Candidate Shared Contract schema hashes (read-only)

| Path | git blob SHA |
|---|---|
| `contracts/v1/manifest.json` | `d8cb373804d9fda3894741dd3964c69c2bb16607` |
| `contracts/v1/schemas/trace-ref.schema.json` | `8ab3bef870442afd6c0efc4f9cb7b6be115ea6f0` |
| `contracts/v1/schemas/audit-ref.schema.json` | `4854b2cc2c1965398ce6aa7944afff1b00bfeea6` |
| `contracts/v1/schemas/contract-envelope.schema.json` | `c2039eb7a5b4c30a0aec1055cd5fb2a07df29d2d` |
| `contracts/v1/schemas/identifier-set.schema.json` | `821351057699ecbf78099c6ad28738176e2c3a95` |
| `contracts/v1/schemas/contract-conflict.schema.json` | `9b930223b61ecbea9f5775622a26809bea84483e` |
| `contracts/v1/schemas/evidence-pack.schema.json` | `862d5da346355e49bdea81dbcd2c678818ce2257` |

No Runtime payload hashes were recorded.

## 7. Static Extraction Method

1. Confirm Enterprise head `1b559fe…` and create Evidence-only branch from that SHA.
2. Read only the five authorized Target files plus read-only Shared Contracts.
3. Perform REFERENCE_ONLY static lookup of supporting types required to interpret identifiers.
4. Extract symbols, fields, methods, annotation attributes, header literals, and control-flow observations.
5. Emit one mapping row per Asset (`A65C-C03-<ASSET_ID>`).
6. Define synthetic-only test scenarios without executing Runtime behavior.
7. Record Evidence integrity checks in validation CSV (not Runtime PASS claims).

## 8. Scanner / Parser Limitations

- Manual static read + deterministic CSV generation; no bytecode / runtime weaving analysis.
- Feign `RequestTemplate.header` duplicate/overwrite semantics are library-dependent and marked `UNKNOWN_REQUIRES_TEST`.
- Absence of a response interceptor for Feign end events is observed only within WF-012; other non-target classes were not expanded into new Targets.
- Production occupancy of CLOB payload fields is unknown without Runtime store access (forbidden).
- OTel concepts are candidate semantic labels only; no SDK APIs are present in Targets.

## 9. Current Legacy Observability Architecture

Legacy path (CURRENT_STATIC_PATH):

1. Java services optionally enable tracing with `execution.trace.enabled=true`.
2. `TraceExecution` annotation marks methods/types.
3. `ExecutionTraceAspect` emits START/END/ERROR events via `TraceServiceClient` when `cdpId` is present.
4. `FeignTraceInterceptor` injects HTTP headers and emits `FEIGN_CALL_START`.
5. Python `@trace_execution` decorator emits analogous events via HTTP POST to `TRACE_SERVICE_URL`.
6. `ExecutionTrace` JPA entity is persistence-capable storage shape for events.

No new Runtime path was created by C03. Future shadow-only OTel path remains a migration plan only.

## 10. WF-010 ExecutionTraceAspect Analysis

<a id="wf-010"></a>

| Observation | Detail |
|---|---|
| Symbols | `ExecutionTraceAspect.trace`, `getTraceExecutionAnnotation`, `sanitizeInput`, `sanitizeOutput` |
| Gate | `@ConditionalOnProperty(name="execution.trace.enabled", havingValue="true", matchIfMissing=false)` |
| Trigger | `@Around` on `@TraceExecution` method or type |
| Identifiers | `traceId=UUID.randomUUID()`; `cdpId=TraceContext.getCdpId()` |
| Skip paths | client null / annotation null / cdpId null → `joinPoint.proceed()` |
| Payload | optional via `traceInput`/`traceOutput`; sanitize records types only |
| Errors | records `errorMessage=e.getMessage()` then rethrows |
| Failure isolation | `FAIL_CLOSED_WORKFLOW` (Independent Review remediation) |
| PHI risk | `METADATA_CAPABLE` (emitted START/END are type metadata; unsanitized error text) |
| Status | `NEEDS_SECURITY_REVIEW` |
| Contracts | TraceRef; AuditRef; ContractEnvelope; IdentifierSet |

Independent Review static control-flow (CURRENT_STATIC_PATH):

1. `recordEvent(start)` before `proceed` — observability throw prevents business invocation.
2. `recordEvent(end)` after successful `proceed` inside same `try` — observability throw enters `catch` and rethrows, converting success into failure.
3. `recordEvent(error)` before `throw e` — observability throw may mask the original business exception.

Desired migration posture remains `FAIL_OPEN_OBSERVABILITY_ONLY` (not claimed as current).

## 11. WF-011 TraceExecution Analysis

<a id="wf-011"></a>

| Observation | Detail |
|---|---|
| Attributes | `service`, `module`, `traceInput` (default true), `traceOutput` (default true) |
| Target | METHOD, TYPE; Retention RUNTIME |
| Capture | Declarative only; no persistence logic |
| PHI risk | `METADATA_CAPABLE` (flags enable consumer capture) |
| Failure isolation | `UNKNOWN_REQUIRES_TEST` (depends on WF-010) |
| Status | `MAPPED_STRUCTURALLY` |
| Contracts | TraceRef; ContractEnvelope |

## 12. WF-012 FeignTraceInterceptor Analysis

<a id="wf-012"></a>

| Observation | Detail |
|---|---|
| Interface | `feign.RequestInterceptor.apply(RequestTemplate)` |
| Gate | same `execution.trace.enabled` property |
| Skip | client null or cdpId null → return |
| Events | `FEIGN_CALL_START` only in this asset |
| Retry | `NO_EXPLICIT_STATIC_EVIDENCE` |
| Failure isolation | `FAIL_CLOSED_WORKFLOW` (Independent Review remediation) |
| PHI risk | `METADATA_CAPABLE` |
| Status | `NEEDS_SECURITY_REVIEW` |
| Contracts | TraceRef; IdentifierSet; ContractEnvelope |

### Actual header literals (static)

| Observed literal | Source of value | Normative contract | Setting order |
|---|---|---|---|
| `X-CDP-Id` | `TraceContext.getCdpId()` | `UNRESOLVED_C_OD_007` | BEFORE `recordEvent(start)` |
| `X-Service-Name` | `template.feignTarget().name()` | `UNRESOLVED_C_OD_007` | AFTER `recordEvent(start)` |
| `X-Method-Name` | `extractMethodFromUrl(url)` | `UNRESOLVED_C_OD_007` | AFTER `recordEvent(start)` |
| `X-Trace-Id` | `UUID.randomUUID()` | `UNRESOLVED_C_OD_007` | AFTER `recordEvent(start)` |
| `X-Start-Time` | `System.currentTimeMillis()` | `UNRESOLVED_C_OD_007` | AFTER `recordEvent(start)` |

Classification:

```text
Observed literal: STATICALLY_OBSERVED
Normative contract: UNRESOLVED_C_OD_007
NOT: APPROVED_HEADER_CONTRACT
```

If `recordEvent(start)` throws: interceptor exception can abort the outbound Feign call; the four post-event headers are never set. Feign duplicate/caller header library semantics remain `UNKNOWN_REQUIRES_TEST` (separate from this control-flow fact).

## 13. WF-013 ExecutionTrace Analysis

<a id="wf-013"></a>

| Field group | Fields |
|---|---|
| Identifiers | `cdpId`, `traceId` |
| Identity/ops | `eventType`, `service`, `module`, `method`, `step`, `status`, `duration`, `timestamp` |
| Payload-capable | `inputData` (CLOB), `outputData` (CLOB), `errorMessage` (CLOB), `requestUrl` |
| Table | `execution_trace` with indexes on `cdp_id`, `event_timestamp`, `service` |

```text
ExecutionTrace treated as Audit SoT: no
Trace collection != payload persistence permission
PHI risk: PAYLOAD_CAPABLE
Status: NEEDS_PRIVACY_REVIEW
Candidate contracts: TraceRef; AuditRef; ContractConflict
```

## 14. ENG-006 trace_decorator Analysis

<a id="eng-006"></a>

| Observation | Detail |
|---|---|
| API | `trace_execution(service, module="")` |
| Context | `ContextVar` `_cdp_id` via `set_cdp_id` / `get_cdp_id` |
| Client | `TraceClient.record_event` → POST `{TRACE_SERVICE_URL}/api/v1/trace/events` |
| Failure | LAYER_A: `record_event` catches Exception, logs, does not re-raise |
| Business errors | error event attempted, then original exception re-raised |
| Skip | missing `cdp_id` → call function directly |
| Payload | no args/return capture statically; `errorMessage=str(e)` |
| Sync wrapper | `loop.run_until_complete(async_wrapper)` — already-running loop can prevent business execution |
| Failure isolation | `UNKNOWN_REQUIRES_TEST` (whole-asset; HTTP send isolated but sync wrapper not whole-asset fail-open) |
| PHI risk | `METADATA_CAPABLE` |
| Status | `NEEDS_SECURITY_REVIEW` |

```text
LAYER_A TRACE_HTTP_SEND_FAILURE_ISOLATED: yes
Whole-asset FAIL_OPEN_OBSERVABILITY_ONLY: no (overclaim remediated)
NOT PATIENT_SAFETY_FAIL_OPEN
```

## 15. Trace Identifier Sources

| Asset | Sources |
|---|---|
| WF-010 | UUID `traceId`; ThreadLocal `cdpId` |
| WF-011 | none (annotation) |
| WF-012 | UUID → `X-Trace-Id` + event; `cdpId` → `X-CDP-Id` + event |
| WF-013 | persisted `cdpId`, `traceId` columns |
| ENG-006 | `uuid.uuid4()`; ContextVar `cdp_id` |

## 16. Parent Context Sources

No Target statically defines a parent span / parent context header.

```text
parent_context_source (all assets):
NONE_STATIC_PARENT_SPAN or NONE_IN_ANNOTATION/ENTITY

Correlation present via cdpId only.
```

## 17. Propagation Transports

| Asset | Transport |
|---|---|
| WF-010 | none in-asset (event sink client) |
| WF-011 | none |
| WF-012 | HTTP request headers (Feign) |
| WF-013 | none (persistence model) |
| ENG-006 | HTTP JSON POST event body |

## 18. Actual Header Literals

See §12. Summary of STATICALLY_OBSERVED NON_NORMATIVE literals:

```text
X-CDP-Id
X-Service-Name
X-Method-Name
X-Trace-Id
X-Start-Time
```

## 19. TraceRef Mapping

Candidate mapping (not Runtime wiring):

| Legacy concept | Candidate Contract |
|---|---|
| `traceId` / `X-Trace-Id` | TraceRef (execution/correlation reference) |
| `cdpId` / `X-CDP-Id` | IdentifierSet / TraceRef correlation candidate |
| service/module/method | ContractEnvelope operation identity candidates |
| event status/error | TraceRef/envelope status candidates |

```text
Contract version: 1.0.0
Negotiation: EXACT
contracts/v1: READ_ONLY
```

## 20. AuditRef Separation

```text
TraceRef:
execution/correlation reference

AuditRef:
governance/audit reference

TraceRef != AuditRef
trace propagation != audit authorization
trace collection != permission to persist payload
ExecutionTrace != AUDIT_SOURCE_OF_TRUTH
```

## 21. Failure Isolation Analysis

Post Independent Review (asset-level CURRENT_STATIC_PATH):

| Asset | Classification | Rationale |
|---|---|---|
| WF-010 | `FAIL_CLOSED_WORKFLOW` | START blocks proceed; END can convert success to failure; ERROR can mask business exception |
| WF-011 | `UNKNOWN_REQUIRES_TEST` | annotation has no runtime control flow |
| WF-012 | `FAIL_CLOSED_WORKFLOW` | `recordEvent(start)` throw can abort Feign outbound call |
| WF-013 | `UNKNOWN_REQUIRES_TEST` | entity has no failure path |
| ENG-006 | `UNKNOWN_REQUIRES_TEST` | HTTP send isolated (LAYER_A); sync_wrapper running-loop path can break workflow |

```text
FAIL_OPEN_OBSERVABILITY_ONLY
=
desired migration posture / HTTP-layer note
!=
current whole-asset classification for WF-010/WF-012/ENG-006
!=
PATIENT_SAFETY_FAIL_OPEN

Runtime failure behavior verified: no
C-RISK-006: remains OPEN with stronger static Evidence
```

## 22. Retry / Fallback Analysis

| Asset | Retry | Fallback |
|---|---|---|
| WF-010 | `NO_EXPLICIT_STATIC_EVIDENCE` | skip-and-proceed when client/annotation/cdpId missing |
| WF-011 | `NO_EXPLICIT_STATIC_EVIDENCE` | N/A annotation |
| WF-012 | `NO_EXPLICIT_STATIC_EVIDENCE` | silent skip when client/cdpId missing |
| WF-013 | `NO_EXPLICIT_STATIC_EVIDENCE` | N/A entity |
| ENG-006 | `NO_EXPLICIT_STATIC_EVIDENCE` | skip when no cdp_id; swallow observability HTTP errors |

Retry propagation remains a required synthetic Test Matrix family despite no explicit retry code.

## 23. Payload Capture Capability

| Capability | WF-010 | WF-011 | WF-012 | WF-013 | ENG-006 |
|---|---|---|---|---|---|
| request body capable | indirect via args if flags true | flag only | no in-class | `inputData` CLOB | no |
| response body capable | indirect via return if flags true | flag only | no in-class | `outputData` CLOB | no |
| method args capable | yes (sanitized types observed) | flag | no | via inputData | no static capture |
| return value capable | yes (type-only observed) | flag | no | via outputData | no static capture |
| exception message capable | yes (`e.getMessage()`) | no | no | `errorMessage` CLOB | yes (`str(e)`) |
| patient identifier capable | `cdpId` correlation | no | `cdpId` header | `cdpId` column | `cdpId` |
| arbitrary metadata capable | service/module/method/status | service/module | url/service/method/headers | step/url/status | service/module/method/status |

```text
CAPABILITY
!=
OBSERVED_STATIC_PATH
!=
RUNTIME_BEHAVIOR_UNKNOWN / production claim
```

## 24. PHI Capture Risk

| Asset | Risk |
|---|---|
| WF-010 | `PAYLOAD_CAPABLE` |
| WF-011 | `METADATA_CAPABLE` |
| WF-012 | `METADATA_CAPABLE` |
| WF-013 | `PAYLOAD_CAPABLE` |
| ENG-006 | `METADATA_CAPABLE` |

Static PHI-capable source read: **yes**. Patient content accessed: **no**.

## 25. Redaction Boundary

Future safety targets (not implemented by C03):

- raw request body not persisted
- raw response body not persisted
- prompt/provider response not persisted
- patient identifier token redacted or excluded
- exception message sanitized
- structured metadata allowlist

Current static notes:

- WF-010 sanitize is type-only and does not sanitize `errorMessage`.
- WF-013 schema permits CLOB payloads without redaction logic.
- No production PHI policy is claimed.

## 26. Retention Dependencies

| Asset | Dependency |
|---|---|
| WF-010 / WF-012 / ENG-006 | `EXTERNAL_TRACE_SERVICE_REFERENCE` |
| WF-013 | `ENTITY_PERSISTENCE_CAPABLE` (`execution_trace`) |
| WF-011 | `NONE_STATIC` |

No Runtime retention config or DB records were accessed.

## 27. Synthetic Test Matrix Summary

Artifact: `a6-5-c-observability-test-matrix.csv`

```text
Rows: 40 (pre-review 37; +TST-038/039/040 from Independent Review)
Status: PLANNED (= definition exists; != test executed; != runtime PASS)
Required categories: 6/6
Feign scenario families: 7/7
PHI scenario families: 6/6
ENG-006 running-loop scenario: A65C-C03-TST-040 present
external_dependency_required=yes: 0
runtime_store_required=yes: 0
patient_data_required=yes: 0
Target without test coverage: 0
Runtime test execution claimed: no
PHI expected_redaction_behavior: FUTURE_TARGET (not current implemented guarantee)
```

## 28. OTel Candidate Mapping

Candidate semantics only:

| Concept | Mapping posture |
|---|---|
| `trace_id` | legacy UUID / `traceId` / `X-Trace-Id` |
| `span_id` | no distinct field; UNKNOWN_FAIL_CLOSED candidate |
| `parent_span_id` | NONE_STATIC |
| `trace_flags` | UNKNOWN_FAIL_CLOSED |
| `baggage boundary` | UNKNOWN (headers are non-normative) |
| `service name` | annotation/interceptor/decorator `service` |
| `operation name` | method / extracted URL segment |
| `error status` | `ERROR` / `errorMessage` |
| `attributes allowlist` | future allowlist; not implemented |

```text
OTel dependency added: no
Collector: no
Exporter: no
Runtime instrumentation: no
Production cutover: no
```

## 29. Legacy Coexistence

```text
legacy trace path retained: yes
new path shadow-only candidate: plan only (not implemented)
no dual-write without separate authorization: yes
no old-path deletion: yes
rollback entry required: yes (see §30)
observability-disabled mode required: yes (see §31)
```

## 30. Rollback Strategy

Documented rollback entry (plan-level; not executed):

1. Keep all five legacy Targets unmodified and present.
2. Do not delete `execution.trace.enabled` gate or Python decorator skip path.
3. Any future shadow path (unauthorized here) must be disableable independently.
4. Rollback = disable new path + retain legacy path; never delete legacy first.
5. Dual-write requires separate explicit authorization before any attempt.

## 31. Observability-disabled Mode

| Stack | Disable / skip mechanism (CURRENT_STATIC_PATH) |
|---|---|
| Java AOP / Feign | `execution.trace.enabled` not `true` (`matchIfMissing=false`) |
| Java runtime skip | missing `TraceServiceClient` or missing `cdpId` |
| Python | missing ContextVar `cdp_id` skips decorator tracing |

## 32. C-OD-006

```text
C-OD-006: UNRESOLVED / FAIL_CLOSED
TraceRef != AuditRef preserved in Evidence
No replacement decision made
No runtime ownership decision made
```

## 33. C-OD-007

```text
C-OD-007: UNRESOLVED / FAIL_CLOSED
Header literals: STATICALLY_OBSERVED NON_NORMATIVE
Normative header contract: not established by C03
```

## 34. C-OD-008

```text
C-OD-008: UNRESOLVED / FAIL_CLOSED
Synthetic failure classes defined
Runtime isolation not verified
UNKNOWN_REQUIRES_TEST preferred when unproven
```

## 35. C-OD-009

```text
C-OD-009: UNRESOLVED / FAIL_CLOSED
OTel treated as candidate semantic mapping only
No install / Collector / Exporter / cutover
```

## 36. C-OD-010

```text
C-OD-010: UNRESOLVED / FAIL_CLOSED
Synthetic-only PHI scenarios defined
No production retention/sampling policy authored
No real patient fixtures
```

## 37. C03-related Open Risks

Risk register file not modified. All `C-RISK-001..012` remain `OPEN`.

Directly relevant and still OPEN:

| Risk | Title | Control in C03 Evidence |
|---|---|---|
| C-RISK-004 | PHI_CAPABLE_STATIC_PATH_SCOPE_CONFUSION | static read ≠ patient data; documented |
| C-RISK-005 | TRACE_AND_AUDIT_SEMANTICS_CONFLATED | TraceRef≠AuditRef enforced in Evidence |
| C-RISK-006 | OBSERVABILITY_FAILURE_BREAKS_WORKFLOW | UNKNOWN / FAIL_OPEN_OBSERVABILITY_ONLY classifications |
| C-RISK-007 | FEIGN_TRACE_CONTEXT_DUPLICATED_OR_OVERWRITTEN | synthetic duplicate/caller scenarios |
| C-RISK-008 | RAW_PAYLOAD_OR_PHI_CAPTURED_IN_TRACE | PHI matrix; no production claim |
| C-RISK-011 | LEGACY_TRACE_PATH_REMOVED_BEFORE_FALLBACK_EXISTS | retention + rollback docs |
| C-RISK-012 | EXTERNAL_OTEL_OR_RUNTIME_DEPENDENCY_REQUIRED | zero OTel/runtime deps added |

## 38. Validation Summary

Artifact: `a6-5-c-observability-validation-evidence.csv`

```text
Implementation Execution A65C-C03-20260807-1B559FE rows preserved: 65
Independent Review Execution A65C-C03-REVIEW-20260807-BD1EE50 rows: 72
Total validation rows: 137
Missing required review checks: 0
Duplicate validation IDs: 0
Runtime behavior verified: no
Production behavior verified: no
```

LOCAL_C03_REVIEW_VERIFICATION (not CI PASS):

```text
pip check: No broken requirements found
A6: 11 schemas / 25 assets / 5 eval cases / 0 issues
A6 pytest: 59 passed
Contracts: 13 schemas / 13 valid / 33 invalid / exit 0
Contracts pytest: 110 passed
CI: NO_CI_CONFIGURED
```

## 39. Safety Boundary

```text
Static PHI-capable source read: yes
Patient content: no
Live/staging: no
Runtime stores: no
Trace DB: no
Clinical content: no
Prompt/provider content: no
Medical record: no
External model/API: no

Application source modified: no
AOP/Feign/Trace entity/Python decorator modified: no
Contracts/Capability/Runtime modified: no
Adapters / Runtime wiring: no
OTel dependency / Collector / Exporter: no

Approved clinical rules: 0
Approved thresholds: 0
Approved hypotheses: 0
Approved medical sources: 0
```

## 40. Scope Boundary

```text
Changed repository files (this task): 4
- a6-5-c-observability-mapping.csv
- a6-5-c-observability-test-matrix.csv
- a6-5-c-observability-validation-evidence.csv
- legacy-observability-migration.md

Out-of-scope modifications: 0
C01 Evidence modifications: 0
C02/B04 artifacts: 0
Clinical extraction: 0
```

### Supporting references outside Target (REFERENCE_ONLY)

| Path | Reason |
|---|---|
| `diagnosis-service/src/main/java/com/aidoctor/diagnosis/util/TraceContext.java` | Interpret `cdpId` ThreadLocal source used by WF-010/WF-012 |
| `contracts/v1/schemas/*.json` + `manifest.json` | Confirm candidate Contract names/version immutability |

These references are **not** additional Mapping Targets.

## 41. Final Implementation Conclusion

```text
Observability mapping rows: 5
Unique Mapping IDs: 5
Test Matrix rows: 40
Test categories: 6/6
Feign required scenario families: 7/7
PHI required scenario families: 6/6
Implementation Validation preserved: 65
Independent Review Validation: 72
Total Validation: 137
runtime_cutover_authorized: no (all rows)

Failure isolation post-review:
FAIL_CLOSED_WORKFLOW: 2 (WF-010, WF-012)
UNKNOWN_REQUIRES_TEST: 3 (WF-011, WF-013, ENG-006)
FAIL_OPEN_OBSERVABILITY_ONLY: 0
FAIL_ISOLATED: 0

PHI post-review:
METADATA_CAPABLE: 4
PAYLOAD_CAPABLE: 1 (WF-013)

Runtime behavior verified: no
Production behavior verified: no
OTel deployed: no

Post Independent Review status target:
READY_FOR_A6_5_C_C03_REVIEW_INTEGRATION
```

C03 Evidence remains static/synthetic only. Review Integration and Enterprise Merge of PR #23 require separate authorization.
