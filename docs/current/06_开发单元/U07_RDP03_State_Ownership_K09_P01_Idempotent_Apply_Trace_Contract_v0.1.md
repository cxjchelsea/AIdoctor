# U07-RDP-03 State Ownership / K09-P01 Mutation / Idempotent Apply / Trace Contract v0.1

> Unit: **U07 — User Answer Resume and Idempotent Recovery**
> Readiness gap: **B-U07-RG-03**
> Upstream RDP-01: [PR #266](https://github.com/cxjchelsea/AIdoctor/pull/266) — conditionally design-accepted in [PR #270](https://github.com/cxjchelsea/AIdoctor/pull/270)
> Upstream RDP-02: [PR #271](https://github.com/cxjchelsea/AIdoctor/pull/271) @ `3fe93aa2cacd2eb76e94984ae56a0967ac634ac5` — conditionally design-accepted in [PR #275](https://github.com/cxjchelsea/AIdoctor/pull/275) @ `56f28c286951ec1ec79a59aec5be776391017020`
> Source reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` (bounded directly inspected artifacts only)
> Status: **DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN**
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
  → APPLIED
  → U02_HANDOFF_ENQUEUED
  → U02_HANDOFF_ACKNOWLEDGED (downstream progress only)

Any stage
  → BLOCKED_TERMINAL / BLOCKED_CONFLICT / RECONCILIATION_REQUIRED
```

**APPLIED criteria:** positive authoritative evidence of the **same** root effect for (a) successful/reconciled P02 resume, (b) F3-owned Question/Gap answer consequence when applicable, (c) P01-proven Pending Question consume and (d) Consultation ACTIVE transition with matching previous wait ref and committed version. No trace or effect-journal status alone can establish any of these facts. U02 fact commit is **not** required for U07 APPLIED, but durable one-time U02 handoff eligibility is required before U07 declares workflow closure; `APPLIED` may precede independently delivered handoff but requires durable, crash-repairable intent in the chosen choreography.

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

The *question status names are design values to reconcile against frozen F3 contracts*; a generic U07 mapper must not unilaterally pick `ANSWERED` for an ambiguous response or create clinical facts. An answer may be present without all Gap needs satisfied. A new F3 answer-consumption contract is **REQUIRED**, not proven implemented. `CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED` must either confirm reuse of approved F3 ownership or amend it under independent review before implementation.

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

## 9. Consultation WAITING_USER → ACTIVE owner transition

Chosen candidate: **Consultation lifecycle owner transition under the same `findByIdForUpdate` Consultation row fence** shared with RDP-02/U15. The existing `ConsultationRecord.enterWaitingUser` is one-directional U06 functionality; a U07-specific owner transition and immutable effect/journal are **NOT_IMPLEMENTED**.

```text
ConsultationResumeTransitionCommandV1 {
  consultation_id,
  exact_expected_lifecycle = WAITING_USER,
  exact_expected_current_wait_effect_id,
  expected_consultation_row_version,
  expected_u15_terminal_generation,
  f8_accepted_decision_ref,
  root_resume_effect_id,
  p02_resume_success_ref,
  f3_owner_effect_commit_ref,
  pending_consume_commit_ref
}
```

Inside owner-governed transaction, Consultation row must still be WAITING_USER with the same parent wait and winner root; verify U15/Question currentness and positive P01/F3 readback. Transition to ACTIVE and clear/consume `current_wait_effect_id` **only on authorized owner commit**, with stable `consultation_active_effect_id`. If already ACTIVE from same root, exact replay; if ACTIVE from a different cause, fail closed. A `WAITING_USER` row may not be forcibly activated solely because P02 returned RESUMED.

If Clinical State and Consultation do not share one atomic resource manager, exact ordering/compensation of Pending Question consume and ACTIVE transition must be designed as a **durable saga with write-ahead intent and reconciliation**, **not claimed atomic**. This is a blocking **physical design decision for RDP-03 review / RDP-05**; distributed transactions cannot be assumed available. The system must fence any new wait/answer while partially reconciled.

## 10. Cross-owner apply topology and crash-safe journal

### 10.1 Proposed V1 authority journal

```text
U07ApplyJournalV1 {
 root_resume_effect_id PK,
 wait_apply_authority_key UNIQUE,
 canonical_answer_event_id, f8_decision_id, f8_claim_generation,
 consultation_id, question_id, parent_wait_effect_id,
 current_stage, stage_generation,
 p02_runtime_result_ref,
 f3_answer_effect_id + commit_ref,
 pending_consume_effect_id + commit_ref,
 consultation_active_effect_id + commit_ref,
 u02_handoff_effect_id + enqueue_ref,
 current_owner_version_refs,
 first_seen_at, updated_at, failure_class, trace_ref
}
```

All stage transitions are monotonic, CAS on `stage_generation`, and persisted before performing/retrying next stage. Never use an in-memory boolean, trace-only record, or Foundation effect-ledger entry as definitive evidence of completed clinical mutation. The journal's **wait uniqueness constraint** enforces one root apply; its presence alone is not proof of owner effects.

### 10.2 Transaction boundaries (proposed, not verified)

- **Tx A**: F8 immutable ACCEPTED/claim already durably committed by RDP-02. U07 claims existing same-wait ApplyJournal root under Consultation/U15 fence; no P01 mutation yet.
- **Tx B**: P02 runtime resume is executed/resolved under RDP-04. Record durable P02 authority result, and only report RESUMED_VERIFIED with P02 owner evidence.
- **Tx C**: F3 owner lifecycle decision and P01 patch/commit/readback; expected Pending Question version. If F3 and P01 can atomically commit the required Question/Pending fields, use one review-approved K09 patch; otherwise independently fenced owner actions + journaled compensation needed.
- **Tx D**: Consultation lifecycle owner transitions ACTIVE through same Consultation/U15 lock and readback.
- **Tx E**: reconcile APPLIED, persist **unique U02 handoff intent** and outbox claim, subsequently deliver/ack via RDP-04 approved transport.

**Mandatory atomicity question:** APPLIED vs U02 handoff outbox must be in the same durable transactional resource, **or** implementation must provide a previously accepted crash-proof write-ahead stage that reconstructs the exact one handoff after APPLIED. No “APPLIED with lost answer” success. The exact physical topology is an **open RDP-03 independent review question**, and readiness cannot pass before one option is frozen with owner consent.

No external side effects, real clinical handoff or PHI flows are permitted in this design phase.

### 10.3 Crash / retry matrix

| Window | Required reconciliation | Forbidden |
|---|---|---|
| F8 ACCEPTED before U07 effect claim | acquire/reconcile unique root and current U15 fence | second F8 ACCEPTED |
| Apply journal CLAIMED before P02 | query same P02 owner resume status; one resume | blind P02 replay |
| P02 resume finished before journal ack | reconcile P02 exact committed result | assuming not resumed and rerunning |
| F3 owner effect committed before journal ack | lookup F3/P01 CommitResult and authoritative readback | second owner mutation |
| Pending Question consumed before ACTIVE | preserve same root and block competing wait; resume governed remaining effect | clearing another Question |
| Consultation ACTIVE before APPLIED mark | read Consultation owner effect and P01 state; finalize only when all evidence aligns | pretending P01 committed |
| APPLIED before outbox persisted | **must be impossible by atomic outbox or have durable recoverable intention before APPLIED** | losing the answer |
| Outbox persisted before send | repeat same message/effect identity | producing new U02 handoff ID |
| U02 accepted but response lost | use target idempotency/receipt, not re-send as new fact | second U02 fact commit |
| U15 terminalizes during partial resume | block further ordinary effects and begin governed recovery; retain F8 original decision | force ACTIVE or rewrite F8 |
| Commit outcome UNKNOWN | inspect owner state/journal under fresh transaction | optimistic APPLIED |

### 10.4 Safety and side-effect policy

Before *every* novel clinical-state mutation or external U02 delivery, the current U15 cancel/expiry and same-wait F8 winner must be verified at the correct owner fence. If U15 expires between F8 ACCEPTED and P01 apply, no new ordinary effect. If cancellation happens **after a subset of owner mutations**, freeze continuation and route owner-led reconciliation; do not imply a perfect rollback if writes already committed. Never convert partially applied answers into a new ordinary waiting session.

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
| U07 producer/path authorization, K09/P01 readback | NOT_VERIFIED | schema/field/owner/CAS tests and source authority |
| Consultation ACTIVE transition + same-root replay | NOT_IMPLEMENTED | owner API, DB and concurrency evidence |
| RDP-04 P02 resume / U02 handoff | FUTURE DESIGN | runtime success/recovery/outbox owner contracts |
| RDP-05 dual-dialect / atomicity / registry | FUTURE DESIGN | explicit chosen physical topology and per-dialect tests |
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
| U07-A03-20 | APPLIED/outbox interrupted | no lost handoff; atomic/recoverable unique intent |
| U07-A03-21 | U02 ack lost | one logical U02 handoff / no duplicate Clinical Fact |
| U07-A03-22 | concurrent U15/F8/U07 apply | shared fence respects owner terminal commit order |
| U07-A03-23 | Clinical State and Consultation separate DBs | fail readiness until governed saga topology proven |
| U07-A03-24 | F3 bridge or U15 shared fence not authorized | design-only; implementation blocked |
| U07-A03-25 | same wait answer payload digest but other Question | no cross-Question effect deduplication |
| U07-A03-26 | PHI-bearing payload/production profile | blocked; synthetic-only |
| U07-A03-27 | trace evidence missing before required commit | fail-closed or owner-authorized recovery; no fake commit |
| U07-A03-28 | replay old ACCEPTED after U15 expiry | historical verdict unchanged, no unauthorized new effect |

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
```

## 15. Formal candidate status / next gate

```text
U07-RDP-03 = DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN
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

Next gate: **U07-RDP-03 Independent Design Review** against an exact design HEAD. The review must explicitly decide whether §9–10 leave any critical unresolved **physical atomicity blocker** that requires targeted RDP-03 remediation before design acceptance. No runtime edits, merge, patient flows, clinical activation, or authorization under this document.
