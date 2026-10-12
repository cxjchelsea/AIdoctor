# U07 D5-O2 Stage B — Actual U06 Spring Transaction Wiring / Effective Transaction Boundary Design v0.1

- 日期：2026-10-12
- 源码基线：远程 `main=2834714354b02a88ccf73773511af706554cd9fb`（PR #426 standard merge commit；Stage A 10 test PASS）
- 角色：**设计候选 / 需定向独立设计复审**。本文不注册生产 U06 Bean、不改变 `src/main`、不执行 DDL/真患者写入，不解除生产权限。不批准合并，也不把 Stage A 的 isolated candidate 证明写成 production topology。
- 作用域：U06 Profile-B mode2 的真实 Spring 注入路径、事务 ownership、每次数据库 commit 与阶段性跨存储/交付一致性；有条件覆盖 mode1/mode3 的「排除」而非顺带激活。

## 0. 最重要的结论

**问题不等于“给 final 类添加 @Service 或 @Transactional”。** 目前 U06 的 `@Transactional` 在多个无 Bean 注册的 `final` 服务上，不能从源码证明已被 Spring AOP 调用；mode2 同时跨 synthetic StateCommitter、delivery、Consultation/Wait、Runtime/Checkpoint 和 trace，已有部分提交与 `RECONCILIATION_REQUIRED` 语义。把 `U06ProfileBApplicationService.execute()` 全部裹进大事务会跨外部/合成确认、持续锁、改变已有先提交/再补偿的真实语义，**禁止作为默认整改**。

**推荐方案 B1**：新建**非生产隔离的 Spring 事务执行端口**与短事务 commit owner；U06 原业务语义保持原状，显式 `TransactionTemplate(JpaTransactionManager)` 只包当前 MySQL 的**局部原子组**；在冻结调用图、类型化结果/证据、分阶段持久化以及 fail-closed 后，才允许进一步决定将这些端口接入哪一种生产授权 Profile。只有经过真实 Bean/Proxy/JPA/JDBC 数据证据、精准授权才可评价 actual U06 runtime integration，不能把 test-only wiring 叫 production wiring。

## 1. 源码核验 / 真实约束

| 入口/组件 | 当前源码事实（来自 main） | 当前已证明 | 待确认 |
|---|---|---|---|
| `DiagnosisServiceApplication` | 全量 Boot scan、JPA auditing、Feign/Discovery；本身没有 U06 专项注册入口 | 有启动类 | 真实目标 environment 启动时的 U06 wiring / TxManager identities |
| `U06ProfileBApplicationService` | `public final`，`new` 构造，有 `execute`，其中 trace.start/mode1,2,3/trace.complete，未标 `@Service` | Synthetic structural behavior | 是否具正式治理入口与生产 Bean，哪种 Profile 可激活 |
| `U06SyntheticP01Runtime` | `final`，内部 StateCommitter/Backend，保存 stable patch/readback；可能为注入 backend | 只证明设计约束 | backend 是否持久/与其他 JPA 同 DB/实际事务参与者 |
| `U06SyntheticDeliveryService` | `final`，`confirm` 委派 `U06DeliveryStore`；ScopeAuthorization 显式拒绝 real recipient/external/prod | synthetic-only | 真实外部投递契约/transactional outbox 不在本 Stage B 自动开放 |
| `JdbcU06DeliveryStore` | `final implements U06DeliveryStore`，`reconcileConfirmed @Transactional(SERIALIZABLE)` 且六张 delivery 表多 SQL | 部分代码有注解 | 能否经接口 JDK proxy + advised boundary，当前 Bean 是否注册 |
| `ConsultationWaitTransitionService` | `final` + `establish @Transactional(SERIALIZABLE)`，先更新 consultation，再插入 wait-effect | Stage A test-only 外层 Template 通过 | 原始 `@Transactional` 的 Spring advice、生产注入/transaction owner |
| `RuntimeWaitCheckpointService` | `final`，`initialize` 无注解，`reserve @Transactional(SERIALIZABLE)`：checkpoint + thread | Stage A 外层 Template 测了 rollback | initialize 与 reserve 跨 commit / 注解实际生效 |
| `RuntimeThreadWaitTransitionService` | `final` + `enterAwaitingUser @Transactional(SERIALIZABLE)`：锁 thread 和 checkpoint 再变 AWAITING_USER | 单体代码语义 | 与 reserve 事务分段/冲突窗口 |
| `U06WaitCoordinator` | `final`，顺序 `initialize → reserve → enterAwaitingUser → projector.project` | 固定顺序 | 任意相邻故障后的 persistent state/readback 与 recoverability |
| `JdbcU06GovernedExecutionTraceStore` | `final`，start insert/complete update，默认普通 JDBC 调用，未见事务声明 | 存在可分开提交的轨迹记录 | 与 U06 主链权限及失败 terminalization 的因果先后 |
| `ClinicalCdpStateRepositoryAdapter` | `@Component` non-final，`attemptAtomicCommit @Transactional`，CDP 加锁/版本表/CDP save | Spring 候选 | 此 adapter 是否被 U06 Backend 注入，CDP TxManager 是否与 wait DB 同一个 |
| `RuntimeBindingService` | `@Service` non-final，`bind @Transactional` | Spring 可扫描 | U06 受控入口是否实际消费，以及 runtime binding authority |
| `U06 RDP06 workflow` | 运行 profile `SYNTHETIC_STRUCTURAL_NONPROD`；禁止在 `runtime/u06` 添加 `@Service/@Component/@Configuration/@Bean/@Profile`；禁止直连生产 adapter/外呼 | 禁止绕开 Gate | 受控扩展必须通过独立授权；不能偷偷改旧 guard |

