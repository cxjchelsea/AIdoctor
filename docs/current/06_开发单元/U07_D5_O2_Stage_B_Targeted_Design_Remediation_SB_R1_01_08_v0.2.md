# U07 D5-O2 Stage B — Targeted Design Remediation SB-R1-01..08 v0.2

日期：2026-10-12。冻结 source `main=2834714354b02a88ccf73773511af706554cd9fb`，基于 Stage B v0.1 Draft PR #432 (`7b613d042da432ca08f6be95b98f5a656847c78e`) 和 Targeted Design Review PR #433 (`93695ec225d1e846dd5676f748a1b3e769ed7c61`)。本文件是独立的**设计整改候选**；若其与 v0.1 冲突，正式批准后以本文件标为 `NORMATIVE` 的边界为准。**未批准前不得实现或生产接线**。不得将 Stage A 实测 10 tests 迁移为本 Stage B 的运行证据；不更改 production Java、旧 U06 workflow 或数据库迁移。

## 0. 修复映射与可验证目标

| Finding | v0.2 修复位置 | 判定范围 |
| --- | --- | --- |
| SB-R1-01 BLOCKER | §1 三级证据/授权隔离与 B2 入门门禁 | test-only/production 不混淆 |
| SB-R1-02 BLOCKER | §2 TxOwnerBinding、事务传播/隔离/资源身份冻结 | 能检验，而非只选 `JpaTransactionManager` 类型 |
| SB-R1-03 BLOCKER | §3 三态提交及逐窗口故障/对账矩阵 | partial commit 与业务返回严格区分 |
| SB-R1-04 BLOCKER | §4 StateCommitter Backend 归属读取方案 | 未确认的一律 UNKNOWN，禁止跨 Store 原子性推断 |
| SB-R1-05 REQUIRED | §5 D1 六表单管理器/真实故障及重放 | 不依赖 final 类未证明的 AOP |
| SB-R1-06 REQUIRED | §6 文件范围、测试编号、证据清单 | Java8/MySQL8 与 exact SHA 可复现 |
| SB-R1-07 REQUIRED | §1/§6 profile 约束和原 CI guard 保留 | 默认生产不可激活 |
| SB-R1-08 REQUIRED | §3/§7 持久状态 vs projector/trace/F8 | 不把 hash 或 trace 作为 Resume Authority |

## 1. NORMATIVE — 三个不可互相继承的认证空间

**B0 SOURCE_TOPOLOGY_ATTESTATION（只读源级/受控装配证明）**：以真实 source tree、构造者、接口注入、应用启动 Profile 和 Spring bean configuration 标记对象 provenance。默认 `UNREGISTERED_OR_UNVERIFIED` 仅表示当前源码查不到可靠装配证据，不等于启动过生产容器并观察了绝对 Bean 缺席。若运行测试专用上下文，只能输出 `TEST_CONTEXT`，不能写 `PRODUCTION_BEAN_PRESENT`。

**B1 ISOLATED_EFFECTIVE_TX_PORT（可实现的非生产原型）**：JUnit/test fixture 显式指定唯一 manager/EMF/DataSource，`TransactionTemplate` 包裹单个既有 final 业务方法（此时 final 类是普通对象）；成功只证明 `CALLER_OWNED_LOCAL_TRANSACTION`。B1 的 stage commit 与异步、外部 I/O、临床权限严格无关。

**B2 ACTUAL_U06_TARGET_RUNTIME_WIRING（未来单独授权）**：必须指定唯一 target application/profile、实际进入 U06 的被授权调用入口、Spring `BeanFactory` 的真正 Bean 身份、Advisor/TransactionInterceptor 以及参与资源/事务管理器；用合法环境捕获调用时 advice 被触发、effective rollback/readback。**不得仅以 B1 Template 成功宣告 B2**。若原 final 服务完全无 Bean，就记录 `ORIGINAL_ANNOTATION_NOT_ACTIVE` 并在另一个受控实现单独提出新的非 final façade/port；禁止自调用代理绕过、批量移除 final 或默认扫描 `@Service`。

