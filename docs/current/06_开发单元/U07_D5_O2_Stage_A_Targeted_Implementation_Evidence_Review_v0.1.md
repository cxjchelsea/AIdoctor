# U07 D5-O2 Stage A — Targeted Implementation / Evidence Review v0.1

2026-10-12. Exact main `435c456e7bf43601f0445f82932c3584fa1b8170`, PR #426 head `62ed39d4614057cb6fa44123cb54db7b36b19a0a`; reviewed six files by exact-ref source read, not inferred from PR description. GitHub Actions run `38205167651`, job `114666934773` = SUCCESS; Surefire artifact `11726828365`. Job log confirms 3 tests / 0 fail / 0 errors / 0 skipped: `U07D5O2StageATopologyTest` (2) and `U07D5O2StageATxPhysicalTest` (1), with Java8 `test-compile` success and disposable MySQL8 bootstrap success. This is **valid targeted execution evidence**, not verification closure for every planned A0/A1/A2 test case, original U06 production Bean topology, or multi-owner atomicity.

## 1. Accepted verified scope

- `U07D5O2StageAFixtureConfig` configures test-only Hikari fixed localhost:33326 schema, single `JpaTransactionManager`, shared `JdbcTemplate` and `TransactionTemplate`; no production `src/main` changes.
- `U07D5O2StageATopologyTest` proves the actual *candidate test context* starts with exactly four JPA managed entity types, four Spring Data repository beans, one transaction manager and one EMF. It demonstrates final status/lack of `@Service` annotations on three original services; it does **not** inspect a production Bean graph.
- `U07D5O2StageATxPhysicalTest` exercises one `RuntimeThreadStateRecord` `saveAndFlush` inside an explicit outer transaction. In-transaction JDBC sees it; separate thread/JDBC connection does not see the uncommitted row; rollback leaves it absent; commit makes it visible and cleanup removes it. This is persuasive physical evidence for **that single table, candidate manager, candidate schema**.
- `bootstrap-stage-a-schema.py` checks the original V2/V3/V6 SQL Git blob hashes and no network is used inside the script. This is `OWNER_SCHEMA_SLICE`, not V1→V6 Flyway compatibility.
- Explicit scoped CI job uses MySQL8, Java8, compile, targeted tests, checked nonzero Surefire test totals; all steps succeeded. Exact PR six-file inventory verified.

## 2. Findings

### IA-01 HIGH / REQUIRED BEFORE CLOSURE — Required two-owner atomicity not tested

The physical test writes only `clinical_runtime_thread_state`. Despite the design agreement, there is **no** exercise of `ConsultationWaitTransitionService.establish()` involving consultation + wait effect, no `RuntimeWaitCheckpointService.reserve()` involving checkpoint + thread, and no injected failure between each pair followed by independent readback. No `WAIT_CHECKPOINTED` staging failure/readback or concurrency conflict. The current tests cannot close D5-O2 Stage A physical multi-owner transaction contract. Add source-grounded multi-row fault-injection tests with explicit `TransactionTemplate`, separately report valid stage boundary vs atomic sub-operation, never assert unproven production annotation advice.

### IA-02 HIGH / REQUIRED BEFORE CLOSURE — Negative controls/manager mismatch unexercised

`originalFinalServicesAreNotImplicitlyAdvised()` merely checks `final` and missing `@Service`; it doesn't prove attempted class proxy failure, original service unadvised execution, JDK-interface advice distinction or unexpected multiple manager. Candidate `PlatformTransactionManager` existence and `TransactionSynchronizationManager.hasResource(ds)` do not prove the original final annotated method is actually intercepted. The suite has no wrong DS/EMF/manager negative control, so a broken candidate wiring variant isn't demonstrably rejected. Add explicit negative cases that prevent success classification for wrong manager / split datasource and asserted exception or unadvised branch.

### IA-03 HIGH / REQUIRED BEFORE CLOSURE — Independent observer not least-privilege / physical identity unrecorded

`separatelyCommittedCount(ds, key)` runs on another thread, so it does not inherit Spring ThreadLocal writer resources and does validate independent connection visibility in normal pool operation. However it obtains connections from **the same writer HikariDataSource** under `stagea_writer`, not the deliberately created `stagea_reader` SELECT-only principal. Its observer result is `COUNT` only; no explicit `CONNECTION_ID()` proof that writer and observer differ, manager/DataSource/EMF bound identity manifest, or transport provenance. To meet the strict observation/least-privilege contract, introduce a distinct reader DataSource/credentials, assert distinct MySQL physical connection IDs while writer is live, and record sanitized topology evidence. No credentials in artifacts/logs.

