# NC-CLOSE-06 First Vertical-Slice E2E Design

> Dated: 2026-08-18
>
> Classification: `DESIGN_ONLY` / `NO EXECUTABLE E2E` / `NO CLINICAL E2E`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Scope ID: `NC-CLOSE-E2E-01`
>
> Exact Enterprise Base: `f08f26a1e1187c19cad73351ebc048388d7fdbc8`
>
> Base tree: `f0493a3f1e24378d917afe7d338b4b5895b2b791`
>
> Authorization token:
> `NC_CLOSE_06_INTEGRATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Status: `DESIGN_AUTHORED_PENDING_REVIEW`

```text
DESIGN_ONLY
EXECUTABLE_E2E_AUTHORIZED=false
CLINICAL_EXECUTION_AUTHORIZED=false
PROVIDER_AUTHORIZED=false
PHI_AUTHORIZED=false
implementation_authorized=false
```

This artifact designs the first non-clinical vertical slice. It does
**not** execute the slice, approve clinical behavior, or prove runtime
integration.

## 1. Purpose

Illustrate target **control flow** and **authority gates** using already
frozen contracts and ADR boundaries. Every node is tagged as current,
legacy, target, blocked, deferred, mocked, or not authorized.

```text
DESIGN COMPLETE
!=
E2E TEST PASS
!=
RUNTIME_VERIFIED
!=
PHASE_A_NC_CLOSURE_EXIT
```

## 2. Current-versus-target legend

| Tag | Meaning |
|---|---|
| `CURRENT_IMPLEMENTED` | Code-confirmed current path |
| `CURRENT_LEGACY` | Present and overloaded / transitional |
| `TARGET_BOUNDARY` | Frozen ownership; not yet the live writer |
| `DESIGNED_NOT_IMPLEMENTED` | Named in contracts/ADRs; no runtime owner yet |
| `BLOCKED_CLINICAL` | Explicit clinical stop; placeholder only |
| `DEFERRED_BACKEND` | Mechanism frozen; vendor/enablement deferred |
| `MOCKED_FOR_DESIGN` | Design-time stand-in; not a Fake clinical gold |
| `NOT_AUTHORIZED` | Must not be activated by this design |

Do not read the slice as an already-running target stack.

## 3. Bounded first vertical slice

```text
entry / request                         CURRENT_IMPLEMENTED
  → Java orchestration boundary         CURRENT_IMPLEMENTED
  → Python tool / service boundary      CURRENT_LEGACY
  → future Python Runtime boundary      TARGET_BOUNDARY / DESIGNED_NOT_IMPLEMENTED
  → Capability eligibility gate         CURRENT_IMPLEMENTED (skeleton) / BLOCKED_CLINICAL
  → clinical-block placeholder          BLOCKED_CLINICAL
  → Model Runtime Fake / blocked shell  CURRENT_IMPLEMENTED (A7-NC) / NOT_AUTHORIZED real provider
  → ToolContext / ToolResult            CURRENT contract; parallel legacy models
  → StatePatch proposal                 CURRENT contract; not a commit
  → State Committer authority           TARGET_BOUNDARY / DESIGNED_NOT_IMPLEMENTED
  → CommitResult                        CURRENT contract
  → retrieval / evidence boundary       DEFERRED_BACKEND / MOCKED_FOR_DESIGN
  → EvidencePack / provenance gate      CURRENT contract
  → PatientDeliveryView / visibility    CURRENT contract / DESIGNED_NOT_IMPLEMENTED delivery
  → audit / trace separation            CURRENT_LEGACY predecessor / TARGET_BOUNDARY
