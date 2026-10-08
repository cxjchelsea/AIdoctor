# U07-RDP-04 Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Design PR: [#280](https://github.com/cxjchelsea/AIdoctor/pull/280)
> **Exact reviewed design HEAD:** `c9dfc6cc31f3619b99ac8d732ce33751d7e5357c`
> Reviewed blob SHA: `1c40386813af6244985fc9bb7517fa95585daece`
> Previous design HEAD: `b8816272f1d032515164329644194901eded06aa`
> Prior independent review: [PR #281](https://github.com/cxjchelsea/AIdoctor/pull/281)
> **Verdict: PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY.**
> Design-only; no implementation, test execution, merge, patient/PHI or production authorization.

## 1. Independent exact-head evidence

Read PR #280 at its current head and directly fetched the entire design contract, §§1–15, against original three findings in PR #281; checked RDP-03 upstream Stage B/C/D/E/F constraints and U07 Unit Spec. The exact-head GitHub compare from `b8816272..c9dfc6cc` showed **one Markdown design file, 130 additions / 15 deletions**. All **44 scenarios P02-T01..P02-T44** are present, unique and ordered. They are **unexecuted design oracles**.

Original main source evidence includes U06 metadata-only runtime wait checkpoint, versioned Runtime Thread and U06 wait transition, **not** executable U07 restoration/rehydration, Thread parked-state transitions, shared P02/U15 grant transactions, physical negative-effect dispatch barriers, or U02 consumer acceptance. This review does not treat any of those as implemented.

## 2. BF-U07-RDP04-IR-01 — execution-start U15 linearization

**CLOSED / CONDITIONAL DESIGN.**

§6.1 now chooses exactly one `U15_FENCED_RESTORE_ONLY_START_GRANT_V1` and a typed `P02ExecutionStartGrantV1` including stable root/thread/original run/parent wait, immutable request fingerprint, checkpoint/proof digest, owner generation, U15 terminal generation, policy, and durable owner receipt.

The grant is written atomically with P02 `RESTORE_START_AUTHORIZED` phase under a shared Consultation/U15 `PESSIMISTIC_WRITE` lock followed by Thread row lock in deterministic order. Explicit ordering is:
- **U15 terminal commits first:** grant denied; zero physical restoration;
- **grant commits first:** only the same, finite, side-effect-free recovery to parked landing may finish; subsequent C/D/E/business effects are blocked if U15 has terminalized;
- **grant commit unknown:** no first restoration until exact authoritative grant is reconciled.

It does **not** pretend that an SQL row lock can span an external Runtime operation. It also does **not** borrow the RDP-03 U02 dispatch grant as P02 permission.

**Remaining mandatory implementation/owner condition:** `CA-U07-RDP04-P02-EXECUTION-START-FENCE-01 = REQUIRED / NOT_AUTHORIZED`; U15 must explicitly approve this inert grant-first semantics, and P02/Consultation/U15 must actually share the requisite transaction manager. Without it, V1 `NOT_APPLICABLE / NOT_READY`.

## 3. BF-U07-RDP04-IR-02 — safe parked landing

**CLOSED / CONDITIONAL DESIGN.**

§5 Path A/B and §6.2 now consistently require restoration into **`RESUMED_READY_BUT_NOT_DISPATCHED`**. P02 `RESUMED_VERIFIED` means only that owner state, continuation cursor and historical bindings were safely restored and durably read back at the parked landing; **no post-wait business step is executed**.

An explicit dispatcher-wide deny rule applies to scheduler nodes, callbacks, retry recovery, model/tool requests, external effects, notifications, Clinical State mutation and U02 delivery. This makes program-counter position a **data fact**, not a permission. Stage C/D/E/F remain separate owner-governed effects. General post-landing scheduler continuation needs its own authorization, not F8 ACCEPTED/P02 verified readiness.

New scenarios P02-T36..38 and T44 bind the negative-effect behavior. **Physical follow-through** remains `CA-U07-RDP04-P02-LANDING-BARRIER-01 = REQUIRED / NOT_AUTHORIZED`; every dispatch/callback adapter must demonstrably enforce the same guard before positive readiness.

## 4. BF-U07-RDP04-IR-03 — historical rehydrate equivalence/proof

**CLOSED / DESIGN.**

§5.1 now freezes `P02RehydrateProofV1` with immutable source execution plan/cursor and digests, original U06 issuance, frozen Registry/Runtime/Harness/Scheduler/Policy/contract version vector, reviewed recipe identity, complete historical effect manifest with coverage proof, positive owner-terminal receipts, deterministic continuation Run ID and expected parked-state/cursor digest.

Every potentially external-effect-bearing original Tool/Skill/Workflow attempt must be accounted for by **COMMITTED** or **PROVEN_NOT_EXECUTED**, verified by its owner. Missing effects, traces-only, timeouts, UNKNOWN, PARTIAL_SUCCESS or unsettled in-flight attempts are explicitly excluded, with zero replay. Reconstruction can only produce a side-effect-free parked state, verified by owner readback and digest.

No claim is made that current U06 metadata record itself stores such a proof. `CA-U07-RDP04-P02-REHYDRATE-OWNER-01 = REQUIRED / NOT_AUTHORIZED` remains necessary for authenticating manifest completeness and usable replay-safe runtime sources. If historical executable state or full authoritative proof is unavailable, governed failure is required and F8 historical ACCEPTED cannot be rewritten.

## 5. Compatibility and design-only acceptance

| Boundary | Review disposition |
|---|---|
| RDP-01 canonical original USER_ANSWER/RESUME_REQUEST binding | preserved |
| RDP-02 F8 ACCEPTED is business legality, not Runtime success | preserved |
| RDP-03 Stage B only consumes P02 verified **parked** owner result | preserved |
| RDP-03 Stage C single F3/P01 patch; D Consultation ACTIVE; E APPLIED+Outbox | preserved and still independently guarded |
| RDP-03 Stage F U15-approved Outbox dispatch grant | separate from P02 restore grant |
| U02 clinical fact formation vs producer outbox | separate consumer authority, NOT_VERIFIED |
| F8 acceptance after P02 failure | historical verdict unchanged |
| P05 Trace | evidence only, no runtime/clinical mutation authority |
| MySQL/Oracle, shared transaction manager, parked dispatch interposition | physical proof REQUIRED / NOT_VERIFIED |
| 44 design oracles | enumerated and sequential, **NOT EXECUTED** |

**Important conditional limitation:** `RESUMED_VERIFIED` does not certify patient/business workflow completion. It only proves safe Runtime restoration and parked readback. Any future post-landing general node execution requires separately authorized runtime continuation scope and Safety/U15 gating. Nothing in RDP-04 grants a second clinical delivery or allows bypassing RDP-03 stages.

## 6. Dependency register unchanged and augmented

```text
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

Physical P02 resume Thread/Run/Checkpoint and guard journal = NOT_IMPLEMENTED / NOT_VERIFIED
U07 RDP-05 Capability/Dependency/Applicability = FUTURE DESIGN
U07 RDP-06 verification/evidence = FUTURE DESIGN
```

## 7. Formal gate decision

```text
U07-RDP-04 Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

Exact reviewed HEAD = c9dfc6cc31f3619b99ac8d732ce33751d7e5357c
BF-U07-RDP04-IR-01 = CLOSED / CONDITIONAL_DESIGN
BF-U07-RDP04-IR-02 = CLOSED / CONDITIONAL_DESIGN
BF-U07-RDP04-IR-03 = CLOSED / DESIGN

B-U07-RG-04 = DESIGN_REVIEWED / IMPLEMENTATION_BLOCKED
U07-RDP-04 = CONDITIONALLY_ACCEPTED_DESIGN / PENDING_AGGREGATE_COMPATIBILITY

U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / PHI / real-patient = BLOCKED
```

**Next permitted design step:** `U07-RDP-05 Capability / Dependency / Applicability Contract` with explicit P02 executable-checkpoint or full rehydrate-proof feasibility, U15 owner grant policies, shared transaction-manager capability, parked dispatch enforcement, RDP-03 state owner bridges and U02 consumer idempotency. Do not advance to implementation or treat downstream compatibility as proven merely from RDP-04 design PASS.

No runtime source changes, tests, schema migrations, merge, clinical side effects, external delivery or authorization were performed by this review.
