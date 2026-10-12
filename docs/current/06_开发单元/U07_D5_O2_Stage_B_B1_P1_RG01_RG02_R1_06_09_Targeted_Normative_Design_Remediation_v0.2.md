# U07 D5-O2 Stage B — B1-P1 Targeted Normative Remediation v0.2

Date: 2026-10-12. Exact baseline main = 3b0589347d90af5ec5b86fbdad5eecabcce41a24. Inputs: design PR #441 e10a008dd0d183ba73b644708a2321a2b9a0c312 and review PR #442 98991ad96a5c9a5aeffe4b7c451bcd49ac2432db.

STATUS: DESIGN CANDIDATE; independent re-review required; no P1 code, production change, merger, clinical authority or deployment. REAL_D5=NOT_READY. This amends only the ambiguous result classifications, six-table predicate, race scheduling, physical observation and implementation inventory in v0.1.

## A. Remediation decisions and four separately reported planes

Report every case in FOUR distinct planes, never coerce one into another:

- METHOD: ORIGINAL_NEW_SUCCESS, ORIGINAL_REPLAY_TRUE, ORIGINAL_THROW(exceptionClass, sqlState/vendorCode), ORIGINAL_NO_RESULT.
- TRANSACTION: COMMITTED_OBSERVED, ROLLED_BACK_OBSERVED, UNKNOWN_COMMIT, NOT_ATTEMPTED.
- DATABASE: SIX_COMPLETE, SIX_ABSENT, SIX_PARTIAL_OR_CONFLICT, UNOBSERVED.
- TEST: ATOMICITY_PASS, SOURCE_CHARACTERIZATION_PASS, RACE_INVARIANT_PASS, EXPECTED_RACE_FAILURE_WITH_VERIFIED_STATE, SOURCE_GAP_CONFIRMED, TEST_FAIL, HARNESS_TIMEOUT_FAIL, NOT_OBSERVED.

DOWNSTREAM_AUTHORITY must always be DENIED; a synthetic confirmed record does not permit real delivery, patient mutation, F8 resume or production wiring. Method returning replay=true is never by itself proof of six-table authority. A deadlock/duplicate key/lock wait error is not COMMITTED or replay success. If a commit acknowledgment is lost, preserve UNKNOWN_COMMIT until separately demonstrated by reliable fresh independent readback; P1 is NOT authorized to simulate physical power loss with a mock and claim recovery verified.

## B. RG-02, R1-07/08 — authoritative six-table predicate

A fresh SELECT-only MySQL connection, newly opened **after** the transaction, must read *all* rows for delivery_effect_id from all six tables and verify uniqueness, cross-row identity, command correlation and values:

1. u06_delivery_authority: exactly one row with matching selection_effect_id, effect ID, delivery ID and CONFIRMED_TERMINAL.
2. u06_delivery_intent: one with matching effect/delivery/question, content_fingerprint, endpoint_ref, idempotency_key and READY.
3. u06_delivery_attempt: exactly one, attempt_id=effectId+:attempt:1, matching delivery ID, SYNTHETIC_ACCEPTED.
4. u06_delivery_receipt: exactly one, receipt_id=effectId+:receipt:1, matching delivery ID, SYNTHETIC_CONFIRMED and evidence_type SYNTHETIC.
5. u06_delivery_confirmation: exactly one, matching confirmation_evaluation_id/effect/delivery and confirmation_fingerprint, status CONFIRMED.
6. u06_delivery_ledger: exactly one with matching effect/delivery, confirmation_status CONFIRMED and canonical_fingerprint matching command confirmation fingerprint.

The vector [1,1,1,1,1,1] is NECESSARY BUT NOT SUFFICIENT; IDs and values must all align. Attempt and receipt only have their own PKs in original V6, so count *all* per-effect matches, including malformed alternate identifiers; do not infer business uniqueness from their PKs. Record a digest of full six-table identity graph. Original JdbcU06DeliveryStore.find() uses only a FOUR-table INNER JOIN (ledger/authority/intent/confirmation) and cannot prove auxiliary attempt/receipt existence. Accordingly:
- A passing P1 SOURCE_CHARACTERIZATION test can prove original method returns replay=true on intact quartet missing attempt/receipt.
- That same observation must record SOURCE_GAP_CONFIRMED and a separately FAILED/NOT_ESTABLISHED claim of FULL_SIX_TABLE_REPLAY_SAFE.
- Never silently repair original production class, call this a safe replay, or authorize real effects. A future controlled production design amendment is needed to repair any confirmed source defect.

