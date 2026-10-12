# U07 D5-O2 Stage B — B1-P1-A PR #445 Exact-Main Diff / Merge Readiness Assessment v0.1

Date: 2026-10-12. Target: PR #445 only. **Review/readiness assessment, not a merge instruction or authorization.** This is same-assistant review, not independently signed human approval.

## Exact sources and frozen identity

- Remote exact target `main=3b0589347d90af5ec5b86fbdad5eecabcce41a24`, confirmed unchanged by an exact baseline compare.
- PR #445 exact head `abc7c9c70adf128dab0041364697128a8dda8db2`, base `3b0589347d90af5ec5b86fbdad5eecabcce41a24`. Currently `open`, `draft=true`, `merged=false`.
- GitHub pull API: `mergeable=true` and `mergeable_state=clean`; that describes code merge/conflict feasibility **not permission** to merge a Draft PR.
- Compare `main..PR HEAD`: eight **ADDED** files, **705 additions / 0 deletions**; files listed below. No source modifications, deletion, `src/main` code, Flyway, production configuration, existing P0/Stage A files or delivery wiring.

### Exact eight-file scope

1. `.github/workflows/u07-d5-o2-stage-b-p1.yml`
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1AtomicityTest.java`
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1Evidence.java`
4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1ReplayConflictTest.java`
5. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/p1/StageBP1TestConfig.java`
6. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p1/manifest.json`
7. `tools/u07_d5_o2_tx_topology/stageb/p1/bootstrap-p1-schema.py`
8. `tools/u07_d5_o2_tx_topology/stageb/p1/verify-p1-evidence.py`

## Tests and evidence attached to precisely that HEAD

- Exact-head Java 8 / real disposable MySQL 8 job: [Actions run #38223135077](https://github.com/cxjchelsea/AIdoctor/actions/runs/38223135077), **success**. Original Surefire: six SQL after-write rollback methods + two clean commit/sequential replay methods = **8 passed, 0 failed, 0 errored, 0 skipped**. CI verifier `STAGE_B_P1A_VERIFIED_CASES=8`; no test evidence inferred merely from test names.
- Same-head overlapping existing Stage A [run #38223135207] and Stage B P0 [run #38223135095] both returned `success`. Neither authorizes P1-B/C or production.
- Artifact `11734952822` (`u07-d5-o2-stage-b-p1a-surefire`, ZIP 12,912 bytes, `expired=false`), independently downloaded and byte/ZIP-CRC inspected in the R2 re-review; SHA-256 `4fbcafa443497be1e135bbe9eec093e8a3b47a33401449adeeac31bbc2a5896b`. Artifact includes two Surefire XML (6+2) and machine-validated case-level JSON. As documented in review PR #447, evidence includes full six-table graph SHA-256 from SELECT-only readback, invariant protected healthy graph before/after each fault, exact ordered SQL signatures, physical P1 SELECT-only DELETE denial, bound JUnit/class/case/run identifiers and accurate PR-head vs GitHub synthetic merge checkout separation. This is test-runner trust-bounded evidence, not independently signed production DB audit.
- Security/evidence targeted re-review [PR #447](https://github.com/cxjchelsea/AIdoctor/pull/447), HEAD `8e9f20a6f79e7eef88bfc3b0734dc1fa60d32b15`, concluded `ACCEPTED_IN_ISOLATED_SCOPE` and R1-01..04 closed within bounded test-only scope. **This is a same-assistant technical assessment, not formal GitHub PR review nor an independent human approval.**

## Platform and authorization gates

| Check | Observed | Assessment |
| --- | --- | --- |
| Main exact SHA | `3b058934...` unchanged | PASS |
| PR #445 exact branch HEAD | `abc7c9c7...` | PASS |
| Eight-file test-only diff | exactly eight ADD, no production/migration | PASS |
| Exact-head P1-A physical Java8/MySQL8 | 8/0/0/0, run success | PASS |
| Evidence / R2 re-review | ZIP byte+case evidence checked; accepted bounded | PASS_BOUNDED |
| GitHub mergeability | true / clean | PASS_CURRENT |
| Draft state | `draft=true` | **PRE-MERGE ACTION REQUIRED** |
| Formal GitHub PR reviews | no reviews returned | **NO_FORMAL_GITHUB_APPROVAL_AT_CHECK** |
| Rulesets listing | `GET /rulesets?includes_parents=true` returned empty list | NO_VISIBLE_RULESETS |
| Branch protection | `GET /branches/main/protection` returned **403 Resource not accessible by integration** | **NOT_ATTESTED** |
| User permission to actually merge #445 | not granted by request for assessment | **NOT_AUTHORIZED** |

No claim that empty visible rulesets proves there is no branch protection. `mergeable_state=clean` does not prove platform approvals or protected requirements will permit an actual merge. User may later explicitly grant permission specifically to merge **only** PR #445. At execution time, independently re-read main and PR exact heads, eight-file scope, current CI and branch rules. Mark Draft Ready for review if otherwise authorized; **stop** if required approvals/checks cannot be verified or satisfied, rather than bypassing policy. Use **standard merge commit**, never squash/rebase, only after checks and explicit authorization; verify merge parent ordering, exact final main tree and post-merge status afterward. The review/docs PRs are **not** included.

## Safety boundary / exclusions

**B1-P1-A = a nonproduction disposable MySQL8/Java8 test asset**, executing original `final JdbcU06DeliveryStore` under caller-owned explicit JDBC transaction. Its `@Transactional` annotation is **not** shown to be effective via production Spring AOP. It demonstrates exactly N=1..6 post-write fault rollback, six-table clean commit and identical sequential replay in isolated conditions, not partial-ledger or concurrent races. P1-B partial-ledger source characterization and P1-C concurrency remain **NOT_AUTHORIZED**. No UNKNOWN_COMMIT or physical crash/recovery attestation; no StateCommitter clinical/patient data, external transport, production MySQL, Oracle, F8 APPLIED/RESUME, B2 actual Spring wiring, or deployment. `REAL_D5=NOT_READY`.

## Formal merge-readiness decision

```text
PR445_EXACT_MAIN_DIFF_REVIEW = PASS
PR445_EXACT_HEAD_CI = PASS_8_REAL_JAVA8_MYSQL8
PR445_ARTIFACT = PASS_BOUNDED
B1_P1_A_TARGETED_SECURITY_EVIDENCE_RE_REVIEW = ACCEPTED_IN_ISOLATED_SCOPE
PR445_MERGE_READINESS = READY_FOR_SEPARATE_EXPLICIT_AUTHORIZATION_CONDITIONAL_ON_PLATFORM_GATES
PR445_MERGE_AUTHORIZATION = NOT_GRANTED
PR445_MERGE_EXECUTION = NOT_PERFORMED
B1_P1_B_AND_C = NOT_AUTHORIZED
B1_P2_B2 = NOT_AUTHORIZED
REAL_D5 = NOT_READY
```

**Next**: ask the user for a **separate exact permission**, e.g. `AUTHORIZE MERGE #445`, with current named status understood. After explicit permission, recheck exact identities and conditions; make PR Ready for review and only then, if platform gates are met, use standard merge commit to merge **PR #445 only**, followed by exact main post-merge checks. This assessment neither grants that permission nor enables production routing.
