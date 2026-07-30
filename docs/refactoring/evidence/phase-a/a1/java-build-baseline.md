# Phase A1 Java 构建与资产基线

> 执行日期：2026-07-30（Asia/Shanghai）
> Base Branch：`agent/enterprise-agent-refactoring-plan`
> Base Commit：`c573e9901a5385d0cd61b7d8f7f0a8368289321a`
> 工作分支：`agent/phase-a1-java-baseline`
> 范围：仅 Phase A1；未实施 A2-A4 或 A5 以后重构。

## 1. 结论

整体状态为 **PARTIALLY_VALIDATED**。

| 服务 | dependency resolution | compile | clean test | 测试统计 | 结论 |
|---|---|---|---|---|---|
| `diagnosis-service` | PASS（单进程重跑） | FAIL | FAIL（主源码编译阶段） | tests 0，failures 0，errors 0，skipped 0；未进入 Surefire | 代码级编译阻塞 |
| `examination-service` | PASS（单进程重跑） | PASS | PASS | tests 0，failures 0，errors 0，skipped 0；无测试源码 | 可编译，但没有自动化测试覆盖 |

`diagnosis-service` 的一次普通 `mvn test` 返回 0，但它报告 `Nothing to compile` 和 `No sources to compile`，使用了先前失败编译留下的增量产物。`mvn clean test` 清除产物后稳定复现主源码编译失败。因此前者只作为命令事实记录，不作为服务测试通过证据。

## 2. 执行环境与模块结构

- OS：Microsoft Windows 11，`10.0.22631`，amd64。
- Java：Eclipse Temurin OpenJDK `1.8.0_412`。
- Maven：Apache Maven `3.9.12`，平台编码 GBK，默认 Locale `zh_CN`。
- Maven Wrapper：不存在；按规则使用系统 Maven。
- 用户级 `~/.m2/settings.xml`：不存在；Maven 安装目录的全局 `conf/settings.xml` 存在。
- 目标服务均为独立 jar 项目，各自继承 `spring-boot-starter-parent:2.7.8`；仓库无聚合父 POM、无 `<modules>`，两份 POM 均无 Maven profile。
- 仓库另有 `execution-trace-service/pom.xml`，不属于本次 compile/test 目标。
- 检查时 `SPRING_PROFILES_ACTIVE`、Nacos、Redis、MySQL、Oracle 和常见模型 Key 环境变量均未设置；只记录是否设置，未读取值。

## 3. 命令证据

所有 Maven 命令均在对应服务目录执行。大型完整日志未提交。

| 目录 | 命令 | 退出码 | Maven/脚本用时 | 结果与关键证据 |
|---|---|---:|---:|---|
| repo | `git fetch origin agent/enterprise-agent-refactoring-plan` | 0 | 约 2 s | 本地与远端 Base SHA 一致 |
| repo | `git rev-parse HEAD` | 0 | <1 s | `c573e9901a5385d0cd61b7d8f7f0a8368289321a` |
| repo | `java -version` | 0 | <1 s | Temurin 8u412 |
| repo | `mvn -version` | 0 | <1 s | Maven 3.9.12 |
| diagnosis | `mvn -B -ntp dependency:go-offline` | 0 | 117.659 s | 依赖解析成功 |
| examination | `mvn -B -ntp dependency:go-offline`（首次，与另一个 Maven 并行） | 1 | 10.417 s | `.m2` 的 `aether-util-1.1.0.pom.lastUpdated` 写入 AccessDenied；环境并发争用 |
| examination | `mvn -B -ntp dependency:go-offline`（单进程原样重跑） | 0 | 3.589 s | 首次失败不可复现，无代码修复 |
| diagnosis | `mvn -B -ntp compile`（沙箱网络） | 1 | 1.423 s | 访问 Maven Central 被权限策略拒绝；环境阻塞 |
| diagnosis | `mvn -B -ntp compile`（授权联网原样重跑） | 1 | 6.515 s | javac 编译 146 个源文件后出现代码错误 |
| examination | `mvn -B -ntp compile` | 0 | 3.492 s | 编译 12 个源文件；unchecked warning |
| diagnosis | `mvn -B -ntp test` | 0 | 2.581 s | 增量假阳性；0 tests，不能升级为 TEST_VERIFIED |
| diagnosis | `mvn -B -ntp clean test` | 1 | 4.909 s | 清理后在 main compile 失败；未进入测试 |
| examination | `mvn -B -ntp clean test` | 0 | 3.459 s | 编译成功；`No tests to run` |
| 两服务 | `mvn -B -ntp dependency:tree -Dincludes=...` | 0 | 约 3.5 s/3.3 s | 解析依赖版本成功 |

## 4. Build Blocker