Partial seeded states (all distinct, fixture-only):
P1-PARTIAL-01 = ledger only; likely original lookup misses and later ledger PK conflict.
P1-PARTIAL-02 = authority only; possible authority PK collision at first write.
P1-PARTIAL-03 = intent only; possible conflict on second write and earlier writes rolled back.
P1-PARTIAL-04 = confirmation only; possible fifth-write collision and earlier writes rolled back.
P1-PARTIAL-05 = complete JOIN quartet but missing attempt; may replay=true and must detect gap.
P1-PARTIAL-06 = complete JOIN quartet but missing receipt; may replay=true and must detect gap.
Original method behavior is to be OBSERVED, not invented; any deviation is recorded as source characterization. Fixture states are deliberately impossible for a healthy single local TX and **must not** be presented as proof that a normal interrupted transaction persisted half a group. A distinct synthetic fixture-seeder MySQL account may create/clean them only in the isolated schema; ordinary test writer cannot fabricate them as execution side effects. Verify unchanged seed after rollback/replay and clean it in finally.

## C. RG-01/R1-06 — precise concurrency result matrix

Each row below fixes original-method outcome, physically observed DB outcome and P1 verdict. No contender may be called the winner merely because its future completed first.

| ID | Competing commands | Raw method outcomes allowed | Required third-reader observation | Verdict |
| --- | --- | --- | --- | --- |
| P1-RACE-01 | same effect, same Command | one NEW_SUCCESS; other REPLAY_TRUE or SQL race exception | one complete coherent six-table group; subsequent sequential identical replay=true with zero new INSERT | RACE_INVARIANT_PASS only if complete; two aborts/no group TEST_FAIL |
| P1-RACE-02 | same effect, different fingerprint/endpoint | one NEW_SUCCESS, other conflict or SQL race exception | one winner group, losing payload absent; no mismatched replay=true | RACE_INVARIANT_PASS with third-reader winner; two aborts TEST_FAIL |
| P1-RACE-03 | different effects, same idempotency key | one NEW_SUCCESS, one unique/SQL race exception | winner full group, losing effect all six zero | RACE_INVARIANT_PASS with one winner only |
| P1-RACE-04a | different effects, same selection_effect_id | one NEW_SUCCESS, one unique/SQL race exception | one complete group, other all zero, no split authority | RACE_INVARIANT_PASS with observed one winner |
| P1-RACE-04b | different effects, same delivery_id | one NEW_SUCCESS, one unique/SQL race exception | same integrity condition, plus correct intent uniqueness | RACE_INVARIANT_PASS with observed one winner |
| P1-RACE-05 | unrelated distinct effects | both NEW_SUCCESS | two independent complete six-table groups | PASS only if both commit; SQL timeout is not acceptable success |
| P1-RACE-06a | identical losing request *after* race | REPLAY_TRUE (sequential) | same winner full group, zero new writes | SOURCE_CHARACTERIZATION_PASS if true |
| P1-RACE-06b | changed losing request *after* race | typed conflict/SQL exception; never false replay | winner group unchanged | SOURCE_CHARACTERIZATION_PASS if correct conflict |

MySQL 1213 deadlock, 1205 lock-wait timeout, or duplicate key may count as EXPECTED_RACE_FAILURE_WITH_VERIFIED_STATE only in explicitly colliding scenarios with proven final group and correct loser status. The original method does not catch/retry these failures; do not misreport SQL exceptions as replay success. **If both transactions abort, outcome is TEST_FAIL/NO_WINNER_DIAGNOSTIC for cases requiring a winner**. If the result is physically unknowable, report NOT_OBSERVED or UNKNOWN_COMMIT, not passing evidence.

