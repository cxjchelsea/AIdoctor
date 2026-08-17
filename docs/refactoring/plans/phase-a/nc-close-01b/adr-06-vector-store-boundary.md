# ADR-06 Vector Store Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-06 |
| Title | pgvector / Milvus |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `2e5ab0c68e16a9b5d62aa2bdef250f5348d615c3` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01B` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze the **vector retrieval abstraction** and the authority split
between Patient RAG and Medical Evidence RAG.

This ADR does **not** select pgvector, Milvus, FAISS, or any other
vendor, install a vector database, generate embeddings, or approve
medical sources.

## 2. Context

Target Medical RAG design describes BM25 + pgvector + optional graph
index. Current runtime services do not implement a vector store.
Compose mentions `MILVUS_HOST` without a Milvus service or client.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| pgvector runtime / dependency | `ABSENT` | ten runtime services |
| pgvector in architecture docs | `DOCUMENTED` | `成人呼吸道RAG.md`, Freeze Candidate docs |
| `pymilvus` | `ABSENT` | no Python client |
| `MILVUS_HOST=milvus` | `CONFIG_ONLY` | `docker-compose.yml`; **no milvus service** |
| FAISS | `CURRENT_EXPERIMENTAL` | `DRKnows-main` trainers only; non-adopted runtime |
| Current authoritative vector store | `ABSENT` | — |
| Patient vs medical index mixing ban | `DOCUMENTED` | `成人呼吸道RAG.md` non-goals |

## 4. Problem

Selecting pgvector or Milvus now would invent a vendor without
technical adoption evidence and could collapse Patient RAG and
Medical Evidence RAG into one authority.

## 5. Architectural invariants

1. Vector storage is an **INDEX / RETRIEVAL MECHANISM**, not medical
   evidence authority, clinical SoR, or clinical decision authority.
2. Vector similarity ≠ approved medical evidence.
3. Patient facts ≠ medical evidence authority.
4. Even if one future physical backend hosts both corpora, namespace,
   access, release, provenance, and authority remain **logically
   separated**.
5. A vector hit alone cannot satisfy EvidencePack-supported claims.
6. Concrete backend is **DEFERRED**.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A6-A | Select pgvector now |
| A6-B | Select Milvus now |
| A6-C | Adopt DRKnows FAISS as runtime |
| A6-D | **Selected.** Freeze abstraction; defer concrete backend |

## 7. Selected decision

**A6-D — Vector retrieval boundary frozen; concrete backend deferred.**

### 7.1 Current role

No adopted runtime vector store.

### 7.2 Target role

A future retrieval adapter (inside future Python Runtime boundaries
where applicable) may host embeddings as a **candidate generator**.
It does not become SoR, citation, or clinical decision authority.

### 7.3 Patient RAG

Patient RAG may later retrieve patient-specific facts where
**separately authorized**. It must not promote patient-provided
content into approved medical evidence, source authority, or
guideline truth. This ADR does **not** implement Patient RAG.

### 7.4 Medical Evidence RAG

Medical Evidence RAG must preserve KnowledgeReleaseRef,
SourceArtifact, claim-level citation / provenance, EvidencePack
requirements, and applicability / limitations where required.

### 7.5 Enable / disable semantics

Vector retrieval is **optional**. If no backend is configured, the
system must expose `RETRIEVAL_UNAVAILABLE` / insufficient evidence,
not fabricate chunks.

## 8. Current permitted behavior

- Documented target design remaining design-only
- Later technical bake-off of pgvector / Milvus / other backends
- Shared physical hosting **only if** logical isolation is preserved

## 9. Current forbidden behavior

- Selecting or installing pgvector / Milvus / FAISS in this package
- Generating indexes or calling real embedding providers
- Merging Patient RAG and Medical Evidence RAG authority
- Using vector score as clinical truth or patient-visible evidence
- Approving medical sources or clinical gold to make this decision

## 10. Deferral metadata

| Field | Value |
|---|---|
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| Rationale | No runtime vector store exists; Compose Milvus is config-only; vendor selection is not required to freeze authority. |
| Future owner | Retrieval platform / later infra ADR |
| Future trigger | Technical bake-off with volume, latency, filtering, isolation, backup, topology, cost, and failure evidence |
| Required future evidence | data/index volume; latency; technical retrieval benchmark; metadata filtering; tenant isolation; backup/restore; version compatibility; deployment topology; operational burden; cost; failure behavior; migration strategy |
| Enablement gate | Explicit retrieval-adapter configuration; default unused until authorized |
| Stop condition | If selection requires medical-source approval, clinical gold, clinical semantics, or PHI: `PHASE_A_NC_CLOSURE_CLINICAL_BOUNDARY_REACHED` or `PHASE_A_NC_CLOSURE_PHI_BOUNDARY_REACHED` |
| Rollback / deactivation | Future backend must be disableable without inventing evidence |

Clinical source approval and clinical accuracy benchmarks are
**outside this lane** and are not required for this technical ADR.

## 11. Compatibility

- ADR-01: vector store is not the primary application DB.
- ADR-02: embeddings are not SoR.
- ADR-03: no protocol redesign.
- ADR-04: adapters may later live in `packages/python_runtime`; they
  must not absorb Model Runtime or reopen A7-NC.
- ADR-05: vector backend is not orchestration authority.
- ADR-07: hybrid retrieval remains optional; citation still required.
- ADR-10/12: vector DB is not Technical Span / AgentEvent /
  ClinicalDecisionRecord / ComplianceAudit.
- Shared Contracts v1: unchanged.

## 12. Explicit non-goals

Vendor selection; embedding calls; index generation; clinical ranking
policy; Patient RAG implementation.

## 13. Implementation consequences

```text
implementation_authorized: false
```

## 14. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01B.  
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
