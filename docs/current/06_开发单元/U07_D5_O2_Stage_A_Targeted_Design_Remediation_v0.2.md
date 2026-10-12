# U07 D5-O2 Stage A — SA-B01..03 定向设计整改 v0.2

日期：2026-10-12。冻结源码基线 `435c456e7bf43601f0445f82932c3584fa1b8170`，GitHub compare(main,baseline)=identical。本文件是对 Draft PR #423 v0.1 的**定向补充方案**，并非覆写历史结论。本文不产生运行证据，不改生产源码、DDL、root build、旧 U06 Workflow，不执行数据库、不授权 Merge。此前源码审计为 Draft PR #422。

## 0. 决策原则与结果

本轮对 SA-B01（测试 Spring 拓扑）、SA-B02（真实 schema、Java8 构建与 legacy migration 边界）、SA-B03（原始无代理 vs 候选事务有证据的隔离）形成可实现的合同。禁止基于 annotation、Bean 名称、proxy 存在、method success、same URL、同个 ID、调用者 bool 得出事务已提交、production topology 或 business authority。最终 readiness 仍需**后续针对 v0.2 的定向复审**。

三个不同证据空间：
- `SOURCE_BASELINE`：本 main 的源码与明确的 Spring Bean 注册、AOP 可行性；无法启动生产环境时绝不叫「真实生产上下文」。
- `ISOLATED_CANDIDATE`：非生产 test-local Spring Context 和 `TransactionTemplate`（显式选择 JpaTransactionManager）编排原有 repository/service；实验成功只证明此候选。
- `PHYSICAL_MYSQL`：针对候选的真实 MySQL8 / Spring Data JPA / JDBC transaction、独立读回与故障窗口。不是生产拓扑、Oracle 或 APPLIED。

## 1. SA-B01 — 最小上下文和资源装配冻结

### 1.1 原始源码与负控

原始类 `U06ProfileBApplicationService`、`U06WaitCoordinator`、`ConsultationWaitTransitionService`、`RuntimeWaitCheckpointService`、`RuntimeThreadWaitTransitionService` 与 `JdbcU06DeliveryStore` 均为 `final`，U06 wait 服务未见生产 `@Component/@Service` 和现有 `@Bean` 注册；原测试 `U06Rdp06AuthoritativeObservationTest.minimalApp/WaitHarness` 使用 `new` 与 Mockito。其 `@Transactional` 不是活动代理的证据。Spring 的 CGLIB 无法子类代理 final class；仅 `JdbcU06DeliveryStore` 有 `U06DeliveryStore` 接口，存在 JDK proxy 的可能，但依赖真实 Spring Bean、注入接口、拦截 advisor 生效的独立验证，不得推断已实现。不得错误断言「所有 final class 都绝不可能有 JDK proxy」。

不运行全量生产 SpringBootApplication、不加载默认 dev profile；A0 对上述源码给出 `SOURCE_UNWIRED` 和 `STATIC_TRANSACTION_ANNOTATION_ONLY`；这是**静态归属**，而非启动过生产 Spring 的 Bean 缺失证明。若实验需要 Spring Bean registry，则采用本切片候选 context，并输出 `TEST_WIRING` provenance，绝不将其结果写成 `PRODUCTION_BEAN_PRESENT`。

负控矩阵：直接 `new` 的类与方法为 `UNADVISED`；对 final concrete class 强制 `proxyTargetClass=true` 的失败或无法 advice，需按实际运行结果标注，不能伪造固定异常文字；JDK 动态代理候选只允许接口返回类型，必须检验 `TransactionInterceptor` 生效和 `TransactionSynchronizationManager` 实际状态。

### 1.2 测试专用装配图（禁止生产包扫描）

```text
JUnit 5 [non-production only]
  └── StageATestConfig (@Configuration / @EnableTransactionManagement)
       ├── fixed isolated HikariDataSource (JDBC url fixed, lazy probe)
       ├── LocalContainerEntityManagerFactoryBean
       │    └── EXACT whitelist managed entities:
       │       ConsultationRecord, ConsultationWaitEffectRecord,
       │       RuntimeThreadStateRecord, RuntimeWaitCheckpointRecord
       │       [read-only linkage evidence can use JDBC fixture for binding/run]
       ├── JpaTransactionManager [single named stageATransactionManager]
       ├── JdbcTemplate [same DataSource]
       ├── @EnableJpaRepositories [specific 4 repository interfaces]
       ├── TransactionTemplate [explicit same JpaTransactionManager]
       ├── StageACandidateCoordinator [test-only, not production class]
       └── StageATopologyObserver [test-only, no clinical authority]
```

