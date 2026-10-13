# U07 D5-O2 Stage B — B1-P1-C RACE-05 Fail-Closed Diagnostic Evidence / Race-Safety Contract Targeted Review v0.1

Date: 2026-10-13. **Targeted source/artifact technical review, performed by the same assistant — not a separate independent human security sign-off.** Evidence input: implementation Draft PR [#457](https://github.com/cxjchelsea/AIdoctor/pull/457), exact current HEAD `c45256d165b6aaf6d80eb3d6c233dab911515dd8`, base and unchanged main `20a54b4a7478641d8b8d8c12c6213b8e2ea73acc`, previously frozen C-ISOLATED implementation scope review PR #456. This is **not** a merge authorization, source defect fix or production activation.

## 1. Exact two-run facts and limitation

- Tree diff remains **six ADD-only P1C test/fixture/manifest/CI assets**, no production Store, original migration, accepted P1A/P1B, pom or application changes.
- Earlier HEAD `29fd82764816b01588c3afa8e22e963880c99a4e`, [run #38328358819](https://github.com/cxjchelsea/AIdoctor/actions/runs/38328358819): real Java8/isolated MySQL8, **8 passed / 0 failed/errors/skips**, strict verifier printed `STAGE_B_P1C_VERIFIED_CASES=8`. Original Artifact #11781862547, SHA256 `ddc9f1ed15e4b52fc242cc8942b122bd584b777738f47cd5c9b8fee79a368152`; test evidence recorded real MySQL `40001:1213` deadlock and `23000:1062` duplicate-key conflicts, plus physical final-state readings.
- That HEAD lacked RACE-01's own post-race same-command sequential replay assertion; RACE-06A was already a distinct coverage case. A subsequent within-six-file change added RACE-01's own sequential check and matching validator admission.
- **Current exact HEAD** `c45256d165b6aaf6d80eb3d6c233dab911515dd8`, [run #38328629173](https://github.com/cxjchelsea/AIdoctor/actions/runs/38328629173): real Java8/isolated MySQL8, **7 passed, 1 failed, 0 errors, 0 skips**. The lone failure is **RACE-05**, `StageBP1CConcurrencyTest.java:82`: expected 1 `u06_delivery_authority` row for one of the two supposedly independent effects but observed 0. This occurs inside `StageBP1CEvidence.full(reader, command)` **before case-level `StageBP1CEvidence.emit()`**. Original failure Artifact #11781857882 SHA256 `d636cc70b809930d039728a92716cf84e399115ca21d04757dd247f80d8f5d31` was independently inspected as a ZIP: it contains the original Surefire XML failure and **seven** observation files, but **no `race05.json`**.
- Thus the failed run establishes **a final six-table coherence check failed** and **8 test cases executed, 1 failed**. It does **not** establish which of RACE-05's actors returned `ORIGINAL_NEW_SUCCESS`, which transaction committed, whether an InnoDB deadlock/lock timeout was raised for RACE-05, or whether a test-instrumentation/observation flaw caused the absence. **Do not invent the missing RACE-05 actor/final-group facts or allege proven source corruption.** Both runs are physically real; **latest exact HEAD is FAIL**, not PASS.

## 2. Root diagnostic design finding

**R05-DIAG-01 — BLOCKER (failure evidence loss).** In `StageBP1CConcurrencyTest.race()` the test constructs the actor facts, then opens a third Reader and reads both six-row groups, but on `scenario==6` calls `StageBP1CEvidence.full(r,first)` and `full(r,second)` at lines 81–82 **before** putting final vectors/digests into the fact and before writing that fact. The first completeness assertion can terminate the test and permanently suppress the only case-specific physical evidence. This is a deterministic ordering bug in test instrumentation, not proof of a production DB bug. A failure's original Surefire trace alone cannot restore the missing actor and six-table graph metadata.

**R05-DIAG-02 — BLOCKER (outcome classification after true physical failures).** `Actor.committed=true` means the explicit transaction call returned a normal method result, while error/rollback actor fields are collected independently. `RACE-05` **must** still require both actual `ORIGINAL_NEW_SUCCESS`/transaction-returned plus third-reader full 6-row groups. Deadlock `1213`, lock wait `1205`, duplicate key `1062`, two aborts, one new success, zero rows, unknown commit or a physically incomplete group => **RACE-05 FAIL**; no “expected losing worker” exists for two independent effects. The failed test should record actor/SQLState/vendor codes and physical state before emitting the negative verdict, not silently turn exceptions into a pass.

**R05-DIAG-03 — REQUIRED (failure artifact/validator split).** The production-admission validator currently demands one passing Surefire report, all eight positive case facts and full success invariants. Keep that **positive validator strict**. A failing observation must instead be uploaded as an explicitly labeled, test-origin **DIAGNOSTIC_ONLY / NOT_ACCEPTED** fact, never as a synthesized successful case. CI should upload original Surefire and available raw observations under `if: always()`, and the B1P1C acceptance job must remain red when RACE-05 fails. The diagnostic reporting path cannot change an exit code, hide a failing XML case, skip JUnit test execution, or silently substitute `NOT_OBSERVED` for committed six-table safety. If an exception occurs during the independent read itself, capture the error class/safe SQLState and `PHYSICAL_STATE_UNOBSERVED`, not guessed zeros.

**R05-DIAG-04 — REQUIRED (bounded repeat/attribution).** A *new* real MySQL8 test run on the new exact HEAD with actor snapshots and post-termination third-reader facts is needed. Use no artificial sleeps/post-lock barriers or automatic attempts to force a winner. If repeated independently scheduled CI runs are requested, conduct a **bounded, predeclared series** (suggest three full 8-case runs, retaining every pass/fail Artifact) and judge **all runs**; one red run cannot be concealed behind later green runs. Only then propose a source-vs-instrumentation diagnosis. Any change to isolation level, lock wait, original SQL order, concurrent-worker count, underlying schema/indexes, retry, success predicate or six-file allowlist requires a **separately authorized design amendment** first.

## 3. Authorized targeted correction, still within original six files

**Decision: `AUTHORIZE_LIMITED_DIAGNOSTIC_EVIDENCE_REMEDIATION_ONLY`**, under PR #456 six-file authorization; in this action modify **only** already-added P1C test/observation/verifier/CI assets of PR #457, with no added seventh file, no production change or GitHub merge. Specifically:

1. Capture each worker’s **actual** command identity digest (not raw synthetic IDs or passwords), role, JDBC connection ID, isolation/lock timeout, begin/first-SQL/transaction-return/end timestamps, original method outcome, SQLState/vendorCode, successfully completed six-insert prefix and explicit transaction-call-returned flag **before any full-six assertion**. Never label `committed` based on the first INSERT.
2. After both workers actually terminate, open a separate SELECT-only physical Reader. **Capture both** effect identities' real six counts and canonical full-column digests (including all-zero/partial variants), reader role/connection ID, and any observed read exception; then emit the raw diagnostic case fact with `physicalSixTableSafety=UNVERIFIED_OR_FAILED` and `caseAcceptance=DIAGNOSTIC_ONLY` while the case is not yet adjudicated.
3. Independently evaluate full six-row identity coherence for each effect and actor-bound winner count. Only when **both** original Store transactions are confirmed as two separate `NEW_SUCCESS` and each effect has a verified complete six-table group may RACE-05 emit a **separate accepted attestation**. Do not encode `VERIFIED_BY_FRESH_READER` or success before the actual `full()` validations pass. If a `full()` assertion fails, keep prior diagnostic fact and fail JUnit; do not overwrite negative evidence with fabricated positive fields.
4. Keep raw diagnostics available on failure via current dedicated `.github/workflows/u07-d5-o2-stage-b-p1c.yml` `if: always()` Artifact upload, even when Maven fails and the stricter Python verifier does not run. If suitable within frozen six files, provide an explicit diagnostic validation mode that checks schema/case/run/head labels **without converting failure to success**; CI must still fail.
5. Capture a per-run RACE-05 fact before any assertion likely to interrupt physical reporting, preserve failing Surefire XML and all eight expected case names, and keep all safety assertions of RACE-01..06B unchanged. Abort if any worker remains live/timed out; never run the third-reader after an unbounded/unknown worker state. Never print CI credentials or unredacted DB strings.
6. Confirm later implementation exact diff against current main; only the six originally authorized P1C files may appear, nothing from P1-A/P1-B or production. Scope change => stop and return for review.

**Important nuance:** A diagnostic record of observed vectors `[0,0,0,0,0,0]` or partial values is allowed **as evidence of failure**, never as a business-safe final state. The other case facts and success validator remain evidence-only and must not be used to rewrite RACE-05's ground truth.

## 4. Findings, readiness and next gate

| Finding | Review decision |
| --- | --- |
| R05-DIAG-01 missing race05 diagnostic on failed run | **CONFIRMED BLOCKER; targeted evidence reorder authorized** |
| R05-DIAG-02 don't accept one-or-zero winners for independent effects | **SAFETY PREDICATE FROZEN: two committed coherent groups mandatory** |
| R05-DIAG-03 pass/verifier and fail/diagnostic separation | **REQUIRED; targeted test-only contract authorized** |
| R05-DIAG-04 physical reproducibility and attribution | **PENDING NEW EXACT-HEAD RUNS; no causal attribution yet** |
| P1C first CI vs latest CI | **PASS_8_INITIAL_HEAD / FAIL_1_LATEST_HEAD** |
| Production four-table replay logic | **CONFIRMED_NOT_SAFE; separately scoped fix required** |
| Main/PR merge state | **main unchanged; PR #457 Draft and unmerged** |

```text
MAIN_EXACT_HEAD = 20a54b4a7478641d8b8d8c12c6213b8e2ea73acc
PR457_CURRENT_HEAD = c45256d165b6aaf6d80eb3d6c233dab911515dd8
P1C_FROZEN_IMPLEMENTATION_SCOPE = SIX_ADDED_P1C_TEST_CI_FILES_ONLY
RACE05_LATEST_PHYSICAL_CI = FAIL_SIX_TABLE_AUTHORITY_ROW_ABSENT
RACE05_CAUSAL_ATTRIBUTION = NOT_ESTABLISHED
RACE05_FAILURE_EVIDENCE_LOSS = CONFIRMED
RACE05_DIAGNOSTIC_ONLY_CODE_AMENDMENT = AUTHORIZE_LIMITED
RACE05_SUCCESS_CRITERIA = FROZEN_TWO_NEW_SUCCESS_AND_TWO_FULL_GROUPS
RACE05_REPEAT_MYSQL_EVIDENCE = NOT_EXECUTED_ON_AMENDED_HEAD
PR457_IMPLEMENTATION_READINESS = NOT_READY
PR457_MERGE_AUTHORIZATION = NOT_GRANTED
B2_PRODUCTION_REPAIR_OR_WIRING = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next permitted step:** `D5-O2 Stage B — B1-P1-C PR #457 RACE-05 Diagnostic-First Evidence Emission Targeted Implementation + Bounded Real MySQL Reproduction`. Perform only evidence/diagnostic remediation within the existing six P1C test-only paths, rerun Java8/MySQL8, preserve each raw failing/passing Artifact, and then conduct exact-HEAD targeted implementation/evidence review. No direct merge, no altered production Store, no extra DDL/roles/retries, no patient data, no real delivery.
