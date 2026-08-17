# ADR-08 Neo4j Retention and Enable Gate

| Field | Value |
|---|---|
| ADR ID | ADR-08 |
| Title | Neo4j retention and enable gate |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `2e5ab0c68e16a9b5d62aa2bdef250f5348d615c3` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01B` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_ENABLEMENT_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze Neo4j as a **non-authoritative graph retrieval** mechanism and
defer production enablement behind an explicit gate.

This ADR does **not** activate Neo4j in production, change clients,
remove `KnowledgeGraphEngine.diagnose()`, or treat graph edges as
clinical guideline truth.

## 2. Context

Neo4j clients and Compose configuration exist. Graph-derived
possibilities can enter diagnosis-engine fusion. That is a structural
risk to record, not a reason to normalize Neo4j as clinical
authority.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| `diagnosis-engine` Neo4j driver | `CODE_CONFIRMED` | `app/kg-reasoning-engine/kg_client.py` |
| `knowledge-management` Neo4j client | `CODE_CONFIRMED` | `app/utils/neo4j_client.py` |
| Schema / import scripts | `CODE_CONFIRMED` | `scripts/setup_schema.py`, `import_all.py` |
| Compose `neo4j:5` | `CONFIG_VERIFIED` | `docker-compose.yml` |
| Default URI / password in settings | `CONFIG_PRESENT` | `bolt://localhost:7687` / `password` |
| `KnowledgeGraphEngine.diagnose()` | `CODE_CONFIRMED` | feeds `possibilities` into fusion |
| Connection-failure empty result | `CODE_CONFIRMED` | init catch returns empty engine |
| Production enablement | `UNKNOWN` | not verified |
| Graph as clinical guideline authority | **not approved** | this ADR forbids it |

## 4. Problem

Without an enable gate and a hard non-authority rule, graph paths can
be read as diagnosis, treatment, or red-flag truth. Empty-graph
failure can also be misread as a medical negative.

## 5. Architectural invariants

```text
NEO4J_IS_NOT_CLINICAL_GUIDELINE_AUTHORITY
```

Graph edges / paths / candidates MUST NOT by themselves authorize:

- diagnosis
- treatment
- red-flag decision
- clinical recommendation
- medical source approval

Graph output is at most: structured relationship support; navigation;
candidate generation; knowledge linkage; technical graph retrieval.

## 6. Current structural risk

```text
CURRENT_STRUCTURAL_CLINICAL_AUTHORITY_RISK
```

`KnowledgeGraphEngine.diagnose()` may feed graph-derived
possibilities into candidate fusion. This ADR records that fact. It
does **not** remove the code, change behavior, declare the path
clinically validated, or declare Neo4j authoritative.

Graph results remain **non-authoritative** and subject to future
evidence / clinical governance before any clinical consumption.

## 7. Alternatives considered

| ID | Alternative |
|---|---|
| A8-A | Enable Neo4j in production now |
| A8-B | Delete Neo4j from the repository now |
| A8-C | Treat graph paths as guideline truth |
| A8-D | **Selected.** Retain experimental/local clients; freeze non-authority; defer production enablement |

## 8. Selected decision

**A8-D — Enablement deferred; Neo4j is not clinical guideline
authority.**

### 8.1 Current role

Optional / local-compose graph client used by Python knowledge and
diagnosis-engine paths. Production occupancy is `UNKNOWN`.

### 8.2 Target role

Technical graph retrieval adapter with **default-off production
semantics**. Not SoR. Not guideline authority. Not Technical Span /
AgentEvent / ClinicalDecisionRecord / ComplianceAudit.

### 8.3 Production enable gate (DEFERRED)

Future production enablement requires **all** of:

- explicit configuration gate
- default-off production semantics
- technical validation
- graph release / version provenance
- failure isolation
- rollback
- security / access control
- operational ownership
- observability

Clinical approval is **not** a substitute for this technical gate.
Clinical governance remains separately required for any actual
clinical use and is **outside this ADR**.

### 8.4 Failure semantics

Current code may return empty possibilities on connection failure.

This ADR distinguishes:

```text
TECHNICAL_GRAPH_UNAVAILABLE
```

from

```text
MEDICAL_NEGATIVE_RESULT
```

Empty graph result MUST NOT mean: no disease, no risk, or no clinical
relation. Future target semantics should expose explicit
degraded / unavailable state. **That change is not implemented here.**

## 9. Current permitted behavior

- Existing local / experimental clients and Compose service
- Returning empty technical results on connection failure (current)
- Later enablement under the gate above

## 10. Current forbidden behavior

- Production Neo4j activation in this package
- Treating graph output as diagnosis / treatment / red-flag /
  guideline / source-approval authority
- Interpreting empty graph as medical negative
- Removing or “fixing” `diagnose()` in this docs package
- Using clinical approval instead of the technical enable gate

## 11. Deferral metadata

| Field | Value |
|---|---|
| Disposition | `BOUNDARY_FROZEN_ENABLEMENT_DEFERRED` |
| Rationale | Clients exist, but production enablement is unverified and graph-as-truth would violate clinical-authority rules. Phase A needs the gate and non-authority rule, not activation. |
| Future owner | Knowledge-graph platform / operations |
| Future trigger | Explicit production-enable authorization with the evidence list in §8.3 |
| Required future evidence | config gate; default-off; technical validation; release/version; isolation; rollback; security; ownership; observability; tests that empty graph ≠ medical negative |
| Enablement gate | Default-off; explicit config; no silent production on |
| Stop condition | If any proposal requires graph edges as authoritative clinical guideline truth: `PHASE_A_NC_CLOSURE_CLINICAL_BOUNDARY_REACHED` |
| Rollback / deactivation | Production path must return to default-off without changing SoR |

## 12. Compatibility

- ADR-01: Neo4j is not the primary application DB.
- ADR-02: graph is not SoR and not a second clinical writer.
- ADR-03/04/05: graph is not protocol, Runtime owner, or orchestrator.
- ADR-06/07: graph is optional retrieval, not citation replacement.
- ADR-10/12: Neo4j must not absorb Technical Span, AgentEvent,
  ClinicalDecisionRecord, or ComplianceAudit.
- A7-NC: not reopened.
- Shared Contracts v1: unchanged.

## 13. Explicit non-goals

Neo4j activation; client/behavior repair; clinical guideline
authoring; graph corpus approval.

## 14. Implementation consequences

```text
implementation_authorized: false
```

## 15. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01B.  
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
