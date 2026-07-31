# diagnosis-service Java 8 构建修复报告

> 执行日期：2026-07-30（Asia/Shanghai）
> Base Branch：`agent/enterprise-agent-refactoring-plan`
> Base SHA：`bff949a90ac87f61968bbc6e9e00ce19bd4cfd2c`
> 修复分支：`agent/phase-a1-diagnosis-build-repair`

## 1. 范围与证据边界

本任务只恢复 `diagnosis-service` 在 Java 8 下的干净构建，并为实际修改增加最小回归测试。它不是 A2，也不实施 Shared Contracts、State Committer、LangGraph、Capability Package、Model Runtime、Patient Evidence UI、数据库迁移或框架升级。

A1 原始失败证据仍然有效，代表修复前历史快照。本报告只记录后续修复状态。构建通过不等于服务启动、数据库连接、远程调用或固定 Workflow 运行通过。

## 2. 修复前基线

工作目录：`D:\project\AIdoctor\diagnosis-service`

| 命令 | 退出码 | 结果 |
|---|---:|---|
| `java -version` | 0 | Eclipse Temurin OpenJDK `1.8.0_412` |
| `mvn -version` | 0 | Apache Maven `3.9.12`，Java 8u412 |
| `mvn -B -ntp dependency:go-offline` | 0 | PASS；Maven 3.073 s，首次沙箱调用因网络权限失败后授权原样重跑 |
| `mvn -B -ntp clean compile` | 1 | FAIL；编译 146 个源文件，44 个错误，Maven 3.895 s |
| `mvn -B -ntp clean test` | 1 | FAIL；主源码编译阶段同样 44 个错误，未进入 Surefire |

当前复现结果与 A1 的 44 个错误一致。完整逐条清单见 [diagnosis-compile-error-inventory.csv](./diagnosis-compile-error-inventory.csv)。

## 3. 错误分类与根因合并

44 个编译错误合并为 7 个根因：

| 根因 | 错误数 | 分类 | 处理 |
|---|---:|---|---|
| CDP 调用方使用不存在的 `*Map` 别名 | 32 | `METHOD_NAME_MISMATCH` | 调用方改用实体已有规范 getter；List 字段保持 List |
| Agent 调用不存在的 `CDPManager.getCDP` | 3 | `MISSING_METHOD` | 改用语义相同的 `getCDPById`，不增加重复别名 |
| AgentLoop 三项临床规划方法缺失 | 3 | `MISSING_METHOD` / `CLINICAL_SEMANTIC_BLOCKER` | 保留调用并显式 fail-closed；未编造规则 |
| Java 8 使用 `Map.of` | 2 | `JAVA_VERSION_INCOMPATIBILITY` | 使用 Java 8 `HashMap` 加不可变包装，保留字段和不可变性 |
| ToolCaller 无效方法引用 | 1 | `INVALID_METHOD_REFERENCE` | 修复上游 Optional 查找后，既有 `CDP::getSessionId` 恢复有效 |
| CDPFieldWriter 把 Map 传给 List setter | 2 | `TYPE_MISMATCH` | 按字段执行精确 Map/List 校验和转换 |
| 不存在的 finalConclusion setter | 1 | `MISSING_SETTER` | 使用既有 `patient_state.conclusion_package`，不新增列或 JSON 字段 |

## 4. 活动路径核验

- `AgentLoop`、`ClinicalAgentBrain`、`ToolCaller`、`CDPFieldWriter` 等均带 Spring `@Component`/`@Service` 并形成注入图，因此作为 `ACTIVE_AGENT_PATH` 处理。
- 全仓源码搜索未发现 Controller、固定 Workflow、配置 Bean、反射或脚本调用 `ClinicalAgentBrain.runAgent`；目标重构文档将 Java AgentLoop 标为实验/待冻结路径。
- 当前 Controller 主链仍调用 `DiagnosisWorkflowOrchestrator`，本次未修改该类。
- Maven 默认打包全部 `src/main/java`，因此没有排除 Agent 代码。

## 5. 修改与理由

### 5.1 规范 API 与容器类型

调用方改用 `CDP.getPatientState/getTriage/getUncertainty/getDdx/getEvidenceGraph/getWorkupPlan/getManagementPlan`。`ddx`、`evidenceGraph`、`workupPlan`、`managementPlan` 始终保持 `List<Map<String,Object>>`，没有创建 `getDdxMap()`，没有 List/Map 强制互转。

`ClinicalAgentBrain` 和 `ToolCaller` 改用 `CDPManager.getCDPById`，保留原有 `Optional`、异常和 session fallback 语义。

### 5.2 CDPFieldWriter

- Map 字段只接受 Map；List 字段只接受元素为 Map 的 List；
- null 作为整个字段值按既有 CDP/JsonUtil 语义写入，读取时仍表现为空集合；
- 错误类型显式拒绝并返回 `false`，不写入空集合、不丢弃旧数据；
- 支持规范 List 路径，例如 `cdp.ddx[0].probability`；容器不匹配时拒绝；
- final conclusion 写入既有 `patient_state.conclusion_package`。

### 5.3 Java 8 EvidenceFusion

