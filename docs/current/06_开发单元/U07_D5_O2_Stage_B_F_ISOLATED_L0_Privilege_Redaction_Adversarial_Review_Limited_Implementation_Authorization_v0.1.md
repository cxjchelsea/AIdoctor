# U07 D5-O2 Stage B — F-ISOLATED Six-File / L0-L1 Privilege / Redaction Adversarial Design Review + Limited Implementation Authorization v0.1

Date: 2026-10-13. **Targeted same-assistant technical design re-review; not an external independent human security assessment.** Input Draft PR #461 at exact SHA `79946277a33bcadc9ef3ecc74e0694533badf969`, base and remote `main=20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`. Draft P1-C implementation PR #457 remains `eac72825ca80ad34406a1fbd620eed20c17db3fe`, unmerged; F design must not cherry-pick its unmerged test helper or alter it.

**Formal decision: `AUTHORIZE_LIMITED_F_ISOLATED_L0_TEST_ONLY_IMPLEMENTATION`, with exact six new files and hard gates below.** **L1/PROCESS explicitly NOT AUTHORIZED**. This authorizes a **separate Draft implementation PR** containing exclusively synthetic Java8 / disposable MySQL8 L0 Performance Schema forensics, no merge, no Store changes, no retry, no real delivery, no F8/B2/REAL_D5. Acceptance of code/evidence and its merge readiness require distinct exact-HEAD gates after real execution. Any need for a seventh file, modifying old files, L1 PROCESS, extra grants, MySQL global runtime changes, raw status upload or larger budget requires controlled amendment first.

## 1. Source-based adversarial findings / decisions

| ID | Adversarial case | Finding | Decision |
| --- | --- | --- | --- |
| **F-ADV-01** | Six-file F scope accidentally relies on PR #457 test classes not on main | PR #457 is unmerged; reference/importing its source from a main-derived implementation PR would not compile or create hidden dependency. | **BLOCKING DESIGN RISK CLOSED BY CONTRACT**: new six files must be self-contained, invoke original main Store and existing Java8 dependencies only. Compiles independently on exact main without fetching #457 code. |
| **F-ADV-02** | “Read-only” observer can still read unrelated sessions via Performance Schema | L0 SELECT grants are narrow by table but cover active server metadata, so least-privilege alone cannot guarantee no other sessions exposed. | **CLOSED_WITH_ISOLATED_INSTANCE_COMPENSATION**: disposable localhost instance, verified no unrelated app traffic, strict allowed-column SELECT, filter only physical A/B thread mapping, never persist unrelated rows. Runtime negative privilege tests mandatory. |
| **F-ADV-03** | L1 global PROCESS grants broader introspection | PROCESS is global and enables `SHOW ENGINE INNODB STATUS`; cannot be accurately described as least privilege to a single database. | **L1 NOT AUTHORIZED**. No L1 account/PROCESS creation, no `SHOW ENGINE INNODB STATUS` execution, no raw status artifact. `L1_NOT_ENABLED` is the only permitted L1 result under this authorization. Re-review separately if needed. |
| **F-ADV-04** | One captured waiting edge or stale deadlock status presented as “the 1213 cycle” | `data_lock_waits` is transient; captured edge is not by itself a proven complete deadlock cycle. Thread IDs/CONNECTION_IDs and internal lock ID formats need correct handling. | **CLOSED_FAIL_CLOSED**: 1213 victim rollback and live edge evidence are different verdict planes; `FORENSIC_GRAPH_BOUND` only if a coherent, *actually sampled*, two-writer wait-for cycle with both directions and expected lock metadata at same observation epoch is proven. A single edge => `FORENSIC_PARTIAL`, no edge => `FORENSIC_UNAVAILABLE`; neither names definitive culprit index. |
| **F-ADV-05** | Observation timestamps incompatible | JVM nanoTime not comparable to server wall clock, separate MySQL calls may race across events. | **CLOSED_CONTRACT_ONLY**: same-JVM monotonic times can bracket sampling; DB snapshot batches receive start/end monotonic bounds. Do not claim atomic snapshot across separate SQL SELECT statements or wall-clock equality. |
| **F-ADV-06** | Supposed 15 s observation from only 100×25–50ms polling | 100×50ms = about 5s maximum planned sampling, not 15s. | **FIXED**: Observer window **5s maximum, 100 samples maximum, 50ms minimum inter-sample spacing**; stop earlier after both writers terminal; main TX common deadline remains 15s, ready <=3s, per-TX <=10s, MySQL lock wait=5s, executor shutdown<=5s. Real acquisition time may make fewer than 100 observations. No sleep after locks held by Worker, no retry. |
| **F-ADV-07** | “Sanitize” by logging raw SQL then replacing strings | Raw status/LOCK_DATA/SQL/parameters can leak via failures, logger and Surefire before post-hoc cleanup. | **CLOSED_WITH_OUTPUT_ALLOWLIST + ADVERSARIAL_NEGATIVES**: only explicit field projections, SQL whitelist and typed lock metadata, no raw SQL/status/LOCK_DATA/username/credential/path/errors in persisted JSON/JUnit/stdout/artifacts. Unknown columns/types => fail-closed without dumping raw exception text. |
| **F-ADV-08** | One green first-attempt or no deadlock counted as successful causal forensics | Deadlocks nondeterministic; no 1213 is not graph proof. | **CLOSED**: distinct `NO_1213_OBSERVED`, `ROLLBACK_CONFIRMED_1213` and `FORENSIC_GRAPH_BOUND/PARTIAL/UNAVAILABLE` result dimensions. Purely diagnostic smoke may pass admission as valid no-incident observation, but NEVER as achieved root-cause attribution. |
| **F-ADV-09** | Upload everything under `if: always()` | Maven XML may contain raw exception strings, and a broad wildcard can include raw dumps. | **REQUIRED PHYSICAL CHECK**: precise artifact paths to already sanitized test-origin JSON and redacted runner report only; any raw JUnit XML must pass static secret/unsafe-string scan or be excluded with explicit evidence of JUnit outcome via sanitized structured manifest. No credential/string leaks on CI failure. |
| **F-ADV-10** | Legacy transaction grants/data access accidentally carried into F | A new observer should never become a Writer, and no privileged cleanup per-test is needed. | **CLOSED_CONTRACT_ONLY**: isolated schema and unique synthetic effect IDs, root only for ephemeral bootstrap, Writer SELECT/INSERT restricted to six tables, Reader SELECT, L0 observer P_S SELECT only, all unauthorized read/write/DDL/PROCESS negative tests; no Seeder, no ALTER/INDEX, no production DS. |

