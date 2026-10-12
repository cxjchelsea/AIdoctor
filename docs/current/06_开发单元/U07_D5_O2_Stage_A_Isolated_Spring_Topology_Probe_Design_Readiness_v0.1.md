# U07 D5-O2 Stage A — Isolated Spring Transaction Topology Probe Design & Readiness Review v0.1

日期：2026-10-12。精确 remote main = `435c456e7bf43601f0445f82932c3584fa1b8170`，GitHub compare(main, baseline)=identical。依赖 #422 的 D5-O2 v0.2 源码审计（draft、未合并）；本设计直接从 main 验证相关源码。不变更生产代码、DDL、部署配置；本设计不代表 Java/MySQL/Oracle 已执行。结论见第八节。

## 1. 目标、non-goals、来源

Stage A 只回答三层不同的问题：

- A0 BASELINE INVENTORY：实际 `diagnosis-service` 原始 Spring context 中有无指定 writer Bean / proxy / manager / EMF / DS？基线未提供这些 Bean 时，报告 ABSENT/UNWIRED，而非神奇补全成生产 wiring。
- A1 ISOLATED CANDIDATE TOPOLOGY：在专门 `@TestConfiguration` 中，仅对当前仓库真实业务类加最小装配，测 Spring 事务语义、final class proxy 拒绝、TransactionTemplate 与 JPA + JDBC 的资源匹配；全程非生产 MySQL fixture。
- A2 PHYSICAL EXECUTION：MySQL8 真实连接与事务的成功、rollback、部分阶段提交、连接/锁证据，由两个独立 connection 验证；不引入 F8，真实外部 delivery，生产 tenant/actor 权威，患者/PHI，Oracle，真实 resume。

**不可将 A0、A1、A2 的证据合并为“production transaction verified”。** A1 中手工构造的 service + test wrapper 即使行为成功，也只能认证该 isolated candidate 的拓扑。原始 Bean 未注册即 A0=UNWIRED。不得使用 `@SpringBootTest` 配合默认 dev profile、Nacos、Feign、Redis、startup Flyway 或外部系统拉起“生产等价”证据；应用 `application.yml` 默认 active: dev（注释宣称 Oracle 部署，不是物理认证）。 

已审源码：`DiagnosisServiceApplication`（@SpringBootApplication、@EnableJpaAuditing）、`diagnosis-service/pom.xml`（Java8、SpringBoot2.7.8、JPA、AOP、MySQL runtime、Flyway7.15）、`application.yml`；U06ProfileBApplicationService mode2；ConsultationWaitTransitionService；RuntimeWaitCheckpointService；RuntimeThreadWaitTransitionService；U06WaitCoordinator；JdbcU06DeliveryStore；JdbcU06GovernedExecutionTraceStore；ClinicalCdpStateRepositoryAdapter；JPA repositories；U06Rdp06AuthoritativeObservationTest；MySQL V2/V3/V6、Oracle V6；`.github/workflows/u06-rdp06-authoritative-verification.yml` 与 `u07-d5-owner-readback.yml`。原 U06 workflow 具有 no-live guard，不可修改并偷偷解除原隔离合同。

## 2. 预期拓扑证据分类

| 值 | 规则 |
| --- | --- |
| `ABSENT` | 指定目标在原始 context 无 Bean，不能称 active |
| `PRESENT_UNADVISED` | Bean 存在但不是有效事务 advised proxy，或手工 new 直接调用 |
| `PROXY_SEEN` | 看到 `AopUtils.isAopProxy` 和 advisor，仅候选条件，非事务生效 |
| `TX_ADVICE_OBSERVED` | 调用时记录 TxSynchronizationManager.isActualTransactionActive 和事务管理器生效证据 |
| `RESOURCE_BOUND_OBSERVED` | 同请求 JDBC/JPA manager、DataSource、EMF、物理连接身份和受控提交行为交叉认证 |
| `ROLLBACK_VERIFIED` | 写入后抛异常，新的独立连接读不到提交内容 |
| `PARTIAL_COMMIT_OBSERVED` | 仅于有意分阶段实验中，新的独立连接读到早期提交而后续阶段失败 |
| `UNKNOWN` | 证据不全、异常、异步未收敛或根本未测试，fail-closed |

