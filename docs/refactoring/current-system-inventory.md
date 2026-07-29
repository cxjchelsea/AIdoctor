# AIdoctor 当前系统资产盘点

> 文档状态：Draft v2.5 Inventory  
> 更新时间：2026-07-29  
> 盘点范围：`cxjchelsea/AIdoctor` 当前重构分支及其继承的主分支代码  
> 关联文档：[总体架构与模块设计](./overall-architecture-and-module-design.md)

---

## 1. 盘点目的

本文档回答：

- 当前仓库实际包含哪些服务、模块和基础设施；
- 当前系统真实的职责如何分布；
- 哪些资产已通过代码抽样确认；
- 哪些结论只来自文档声明，仍需编译、运行和测试验证；
- 哪些结构性问题会影响新架构落地。

本文档不是最终迁移决策，迁移结论见 [Migration Matrix](./migration-matrix.md)。

---

## 2. 证据等级

| 等级 | 含义 |
|---|---|
| E1 | 已读取实际源代码或构建配置 |
| E2 | 已读取服务级实现文档或 README，尚未验证运行 |
| E3 | 仅由命名、设计文档或历史说明推断 |
| V0 | 未编译、未启动、未运行测试 |
| V1 | 静态接口和依赖已确认 |
| V2 | 本地编译或单元测试通过 |
| V3 | 服务启动和集成调用通过 |
| V4 | 端到端和非功能验收通过 |

当前盘点主要处于 `E1/E2 + V0/V1`。任何 `KEEP` 或 `REMOVE` 结论都不能只依赖 README 中的完成度百分比。

---

## 3. 当前顶层结构

根据仓库 README、Docker Compose 和已抽样源代码，当前主要结构为：

```text
AIdoctor/
├── diagnosis-service/                  Java，CDP 与固定诊断编排
├── examination-service/                Java，检查业务框架
├── health-state-assessment-service/    Python，健康/入口/风险初筛
├── clinical-parsing-service/           Python，病例解析与概念归一化
├── dialog-service/                     Python，信息缺口、问答、上下文
├── diagnosis-engine-service/           Python，多引擎诊断与知识图谱
├── workup-planner-service/             Python，检查建议
├── treatment-engine-service/           Python，治疗与用药建议
├── risk-assessment-service/            Python，风险、分诊和升级
├── explanation-service/                Python，证据链和自然语言解释
├── ocr-service/                        Python，OCR
├── execution-trace-service/            自定义执行追踪，实际目录需进一步确认
├── frontend/                           React + TypeScript
├── common/aidoctor_llm/                公共 LLM 适配
├── science/                            科研与原型内容
├── scripts/                            批处理与辅助脚本
├── docs/                               大量历史设计和开发文档
└── docker-compose.yml
```

### 3.1 结构特征

当前系统是：

> Java 固定 Workflow 编排 + 多个按“脑区/工具”拆分的 Python FastAPI 服务 + React 前端 + MySQL/Oracle/Redis/Neo4j/Nacos。

当前物理服务边界与目标 11 个职责模块并不一致。

---

## 4. 构建与运行技术栈

### 4.1 Java

已确认：

- Spring Boot 2.7.8；
- Java 8；
- Spring Web、JPA、Redis、WebSocket；
- OpenFeign；
- Nacos Discovery；
- Oracle JDBC 与 MySQL JDBC 同时存在；
- Flyway 7.15.0；
- Actuator 与 Micrometer Prometheus；
- Spring Boot Test。

#### 风险

- Java 8 与 Spring Boot 2.7 已进入较老技术基线，是否升级需要独立 ADR；
- Oracle/MySQL 双适配增加迁移复杂度；
- Nacos、Feign 和多微服务结构可能不再适合首版收拢后的 Agent Runtime；
- 已有 Prometheus 指标不等于已有 OpenTelemetry Trace。

### 4.2 Python

抽样服务已确认：

