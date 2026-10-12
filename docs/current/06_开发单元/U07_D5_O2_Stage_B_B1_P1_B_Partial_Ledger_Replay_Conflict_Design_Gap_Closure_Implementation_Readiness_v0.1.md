# U07 D5-O2 Stage B — B1-P1-B Partial Ledger / Replay Conflict Design Gap Closure + Scoped Implementation Readiness v0.1

Date: 2026-10-12. Exact baseline: **main e7562bd9b5390c85e14c3c01424c1004b1479ebc**, containing only the merged nonproduction B1-P1-A PR #445. Related design candidate PR #443 and technical review #444 (Draft/unmerged) remain source inputs, **not** assumed main-frozen standards. Status: **DESIGN CANDIDATE; targeted design re-review required; implementation NOT AUTHORIZED by this document**. REAL_D5=NOT_READY.

## 1. Goal and permission boundary

B1-P1-B answers two questions about the *unchanged* production-source class JdbcU06DeliveryStore.reconcileConfirmed(Command): (1) How does a normal same-effect or cross-effect replay-conflict request behave on a real isolated MySQL8 database? (2) What does the original four-table INNER JOIN do when a separately seeded, intentionally incomplete six-table Delivery record is already persisted?

This is a **source characterization and evidence exercise**, **not** an instruction to make an incomplete ledger safe, activate production Spring advice, repair U06, simulate legitimate crash-produced partial writes, or grant clinical/delivery authority. The original Store implementation, SQL and annotations must remain unchanged. Its @Transactional annotation on a manually constructed final class does **not** prove production-effective AOP. Use caller-owned DataSourceTransactionManager / SERIALIZABLE / REQUIRED and STRICT_NO_AMBIENT_OR_BOUND_RESOURCES as merged B1-P1-A does. No patient/PHI, real recipient, external endpoint, StateCommitter, downstream mutation, F8 resume, Oracle, B1-P1-C racing, B1-P2, or B2.

Six physical tables (V6 immutable original DDL): authority, intent, attempt, receipt, confirmation, ledger. The original **lookup uses ONLY ledger + authority + intent + confirmation**. Normal write path inserts all six in that order. Attempt and receipt have only their individual primary keys; confirmation has PK on confirmation_evaluation_id (not unique effect ID). Evidence must query *all* rows per delivery_effect_id, not treat a single JOIN hit as complete or misstate which constraints actually exist.

## 2. Frozen four-plane classification (all cases)

Each **executed individual JUnit case** records:
- ORIGINAL_METHOD_RESULT: ORIGINAL_NEW_SUCCESS / ORIGINAL_REPLAY_TRUE / ORIGINAL_THROW_TYPED_CONFLICT / ORIGINAL_THROW_SQL_CONSTRAINT / ORIGINAL_THROW_OTHER;
- PHYSICAL_TX_RESULT: COMMITTED_OBSERVED / ROLLED_BACK_OBSERVED / UNKNOWN_COMMIT / NO_NEW_TX_WRITES_OBSERVED;
- SIX_TABLE_GROUP: SIX_COMPLETE / SIX_ABSENT / SIX_PARTIAL_OR_CONFLICT / UNOBSERVED, with counts in the canonical order authority/intent/attempt/receipt/confirmation/ledger and a safe SHA-256 of all six source-row projections (even when individual tables contain zero rows);
- TEST_RESULT: SOURCE_CHARACTERIZATION_PASS / CONFLICT_GUARD_PASS / SOURCE_GAP_CONFIRMED / UNEXPECTED_SOURCE_BEHAVIOR / TEST_FAIL;
- DOWNSTREAM_AUTHORITY: **DENIED always**.

A SOURCE_CHARACTERIZATION_PASS means the test accurately observed the existing method's behavior; it does **not** imply FULL_SIX_TABLE_REPLAY_SAFE. If an intact four-table JOIN quartet without an attempt or receipt yields ORIGINAL_REPLAY_TRUE, emit both SOURCE_CHARACTERIZATION_PASS and SOURCE_GAP_CONFIRMED, and mark the *business safety claim* **FAIL_NOT_ESTABLISHED**. No validator may turn a source gap into a green business-safety verdict or obscure it by excluding the case.