`PROXY_SEEN` 不能提升为 `RESOURCE_BOUND_OBSERVED`。`@Transactional` 修饰 final class 的实例不可以被简单当作 CGLIB 子类代理，测试必须记录基线启动失败/被拒和未代理结果；不能因为失败就自动在测试环境替其加 @Service 并声明生产已有 Bean。

## 3. 精确采集内容和隐私边界

A0 收集目标类名、bean name/数量、是否来自生产装配（区分 test imports）、AopUtils.isAopProxy/isCglibProxy/isJdkDynamicProxy、ultimateTargetClass、advisor/事务 attribute 解析、TransactionManager / EMF / DS Bean 名称、qualifier/primary、包装链的身份映射（不打印凭证）。针对同类型多个 manager 一律禁止自动挑选“第一个”。

A1/A2 采集：外层 transaction active、readOnly、isolation name、transaction name、线程标识、Spring bound resource 是否同一、Hibernate 获取 connection 的已验证 hook 与 `DataSourceUtils.getConnection` 连接代理 unwrap 身份；`SELECT CONNECTION_ID()` 只属于 MySQL物理 evidence，不能单凭 ID 证明 JPA/JDBC share；还要通过同事务的未提交行可见性和 rollback 实验确认，防止连接池/代理伪象。判定要记录 manager 类型（`JpaTransactionManager` vs `DataSourceTransactionManager`）、EMF / datasource identity、physical connection ID、transaction synchronization 和 outcome。明确使用与同一 EMF 兼容的 `JpaTransactionManager` 时才测试 JDBC 参与，不能因为 manager 和 DS 引用相同就假定已加入同一事务。

evidence JSON 只含测试标识、代码及 DDL SHA、固定本地 synthetic fixture ID、environment label、拓扑 verdict、trace step counts、连接/事务不敏感结构信息、测试结果。不得上传 URL 中密码、连接凭证、患者字段、CDP JSON、SQL bind values。CI trace 有意关掉 Hibernate SQL/bind log；失败 artifact 不包含数据库 dump。

## 4. 强制隔离

1. profile `U07_D5O2_STAGE_A_SYNTHETIC`，exact fixed `jdbc:mysql://127.0.0.1:33326/u07_d5o2_stage_a?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`，MySQL8 disposable CI service，拒绝外部端点或任意 URL 覆盖；固定 schema/user 角色，不提供生产账户。该 URL 是**设计拟定值**，并非已经存在的当前配置。
2. Stage A 启动仅 test-local `AnnotationConfigApplicationContext` 或明确受控 non-web `ApplicationContextRunner`（按 JPA auto-config 需求选一），手工列举需要的 JPA repositories/entity package、transaction manager、EMF、DS、JdbcTemplate 和已存在服务；禁止 component scan `com.aidoctor.diagnosis` 全包、`DiagnosisServiceApplication` 自动启动、Feign/Redis/Nacos、webserver、调度器、外部网络。
3. 若选择 test slice `@DataJpaTest`，必须显式关闭 test database auto-replace、Flyway auto-migrate 和 SQL 初始化，且证明不会隐式替换 MySQL 为 H2；建议自构 minimal JPA+Tx Context 降低全应用侧效应。真实生产 wiring 的 A0 验证仅静态 inventory + 受控 isolation context，若需真正生产 Bean graph，另行批准环境后再取，不得宣称当前已完成。
4. MySQL schema fixture 基于原 `diagnosis-service/src/main/resources/db/migration/V2/V3/V6`，必须验原 blobs/digests，依赖 V1/V4/V5 或 Flyway baseline 的完整升级问题要由隔离 bootstrap manifest 处理：不修改既有 migration、不在未经审核的库运行。加载前严格隔离 root bootstrap、JPA/JDBC app reader/writer；独立只读观察账号。
5. 需在任何 SQL 之前拒绝错误 profile/URL、外层未知 Tx、缺少 manager/EMF/DS、测试 provenance 无法对齐；其计数边界从资源构造前开始。外部连接无法被网络层完全禁止时，CI 应只放行 loopback disposable MySQL；测试本身没有外部服务客户端。
6. 原 `.github/workflows/u06-rdp06-authoritative-verification.yml` 的 no-live guard 必须保持；新的 workflow 只在 Stage A 测试文件、workflow 和合规 design file 变化时触发，不通过解封旧 U06 pipeline 获得绿灯。

