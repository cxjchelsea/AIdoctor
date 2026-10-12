# U07 D5-O2 — Actual Spring Transaction Topology & Partial-Commit Source Audit v0.2

日期：2026-10-12。精确远程 main：`435c456e7bf43601f0445f82932c3584fa1b8170`（与本轮远程 main 比较 identical）。本文件为源码静态审计、拓扑证据缺口及设计整改建议；**没有**启动生产 Spring Context、MySQL/Oracle、执行 D5-O2 测试或认证生产环境。D5-O1 的隔离真实 MySQL 测试不能充当本项事务拓扑证明。REAL_D5=NOT_READY；D5-O2_IMPLEMENTATION=NOT_READY。

## 1. 检查范围与事实来源

检查 `diagnosis-service` 运行源代码及现有测试：`DiagnosisServiceApplication`、`pom.xml`、`application.yml`、四个 `config` 类、`U06ProfileBApplicationService`、`U06WaitCoordinator`、`ConsultationWaitTransitionService`、`RuntimeWaitCheckpointService`、`RuntimeThreadWaitTransitionService`、`JdbcU06DeliveryStore`、`U06SyntheticDeliveryService`、`JdbcU06GovernedExecutionTraceStore`、`U06SyntheticP01Runtime`、`ClinicalCdpStateRepositoryAdapter`、`RuntimeBindingService`、JPA repositories、`U06Rdp06AuthoritativeObservationTest`、MySQL V2/V3/V6 及 Oracle V6。目录检查依据 diagnosis-service Git tree recursive API，不是以代码搜索的零结果替代全仓证明。外部运行配置、远程部署和未读服务仍是 UNKNOWN。

## 2. Spring Bean / proxy / resource inventory

| 对象 | main 源码证据 | 静态判定 |
| --- | --- | --- |
| `DiagnosisServiceApplication` | `@SpringBootApplication`, `@EnableJpaAuditing`；未显式声明 D5 writer / TxManager | 依赖 Boot auto-configuration 或外部配置；实际 Bean 图未知 |
| `ClinicalCdpStateRepositoryAdapter` | `@Component`、非 final 类、`attemptAtomicCommit @Transactional` | 具有可被 Spring 代理的源码条件；实际代理/TxManager 未实测 |
| `RuntimeBindingService` | `@Service`、`bind @Transactional` | 同上；不是 U06 等待链共同事务的证明 |
| `ConsultationWaitTransitionService` | `public final class`，非 `@Service/@Component`，`establish @Transactional(SERIALIZABLE)` | 无已核验生产 Bean 注册；若普通 `new` 不经 Spring advice。final 类亦不支持 CGLIB 子类代理；需明确替代方案 |
| `RuntimeWaitCheckpointService` | `public final class`；`initialize` 无注解；`reserve @Transactional(SERIALIZABLE)` | 同上；initialize 可能走单独 repository 事务 |
| `RuntimeThreadWaitTransitionService` | `public final class`；`enterAwaitingUser @Transactional(SERIALIZABLE)` | 同上 |
| `U06WaitCoordinator` | `public final class`；`establish` 无事务注解 | 顺序执行 initialize → reserve → enterAwaitingUser → hash projector，无共同事务声明 |
| `U06ProfileBApplicationService` | `public final class`；`execute/mode2` 无事务注解；测试显式 `new` 构建 | 现有测试不是生产 Spring AOP/manager 认证 |
| `JdbcU06DeliveryStore` | `public final class`，非组件；`reconcileConfirmed @Transactional(SERIALIZABLE)` | 未建立实际代理，若无外层事务，多条 JDBC update 不能仅靠该注解认定原子 |
| `JdbcU06GovernedExecutionTraceStore` | 普通 final 类，`start/complete` 无事务注解 | 若直接执行，trace start 与 terminalization 分阶段；与 wait 不承诺原子 |
| `U06SyntheticP01Runtime` | `createInjected` 手工建 StateCommitter；Idem 内存 Map；Events 空方法 | 不提供耐久 APPLIED / 全局幂等证据 |

补充：Spring Data JPA repository 的自身事务/查询语义不应等同于业务 service 级完整事务。特定 `@Lock(PESSIMISTIC_WRITE)` 查询若在无有效事务上下文中调用，可能直接因事务前提不满足而失败；不应把所有理论阶段都说成已能在当前无代理路径下成功执行。Spring `@Transactional` 经 proxy 生效时还须确认内部调用、自调用、外层传播规则及具体 bean 名称。

