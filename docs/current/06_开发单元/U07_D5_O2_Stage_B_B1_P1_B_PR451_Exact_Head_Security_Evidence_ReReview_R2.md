# U07 D5-O2 Stage B — B1-P1-B PR #451 Exact-HEAD Targeted Security / Evidence Re-Review R2

Date: 2026-10-12. **Same-assistant technical re-review, not separate independent human security signoff**. Implementation PR #451 HEAD `dbbc9b072d7cee1fd03f675fe2567bbb1e512304`, target `main=e7562bd9b5390c85e14c3c01424c1004b1479ebc` (unchanged). Review basis: R1 Draft PR #452 head `6d0062d728f11d5c258886dad92ab6c2f8d9abb9`. This R2 document is review-only and does not authorize PR merge or production work.

## 1. Frozen exact tree and physical runner evidence

- #451 Draft, open, not merged. Exact main-target comparison = **seven ADDED p1b-only** Java test/config/manifest/tools/CI files, zero deletion, zero existing-file modification. Existing P1-A eight files and original production Store/Flyway/application configuration are unchanged.
- [Real Java8 + isolated MySQL8 CI #38229149457](https://github.com/cxjchelsea/AIdoctor/actions/runs/38229149457) SUCCESS: actual Surefire **15 tests, 0 failures, 0 errors, 0 skips** (6 partial-ledger, 9 sequential conflict). Machine validator prints both `STAGE_B_P1B_VERIFIED_CASES=15` and **`P1B_FOUR_TABLE_SOURCE_GAP=CONFIRMED_AND_NOT_SAFE`**. Same-head Stage A and P0 workflows succeeded independently; no claim of B1-P1-C or production CI.
- Artifact **#11737977225**, original ZIP size 14,484 bytes, not expired; independently downloaded and inspected raw ZIP, CRC, Surefire XML and JSON. SHA-256 **`34e0213d8a379043a355f5c5f3178c31000a9338f213dbb21a4665a8a97a0ac1`**. Contents: two original XML reports (6+9) and one structured 15-case evidence bundle. Bundle binds exact PR HEAD `dbbc9b072d7cee1fd03f675fe2567bbb1e512304`, distinct synthetic GitHub merge checkout `46b759eb3bbd6e7a62edea3c8d551d801a75aeae`, source main `e7562bd9b5390c85e14c3c01424c1004b1479ebc`, and run `38229149457`.

## 2. R1 finding dispositions — source and observed physical evidence

| R1 finding | Re-reviewed source / original Artifact | R2 disposition |
| --- | --- | --- |
| **P1B-R1-01** cleanup data previously exported as a constant | `StageBP1BEvidence.cleanup` physically DELETEs by effect with Seeder-only connection, COMMITs, opens fresh Reader, executes actual six-table SELECT, asserts zeros, and returns `CleanupResult` containing six-vector, complete partial-aware canonical digest, physical Seeder/Reader IDs and effect digest. All six test methods compare returned **actual** empty digest against pre-seeding independently queried same-effect empty digest; emitted `fixtureCleanupAfter` comes from `cleanup.state.vector`, **not** `new int[6]`. Independent ZIP checks six zero vectors, nonempty matching SHA-256, correct effect bind and distinct connection IDs; Python validator rejects missing/mismatched fields. | **CLOSED_TEST_ONLY** |
| **P1B-R1-02** three-role physical evidence incomplete | `Hook.bindWriter` confirms transaction-bound JDBC Connection, real MySQL `CURRENT_USER()` writer, `CONNECTION_ID()` and SERIALIZABLE isolation, attempts writer DELETE and requires privilege-denied SQLState `42000`. Reader DELETE negative is invoked; Seeder `CREATE TABLE` negative is invoked before artificial seeding. All six partial cases also assert distinct writer/reader/seeder connection IDs. Evidence includes writer ID/role/isolation, reader ID, seeder ID, negative guard flags, and physical cleanup reader/seeder IDs; Python verifier requires these. This is a **bounded P1B privilege test**, not a complete audit of all verbs/table grants in other environments. | **CLOSED_WITH_PHYSICAL_SCOPE_LIMIT** |
| **P1B-R1-03** loose SQL prefix inspection | Hook now uses exact six normalized **original `JdbcU06DeliveryStore` INSERT signatures** and fixed ordinal sequence, refuses extra/unordered SQL and increments successful counter only **after real `super.update` reports one updated row**. Artifacts show prefix traces of lengths 0/1/4/5 as appropriate; all 15 traces match the source's ordered tables, and the Python verifier matches each trace to expected successful ordinal count. This is test-only instrumentation, **not a production SQL permission firewall**. | **CLOSED_BOUNDED_ORIGINAL_SQL** |
| **P1B-R1-04** source-gap vs business verdict | Tests physically call a partial-aware six-table `snapshot`, then `fullSixCoherent`, and assert **false** for each of the 6 artificially seeded partial effects. For PARTIAL-05 (missing Attempt) and -06 (missing Receipt), physical source returns `replay=true` despite only five rows; both evidence entries require `sourceGapDetected=true`, `fullSixCoherentObserved=false`, `businessReplaySafetyVerdict=FAIL_NOT_ESTABLISHED`, `fullSixTableReplaySafety=FAIL_NOT_ESTABLISHED`. Other partial cases correctly report SQL constraint error, not safe replay. Python fail-closes on omitted or promoted classifications. | **CLOSED_CHARACTERIZATION_ONLY; PRODUCTION SAFETY NOT ESTABLISHED** |
| **P1B-R1-05** verifier negative tests | Current verifier robustly checks 15 distinct Surefire methods, class identity, PR-head checkout identity, source main ancestry, six counts + hashes, original INSERT traces, cleanup identity and source-gap/safety split. **No separate malicious mutation/negative fixture execution was supplied.** | **ADVISORY_OPEN_NONBLOCKING** |

The artifact's physical per-case writer IDs are positive and differ from reader IDs. Six partial cases show separate seed IDs and post-cleanup readback IDs. A subset of same-effect conflict cases legitimately has nonzero candidate group because candidate refers to the **existing healthy effect**; tests distinguish it from 06A-D distinct-effect losing candidates, whose six-table vectors must be zero.

## 3. Trust limits / residual controls

1. ZIP, JUnit and JSON assertions are from the CI/test-runner trust boundary; role negative **flags** are test-origin values, grounded in test assertions. This is not a third-party DB audit or externally signed attestation. We did not reproduce a second independent MySQL8 environment outside the logged run.
2. Only Writer DELETE, Reader DELETE and Seeder DDL privilege negatives were exercised; current tests do not audit all possible SQL permissions for each role. The bounded conclusion is adequate for *isolated synthetic test fixture ownership*, not a comprehensive production security certification.
3. The fixture Seeder deliberately commits synthetic incomplete groups. No evidence suggests production normal six-INSERT SERIALIZABLE transaction commits partial groups under crash; B1-P1-B exposes what **existing source** would do if such data is present.
4. **Original production source remains unsafe for six-table complete replay**: its four-way INNER JOIN omits Attempt and Receipt and can return `replay=true` when one is missing. This defect remains **OPEN** for a separately authorized production Store repair/design gate. It is **not** fixed by B1-P1-B test evidence passing.
5. Original `@Transactional` on manually constructed final Store is not proof of production-effective Spring proxy advice. No concurrent racing, unknown commit, crash recovery, cross-database Oracle, patient/PHI, external messaging, real F8 or B2 runtime activation was authorized or verified.

## 4. R2 formal decision

```text
PR451_EXACT_SEVEN_FILE_MAIN_DIFF = PASS
PR451_EXACT_HEAD_REAL_JAVA8_MYSQL8 = PASS_15
PR451_INDEPENDENT_ZIP_AND_ORIGINAL_XML_JSON_REVIEW = PASS_BOUNDED
P1B_R1_01 = CLOSED_TEST_ONLY
P1B_R1_02 = CLOSED_WITH_PHYSICAL_SCOPE_LIMIT
P1B_R1_03 = CLOSED_BOUNDED_ORIGINAL_SQL
P1B_R1_04 = CLOSED_CHARACTERIZATION_ONLY
P1B_R1_05 = ADVISORY_OPEN_NONBLOCKING
B1P1B_TARGETED_SECURITY_EVIDENCE_RE_REVIEW = ACCEPTED_IN_ISOLATED_SCOPE
B1P1B_PRODUCTION_FOUR_JOIN_REPLAY_SAFETY = NOT_SAFE_GAP_CONFIRMED
PR451_EXACT_MAIN_MERGE_READINESS = NEXT_SEPARATE_GATE
PR451_MERGE_AUTHORIZATION = NOT_GRANTED
B1P1C_CONCURRENCY = NOT_AUTHORIZED
B2_PRODUCTION_U06_AND_F8 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next:** `D5-O2 Stage B — B1-P1-B PR #451 Exact-Main Diff / Merge Readiness Assessment`. Recheck `main`, #451 exact head, seven-file diff, all named CI and artifacts, draft status and GitHub protection/approvals before any separate merge permission. Never implicitly merge #451, #452, this R2 document or any other PR. No production U06 remediation is authorized by this review.