## 5. 探针判定矩阵

| ID | 实验 | 必须判定 |
| --- | --- | --- |
| A0-01 | 原始源码 Bean/registration 与 `@Service/@Component/@Bean` 核对 | final wait services、U06ProfileB、JdbcU06DeliveryStore 缺明确装配 → `UNWIRED`，不能伪造代理 |
| A0-02 | 尝试让 final concrete wait service 接受 class-based Spring AOP | 应失败或显示未 advise（具体实验结果记录）；绝不把异常吃掉变 PASS |
| A0-03 | `ClinicalCdpStateRepositoryAdapter` + RuntimeBindingService 对比 | 注解/组件存在只能称 eligible；真实 advised+manager 为独立实测 |
| A1-01 | test-local 明确 `JpaTransactionManager` 和 JPA+JDBC，同一 DS，外部 `TransactionTemplate` | 两种访问的 bound resources、物理连接及未提交可见性一致才标 `RESOURCE_BOUND_OBSERVED` |
| A1-02 | 故意 wrong manager/DS / EMF 配置 | fail-closed `TOPOLOGY_MISMATCH`，禁止返回 compatible |
| A1-03 | final service 上 `@Transactional` 与外部 wrapper/Template 对照 | 分别报告原 class annotation 不可信与候选 wrapper 有实际 Tx；不可替原类背书 |
| A2-01 | Consultation + WaitEffect，同一个受控 manager 事务中间抛异常 | readback 两者均不提交（real MySQL）；否则 blocker |
| A2-02 | Checkpoint + Thread reserve 中间异常 | 事务内 checkpoint/thread 均回滚；新连接读回 |
| A2-03 | 明确两阶段 `reserve` 后 `enterAwaitingUser` 失败 | 按设计可识别 `WAIT_CHECKPOINTED` 中间态；此实验不能归类为整体原子成功 |
| A2-04 | JPA 写入尚未 commit 时用 JDBC 观察/独立连接对照 | 同事务可见、独立连接不可见，flush 与 commit 分开记录 |
| A2-05 | 外层 rollback 与内部 `saveAndFlush` | `flush` 不足以让独立连接看到已提交结果 |
| A2-06 | 并发（锁顺序、行版本冲突） | 真实双连接 barrier 不使用 sleep；可复现并明确 abort outcome |
| A2-07 | JDBC writer 混用被隔离为单独 resource | 检出非共享事务，不得由注解名称推断一个事务 |
| A2-08 | commit 后断线代理 / 报告值注入 | 仅作为 proxy fault，不得宣称真实网络宕机/COMMIT 未知物理证据 |
| A2-09 | 探针读回工具 D5-O1 自身 | 保持 nonprod-only 与 authority UNKNOWN，不直接接生产业务 |

每类以 exact SHA 绑定独立的 JUnit XML、结构 JSON、CI job log；清楚标出 `real MySQL` vs `proxy injection` vs `static only`。同一 test 在多个 workflow 重复执行不可相加为独立覆盖。

## 6. 工程设计及精确拟议文件范围

建议先只实施 Stage A probe（不修改 production Java/DDL/root build）。

