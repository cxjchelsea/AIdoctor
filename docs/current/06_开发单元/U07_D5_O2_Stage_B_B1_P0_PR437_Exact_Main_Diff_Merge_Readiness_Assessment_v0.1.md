# U07 D5-O2 Stage B — B1-P0 PR #437 Exact-Main Diff / Merge Readiness Assessment v0.1

日期：2026-10-12。实际评估对象仅为 PR #437，目标 `main`。**本文件是合并就绪评估，不是合并授权，也不触发合并。**

## 1. 精确对象与防漂移

- Remote `main=2834714354b02a88ccf73773511af706554cd9fb`，与评审前冻结 baseline compare 为 `identical`。
- PR #437 exact HEAD `99b79a65ad4561f332e3c2440d73b9f4fbfe56d0`，base SHA 为上述 main。GitHub PR = `open`、`draft=true`、`merged=false`、`mergeable=true`、REST `mergeable_state=clean`。已有 22 次提交；判定使用精确 **最终树差异**，不是仅看单一最后提交。
- `compare(main, PR HEAD)` = `ahead`；总共 **8 个文件，全部 status=added，695 additions / 0 deletions**；GitHub PR changed_files=8，与 exact compare 一致。无任何 `src/main`、`application.yml`、Flyway、Oracle、旧 U06 CI 或原 Stage A 文件的变更。
- 当前 GitHub PR review API 返回空列表；同助手提供的设计/实施 R2 review PR #439 不等于 GitHub 平台正式 reviewer approval 或第三方独立安全审查。

### 精确文件清单（均为 ADD）

1. `.github/workflows/u07-d5-o2-stage-b-p0.yml`
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBJdbcFaultHookTest.java`
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBPostWriteFaultJdbcTemplate.java`
4. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBTestConfig.java`
5. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBTxOwnerBindingTest.java`
6. `diagnosis-service/src/test/resources/u07/d5o2/stageb/p0-manifest.json`
7. `tools/u07_d5_o2_tx_topology/stageb/bootstrap-p0-schema.py`
8. `tools/u07_d5_o2_tx_topology/stageb/verify-p0-evidence.py`

## 2. 证据门禁

- Exact-head Actions run #38213805574 `U07 D5-O2 Stage B P0 isolated JPA JDBC fault-hook`: **success**, job #114693750953。Java 8 `test-compile` + real disposable MySQL 8 targeted tests: **5 tests; failures=0; errors=0; skipped=0**, verifier emits `STAGE_B_P0_TESTS_EXECUTED=5`。因 path 范围交叠，Stage A workflow run #38213805438 同 HEAD 也 success；Stage A 成功不提高 B1-P0 或生产授权。
- Artifact #11730243812 `u07-d5-o2-stage-b-p0-surefire`, 11,926 bytes, `expired=false`；已独立下载 ZIP，SHA256 `4b3ab1afd4a3f888afe8dd001ed8e3941a23f1a9d0ea53849b583ed1d57ea2f1`。ZIP 内 2 份 `TEST-*.xml`（2 + 3 methods）和 1 份 `u07-d5o2-stage-b-p0-evidence.json`；具名观察覆盖 5 methods，`actualPrHeadSha=99b79a65...`、`workflowRunId=38213805574`，`testedCheckoutSha=289cbd42...` 是 PR synthetic **merge checkout SHA**，不是 PR HEAD；脚本将 HEAD 的第二父节点绑定至用户提交 HEAD，身份角色未混淆。
- `freshObserverAfterRollback=true`、`postRollbackSumSixTables=0`；测试与 SQL fault hook 仅验证 D1 首次已写 SQL 后 fault，隔离 TX 回滚、另一只读连接独立核验和六表无新行；未覆盖六个故障序号完整矩阵、真实 crash、未知提交。
- Review PR #439 `U07_D5_O2_Stage_B_B1_P0_Targeted_Security_Evidence_ReReview_R2.md` 接受 P0-R1-01～05 在 **test-only** 范围的有限收口；它由同一助手执行，不是独立人员签署的批准。

