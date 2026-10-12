# U07 D5-O2 Stage B — B1-P1 v0.2 Targeted Design Re-Review + Scoped Implementation Authorization Assessment R2

Date: 2026-10-12. **Reviewed** Draft PR #443 exact HEAD `ad93bc67fdfb733cd15457ef1abc9f962e98d292`; pinned remote main `3b0589347d90af5ec5b86fbdad5eecabcce41a24`; underlying v0.1 design PR #441 and targeted review PR #442. The PR contains **one new Markdown document only** and is unmerged. Reviewed against main `JdbcU06DeliveryStore`, `U06DeliveryStore.Command`, V6 DDL and the merged B1-P0 verification assets. **Same-assistant technical re-review, not a separate human signoff. No B1-P1 Java8/MySQL8 execution occurred in this review.**

## 1. Core review decision

**RG-01 and RG-02 design blockers can be closed at the normative-contract level**, not at the implementation/production correctness level. v0.2 correctly distinguishes original method returns from real transaction result, six-table physical authority and test verdict, and treats four-table JOIN/missing auxiliary records as a source-gap characterization rather than a safe replay. Concurrency cases separate true race failure from a synthetic timeout or fabricated winner. The ten-file scope is limited to test-only/CI assets.

**Full B1-P1 implementation readiness is NOT_READY**. Appropriate next step is a constrained first implementation slice for the six post-write fault cases, six-table successful commit and sequential replay, plus authoritative evidence scaffolding. Concurrent-race and partial-ledger seeding are deferred pending targeted executability/contract review. This is a bounded **design-level implementation authorization decision**, not an automatic GitHub merge authorization and not proof the code already works.

## 2. Finding-by-finding disposition

| Finding | R2 conclusion | Basis and limitations |
| --- | --- | --- |
| RG-01 race/replay result ambiguity | **CLOSED_DESIGN_ONLY** | Four independent planes and P1-RACE-01..06 outcomes; original SQL exception cannot be mislabeled replay or commit; zero-winner case is FAIL, third-reader must prove any winner. Physical race correctness not tested. |
| RG-02 four-table JOIN / six-table authority | **CLOSED_DESIGN_ONLY_WITH_KNOWN_SOURCE_GAP** | All six row identities/fingerprints required; PARTIAL-05/06 may show original replay=true but must fail FULL_SIX_TABLE_REPLAY_SAFE. No source repair authorized. |
| R1-06 both-abort / read-before-write | **CLOSED_DESIGN_ONLY** | Both abort/no complete group cannot be reported as success, regardless of lock/deadlock exception ordering. |
| R1-07 artificial partial seed provenance | **CLOSED_DESIGN_ONLY** | Separate disposable seeder identity, committed setup transaction, case cleanup; explicitly not simulated ordinary six-statement crash persistence. |
| R1-08 auxiliary key asymmetry | **CLOSED_DESIGN_ONLY** | Full per-effect scan, attempt/receipt derived IDs and cross-table coherence; do not infer effect uniqueness from their PKs. |
| R1-09 unknown commit classification | **CLOSED_DESIGN_ONLY** | Commit-ack ambiguity remains UNKNOWN_COMMIT; no mock-fault-to-physical-crash upgrade. |
| RG-03 physical observation | **CONDITIONALLY_ACCEPTED; EVIDENCE_PENDING** | Frozen six-vectors + identity digests, fresh independent readback, typed method/transaction/group/test results and exact SHA provenance; machine-validator still must be built. |
| RG-04 bounded concurrency | **CONDITIONALLY_ACCEPTED; SPIKE_REQUIRED** | Start barrier outside transactions, 3s/15s/10s/5s bounded candidates and finally cleanup; actual MySQL lock/scheduler variance and timeout cleanup are not verified. |
| RG-05 ten-file inventory | **ACCEPTED_AS_CEILING_NOT_BLANKET_PERMISSION** | Files only in StageB p1 test/resources/tools and dedicated CI, no production migrations, beans, P0 edits, external calls or credentials. Actual slice scope smaller than full inventory. |

## 3. Residual implementation hazards requiring closure

**P1-R2-01 — BLOCKER BEFORE FULL B1-P1: parameterized case identity and fixture isolation.** v0.2 requires each parameterized P1-CONFLICT-06 variant, P1-RACE-04a/b and P1-RACE-06a/b to map to a distinct Surefire testcase and fact. It has not fixed Java8/JUnit5 method display-name normalization, Maven Surefire reporting strategy or how variant evidence avoids collisions. Implementation must use stable unique JUnit testcase names (individual methods are the default safer option), deterministic case-id manifest, isolated synthetic IDs, one JSON per execution, and fail-closed duplicate/missing verifier. It may not silently count one parameterized parent as four executed cases.

**P1-R2-02 — REQUIRED BEFORE FULL CONCURRENCY: timeout vs MySQL transaction outcome.** A 5-second MySQL lock-wait failure and 10-second Spring tx timeout may interact; `TransactionTemplate` timeout is not independently proof a JDBC driver interrupts every pending call. Any test runner future timeout is HARNESS_TIMEOUT_FAIL, and `shutdownNow()` must not be assumed to force-close a blocking MySQL JDBC connection. Before authorizing concurrent full slice, demonstrate bounded worker shutdown/connection closing in a disposable runner, or use a controlled time-bounded fail-closed CI termination with no PASS.

