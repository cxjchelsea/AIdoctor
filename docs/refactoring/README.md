# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

---

## 1. 当前状态

```text
v2.4：总体架构、主流程、11个模块、共享契约、Phase A-F
v2.5：现状盘点、迁移矩阵、生产工程、冻结流程
v2.6：Capability 扩场景、呼吸道 RAG V1、统一 Prompt/Model Runtime、跨文档一致性、旧设计资产继承
```

当前仍为 **Freeze Candidate**。目标架构和执行体系已经完整；最终 Frozen Baseline 还需要 Phase A 的真实编译、启动、数据库、旧资产、Prompt、模型、知识和 E2E 验证。

```text
目标架构重构
≠ 旧设计全部废弃

目标架构重构
= 旧设计资产验证与继承
+ 状态、安全、治理和工程边界修正
```

---

## 2. 开发入口阅读顺序

### 第一步：理解稳定架构

1. [总体架构与模块设计](./overall-architecture-and-module-design.md)  
   系统形态、主流程、11 个模块、共享契约、状态所有权和部署边界。

2. [架构冻结基线](./architecture-freeze-baseline.md)  
   冻结内容、ADR 规则、冻结门禁和文档优先级。

3. [v2.6 跨文档一致性补充](./v2.6-cross-document-consistency-addendum.md)  
   对总体蓝图和专题长文统一补充 Capability、RAG、知识图谱、Prompt/Model Runtime 和版本链。

### 第二步：理解如何扩展临床场景

4. [Capability Package 规范](./capability-package-specification.md)  
   定义“稳定平台 + 版本化 Capability Package = 新临床场景”。

5. [成人呼吸道 Medical RAG V1](./adult-respiratory-medical-rag-v1-design.md)  
   首个场景的知识内容、来源、摄取、Chunk、索引、检索、Citation、知识图谱和评估。

6. [模型调用与路由矩阵](./model-call-and-routing-matrix.md)  
   哪些任务使用模型、哪些保持确定性、Route、Fallback 和评估。

7. [Prompt 与 Model Runtime](./prompt-and-model-runtime-design.md)  
   Prompt Registry/Loader/Builder、Model Registry/Router/Gateway、Provider Adapter 和 Validator。

### 第三步：理解旧资产如何继承和迁移

8. [原设计资产保留、改造与目标架构映射](./legacy-design-asset-retention-and-mapping.md)  
   对 `docs/AI医生/项目文档` 中双通道、Tool Contract、CDP、AgentState、AuditTrail、AOP、错误处理、评估集、知识演化和临床流程进行 KEEP/ADAPT/EXTRACT/EVALUATE 映射。

9. [当前系统资产盘点](./current-system-inventory.md)  
   Java/Python/Frontend/Storage，以及 Capability、Prompt、Model Call、Knowledge 和 Neo4j 专项 Inventory。

10. [代码与资产迁移矩阵](./migration-matrix.md)  
    KEEP、ADAPT、WRAP、REWRITE、ARCHIVE、REMOVE、SPLIT、MERGE、EXTRACT、EVALUATE。

11. [目标能力覆盖矩阵](./coverage-matrix.md)  
    每项能力和保留资产的 Owner、Phase、Contract、实现、测试和完成门禁。

### 第四步：照路线实施

12. [可执行实施路线](./implementation-roadmap.md)  
    Phase A-F 的代码、旧资产验证、数据、前端、RAG、Model Runtime、评估、发布和下线任务。

13. [数据与基础设施迁移](./data-and-infrastructure-migration.md)  
    PostgreSQL、旧 CDP、Checkpoint、Capability/Prompt/Model/Knowledge Schema、pgvector、BM25、Neo4j、OTel 和回滚。

14. [前端与业务迁移](./frontend-and-business-migration.md)  
    v1/v2 API、患者端、医生端、管理端、Evidence、Release 管理和实时通信。

15. [工程、发布、回滚与下线](./engineering-release-and-decommission-plan.md)  
    CI、测试、Prompt/Model/Knowledge/Capability Release Gate、旧资产替代验证、灰度、回滚、备份和旧服务下线。

---

## 3. 文档优先级

```text
Architecture Freeze Baseline
→ v2.6 Cross-document Consistency Addendum
→ Legacy Design Asset Retention and Mapping
→ Overall Architecture
→ Capability / RAG / Model 详细设计
→ Inventory / Migration / Coverage / Roadmap / Engineering
→ 四份专题长文
→ docs/AI医生/项目文档 等历史设计
```

`legacy-design-asset-retention-and-mapping.md` 是以下文档的权威补充：

- `current-system-inventory.md`：增加 Legacy Design Asset Inventory；
- `migration-matrix.md`：增加设计原则、协议、状态、AOP、评估和知识治理资产；
- `coverage-matrix.md`：要求保留资产绑定 Contract、Phase、Test 和 Gate；
- `implementation-roadmap.md`：在 A6 与 A7 之间增加 A6.5 Legacy Design Asset Validation；
- `engineering-release-and-decommission-plan.md`：增加旧资产替代和下线门禁。

旧文档中的“已完成”描述不能代替代码、测试和运行证据。

---