## 3. 源码可见业务时序（非认证的生产拓扑）

`U06ProfileBApplicationService.execute()`：
1. `trace.start`。
2. `mode2`：`state.commit(selection)`（如尚未选题）；
3. `delivery.confirm` → `JdbcU06DeliveryStore.reconcileConfirmed` **synthetic** 插入多个 delivery 表；
4. `state.commit(clinical)` 写 question delivered / pending；
5. `consultationWait.establish`：locked consultation → lifecycle WAITING_USER → `saveAndFlush` → wait effect `saveAndFlush`；
6. `runtimeWait.establish`：`checkpoints.initialize` → `checkpoints.reserve`（locked thread、插入 checkpoint、thread WAIT_CHECKPOINTED）→ `threads.enterAwaitingUser`（再次 lock，thread AWAITING_USER）→ synthetic hash eligibility。
7. `trace.complete`。
异常处理：步骤 5 失败被转换为 `RECONCILIATION_REQUIRED/U06_CONSULTATION_WAIT_RECONCILIATION_REQUIRED`；步骤 6 失败被转换为 `RECONCILIATION_REQUIRED/U06_RUNTIME_WAIT_RECONCILIATION_REQUIRED`，**并没有自动补偿所有先前阶段**。步骤 7 出错进入外层异常处理，尝试 trace FAILED，但对先前写入无统一回滚承诺。

尤其 `mode2` 的有意分阶段与恢复兼容性，不允许仅向外层随意加 `@Transactional` 就宣布完成。真实 external delivery 不属于本地 DB 原子事务；当前 receipt=`SYNTHETIC` 不能视为真实渠道确认。

## 4. Partial-commit / failure-window matrix

| ID | 故障/竞争注入点 | 源码允许的风险或结果 | 实测状态 |
| --- | --- | --- | --- |
| PC-01 | selection commit 后 delivery 失败 | selection 可能已写入；后续阶段未达；依赖实际 Backend/事务 | NOT_EXECUTED |
| PC-02 | synthetic delivery DB 多表写入第 N 条失败 | 若该 final store 无实际代理/外层 Tx，可能出现部分 delivery rows；具体 JDBC autocommit 待证 | NOT_EXECUTED |
| PC-03 | delivery 后 clinical CDP commit 失败或读回不匹配 | 返回 RECONCILIATION_REQUIRED；delivery 已发生与否需要独立权威 | NOT_EXECUTED |
| PC-04 | clinical delivered 写入后 consultation 锁/版本失败 | 明确返回 RECONCILIATION_REQUIRED；CDP vs consultation 可能阶段分离 | NOT_EXECUTED |
| PC-05 | consultation effect 保存成功后 thread 初始化失败 | consultation 可能已独立提交（取决于代理/Tx）；thread 还不存在 | NOT_EXECUTED |
| PC-06 | checkpoint reserve 成功后 enterAwaitingUser 失败 | thread=WAIT_CHECKPOINTED、checkpoint ACTIVE 可能持久；这是源码可识别阶段但不等于可恢复授权 | NOT_EXECUTED |
| PC-07 | enterAwaitingUser 成功后 hash projector/trace complete 失败 | thread 可能已 AWAITING_USER，但调用返回失败；不能以返回值确定持久状态 | NOT_EXECUTED |
| PC-08 | 两个 mode2 并发，同 ID/不同 payload | 唯一键/锁/CAS 可能阻止重复；DB隔离与真实 commit 顺序未认证 | NOT_EXECUTED |
| PC-09 | JDBC COMMIT / 网络结果不明 | UNKNOWN，必须新的真实连接重新读回；不许可盲重放 | NOT_EXECUTED |
| PC-10 | 多 writer 不遵守同一锁顺序或外层只读事务 | 可能死锁或共享 guard 失效；writer inventory 未全覆盖 | NOT_EXECUTED |

**不可过度推断**：目前只有代码级风险路径，没有真实 MySQL/Oracle 物理部分提交事故证据。D5-O1 73 项属于 SELECT-only 合成结构验证，不填此矩阵的执行结果。

## 5. 现有证据可证明与不能证明

`U06Rdp06AuthoritativeObservationTest` 的 `minimalApp` / `WaitHarness` 使用 Mockito repositories 和 `new ConsultationWaitTransitionService` / `new RuntimeWaitCheckpointService` / `new RuntimeThreadWaitTransitionService` / `new U06WaitCoordinator`；`WaitHarness` 的状态存于 AtomicReference/AtomicInteger。它可以验证调用顺序及异常分类，不能证明 Spring proxy、JPA manager、同物理连接、commit rollback 或数据库 readback。已有 `@Transactional` 的类存在不意味着被注册为 Bean。