所有 B0/B1 新文件只在 `src/test`、测试资源、独立 tools/workflow 下；不向 `runtime/u06` 添加 stereotype；不启动 `DiagnosisServiceApplication` 全量扫描；不调整 `.github/workflows/u06-rdp06-authoritative-verification.yml`，不移除 `SYNTHETIC_STRUCTURAL_NONPROD` 约束；不得连接生产 CDP、真实患者数据、真实外部交付、F8 APPLIED/恢复调度。B2 要新增 **Controlled Amendment + 另一组安全/临床/权限/配置授权**，不由本文件默示。

## 2. NORMATIVE — TxOwnerBinding v0.2 与精确事务配置

### 2.1 结构化事务合同（建议 test-only Java POJO + JSON manifest）

```text
TxOwnerBinding {
  stageId; sourceMainSha; testedCheckoutSha; sourceProvenance;
  profile; authorizationMode; transactionOwner; invocationKind;
  managerBeanName; managerClass; managerIdentity;
  entityManagerFactoryBeanName; entityManagerFactoryIdentity;
  dataSourceBeanName; dataSourceIdentity; jdbcTemplateDataSourceIdentity;
  entityAllowlist; repositoryAllowlist; sqlTableAllowlist;
  propagation; requestedIsolation; observedIsolation;
  rollbackOn; rollbackOnlyObserved; actualTxActive;
  springBoundResourceTypes; writerConnectionId; readerConnectionId;
  effectId; idempotencyKey; parentWaitEffectId; phasePredecessorCommitRef;
  beforeReadbackDigest; afterReadbackDigest;
  outcomeClass; durableOutcome; confirmationSource; evidenceRef;
  productionTopologyVerified=false; realDeliveryAuthorized=false; f8ResumeAuthorized=false;
}
```

字段 `managerBeanName` 仅是命名，不是权威；至少实测 Bean identity、被 manager 管理的 EMF/JDBC DataSource **引用相等**、Spring `TransactionSynchronizationManager` bound resource / actualTxActive、独立连接下提交前不可见和提交后可见/失败回滚。禁止序列化真实 SQL 参数/密钥/PHI/真实用户 ID。绑定不完整或多 manager 有歧义：`OWNER_BINDING_UNKNOWN` -> 阻止写入，不能默认选择 `@Primary`。

### 2.2 B1 Candidate stage frozen owner matrix

| Stage | 明确 manager / 参与者 | 传播 / 隔离候选 | 原子提交范围与外部排除 | Authority |
| --- | --- | --- | --- | --- |
| S1 / S2 | `STATE_COMMITTER_BACKEND_UNRESOLVED`；内部提交主体由 StateCommitter/具体 RepositoryPort 决定 | **不得套入本地 Template** | S1 question selection；S2 delivered+pending+gap（需源 Backend readback） | `UNKNOWN` 直到 §4 核对 |
| D1 | `stageBJdbcTransactionManager` = `DataSourceTransactionManager`，精确绑定 `stageBDataSource` / `stageBJdbcTemplate`；不依赖 JDK proxy | `REQUIRES_NEW` + `SERIALIZABLE`；入口拒绝已有外层事务，**不允许把有上下文的 TX 静默挂起当成独立 authority** | 仅六张 synthetic delivery 表；不含 endpoint dispatch、S2、trace | `TEST_ONLY` |
| C1 | `stageBJpaTransactionManager`，绑定 `stageBEntityManagerFactory` + **同一** `stageBDataSource`、两 JPA repo | `REQUIRES_NEW` + `SERIALIZABLE`；调用前无已活动事务；无 self-invocation | consultation row/version + wait effect | `TEST_ONLY` |
| I1 | 同一具名 `stageBJpaTransactionManager` + runtime thread repository | `REQUIRES_NEW` + `SERIALIZABLE`；已有会话事务 fail closed | 初始化 RuntimeThreadState 独立提交 | `TEST_ONLY` |
| R1 | 同一具名 JPA manager / checkpoint + thread repositories | `REQUIRES_NEW` + `SERIALIZABLE` | checkpoint + thread WAIT_CHECKPOINTED | `TEST_ONLY` |
| A1 | 同一具名 JPA manager / thread + checkpoint | `REQUIRES_NEW` + `SERIALIZABLE` | thread AWAITING_USER + checkpoint 一致性验证 | `TEST_ONLY` |
| T1/T2 | `stageBJdbcTransactionManager`，仅受控 trace JDBC 表；不得共享主链外层事务 | `REQUIRES_NEW` + `READ_COMMITTED`（候选，需观察） | 各次 trace.start / terminal update 独立且 replay-safe；不得据 trace 授权 F8 | `TEST_ONLY` |