### IA-04 MEDIUM / REQUIRED BEFORE CLOSURE — Environment/provenance evidence is incomplete

The bootstrap does validate V2/V3/V6 Git blobs and fixed manifest path/db/port/profile, but `stage-a-manifest.json`'s declared four entities, excluded migration list, authority and production flags are **not** machine-validated by the bootstrap. `source_main_sha` is a frozen baseline constant rather than verified checkout ancestry; the GitHub Action prints the actual `git rev-parse HEAD` but does not persist a typed evidence result connecting checkout SHA, original blob digests, backend and exact assertions. Strict source-vs-candidate-vs-physical evidence taxonomy from #424 is only documented, not enforced at runtime. Missing evidence or invalid cross-plane fields must fail closed.

### IA-05 MEDIUM / REQUIRED SECURITY HARDENING — Disposable-only control stronger in CI than Java runtime

The CI job creates ephemeral MySQL8 and fixed local port, so the observed successful run is safely scoped. The Java `requireEnvironment` validates three environment *strings* and constructs a fixed localhost URL, but does not itself attest to MySQL server instance, exact schema privilege, SELECT-only observer, or server DB version/InnoDB; a pre-existing local instance at the expected endpoint with known static test credentials could be affected if tests run outside CI. Bootstrap emits `GRANT SELECT, INSERT, UPDATE, DELETE` to the writer for every available OWNER_SCHEMA_SLICE table (broader than the single-table initial test). Require server-side schema, account grants and InnoDB verification before mutation, disposable guard/canary, narrower table roles where possible. Keep scope non-production. This does not negate CI results.

### IA-06 LOW / REVIEW HYGIENE — Workflow supply-chain and artifact limitations

Actions `checkout@v4` / `setup-java@v4` / `upload-artifact@v4` use tags, not immutable commit SHAs (unlike some existing workflows). Artifact includes JUnit XML, not a signed provenance manifest; job logs expose debug classpath details but not a separate normalized transaction fact schema. Tighten before closure. No claim that artifacts were independently downloaded and rehashed in this review.

## 3. Test matrix mapping

| Test / gate | Actual evidence | Verdict |
| --- | --- | --- |
| A0 static final and missing Service | Reflection, one test | PASS in source baseline scope |
| A1 four entity/four repository, 1 manager+EMF | Actual Spring candidate context | PASS in test-wiring scope |
| A1 outer Template, JPA+JDBC shared transaction, one thread row | MySQL8 independent-thread observation | PASS for one table |
| A1 wrong resource/manager and actual final-class advice | Not exercised | NOT_VERIFIED |
| A2 Consultation+WaitEffect failure-window rollback | Not exercised | NOT_VERIFIED |
| A2 Checkpoint+Thread failure-window rollback | Not exercised | NOT_VERIFIED |
| A2 WAIT_CHECKPOINTED staged commit/second phase failure | Not exercised | NOT_VERIFIED |
| A2 real 2-connection concurrency/locking and unknown commit | Not exercised | NOT_VERIFIED |
| SELECT-only separate observer account/physical identity | Not exercised | NOT_VERIFIED |
| Strict runtime evidence taxonomy and checkout binding | Not implemented | NOT_VERIFIED |
| Original production Spring topology / Oracle / Flyway full chain | Out of scope | NOT_VERIFIED |

## 4. Decision and bounded next step

`PR426_IMPLEMENTATION_SCOPE=ACCEPTED_AS_TEST_ONLY_CANDIDATE`;
`PR426_TARGETED_CI=PASS_3_TESTS`;
`PR426_IMPLEMENTATION_VERIFICATION_CLOSURE=NOT_READY`;
`PR426_MERGE_AUTHORIZATION=NOT_AUTHORIZED`;
`STAGE_A_NEXT=TARGETED_SECURITY_EVIDENCE_AND_COVERAGE_REMEDIATION`;
`REAL_D5=NOT_READY`, `BUSINESS_AUTHORITY=UNKNOWN`.

The accepted three tests must not be dismissed: they establish an important physical baseline. Equally, three green tests cannot be counted as 10+ unimplemented fault/negative cases. Recommended minimal patch sequence: (1) IA-03 + IA-05 observer segregation + fail-closed environment evidence, (2) IA-01 multi-row real JPA rollback/staging cases, (3) IA-02 wrong-manager/DS negative tests, (4) IA-04 typed provenance and IA-06 workflow/artifacts. Keep changes under isolated test/fixture/bootstrap/workflow, no production Java, no merge, then re-run Java8 + MySQL8 and conduct new targeted review. 

**Reviewer independence:** performed by same assistant as implementation, so "targeted review" is accurate; NOT an independent human/separate-reviewer signoff.
