# U07 D5-O2 Stage B — F-ISOLATED PR #463 Exact-HEAD L0 Security / Redaction / Forensic Evidence R1 Targeted Technical Review

Date: 2026-10-13. **REVIEW-ONLY. R1 verdict: `BLOCKED_REMEDIATION_REQUIRED`.** This is a newly authored source/artifact technical review by the **same assistant**, not a separate human-independent security sign-off. It does not change code, run new Java/MySQL tests, grant implementation scope, or authorize merges.

## 1. Exact head / scope and original physical evidence

- Remote `main`: `20a54b4a7478641d8b8d8c12c6213b8e2ea73acc` unchanged. F-ISOLATED implementation Draft [PR #463](https://github.com/cxjchelsea/AIdoctor/pull/463) exact HEAD `c849d43bf311f0b3865812ce7dbcdf852050e157`, unmerged; base exact main. GitHub exact compare: **six ADD-only files**, zero deletions/modifications, 749 added lines: standalone `.github/workflows/u07-d5-o2-stage-b-forensics.yml`; the three Java test classes `StageBFDeadlockForensicsTest.java`, `StageBFObserverEvidence.java`, `StageBFTestConfig.java`; forensics `manifest.json`; `bootstrap-and-verify.py`. No original production `JdbcU06DeliveryStore`, Flyway, P1A/B/C, pom/app code edited. Limited L0-only implementation authority in [Draft PR #462](https://github.com/cxjchelsea/AIdoctor/pull/462) respected; **L1 PROCESS was not used**.
- An initial earlier-HEAD CI #38332282875 failed with business-schema JDBC default selection by L0. This was corrected by connecting L0 to `performance_schema` and targeting forbidden business schema explicitly, *without adding grants*.
- At the **reviewed exact HEAD**, [Actions run #38332462960](https://github.com/cxjchelsea/AIdoctor/actions/runs/38332462960) has three distinct Java8 + disposable MySQL8 job attempts, each SUCCESS; **one JUnit method passed with no failures/errors/skips**, and the positive verifier emitted `F_ISOLATED_L0_ADMISSION=EVIDENCE_PIPELINE_VALID`, `F_ISOLATED_CAUSE_PROVEN=false`.
- Original artifacts independently downloaded in this R1, checked against GitHub metadata SHA-256, ZIP CRC and member paths:
  - Attempt 1, job `115085059595`, Artifact **#11782969293**, `118c2dce70c76c4bd64d833f86251bb5381d7684a261bc2bd42e54cd688fbadb`: real `40001:1213`, victim A vector [0,0,0,0,0,0], other B [1,1,1,1,1,1], **7 samples, NO_EDGE_CAPTURED**.
  - Attempt 2, job `115085678382`, Artifact **#11783377592**, `a643326df6bc2c2bd0e5f4bbb612153c37769ee98c9a1df05b48f10cc4fbe0ee`: real `40001:1213`, victim B 0/6, other A 6/6, **5 samples, PARTIAL_EDGE_ONLY**.
  - Attempt 3, job `115086054736`, Artifact **#11782893300**, `639adf382a9f6aeaa0ddb1cc8462238f5d0dc2a86bc9758d34acd9bb960b428f`: real `40001:1213`, victim B 0/6, other A 6/6, **5 samples, NO_EDGE_CAPTURED**.
- Each ZIP contains **exactly three safe-intended JSON**: `stage-b-f-observations/forensic-incident.json`, `stage-b-f-observations/role-negative.json`, `f-isolated-verification.json`. No raw `SHOW ENGINE INNODB STATUS` dump was present. **The ZIP does not contain original Surefire XML or independently reconstructible captured lock edges.** The logs show 1 passing JUnit test per job, but that is a **separate observed source**, not a ZIP member. Role negative flags are emitted true in all three; their underlying SQL failures are asserted during JUnit, not independently represented in ZIP evidence.

## 2. R1 blockers (must close before scoped implementation verification closure or merge)

### R1-01 — BLOCKER: Negative redaction tests are not actually negative parser tests

- In `tools/u07_d5_o2_tx_topology/stageb/forensics/bootstrap-and-verify.py`, `negative_parser_tests()` creates a dictionary containing each forbidden marker, JSON serializes it, then only checks the marker *is present*; it **never passes malicious input into the actual `verify` rejection path**, nor asserts a nonzero failure. It is tautological, not evidence of reject behavior.
- In `StageBFObserverEvidence.validateSafe()`, output acceptance is **a substring blacklist**, not the frozen typed allowlist. Unrecognized additional fields and strings without enumerated marker can be serialized. `evidence(...).putAll(more)` is also able to overwrite reserved identity/authority fields without a schema rejection. Schema/table index checks occur in the live query path but Java's final arbitrary JSON map is not type/field whitelisted.
- **Remediation:** within existing six file paths (no new file), introduce exact allowed keys/typed schema and immutable reserved provenance/authority fields; enforce known table/index locks and bounded enum values in the output serializer; exercise actual Python verifier with rejected mutated JSON copied to safe temporary paths (unknown key, fake secret not matching existing marker, forbidden SQL/LOCK_DATA, unrelated schema/index, overridden source SHA, false `L1_NOT_ENABLED`, oversized output). Fail closed and keep negative fixtures entirely synthetic. Do not insert raw secrets into real CI artifacts.
- **Closure oracle:** mutation test actually invokes and observes verifier nonzero return with a stable reject code, while valid original physical facts remain accepted.

### R1-02 — BLOCKER: No independently replayable physical lock-edge attestation

- `StageBFObserverEvidence.Sample` records `aThread/bThread`, `selected` lock-edge maps, edges counts and opposing direction in JVM memory, but `StageBFDeadlockForensicsTest` exports **only** source verdict/forensic verdict/sample count plus worker facts. Consequently Artifact #11783377592's `PARTIAL_EDGE_ONLY` **cannot be independently checked against lock identities, captured index/table/mode, lock owner or observer session**.
- The current `LIVE_TWO_WRITER_CYCLE_BOUND` classifier counts opposing requester↔blocker direction in one ResultSet iteration and checks 1213, but the Python verifier checks **only** that verdict and the existence of a 1213 actor, not actual two-worker thread bindings, same sampling epoch or coherent lock relationships. The query joins **requesting** `data_locks` but does not independently attest the **blocking** lock record or capture a verified engine/transaction crosswalk. `ENGINE_LOCK_ID` is opaque; do not parse it.
- **Remediation:** select and serialize only reviewed redacted metadata for both requesting and blocking lock records, with typed `observerConnectionId`, two writer CONNECTION_ID→P_S THREAD_ID mappings, sample epoch, lock engine identity, vetted schema/table/index, both lock modes/status, exact waiter↔blocker direction and bounded (prefer 24 maximum) set of edges. Reject unknown/overlong metadata; protect `LOCK_DATA`/raw SQL from serializer and logs. Verify actual index against immutable DDL allowlist (regex alone is insufficient). Require Python validator to derive result from evidence rather than trusting input verdict. No live edge→deadlock-cycle inference without two correctly bound directions **within one epoch**, original 1213, coherent attribution.
- **Closure oracle:** independent replay of redacted original record yields identical partial/bound/unavailable verdict and negative-mutant lock-edge tests fail; no invented graph. `NO_EDGE_CAPTURED` remains an admissible *honest diagnostic absence*, not full root-cause proof.

### R1-03 — BLOCKER: Failure-path Artifact admission is not protected by a sanitizer gate

- Workflow has `upload-artifact@... if: always()` with specific paths (good), **but no pre-upload safe-artifact validation on failure**. Python `verify` is invoked only after Maven succeeds, and a failed or interrupted JUnit test can leave `forensic-incident.json` emitted earlier with `CAPTURE_NOT_STARTED` or `DIAGNOSTIC_ONLY_NOT_ACCEPTED`; those files are still uploadable. The redaction logic `validateSafe()` does not enforce all possible unknown fields, and Error/Exception code paths risk text reaching Maven/JUnit logs. The ZIP is safe-intended in three green runs; that does not prove failure-path safety.
- **Remediation:** separate a fail-closed **sanitization-only upload gate** (not a success admission) that runs independently even if Maven fails, checks exact allowed filenames, schema, type/size, secret markers, raw error/SQL fragments and reject sentinel behavior. Upload either sanitized diagnostic evidence or a minimal safe sentinel; never artifact raw Surefire unless independently scanned. Preserve red CI verdict on any test or security failure. Avoid glob over entire test target.
- **Closure oracle:** deliberately failing JUnit/observer/error-path fixtures do not upload secret-bearing facts or success claims; failing tests remain red, original evidence survives in limited safe form.

### R1-04 — REQUIRED: JDBC network timeout executor lifecycle is unbounded

- `StageBFTestConfig.connection()` allocates a new `Executors.newSingleThreadExecutor` per JDBC connection when calling `setNetworkTimeout()`; it is **never shutdown**. Daemon thread factory does not by itself establish a bounded resource lifecycle. The review contract requires no leaked executors, bounded cancellation and no zombie observer.
- **Remediation:** use a shared bounded executor with explicit shutdown/awaitTermination in `AutoCloseable` owner or equivalent lifecycle-safe bounded mechanism; verify no unbounded JDBC/observer worker/role connections, timeout cases and clean teardown. Avoid weakening time limits to mask failures.
- **Closure oracle:** targeted lifecycle test proves created executors terminate after success and failure.

### R1-05 — REQUIRED: Source error and complete six-table proof are weaker than claimed

- `StageBFDeadlockForensicsTest` accepts `aSuccess||bSuccess||victim` and does not reject all combinations of `OTHER_EXCEPTION/OTHER_SQL_ERROR` when another actor succeeds. It validates **six counts**, but not the cross-table identity, attempt/receipt status or canonical full-column digest. This is acceptable for a narrowly labeled diagnostic but **does not support** stronger transaction/safety claims.
- **Remediation:** preserve `DIAGNOSTIC_PIPELINE_ONLY`; make any unexpected second actor outcome explicitly typed and rejected for positive characterization, confirm exactly expected complete-group/empty-group and full identities if claiming coherent six-table verification, and record trace status separate from source safety. Avoid declaring full-six business replay correctness, especially because original four-way Store `find()` remains unsafe.
- **Closure oracle:** injected unexpected exception/partial-row/incorrect identity is rejected, while original real 1213 + empty victim/complete other remains a diagnostic observation.

### R1-06 — REQUIRED: CI immutable source + evidence provenance coverage incomplete

- Python checks candidate PR head against synthetic merge parent and original DDL content blobs, but accepts arbitrary extra top-level fields and verdict strings that are not proven from raw facts. It does not require `role-negative.json` to contain per-probe SQLState/vendor-code facts; `rolesVerified=true` is not an independently auditable permission matrix. `forensic-incident.json` lacks actual observer session and sample-level edges; runner source facts alone do not prove precise graph attribution.
- **Remediation:** typed per-negative SQL verdict matrix and true privilege-denied codes for Writer, Reader, Observer; strict connection identity uniqueness; independent run/attempt/checkout/DDL binding for all artifact members. Persist a reviewed sanitized parser version and allowlist manifest, and perform byte-budget/filename provenance checks before publishing.
- **Closure oracle:** tampering any binding, deleting one role negative, or forging `FORENSIC_GRAPH_BOUND` without physical edge proof makes verification fail.

## 3. Positives and bounded interpretation

- Six-file ADD-only scope, disposable loopback MySQL8, frozen original main DDL, Java8 compilation and all three physical CI job attempts: **PASS**.
- L0 is genuinely connected to `performance_schema`, with table-level SELECTs on `data_locks/data_lock_waits/threads`; no L1 account/PROCESS or raw `SHOW ENGINE STATUS` reads in this implementation. The JUnit unauthorized DML/PROCESS probes **executed and passed** in three runs. This supports `L0_PHYSICAL_SMOKE=PASS`, **not complete independent credential audit**.
- Reproduced real SQLState `40001:1213` on three disposable MySQL8 runs; in each, one actor's six-table group is empty and the other has six rows. This is test-only source characterization. No full wait-for cycle was captured; `FORENSIC_CAUSE_PROVEN=false` was correctly emitted in all attempts.
- Reviewer does not independently execute a new MySQL job here; R1 judgments are anchored to frozen source + original CI runs + original downloaded ZIP inspection.
- R1 cannot authorize L1/PROCESS, production Store repair, whole transaction retry or use of diagnosis/patient data, nor infer that a four-table join establishes complete replay.

## 4. Targeted remediation authority and immutable guardrails

**Recommendation:** perform `F-ISOLATED PR #463 R1-01..06 Targeted Security / Evidence Contract Remediation` **only after explicit user request/implementation authorization**. All changes must be within the already six-new-file Draft PR #463; if requiring a seventh path or changed P1A/B/C/production file, return to controlled amendment. Do not add L1 PROCESS, external calls, retry, SQL transaction-level changes, altered isolation, relaxed 1213 interpretation or fake lock evidence. Prefer fixing evidence validity before additional reruns.

**Required follow-on gates:** (i) targeted physical Java8/MySQL8 run of exact corrected HEAD; (ii) **negative mutation** verifier/execution checks, including redaction and failure-upload gates; (iii) bounded real attempts if incident attribution changes; (iv) independent evidence/semantic re-review; (v) exact-main diff, required checks and explicit merge authorization, all separate.

## 5. Formal R1 decision

```text
MAIN_EXACT_HEAD = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
PR463_EXACT_HEAD = c849d43bf311f0b3865812ce7dbcdf852050e157
PR463_EXACT_DIFF = PASS_6_ADDED_F_ONLY_FILES
PR463_JAVA8_MYSQL8_PHYSICAL_SMOKE = PASS_3_OF_3
PR463_L0_PHYSICAL_NEGATIVE_GRANT_SMOKE = PASS_BOUNDED
PR463_L1_PROCESS = NOT_AUTHORIZED_NOT_USED
PR463_ORIGINAL_1213_VICTIM_ROLLBACK = OBSERVED_3_OF_3
PR463_LOCK_GRAPH = ZERO_FULL_CYCLE; ONE_PARTIAL_EDGE_ONLY
R1_01_REDACTION_NEGATIVE_TEST = BLOCKER
R1_02_LOCK_EDGE_ATTESTATION = BLOCKER
R1_03_FAILING_ARTIFACT_ADMISSION = BLOCKER
R1_04_NETWORK_TIMEOUT_RESOURCE_LIFECYCLE = REQUIRED
R1_05_SOURCE_AND_SIX_TABLE_DIAGNOSTIC_PREDICATE = REQUIRED
R1_06_EVIDENCE_PROVENANCE_AND_NEGATIVE_MATRIX = REQUIRED
PR463_R1_SECURITY_EVIDENCE_REVIEW = BLOCKED_REMEDIATION_REQUIRED
PR463_IMPLEMENTATION_VERIFICATION_CLOSURE = NOT_READY
PR463_MERGE_AUTHORIZATION = NOT_GRANTED
B1_P1_C_ORIGINAL_RACE05_VERDICT = UNCHANGED_NOT_STABLY_MET
ORIGINAL_FOUR_JOIN_REPLAY = CONFIRMED_NOT_SAFE
WHOLE_TX_RETRY_B2_REAL_D5 = NOT_AUTHORIZED
```

**Next:** `D5-O2 Stage B — F-ISOLATED PR #463 R1-01..06 Targeted Security / Evidence Contract Remediation`, preserving the six new test-only files. This report is not an approval to merge or implement production fixes.