实现阶段须确认 Spring Data JPA 仓库配置的 `entityManagerFactoryRef` / `transactionManagerRef` 精确匹配上述唯一 Bean 名称，多个 TxManager/EMF/DS fail-closed，`@Primary` 不能代替 explicit binding。注意 `@EnableJpaRepositories(basePackageClasses=...)` 会扫描整个指定 package；应先枚举相邻 repository 并确认不会误注册其他模块。EMF 要限制 managed class 白名单，不能把整个 `com.aidoctor.diagnosis` 扫入 JPA，避免无关实体触发 schema-validation 和数据副作用。若 `LocalContainerEntityManagerFactoryBean.packagesToScan` 无法满足精确白名单，采用显式 `PersistenceUnitPostProcessor` / managed class names，或重构测试 slice，但不能跳过白名单检查。

配置：Hibernate DDL = validate / none（先验证 schema，再禁止自动建表），`spring.jpa.open-in-view=false`，关闭 Flyway auto-run、SQL init、webserver、Discovery/Feign/Redis、scheduler、远程 tracing；不使用 `@SpringBootTest(classes=DiagnosisServiceApplication.class)`。所有实际连接均在前置纯配置/fixture 校验通过后开启；Spring ORM EMF 启动可能会读取数据库 metadata，必须记录这类 **startup DB I/O**，不能宣称构造 0 SQL。零连接承诺只对 pre-context profile/URL/manifest 拒绝成立，之后明确测试计数域。

### 1.3 事务可观测性协议

分三个阈值，不得跨越：
1. `BEAN_AND_PROXY`: bean source（SOURCE_BASELINE 或 TEST_WIRING）、target class、proxy kind/advisors、transaction attribute、manager/EMF/DS identities。
2. `TX_BOUND`: 在真实被测 method 调用中读取 `TransactionSynchronizationManager.isActualTransactionActive()`、bound resource map（仅类型/身份，不打印内容）、isolation/readOnly。外层 `TransactionTemplate` 不得用于证明原始 final 类自己的 `@Transactional` advice。
3. `PHYSICAL_ATOMICITY`: JPA `saveAndFlush` 后，`JdbcTemplate` 在同事务读取新行，独立另一个物理连接在提交前读不到，rollback 后也读不到；commit 后新连接读到。断言 `DataSourceUtils` bound physical connection、Hibernate 同事务 hook 与 MySQL `CONNECTION_ID()`，加行为证据交叉证明；仅连接 ID/相同 DataSource 不是充分条件。

写入必须以真实 MySQL 8 的 InnoDB 表和独立账号执行。两个独立连接应在不同线程/非同一 Spring 事务资源作用域，确保不会从 Spring 连接上下文继承事务；使用 barrier + timeout + finally，不靠 sleep。negative control 注入不同 DataSource 或 manager，必须分类 `TOPOLOGY_MISMATCH` 而不是 PASS。

## 2. SA-B02 — DDL / Flyway7 / Java8 编译边界冻结

### 2.1 原始 DDL provenance（Git blobs，main 实读）

| 文件 | Git blob |
| --- | --- |
| MySQL V1 `V1__create_agent_state_and_audit_trail.sql` | `dfd87844be26015131fffa57430d3d982b5a4ef6` |
| MySQL V2 `V2__create_clinical_runtime_foundation.sql` | `73029495c5f46e8576e34f8c9a77644fb8c1c4c4` |
| MySQL V3 `V3__create_clinical_consultation.sql` | `ccc1aa30b8e9405bd07249bae1d3531853f7510b` |
| MySQL V4 `V4__create_capability_governance_foundation.sql` | `e735e5f96bd11982084236d432d326289f4dda28` |
| MySQL V5 `V5__extend_capability_trace.sql` | `acf748dc0ff2346eeede5c258c299bdbc3e1cc06` |
| MySQL V6 `V6__add_u06_wait_runtime.sql` | `7ed5a82bfba3b3fed2666bee969a7fe276e67566` |