- FastAPI 0.104.1；
- Uvicorn 0.24.0；
- Pydantic 版本不完全一致：`2.5.0` 与 `>=2.7.0` 并存；
- HTTPX；
- Prometheus Client；
- Redis、WebSocket；
- Neo4j、scikit-learn、Transformers、spaCy 等按服务分散依赖；
- 多个服务通过 `-e ../common/aidoctor_llm` 引用公共 LLM 包。

#### 风险

- 各服务依赖版本不统一；
- 同类中间件、日志、指标和异常处理重复；
- Pydantic 数据模型分散定义，容易出现同名不同义；
- 多个 Python 服务为每个临床步骤独立部署，增加跨服务状态与版本协调成本。

### 4.3 前端

已确认：

- React 18；
- TypeScript 5；
- Vite 5；
- Ant Design；
- Zustand；
- Axios；
- React Flow；
- SockJS + STOMP；
- 具备 `dev/build/lint`，未发现 `test` script。

#### 风险

- 当前实时通信绑定 WebSocket/STOMP 设计；
- 新 Runtime 的 Interrupt/Resume、SSE/WS 与 ReviewTask 需要重新定义前端状态机；
- Trace 管理页面可能绑定自定义事件模型。

---

## 5. 当前容器和基础设施

Docker Compose 当前显式包含：

- `diagnosis-service`；
- `examination-service`；
- `diagnosis-engine-service`；
- `workup-planner-service`；
- `treatment-engine-service`；
- `risk-assessment-service`；
- `ocr-service`；
- MySQL 8；
- Redis 7；
- Neo4j 5；
- Nacos 2.2.3。

### 5.1 未在 Compose 中完整体现的服务

README 描述但当前 Compose 未显式编排：

- health-state-assessment-service；
- clinical-parsing-service；
- dialog-service；
- explanation-service；
- execution-trace-service；
- frontend；
- Milvus。

### 5.2 明显不一致

- diagnosis-engine-service 配置了 `MILVUS_HOST=milvus`，但 Compose 未定义 Milvus；
- diagnosis-service 环境中只显式配置 diagnosis-engine 和 OCR URL，其他下游服务依赖可能位于配置文件；
- Nacos 配置使用 MySQL 中的 `nacos` 数据库，但 Compose 只显式创建 `aidoctor` 数据库；
- Compose 包含硬编码默认密码；
- 没有 OTel Collector、Tempo/Jaeger、Prometheus Server、Loki 或 Grafana。

### 5.3 当前判断

现有 Compose 更接近局部开发编排，而不是完整可重复的生产环境定义。

---

## 6. diagnosis-service 盘点

### 6.1 当前职责

代码抽样确认：

- 对外暴露诊断启动、继续、状态和结果接口；
- 创建 CDP；
- 调用健康状态判定服务；
- 根据结果进入 clinical mode 或 wellness mode；
- 组织固定 Workflow；
- 直接构造下一动作和用户响应；
- 管理 CDP 版本；
- 使用自定义 TraceContext；
- 保留旧接口兼容。

### 6.2 当前 CDP 模型

当前 `CDP` 为单表聚合，包含多个 CLOB JSON 字段：

- health_state_assessment；
- wellness_plan；
- patient_state；
- ddx；
- evidence_graph；
- workup_plan；
- management_plan；
- triage；
- uncertainty；
- audit_info；
- execution_trace。

特点：

- 使用 Map/List 作为应用层类型；
- 字段语义较宽；
- 临床事实、推断、计划、审计和执行追踪位于同一聚合；
- execution trace 被写入 CDP；
- conclusion package 被塞入 patient_state；
- 缺少 SourceArtifact 和 Observation 级来源关系。

### 6.3 当前并发和版本

`CDPManager` 已有：

- 版本号；
- 写时版本副本；
- 悲观锁 `SELECT FOR UPDATE`；
- 更新重试；
- Map 驱动的字段更新。

### 6.4 主要问题

