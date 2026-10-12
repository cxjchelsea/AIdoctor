# U07 D5-O2 Stage B — B1-P1-A PR #445 Targeted Security / Evidence Re-Review R2

Date: 2026-10-12. Read-only **same-assistant** technical re-review, not separately signed independent human security approval. Examined original implementation Draft PR #445 exact HEAD `abc7c9c70adf128dab0041364697128a8dda8db2`, base and unchanged target `main=3b0589347d90af5ec5b86fbdad5eecabcce41a24`. Earlier issue review: PR #446 R1. No change to implementation or production; this document records review and does not authorize merging.

## 1. Exact scope and independently inspected physical evidence

- PR #445 remains Draft and unmerged. Its exact main-target diff contains **eight ADD-only authorized** B1-P1-A test/config/manifest/CI files: `.github/workflows/u07-d5-o2-stage-b-p1.yml`, four `StageBP1*` Java test files under `diagnosis-service/src/test/.../stageb/p1/`, `p1/manifest.json` and two `tools/u07_d5_o2_tx_topology/stageb/p1/` scripts. No `src/main`, production wiring, Flyway, original P0/Stage A edits, Oracle, or live-delivery code.
- Real GitHub Actions [run #38223135077](https://github.com/cxjchelsea/AIdoctor/actions/runs/38223135077), job #114724009056: Java 8 compile and isolated MySQL 8 test suite **SUCCESS**, Surefire `6 + 2 = 8 tests`, zero failures/errors/skips; strict verifier emits `STAGE_B_P1A_VERIFIED_CASES=8`. Separately triggered overlapping P0 and Stage A workflows also success; these do not promote P1-A/production authority.
- Artifact #11734952822 (`u07-d5-o2-stage-b-p1a-surefire`, 12,912 bytes) was freshly downloaded; independent ZIP CRC and SHA-256 checked: `4fbcafa443497be1e135bbe9eec093e8a3b47a33401449adeeac31bbc2a5896b`. Exactly two original Surefire XML reports (6 + 2) and structured `u07-d5o2-stage-b-p1a-evidence.json`; eight method facts. Bundle PR HEAD `abc7c9c70adf128dab0041364697128a8dda8db2`, workflow run `38223135077`, and GitHub synthetic merge checkout `f1d0a02ac90238dde2ac772da7e10352d5ffc0b9` are distinct and correctly identified.
- Original method `JdbcU06DeliveryStore.reconcileConfirmed` remains unchanged and is manually instantiated; six ordered delivery INSERTs run in **explicit caller-owned local** `DataSourceTransactionManager` `SERIALIZABLE` scopes, not production Spring AOP. Hook calls actual `super.update` before throwing the precise N=1..6 exception. Fresh SELECT-only MySQL connections check per-effect rollback count vector `[0,0,0,0,0,0]`; unrelated fully committed group is protected. Happy path commits one coherent six-table group and sequential identical replay returns replay=true without added INSERTs. This covers only **B1-P1-A**.

## 2. R1 finding disposition

| R1 finding | Evidence verified on exact HEAD | R2 decision |
| --- | --- | --- |
| **P1A-R1-01**, six-table identity graph missing | `StageBP1Evidence.graphDigest()` first calls `complete()`, validating all six required table rows, cross-table delivery/effect keys, statuses and source command fingerprints. It then canonicalizes **all actual columns** in each table row with sorted column names, table names, domain separator `P1A-SIX-GRAPH-V1`, length-prefixing and SHA-256. Positive/replay tests export digests from actual independent SELECT-only DB readback. Verifier requires nonempty 64-char lowercase SHA-256 shape, proper JUnit identity, case and exact run provenance. | **CLOSED_TEST_ONLY** |
| **P1A-R1-02**, healthy protected effect only count-checked | Every N=1..6 fault test reads healthy group before/after via independent connection, asserts `complete()` and **full graph digest equality**, emits two valid 64-hex digests; independent Artifact review confirms six pairs equal. Before/after six-count vectors remain `[1,1,1,1,1,1]`, and candidate rollback vector is all zero. Distinct candidate/protected effect ID digest assertions and manifest verifier included. | **CLOSED_N1_TO_N6** |
| **P1A-R1-03**, no reader write-negative | `assertReaderPermission()` verifies physical MySQL `CURRENT_USER()` begins with `stagebp1_reader@`, executes zero-row `DELETE FROM u06_delivery_authority`, requires JDBC SQLException with SQLState `42000`. All 8 tests invoke this check; their facts contain `readerDmlDenied=true`, and verifier requires it. This proves DELETE-denial on one table in this isolated P1 database, **not a privilege audit of every table/operation**. | **CLOSED_WITH_SCOPE_LIMIT** |
| **P1A-R1-04**, SQL signature and TX provenance under-specified | Hook now matches **exact normalized six original INSERT signatures** and order; asserts real Spring-bound transactional connection, executes real `super.update` (one affected row) before fault. Caller checks manager-to-DS identity and actual Connection isolation `SERIALIZABLE`. Facts bind original Surefire class/method, exact PR head, GitHub synthetic checkout, workflow run ID, strict SQL flag, isolation and per-case outcomes. Python verifier checks hash shape/equality, six ordinal positions and all 8 executed methods, failing closed on missing/skipped/mismatched facts. Hook remains **test-only fault instrumentation, not a SQL authorization firewall**. | **CLOSED_BOUNDED** |
| **P1A-R1-05**, evidence provenance advisory | Bundle and each emitted fact distinguish exact PR head from checkout and bind class, method and run. Still self-generated by test runner; no external, tamper-proof DB audit has been performed. | **RESOLVED_WITH_TEST_TRUST_BOUNDARY** |

## 3. Explicit residual limitations / observations

1. The structured evidence contains SHA-256 commitments and observed counts but not raw six-table rows, by design to avoid exporting individual synthetic IDs broadly. This is **verifiable provenance/consistency of the tests and their code**, not an independent re-computation of row hashes using an external trusted database observer. Do not claim cryptographic end-to-end attestation of external systems.
2. SQL normalization checks exact original normalized INSERT strings, but `Hook` is an instrumented test `JdbcTemplate`, not an authorization firewall. A separate positive test that attempts intentionally malformed SQL would enhance negative coverage but is not required to close **P1-A bounded original-call observation** and is not production guard evidence.
3. Only one reader DELETE permission negative was physically executed per case; it does not prove every INSERT/UPDATE/DELETE permission against every table, nor independently attest arbitrary environments outside the fixed disposable CI. The test-only environment guards and original V2/V3/V6 hashes constrain its claim.
4. The method's **four-table JOIN replay** can still return replay=true when attempt or receipt is missing; such deliberately partial ledgers are **B1-P1-B**, not part of P1-A happy+normal sequential replay. Concurrent races are **B1-P1-C**. UNKNOWN_COMMIT, crash recovery, actual runtime Spring proxies/production transactions, patient/PHI, external delivery and F8 remain untested or unauthorized.

## 4. Formal decision

```text
PR445_EXACT_HEAD_8_FILE_SCOPE = PASS
B1_P1_A_REAL_JAVA8_MYSQL8 = PASS_8_TESTS
B1_P1_A_ARTIFACT_ZIP_CONTENT_REVIEW = PASS_BOUNDED
P1A_R1_01 = CLOSED_TEST_ONLY
P1A_R1_02 = CLOSED_N1_TO_N6
P1A_R1_03 = CLOSED_WITH_SCOPE_LIMIT
P1A_R1_04 = CLOSED_BOUNDED
P1A_R1_05 = RESOLVED_WITH_TEST_TRUST_BOUNDARY
B1_P1_A_TARGETED_SECURITY_EVIDENCE_RE_REVIEW = ACCEPTED_IN_ISOLATED_SCOPE
PR445_EXACT_MAIN_MERGE_READINESS = NEXT_SEPARATE_GATE
PR445_MERGE_AUTHORIZATION = NOT_GRANTED
B1_P1_B_PARTIAL_LEDGER = NOT_AUTHORIZED
B1_P1_C_CONCURRENCY = NOT_AUTHORIZED
B1_P2_AND_B2_PRODUCTION = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next:** `D5-O2 Stage B — B1-P1-A PR #445 Exact-Main Diff / Merge Readiness Assessment`, including refreshed main/PR HEAD/CI/branch protection. This review PR remains Draft; no merge of PR #445 or any design/review PR may occur absent a **separate explicit user authorization**. This review is by the same assistant, not an independent human security signature.
