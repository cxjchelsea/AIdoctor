# U07 D5-O2 Stage B — Targeted Design Re-Review + B1 Implementation Readiness v0.2-R2

日期：2026-10-12。核对远程 `main=2834714354b02a88ccf73773511af706554cd9fb` 与 Stage B v0.2 PR #434 `HEAD=c9a53a4c48348724930ca93b3b1f17c0d02f2e32`。仅一份新增设计文件，PR Draft、未合并。审查 Stage B v0.1 PR #432、首次评审 PR #433 与 v0.2 修订；交叉检查 Stage A 已合入的 test fixture 和 U06 源码。**这是同一助手作的定向代码/设计复审，不是另一位独立人员签署的批准**。未执行 Stage B 新代码或真实 MySQL 测试。

## 1. 收敛结论与范围

- **方向批准，执行就绪不批准**：B0 只读源码与目标 Bean 构造归属调查 = `READY_FOR_READ_ONLY_ATTESTATION`；B1 `ISOLATED_EFFECTIVE_TX_PORT` 非生产方向 = `CONDITIONALLY_ACCEPTED`，但 `IMPLEMENTATION_READINESS=NOT_READY_WITH_TARGETED_FIXES`。
- B2 actual U06 target-runtime wiring 和真实 `@Transactional` advice = `UNVERIFIED/UNAUTHORIZED`。B1 的手工构造 final class + explicit Template 不能证明 B2。
- Stage A Java 8 / MySQL 8 10-pass evidence 可作为 test harness 机制参考，不可替代 Stage B 的新 head、新数据库六表与故障用例。
- main、生产 `src/main`、Flyway、Oracle、真实外部交付、F8 APPLIED 均未在本轮修改/激活。

## 2. 对 SB-R1-01..08 的再审

| 原 finding | v0.2 定向评估 | 原因和剩余门禁 |
| --- | --- | --- |
| SB-R1-01 | DESIGN_ACCEPTED | 三证据空间 B0 源码、B1 隔离事务、B2 生产接线清楚且 mutually nonauthoritative |
| SB-R1-02 | CONDITIONALLY_ACCEPTED / R2-B01 | 具名 Jpa/DSTx manager、同 DS 和串行短事务有设计，但双 manager 同一 thread 的资源 holder 清理及 `REQUIRES_NEW` 实际传播验证不足 |
| SB-R1-03 | DESIGN_ACCEPTED_WITH_REQUIRED_TESTS | S1/D1/S2/C1/I1/R1/A1/trace/candidate hash 的三态结果和原 mode2 返回差异写清，尚未实测 fault cuts |
| SB-R1-04 | ACCEPTED_AS_FAIL_CLOSED_SOURCE_CONTRACT | StateCommitter Backend 归属真实未知，明确 `BACKEND_OWNER_UNKNOWN` 时拒绝声称 end-to-end authority；须执行 B0 source search |
| SB-R1-05 | CONDITIONALLY_ACCEPTED / R2-B02 | 固定 D1 = JDBC DataSourceTxManager，六表顺序和 fail injections 设计良好；原方法可拦截点与 SQL 语义不改变仍需物理执行方案 |
| SB-R1-06 | CONDITIONALLY_ACCEPTED / R2-B03 | 文件、JUnit 类、编号和 SHA 合同已列，缺 method-id→真实 SQL 观察结果不可伪造的证据链与 fail-closed validator |
| SB-R1-07 | DESIGN_ACCEPTED | 明确限定 src/test & tools / 不扫描生产 / 保留 U06 RDP06 guard；运行时 CI 隔离必须实测 |
| SB-R1-08 | DESIGN_ACCEPTED_WITH_REQUIRED_TESTS | hash、Trace、WAIT_CHECKPOINTED 都不直接授予 F8 resume，需用失败恢复测试证明 |

## 3. 新发现（拟阻断 B1 整体授权）

### R2-B01 — HIGH / BLOCKER: 双管理器串行资源独立性需要进一步冻结

B1 明确 `stageBJdbcTransactionManager(DataSourceTransactionManager)` 与 `stageBJpaTransactionManager(JpaTransactionManager)` 共享 `stageBDataSource`，从同一线程按 stage 顺序执行。这并非理论上绝对错误：**在每个阶段都位于无 ambient transaction 的调用边界、之前 stage 的 connection holder 确实释放且独立 manager 的 JPA/SQL 资源不混用时**，可以作为测试候选。

