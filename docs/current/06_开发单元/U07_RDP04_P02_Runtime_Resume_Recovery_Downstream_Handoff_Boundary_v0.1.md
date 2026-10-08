# U07-RDP-04 P02 Runtime Resume / Recovery / Downstream Handoff Boundary v0.1

> Unit: U07 — User Answer Resume / Idempotent Recovery
> Design scope: P02 durable runtime restoration, checkpoint compatibility, recovery, runtime owner fencing, U02 handoff transport boundary, trace and negative-effect oracles
> Parent design: RDP-01 and RDP-02 = CONDITIONALLY_ACCEPTED_DESIGN; RDP-03 = CONDITIONALLY_ACCEPTED_DESIGN after independent re-review [PR #279](https://github.com/cxjchelsea/AIdoctor/pull/279), exact review commit 885cb49c681685036df18bc94759857b467c53e0
> Runtime/source inspection baseline: main@6d4fd787600e3a57f01f3e17893e6d98893ac546
> Status: **DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN**
> This document does not authorize implementation, schema migration, merge, runtime activation, external U02 delivery, PHI, PROFILE-A or real patients.

---

## 1. Purpose and immutable authority boundaries

U07 RDP-04 makes a **durably accepted** canonical USER_ANSWER eligible for P02 runtime continuation, when a compatible persisted Runtime execution or independently validated reconstruction basis exists. P02 owns Runtime Thread/Run/Checkpoint truth and executable context. It does **not** own F8 business verdict, F3 Question/Gap or Pending Question Clinical State, Consultation lifecycle, P01 State Governance, U15 terminal truth, U02 Clinical Facts or outward answer delivery.

~~~text
RDP-01 canonical USER_ANSWER or original target of RESUME_REQUEST
 → RDP-02 F8 ACCEPTED (durable and original)
 → RDP-03 apply-root claim + fresh U15/currentness fence
 → RDP-04 P02 runtime compatibility + exact owner recovery
 → RDP-03 Stage C single F3-authorized K09/P01 commit
 → RDP-03 Stage D Consultation ACTIVE owner commit
 → RDP-03 Stage E APPLIED + durable U02 Outbox in ONE transaction
 → RDP-03 Stage F U15-approved dispatch grant
 → RDP-04 transport adapter dispatches SAME U02 handoff effect ID
 → U02 independently admits/interprets answer and commits clinical facts
~~~

Invariants:
- **Business Resume legality != Runtime Resume compatibility != Clinical State effect != U02 clinical interpretation.**
- F8 ACCEPTED is historical immutable truth even if P02 fails, expires, or cannot reconstruct.
- P02 cannot mark F8 APPLIED, cannot change Consultation ACTIVE, cannot clear Pending Question, cannot produce medical facts, and cannot dispatch U02 without Stage E + U15 Stage F grant.
- Runtime-ready without a later RDP-03/U15 effect fence conveys **no right** to mutate Clinical State.
- RDP-04 cannot make a newer Run/Thread/Checkpoint silently supersede old WAITING_USER provenance.

## 2. Inspected main assets and physical gaps

| Asset | Verified property from inspected main | Cannot infer |
|---|---|---|
| RuntimeThreadStateRecord | Versioned clinical_runtime_thread_state; ACTIVE / WAIT_CHECKPOINTED / AWAITING_USER; current run/checkpoint/wait refs | No U07 RESUMING/RESUMED transition API on current source |
| RuntimeThreadWaitTransitionService | U06 locks Thread and enters AWAITING_USER under SERIALIZABLE | Does not consume checkpoint or resume a Run |
| RuntimeWaitCheckpointRecord | Durable U06 wait metadata (consultation/thread/run/question/pending, delivery/wait refs, Clinical version, dependency/policy, fingerprint), status ACTIVE | **Not proof of serialized executable scheduler/program state or full reconstruction data** |
| RuntimeWaitCheckpointService | reserve() locks thread, checks exact matching checkpoint/run/wait and persists wait checkpoint | No P02 compatibility checker, full rehydration, or resumed owner-result store demonstrated |
| U06WaitCoordinator | Performs initialize→reserve→enterAwaitingUser→eligibility projection | A hash/projected eligibility is not an independently verified resume issuance or reusable runtime image |
| U07ResumeEligibilityProjector | Stable hash from parent wait and checkpoint, requires caller-supplied awaiting flag | Does not certify current wait, original issuance record or future recoverability |
| U07 Unit Spec §11 | Missing/stale checkpoint is Runtime repair/failure, not business REJECTED | No implicit safe replan or “use latest model” fallback |
| RDP-03 §9–10 | Single guarded DB staged Saga, stage C atomic P01, D owner ACTIVE, E APPLIED+Outbox, F dispatch-grant gate | Conditional design, not executable code or authorization |

Main evidence inspection is **bounded**, not a claim to have audited every runtime source/consumer. Before implementation RDP-05 must provide an exact-source audit of P02 checkpoint format/serializer, scheduler, run ownership, U02 consumer adapter, schema provenance, and transaction managers.

## 3. Frozen inbound and original identity

~~~text
P02ResumeRequestV1 {
  contract_version = "u07.p02.resume.v1",
  root_resume_effect_id, stable_p02_resume_request_id,
  canonical_answer_event_id, canonical_event_binding_ref, answer_digest,
  f8_accepted_decision_id, f8_claim_generation, f8_policy_version,
  consultation_id, question_id, parent_delivered_wait_effect_id,
  resume_eligibility_id + authoritative_issuance_ref,
  original_thread_id, original_run_id, original_checkpoint_id?,
  original_pending_question_ref, original_clinical_state_version,
  original_dependency_binding_ref?, original_question_policy_ref?,
  requested_runtime_contract_version, requested_harness_version?,
  trusted_scope_ref, owner_snapshot_ref, u15_terminal_generation,
  correlation_id, trace_id
}
stable_p02_resume_request_id =
  hash("u07-p02-resume-v1", root_resume_effect_id, original_thread_id,
       original_run_id, parent_delivered_wait_effect_id)
~~~

All ids must derive from immutable canonical input and original parent wait, **not** a network retry, latest scheduler run, new checkpoint or trace. A RESUME_REQUEST must resolve its existing USER_ANSWER and reuse this exact resume request ID. It is **not** a second runtime operation. Any protected identity conflict or changed answer digest/binding fails closed before a physical resume.

Required input evidence:
1. RDP-01 original canonical answer and durable binding, plus **authoritative U06 issuance**, not a reconstructed eligibility hash.
2. RDP-02 same-event ACCEPTED/winning claim in committed F8 ledger.
3. RDP-03 unique same-wait ApplyJournal root and currentness; F8 historical ACCEPTED alone is insufficient.
4. Original U06 WAITING_USER delivery/checkpoint/Question provenance; U15/current Consultation facts.
5. Independently authorized PROFILE-B synthetic, non-production scope. Real patients and PHI remain blocked.

## 4. P02 runtime compatibility and classification

P02 must produce a reproducible, **owner-issued** compatibility decision using runtime truth and original frozen basis. Check in strict order:

1. **Idempotent result/recovery first**: locate same stable P02 request; if already completed, verify immutable fingerprint and return original owner outcome; if in-flight, attach status, not start another execution.
2. **Owner binding**: Consultation, thread, run, parent wait, question, pending, current checkpoint ID and original issued eligibility must agree; protected conflicts fail closed. P02 never reassigns a foreign wait to a current active thread.
3. **Runtime Thread state**: expected AWAITING_USER (or an independently proven same-request RESUMING/RESUMED state). WAIT_CHECKPOINTED is incomplete wait; ACTIVE for a different effect is conflict. Thread/Run liveness and cancel/preempt state must be checked by P02 authority, not inferred by F8.
4. **Checkpoint physical integrity**: load exact original ID; verify digest/schema, persistent source, owner, serialized executable continuation payload if available, wait binding and authored version. A U06 metadata-only checkpoint is **INSUFFICIENT** for executable restoration.
5. **Compatibility**: frozen scheduler/harness/runtime contract, capability/registry pin, workflow/state-machine version, tool bindings and allowed versions; no rebinding to latest configuration, no policy regeneration and no unaudited migration.
6. **Revalidation**: compare original clinical/dependency/question policy refs with owner-authorized currentness criteria; Clinical State changes are validated by their owners, not unilaterally accepted by P02.
7. **Safety/terminal fence**: before a novel physical Run operation, verify authorized RDP-03/U15 runtime resume barrier. Concurrent terminalization after a P02 owner effect is handled as partial-success reconciliation, never “business REJECTED.”

~~~text
P02CompatibilityDecisionV1 {
 stable_p02_resume_request_id, immutable_request_fingerprint,
 status = COMPATIBLE_CHECKPOINT | REHYDRATE_ELIGIBLE |
          INCOMPATIBLE | INSUFFICIENT_EVIDENCE | BLOCKED_OWNER |
          DEPENDENCY_UNAVAILABLE,
 cause_code, original_checkpoint_ref?, executable_payload_ref?,
 verified_runtime_contract_ref?, original_version_vector,
 frozen_binding_fingerprint, authorized_rehydrate_basis_ref?,
 owner_read_epoch + fence_ref,
 trace_ref
}
~~~

Only COMPATIBLE_CHECKPOINT or separately certified REHYDRATE_ELIGIBLE may initiate runtime restoration. INCOMPATIBLE/BLOCKED_OWNER are not F8 verdicts; evidence uncertainty is INSUFFICIENT_EVIDENCE, not optimistic COMPATIBLE.

## 5. Selected V1 runtime recovery strategy and non-applicable cases

**Single V1 decision policy: EXACT_RESTORE_ELSE_AUTHORIZED_REHYDRATE_ELSE_GOVERNED_FAILURE.** No generic LLM reconstruction, speculative replay, silent fresh-thread start or latest-checkpoint adoption.

### Path A — exact checkpoint restore

- Checkpoint contains independently proven executable continuation image (program counter/task state, frozen workflow/registry/capability versions, stable pending operation identity, runtime data required for deterministic continuation) with an integrity digest.
- P02 verifies it equals the original U06 wait provenance, then atomically claims the exact Thread/Run for the same P02 request under owner-level lock/CAS.
- Resume to the **post-user-wait boundary** only; no replay of U06 delivery, Question selection or previously executed tools, and no inference of U02 facts.
- If current main metadata checkpoint lacks executable content, **Path A NOT_APPLICABLE**.

### Path B — bounded historical rehydration

Only when all are present:
- U06 original authoritative delivered-wait and eligibility issuance, original answer binding, original **approved deterministic reconstruction recipe**, immutable historical dependency snapshots or verifiable owner references, version-compatible runtime/scheduler state, and complete replay-safety classification for prior steps.
- Owner-approved reconstruction must derive the execution context at the waiting boundary **without replaying external effects** or silently changing the original Question/answer. It may reuse original run identity or authoritatively record a versioned continuation run linked one-to-one with original run; fresh IDs are never invented per retry.
- Before rehydrate, P02 must prove source provenance, exact compatibility/semantic equivalence, and original pending tool effects are known complete or explicitly non-replayed. If any step is UNKNOWN or relies on unverified prior tool effects, **no rehydrate**.
- B requires **CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED** to approve P02 authority, reconstruction schema, proof and recovery source. The current U06 wait metadata alone does not satisfy this recipe.

### Path C — governed failure

If A and B cannot be positively proven: output **INCOMPATIBLE** or **INSUFFICIENT_EVIDENCE** with owner reason, preserve original F8 ACCEPTED, protect same-wait winner/ApplyJournal, and route U14/P02 owner recovery. Do **not** create another Question, synthesize U06 issuance, clear Clinical Pending, set Consultation ACTIVE, or handoff to U02.

### Compatibility result table

| Evidence | P02 outcome | Effects |
|---|---|---|
| Exact executable image valid and version-compatible | COMPATIBLE_CHECKPOINT → RESTORE | one owner-controlled resume attempt |
| Metadata-only checkpoint but proven governed deterministic recipe | REHYDRATE_ELIGIBLE → REHYDRATE | one owner-controlled reconstructed continuation |
| Metadata-only checkpoint; no approved recipe | INSUFFICIENT_EVIDENCE | no runtime execute / K09 |
| Checkpoint absent; original eligibility issuance and approved recipe both verified | REHYDRATE_ELIGIBLE | not a business reject; repair only under P02 |
| Schema incompatibility or unresolved historical capability binding | INCOMPATIBLE | no silent “latest” migration |
| Current Thread foreign run/new wait or U15 terminal first | BLOCKED_OWNER | no runtime execute |
| P02 owner store temporarily inaccessible | DEPENDENCY_UNAVAILABLE | DEFER/retry exact request only |

## 6. P02 operation claim, runtime transitions and concurrency

**Proposed new physical components, not existing main code:**
- P02ResumeJournalV1 (stable ID unique, root/wait unique, request fingerprint, owner thread row_version, original checkpoint, selected Path A/B, phase, owner fence, runtime result and trace).
- P02ResumeThreadTransitionService, RuntimeResumeCompatibilityEvaluator, RuntimeRehydrationAuthority, P02RunContinuationResultStore.

~~~text
P02ResumeJournalV1 {
 stable_p02_resume_request_id PK, root_resume_effect_id UNIQUE,
 original_thread_id, original_run_id, original_checkpoint_id?,
 canonical_answer_event_id, parent_wait_effect_id,
 immutable_request_fingerprint, frozen_dependency_version_vector,
 selected_path, current_status,
 owner_thread_row_version, owner_resume_generation,
 consultation_u15_fence_version, runtime_continuation_run_id?,
 runtime_result_id?, runtime_result_digest?, trace_ref, failure_reason
}
~~~

Owner runtime phases:
~~~text
NONE → INTENT_DURABLE → CLAIMED
     → RESTORING_OR_REHYDRATING
     → RESUMED_VERIFIED
     → OBSERVED_BY_U07_APPLY
or → BLOCKED_OWNER / INCOMPATIBLE / INSUFFICIENT_EVIDENCE
or → UNKNOWN_COMMIT / RECONCILIATION_REQUIRED
~~~

Physical constraints:
- One stable resume identity owns one Thread/Run/wait continuation; same canonical retry attaches, different effect cannot claim same wait.
- The authoritative Thread row uses PESSIMISTIC_WRITE / @Version, but U07 **must add** governed transition status and replay receipts; U06 only implements reserve and enter-awaiting.
- **Lock order:** shared Consultation/U15 guard FIRST for first physical claim, then Runtime Thread row, then checkpoint/owner journal and effect keys in deterministic order. U15/F3 owner bridges must agree or V1 NOT_READY.
- Do not hold a DB transaction across an external runtime executable step or tool call. Before invocation, persist the exact operation identity and fencing token; after execution, reconcile outcome against persisted owner evidence.
- If U15 terminalizes after claim but before physical execution, a fresh owner-authorized execution-start fence must block new Run actions. If U15 terminalizes while the execution is in flight, owner control must cancel/suspend/fence subsequent effects; completion may be logged, but is not authorization to apply Clinical State.
- Unknown physical execution result must be resolved from P02 owner journal plus durable runtime result; **never rerun unclassified steps**. An original unknown tool effect cannot be reissued merely to “recover the checkpoint.”
- Pre-existing Runtime task cancellation/preemption/safety barrier cannot be downgraded by U07.

## 7. Exact P02 result contract and stage-B handoff to RDP-03

~~~text
P02ResumeResultV1 {
 contract_version, stable_p02_resume_request_id, root_resume_effect_id,
 canonical_answer_event_id, original_wait_effect_id,
 original_thread_id, original_run_id, original_checkpoint_id?,
 selected_recovery_path, runtime_continuation_run_id,
 result_status = RESUMED_VERIFIED | ALREADY_RESUMED |
                 INCOMPATIBLE | BLOCKED_OWNER | INSUFFICIENT_EVIDENCE |
                 DEPENDENCY_UNAVAILABLE | UNKNOWN_COMMIT,
 owner_commit_ref?, owner_thread_version_after?,
 executable_state_digest?, original_history_binding_fingerprint,
 owner_compatibility_decision_ref, runtime_execution_receipt_ref?,
 u15_fence_observed_ref, checkpoint_repair_ref?,
 failure_reason_code?, trace_ref
}
~~~

**RESUMED_VERIFIED** requires durable owner result, matching original wait/run, valid continuation identity, reconciled pending step results and a readback proving the Thread/Run state. **ALREADY_RESUMED** is a read-only replay of the same root with original result evidence; no further execution. If P02 can only establish “metadata checkpoint exists”, it cannot return RESUMED_VERIFIED.

RDP-03 may progress stage B only on RESUMED_VERIFIED or ALREADY_RESUMED **with exact owner-result proof** and fresh C-stage U15/Clinical owner fence. It cannot treat a P02 timeout, provisional COMPATIBLE, metadata-only checkpoint or in-memory callback as stage B success. F8 historical decision remains unchanged on all result statuses.

## 8. Recovery after crashes and unknown outcomes

| Window | Reconciliation | Forbidden |
|---|---|---|
| Original F8 ACCEPTED but no P02 request intent | derive same ID, owner-claim when U15 still authorizes | new F8 verdict |
| P02 request intent committed, caller crashed | reattach same P02 journal/root | second operation identity |
| Thread CLAIMED but before runtime executes | read matching claim/status, verify start fence, execute once | blind new Run |
| Executable checkpoint corrupt/missing after claim | INSUFFICIENT_EVIDENCE / owner repair if approved | fake RESUMED_VERIFIED |
| Rehydrate prepared before owner commit | on restart inspect durable continuation identity and recipe hash | redo unclassified effects |
| Runtime restored but owner result not persisted | read Thread/Run durable runtime state; if effect unknown → UNKNOWN_COMMIT | claim success from log |
| P02 owner result committed but U07 stage B not acked | return same RESUMED_VERIFIED and reconcile ApplyJournal | second restoration |
| U15 terminal before P02 physical start | BLOCKED_OWNER and no newly executed steps | force Resume |
| U15 terminal during P02 execution | cancel/suspend/fence subsequent steps; owner result recorded; RDP-03 C blocks | pretend Clinical State applied |
| P01 C fails after P02 RESUMED_VERIFIED | preserve runtime fact, journal blocked/recover via U14/P01 | revert F8 ACCEPTED or force C |
| Consultation D or APPLIED E blocked | do not reenter external P02; reconcile owner stages | double runtime resume |
| U02 delivery receipt lost | route via same granted Outbox effect, U02 idempotency query | re-run P02 for response delivery |
| Commit outcome UNKNOWN | new transaction query by stable P02 ID + version/fingerprint | optimistic success or blind retry |

## 9. Downstream U02 handoff: P02 is not the dispatcher

After stage B, **RDP-04 does not send the raw answer**. It must wait for **RDP-03 Stage E** to commit the unique Outbox together with APPLIED, and **Stage F** to commit the U15-controlled immutable dispatch grant. P02's runtime success or U07 APPLIED alone is not a send permission.

~~~text
U07ToU02HandoffV1 {
 contract_version = "u07.u02.handoff.v1",
 handoff_effect_id = u02_handoff_effect_id,
 root_resume_effect_id, canonical_answer_event_id,
 immutable_answer_binding_ref, synthetic_payload_digest,
 original_question_id, parent_delivered_wait_effect_id,
 consultation_id, scope_ref, clinical_state_version_after_c,
 f8_accepted_ref, p02_resumed_result_ref,
 f3_p01_answer_and_pending_commit_ref,
 consultation_active_effect_ref, applied_outbox_commit_ref,
 u15_dispatch_grant_id, u15_dispatch_policy_version,
 correlation_id, trace_id
}
~~~

- Producer must reference **original RDP-01 committed synthetic bytes/binding**, not recomputed text, recent summary, a model interpretation or client payload. For any future PHI/PROFILE-A, transport permissions, encryption and retention require a separate clinical authorization not supplied here.
- Unique dispatch source is Outbox `handoff_effect_id`. RDP-04 worker cannot mint another event ID or U02 Clinical Fact ID.
- Outbox transition PENDING → DISPATCH_AUTHORIZED must follow RDP-03 §10.2 F under Consultation/U15 guard. U15 wins before grant → BLOCKED_TERMINAL, no U02 send. Grant wins before later U15 terminal → **same granted effect only** may continue, **if and only if** explicit U15-approved irrevocable-grant policy and U02 consumer governance are available. No DB lock assumed across a network call.
- Worker marks DISPATCH_STARTED with claim generation, then delivers at least once to U02 using stable idempotency key. Unknown U02 response → query consumer receipt/retry same ID; never assert exactly-once Clinical Facts from producer-side outbox alone.
- U02 independently validates authorized intake, digest and scope, then owns clinical observation interpretation and P01 Clinical Facts. Runtime does not prescribe the clinical meaning of a USER_ANSWER.
- External delivery requires **CA-U07-RDP03-U15-DISPATCH-GRANT-01** and RDP-04 U02 consumer replay/authority approval, not the existence of P02 RESUMED_VERIFIED.

## 10. Failure routing and correction ownership

| Failure domain | Owner | RDP-04 effect |
|---|---|---|
| RDP-01 provenance/eligibility not established | U06/Foundation | no P02 invocation; blocked ingress |
| F8 EXPIRED/REJECTED/DUPLICATE or DEFER | F8 | no new P02; replay old result when relevant |
| P02 checkpoint absent, recipe eligible | P02 | governed deterministic rehydrate |
| P02 incompatible/corrupt/unknown or missing owner capability | P02/U14 | no Clinical State mutation, outcome evidence |
| F3/P01 commit conflicts after Resume | F3/P01/U14 | do not repeat P02; preserve historical evidence |
| U15 cancellation/expiry before mutation | U15 | block C/D/E/F as applicable; never rewrite F8 |
| APPLIED complete but Outbox/dispatch grant unavailable | RDP-03/U15/RDP-04 worker | durable reconciliation, no implicit send |
| U02 consumer error | U02/U14 | replay same handoff if authorized, no P02 re-execution |
| Clinical safety conflict | K0/U15/U14 | fail closed or escalate; no autonomous clinical replan |

No failure class may be silently converted into user-facing “answered successfully” or fake APPLIED.

## 11. P05 Trace and P06 scope/version binding

~~~text
P02ResumeTraceV1 {
 trace_id, correlation_id, canonical_answer_event_id,
 f8_accepted_ref, root_resume_effect_id, p02_request_id,
 consultation_ref, thread_ref, original_run_ref, original_checkpoint_ref?,
 original_eligibility_issuance_ref, parent_wait_effect_ref,
 runtime_compatibility_status + cause,
 recovery_path + authoritative_recipe_ref?,
 original_harness_registry_version_vector,
 thread_claim_generation, selected_continuation_run_ref?,
 u15_fence_generation + owner_read_epoch,
 result_status + owner_receipt_ref?, crash_reconcile_reason?,
 stage_b_apply_journal_ref, downstream_outbox_ref?,
 u15_dispatch_grant_ref?, u02_consumer_ack_ref?, error_class
}
~~~

Trace must distinguish requested, claimed, physical executed, committed, verified and reconciled. Trace is neither Runtime Thread truth nor Clinical State. Bind scope, consultation, capability/registry snapshot and synthetic PROFILE-B; no real identifiers, raw answer content or sensitive payload in trace. Failing a mandatory owner audit trail must fail closed before novel effect; asynchronous trace display failure cannot undo authoritative durable owner commit.

## 12. Exact RDP dependency and authorization register

| Dependency | State | Readiness condition |
|---|---|---|
| RDP-01 canonical binding / full Foundation audit | CONDITIONALLY_ACCEPTED / gate NOT_PASSED | SHA-pinned Foundation compatibility audit + original payload/issuance proof |
| CA-U06-U07-ELIG-ISSUANCE-01 | REQUIRED / NOT_AUTHORIZED | durable original issuance, not projected eligibility hash |
| RDP-02 F8 decision and same-wait claim | CONDITIONALLY_ACCEPTED | durable first-verdict/claim, U15 shared fence proof |
| CA-U07-RDP02-U15-SHARED-FENCE-01 | REQUIRED / NOT_AUTHORIZED | U15 Consultation owner serial fence |
| RDP-03 staged apply / APPLIED+Outbox | CONDITIONALLY_ACCEPTED | owner C/D/E physical guard, commit and readback |
| CA-U07-RDP03-F3-ANSWER-BRIDGE-01 | REQUIRED / NOT_AUTHORIZED | F3 owner Question/Gap/Pending authorization |
| CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 | REQUIRED / NOT_AUTHORIZED | P01 Clinical patch shares Consultation/U15 commit-time fence |
| CA-U07-RDP03-U15-DISPATCH-GRANT-01 | REQUIRED / NOT_AUTHORIZED | U15 approves grant-first vs terminal ordering and bounded later send |
| CA-U07-RDP04-P02-REHYDRATE-OWNER-01 | REQUIRED / NOT_AUTHORIZED | exact owner rehydrate recipe, executable state and replay safety |
| CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01 | REQUIRED / NOT_AUTHORIZED | U02 scope/admission, stable handoff ID, receipt query, typed retry semantics |
| P02 Thread/Run/Checkpoint resume physical APIs | NOT_IMPLEMENTED / NOT_VERIFIED | runtime state migration, resumable payload, owner lock/CAS, result receipts |
| RDP-05 cross-provider compatibility | FUTURE DESIGN | chosen DB topologies, dialect-specific time/fence, registry, ownership, P02 execution semantics |
| RDP-06 verification evidence | FUTURE DESIGN | synthetic concurrent/crash/negative-effects tests with exact heads |
| PROFILE-A / real patient / PHI / production | BLOCKED | separate safety, regulatory and clinical activation authorization |

No “conditional design acceptance” removes any of these gates.

## 13. Design-only verification oracle catalog

| ID | Scenario | Required outcome |
|---|---|---|
| P02-T01 | F8 not ACCEPTED | zero new P02 invocation / Runtime effect |
| P02-T02 | original F8 ACCEPTED, same wait/root, valid executable checkpoint | compatible restore, one owner result |
| P02-T03 | identical RESUME_REQUEST points to original canonical answer | reattach same P02 request and owner outcome |
| P02-T04 | same canonical event, protected digest changed | fail-closed identity conflict |
| P02-T05 | RDP-01 U06 original issuance absent, only derived hash | no resume; blocked prerequisite |
| P02-T06 | checkpoint record contains only U06 metadata | no executable restore evidence |
| P02-T07 | checkpoint absent but approved deterministic rehydrate recipe and full evidence | REHYDRATE_ELIGIBLE |
| P02-T08 | checkpoint absent without recipe | INSUFFICIENT_EVIDENCE, no runtime effect |
| P02-T09 | schema/version incompatible and no reviewed migration | INCOMPATIBLE, no rebind to latest |
| P02-T10 | Thread AWAITING_USER but wrong run/wait/Question | BLOCKED_OWNER, no hijack |
| P02-T11 | competing roots attempt same thread/wait resume | at most one owner claim |
| P02-T12 | Thread CLAIMED then crash | exact owner claim reconciliation, no second identity |
| P02-T13 | restore finishes, owner result before reply | return same verified owner result |
| P02-T14 | physical execution outcome unknown and tool step replay-unsafe | UNKNOWN_COMMIT, no re-execution |
| P02-T15 | U15 wins before physical resume start | block runtime execution |
| P02-T16 | U15 terminalizes while P02 executing | halt follow-on effects; preserve owner facts and F8 historical ACCEPTED |
| P02-T17 | runtime succeeds, P01 Stage C later conflicts | no second Runtime restore; owner reconciliation |
| P02-T18 | P02 RESUMED_VERIFIED but no Stage E APPLIED/outbox | zero U02 dispatch |
| P02-T19 | Stage E APPLIED+PENDING, no Stage F grant | zero U02 dispatch |
| P02-T20 | U15 cancels before dispatch grant | BLOCKED_TERMINAL outbox, zero send |
| P02-T21 | grant commits before U15 terminal, network send later | same immutable effect may continue ONLY with approved U15 grant policy |
| P02-T22 | dispatch grant commit unknown | no send until exact durable grant proven |
| P02-T23 | U02 ACK lost, same granted handoff ID | replay/query receipt, no new Clinical Fact identity |
| P02-T24 | checkpoint compatible but original registry binding differs | INCOMPATIBLE unless controlled approved migration |
| P02-T25 | external tool effects in past run unclassified | no rehydrate/replay |
| P02-T26 | P02 result COMMITTED but runtime readback not aligned | RECONCILIATION_REQUIRED, not RESUMED_VERIFIED |
| P02-T27 | U02 consumer lacks idempotent accept/replay query | NOT_READY, no delivery |
| P02-T28 | client or trace carries actual PHI / PROFILE-A | blocked by synthetic-only authorization |
| P02-T29 | owner resuming state stale due to concurrent run claim | CAS conflict, no new continuation |
| P02-T30 | Runtime metadata-only resume falsely marked complete | verification fails, no RDP-03 Stage C |
| P02-T31 | DB guard cannot be shared for physical claim/starting stage | NOT_APPLICABLE/NOT_READY, no optimistic resume |
| P02-T32 | remote result committed, duplicate request arrives | same stable owner result, zero repeated side effects |

These are **32 unexecuted design oracles**; RDP-06 must later supply runnable tests, actual DB evidence, negative outbound-call assertions, provenance and exact owner receipts.

## 14. Independent review questions

~~~text
IR-U07-RDP04-01  Does metadata-only U06 checkpoint fail closed unless executable image or authorized rehydrate evidence is proven?
IR-U07-RDP04-02  Is P02 original Thread/Run/wait ownership fully fenced, including missing checkpoint and foreign current run?
IR-U07-RDP04-03  Can runtime continuation be called RESUMED_VERIFIED only on authoritative executable-state result and readback?
IR-U07-RDP04-04  Does P02 retry/rehydrate prevent replay of unknown or already-applied external tool effects?
IR-U07-RDP04-05  Does a post-P02 U15 terminal prevent Stage C/D/E without rewriting historical F8?
IR-U07-RDP04-06  Is RDP-04's transport adapter prohibited from dispatching before APPLIED+outbox AND U15 durable grant?
IR-U07-RDP04-07  Is U02 consumer replay/ack proof separate from producer outbox and Clinical Fact authority?
IR-U07-RDP04-08  Are all new/inherited controlled amendments explicit blockers?
IR-U07-RDP04-09  Is runtime physical lock/order and execution-start fence implementable against current main, or explicitly NOT_READY?
IR-U07-RDP04-10  Do F8/P02/ApplyJournal/Outbox/P05 have non-overlapping authority and consistent stable IDs?
~~~

## 15. Formal candidate outcome / next step

~~~text
U07-RDP-04 = DESIGN_CANDIDATE / READY_FOR_INDEPENDENT_DESIGN_REVIEW / NOT_FROZEN
B-U07-RG-04 = OPEN / NOT_CLOSED
U07-RDP-01 = CONDITIONALLY_ACCEPTED_DESIGN
U07-RDP-02 = CONDITIONALLY_ACCEPTED_DESIGN
U07-RDP-03 = CONDITIONALLY_ACCEPTED_DESIGN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-U15-DISPATCH-GRANT-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01 = REQUIRED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real patients = BLOCKED
~~~

**Next gate: U07-RDP-04 Independent Design Review** against an exact HEAD. It must check the real metadata-only checkpoint gap, bounded executable replay-safety contract, stable continuation/claim under U15, and strict Stage E/F handoff ownership. No code, migrations, merge, external send or production activation is authorized here.