V1 包含 `CLOB` 列和 MySQL 风格 DDL 组合；其在 MySQL8 + Flyway7.15 的**完整迁移兼容性尚未实测**，不擅自宣称失败或通过。V2/V3/V6 分别提供 binding/run、consultation、wait/trace/delivery 表。V4/V5 为 capability trace，无必要引入 Stage A owner rollback 实验；Oracle 迁移独立保留未覆盖。

### 2.2 双轨证据策略

- `OWNER_SCHEMA_SLICE`：一次性 MySQL8 schema `u07_d5o2_stage_a`，bootstrap 在校验原 V2→V3→V6 的 digest + Git blob 后加载未经修改的原始 SQL bytes；使用清单记录**刻意跳过 V1/V4/V5**。它只认证相应 Owner 表结构和 Java ORM 事务行为，不能证明 Flyway 全迁移/legacy baseline。
- `LEGACY_FLYWAY_MATRIX`：单独评估真实 Flyway7.15 V1→V6 所需的 MySQL 版本、schema baseline、迁移 history 与完整校验；不阻塞纯 `OWNER_SCHEMA_SLICE` 物理实验的执行，但阻断宣告 release migration ready。若必须全量表 JPA validate 则先确认所有必需实体对应表；不得在原 migrator 脚本中静默改 SQL。

Stage A bootstrap 新脚本只能为本轮 test schema 写入，校验原始 bytes、sha256、Git blob、固定 DB/port、root 仅 schema bootstrap、候选 writer 最小表 DML、observer 只读；不复用 D5-O1 的 `o1_reader/o1_writer` 凭证，不变更原 DDL。禁止把 URL 任意替换为网络主机或生产库。校验数据库版本、InnoDB 和索引角色，报告真实观察值而非 manifest 预期值。

### 2.3 精确 Java8 构建策略

使用现有 `diagnosis-service/pom.xml`（Java 8、Spring Boot 2.7.8、spring-data-jpa、AOP、Flyway7.15.0、MySQL runtime；无需新增 root/pom），先 `mvn -B -f contracts/v1/bindings/java/pom.xml install -DskipTests`，再 `mvn -B -f diagnosis-service/pom.xml -DskipTests test-compile`，最后运行有独立测试类名的针对性 Maven test；注意 `*IT` 默认可能不会由 Surefire 执行，使用显式 `-Dtest=...` 或改名为 `*Test` 并检查 Surefire XML 中测试数大于零，不得把 BUILD SUCCESS、0 tests 视作 PASS。CI Java8 setup 与 MySQL8 disposable container 可复用 D5-O1 pattern，但不能复用它的业务授权。

## 3. SA-B03 — 永久隔离「原始」与「候选」证据

证据 schema 强制字段：
```json
{
  "schema": "U07_D5O2_STAGE_A_EVIDENCE_V1",
  "sourceMainSha": "435c456e7bf43601f0445f82932c3584fa1b8170",
  "testedCheckoutSha": "ACTUAL_WORKFLOW_SHA",
  "evidencePlane": "SOURCE_BASELINE | ISOLATED_CANDIDATE | PHYSICAL_MYSQL",
  "wiringProvenance": "STATIC_SOURCE | TEST_ONLY_CONFIG",
  "schemaMode": "OWNER_SCHEMA_SLICE | LEGACY_FLYWAY_MATRIX",
  "backend": "MYSQL8_DISPOSABLE",
  "assertion": "TEST_CASE_ID",
  "observedResult": "EXPLICIT_RESULT",
  "authority": "UNKNOWN",
  "productionTopologyVerified": false
}
```
字符串如 `ACTUAL_WORKFLOW_SHA` 必须在运行后由 workflow 填入真实值，上述为设计 schema 示例，不能作为已生成证据。若 `wiringProvenance=TEST_ONLY_CONFIG`，禁止输出 `PRODUCTION_BEAN_PRESENT`、`PRODUCTION_TOPOLOGY_VERIFIED`。若 `schemaMode=OWNER_SCHEMA_SLICE`，禁止 `FLYWAY_BASELINE_COMPATIBLE`。若 `evidencePlane=SOURCE_BASELINE`，禁止 `TX_ADVICE_OBSERVED`、`PHYSICAL_ATOMICITY_VERIFIED`。