Deterministic bounded protocol (RG-04):
- Two Java8 ExecutorService workers, one named JDBC transaction, separate physical connections, separate hook instance and synthetic IDs per actor. CountDownLatch coordinates START **outside database transactions and before any SELECT/lock**; no barrier after lock acquisition, or it could fabricate a deadlock.
- Candidate budgets to verify in real runner: start latch max 3s; each Future.get max 15s; TransactionTemplate timeout 10s; per-session MySQL innodb_lock_wait_timeout=5s; Executor shutdownNow plus awaitTermination max 5s in finally. **HARNESS_TIMEOUT_FAIL** on startup timeout, unresponsive worker, cleanup failure, resource leak or unreadable final DB. Any change of time budget requires reviewed amendment.
- Third fresh SELECT-only MySQL connection after both workers terminate; check all six per-effect rows, identity and winner/loser statuses. Run a no-conflict two-winner positive control. Record actual writer/reader CONNECTION_ID, lock time, error SQLState/vendorCode, whether an expected DB contention error was observed; no reliance on scheduling luck as evidence of correctness.

## D. Six faults, replay, conflict — normative per-test matrix

P1-FAULT-01..06: each method uses a fresh hook/unique effect, unmodified JdbcU06DeliveryStore, a *single* explicit caller-owned DataSourceTransactionManager TransactionTemplate (PROPAGATION_REQUIRED, SERIALIZABLE, no outer thread resource). Hook calls super.update(originalSql,args) and only afterwards injects unchecked failure at ordinal N; assert exactly N successful physical INSERTs before failure. Assert method throws expected injected exception, original TX rolled back, beforeSix=[0,0,0,0,0,0], independent in-flight read sees zeros, and *fresh post-unwind observer connection* sees afterSix=[0,0,0,0,0,0]. A previously committed unrelated six-table group stays [1,1,1,1,1,1] with stable digest. At N=6 last INSERT must also roll back. In-flight SELECT is a visibility check and must not be permitted to block long enough to create an artificial deadlock.

P1-HAPPY-01: no-fault original six inserts, method new success and committed, six complete predicate true (keys/statuses/fingerprints, not counts only).
P1-REPLAY-01: exact same Command after settled committed operation gives replay=true, no new original delivery INSERT and unchanged complete group.

P1-CONFLICT-01..06: (01) same effect changed content fingerprint; (02) endpoint; (03) confirmation fingerprint; (04) delivery ID; (05) idempotency key; (06) distinct effect reuses selection ID, delivery ID, idempotency key or confirmation-evaluation ID as FOUR separately identified parameterized variants. These must observe typed conflict or SQL unique constraint (not replay success); original valid six-table group untouched and no partial group for losing new effect. Every variant must have a unique testcase/evidence case ID, not just an aggregate summary.

P1-PARTIAL-01..06: seed and interpretation as §B. Known full quartet missing auxiliary rows can produce SOURCE_CHARACTERIZATION_PASS + SOURCE_GAP_CONFIRMED, never a FULL_SIX_TABLE_REPLAY_SAFE pass.

P1-SEC-01..03: (01) mismatched manager/DS identity rejected before transaction starts, (02) ambient thread resources/profile/schema mismatch rejected, (03) independent SELECT-only reader cannot execute INSERT/UPDATE/DELETE and fixture-seeder privilege is separately scoped.

## E. RG-03/R1-09 — evidence and physical result schema

For each executed named JUnit testcase and parameterized variant, emit exactly one synthetic JSON fact and map it 1:1 to a fixed manifest. Required:
sourceMainSha, actualPrHeadSha, githubSyntheticMergeCheckoutSha, githubWorkflowRunId, junitClass, junitMethod, caseId, variantId, profile, schema, mysqlVersion, sixInnoDbChecks, V2/V3/V6 immutable Git blobs, managerId, dataSourceId, propagation, requestedIsolation, observedWriterIsolation, writerConnectionIds, inFlightReaderConnectionId, freshPostReaderConnectionId, readerSelectOnlyVerified, syntheticEffectId, syntheticSelectionId, deliveryIdDigest, key/fingerprintDigests, faultOrdinal, successfulSqlWrites, methodResult, exceptionClassAndSafeCode, physicalTransactionResult, sixBeforeVector, sixInFlightVector, sixAfterVector, sixTableIdentityGraphDigest, unrelatedGroupBeforeAfterDigest, raceOutcomes, harnessTimeout, sourceGapDetected, testVerdict, downstreamAuthority=false.

