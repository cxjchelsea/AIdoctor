# U07 D5-O2 Stage B — B1-P1-C C-ISOLATED Race / Timeout / Evidence Contract Targeted Technical Re-Review + Scoped Implementation Authorization v0.1

Date: 2026-10-13. **SOURCE-BASED TARGETED TECHNICAL RE-REVIEW**, produced by the same assistant that worked on the implementation sequence; **not an independent human sign-off**. Input is [PR #455](https://github.com/cxjchelsea/AIdoctor/pull/455) exact Draft HEAD **`cda649172351fdcd29fc60012bf94280cef7ad69`**, based on and separately rechecked against unchanged `main` **`20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`**. Confirmed original `JdbcU06DeliveryStore.reconcileConfirmed()`, six original MySQL V6 delivery tables, P1-A/P1-B isolation contracts. No B1-P1-C code exists in this review; no real concurrency execution/CI result is claimed. No merge or production activation permission is granted by this document.

## 1. Disposition and isolated scope authority

**Decision: `AUTHORIZE_LIMITED_C_ISOLATED_TEST_ONLY_IMPLEMENTATION`**, **conditional on the exact executable contract below**, for creation of **one new Draft implementation PR** based on the above exact main with **only six newly added files** in §2, and only Java8 / disposable local MySQL8 concurrency tests with eight independently named test methods. This is an implementation-scope authorization, **NOT** confirmation of readiness to merge or proof that the race outcomes will be green. If any of the design conditions cannot be achieved within six new files or conflicts with the observed original Store, stop and return for a controlled design/authorization amendment. A later actual human independent review (when required by project process) and exact-head CI/Artifact/review/merge gates remain separate.

Original `find()` checks a four-way ledger/authority/intent/confirmation INNER JOIN, **not attempt/receipt**; P1-B has physically confirmed false replay on committed synthetic five-row partial groups. Concurrent tests must preserve `ORIGINAL_FOUR_TABLE_REPLAY_GAP=CONFIRMED_NOT_SAFE` and never claim production six-table-safe replay, transaction annotation weaving, crash recovery, F8 resume or real delivery.

## 2. Exact frozen six-file ADD-only inventory

1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CTestConfig.java`
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CConcurrencyTest.java`
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CEvidence.java`
4. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p1c/manifest.json`
5. `tools/u07_d5_o2_tx_topology/stageb/p1c/bootstrap-and-verify.py`
6. `.github/workflows/u07-d5-o2-stage-b-p1c.yml`

No editing any existing file, especially original production `JdbcU06DeliveryStore`, Flyway migrations, `pom.xml`, application profile, accepted Stage A/P0/P1-A/P1-B tests, CI or manifests. No imports of P1-A/P1-B helpers unless an explicitly reviewed shared-contract amendment first allows it. `stageb/p1c/` sibling paths and CI path filters prevent touching the accepted test suites. All six files are additions; no `StageBP1ConcurrencyTest` is to be added under existing P1-A's `stageb/p1/` directory. Use a single physical MySQL8 CI job on localhost **127.0.0.1:33330**, schema `u07_d5o2_stage_b_p1c`, profile `U07_D5O2_STAGE_B_P1C_SYNTHETIC`. V2/V3/V6 original MySQL migration content must match immutable Git blob SHA1: `73029495c5f46e8576e34f8c9a77644fb8c1c4c4`, `ccc1aa30b8e9405bd07249bae1d3531853f7510b`, `7ed5a82bfba3b3fed2666bee969a7fe276e67566`. Fail if actual schema/table engine differs; all six actual delivery tables must be InnoDB.

Only two restricted runtime role types: `stagebp1c_writer` has SELECT and INSERT **only** on six original tables; `stagebp1c_reader` SELECT only on six original tables. Use two distinct writer **physical JDBC connections** (same role name permitted), and one independent SELECT-only reader connection. Explicitly verify `CURRENT_USER()`, `CONNECTION_ID()`, `DATABASE()`, isolation and per-session `innodb_lock_wait_timeout`. Writer DELETE and Reader DELETE must fail with physical privilege-denied SQLState 42000; no Seeder because no malformed fixture setup is needed. MySQL root initializes ephemeral schema and scoped users only and is not exposed to test JVM as business DataSource. Generate ephemeral masked CI credentials; do not print/upload secrets. **Do not grant DELETE merely to clean up**: use unique synthetic IDs for every test, the whole MySQL service is destroyed after the CI job. No user-controlled/prod DB URL is allowed.

## 3. Eight-case matrix and strict winner adjudication

| Case ID | Inputs | Valid original method behavior | Acceptance proof |
| --- | --- | --- | --- |
| P1C-RACE-01 | same effect, same complete Command | one `NEW_SUCCESS`; second may be original `REPLAY_TRUE` after first COMMIT, or a documented SQL contention/constraint error | **exactly one physically committed coherent six-row group**, no duplicate/mixed group; follow-on identical sequential replay true and zero INSERT |
| P1C-RACE-02 | same effect, Command B changes only contentFingerprint or endpointRef | exactly one `NEW_SUCCESS`, loser typed `U06_DELIVERY_REPLAY_CONFLICT` or a documented SQL contention/constraint failure; **no mismatched replay** | one full winner graph bound to A or B; explicit losing changed payload absent |
| P1C-RACE-03 | different effect, reused idempotency_key | one `NEW_SUCCESS`, other actual SQL contention/constraint failure | winner 6/6; loser 0/6; exactly one intent under the shared key |
| P1C-RACE-04A | different effect, reused selection_effect_id | one `NEW_SUCCESS`, other actual SQL contention/constraint failure | winner 6/6; loser 0/6; no authority split |
| P1C-RACE-04B | different effect, reused delivery_id | one `NEW_SUCCESS`, other actual SQL contention/constraint failure | winner 6/6; loser 0/6; intent delivery uniqueness physically enforced |
| P1C-RACE-05 | two unrelated effects, all unique keys disjoint | **both** `NEW_SUCCESS`, no exception accepted | two independent coherent six-row groups; **both commits actually complete** |
| P1C-RACE-06A | own freshly performed RACE-01 precursor then identical sequential request | replay true, no SQL INSERT in sequential call | same winner all-column SHA before/after, no altered rows |
| P1C-RACE-06B | own freshly performed RACE-02 precursor then changed sequential request **derived from actual winner** | typed `U06_DELIVERY_REPLAY_CONFLICT`; no replay true | original winner group digest unchanged and no extra rows |

Eight explicit Java JUnit5 test methods with 1:1 canonical case IDs and real Surefire XML. RACE-06A/B each performs its own bounded two-worker precursor, then fresh serial call: **no inter-method dependence or shared fixture/global winner state**. Strictly avoid treating a method result `NEW_SUCCESS` as COMMITTED until its transaction manager call has returned after commit. If the transaction commits but method/driver cannot confirm acknowledgement, classify `UNKNOWN_COMMIT/NOT_OBSERVED` and fail this test-only scope; don't infer success from a temporarily visible row. All winners are confirmed by a third fresh SELECT-only reader after worker termination with full six-table canonical row projections, unique identity and status consistency including Attempt and Receipt (not four-table JOIN alone).

**Collision handling:** MySQL deadlock `1213`, lock timeout `1205`, or unique key `1062` may count only as the *losing worker's real SQL exception* for collision cases 01..04B where the other worker has a proved new COMMIT and the final graph meets the case. Preserve Java exception type, nested SQLException SQLState and vendorCode, and actual SQL success prefix. Do not mark a deadlock/timeout as replay success or evidence of guaranteed correctness. For RACE-05, any such error is failure, not a tolerated loser. **Both aborted/no winner is a failed source/harness characterization**, not `PASS`. **Double NEW_SUCCESS** for one effect or shared unique key is a failure even when final DB happens to contain only one group.

## 4. Scheduler design, precedence and timeout/fail-safe (G02/G04)

- Two Java 8 `ExecutorService` tasks, exactly two transaction templates with caller-owned `DataSourceTransactionManager`, `REQUIRED`, **SERIALIZABLE**, transaction timeout 10 seconds. Explicitly no ambient thread Spring transactional resource entering a worker. Individual writer connection IDs must differ. The `@Transactional` annotation on the manually constructed final Store is **not** used as proof of advice.
- Gate READY and START **before beginning either DB transaction**. `readyLatch=CountDownLatch(2)`; `startLatch=CountDownLatch(1)`. Each worker decrements READY then waits START with bounded interruption handling; release START only after both reached READY. Never await each other once a JDBC transaction/SELECT/lock has begun. READY deadline = **3s**, common `System.nanoTime()` future deadline = **15s total, not 15s each**, TX timeout = **10s**, MySQL session `innodb_lock_wait_timeout=5` and verify session value, executor `shutdownNow` + bounded `awaitTermination` **5s** in finally. A worker entering DB TX before START, post-lock barrier, unresponsive task, nonterminated executor or leaked TX marks test `HARNESS_TIMEOUT_FAIL`.
- Measure worker READY, release START, actual TX begin, first real SQL observation, final method+transaction end timestamps using **monotonic** nanos. `overlapInterval` checks actual writer TX intervals; this is an **attempted overlap indicator, not itself proof of simultaneous lock conflict or linearizability**. Also record first SELECT/INSERT stage and real SQL lock/constraint outcomes. No claim of DB contention unless real SQLState/vendorCode or sufficiently direct DB evidence was recorded. If the case requires demonstrated overlapping TX and observed intervals do not overlap, `NOT_OBSERVED` and fail acceptance; no artificial post-lock barrier, sleeps, unlimited retry, or rewriting expected outcome.
- Source/harness error precedence: **(1) wrong CI profile/schema/role/credentials/immutable DDL; (2) worker timeout/interruption/nontermination/rollback unconfirmed; (3) missing or inconsistent raw evidence; (4) physically invalid six-table final state; (5) invalid source-method outcome; (6) valid race result.** A normal `1213/1205/1062` observed on a losing worker does not override a missing winner or harness timeout; all ambiguous cases fail closed.
- Never invoke third-reader final attestation while a worker is live or a transaction is in doubt. For timeout, attempt bounded cancel and shutdown; if not safely terminal, emit `NOT_OBSERVED` diagnostic without physical success verdict. Do not delete rows or release shared fixtures while a worker is active. A fresh reader is SELECT-only with negative physical DELETE probe; ensure reader connection physically different from both writer IDs.
- MySQL `SERIALIZABLE` initial `find()` on absent key can produce next-key lock/deadlock interactions. Source may therefore fail one or both attempts in some scheduling interleavings. **This authorization does not pre-declare the source race-safe or guarantee green tests.** First real CI smoke is a *feasibility observation*: if double aborts/no winner, unknown commit, artificial synchronization or repeated DB contention makes the matrix unachievable, halt and raise a separately reviewed design amendment, not “fix” expectations with sleeps, fake facts or new production retry.

## 5. Strict physical evidence and validator boundary

Test-only evidence helper must materialize each case fact from actual JUnit runner and JDBC observations; separate Python `verify` reads only original Surefire XML, raw test-emitted JSON and immutable repository/CI identity—**never invents** a success fact. Manifest binds eight unique methods/classnames, sourceMainSHA `20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`, original immutable three Flyway Git blobs, exactly six tables, MySQL schema/port/roles, expected verdicts and `downstreamAuthority=false`. Bind actual PR branch HEAD to synthetic merge checkout's second parent, and same-run ID, Java8/MySQL8 runner/schema, physical role/connection identities. If a changed main/new PR base invalidates provenance, stop/review.

For every one of eight cases require:
- two unique actor records: scoped synthetic effect/key/payload digests, actual `CURRENT_USER()`/CONNECTION_ID, SERIALIZABLE, DB lock-wait value 5, source outcome, committed/rolled-back/unknown TX fact, real `SQLException` SQLState+vendor code, exact six originally ordered SQL signatures and per-actor successful prefix (0..6), bound JDBC resource, monotonic start/first SQL/commit-return/end times;
- control plane: both READY before START, common 15s race deadline, distinct writer IDs, TX interval overlap observed and separated from proof of SQL contention, confirmed executor stopped, no TX resource leaks, no worker timeout, no mock scheduling;
- third-reader physical facts: six-table row vectors + full sorted all-column canonical SHA-256 for each effect even when empty, exactly one-row-per-table and matching logical delivery/selection/statuses/attempt/receipt identity for each committed group, losing group six zeros for distinct effects, winner bound to real actor payload, no orphan receipt/attempt, winner count = 1 on collision / 2 on independent, immutable graph after sequential replay for 06A/B; post-reader SELECT-only verified with actual negative DELETE.
- `raceOutcome` distinct from `sourceOutcome` distinct from `physicalSixTableSafety` distinct from `businessReplaySafetyVerdict`. `originalFourTableReplayGapStillOpen=true` for **all** cases and never promote tested race coherence to full production six-table replay-safe verdict. `downstreamAuthority=false`.

Python validator must fail missing/extra/duplicated JUnit tests/facts, skips/failures, mismatched class/case/PR HEAD/synthetic merge, wrong DB schema or DDL blob, unknown password/role, writer-reader collision, unexpected SQL ordering, invalid observed outcome/SQLState, wrong winner count, invalid row vector/digest, no real third-reader or incomplete executor termination, timeout or unknown commitment, unverified identity. GitHub upload must include original Surefire and test-emitted case evidence, bounded to P1C. No production logs, PHI or secrets in artifact.

**Cleanup model:** No physical row cleanup within the actual test process; every case uses unique synthetic IDs and disposable per-job MySQL. Root/schema teardown is performed by CI container lifecycle after all writer tasks have terminated; no privileged per-case cleaning account required. Tests verify absence of unintended unrelated rows in the per-effect snapshot and assert isolated namespaces. Never claim an absent `cleanup=true` without actual cleanup. Document per-case no-seeder/no-delete lifecycle explicitly in JSON.

## 6. Review findings and decision

| Design finding | R2 review finding | Disposition |
| --- | --- | --- |
| **B1C-G01** exact eight-case matrix | Source-consistent with original Store and original MySQL PK/UNIQUE limits. Precursors for 06A/B independently repeat races. | **CLOSED_DESIGN_ONLY** |
| **B1C-G02** race/timeout under SERIALIZABLE | Bounded pre-TX latch, common 15s deadline, 10s TX, 5s lock wait, 5s final shutdown; no post-lock barrier; classify double abort/timeout as failures. Physical feasibility must be tested, never asserted in advance. | **CLOSED_CONTRACT_PHYSICAL_EVIDENCE_PENDING** |
| **B1C-G03** exact file/CI schema ownership | Six ADD-only sibling P1C files, separate schema `u07_d5o2_stage_b_p1c` and port 33330, unchanged A/B source/runner. | **CLOSED_DESIGN_ONLY; SIX-FILE_SCOPE_FROZEN** |
| **B1C-G04** evidence/lifecycle | Independent writer conn identities, complete third-reader final graph, original SQL sequence, source-vs-business classification, explicit precedence, no seeder or in-process DELETE. | **CLOSED_CONTRACT_PHYSICAL_EVIDENCE_PENDING** |
| **B1C-G05** original four-join production defect | Still confirmed from B1-P1-B; this review does not remediate or authorize production. | **OPEN_PRODUCTION_SAFETY_GAP_NONBLOCKING_FOR_TEST_CODE_ONLY** |
| **B1C-G06** SQL contention/constraint truth | Original schema constrained keys separately; classify only actual `1062/1213/1205` from real JDBC, one committed winner mandatory for collision success. | **CLOSED_CONTRACT_PHYSICAL_EVIDENCE_PENDING** |

### Formal scoped authorization

```text
SOURCE_DESIGN_PR455_HEAD = cda649172351fdcd29fc60012bf94280cef7ad69
BASE_MAIN = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
B1P1C_DESIGN_REVIEW = ACCEPTED_WITH_PHYSICAL_VERIFICATION_CONDITIONS
B1P1C_SIX_FILE_ADD_ONLY_SCOPE = FROZEN
B1P1C_SCOPED_IMPLEMENTATION_AUTHORIZATION = AUTHORIZE_LIMITED_C_ISOLATED_TEST_ONLY
B1P1C_JAVA8_MYSQL8_EXECUTION = NOT_EXECUTED
B1P1C_CONCURRENCY_SOURCE_SAFETY = NOT_ESTABLISHED
B1P1C_INDEPENDENT_HUMAN_SIGNOFF = NOT_OBTAINED
B1P1C_IMPLEMENTATION_MERGE_AUTHORIZATION = NOT_GRANTED
ORIGINAL_FOUR_TABLE_REPLAY_GAP = CONFIRMED_NOT_SAFE
B2_U06_PRODUCTION_FIX_OR_WIRING = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next allowed action:** `D5-O2 Stage B — B1-P1-C C-ISOLATED Two-Worker Concurrent Replay / Conflict Implementation + Real Java8/MySQL8 Feasibility Smoke` on a **new Draft implementation PR with precisely the six added files** above, pinning exact main. If actual Java8 compile/MySQL8 source behavior fails contract, capture safe diagnostics, stop and request controlled amendment/targeted review rather than silently relaxing invariants. A separate exact-HEAD security/evidence review and exact-main merge-authorization gate are required afterward. No production Store, real patient, external message, F8 or B2 scope is granted.
