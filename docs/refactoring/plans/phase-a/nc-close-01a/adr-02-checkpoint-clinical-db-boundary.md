# ADR-02 Checkpoint / Clinical DB Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-02 |
| Title | Checkpoint / Clinical DB Boundary |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `8a1a7691cdc3e95c5c9fd85e482c73073b5530f6` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01A` |
| Decision depth | `FULL_DECISION_REQUIRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Define durable ownership between:

- **A.** execution / checkpoint persistence
- **B.** authoritative application / clinical system-of-record (SoR) persistence

This ADR does not select a production primary database (ADR-01 remains a
soft input). It does not implement checkpoint storage, State Committer,
or schema migration.

## 2. Context

Target architecture documents describe Thread / Checkpoint / Resume as
durable-execution concerns, and CDP as the encounter-level clinical
working state. Current code already persists CDP, CDPVersion, AgentState,
and AuditTrail in Java. No LangGraph checkpointer exists.

`contracts/v1` already defines `StatePatch` as a **proposal** for a
future State Committer, not as an already-committed fact.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| CDP JPA entity and `CDPManager` write path | `CODE_CONFIRMED` | `diagnosis-service/.../entity/CDP.java`, `.../cdp/CDPManager.java` |
| CDPVersion copy-on-write snapshots | `CODE_CONFIRMED` | `CDPVersion.java`, `CDPVersionService.java` |
| AgentState session/strategy persistence | `CODE_CONFIRMED` | `AgentState.java`, `AgentStateManager.java` |
| AuditTrail persistence | `CODE_CONFIRMED` | `AuditTrail.java`, `AuditTrailManager.java` |
| `StatePatch` / `CommitResult` schemas | `CODE_CONFIRMED` | `contracts/v1/schemas/state-patch.schema.json`, `commit-result.schema.json` |
| StatePatch description: never an already-committed change | `CODE_CONFIRMED` | schema `description` |
| No LangGraph / checkpointer in Java or Python | `CODE_CONFIRMED` | repository-wide search = 0 |
| Durable Execution design | `DOCUMENTED` | `docs/refactoring/Durable Execution 与可观测性扩展方案.md` |
| Build / test / runtime of this decision | not claimed | no `BUILD_VERIFIED` / `TEST_VERIFIED` / `RUNTIME_VERIFIED` |

## 4. Problem

If execution-engine storage and clinical SoR storage are not separated,
resume artifacts can be mistaken for medical truth, retries can double-write
clinical fields, and future LangGraph checkpoint blobs can silently become
a second SoR.

## 5. Architectural invariants

1. Checkpoint / execution state is **never** clinical authority.
2. Authoritative encounter facts are committed only through a governed
   SoR write path. Target owner of that path is **State Committer**.
3. Current `CDPManager` is a **transitional** SoR writer, not a waiver of
   State Committer governance.
4. `StatePatch` is a proposal. `CommitResult` is the commit outcome.
5. Trace / AgentEvent / Audit records are not SoR substitutes.
6. ADR-01 (primary DB vendor) is not required to freeze this boundary.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A2-A | Single database table family for both checkpoint and CDP |
| A2-B | LangGraph checkpoint as clinical SoR |
| A2-C | **Selected.** Split execution/checkpoint store from SoR; State Committer is the only authoritative committer |
| A2-D | Keep Java `CDPManager` as permanent unique writer and never introduce State Committer |

## 7. Selected decision

**A2-C — Split execution/checkpoint persistence from authoritative SoR.
Checkpoint is recoverability only. Clinical/application truth is SoR-only.
Future State Committer is the only component allowed to commit
authoritative state.**

### 7.1 What belongs in execution / checkpoint persistence

Allowed:

- graph / workflow position
- interrupt / resume tokens
- retry / attempt counters
- lease / lock metadata for a run
- pending tool-call identifiers
- workflow / graph version
- binding to `cdp_id`, `base_version`, case/thread id
- non-authoritative working copies used only to resume execution

Forbidden as checkpoint-only authority:

- confirmed clinical observations
- DDx / triage / workup / management as committed truth
- Safety / red-flag determinations as committed truth
- patient-delivery claims

### 7.2 What belongs only in authoritative CDP / application storage

- committed encounter working state currently modeled on CDP
  (`patient_state`, `ddx`, `evidence_graph`, `workup_plan`,
  `management_plan`, `triage`, and peer SoR fields)
- CDPVersion history of committed SoR snapshots
- identity of the last successful authoritative commit

### 7.3 Is checkpoint authoritative for clinical facts?

**No.** Persistence inside a checkpointer, blob, or AgentState row does
not create clinical truth.

### 7.4 Who owns recovery / resume metadata?

Future **Python Runtime** owns execution/checkpoint adapters
(see ADR-04 / ADR-05). Java may remain the current production
orchestrator until a separately authorized migration. Ownership of
**resume metadata** is an execution-runtime concern, not a clinical-SoR
concern.

