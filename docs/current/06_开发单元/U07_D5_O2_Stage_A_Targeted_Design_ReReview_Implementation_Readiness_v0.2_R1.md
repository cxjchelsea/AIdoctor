# U07 D5-O2 Stage A — Targeted Design Re-Review + Implementation Readiness v0.2-R1

2026-10-12 | source main `435c456e7bf43601f0445f82932c3584fa1b8170` | design candidate Draft #424 exact head `733580437d39cfa2f6d84be90a197a17f685f2c3`.
本次仅源码及设计复审，没有启动 Maven/Spring/MySQL8/Oracle，也没有修改生产代码、运行迁移、执行独立人员审查。本次结论仅对 Stage A **隔离验证器构建**有效，不构成生产集成授权。

## 1. 评审范围与不可替代证据

复核 #424 整改文档，对照 `diagnosis-service/pom.xml`、`DiagnosisServiceApplication`、`application.yml`、U06 Wait/Delivery 相关 final 类、`U06Rdp06AuthoritativeObservationTest`、目标四个实体/Repository、MySQL V1–V6 原始 migrations、`.github/workflows/u06-rdp06-authoritative-verification.yml` 和 `u07-d5-owner-readback.yml`。

main 与预期 `435c456e7bf43601f0445f82932c3584fa1b8170` 比较 identical；#424 draft open, unmerged。编译可行性只判断为源码级 plausible；Java8 Maven 实测尚未发生。`SOURCE_BASELINE`、`ISOLATED_CANDIDATE`、`PHYSICAL_MYSQL` 不能混合，`authority=UNKNOWN`，`productionTopologyVerified=false`。

## 2. SA-B01 — 实体/Repository 精确装配复审

### 2.1 新发现 R1-F01 — @EnableJpaRepositories 包扫描扩大

#424 设计使用 `@EnableJpaRepositories(basePackageClasses=...)` 然后声称只注册四个 Repo，但包级扫描本身不提供这一保证。实际 repository 分布：
- `runtime/u01`: `ConsultationRepository`。
- `runtime/u06/wait`: `ConsultationWaitEffectRepository`。
- `runtime/foundation`: `RuntimeThreadStateRepository`、`RuntimeWaitCheckpointRepository`，**另外还包括** `RuntimeBindingRepository`、`CanonicalBusinessEventRepository`、`ClinicalRunRepository`（至少三项）。
由当前精确 git tree 确认。若这些额外 Repository 创建时其 Entity 不在 EMF whitelist，可能出现 `Not a managed type`；即使能启动也破坏白名单合同。

整改方案（实施前置，未证实已可编译）：使用 `@EnableJpaRepositories` 的 include/exclude filters 对 *四个具体接口* 做 class-assignable 精确筛选，并显式记录注册后的 `JpaRepository` beans 集合，与四接口做集合全等；必要时单独注册 `JpaRepositoryFactoryBean`，但严禁绕回包级默认扫描。必须在 Spring Boot 2.7.8 / Spring Data JPA 实际启动测试中验证 filter 语义。任何 extra repo/bean = hard FAIL。

### 2.2 新发现 R1-F02 — JPA Entity package scan 不精确

四个 `@Entity` 分布于 `runtime/u01`、`runtime/u06/wait`、`runtime/foundation`，后者还有 `RuntimeBindingRecord`、`CanonicalBusinessEventRecord`、`ClinicalRunRecord` 等 Entity。`LocalContainerEntityManagerFactoryBean.setPackagesToScan` 若指定 `foundation` 包无法确保仅 four entities，可能触发缺表/额外 schema 读写。

整改方案：在实体发现和 Hibernate metadata build 之前提供**明确的四个 managed-class 集合**；候选路线为 test-only persistence unit 明确列举四 class，禁止 unlisted classes（视 Spring ORM 5.3 / Hibernate5 实现性验证），或者由 test-only `PersistenceUnitPostProcessor` 精确编辑 managed class 列表且验证实际 `EntityManagerFactory.getMetamodel().getEntities()` 集合全等。若需要单独 `src/test/resources/META-INF/u07-d5o2-persistence.xml`，应正式增补第七文件或把白名单明确定义在现有 FixtureConfig 内，**不允许为了坚持六文件数量而降级扫描**。任何额外 Entity = hard FAIL。此前仅以 Java 类名数组写在文档中尚未使该限制可执行。