说明：在无 ambient transaction 的新入口 `REQUIRES_NEW` 用于明确**每次单独提交**；前置 guard 使传播策略不会意外地挂起调用者事务。实际 mysql isolation 由 `SELECT @@transaction_isolation` 或连接 API 在活动 transaction 内证实，不能将请求的 `SERIALIZABLE` 误当作生效事实。JPA 与 JDBC 共享一个 `DataSource`，但 manager 必须由**相应 stage 唯一控制**：同一阶段不可混用两个 manager；跨阶段也不能据共库宣称全局原子性。Stage B B1 如需单独 D1 manager 与 JPA manager，必须在配置层使用清晰具名 Bean，禁止隐式注入/多 manager 二义性。无法保证时拒绝执行并返回 `OWNER_BINDING_UNKNOWN`。DDL 和 JPA entity whitelist 沿用原 Stage A 可证实的子集，再按六张 D1 表扩展，不能包级扫入生产实体。

**Rollback**：所有本地 stage 对 `RuntimeException/Error` 回滚；若使用受检异常，adapter 必须转成同一 fail-closed `StageTxFailure` 并确保 rollback-only；数据库写入后显式抛错需要真实 MySQL 重新独立连接 readback。不得在 TxTemplate lambda 内捕获异常后正常 return 而意外提交部分数据。只有事务资源观察、独立读回和一致性核验确认后才能标 `COMMITTED`；driver 报错/连接中断时为 `UNKNOWN_COMMIT`，禁止把异常一律转换为 `ROLLED_BACK`。

## 3. NORMATIVE — 持久阶段 / Fault Window / Outcome Matrix

**三态物理判定**：`COMMITTED_OBSERVED`（独立只读连接核对完整 effect/version/fingerprint）、`ROLLED_BACK_OBSERVED`（已知前置状态+完整无新增验证）、`UNKNOWN_COMMIT`（无法证明前两种）。另外 `CONFLICT`、`REPLAY_MATCH` 是逻辑判定，不能替代物理三态。状态读取不可达时禁止 `COMMITTED_OBSERVED`。