### 7.5 Binding requirements

Every checkpoint record MUST bind:

- conversation / case / thread identity
- `cdp_id`
- SoR `base_version` (or equivalent committed version)
- workflow / graph version
- release / provenance identifier of the executing package

Unbound checkpoint records are invalid and must not be resumed.

### 7.6 What may be reconstructed

- derived UI views
- technical spans / AgentEvents from SoR + audit + trace
- checkpoint working copies after a successful SoR commit

### 7.7 What must be durably persisted

- authoritative SoR commits (via State Committer in the target model)
- audit of those commits
- enough execution metadata to resume **or** to fail closed and restart
  from last committed SoR version

### 7.8 Conflict rule

If checkpoint state conflicts with authoritative CDP / SoR state:

```text
SoR / last CommitResult WINS
checkpoint becomes non-authoritative
resume may only rebuild a StatePatch proposal against current base_version
```

Silent overwrite of SoR from checkpoint is forbidden.

### 7.9 Who may commit authoritative state?

**Target:** only State Committer.
**Current transitional fact:** `CDPManager` / `CDPController` / field
writers perform SoR writes today. That is `CODE_CONFIRMED` debt, not
the selected target authority.

### 7.10 Future role of State Committer

State Committer is the unique authoritative writer. It accepts
`StatePatch` proposals, enforces Capability / policy / version /
idempotency, and emits `CommitResult`. Python Runtime, LangGraph,
tools, and Java orchestrators may **propose**, never directly become
SoR.

### 7.11 Idempotency / retry / recovery

- Reads may retry.
- Authoritative writes require `idempotency_key` (already on `StatePatch`).
- Recovery replays proposals against current `base_version`.
- Retry must not create a second clinical fact from the same logical
  commit.

### 7.12 Never-clinical-because-persisted rule

Information MUST NOT become clinical truth merely because LangGraph,
a checkpointer, AgentState, or a trace store persisted it.

## 8. Rationale

The repository already has a CDP SoR write path and a contract that
treats patches as proposals. The missing piece is an explicit ban on
treating future checkpoint storage as a second SoR. This can be decided
without choosing a database vendor.

## 9. Rejected alternatives

- **A2-A** mixes recoverability with medical authority.
- **A2-B** makes LangGraph storage clinically authoritative.
- **A2-D** freezes a transitional Java writer as the permanent model and
  conflicts with already-published `StatePatch` / State Committer
  contracts.

## 10. Tradeoffs

Separating stores adds operational complexity later. It prevents a
cheaper but unsafe “one blob is the truth” design. Transitional
`CDPManager` writes remain until a separately authorized
implementation.

## 11. Dependencies

- Soft input: ADR-01 (primary DB vendor / hosting).
- Consumed by: ADR-03 (what identities cross the protocol), ADR-04
  (checkpoint adapter location), ADR-05 (LangGraph may not write SoR).

## 12. Compatibility requirements

- Shared Contracts v1 `StatePatch` / `CommitResult` semantics unchanged.
- Existing CDP JSON field meanings are not redesigned here.

## 13. Migration implications

Implementation (not authorized now) must introduce checkpoint storage
and State Committer without migrating the production primary DB as a
hidden side effect. If a production primary DB change becomes necessary:

```text
PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED
```

## 14. Implementation consequences

Later authorized work may add checkpoint adapters and State Committer.
This ADR does not add them.

## 15. Deferred questions

| Question | Reason | Owner | Trigger | Evidence | Stop |
|---|---|---|---|---|---|
| Concrete checkpoint backend / table | vendor/hosting | ADR-01 / later infra ADR | 01B or implementation auth | deployment evidence | do not pick vendor here |
| Cutover date from CDPManager to State Committer | implementation | future 01A implementation | explicit implementation auth | design + tests | no silent cutover |

## 16. Explicit non-goals

Production DB selection; clinical field semantics; PHI production
policy; medical-source approval; implementing State Committer.

## 17. Architecture stop conditions

- Production primary DB migration required to state this boundary
- Shared Contracts v1 semantic change required
- Checkpoint declared clinical authority

## 18. Verification required before implementation / adoption

Independent Review of this ADR; later implementation requires its own
authorization, tests, and fail-closed conflict tests. No
`RUNTIME_VERIFIED` claim is made now.

## 19. Clinical / Provider / PHI exclusions

No clinical content, provider, or PHI policy is decided.

## 20. Relationships

- ADR-03 must propagate `cdp_id` + SoR version, not checkpoint blobs as
  truth.
- ADR-04 owns checkpoint adapters inside Python Runtime.
- ADR-05 forbids LangGraph SoR writes.
- A5: consume `StatePatch` / `CommitResult`; do not change them.
- A7-NC: Model Runtime outputs remain non-SoR until committed.

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01A.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.

## 22. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01A Draft PR.
Implementation remains `NOT_AUTHORIZED`.