### 2.3 代理与事务归属

`ConsultationWaitTransitionService`、`RuntimeWaitCheckpointService`、`RuntimeThreadWaitTransitionService` 均为 final 类但方法含 @Transactional；测试直接 new，不可证明其 annotation 已由 AOP 拦截。final CGLIB 无法子类代理；JDK 代理只对接口及正确 Bean/interface injection 可行，不能笼统断言所有 final class 不可代理。隔离实验中 use explicit `TransactionTemplate(JpaTransactionManager)` 可保证 test-only candidate 的某个事务边界，但不能证明原类 annotation 生效。

**SA-B01 verdict: DESIGN_DIRECTION_ACCEPTED_WITH_EXECUTION_GATES。** 代码阶段开始可设计 test-only config；不允许宣布拓扑已经 VERIFIED。

## 3. SA-B02 — MySQL schema 与构建复审

MySQL 原始 migration Git blobs 均与 #424 明示的六项一一对应：
- V1 `dfd87844be26015131fffa57430d3d982b5a4ef6`；
- V2 `73029495c5f46e8576e34f8c9a77644fb8c1c4c4`；
- V3 `ccc1aa30b8e9405bd07249bae1d3531853f7510b`；
- V4 `e735e5f96bd11982084236d432d326289f4dda28`；
- V5 `acf748dc0ff2346eeede5c258c299bdbc3e1cc06`；
- V6 `7ed5a82bfba3b3fed2666bee969a7fe276e67566`。

V2 → V3 → V6 的前提在 Owner 表范围内合理：V6 的 ALTER `clinical_consultation` 依赖 V3；V6 还创建 consultation wait effect、runtime thread、checkpoint、delivery 和 trace。由于跳过 V1/V4/V5，`OWNER_SCHEMA_SLICE` 不得宣称 Flyway baseline/full migration compatible。此前 D5-O1 的 bootstrap 已证明 schema-by-original-blob 可设计，但不是 Stage A 的 Java+JPA 真机通过。

`diagnosis-service/pom.xml` 为 Java8/Boot2.7.8/JPA/AOP/Flyway7.15.0 的合理 test classpath，`contracts/v1/bindings/java/pom.xml` 的 install dependency 已进入拟定 CI 顺序；不过未执行 Maven, 不声明 test-compile 成功。独立 test 使用 `*Test` 确保 Surefire 默认可发现，并强制 XML 的 tests>0；不得用 `-DskipTests` 的 test-compile 代表执行成功。

新增 R1-F03（必需）：schema bootstrap 在生成任何 CREATE/GRANT/SQL 前，应先验证 localhost:固定端口、固定 schema、MySQL8、InnoDB、原文件 sha256 和 Git blob；在 CI 中根本不允许配置生产连接；bootstrap root account 与 app writer + independent read-only observer 分开；fixture 主键与跨 test 清理由新 schema 或专用 fixture ID 保障。不要将 `schemaMode=OWNER_SCHEMA_SLICE` 推断为完整 Flyway。

**SA-B02 verdict: DESIGN_BOUNDARY_ACCEPTED_WITH_RUNTIME_PRECONDITIONS。** Flyway full-chain 仍单独 NOT_VERIFIED，不阻止只读设计审查/隔离原型编码。

## 4. SA-B03 — 三种证据空间与 outcome taxonomy 复审