**P1-R2-03 — REQUIRED BEFORE FULL PHYSICAL ACCEPTANCE: six-row predicate/data ownership.** An all-zero count vector alone does not establish rollback on a previously existing effect; N=1..6 must operate on new unique synthetic IDs and first prove before=0, observe N actual writes, then query from fresh independent SELECT-only connection. A healthy pre-committed unrelated effect is a protected positive control. Correctly classify seeded-source-gap characterization independently from complete authority. Confirm role separation physically: writer, reader, fixture seeder; no admin privileges in target test execution.

**P1-R2-04 — REQUIRED BEFORE FULL RACE ACCEPTANCE: known original-method races.** Under SERIALIZABLE original four-table `find()` and six consecutive INSERTs, same-effect request might replay after winner or fail SQL; a negative exception can be an allowed raw observation, but is not a verified stable replay without later sequential read. Enforce third-reader winner/loser and both-commit positive controls. No blanket claim of linearizability or production exactly-once semantics.

**P1-R2-05 — REQUIRED BEFORE EXECUTION EVIDENCE CLOSURE: structured fact ownership.** JSON physical evidence must be emitted by tests, not filled in by Python verifier, and compared with raw Surefire exact-head output; require per-case original-error code, ordinal, six vectors and full identity graph plus physical readback. Separate GitHub synthetic merge checkout HEAD from exact source PR HEAD; no production authority fields may be set true.

## 4. Explicit bounded implementation authorization assessment

### B1-P1-A — isolated atomicity / sequential replay foundation

**`AUTHORIZE_LIMITED_TEST_ONLY_IMPLEMENTATION` (scoped decision).** P1-A may create a *new Draft implementation PR based on exact main* with only the following files drawn from the ten-file ceiling:

1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1TestConfig.java`
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1AtomicityTest.java`
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1ReplayConflictTest.java` — **only** happy path and identical sequential replay in Slice A, no full conflict matrix
4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1Evidence.java`
5. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json`
6. `tools/u07_d5_o2_tx_topology/stageb/p1/bootstrap-p1-schema.py`
7. `tools/u07_d5_o2_tx_topology/stageb/p1/verify-p1-evidence.py`
8. `.github/workflows/u07-d5-o2-stage-b-p1.yml`

One physical test for each `P1-FAULT-01..06` (real post-successful original SQL write and rollback verified via fresh read-only session), `P1-HAPPY-01` (six-table success+full identity), and `P1-REPLAY-01` (no new writes / original snapshot replay true and full group retained). Maintain an unrelated healthy six-table effect as positive control. Java8 real MySQL8 disposable schema `u07_d5o2_stage_b_p1` loopback port 33328, exact V2/V3/V6 Git blob hashes, minimal test context, named JDBC manager with STRICT_NO_AMBIENT, fresh observers, physically separate write/SELECT-only roles, full per-case Surefire+JSON validation. In Slice A the separate fixture seeder role need only be frozen in bootstrap/manifest, not used until a later authorization. The existing StageB P0 source/class/files must remain unchanged. If hook test-only subclass duplication into allowed classes cannot be compiled, stop for scope amendment.

**This authorization does not waive** exact Java8 compile, physical MySQL run, per-case evidentiary facts and targeted implementation/security review. A green CI by itself is not merge authorization.

### B1-P1-B — adversarial partial-ledger source characterization

`NOT_AUTHORIZED_YET`. Requires closure of source-gap expected evidence and a test-only fixture-seeder role/scoped cleanup protocol, plus a separate approval prior to implementing StageBP1PartialLedgerTest or full conflict variants.

### B1-P1-C — deterministic concurrency

`NOT_AUTHORIZED_YET`. Requires P1-R2-01/R2-02 executable resolution, including stable parameterized identifiers, bounded lock waits, true shutdown/cleanup and exact third-reader verdict, before implementing StageBP1ConcurrencyTest.

### Production and downstream

**NEVER AUTHORIZED** by this document: source `JdbcU06DeliveryStore` modifications, real Spring profile/Bean AOP activation, StateCommitter / S2 delivery transition, B1-P2 cross-stage recovery, B2, real patient or external delivery, F8, MySQL production, Oracle, or merging any PR.

## 5. Formal gate record

```text
PR443_EXACT_HEAD_TARGETED_DESIGN_RE_REVIEW = COMPLETE
B1P1_RG01_RG02_R1_06_09 = CLOSED_DESIGN_ONLY
B1P1_RG03_RG04 = CONDITIONALLY_ACCEPTED_PHYSICAL_EVIDENCE_PENDING
B1P1_RG05_TEN_FILE_CEILING = ACCEPTED_WITH_SUB_SLICE_RESTRICTIONS
B1P1_A_IMPLEMENTATION_AUTHORIZATION = AUTHORIZE_LIMITED_TEST_ONLY
B1P1_A_JAVA8_MYSQL8_VERIFICATION = NOT_EXECUTED
B1P1_B_PARTIAL_LEDGER_IMPLEMENTATION = NOT_AUTHORIZED
B1P1_C_CONCURRENCY_IMPLEMENTATION = NOT_AUTHORIZED
B1P1_FULL_IMPLEMENTATION_READINESS = NOT_READY
B1P1_FULL_SIX_TABLE_REPLAY_SAFE = NOT_ESTABLISHED
PR443_MERGE_AUTHORIZATION = NOT_GRANTED
REAL_D5 = NOT_READY
```

**Next authorized operation:** `D5-O2 Stage B — B1-P1-A Isolated Six-Ordinal Atomicity + Sequential Replay Implementation`, within the eight-file test-only scope above; independently verify physical evidence before any broader authorization. For full P1, first close P1-R2-01/R2-02 then independently authorize slices B/C. This re-review is a technical assessment by the same assistant, not a separate independent human security approval.
