# ADR-07 BM25 Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-07 |
| Title | BM25 |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `2e5ab0c68e16a9b5d62aa2bdef250f5348d615c3` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01B` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze BM25 as an **optional lexical retrieval** component and its
subordination to the evidence / citation model.

This ADR does **not** implement BM25, choose a library, or build a
lexical corpus.

## 2. Context

Target Medical RAG design lists BM25 beside pgvector. Current
repository runtime has no BM25 or other lexical retrieval
implementation.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| BM25 / `rank_bm25` / Whoosh / Elasticsearch in runtime | `ABSENT` | py / java / yml search |
| Lexical retrieval runtime | `ABSENT` | — |
| BM25 in architecture docs | `DOCUMENTED` | Freeze Candidate / `成人呼吸道RAG.md` |

## 4. Problem

Implementing BM25 now would add retrieval infrastructure without a
frozen optional/subordinate role, and could be mistaken for evidence
or citation authority.

## 5. Architectural invariants

1. BM25 is optional retrieval support only.
2. Permitted future roles: lexical candidate generation; hybrid
   retrieval support; ranking **support**.
3. Forbidden roles: evidence authority; citation replacement; clinical
   source approval; diagnosis / treatment / clinical decision
   authority.
4. Vector-only, lexical-only, hybrid, or degraded retrieval **never**
   makes citation optional.
5. Preserve EvidencePack, SourceArtifact, KnowledgeReleaseRef, and
   claim-level citation.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A7-A | Implement BM25 now to close Phase A |
| A7-B | Declare BM25 permanently forbidden |
| A7-C | **Selected.** Optional component; concrete implementation deferred |
| A7-D | Allow BM25 hits to satisfy EvidencePack citation |

## 7. Selected decision

**A7-C — BM25 optional; concrete implementation deferred.**

### 7.1 Current role

Absent.

### 7.2 Target role

Optional lexical adapter. Not an evidence authority.

### 7.3 Enable / disable semantics

BM25 is **optional**. Enablement is through a future retrieval
configuration / adapter. Default is unused until separately
authorized.

### 7.4 Failure / degraded semantics

If lexical retrieval is unavailable:

```text
DEGRADED_VECTOR_ONLY
```

when a vector component is available and authorized to run, or

```text
RETRIEVAL_UNAVAILABLE
```

when no authorized retrieval component remains.

Do **not** define a clinical fallback. Failure must not fabricate
evidence.

## 8. Current permitted behavior

- Leaving BM25 unimplemented
- Later adding a lexical adapter under separate authorization
- Technical degraded modes listed above

## 9. Current forbidden behavior

- Implementing BM25 in this package
- Replacing citation with lexical score
- Using BM25 as medical-source approval
- Inventing clinical fallback when the index is missing

## 10. Deferral metadata

| Field | Value |
|---|---|
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| Rationale | No runtime BM25 exists; Phase A only needs the optional/subordinate role frozen. |
| Future owner | Retrieval platform / later retrieval adapter ADR |
| Future trigger | Medical Evidence RAG implementation authorization that needs lexical candidate generation |
| Required future evidence | corpus/index versioning; disable path; degraded-mode tests that citation still holds; operational ownership |
| Enablement gate | Explicit retrieval-adapter flag; default unused |
| Stop condition | If BM25 is proposed as evidence/citation/source-approval authority: architecture / clinical boundary, fail closed |
| Rollback / deactivation | Must be switch-offable to `DEGRADED_VECTOR_ONLY` or `RETRIEVAL_UNAVAILABLE` |

## 11. Compatibility

- ADR-06: hybrid is allowed; neither backend is evidence authority.
- ADR-08: lexical index is not a knowledge-graph authority.
- ADR-02/03/04/05: BM25 is not SoR, protocol, Runtime owner, or
  orchestrator.
- Shared Contracts v1: unchanged.

## 12. Explicit non-goals

BM25 implementation; corpus construction; clinical ranking thresholds.

## 13. Implementation consequences

```text
implementation_authorized: false
```

## 14. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01B.  
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