关键发现：Spring CGLIB **无法正常代理 final concrete class**；`JdbcU06DeliveryStore` 因实现 `U06DeliveryStore`，可有**接口 JDK 代理候选**，但需要真实 Bean + 以接口注入 + TransactionInterceptor 命中证据，不可因 `implements` 就视为已生效。Spring Data Repository 自己的默认事务**不等于**跨多次 repository calls 的同一原子事务，尤其在手工 `new finalService` 时。

## 2. 建议的主链、事务阶段与职责图

```text
Governed NONPROD admission gate (no PHI, no external delivery, no production write)
   |
   v
Profile-B (orchestration ONLY; NOT outer @Transactional)
   |
   +-- state.commit QUESTION_SELECTION [StateCommitter own transaction/unknown backend]
   |     durable boundary S1 --> stop/reconcile on failure
   |
   +-- delivery.confirm [synthetic only, bounded JDBC local D1 if authorized]
   |     durable boundary D1 --> no real external effect implied
   |
   +-- state.commit QUESTION_DELIVERED/PENDING [own controlled S2]
   |     durable boundary S2 --> readback; not globally atomic with S1/D1
   |
   +-- ConsultationWaitPort.establish [JPA TX-C: consultation+effect]
   |     durable boundary C1 --> visible failure/replay
   |
   +-- RuntimeThreadInitializePort [JPA TX-I: initial row if needed]
   |     durable boundary I1 (could remain already committed)
   |
   +-- RuntimeReservePort [JPA TX-R: checkpoint+thread WAIT_CHECKPOINTED]
   |     durable boundary R1 --> survives next-phase failure
   |
   +-- RuntimeAwaitPort [JPA TX-A: thread AWAITING_USER + CP validation]
   |     durable boundary A1
   |
   +-- ResumeEligibilityProjector [pure projection, not activation]
   |
   +-- Trace terminalization [separate, idempotent, no authority upgrade]
   |
   v
WAIT_ESTABLISHED only if C1/I1/R1/A1 + cross-owner readback + policy gates OK
otherwise RECONCILIATION_REQUIRED / FAIL_CLOSED, never APPLIED/resume by inference
```

**这里的 S1/S2/D1/C1/I1/R1/A1 是物理候选阶段名，不是重命名现有业务 Development Unit。** 保持原有 U06 effect/command/status/idempotency 身份和 `RECONCILIATION_REQUIRED` 语义，不能为了“事务打通”重写成功/失败路径。向前推广生产之前，必须先识别上游 StateCommitter 是否同数据库且确切属于哪个 TxManager；即使恰巧是同一个 MySQL，未经 authorizer 明确批准也**不能默认**把 S2/C1/R1 等包成一事务。