### diagnosis-service：FAILED（代码问题）

主要错误组：

- `CDP` 与调用者 API 不一致：缺少 `getUncertaintyMap()`、`getTriageMap()`、`getDdxMap()`、`getPatientStateMap()`、`getWorkupPlanMap()`、`getManagementPlanMap()`、`getEvidenceGraphMap()`、`getFinalConclusionMap()` 等方法。
- `CDPManager` 与 Agent 调用不一致：缺少 `getCDP(String)`。
- `AgentLoop` 引用了未实现的 `checkRedFlagPriority`、`checkConflictReview` 和 `checkInsufficientEvidence`。
- Java 8 基线代码使用 Java 9+ `Map.of`（`EvidenceFusion`）。
- `CDPFieldWriter` 存在 `Map`/`List<Map<...>>` 类型不兼容和缺失 setter。
- `ToolCaller` 存在无效方法引用。

这些错误跨 CDP、Agent Loop、工具调用和临床状态语义，不属于 A1 允许的明确单点低风险修复。因此未修改源码，未创建 build fix commit。

### examination-service：无 compile blocker

编译和 clean test 生命周期均成功；警告为 `ExaminationService` 未检查类型转换。没有 `src/test`，因此测试成功只证明 Maven 生命周期可执行，不证明业务行为。

## 5. Java 与基础设施依赖

| service | groupId/artifactId | version | 来源/直接性 | 实际用途 | 证据 | 风险或阻塞 |
|---|---|---|---|---|---|---|
| both | `org.springframework.boot:spring-boot-starter-parent` | 2.7.8 | parent/direct | 依赖与插件管理 | CODE_CONFIRMED | 已停止主流支持，A1 不升级 |
| both | `org.springframework.boot:spring-boot-starter-web` | 2.7.8 | direct | MVC/REST/Tomcat | RUNTIME_VERIFIED | examination 编译验证；diagnosis 源码失败 |
| both | `spring-boot-starter-data-jpa` / `hibernate-core` | 2.7.8 / 5.6.14.Final | direct/transitive | JPA Repository/ORM | CODE_CONFIRMED | 运行需数据库 |
| both | `com.oracle.database.jdbc:ojdbc8` | 21.5.0.0 | direct | Oracle JDBC | CODE_CONFIRMED | 未连接真实 Oracle |
| diagnosis | `com.oracle.database.nls:orai18n` | 21.5.0.0 | direct | Oracle ZHS16GBK | CODE_CONFIRMED | 未运行验证 |
| both | `com.mysql:mysql-connector-j` | 8.0.32 | direct runtime | 本地 MySQL | CODE_CONFIRMED | 配置含本地开发默认凭据；未连接数据库 |
| diagnosis | `org.flywaydb:flyway-core` | 7.15.0 | direct | Oracle/MySQL migration | CODE_CONFIRMED | examination 无 Flyway；migration 方言/目录选择未运行验证 |
| diagnosis | `spring-boot-starter-data-redis` / `spring-data-redis` | 2.7.8 / 2.7.7 | direct/transitive | Redis 候选基础设施 | CODE_CONFIRMED | Java 搜索未确认业务使用点；未运行 Redis |
| diagnosis | `spring-cloud-starter-alibaba-nacos-discovery` | 2021.0.5.0 | direct | 服务发现 | CODE_CONFIRMED | 环境变量未配置；compile 不需 Nacos |
| both | `spring-cloud-starter-openfeign` | 3.1.8 | direct | 远程服务 Client | CODE_CONFIRMED | 多个 localhost 默认 URL；未启动依赖服务 |
| diagnosis | `spring-boot-starter-websocket` | 2.7.8 | direct | WebSocket/STOMP 依赖 | CODE_CONFIRMED | 目标 Java 源码中未定位 endpoint/config，实际用途 UNKNOWN |
| diagnosis | `spring-boot-starter-aop` | 2.7.8 | direct | `@TraceExecution` 切面 | CODE_CONFIRMED | Trace 默认关闭；未运行验证切点 |
| both | `spring-boot-starter-validation` | 2.7.8 | direct | Bean Validation | CODE_CONFIRMED | diagnosis controller 使用 `@Valid` |
| both | `org.projectlombok:lombok` | 1.18.24 | direct | 代码生成 | RUNTIME_VERIFIED | examination 编译验证 |
| both | `com.vladmihalcea:hibernate-types-52` | 2.21.1 | direct | JSON 类型候选 | CODE_CONFIRMED | CDP 实体实际使用 CLOB+手工 Jackson |
| both | `io.micrometer:micrometer-registry-prometheus` | 1.9.7 | direct | Prometheus metrics | CODE_CONFIRMED | 未启动验证 |
| both | `spring-boot-starter-actuator` | 2.7.8 | direct | 健康/指标 | CODE_CONFIRMED | 未启动验证 |
| both | `spring-boot-starter-test` | 2.7.8 | direct test | JUnit 5.8.2 / Mockito 4.5.1 | CODE_CONFIRMED | 两服务均无测试源码 |
| both | `maven-compiler-plugin` | 3.8.1 | direct plugin | Java 8 compile | RUNTIME_VERIFIED | diagnosis 暴露 Java 8/API 不一致错误 |
| both | `spring-boot-maven-plugin` | 2.7.8 | parent-managed plugin | 打包/运行 | CODE_CONFIRMED | A1 未 package/start |
| both | MyBatis | absent | POM/源码搜索 | 不适用 | CODE_CONFIRMED | NOT APPLICABLE |
| both | Java 模型/AI SDK | absent | POM/源码搜索 | 无直接模型 SDK | CODE_CONFIRMED | 模型能力仅通过远端服务候选 |