Official MySQL 8.0 reference:
- `performance_schema.data_lock_waits` has requesting/blocking lock and THREAD_ID fields and requires ordinary table SELECT, not PROCESS: https://dev.mysql.com/doc/refman/8.0/en/performance-schema-data-lock-waits-table.html
- `data_locks` `ENGINE_LOCK_ID` internal format is unstable; joining must be by exact equality, not parsing lock IDs: https://dev.mysql.com/doc/refman/8.0/en/performance-schema-data-locks-table.html
- A `PROCESSLIST_ID` ↔ `THREAD_ID` correlation is accessible via `performance_schema.threads`, on the same disposable instance. **Actual permissions must be verified on the real MySQL8 minor version**, not asserted from docs alone.

## 2. Exactly six authorized ADD-only files

New standalone Draft PR must be branched from exact `main=20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`, rechecking drift before implementation:

1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFDeadlockForensicsTest.java`
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFObserverEvidence.java`
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/forensics/StageBFTestConfig.java`
4. `diagnosis-service/src/test/resources/u07/d5o2/stageb/forensics/manifest.json`
5. `tools/u07_d5_o2_tx_topology/stageb/forensics/bootstrap-and-verify.py`
6. `.github/workflows/u07-d5-o2-stage-b-forensics.yml`

All six files must be **new**. The implementation may embed test-only redaction helpers inside the three approved Java classes/script; no added redactor source file, no import of unmerged #457 classes, no `pom.xml` or application config alteration. Workflow path filters only the new forensics paths; do not edit accepted P1A/B assets or Draft P1C CI. If Java8 compile or physical permission preflight requires new paths/dependencies, STOP and request a controlled amendment.

Environment:
- MySQL 8.0 disposable single-service local port **33331**, schema **`u07_d5o2_f_deadlock`**, profile **`U07_D5_O2_RACE05_F_ISOLATED_SYNTHETIC`**.
- Original unchanged V2/V3/V6 migration Git blob SHAs `73029495c5f46e8576e34f8c9a77644fb8c1c4c4`, `ccc1aa30b8e9405bd07249bae1d3531853f7510b`, `7ed5a82bfba3b3fed2666bee969a7fe276e67566`; all six original delivery tables must physically be InnoDB with original PK/UNIQUE names. No new indexes.
- Generate separate masked temporary passwords for Writer, Reader and L0 Observer. Root credentials cannot be provided to the test JVM, passed to the Observer or uploaded. MySQL must bind loopback port, with no external apps and no real patient data.

## 3. L0-only exact privilege allowlist and negative tests

L0 `stagebf_observer_ps` receives **only**:
```sql
GRANT SELECT ON performance_schema.data_locks TO 'stagebf_observer_ps'@'%';
GRANT SELECT ON performance_schema.data_lock_waits TO 'stagebf_observer_ps'@'%';
GRANT SELECT ON performance_schema.threads TO 'stagebf_observer_ps'@'%';
```
No `PROCESS`, `SUPER`, `FILE`, `RELOAD`, `ALL`, `GRANT OPTION`, wildcard app table SELECT or any DML/DDL. No L1 identity is created in the first limited PR.

Writer: SELECT/INSERT on the exact original six business tables only. Reader: SELECT on those six only. `stagebf_observer_ps` cannot access original six business tables, nor read `mysql.user`. Physical negative tests require unauthorized Writer DELETE/UPDATE and Reader DML, Observer business-table SELECT/INSERT/UPDATE/DELETE, `CREATE TABLE`, and `SHOW ENGINE INNODB STATUS` to fail with actual MySQL privilege errors; `SHOW GRANTS` checked. If SHOW ENGINE unexpectedly succeeds without PROCESS (target-version policy variance), fail closed and record only a sanitized privilege discrepancy, not raw output. An inability to query allowed P_S tables => preflight failure, not blanket privilege escalation.

## 4. Exact incident-binding and no-false-attribution oracle

For each real case, record actual `CONNECTION_ID()` from A/B and L0 observer, observer thread mapping `threads(PROCESSLIST_ID,THREAD_ID)`, request/block thread IDs from `data_lock_waits`, and lock details by joining exact `(ENGINE, ENGINE_LOCK_ID)` to `data_locks`. Compare with six-table allowed schema and a pre-extracted immutable DDL index-name allowlist. Raw `ENGINE_LOCK_ID` must **not be parsed as a stable structured format**; do not serialize raw `LOCK_DATA`, `PROCESSLIST_INFO`, `SQL_TEXT` or server user strings.

Three separate evidence planes:
- `sourceVerdict`: original Store / SQLState `40001:1213` / raw method outcome;
- `transactionVerdict`: confirmed new commit vs deadlock victim rollback, from real transaction manager exit, no guessing;
- `forensicVerdict`: `LIVE_TWO_WRITER_CYCLE_BOUND`, `PARTIAL_EDGE_ONLY`, `NO_EDGE_CAPTURED`, `NO_1213_OBSERVED`, `UNBOUND`, `CAPTURE_FAILED`.

For **`LIVE_TWO_WRITER_CYCLE_BOUND`**, require a real 1213 for same actor pair, exact writer-ID to P_S thread mapping, opposing requesting→blocking edges within same bounded sample epoch, and both corresponding lock metadata correctly associated with the two writers, along with observed original SQL/tx interval overlap and allowed table/index names. A single edge or a lock snapshot that cannot prove a cycle **may establish an observed wait dependency**, not a deadlock cycle. No `SHOW ENGINE` status is collected in L0 implementation. Because the deadlock may resolve before polling, `NO_EDGE_CAPTURED` is honest/acceptable as non-attributed diagnostic output, **not** a pass for precise root-cause gate.

Test acceptance must distinguish `EVIDENCE_PIPELINE_VALID` from `FORENSIC_CAUSE_PROVEN`. A run may be admitted as valid diagnostic data without witnessing any 1213; it must never print a fake `FORENSIC_GRAPH_BOUND` status.

## 5. Deterministic budgets and redact-before-output safety

Timeout budget fixed for **test-only initial implementation**:
- READY ≤3 s; both workers START outside DB TX.
- Common race deadline ≤15 s (not 15 s per Future); each full TX ≤10 s; session `innodb_lock_wait_timeout=5` and observed.
- L0 observer sampling maximum **5 s elapsed AND ≤100 queries**, minimum **50 ms between sample initiations**; stop when both worker transactions terminate. Start/stop with bounded executor lifecycle, `awaitTermination≤5s`. If deadline, observation error, cancelled/unfinished task or partial evidence, report `CAPTURE_FAILED/NOT_OBSERVED`, not positive incident attribution. The 50ms spacing is a maximum-rate budget, not a guarantee of 100 samples or complete 5s coverage.
- Require bounded client JDBC query timeout for every observer call, no unbounded blocking on Performance Schema SELECT. Do not insert sleeps/barriers between original Store SELECT and INSERT.
- Run **three independently scheduled whole-suite attempts** in disposable MySQL environments once implementation is compiled and scope accepted. Preserve all pass/fail, no cherry-picking. A no-deadlock run is not evidence of absence of rare 1213.

Whitelist-only redaction pipeline:
- Allowed named metadata: enum verdicts; run+attempt identifiers; synthetic effect digests; signed integer connection/thread ids; bounded event monotonic times; vetted original six table names, DDL index names, normalized `ENGINE`, lock type/mode/status and required requesting/blocking link; SHA256 canonical manifest. Reject unknown/object schemas and out-of-range types.
- **Not allowed anywhere**: raw `SHOW ENGINE` output, raw SQL, row `LOCK_DATA`, SQL literals and bind params, MySQL credentials, unrelated thread fields, exception message/stack if it contains database SQL, synthetic leak markers, path/host/env secrets. Extract only the explicit SELECT-column subset; sanitize exception mapping by class/known SQLState/error code (not `e.toString()`).
- Adversarial negative suite must inject fake password, fake patient/person/phone marker, raw query text, `LOCK_DATA` and unrelated schema/index data; inspect final JSON, XML, CI log and archived bytes. **No broad artifact glob**; upload only named safe JSON, independently sanitized run-verdict evidence and reviewed XML. Original raw files kept ephemeral and not uploaded. Before `if: always()` artifact upload, perform fail-closed allowlist/marker check; if sanitizer fails, only a safe error sentinel JSON may be emitted and upload any raw output must be skipped.
- Limit normalized record ≤64KiB, total artifact ≤1MiB; overflow ⇒ CAPTURE_FAILED, never truncate to falsely bound a complete cycle.

## 6. Required implementation verification and stop rules

Stage I: self-contained Java8 compile + isolated MySQL8 schema immutable DDL check; actual L0 grants and negative SQL permission observations; no PROCESS account.
Stage II: actual two-worker original Store transaction, third-reader six-table source characterization and separately observing L0 thread/lock sample; no automatic retry, SQL lock hints, custom schema, changed SERIALIZABLE or prod routing.
Stage III: adversarial redaction and incident attribution test cases, including one edge only, stale/unmapped actor ids, wrong schema/index, no waiting edge, no 1213, overflow, secret marker, observer privilege escalation, unresponsive threads and partial six-table graph.
Stage IV: exact-HEAD run ×3; original artifacts and statuses independently reviewed; **only** then a separate implementation/security/evidence R1 gate and merge readiness may be assessed.

Stop immediately if exact base main moves, more than six paths are needed, L0 P_S privilege behavior disagrees with the approved allowlist, raw unredacted data is published, or actual observation relies on unapproved L1. Do not auto-adjust access rights, permit broader table privileges, simulate physical deadlock edges, or convert a failed original source RACE05 into success.

## 7. Formal decision

```text
PR461_DESIGN_EXACT_HEAD = 79946277a33bcadc9ef3ecc74e0694533badf969
MAIN_EXACT_HEAD = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
F_ISOLATED_DESIGN_RE_REVIEW = ACCEPTED_WITH_LIMITED_L0_SCOPE
F_ISOLATED_FILE_SCOPE = FROZEN_6_ADD_ONLY
F_ISOLATED_L0_PRIVILEGE_POLICY = FROZEN_MINIMAL_THREE_P_S_TABLE_SELECT
F_ISOLATED_L1_PROCESS = NOT_AUTHORIZED
F_ISOLATED_SECURITY_REDACTION_CONTRACT = FROZEN; PHYSICAL_NEGATIVES_PENDING
F_ISOLATED_INCIDENT_ATTRIBUTION = FAIL_CLOSED_TWO_WRITER_CYCLE_ONLY
F_ISOLATED_SCOPED_IMPLEMENTATION_AUTHORIZATION = AUTHORIZE_L0_ONLY_TEST_ASSETS
F_ISOLATED_REAL_JAVA8_MYSQL8 = NOT_EXECUTED
F_ISOLATED_MERGE_AUTHORIZATION = NOT_GRANTED
CALLER_OWNED_WHOLE_TX_RETRY_IMPLEMENTATION = NOT_AUTHORIZED
ORIGINAL_P1C_RACE05_VERDICT = UNCHANGED_NOT_STABLY_PASSING
ORIGINAL_FOUR_JOIN_REPLAY = CONFIRMED_NOT_SAFE
B2_AND_REAL_D5 = NOT_AUTHORIZED
```

**Next allowed task:** `D5-O2 Stage B — F-ISOLATED L0-Only Six-File Deadlock Forensics Implementation + Real Java8/MySQL8 Privilege / Redaction Smoke` on a **new Draft PR**. L1, broader MySQL permissions, replay source repair, retry, production, patient data, F8 or any merge require separately approved authorizations. This document does not claim independent human review or physical validation beyond the prior baseline.