## 3. Effective Transaction Ownership Contract（冻结建议）

| 边界 ID | 操作/持久化集合 | 候选事务 owner | 不得纳入 | 故障时必须确认 |
|---|---|---|---|---|
| TX-S1 | U06 state selection Committer patch | StateCommitter/Backend authority（待证） | delivery、wait、trace | commit 结果未知时禁止重新构造 effect；查权威状态 |
| TX-D1 | 6 张 synthetic delivery 表 insert/replay | **显式本地 JDBC/JPA manager 或经接口 advice；择一** | real endpoint dispatch、S2 | 六表必须全有或全无；synthetic receipt 不是外部完成证明 |
| TX-S2 | question delivered + pending_question + gap changes | StateCommitter/Backend authority（待证） | C1、R1、D1 | delivered readback 与 fingerprint/parent 绑定 |
| TX-C1 | consultation status/version + wait_effect | Stage B `ConsultationWaitTxPort` / JpaTransactionManager | delivery、trace、other state | 要么两者同 commit，要么均无；失败不得改变 consultation lifecycle |
| TX-I1 | runtime thread `initialize` | Stage B `RuntimeInitTxPort` / JpaTransactionManager | R1 + A1（默认不同 commit） | 重试幂等+绑定同 consultation；race loser 重新读回需事务隔离注意 |
| TX-R1 | wait_checkpoint + thread WAIT_CHECKPOINTED | Stage B `RuntimeReserveTxPort` / JpaTransactionManager | A1、delivery、F8 | 同事务 commit/rollback；失败后据 checkpoint+thread 读回判定 |
| TX-A1 | thread AWAITING_USER + CP consistency check | Stage B `RuntimeAwaitTxPort` / JpaTransactionManager | R1 默认独立；projection | A1 fail 时保留 R1 checkpoint 并标 reconciliation，不能称完全 ready |
| TX-T1/T2 | trace start / terminal transition | Trace store own manager（待证） | 临床 owner commit、外部投递 | trace 不能代替业务成功事实，FAILED 不覆盖已有 terminal |

**推荐实现形式**：保留原 final 服务与现有构造方式不动；Stage B 的 adapter 以明确 `JpaTransactionManager` 注入 `TransactionTemplate`，在外部调用线程中用 `tx.execute(...)` 包裹对原实现的**单阶段**方法调用；每个阶段的事实经独立 reader 验证。该方案无需声称 final 服务上的 @Transactional 自身生效。需要单独 `EffectiveTxAuthority` / `TxOwnerBinding` 合同，字段至少 `stage, resourceSet, txManagerBeanId, emFactoryIdentity, dataSourceIdentity, requestedIsolation, actualIsolation, invocationKind, callerProxyKind, commitObservation, sourceProfile, evidenceRef`。Unknown/missing/mismatch = REJECT。

如果后续必须让 `@Transactional` **真正在原服务上生效**，优先选择**新的 non-final、可注入 Spring façade/port 实现**，按 bean proxy 外部调用；禁止 self-invocation 以为 advice 被触发；亦不可简单删除 final/统一加 @Service 使整套 U06 生产激活。当前 B1 推荐路线不是原类 advice 复活，而是独立、确证的 *effective* transaction owner；这是 deliberate design choice，需要审查确认需求是否允许。

## 4. 三种方案对比与决定

| 方案 | 优点 | 问题 | 本轮决策 |
|---|---|---|---|
| B0：对 `U06ProfileBApplicationService.execute()` 加大事务 | 看起来改动少 | 连接/跨状态存储/模拟交付和 trace 共锁；抹去合法 partial-commit/reconciliation | **REJECT** |
| B1：显式 TxPort + JpaTransactionManager/TransactionTemplate 的短事务 | 事务 owner 可证明，final 类不需改动，Stage A 已有物理基线 | 需要谨慎冻结阶段边界、识别 multi-DB 归属、检测跨阶段故障 | **RECOMMEND** |
| B2：移除 final / 将多个服务注解驱动成 Spring Bean | 表面自然 | 需要改原类语义、可能被旧 CI guard 禁止、self-invocation/接口注入不确定，生产意外激活 | **DEFER**，只在 B1 无法满足合同且受控批准后考虑 |