MySQL V6 定义 consultation wait effect / thread / checkpoint、delivery 多表和 trace；Oracle V6 亦有相应结构。唯一键只证明单表约束，不构成跨 owner shared transaction/撤销/业务权限证明。

`application.yml` 默认 active=dev；未从仓库已读配置找到可认证的特定部署 DataSource / TxManager / EMF 拓扑。Spring Boot 2.7.8 + starter-data-jpa + AOP 仅是依赖可用性，不是实际运行证据。

## 6. v0.2 设计调整和最小文件策略

不能在当前阶段直接把整个 `mode2` 改成全局原子事务，因为它覆盖 synthetic delivery、CDP state、consultation、runtime、trace，存在已冻结的阶段恢复行为。先划分 **Stage A：生产装配/事务前提认证（read-only verification only）**、**Stage B：仅等待链 owner 写入的原子性合同设计**；再决定是否实施 Stage C（受控新路径写入）。

Stage A 计划文件：
- `docs/current/06_开发单元/U07_D5_O2_Actual_Spring_Transaction_Topology_Partial_Commit_Source_Audit_v0.2.md`（本文）。
- 条件新增隔离 test/support 的 `U07D5O2SpringTopologyIT`：从明确非生产 Spring context 读 `AopUtils.isAopProxy`、实际目标与切面、transaction manager identity、entityManagerFactory、DataSource identity，执行事务中 getConnection/connection ID 认证；绝不暴露 patient data。
- 条件新增 `U07D5O2PartialCommitIT`：仅一次性 MySQL8 fixture、现有原迁移 V2/V3/V6、真实 JPA+JDBC 编排，阶段注入，单独连接验证数据；若需 bean wiring，仅测试专用配置，不能误称生产 wiring。

Stage B 改进：
1. 明确 consultation-effect 与 reserve/checkpoint-thread 是否两个可保留的单独事务（兼容模式），还是审批新原子 wait owner 切片；**先作选择再实现**。
2. 对现有 final 业务类，拟由非 final Spring-managed wrapper / 明确 `TransactionTemplate` 边界，而非指望 final 类上的 AOP；同时验证 JpaTransactionManager/JdbcTemplate 共用 DataSource 时 JDBC 实际是否参与同一个事务。
3. 独立持久读回返回 `MATCH/ABSENT/STALE/INCONSISTENT/UNAVAILABLE`，authority 固定 UNKNOWN，禁止从 Spring 方法正常返回提升为 APPLIED/resume。
4. 保留原 U06 阶段分类、幂等键和历史中间状态读取；不得更改生产入口、外部 side effects、原 DDL、F8 finalize、Oracle 路由。
5. 未有 external delivery、真实 permission/epoch、当前 CDP、APPLIED/canonical/source、outbox 证据前，不宣告 REAL_D5 READY。

## 7. 门禁与实施就绪

| Finding | 严重性 | 关闭证据 |
| --- | --- | --- |
| O2-B01 | BLOCKER | 真实 Spring Context 输出 writer bean/proxy/manager/EMF/DS identity；final class 使用独立有证据的事务执行方案 |
| O2-B02 | BLOCKER | 明确兼容现有分阶段 commit/RECONCILIATION_REQUIRED 及历史恢复的状态转移/回滚边界 |
| O2-B03 | BLOCKER for production | 真实 tenant/actor/action scope、撤销和可信来源；测试限定 nonproduction-only |
| O2-R01 | REQUIRED | 锁顺序、事务隔离/传播、异常翻译、重试位置与 deadlock 回归冻结 |
| O2-R02 | REQUIRED | 持久幂等身份、提交结果 UNKNOWN 的独立复核合同 |
| O2-R03 | REQUIRED | 真实 MySQL rollback、两连接并发、阶段 fault injection；Oracle 证据单列未完成 |

评审结论：`SOURCE_AUDIT=COMPLETED_IN_ENUMERATED_SCOPE`，`TOPOLOGY_PHYSICAL_VERIFICATION=NOT_EXECUTED`，`D5_O2_V02=DESIGN_REMEDIATION_REQUIRED`，`IMPLEMENTATION_READINESS=NOT_READY`，`REAL_D5=NOT_READY`。本文同助手核查不构成独立人员批准。未测试、未合并生产代码；下一步仅 Stage A 的隔离拓扑探针 **定向设计整改与复审**。