- 任意 Map 更新不能表达字段级权限和来源；
- 悲观锁与未来 Resume/多 Actor 并发模型不匹配；
- CDP 同时承担 Encounter 状态、临床事实、推断、执行 Trace 和交付；
- Java 编排直接决定临床流程，不利于 Python LangGraph 成为唯一 Runtime；
- 健康判定服务失败时默认继续临床流程，安全降级策略需要重新评审；
- 当前事务内存在远程调用与复杂业务编排风险。

### 6.5 可复用资产

- 对外业务 API 形态；
- Encounter/CDP 创建和查询经验；
- 旧接口兼容思路；
- 版本记录代码；
- JPA/Flyway 基础；
- 固定 Workflow 作为 fallback 的业务案例；
- 部分响应 DTO 和错误处理。

---

## 7. clinical-parsing-service 盘点

### 7.1 当前职责

代码确认：

- 文本病例解析；
- 症状、体征、病史、药物、过敏和检查抽取；
- CUI/ICD/SNOMED/LOINC/ATC 归一化；
- ToolContext 读取 CDP；
- 返回统一 ToolResult；
- 生成 `suggested_writes` 写回 `cdp.patient_state`。

### 7.2 可复用资产

- 词表和标准编码映射；
- 症状、药物、过敏和检查抽取逻辑；
- 结构化结果转换；
- ToolResult 适配经验；
- 相关回归样例和服务文档。

### 7.3 主要问题

- 输出仍以大块 `patient_state` Map 写回；
- 没有 Observation Candidate、SourceArtifact、asserted_by、status、valid time；
- 部分缺失值被代码直接设置为 `None`，需要明确“不知道”而非补齐；
- confidence、completeness、accuracy 使用固定常量；
- 证据对象把概念归一化和临床证据混为一个统一结构；
- 自己读取 CDP，增加工具对状态存储的耦合。

---

## 8. dialog-service 盘点

### 8.1 当前职责

代码确认该服务同时承担：

- 下一问题生成；
- 用户输入理解；
- 信息缺口识别；
- 字段配置；
- 分流路径；
- 关键证据采集；
- WebSocket；
- 对话上下文清理；
- CDP 读取；
- ToolResult 适配；
- suggested writes；
- Prometheus 指标；
- 进程资源后台采集。

### 8.2 结构问题

这是当前职责混合最明显的服务之一，横跨：

- Clinical Intelligence；
- Context & Memory；
- Agent Runtime；
- Business Communication；
- Tool Governance；
- Clinical State。

### 8.3 可复用资产

- 信息缺口识别；
- 避免重复提问；
- 问题模板和表达；
- NLU/NLG 适配；
- 现有 WebSocket 交互经验；
- ToolContext/ToolResult 尝试；
- Prometheus 指标代码。

### 8.4 需要拆出的内容

```text
Information Gap / Question Selection
→ Clinical Intelligence

Question Wording / NLG
→ Model Governance + Delivery

Conversation Window / Summary
→ Context & Memory

WebSocket / 用户通信
→ Business & Care Delivery

CDP direct read/write
→ Runtime Context + State Committer

Route and loop decision
→ Agent Runtime
```

---

## 9. health-state-assessment-service 盘点

### 9.1 当前职责

代码确认：

- 入口判定；
- 工作态判断；
- 风险等级；
- 红旗和危险信号；
- 健康管理 A1-A5 流程；
- ToolContext/CDP 读取；
- suggested writes；
- 健康计划生成。

### 9.2 主要问题

- Safety、工作态路由、健康管理业务和随访混在一个服务；
- 规则输出可直接建议写入 health_state_assessment/wellness_plan；
- 与 risk-assessment-service 存在风险和分诊职责重叠；
- 健康管理流程不属于首个呼吸道临床 Agent 主链路的必要前置。

### 9.3 可复用资产

- 红旗规则；
- 风险分级规则；
- 特殊场景和输入校验；
- 健康管理流程可作为后续独立 Capability；
- 规则测试案例。

---

## 10. diagnosis-engine-service 盘点

### 10.1 当前职责

文档和依赖确认：

- 规则引擎；
- Neo4j 知识图谱推理；
- 统计模型；
- LLM 推理；
- 鉴别诊断；
- 多引擎加权融合；
- 三层候选分类；
- 证据链和路径。