#424 的 `SOURCE_BASELINE`、`ISOLATED_CANDIDATE`、`PHYSICAL_MYSQL`、`wiringProvenance`、`schemaMode`、`authority=UNKNOWN`、`productionTopologyVerified=false` 约束方向正确。要求增加 machine-checkable cross-field invariant：
- `SOURCE_BASELINE` 只能 `STATIC_SOURCE`，`backend` 可为 `NOT_APPLICABLE`，不得硬填 MYSQL8 产生“连接被验证”的歧义；不得宣称 TX_BOUND/ROLLBACK_VERIFIED；
- `ISOLATED_CANDIDATE` 必须 `TEST_ONLY_CONFIG`，可报告 Bean/Proxy/manager but 不报告生产归属；
- `PHYSICAL_MYSQL` 必须 `TEST_ONLY_CONFIG` + `OWNER_SCHEMA_SLICE`（或明确受控独立 Flyway matrix），且真实 MySQL connection/readback evidence 才能标 COMMIT/ROLLBACK；
- `testCheckoutSha` 由运行中的 git 读出；`sourceMainSha` 是冻结对照，不代表测试 checkout；若本轮 docs commits 未合并，必须保留 code SHA/test HEAD、DDL blob、workflow run ID 各自独立；
- 失败/缺失/fixture 注入与物理网络故障结果必须 UNKNOWN 或对应负控类，不得提升生产/APPLIED/resume。
增加测试对错误 plane/wiring/schema/authority 输出的显式负控，证据校验器出现错配 fail closed。

**SA-B03 verdict: ACCEPTED_FOR_PROBE_IMPLEMENTATION（需要负控执行）。**

## 5. Implementation readiness 决策

| Gate | 判定 | 意义 |
| --- | --- | --- |
| Stage A architecture | `CONDITIONALLY_READY_FOR_ISOLATED_IMPLEMENTATION` | 允许在非生产的 test-only 作用域实现探针；不能先修改生产语义 |
| SA-B01 | `CONDITIONAL: R1-F01/F02` | 精确 Repo/Entity 白名单必须可执行且 Spring 启动时集合全等，不能用全包扫描 |
| SA-B02 | `CONDITIONAL: R1-F03` | 真 MySQL、固定 schema、DDL manifest、Java8 真实编译和手工/CI 对照 |
| SA-B03 | `DESIGN_ACCEPTED; NEGATIVE_TEST_PENDING` | 输出 schema 分类须自动 fail-closed |
| Actual Java compile and context startup | `NOT_EXECUTED` | 不能宣称 PASS |
| Real MySQL/JPA rollback/concurrency | `NOT_EXECUTED` | 不能宣称 PASS |
| Production Spring topology | `UNVERIFIED` | Stage A 不能自动覆盖 |
| D5-O2 full implementation | `NOT_AUTHORIZED` | 仅 test-only isolated candidate coding 有条件准备度 |
| REAL_D5 | `NOT_READY` | clinical/delivery/auth/epoch/APPLIED 仍缺 |

条件门禁的操作含义：**可以开始受约束的 Stage A 隔离探针代码实现**，但第一步就必须将 R1-F01/R1-F02 做成可执行的 JPA bean/metamodel invariant；若失败就立即停在 `IMPLEMENTATION_BLOCKED`，不允许绕过负控或宣布 A1/A2 ready。R1-F03 验证完成后再运行写入性 MySQL tests。任何文件超出白名单的增加先在候选 PR 精确申报和复审，不借口“六文件数固定”而放宽安全机制。

## 6. 精确提交/运行边界

后续候选只改 `diagnosis-service/src/test/java/.../d5o2/*`、`diagnosis-service/src/test/resources/u07/d5o2/*`、`tools/u07_d5_o2_tx_topology/*`、`.github/workflows/u07-d5-o2-stage-a.yml`，以及真实验证完成后的独立 evidence 文档。若新增 test-only persistence unit XML，则追加精确清单并说明白名单，不修改 `src/main`、`pom.xml`、`application.yml`、原 V1–V6、U06 worklow、D5-O1 / F8。创建 Draft PR；启动前 fail-closed 环境 guard；保留 exact source baseline；不要自动合并。每项 JUnit、schema provenance 和真实 MySQL test count 均需独立结果，再进行 targeted implementation review。

审查非独立人员；本文件的 `CONDITIONALLY_READY` 不是生产授权，也不是声称已完成 Stage A。下一步执行 `Stage A — Isolated Test-Only Spring/JPA Topology Probe Implementation`（优先 R1-F01/F02），只在正式证据后推进物理验证。