目前的 guard 仅检查 `isActualTransactionActive()`，不足以拒绝 Spring `TransactionSynchronizationManager` 中残留的 `DataSource` / `EntityManagerFactory` bound resources、synchronization-active 或 misordered manager invocation。设计应在**每次 stage 开始前**拒绝 active transaction / active synchronization / any bound resource for candidate DS & EMF；stage 结束后校验 release/no leak，并验证：
- 先 D1 后 C1、先 C1 后 D1 都能得到**不同生命周期**的物理事务；没有跨 manager 同时绑定 DS。
- 故意让两个 manager 进入 nested/interleaved transaction，必须 fail closed。
- 名称匹配不够，manager→DS、JPA manager→EMF/DS、JDBC template→DS 要通过实际 bean identity + connection ID + SQL observer 验证。
- 不要仅用 `@@transaction_isolation` 判断**当前活跃事务**隔离级别而忽略设置发生的时序；同时验证 Spring 事务定义/requested isolation 与 JDBC Connection effective `getTransactionIsolation()`；JPA provider 可能有不同的获取路径。

**建议正式候选传播策略改为** `PROPAGATION_REQUIRED` 配合 `STRICT_NO_AMBIENT_TRANSACTION`，或保留 `REQUIRES_NEW` 但必须在入口完整禁止任何 ambient/bound resource，并在 test 中验证 suspend 不会发生。前者语义更不易误导且等价于无外层上下文情况下单 stage 新事务；应在实施设计中**二选一固定**，不可让代码自行选择。可选择 `REQUIRES_NEW + STRICT_NO_AMBIENT_AND_NO_BOUND_RESOURCES` 作为暂定兼容 v0.2。

### R2-B02 — HIGH / BLOCKER: D1 的逐 SQL 真正写后注入机制未达到可实施级

`JdbcU06DeliveryStore.reconcileConfirmed()` 为 final class，构造接收 `JdbcTemplate`，连续执行六条 `jdbc.update`。**不能用Mockito模拟“已完成第 n 条数据库写入”**。设计需新增仅测试层 `FaultInjectingJdbcTemplate` 或委托式 DataSource/JdbcOperations 适配层：让每个 `jdbc.update` 先真正通过原 SQL 写入 MySQL，再按 1..6 次计数抛故障。只允许对 exact six-table `INSERT INTO u06_delivery_*` 添加计数；查询/replay `SELECT` 不计数，不能在每个测试变更 SQL 或生产 store。错误发生后必须完成整个 TX 的 rollback，独立 reader 验证六张表均无本 effect 的新行，且不意外清理其他合法 effect。对注入后最后一次 SQL 后抛错也必须证实同一事务回滚。

**无隐式安全捷径**：若该 adapter 构造/override 受 `JdbcTemplate` API 限制，需先以一个 Java8 编译 proof 证实可行；否则先实施 test-only JDBC connection proxy 方案并经过定向复审。不可因为文档写了六个 case 就视为已实现。

### R2-B03 — MEDIUM / REQUIRED BEFORE EVIDENCE CLOSURE: 用例级权威证据尚缺桥接

测试矩阵编号已经列出，但 `verify-stage-b-evidence.py` 尚无机器可验证的每个 JUnit method 与 case ID 映射、每次测试 SQL before/after digest、read-only observer principal、TxOwnerBean/connection/rollback outcome 证据的完整生成与交叉验证流程。要求：
- 静态 expected-case manifest，每个独立 `B1-*` 与 `TEST-*.xml` testcase name 精确匹配；任意缺失、skip、failure、重复均 fail-closed；
- 记录测试读回的可校验 JSON 事实和同运行同 exact checkout SHA/DB/schema/run id；若原始输出缺少观察点则保留 `NOT_OBSERVED`，不能由脚本推定 PASS；
- artifact 均为 synthetic、没有 DB password/PHI/写账号 secret；尽量用单独 SELECT-only 角色观察；
- `UNKNOWN_COMMIT` 用例需要真实连接异常或明确降级为状态分类单元测试，不能通过 Mockito 造出 physical crash 标签。