## 4. 四份专题长文

1. [企业级临床 Agent 重构主方案](./enterprise-agent-refactoring-plan.md)  
   Clinical Intelligence、Safety Loop、Diagnostic Loop、医生接管、Delivery、Care Navigation 和 Follow-up。

2. [临床数据与循证智能扩展](./clinical-data-and-evidence-intelligence-extension.md)  
   Longitudinal Record、Encounter CDP、Evidence Ledger、Evidence Intelligence、PICO、引用和研究治理。

3. [Agent Runtime Foundations 扩展](./agent-runtime-foundations-extension.md)  
   Context、Memory、Patient/Medical RAG、Skill、Model Router、Security 和 Eval。

4. [Durable Execution 与可观测性扩展](./durable-execution-and-observability-extension.md)  
   Thread、Checkpoint、Interrupt/Resume、Lease、Outbox/Inbox、Replay、OTel、Audit。

专题长文未逐字复制 v2.6 新设计；涉及 Capability、RAG、Knowledge Graph、Prompt/Model Runtime、版本链和旧设计资产继承时，以权威补充及对应详细设计为准。

---

## 5. 系统定位

```text
目标系统
= 确定性的安全与生命周期 Workflow
+ 受约束的 Agent 下一动作决策
```

不可绕过：Capability/Consent/Permission、输入质量、Context Policy、State Committer、Mandatory Safety、结果验证、Checkpoint、医生审核、输出安全和幂等。

动态范围：继续提问、检索患者历史或医学证据、调用批准 Tool/Skill、请求医生、停止并交付。

---

## 6. 场景扩展方式

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

首个 Capability 为 `adult_respiratory_v1`。它用于验证通用平台，不是永久限制到呼吸道。

新增场景必须增加 Scope、Terminology、Observation、Safety、Question、Hypothesis、Knowledge、Prompt、Model Route、Tool/Skill、Delivery 和 Eval，不允许只更换 Prompt。

旧设计中的问题清单、红旗、三层候选、验证计划和结论包必须先经过资产验证，再抽取进入 Capability Package。

---

## 7. 原设计资产继承原则

```text
Legacy Design Evidence
→ Inventory
→ Retention Decision
→ Target Mapping
→ Migration
→ Validation
→ Shadow / Rollback
→ Decommission
```

重点保留并改造：

- 双通道推理；
- 单一编排责任链；
- 无状态工具；
- ToolContext / ToolResult；
- CDP 聚合视图和 Copy-on-Write；
- AgentState 的预算、尝试、回退和停止条件；
- AuditTrail 追加写；
- AOP 横切追踪；
- 分层错误处理；
- Static / Interactive / Trajectory 三类评估集；
- Knowledge Sandbox/Staging/Publish Gate；
- 五步循证策略和终点结论包。

不原样继承：

- Tool 直接读取或写完整 CDP；
- Agent、LLM、规则或检索直接提交最终临床状态；
- Audit、Trace、Delivery 全放进大 JSON；
- Redis 作为唯一恢复事实源；
- 自定义 Header/UUID 替代标准 Trace；
- 图谱路径替代 Citation；
- 高风险治疗自动进入首个 Capability。

---

## 8. AOP 与 Trace 的目标形态

```text
@TraceExecution
→ 保留领域语义或迁移为 @WithSpan / @Observed

ExecutionTraceAspect
→ OpenTelemetry Span Adapter

TraceContext
→ OTel Context + MDC

Feign Trace Interceptor
→ W3C traceparent / tracestate

execution-trace-service
→ AgentEvent Store/UI + OTel Backend
```

AOP 可处理 Trace、Metric、Logging 和 PHI-safe 遥测元数据；不得隐藏执行红旗、分诊、状态提交、模型路由和医生审核等临床决策。

Trace 失败不得阻断临床主流程。

---

## 9. 统一 Model Runtime

```text
Graph Node / Clinical Module
→ Context Assembly
→ Prompt Loader / Builder
→ Model Router / Gateway
→ Provider Adapter
→ Structured Output Validator
→ Candidate / Decision / Draft
```

业务模块不得直接调用模型供应商 SDK。State Committer、最终 Safety/Triage、权限、Checkpoint/Resume、幂等和 Audit 保持确定性。

---

## 10. RAG V1 基线

```text
Source Registry
+ PostgreSQL Metadata
+ Structure-aware Chunk
+ BM25
+ pgvector
+ Reranker
+ Claim-Level Citation
+ EvidencePack
+ Knowledge Release
```

Patient RAG 与 Medical RAG 必须隔离。Knowledge Graph 是术语、多跳和 Query Expansion 增强，只有在来源治理和净收益评估通过后才能进入生产。

原知识演化设计中的 Sandbox、Staging、Publish Gate 和 Rollback 思想保留；自动知识演化 Agent 集群延后。

---

## 11. 11 个一级模块

