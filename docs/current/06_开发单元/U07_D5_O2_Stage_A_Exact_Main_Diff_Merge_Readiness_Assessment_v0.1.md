# U07 D5-O2 Stage A — PR #426 Exact-Main Diff and Merge Readiness Assessment v0.1

2026-10-12。remote main `435c456e7bf43601f0445f82932c3584fa1b8170`，对 PR #426 head `2defbde65a1cf36fd9dc957e0f63895f3fec5f4d`；主分支与此前冻结值相同。GitHub compare(main,head) = ahead, 19 commits, **exactly 9 added files**, zero changed/deleted production files. PR #426 open draft, merged=false, mergeable=true。PR #430 为复审文档 Draft，未合并。此文档只进行就绪评估，不实施合并。

## 1. Exact-main file inventory

1. `.github/workflows/u07-d5-o2-stage-a.yml` — 独立测试 CI，仅对 Stage A 指定路径的 PR 变更触发，最小 contents:read，MySQL 8 disposable service，固定 Java 8，上传 Surefire+provenance artifact。
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageAFixtureConfig.java` — test-only Spring/JPA manager、四实体/四 repository / fixed localhost datasource。
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATopologyTest.java` — static original-class negative + candidate bean/metamodel assertions。
4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATxPhysicalTest.java` — real MySQL single-owner commit/rollback + SELECT-only reader。
5. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageAMultiOwnerTest.java` — two-owner rollback, injected second repo failure, staged checkpoint commit evidence。
6. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageANegativeTest.java` — final CGLIB and mismatched manager negatives。
7. `diagnosis-service/src/test/resources/u07/d5o2/stage-a-manifest.json` — pinned original V2/V3/V6 schema provenance and UNKNOWN authority。
8. `tools/u07_d5_o2_tx_topology/bootstrap-stage-a-schema.py` — validates original DDL Git blobs, emits isolated MySQL bootstrap/least-privilege test roles, no live network calls in script。
9. `tools/u07_d5_o2_tx_topology/verify-stage-a-evidence.py` — validates 4 Surefire test classes, DDL digests, source ancestry, run ID and nonproduction authority; emits structured JSON。

No changes under `diagnosis-service/src/main`, legacy migration files, main pom, default application.yml, U06 authoritative workflow, production services, F8 integration, or patient/PHI stores. **No production runtime gate is lifted.**

## 2. Exact CI/evidence binding

Run #38207170535, job #114673108430 complete SUCCESS. Logs: Java8 test-compile SUCCESS; MySQL8 bootstrap SUCCESS; four JUnit classes total **10 run, 0 failures, 0 errors, 0 skips**. `STAGE_A_SYNTHETIC_MYSQL_TESTS_EXECUTED=10`. Artifact ID #11728160595, name `u07-d5-o2-stage-a-surefire`, 21,681 bytes, nonexpired. This is evidence for tested checkout head `2defbde65a1cf36fd9dc957e0f63895f3fec5f4d`, not hypothetical future merge commit. Artifact metadata verified, but ZIP contents were not independently downloaded and byte-hashed in this assessment; do not overclaim.

PR #430 R2 review accepted IA-01..06 only in nonprod candidate scope. Its continuing R2-01/02/03/04 findings are retained after possible merge.

## 3. Merge-readiness conditions and limits

- **Diff integrity: PASS** — exactly nine added test/fixture/tool/workflow files.
- **Original main SHA: PASS** — unchanged `435c456e7bf43601f0445f82932c3584fa1b8170` and HEAD `2defbde65a1cf36fd9dc957e0f63895f3fec5f4d`.
- **Scoped real CI: PASS** — ten real tests, success on exact head.
- **Bounded semantic review: ACCEPTED_WITH_RESIDUALS** — candidate uses explicit test-only `TransactionTemplate`, not production annotations/real clinical execution.
- **Workflow/disposable risk: ACCEPTABLE_ONLY_AS_CI_TEST_ASSET** — known static synthetic credentials and local loopback are specifically for disposable CI; do not run on a real/resident MySQL instance or reuse for actual patient data.
- **Approval prerequisites: PENDING** — GitHub PR remains Draft; no external reviewer permission/branch ruleset approvals are asserted. GitHub's mergeable flag is not merge authorization. Do not auto-mark Ready For Review or merge without explicit user authorization.
- **Post-merge verification: REQUIRED WHEN AUTHORIZED** — exact main head, nine-file scope, no unexpected CI/rules changes, standard merge commit, then verify merged main and scoped tests/metadata; don't auto-merge PR #430 or any other PR.
- **Production semantics: NOT_READY** — actual U06 Bean/proxy topology, physical lock races, crash/unknown commit, full legacy Flyway, Oracle, F8 APPLIED/resume and actual authority remain separate deliverables.

## 4. Formal decision

`PR426_EXACT_MAIN_DIFF_REVIEW = PASS`
`PR426_CI_EVIDENCE = PASS_10_TESTS`
`PR426_MERGE_READINESS = READY_FOR_SEPARATE_EXPLICIT_AUTHORIZATION_AS_TEST_ONLY_ASSET`
`PR426_MERGE_EXECUTION = NOT_AUTHORIZED_IN_THIS_STEP`
`STAGE_A_ISOLATED_PROBE = BOUNDED_EVIDENCE_ACCEPTED`
`STAGE_A_FULL_TOPOLOGY = NOT_READY`
`REAL_D5 = NOT_READY`

Next: request **explicit** merge authorization for **PR #426 only** (if the owner accepts limited-scope review), then rerun exact main-target HEAD/diff/CI/branch protection checks before doing a single standard merge commit. Do not merge any design/review Draft PR by implication. An independent human or separate reviewer has not signed off; review performed by same assistant.
