# U07 D5-O2 Stage B — B1-P1-B PR #451 Exact-HEAD Implementation / Security / Evidence Review R1

Date: 2026-10-12. Technical source/evidence review by the **same assistant**, not separately signed independent human approval. Reviewed the exact implementation Draft PR #451 at **93886b2b746ad36a0d7c572fe521a4526a349ade**, target and unchanged remote main **e7562bd9b5390c85e14c3c01424c1004b1479ebc**. Scope authorization input: Draft review PR #450 at **7c76bcfeae4c42e35d1b2b3fde17fc45fa4fde35**. No implementation change or merge is authorized by this R1.

## 1. Exact diff / CI / original artifact independently rechecked

- PR #451 open, Draft, unmerged; precise main-target diff **seven ADDED files only**, 829 lines added, zero deletions, all exclusively sibling \`stageb/p1b\` Java test/manifest/tools and its dedicated \`.github/workflows/u07-d5-o2-stage-b-p1b.yml\`. No existing B1-P1-A, \`src/main\`, Flyway, application configuration or delivery transport changes. The P1-A path-filter does not include sibling \`p1b\` paths.
- Real [Actions run #38227019373](https://github.com/cxjchelsea/AIdoctor/actions/runs/38227019373): **SUCCESS** Java 8 / disposable MySQL 8; Surefire original **6 partial + 9 conflict = 15 methods passed**, zero failures/errors/skips; machine validator \`STAGE_B_P1B_VERIFIED_CASES=15\` and \`P1B_FOUR_TABLE_SOURCE_GAP=CONFIRMED_AND_NOT_SAFE\`. Same-head Stage A #38227019138 and Stage B P0 #38227019083 also \`success\`, independently of P1-B. An initial older HEAD CI run #38226792783 had five erroneous same-effect fixture assertions; fixed at exact reviewed HEAD; do not represent initial CI as passing.
- Downloaded original Artifact **#11736529377** again and inspected bytes/ZIP CRC/Surefire XML/15 observations. ZIP **SHA-256 \`57fb9259ce3844ef5cd4e21a764bfbd67333f816d78a6fc8d3a6ecebb6c4062a\`**; contents: two original Surefire XML reports (6+9) plus one structured source-characterization JSON bundle. Bundle pins source-main \`e7562bd9b5390c85e14c3c01424c1004b1479ebc\`, PR-head \`93886b2b746ad36a0d7c572fe521a4526a349ade\`, synthetic merge checkout \`8ad26863302bb1d44998eeaf932e13bd53232502\`, run \`38227019373\`, and \`downstreamAuthority=false\`.
- Verified physically reported case outcomes:
  - PARTIAL-01 [0,0,0,0,0,1], 02 [1,0,0,0,0,0], 03 [0,1,0,0,0,0], 04 [0,0,0,0,1,0]: SQL duplicate-key \`23000:1062\`, seeded identity digests unchanged, original successful INSERT counts 5/0/1/4.
  - PARTIAL-05 [1,1,0,1,1,1] and 06 [1,1,1,0,1,1]: **ORIGINAL_REPLAY_TRUE with missing Attempt/Receipt respectively**; both \`sourceGapDetected=true\`, \`fullSixTableReplaySafety=FAIL_NOT_ESTABLISHED\`, zero original new INSERT. This is **a confirmed source flaw, NOT six-table replay safety PASS**.
  - CONFLICT-01..05 typed \`U06_DELIVERY_REPLAY_CONFLICT\` with zero original INSERT, original healthy six-table graph invariant; 06A..D return real MySQL duplicate-key \`23000:1062\`, original INSERT successes 0/1/1/4 and losing distinct-effect six-row vector zero; protected group digest invariant.
- The CI produces only test-runner-origin claims, not an independent signed external DB audit. No physical real crash, unknown commit, production Spring AOP or actual external delivery was tested.

## 2. Implementation / security / evidence findings

| ID | Severity and R1 verdict | Evidence and concern | Targeted action |
| --- | --- | --- | --- |
| **P1B-R1-01** | **BLOCKER — cleanup physical fact gap** | \`StageBP1BEvidence.cleanup()\` correctly performs seeder-only DELETE+commit then freshly reads and asserts an all-zero six-vector. **But** \`StageBP1BPartialLedgerTest\` writes \`fact.put("fixtureCleanupAfter",new int[6])\`, and \`cleanupVerified=true\` is passed as a literal. The exported record is not an actual captured post-cleanup readback; verifier checks only the self-reported constant. It cannot independently verify the physical cleanup fact, or bind it to the same synthetic effect and seeder session. | Return a typed cleanup evidence result from \`cleanup()\` including actual post-cleanup vector/digest, dedicated reader connection ID, seeder connection ID and fixture effect digest. Emit that result (never constant) into each case JSON; require exact six zeros, actual zero-group digest consistent with the same canonical implementation, and unique per-case cleanup identity in Python verifier. Preserve failure-in-finally semantics. |
| **P1B-R1-02** | **BLOCKER — writer/role authority proof incomplete** | \`StageBP1BEvidence.fact()\` exports only \`readerConnectionId\` and \`seederConnectionId\`, not actual writer connection ID, transaction manager/data source identity or observed SERIALIZABLE isolation. \`call()\` checks isolation internally, and \`StageBP1BTestConfig.schemaPreflight()\` checks writer \`CURRENT_USER()\`, but case-level three-role separation cannot be independently assessed from the artifact. More importantly, writer negative DELETE, seeder privilege restrictions and per-case reader denial are not checked by the machine validator. | Physically record role/current user and distinct relevant physical writer, reader and seeder connections plus test-owned isolation and named DS identity. Exercise writer DELETE denial and reader DELETE denial, preferably seeder DDL-denial, with observable SQLState; verifier requires role assertions and cross-role physical identity (not merely supplied role strings). Never broaden grants or log credentials. |
| **P1B-R1-03** | **REQUIRED — unverified cause-to-effect / SQL hook scope** | B-only Hook merely checks SQL prefix \`INSERT INTO U06_DELIVERY_\` and active Spring transaction; it does not assert exact six original ordered SQL signatures/arg positions. The pre-merge artifact proves source tests run and expected INSERT counts but is not an exact SQL trace attestation. Original \`JdbcU06DeliveryStore\` remains unmodified. | Pin exact normalized six original INSERT signatures/order like B1-P1-A, assert affected row = 1, capture ordinal and JDBC resource isolation. Reject unrecognized/extra SQL and verify per-case expected INSERT count and source-error code; do not call Hook a production SQL firewall. |
| **P1B-R1-04** | **REQUIRED — source-gap vs business gate robustness** | Python verifier **correctly requires** both PARTIAL-05/06 source-gap true and \`FAIL_NOT_ESTABLISHED\`; bundle correctly keeps overall \`fullSixTableReplaySafe=false\`. Yet graph digests are test-origin hashes, checked mostly for syntax/equality, and \`fullSixTableReplaySafe\` string is asserted for all PARTIAL cases by tests. Good for source characterization, but not for claiming complete authority. | Keep explicit two-plane \`SOURCE_CHARACTERIZATION_PASS\` vs \`BUSINESS_REPLAY_SAFETY_FAIL\`; emit witnessed *full-six predicate false* with per-table orphan/identity facts for partial states, and fail validator if it is promoted or omitted. Do not mark the Stage B delivery process production-safe. |
| **P1B-R1-05** | **ADVISORY — negative tests / case identity** | Exact 15 JUnit class/method names, JSON count, row-count vectors, SQLState 23000:1062 and exact PR-head/run binding are tested by verifier. Conflict 01..05 intentionally share the healthy effect, while 06A..D use distinct losing effects; current HEAD correctly handles this. Consider test-mutation probes (missing row, wrong digest, invalid writer permission, changed SQL) to test the verifier's own fail-closed behavior. | Add bounded verifier self-negative fixtures or isolated verifier tests within existing seven files if compatible; never generate positive facts in Python. |

## 3. Bounded security and provenance notes

- P1-B uses fresh \`u07_d5o2_stage_b_p1b\` MySQL8 schema, port 33329 and a distinct workflow. Credentials are generated with \`openssl rand -hex 24\` in runner and masked before being passed to role-specific connections. Root used only to bootstrap DB. SQL setup is written to /tmp and removed after import; no P1-A original schema/role/manifest modification is in the diff.
- \`writer\` grant = SELECT/INSERT only; \`reader\` grant = SELECT only; \`seeder\` grant = SELECT/INSERT/DELETE only on six tables. Physical role grants were enacted during passing run and read-only DELETE was exercised in tests. **However** least privilege does not become fully attested until per-case role/permission negatives and actual connection provenance are captured and checked.
- Test seeder creates intentionally incomplete committed records. Such states are synthetic preconditions and **not** evidence that a single six-INSERT atomic transaction can normally commit partially.
- Partial snapshot function supports 0..N rows and hashes sorted complete per-row columns, unlike P1-A \`graphDigest()\` which demands all six; six-table full predicate is separately checked only for healthy reference groups. Scope remains characterization, not source repair.

## 4. Review decision and next work

\`\`\`text
PR451_EXACT_MAIN_SEVEN_ADDED_TEST_ONLY_FILES = PASS
PR451_REAL_JAVA8_MYSQL8_EXACT_HEAD = PASS_15
PR451_ORIGINAL_ARTIFACT_INDEPENDENT_CHECK = PASS_BOUNDED
P1B_FOUR_TABLE_SOURCE_GAP = PHYSICALLY_CONFIRMED_NOT_SAFE
P1B_R1_01 = BLOCKER
P1B_R1_02 = BLOCKER
P1B_R1_03 = REQUIRED
P1B_R1_04 = REQUIRED
P1B_R1_05 = ADVISORY
B1_P1_B_IMPLEMENTATION_SECURITY_EVIDENCE_REVIEW = NOT_CLOSED
PR451_MERGE_READINESS = NOT_READY
PR451_MERGE_AUTHORIZATION = NOT_GRANTED
P1_C_CONCURRENCY = NOT_AUTHORIZED
B2_PRODUCTION_SPRING_WIRING = NOT_AUTHORIZED
REAL_D5 = NOT_READY
\`\`\`

**Next:** \`D5-O2 Stage B — B1-P1-B PR #451 R1-01..04 Targeted Evidence / Role / SQL-Trace Remediation\`, strictly within the **same seven already-authorized test/CI/manifest files**; any extra file or production change requires fresh scope authorization. Rerun Java8/MySQL8 15-case real suite on **new exact HEAD**, independently download & inspect fresh artifact, then perform separate targeted R2 security/evidence re-review before any exact-main merge-readiness request. **No PR merge, production source repair, patient data, F8 resume or live delivery** permitted by this review.