- `tools/u07_d5_o2_tx_topology/pom.xml`：候选独立 Maven 模块，依赖 `diagnosis-service` 的 compilation/testing 方式需先证实（现有 diagnosis-service 的 `shared-contracts` 手工安装且非仓库根多模块）。**阻断点：若候选模块直接依赖 diagnosis-service jar 需要明确如何构建并以 test-only classpath 读取 production types，不能随意跨 module 复制 JPA entity 或伪造实现；建议优先把测试类放在 diagnosis-service/src/test 下，而不是人为复制生产源码。**
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATopologyTest.java`：A0 + A1 topology manifest，绝不使用 `@SpringBootTest` 的默认 dev context。
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageATxPhysicalIT.java`：A2 MySQL 单/双连接、rollback与阶段提交。
- `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u07/d5o2/U07D5O2StageAFixtureConfig.java`：明确 test-only JPA/JDBC/proxy/manager Bean 装配，显式模块白名单。
- `diagnosis-service/src/test/resources/u07/d5o2/stage-a-manifest.json`：固定 schema digests / connection profile / negative-case期待值。
- `tools/u07_d5_o2_tx_topology/bootstrap-stage-a-schema.py`：只允许 disposable MySQL，核验原 DDL，无权限提升或镜像 owner 表。
- `.github/workflows/u07-d5-o2-stage-a.yml`：Java8/真实MySQL8，无生产 secret，固定端口、隔离部署与 provenance。
- `docs/current/06_开发单元/U07_D5_O2_Stage_A_Implementation_Evidence_v0.1.md`：待实现及实测时才创建，不预填成功结果。

本阶段**不在设计 PR 中提前创建上述测试/工作流文件**。目标先对 Java test classpath、JPA entity scanning、schema baseline、物理连接观察 API 和 CI 网络隔离完成定向设计整改，再授权实施。首选精简为 diagnosis-service 内三个 test Java + manifest + bootstrap + workflow + evidence 共 7 文件（含本设计另计），不落地没有使用价值的独立 pom。最终须依据构建验证冻结确切 inventory。

## 7. 定向设计评审：不以“多写测试”取代必要依赖

- `SA-B01` BLOCKER：缺经验证的 test-local minimal JpaTransactionManager/EMF/DS wiring、proxy 与注解语义演示；A0 不可声明生产 active。
- `SA-B02` BLOCKER：诊断服务原装 Flyway7/legacy baseline、V2/V3/V6 加载前提及 JPA entity/Java8 classpath 未冻结；不得把只含孤立 V2/V3/V6 的 disposable schema 宣称真实 deployment equivalence。
- `SA-B03` BLOCKER：不能在同一证据中混淆原无代理 U06 final 类与 test wrapper 事务候选；必须强制两套命名/结果分类。
- `SA-R01` REQUIRED：JPA/JDBC 同一物理 connection 的 instrumentation 需适配 Hibernate5/Boot2.7、延迟取连接/代理，不能只用连接 ID 或 Bean hash。
- `SA-R02` REQUIRED：fail-closed fixture profile/URL、启动零外部 side effect、无 credential artifact 的可执行断言。
- `SA-R03` REQUIRED：A2 staged vs atomic outcomes 要维护当前 `RECONCILIATION_REQUIRED` 语义，MySQL real vs proxy 注入分别报。
- `SA-R04` REQUIRED：小范围文件与 workflow 的精确编译策略、GitHub test checkout SHA、original DDL digest、reuse historical 129 与 O1 73 的区分。

本评审由同一助手实施，不是独立人员审批。审查结论：`STAGE_A_DESIGN=V0.1_REVIEWED_WITH_BLOCKERS`；`STAGE_A_IMPLEMENTATION_READINESS=NOT_READY`；`REAL_D5=NOT_READY`；`PRODUCTION_TOPOLOGY=UNVERIFIED`。

## 8. 可供下一轮立即处理的整改顺序

(1) `SA-B01`：首先落出**无副作用的测试 ApplicationContext 装配图和 capability boundary**，证明原 U06 Bean 缺失、final AOP 不可直接授权，确定 JpaTransactionManager + EntityManagerFactory + DataSource 三者的明确 wiring，并保证 Spring Data JPA repositories 的实体扫描。

(2) `SA-B02`：逐项核对 MySQL V1→V6 与 `Flyway7.15` baseline；制定 disposable schema bootstrap 和只用于 Stage A 的受控测试连接；不复用 D5-O1 账号越权写。

(3) `SA-B03/SA-R01`：冻结 read-only observation protocol：bean identity + advisor + transaction active + bound EMF/DS + JPA/JDBC 未提交可见性 + rollback/new connection；两个严格不同的输出 category：`ORIGINAL_UNWIRED` 与 `TEST_CANDIDATE_VALIDATED`。

(4) 定向设计复审与实施授权；随后隔离实施、真实 Java8/MySQL8 workflow、针对结果再次审查。禁止在此之前修改生产入口或合并设计 PR。