### R2-B04 — MEDIUM / REQUIRED IN IMPLEMENTATION: 追踪与前置状态的权限边界需更精确

T1/T2 的候选 `READ_COMMITTED` 与主链 `SERIALIZABLE` 可能不同，允许只在局部 trace SQL、凭据/资源 whitelist 全匹配且真实隔离实测时使用；原 Trace.final 服务并无 `@Transactional` 生效证据。若 T1 启动记录失败，不能推断 S1 没发生，必须在原 `execute` 的实际调用位置注入；若 T2 失败，必须避免把已持久 WAIT 状态伪装为未建立。Trace 完整失败映射需保留源方法行为而非 B1 自行改 `U06ExecutionResult`。元数据 `WAIT_ESTABLISHED` 仅为有限 candidate；F8 authorization 一律 false。

## 4. B1 实施授权分段方案

- **B0-R (read only) SOURCE_WIRING_ATTESTATION**: `READY_FOR_READ_ONLY_ACTION`，查 U06 `createInjected()`、Backend implementors、Spring Bean/transaction manager/profile/调用者、production app startup profile evidence。缺运行期内容标 UNKNOWN，不启动生产服务。
- **B1-P0 SPIKE (test only)**: `CONDITIONALLY_READY`，仅新增隔离 `StageBTestConfig` + 最小 `StageBTxOwnerBindingTest`，只覆盖 D1/C1 manager resource identity、no ambient/DS resource guard、effective isolation、顺序双 manager、wrong-manager negative；以及一条 compiler-backed JdbcTemplate fault-hook proof。不开启全 U06 mode2，不声明 D1 六表完整通过。需单独实施授权。
- **B1-P1 FULL LOCAL TX PORT**: `NOT_READY`，必须先关闭 R2-B01/B02 的明确合同，再开始 D1 六表写后故障、C1/I1/R1/A1 故障。
- **B1-P2 CROSS-PHASE / EVIDENCE CLOSURE**: `NOT_READY`，必须完成 R2-B03/B04 + Java8/MySQL8 exact-head real CI、case-level evidence 复核。
- **B2 ACTUAL TARGET APPLICATION BEAN/PROFILE ACTIVATION**: `NOT_AUTHORIZED`，另立 Controlled Amendment。

P0 测试应沿用 Stage A 的 MySQL8 本地一次性测试库、严格固定端口/库/源 DDL、original schema-owner slice；至少 TX-01/02 positive&negative、D1 SQL hook Java8 compile proof、`DataSourceTransactionManager` 与 `JpaTransactionManager` 双 bean 实际释放证明；仅新建 `src/test` + `tools` + 独立 CI，不修改 Stage A 原已合并证据。无 runner evidence 不得写 `P0_PASS`。

## 5. Gate 结论

`STAGE_B_V02_EXACT_HEAD_REVIEW=COMPLETE`
`SB_R1_01_08_DESIGN_REMEDIATION=PARTIALLY_ACCEPTED_WITH_TARGETED_RESIDUALS`
`B0_SOURCE_WIRING_ATTESTATION=READY_FOR_READ_ONLY_ACTION`
`B1_P0_ISOLATED_TX_MANAGER_AND_D1_FAULT_HOOK_SPIKE=CONDITIONALLY_READY_FOR_SEPARATE_IMPLEMENTATION_AUTHORIZATION`
`B1_FULL_IMPLEMENTATION_READINESS=NOT_READY`
`B1_TARGETED_BLOCKERS=R2-B01,R2-B02`
`B1_EVIDENCE_CLOSURE_REQUIREMENTS=R2-B03,R2-B04`
`ACTUAL_U06_SPRING_PRODUCTION_WIRING=UNVERIFIED`
`PR434_MERGE_AUTHORIZATION=NOT_GRANTED`
`REAL_D5=NOT_READY`

**下一步推荐**：`D5-O2 Stage B — R2-B01/B02 Targeted Normative Design Fix + B1-P0 Execution Authorization Decision`。可以并行开展 B0 只读归属追踪，不能直接声明 Stage B 整体 implementation ready。新 Stage B 任何真正的 Java/MySQL PASS 需要新 exact HEAD CI 证据。
