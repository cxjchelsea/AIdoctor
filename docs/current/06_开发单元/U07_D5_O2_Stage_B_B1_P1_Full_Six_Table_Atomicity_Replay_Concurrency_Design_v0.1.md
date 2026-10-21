# U07 D5-O2 Stage B — B1-P1 Full Six-Table Atomicity / Replay / Concurrency Design v0.1

Date: 2026-10-12. Source base `main=3b0589347d90af5ec5b86fbdad5eecabcce41a24` (PR #437 B1-P0 standard merge). Status: **design candidate, independent design review required; not implementation or merge authorization**. Invariant `REAL_D5=NOT_READY`.

## 1. Scope, inheritance, exclusions

B1-P1 tests only the existing `JdbcU06DeliveryStore.reconcileConfirmed(U06DeliveryStore.Command)` against **disposable synthetic MySQL8/InnoDB** via explicit caller-owned `DataSourceTransactionManager`, `PROPAGATION_REQUIRED` and `STRICT_NO_AMBIENT_OR_BOUND_RESOURCES`. Retain original unchanged `final` production class, SQL, tables and `@Transactional` annotation but **do not attribute effective transaction ownership to the annotation**. No production Spring Bean activation, outbound endpoint, recipient, StateCommitter S1/S2, clinical patient data, Oracle, F8 resume, B1-P2 cross-phase recovery, or real database migration.

Inherited *as tested mechanisms*, not new P1 evidence: P0 #437 isolated fixture, exact V2/V3/V6 DDL source-blobs, distinct writer/SELECT-only MySQL roles, Java8 setup, Spring manager object/DS identity preflight, real N=1 post-insert hook with fresh-connection rollback readback, per-case Surefire-to-JSON correlation. Do not rewrite Stage A/P0 existing manifest, workflow or existing tests. Stage P1 should use separate subpackage, environment/profile/schema/port/manifest/workflow and own tested HEAD.

## 2. Six-table ordered write contract (literal implementation)

Original `JdbcU06DeliveryStore.reconcileConfirmed` first performs a **four-table INNER JOIN lookup** by `delivery_effect_id`, then for new Effect writes:

| ordinal | table | uniqueness and semantic identity |
| --- | --- | --- |
| 1 | `u06_delivery_authority` | PK `selection_effect_id`; unique `delivery_effect_id`; authority `CONFIRMED_TERMINAL` |
| 2 | `u06_delivery_intent` | PK `delivery_effect_id`; unique `delivery_id`, `idempotency_key`; question, content, endpoint |
| 3 | `u06_delivery_attempt` | PK `delivery_effect_id + ':attempt:1'`; synthetic accepted |
| 4 | `u06_delivery_receipt` | PK `delivery_effect_id + ':receipt:1'`; `evidence_type=SYNTHETIC` |
| 5 | `u06_delivery_confirmation` | PK `confirmation_evaluation_id`; fingerprints and IDs |
| 6 | `u06_delivery_ledger` | PK `delivery_effect_id`; `confirmation_status=CONFIRMED` and canonical fingerprint |

**Contract reality:** the read query joins `ledger`, `authority`, `intent`, `confirmation` only; `attempt` and `receipt` are not joined. An externally created partial ledger can therefore be invisible or appear complete while auxiliary rows are missing. P1 must distinguish `ORIGINAL_METHOD_BEHAVIOR_OBSERVED` from `SIX_TABLE_RECONCILED_AUTHORITY`; do not label a replay valid merely because method returns `replay=true`. If real source behavior violates required invariant, **record a finding / fail acceptance** rather than quietly add production repair logic or claim authorization.

## 3. Transaction authority, failures and observer model

P1 chooses `P1_DELIVERY_LOCAL_JDBC_TEMPLATE`: explicit named `DataSourceTransactionManager` bound by object identity to P1 `JdbcTemplate` and one Hikari DataSource; fail closed if Spring actual transaction/synchronization/bound resource exists at entry, or a manager/DS identity differs. The wrapper opens **one short SERIALIZABLE** transaction for the lookup+six inserts; **no external calls inside**; rollback on unchecked exception. `TransactionTemplate` commit normally returns only after transaction manager reports completion; if connection/commit acknowledgement is lost, classify `UNKNOWN_COMMIT` until fresh independent readback. No blind new effect ID or conversion of unknown to rollback.

P1 post-write hook must continue executing the unchanged original SQL via `super.update(String,Object...)`, then inject exactly `N∈{1,2,3,4,5,6}` after a successful insert, without mocking write success; count only original exact allowed inserts and correct ordering, not SELECT. Test-only Java8 hook extension or copy confined to P1 test classpath. Do not share mutable failure counters between concurrent threads; one hook **instance per invocation**. For race tests with no fault, use a no-fault instrumented hook instance per request.

For each fault N:
1. Allocate unique synthetic `selectionEffectId,deliveryEffectId,deliveryId,idempotencyKey,confirmationEvaluationId`; capture fresh SELECT-only baseline count **for each of six tables** = 0.
2. Enter explicit authoritative JDBC TX; run unchanged store with hook N. In-hook assert `TransactionSynchronizationManager.isActualTransactionActive()`, actual bound connection, isolation SERIALIZABLE, writer connection ID, successful `N`-th SQL. An **independent read-only observer** sees no uncommitted inserted rows. A read-only DML attempt is denied (need not repeat every case if separately asserted by environment test).
3. Propagate precise fault; verify `TransactionTemplate` rolls back and resource map empty. After unwind open **fresh observer connection** (not reused REPEATABLE READ snapshot); count every table by the same `delivery_effect_id` and assert all 0.
4. Assert an **unrelated previously COMMITTED six-table effect** remains present and unchanged; otherwise test invalid. Record DB/version/engine/provenance, error code, SQL ordinal, before/uncommitted/after 6-count vectors, writer and reader identities, actual isolation, run/PR HEAD and transaction terminal class.
5. A source-scope native test cannot prove power-loss durability or every possible crash window. `UNKNOWN_COMMIT` is separate future evidence, not a test-created SUCCESS label.

**Required positive**: no-fault 6/6 inserts, count vector `[1,1,1,1,1,1]`, correct fields/status/fingerprint and one committed delivery, second identical `Command` returns `replay=true` with zero new writes and exactly same readback. Negatives: same effect/change contentFp, endpoint, confirmationFp, deliveryId or idempotency; cross-effect reuse of `selection_effect_id`, `delivery_id` and `idempotency_key`; conflicting `confirmation_evaluation_id` and replay equality. Reject DB constraint error as **CONFLICT/UNKNOWN pending readback**, not `REPLAY_MATCH` by assumption.

## 4. Concurrency and race contract

Run two distinct physical JDBC connections/transaction calls with deterministic synchronization barriers (bounded CountDownLatch / CyclicBarrier or test-only SQL hook) **outside network/production code**; an intentional barrier inside both transactions after a SERIALIZABLE SELECT may deadlock in MySQL. Tests must use a finite timeout and capture outcomes; **do not force that both racers complete a lock acquisition before releasing a barrier**. Verify deterministic start ordering with `ExecutorService` and bounded futures, distinct thread-bound resources and final readback using a third SELECT-only session.

| case | contenders | permitted raw observations | required authoritative conclusion |
| --- | --- | --- | --- |
| C1 same Effect/identical Command | A,B | one commit plus B replay OR B deadlock/serialization/duplicate-key failure | exactly one complete 6/6 fact group; loser must not report successful external confirmation without same authoritative readback; a **later sequential same-command replay** must be true, 0 new writes |
| C2 same Effect/different fp or endpoint | A,B | one commit, other conflict/serialization failure | never two confirmation groups; loser cannot claim `REPLAY_MATCH`; exact persisted winner controls conflict |
| C3 different Effect/same `idempotency_key` | A,B | unique-key/serialization failure for one contender | at most one effective confirmation; loser must not issue new downstream effect |
| C4 different Effect/same `selection_effect_id` or `delivery_id` | A,B | one commit, unique/serialization failure | no split authority across six tables; third-reader verifies correlation |
| C5 unrelated effects | A,B | both can commit unless environmental lock/timeout | both 6/6 stable, no interleaved cross-effect rows |
| C6 retry after losing concurrent race | losing request after winners settle | same command replay or typed conflict based on winner identity | no unverified `COMMITTED` claim; do not turn duplicate-key throwable into true replay without readback |

**Important**: current `find()` + insert flow does **not** catch, classify or retry MySQL deadlock/duplicate-key exceptions. P1 is to **measure** behavior, not retrofit production exception/replay handling. A deadlock/timeout result can be a legitimate *negative observation* but cannot be counted as an expected replay success or proof of a production retry policy. Candidate requirement to automatically make loser replay true would demand controlled production-design amendment; P1 cannot quietly implement it.

## 5. Adversarial partial-ledger contract

Because the original read path is a four-way INNER JOIN and does not include attempt/receipt, include **isolated synthetic manually seeded DB states only**, with reader/writer scoped to one disposable schema:
- `ledger` only / `authority` only / `intent` only / `confirmation` only: call original method, capture duplicate/conflict/insert path, and classify `PARTIAL_AUTHORITY_DETECTED` (test-side), not successful replay; leave seed unchanged on rollback.
- complete JOIN quartet missing `attempt` or `receipt`: existing method might return replay=true. **That is a known design-level gap** requiring an explicit assertion that full six-table authority is NOT satisfied, and a finding. Do not change original implementation in test-only P1 to conceal it.
- malformed matching quartet with different canonical fingerprint or confirmation status must reject. Matching effect but different incoming idempotency/endpoint must reject.

P1's acceptance is about **safe classification and evidence**, not asserting that the original six-table store already has production-correct recovery from corrupted ledgers. Findings should be separated into `SOURCE_DEFECT_CONFIRMED` vs `NOT_REPRODUCED` and routed to later controlled remediation if needed.

## 6. Proposed test-only implementation inventory and cases

Separate new files (proposed; **not yet approved**):
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1TestConfig.java`
- `.../p1/StageBP1AtomicityTest.java`
- `.../p1/StageBP1ReplayConflictTest.java`
- `.../p1/StageBP1ConcurrencyTest.java`
- `.../p1/StageBP1PartialLedgerTest.java`
- `.../p1/StageBP1Evidence.java` (test-only, fail-closed per-case facts, manager/reader IDs)
- `diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json`
- `tools/u07_d5_o2_tx_topology/stageb/p1/bootstrap-p1-schema.py`
- `tools/u07_d5_o2_tx_topology/stageb/p1/verify-p1-evidence.py`
- `.github/workflows/u07-d5-o2-stage-b-p1.yml`

Proposed IDs: `P1-FAULT-01..06` six independent ordinal cases; `P1-HAPPY-01`, `P1-REPLAY-01`; `P1-CONFLICT-01..06`; `P1-RACE-01..06`; `P1-PARTIAL-01..06`; `P1-SEC-01..03` (wrong DS / no prior ambient resource / reader DML prohibition). Implementation may combine tests but must map **each ID to one actual executed JUnit method and one specific evidence fact**, not one class-level summary. If fixtures need more files, amend whitelist before implementation.

CI constraints: dedicated P1 localhost port **33328** / schema `u07_d5o2_stage_b_p1`, own synthetic roles, Java 8, MySQL 8 disposable CI service; pinned original DDL V2/V3/V6 blob IDs must match current source, no Flyway V1/V4/V5/Oracle or production database access, validate physical schema, InnoDB, writer/reader privilege identities. The P0 `sourceMainSha` remains a historical baseline pinned to pre-merge SHA: never rewrite it; P1 `sourceMainSha=3b0589347d90af5ec5b86fbdad5eecabcce41a24` in new manifest. GitHub PR synthetic merge checkout HEAD vs exact candidate PR HEAD must be distinguished in evidence. All Artifacts synthetic and free of credentials or PHI. Dedicated job performs `test-compile`, JUnit exact method checks, machine validator, uploads full Surefire XML plus per-case physical evidence. Unexpected skip, timeout, deadlock **outside expected named race outcome**, absent physical observation, missing case, conflicting SHA or mismatched table count => fail closed.

## 7. Design invariants / security boundaries

- INV-P1-01: an all-or-nothing group of six per effect after a locally confirmed commit or injected rollback; **no partial per-effect record** in normal six-insert transaction.
- INV-P1-02: `replay=true` from method is **insufficient** as six-table authority without attempt/receipt readback and all fingerprints/IDs.
- INV-P1-03: no effect may be confirmed twice by rival writers; no duplicated idempotency or selection authority. Exactly-one-winner evidence requires third reader and full six table identity graph.
- INV-P1-04: no test converts a deadlock, lock timeout, duplicate key or connection failure into `COMMITTED` without fresh independent readback. `UNKNOWN_COMMIT` remains unknown until proven.
- INV-P1-05: **P1 tests use a single local JDBC transaction** only; no JPA/StateCommitter/external dispatch in this transaction; P0 dual-manager tests are inherited but not retested as P1's live target-runtime claim.
- INV-P1-06: no S2 delivered state, real recipient, F8 APPLIED or production activation merely because `SYNTHETIC_CONFIRMED` exists.
- INV-P1-07: barriers must be bounded and not introduce an artificial unrelated deadlock; a race that produces only timeouts cannot count as verified normal replay.
- INV-P1-08: original source must remain unchanged; if a source defect is discovered, document it and stop that acceptance criterion for separate remediation authorization.

## 8. Implementation readiness and blockers to independent review

`P1_DESIGN=V0.1_CANDIDATE`

`P1_IMPLEMENTATION_READINESS=NOT_READY_PENDING_TARGETED_INDEPENDENT_DESIGN_REVIEW`

`P1_EXECUTION_AUTHORIZATION=NOT_GRANTED`

**B1P1-RG-01 (BLOCKER)**: Precisely freeze acceptable race results, timeout/deadlock observation and **which business replay expectation** applies when original store throws after a race; do not require impossible automatic retry.

**B1P1-RG-02 (BLOCKER)**: Complete six-table replay authority contract vs original four-table INNER JOIN and absent attempt/receipt: decide whether P1 only detects source gap, or expects later controlled repair; cannot claim `FULL_SIX_TABLE_REPLAY_SAFE` until closed.

**B1P1-RG-03 (REQUIRED)**: Proof that an independent SELECT-only reader and per-table effect IDs can be verified for each test case with fresh connection; no cross-test mutable hook state, DDL provenance and exact PR HEAD binding.

**B1P1-RG-04 (REQUIRED)**: Bounded real MySQL concurrency fault orchestration cannot hang or accidentally treat a forced deadlock as production concurrency correctness.

**B1P1-RG-05 (REQUIRED)**: Proposed 10-file test-only inventory and CI profile need explicit design approval; no expansion to `src/main` or automatic Spring application scan. Stage P0 historic manifest must remain unchanged.

**Recommended next step:** `D5-O2 Stage B — B1-P1 Targeted Independent Design Review + Gap Closure Decision` to assess RG-01..05, then if needed a narrowly scoped v0.2 normative amendment and B1-P1 implementation authorization. No merge or implementation performed by this design.
