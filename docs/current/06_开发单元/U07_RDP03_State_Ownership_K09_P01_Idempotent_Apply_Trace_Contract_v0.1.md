# U07-RDP-03 State Ownership / K09-P01 Mutation / Idempotent Apply / Trace Contract v0.1

> Unit: **U07 — User Answer Resume and Idempotent Recovery**
> Readiness gap: **B-U07-RG-03**
> Upstream RDP-01: [PR #266](https://github.com/cxjchelsea/AIdoctor/pull/266) — conditionally design-accepted in [PR #270](https://github.com/cxjchelsea/AIdoctor/pull/270)
> Upstream RDP-02: [PR #271](https://github.com/cxjchelsea/AIdoctor/pull/271) @ `3fe93aa2cacd2eb76e94984ae56a0967ac634ac5` — conditionally design-accepted in [PR #275](https://github.com/cxjchelsea/AIdoctor/pull/275) @ `56f28c286951ec1ec79a59aec5be776391017020`
> Source reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` (bounded directly inspected artifacts only)
> Status: **TARGETED_REMEDIATION_CANDIDATE / PENDING_INDEPENDENT_RE_REVIEW / NOT_FROZEN**
> Independent design review [PR #277](https://github.com/cxjchelsea/AIdoctor/pull/277) @ `253032f718d85a0bbbb073ccbaee0d04adf961e7`: REVISE_REQUIRED. Three design blockers addressed by this author-side amendment; none independently CLOSED.
> Scope: **STATE OWNER / EFFECT CLAIM / K09-P01 PROPOSAL / APPLY / COMPENSATION / TRACE DESIGN ONLY**
> No Runtime implementation, merge, PHI, PROFILE-A, clinical production, live patients, or external side-effect authorization.

---

## 1. Purpose and strict separation of authority

RDP-01 establishes a valid canonical answer identity and immutable synthetic payload. RDP-02 decides F8 business legality. **Neither may mutate Clinical State or mark downstream resume applied.** RDP-03 specifies the owner-coordinated consequences after the F8 decision, using the runtime restoration and delivery contracts that will be finalized separately in RDP-04.

```text
RDP-01 canonical USER_ANSWER identity
 → RDP-02 immutable F8 decision ACCEPTED
 → RDP-03 acquire same-wait effect authority + fresh terminal/currentness fence
 → RDP-04 P02 runtime compatibility/rehydration result (not authored by RDP-03)
 → RDP-03 owner-issued F3/Question + PendingQuestion K09 StateChangeProposal
 → P01/G2 authorized CommitResult + authoritative readback
 → governed Consultation WAITING_USER -> ACTIVE
 → APPLIED exact-effect reconciliation (only after required effects committed)
 → durable one-time U02 handoff intent / RDP-04 downstream transport
```

Required invariant:

```text
canonical event != F8 verdict != P02 runtime result
!= K09 proposal != P01 CommitResult != Clinical State
!= APPLIED != U02 clinical fact commit != Trace
```

No `DUPLICATE`, `EXPIRED`, `REJECTED`, pre-admission block, or operational F8 DEFER may initiate ordinary resume mutation. Historical F8 ACCEPTED is not a standing permission: each new apply attempt requires a **fresh versioned U15/Consultation/F3 state barrier**. A checkpoint failure must not rewrite the original F8 verdict.

## 2. Concrete baseline and non-equivalence

| Main artifact inspected | Observed fact | RDP-03 implication |
|---|---|---|
| `runtime/u01/ConsultationRecord.java` | `clinical_consultation`, lifecycle `ACTIVE` / `WAITING_USER`, `current_wait_effect_id`, `row_version @Version`; `enterWaitingUser` only | `WAITING_USER -> ACTIVE` resume method is **NOT DEMONSTRATED**; must design owner-controlled transition and CAS |
| `runtime/u01/ConsultationRepository.java` | `findByIdForUpdate` uses `PESSIMISTIC_WRITE` | Same Consultation lock is candidate for F8/U15 and apply; does not prove U15 integration |
| `runtime/u06/wait/ConsultationWaitTransitionService.java` | U06 enters WAITING_USER with Consultation row lock, `SERIALIZABLE`, and wait effect | U06 method **cannot** perform U07 resume; no blind reuse |
| `runtime/u06/state/U06SyntheticP01Runtime.java` | Injected `StateCommitter`, `StatePatch`, `ADD/REPLACE/REMOVE`, readback; U06-specific allowed paths | Reusable *pattern*, not a U07 writer. U07 path permissions, patch semantics, and durable replay must be independently authorized |
| `runtime/effects/CanonicalEffectLedger.java` | `inspect / createIfAbsent`, immutable generic effect record | Ledger is not P01 owner, scheduler, side-effect executor or cross-effect atomic commit |
| `U06_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md` | F3 owns Question/Gap, K09→P01/G2 commit + readback and durable effect evidence | U07 must request F3-owned consequences; never impersonate F3 |
| `U05_RDP03_State_Ownership_K09_P01_Mutation_Trace_Contract_v0.1.md` | Decision, stable effect/proposal, P01 CommitResult, authoritative readback and replay-first boundaries separated | Follow semantic model; not direct proof of U07 physical capability |
| `U07_Unit_Spec_v0.1` | Accepted answer not Clinical Truth; Runtime repair separate; APPLIED/ACTIVE/U02 handoff distinct | Preserve unit-level business invariants |

**Do not assert** that generic Foundation, U06 synthetic P01, F3 owner bridge, U15 shared fencing, P02 resume, clinical production P01 or exactly-once U02 transport already implement this entire U07 chain. Current physical completeness is NOT_VERIFIED.

## 3. Explicit state ownership matrix

| Object / fact | Authoritative owner | Allowed U07-RDP-03 activity | Forbidden |
|---|---|---|---|
| Canonical event ID and inline payload | Foundation/RDP-01 | reference/verify/reconcile | rewrite event or synthetic answer |
| F8 business verdict and same-wait winner | F8/RDP-02 | consume immutable ACCEPTED + claim generation | make new ACCEPTED or rewrite verdict |
| Question/Gap lifecycle | **F3** | ask F3 for reviewed answer-consumed decision and authorized effect proposal | direct `Question.ANSWER_RECEIVED` / `Gap.ANSWERED` assignment |
| Pending Question / Clinical State | P01/G2 with F3 owner policy | propose exact consume/update via owner-authorized K09 | blind REPLACE/CLEAR; direct repository write |
| Consultation lifecycle | Consultation lifecycle owner, P01 governance as applicable | request fenced `WAITING_USER -> ACTIVE` | mutation without owner lock/current wait ref |
| U06 delivery/wait provenance | U06 | verify/read persisted delivered parent wait | fabricate delivery or issuance |
| U15 cancel/expiry/terminal | U15 | versioned fresh read/fence; block effects | supersede or reinterpret terminal truth |
| Thread/Run/Checkpoint and runtime resume | P02 | consume immutable runtime outcome | self-rehydrate/retry unowned state |
| Canonical effect intent and application history | U07 effect coordinator under P01/F3 authorization | own idempotency/audit envelope | treat effect ledger row as Clinical State |
| User answer → observation/fact | U02/F2/G2 | create exact authorized handoff intent only | interpret text or advance clinical facts |
| Trace and audit | P05 | emit refs/typed outcomes | use trace as decision authority |
| Error escalation/repair | U14/D07 as applicable | record blocked/reconcile and request owner action | fake APPLIED, forced ACTIVE or retry side effects |

## 4. Frozen logical inbound to RDP-03

```text
U07ApplyRequestV1 {
 contract_version = "u07.apply.v1",
 canonical_answer_event_id, answer_payload_digest, trusted_scope_ref,
 question_id, pending_question_ref, parent_delivered_wait_effect_id,
 resume_eligibility_ref, consultation_id, thread_id, run_id, checkpoint_ref?,
 source_clinical_state_version,
 f8_decision_ref, f8_decision_fingerprint, f8_claim_generation,
 f8_decision_effective_at, f8_owner_version_evidence,
 u06_wait_owner_ref, u15_terminal_owner_ref, f3_question_owner_ref,
 historical_binding_refs,
 trace_id, correlation_id
}
```

Only a **durably committed** F8 `ACCEPTED` decision for the **same canonical answer ID** is eligible. Before effect claim, bind the RDP-01 canonical payload, RDP-02 winner and F8 decision IDs; enforce exact Consultation / Question / parent wait / scope / original Clinical version match. RDP-03 must not silently substitute newest Runtime bindings or accept input-supplied clinical state versions as authority.

F8 `DUPLICATE` of a *different canonical ID* may return read-only original applied reference, but must never be considered a new apply request.

## 5. Stable effect identity and conflict prevention

### 5.1 Deterministic identity scheme (logical design)

All identities are stable, owner-namespaced, versioned, canonical frame hashes; no trace ID, attempt number, transport alias, wall clock or mutable runtime checkpoint is incorporated:

```text
root_resume_effect_id =
  hash("u07-resume-effect-v1", consultation_id, question_id,
       parent_delivered_wait_effect_id, canonical_answer_event_id)

f3_answer_effect_id =
  hash("u07-f3-answered-v1", root_resume_effect_id, "f3-owner")

pending_consume_effect_id =
  hash("u07-pending-consume-v1", root_resume_effect_id, "p01")

consultation_active_effect_id =
  hash("u07-consultation-active-v1", root_resume_effect_id, "consultation-owner")

u02_handoff_effect_id =
  hash("u07-u02-handoff-v1", root_resume_effect_id, "u02")

proposal_id = hash("u07-k09-proposal-v1", effect_id, proposal_contract_version)
idempotency_key = hash("u07-k09-idempotency-v1", effect_id, proposal_contract_version)
```

Canonical framing requires typed fields, normalized IDs and exact version; hash algorithm/domain separators and schema registry registration are **RDP-05 dependencies**, not implied code. Original canonical ID is used for effects, not retry alias. **Same Question/wait but different accepted answer must not yield a second applied root**—the **wait ownership key** additionally enforces uniqueness, not only the root hash.

### 5.2 Effect/fingerprint rules

Store canonical immutable input fingerprint containing exact F8 decision ref/fingerprint, source owner versions, answer digest and historical refs. Same ID + changed payload/binding/proposal → `U07_EFFECT_REPLAY_CONFLICT`, fail closed. Same ID + same fingerprint → inspect durable effect stage and **reconcile**; do not create a new proposal by recomputing from current state.

Two levels of identity:
- `root_resume_effect_id`: exact accepted answer's stable intended application.
- `wait_apply_authority_key = (consultation_id, question_id, parent_delivered_wait_effect_id)`: only **one** root may ever hold ordinary apply/winner authority. This key is bound to the F8 winner and U15 fence; no second root may bypass it.

Existing `CanonicalEffectLedger` only records immutable generic effects and does **not** supply an atomic status mutation API. The durable U07 apply journal/unique constraints remain **new proposed physical surfaces**; do not represent ledger as an already-ready exact-once executor.

## 6. Apply state machine: no false APPLIED

```text
NOT_STARTED
  → CLAIMED
  → WAITING_RUNTIME_COMPATIBILITY
  → RUNTIME_RESUMED_VERIFIED
  → F3_OWNER_EFFECT_COMMITTED
  → PENDING_CONSUMED_COMMITTED
  → CONSULTATION_ACTIVE_COMMITTED
  → APPLIED_WITH_DURABLE_U02_INTENT     # one journal/outbox DB transaction
  → U02_HANDOFF_DISPATCHED
  → U02_HANDOFF_ACKNOWLEDGED (downstream progress only)

Any stage
  → BLOCKED_TERMINAL / BLOCKED_CONFLICT / RECONCILIATION_REQUIRED
```

**APPLIED criteria:** a single atomic journal/outbox transaction (§10.2 Tx E) may record `APPLIED_WITH_DURABLE_U02_INTENT` only after positive authoritative evidence of the **same** root effect for (a) successful/reconciled P02 resume, (b) F3-owned Question/Gap answer consequence when applicable, (c) P01-proven Pending Question consume and (d) Consultation ACTIVE transition with matching previous wait ref and committed version. No trace or effect-journal status alone can establish any of these facts. U02 fact commit is **not** required for U07 APPLIED. **In selected V1 there is no committed APPLIED stage without the identically committed unique U02 outbox intent**; delivery/consumer ACK can occur later, but U07 ordinary workflow closure remains contingent on the appropriate durable handoff confirmation. Independent P02 success or Consultation ACTIVE is insufficient to set APPLIED.

If F3/Question mutation is NOT_APPLICABLE by owner decision (not an absent owner), persist typed owner evidence. Every mandatory step is either proven committed or proven inapplicable; `UNKNOWN`, `FAILED`, runtime `INCOMPATIBLE` and blocked state cannot be elevated into APPLIED.

Status of F8 historical `ACCEPTED` never mutates into `REJECTED` due to a P02 or P01 failure.

## 7. F3 owner bridge and Pending Question mutations

### 7.1 Owner-issued decision before K09

```text
U07 accepted canonical answer + authoritative delivered Question
 → F3AnswerConsumptionDecisionV1 {
   owner = F3,
   canonical_answer_event_id,
   question_id, gap_id?,
   old_question_status = DELIVERED_TO_USER,
   next_question_status = ANSWER_RECEIVED?,
   next_gap_status = ANSWERED? | KEEP_OPEN?,
   pending_question_consume_intent,
   source_clinical_state_version, question_owner_version,
   evidence_refs, reason_code, decision_fingerprint
 }
 → K09 proposal issued only with verified F3 owner authorization
```

The *question status names are design values to reconcile against frozen F3 contracts*; a generic U07 mapper must not unilaterally pick `ANSWERED` for an ambiguous response or create clinical facts. An answer may be present without all Gap needs satisfied. A new F3 answer-consumption contract is **REQUIRED**, not proven implemented. `CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED` must either confirm reuse of approved F3 ownership or amend it under independent review before implementation.

### 7.2 Pending Question exact consume

No blind `REPLACE` or unconditional `REMOVE`. Candidate protected state shape:

```text
PendingQuestionConsumedV1 {
  previous_pending_question_ref,
  question_id,
  parent_delivered_wait_effect_id,
  canonical_answer_event_id,
  consumed_by_root_effect_id,
  old_state_fingerprint,
  f3_answer_owner_effect_ref,
  source_clinical_state_version,
  consumed_at_authority_ref
}
```

Under P01/G2 validation, CAS on the *exact old PendingQuestion ref* + Clinical version; if already consumed by the **same** root, exact replay is no-op with original CommitResult; if consumed by a different root or replaced/new Question, CONFLICT and governed reconciliation. Preserve historical answered ref rather than erasing clinical provenance.

### 7.3 K09 proposal: immutable design contract

```text
U07GovernedAnswerPatchV1 {
  proposal_id, idempotency_key, effect_id,
  contract_version, producer = "u07-f3-owner-bridge",
  scope / consultation_id / cdp_id,
  base_clinical_state_version,
  authorized_field_path_set,
  operations[] = {
    path, op, expected_current_value/ref,
    expected_owner_version, value,
    source = RULE_DERIVED, sensitivity = INTERNAL_SENSITIVE
  },
  f8_accepted_ref, f3_owner_decision_ref, u15_fence_ref,
  parent_wait_ref, original_answer_ref,
  evidence_refs, correlation_id, trace_id
}
```

Permissible candidate paths only after P01 field permission review:
- `/patient_state/questions/{question_id}` — F3 owner-only accepted answer lifecycle state;
- `/patient_state/information_gaps/{gap_id}` — F3 owner-only and only if applicable;
- `/patient_state/pending_question` — consume current **exact** pending pointer.

Actual path permissions cannot be inherited from `U06SyntheticP01Runtime` automatically; its producer and scope are U06-specific. No `/patient_state/clinical_observations`, Facts, Risk, Safety, DDx, readiness overwrite, or arbitrary dynamic path.

## 8. P01 commit / readback semantics

Every new proposed mutation must pass:
1. Trusted producer, capability/field authorization, correct Consultation/CDP/scope.
2. Durable F8 ACCEPTED and same-wait F8 winner bound to `root_resume_effect_id`.
3. **Fresh** shared Consultation/U15 owner generation, authoritative business wait currentness and F3 Question status.
4. RDP-04 P02 resume status committed and aligned with the same run/wait/history.
5. Correct source Clinical version and exact pending state compare-and-swap.
6. Immutable owner-issued proposal fingerprint, canonical effect ID, idempotency key, field path set, sensitivity/source and trace provenance.
7. Governed `StateCommitter.commit` through P01/G2; no direct CDP repository or uncontrolled in-memory mutation.
8. `CommitResult` plus authoritative state **readback** proving the intended effect/value under expected version.

| P01 result | U07 effect interpretation |
|---|---|
| `COMMITTED` and readback matches | mark exact owner effect COMMITTED |
| same-id exact replay/NO_OP with prior authoritative effect evidence | reconcile original; zero new version increment |
| `CONFLICT` | re-read owner facts; same-root commit if proven else BLOCKED_CONFLICT; no blind patch regeneration |
| `REJECTED` | no APPLIED; preserve reason/trace, request owner review |
| `FAILED` or timeout/unknown | assume uncertain; idempotent prior-effect lookup and readback before any retry |
| readback mismatch even when return says COMMITTED | QUARANTINE / RECONCILIATION_REQUIRED; no APPLIED |

Frozen P01 commit field privileges, patch operators, version and schema compatibility require explicit U07 readiness verification. Clinical State commit can advance version **only when an authorized new effect actually commits**. Same event replay never manufactures another version increment.

## 9. Consultation WAITING_USER → ACTIVE and the chosen cross-resource topology

**Selected V1 = `SINGLE_GUARD_DB_STAGED_SAGA_V1` (one physical design, not options).**

The workflow is a durable **staged saga**, with P02 owner execution independently reconciled, and F3/P01 owner commits followed by Consultation ACTIVE owner commit. There is **no transaction that spans P02 Runtime, Clinical State, Consultation and U02 delivery**. However, every *Clinical State or Consultation mutating stage* requires a proven, **same primary relational database and transaction manager** containing:
- authoritative `clinical_consultation` row/lock and terminal-fence generation shared by U15;
- P01/G2 Clinical State commit guard and the F3-governed Question/Gap/Pending Question data needed for **one atomic P01 patch**;
- U07 ApplyJournal, F8 wait winner reference, and the unique U02 outbox record.

P02 itself remains an external/runtime owner, not artificially enlisted into this DB transaction. U02 consumer is not assumed to share the DB. This selection is **conditional physical design only**: current inspected code establishes Consultation lock and a U06 synthetic StateCommitter *pattern*, **not** actual P01/U15 storage co-location or transaction manager compatibility. If P01/Consultation/U15/Journal are in distinct databases, **this V1 is NOT_APPLICABLE / U07 NOT_READY**, not “fall back to a loose saga” or emulate a distributed lock with a stale pre-check. Any alternative needs its own controlled amendment and independent review before implementation.

### 9.1 Frozen owner-resource map

| Step | Owner and transaction authority | Intent-before-effect and commit proof |
|---|---|---|
| A — root claim | U07 coordinator; shared guard DB with Consultation row lock | append immutable root/claim intent, unique `wait_apply_authority_key`, committed stage generation and F8 winner binding |
| B — P02 resume | P02 runtime authority; independent transaction and RDP-04 reconciliation | durable P02 command/request identity before invoking; exact P02 owner result/restore evidence afterward; unknown outcome must query P02, never blind replay |
| C — F3 Question/Gap + Pending consume | **one F3-owner-approved K09 StatePatch**, executed by P01/G2 **under same guard DB transaction + Consultation/U15 lock**, one Clinical version advance | stage-C intent + immutable F3 decision and K09 proposal committed before new mutation; one P01 authoritative CommitResult and readback bound to same effect/root |
| D — Consultation ACTIVE | Consultation lifecycle owner; separate staged transaction in same guard DB with lock and terminal epoch | prior D-intent, P01/F3 readback, compare exact WAITING_USER + wait ref + owner versions, ACTIVE owner effect committed with Consultation row version |
| E — APPLIED + U02 outbox | U07 coordinator; **one guard DB transaction** writing terminal ApplyJournal stage and unique durable outbox intent | no APPLIED without unique outbox; one `u02_handoff_effect_id` and immutable payload reference |
| F — U02 delivery/ACK | RDP-04 transport/U02 owner outside guard DB | dispatch outbox using same stable idempotency key; record target U02 receipt/ACK; clinical fact interpretation stays U02 |

**Transaction C is not two owner writes:** F3 owns the *semantic* Question/Gap transition and authorizes its decision; P01 owns the **single** authoritative K09 patch containing F3 Question, conditional Gap, and exact Pending Question CAS updates. This does not reassign F3 ownership to U07. The F3 bridge and P01 producer grant are **required upstream amendments** and must prove combined field permission, patch atomicity and readback. No cross-database F3 effect write can run out-of-band as a second authoritative Question mutation.

### 9.2 Resume Consultation owner command

```text
ConsultationResumeTransitionCommandV1 {
 consultation_id, exact_expected_lifecycle = WAITING_USER,
 exact_expected_current_wait_effect_id, expected_consultation_row_version,
 expected_u15_terminal_generation, expected_apply_stage_generation,
 f8_accepted_decision_ref, root_resume_effect_id,
 p02_resume_success_ref, f3_p01_owner_commit_ref,
 pending_consume_commit_ref, consultation_active_effect_id
}
```

D is legal only after verified P02 and stage C commit/readback. In the Consultation owner transaction, lock the same `clinical_consultation` row, validate current wait ref/status, F8 winner identity, committed U15 terminal generation and exact previous clinical owner effect. Change `WAITING_USER -> ACTIVE`, consume `current_wait_effect_id` through the authorized owner method, and commit a durable `consultation_active_effect_id` as a **same transaction** effect-evidence row. An exact same-root replay returns original effect; ACTIVE for another reason is CONFLICT.

The existing `ConsultationRecord.enterWaitingUser` **does not implement D**. The new transition and atomic owner-evidence row require independent contract/DB verification; no automatic activation after a P02 success.

### 9.3 Partial-stage irrevocability

Once stage C has committed, its authoritative Clinical State is **not presumed reversible**. If U15 terminalizes before stage D, mark saga `BLOCKED_TERMINAL_AFTER_C`, preserve C owner truth, inhibit D/E/F and raise U14/F3/P01 owner reconciliation. Do not reopen a new Question/Wait, fake an automatic rollback or grant a second winner. Only explicit owner-governed correction actions may compensate; such actions require separate stable IDs and authorization.

## 10. Selected durable saga, U15 fencing, and APPLIED/outbox closure

### 10.1 Required journal/outbox structure

```text
U07ApplyJournalV1 {
 root_resume_effect_id PK,
 wait_apply_authority_key UNIQUE, canonical_answer_event_id,
 f8_decision_id, f8_claim_generation, consultation_id, question_id,
 parent_wait_effect_id, claim_generation, apply_stage_generation,
 frozen_u15_terminal_generation, current_stage,
 p02_request_id + result_ref, f3_p01_proposal_id + commit_ref,
 clinical_state_version_after_c, consultation_active_effect_id + owner_version,
 u02_handoff_effect_id, outbox_ref, failure_class, trace_ref,
 created_at, updated_at
}
U07U02HandoffOutboxV1 {
 handoff_effect_id UNIQUE, root_resume_effect_id UNIQUE,
 canonical_answer_event_id, immutable_answer_payload_ref + digest,
 exact_question_parent_wait_refs, source_clinical_state_version,
 f8_decision_ref, p02_result_ref, applied_effect_refs, scope_ref,
 status = PENDING | CLAIMED | DELIVERED | ACKNOWLEDGED | RECONCILE_REQUIRED,
 dispatch_generation, consumer_idempotency_key,
 consumer_acceptance_ref?, created_at, updated_at
}
```

Neither table is claimed to exist on main. Storage = same guard DB and transaction manager as Consultation/U15 owner data. Outbox record must be encrypted/scope-protected as applicable, contain **only authorized references**, not raw answer text or new Clinical Fact.

### 10.2 Exact stage transaction protocol

**A: Claim.** Under Consultation/U15 common row lock, validate durable F8 ACCEPTED with winner+generation, legal wait and source refs. Persist unique root claim and stage-A intent in guard DB. If existing root returns same fingerprint, reconcile existing journal. Another root for same wait → blocked. U15 can terminalize only through shared lock.

**B: P02.** Persist stage-B request identity in guard DB **before external P02 invocation**, then invoke RDP-04 with same stable operation ID. Unknown call outcome requires P02 authoritative lookup. Only committed P02 success evidence advances to `RUNTIME_RESUMED_VERIFIED`. F8 remains ACCEPTED even if P02 ultimately fails.

**C: Single K09/P01 commit.** Persist C-stage intent and F3 owner decision/proposal identity before commit. Actual *owner mutation transaction* must (1) acquire the Consultation/U15 guard row lock FIRST, (2) revalidate F8 winner, U15 fence, Consultation WAITING_USER/current wait and Question/Pending owner versions, (3) verify P02 authority, (4) execute **one versioned P01 StatePatch** covering F3 Question/Gap + Pending consume and durable effect receipt under the same primary DB transaction, (5) commit. P01 transaction must be a true participant in this transaction manager; a separate HTTP/local synthetic P01 that commits elsewhere is **NOT_APPLICABLE**. The P01 authorized field/producer bridge and serial lock participation must be verified by RDP-05/06. Readback afterward must prove the exact root; otherwise `RECONCILIATION_REQUIRED`, not success.

**D: Consultation ACTIVE.** Persist D-stage intent; in a new transaction lock Consultation/U15 and verify all C owner readback/versions, same root/wait and no terminal generation change. Commit ACTIVE owner change **and its unique owner-effect evidence in one transaction**; readback and stage journal reconciliation follow. If terminal owner won before D commit, block D without falsifying prior C.

**E: Mandatory APPLIED + outbox atomic write.** Persist E intent stage in the journal while under shared lock. In **one physical guard DB transaction**, re-read all positive owner evidence (B/C/D), CAS `apply_stage_generation`, and insert `U07U02HandoffOutboxV1` with `handoff_effect_id = u02_handoff_effect_id` **in the same COMMIT that writes journal state `APPLIED_WITH_DURABLE_U02_INTENT`**. If duplicate row exists with matching immutable fingerprint, reconcile; mismatch = conflict/quarantine. Partial commit, missing outbox with APPLIED, or ambiguous result means fail closed and reconciliation, not a fresh handoff. Publication must happen only after committed outbox.

**F: Handoff dispatch.** RDP-04 outbox worker claims PENDING by CAS and delivers the **same** effect ID to U02. At-least-once network dispatch is acceptable *only with* independently verified U02 idempotent acceptance/replay query. ACK-lost → query same receipt or retry same ID, not generate another answer/fact. `ACKNOWLEDGED` does not mean Clinical Fact commit. Consumer-level exactly-once clinical effect requires its own U02/P01 proof, not inferred from this outbox.

### 10.3 Explicit terminal-fence commit predicate (BF-U07-RDP03-IR-03)

**Chosen guard:** a **shared Consultation row `PESSIMISTIC_WRITE` lock + monotonic `u15_terminal_generation` / Consultation `row_version`** across F8, U15 terminalization, stage C P01 commit, D Consultation transition and E APPLIED/outbox commit. The *mutation's transaction* must own this lock and compare generation **at commit**, not only an earlier RDP-03 admission or stage intent. U15 must lock and advance the same owner barrier in its own terminalization commit. This is a **proposed prerequisite**, not an observed U15 feature.

For every stage C/D/E:
```text
LOCK Consultation shared guard
  → owner facts fresh: consultation.status, current_wait_effect_id,
    f8_winner_ref, u15_terminal_generation, stage_generation
  → requires no authoritative expired/cancelled/superseded barrier
  → verify P01 owner/current Clinical version (C/D)
  → execute stage-specific mutation in SAME guarded transaction
  → persist owner receipt + generation/currentness witness
COMMIT (success or rollback-only; UNKNOWN → fresh exact-effect reconciliation)
```

Physical COMMIT is protected against a concurrently committed U15 mutation by this common lock, **provided P01 and U15 actually share the same transaction manager**. Owner-visible deadline passage without a U15 writer is also independently checked using certified primary DB **statement-current time** in C/D/E's final conditional mutation statement; the original RDP-02 statement-linearization rule is not silently repurposed as indefinite APPLIED permission. All deadline+time-domain details and MySQL/Oracle atomic conditional expression mapping must be proved. A precheck alone does not authorize a late write.

If U15 commits first → C/D/E must block new ordinary mutations; F8 prior ACCEPTED stays historical. If C commits first then U15 → C state is historical owner truth; D/E blocked; owner-governed reconciliation, no invented rollback or second winner. If D commits first then U15 → D owner truth preserved; E/F must not claim normal business delivery without separately reviewed terminal currentness. All uncertain commit/replay outcomes use immutable root evidence, not optimistic APPLIED.

**Physical dependency:** `CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED`. It extends (not silently satisfies) `CA-U07-RDP02-U15-SHARED-FENCE-01` with exact P01 same-transaction participation and deadline/fence predicates. RDP-05 must verify Consultation/P01/F3/U15 resource locality; otherwise V1 is NOT_APPLICABLE and Implementation Readiness remains NOT_READY.

### 10.4 Crash/reconciliation table

| Crash/race | Frozen response |
|---|---|
| A committed, crash before B | one journal claim; reattach P02 same request |
| B request emitted, P02 outcome unknown | P02 owner lookup by request ID, no second physical resume |
| P02 completed, B receipt missing | reconcile P02 result, write same B evidence |
| C effect commit succeeds but ack lost | query P01 exact commit and authoritative Clinical State readback; no second version advance |
| D intent committed, U15 terminalizes before D | D transaction sees newer terminal generation → blocked; C retained |
| D ACTIVE owner-effect commit succeeds, journal write lost | read Consultation/effect ID, reconcile stage; no second ACTIVE |
| **E outbox insert succeeds, E journal APPLIED write fails** | **same transaction rolls back both**; no orphan outbox/false APPLIED |
| **E journal APPLIED succeeds, outbox insert fails** | **same transaction rolls back both**; no lost handoff |
| E COMMIT unknown | fresh transaction inspect journal and outbox by stable root/handoff IDs; no optimistic APPLIED |
| APPLIED committed, outbox dispatch not started | durable PENDING outbox drives recovery |
| Outbox dispatched, U02 receipt/ACK lost | retry/query same consumer idempotency; no second handoff ID |
| U15 terminalizes between C precheck and actual P01 commit | impossible to pass stage C guard if U15 acquired Consultation lock first; physical mismatch blocks implementation |
| U15 terminalizes between C and D | block D; retain C; owner correction only |
| U15 terminalizes between D and E | E checks current terminal fence; no APPLIED/outbox publication; owner reconciliation |
| Two roots racing same wait | unique wait key + shared lock preserve first F8 winner/root; loser blocked |
| Different DB/transaction manager detected | V1 NOT_APPLICABLE / NOT_READY, no optimistic partial mutate |

### 10.5 Invariants and physical-evidence gap

- No arbitrary compensation: correction of committed Question/Pending or Consultation facts requires F3/P01/Consultation owner authorization and a new explicit effect; do not erase historical evidence.
- No global exactly-once transport claim: producer outbox ensures one logical handoff intent, U02 acceptance must be idempotent and verified.
- No `APPLIED` without same-commit durable unique U02 intent; no handoff before APPLIED commit; no new state mutation after a U15 terminal win.
- Stage progression is CAS monotonic; a journal `APPLIED` is **not itself proof** of clinical fact commit or owner state; readback refs must be verified.

This is an *exact selected V1 design*, not proof of actual one-DB P01/Consultation/U15 colocation, atomic patch support, dialect-specific SQL or existing consumer idempotency. These are **hard readiness blockers**, not optional enhancements.

## 11. P05 Trace / evidence contract

```text
U07ApplyTraceRecordV1 {
 schema_version,
 trace_id, correlation_id, consultation_ref, cdp_ref,
 canonical_answer_event_id, event_binding_fingerprint,
 f8_decision_ref, f8_verdict, f8_claim_generation,
 f8_decision_effective_at, temporal_authority_ref,
 parent_wait_effect_ref, question_ref, pending_question_ref,
 u06_eligibility_issuance_ref,
 source_clinical_state_version,
 u15_owner_terminal_ref + fence_generation,
 p02_runtime_result_ref + checkpoint_reconcile_ref,
 root_resume_effect_id, wait_apply_authority_key,
 f3_answer_owner_decision_ref + effect_commit_ref,
 pending_consume_proposal_ref + p01_commit_ref,
 consultation_active_effect_ref + owner_version,
 applied_stage + applied_decision_evidence_ref,
 u02_handoff_effect_id + outbox_ref + transport_ack_ref,
 retry_attempt_ref, cause/effect_refs, failure_class,
 evidence_collection_time
}
```

Trace must link **input → owner decision → proposal → CommitResult → authoritative readback → apply stage → handoff** and record which facts were verified vs pending. It is not a state store or proof of owner state by itself. PHI minimization: no raw answer, no patient narrative, use scope-protected refs and digests, no disclosure via duplicate response.

P05 trace persistence failure before a required owner mutation => **fail-closed** if mandatory audit evidence cannot be durably reconstructed. A later asynchronous trace presentation failure cannot rewrite committed clinical state; reconcile through owner journal/evidence.

## 12. Mandatory upstream / downstream compatibility and blockers

| Dependency | State | Required evidence for positive readiness |
|---|---|---|
| RDP-01 canonical event design | CONDITIONALLY_ACCEPTED | Foundation exhaustive SHA-pinned source impact scan |
| `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` | REQUIRED / NOT_PASSED | independent compatibility verdict |
| `CA-U06-U07-ELIG-ISSUANCE-01` | REQUIRED / NOT_AUTHORIZED | authoritative issuance/retrieval, not hash-only reconstruction |
| RDP-02 F8 business decision | CONDITIONALLY_ACCEPTED | physical atomic conditional SQL and same-wait winner proof |
| `CA-U07-RDP02-U15-SHARED-FENCE-01` | REQUIRED / NOT_AUTHORIZED | U15 owner uses approved shared Consultation/U15 fencing |
| `CA-U07-RDP03-F3-ANSWER-BRIDGE-01` | REQUIRED / NOT_AUTHORIZED | exact F3 owner answer/Question/Gap/Pending policy and permissions |
| U07 producer/path authorization, single K09 StatePatch and P01 readback | NOT_VERIFIED | F3-authorized combined Question/Gap/Pending mutation; schema/field/owner/CAS tests, exact same-transaction guard evidence |
| Consultation ACTIVE transition + same-root replay | NOT_IMPLEMENTED | same guard DB owner API, one-commit effect receipt, DB and concurrency evidence |
| RDP-04 P02 resume / U02 handoff | FUTURE DESIGN | runtime success/recovery/outbox owner contracts |
| `CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01` | REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED | same-DB/transaction-manager Consultation lock + P01 and U15 commit-time/clock fence proof |
| RDP-05 dual-dialect / atomicity / registry | FUTURE DESIGN | prove selected SINGLE_GUARD_DB_STAGED_SAGA_V1 and MySQL/Oracle conditional statement/clock tests |
| RDP-06 verification | FUTURE DESIGN | exact-head oracles, four gate/evidence stages |

**No standalone RDP-03 design PASS removes any of these implementation blockers.**

## 13. Design-only verification oracle matrix

| ID | Scenario | Expected owner/effect outcome |
|---|---|---|
| U07-A03-01 | F8 not ACCEPTED | no ApplyJournal effect, no Runtime, no P01 mutation |
| U07-A03-02 | F8 historical ACCEPTED but U15 now terminal | no new ordinary effect, governed blocked reconciliation |
| U07-A03-03 | valid ACCEPTED + P02 verified + owner state current | exactly one F3/P01/Consultation effect chain |
| U07-A03-04 | identical same-canonical replay before apply | same root/effect IDs, no second owner mutation |
| U07-A03-05 | duplicate after APPLIED | original applied evidence, zero new effects |
| U07-A03-06 | same wait different answer event | second root cannot claim or apply |
| U07-A03-07 | changed digest under same root | fail-closed fingerprint conflict |
| U07-A03-08 | Pending Question replaced by newer Question | P01 conflict, no blind clear |
| U07-A03-09 | F3 Question owner decision absent | blocked dependency, no fabricated ANSWER_RECEIVED |
| U07-A03-10 | F3 Gap still incomplete after answer | do not mark Gap ANSWERED by U07 guess |
| U07-A03-11 | P01 COMMITTED but readback mismatch | QUARANTINE, no APPLIED |
| U07-A03-12 | P01 CONFLICT from stale Clinical version | reload/reconcile, no new blind patch |
| U07-A03-13 | P01 successful exact replay | zero additional Clinical version increment |
| U07-A03-14 | P02 failure after F8 ACCEPTED | F8 remains ACCEPTED, no APPLIED |
| U07-A03-15 | P02 resume succeeded then crash | reconcile same P02 owner result, not second resume |
| U07-A03-16 | Question mutation committed then crash | same-root owner-effect readback, no second Question mutation |
| U07-A03-17 | Pending consumed before ACTIVE then U15 cancel | block ACTIVE; protected reconciliation |
| U07-A03-18 | Consultation ACTIVE already from different effect | terminal conflict, no overwrite |
| U07-A03-19 | committed owner effects, crash before APPLIED | reconcile all exact refs, one APPLIED |
| U07-A03-20 | APPLIED/outbox interrupted | Tx E atomic rollback-or-commit together; never APPLIED without PENDING outbox |
| U07-A03-21 | U02 ack lost | retry/query stable consumer idempotency ID; no **claim** of exactly-once Clinical Fact absent U02 evidence |
| U07-A03-22 | concurrent U15/F8/U07 apply | shared fence respects owner terminal commit order |
| U07-A03-23 | Clinical State/P01 and Consultation in separate DB or transaction manager | selected V1 NOT_APPLICABLE / NOT_READY; no fallback to unfenced loose saga |
| U07-A03-24 | F3 bridge or U15 shared fence not authorized | design-only; implementation blocked |
| U07-A03-25 | same wait answer payload digest but other Question | no cross-Question effect deduplication |
| U07-A03-26 | PHI-bearing payload/production profile | blocked; synthetic-only |
| U07-A03-27 | trace evidence missing before required commit | fail-closed or owner-authorized recovery; no fake commit |
| U07-A03-28 | replay old ACCEPTED after U15 expiry | historical verdict unchanged, no unauthorized new effect |
| U07-A03-29 | stage C F3/Question/Pending single P01 patch, exact same-root retry | one Clinical version advance, one durable owner effect and identical replay proof |
| U07-A03-30 | stage C P01 guard sees U15 cancellation committed between intent and transaction | no P01 mutation; no APPLIED; F8 historical ACCEPTED retained |
| U07-A03-31 | U15 cancels after C but before D | C preserved as authority; D blocked, owner-led correction only |
| U07-A03-32 | U15 cancels after D but before E | E blocked with no APPLIED/outbox; retain D owner evidence |
| U07-A03-33 | Tx E outbox INSERT succeeded but journal APPLIED update fails | single DB transaction rolls back both |
| U07-A03-34 | Tx E APPLIED update succeeded but outbox INSERT fails | single DB transaction rolls back both |
| U07-A03-35 | Tx E commit unknown | independently query both journal/outbox same stable IDs, no optimism |
| U07-A03-36 | P02 or U02 remote response lost | owner lookup/replay with stable request/handoff ID, no new physical identity |
| U07-A03-37 | P01 separate transaction manager cannot join Consultation/U15 guard | V1 NOT_APPLICABLE and readiness blocked |
| U07-A03-38 | shared Consultation lock held; deadline passes before final P01 conditional statement | stage C/D/E rejects new mutation on certified DB statement-time predicate |

All cases are **unexecuted design oracles**. Future RDP-06 must attach exact migration/source SHAs, executable tests, negative-side-effect proofs, both DB dialect evidence and independently checked provenance.

## 14. Independent design review questions

```text
IR-U07-RDP03-01  Does the F3 owner answer-consumption bridge preserve F3 Question/Gap ownership?
IR-U07-RDP03-02  Can the exact K09/P01 patch and field permissions be authorized without reusing U06 grants?
IR-U07-RDP03-03  Is the chosen ApplyJournal/effect ID sufficient to prevent a second same-wait apply?
IR-U07-RDP03-04  Which *one* atomicity topology governs P01, Consultation ACTIVE, APPLIED and U02 outbox?
IR-U07-RDP03-05  Can an ACCEPTED winner be terminally blocked without rewriting F8 or losing prior writes?
IR-U07-RDP03-06  Is the APPLIED definition stable under P02 crash/repair and F3/P01 partial commit?
IR-U07-RDP03-07  Are U15 common fence, U06 issuance and RDP-01 Foundation audit still authoritative blockers?
IR-U07-RDP03-08  Does P05 record sufficient owner evidence without using Trace as clinical truth?
IR-U07-RDP03-09  Are U02 exactly-once delivery and Clinical Fact interpretation clearly separate?
IR-U07-RDP03-10  Are existing P01/Consultation assets real, and are missing adapters labeled unimplemented?
IR-U07-RDP03-11  Does the selected SINGLE_GUARD_DB_STAGED_SAGA_V1 bind every mutating stage to one verified primary DB/transaction manager?
IR-U07-RDP03-12  Is one F3-authorized P01 Question/Gap/Pending patch truly atomic, with no owner takeover?
IR-U07-RDP03-13  Does Tx E commit APPLIED and unique outbox PENDING in the same guard DB transaction, with exact crash recovery?
IR-U07-RDP03-14  Does U15 terminalization fence stage C/D/E **inside** owner commit and include independent statement-time deadline checks?
IR-U07-RDP03-15  Are both new and inherited controlled amendments explicitly blocking Implementation Readiness?
```

## 15. Formal candidate status / next gate

```text
U07-RDP-03 = TARGETED_REMEDIATION_CANDIDATE / READY_FOR_TARGETED_INDEPENDENT_RE_REVIEW / NOT_FROZEN
BF-U07-RDP03-IR-01..03 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
B-U07-RG-03 = OPEN / NOT_CLOSED
U07-RDP-01 = CONDITIONALLY_ACCEPTED_DESIGN
U07-RDP-02 = CONDITIONALLY_ACCEPTED_DESIGN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

Next gate: **U07-RDP-03 Targeted Independent Design Re-Review** against this amended exact design HEAD. Check §9–10 for one enforceable physical topology, terminal-fenced stage C/D/E owner commits, and atomic APPLIED+outbox. Require positive independent confirmation before closing BF-01..03. No runtime edits, merge, patient flows, clinical activation, or authorization under this document.