```

### 3.1 Node inventory

| Node | Tag | Justification |
|---|---|---|
| HTTP entry (`DiagnosisController` and peers) | `CURRENT_IMPLEMENTED` | NC-CLOSE-04 WF path |
| Java orchestration (`DiagnosisOrchestrationService` / WorkflowOrchestrator) | `CURRENT_IMPLEMENTED` | NC-CLOSE-04 WF-01 |
| `AgentLoop` / `ClinicalAgentBrain` | `CURRENT_LEGACY` | Present; **not** HTTP authority |
| Python tool / engine services | `CURRENT_LEGACY` | Ten-service inventory; mixed runtime evidence |
| Future `packages/python_runtime` | `TARGET_BOUNDARY` / `DESIGNED_NOT_IMPLEMENTED` | ADR-04 |
| `adult_respiratory_v1` eligibility | `BLOCKED_CLINICAL` | DRAFT / NOT_IMPLEMENTED / Production BLOCKED |
| Clinical decision / prompt / threshold | `BLOCKED_CLINICAL` | Lane exclusion |
| Model Runtime Fake / blocked shell | `CURRENT_IMPLEMENTED` | A7-NC COMPLETE; real provider `NOT_AUTHORIZED` |
| `ToolContext` / `ToolResult` v1 | `CURRENT_IMPLEMENTED` (contract) | NC-CLOSE-02; runtime models remain parallel |
| `StatePatch` | `CURRENT_IMPLEMENTED` (contract) | Proposal only |
| Current Java `CDPManager` write | `CURRENT_LEGACY` | Transitional SoR writer (ADR-02) |
| State Committer | `TARGET_BOUNDARY` / `DESIGNED_NOT_IMPLEMENTED` | Unique target clinical writer |
| `CommitResult` | `CURRENT_IMPLEMENTED` (contract) | Commit outcome shape |
| Vector / BM25 | `DEFERRED_BACKEND` | ADR-06 / ADR-07 |
| Neo4j | `CURRENT_LEGACY` / `DEFERRED_BACKEND` | Clients exist; production enablement deferred; not guideline authority |
| `EvidencePack` / `SourceArtifact` / `KnowledgeReleaseRef` | `CURRENT_IMPLEMENTED` (contract) | Citation/provenance authority |
| `PatientDeliveryView` | `CURRENT_IMPLEMENTED` (contract) | Visibility constraint; delivery runtime not proven |
| `ExecutionTrace` / `AuditTrail` | `CURRENT_LEGACY` | Technical-span / mixed-audit predecessors (ADR-12) |
| Technical Span / AgentEvent / CDR / ComplianceAudit | `TARGET_BOUNDARY` / `DESIGNED_NOT_IMPLEMENTED` | ADR-12 |
| Secret Manager / OTel backend | `DEFERRED_BACKEND` | ADR-09 / ADR-10 |

LangGraph, if shown, is `TARGET_BOUNDARY` / bounded role only (ADR-05).
It is **not** orchestration authority in this slice.

## 4. Clinical placeholders

The slice **stops** before clinical activation. Use only:

```text
CLINICAL_GATE_BLOCKED
CLINICAL_CAPABILITY_NOT_ENABLED
CLINICAL_DECISION_NOT_EXECUTED
```

Forbidden in this design as executed content:

- real diagnosis / treatment / red-flag
- clinical thresholds / prompts / hypotheses
- medical gold / approved medical output
- patient data / PHI

Neo4j candidate fusion, if mentioned, is
`CURRENT_STRUCTURAL_CLINICAL_AUTHORITY_RISK` and remains
`NEO4J_IS_NOT_CLINICAL_GUIDELINE_AUTHORITY`. This design does not
repair or enable it.

## 5. Provider boundary

```text
real provider = 0
credentials = 0
network inference = 0
paid / external model API = 0
```

Model Runtime may appear only as:

- Deterministic Fake (A7-NC)
- blocked-shell / `LegacyLLMDisabledError` containment
- design-level `MODEL_ROUTE_BLOCKED`

Do not reopen A7-NC semantics.

## 6. Deterministic gate ordering

Technical order only. No new clinical policy.

1. Request identity / correlation (`IdentifierSet` / envelope ids)
2. Contract / version validation (Shared Contracts v1)
3. Capability eligibility (`adult_respiratory_v1` remains DRAFT)
4. Clinical-block gate → `CLINICAL_GATE_BLOCKED` if clinical work is requested
5. Runtime / model-route gate (Fake or blocked shell; real provider forbidden)
6. Tool boundary (`ToolContext` in / `ToolResult` out)
7. `StatePatch` proposal (never an already-committed fact)
8. State Committer authority (target unique writer; current `CDPManager` is transitional)
9. Evidence / provenance gate (`EvidencePack` + `KnowledgeReleaseRef` + citation)
10. Delivery visibility gate (`PatientDeliveryView`)
11. Audit / trace separation (Technical Span ≠ AgentEvent ≠ CDR ≠ ComplianceAudit)

A later gate must not silently undo an earlier fail-closed decision.

## 7. Failure matrix

No clinical fallback is defined. Fabricating evidence is forbidden.

| Failure | Owner | Technical status | Open / closed | Patient visibility | Clinician / system visibility | Audit | Trace | Remediation owner |
|---|---|---|---|---|---|---|---|---|
| Contract mismatch | Contract / binding owner | `CONTRACT_REJECTED` | fail-closed | none | system error / conflict | AuditRef if request accepted | TraceRef on reject path | NC-CLOSE-02 consumers (later) |
| Capability blocked | Capability gate | `CLINICAL_CAPABILITY_NOT_ENABLED` | fail-closed | none | blocked capability | record block, not clinical content | technical span only | A6 / A7-CL (outside NC) |
| Python Runtime unavailable | ADR-04 target owner | `RUNTIME_UNAVAILABLE` | fail-closed for that hop | none | technical unavailable | no invented tool result | span + error class | later Runtime implementation |
| Model Runtime route blocked | A7-NC route policy | `MODEL_ROUTE_BLOCKED` | fail-closed | none | blocked-shell / Fake-only | no provider payload | technical span | A7-NC remains CLOSED; no reopen |
| Tool unavailable | Tool service owner | `TOOL_UNAVAILABLE` | fail-closed for that tool | none | tool error | no fabricated ToolResult | isolated if possible | per-service remediation (not 06) |
| StatePatch rejected | State Committer / validator | `STATE_PATCH_REJECTED` | fail-closed | none | conflict / reject | AuditRef on reject | span | ADR-02 boundary; no second writer |
| CommitResult failure | State Committer | `COMMIT_FAILED` | fail-closed | none | commit failed | audit of failed commit | span | Phase B / later implementation |
| Vector unavailable | Retrieval adapter | `RETRIEVAL_UNAVAILABLE` / insufficient evidence | fail-closed for evidence use | no fabricated chunks | insufficient evidence | no EvidencePack claim | technical | ADR-06; vendor still deferred |
| BM25 unavailable | Lexical adapter | `DEGRADED_VECTOR_ONLY` or `RETRIEVAL_UNAVAILABLE` | technical degrade only | no lexical-as-truth | degraded retrieval | citation still required | technical | ADR-07 |
| Neo4j unavailable | Graph adapter | `TECHNICAL_GRAPH_UNAVAILABLE` | technical degrade | **not** “no disease” | graph unavailable | must not encode medical negative | technical | ADR-08; no enablement here |
| Stale index | Retrieval / release owner | `INDEX_STALE` | fail-closed for “current” evidence | not current knowledge | stale / not current | no silent current claim | technical | later retrieval ops |
| KnowledgeRelease mismatch | Evidence / knowledge release | `RELEASE_MISMATCH` | fail-closed | none | mixed-release rejected | no mixed-release EvidencePack | technical | ADR-06 provenance |
| EvidencePack provenance insufficient | Evidence gate | `PROVENANCE_INSUFFICIENT` | fail-closed | none | insufficient citation | cannot support claim | technical | contracts v1 invariant |
| Trace unavailable | Observability owner | `TECHNICAL_TRACE_UNAVAILABLE` | target fail-open for workflow | none | workflow may continue | audit ≠ trace | current: isolation partial | NC-CLOSE-04 debt; 07 judges Exit |
| Delivery unavailable | Delivery owner | `DELIVERY_UNAVAILABLE` | fail-closed for patient view | none | system-only | no silent patient reveal | technical | later delivery implementation |

Retrieval invariants (ADR-06 / 07 / 08):

```text
vector unavailable     → RETRIEVAL_UNAVAILABLE / insufficient evidence
BM25 unavailable       → DEGRADED_VECTOR_ONLY or RETRIEVAL_UNAVAILABLE
Neo4j unavailable      → TECHNICAL_GRAPH_UNAVAILABLE
TECHNICAL_GRAPH_UNAVAILABLE != MEDICAL_NEGATIVE_RESULT
stale index            → not current approved knowledge
release mismatch       → fail closed for evidence use
missing provenance     → cannot support an EvidencePack claim
```

Current Neo4j connection failure may collapse to empty `possibilities`.
That is **current implementation debt**, not a designed medical negative.
This design records the distinction and does not change code.

Current trace collaborator-contract breaches may fail-close workflow.
Target (ADR-10/11): technical telemetry fail-open. Classification stays
`CURRENT_LEGACY` versus `TARGET_BOUNDARY`.

## 8. Version / provenance map

Use existing Shared Contracts v1 only. No new fields.

| Concept | Role in this slice |
|---|---|
| Shared Contracts v1 | Semantic envelope; `IMMUTABLE_WITHIN_LANE` |
| `ContractEnvelope` | Versioned request/response wrapper |
| `IdentifierSet` | Correlation / encounter / request identity |
| Capability version | `adult_respiratory_v1` remains DRAFT |
| ModelSpec / Model Runtime version | A7-NC CLOSED / STABLE; Fake or blocked |
| `ToolContext` / `ToolResult` | Tool hop shapes |
| `StatePatch` | Proposal toward State Committer |
| `CommitResult` | Commit outcome; not a proposal |
| `ContractConflict` | Structured reject |
| `KnowledgeReleaseRef` | Evidence release identity |
| `SourceArtifact` | Source object |
| `EvidencePack` | Claim-level citation/provenance pack |
| `TraceRef` | Reference only; not ExecutionTrace payload |
| `AuditRef` | Reference only; not AuditTrail payload |
| `PatientDeliveryView` | Visibility-constrained delivery |

`VECTOR_HIT != APPROVED_MEDICAL_EVIDENCE`.  
`BM25_HIT != citation`.  
`GRAPH_PATH != clinical guideline authority`.

## 9. 01A / 01B / 01C consumption

- Checkpoint ≠ clinical SoR. State Committer is the unique **target** writer.
- Java↔Python remains versioned HTTP/REST JSON (ADR-03). No protocol redesign.
- Retrieval adapters must not become Runtime or LangGraph authority.
- Patient RAG ≠ Medical Evidence RAG even if a future physical store is shared.
- Concrete DB vendor, pgvector/Milvus, BM25 library, Neo4j production on,
  Secret Manager vendor, and OTel backend remain deferred.
- Technical Span / AgentEvent / ClinicalDecisionRecord / ComplianceAudit
  stay distinct (ADR-12).

## 10. Completeness claim

This design is complete when, and only when:

- the slice is bounded;
- every node is tagged;
- gate order is deterministic;
- the failure matrix exists;
- version/provenance uses current contracts;
- clinical / provider / PHI remain blocked.

It is **not** complete by running a test.

## 11. Explicit non-goals

Executable JUnit / pytest / browser E2E; clinical fixtures; gold;
provider calls; embeddings; production DB inspection; Neo4j enablement;
OTel install; State Committer implementation; NC-CLOSE-07 Exit Review.