Independent SELECT-only reads must occur using distinct physical connection while writer transaction active and *new read transaction* after completion, not a stale REPEATABLE READ snapshot. Writer, reader and fixture seeder must have distinct roles. Never log passwords, PHI, real endpoints or live patient/user identifiers.

Verifier consumes original JUnit Surefire XML plus **test-emitted** observations; it must not synthesize or default positive facts. Any missing/skipped/duplicate/mismatch, wrong PR HEAD vs GitHub PR synthetic checkout, non-InnoDB, wrong DB/schema, reader privilege mismatch, unexpected deadlock, or HARNESS_TIMEOUT must fail closed. Do not claim real physical UNKNOWN_COMMIT/crash recovery without an authorized commit-acknowledgement-loss exercise; for P1 it remains NOT_TESTED.

## F. RG-05 — precise ten-file candidate scope and CI

Exactly ten allowed new files, subject to target design re-review before implementation:
1. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1TestConfig.java
2. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1AtomicityTest.java
3. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1ReplayConflictTest.java
4. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1ConcurrencyTest.java
5. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1PartialLedgerTest.java
6. diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1Evidence.java
7. diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json
8. tools/u07_d5_o2_tx_topology/stageb/p1/bootstrap-p1-schema.py
9. tools/u07_d5_o2_tx_topology/stageb/p1/verify-p1-evidence.py
10. .github/workflows/u07-d5-o2-stage-b-p1.yml

Production source, migrations, old U06 safety workflow, existing P0/Stage A tests, pom.xml and application.yml are out of bounds. If more fixtures are needed, use package-private classes in one authorized test file or seek separate amendment. Fixed new disposable MySQL8 port 33328, schema u07_d5o2_stage_b_p1, profile U07_D5O2_STAGE_B_P1_SYNTHETIC; distinct schema-only writer, SELECT-only reader and controlled test seeder, root only during bootstrap. Pin V2/V3/V6 blobs 73029495c5f46e8576e34f8c9a77644fb8c1c4c4 / ccc1aa30b8e9405bd07249bae1d3531853f7510b / 7ed5a82bfba3b3fed2666bee969a7fe276e67566 and P1 sourceMainSha 3b0589347d90af5ec5b86fbdad5eecabcce41a24. Never update P0's historical sourceMainSha.

CI: explicit Java 8, local disposable mysql:8.0, original DDL blobs verified, no full app scan or external service call, bounded races, exact PR HEAD/merge checkout parent validation, real JUnit/Surefire, per-case physical evidence validator, uploaded artifacts. Existing P0 path-filter overlap may invoke P0, whose PASS is not a substitute for P1.

## G. Normative gate status

B1P1_RG01 = DESIGN_REMEDIATED_RE_REVIEW_PENDING
B1P1_RG02 = DESIGN_REMEDIATED_RE_REVIEW_PENDING
B1P1_R1_06_09 = DESIGN_REMEDIATED_RE_REVIEW_PENDING
B1P1_RG03 = EVIDENCE_SCHEMA_FROZEN_EXECUTION_PENDING
B1P1_RG04 = RACE_PROTOCOL_FROZEN_EXECUTION_PENDING
B1P1_RG05 = TEN_FILE_SCOPE_FROZEN_RE_REVIEW_PENDING
B1P1_V02_DESIGN = SUBMITTED_FOR_TARGETED_RE_REVIEW
B1P1_FULL_SIX_TABLE_REPLAY_SAFE = NOT_ESTABLISHED
B1P1_IMPLEMENTATION_READINESS = NOT_READY
B1P1_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
B1P1_PHYSICAL_TESTS = NOT_EXECUTED
B1P2_AND_B2 = NOT_AUTHORIZED
REAL_D5 = NOT_READY

NEXT: D5-O2 Stage B — B1-P1 v0.2 Targeted Independent Design Re-Review + Scoped Implementation Authorization Assessment. Re-review must assess race verdict feasibility, source-gap split, parameterized testcase mapping, distinct three DB roles and artifact scope, then separately decide implementation readiness. No PR merger or production authority follows from this design.