`Map.of` 替换为 Java 8 可用的局部 `HashMap` 并通过 `Collections.unmodifiableMap` 返回。字段仍为 `tool_id` 和 `value`；现有缺失 probability 的 `0.0` 默认行为不变；调用方不能修改冲突值 Map。

### 5.4 AgentLoop 临床语义阻塞

仓库中没有与 `checkRedFlagPriority`、`checkConflictReview`、`checkInsufficientEvidence` 等价的确定性实现。新增 `ClinicalSemanticBlockerException`，三项调用均显式抛出并沿 `AgentLoop.runLoop`、`ClinicalAgentBrain.runAgent` 传播；上层没有捕获后默认继续。固定 Workflow 不调用 AgentLoop，因此不受影响。

状态：编译阻塞已解决，但 AgentLoop 的临床策略仍为 `BLOCKED_FAIL_CLOSED`，不得称为可运行 Agent 路径。

### 5.5 CDP 快照 round-trip

`JsonUtil.fromJson` 对 CDP 使用与 `toJson` 相同的 field mapper，避免 Map/List getter 与底层 CLOB String 字段之间的 Jackson 类型漂移。数据库列、JSON 字段和版本快照格式未改变。

## 6. 新增测试

| 测试类 | 数量 | 覆盖 |
|---|---:|---|
| `CDPSerializationTest` | 3 | Map/List、null、空集合、嵌套数据、CDP JSON round-trip |
| `CDPFieldWriterTest` | 7 | 正确 Map/List、全部修复的 List 字段、null、空集合、错误类型、非 Map 元素、嵌套写入、结论包 |
| `EvidenceFusionTest` | 3 | Java 8 返回字段、不可变性、null payload、缺失 probability |
| `ClinicalAgentBrainTest` | 2 | 规范 Manager 方法、Optional 缺失异常语义 |
| `ToolCallerTest` | 2 | 规范 Manager 方法、DDX 保持 List |
| `AgentLoopFailClosedTest` | 3 | 红旗、冲突、证据不足均不能默认放行 |
| **合计** | **20** | failures 0，errors 0，skipped 0 |

这些是纯代码和 Mockito 单元测试，不是端到端测试，也未启动 Spring Context、数据库、Nacos、Redis 或远程服务。

## 7. 修复后构建结果

| 命令 | 退出码 | 源文件/测试 | 结果 | Maven 用时 |
|---|---:|---|---|---:|
| `mvn -B -ntp clean compile` | 0 | main 147 | PASS | 4.191 s |
| `mvn -B -ntp clean test` | 0 | main 147；test 6；tests 20 | PASS；0/0/0 | 7.327 s |
| `mvn -B -ntp clean package` | 0 | main 147；test 6；tests 20 | PASS；生成 `diagnosis-service-1.0.0-SNAPSHOT.jar` | 8.594 s |

既有 warning：`FeignTraceResponseInterceptor` 使用 deprecated API；`CDPController` 存在 unchecked 操作。本任务未通过 suppress、compiler exclude 或降低严格度隐藏警告。

## 8. 行为保护

完整清单见 [diagnosis-behavior-preservation-checklist.csv](./diagnosis-behavior-preservation-checklist.csv)。没有修改 Controller/Feign endpoint、数据库 Schema/migration、Prompt、模型 Provider、风险等级、红旗阈值、fallback、固定 Workflow、`cdpStatus`、患者输出、Evidence UI、Trace 协议或 A2 Python 文件。

## 9. UNKNOWN、BLOCKED 与剩余风险

### UNKNOWN

- 服务启动、Spring Bean 完整装配、数据库/Flyway、Redis/Nacos 和全部远程 Client；
- 固定 Workflow 正常/失败路径及患者结果；
- 真实 CDP 数据与历史快照兼容性；
- Agent 路径是否被仓库外部调用。

### BLOCKED

- AgentLoop 三项临床规划策略没有确定性实现；当前明确 fail-closed。
- 未验证 AgentLoop 运行时路径，不能升级为运行能力。

### 剩余风险

- 既有 JsonUtil 对非法 JSON 返回空集合的历史行为未在本修复中重构；
- 固定 Workflow 已知 TODO/fallback 行为仍保留；
- 单元测试覆盖本次修复点，不代表完整诊断系统临床正确性。

## 10. 证据等级结论

- `diagnosis-service`：达到 `BUILD_VERIFIED`，因为干净 compile/test/package 均通过且 tests > 0。
- CDP 序列化与类型、CDPFieldWriter、EvidenceFusion Java 8 行为、Manager/ToolCaller 规范调用、AgentLoop fail-closed：达到局部 `TEST_VERIFIED`。
- 没有服务启动和真实调用证据，因此仍无 `RUNTIME_VERIFIED`。
- 没有真实数据库/缓存/图谱样本，因此仍无 `DATA_VERIFIED`。

## 11. 复现步骤

```powershell
git switch agent/phase-a1-diagnosis-build-repair
Set-Location diagnosis-service
java -version
mvn -version
mvn -B -ntp dependency:go-offline
mvn -B -ntp clean compile
mvn -B -ntp clean test
mvn -B -ntp clean package
```
