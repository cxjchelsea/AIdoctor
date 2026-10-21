# U07 D5-O2 Stage A — IA-01..06 Targeted Implementation / Evidence Re-Review v0.2

日期：2026-10-12。精确 main `435c456e7bf43601f0445f82932c3584fa1b8170`，PR #426 HEAD `2defbde65a1cf36fd9dc957e0f63895f3fec5f4d`，Draft、未合并。本次针对 PR #427 首次实现评审后六项整改，重新检查九文件差异、关键 Java 测试和证据脚本、GitHub Actions Run #38207170535 / job #114673108430。此复审由同一助手完成，非独立人员签署。

## 1. 已验证的真实范围

- CI run SUCCESS，Java8 compile SUCCESS；MySQL8 disposable DB 初始化 SUCCESS；JUnit 总数 **10 tests, 0 fail, 0 errors, 0 skipped**。
- TopologyTest 2 PASS：反射静态 final 负控 + 测试专用 Spring context 的 4 个 JPA entities/4 个 repositories/1 manager/1 EMF。
- TxPhysicalTest 2 PASS：SELECT-only reader 拒绝 DELETE，真实 MySQL JPA/JDBC 事务下未提交不可见、rollback 后不可见、commit 后可见；不同连接身份断言。
- MultiOwnerTest 4 PASS：Consultation + WaitEffect、Checkpoint + Thread 的外层 TransactionTemplate 回滚；第二 repository Mockito 故障注入；提交后 WAIT_CHECKPOINTED 可读。
- NegativeTest 2 PASS：final concrete service CGLIB 拒绝；wrong DataSourceTransactionManager/expected datasource mismatch fail-closed。
- 成功 CI 运行了 verify-stage-a-evidence.py，并上传 Artifact #11728160595，名字 u07-d5-o2-stage-a-surefire，21,681 bytes，未过期。仅验证了运行日志与 Artifact 元数据；**本轮没有独立下载并逐文件核对 Artifact ZIP bytes**。

## 2. IA-01..06 重新判定

| Finding | 判定 | 限定条件 |
| --- | --- | --- |
| IA-01 | TARGETED_COVERAGE_ACCEPTED | 双 Owner 回滚、第二 repo fail、WAIT_CHECKPOINTED 实测；是 test-only outer Template + Mockito 软件故障，不是生产代理、真实断网或 crash |
| IA-02 | TARGETED_NEGATIVES_ACCEPTED | final CGLIB 与 wrong manager 分支 PASS；不代表 JDK proxy 或生产 Bean graph 已验证 |
| IA-03 | ACCEPTED_FOR_TEST_SLICE | SELECT-only reader、DML denial、不同 MySQL connection ID；单次 ID 并非全生命周期连接跟踪 |
| IA-04 | BASELINE_EVIDENCE_ACCEPTED_WITH_RESIDUAL | 原 main/actual HEAD、DDL Git blobs/SHA256、JUnit 四类计数、UNKNOWN authority、run ID 由脚本编制；未生成每个断言的结构化 tx/connection/fault 事实，artifact 未独立重算 |
| IA-05 | CI_NONPROD_BOUNDARY_ACCEPTED_WITH_RESIDUAL | loopback 固定库、MySQL8/schema/user/InnoDB、最小权限和 manifest；环境变量 GITHUB_ACTIONS=true 可以模拟，离开 disposable runner 不具有可靠物理隔离证明 |
| IA-06 | ACCEPTED_FOR_CURRENT_RUN | Actions 固定 SHA，YAML 修复后正常启动，Surefire+JSON Artifact 上传成功；未重新计算 ZIP 字节 digest |

## 3. 保留发现及风险边界

- R2-01 HIGH / FOLLOW-ON：U06 生产 final classes 的原始 Spring Bean 注册、JDK/CGLIB proxy 与 @Transactional 方法级 advice 未被本候选证明。显式 TransactionTemplate 包裹不能冒充生产原类 advice 生效。
- R2-02 HIGH / FOLLOW-ON：真实双连接锁争用、版本冲突、阶段间崩溃、commit unknown / 网络断链都未完成。Mockito 第二 Repository 故障是软件注入，不能当作 MySQL物理故障。
- R2-03 MEDIUM / EVIDENCE：聚合 JSON 只有 class-level test counts 和 DDL/run 来源，缺每个 case 的事务/连接身份观察，且 Artifact ZIP 未被独立读出校验。
- R2-04 MEDIUM / ENVIRONMENT：固定本地端口和 GITHUB_ACTIONS 字符串不是不可伪造的 ephemeral MySQL 保证；仅接受当前成功 CI 的 disposable service 证据。
- Out of scope：Oracle、完整 V1→V6 Flyway 历史、真实 PHI/clinical delivery、F8 APPLIED/resume/production authority。

## 4. Gate 判定

- PR426_EXACT_HEAD_CI = PASS_10_TESTS
- IA01_IA06_TARGETED_REMEDIATION = ACCEPTED_IN_BOUNDED_ISOLATED_SCOPE
- STAGE_A_ISOLATED_PROBE = EVIDENCE_ACCEPTED_FOR_TEST_ONLY_CANDIDATE
- STAGE_A_FULL_TOPOLOGY_CLOSURE = NOT_READY
- PRODUCTION_SPRING_TOPOLOGY = UNVERIFIED
- PR426_MERGE_AUTHORIZATION = NOT_GRANTED_BY_THIS_REVIEW
- REAL_D5 = NOT_READY

下一步：D5-O2 Stage A — Exact-Main Diff / Merge Readiness Assessment。仅评估是否可把 PR #426 作为不带生产 authority 的 nonprod test asset 合并；如需实际合并，必须另行明确授权。R2-01/02 属于后续真实拓扑 / 并发 / 故障验证，不得因该 PR 合并而消失。