## 3. 安全/合并条件检查

| Gate | 当前证据 | 判定 |
| --- | --- | --- |
| main 精确未漂移 | `283471...` unchanged | PASS |
| PR exact HEAD/base | `99b79a...` / `283471...` | PASS |
| 变更限制 | 8 new test-only/workflow/tools files | PASS |
| Exact-head Stage B P0 CI | real Java 8 / MySQL 8 5/0/0/0 | PASS |
| Artifact 原始 ZIP/用例级证据 | #11730243812, SHA256 pinned, XML+JSON | PASS_BOUNDED |
| GitHub 合并冲突 | `mergeable=true`, `mergeable_state=clean` | PASS_CURRENT |
| PR 目前为 Draft | `draft=true` | **PRE-MERGE ACTION REQUIRED** |
| GitHub PR reviews | currently none | NO_FORMAL_REVIEW_ON_GITHUB |
| Branch protection | administration endpoint GET returned **403 Resource not accessible by integration** | **NOT_ATTESTED** |
| GitHub repository rulesets | public `/rulesets?includes_parents=true` returned `[]`，不等于完整 branch protection 可读 | NO_VISIBLE_RULESET; BRANCH_PROTECTION_UNKNOWN |
| 明确用户合并授权 | 本步骤未提供 | **NOT_GRANTED** |

`mergeable=true` 并不自动允许 Draft PR 执行合并，亦不取代必要审批。用户若以后单独授权合并 #437，仍须重新核验精确 main HEAD/base、八文件差异、最新 CI 和平台限制；必要时只能将 #437 标记 Ready for review 后按 GitHub 保护规则执行，若需要正式批准则暂停，不可绕过。

## 4. 严格保留的范围边界

本 PR 仅作为 **U07 D5-O2 Stage B B1-P0 非生产验证资产**：测试目录手动 Spring 最小上下文、test-only manager resource guard、`JdbcTemplate` 六表原始 INSERT 的 **N=1** 故障探针、一次性 MySQL8/Java8 CI、可审计的源头/运行证据。

**不意味着**：原 U06 final service 的 `@Transactional` 获得真实 Spring AOP advice、实际生产 Bean wiring 生效、S1/S2 StateCommitter backend 与 CDP TxManager 已确认；不授权 B1-P1 六个故障序号/幂等/并发，B1-P2 跨阶段恢复、B2 生产接线、真实临床/PHI、Oracle、F8 APPLIED、外部投递或部署。`REAL_D5=NOT_READY`。

## 5. 正式合并就绪评估结论

```text
PR437_EXACT_MAIN_DIFF_REVIEW = PASS
PR437_EXACT_HEAD_CI = PASS_5_TESTS
PR437_ARTIFACT_EVIDENCE = PASS_BOUNDED
PR437_SCOPE = TEST_ONLY_B1_P0
PR437_MERGE_READINESS = READY_FOR_SEPARATE_EXPLICIT_AUTHORIZATION_CONDITIONAL_ON_PLATFORM_GATES
PR437_MERGE_EXECUTION = NOT_AUTHORIZED
B1_P0_ISOLATED_SECURITY_RE_REVIEW = ACCEPTED_WITH_SCOPE_LIMIT
B1_P1_P2 = NOT_AUTHORIZED
B2_ACTUAL_PRODUCTION_WIRING = UNVERIFIED
REAL_D5 = NOT_READY
```

**下一步：** 申请用户单独明确的 `AUTHORIZE MERGE #437`（仅此 PR）；得到授权后先复核全部事实、处理 Draft→Ready for review、遵守未知/可见 GitHub 分支保护，并仅采用 **standard merge commit** 合并此 PR，完成 main 侧 post-merge verification。**本文件与其 Review PR 不允许一并合并。**
