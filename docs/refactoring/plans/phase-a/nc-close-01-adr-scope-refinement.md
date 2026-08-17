# NC-CLOSE-01 ADR Foundation Scope Refinement

> Dated: 2026-08-17
>
> Classification: `DOCS-ONLY` / `PLANNING-ONLY` / `NO ADR OUTCOME`
>
> Lane: `PHASE_A_NC_CLOSURE`
>
> Parent batch: `NC-CLOSE-01` = ADR Foundation
>
> Exact Enterprise Base: `778d79420e59b8acb80cae0a2ef8a17ca2d68be2`
>
> PR #41 merge: `778d79420e59b8acb80cae0a2ef8a17ca2d68be2`
>
> A6.5: `LEGACY_GOVERNANCE_CLOSED` / Exit `PASS_LEGACY_GOVERNANCE_ONLY`

## 1. Purpose

Refine the internal structure and decision depth of the existing umbrella
batch `NC-CLOSE-01` into three logical sub-batches:

```text
NC-CLOSE-01A  Runtime & State Boundaries
NC-CLOSE-01B  Data & Retrieval Boundaries
NC-CLOSE-01C  Platform & Observability Boundaries
```

This is sequencing / decision-depth refinement only.

Machine-readable companion:
[nc-close-01-adr-refinement-register.csv](./nc-close-01-adr-refinement-register.csv)

## 2. Non-authorization statement

This artifact does **not** authorize or execute any ADR.

```text
NC-CLOSE-01:  NOT_AUTHORIZED
NC-CLOSE-01A: NOT_AUTHORIZED
NC-CLOSE-01B: NOT_AUTHORIZED
NC-CLOSE-01C: NOT_AUTHORIZED
A8 / A9 / A10 / A11: NOT_AUTHORIZED
implementation_authorized: false  (all 12 ADRs)
```

It does not select ADR outcomes, production backends, vendors, or
enablement. `UNKNOWN` is not a substitute for a later explicit defer
decision.

Future extension documents are **not** current requirements.

## 3. Authority split

| Artifact | Authority |
|---|---|
| [phase-a-non-clinical-closure-scope-register.csv](./phase-a-non-clinical-closure-scope-register.csv) | Authoritative for `IN_LANE` / `OUT_OF_LANE` classification |
| [phase-a-non-clinical-closure-roadmap-amendment.md](./phase-a-non-clinical-closure-roadmap-amendment.md) | Authoritative for lane purpose, exclusions, and umbrella batch IDs |
| This refinement + companion CSV | Authoritative **only** for `NC-CLOSE-01` internal sequencing and ADR decision depth |

Do not fork or replace lane-scope authority. Parent batch ID `NC-CLOSE-01`
is preserved. No ADR is removed from the umbrella.

## 4. Twelve-ADR inventory

The existing 12 ADR scope items remain in scope. None may disappear
because a concrete backend is deferred.

| adr_id | Title | refined_batch | decision_depth |
|---|---|---|---|
| ADR-01 | Primary DB | NC-CLOSE-01B | `BOUNDARY_DECISION_REQUIRED` |
| ADR-02 | Checkpoint / clinical DB boundary | NC-CLOSE-01A | `FULL_DECISION_REQUIRED` |
| ADR-03 | Java / Python protocol | NC-CLOSE-01A | `FULL_DECISION_REQUIRED` |
| ADR-04 | Python single Runtime package | NC-CLOSE-01A | `FULL_DECISION_REQUIRED` |
| ADR-05 | LangGraph usage | NC-CLOSE-01A | `FULL_DECISION_REQUIRED` |
| ADR-06 | pgvector / Milvus | NC-CLOSE-01B | `BOUNDARY_DECISION_REQUIRED` |
| ADR-07 | BM25 | NC-CLOSE-01B | `BOUNDARY_DECISION_REQUIRED` |
| ADR-08 | Neo4j | NC-CLOSE-01B | `BOUNDARY_DECISION_REQUIRED` |
| ADR-09 | Secret Manager mechanics | NC-CLOSE-01C | `BOUNDARY_DECISION_REQUIRED` |
| ADR-10 | OTel Backend | NC-CLOSE-01C | `BOUNDARY_DECISION_REQUIRED` |
| ADR-11 | AOP → OTel migration | NC-CLOSE-01C | `FULL_DECISION_REQUIRED` |
| ADR-12 | Legacy Trace / AgentEvent ownership | NC-CLOSE-01C | `FULL_DECISION_REQUIRED` |

## 5. Decision-depth definition

### 5.1 `FULL_DECISION_REQUIRED`

Phase A requires a durable architectural decision about ownership,
protocol, runtime role, or migration strategy.

