# NC-CLOSE-01B Data & Retrieval Boundaries

> Dated: 2026-08-17
>
> Classification: `ARCHITECTURE_DECISION_ONLY` / `NO IMPLEMENTATION` / `DOCS_ONLY`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Parent batch: `NC-CLOSE-01` = ADR Foundation (umbrella implementation
> remains `NOT_AUTHORIZED`)
>
> Refined batch: `NC-CLOSE-01B`
>
> Exact Enterprise Base: `2e5ab0c68e16a9b5d62aa2bdef250f5348d615c3`
>
> Base provenance: PR #49 / NC-CLOSE-05 `MERGED_AND_VERIFIED`
>
> Authorization token:
> `NC_CLOSE_01B_ADR_DECISION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> ADR status: `PROPOSED_DECIDED_PENDING_REVIEW`
>
> implementation_authorized: `false`

## 1. Purpose

This is the integration / index artifact for the four NC-CLOSE-01B
architecture decisions. It freezes data and retrieval **boundaries**
without selecting vendors, migrating databases, installing vector
stores, implementing BM25, or activating Neo4j.

```text
implementation_authorized: false
```

## 2. Decision package

| ADR | File | Decision depth | Disposition |
|---|---|---|---|
| ADR-01 | [adr-01-primary-db-boundary.md](./adr-01-primary-db-boundary.md) | `BOUNDARY_DECISION_REQUIRED` | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| ADR-06 | [adr-06-vector-store-boundary.md](./adr-06-vector-store-boundary.md) | `BOUNDARY_DECISION_REQUIRED` | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| ADR-07 | [adr-07-bm25-boundary.md](./adr-07-bm25-boundary.md) | `BOUNDARY_DECISION_REQUIRED` | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| ADR-08 | [adr-08-neo4j-boundary.md](./adr-08-neo4j-boundary.md) | `BOUNDARY_DECISION_REQUIRED` | `BOUNDARY_FROZEN_ENABLEMENT_DEFERRED` |

Each ADR status is `PROPOSED_DECIDED_PENDING_REVIEW`.
None is `APPROVED`, `MERGED`, `IMPLEMENTED`, or `RUNTIME_VERIFIED`.

## 3. Control state that must remain true

```text
NC-CLOSE-01:                 NOT_AUTHORIZED as umbrella implementation
NC-CLOSE-01A ADR Decision:   MERGED_AND_VERIFIED
NC-CLOSE-01A implementation: NOT_AUTHORIZED
NC-CLOSE-01B ADR Decision:   PROPOSED_DECIDED_PENDING_REVIEW
NC-CLOSE-01B implementation: NOT_AUTHORIZED
NC-CLOSE-01C ADR Decision:   MERGED_AND_VERIFIED
NC-CLOSE-01C implementation: NOT_AUTHORIZED
NC-CLOSE-02 / 03:            MERGED_AND_VERIFIED
NC-CLOSE-04:                 PARTIALLY_VALIDATED
NC-CLOSE-05:                 RUNTIME_EVIDENCE_ACCOUNTING_COMPLETE
NC-CLOSE-06 / 07:            NOT_AUTHORIZED
A7-NC:                       COMPLETE
A7-CL:                       BLOCKED
Clinical Runtime:            NOT_ENABLED
Production:                  BLOCKED
Phase B:                     NOT_AUTHORIZED
```

## 4. Cross-ADR data ownership matrix

| Record / store | Current owner | Future owner | SoR? | Storage role | Retrieval role | Governing ADR |
|---|---|---|---|---|---|---|
| CDP / encounter state | Java `CDPManager` (transitional) | State Committer | Target yes | Application / clinical working state | none | ADR-02 + ADR-01 |
| Checkpoint / resume | No dedicated checkpointer; AgentState may co-locate | Python Runtime checkpoint adapter | **No** | Execution recoverability | none | ADR-02 / 04 / 05 |
| Clinical committed record | Transitional CDP writes | State Committer only | Yes | SoR | none | ADR-02 |
| Patient facts | CDP fields / designed Patient RAG | Isolated Patient RAG | Patient ≠ medical evidence | Patient-side | Patient RAG only | ADR-06 |
| Medical evidence chunks | Design / Capability draft | Medical Evidence RAG + KnowledgeRelease | Evidence authority is release/citation | Evidence corpus | Medical RAG | ADR-06 |
| Embeddings | `ABSENT` | Vector adapter (vendor deferred) | **No** | Index mechanism | Candidate recall | ADR-06 |
| Lexical index | `ABSENT` | Optional BM25 adapter | **No** | Lexical index | Candidate generation | ADR-07 |
| Knowledge graph | Neo4j clients (local/experimental) | Graph adapter; production default-off | **No** | Relationship navigation | Candidate / linkage | ADR-08 |
| Technical Span | `execution-trace-service` predecessor | OTel span (backend deferred) | **No** | Telemetry | none | ADR-10 / 12 |
| AgentEvent | Designed; AuditTrail mixed predecessor | Independent AgentEvent | **No** | Workflow evidence | none | ADR-12 |
| ClinicalDecisionRecord | Not implemented | Via State Committer | Decision record ≠ retrieval | SoR-adjacent | none | ADR-12 + ADR-02 |
| ComplianceAudit | `AuditTrail` mixed predecessor | Independent ComplianceAudit | Audit ≠ SoR | Audit store | none | ADR-12 |

Physical co-location of CDP / AgentState / related JPA rows is
`PHYSICAL_COLOCATION_PRESENT_OR_POSSIBLE`. Logical authority remains
`LOGICAL_AUTHORITY_SEPARATED_BY_ADR_02`.

No new schemas are invented.

## 5. Retrieval failure semantics

| Condition | Technical outcome | Must not mean |
|---|---|---|
| Vector store unavailable | `RETRIEVAL_UNAVAILABLE` / insufficient evidence | fabricated chunks |
| BM25 unavailable | `DEGRADED_VECTOR_ONLY` or `RETRIEVAL_UNAVAILABLE` | clinical fallback |
| Neo4j unavailable | `TECHNICAL_GRAPH_UNAVAILABLE` | `MEDICAL_NEGATIVE_RESULT` |
| Index stale | do not silently use as current evidence | current approved knowledge |
| KnowledgeRelease mismatch | fail closed for evidence use | mixed-release claims |
| Missing provenance | do not promote into EvidencePack-supported claim | cited medical evidence |

Never fabricate retrieval evidence. No clinical fallback is defined.

## 6. Evidence / provenance invariants

Storage technologies are mechanisms. Authority remains with the
evidence / release model.

Preserve:

- EvidencePack
- SourceArtifact
- KnowledgeReleaseRef
- claim-level citation / source-span relationship
- PatientDeliveryView visibility constraints

No backend score becomes patient-visible clinical evidence by default.
Shared Contracts v1 semantics are unchanged.

## 7. Cross-ADR compatibility

| Check | Result |
|---|---|
| ADR-01 does not reopen ADR-02; no second clinical writer | **Consistent** |
| ADR-01 does not select or migrate production primary DB | **Consistent** |
| ADR-03 public protocol remains versioned HTTP/REST JSON | **Consistent** |
| ADR-04 retrieval adapters do not become Runtime owner or absorb Model Runtime | **Consistent** |
| ADR-05 retrieval backend is not orchestration / LangGraph authority | **Consistent** |
| ADR-06 Patient RAG ≠ Medical Evidence RAG | **Consistent** |
| ADR-07 BM25 never replaces citation | **Consistent** |
| ADR-08 `NEO4J_IS_NOT_CLINICAL_GUIDELINE_AUTHORITY` | **Consistent** |
| ADR-10/11/12 stores remain distinct from retrieval / primary DB | **Consistent** |
| A7-NC not reopened | **Consistent** |
| Shared Contracts v1 unchanged | **Consistent** |

```text
Contradictions: 0
Package coherent: YES
```

## 8. Clinical / provider / PHI exclusions

```text
clinical source approval / medical gold / clinical thresholds = 0
real provider / credentials / network inference / embeddings = 0
patient data / PHI / production DB content inspection = 0
Phase B / State Committer implementation = 0
```

## 9. NC-CLOSE-06 stable inputs

Future NC-CLOSE-06 may **consume** these outputs. This package does
**not** author E2E design, coverage, or reconciliation.

- SoR vs checkpoint vs retrieval vs trace/audit ownership
- current relational / JPA path retained during Phase A
- concrete production DB vendor deferred
- vector retrieval abstraction frozen
- pgvector / Milvus concrete choice deferred
- Patient RAG distinct from Medical Evidence RAG
- BM25 optional and subordinate to citation
- Neo4j non-authoritative
- Neo4j production enablement deferred / default-off target
- unavailable / degraded retrieval semantics
- release / provenance invariants

## 10. Explicit no-implementation statement

Forbidden in this package and by this token:

- database migration or vendor selection
- PostgreSQL / pgvector / Milvus install
- BM25 implementation
- Neo4j production activation
- Flyway / Compose / `application.yml` / credential edits
- Shared Contracts v1 semantic change
- NC-CLOSE-06 / Phase B / OTel / Secret Manager implementation

## 11. Decision owner / next gate

Owner: Phase A NC Closure / NC-CLOSE-01B.  
State: `PROPOSED_DECIDED_PENDING_REVIEW`.

Next gate: Combined Independent Review + Merge Review of the exact
Draft Head. Do not mark Ready from authoring. Do not implement.