负控要求：同名 Bean test wrapper 与原始类不可共享同一个 evidence key；必须区分 `ORIGINAL_FINAL_UNWIRED`、`TEST_CANDIDATE_JPA_TX`、`TEST_NEGATIVE_MISMATCH`。缺原始证据、manager identity 不明、JDBC/JPA 连接关系不明时 `UNKNOWN`，而不是 `MATCH`。不向用户显示 sensitive SQL/凭证/临床数据，snapshot 不升级为持久 APPLIED 或恢复许可。

## 4. 缩减的实施 inventory（拟议、未创建）

遵守最小改动原则：用 `diagnosis-service/src/test` 原有 Java8/test classpath；**不创建独立 tools pom**。Stage A 首批只新增下列 6 个验证/辅助文件（正式证据文档在实测后另计，设计文档与本次审查文档另计）：

1. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATopologyTest.java` — A0/ A1 负控、注册与代理/管理器证据；
2. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATxPhysicalTest.java` — A2 真实 MySQL 事务/rollback/可见性；
3. `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageAFixtureConfig.java` — 最小 JPA、唯一事务管理器、白名单实体+repository、JDBC；
4. `diagnosis-service/src/test/resources/u07/d5o2/stage-a-manifest.json` — DDL origin、限定 profile、negative cases；
5. `tools/u07_d5_o2_tx_topology/bootstrap-stage-a-schema.py` — 仅 bootstrap，不作为可加载 Spring 模块；
6. `.github/workflows/u07-d5-o2-stage-a.yml` — fixed MySQL 8 + Java8，test reports 与 evidence JSON，真实 checkout SHA。

运行后新增 `docs/current/06_开发单元/U07_D5_O2_Stage_A_Implementation_Evidence_v0.1.md`，必须填入真实测试结果与 HEAD，不可提前预填 PASS。不要修改生产 U06 final 类、`application.yml`、原迁移、root build、O1 reader、F8/Source、U06 原 workflow。若测试 JPA managed-class 白名单不可在六文件内落地，**阻止 implementation**，在复审中调整 inventory，不通过 package-wide scan 规避。

## 5. 验收与定向复审准则

必须用**真实 Spring context + MySQL8 + JPA repository + JDBC**验证：原始 final/no-bean 负控；test-only JpaTransactionManager 被明确选用；两个实体集合的 saveAndFlush/rollback；独立 observer 不可见未提交，commit 后可见；人为 wrong DS/manager 拒绝；同线程同 DS 但没有 manager 的反例；`WAIT_CHECKPOINTED` 被分类为分阶段而非 APPLIED；status/category 不交叉；测试报告绑定 exact SHA。MySQL 物理 rollback 结果不补齐真实网络 crash 与 Oracle。临床/投递权威一律 UNKNOWN。

复审结论（本轮同助手设计整改，**不是独立人员批准**）：
- `SA-B01=DESIGN_REMEDIATED_REVIEW_PENDING`（装配图、代理负控、候选资源边界已明示，但未 test compile）；
- `SA-B02=DESIGN_REMEDIATED_REVIEW_PENDING`（原始 DDL Git blobs、OWNER_SCHEMA_SLICE 与 LEGACY_FLYWAY_MATRIX 分轨、Java8/Surefire 固定）；
- `SA-B03=DESIGN_REMEDIATED_REVIEW_PENDING`（证据 schema / provenance / fail-closed 防混淆）；
- `STAGE_A_IMPLEMENTATION_READINESS=RE_REVIEW_PENDING`（不可自动升级 READY）；
- `PRODUCTION_TOPOLOGY=UNVERIFIED`，`REAL_D5=NOT_READY`。

下一步：`Stage A — Targeted Independent Design Re-Review + Implementation Readiness`（若仅同助手执行，必须称 targeted re-review，不得冒称独立人员审查），核验 Java8 类路径、所有必要 JPA Entity/Repository 扫描、schema startup side effects 和六文件可行性；随后才考虑授权隔离实现。
