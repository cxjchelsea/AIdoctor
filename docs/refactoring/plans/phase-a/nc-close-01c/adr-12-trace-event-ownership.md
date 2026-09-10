# ADR-12 Trace / AgentEvent / Clinical Decision / Audit Ownership

| Field | Value |
|---|---|
| ADR ID | ADR-12 |
| Title | Trace / AgentEvent / Clinical Decision / Audit Ownership |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `e9f4baa791009ae16b6cc956a31bcd4bbd428213` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01C` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| Disposition | `FULL_DECISION` / `SEPARATE_OWNERSHIP_MODEL` |
| implementation_authorized | `false` |

## 1. Decision scope

Make the ownership split durable among four classes:

- Technical Span
- AgentEvent
- ClinicalDecisionRecord
- ComplianceAudit

This ADR does not implement stores, schemas, or clinical content. It
does not claim legal/regulatory compliance.

## 2. Context

Target architecture already names the four classes as independent
(`架构冻结基线.md`, Durable Execution design, module design). Current
runtime collapses technical calls into `ExecutionTrace` and mixes
tool/decision/CDP updates into `AuditTrail`.

Without a full ownership decision, later NC-CLOSE-04 work would treat
“trace”, “event”, “audit trail”, and “log” as interchangeable.

## 3. Current repository evidence

| Evidence | Classification | Note |
|---|---|---|
| `ExecutionTrace` / `execution-trace-service` | `CODE_CONFIRMED` | CURRENT technical-trace predecessor |
| `frontend-admin` consumers of that shape | `CODE_CONFIRMED` | timeline / call-graph / WebSocket |
| `AuditTrail` / `AuditTrailManager` | `CODE_CONFIRMED` | `tool_call` / `cdp_update` / `agent_decision` mixed predecessor |
| `AgentEvent` type/store | designed, **not implemented** | `DOCUMENTED` |
| `ClinicalDecisionRecord` type/store | designed, **not implemented** | `DOCUMENTED` |
| `ComplianceAudit` type/store | designed, **not implemented** | `DOCUMENTED` |
| `contracts/v1` `TraceRef` / `AuditRef` | `CODE_CONFIRMED` | references only; no payload embed; semantics **unchanged** |
| Private-reasoning ban in contract tests | `CODE_CONFIRMED` / `TEST_VERIFIED` | forbidden field names on public contracts |
| Structural CLOB / error-text risk | `CODE_CONFIRMED` | `STRUCTURAL_RISK`, not proof of production PHI |

## 4. Problem

One generic event model cannot be simultaneously:

- sampled technical telemetry
- durable workflow evidence
- clinical decision authority
- compliance audit authority

Collapsing them makes OTel spans look like medical records and makes
audit inherit telemetry sampling.

## 5. Architectural invariants

1. The four classes remain distinct. They must not collapse into one
   generic trace / event / audit-trail / log model.
2. OTel Technical Span is never clinical or audit SoR.
3. ClinicalDecisionRecord ownership stays compatible with ADR-02:
   authoritative clinical writes pass through the State Committer /
   clinical commit boundary.
4. Logs are not audit authority. Metrics are not trace authority.
5. ClinicalDecisionRecord and ComplianceAudit must not be dropped
   because technical telemetry is sampled.
6. Hidden chain-of-thought / private model reasoning is not a default
   persistable field of Technical Span, AgentEvent, or ComplianceAudit.
7. No clinical schema, PHI implementation, or regulatory certification
   is decided here.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A12-A | One generic event/audit model |
| A12-B | Treat `execution-trace-service` as AgentEvent + audit + clinical record |
| A12-C | Treat `AuditTrail` as already-sufficient ComplianceAudit |
| A12-D | **Selected.** Four separate ownership classes |

## 7. Selected decision

**A12-D — SEPARATE_OWNERSHIP_MODEL.**

### 7.1 Technical Span

Technical Span is **engineering observability**.

Examples: HTTP/service calls, tool timing, latency, errors, technical
model-runtime invocation **metadata**.

| Property | Decision |
|---|---|
| Authority | derived technical evidence |
| System-of-record | **NO** |
| Sampling | **MAY** be sampled under a future observability policy |
| Storage target | technical telemetry backend (ADR-10) |
| CURRENT predecessor | `ExecutionTrace` / `execution-trace-service` |
| Must not become | clinical truth; ClinicalDecisionRecord; ComplianceAudit authority |

### 7.2 AgentEvent

AgentEvent represents **structured workflow / runtime lifecycle
evidence**.

Potential examples (illustrative, not a schema):

- run started
- node entered
- tool invoked
- interrupt raised
- resume occurred
- delivery state changed
- reason code emitted

It is **not**:

- hidden model reasoning
- raw chain-of-thought
- clinical decision SoR
- a generic Technical Span

| Property | Decision |
|---|---|
| Target producer | future controlled Runtime / workflow layer |
| Status | `DESIGNED` / `NOT_IMPLEMENTED` |
| Schema / event bus | **not** invented or implemented here |

### 7.3 ClinicalDecisionRecord

This ADR defines **ownership boundary only**.

ClinicalDecisionRecord represents future **approved / committed
clinically meaningful decision evidence**.

It is **not** OTel telemetry, Technical Span, AgentEvent, or a debug
log.

Target authority remains compatible with ADR-02: clinical SoR writes
ultimately pass through the authoritative clinical state/commit
boundary (future State Committer).

Do not define medical fields. Do not create a clinical schema. Do not
activate Clinical Runtime.

```text
Status: DESIGNED / NOT_IMPLEMENTED / CLINICAL TRACK BLOCKED
```

### 7.4 ComplianceAudit

ComplianceAudit represents durable **security / governance audit
evidence** for protected actions.

Architecture-level properties:

- actor / action attribution
- durability
- append-oriented / tamper-resistant expectation
- version / correlation linkage
- restricted access
- separate retention ownership

It is **not** sampled technical telemetry, an ordinary application log,
or a ClinicalDecisionRecord.

Do **not** claim: HIPAA compliant, GDPR compliant, certified, or
regulator-approved.

CURRENT `AuditTrail` is a **legacy/mixed predecessor**, not proof of
regulatory compliance.

## 8. Event relationship matrix

| Field | Technical Span | AgentEvent | ClinicalDecisionRecord | ComplianceAudit |
|---|---|---|---|---|
| Purpose | operational timing/errors/calls | structured workflow/runtime lifecycle | approved/committed clinical decision evidence | protected-action governance audit |
| Authority | derived technical | orchestration evidence; not clinical truth | future clinical-record authority | future audit authority |
| Durability | short-to-medium; backend policy | durable event-store policy, separate from span sampling | must persist | must persist |
| Mutability | derived; replaceable | append-oriented | not disposable telemetry | append-oriented / tamper-resistant expectation |
| Producer | CURRENT AOP/Feign; TARGET OTel instrumentation | TARGET Runtime / workflow | TARGET clinical commit path / State Committer | TARGET protected-action writers |
| Consumer | ops, admin call-graph | Runtime/dev, recovery UI | clinician review (not enabled) | security/governance (not certified) |
| Storage owner | technical telemetry backend; CURRENT `execution_trace` | future AgentEvent store | future Clinical Decision store | future Audit store; CURRENT `audit_trail` is mixed predecessor |
| Correlation IDs | `trace_id` / future span; `cdp_id` | `run_id` / `thread_id` / optional `trace_id` | `cdp_id` + SoR version | `audit_id` + `cdp_id` / version |
| PHI policy | default: no raw PHI; allowlist/metadata/redaction-aware | no raw PHI / no private reasoning by default | ownership only; no schema; clinical track blocked | minimum necessary; no full prompt dump |
| Clinical meaning | NO | NO | YES (ownership only) | NO |
| Sampling | MAY | separate durability policy | MUST NOT inherit span sampling | MUST NOT inherit span sampling |
| OTel fit | YES | NO as SoR | NO | NO |
| System-of-record | NO | NO | YES (future clinical record) | YES (future audit record) |
| Retention owner | Platform Observability | Runtime / event-store owner | Clinical / SoR governance | Security / audit governance |

## 9. Logging / metrics separation

| Signal | Authority |
|---|---|
| Application Log | debug/ops text; **not** audit authority |
| Metric | aggregated performance; **not** trace authority |
| Technical Span | technical telemetry only |
| AgentEvent | workflow evidence; not a span dump |
| ClinicalDecisionRecord | clinical record; not telemetry |
| ComplianceAudit | audit record; not a log file |

Audit data must not inherit telemetry sampling automatically.

## 10. Sampling boundary

Principle only; **no percentages**:

- Technical Span: may be sampled in future.
- Metrics: aggregated by nature.
- AgentEvent: durability policy separate from technical sampling.
- ClinicalDecisionRecord: must **not** be dropped due to telemetry sampling.
- ComplianceAudit: must **not** be dropped due to telemetry sampling.

## 11. Privacy / private-reasoning boundary

Technical telemetry **MUST NOT** by default persist raw PHI.

Default technical attributes should be:

- allowlist-based
- metadata-oriented
- redaction-aware

Redaction is **not** implemented here. Actual PHI is **not** inspected.

Documented `STRUCTURAL_RISK` (not proof of production PHI):

- `input_data` / `output_data` CLOB fields
- `error_message` / exception text
- raw payload-capable DTO fields
- unauthenticated admin WebSocket pattern (access-control debt)

Hidden chain-of-thought, private model reasoning, and raw internal
reasoning traces **MUST NOT** be persisted as default Technical Span,
AgentEvent, or ComplianceAudit content.

Approved/public rationale, if later needed, is a separate product or
clinical artifact. That schema is **not** designed here.

## 12. Legacy asset disposition

These are **target migration dispositions**, not implementation actions.
This ADR does not archive, delete, or rewrite the assets.

| Asset | Disposition class | Decision vs later evaluation |
|---|---|---|
| `ExecutionTrace` | `ADAPT` / `BRIDGE` during migration | **Decision:** remains CURRENT technical-span predecessor; adapt/bridge under ADR-11 |
| `execution-trace-service` | `EVALUATE` / `WRAP` / `ADAPT` during future migration | **Decision:** do not archive now. **Evaluation** of wrap vs split is deferred to authorized implementation |
| `AuditTrail` / `AuditTrailManager` | `SPLIT` / `ADAPT` **candidate** | **Decision:** it currently mixes categories and is not ComplianceAudit proof. Exact split design is later evaluation |
| `frontend-admin` trace consumers | compatibility constraint for ADR-11 | **Decision:** preserve or explicitly map while still required; no UI rewrite now |

## 13. Rationale

Design documents already separate the four classes. Current code
conflates them. A full ownership decision can be made without
implementing stores or clinical schemas.

## 14. Rejected alternatives

- **A12-A** recreates the current conflation as the target.
- **A12-B** overloads the legacy trace service.
- **A12-C** treats mixed `AuditTrail` as regulatory compliance.

## 15. Compatibility

- ADR-02: ClinicalDecisionRecord is not a checkpoint and not a span.
- ADR-03: correlation IDs may travel on the protocol; they do not make
  a span into a clinical record.
- ADR-04 / ADR-05: Runtime / LangGraph may produce AgentEvents and
  Technical Spans; they must not write clinical SoR.
- Shared Contracts v1 unchanged.
- A7-NC unchanged.

## 16. Implementation consequences

Later authorized work may introduce separate stores and adapters. This
ADR does not add tables, buses, schemas, or UI.

## 17. Explicit non-goals / forbidden claims

Do **not** read this ADR as:

- AgentEvent / ClinicalDecisionRecord / ComplianceAudit implemented
- HIPAA / GDPR / certified / regulator-approved
- clinical schema approved
- Clinical Runtime enabled
- PHI handled
- `AuditTrail` already compliant
- NC-CLOSE-04 implemented

## 18. Architecture stop conditions

```text
PHASE_A_NC_CLOSURE_CLINICAL_BOUNDARY_REACHED
PHASE_A_NC_CLOSURE_PHI_BOUNDARY_REACHED
A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW
```

if stating ownership would require clinical activation, PHI
implementation, or Shared Contracts semantic change.

## 19. Evidence truth

| Claim | Label |
|---|---|
| Four-class ownership decision | `DOCUMENTED` (this ADR) |
| Legacy predecessors | `CODE_CONFIRMED` |
| New stores implemented | **NO** / `NOT_IMPLEMENTED` |
| Clinical record implementation | `NOT_IMPLEMENTED` / `BLOCKED` |
| Compliance implementation | `NOT_IMPLEMENTED` |
| Regulatory compliance | **not claimed** |
| Production PHI presence | `UNKNOWN` (structural risk only) |

## 20. Relationships

- `HARD_INPUT_FOR_NC_CLOSE_04_TRACE_OBSERVABILITY`
- Consumed by ADR-10 (what OTel may store) and ADR-11 (what legacy
  events map to)

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01C.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.

## 22. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01C Draft PR.
Implementation remains `NOT_AUTHORIZED`.
