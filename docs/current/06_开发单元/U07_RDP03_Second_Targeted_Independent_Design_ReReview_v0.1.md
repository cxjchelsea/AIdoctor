# U07-RDP-03 Second Targeted Independent Design Re-Review v0.1

> Date: 2026-10-08
> Target design: [PR #276](https://github.com/cxjchelsea/AIdoctor/pull/276)
> **Exact reviewed HEAD:** `6bc80c277ec9ee6ad3ed7cc75f39ac55c11e95c5`
> Prior design HEAD: `fc961a778fb1099c2e6c37540fa47f374182b67b`
> Prior review: [PR #278](https://github.com/cxjchelsea/AIdoctor/pull/278)
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY.**
> This is an independent design-only decision, **not** source-code execution, migration, implementation readiness, merge or production authorization.

## 1. Evidence and exact-head review scope

Fetched the design contract and reviewed all of §§6–10, 12–15, all `U07-A03-01..45` design-oracle IDs and PR #278 blocker definitions. The exact-head comparison contains **one Markdown file, 53 additions and 17 deletions**. The 45 oracle IDs are unique and in sequence; they are **not executed tests**. Existing main evidence cited in the original review provides Consultation lock/version and a U06-scoped P01 pattern, **not** actual U07 owner bridges, distributed Saga code, shared P01/U15 transaction manager, U15 dispatch authority or U02 consumer idempotency. These remain blocking upstream/physical prerequisites.

## 2. BF-U07-RDP03-TR-01 — Atomic C stage versus state machine

**CLOSED / DESIGN.**

§6 replaces two sequential committed-looking milestones with exactly one **`F3_P01_ANSWER_AND_PENDING_COMMITTED`**. §9.1 and §10.2 C consistently specify one F3-owner-authorized K09 StatePatch and one P01/G2 CommitResult for Question, optional Gap and Pending consume, with a single Clinical version advance and authoritative readback. §10.1 binds `stage_c_single_p01_effect_id + proposal_id + commit_result_ref` and a single version/readback fingerprint; any Question/Pending journal projections are non-authoritative derived representations, not independently committed subeffects.

Recovery cannot use a delayed or incomplete projection to retry a second patch: it must inspect the **same** owner receipt and authoritative Clinical State. The updated `U07-A03-16` and new `U07-A03-39/40` provide crash and partial-journal-projection oracles. This removes the exact contradiction found in PR #278.

**Implementation dependencies unchanged:** actual F3 answer-consumption owner authorization; U07 producer/field grants; one P01 atomic patch with same-transaction Consultation/U15 fence; dialect/version/commit-readback proof. None is proven by this design PASS.

## 3. BF-U07-RDP03-TR-02 — U15 terminal versus U02 outbox first dispatch

**CLOSED / CONDITIONAL DESIGN.**

§10.2 F now chooses exactly **`FENCED_DURABLE_DISPATCH_GRANT_V1`**, eliminating the previous unguarded PENDING→send path:

1. E atomically commits `APPLIED_WITH_DURABLE_U02_INTENT` and unique PENDING outbox, **not yet dispatch authority**.
2. The RDP-04 dispatcher must acquire the same authoritative Consultation/U15 lock and verify the exact root/wait/winner, owner terminal generation, effective deadline and authorized U15 delivery policy.
3. Under this same guard transaction it CAS-es PENDING→`DISPATCH_AUTHORIZED` and persists immutable `dispatch_grant_id` + owner generation/policy version. Any missing/ambiguous authority blocks sending.
4. If U15 terminalizes before the grant wins, outbox becomes `BLOCKED_TERMINAL`, with **zero ordinary U02 sends**. F8 historical ACCEPTED and completed APPLIED remain historical facts.
5. If the grant commits before U15 terminalizes, the design explicitly chooses **irrevocable authorization of the same immutable handoff ID only**, and does **not** pretend a DB lock spans the network call. All post-terminal continuation depends on the **explicitly authorized U15 policy**; without such owner approval, V1 is `NOT_APPLICABLE / NOT_READY`.
6. The worker may then move through DISPATCH_STARTED / DELIVERED / ACKNOWLEDGED using the same U02 consumer idempotency key, with independent U02 admission and clinical-fact authority. Unknown grant COMMIT or ACK must be reconciled against the durable grant/consumer receipt before retry.

New `U07-A03-41..45` cover terminal-before-grant, grant-before-terminal-before-send, grant outcome unknown, unsupported policy/consumer and ACK lost. §15 now explicitly registers the new `CA-U07-RDP03-U15-DISPATCH-GRANT-01` as `REQUIRED / NOT_AUTHORIZED`.

**Conditional nature is material:** approval of grant irrevocability across subsequent U15 cancellation, expiry or safety changes has **not** occurred. A future owner policy may reject this V1. Likewise neither network transport exactly-once nor U02 Clinical Fact exactly-once is claimed. This design result permits downstream **design work only**, not sending answers.

## 4. Original BF-U07-RDP03-IR-01..03 disposition

| Original finding | Status after target remediation | Conditions |
|---|---|---|
| IR-01 cross-resource topology | CONDITIONAL_DESIGN_RESOLUTION | `SINGLE_GUARD_DB_STAGED_SAGA_V1` selected; true same-DB P01/Consultation/U15/Journal guard must be proven |
| IR-02 APPLIED versus U02 outbox | CONDITIONAL_DESIGN_RESOLUTION | One Tx E with same-commit APPLIED and unique durable PENDING outbox; DB physical compatibility not yet demonstrated |
| IR-03 U15 vs partial C/D/E commits | CONDITIONAL_DESIGN_RESOLUTION | Shared Consultation row/terminal generation and DB statement-time guard required inside owner transactions; not present/proven on main |
| TR-01 atomic Stage C mismatch | **CLOSED / DESIGN** | Physical single owner/P01 commit needs later evidence |
| TR-02 post-APPLIED dispatch ambiguity | **CLOSED / CONDITIONAL DESIGN** | U15 dispatch-grant policy amendment must be authorized and verified |

Owner separation remains valid: F8 verdict, P02 Runtime, F3 Question/Gap policy, P01/G2 Clinical State mutation, Consultation lifecycle, U15 terminal truth, U02 Clinical Facts, P05 Trace. Exact same-wait root, crash readback, immutable F8 ACCEPTED and fail-closed owner uncertainty preserved.

## 5. Current dependency register / no authorization

```text
GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = REQUIRED / NOT_PASSED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP02-U15-SHARED-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-F3-ANSWER-BRIDGE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-P01-U15-SHARED-COMMIT-FENCE-01 = REQUIRED / NOT_AUTHORIZED
CA-U07-RDP03-U15-DISPATCH-GRANT-01 = REQUIRED / NOT_AUTHORIZED

RDP-04 P02/runtime and U02 downstream transport = FUTURE DESIGN
RDP-05 same-DB/dual-dialect/owner authority verification = FUTURE DESIGN
RDP-06 exact-head executable tests / negative effects = FUTURE DESIGN
```

No missing or unimplemented owner bridge is waived by this review. If U15 owner cannot authorize the chosen after-grant continuation policy, or if P01/Consultation/U15 cannot share the required primary DB transaction manager, the selected design must be revised under controlled amendment and U07 remains NOT_READY.

## 6. Formal gate decision

```text
U07-RDP-03 Second Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE
Reviewed design HEAD = 6bc80c277ec9ee6ad3ed7cc75f39ac55c11e95c5

BF-U07-RDP03-TR-01 = CLOSED / DESIGN
BF-U07-RDP03-TR-02 = CLOSED / CONDITIONAL_DESIGN

BF-U07-RDP03-IR-01..03 = CONDITIONAL_DESIGN_RESOLUTION
B-U07-RG-03 = DESIGN_REVIEWED / IMPLEMENTATION_BLOCKED
U07-RDP-03 = CONDITIONALLY_ACCEPTED_DESIGN / PENDING_AGGREGATE_COMPATIBILITY

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real patient = BLOCKED
```

**Next governed design step:** `U07-RDP-04 P02 Runtime Resume / Recovery / Downstream Handoff Boundary`, carrying forward the exact F3/P01 and U15 common fence and dispatch-grant upstream controlled amendments. This independent review does not merge PR #276, authorize runtime implementation, approve any live dispatch, run executable tests or enable clinical production.