## 5. Stage B 安全 Profile、分阶段实现范围与禁用项

**Stage B0 — source/wiring attestation（无 DDL/无临床写）**
1. 在仓库单独目录建立原 U06 手工构造对象图与 Spring Bean graph 的差异矩阵（源文件、构造入口、注入类型、advisor、proxy、active tx manager）。
2. 明确所使用 DataSource/JPA EMF/JdbcTemplate 和 U06 StateCommitter Backend 是否同一个 persistent owner；未明确时 fail-closed。
3. 建立 `NONPROD_ONLY` 测试切片，沿用 Stage A MySQL8 + Java8 + SELECT-only reader 与不可伪造性限制；不得使用 `DiagnosisServiceApplication` 全量扫描、默认 dev profile。
4. 验证原类无 advice 负控、新 façade proxy 或 Template 进入真实 tx active，JPA 和 JDBC connection ID、跨线程 readback。

**Stage B1 — synthetic local TxPort prototype（单个候选 Draft PR，不激活生产）**
1. 独立 adapter：`ConsultationWaitTxPort`、`RuntimeInitTxPort`、`RuntimeReserveTxPort`、`RuntimeAwaitTxPort`、`SyntheticDeliveryTxPort`、`TraceTxPort`（可分批，不必一次实现）。
2. 每个 wrapper 的实际 boundary 对应表、事务隔离、传播、成功/失败/REPLAY 读回、双连接事实均记录；不允许 mock callback 成功代替真实 MySQL commit。
3. 重现 mode2 S1→D1→S2→C1→I1→R1→A1 顺序。每个阶段之间插入失败和重放，严格断言 persisted partial state、trace outcome 和 `RECONCILIATION_REQUIRED`；既不自动撤销先前合法提交，也不凭 trace 推断成功。
4. 并发两请求相同 consultation/parent/effect、不同 effect/idempotency、锁冲突、行版本/CAS、超时 rollback、未知完成状态单列。真实 crash/unknown commit 若工具无法证实，明确标 NOT_EXECUTED，不能靠 Mockito 冒充。
5. 与 U06 RDP06 原 workflow/既有单元测试隔离，不增添可扫描的生产 U06 `@Component`。若必须新增生产注入配置，先申请受控 amendment 授权，不得擅改 guard。

**Stage B2 — production effective wiring gate（未来，**本轮不授权**）**
- 确定官方实际运行 profile、临床审批、真实配置/secret 安全、transaction ownership、多数据源绑定、真实数据库迁移/rollback、监测与 operator recovery；独立专项 review + run evidence，再决定是否引入生产 Bean/真实 U06 pathway。
- 必须修正/分离旧 synthetic guard 的适用范围，而不是删掉 guard。真 U06 接线与真患者流量、F8 APPLIED、Scheduler/恢复、Oracle 需分别授权。

## 6. 关键不变量、失败矩阵与验收门禁

- INV-B01: `transactionOwner != null` 且 manager/EMF/DS 与该阶段持久化参与者**运行时**一致，Unknown fail-closed。
- INV-B02: `Transactional annotation present` 不等于 `ProxyAdviceObserved`；`TemplateExecuteWithBoundResources` 只证明 wrapper，不能称原 final class advised。
- INV-B03: 单阶段内任意第 N 次写入失败，局部所有表回滚；但已结束的上一阶段不回滚、不重新写入不同 idempotency key。
- INV-B04: `WAIT_ESTABLISHED` 必须验证 C1 + R1 + A1 对应持久状态一致；`WAIT_CHECKPOINTED` 本身不得授予 F8 resume。
- INV-B05: `RECONCILIATION_REQUIRED` 必须携带最小已提交事实与不可执行侧效应信息；unknown commit 查 authority，不做盲重试。
- INV-B06: 不允许在一个数据库 TX 内调用外部网络/投递或跨数据库 StateCommitter 未声明的写路径。
- INV-B07: 业务 authority、临床 D04/安全/患者许可与事务成功分离；「数据库 commit 成功」不提升「临床有效」。
- INV-B08: 旧 U06 mode1/mode3 不因新接线改变默认 path/profile，旧 mock tests、全量回归不被绕开。
- INV-B09: 任何连接/凭证/真实 PHI 不允许流入 CI artifact；实体/Repository/schema 白名单固定且可实测。
- INV-B10: 失败的 trace.complete 不能制造虚假 WAIT_ESTABLISHED 或覆盖先前提交的 reconciliation evidence。

