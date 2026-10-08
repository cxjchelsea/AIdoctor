# U07 Initial Implementation Readiness / Gap Review v0.1

> Review date: 2026-10-08
> Repository: cxjchelsea/AIdoctor
> Integration base: main@6d4fd787600e3a57f01f3e17893e6d98893ac546
> Design review basis: review/u07-unit-spec-independent-design-v1@9fb8726421ffdb4ed8784560150afcb716f0b4f9
> Unit Spec design exact head: 9c899fdbe2136d88ed1d3bf5c2dd9b6d2b702272
> Scope: **readiness assessment and RDP blocker registration only**
> Verdict: **NOT_READY / IMPLEMENTATION_NOT_AUTHORIZED**
>
> This document does not authorize code implementation, upstream PR merge, production, PROFILE-A, live resume, release activation, PHI, or real-patient traffic.

## 1. Baseline and source separation

Repository main has U06 M1 and M2 merge lineage; U07 entry gate, Unit Spec, and independent design review are separate **open stacked PRs** #260, #261, #262. Therefore the reviewed U07 design is **not part of main**. The Unit Spec is DESIGN_CANDIDATE as authored, and the independent design review at #262 gives **PASS / NO_BLOCKER at Unit Spec design level**; this is not implementation readiness.

Inputs:
- [PR #260](https://github.com/cxjchelsea/AIdoctor/pull/260), U07 Entry Gate Assessment
- [PR #261](https://github.com/cxjchelsea/AIdoctor/pull/261), U07 Unit Spec v0.1
- [PR #262](https://github.com/cxjchelsea/AIdoctor/pull/262), Independent Design Review (PASS, 3 RDP carry-forward decisions)
- [main@6d4fd787](https://github.com/cxjchelsea/AIdoctor/commit/6d4fd787600e3a57f01f3e17893e6d98893ac546)
- Frozen Phase 5/6/7/8/9 and U06 RDP-03/04 are governing upstream references; exact cross-document field-by-field equivalence still requires the RDP/aggregate reviewers.

Evidence classification: `VERIFIED_SOURCE` means directly observed in a referenced artifact/code; `OPEN_DESIGN` means expressly deferred in Unit Spec/independent review; `NOT_DEMONSTRATED` means not proven by reviewed evidence, **not** a repository-wide absence claim.

## 2. Unit scope and invariants

U07 = 用户回答 Resume 与幂等恢复. 
```text
USER_ANSWER / RESUME_REQUEST -> canonical business event
 -> authoritative wait/Pending Question validation
 -> F8 Business Resume Decision (ACCEPTED/DUPLICATE/EXPIRED/REJECTED)
 -> P02 Runtime compatibility / rehydrate
 -> governed APPLIED / F3 and Pending Question consequences / Consultation ACTIVE
 -> one durable U02 handoff
```
Hard invariants:
- Business validity != Runtime resume compatibility.
- ACCEPTED answer != Clinical Fact; U02 owns interpretation and fact commit.
- Checkpoint != Clinical Truth; duplicate != stale-truth replay.
- F3 retains Question/Gap lifecycle ownership.
- No U05/U08 bypass; no ungoverned P01/K09 mutation.
- No real side effects or production activation under this review.

## 3. Code-level reuse inventory (main exact baseline)

| Existing artifact | Direct observation | Reuse vs gap |
|---|---|---|
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `resolveOrCreate(eventId, consultationId, eventType, idempotencyKey, payloadDigest)` performs same-identity checks and concurrent unique-key collision resolution; class explicitly excludes ACCEPTED/DUPLICATE/EXPIRED/REJECTED/APPLIED | REUSE canonical ledger; U07 event binding and F8 decision policy remain separate |
| `runtime/foundation/RuntimeWaitCheckpointService.java` | `initialize` / `reserve` enforce U06 wait checkpoint identity, with Thread state and serializable reservation | REUSE wait evidence; no U07 restore/rehydrate contract proven from this service |
| `runtime/u06/wait/U06WaitCoordinator.java` | Establishes U06 wait and projects eligibility after entering `AWAITING_USER` | UPSTREAM asset, not U07 answer consumer |
| `runtime/u06/wait/U07ResumeEligibilityProjector.java` | Stable eligibility derived from parent wait effect + checkpoint, requiring `threadAwaitingUser` | Identifier is not proof of current live business validity at later answer time |
| `runtime/u02/U02ClinicalFactApplicationService.java` and `U02ClinicalFactCommitService.java` | Existing downstream U02 entry/commit classes present in main tree | REUSE boundary, but exact U07-to-U02 durable payload/delivery not yet frozen |

Repository main tree contains `runtime/u01`–`runtime/u06` directories but no dedicated `runtime/u07` directory. This corroborates U07 governed-unit implementation not started; it does not prove all P02/Runtime capabilities absent globally.

## 4. Formal blocker register

All six categories are **P0 readiness design blockers**: required before a positive Implementation Readiness decision. This does not mean defects were found in the already-PASS Unit Spec.

### B-U07-RG-01 — Consumer Inbound / Canonical Event Admission

**Status:** OPEN_DESIGN / RDP-01 REQUIRED. **Evidence:** Unit Spec §§7–8 explicitly defer event ID/schema, same semantic answer/new ID policy, replay/currentness; independent review NB-U07-01 and NB-U07-03. CanonicalBusinessEventLedger is generic foundation, not U07 admission.

**Missing design / physical decisions:**
1. Exact typed inbound schema for USER_ANSWER vs RESUME_REQUEST; decide whether RESUME_REQUEST carries a prior accepted-answer ref or initiates a new event.
2. Define canonical identity/idempotency key construction, protected fields (consultation, question, eligibility, wait-effect, payload ref/digest, event time, scope), conflict rules, and same-answer/new-event-ID policy.
3. Bind U06 eligibility to *current authoritative* consultation, pending question, delivery provenance and wait effect rather than trusting an eligibility hash.
4. Validation order for auth/scope, malformed event, replay conflict, terminal/cancelled/expired state, and payload retention/PHI boundaries.
5. Physical controller/consumer + durable ledger transaction placement and test seams.

**Acceptance:** RDP-01 reviewed schemas + adapter design + deterministic identity table + conflict/replay matrix; rejected admission yields zero runtime resume, zero P01 mutation and zero U02 handoff; simultaneous same-key requests resolve to one canonical event.

### B-U07-RG-02 — F8 Business Resume Decision

**Status:** OPEN_DESIGN / RDP-02 REQUIRED. **Evidence:** Unit Spec §§9–10 explicitly defers precedence among DUPLICATE/EXPIRED/REJECTED and business-owner decision rules.

**Missing design / physical decisions:**
1. Frozen F8 authority, Policy/Rule ID, version and deterministic decision inputs/outputs.
2. Decision precedence matrix for already APPLIED, duplicate-in-flight, stale/other Question, wrong parent wait effect, superseded/expired Question, cancelled Consultation, mismatch of historically bound clinical version.
3. Explicit distinction between same transport event replay, semantically equivalent new event ID, conflicting answer and genuinely independent answer.
4. Stable ACCEPTED decision reference and its durability: Runtime failure cannot rewrite ACCEPTED to REJECTED.
5. Controlled compatibility with U15 expiry/cancel truth, with no unauthorized lifecycle takeover.

**Acceptance:** total/ordered F8 decision table with race and precedence tests; exactly one authoritative outcome; F8 never reads checkpoint health as business-validity verdict; replay preserves first historical ACCEPTED when relevant.

### B-U07-RG-03 — State Ownership / Mutation / Idempotent Apply / Trace

**Status:** OPEN_DESIGN / RDP-03 REQUIRED. **Evidence:** Unit Spec §12 and independent review NB-U07-02 defer exact APPLIED / ACTIVE / Pending Question / F3 / U02 choreography.

**Missing design / physical decisions:**
1. State/owner matrix mapped to concrete K09 patch schema, P01/G2 proposal, base version, optimistic CAS and CommitResult.
2. F3-owned Question ANSWER_RECEIVED and Gap ANSWERED bridge; U07 may coordinate but cannot become F3 writer.
3. Pending Question consume/clear semantics, Consultation WAITING_USER→ACTIVE, exact dependency ordering, atomic boundaries and intermediate externally visible states.
4. Stable effect IDs, canonical event + business decision + runtime + F3 + P01 + U02 trace/provenance keys.
5. Persisted apply journal/APPLIED evidence and reconciliation protocol when crash occurs after any individual effect; concurrent competing answers on same wait.

**Acceptance:** reviewed mutation/authority matrix, transition graph, durable effect journal, optimistic concurrency contract and crash-by-crash replay table; no dual APPLIED effects; no direct U07 Clinical Truth write.

### B-U07-RG-04 — P02 Runtime Resume / Checkpoint / Downstream

**Status:** OPEN_DESIGN / RDP-04 REQUIRED. **Evidence:** Unit Spec §11 and §12/13 defer status names, compatibility and choreography; RuntimeWaitCheckpointService demonstrates U06 **wait reservation**, not the full U07 recovery.

**Missing design / physical decisions:**
1. P02 resume command/result, checkpoint/run/thread identities and legal transitions; separate `RESUMABLE/RESUMED/RECONSTRUCTION_REQUIRED/INCOMPATIBLE/FAILED` semantics (exact names to freeze).
2. Compatibility with clinical state version, historical capabilities, contract/schema, scopes, current pending interaction and wait-effect provenance; no silent latest-binding substitution.
3. Missing/stale checkpoint repair from authoritative state + canonical event + ledger, without fabricating a business REJECTED.
4. Fencing/lease/concurrency and replay safety across resume, APPLIED and Consultation ACTIVE.
5. Durable U02 handoff, idempotent delivery/acknowledgement, Scheduler entry semantics, U14 failure and U15 cancellation/expiry ownership.
6. No U05/U08 shortcut and no accidental second U02 interpretation for same accepted answer.

**Acceptance:** end-to-end crash/recovery table with distinct Business and Runtime results; verified repair vs incompatibility policy; one durable U02 intent per applied event; failure never invents Clinical Facts.

### B-U07-RG-05 — Capability / Dependency / Applicability

**Status:** OPEN_DESIGN / RDP-05 REQUIRED. **Evidence:** Unit Spec §4 fixes P01/P02/P05/P06 and explicitly excludes Clinical AI/P03/P04 for U07 business decision.

**Missing design / physical decisions:**
1. Dependency registry and physical owner/adapter signatures: P01 state governance, P02 durable resume, P05 trace/audit, P06 scope/binding/version.
2. Applicability matrix across wait/eligibility age, pending question, consultation lifecycle, U15 cancellation, checkpoint repairability and historical binding compatibility.
3. Exact runtime available vs interface-only vs missing capability classification and authorized shared-runtime change allowlist; no automatic promotion of a synthetic profile to PROFILE-A.
4. Explicit fail-closed behavior for missing dependencies, invalid scopes, cross-consultation / cross-tenant refs, unsupported contracts and profile violations.
5. No new clinical LLM or knowledge dependencies introduced without controlled semantic amendment.

**Acceptance:** reviewed versioned dependency/compatibility manifest and concrete call graph; each required capability has a deterministic implementation owner, test double and integration test; missing P02 does not fall back to blind workflow resume.

### B-U07-RG-06 — Verification / Durable Evidence

**Status:** OPEN_DESIGN / RDP-06 REQUIRED. **Evidence:** Independent review §13 confirms 26 semantic acceptance scenarios and 10 regressions at Unit Spec level, explicitly requiring executable oracle, fixture and durable evidence contracts.

**Missing design / physical decisions:**
1. Executable scenario IDs for all 26 acceptance / 10 regression assertions, with expected result/status, prohibited effects and failure codes.
2. Authoritative independent oracle and synthetic fixture manifest pinned to exact HEAD, contract versions, reviewed RDP digests and executable hashes.
3. Concurrency, duplicate-before/after-APPLIED, wrong binding, stale eligibility, crash C1..C8, checkpoint repair, historical versions, U02 failure and exact-once handoff coverage.
4. Runner entrypoint, deterministic reproducibility, negative assertions (zero PHI, zero live/external IO, zero unintended writes), coverage threshold and fail-closed evaluator.
5. Durable evidence bundle, provenance, checksums, trusted execution environment, CI status and independent evidence-only review handoff.

**Acceptance:** runner + oracle + fixtures + manifest approved; all required cases pass on pinned implementation SHA with independently reviewable durable artifact; no test-only fixture promoted to real-patient authorization.

## 5. Dependency / closure sequence

```text
RDP-01 canonical event/admission
     ↓
RDP-02 F8 authority and decision precedence
     ↓
RDP-03 governed effect choreography ─┐
RDP-04 P02 resume / U02 handoff ─────┼─> RDP-05 complete dependency/applicability closure
                                     └─> RDP-06 verifiable evidence design
     ↓
Aggregate Compatibility Review / Controlled Amendment (if required)
     ↓
Implementation Readiness Re-Evaluation
     ↓
Explicit Implementation Authorization Decision
```

RDP-03 and RDP-04 may be designed in parallel but must be jointly reconciled before any authorization.

## 6. Separate findings: not blockers on U07 Unit Spec

- `NB-U07-01`: same semantic answer + new business event ID — resolve via RDP-01/02.
- `NB-U07-02`: APPLIED/ACTIVE/PendingQuestion/F3/U02 exact ordering — resolve via RDP-03/04.
- `NB-U07-03`: RESUME_REQUEST vs USER_ANSWER physical relation — resolve via RDP-01.
- U06 projector and Foundation ledger are **reusable upstream assets**, not proof of complete U07.
- A PASS of the independent Unit Spec review is not a PASS of RDP-01..06 or Production Readiness.

## 7. Decision

```text
U01-U06 completed within their accepted non-production baseline = SUPPORTED BY MAIN MERGE LINEAGE
U07 Unit Spec Independent Design Review = PASS / CLOSED (PR #262 review record)
U07 RDP-01..06 = OPEN / REQUIRED
U07 Aggregate Physical Compatibility = NOT_REVIEWED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
U07 Formal Implementation = NOT_STARTED AS GOVERNED UNIT
Production / PROFILE-A / live / real-patient = BLOCKED
```

**Next concrete work:** U07-RDP-01 Consumer Inbound / Event Admission Contract, using this review as the blocker baseline, followed by RDP-02 through RDP-06. Do not start U07 code or merge any PR solely on this assessment.