UNKNOWN_COMMIT is never inferred simply from a caught exception; no physical commitment ambiguity/crash-recovery experiment is authorized in B1-P1-B.

## 3. Detailed partial-ledger matrix: separate synthetic fixture-seeding authority

Only a distinct **stagebp1_seeder** account can create artificial partial records. It has per-table scoped DML privileges in *disposable schema* u07_d5o2_stage_b_p1; production and app users cannot call it. Each partial case:
1. starts from **unique, synthetic effect, selection, delivery, evaluation and idempotency IDs**; cross-test identities never overlap; assert empty before seeding (all six zero);
2. commits a deliberately incomplete fixture using a separate seeder connection and an explicit setup transaction (never through the Store or within the Store's transaction); captures exact six-table partial vector **and a canonical projection digest** using a separate SELECT-only account;
3. invokes **unchanged** original Store with the *same matching command* in its own short SERIALIZABLE transaction, captures method return or precise Java/SQLState/vendor error and any actual SQL write attempts;
4. after completion, a **fresh SELECT-only connection** checks all six rows and identity graph (including unexpected extras and orphan identifiers). For throws, assert the seeded partial group is unchanged; for replay, assert no new INSERTs and unchanged partial state, while recording source-gap if the six-table predicate is false;
5. cleans up its *own* synthetic fixture through seeder-only DELETE, verifying all six per-effect rows gone; never let a test hide a failure in finally, never delete unrelated already-committed healthy positive control. A cleanup failure is a hard test failure. Before/after protected healthy complete-group digest must match.

Separate processes/connection pools or per-case isolated database namespace can be used; the exact selection must be frozen before implementation. Seeder DML must not be executed by any production bean or test candidate writer merely because writer already has DML for original operations. A seeded partial state is **not claimed as something produced by an ordinary single atomic six-INSERT Store execution or a real application crash**.

| Case | Seeder committed rows for same effect | Anticipated original method path, to be PHYSICALLY OBSERVED | Valid test characterization (never a production-safe-replay assertion) |
| --- | --- | --- | --- |
| P1-PARTIAL-01 | ledger only | four-way JOIN misses, attempts six INSERTs, likely ledger PK conflict on #6 | throw/rollback, seeded ledger unchanged, original outcome recorded |
| P1-PARTIAL-02 | authority only | JOIN misses, authority PK or unique-effect collision on #1 | throw/rollback, seed unchanged |
| P1-PARTIAL-03 | intent only | JOIN misses, intent effect PK collision on #2; authority write rolled back | throw/rollback, seed unchanged |
| P1-PARTIAL-04 | confirmation only | JOIN misses, confirmation PK conflict on #5; prior writes rolled back | throw/rollback, seed unchanged |
| P1-PARTIAL-05 | matching ledger+authority+intent+confirmation; **attempt absent**, receipt present | JOIN can return replay=true despite absent attempt | record source gap separately; incomplete six-table group is never accepted |
| P1-PARTIAL-06 | matching quartet, attempt present, **receipt absent** | JOIN can return replay=true despite absent receipt | record source gap separately; incomplete six-table group is never accepted |

Important: P1-PARTIAL-05/06 seeds **exactly five rows, omitting only one auxiliary row**; this isolates each missing-side condition. For all six, no mutation of preexisting fixtures is acceptable. Exact error ordinal is **a source-based hypothesis, not an invented CI result**. If first SQL outcome differs, report the observed trace and classify UNEXPECTED_SOURCE_BEHAVIOR rather than forcing the anticipated label.

For matched quartet tests, assert every field used by Existing.matches(Command) genuinely matches, including selectionEffectId, deliveryEffectId, deliveryId, question, contentFingerprint, endpointRef, idempotencyKey, confirmationEvaluationId and both canonical/confirmation fingerprints; confirmationStatus must be CONFIRMED. Record original replay=true separately from full six-table failure. Include one clean **complete six-table positive control** from P1-A to ensure the discriminator is sensitive to missing auxiliary rows.

## 4. Replay-conflict matrix: deterministic sequential only

Create and COMMIT a complete healthy six-table record through the unmodified Store under caller-owned TX. Commit before running any conflict candidate. No racing workers; B1-P1-C remains out-of-scope. Capture original six-table graph digest, six-row count vector, and conflict candidate identifiers. Every variant must guarantee the previously committed winner remains exactly unchanged, that no attempted downstream delivery occurs, and that a distinct-effect loser has zero new records after the failed transaction.

| Case ID | Candidate variation | Source-specific expected guard | Evidence required |
| --- | --- | --- | --- |
| P1-CONFLICT-01 | same delivery_effect_id, changed contentFingerprint | Existing.matches mismatch => U06_DELIVERY_REPLAY_CONFLICT | typed conflict; full graph digest unchanged |
| P1-CONFLICT-02 | same effect, changed endpointRef | Existing.matches mismatch | same |
| P1-CONFLICT-03 | same effect, changed confirmationFingerprint | both confirmation/canonical matching fail | same |
| P1-CONFLICT-04 | same effect, changed deliveryId | Existing.matches mismatch | same |
| P1-CONFLICT-05 | same effect, changed idempotencyKey | Existing.matches mismatch | same |
| P1-CONFLICT-06A | distinct effect, reused selectionEffectId | original find(newEffect) misses; authority selection PK conflict | SQL constraint; new effect absent |
| P1-CONFLICT-06B | distinct effect, reused deliveryId | intent unique(delivery_id) conflict (authority must roll back) | same |
| P1-CONFLICT-06C | distinct effect, reused idempotencyKey | intent unique(idempotency_key) conflict | same |
| P1-CONFLICT-06D | distinct effect, reused confirmationEvaluationId | confirmation PK conflict on later INSERT, earlier writes roll back | same |

**Nine** individually named JUnit cases (01..05 and 06A..06D), not six ambiguous parent cases. Every case has one unique caseId, testcase classname+name, method result, source SQLState/vendorCode or exact typed conflict, original winner digest before/after, candidate effect six-count vector, source-of-authority classification, PR/run provenance, and no real delivery. Distinct-effect 06D must not assert a non-existent unique constraint on confirmation.delivery_effect_id; use the actual confirmation_evaluation_id primary key.

For matching (unchanged) Command sequential replay, inherited P1-A verifies replay=true and zero new INSERTs; B1-P1-B must not re-label it as a conflict, and should preserve P1-A as independent regression evidence.

## 5. Test fixture / CI design boundaries and candidate inventory

**B1-P1-A is now merged** with exact main source HEAD e7562bd9b5390c85e14c3c01424c1004b1479ebc; its historical manifest sourceMainSha points to the earlier P1-A implementation baseline 3b0589347d90af5ec5b86fbdad5eecabcce41a24 and **must not be rewritten**. Existing P1-A tests, SQL hook, CI and validator must remain intact unless a separate, reviewed shared-capability amendment is explicitly approved.

The earlier B1-P1 full ten-file design inventory considered two additional **Java classes**:
- diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1PartialLedgerTest.java
- diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1ConcurrencyTest.java

**B1-P1-B may request only PartialLedgerTest, never ConcurrencyTest.** Nine conflict cases might fit package-private helpers / existing StageBP1ReplayConflictTest, but extending the merged P1-A test file, the pinned P1-A manifest, the stage P1-A workflow and verifier **would alter an already accepted baseline**. No such edits are implicitly authorized. Until a targeted review explicitly approves the *exact new/modified file list*, implement either:
- Preferred **B-ISOLATED**: a new dedicated B1-P1-B test class (containing partial-ledger and conflict cases), a **new B-specific manifest/validator/CI workflow** and case-evidence helper in a strictly frozen allowlist, keeping all 8 merged P1-A files untouched; **requires controlled amendment** to the earlier ten-file ceiling; OR
- **B-SHARED**: explicitly amend the previously merged P1-A StageBP1ReplayConflictTest, manifest and verifier under an audited shared-evidence compatibility plan, demonstrating P1-A eight methods and exact historical evidence contracts remain intact. This is *not* favored until independent impact review.

No implementation readiness until the file/CI choice is frozen and exact path inventory reviewed. On future implementation: run in a disposable MySQL8 service, Java8, schema+role+DDL digest guards pinned to exact V2/V3/V6 original Git blobs (73029495c5f46e8576e34f8c9a77644fb8c1c4c4, ccc1aa30b8e9405bd07249bae1d3531853f7510b, 7ed5a82bfba3b3fed2666bee969a7fe276e67566); can use separate B-only schema and port if isolation provenance is explicit. Root bootstrap only, distinct transactional Store writer, SELECT-only observer and restricted fixture seeder credentials, least privilege by exact six tables. No production app scan, no external delivery, no patient data or F8.

B implementation must actually compile Java8, run all 6 partial + 9 conflict cases against real MySQL8, upload original Surefire XML and per-case physical observations; verifier must *consume* (not invent) facts. Require evidence provenance showing main baseline, PR head, tested synthetic merge checkout (second parent), workflowRunId, JUnit class/testcase identity, schema/connection/role, fixture seeding setup+cleanup, pre/post six vectors and canonical digests, typed source outcome and exception, separate source-gap and six-table-business-safety statuses. Unexpected skip/duplicate/missing/cross-schema test, wrong role/DDL, SQL timeout or fixture cleanup leak => fail closed.

## 6. Gap closure / implementation readiness assessment

| Finding | Decision now | Required before implementation authorization |
| --- | --- | --- |
| **B1B-G01** partial-ledger behavior expected outcomes | DESIGN CONTRACT FROZEN | independent review of six physical fixture states and exact error-class interpretation; protect source-gap split verdict |
| **B1B-G02** replay conflict variants | DESIGN CONTRACT FROZEN | approve nine individually named case IDs and exact unique constraints |
| **B1B-G03** seeder and read-only observer permissions | CONDITIONAL — BLOCKER | freeze dedicated scoped credential handling, independently committed seed, per-case cleanup and truly separate reader/writer, prove no production credential paths |
| **B1B-G04** post-P1-A file and evidence ownership | **OPEN — BLOCKER** | choose B-ISOLATED vs B-SHARED and freeze exact files, workflow, manifest, validator and regression impact before creating any test code |
| **B1B-G05** canonical digest for partial (0..5 rows) | CONDITIONAL — REQUIRED | define one all-row projection that works with missing tables and multiple-orphan rows; reused P1-A graphDigest() calls complete() and **cannot** digest partial states |
| **B1B-G06** exact CI / runner evidence boundary | CONDITIONAL — REQUIRED | pin workflow/baseline PR-head semantics and P1-A regression plus new suite isolation; ensure tests cannot turn SOURCE_GAP_CONFIRMED into safe replay green label |

**Crucial finding:** the merged P1-A graphDigest() requires all six rows and cannot be used for partial-ledger evidence. A separate generic read-only all-rows canonical digest is necessary. Implementing P1-B with only the old P1-A complete() verifier would produce errors or conceal the condition.

Formal gate decision:
\`\`\`text
MAIN_BASELINE = e7562bd9b5390c85e14c3c01424c1004b1479ebc
B1_P1_A = MERGED_TEST_ONLY
B1_P1_B_PARTIAL_MATRIX = DESIGN_DEFINED_6_CASES
B1_P1_B_CONFLICT_MATRIX = DESIGN_DEFINED_9_CASES
B1_P1_B_SOURCE_GAP_SAFE_REPLAY = NOT_ESTABLISHED
B1_P1_B_DESIGN_GAP_CLOSURE = CONDITIONAL_WITH_OPEN_SCOPE_AND_SEEDER_BLOCKERS
B1_P1_B_IMPLEMENTATION_READINESS = NOT_READY
B1_P1_B_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
B1_P1_B_REAL_MYSQL_TESTS = NOT_EXECUTED
B1_P1_C_CONCURRENCY = NOT_AUTHORIZED
B1_P2_AND_B2_PRODUCTION = NOT_AUTHORIZED
REAL_D5 = NOT_READY
\`\`\`

**Next recommended step:** **B1-P1-B B-ISOLATED Test-Only File/Seeder/Evidence Contract Targeted Design Review + Implementation Authorization Decision**. Close B1B-G03/G04 and the partial-digest G05; only then create a dedicated B-only test implementation Draft PR. This design document itself grants no production repair, code change, repository merge, clinical activation or delivery authority.
