# U07 D5-O2 — Separate Exact-Main Physical Runner: Six-File Scope / Provenance / Fail-Closed Detailed Design v0.1

> STATUS: design-only candidate, independently review before implementation. Target immutable source main `33c0914e843d7976c7ff2272338377aa4925f443`. PR #463 already merged; PR #465 remains Draft. No authority to run code on production, merge, change existing PR-only workflow, or implement Whole-TX Retry.

## 1. Purpose, evidence boundary and key incompatibility

The existing `.github/workflows/u07-d5-o2-stage-b-forensics.yml` is restricted to `pull_request`, checks `HEAD^2 == U07_F_PR_HEAD_SHA`, and uses `github.event.pull_request.head.sha`. Existing test harness Java and `bootstrap-and-verify.py verify` expect the second parent of a synthetic PR merge. A genuine main commit has no PR-head identity and must never be relabelled as one. Reusing the old entrypoint directly, or blindly adding `push:`/`workflow_dispatch:`, **cannot establish a main-bound witness**.

Define distinct workflow/job `main-exact-f-physical` and distinct evidence `U07_F_MAIN_L0_EVIDENCE_V1`. The original PR-only workflow and original test evidence schema remain unchanged.

## 2. Proposed exact implementation file inventory (NOT authorized)

A limited ADD-only candidate in a *separate implementation PR*:

1. `.github/workflows/u07-d5-o2-main-exact-physical.yml` — event, immutable commit checkout and security/negative CI wiring
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/mainexact/MainExactForensicsTest.java` — Java8 original Store witness and six-table readback
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/mainexact/MainExactEvidence.java` — main-specific positive allowlist schema, provenance, observer L0 and redaction
4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/mainexact/MainExactTestConfig.java` — disposable JDBC roles, scoped transaction owner, timeout/resource cleanup
5. `diagnosis-service/src/test/resources/u07/d5o2/stageb/mainexact/manifest.json` — pinned source SHA and DDL blob manifest with versioned role grant restrictions
6. `tools/u07_d5_o2_tx_topology/stageb/mainexact/bootstrap-and-verify.py` — isolated schema bootstrap, main-specific evidence/schema/provenance verification, negative safe-upload, no PR-head linkage

This six-file ADD-only inventory is a *maximum*, not automatic authority. No POM edits, no production Store/migrations, no privileges beyond L0, no existing PR-only forensics file edits, no workflow edits outside new file, no agents, retry coordinator, or B2. Any dependency requiring more files is a **scope escalation requiring a fresh design decision**.

## 3. Immutable source binding and trustworthy execution

- `EXPECTED_SOURCE_MAIN_SHA=33c0914e843d7976c7ff2272338377aa4925f443` is hardcoded in the new workflow and manifest; not provided by untrusted PR comments/inputs, branch names or checkout expressions.
- Trigger candidate: `workflow_dispatch` *on default-branch workflow definition only*, without arbitrary ref / workflow inputs. Since CI workflow code cannot be dispatched before merge and GitHub workflow dispatch normally executes the requested workflow ref, a **separate merged test-only workflow authorization** is required before any dispatch on main. Alternatively an owner-provisioned isolated runner may execute a **content-pinned** workflow copy, with separately reviewed attestation. No claim of main execution from a PR synthetic checkout.
- Checkout `ref: EXPECTED_SOURCE_MAIN_SHA` via allowlisted and pinned `actions/checkout`, `persist-credentials: false`, fetch depth sufficient for commit inspection. Enforce `git rev-parse HEAD == EXPECTED_SOURCE_MAIN_SHA` and `git cat-file -t HEAD == commit`; manifest DDL blobs immutable and identical to the target main tree.
- **Important paradox**: a workflow/code helper added by a *later* PR cannot exist within a checkout of earlier immutable main `33c0914e`. Resolve with **two distinct trees**: control-plane runner from a later reviewed commit (with its own `runnerControlSha`) and source-under-test checkout of exact `33c0914e` into a separate clean, read-only directory. Never run untrusted scripts from source checkout unless separately pinned; never attribute runner SHA to the target main SHA.
- Workflow event may carry new default main SHA while target source SHA stays frozen. Evidence must carry `runnerControlSha`, `sourceUnderTestSha`, `workflowRunId`, `runAttempt`, and `eventType`, with independent equality checks and explicit nonidentity.
- If `main` moved, DO NOT quietly reinterpret sourceUnderTest as the new main. Issue evidence for historical target `33c0914e` only; require a new exact-main gate to claim latest main.

## 4. Exact security profile

- Dedicated disposable MySQL8 (pinned major 8, image digest frozen before implementation), loopback-only port and schema distinct from existing PR F-ISOLATED and P1B.
- Ephemeral secrets generated on runner; only session-scoped reader/writer grants; observer restricted to exact `performance_schema.data_locks`, `data_lock_waits`, `threads` table SELECT. No PROCESS, no SHOW ENGINE INNODB STATUS, no broad `performance_schema.*`, no raw LOCK_DATA.
- Single-use synthetic identities; no actual patient data or public notification; original `JdbcU06DeliveryStore` source unchanged.
- Java 8 compile verified; timed two independent writer transactions and fresh separate Reader; 1213/40001 victim and winner six-table rows; optional L0 observation edges never promoted to exact root cause unless same-incident, same-epoch, both physical writer connections/threads match.

## 5. Evidence types and admission

`MainSourceProvenance` fields (all required, strict allowlist): `schemaVersion="U07_F_MAIN_L0_EVIDENCE_V1"`, `targetSourceSha`, `checkoutVerifiedSha`, `runnerControlSha`, `runnerWorkflowPath`, `eventType`, `workflowRunId`, `runAttempt`, `schemaDigest`, `mysqlVersion`, `javaVersion`, `executionMode="MAIN_EXACT"`, `l1Enabled=false`, `productionAuthority=false`.

`MainIncident`: immutable synthetic `caseId`; writer actor result and SQLSTATE/code, independent physical connection identities and start/end times; reader connection ID and six-vector; redacted observer L0 counters, selected whitelisted edge records, `NO_EDGE_CAPTURED | PARTIAL_EDGE_ONLY | LIVE_TWO_WRITER_CYCLE_BOUND` verdict; original source first attempt remains source-only and **may FAIL**. Do not fabricate success or retry.

`MainNegativeEvidence`: deny grants and redaction negative matrix, guard contract outcomes, executed failure-injection outcomes, plus `uploadSkippedOnFailure=true` only on a genuine skipped upload job step.

An *independent* Python verifier (not the PR-head one) validates the three JSON evidence files, mutually consistent `targetSourceSha`/`runnerControlSha`/runId/case identity, allowlists, file names, max bytes, symlink/path traversal rejection, SHA256, and mismatch/tamper mutants. Insecure or incomplete result means **no upload**. No secret/raw SQL/stack trace/LOCK_DATA is permitted in uploaded JSON or public console.

## 6. CI profile and failure injection

- Two separate jobs: physical main source witness; independent negative fail-injection with deliberately failing shell command that writes no sensitive command literals into public workflow logs. `if: always()` admission gate must genuinely fail closed and uploaded artifact step must report `skipped`. Keep exact evidence directory absent in the negative job.
- For physical job, bind checks to `sourceUnderTestSha == EXPECTED_SOURCE_MAIN_SHA` independently of `GITHUB_SHA`. A successful Maven test compile alone is not admission: check JUnit suite result, exact test name/count, MySQL versions, all-six reader identity and least-privilege negatives.
- Security controls must ensure artifacts are only persisted after admission on success, with private Maven log deletion and explicit boolean gating; protect even if Maven fails early, artifact missing, runner canceled or compromised data names.
- No release, merge, production, L1 or WHOLE_TX_RETRY authority results from this run.

## 7. Explicit review gates

| Gate | Pass evidence |
|---|---|
| MR-G1 Scope | Exact six ADD-only files, no production / old PR runner edits |
| MR-G2 Runner ownership | Reviewed default-branch control plane, no arbitrary ref/event permissions, pinned runnerControlSha |
| MR-G3 Source identity | Independent checkout exact `33c0914e` in dedicated directory; compare `git rev-parse HEAD`, tree blob manifest, event/target bound |
| MR-G4 Physical | Real Java8/MySQL8, original Store, no reconnect/retry changes; six rows and 1213 recorded exactly |
| MR-G5 Permission | Actual L0 allowlisted grants and least-privilege negative probes; L1 forbidden |
| MR-G6 Redaction / failure | Deterministic parser negatives, console screen, actual Actions upload skip on simulated failure |
| MR-G7 Evidence | New main-specific JSON, independent SHA/CRC, run provenance, safe artifact only |
| MR-G8 Scope of claim | Historical main snapshot vs current main correctly distinguished; original first-attempt failure never rewritten |

**Implementation readiness decision for this file:** `RUNNER_FILE_SCOPE_PROPOSED`; `RUNNER_DESIGN_INDEPENDENT_REVIEW=PENDING`; `EXACT_MAIN_RUNNER_IMPLEMENTATION_AUTHORIZATION=NOT_GRANTED`; `POST_MAIN_PHYSICAL_BASELINE=PENDING`; `PR465_MERGE_AUTHORIZATION=NOT_GRANTED`.

---

## 8. MR-R1 — executable dual-tree / historical Store bytecode provenance

**Two immutable checkouts:** `CONTROL` is independently reviewed runner commit `runnerControlSha`, containing the six candidate runner files. `SOURCE` is the independently checked out `33c0914e843d7976c7ff2272338377aa4925f443` historical main. Paths under `$RUNNER_TEMP/control` and `$RUNNER_TEMP/source` never overlap. Neither is a PR synthetic merge; pin both HEADs and tree hashes, and reject uncommitted tracked-file changes. Build output may be created only in disposable Maven `target/` and isolated local artifact cache.

**Candidate Java8 build sequence (needs smoke proof):**
1. On SOURCE, assert HEAD, tree, `JdbcU06DeliveryStore.java` Git blob hash, `diagnosis-service/pom.xml` blob and DDL blob manifest. Install SOURCE `contracts/v1/bindings/java` dependency. `mvn -B -f diagnosis-service/pom.xml -DskipTests test-compile` builds historical main sources. Record hash of SOURCE `target/classes/com/aidoctor/diagnosis/runtime/u06/delivery/JdbcU06DeliveryStore.class`, class version 52, Java8 and Maven versions.
2. Compile only CONTROL `mainexact` test Java sources to a **separate augmentation folder** using the SOURCE test-scope resolved dependency classpath and SOURCE `target/classes`; no POM or source edits. Reject CONTROL attempting to define Store production classes or shadow their package/name. The exact Maven classpath derivation/test discovery behavior is **not demonstrated** and is a pre-implementation blocker.
3. Execute exclusively selected new JUnit tests with an explicitly assembled classpath or independently checked Surefire mode. In particular, Surefire often loads `target/test-classes` ahead of `target/classes`, so forbid duplicate Store classes and assert the **actual live ClassLoader CodeSource canonical path** matches SOURCE `target/classes`. Extract runtime .class bytes, SHA256-match the freshly compiled SOURCE Store hash, and assert SOURCE production Store has not been copied from CONTROL. A test shadow-class injection must fail before any DB write.
4. Bind `compiledStoreSha256`, `sourceStoreBlobSha`, `sourcePomBlobSha`, `sourceTreeSha`, CONTROL Java test class digests and dependency/classpath digest into each evidence artifact. Require reproducibility/clean checkout and reject stale output. `git rev-parse` alone is insufficient physical bytecode proof.

**Six-file feasibility decision:** proposed inventory remains the maximum ADD-only bound, **not sufficient-by-assertion**. If no six-file, no-POM, unmodified-Store Java8 build/test selection can be independently demonstrated, return to design for scope amendment. No implementation authorized.

## 9. MR-R2/R6 — immutable control-plane lifecycle and historical/current-main distinction

- Separate `runnerControlSha` from `sourceUnderTestSha`. The new workflow can only be dispatched from a reviewed control commit on default branch; no user-controlled ref or target SHA input. Validate event type, actor/repository/ref and control workflow Git blob. `GITHUB_SHA` identifies *control event* and may legitimately differ from SOURCE SHA, but it cannot silently substitute for the target witness. Pin workflow and manifest blobs and runner control commit, and independently verify CONTROL checkout.
- Because a new workflow must be merged into main before standard `workflow_dispatch`, its introduction changes main from `33c0914e`. A run pinned to the old SHA is a `HISTORICAL_POST_463_SOURCE_WITNESS`, **not** `CURRENT_MAIN_PHYSICAL_VERIFIED`. A new exact-current-main gate requires a fresh target and verification after the new workflow merge.
- Two independently reviewed source/control code origins, minimal `contents: read` permission, no arbitrary fork code, no production data or credentials. A failed identity comparison prevents witness, evidence admission and artifact upload. Runner disable/removal requires separate controlled change; never auto-rebase the historical source target.

## 10. MR-R3/R4 — explicit six-file roles, immutable evidence and negative witnesses

Workflow: checkout/run control and source identities, Java8/MySQL8, real failure-path upload admission. `MainExactForensicsTest`: isolated Store invocations. `MainExactEvidence`: six-table readback, loaded-class provenance and allowlist evidence. `MainExactTestConfig`: disposable connection roles/transaction ownership. `manifest.json`: both source and CONTROL binding authority, DDL/dependency hashes. `bootstrap-and-verify.py`: separate main schema validator, provenance binding, privilege/redaction negatives and fail-closed safe upload. Existing PR-only F workflow and all production/POM/DDL untouched.

Mandatory shared evidence tuple:
```text
schemaVersion, sourceUnderTestSha, sourceTreeSha,
sourceStoreBlobSha, sourcePomBlobSha, compiledStoreSha256,
loadedStoreCodeSource, runnerControlSha, runnerWorkflowBlobSha,
runnerManifestBlobSha, workflowRunId, runAttempt,
executionMode=HISTORICAL_MAIN_EXACT, testCaseId,
writerConnectionIds, readerConnectionId, mysqlVersion, javaVersion
```
Three admitted JSON files must agree on shared values (or reference an independently validated immutable digest). Reject stale run attempt, partial/unknown JSON keys, symlinks/hardlinks, nonregular or oversized files, path traversal, SHA mismatch, Store class shadowing, and artifact leftovers after injected failure. No raw SQL, secrets, LOCK_DATA, patient identifiers, stack traces in console or artifact. GitHub Actions prints `run:` scripts into public logs: do not include literal secret/test-sentinel contents in that script even when redirecting command output. Negative tests must prove actual `upload-artifact` skipped, not only Python parser returns nonzero.

## 11. MR-R5 — separated result verdicts and no causal overclaim

`sourceFirstAttemptVerdict = BOTH_NEW_SUCCESS | FAILED_1213 | FAILED_OTHER | UNKNOWN`; when first attempt 1213 victim rolled back and counterpart committed, `FAILED_1213` is mandatory. `eventualRetryVerdict=NOT_EXECUTED` always for this Runner. `lockGraphVerdict=NO_EDGE_CAPTURED | PARTIAL_EDGE_ONLY | LIVE_TWO_WRITER_CYCLE_BOUND`, diagnostic only. `physicalBaselineVerdict=SOURCE_EXECUTION_VERIFIED | EVIDENCE_REJECTED | NOT_RUN` requires Java8, MySQL8, exact loaded Store bytecode, least-privilege negatives and independent six-table readback; it never rewrites original RACE-05 first-attempt failing business verdict.

**MR-R1..06 proposed disposition:** `TARGETED_DESIGN_SUBMITTED`, `DUAL_TREE_COMPILE_TEST_FEASIBILITY=UNPROVEN`, `RUNNER_IMPLEMENTATION_AUTHORIZATION=NOT_GRANTED`, `CURRENT_MAIN_PHYSICAL_BASELINE=PENDING`.
