# U07-RDP-05 Capability / Dependency / Applicability Contract v0.1

> Development Unit: **U07 — User Answer Resume and Idempotent Recovery**
> Gap: **B-U07-RG-05**
> Design-input baseline: RDP-01..04 independently **CONDITIONALLY_ACCEPTED_DESIGN** (last RDP-04 review [PR #282](https://github.com/cxjchelsea/AIdoctor/pull/282), review commit `b536221bcb66cbce43e559093431cb07631d78f3`)
> Bounded source-inspection baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Status: **DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN**
> Coverage: capability inventory, producer/owner signatures, dependency currentness and applicability, transaction-domain constraints, upstream amendment register, physical impact, readiness gates and design oracles.
> **Does not authorize** implementation, schema changes, PR merge, live P02 resume, U02 delivery, PROFILE-A, PHI or real-patient production.

## 1. Why RDP-05 exists

RDP-01 proves admission/identity; RDP-02 F8 determines lawful business wait validity; RDP-03 governs effect application; RDP-04 determines P02 recoverability and routes an authorized Outbox to U02. None proves that **all underlying physical capabilities share correct versions, permissions, currentness, transaction manager or authorization**.

RDP-05 is the **pre-readiness dependency authority contract**: for each U07 phase state the exact capability required, its owner, evidence needed for availability, whether current or historical resolution applies, whether the current code provides it, and what blocks U07 if evidence is absent. A name, Registry key, interface class, fake implementation, synthetic test or successful design review is **not** runtime capability proof.

This contract **never** changes F8 business verdict, manufactures U06 eligibility, directly commits Clinical State, restores a P02 checkpoint, grants U15 authority, or interprets answer as Clinical Fact.

## 2. Source-grounded baseline: verified reuse versus open physical capability

| Asset examined on source baseline | What source positively demonstrates | Does NOT establish |
|---|---|---|
| `runtime/foundation/CanonicalBusinessEventLedger.java` | transactionally `resolveOrCreate` with identity/idempotency conflict guard | U07 canonical *side-binding* same-DB atomic record, F8 verdict/decision ledger |
| `runtime/foundation/RuntimeBindingRecord.java` | per-Consultation runtime authority, Thread, CDP, scope/capability-set/contract version persisted | complete historical Registry/Tool/Harness/Workflow binding vector or executable U07 P06 resolver |
| `runtime/foundation/RuntimeThreadStateRecord.java` | versioned Thread; ACTIVE / WAIT_CHECKPOINTED / AWAITING_USER, run/checkpoint/wait IDs | U07 PARKED `RESUMED_READY_BUT_NOT_DISPATCHED`, restoration/start grants, every dispatcher path checked |
| `runtime/foundation/RuntimeWaitCheckpointRecord.java` | durable U06 wait provenance: run/thread/Question/Pending, delivered wait/confirmation, Clinical version and dependency/policy refs | executable program cursor image, full replay manifest, P02 rehydration evidence |
| `RuntimeWaitCheckpointService` + `RuntimeThreadWaitTransitionService` | U06 reserve and enter AWAITING_USER with Thread lock | U07 P02 Restore/Resume, run continuation or owner result |
| `u06/wait/U07ResumeEligibilityProjector.java` | stable eligibility hash requiring caller-provided awaiting flag | authoritative U06 issuance/trace/reattachment records required by RDP-01 |
| `u06/state/U06SyntheticP01Runtime.java` | injected StateCommitter, U06 producer/path grants, commit/readback synthetic pattern | U07 combined F3 Question/Gap/Pending mutation privilege, U15-shared commit |
| `u06/delivery/JdbcU06DeliveryStore.java` | bounded U06 synthetic delivery/confirmation ledger and effect identity | U07 unique U02 Outbox, U15 first-send grant or U02 consumer replay receipt |
| `u06/trace/JdbcU06GovernedExecutionTraceStore.java` | U06-scoped durable lifecycle trace/replay conflict logic | universal U07 P05 event/effect/owner-proof schema and independent authority |
| `runtime/effects/CanonicalEffectLedger.java` | generic effect recording/reconciliation surface | U07 owner state journal, effect mutation atomicity or external delivery |
| Consultation record/repository (per prior RDP-03 inspection) | WAITING_USER row with version and pessimistic-write owner accessor | U15 participates, P01 same physical transaction, authorized WAITING→ACTIVE |
| U07 RDP-01..04 docs and exact independent review artifacts | reviewed **design contracts** | functioning U07 source, passed physical integration tests or implementation authority |

**Evidence vocabulary:** `SOURCE_VERIFIED_REUSABLE_PART` means bounded source demonstrated only the named primitive; `SOURCE_VERIFIED_SCOPE_SPECIFIC` means an existing U06/other unit capability cannot be promoted to U07; `NOT_DEMONSTRATED` means checked evidence does not prove completeness, not a global absence claim; `DESIGN_ONLY` means reviewed contracts with no physical proof; `UNAUTHORIZED_DEPENDENCY` means a new capability cannot be used even if code becomes available.

## 3. Owners and non-delegable permissions

| Owner | Positive authority for U07 | Explicitly not that owner's authority |
|---|---|---|
| Foundation/P06 | canonical event ID, same-DB U07 side-binding, Scope/version/Registry authority | deciding F8 legality |
| U06 | original Question delivery, WAITING_USER and **durable issuance** of resume eligibility | inventing P02 compatibility or reissuing another Question under U07 |
| F8/RDP-02 | ACCEPTED/DUPLICATE/EXPIRED/REJECTED, same-wait first winner, immutable verdict | marking APPLIED or restoring Runtime |
| U15 | cancellation/expiry/terminal generation, shared Consultation fence, **separate** restore-start and outbound dispatch policy | clinical fact interpretation or ad-hoc client time |
| P02 | original Thread/Run/Checkpoint, executable compatibility, inert restore or certified deterministic rehydrate, parked result | F8 verdict, F3 Question/Gap, Clinical State or unlicensed scheduler execution |
| F3 | semantic Question/Gap status and answer-consume owner decision | direct physical CDP mutation without P01 |
| K09/P01/G2 | one F3-authorized Question/Gap/Pending commit, correct CAS and owner readback | U15 terminal decision; second independent Clinical version for one C effect |
| Consultation lifecycle owner | guarded WAITING_USER→ACTIVE and owner receipt | bypassing U15/F3/P01 fence |
| U07 coordinator | root claim, ApplyJournal staged saga, stage E APPLIED+outbox, controlled reconciliation | all other owners' source-of-truth facts |
| RDP-04 Outbox worker/U02 consumer | sends same-granted effect, U02 validates/replays admission & interprets Clinical Facts | using outbox as exactly-once Clinical Fact proof |
| P05 | Trace/audit refs and replay provenance | deciding any clinical/business/Runtime owner fact |
| U14/K0 | governed exceptions/safety and owner-led recovery | auto-force APPLIED, falsify owner receipts or bypass authorizations |

## 4. Exact dependency vocabulary

```text
Requiredness:
  REQUIRED_NOW | REQUIRED_IF_SELECTED_PATH | REQUIRED_AT_NEXT_STAGE |
  HISTORICAL_PROVENANCE_ONLY | OPTIONAL_IF_APPROVED | NOT_APPLICABLE

Capability availability:
  SOURCE_VERIFIED_REUSABLE_PART | SOURCE_VERIFIED_SCOPE_SPECIFIC |
  PROVEN_READY_FOR_PROFILE | DESIGN_ONLY | NOT_DEMONSTRATED |
  UNAUTHORIZED_DEPENDENCY | STALE | CONFLICTED | UNAVAILABLE

Resolved applicability:
  PRESENT_CURRENT | PRESENT_HISTORICAL_CERTIFIED |
  NOT_YET_APPLICABLE | NOT_APPLICABLE |
  BLOCKED_AUTHORITY | BLOCKED_VERSION | BLOCKED_PHYSICAL |
  RECONCILIATION_REQUIRED
```

`PRESENT_CURRENT` is **not** permission for historical rebinding. `PRESENT_HISTORICAL_CERTIFIED` is not permission to use latest release or to revive expired business wait. `NOT_APPLICABLE` can be issued only when the selected **frozen** path has an explicit exclusion rule; cannot be a fallback for a missing mandatory dependency. `OPTIONAL_IF_APPROVED` is not "missing is okay"; it requires profile-specific owner policy. `NOT_YET_APPLICABLE` means stage cannot consume the input yet and is never silently promoted to successful execution.

## 5. Dependency manifest envelope and resolver output

```text
U07DependencyAssessmentV1 {
  contract_version = "u07.rdp05.dependencies.v1",
  assessment_id, exact_source_sha, profile_id,
  canonical_answer_event_id, root_resume_effect_id?,
  consultation_id, thread_id, original_run_id, original_checkpoint_id?,
  parent_delivered_wait_effect_id, original_eligibility_issuance_ref?,
  f8_decision_id?, f8_winner_ref?, f8_decision_version?,
  original_scope_ref, historical_binding_fingerprint,
  current_u15_terminal_generation?, owner_snapshot_epoch?,
  selected_resume_path = NOT_SELECTED | EXACT_RESTORE |
                         CERTIFIED_REHYDRATE | GOVERNED_FAILURE,
  requested_stage = ADMISSION | F8 | APPLY_CLAIM | P02_RESTORE |
                    P01_APPLY | CONSULTATION_ACTIVE | APPLIED_OUTBOX |
                    U02_DISPATCH_GRANT | U02_CONSUMER_ACCEPT,
  dependencies[] = {
    capability_id, owner_namespace, requiredness, applicability,
    expected_contract_version, original_binding_ref?,
    current_binding_ref?, binding_fingerprint?,
    source_kind, source_sha?, executable_adapter_ref?,
    authorization_profile_ref?, capability_proof_ref?,
    transaction_manager_ref?, primary_db_ref?, version_vector?,
    owner_fence_generation?, evaluation_result, reason_code,
    negative_effect_evidence_ref?, independent_review_ref?
  },
  aggregate = READY_FOR_STAGE | BLOCKED | DEFERRED_RECONCILIATION,
  decision_reason, evidence_fingerprint, trace_ref
}
```

**Trust/immutability:** build from authoritative owner/Registry sources, never client-supplied positive capability flags. Bind assessment and source SHAs to one immutable evaluation snapshot with explicit owner version vector; retry same assessment ID with altered binding/fingerprint is CONFLICTED. Evaluations after a Consultation/U15 generation change must reacquire owner evidence; a cached `READY_FOR_STAGE` cannot outlive its bounded generation.

**Requiredness rule:** for requested stage `S`, aggregate READY only if **every** REQUIRED_NOW and selected-path REQUIRED_IF_SELECTED_PATH item has independently verified contract/version/scope/producer permission, actual executable adapter, owner receipt, selected-profile permission and required shared transaction/effect fencing; any missing/denied = BLOCKED, transient authority gap = DEFERRED_RECONCILIATION with **zero novel effects**. Future-stage capabilities may be NOT_YET_APPLICABLE for that stage but must remain in the overall U07 readiness inventory. This is a precondition check, **not an authorization decision itself**.

## 6. Stage × capability applicability matrix

Codes: **R** required for ordinary stage; **C** required when path is selected; **H** historical owner evidence; **G** prerequisite before any novel effect; **—** not applicable; **F** future-stage dependency tracked but not a premature gate.

| Capability | Ingress RDP-01 | F8 RDP-02 | P02 restore RDP-04 | Stage C/D RDP-03 | Stage E Outbox | Stage F/U02 |
|---|---|---|---|---|---|---|
| Foundation canonical event + inline binding | R | H | H | H | H | H |
| U06 delivered-wait + original eligibility issuance | R | R | H | H | H | H |
| F8 committed ACCEPTED/wait winner | — | R | R | R | R | H |
| Consultation/U15 shared terminal guard + DB clock | H | G | G | G | G | G |
| P06 original/current Scope/Registry/version resolution | R | R | R | R | H | R |
| P02 executable checkpoint | — | — | C exact restore | F | F | F |
| P02 certified rehydrate proof + owner recipe | — | — | C rehydrate | F | F | F |
| P02 parked landing + restore-only U15 start grant | — | — | R | H | H | H |
| F3 Question/Gap answer policy + K09/P01 single patch | — | — | F | R | H | H |
| Consultation ACTIVE owner effect | — | — | F | R | H | H |
| U07 ApplyJournal + stable effect receipt | — | H | R | R | R | H |
| APPLIED+Outbox same guard-DB COMMIT | — | — | F | F | R | H |
| U15 committed U02 dispatch-grant | — | — | F | F | F | G |
| U02 consumer idempotent admission/receipt lookup | — | — | F | F | F | R |
| P05 Trace/evidence provenance | R | R | R | R | R | R |
| U14 governed failure/safety escalation | C failure | C failure | C failure | C failure | C failure | C failure |
| P03 LLM inference / P04 knowledge lookup | — | — | — | — | — | — |

**Important:** P03/P04 are **NOT REQUIRED** to determine lawful Resume or to park a recovered Runtime. They may be required later by U02 or a future separately authorized post-landing clinical execution, but they cannot silently become U07 F8/P02 required prerequisites nor license post-landing model calls. U05/U08 Scheduler reentry is NOT_APPLICABLE before U02 under U07 V1.

## 7. Path-dependent applicability and negative proofs

| Situation | Mandatory evidence / decision | What cannot happen |
|---|---|---|
| PROFILE-B synthetic, current wait, confirmed issuance, valid F8 | current authorized owner/currentness and physical profile | does not auto-prove executable P02 |
| canonical RESUME_REQUEST of already accepted USER_ANSWER | resolve original event+decision+root; historical reattach | no new user answer or new root |
| F8 DUPLICATE / EXPIRED / REJECTED | original typed verdict/history; no new ordinary effect | no P02 restore, P01 C, Outbox |
| F8 ACCEPTED but U15 subsequently terminal | F8 immutable; new effect-stage U15 guard blocks | don't rewrite F8 as REJECTED |
| missing original U06 eligibility issuance | `BLOCKED_AUTHORITY` even with stable projected ID | no "hash proves issuance" |
| exact executable checkpoint + original version vector | P02 compatibility proof, restore-only start grant | no program-counter business execution |
| **metadata-only U06 checkpoint** | source demonstrates wait provenance only | **not** COMPATIBLE_CHECKPOINT |
| missing/stale checkpoint with full signed complete rehydrate proof | `CERTIFIED_REHYDRATE`, owner grant, parked readback | no historical effect replay |
| missing checkpoint without full effect manifest/plan | `BLOCKED_PHYSICAL / INSUFFICIENT_EVIDENCE` | no fabricated continuity |
| unknown/PARTIAL historical Tool/Workflow outcome | authoritative outcome reconciliation | no rehydrate/reissue |
| post-grant U15 terminal before inert restore starts | only same finite inert grant may finish under approved policy; Stage C prohibited | never general RUNNING |
| P02 RESUMED_VERIFIED parked, P01 producer not authorized | Stage C blocked, Thread remains parked | no APPLIED |
| Stage C committed, U15 terminal before D | preserve C truth; owner-led reconciliation | no forced ACTIVE |
| APPLIED and Outbox PENDING, U15 terminal before F grant | outbox BLOCKED_TERMINAL, zero sends | no direct U02 |
| F grant commits first, U15 later terminates | only same-grant handoff may finish if U15 owner policy approved | never new clinical actions |
| U02 ACK/receipt unknown | same handoff ID consumer query/replay | no producer claim of exactly-once fact |
| PROFILE-A / PHI / patient | BLOCKED entirely | no live execution/dispatch from this design |

## 8. Selected physical transaction and temporal authority contract

**One selected design topology, no implicit fallback:** `SINGLE_GUARD_DB_STAGED_SAGA_V1`, inherited from RDP-03. The primary DB / transaction manager for authoritative Consultation/U15 owner row, F8 winner, U07 binding (RDP-01), P01 stage-C Clinical State commit guard, ApplyJournal/Outbox E, and P02 restore-start grant must be demonstrably compatible with the selected **same transaction manager and Consultation row lock**. The P02 physical restoration work and external U02 transport happen outside DB transactions, but their **grants** are committed in the correct shared-guard transaction first.

RDP-01 requires same-transaction Foundation event + inline binding. F8 first-decision uses statement-current DB time. Stage C requires ONE owner-authorized P01 patch and ONE Clinical version; D Consultation ACTIVE; E APPLIED and unique Outbox in one COMMIT; P02 start grant and F U02 dispatch grant each independently ordered against U15 terminalization.

**Timestamp requirements:** certified primary DB statement-current time at actual final conditional mutation, one comparable authoritative deadline domain and precision in both MySQL/Oracle; `timestamp now from JVM` or `transaction-start time` cannot replace it. The U15 monotonic terminal generation is a second concurrent-owner fence, not a proxy for an expired deadline that U15 has not persisted. Evidence must include U15 terminal-before, granted-before-terminal, deadline boundary and unknown COMMIT.

**Fail closed if any of**: transaction-manager co-location cannot be proven; DB dialect offers only unsound time predicate; P01 patch commits independently; U15 does not acquire identical row lock; P02 start and U02 dispatch grants cannot join the shared guard; producer adapters bypass central parked barrier. This V1 is **NOT_APPLICABLE / NOT_READY**, no “best effort” saga or alternate SQL timing without Controlled Amendment + independent review.

**Lock order:** Consultation/U15 row → canonical winner/apply root as required → P02 Thread → checkpoint/journal/effect/outbox (deterministic sorted order per transaction); transaction participants that do not need Thread must omit that lock but cannot acquire it before Consultation if they later need Consultation. RDP-05 does not authorize a long database lock across Runtime restoration or network calls.

## 9. Binding/profile/authority envelope

```text
U07ExecutionDependencyBindingV1 {
 binding_id, profile_id = PROFILE_B_SYNTHETIC_STRUCTURAL_NONPROD,
 scope_ref, consultation_ref, original_thread_ref, original_run_ref,
 canonical_event_binding_ref, u06_issuance_ref,
 f8_decision_policy_ref, f8_owner_ref,
 u15_owner_version_ref, original_wait_deadline_authority_ref,
 original_runtime_capability_binding_ref, original_registry_manifest_ref?,
 p02_runtime_version_vector_ref, checkpoint_format_ref?,
 rehydrate_proof_ref?, p02_restore_grant_policy_ref,
 p02_parked_barrier_capability_ref,
 f3_question_owner_authorization_ref?, p01_field_grant_ref?,
 p01_shared_commit_fence_ref?, consultation_active_owner_ref?,
 u07_apply_journal_ref, outbox_schema_ref?,
 u15_dispatch_grant_policy_ref?, u02_consumer_contract_ref?,
 p05_trace_schema_ref, u14_failure_policy_ref,
 canonical_fingerprint, owner_snapshot_epoch
}
```

Physical P06 `RuntimeBindingRecord` existing columns are a baseline **subset**; do not relabel them as containing U07 registry/Tool/Harness snapshots, Field Grants, U15 policy authority or complete immutable replay manifest. Historical binding refs remain pinned at wait time; current references are separately resolved for U15/permission/safety. Identical names with different SHA/semantic version are CONFLICTED, not “latest compatible.” `PROFILE_B_SYNTHETIC_STRUCTURAL_NONPROD` allows only authorized synthetic fixtures and no real patient; a test double proves shape, **not** U07 production capability.

## 10. Capability acceptance quality gates

A dependency may change to `PROVEN_READY_FOR_PROFILE` only with:

1. **Source:** exact code/config/migration/Registry head and declared application role; not a class name alone.
2. **Owner:** owner decision and approved producer/capability/field permissions for chosen profile.
3. **Schema:** frozen typed command/result, version, immutable ID/frame and migration evidence in MySQL and Oracle where applicable.
4. **Semantics:** positive tests of accepted flow and negative tests showing zero U02/Tool/P01 side effects when blocked.
5. **Idempotency:** same-event replay, different-canonical-same-wait conflict, unknown commit reconciliation, unique owner receipt and fixed root.
6. **Concurrency:** U15 expiry/cancel vs F8/Stage C/D/E and separate P02/F restore/dispatch grants.
7. **Runtime:** actual serialized executable snapshot **or** complete owner-certified P02 rehydrate proof, restored Thread parked at central dispatcher barrier.
8. **Scope:** synthetic PROFILE-B only, no PHI, no real production, no capability substitution.
9. **P05:** durable Trace/audit (evidence links but no authority).
10. **Independence:** exact-head verification including reproducible runner, independent evidence-only check and explicit gate decision.

Failure of **any** required quality gate leaves owner capability `NOT_VERIFIED` and U07 Implementation Readiness `NOT_READY`; test simulation/mock passes must be tagged `STRUCTURAL_ONLY` and never substituted for physical owner evidence.

## 11. Controlled-amendment / dependency authorization register

| Gate / CA | State | Blocking owner proof |
|---|---|---|
| `GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01` | REQUIRED / NOT_PASSED | exhaustive SHA-pinned Foundation call sites, ledger/side-binding transaction interoperability |
| `CA-U06-U07-ELIG-ISSUANCE-01` | REQUIRED / NOT_AUTHORIZED | U06 durable eligibility issuance and authoritative current/historical retrieval |
| `CA-U07-RDP02-U15-SHARED-FENCE-01` | REQUIRED / NOT_AUTHORIZED | U15 same Consultation lock, first decision/winner/terminal ordering |
| `CA-U07-RDP03-F3-ANSWER-BRIDGE-01` | REQUIRED / NOT_AUTHORIZED | F3 Question/Gap owner-issued decision and combined P01 field grants |
| `CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01` | REQUIRED / NOT_AUTHORIZED | same-DB P01 Stage C guarded commit with statement-time and version checks |
| `CA-U07-RDP03-U15-DISPATCH-GRANT-01` | REQUIRED / NOT_AUTHORIZED | U15 approval of bounded same-effect irrevocable post-grant Outbox dispatch |
| `CA-U07-RDP04-P02-REHYDRATE-OWNER-01` | REQUIRED / NOT_AUTHORIZED | P02 full plan/cursor + all historical effect owner receipts and deterministic recipe |
| `CA-U07-RDP04-P02-EXECUTION-START-FENCE-01` | REQUIRED / NOT_AUTHORIZED | U15-approved inert start grant and same-DB atomic ordering |
| `CA-U07-RDP04-P02-LANDING-BARRIER-01` | REQUIRED / NOT_AUTHORIZED | Thread parked state, centralized deny-all business dispatch across all executor paths |
| `CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01` | REQUIRED / NOT_AUTHORIZED | consumer admission, idempotent handoff receipt query and no duplicate Clinical Fact identity |

**No U07 RDP-05 requirement or successful design review constitutes approval of a CA.** Each amendment requires its own scope, owner approval, source/manifest update, independent review, and non-production evidence. RDP-05 itself does not choose to waive/defer any above blocker.

## 12. Physical implementation impact by component (planning only)

| Impact ID | Proposed change | Owner | Gate |
|---|---|---|---|
| U07-RDP05-IMP-01 | Foundation canonical side-binding schema/atomic adapter and original answer synthetic storage | Foundation/P06 | RDP-01 audit |
| IMP-02 | U06 durable eligibility issuance store/lookup + original wait evidence | U06 | issuance CA |
| IMP-03 | F8 decision ledger, same-wait winner, SQL time/dialect and U15 shared fence | F8/U15 | RDP-02 CA |
| IMP-04 | U07 ApplyJournal/root stages, unique effect, same-guard transaction topology | U07 | RDP-03 readiness |
| IMP-05 | F3-issued answer consumption and one K09/P01 Question/Gap/Pending commit | F3/P01 | F3 CA + P01/U15 CA |
| IMP-06 | Consultation ACTIVE owner transition, owner-effect evidence/reconciliation | Consultation/U15 | shared fence and lifecycle owner |
| IMP-07 | P02 immutable checkpoint executable image or owner-certified full rehydrate proof | P02/P06/external effect owners | P02 rehydrate CA |
| IMP-08 | P02 start grant/Thread parked state/global dispatcher barrier/resume receipt | P02/U15 | start-fence and landing CAs |
| IMP-09 | APPLIED+Outbox same COMMIT, U15 F dispatch grant, worker idempotency | U07/U15 | dispatch CA |
| IMP-10 | U02 handoff admission/receipt lookup and fact ownership | U02 | U02 consumer CA |
| IMP-11 | unified U07 P05 Trace plus scoped immutable version/owner refs | P05/P06 | schema/trace compatibility |
| IMP-12 | dual-dialect migrations, negative spies, crash/race/evidence runner | Verification/owners | RDP-06 gate |

No item is a permission to code/merge. Proposed classes/tables not demonstrated in source are **NOT_IMPLEMENTED / NOT_VERIFIED** until proved otherwise. Changes to U06/F3/U15/P01/P02/U02 owner components cannot be silently implemented from a U07-only PR.

## 13. Profile applicability and fail-closed behavior

**PROFILE-B: SYNTHETIC_STRUCTURAL_NONPROD** is the only in-scope profile for design and future controlled verification. Every required owner/field/Scope grant must explicitly authorize this profile, even if a synthetic adapter appears to work. Production capability is not inherited from structural evidence. Data must be synthetic with bounded retention and no PHI.

**PROFILE-A: REAL_GOVERNED / patient** is `NOT_APPLICABLE / BLOCKED` in this design and requires separate later clinical/safety, privacy, real provider, consent, U02 clinical fact validation, release activation and production permissions. A `NOT_APPLICABLE` entry must not be interpreted as PROFILE-A-ready.

**Failure:** `UNAVAILABLE` or transient owner snapshot conflict => DEFERRED_RECONCILIATION with bounded retry; `STALE` original binding => owner review/migration, not rebind; `CONFLICTED` ID/fingerprint => reject physical effect, preserve historical F8; `BLOCKED_AUTHORITY`/NOT_AUTHORIZED => no new action and await explicitly governed owner change; `BLOCKED_PHYSICAL` => implementation NOT_READY, not a runtime fallback to metadata-only restore or unguarded transport.

## 14. RDP-01..04 compatibility checklist

| Previous RDP | Compatibility assertions required before independent aggregate review |
|---|---|
| RDP-01 | original USER_ANSWER inline payload and side-binding committed with Foundation canonical event, original U06 eligibility resolvable; RESUME_REQUEST target-only identity |
| RDP-02 | F8 winner ledger and timestamp authority not replaced by Runtime compatibility, explicit U15 fence and historical ACCEPTED unchanged |
| RDP-03 | one F3/P01 Stage C owner commit; D ACTIVE; E APPLIED+unique Outbox same COMMIT; F U15 dispatch grant; root conflict and partial owner reconciliation |
| RDP-04 | checkpoint executable **or** complete signed historical rehydrate proof; restore-only U15 grant, parked result, no scheduler/tool effects before authorized stages, U02 consumer idem |
| U07 Unit Spec | BL-03 wait/answer/resume only; no U05/U08 bypass and no Clinical AI dependence for legality; U02 owns facts |
| RDP-06 next | tests must assert negative external effects and binding/owner evidence under exact physical artifact heads |

An aggregate compatibility review must not conclude `READY` from four conditionally accepted designs plus a dependency manifest without closing the actual physical and approval gaps.

## 15. Design-only oracle catalog

| ID | Scenario | Expected assessment/action |
|---|---|---|
| CAP-T01 | Foundation canonical event exists but no atomic U07 side-binding | BLOCKED_PHYSICAL; no F8 acceptance based on partial row |
| CAP-T02 | identical USER_ANSWER replay exact binding | original canonical ID, no new decision/root |
| CAP-T03 | only U06 projected `u07elig` hash, no issuance authority | BLOCKED_AUTHORITY |
| CAP-T04 | F8 ACCEPTED but U15 terminal after verdict | historical ACCEPTED unchanged; no new stage |
| CAP-T05 | U06 wait checkpoint metadata present, no executable image | P02 exact restore NOT_APPLICABLE |
| CAP-T06 | full owner-certified historical rehydrate proof | rehydrate path conditional eligible, no historical effect replay |
| CAP-T07 | historical prior Tool effect UNKNOWN | BLOCKED_PHYSICAL; zero rehydrate |
| CAP-T08 | P02 result parked and P01 owner field grant unavailable | Stage C BLOCKED; no APPLIED |
| CAP-T09 | Stage C P01 separate transaction manager from U15 guard | V1 NOT_APPLICABLE / NOT_READY |
| CAP-T10 | U15 cancel before P02 restore-start grant | zero first restore |
| CAP-T11 | P02 restore-only grant before U15 cancel | only same inert restore to parked, later C blocked |
| CAP-T12 | parked Thread with scheduler callback | central barrier denies all business/tool/U02 execution |
| CAP-T13 | F3 Question approval exists but Pending field grant absent | combined Stage C BLOCKED; not partial commit |
| CAP-T14 | Stage C same root P01 COMMITTED but projection delayed | readback same owner receipt; zero second Clinical version |
| CAP-T15 | D Consultation ACTIVE before E; U15 terminalizes | E blocked; owner reconciliation |
| CAP-T16 | E APPLIED succeeds, Outbox insert fails | both rollback in one DB COMMIT |
| CAP-T17 | E APPLIED+Outbox committed, F grant absent | zero U02 sends |
| CAP-T18 | U15 wins before F grant | PENDING→BLOCKED_TERMINAL; zero sends |
| CAP-T19 | F grant wins then U15 terminal | same-effect send only if approved U15 policy |
| CAP-T20 | U02 receipt lost or consumer idempotency absent | re-query same ID or no delivery, no duplicate clinical fact claim |
| CAP-T21 | P06 original Registry version stale vs latest | historical certified or blocked; no latest rebinding |
| CAP-T22 | P05 trace exists but owner P01 receipt missing | Trace not authority; cannot advance APPLIED |
| CAP-T23 | PROFILE-A PHI or live patient | BLOCKED regardless of synthetic evidence |
| CAP-T24 | P03/P04 unavailable during F8 legality | NOT_APPLICABLE, not a false blocker for F8 |
| CAP-T25 | U02 follow-on reasoning later needs P03/P04 | external U02 contract, never implicit U07 continuation |
| CAP-T26 | exact-head physical/source binding unknown | NOT_DEMONSTRATED / NOT_READY, no invented PASS |
| CAP-T27 | MySQL/Oracle statement time divergent/unknown | BLOCKED_PHYSICAL; no winner/APPLIED |
| CAP-T28 | Foundation audit NOT_PASSED while RDP-05 designs appear complete | U07 readiness still NOT_READY |
| CAP-T29 | any CA above NOT_AUTHORIZED | stage requiring it BLOCKED; overall readiness NOT_READY |
| CAP-T30 | two authorities provide incompatible owner version vectors | CONFLICTED or RECONCILIATION_REQUIRED, zero new effects |
| CAP-T31 | P02 executable restore evidence is true but global parked guard absent | restore not authorized, NOT_READY |
| CAP-T32 | same-root Outbox dispatch retries after grant commit unknown | reconcile grant/consumer refs, no speculative duplicate |
| CAP-T33 | synthetic U06 P01 commit path re-used for U07 without producer/path grant | BLOCKED_AUTHORITY |
| CAP-T34 | U07 ApplyJournal says APPLIED but owner readback mismatches | quarantine, NOT_READY, no U02 |
| CAP-T35 | latest capability exists but original Run bound to older version | require certified historical compatibility/migration |
| CAP-T36 | end-to-end mocks pass while physical receipt absent | STRUCTURAL_ONLY; not PROVEN_READY_FOR_PROFILE |

These are **36 unexecuted design oracles**. RDP-06 must later provide test names, source/migration SHA bindings, fixtures, negative-effects evidence, DB-dialect concurrency and owner-level receipt proofs.

## 16. Independent review questions and current decision

```text
IR-U07-RDP05-01  Does the inventory separate source-verified primitives from U07-ready runtime capabilities?
IR-U07-RDP05-02  Does every stage and selected path have a complete requiredness/applicability rule?
IR-U07-RDP05-03  Is historical P06 binding resolution distinct from U15 current terminal/scope authority?
IR-U07-RDP05-04  Is the single shared DB/transaction manager requirement consistent across RDP-01..04?
IR-U07-RDP05-05  Are metadata-only U06 checkpoint and projected eligibility correctly classified?
IR-U07-RDP05-06  Are U15 restore start vs outbound dispatch distinct separately-approved grant policies?
IR-U07-RDP05-07  Is parked P02 result required and early Tool/U02 business dispatch prohibited?
IR-U07-RDP05-08  Are all inherited CA/gates pending and none implicitly approved or deferred?
IR-U07-RDP05-09  Is U02 consumer idempotency separate from Outbox and clinical fact interpretation?
IR-U07-RDP05-10  Are P03/P04 not falsely made required for F8/P02 legality but preserved for future U02?
IR-U07-RDP05-11  Does every positive readiness require exact-head executable proof beyond mocks?
IR-U07-RDP05-12  Are PROFILE-A/PHI/real patients explicitly blocked?
```

Formal design-only candidate state:
```text
U07-RDP-05 = DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN
B-U07-RG-05 = OPEN / NOT_CLOSED
U07-RDP-01..04 = CONDITIONALLY_ACCEPTED_DESIGN
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED

CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-U15-DISPATCH-GRANT-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-EXECUTION-START-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-LANDING-BARRIER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01 = REQUIRED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Next gate:** `U07-RDP-05 Independent Design Review` at exact HEAD. Assess completeness of ownership, producer permissions, time/lock topology, current vs historical applicability, quality-gate evidence, and whether remaining assumptions should become explicit blocker remediation. No Runtime code, merge or production permissions follow from this file.