| Cut / 注入位置 | 已知可保留的前阶段事实 | 本阶段的错误/未知结果 | 可报告业务 outcome（仅候选，不改原 U06 映射） | 重试/恢复及禁止 |
| --- | --- | --- | --- | --- |
| Pre-S1 / admission、preflight | 无阶段写，trace 可能未启动 | 拒绝 | `ADMISSION_REJECTED` / no mutation | 禁止推断新 commit |
| S1 selection commit | S1 可能已完成，即使下一阶段失败也不回滚 | S1 unsuccessful: `FAILURE_REQUIRED`；commit unknown：状态 `UNKNOWN` | 原 mode2 的 `FAILURE_REQUIRED` 分支保持；未知只作 reconciliation-internal 标记 | 依 effectId+patch/replay fingerprint 读 StateCommitter 权威；禁止盲新 ID |
| D1 synthetic ledger | S1 可保留 | D1 六表提交失败需要独立检验全无或完整；未知依 ledger+子表读回 | 原 execute 可能抛异常且 trace 走 FAILED，不允许擅自改成 `WAIT_ESTABLISHED` | 不产生真实投递；只恢复相同 effectId/idempotency |
| S2 delivered/pending | S1、D1 可保留 | S2 status non-COMMITTED / readback mismatch | `RECONCILIATION_REQUIRED`（与源码一致） | 不能取消已观察到的 S1/D1；pending hash 与 delivery confirmation binding 一致 |
| C1 consultation wait | S1/D1/S2 可保留 | 两表必须一起 commit 或 rollback；unknown -> `UNKNOWN_COMMIT` | `RECONCILIATION_REQUIRED`，保留 `U06_CONSULTATION_WAIT_RECONCILIATION_REQUIRED` 原语义 | 读 consultation.lifecycle/currentWaitEffect + waitEffect / fingerprint/version；不可继续 R1 |
| I1 thread initialization | 前述 + C1 可保留 | 可能线程 ACTIVE 单独 commit；冲突需 consultation binding 查验 | `RECONCILIATION_REQUIRED`，纳入 runtime wait 失败子类 | 同 consultationId 的同 ID 读回；禁止误删之前成功创建的合法 row |
| R1 checkpoint reservation | C1/I1 可保留，thread ACTIVE 或旧合法状态 | rollback：checkpoint 不存在/线程保持 ACTIVE；commit：CP+WAIT_CHECKPOINTED 都在；unknown：重读 | `RECONCILIATION_REQUIRED` | R1 committed ≠ F8 resume/WAIT_ESTABLISHED；只允许按原 cp/effect replay |
| A1 transition awaiting | R1 `WAIT_CHECKPOINTED` 可保留 | fail：thread仍 WAIT_CHECKPOINTED；success：AWAITING_USER 且 CP 正确 | fail `RECONCILIATION_REQUIRED`；候选完成需全链 readback | 不能把 R1 成功错报为 A1 成功；不凭 projector hash 授权 |
| Projector | 持久阶段 A1 可能已经成功 | 纯函数/hash 异常，持久结果不会撤销 | 依实读事实作 reconciliation，不基于 hash 宣告 success | 必须区分 `eligibleCandidateHash` 与真正运行时恢复许可 |
| T1/T2 Trace | 既有已提交阶段不可被 trace rollback | trace start/complete 失败、terminal replay 冲突 | trace-only failure 不得伪造业务成功；如何上报需后续 UX/contract 核对 | 不覆盖已有 terminal；operator reconcile，不能以 FAILED 清除真实已提交状态 |
| Any `UNKNOWN_COMMIT` | 前阶段所有已观察持久事实仍有效 | 不得默认 commit 或 rollback | `RECONCILIATION_REQUIRED/UNKNOWN` 内部型，外部结果映射另审 | 先 read-after-reconnect + fingerprint/version；证明前禁止 dispatch/resume |

**与现有源码的区别**：此矩阵是 Stage B 的预期「持久事实/可报告业务结果」设计合同，不声称已实测每个分支。原 `U06ProfileBApplicationService` mode2 C1/runtime catch 返回 `RECONCILIATION_REQUIRED`；S1 非 COMMITTED 用 `FAILURE_REQUIRED`；S2 非 COMMITTED / readback mismatch 用 `RECONCILIATION_REQUIRED`；D1 抛异常通常由 execute 最外 catch 处理，跟 C1 内部 catch 不同。不得在 future prototype 无差别抹平这些差异。异常与 commit status 不一致时要保留原可验证来源字段，并交给评审确定最终对外状态。

