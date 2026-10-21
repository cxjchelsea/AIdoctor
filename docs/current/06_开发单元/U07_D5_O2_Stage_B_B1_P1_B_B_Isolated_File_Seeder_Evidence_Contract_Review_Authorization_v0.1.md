# U07 D5-O2 Stage B — B1-P1-B B-ISOLATED Targeted File / Seeder / Evidence Contract Review and Scoped Implementation Authorization v0.1

Date: 2026-10-12. Source: PR #449 exact HEAD \`7fbef601b6a78bda57eec2f66d001e77a21cdb8d\` (Draft, one design document); merged main \`e7562bd9b5390c85e14c3c01424c1004b1479ebc\` (contains #445 B1-P1-A). The review was performed by the same assistant examining source and repository facts, **not an independent human reviewer signoff**. No B1-P1-B implementation, Java8 compilation, physical MySQL8 test or CI result is claimed by this document. This is a **scoped test-only design authorization decision**, not a GitHub PR merge authorization.

## 1. Review summary and choice

The B-ISOLATED direction is accepted. Reject B-SHARED for this slice: merged B1-P1-A's eight files, P1-A manifest/sourceMainSha, R2 evidence and Java8/MySQL8 CI must remain unchanged. Review of current \`.github/workflows/u07-d5-o2-stage-b-p1.yml\` finds its path filters include \`diagnosis-service/src/test/**/stageb/p1/**\`, \`test/resources/.../p1/**\` and \`tools/.../stageb/p1/**\`; merely adding B files under those directories would trigger its existing path filters. Therefore isolate B in **sibling \`stageb/p1b/\`**, a new independent script subdirectory, new dedicated CI workflow and **separate disposable schema/port**. The earlier ten-file full-P1 inventory was a pre-merge design ceiling, not authority to mutate accepted P1-A. This document is the *controlled scope amendment for independent B-only files*.

**Decision: AUTHORIZE_LIMITED_TEST_ONLY_IMPLEMENTATION** for exactly the seven *ADDED* paths in §2, implementing the 6 partial-ledger source characterization cases and 9 sequential replay/conflict cases with bounded MySQL8 CI and fail-closed evidence. This authorizes creation of a new **Draft implementation PR based on the exact named main**, not merge, production Store editing, P1-C racing, B2 wiring or external delivery. Execution gates and residual risks are explicit below. Deviating from those seven file paths, changing DDL credentials, production code, existing A files, or runner layout needs another reviewed amendment.

## 2. Frozen seven-file isolated inventory — additions only

1. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BTestConfig.java\`
2. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BPartialLedgerTest.java\`
3. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BReplayConflictTest.java\`
4. \`diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BEvidence.java\` (may contain package-private fixture seeder, canonical reader and SQL trace helper classes)
5. \`diagnosis-service/src/test/resources/u07/d5o2/stageb/p1b/manifest.json\`
6. \`tools/u07_d5_o2_tx_topology/stageb/p1b/bootstrap-and-verify.py\` (two explicit CLI modes: \`bootstrap\` prints pinned SQL; \`verify\` consumes real Surefire+JSON; **no new files**)
7. \`.github/workflows/u07-d5-o2-stage-b-p1b.yml\`

All seven additions exclusively test/fixture/CI. No modifications to main source, Flyway, \`pom.xml\`, app profiles, P1-A's eight original files or its CI. Two test classes have **15 uniquely named @Test methods** (6 \`partial01..06\`; 9 \`conflict01..05\` and \`conflict06A..D\`); a shared evidence class supplies all helpers. If JUnit compilation or scope demands a further file, stop, amend the frozen inventory and re-review before writing it. No implicit \`StageBP1ConcurrencyTest.java\` authorization.

Because Java8/JUnit5 + Maven Surefire method representation is not necessarily the human display name, the exact manifest **must** bind each class and real JUnit testcase name 1:1 to canonical caseId and one physical JSON fact. The machine verifier must fail on missing/duplicate/extra/skip/failure or classname mismatch; do not count a parameterized parent once for four distinct conflicts.

## 3. New B-only disposable MySQL8 and three-role authority

- B-only database \`u07_d5o2_stage_b_p1b\` on **127.0.0.1:33329**, environment \`U07_D5O2_STAGE_B_P1B_SYNTHETIC\`; use a new per-job ephemeral mysql:8.0 service container in GitHub Actions. **Never reuse P1-A port 33328, database u07_d5o2_stage_b_p1, or P1-A role names.**
- Load precisely original MySQL DDL V2/V3/V6 pinned by exact Git blob SHA (V2 \`73029495c5f46e8576e34f8c9a77644fb8c1c4c4\`, V3 \`ccc1aa30b8e9405bd07249bae1d3531853f7510b\`, V6 \`7ed5a82bfba3b3fed2666bee969a7fe276e67566\`). Their original name, contents, engine/InnoDB physical checks and per-six-table schema are verified; no production migration/Flyway update or extra constraints. CI root/bootstrap account is available **only** to initialize the ephemeral DB and mint the three restricted roles.
- \`stagebp1b_writer\`: **SELECT + INSERT** against only the six pinned tables. Needed to exercise original six INSERTs and reads, and insufficient to seed/clean arbitrary partial cases via DELETE or UPDATE; no CREATE/ALTER/DROP/GRANT/global privileges.
- \`stagebp1b_reader\`: **SELECT only** on those six tables. Verify CURRENT_USER() and schema on physical connection; negative **zero-row DELETE** (expected privilege denied) and optionally INSERT/UPDATE negations. Reader must be a separate physical session from writer and seeder, and new session after each tx.
- \`stagebp1b_seeder\`: **SELECT + INSERT + DELETE** scoped to only six tables, no UPDATE/DDL/global roles, for synthetic deliberately corrupt setup and matching fixture cleanup **only**. Use a separate connection and explicit commit for setup, *never* borrow writer's DataSource or transaction.
- Role secrets are disposable generated/local job values passed via CI job env/secret inputs; **do not bake literal passwords into tracked Java or Python or ship them to Artifact**. Never allow user-supplied production JDBC URL. Fail closed unless CI identity, exact localhost+port+schema+profile and role grants match; do not print secret values. If credentials must be passed to bootstrap, consume environment variables and ensure logs, exceptions, artifacts and Java test system properties redact credentials. The GitHub job may set fixed *obviously synthetic* disposable credentials in the workflow only if threat review specifically approves, but prefer ephemeral masked per-job random values.
- Seeder duties are **not ordinary business workflow actions**. Artificial five-row/one-row fixtures cannot be represented as normal crash leftovers from a healthy single local DB transaction. No F8 authorization or production delivery implied.

## 4. Exact source behavior / test cases

The unchanged \`JdbcU06DeliveryStore.find()\` does a four-way INNER JOIN (ledger, authority, intent, confirmation), excluding attempt and receipt, and \`Existing.matches()\` checks a fixed subset of command fields. Actual V6 schema keys:
\`authority\` PK selection_effect_id / UNIQUE delivery_effect_id;
\`intent\` PK effect / UNIQUE delivery_id and idempotency_key;
\`attempt\` PK attempt_id ONLY;
\`receipt\` PK receipt_id ONLY;
\`confirmation\` PK confirmation_evaluation_id ONLY;
\`ledger\` PK delivery_effect_id.
Do not infer other unique constraints.

### Partial-ledger six cases

P1-PARTIAL-01 ledger only; P1-PARTIAL-02 authority only; P1-PARTIAL-03 intent only; P1-PARTIAL-04 confirmation only; P1-PARTIAL-05 matching authoritative quartet + receipt, **without attempt**; P1-PARTIAL-06 matching quartet + attempt, **without receipt**. For 05/06, the seeded row count is exactly five (missing only the singled-out auxiliary row). For 01..04 the four-table JOIN misses the incomplete group, and insertion normally encounters an existing PK or UNIQUE constraint; anticipated failing ordinal is not asserted as a physical fact until observed. Each test seeds with the dedicated seeder account, commits seed, closes seeder transaction, measures six-table vector & digest with reader, then invokes **unchanged production-source Store** using caller-owned explicit SERIALIZABLE TransactionTemplate with the separate writer account. After return/throw, new independent reader verifies full all-row digest equality (no mutation), no hidden committed writes, and no unexpected orphan records.

For 05/06 the four-way JOIN may return \`replay=true\` even though one required auxiliary row is absent. Record original \`ORIGINAL_REPLAY_TRUE\`, \`SOURCE_GAP_CONFIRMED\`, and \`FULL_SIX_TABLE_REPLAY_SAFE = FAIL_NOT_ESTABLISHED\` **simultaneously**. These characterization tests can PASS as *detectors* of the source gap while the business safety claim MUST remain NOT_PROVEN. The CI success status describes 15 source-characterization tests, never complete business replay safety. For 01..04 do not upgrade a SQL exception to replay, and require the seed and an unrelated fully-committed protected healthy group to be unchanged. Seeder cleanup must execute in finally and verify zero records for fixture-specific effects; any failed cleanup fails the test even if the Store outcome was as predicted.

### Nine conflict cases

After a complete committed healthy group using Store, separately issue (sequential only) P1-CONFLICT-01 same-effect changed contentFingerprint, 02 endpointRef, 03 confirmationFingerprint, 04 deliveryId, 05 idempotencyKey: all require an explicit \`U06_DELIVERY_REPLAY_CONFLICT\` typed error and preserved healthy all-row digest. P1-CONFLICT-06A new effect reuses selectionEffectId (authority PK), 06B new effect reuses deliveryId (intent UNIQUE), 06C new effect reuses idempotencyKey (intent UNIQUE), 06D new effect reuses confirmationEvaluationId (confirmation PK). Every distinct-effect candidate must have a zero-after six-table vector and original committed healthy digest invariant, with original SQLState/vendor code recorded. Database constraint exceptions are not a replay result. The tests do not introduce a new UNIQUE index absent from the original DDL. No concurrency worker is authorized.

## 5. Partial-aware canonical state, source classification and evidence

Merged P1-A \`StageBP1Evidence.graphDigest()\` first calls \`complete()\` and therefore **cannot** encode partial groups. B1-P1-B must define in its own helper a new **PARTIAL_AWARE_SIX_TABLE_GRAPH_V1** with:
- six tables in fixed authority/intent/attempt/receipt/confirmation/ledger order, each exact original schema table name and all rows queried by \`delivery_effect_id\` (0..N), not \`queryForObject\`;
- each row projected as all actual DB column names and values, sorted by column name and then by deterministic encoded row bytes; prefix exact table name, row count, typed/null representation, field lengths, and version/domain separator; hash SHA-256 over canonical UTF-8 bytes;
- **return a stable digest even for six zero-row tables, five-row gap, or more-than-one-row anomalous auxiliary table**. Row count vector and digest must both be emitted; a digest shape alone cannot certify an actual read.
- require per-case artificial fixture seed-before/store-after/cleanup-after vectors, digests and connection identity; assert fixture-before and fixture-after digest equality; a protected normal complete group digest must be invariant as a positive control.
- on originally incomplete groups, full-six coherence predicate must return **false**, not throw because one table missing. The separate complete-group predicate verifies effect/delivery/selection/evaluation associations, fingerprints/statuses and exactly one row per table; four-table JOIN result alone is insufficient.

Each fact binds \`sourceMainSha=e7562bd9b5390c85e14c3c01424c1004b1479ebc\`, original immutable DDL Git blobs, **actual PR branch HEAD** separately from synthetic merge checkout HEAD (PR HEAD = checkout merge commit's second parent), workflowRunId, exact JUnit class/method, caseId and variantId, physical schema/role/reader+writer+seeder connection IDs, method outcome (including SQLException class, SQLState and vendor code), transaction result, six vectors+digests, original Store INSERT count, cleanup verified, \`sourceGapDetected\`, \`testVerdict\`, \`fullSixTableReplaySafety\`, \`downstreamAuthority=false\`. For no external audit requirement, test-emitted physical results and raw Surefire XML are the trust boundary; Python validation cannot manufacture them.

Verifier must require precisely **15 real executed tests**, no skip, no duplicate/missing case/fact, no logs holding credentials, no wrong SHA/baseline/role/schema/engine, no leaked fixture, no unwound transaction resources. For PARTIAL-05/06 require both SOURCE_GAP_CONFIRMED and safety FAIL_NOT_ESTABLISHED as **mandatory**; any assertion that incomplete group replay is safe must fail closed. For a source surprise record UNEXPECTED_SOURCE_BEHAVIOR and fail case acceptance rather than force a guessed error ordinal. A JUnit method PASS only means the declared expected *source characterization* was actually and correctly observed.

## 6. CI / security / provenance execution requirements

A new dedicated \`.github/workflows/u07-d5-o2-stage-b-p1b.yml\` triggers only on \`.../stageb/p1b/**\`, \`.../stageb/p1b/manifest.json\`, \`tools/.../stageb/p1b/**\`, and its own workflow. No P1-A file modifications and no path-filter overlap. New MySQL8 service binds loopback port 33329 with schema exclusively B, Java8 real Maven compile and explicit JUnit test class whitelist, per-case test-origin observation JSON, exact SHA-checking Python verifier, upload all original Surefire XML and the final bundle even on failure where safe. Limit runner/job time, DB transaction timeout, and connection pool size; all seeds and fixture lifecycle are finite. Fail closed on incomplete cleanup, source behavior mismatch or privilege negation failure. The P1-A suite need not run on B-only changes if its unchanged files are provably unaffected; a separate **P1-A compatibility audit** checks no modifications, and no B candidate code imports/mutates P1-A fixture or evidence.

The \`bootstrap-and-verify.py\` in §2 has exactly two explicit, mutually exclusive modes: \`bootstrap\` verifies manifest, hashes DDL and produces only B-schema setup SQL (the runner performs DB connection), \`verify\` reads original Surefire + test-emitted observation JSON. Credential generation and masking occur only in Actions bash/env, not in emitted SQL beyond statements executed against disposable MySQL; redact any generated SQL file during artifact upload. Avoid globs that could leak unrelated P1-A reports.

## 7. Disposition of findings and authorization

| Finding from PR #449 | Resolution | Decision |
| --- | --- | --- |
| B1B-G01 six partial states / source gap | six exact state vectors + explicit source-vs-business verdict and cleanup | **CLOSED_DESIGN_ONLY** |
| B1B-G02 conflict cases / uniqueness | 9 exact separate JUnit methods and original DDL key mapping | **CLOSED_DESIGN_ONLY** |
| B1B-G03 seeder / writer / reader boundary | 3 distinct B-only least-privilege roles, setup+cleanup transaction, separate physical reader, CI-generated disposable credentials | **CLOSED_CONTRACT; PHYSICAL_PROOF_PENDING** |
| B1B-G04 file/CI ownership | B-ISOLATED sibling p1b tree, exactly 7 added files, new DB/port/profile/workflow; no P1-A changes | **CLOSED_DESIGN_ONLY** |
| B1B-G05 partial-aware all-rows graph | versioned, deterministic 0..N-row canonical digest and complete predicate split | **CLOSED_CONTRACT; TEST_PROOF_PENDING** |
| B1B-G06 provenance & fail-closed CI | separate B workflow and manifest, strict 15 case facts, dual-head provenance, protected positive control, forced source-gap flag | **CLOSED_CONTRACT; CI_PROOF_PENDING** |

**Implementation authorization**: \`AUTHORIZE_LIMITED_TEST_ONLY_IMPLEMENTATION\` for **exactly seven ADDED files in §2** and exact source-characterization scope. Conditional *implementation acceptance*, not authorization precondition: Java8 test compilation, real disposable MySQL8 15-case execution, independent Artifact verification, security / evidence re-review and exact main diff review. If role separation, full 15-case evidence, schema/port isolation or source-gap fail-closed design proves unimplementable within frozen seven files, **STOP and return for targeted amendment**, do not modify P1-A or production.

\`\`\`text
PR449_EXACT_HEAD_DESIGN_INPUT = 7fbef601b6a78bda57eec2f66d001e77a21cdb8d
BASE_MAIN = e7562bd9b5390c85e14c3c01424c1004b1479ebc
B1P1B_DESIGN_SCOPE = B_ISOLATED_SEVEN_NEW_TEST_ONLY_FILES
B1P1B_G01_G02_G04 = CLOSED_DESIGN_ONLY
B1P1B_G03_G05_G06 = CLOSED_CONTRACT_PHYSICAL_EVIDENCE_PENDING
B1P1B_SCOPED_IMPLEMENTATION_AUTHORIZATION = AUTHORIZE_LIMITED_TEST_ONLY
B1P1B_PHYSICAL_JAVA8_MYSQL8_TESTS = NOT_EXECUTED
B1P1B_SOURCE_GAP_REPLAY_SAFETY = NOT_ESTABLISHED
B1P1B_MERGE_AUTHORIZATION = NOT_GRANTED
B1P1C_CONCURRENCY = NOT_AUTHORIZED
B1P2_B2_PRODUCTION = NOT_AUTHORIZED
REAL_D5 = NOT_READY
\`\`\`

**Next permitted task:** \`D5-O2 Stage B — B1-P1-B B-ISOLATED Partial Ledger + Replay Conflict Implementation\` using a *new Draft PR* based on exact main, only seven new files and the scope/CI conditions above. This review document is Draft/unmerged and itself does not execute code or authorize a PR merge. All real patient, external messaging and production U06 changes remain prohibited.
