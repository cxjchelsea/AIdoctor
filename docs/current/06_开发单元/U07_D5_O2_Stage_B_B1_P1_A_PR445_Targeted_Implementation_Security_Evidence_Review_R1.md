# U07 D5-O2 Stage B — B1-P1-A PR #445 Exact-HEAD Targeted Implementation / Security / Evidence Review R1

Date 2026-10-12. **Exact implementation PR #445 HEAD** `0a58b3f3d71838f095036acefafcd69955e68d39`, target `main=3b0589347d90af5ec5b86fbdad5eecabcce41a24`; PR is Draft and unmerged. Design-level B1-P1-A limited authorization from review PR #444. This is a same-assistant source/security/evidence review, **not a separately signed independent human review**, and not merge authorization.

## 1. Exact scope, physical CI, separately downloaded Artifact

- Exact `main..PR445` diff = **8 new files**, all within B1-P1-A permitted test-only list: `StageBP1TestConfig.java`, `StageBP1AtomicityTest.java`, `StageBP1ReplayConflictTest.java`, `StageBP1Evidence.java`, P1 `manifest.json`, P1 `bootstrap-p1-schema.py`, P1 `verify-p1-evidence.py` and `.github/workflows/u07-d5-o2-stage-b-p1.yml`. **Zero** production source, migration, old P0 tests/workflow or application changes.
- GitHub Actions **run #38219507200**, job #114712082452 = SUCCESS for PR #445 reviewed HEAD. Java8 test compile succeeded; disposable MySQL8 real JDBC tests **8 passed, 0 failed, 0 errors, 0 skipped** (six `fault01..06`, `happy01`, `replay01`). `STAGE_B_P1A_VERIFIED_CASES=8`. Same-head overlapping Stage A and P0 CI also completed successfully; these do not enlarge P1-A authority.
- Downloaded original Artifact **#11733812524**, ZIP 11,820 bytes, SHA256 **`9f667d86e0f65fdacebb64e42f31217c06c9d36e416260cc7c196e81e98e61e2`**; ZIP CRC validated. Contains two Surefire XML documents (6 + 2 named real methods) and `u07-d5o2-stage-b-p1a-evidence.json`. The evidence correctly distinguishes exact PR HEAD `0a58b3f3d71838f095036acefafcd69955e68d39` from GitHub synthetic merge checkout `795b1039321e9fe14095df78741fb0c3cef46480` and names run `38219507200`. Eight typed method/case facts are present. No known predefined synthetic DB password strings found in ZIP bytes (not a full secrets audit).
- For each N in 1..6, hook's actual `super.update(originalSql,args)` completes before injecting an unchecked `Injected(N)`; JUnit asserts the precise ordinal and `writes=N`. A separate in-flight SELECT-only session records six zero visible committed rows and distinct writer/reader connection IDs. After exception unwinds `TransactionTemplate`, a **new SELECT-only session** observes `sixAfter=[0,0,0,0,0,0]`. An unrelated healthy 6/6 group remains present. `happy01`: 6 original INSERTs then all six required records present. `replay01`: original source returns `replay=true`, instrumented `writes=0`, complete table group observed.
- Initial CI compile failure due missing Java checked SQLException declarations was corrected before the final successful exact-head CI; **do not claim first run succeeded**.

## 2. Findings and scope of what the tests actually prove

