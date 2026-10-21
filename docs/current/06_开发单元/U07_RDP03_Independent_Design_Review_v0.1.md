# U07-RDP-03 Independent Design Review v0.1

> Review date: 2026-10-08
> Design PR: [#276](https://github.com/cxjchelsea/AIdoctor/pull/276)
> **Exact reviewed design HEAD:** `253032f718d85a0bbbb073ccbaee0d04adf961e7`
> Review baseline: RDP-01 conditional review PR #270; RDP-02 conditional review PR #275 @ `56f28c286951ec1ec79a59aec5be776391017020`
> Source evidence baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED / THREE DESIGN BLOCKERS.** Review-only. No implementation authorization.

## 1. Evidence and scope

Inspected exact-head `docs/current/06_开发单元/U07_RDP03_State_Ownership_K09_P01_Idempotent_Apply_Trace_Contract_v0.1.md` (15 sections, 28 unexecuted design oracles). Independently compared logical ownership against Unit Spec v0.1, U05/U06 RDP-03 governed K09/P01 design, and bounded main source inspection: `ConsultationRecord`, `ConsultationRepository.findByIdForUpdate`, `ConsultationWaitTransitionService`, `U06SyntheticP01Runtime`, `CanonicalEffectLedger`. Existing Consultation locks and a U06-specific P01 writer **do not** establish executable U07 Apply, F3 answer owner bridge or U15 common fencing.

## 2. Accepted design properties

- F8 immutable ACCEPTED is separate from P02 resume success, K09/P01 proposal/commit, authoritative Clinical State, U07 APPLIED and U02 clinical fact formation.
- Explicit F3 ownership of Question/Gap and P01/G2 exclusive Clinical State mutation; U07 cannot unilaterally set ANSWERED or create Clinical Facts.
- Stable root_resume_effect_id per canonical USER_ANSWER and unique same-wait key; distinct canonical answer event cannot create a second ordinary apply effect.
- P01 result/readback distinctions and fail-closed `UNKNOWN/CONFLICT/REJECTED/readback mismatch`; no stale-version patch regeneration.
- Consultation WAITING_USER→ACTIVE is governed by Consultation owner with row-version/wait-ref verification, not by unverified P02 success.
- Durable ApplyJournal is an explicit **proposed** authority/evidence record, not an existing implementation or a substitute for owner truth.
- P05 Trace contains provenance, owner decisions, proposals, commit references and readback but is **not** a state authority. No PHI/PROFILE-A production capability claimed.
- RDP-01 Foundation audit, U06 issuance amendment, RDP-02 U15 fence amendment, and newly registered F3 answer bridge remain explicitly NOT_AUTHORIZED / blocked.

## 3. BF-U07-RDP03-IR-01 — unresolved single physical cross-resource apply topology

**BLOCKER / AUTHORITY_ATOMICITY_UNFROZEN.**

§9 states that Clinical State and Consultation may belong to separate resource managers, then leaves an alternative between atomic transaction and durable saga/compensation. §10.2 says F3/P01 “if atomic then one patch, otherwise owner actions with compensation”. The contract does **not choose one V1 topology** or bind its prerequisites; U07 cannot know whether P01 Question and Pending changes, Consultation ACTIVE, and ApplyJournal milestones are in one commit scope or independent commits.

**Required remediation:**
1. Freeze one synthetic V1 topology and the authoritative storage/transaction owner for each stage: F3 Question/Gap + Pending Question, Consultation transition, ApplyJournal and outbox. The choice may be a conservative durable saga with independent transactions, but must be singular, not “either/or”.
2. For each stage specify durable **intent-before-effect**, exact owner commit identity, readback, a durable completion/reconciliation record, and hard invariants when a partially committed prior stage conflicts with newer U15/F3/Consultation state.
3. Define whether Question/Gap and Pending consumption form one P01 StatePatch or require a reviewed cross-owner bridge. Do not allow duplicate clinical version advances on replay.
4. Carry required owner-specific U07 P01 grants, F3 bridge and physical compatibility tests to RDP-05/06. No absent adapter should be declared existing.

Evidence: §§7–10, particularly §9 “same atomic resource manager or durable saga”, §10.2 Tx C–D.

## 4. BF-U07-RDP03-IR-02 — APPLIED and unique U02 handoff intent durability

**BLOCKER / APPLIED_OUTBOX_CRASH_WINDOW.**

§6 permits `CONSULTATION_ACTIVE_COMMITTED → APPLIED → U02_HANDOFF_ENQUEUED`, while stating that a durable recoverable handoff intent is required before U07 workflow closure. §10.2 Tx E leaves two different options: APPLIED and outbox same atomic transaction **or** a “previously accepted crash-proof write-ahead stage.” Neither option is selected. The proposed journal has `u02_handoff_effect_id + enqueue_ref` but no frozen **pre-APPLIED durable handoff intent** protocol or authoritative outbox state machine.

As a result an execution might persist APPLIED and crash before a durable handoff exists, with no guaranteed unique reconstruction record or exactly-one downstream eligibility. Text that a failure “must be impossible” is not a physical design.

**Required remediation:**
- Select one V1: e.g. same-DB atomic `APPLIED + unique outbox(intent)`, or a unique write-ahead outbox intent durably committed *before* APPLIED with exact recovery owner. Freeze key, retention, enqueue/dispatch/ack states, producer/consumer idempotency and crash reconciliation at each boundary.
- Distinguish `APPLIED` = governed U07 state effects committed from `HANDOFF_PENDING/DELIVERED/ACKNOWLEDGED`; do not claim U02 clinical fact exactly once without consumer idempotent acceptance evidence.
- Prove that an APPLIED replay with an absent outbox does not emit a newly generated effect ID or lose the original canonical answer.
- Add exact crash oracles for APPLIED/outbox commit uncertainty and for U02-ack-lost duplicate delivery.

Evidence: §6 state machine, §10.1 journal, §10.2 Tx E, §10.3 crash matrix, U07-A03-20/21.

## 5. BF-U07-RDP03-IR-03 — U15 terminal fence across partial state commits

**BLOCKER / PARTIAL_COMMIT_TERMINALIZATION_UNSPECIFIED.**

§8 requires a fresh U15 owner generation before P01 mutation and §10.4 requires re-check before each new clinical-state mutation, which are sound safety principles. But §§9–10 do not freeze a cross-owner mechanism making the **final** P01 mutation authorization/currentness check mutually ordered with U15 terminalization. A separate P01 store could commit its patch **after U15 terminalizes** although a pre-commit check was previously valid. The existing Consultation lock only serializes actors actually participating in it; a P01 CommitResult is not automatically fenced by Consultation's row lock.

**Required remediation:**
- Choose a shared terminalization guard per **mutating stage**, defining what lock/epoch/CAS P01 actually checks **at commit**, not merely at intent creation.
- When U15 wins before a stage commit: no new ordinary owner effect, record blocked stage and preserve historical F8 ACCEPTED. When a stage commits before U15, retain its factual evidence; halt subsequent stages and route owner-governed reconciliation without fabricating rollback.
- Specify a stable apply-stage fencing generation spanning Consultation, F3/P01 and ApplyJournal; fail closed if U15 cannot participate in owner transaction domain. Register a separate controlled amendment or bind the existing U15 CA to explicit cross-P01 constraints, never claim it already works.
- Add race tests: U15 cancels between K09 validation and P01 commit; terminalization between F3 owner write and Pending consume; terminalization between Pending consume and Consultation ACTIVE; retry after cancellation/partially applied state.

Evidence: §§8–10, U07-A03-17/22, missing physical owner bridge in main.

## 6. Independent review question results

| Original review question | Disposition |
|---|---|
| IR-01 F3 Question/Gap owner boundary | PASS / owner contract still REQUIRED |
| IR-02 K09/P01 grants and CAS | CONDITIONAL / U07 producer implementation unverified |
| IR-03 root vs same-wait unique idempotency | PASS at logical identity level |
| IR-04 cross-resource physical topology | **BLOCKED BF-01 / BF-02** |
| IR-05 post-ACCEPTED U15 terminal effects | **BLOCKED BF-03** |
| IR-06 APPLIED under P02/P01 partial recovery | CONDITIONAL / BF-01/02 |
| IR-07 inherited dependencies | PASS as registered blockers |
| IR-08 Trace owner evidence | PASS / design |
| IR-09 U02 one logical handoff vs Clinical Facts | CONDITIONAL / BF-02 |
| IR-10 baseline source asset claims | PASS: missing facilities labeled unimplemented |

## 7. Gate and next step

```text
U07-RDP-03 Independent Design Review = REVISE_REQUIRED
Exact reviewed HEAD = 253032f718d85a0bbbb073ccbaee0d04adf961e7

BF-U07-RDP03-IR-01 = OPEN / CROSS_RESOURCE_APPLY_TOPOLOGY
BF-U07-RDP03-IR-02 = OPEN / APPLIED_OUTBOX_DURABILITY
BF-U07-RDP03-IR-03 = OPEN / TERMINAL_FENCE_PARTIAL_COMMIT

B-U07-RG-03 = OPEN
U07-RDP-03 = DESIGN_CANDIDATE / NOT_FROZEN

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real patient = BLOCKED
```

**Next permitted step:** U07-RDP-03 Targeted Design Remediation on original design PR #276, explicitly selecting one physical saga/transaction topology and solving the two dependent effect/terminal crash barriers; then an exact-head U07-RDP-03 Targeted Independent Design Re-Review.

No merge, runtime code, schema migration, clinical operation, deployment or implementation authorization accompanies this review.