未发现 POM 中的私有 repository。依赖来自 Maven Central/各 BOM 管理；用户级 Maven settings 不存在。

## 6. API、数据和测试资产摘要

- `diagnosis-service`：3 个 Controller，确认 `/api/v1/diagnosis/start`、`/continue`、`/{cdpId}/status`、`/{cdpId}/result`、旧 `/{diagnosisId}/answer`、CDP 读写/Trace 摘要接口和 `/health`；11 个 Feign Client（含 Trace RestTemplate Client）；10 个 JPA Entity；9 个 Repository；3 个 SQL migration 文件；无测试源码。
- `examination-service`：2 个 Controller、4 个业务 API、1 个 OCR Feign Client、2 个 Entity、2 个 Repository；无 migration、无测试源码。
- Spring Security/认证依赖和控制器鉴权注解未定位，因此这些 API 的认证状态为 `UNKNOWN`/代码层未见门禁。
- 详细清单见 [java-api-inventory.csv](./java-api-inventory.csv)。

## 7. CDP 状态读写摘要

- `CDPManager.createCDP` 创建大 JSON/CLOB 聚合和初始版本；`updateCDP` 使用 `PESSIMISTIC_WRITE` 查询、先保存旧快照、应用 `Map<String,Object>` 更新并自增版本。
- 调用者没有提交 expected version；当前版本自增不是调用方乐观并发检查。
- `conclusionPackage` 被嵌入 `patientState.conclusion_package`；Trace 摘要通过公开 API 回写 `executionTrace`。
- Workflow、Controller、Trace 服务回调和 Agent/工具路径形成潜在多写者。
- `CDPReplayService`、`CDPRollbackService` 会保存版本快照反序列化出的 CDP；数据库/真实数据未检查，不能标为 DATA_VERIFIED。
- 详细清单见 [java-state-write-inventory.csv](./java-state-write-inventory.csv)。

## 8. Fixed Workflow 真实调用链

以下为 `CODE_CONFIRMED`，不是运行验证：

```text
DiagnosisController /start
→ DiagnosisOrchestrationService.startDiagnosis
→ CDPManager.createCDP
→ HealthStateAssessmentClient.assessHealthState
   ↳ 失败：默认 clinical_mode / riskLevel L4 并继续
→ CDPManager.updateCDP
→ 返回问题（不立即执行完整五步）

DiagnosisController /continue
→ DiagnosisOrchestrationService.continueDiagnosis
→ CDP patientState 写入回答
→ DiagnosisWorkflowOrchestrator.step1IdentifyProblem
   → clinical-parsing（失败后原状态继续）
   → dialog identify/generate（失败后默认问题继续）
→ 完整度达到代码阈值时 executeRemainingSteps
→ Step 2 diagnosis-engine（失败后空 DDX 继续）+ risk（失败后继续）
→ Step 3 diagnosis-engine organize（失败则抛错）+ dialog routing（失败后继续）
→ Step 4 dialog evidence（失败则抛错）+ diagnosis/workup（失败后继续）
→ Step 5 parsing/ranking/treatment/risk/explanation（均失败后继续）
→ 无论 Step 5 结论包是否生成，写 `cdpStatus=completed`
→ CDPManager.updateCDP / version snapshot
```

多个 `parse*Response` 仍为 TODO 并返回空集合/Map，所以即使远端返回成功也可能空结果继续。`AgentLoop` 类存在，但上述 Controller 主链明确调用固定 Orchestrator；且 AgentLoop 当前不能编译。两者运行关系为 `UNKNOWN`。

## 9. AOP / Trace 摘要

