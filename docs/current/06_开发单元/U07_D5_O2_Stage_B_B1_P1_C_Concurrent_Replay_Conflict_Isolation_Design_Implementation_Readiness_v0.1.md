# U07 D5-O2 Stage B — B1-P1-C Concurrent Replay / Conflict Isolation Design + Implementation Readiness v0.1

Date: 2026-10-12. **DESIGN CANDIDATE — IMPLEMENTATION NOT YET AUTHORIZED**. Exact source main: \`20a54b4a7478641d8b8d8c12c6213b8e2ea73acc\` (PR #451 B1-P1-B standard-merged). Main frozen at this commit during design. Stage B P1-A and P1-B test-only assets are merged; they do **not** authorize P1-C races or production wiring. This design must have a separately recorded targeted review / scope-authorization decision before anyone implements. No merge or real patient/delivery permission follows.

## 1. Why this unit exists / physical source truth

Original \`diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.java\` is unchanged and manually instantiated in prior isolated tests. \`reconcileConfirmed\` uses \`find(deliveryEffectId)\` with a four-table INNER JOIN (ledger/authority/intent/confirmation), then six ordered JDBC INSERTs (authority → intent → attempt → receipt → confirmation → ledger). The source declares \`@Transactional(SERIALIZABLE)\` on a final class, but P1 tests need an **explicit caller-owned** \`DataSourceTransactionManager\` transaction, not an assumed effective Spring proxy. MySQL V6 DDL has authoritative unique keys: authority selection_effect_id PK and delivery_effect_id UNIQUE; intent delivery_effect_id PK, delivery_id and idempotency_key UNIQUE; attempt attempt_id PK; receipt receipt_id PK; confirmation confirmation_evaluation_id PK; ledger delivery_effect_id PK. Do **not** invent effect uniqueness on attempt/receipt or confirmation.

PR #451 P1-B physically established the unsafe original four-way JOIN false-positive replay when Attempt or Receipt is missing. P1-C **does not fix that source flaw** and cannot claim full production six-table replay safety; every scenario must distinguish accepted test source characterization from business authority. No new production code, schema/index/migration, Spring app wiring, outbound HTTP/SMS, PHI, F8 checkpoint/resume, Oracle, or B2.

P1-C asks whether two real MySQL8 transactions concurrently using the **unchanged** original Store produce a bounded, physically observed coherent result and correctly classified SQL contention; no mock/fake concurrency or artificially patched Store result may be used to establish this.

## 2. Eight separately named cases, two-worker design

Run 8 individually identifiable JUnit methods, each creating *two* workers using independent physical JDBC connections and distinct test-only JdbcTemplate instrumentation, plus a third fresh SELECT-only observer after both workers terminate. Each race uses unique synthetic identifiers; no real downstream operations. **Scheduling alone does not guarantee collision**; record actual start/transaction phases and distinguish attempted overlap from proven DB contention.

| ID | Exact race | Authorized original-method outcomes | Mandatory physical final-state verdict |
| --- | --- | --- | --- |
| P1C-RACE-01 | same effect, identical command | exactly one NEW_SUCCESS; other REPLAY_TRUE or documented SQL contention/constraint exception | exactly one complete six-table effect, stable exact identity graph; third sequential same-command replay true with 0 new writes |
| P1C-RACE-02 | same effect, different content fingerprint or endpoint | one NEW_SUCCESS; other typed \`U06_DELIVERY_REPLAY_CONFLICT\` or documented SQL contention/constraint failure; **never** REPLAY_TRUE of changed payload | one complete winner, no mixed fingerprints/statuses, changed request must not be accepted as identical replay |
| P1C-RACE-03 | two distinct effects sharing same idempotency_key | one NEW_SUCCESS, other original SQL duplicate/lock/deadlock outcome | winner full 6-table group and loser 0/6, shared idempotency key bound to exactly one committed intent |
| P1C-RACE-04A | two distinct effects sharing selection_effect_id | one NEW_SUCCESS, other original SQL duplicate/lock/deadlock | winner full, loser empty, no split authority |
| P1C-RACE-04B | two distinct effects sharing delivery_id | one NEW_SUCCESS, other original SQL duplicate/lock/deadlock | winner full, loser empty; original intent.delivery_id UNIQUE adjudicates |
| P1C-RACE-05 | two unrelated effects with disjoint unique identities | **both** NEW_SUCCESS | both complete; no SQL lock error permitted as successful characterization |
| P1C-RACE-06A | identical sequential replay **after** a verified RACE-01 winner | \`replay=true\`, zero new INSERT | winner full before and after; no new six-table rows |
| P1C-RACE-06B | changed sequential replay **after** a verified RACE-02 winner | explicit typed conflict (or previously justified source-specific SQL constraint), **never replay=true** | same winner digest, no loser partial effect |

RACE-06A/B must be individual JUnit tests that **perform their own two-worker precursor** under the same bounded race harness, then execute the sequential request after both workers have terminated; do not depend on JUnit test order or share mutable database fixtures between test methods. RACE-02's candidate must have the same deliveryEffectId but a changed fingerprint; the winner may be either payload. For post-race 06B, derive changed candidate from the **actual winner** after reading the authoritative six-table state rather than guessing which thread won.

**Atomicity and verdict rules:** A winner is established only by a successful original method call **and** third-reader verified complete six-table row graph for that exact command. “One winner” never means first JDBC INSERT or first thread to acquire a latch. Reject double success, two aborts/no committed winner, no readable final result, split/mixed rows or any false “replay=true” on incompatible payload. Where required, outcomes are recorded as ORIGINAL_NEW_SUCCESS / ORIGINAL_REPLAY_TRUE / ORIGINAL_THROW_TYPED_CONFLICT / ORIGINAL_THROW_SQL_1062 / ORIGINAL_THROW_SQL_1213 / ORIGINAL_THROW_SQL_1205 / OTHER_SQL_ERROR / OTHER_EXCEPTION / WORKER_TIMEOUT / NOT_OBSERVED. Error classes/SQLState/vendor codes must be reported from **actual exceptions**; \`1213\` deadlock and \`1205\` lock-wait timeout can count only as **EXPECTED_RACE_FAILURE_WITH_VERIFIED_STATE** for explicitly colliding cases with exactly one physically verified winner. No retry/re-plan is implemented or falsely inferred; raw Store does not auto-retry.

## 3. Real scheduling, timeout and cancellation protocol

- Use **two Java 8 ExecutorService workers**, at most 2 active writers; each has a new, independent TransactionTemplate for the same configured DataSourceTransactionManager and its **own physical JDBC connection** (verified \`SELECT CONNECTION_ID()\` and distinct roles). Explicit \`PROPAGATION_REQUIRED\`, **SERIALIZABLE**, bounded transaction timeout 10 seconds. Do not rely on production Spring \`@Transactional\` interception. Prohibit any ambient/bound TX resource before and after each thread.
- A start latch \`CountDownLatch(1)\` gates the workers **before** either thread begins its DB transaction or executes SELECT/locks. Test thread validates both workers signaled READY (2-count latch) before releasing START. **Never put a barrier in a transaction after acquiring a lock**: fabricated deadlock/harness timeout would invalidate the result.
- Freeze budgets from earlier v0.2 design: READY wait maximum 3s; \`Future.get\` maximum 15s **per worker with a common global race deadline**; transaction timeout 10s; MySQL session \`innodb_lock_wait_timeout=5\` (verified on each writer session, scoped to disposable CI only); executor \`shutdownNow()\`, then \`awaitTermination<=5s\` in \`finally\`. A 15s budget per Future may not silently allow >15s aggregate; compute one monotonic overall deadline. A timeout/cancel/unresponsive worker, interrupted test, leaked connection, unfinished transaction or failed cleanup means **HARNESS_TIMEOUT_FAIL/TEST_FAIL**, never PASS.
- Latch signal occurs outside TX and is **not itself concurrency proof**. Record READY timestamps, START release and each DB transaction begin, first original SQL observation, finish; overlap flag must derive from observed time intervals (monotonic nanos, not wall clock). For a race case, if no overlap was observed, label NOT_OBSERVED/RETRY_NOT_AUTHORIZED (do not fabricate race PASS); do not add unbounded sleeps or retries. One optional bounded second attempt could be considered only after explicit independent review of scope and evidence disposition.
- Third independent Reader opens only **after both workers are terminal**; before that no post-state read can claim final commit. For RACE-01/02 the eventual winner is read back through all six tables with physical IDs/statuses/fingerprints checked; RACE-03/04A/04B record winner and losing 6-vector, complete cross-table identity and absence of orphan extra rows. RACE-05 checks both independently complete; RACE-06 variants read again after sequential request and compare canonical all-column digests.
- Do not release resource locks or DELETE race fixtures while threads might still run. Any unresponsive worker or cleanup failure leaves the case failed, not silently \`pass\`. Final reader must use SELECT-only account, verify write denial, physical role/schema and distinct connection IDs; do not reuse active worker JDBC resource.

## 4. Separate P1-C test/DB/CI ownership — **proposed**, not yet authorized

Prior P1 v0.2 proposed \`StageBP1ConcurrencyTest.java\` under merged P1-A's \`stageb/p1/\`, but that location would trigger/modify existing P1-A asset ownership. Stage B P1-B adopted sibling \`p1b\` and has itself merged. Recommend **C-ISOLATED** sibling \`p1c\`: separate disposable MySQL8 schema \`u07_d5o2_stage_b_p1c\`, localhost port **33330**, dedicated profile \`U07_D5O2_STAGE_B_P1C_SYNTHETIC\`, no P1-A/P1-B schema/table/user/port reuse. Original V2/V3/V6 exact immutable DDL Git blobs remain \`73029495c5f46e8576e34f8c9a77644fb8c1c4c4\`, \`ccc1aa30b8e9405bd07249bae1d3531853f7510b\`, \`7ed5a82bfba3b3fed2666bee969a7fe276e67566\`.

**Proposed six newly added files only** (exact authorization must wait for targeted independent design review):
1. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CTestConfig.java\`
2. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CConcurrencyTest.java\`
3. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1c/StageBP1CEvidence.java\`
4. \`diagnosis-service/src/test/resources/u07/d5o2/stageb/p1c/manifest.json\`
5. \`tools/u07_d5_o2_tx_topology/stageb/p1c/bootstrap-and-verify.py\`
6. \`.github/workflows/u07-d5-o2-stage-b-p1c.yml\`

Use two root-created role types only if sufficiency is independently established: \`stagebp1c_writer\` SELECT/INSERT to exactly six pinned tables; \`stagebp1c_reader\` SELECT-only, with negative physical DELETE probe. No seeded malformed rows are needed for concurrency matrix; therefore **no Seeder privileges** proposed. Disposable MySQL root bootstrap credentials are separate from test JDBC runtime; generate masked per-job credentials, never log or upload generated secrets. Fix exact original MySQL8 DDL and InnoDB engine per table. B-specific tests stay unchanged and are not imported as unreviewed shared helpers. A positive sequential baseline must be physically tested within C to prove runner correctness. CI must compile on Java 8, run only whitelisted P1-C tests against localhost disposable MySQL8, and upload actual Surefire XML + **test-emitted** per-case JSON; a Python verifier must fail on missing/duplicate/skipped cases and cannot synthesize success.

The six-file inventory is a **review target**, not blanket authorization to modify any existing file (including main Store, P1-B fixture/CI or Maven pom). If implementation needs a 7th file or a changed existing file, return to scope amendment first. Path filters must cover *only* new \`p1c\` code/resources/tool and its own workflow. Never suggest that B1-P1-C CI coverage proves P1-A/B production correctness.

## 5. Verifiable structured evidence and negative controls

For each one of the eight exact methods, test writes one machine-readable case record (unique class/method/caseId/variantId) with the following:
- Source identity \`sourceMainSha=20a54b4a7478641d8b8d8c12c6213b8e2ea73acc\`, separately \`actualPrHeadSha\`, \`githubSyntheticMergeCheckoutSha\` (must verify PR checkout second parent), workflow run ID, original immutable V2/V3/V6 DDL blobs, scoped schema/port/profile/MySQL major 8 and six InnoDB engines.
- \`actorA\`/\`actorB\`: unique effect/delivery/selection/idempotency/fingerprint digests; actual \`CURRENT_USER()\` and \`CONNECTION_ID()\`, observed SERIALIZABLE and MySQL \`innodb_lock_wait_timeout\`, transaction and method outcome, successful ordered original SQL counts and trace, SQLState/vendorCode for real exception, and **observed** START/BEGIN/END monotonic timestamps. Record the actual winner actor by committed physical identity rather than thread order.
- \`raceControl\`: two READY observed before START, physically distinct writer session IDs, actual interval overlap, common overall future deadline, executor shutdown+terminated, no bound resources, no timed-out/unknown thread. None may default to a positive value; report explicit \`HARNESS_TIMEOUT_FAIL\` or fail closed.
- \`physicalFinal\`: third Reader session ID+verified SELECT-only, exact six-vector for both effect IDs (or one when shared), canonical SHA-256 of *all* six-table actual column rows (partial-aware 0..N), complete-group business-identity predicate including Attempt and Receipt, count of committed winners and losers, comparison with protected baseline, no mixed rows, no orphan receipt/attempt, no leaked partial loser, and ordered fresh-reader check after sequential post-race request for 06A/B.
- Typed \`sourceMethodVerdict\`, separate \`dbTransactionVerdict\`, \`sixTableIntegrityVerdict\`, \`raceHarnessVerdict\`, \`businessReplaySafetyVerdict\`, \`downstreamAuthority=false\` and explicit \`originalFourTableReplayGapStillOpen=true\`. Never label \`UNKNOWN_COMMIT\` as verified resolution: crash/ACK-loss not being exercised.

Negative checks: reader write denial, writer unwanted DELETE denial, unexpected SQL/unknown graph, same-effect two distinct winner payloads, missing/duplicate XML method, missing/duplicate JSON fact, source PR HEAD mismatch, source main/tree drift, SQL constraint not matching actual schema, race timeout or cleanup leakage, and attempts to claim safe replay from partial graph must all fail closed. A digest shape/equality is not substitute for actual test-source readback. No fake source-produced commit row insertion in Python.

## 6. Implementation readiness blockers / reviewable gates

| Finding | Observed current state | Closure required |
| --- | --- | --- |
| **B1C-G01** race matrix / physical verdict | Eight individually identified source-characterization cases designed | **DESIGN_DEFINED**; review actual source-based allowed exceptional outcomes and how to prove one winner |
| **B1C-G02** concurrency scheduling and timeout authority | 3s READY, 15s common deadline, 10s transaction, 5s lock wait, 5s shutdown budget specified | **CONTRACT_DEFINED, PHYSICAL FEASIBILITY PENDING**. Require independent review of concurrent SERIALIZABLE read locks and whether two simultaneous SELECTs can deadlock *both* attempts; do not promise one winner without real evidence |
| **B1C-G03** file/CI schema isolation | Proposed six new \`p1c\` files, new schema/port, no P1-A/B edits | **SCOPE_FREEZE PENDING INDEPENDENT DESIGN REVIEW** |
| **B1C-G04** worker lifecycle and evidence fidelity | Two physical writers + third reader, no DB-lock barrier, origin case facts | **CONTRACT_DEFINED, RUNNER/EVIDENCE REVIEW PENDING**, including source-vs-harness error precedence |
| **B1C-G05** prior unsafe four-way JOIN | Confirmed on real MySQL8 in merged B1-P1-B | **REMAINING PRODUCTION SAFETY GAP**; does not prohibit test-only characterization but forbids any production safety claims |
| **B1C-G06** original DB schema uniqueness / lock contention classification | V6 physical uniqueness and expected MySQL \`1213/1205/1062\` documented | **NEEDS TARGETED REVIEW**: select original constraints, prove observed actual error; no invented linearizability or lock order |

**Critical readiness point:** simultaneous \`SERIALIZABLE\` \`find()\` (two absent-key SELECTs) can interact with InnoDB next-key/gap locking and trigger deadlocks/timeouts. It is **not valid** to declare RACE-01..04 PASS merely because both actors reported SQL errors or a latch synchronized them. The design intentionally specifies a winner and a coherent six-table physical group; a no-winner outcome is captured as an actionable source/harness diagnostic, never silently turned green. If such a phenomenon is repeatable, revisit verdict semantics under a controlled design amendment instead of inserting sleeps/retries in the harness to force green.

## 7. Formal design/readiness decision

\`\`\`text
BASE_MAIN = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
B1_P1_A = MERGED_TEST_ONLY
B1_P1_B = MERGED_TEST_ONLY; ORIGINAL_FOUR_TABLE_REPLAY_GAP_CONFIRMED_NOT_SAFE
B1_P1_C_RACE_MATRIX = DESIGN_DEFINED_8_CASES
B1_P1_C_SCHEDULING_TIMEOUT_EVIDENCE = DESIGN_DEFINED_PHYSICAL_REVIEW_PENDING
B1_P1_C_ISOLATED_FILE_CI_INVENTORY = PROPOSED_6_NEW_FILES_NOT_AUTHORIZED
B1_P1_C_IMPLEMENTATION_READINESS = NOT_READY
B1_P1_C_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
B1_P1_C_REAL_JAVA8_MYSQL8_EVIDENCE = NOT_EXECUTED
B2_REAL_PRODUCTION_SPRING_U06 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
\`\`\`

**Next step:** \`D5-O2 Stage B — B1-P1-C C-ISOLATED Race / Timeout / Evidence Contract Targeted Independent Design Review + Scoped Implementation Authorization Decision\`. Before code implementation, close G02/G03/G04/G06 design review and freeze six-file list or a controlled amendment. Do not modify the Store, migration, existing P1-A/B assets, production authorization or external delivery.