**持久一致性判定字段**：`consultationId / parentWaitEffectId / effectId / deliveryId / checkpointId / runId / threadId / expectedPriorRowVersion / committedRowVersion / stateVersion / phaseId / attemptId / commitOwner / beforeDigest / afterDigest`，涉及真实用户信息时只记录不可逆 synthetic fixture id，不可写 PHI。每个阶段开始前校验已提交 predecessor token；C1、R1、A1 后必须从**独立连接**读关系约束；Trace 不作为权威主记录。任何 `WAIT_ESTABLISHED` 只在完整 durable graph + 未过期安全/许可校验后形成候选；F8 APPLIED/resume 仍另需明确授权。

## 4. NORMATIVE — S1/S2 StateCommitter 资源归属溯源

源码可定位：`U06SyntheticP01Runtime.createInjected(storeRef,consultationId,cdpId,clock,backend)` -> `backend.repositoryPort()` -> `new StateCommitter(port,...)` -> `committer.commit(stable.patch)` -> `backend.readCurrent(cdpId)`。这只能证明**调用链形状**，无法证明实际 `StateRepositoryPort` 对应 `ClinicalCdpStateRepositoryAdapter`，也无法证明其与 wait tables 同一个 Spring manager。即使 `ClinicalCdpStateRepositoryAdapter` 有 `@Component/@Transactional`，没有构造者 + 实际 bean + data source/EMF/connection 的证据，仍必须 `BACKEND_OWNER_UNKNOWN`。

**B0 evidence intake**：源码 search `createInjected(`、`implements U06SyntheticP01Runtime.Backend`、`StateRepositoryPort` 提供者及 `getReadStoreRef/getCommitStoreRef`；记录静态具体 class、构造点、调用方、启用 profile、实际 `StateRepositoryPort` 与 bean identity。如果走 in-memory/Mockito backend，只写 `SYNTHETIC_IN_MEMORY`。若测试 spring-context 注入 CDP adapter，建立**单独** `TARGET_CONFIG_CANDIDATE`，不得改写原 runtime provenance。真实物理一致性需要独立连接、适当 scope 的测试 DB、实际 `DataSource`/JPA tx-manager 元数据、commit result 与 durable readback 配套；无法归属就停止 S1/S2 生产事务讨论，**B1 Local TxPort 仅允许在 fixed stub 前置 S1/S2 的受控隔离测试中测试 D1/C1/I1/R1/A1**，不能输出 end-to-end production result。

## 5. NORMATIVE — D1 六表事务/重放验证

选择：`JDBC_LOCAL_TEMPLATE`，`DataSourceTransactionManager` 显式唯一管理 `stageBJdbcTemplate` 的相同 `stageBDataSource`。不依赖 `JdbcU06DeliveryStore @Transactional` 注解是否被接口 JDK proxy 拦截；**原 final 方法由测试 TxPort 从外部显式调用**。六张表和顺序精确来自当前源码：

1. `u06_delivery_authority`，selectionEffectId + deliveryEffectId + deliveryId；
2. `u06_delivery_intent`，question/content fingerprint、synthetic endpoint、idempotency；
3. `u06_delivery_attempt`，SYNTHETIC_ACCEPTED；
4. `u06_delivery_receipt`，evidence_type=SYNTHETIC、SYNTHETIC_CONFIRMED；
5. `u06_delivery_confirmation`，confirmationEvaluationId、fingerprint；
6. `u06_delivery_ledger`，confirmationStatus=CONFIRMED。

**注入原则**：测试专用 JDBC adapter 在真实 MySQL 中分别于第 1..6 次写入后抛 `StageBFaultInjected`；同 manager 上 6 表在异常后均为未提交/回滚（只读观察者验证），且 previous S1 fixture 不撤销；正向完整 6 表 commit，再相同 `effectId + idempotencyKey + confirmationFingerprint` 重放，验证不重复写；重用同 effectId 换 fingerprint/endpoint 拒绝；ledger 仅 partial 的人工受控 fixture 不能归类为 fully confirmed，须 `INCONSISTENT_DELIVERY_LEDGER` 或 UNKNOWN，而不是成功回放。测试实现须以原 JdbcU06DeliveryStore SQL/读回的输入参数约束为准，不能只 mock 6 次调用。对 “第 1..6 次 SQL 后注入” 这种技术插桩若需 wrap `JdbcTemplate`，只能在 test-only 注入层完成，不修改生产六表 store。

