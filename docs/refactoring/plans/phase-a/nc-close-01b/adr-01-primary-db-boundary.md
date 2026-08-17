# ADR-01 Primary DB Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-01 |
| Title | Primary DB |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `2e5ab0c68e16a9b5d62aa2bdef250f5348d615c3` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01B` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze the **authority and ownership boundary** of application /
clinical working-state persistence.

This ADR does **not** select a production database vendor, migrate
Oracle or MySQL, implement State Committer, or create a checkpoint
database.

## 2. Context

ADR-02 already froze: checkpoint ≠ clinical SoR, and future State
Committer is the unique target authoritative clinical writer. ADR-01
is the remaining **physical / vendor** question. Repository evidence
does not establish one concrete production vendor as necessary to
close Phase A.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| Java JPA CDP write path | `CODE_CONFIRMED` | `CDPRepository`, `CDPManager.updateCDP` |
| CDPVersion / AgentState / AuditTrail JPA | `CODE_CONFIRMED` | matching repositories in `diagnosis-service` |
| Default profile comment: company Oracle | `DOCUMENTED` | `application.yml` `active: dev` |
| Oracle JDBC + `orai18n` | `DEPENDENCY_ONLY` | `diagnosis-service/pom.xml` |
| Oracle Flyway scripts | `DOCUMENTED` | `db/migration-oracle/` |
| In-repo `application-dev.yml` with JDBC URL | `ABSENT` | Enterprise tree has only `application.yml` |
| MySQL connector (local debug) | `DEPENDENCY_ONLY` | `mysql-connector-j` runtime scope |
| Local MySQL profile | `CONFIG_VERIFIED` | `examination-service` `application-mysql.yml` |
| Compose MySQL + `DATABASE_URL=jdbc:mysql` | `CONFIG_VERIFIED` | `docker-compose.yml` |
| Exact production physical vendor | `UNKNOWN` | not verified from repository alone |
| Python competing authoritative DB writer | `ABSENT` | no SQLAlchemy / pymysql SoR path found |
| Production occupancy | `UNKNOWN` | not inspected; PHI forbidden |

Do **not** convert this table into `Production DB = Oracle` or
`Production DB = MySQL`.

## 4. Problem

If Phase A required picking or migrating the production primary DB,
the lane would trigger Architecture Review and could collapse SoR,
checkpoint, retrieval, trace, and audit into one vendor story.

## 5. Architectural invariants

1. Clinical / application authoritative state ≠ checkpoint persistence
   ≠ retrieval store ≠ technical trace ≠ audit authority.
2. Physical co-location **MAY** exist. It does **NOT** merge logical
   authority.
3. `PHYSICAL_COLOCATION_PRESENT_OR_POSSIBLE` for current CDP /
   AgentState / related JPA rows. Logical authority remains
   `LOGICAL_AUTHORITY_SEPARATED_BY_ADR_02`.
4. Current Java `CDPManager` is a **transitional** SoR writer, not a
   waiver of State Committer.
5. No second independent clinical writer is created by this ADR.
6. Concrete production vendor is **not selected**.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A1-A | Select PostgreSQL / Oracle / MySQL as production primary now |
| A1-B | Migrate production primary during Phase A |
| A1-C | **Selected.** Freeze ownership/abstraction; retain current JPA path; defer vendor |
| A1-D | Treat Compose MySQL as proven production SoR |

## 7. Selected decision

**A1-C — Boundary frozen; concrete production backend deferred.**

### 7.1 Current role

Java JPA in `diagnosis-service` is the **current transitional**
writer of encounter working state (CDP and related application
records).

### 7.2 Target role

Future **State Committer** is the unique target authoritative
clinical / application committer (ADR-02). Python Runtime, tools,
LangGraph, retrieval backends, and Neo4j may **propose**, never
become SoR.

### 7.3 Authoritative / non-authoritative

| Store | Authoritative for clinical/application facts? |
|---|---|
| Current JPA CDP path | Transitional current yes; target no (State Committer) |
| Checkpoint / AgentState | **No** |
| Vector / BM25 / Neo4j | **No** |
| Technical Span / execution-trace | **No** |
| AuditTrail / ComplianceAudit | **No** (audit authority ≠ SoR) |

### 7.4 Enable / disable semantics

This ADR does not enable or disable a database. Existing persistence
may continue. New vendor enablement is out of scope.

## 8. Current permitted behavior

- Existing Java JPA / CDP persistence path
- Current physical DB arrangement
- Local / Compose MySQL configuration
- Documented Oracle-compatible dependency path
- Later State Committer integration under **separate** Phase B /
  implementation authorization

## 9. Current forbidden behavior

- Production DB vendor selection as a Phase A close condition
- Production schema or data migration
- State Committer implementation
- Checkpoint database implementation
- Creating a second clinical writer
- Treating Compose MySQL or Oracle JDBC as `PRODUCTION_VERIFIED`

## 10. Deferral metadata

| Field | Value |
|---|---|
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| Rationale | Repository evidence does not establish one concrete production vendor as necessary to close Phase A, and replacing the production DB is not needed to freeze authority boundaries. |
| Future owner | Platform / Data Architecture; Architecture Review if production primary changes |
| Future trigger | State Committer implementation proves a concrete physical-storage requirement; compliance; capacity/availability; managed-service; backup/recovery; production topology change |
| Required future evidence | deployment topology; backup/restore; migration strategy; compatibility; availability/recovery; rollback; security; operational ownership |
| Enablement / migration gate | Separate Architecture Review **before** any production primary replacement |
| Stop condition | If changing production primary DB becomes necessary: `PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED` |
| Rollback / deactivation | Not applicable to a deferred vendor; current path remains until a separately authorized cutover |

## 11. Compatibility

- ADR-02: not reopened. Checkpoint remains non-SoR.
- ADR-03: public Java↔Python protocol unchanged.
- ADR-04 / ADR-05: storage choice is not Runtime or LangGraph authority.
- ADR-10 / ADR-12: primary application DB is not Technical Span,
  AgentEvent, ClinicalDecisionRecord, or ComplianceAudit authority.
- Shared Contracts v1: unchanged.

## 12. Explicit non-goals

Vendor selection; Flyway/Compose/`application.yml` edits; data
movement; PHI inspection; clinical schema redesign.

## 13. Implementation consequences

```text
implementation_authorized: false
```

This ADR authors a boundary only. No database work is authorized.

## 14. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01B.  
State: `PROPOSED_DECIDED_PENDING_REVIEW`.  
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.
