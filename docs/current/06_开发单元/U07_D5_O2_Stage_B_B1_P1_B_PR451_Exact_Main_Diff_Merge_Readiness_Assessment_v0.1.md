# U07 D5-O2 Stage B — B1-P1-B PR #451 Exact-Main Diff / Merge Readiness Assessment v0.1

Date: 2026-10-12. **Readiness-only decision; NOT merge authorization.** This is a same-assistant technical assessment, not a separately approved GitHub code review nor an independent human safety sign-off.

## 1. Exact identities, main target, and authorized file boundary

- The remote `main` matches exact SHA `e7562bd9b5390c85e14c3c01424c1004b1479ebc` with no drift.
- Implementation Draft [PR #451](https://github.com/cxjchelsea/AIdoctor/pull/451) has exact HEAD `dbbc9b072d7cee1fd03f675fe2567bbb1e512304`, base `e7562bd9b5390c85e14c3c01424c1004b1479ebc`, `draft=true`, `merged=false`.
- GitHub compare main..PR HEAD shows exactly **7 ADDED files, 980 added lines, 0 deletions**; no existing files modified. All paths are precisely those allowed under the isolated test-only limited implementation authority in design-review Draft PR #450. All seven are:
  1. `.github/workflows/u07-d5-o2-stage-b-p1b.yml`
  2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BEvidence.java`
  3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BPartialLedgerTest.java`
  4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BReplayConflictTest.java`
  5. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1b/StageBP1BTestConfig.java`
  6. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p1b/manifest.json`
  7. `tools/u07_d5_o2_tx_topology/stageb/p1b/bootstrap-and-verify.py`
- No main-source `JdbcU06DeliveryStore` changes, Flyway migrations, app configurations, original merged P1-A tests/workflow, production routing or external delivery. This is a **test asset integration**, not a fix or production authority.

## 2. Bound physical test and artifact evidence

- Exact-head [Actions run #38229149457](https://github.com/cxjchelsea/AIdoctor/actions/runs/38229149457), Java 8 + disposable MySQL 8, **SUCCESS**, Surefire **15 passed / 0 failures / 0 errors / 0 skipped**: six artificial partial-ledger source-characterization cases, nine sequential replay-conflict cases. Strict test-evidence validator emitted `STAGE_B_P1B_VERIFIED_CASES=15` and `P1B_FOUR_TABLE_SOURCE_GAP=CONFIRMED_AND_NOT_SAFE`.
- Same reviewed HEAD Stage A run #38229149367 and Stage B P0 run #38229149379 both independently returned `success`; do not transfer their approval to other development units.
- Artifact **#11737977225**, name `u07-d5-o2-stage-b-p1b-evidence`, `expired=false`, GitHub digest `sha256:34e0213d8a379043a355f5c5f3178c31000a9338f213dbb21a4665a8a97a0ac1`. Original downloaded ZIP, CRC, XML and 15 typed observations were independently checked in the preceding R2 evidence re-review. This current assessment re-checked artifact metadata and exact HEAD-to-run binding; it does not claim a new MySQL execution or separate independent security auditor.
- [Draft PR #453](https://github.com/cxjchelsea/AIdoctor/pull/453), R2 exact-HEAD targeted evidence/security assessment, accepted **in isolated test-only scope**, closing R1-01 post-cleanup physical readback/digest, R1-02 bounded physical role/privilege proof, R1-03 exact six ordered SQL source signatures, and R1-04 separation of source behavior from business-safety failure. R1-05 verifier mutation-negative testing remains nonblocking advisory; R2 is a **same-assistant review document**, not an actual formal GitHub approval.

## 3. Source defect is explicitly NOT resolved

The unchanged `JdbcU06DeliveryStore.find()` performs a four-way INNER JOIN (ledger+authority+intent+confirmation) and omits attempt+receipt. In a deliberately seeded committed five-row record missing Attempt or Receipt, the original Store returns `replay=true` despite an incomplete physical six-table group. B1-P1-B correctly records `SOURCE_GAP_CONFIRMED`, `fullSixCoherentObserved=false`, and `businessReplaySafetyVerdict=FAIL_NOT_ESTABLISHED`.

This is a **confirmed unsafe source behavior** but **not a blocker to merging this bounded nonproduction test suite as a regression/source-characterization asset**, provided descriptions, review and merge comments preserve the NOT_SAFE finding and no production safety claim is made. The defect **is** a blocker to any future claim of full-six replay safety, B2 production wiring, REAL_D5 or external delivery activation. A separate source remediation scope/authorization would be needed.

## 4. GitHub platform and execution prerequisites

| Gate | Current observed state | Decision |
| --- | --- | --- |
| Remote main exact SHA | `e7562bd9b5390c85e14c3c01424c1004b1479ebc` unchanged | PASS |
| Implementation PR #451 exact HEAD | `dbbc9b072d7cee1fd03f675fe2567bbb1e512304` | PASS |
| Authorized main-target diff | Exactly seven ADD-only test/CI files; 980 additions, no deletions | PASS |
| Physical real Java8/MySQL8 | 15 passed, zero failing/errored/skipped, run successful | PASS |
| Artifact and R2 re-review | Existing original ZIP independently reviewed, R2 accepted bounded | PASS_BOUNDED |
| GitHub current mergeability | `mergeable=true`, `mergeable_state=clean` | CURRENTLY_CLEAN_NOT_AN_AUTHORIZATION |
| Draft status | PR #451 `draft=true` | **REQUIRES READY-FOR-REVIEW** |
| GitHub PR reviews | **0** formal submitted reviews returned | **NO_FORMAL_APPROVAL_OBSERVED** |
| Repository rulesets | API lists `[]` to this integration | NO_VISIBLE_RULESETS; NOT a guarantee of no required protection |
| main branch protection | `GET /branches/main/protection` returned **403 Resource not accessible by integration** | **NOT_ATTESTED; CHECK BEFORE MERGE** |
| Explicit user merge authorization for #451 | Only asked for readiness review, not merge | **NOT_GRANTED** |

No execution should infer approval from an empty ruleset response, green CI, or `mergeable_state=clean`. For any later user-authorized merge: re-read the exact current main and PR head and required GitHub checks, verify all seven paths and CI have not changed, mark #451 Ready for review if appropriate, satisfy any required approvals/branch protections instead of bypassing them, and **merge only PR #451 using standard merge commit**. Then independently verify merge parents, exact eight? **No: seven** newly added test-only files, final main and post-merge results. Do not merge related design/review PRs (#449/#450/#452/#453 or this assessment) without separate approval.

## 5. Decision

```text
PR451_EXACT_MAIN_DIFF = PASS_7_ADDED_TEST_ONLY_FILES
PR451_HEAD_JAVA8_MYSQL8_CI = PASS_15
PR451_ARTIFACT_EVIDENCE = PASS_BOUNDED
PR451_R2_SECURITY_EVIDENCE_RE_REVIEW = ACCEPTED_IN_ISOLATED_SCOPE
PR451_SOURCE_GAP = CONFIRMED_NOT_SAFE
PR451_MERGE_READINESS = READY_FOR_SEPARATE_EXPLICIT_AUTHORIZATION_CONDITIONAL_ON_PLATFORM_GATES
PR451_MERGE_AUTHORIZATION = NOT_GRANTED
PR451_MERGE_EXECUTION = NOT_PERFORMED
B1P1C_CONCURRENCY = NOT_AUTHORIZED
B2_PRODUCTION_U06_F8 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next:** a separate request `AUTHORIZE MERGE #451` if the user wishes to integrate the **nonproduction diagnostic tests** only. An explicit permission is not a waiver of GitHub platform conditions; failure to verify or satisfy required branch rules must stop execution. No production Store repair, patient/PHI, external delivery, F8 or REAL_D5 activation is included.