### 10.2 可复用资产

- Neo4j 客户端和路径检索；
- 路径评分与可视化数据；
- 规则引擎骨架；
- 多引擎 Adapter；
- 候选分层概念；
- 现有病例和推理样例。

### 10.3 风险

- 多引擎融合分数不应直接作为临床置信度；
- LLM、规则、KG 和统计结果需要统一为 DiagnosticHypothesis Candidate；
- 必须明确支持、反对和缺失证据；
- Milvus 配置与 Compose 不一致；
- KG/RAG 与 Evidence Intelligence 边界未明确；
- 当前服务是否真正具有完整模型和规则库需要运行验证。

---

## 11. risk-assessment-service 盘点

### 当前职责

- 风险评估；
- 分诊；
- 升级规则；
- 终点结论包。

### 问题

- 与 health-state-assessment 的红旗、风险和分诊重叠；
- Conclusion Package 不属于 Safety Engine；
- 分诊规则、风险预测和输出打包需要拆分；
- 必须确认高风险路径是否可被其他 Planner 覆盖。

### 可复用资产

- 分诊规则；
- 升级条件；
- reason codes；
- 高风险测试样例。

---

## 12. workup、treatment、explanation 和 OCR

### 12.1 workup-planner-service

可复用：检查建议和验证计划领域逻辑。  
风险：首个 Capability 不需要完整检查规划；未来必须作为受控 Tool，由医生和证据策略约束。

### 12.2 treatment-engine-service

可复用：领域术语、药物数据和历史原型。  
风险：治疗和用药属于高风险能力，当前不应进入自动生产主链路。

### 12.3 explanation-service

可复用：自然语言表达、证据路径展示和结论包展示。  
风险：当前可能同时承担证据真值、临床解释和交付，必须拆分为 Evidence、Decision Record 与 Presentation。

### 12.4 ocr-service

可复用：OCR Adapter。  
风险：OCR 文本不能直接成为临床事实，必须保留 SourceArtifact、质量状态和人工确认路径。

---

## 13. examination-service 盘点

当前为 Spring Boot 框架，README 标注多项 TODO。

判断：

- 现有框架本身价值有限；
- 检查上传、存储、OCR 编排属于 Business & Care Delivery / Evidence Intake；
- 不应继续与 workup planner 混淆；
- 可保留 API 需求和上传业务经验，具体实现需按 SourceArtifact 和 Consent 重写。

---

## 14. execution trace 与可观测性

### 14.1 当前已知能力

README 描述：

- WebSocket 实时事件；
- 数据流转图；
- 服务调用图；
- 时间线；
- 模块调用树；
- 耗时和错误统计。

Java CDP 还直接包含 `execution_trace` 字段，自定义 `TraceContext` 用于 cdpId 传播。

### 14.2 主要问题

- 技术 Trace、Agent 事件和业务状态可能混合；
- Trace 摘要位于 CDP；
- 当前不是标准 OpenTelemetry Trace；
- 自定义 Trace 不能承担 Checkpoint、ClinicalDecisionRecord 或 Audit；
- PHI 字段策略未确认。

### 14.3 可复用资产

- AgentEvent 时间线 UI；
- 执行树展示；
- 调试页面；
- trace/cdp 关联经验；
- 业务事件可视化。

---

## 15. common/aidoctor_llm

### 目标价值

- 统一模型供应商调用；
- 保留 OpenAI、Ollama 等 Adapter；
- 作为 Model Registry/Router 的底层 Provider Adapter。

### 需要验证

- 是否携带完整 Prompt/PHI 日志；
- 超时、重试和流式输出；
- 模型版本和路由信息；
- 结构化输出校验；
- Token/成本记录；
- 是否被各服务以不一致方式调用。

---

## 16. 文档、science 与 scripts

### 16.1 docs

当前包含多代架构、五脑思想、业务方案和开发过程文件。

风险：

- 多份文档可能同时声称为当前真值；
- 旧“脑区”边界与新模块边界冲突；
- README 完成度不等同运行证据。

