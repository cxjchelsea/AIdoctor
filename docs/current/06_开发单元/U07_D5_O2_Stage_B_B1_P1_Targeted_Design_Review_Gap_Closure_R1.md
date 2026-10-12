# U07 D5-O2 Stage B — B1-P1 Targeted Design Review / Gap Closure Decision R1

Date 2026-10-12. Read-only review of PR #441 exact HEAD `e10a008dd0d183ba73b644708a2321a2b9a0c312` against exact `main=3b0589347d90af5ec5b86fbdad5eecabcce41a24`. #441 remains Draft and changes exactly one design Markdown file. Verified against original `JdbcU06DeliveryStore`, `U06DeliveryStore.Command`, Flyway V6 six-table DDL, and merged B1-P0 test infrastructure. **This is a same-assistant source-based review, not a separately signed independent review. No P1 Java/MySQL tests ran.**

## 1. Review decision

`B1P1_V01_DESIGN_DIRECTION=ACCEPTED_WITH_TARGETED_BLOCKERS` — the proposed nonproduction Java8/MySQL8 fault, replay, concurrency and partial-ledger test planes are appropriate.

`B1P1_FULL_IMPLEMENTATION_READINESS=NOT_READY` — the proposed matrices and interpretation rules require design remediation before granting any implementation authorization.

`P1_TEST_ONLY_SCOPE=ACCEPTED_IN_PRINCIPLE`; `P1_IMPLEMENTATION_AUTHORIZATION=NOT_GRANTED`; `REAL_D5=NOT_READY`.

The review distinguishes four claims that must never be conflated: **(a)** original method result (`Snapshot.replay` or exception); **(b)** whether each JDBC transaction committed/rolled back/unknown; **(c)** whether six tables form a complete and internally coherent durable group; **(d)** downstream delivery/clinical authority (always false here). A test may expose a source defect without silently treating it as test harness failure or granting production approval.

## 2. Gap closure decision by original finding

| Finding | Decision | Rationale | Required normative action |
| --- | --- | --- | --- |
| **B1P1-RG-01** concurrency/result semantics | **OPEN — BLOCKER** | Design correctly notes `JdbcU06DeliveryStore` does not catch database deadlocks or unique-key exceptions, but C1..C6 still mix *raw observations*, allowed database exceptions, transaction commit state and desired *business replay* results without exact acceptance rules. Under SERIALIZABLE initial SELECT, deadlock/lock wait timing can vary; not all losing requests must return a replay snapshot. | Freeze a three-column matrix for each scenario: **raw original method** (success/replay/throw), **physical authoritative six-table state**, **P1 test verdict**. Model duplicate key/deadlock as `EXPECTED_RACE_FAILURE` only when final third-reader invariant is proven. No automatic replay classification or silent retry. Require a separate subsequent replay test after winner's commit. |
| **B1P1-RG-02** four-table JOIN vs six-table authority | **OPEN — BLOCKER** | Original lookup joins ledger/authority/intent/confirmation, not attempt or receipt. An intact quartet with missing attempt/receipt can return `replay=true` despite incomplete six-table group; ledger-only or partial quartet may lead down insert path. v0.1 flags this but does not freeze whether P1 “passes” on reproducing it or fails an intended invariant. | Fix test goals: **P1 source-characterization PASS** when precisely reproducing method output and independently detecting incomplete six-table group; **six-table replay safety FAIL / SOURCE_DEFECT_CANDIDATE** when `replay=true` despite missing attempt/receipt. Neither result authorizes downstream authority. A later controlled design amendment is needed for production correction. |
| **B1P1-RG-03** physical evidence/reader | **CONDITIONALLY CLOSED AT DESIGN LEVEL; EXECUTION EVIDENCE OPEN** | P0 proves a fresh SELECT-only session for N=1; proposal requires per-table vectors for N=1..6, no shared counters, original DDL hashes, exact PR-head provenance. But evidence must distinguish in-transaction observer, post-rollback fresh observer, and committed healthy control; do not trust a reused consistent-read snapshot. | Freeze per-case schema: six `before/inflight/after` counts, writer and distinct reader connection IDs, exact ordinal, original method result/exception class, tx result state, role verification, source SHA/PR head/synthetic merge checkout/run. Each named scenario maps to one executed JUnit method and one JSON fact; missing data fail closed. |
| **B1P1-RG-04** bounded true concurrency | **OPEN — REQUIRED** | Bounded futures are stated but no deterministic progress contract, timeout budget, lock wait timeout, startup barrier placement or cleanup invariants are frozen. A “two threads both blocked after SELECT” barrier can itself create deadlock and false positives. | Define latch release before first DB write/lock boundary, maximum future/SQL lock timeout, resource release in finally, independent third-reader snapshot, at least one no-conflict concurrent positive control. Separate expected DB race failures from harness timeout; harness timeout is FAIL, never PASS. |
| **B1P1-RG-05** file/CI scope | **CONDITIONALLY CLOSED AT DESIGN LEVEL; EXACT INVENTORY REVIEW PENDING** | Ten proposed files are strictly test/config/CI/scripts and production excluded, but use a new P1 physical schema/profile/port (33328) with schema/DDL identity as strict guard. Need freeze interaction between CI concurrency job and P0 gate, plus exact Java class/test names and artifact relationship before execution authorization. | Freeze exact file inventory and P1 case manifest/roles/lease/timeouts, no `src/main`, no Flyway or original P0 edits. Any extra file requires controlled amendment. Do not enable production scan. |