应至少执行：不同 profile 无 Bean 注册/无操作；源原 final 类 CGLIB 负控；candidate façade advice 与 Template 真 tx active；TX-D1 六张表第 1...6 步任意失败原子回滚；TX-C1/C2 与 TX-I1/R1/A1 各中间故障与 SELECT-only reader；C1 成功 R1 失败后的合法 partially committed state；R1 成功 A1 失败后的 WAIT_CHECKPOINTED；同一 effect replay/不同 effect conflict；并发重入与 crash-window/unknown-outcome「未知不得升格」。真 MySQL8 + Java8 通过不代表 Oracle/Flyway/production activation。

## 7. 拟议的实施文件清单（需要后续 Review 冻结；本轮不创建）

- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/*` — Stage B 测试专用最小装配、failing callback、真实 MySQL T1..；
- `diagnosis-service/src/test/resources/u07/d5o2/stageb/*` — schema/profile/allowlist/evidence manifest；
- `tools/u07_d5_o2_tx_topology/stageb/*` — DDL provenance/CI evidence 与受控权限脚本；
- `.github/workflows/u07-d5-o2-stage-b.yml` — 独立 workflow Java8/MySQL8 真实测试；
- 如需设计 **新的生产包层**（仅未来 Controlled Amendment）：`diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/tx/*`（外置 domain U06 路径且非自动扫描/非默认启用；必须独立授权）。**当前 B0/B1 不创建 src/main 类**。

Stage A #426 的 bootstrap `source_main_sha` 永久 pinned 于历史 DDL 基线；Stage B 若复用，采用单独证据 manifest 记录 Stage A SHA，不得篡改 Stage A 旧 evidence 文件或硬编码最新 main 为旧测试结果。

## 8. 本轮 readiness / 待决阻断

`STAGE_B_DESIGN=V0.1_CANDIDATE`

`STAGE_B_IMPLEMENTATION_READINESS=NOT_READY_FOR_PRODUCTION; B0_SOURCE_ATTESTATION_DESIGN_READY`

`STAGE_B_PRODUCTION_WIRING=UNAUTHORIZED`

`REAL_D5=NOT_READY`

**BR-B01 (BLOCKER)**：真实 U06 Spring Bean 构造/调用者 profile、实际 target runtime container 未确认；不能从 `@Transactional` 推定有效。
**BR-B02 (BLOCKER)**：U06 StateCommitter Backend、JPA/JDBC/Trace 的真实 DataSource/TxManager binding 及数据库归属尚未形成可验证事实。
**BR-B03 (BLOCKER)**：TX-D1 交付六表在接口 JDK proxy vs explicit Template 间选择、局部错误回滚/重放权威未定。
**BR-B04 (BLOCKER)**：C1→I1→R1→A1 的原始部分提交语义，需要按故障窗口明确恢复/对账结果合同，否则新的 wrapper 可能改变原业务路径。
**BR-B05 (REQUIRED)**：生产激活会冲突旧 U06 synthetic CI guard，必须另行申请 Controlled Amendment；本设计禁止绕开它。
**BR-B06 (REQUIRED)**：真实 MySQL8/Java8 + proxy/connection physical evidence 的 B0/B1 测试用例/manifest、Artifact 对应 SHA 尚未实际冻结/执行。

**推荐下一步**：`D5-O2 Stage B — Targeted Independent Design Review`，专审 B1 短事务方案、六个 Blocker、合并后的 Stage A 证据可继承边界；再对必要 blocker 做定向整改。复审后才可申请 Stage B0/B1 非生产 test-only 实现授权。本文件不应在未经复审时宣告 `STAGE_B_READY`。