**禁止外部副作用**：所有 scope/ref 为 fixture，`externalSideEffectAllowed=false`、`realRecipientAllowed=false`、`productionStoreAllowed=false`，运行上下文固定 disposable MySQL8 且无对外网络；Synthetic receipt 不等于投递到真实用户。

## 6. NORMATIVE — B0/B1 实施清单与可执行验收

### 6.1 文件范围（冻结建议，正式复审后可据精准 diff 授权）

- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/stageb/StageBTestConfig.java` — 最小手动 context、双具名 TxManager/共享 test DB DataSource、实体及 Repository 白名单/manager identity；
- `.../stageb/StageBTxOwnerBindingTest.java` — runtime resource identity/isolation/propagation/proxy negative controls；
- `.../stageb/StageBDeliveryAtomicityTest.java` — D1 六表第 1..6 次真实写入后注入+replay/冲突；
- `.../stageb/StageBPhaseRecoveryTest.java` — C1/I1/R1/A1 的 durable phase 与 cross-phase 故障矩阵；
- `.../stageb/StageBStateBackendBoundaryTest.java` — S1/S2 backend source binding / nonprod stub、fail-closed mismatch；
- `.../stageb/StageBTraceProjectionTest.java` — trace failure、projector failure、F8 fail-closed；
- `diagnosis-service/src/test/resources/u07/d5o2/stageb/manifest.json` — exact main ancestor、DDL SHA256/blob、schema/table/entity allowlist、sample phase ID、no-real-authority；
- `tools/u07_d5_o2_tx_topology/stageb/bootstrap-stage-b-schema.py` — original DDL read/check + disposable bootstrapping、read-only role、pre-write SQL guard；
- `tools/u07_d5_o2_tx_topology/stageb/verify-stage-b-evidence.py` — testcase-level fact manifest/check, fail closed missing case SHA；
- `.github/workflows/u07-d5-o2-stage-b.yml` — Java8/disposable MySQL8, exact-target path, locked Actions SHA, JUnit + per-case evidence upload。

不得改变旧 Stage A tests/manifest/CI，`diagnosis-service/src/main`、`application.yml`、root `pom.xml`、Flyway V1-V6、旧 U06 CI/guard、任何生产 Clinical/CDP 数据；此清单为**提案**，若 physical test requires extra fixture file，必须先在 PR 明示范围并审查，不能自动扩大。JUnit 类名使用 `*Test` 确保 Surefire 真执行；实际 target `test-compile` 与 test count >0 才算 gate，有 issue 就改代码/复验，不得提前写 PASS。

### 6.2 预期检查编号（全部真实执行且负控必须真正失败）

| ID | 必要断言 |
| --- | --- |
| B0-01 | 源级 U06 object graph 与 Spring Bean graph provenance 明确；无生产 profile 时未授权 U06 Bean 不激活 |
| B0-02 | final unadvised / JDK-interface optional proxy negative；以 advice 运行证据区分 wrapper-owned |
| B1-TX-01 | 实际 DataSource/EMF/manager 引用、binding 及正确 connection ID；错 manager / 混用 DS / 缺身份拒绝 |
| B1-TX-02 | 有 ambient transaction 时拒绝进入 stage；显式 REQUIRES_NEW + SERIALIZABLE 有效隔离级别实测 |
| B1-D1-01..06 | 每个 D1 SQL 后失败、六表一致 rollback+SELECT-only readback |
| B1-D1-07..09 | 正常六表 commit、同 effect replay、conflict + partial ledger negative |
| B1-C1-01..02 | Consultation + WaitEffect 局部原子及异常回滚 |
| B1-I1-01 | initialize 幂等、不同 consultation binding 冲突 |
| B1-R1-01..02 | CP + thread reservation 同 commit、失败回滚且维持先前 I1 |
| B1-A1-01..02 | awaiting transition commit；失败保留 R1 WAIT_CHECKPOINTED，F8 禁止 |
| B1-CROSS-01..03 | C1 成功 I1/R1 失败、R1 成功 A1 失败、retry/replay 下的持久快照 |
| B1-TRACE-01..02 | trace.start/complete 独立失败及 terminal conflict，不改临床持久事实 |
| B1-PROJECT-01 | projector/hash 不构成 physical authority，抛错后仍是 reconciliation |
| B1-NEG-01..03 | stale fingerprint、已锁冲突、wrong TxManager/unknown commit 三态拒绝 |

运行结果不允许用 `test class count` 代替独立 case coverage；证据 JSON 至少记录每个 ID 的 `testcaseName,status,stageId,evidencePlane,sourceHead,actualHead,dbVersion,connectionIds,managerIdentity,isolation,phaseBeforeDigest,phaseAfterDigest,physicalOutcome,authorityUnknown,verificationRunId`。每个 ID 都绑定具体 JUnit method/XML 与 observable SQL row digest，不应包含可逆真实患者数据/凭据。

## 7. NORMATIVE — 永不越权的不变量

1. `WAIT_CHECKPOINTED` 只是 R1 成功，绝不授权 `AWAITING_USER` 之后的恢复；只有 A1 真实完成且 C1/R1/A1 graph 同关联身份一致时，可形成 **候选** `WAIT_ESTABLISHED`。
2. `U07ResumeEligibilityProjector.project()` 的 hash 只是 deterministic candidate，不能据此绕开 U07/F8 授权、Claim/lease、权限和临床安全判断。
3. `JdbcU06GovernedExecutionTraceStore.start/complete` 的 STARTED/FAILED/COMPLETED 是审计事实，不是事务 commit authority，trace terminal failure 不撤销已经正确提交的其他阶段，也不得把之前失败阶段错误标为成功。
4. 任意阶段 `UNKNOWN_COMMIT` 禁止继续后续权威操作；独立读回若不可用保持 UNKNOWN、不假定 ROLLBACK；恢复必须复用既有 effect/patch/confirmation fingerprint。
5. U06 mode1/mode3 不因 Stage B 原型改变；不得修改临床/患者/真实投递行为。
6. Repo/Entity whitelist、物理库身份/角色、transaction source provenance 缺一项即 fail closed；CI 只允许 synthetic disposable schema，profile 字符串自身不构成物理安全证明。

## 8. v0.2 完成度与下一步

**本轮已经完成的是“设计合同整改”，不是“运行事实确认”**：

- `SB-R1-01..08=DESIGN_REMEDIATED_REVIEW_PENDING`
- `STAGE_B_V02_DESIGN=SUBMITTED_FOR_TARGETED_REVIEW`
- `STAGE_B_B0_STATIC_SOURCE_ATTESTATION=READY_FOR_READ_ONLY_CONFIRMATION`
- `STAGE_B_B1_IMPLEMENTATION_READINESS=RE_REVIEW_PENDING`
- `STAGE_B_ACTUAL_PRODUCTION_WIRING=UNVERIFIED/UNAUTHORIZED`
- `REAL_D5=NOT_READY`

复审必须特别核验 **两 manager 同 DS 的 JPA/JDBC transaction resource isolation 是否被误用**、`REQUIRES_NEW` 与 ambient guard 是否冲突、D1 写后注入层能否不改生产 SQL 地实现、phase matrix 是否与现有 mode2 catch/trace 语义一致、B1 的静态 source Backend 若未知是否严格限制验收。若有不可实施/误导的合同，仍须 targeted remediation；随后才能条件授权 isolated B1 prototype，不能绕过独立审查/生产授权。
