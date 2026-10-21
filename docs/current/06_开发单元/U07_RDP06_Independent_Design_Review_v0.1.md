# U07-RDP-06 Independent Design Review v0.1

> Review date: 2026-10-08
> Design PR: [#286](https://github.com/cxjchelsea/AIdoctor/pull/286)
> **Exact reviewed design HEAD:** `c7abe477cf69dde27641b6be0099df3423959f90`
> **Design blob:** `b668949d70a9a5e95d4344857cd634cf97788302`
> Parent review baseline: `15e335759b9cf7acaeca8c7424ff971034c4c1f1`
> Bounded inspected runtime baseline: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Verdict: REVISE_REQUIRED — THREE DESIGN BLOCKERS.**
> Review only; no source implementation, runner creation, case execution, merge, authorization or live clinical traffic.

## 1. Exact-head evidence and accepted design strengths

Fetched PR #286 and its complete new document §§1–16. The 60 `U07-VG-001..060` rows are unique, sequential and **unexecuted design case requirements**, not actual independent machine-oracle entries. Cross-checked against U07 Unit Spec completion scope (§26), the prior RDP-05 accepted design and U06-RDP-06 verification model/runner precedent.

Retain:
- Explicit separation of frozen owner contracts → independently reviewed Oracle/Fixture → SUT observations → Comparator → durable bundle → evidence-only review.
- A `CONTRACT_EXPECTATION_GAP` fail-closed rule, SHA/exact-head binding and no uncontrolled refreeze after implementation changes.
- Layered negative-effect checks for F8, P02, P01/Consultation, Outbox/U02, U15, owner-version revocation and two SQL dialects.
- U06 metadata Checkpoint correctly classified as non-executable; parked Runtime barrier and two distinct U15 grants kept separate.
- PROFILE-B synthetic-only isolation, no PHI/production, distinct runner/evidence vs design and merge authorizations.
- Explicit `INVALID_EVIDENCE / INCOMPLETE / FAIL / PASS_PENDING... / PASS_REVIEWED` distinctions and pending upstream Foundation/CA blockers.

These are strong design foundations, but do not close the three material authority gaps below.

## 2. BF-U07-RDP06-IR-01 — conditional and incomplete expected-oracle authority

**OPEN / NON_DETERMINISTIC_EXPECTATION_AND_TRACEABILITY.**

§5 says each machine case must carry an independent deterministic expected owner outcome and `source_of_expected_authority`, yet several rows in §6 permit competing outcomes without explicit condition partitions or frozen policy reference:
- `U07-VG-016`: `REJECTED/owner-terminal` for U15-first;
- `U07-VG-020`: `DEFER/F8_TIME_AUTHORITY_UNAVAILABLE`;
- `U07-VG-035`: a “full certified proof” without mandatory version/proof acceptance fixture boundary;
- `U07-VG-038` and `045`: “if explicitly approved / under authorized U15 policy” without an `authorized_policy_ref` fixture variant and expected outcome on owner-policy rejection;
- `U07-VG-050`: phases “distinct” without exact expected durable counts or receipts.

A row phrased “X or Y” allows the implementation to decide its own expectation unless a fixture binds a specific authoritative outcome. §6 also claims its 60 scenarios adequately cover U07, but does not provide a **Unit Spec §26 / Initial Gap Review / RDP-01..05 oracle-to-case coverage matrix** enumerating mandatory invariants, original RDP oracle IDs and any intentionally non-applicable cases. Counts alone are not proof of complete contractual coverage.

**Required targeted design remediation:**
1. Freeze a typed per-case `EXPECTED` contract where outcome/status/count/owner receipt relations are **one deterministically derived value per fixture+policy version**; split alternative branches into named cases or explicit disjoint fixture partitions. Owner-approved policy missing => `CONTRACT_EXPECTATION_GAP` / NOT_AUTHORIZED, not an acceptable alternative output.
2. Add a case coverage/trace matrix linking U07 Unit Spec §26 acceptance invariants, `B-U07-RG-01..06`, and RDP-01..05 frozen oracle IDs (`U07-A03-01..45`, `P02-T01..44`, `CAP-T01..48` and RDP-01/02 original case IDs) to 60 case IDs. Each uncovered mandatory invariant must be added to catalog or explicitly documented as out of U07 scope.
3. Freeze negative-effect counts and owner readback fields for each conditional branch, with source-of-truth authority independent of SUT observation. Future machine-readable Oracle still requires its separate Oracle Review Gate.

## 3. BF-U07-RDP06-IR-02 — pre-authorization negative evidence vs fully authorized positive test execution

**OPEN / AUTHORIZATION_AND_EXECUTION_PROFILE_COLLAPSE.**

§10 runner step 1 requires **all mandatory owner CAs authorized before the runner starts**, while §6 `U07-VG-058` requires actively testing a **pending Foundation audit or non-authorized CA** and proving overall readiness remains NOT_READY. §9 harness self-test HG-09 likewise injects a pending CA. Without separate phases, the runner will abort before recording the negative cases; or the implementation may weaken the global authorization gate just to execute a positive path. The document does not freeze a formal non-effect pre-authorization assessment tier vs a privileged post-authorization physical execution tier.

**Required targeted design remediation:**
1. Distinguish `TIER-0 AUTHORITY_NEGATIVE_PRECHECK` (only trusted manifest/decision evaluator, no SUT runtime, DB write, restoration, or outbound call; demonstrates blocked CAs/audit with sealed synthetic evidence) from `TIER-1 GOVERNED_PROFILE_B_PHYSICAL` (full authorized owners, isolated real DB + synthetic test endpoint, positive and negative SUT execution).
2. Define two verdicts or one composed verdict: `TIER-0 PASS` proves fail-closed blocking, **never** proves runtime readiness; overall `PASS_PENDING...` requires TIER-1 physical evidence for all mandatory cases and all owner authorization. Cases like VG-058/HG-09 may be evaluated in Tier-0 without silently granting CA, while positive Stage C/F/P02 scenarios remain inaccessible without authorizations.
3. Freeze fail-closed behavior and per-tier observation scope if the authoritative gate is missing, changed or pending; no fixture flag can elevate a pending CA to AUTHORIZED.

## 4. BF-U07-RDP06-IR-03 — U02 logical once versus physical delivery/counting semantics and test isolation

**OPEN / U02_HANDOFF_COUNT_AND_OBSERVATION_BOUNDARY.**

U07 Unit Spec §26 requires “exactly-one U02 handoff,” while RDP-03/04 explicitly permit **at-least-once** network dispatch/retry using an immutable same handoff ID. §6 VG-046 and §11 `u02_send_attempts` include retries but the contract never freezes the count semantics of “one handoff”, successful consumer admission, network attempts, ACK loss and subsequent U02 Clinical Fact effects. `VG-046` allows replay and only says “no producer exactly-once fact assumption”, not what should be exactly once. §4 says deny “all real outbound sockets” and allow a synthetic receiver, but no enforceable transport boundary is defined for attempts to the isolated receiver vs blocked unapproved network, and §11 observation only has aggregate send attempts without canonical unique effect count/dedup receipts.

A defective implementation could “pass” by making two **different** handoff IDs (two clinical facts) while returning one ACK, or by hiding network attempts inside an uninstrumented adapter; conversely a correct same-key retry might fail an overbroad `send_count == 1` check.

**Required targeted design remediation:**
1. Freeze separate `logical_u02_handoff_intents_unique`, `dispatch_grants_unique`, `physical_send_attempts`, `synthetic_consumer_receive_attempts`, `consumer_admissions_unique`, `consumer_idempotent_replays`, and `u02_clinical_fact_commits` counters, with effect-ID equality, stage-specific baselines and owner-signed readback.
2. Define expected counts for E/F, no-grant, ACK-lost/retry, consumer rejection, U15 late-terminal and producer crash variants; distinguish U07 proof of one logical immutable handoff from U02's separately governed clinical fact idempotency and no inference of Clinical Fact formation from an ACK.
3. Specify precisely how the synthetic U02 transport is isolated and instrumented (allowlisted in-process/loopback receiver or dedicated sandbox endpoint, controlled endpoint identity, network interception before sending, 0 *unauthorized* connections rather than 0 all physical attempts), with deliberate tripwires proving unauthorized egress is observed. Missing any send intercept => INVALID_EVIDENCE, not 0 sends.

## 5. Additional nonblocking clarification

- A complete U07 physical verification suite is blocked while source owner changes/authorization prerequisites are absent; independent RDP-06 design acceptance must not imply runnable verification.
- U06's verified runner structure is a design precedent, **not** an authority transfer of U06 oracle/profile to U07.
- The dual-dialect list references compact `VG-006` notation while catalog uses `U07-VG-006`; implementers should resolve against one canonical ID namespace; not a standalone blocker.
- A historical committed effect after a later policy revocation remains an owner fact; only novel effects must be denied by the latest owner grant.

## 6. Formal decision and carry-forward

```text
U07-RDP-06 Independent Design Review = REVISE_REQUIRED
Exact reviewed HEAD = c7abe477cf69dde27641b6be0099df3423959f90

BF-U07-RDP06-IR-01 = OPEN / INDETERMINATE_ORACLE_EXPECTATIONS
BF-U07-RDP06-IR-02 = OPEN / AUTHORIZATION_TIER_COLLAPSE
BF-U07-RDP06-IR-03 = OPEN / U02_HANDOFF_COUNT_AND_ISOLATION

B-U07-RG-06 = OPEN / NOT_CLOSED
U07-RDP-06 = DESIGN_CANDIDATE / NOT_FROZEN
U07 Aggregate Compatibility Review = NOT_PERFORMED

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
U07 runner/physical tests = NOT_IMPLEMENTED / NOT_EXECUTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Next permitted step:** `U07-RDP-06 Targeted Design Remediation` on design PR #286, with exact independent re-review afterward. Do not authorize implementation, Oracle/Fixture authority freeze, live U02 transport, or aggregate design PASS before these three contracts are coherent.

This review only adds a Markdown review artifact on a distinct PR branch; it does not modify original design, tests or production.
