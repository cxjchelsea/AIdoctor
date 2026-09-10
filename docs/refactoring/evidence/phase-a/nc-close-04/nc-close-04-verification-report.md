# NC-CLOSE-04 Workflow / Trace Verification Report

> Dated: 2026-08-17
>
> Classification: `VERIFICATION_ONLY` / `NO PRODUCTION REMEDIATION`
>
> Authorization token: `NC_CLOSE_04_VERIFICATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Exact Base: `11f855d1082ebb1492593f708052316e7924fac1`
>
> Base tree: `cafa3f1bab2409809920d427625f2492357bece8`
>
> implementation_authorized: `false` (verification / test harness / evidence only)

## 1. Purpose

This report records W2 characterization of current Fixed Workflow and
legacy technical tracing. It does not implement observability, repair
production behavior, or close NC-CLOSE-04.

```text
TEST PASS != REQUIREMENT PASS
```

## 2. Package state

```text
NC-CLOSE-WF-01:    VERIFIED (technical characterization; no clinical claim)
NC-CLOSE-TRACE-01: VERIFICATION_EXECUTED_BLOCKED_BY_CURRENT_IMPLEMENTATION
NC-CLOSE-04:       VERIFICATION_EXECUTED_WITH_BLOCKING_FINDINGS_PENDING_REVIEW
```

## 3. Reused A6.5 C03 evidence

C03 five-asset blobs still match this Base for:

- `ExecutionTraceAspect` `a94e9ef866a8c43d97f64aa5605663f97bc2add0`
- `@TraceExecution` `4dd1f00c7341290045c8395783e27df07f94b888`
- `FeignTraceInterceptor` `3d7f501267660d7ec3952570d141ffedfec4c529`
- `TraceContext` `0ede1613483c441307fcdd23325c99199294e29a`
- `TraceServiceClient` `504c35256265b413af005d7e5634cbe839f4dee3`

Selected prior IDs executed as new 04 rows: `A65C-C03-TST-001` `002`
`003` `007` `009` `010` `012` `013` `016` `020` `032` `036` `038`.

C-OD-006/008/009 ownership/isolation/coexistence targets are
`SUPERSEDED_BY_01C_DECISION`. C03 planned matrices remain
`INSUFFICIENT_FOR_NC_CLOSE_04` as runtime proof.

## 4. Tests executed

Local command: `mvn -f diagnosis-service/pom.xml test`

| Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| NcClose04WorkflowVerificationTest | 10 | 0 | 0 | 0 |
| NcClose04TraceVerificationTest | 14 | 0 | 0 | 0 |
| Pre-existing baseline | 20 | 0 | 0 | 0 |
| **Total** | **44** | **0** | **0** | **0** |

New NC-CLOSE-04 tests: **24**.

Evidence level: `TEST_VERIFIED` (W2). Not `RUNTIME_VERIFIED`.

## 5. WF-01 summary

Authoritative path: `DiagnosisController` → `DiagnosisOrchestrationService`
→ `DiagnosisWorkflowOrchestrator` / `WellnessScreeningOrchestrator`.
`AgentLoop` / `ClinicalAgentBrain` are not HTTP authority.

Stable double-run fields: `status` `workMode` `currentStep` completeness
gate outcome fallback class. Ignored: timestamps UUIDs session/cdp ids
narrative.

No clinical policy change. No PHI. No provider.

## 6. TRACE-01 summary

Enablement: DEFAULT off; CONFIGURABLE; PRODUCTION occupancy UNKNOWN.

Coverage: `executeRemainingSteps` unannotated → `PARTIALLY_VALIDATED`.

Failure isolation:

- `TraceServiceClient` HTTP swallow → `PASS_VERIFIED` / `FAIL_ISOLATED`
- Aspect start/end throw → `FAIL_CURRENT_IMPLEMENTATION` / `FAIL_CLOSED_WORKFLOW`
- Feign apply throw → `FAIL_CURRENT_IMPLEMENTATION` / `FAIL_CLOSED_WORKFLOW`

OTel SDK/exporter: ABSENT. Legacy removal: `LEGACY_TRACE_REMOVAL_NOT_ELIGIBLE`.

ADR-12: ExecutionTrace = `TECHNICAL_SPAN_PREDECESSOR`; AuditTrail =
`MIXED_OVERLOADED_LEGACY`; AgentEvent / ClinicalDecisionRecord /
ComplianceAudit = `DESIGNED_NOT_IMPLEMENTED`.

Admin consumers: static `ExecutionTrace` / `TraceSummary` / REST
`/api/v1/trace/*` / WS `/api/v1/trace/ws` recorded; no frontend rewrite.

## 7. Blocking findings

| ID | Target | Current | Target boundary | Severity |
|---|---|---|---|---|
| NC04-F-TRACE-ASPECT-START | ExecutionTraceAspect | start `recordEvent` throw prevents `proceed` | ADR-10/11 fail-open technical telemetry | BLOCKING_FOR_NC_CLOSE_04_EXIT |
| NC04-F-TRACE-ASPECT-END | ExecutionTraceAspect | end `recordEvent` throw converts success to failure | same | BLOCKING_FOR_NC_CLOSE_04_EXIT |
| NC04-F-TRACE-FEIGN-APPLY | FeignTraceInterceptor | `recordEvent` throw aborts Feign apply | same | BLOCKING_FOR_NC_CLOSE_04_EXIT |

Production remediation required: YES (separate token).
This package does not implement the fix.

## 8. Non-blocking findings

- `executeRemainingSteps` missing `@TraceExecution` — MAJOR coverage / `PARTIALLY_VALIDATED`
- `errorMessage` carries raw canary text — MAJOR structural `PAYLOAD_CAPABLE_CURRENT_STRUCTURE`
- ExecutionTraceEvent input/output Object fields — structural payload capability
- TraceContext no cross-thread propagation — documented debt
- Production `execution.trace.enabled` occupancy — `RUNTIME_ENVIRONMENT_UNKNOWN`
- Python `aidoctor_trace` unused — SUPPORTING only

## 9. Remaining UNKNOWNs

| Unknown | Class | Blocking |
|---|---|---|
| Production trace enablement occupancy | RUNTIME_ENVIRONMENT_UNKNOWN | NO |
| Deployed multi-service header E2E | RUNTIME_ENVIRONMENT_UNKNOWN | NO |
| OTel equivalence | DEFERRED_IMPLEMENTATION_UNKNOWN | NO |
| Feign library timeout numbers | NON_BLOCKING_UNKNOWN | NO |

## 10. Unauthorized work check

Production Java/Python/trace-service/frontend/contracts/CI/deps/DB/
clinical/provider/PHI/OTel/Secret Manager/State Committer/01B/Phase B:
**all 0**.

## 11. Next gate

Combined Independent Review + Merge Review of the exact Draft Head.

Because blocking current-implementation defects were proven, review must
decide whether this evidence package is mergeable and whether a separate
targeted remediation authorization is required.

Do not remediate in this package.
Do not mark Ready from authoring.
Do not merge from authoring.
