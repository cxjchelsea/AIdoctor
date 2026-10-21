# U07-RDP-04 Independent Design Review v0.1

> Review date: 2026-10-08
> Design PR: [#280](https://github.com/cxjchelsea/AIdoctor/pull/280)
> **Exact reviewed design HEAD**: `b8816272f1d032515164329644194901eded06aa`
> Design file blob: `9db64f406fee6fb2b9d88636202f88356c0e9785`
> Base: `885cb49c681685036df18bc94759857b467c53e0` (RDP-03 conditional design review)
> Source reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / THREE DESIGN BLOCKERS.** Design-only; no code, executable evidence, merge or authorization.

## 1. Independent method / evidence

Fetched exact PR #280 and its only changed document, read all sections §§1–15 and 32 sequential unique design oracles `P02-T01..P02-T32`, checked the U07 Unit Spec §11–13 and RDP-03 Stage B/C/D/E/F against this contract, and previously inspected main's `RuntimeThreadStateRecord`, `RuntimeThreadWaitTransitionService`, `RuntimeWaitCheckpointRecord`, `RuntimeWaitCheckpointService`, `U06WaitCoordinator`, `U07ResumeEligibilityProjector`.

Main currently demonstrates a versioned Thread row with WAIT_CHECKPOINTED / AWAITING_USER, and a durable **wait-provenance metadata** checkpoint with fields such as thread/run/question/delivery/wait refs, versions and fingerprint. It does NOT demonstrate a serialized executable continuation, U07 RESUMING/RESUMED owner transition, a replay-safe historical reconstruction recipe, a P02 resume result ledger, a U15 execution-start barrier or an implemented U02 handoff consumer. Any claim that metadata alone gives runnable resume is correctly rejected in the candidate.

No source code was modified and no database/runtime verification was performed by this review.

## 2. Strengths retained

1. Distinguishes original F8 business ACCEPTED from P02 runtime compatibility, RDP-03 governed APPLIED and U02 Clinical Facts.
2. Uses immutable original canonical answer and same stable `stable_p02_resume_request_id` for USER_ANSWER replays and RESUME_REQUEST reattachment.
3. Defines exact restore / authorized historical rehydration / governed failure, explicitly refusing metadata-only checkpoint, unverified original issuance and latest-capability silent rebinding.
4. Rejects physical tool replay when historical external effects are unknown; failure must not rewrite historical F8 ACCEPTED.
5. RDP-03 Stage E APPLIED+Outbox and U15 Stage F durable dispatch grant are necessary; P02 success alone cannot justify external U02 delivery.
6. Carries upstream Foundation audit, U06 eligibility, U15 common fence, F3/P01 and U15 dispatch-grant controlled amendments, and marks new rehydrate/U02-consumer capabilities NOT_AUTHORIZED.

These are good boundary requirements but do not close the blockers below.

## 3. BF-U07-RDP04-IR-01 — Execution-start vs U15 terminalization lacks a linearization contract

**OPEN / EXECUTION_START_FENCE_RACE**.

§6 describes a persisted owner claim under Consultation/U15 guard followed by a separate “fresh owner-authorized execution-start fence” before invoking the Runtime outside the DB transaction. But it does not freeze the authoritative **execution-start grant** object, which transaction records it, how a concurrently committed U15 terminal event orders against it, or whether grant-first executable work is permitted to continue after terminalization. Checking U15 and releasing a lock before a physical call is vulnerable to:

~~~text
T1  P02 checks Consultation/U15 under row lock: wait still current
T2  P02 releases transaction / lock
T3  U15 commits terminalization, invalidates wait
T4  P02 begins restoring/executing the Runtime from old pre-check
~~~

This violates the candidate's assertion “U15 wins before physical resume start -> no executed steps”. The same design already handles the analogous outbox/network window with a **durable dispatch grant**; P02 needs a separately reasoned and explicitly authorized runtime-start ordering rule, not a lock claimed to span external execution.

**Required amendment:** select one durable `P02ExecutionStartGrantV1` or equivalent owner-approved start linearization protocol with immutable root/run/wait/fence identity, atomic Consultation/U15 guarded CAS, phase change and owner policy; specify U15-first vs grant-first vs grant-commit-unknown races; grant expiry, cancellation observation, already-started work and exactly which effects may continue; give concrete lock order and crash oracles. If no U15-approved grant or equivalent operation fencing can be implemented, first runtime execution must fail closed and U07 remains NOT_READY. Do not inherit the **U02 dispatch grant** as implicit P02 execution authorization.

## 4. BF-U07-RDP04-IR-02 — “post-user-wait restore” may execute unauthorized downstream effects

**OPEN / P02_CONTINUATION_SIDE_EFFECT_BOUNDARY**.

§5 Path A says resume at the post-user-wait boundary and §6 describes actual executable continuation. Yet the RDP-03 agreed protocol requires:
- P02 only establishes a verified Runtime state and **Stage B** durable result;
- then F3/P01 **Stage C**, Consultation **Stage D**, APPLIED+Outbox **Stage E**;
- finally U15-granted **Stage F** U02 delivery.

A resumed workflow may automatically execute nodes/tools/side effects immediately after the wait point (e.g. U02 handoff, new workflow planning, data mutations) **before C/D/E/F** if the saved program counter simply advances. Saying “P02 cannot dispatch U02” at the service API level is not a guard against the resumed worker's internal steps.

**Required amendment:** freeze an explicit **resume landing barrier** and a bounded set of allowed pre-Stage-C runtime operations (ideally restore/validate runtime cursor/owner receipt only, no executable post-wait business nodes). Design how executable Thread/Run is parked at `RESUMED_READY_BUT_NOT_DISPATCHED` or equivalent after owner recovery, and which authority subsequently authorizes continuation after RDP-03 C/D/E and U15 Stage F. Explicitly prohibit U02, model/tool external effects and new Clinical state mutation from any resumed worker prior to owner gates, irrespective of program counter. Declare what exactly `RESUMED_VERIFIED` means—valid durable runtime state vs actual resumed downstream execution—and ensure RDP-03 Stage B consumes only the former. Add negative executable-step oracles and crash scenarios.

Without such a barrier, the design's correct narrative separation cannot be enforced physically.

## 5. BF-U07-RDP04-IR-03 — Rehydration replay-equivalence and unknown-effect authority not closed

**OPEN / REHYDRATION_PROOF_CONTRACT_INCOMPLETE**.

§5 Path B lists an “approved deterministic reconstruction recipe”, complete prior replay-safety classification and immutable historical snapshots, but does not define the exact **persisted proof contract** or boundary of reconstructible state. §6 permits a one-to-one continuation Run and says unknown physical outcomes must not be replayed, but no stable continuation derivation/operation journal linkage or authority for deciding “historical effects known complete” is frozen. A recipe citation alone does not guarantee that a newly constructed executable Run is semantically the same continuation, particularly when the original checkpoint contains only waiting provenance.

A concrete failure:
- U06 delivered a Question, persisted only metadata; a prior workflow tool returned with ambiguous external completion.
- Later the checkpoint is missing. The proposed rehydrate code could reconstruct a plausible execution cursor from Question/Run IDs, falsely marking prior tool work “not replayed” without a complete owner-effect ledger.
- New continuation runs or automatically reruns the pending tool, yielding duplicate side effects.

**Required amendment:** freeze `P02RehydrateProofV1` covering exact immutable original serialized execution plan/cursor or reviewed deterministic equivalent, frozen policy/registry/harness versions, replay manifest of every prior effect (owner identity + terminal completion evidence + replay safety), runtime state digest, unique continuation Run derived from original root, source availability and status of unknown/in-flight effects; define who signs/validates these facts, how to reject a missing element, and when `REHYDRATE_ELIGIBLE` is forbidden. For V1, **fail-closed** if no executable checkpoint or full owner-certified reconstruction proof; do not infer proof from U06 metadata or from a standalone deterministic recipe name. Register and keep `CA-U07-RDP04-P02-REHYDRATE-OWNER-01` unapproved until physical proof is reviewed.

## 6. Further review notes (not separate blockers)

- The new `CA-U07-RDP04-U02-CONSUMER-IDEMPOTENCY-01` is correctly pending. No producer-side Outbox or dispatch grant alone proves exactly-once U02 Clinical Fact mutation.
- Lock hierarchy and transaction-manager locality across Consultation/U15/P02 Thread and ApplyJournal have not been physically verified. Carry into RDP-05; no optimistic cross-store execution.
- F8 original ACCEPTED and RDP-03 APPLIED truth remain immutable on runtime errors.
- P02 result enum `ALREADY_RESUMED` must only read back the same root's authoritative effect; reconcile unknown result before retry.
- Existing Unit Spec logical runtime outcomes do not freeze exact enum names; RDP-04 naming is allowable if downstream contracts align and independent evidence proves behavior.
- RDP-04 should retain P05 Trace as evidence, never as an execution permission.

## 7. Gate result and next step

~~~text
U07-RDP-04 Independent Design Review = REVISE_REQUIRED
Reviewed exact HEAD = b8816272f1d032515164329644194901eded06aa

BF-U07-RDP04-IR-01 = OPEN / EXECUTION_START_FENCE_RACE
BF-U07-RDP04-IR-02 = OPEN / UNAUTHORIZED_CONTINUATION_EFFECT
BF-U07-RDP04-IR-03 = OPEN / REHYDRATION_PROOF_INCOMPLETE

B-U07-RG-04 = OPEN
U07-RDP-04 = DESIGN_CANDIDATE / NOT_FROZEN

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
Production / PROFILE-A / PHI / real-patient = BLOCKED
~~~

**Next permitted step:** U07-RDP-04 Targeted Design Remediation on original PR #280 to freeze the execution-start grant/U15 causal policy, prevent early resumed-worker side effects, and define complete rehydrate-proof/unknown-step authority; then an exact-head U07-RDP-04 Targeted Independent Design Re-Review. No merge, runtime edits or authorizations.