It still does **not** authorize implementation.

### 5.2 `BOUNDARY_DECISION_REQUIRED`

Phase A must freeze:

- abstraction boundary
- ownership
- enable / disable semantics
- compatibility requirements
- stop conditions
- evidence required for future adoption

Phase A may explicitly defer:

- concrete vendor
- concrete hosted backend
- production migration
- production enablement

A legitimate later ADR result may be:

```text
BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED
```

That counts as an ADR decision only if the deferral records:

- rationale
- future decision owner
- trigger
- evidence requirement
- stop condition

Do not use `UNKNOWN` as a substitute for an explicit defer decision.

## 6. Sub-batch mapping

### 6.1 NC-CLOSE-01A — Runtime & State Boundaries

```text
ADR-02, ADR-03, ADR-04, ADR-05
Default depth: FULL_DECISION_REQUIRED
```

Soft dependency for NC-CLOSE-02 Bindings, NC-CLOSE-03 CI, and
NC-CLOSE-04 Workflow/Trace. Not a newly invented hard gate.

### 6.2 NC-CLOSE-01B — Data & Retrieval Boundaries

```text
ADR-01, ADR-06, ADR-07, ADR-08
Default depth: BOUNDARY_DECISION_REQUIRED
Concrete backend / production adoption: DEFERRED_WITH_EXPLICIT_GATE
  where evidence supports deferral
```

Does **not** block NC-CLOSE-02 by default.
Does **not** block most NC-CLOSE-03 work by default.
Must have valid ADR dispositions before `PHASE_A_NC_CLOSURE_EXIT`.

### 6.3 NC-CLOSE-01C — Platform & Observability Boundaries

```text
ADR-09  BOUNDARY_DECISION_REQUIRED
ADR-10  BOUNDARY_DECISION_REQUIRED
ADR-11  FULL_DECISION_REQUIRED
ADR-12  FULL_DECISION_REQUIRED
```

Must complete before the relevant NC-CLOSE-04 Trace / Observability
closure. Soft, evidence-based — not a new hard serialization of the
whole lane.

## 7. Dependency graph

```text
NC-CLOSE-01 umbrella
  → NC-CLOSE-01A
      soft → NC-CLOSE-02 Bindings
      soft → NC-CLOSE-03 CI
      soft → NC-CLOSE-04 Workflow / Trace
  → NC-CLOSE-01C
      must complete before relevant NC-CLOSE-04 Trace / Observability closure
  → NC-CLOSE-01B
      may proceed independently where safe
      required valid dispositions before PHASE_A_NC_CLOSURE_EXIT
```

Recommended current order:

```text
NC-CLOSE-01 umbrella
  → 01A
  → 01C / downstream structural work
  → 01B may proceed independently where safe
  → all required ADR dispositions before NC Closure Exit
```

Do not invent hard dependencies without evidence.
Do not mark any sub-batch authorized.

Existing register note retained as **soft** only: ADR-02 may consider
ADR-01 as a soft input. That does not make 01B a hard predecessor of 01A.

## 8. Per-ADR evidence expectations and guardrails

### ADR-01 Primary DB

- Freeze abstraction, ownership, compatibility, and stop conditions.
- Do **not** select a new production primary DB in this refinement.
- If changing the production primary DB becomes necessary:
  `PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED` and separate
  Architecture Review.
- A valid later ADR result may preserve the current production primary
  DB and defer migration, if the deferral fields in §5.2 are complete.

### ADR-02 Checkpoint / clinical DB boundary

- Define checkpoint persistence versus clinical system-of-record ownership.
- Do not decide clinical data semantics.

### ADR-03 Java / Python protocol

- Define the interoperability boundary.
- Do not reopen Shared Contracts v1 semantics.

### ADR-04 Python single Runtime package

- Define Python Runtime package ownership / layout boundary.
- Do not reopen completed A7-NC Model Runtime semantics.

### ADR-05 LangGraph usage

- Decide where LangGraph is allowed and where fixed workflow /
  deterministic control remains authoritative.
- Do not authorize autonomous unconstrained agents.

### ADR-06 pgvector / Milvus

- Do not force a vendor selection merely to close Phase A.
- Future concrete selection depends on technical evidence and retrieval
  requirements.
- Clinical source approval is outside this lane.

### ADR-07 BM25

- May be treated as an optional retrieval component.
- Must never replace evidence / citation requirements.

### ADR-08 Neo4j

- Must not become clinical guideline authority.
- Production enablement requires a separate enable gate and supporting
  evidence.

### ADR-09 Secret Manager mechanics

- Freeze secret-access mechanics / abstraction only.
- Do not authorize real provider credentials.
- Concrete Secret Manager vendor may be deferred.