| 编号 | 模块 | 核心职责 |
|---|---|---|
| 1 | Business & Care Delivery | 身份、Encounter、Review、Delivery、Navigation、Follow-up |
| 2 | Clinical Domain Contracts | Java/Python/DB/Event/Frontend 共享语言 |
| 3 | Clinical State & Data Foundation | CDP、Observation、Ledger、State Committer、长期记录 |
| 4 | Safety & Policy Engine | 红旗、分诊、Capability、权限和不可绕过策略 |
| 5 | Clinical Intelligence | 临床理解、候选、信息缺口、问题和停止策略 |
| 6 | Evidence Intelligence & RAG | Patient History、医学证据、Citation、适用性和冲突 |
| 7 | Agent Runtime | LangGraph、Route、Interrupt、Resume、Retry、Fallback |
| 8 | Context & Memory | Context、Summary、Token、Memory Gate |
| 9 | Tool, Skill & Model Governance | Tool、Skill、Prompt、Model、Capability 发布治理 |
| 10 | Durable Execution | Thread、Checkpoint、Lease、Outbox 和幂等 |
| 11 | Observability, Audit & Evaluation | OTel、AgentEvent、Decision、Audit、Replay、Eval |

Capability、Prompt/Model Runtime、Knowledge Release 和 Legacy Asset Mapping 是现有模块内部能力，不新增第十二个一级模块。

---

## 12. 实施路线一览

### Phase A：真实基线、资产继承与冻结

- Java/Python/Frontend 编译、启动和测试；
- 全量 Service/API/Table/Prompt/Model/Knowledge/Rule Inventory；
- Legacy Design Asset Inventory 与代码证据映射；
- Shared Contracts；
- Capability/Prompt/Model/Knowledge Schema；
- 数据库、Runtime、RAG、Graph 和框架 ADR；
- CI 和固定 Workflow 基线；
- Freeze Review。

### Phase B：临床状态和 Safety

- EncounterCDP、Observation、SourceArtifact；
- StatePatch/CommitResult、State Committer；
- Legacy CDP Adapter；
- `adult_respiratory_v1` Safety Pack；
- v2 Business API 基础。

### Phase C：Agent Runtime 和 Model Runtime

- FastAPI + LangGraph；
- ContextEnvelope、QuestionDecision；
- Prompt Loader/Builder、Model Gateway、ProviderAdapter；
- extraction/question wording routes；
- Checkpoint、Interrupt/Resume；
- OTel v1、AOP 迁移和 Crash Matrix。

### Phase D：有限推理和 RAG

- DiagnosticHypothesis；
- Patient History/Memory；
- Source Registry、Knowledge Release；
- PostgreSQL/pgvector/BM25/Reranker；
- EvidencePack/Citation；
- Knowledge Graph 对照实验；
- OCR/Artifact。

### Phase E：医生审核和业务闭环

- ReviewTask、Clinician Resume；
- 三类 Delivery；
- Care Navigation、Follow-up；
- Outbox/Inbox、模拟外部动作；
- 医生端和患者闭环。

### Phase F：生产治理和下线

- 完整 Tool/Skill/Prompt/Model/Capability Governance；
- Durable Hardening；
- OTel Stack、Eval、Replay；
- Shadow/Canary；
- Backup/DR；
- 旧服务、旧库、旧索引和旧 Trace 链路下线。

---

## 13. Phase A 正式执行顺序

```text
A1 Java 基线
→ A2 Python 基线
→ A3 Frontend 与 Docker 基线
→ A4 数据资产盘点
→ A5 Shared Contracts v1
→ A6 adult_respiratory_v1 Capability Package 骨架
→ A6.5 Legacy Design Asset Validation
→ A7 Model Runtime 骨架
→ A8 ADR
→ A9 CI 与开发环境
→ A10 固定 Workflow、AOP/Trace 基线
→ A11 Freeze Review
```

A6.5 必须输出：

```text
legacy-design-asset-inventory.csv
legacy-design-code-evidence.csv
legacy-contract-extraction.md
legacy-clinical-policy-extraction.md
legacy-eval-asset-inventory.csv
legacy-observability-migration.md
legacy-asset-decommission-register.csv
```

在 A11 前，不应直接建设完整 LangGraph、完整医学知识库或大规模知识图谱，也不允许未经验证删除旧服务、规则、Prompt、评估集或 AOP/Trace 资产。

---

## 14. 防遗漏和下线规则

所有新增能力和保留资产必须进入 Coverage Matrix：

```text
Legacy Asset / Target Capability
→ Owner Module
→ Phase
→ Contract
→ Implementation
→ Validation Suite
→ Completion Gate
```

所有旧资产变化更新 Migration Matrix；所有架构偏离提交 ADR；所有生产变化绑定 Release Manifest。

旧资产不能因为新目录已创建就下线，必须满足：

```text
替代实现完成
+ 契约兼容验证
+ 回归通过
+ Shadow对比通过
+ 数据迁移或归档
+ Fallback可用
+ Rollback测试通过
+ 全仓引用为空
+ Owner批准
+ 回滚窗口结束
```

---

## 15. 当前下一步

文档体系已能够支撑开发，下一步停止继续扩展总体概念，正式执行 Phase A：

```text
真实运行
→ 全量盘点
→ Capability骨架
→ 旧设计资产验证
→ 契约和Schema
→ ADR
→ CI和固定Workflow基线
→ Frozen Baseline Review
```