- `ExecutionTraceAspect` 的 pointcut：`@annotation(TraceExecution) || @within(TraceExecution)`；配置 `execution.trace.enabled=true` 时才创建，默认缺失即关闭。
- 实际注解位置：`DiagnosisWorkflowOrchestrator.executeDiagnosisWorkflow`、公开 `step1IdentifyProblem`、`WellnessScreeningOrchestrator.executeWellnessScreening`。
- Aspect 在调用业务方法前同步调用远端 Trace；`TraceServiceClient.recordEvent` 内吞远端异常，因此一般不会阻断 `proceed()`，但同步超时仍会增加业务延迟。
- 输入/输出仅记录类型而非值，降低但不能完全消除 PHI 风险；Feign URL 与错误消息仍可能携带敏感信息。
- `TraceContext` 是裸 `ThreadLocal<String>`；Java 目标代码未发现 `@Async`、Executor、CompletableFuture 或 MDC 传播实现。
- Feign 使用自定义 `X-CDP-Id`、`X-Trace-Id`、`X-Start-Time`、`X-Service-Name`、`X-Method-Name`，不是 W3C Trace Context。
- 详细清单见 [java-observability-inventory.csv](./java-observability-inventory.csv)。

## 10. Prompt、模型调用与患者 Delivery 差距

- Java POM 未发现模型 SDK，Java 源码未发现 Prompt 模板、Provider API Key 引用或直接 OpenAI/Qwen/Claude 调用。
- `DiagnosisEngineClient`、`DialogServiceClient`、`ExplanationServiceClient` 等远端接口可能在下游调用模型；Java 端本身不能确认 Provider、模型、temperature、token、timeout 或 retry，证据为 `UNKNOWN`。
- `DDxCandidateGenerator` 只在注释中提到“路径注入 LLM”；`ErrorCode` 和 DTO 中的 LLM 字段是 `CODE_CONFIRMED` 资产，但不是实际 Java 模型调用。
- 现有 `ConclusionPackage`、`DiagnosisResponse/Result` 和 result API 存在；没有发现 `PatientDeliveryView`、`PatientEvidenceCard`、Citation DTO 或 UI JSON Schema。
- 当前 result 组装会从 CDP 读取结论包和内部状态；代码级字段白名单/患者角色鉴权未确认，存在内部候选、远程结果或模型衍生内容泄漏风险。与 [患者端证据与引用 UI 契约](../../../患者端证据与引用UI契约.md) 的版本快照、claim-level citation、患者友好限制和审核状态差距显著。

## 11. 已验证事实、UNKNOWN 与 BLOCKED

### 已验证事实

- 两服务依赖均可解析；examination 可 clean compile/test 生命周期执行。
- diagnosis 在干净构建中不能编译；失败是实际代码错误，不是数据库/Nacos/Redis 未启动导致。
- 两服务均没有 Java 测试源码，不能产生 TEST_VERIFIED 业务结论。
- API、CDP、Workflow、Trace 和远端 Client 已达到代码级盘点（CODE_CONFIRMED）。

### UNKNOWN

- 两服务启动、数据库连接、Flyway 实际执行、Redis/Nacos/WebSocket、真实固定 Workflow E2E。
- 数据库、缓存、图谱或索引内容；本次没有 DATA_VERIFIED 结论。
- API authentication/authorization 与患者字段隔离。
- Trace 切面实际生效、代理 self-invocation、远端超时上限和异步传播。
- 下游 Python 服务内部 Prompt、模型供应商和 fallback；属于 A2，不在 A1 展开。

### BLOCKED

- diagnosis compile/test：被仓库内多组源码契约错误阻塞。
- diagnosis 运行与 Workflow runtime verification：被 compile blocker 阻塞，且运行还需要数据库和多项远端服务。
- examination 集成/Context 测试：无测试资产，且实际运行需要数据库；上传/OCR 流程还需要 OCR 服务和可写上传目录。
- Draft PR 工具：初始检查时本机未安装 GitHub CLI `gh`；发布步骤需安装并认证后执行。

## 12. A1 范围外事项

未实施 Java 功能修复、A2 Python、A3 Frontend/Docker、A4 数据盘点、Shared Contracts、Capability Package、Patient Evidence UI、CDP 重构、State Committer、OpenTelemetry、Model Gateway、数据库迁移或框架升级。

## 13. 复现步骤

```powershell
git fetch origin agent/enterprise-agent-refactoring-plan
git switch agent/phase-a1-java-baseline
java -version
mvn -version

Set-Location diagnosis-service
mvn -B -ntp dependency:go-offline
mvn -B -ntp compile
mvn -B -ntp clean test

Set-Location ..\examination-service
mvn -B -ntp dependency:go-offline
mvn -B -ntp compile
mvn -B -ntp clean test
```

在受限沙箱中 Maven Central 访问可能需要网络授权。不要并行运行首次依赖下载，以避免共享 `.m2` tracking file 争用。