建议：

```text
docs/refactoring/       当前架构真值
docs/archive/           旧设计和历史过程
docs/modules/           后续模块详细设计
docs/contracts/         契约说明
docs/adr/               决策记录
```

### 16.2 science

归入实验和研究，不进入生产依赖图。

### 16.3 scripts

分为：

- production-ops：需要测试和维护；
- migration：一次性数据迁移；
- developer-tools：本地辅助；
- experiments：归档。

任何脚本在分类前不得进入生产发布流程。

---

## 17. 跨系统结构问题

### 17.1 数据契约碎片化

- Java DTO、CDP Map、Python Pydantic 模型各自定义；
- ToolResult 在多个服务中复制；
- 字段路径依赖字符串；
- 版本和错误状态不统一。

### 17.2 状态所有权不清

- Java Orchestration、Python Tool、Dialog 和 Trace 都可能更新 CDP；
- Clinical Fact、Hypothesis、Plan、Audit、Trace 混在 CDP；
- 当前没有真正的 State Committer 门禁。

### 17.3 微服务拆分过早

- 多个 Python 服务按步骤拆分；
- 相同中间件重复；
- 每次状态更新需要跨服务读取 CDP；
- 版本、超时、错误和 Trace 难以统一。

### 17.4 安全职责重复

- health-state-assessment 与 risk-assessment 同时处理风险；
- Planner 与固定 Workflow 可能生成不同安全结论；
- 降级逻辑存在默认继续路径，需要专项评审。

### 17.5 可观测性未标准化

- 已有 Prometheus 指标和自定义 Trace；
- 缺少统一 OTel 上下文和 Collector；
- Trace、AgentEvent、ClinicalDecisionRecord、Audit 未分库分责。

### 17.6 工程一致性不足

- Python 版本依赖不统一；
- Compose 不覆盖所有服务；
- 默认密码和环境配置不适合生产；
- 前端无显式测试 script；
- CI、自动契约测试和安全扫描状态仍需确认。

---

## 18. Phase A 进一步盘点清单

### 18.1 代码

- [ ] 每个服务入口和路由；
- [ ] 每个核心类、函数和依赖；
- [ ] 所有 CDP 读写位置；
- [ ] 所有远程调用；
- [ ] 所有后台任务；
- [ ] 所有 Trace/Log/Audit 写入；
- [ ] 所有模型调用；
- [ ] 所有 Prompt 和模板；
- [ ] 所有规则和词表；
- [ ] 所有 TODO、stub 和固定常量。

### 18.2 数据

- [ ] 数据库表和索引；
- [ ] Flyway 脚本；
- [ ] CDP 版本表；
- [ ] JSON/CLOB 实际样本；
- [ ] Redis key；
- [ ] Neo4j schema 和数据规模；
- [ ] 文件和报告存储；
- [ ] 患者标识和 Consent 数据。

### 18.3 运行

- [ ] Java 编译；
- [ ] Python 依赖安装；
- [ ] 前端 build/lint；
- [ ] Docker Compose 启动；
- [ ] 数据库初始化；
- [ ] 服务健康检查；
- [ ] 固定 Workflow E2E；
- [ ] 失败与降级测试；
- [ ] 性能基线；
- [ ] PHI 日志扫描。

---

## 19. 当前盘点结论

当前项目并非“完全不可用的旧代码”，已经包含大量有价值的领域原型：

- CDP 和版本管理；
- 临床解析与编码归一化；
- 信息缺口和追问；
- 红旗与分诊规则；
- 知识图谱和多引擎推理；
- 解释和可视化；
- ToolContext/ToolResult 尝试；
- Trace 管理 UI；
- React 交互界面。

但当前结构的主要问题是：

> 职责以“流程步骤/脑区服务”拆分，而新架构需要以状态所有权、安全边界和 Runtime 职责重新组织。

因此总体策略应为：

> **保留领域知识和经过验证的算法，重写共享契约和状态边界；用 Adapter 逐步迁移，不进行一次性全仓推倒。**