| Finding | Severity / disposition | Verified evidence / deficiency | Required remediation |
| --- | --- | --- | --- |
| **P1A-R1-01** | **BLOCKER — artifact authority/identity** | Java `StageBP1Evidence.complete()` physically checks six-table key/status/fingerprint coherence for `happy01`, `replay01`, and healthy controls, but the emitted JSON contains primarily **six count vectors**, connection numbers and labels. No persisted per-case **six-table identity graph digest** or strictly bound synthetic effect/command identity. The Python verifier only asserts counts plus self-reported verdicts; artifact bytes cannot independently attest the checks' full six-table identity graph. | Add deterministic canonical six-table row projection (including effect, delivery, authority, attempt, receipt, confirmation keys/status/fingerprints), canonical digest with domain separation and case binding; record protected synthetic identifiers or safe digests and verify digest presence, shape, equality/invariance. Tests must compute facts from actual SELECT-only readback; validator must reject absent, inconsistent or duplicated facts, never fabricate them. |
| **P1A-R1-02** | **BLOCKER — protected healthy control** | `StageBP1AtomicityTest.fault()` verifies healthy control `counts=[1...1]` and calls `complete()` after faults, but only compares row-count vectors before/after. A change to one or more valid-but-different values with same counts would not be proven absent by the exported evidence. | Read the full protected healthy six-table canonical projection before and after fault; assert **identical deterministic digest** and record both. In `verify-p1-evidence.py`, fail unless two nonempty valid hashes are equal; similarly bind actual candidate effect identity to rollback vectors. |
| **P1A-R1-03** | **REQUIRED — physical read-only permission** | P1 bootstrap grants `stagebp1_reader` SELECT only; tests open a separate reader and validate schema/engine, but do **not exercise a negative INSERT/UPDATE/DELETE using that P1 reader**. P0's privilege negative was performed with **different P0 role and database** and cannot be substituted as P1 physical attestation. A separate partial-state seeder is created with privileges but never used in A, consistent with scope. | In P1-A allowed test files, physically attempt and reject SELECT-only reader DML in disposable schema, assert username/current schema and failure SQL state; export a case-level fact and verify it. Do not alter grants to make tests pass. |
| **P1A-R1-04** | **REQUIRED — hook/input and verdict strictness** | `StageBP1Evidence.Hook.update()` validates ordered table prefix and a bound active Spring transaction; after `super.update` it injects at N. It does not enforce exact original statement text/allowed columns, so it is **instrumentation, not an SQL firewall**. `physicalTransactionResult=ROLLED_BACK_OBSERVED` is assigned after unit checks; the verifier does not independently bind SQL order, original target effect, observer IDs, isolation or positive control hashes. | Limit the positive claim to unchanged original source SQL observed by the test; tighten exact normalized six INSERT signature/column sequence and emitted trace facts, record observed isolation/manager-resource binding and source method outcome; add no-fault/unknown SQL negative if feasible within A. Never portray the test-only Hook as production SQL authorization. |
| **P1A-R1-05** | **ADVISORY — evidence provenance** | The verifier checks that synthetic PR merge checkout's second parent equals a workflow env-provided PR HEAD and checks source ancestry; 8 Surefire methods and matching test-emitted JSON are found. It does not bind each JSON observation to the current workflow/run/checkout itself (provenance is only added at bundle level); its method-to-class mapping relies on testcase name rather than requiring the exact expected class. | Require each test fact to carry case/class/run/PR-head or explicitly certify that bundle-level attestation is the only trust boundary, and check full class+method mapping. Preserve distinction between synthetic checkout SHA and PR-head SHA. |

## 3. Bounded verdict

```text
PR445_EXACT_DIFF_8_NEW_TEST_ONLY_FILES = PASS
PR445_JAVA8_MYSQL8_EXACT_HEAD_CI = PASS_8_TESTS
PR445_ARTIFACT_ZIP_READ_AND_INTEGRITY = PASS
P1A_N1_TO_N6_PHYSICAL_ROLLBACK = ACCEPTED_BOUNDED
P1A_HAPPY_AND_SEQUENTIAL_REPLAY = ACCEPTED_BOUNDED
P1A_FULL_IDENTITY_AND_HEALTHY_INTEGRITY_EVIDENCE = NOT_CLOSED
P1A_R1_01 = BLOCKER
P1A_R1_02 = BLOCKER
P1A_R1_03 = REQUIRED
P1A_R1_04 = REQUIRED
P1A_R1_05 = ADVISORY
P1A_IMPLEMENTATION_SECURITY_EVIDENCE_REVIEW = NOT_CLOSED
PR445_MERGE_READINESS = NOT_READY
PR445_MERGE_AUTHORIZATION = NOT_GRANTED
B1P1_B_PARTIAL_LEDGER = NOT_AUTHORIZED
B1P1_C_CONCURRENCY = NOT_AUTHORIZED
B1P2_B2_PRODUCTION_U06 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next:** `D5-O2 Stage B — B1-P1-A PR #445 R1-01..04 Targeted Evidence / Security Remediation`, within the **same eight already-authorized files**, with any extra path requiring new scope authorization. Then re-run exact HEAD Java8/MySQL8, independently download and check new Artifact, and perform a fresh targeted re-review. No production Store edits, six-table source repairs, concurrency/partial ledger expansion, merge or real delivery authority is implied.