### ADR-10 OTel Backend

- Freeze telemetry exporter / backend boundary.
- Do not require selection of a hosted OTel backend if deployment
  evidence is not yet available.

### ADR-11 AOP → OTel migration

- Require an explicit migration / bridge strategy between existing AOP
  tracing and OTel.
- Do not implicitly remove fallback / legacy observability before
  verification.

### ADR-12 Legacy Trace / AgentEvent ownership

Must distinguish ownership of:

```text
Technical Span
AgentEvent
ClinicalDecisionRecord
ComplianceAudit
```

Do not collapse these into one event model.

## 9. Architecture stop conditions

| Trigger | Stop |
|---|---|
| Production primary DB change required | `PHASE_A_NC_CLOSURE_ARCHITECTURE_BOUNDARY_REACHED` + Architecture Review |
| Shared Contracts v1 semantic change | `A5_CONTRACT_SEMANTIC_CHANGE_REQUIRES_SEPARATE_REVIEW` |
| Reopen completed A7-NC Model Runtime semantics | separate architecture review; not this lane |
| Real provider credentials / paid API / network inference | `PHASE_A_NC_CLOSURE_PROVIDER_BOUNDARY_REACHED` |
| Clinical activation, medical-source approval, PHI, clinical gold | `PHASE_A_NC_CLOSURE_CLINICAL_BOUNDARY_REACHED` or `PHASE_A_NC_CLOSURE_PHI_BOUNDARY_REACHED` |
| Neo4j treated as clinical guideline authority | architecture / clinical boundary; fail closed |
| Immediate Architecture Refreeze required to continue | stop; do not proceed under this refinement |

This refinement must not remove an existing Freeze blocker without an
equivalent gate.

## 10. Clinical / provider / PHI exclusions

Forbidden in this refinement and in any later ADR executed under this
lane without a separate authorized clinical track:

```text
Clinical Prompt / rules / thresholds / hypotheses
Medical source approval
Clinical gold
Patient data / PHI
Clinical Runtime enablement
adult_respiratory_v1 activation
A7-CL
Provider credentials / real provider / external model API
Phase B
Future Extension implementation
```

## 11. Recommended execution order

1. Keep `NC-CLOSE-01` as the umbrella ID.
2. When separately authorized, prefer 01A first (runtime / state /
   protocol / LangGraph role).
3. Advance 01C before relying on NC-CLOSE-04 Trace / Observability
   closure.
4. Allow 01B to proceed independently where safe; require valid
   dispositions before NC Closure Exit.
5. Do not create 01A / 01B / 01C implementation branches from this
   planning artifact.

## 12. Future review requirements

Any later ADR package under `NC-CLOSE-01` / 01A / 01B / 01C requires:

```text
Authorization Assessment
→ Explicit Authorization
→ Implementation (docs/ADR text only until separately authorized)
→ Independent Review
→ Merge Review
→ Explicit Merge Authorization
→ STANDARD MERGE COMMIT
→ Post-Merge Verification
```

This refinement PR's next gate is Combined Independent Review + Merge
Review of the exact Head. It does **not** authorize NC-CLOSE-01A.

After successful merge + PMV, the next authorization target is:

```text
NC-CLOSE-01A Authorization / ADR Decision
```

Not:

```text
NC-CLOSE-01 umbrella implementation authorization
NC-CLOSE-01B authorization
NC-CLOSE-01C authorization
A8 implementation authorization
```

```text
NC-CLOSE-01:  NOT_AUTHORIZED
NC-CLOSE-01A: NOT_AUTHORIZED until separate explicit authorization
NC-CLOSE-01B: NOT_AUTHORIZED
NC-CLOSE-01C: NOT_AUTHORIZED
```

## 13. Exit relationship

```text
Valid ADR dispositions for all 12 items
  are required before PHASE_A_NC_CLOSURE_EXIT.
```

Disposition may be a full decision or
`BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` with complete deferral
fields.

```text
This refinement != NC-CLOSE-01 AUTHORIZED
This refinement != A8 AUTHORIZED
PHASE_A_NC_CLOSURE_EXIT != Frozen Baseline
A11 eligibility != A11 PASS
```

## 14. Resulting control state after this planning change

```text
NC-CLOSE-01 / 01A / 01B / 01C: NOT_AUTHORIZED
A8 / A9 / A10 / A11: NOT_AUTHORIZED
A7-CL: BLOCKED
Clinical Runtime: NOT_ENABLED
Production: BLOCKED
Phase B: NOT_AUTHORIZED
Future New Clinical Track: NOT_AUTHORIZED
implementation_authorized: false
```