## 3. Additional specific review findings

**B1P1-R1-06 — REQUIRED: Same-effect read-before-write and collision classification.** `JdbcU06DeliveryStore.find(effect)` performs a JOIN before all six INSERTs; under MySQL SERIALIZABLE, concurrent contenders can both fail due to lock conflicts, depending on conditions. Requirement “exactly one winner” cannot be unconditionally counted as verified if both transactions abort and no committed group exists; only conditional `ONE_WINNER_OBSERVED` supports race-specific success. A two-abort result is a real failure/diagnostic, not `PASS` nor fabricated commit.

**B1P1-R1-07 — REQUIRED: Partial ledger seed and write privilege split.** Manually seeded partial states are intentionally impossible under the normal single transaction if it commits or rolls back atomically. They must be created **only** by dedicated disposable test-fixture seed privilege, outside the unit under test; cannot be claimed as ordinary crash behavior. Verify quarantining/clearing each seed between tests and prove no cross-test side effects. If a full JOIN quartet missing auxiliary rows returns replay, assert *source semantic gap* separately from a passing characterization test.

**B1P1-R1-08 — REQUIRED: Unique-constraint coverage is asymmetric.** `delivery_attempt` and `delivery_receipt` only have their own PK, so concurrent collisions may be seen first in `authority` or `intent`, and are not a proof every auxiliary row shares an independently-enforced business unique key. Physical third-reader must verify all six table rows and IDs, not only the ledger and intention.

**B1P1-R1-09 — REQUIRED: Real correctness under connection failure cannot be inferred from exceptions.** Actual `UNKNOWN_COMMIT` requires uncertainty evidence around commit acknowledgement, not an arbitrary fault exception; stay `NOT_TESTED` if no physical scenario. Do not override `UNKNOWN_COMMIT` with `ROLLED_BACK_OBSERVED` unless a correctly scoped fresh readback proves it.

## 4. Minimal normative remediation contract for v0.2

Before implementation, produce an exact scoped amendment that includes:

1. A **scenario classification table** keyed by P1-FAULT-01..06, P1-HAPPY-01, P1-REPLAY-01, P1-CONFLICT-01..06, P1-RACE-01..06, P1-PARTIAL-01..06, P1-SEC-01..03. Each row specifies original-method outcome, commit evidence state, six-table vector, business authority (always denied), and scenario verdict; explicit path for `EXPECTED_RACE_FAILURE` versus `HARNESS_TIMEOUT`.
2. A complete **six-table group predicate** requiring one coherent authority/intent/attempt/receipt/confirmation/ledger, keyed by `delivery_effect_id`, with exact command-binding fingerprints and necessary stable IDs; don't use four-way JOIN alone. Mark `PARTIAL_LEDGER_SOURCE_GAP` separately and block approval of `FULL_SIX_TABLE_REPLAY_SAFE`.
3. Deterministic **two-actor race protocol**: synchronized entry but no mandatory barrier under locks, bounded futures and MySQL lock wait, explicit cleanup, independent third-reader adjudication, no expected success based only on unique-key errors; one no-conflict concurrency positive.
4. **N=1..6 hook isolation**: each test resets a new hook and fixture ID, original SQL executed via super.update then fault; original SQL/final class unchanged. Uncommitted state observed using separate SELECT-only connection and rollback state checked via new read transaction. Unrelated healthy 6/6 group remains intact.
5. Frozen **10-file P1-only inventory, machine-verifiable manifest and CI provenance**; run Java8/MySQL8 physical tests and attach original JUnit XML plus per-case observational JSON. Running these cases cannot be inferred from P0's five PASS.

## 5. Gate outcome and next action

```text
PR441_EXACT_HEAD_DESIGN_REVIEW = COMPLETE_WITH_FINDINGS
B1P1_RG01 = OPEN_BLOCKER
B1P1_RG02 = OPEN_BLOCKER
B1P1_RG03 = DESIGN_CONDITIONALLY_ACCEPTED_EVIDENCE_PENDING
B1P1_RG04 = OPEN_REQUIRED
B1P1_RG05 = DESIGN_CONDITIONALLY_ACCEPTED_INVENTORY_PENDING
B1P1_R1_06_09 = REQUIRED
B1P1_IMPLEMENTATION_READINESS = NOT_READY
B1P1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
B1P1_PRODUCTION_REPAIR = NOT_AUTHORIZED
B1P2_AND_B2 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next recommended step:** `D5-O2 Stage B — B1-P1 RG-01/RG-02 + R1-06..09 Targeted Normative Design Remediation`, with RG-03/04/05 acceptance conditions included; follow with targeted re-review and a *separate* bounded implementation-authorization decision. No PR merge, production edits, execution of real P1 suite, patient/PHI data or external side effects authorized by this review.
