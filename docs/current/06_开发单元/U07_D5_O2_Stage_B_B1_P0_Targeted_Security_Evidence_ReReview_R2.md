# U07 D5-O2 Stage B — B1-P0 Targeted Security / Evidence Re-Review R2

Date: 2026-10-12. Pinned exact remote main `2834714354b02a88ccf73773511af706554cd9fb`, Draft implementation PR #437 HEAD `99b79a65ad4561f332e3c2440d73b9f4fbfe56d0`. Original review PR #438 HEAD `2ec05f57ba59b473dd51dd94d1c5e3d8adcd7dae`. Same-assistant technical re-review, **not independent human signoff**; no production or merge authorization.

## 1. Review subject and independently checked evidence

1. PR #437 still contains precisely **eight new test-only files** (dedicated workflow, four test Java classes, manifest and two scripts). No production source, Flyway migrations, original Stage A, original U06 workflow or application configuration touched. Main remains unchanged.
2. New latest-head GitHub Actions [run #38213805574](https://github.com/cxjchelsea/AIdoctor/actions/runs/38213805574), job `114693750953`, is SUCCESS; Java8 test compile and MySQL8 tests succeeded, **5 tests, 0 failures, 0 errors, 0 skipped**, evidence verifier `STAGE_B_P0_TESTS_EXECUTED=5`.
3. Downloaded exact Artifact #11730243812, 11,926 bytes, ZIP SHA256 `4b3ab1afd4a3f888afe8dd001ed8e3941a23f1a9d0ea53849b583ed1d57ea2f1`. Inspected three ZIP members: `surefire-reports/TEST-...StageBJdbcFaultHookTest.xml` (2 methods), `surefire-reports/TEST-...StageBTxOwnerBindingTest.xml` (3 methods), and `u07-d5o2-stage-b-p0-evidence.json`. XML shows 0 fail/error/skip; method names match five observation records. No literal synthetic test DB password string found in the ZIP bytes (not a comprehensive secret scanner).
4. Evidence JSON reports test checkout SHA `289cbd42c88dd5a74d046d27c05f429841e2bf23` (GitHub synthetic PR merge checkout, **not** branch HEAD), correct PR head `99b79a65ad4561f332e3c2440d73b9f4fbfe56d0`, Run ID `38213805574`, source baseline and five method observations. The workflow/validator checks `HEAD^2 == github.event.pull_request.head.sha`; two distinct SHA roles are preserved.
5. Observation JSON for physical hook: `faultOrdinal=1`, `successfulPhysicalSqlWrites=1`, `writerTxActiveAtFault=true`, `writerConnectionId=20`, `readerConnectionId=22`, `uncommittedReaderCount=0`, `freshObserverAfterRollback=true`, `postRollbackSumSixTables=0`, `readOnlyDeleteDenied=true`. Spring manager probe reports JDBC and JPA `Connection.TRANSACTION_SERIALIZABLE` (=8), manager resource identity and both sequential orders, plus two nested-call rejections and wrong-manager denial before callback. JPA/JDBC pooled connection IDs equal 27 in the recorded terminal sample: **not** evidence of overlapping active transactions; ordered stage cleanup assertions are what the test actually supports.

## 2. Finding disposition

| ID | R1 deficiency | Code/evidence re-review | R2 outcome |
| --- | --- | --- | --- |
| P0-R1-01 | Wrong manager negative manually threw after entering its transaction | `StageBTxOwnerBindingTest.stage(supplied,expected,ds,emf,operation)` checks clean resource map, **manager object identity** and manager-to-DS/EMF identity **before** creating/executing the `TransactionTemplate`. Wrong-manager test offers same-URL different DS, records callback not entered; nested calls rejected while active resources are held. This guard lives in test code only; do not claim production TxPort protection. | **ACCEPTED_P0_TEST_ONLY** |
| P0-R1-02 | Reader reused for pre/post, potential REPEATABLE READ false-zero; poor writer/reader evidence | Independent SELECT-only reader inspects uncommitted first write, then **fresh SELECT-only session** reads six tables after rollback; denies reader DELETE and records writer/reader connection IDs and effective transaction status. Checks cleanup via `clean()`. | **ACCEPTED_P0_N1_ONLY** |
| P0-R1-03 | No method-level physical observation evidence | Tests emit five separate JSON observations; evidence script binds each exact JUnit method to expected case ID, checks per-method structural/factual invariants, validates source DDL blobs, PR second-parent HEAD and run ID; independent ZIP inspection confirms reports/evidence coexist. A test owns its observations: this is evidence consistency rather than cryptographic proof of every SQL event. | **ACCEPTED_BOUNDED_EVIDENCE** |
| P0-R1-04 | Environment/disposable schema and read-only role not physically checked | Physical MySQL 8 `VERSION()`, `DATABASE()`, six delivery tables' InnoDB engines, pinned V2/V3/V6 SQL Git blobs, SELECT-only DML negative and fixed local endpoint. These are meaningful CI facts, but `GITHUB_ACTIONS`/fixed URL alone cannot attest an arbitrary non-CI environment, and the test does not prove full grants across all tables nor production isolation. | **ACCEPTED_WITH_SCOPE_LIMIT** |
| P0-R1-05 | SQL hook's unknown DML accepted, no negative | Test-only `StageBPostWriteFaultJdbcTemplate.update(String,Object...)` now requires exact next delivery INSERT table, no non-INSERT/unordered DML, active Spring transaction, Spring-bound connection; delegates **original SQL** to `super.update` first, throws after successful write ordinal. Negative test verifies unexpected delivery INSERT and DELETE fail before mutation; hooks are fault instrumentation, **not a production SQL authorization firewall**. | **ACCEPTED_P0_ONLY** |

## 3. Residual boundaries (not reopened P0 blockers)

- **R2-LIMIT-01**: Tests cover only the **first** of six SQL post-write fault ordinals; N=2..6 complete rollback/atomicity/replay/race tests belong to B1-P1, **not authorized by this review**.
- **R2-LIMIT-02**: Resource guard and identity checks are **test-class helper**, not an integrated production or B1-P1 TxPort. Never claim real U06 `@Transactional` effective Spring proxy/advice, real profile, StateCommitter owner or clinical execution authority.
- **R2-LIMIT-03**: Independent reader and per-case JSON still rely on test-emitted observations, not an external DB audit. Artifact was downloaded and ZIP inspected, but this does not make the test environment tamper-proof.
- **R2-LIMIT-04**: No real physical crash, unknown-commit, full Flyway V1–V6, Oracle or real external delivery; `F8_APPLIED/RESUME` remains unauthorized.

## 4. Formal disposition

```text
PR437_EXACT_HEAD_AND_FILE_SCOPE = PASS_8_TEST_ONLY_FILES
B1_P0_REAL_JAVA8_MYSQL8 = PASS_5_TESTS
B1_P0_ARTIFACT_BYTE_AND_CONTENT_REVIEW = PASS_BOUNDED
P0_R1_01 = CLOSED_TEST_ONLY
P0_R1_02 = CLOSED_N1_SCOPE
P0_R1_03 = CLOSED_BOUNDED_EVIDENCE
P0_R1_04 = CLOSED_WITH_SCOPE_LIMIT
P0_R1_05 = CLOSED_TEST_ONLY
B1_P0_TARGETED_SECURITY_EVIDENCE_RE_REVIEW = ACCEPTED_IN_ISOLATED_SCOPE
B1_P0_EXACT_MAIN_MERGE_READINESS_REVIEW = NEXT_SEPARATE_GATE
PR437_MERGE_AUTHORIZATION = NOT_GRANTED
B1_P1_P2_IMPLEMENTATION = NOT_AUTHORIZED
B2_ACTUAL_U06_PRODUCTION_SPRING_WIRING = UNVERIFIED
REAL_D5 = NOT_READY
```

**Next:** `D5-O2 Stage B — B1-P0 PR #437 Exact-Main Diff / Merge Readiness Assessment`, with a fresh exact-head/main/CI/branch protection check. Only an explicit later user merge authorization may permit a standard merge commit of PR #437; no other Draft design/review PR can be merged by implication.
