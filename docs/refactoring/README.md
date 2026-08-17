# AIdoctor 重构方案导航

> 当前整合版本：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-30  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

---

## 1. 当前状态

```text
v2.4：总体架构、主流程、11 个模块、共享契约、Phase A-F
v2.5：现状盘点、迁移矩阵、生产工程、冻结流程
v2.6：临床场景扩展、呼吸道 RAG V1、统一 Prompt/Model Runtime、旧设计资产继承和患者证据 UI 契约
```

当前仍为 **Freeze Candidate**。目标架构和执行体系已经完整；最终 Frozen Baseline 仍需要 Phase A 的真实编译、启动、数据库、旧资产、Prompt、模型、知识和 E2E 验证。

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

1. [总架构与模块设计](./总架构与模块设计.md)  
   系统形态、主流程、11 个模块、共享契约、状态所有权和部署边界。

2. [架构冻结基线](./架构冻结基线.md)  
   冻结内容、ADR 规则、冻结门禁和文档优先级。

3. [v2.6 跨文档一致性补充](./v2.6跨文档一致性补充.md)  
   对总体蓝图和专题长文统一补充 Capability、RAG、知识图谱、Prompt/Model Runtime 和版本链。

### 第二步：理解临床场景与模型、知识能力

4. [Capability Package 规范](./capability-package规范.md)  
   定义每个临床场景需要提供的范围、规则、知识、运行约束和评估资产。

5. [成人呼吸道 RAG](./成人呼吸道RAG.md)  
   首个场景的知识来源、摄取、Chunk、索引、检索、Citation、知识图谱和评估。

6. [模型调用与路由矩阵](./模型调用与路由矩阵.md)  
   哪些任务使用模型、哪些保持确定性、Route、Fallback 和评估。

7. [Prompt 与 Model Runtime 设计](./prompt-and-model-runtime设计.md)  
   Prompt Registry/Loader/Builder、Model Registry/Router/Gateway、Provider Adapter 和 Validator。

### 第三步：理解患者、医生和前端交付

8. [前端与业务迁移](./前端与业务迁移.md)  
   v1/v2 API、患者端、医生端、管理端、Evidence、Release 管理和实时通信。

9. [患者端证据与引用 UI 契约](./患者端证据与引用UI契约.md)  
   PatientDelivery、证据卡、来源引用、冲突和局限展示、版本一致性、权限与 Contract Test。

### 第四步：理解旧资产如何继承和迁移

10. [原设计资产保留、改造与目标架构映射](./原设计资产保留、改造与目标架构映射.md)  
    对 `docs/AI医生/项目文档` 中双通道、Tool Contract、CDP、AgentState、AuditTrail、AOP、评估集、知识演化和临床流程进行映射。

11. [当前系统资产盘点](./当前系统资产盘点.md)  
    Java、Python、Frontend、Storage，以及 Capability、Prompt、Model Call、Knowledge 和 Neo4j 专项 Inventory。

12. [代码、数据与设计资产迁移矩阵](./代码、数据与设计资产迁移矩阵.md)  
    KEEP、ADAPT、WRAP、REWRITE、ARCHIVE、REMOVE、SPLIT、MERGE、EXTRACT、EVALUATE 和 DEFER。

13. [目标能力与旧设计资产覆盖矩阵](./目标能力与旧设计资产覆盖矩阵.md)  
    每项能力和保留资产的 Owner、Phase、Contract、实现、测试和完成门禁。

### 第五步：照路线实施

14. [可执行实施路线](./可执行实施路线.md)  
    Phase A-F 的代码、旧资产验证、数据、前端、RAG、Model Runtime、评估、发布和下线任务。

15. [数据与基础设施迁移](./数据与基础设施迁移.md)  
    PostgreSQL、旧 CDP、Checkpoint、Schema、pgvector、BM25、Neo4j、OTel 和回滚。

16. [工程、发布、回滚与下线](./工程、发布、回滚与下线.md)  
    CI、测试、Release Gate、灰度、回滚、备份和旧服务下线。

---

## 3. 四份专题长文

1. [企业级临床 Agent 重构主方案](./企业级临床agent重构主方案.md)  
   Clinical Intelligence、Safety Loop、Diagnostic Loop、医生接管、Delivery、Care Navigation 和 Follow-up。

2. [临床数据与循证智能扩展方案](./临床数据与循证智能扩展方案.md)  
   Longitudinal Record、Encounter CDP、Evidence Ledger、Evidence Intelligence、PICO、Citation 和研究治理。

3. [Agent Runtime Foundations 扩展](./agent-runtime-foundations-扩展.md)  
   Context、Memory、Patient/Medical RAG、Skill、Model Router、Security 和 Eval。

4. [Durable Execution 与可观测性扩展方案](./Durable%20Execution%20与可观测性扩展方案.md)  
   Thread、Checkpoint、Interrupt/Resume、Lease、Outbox/Inbox、Replay、OTel 和 Audit。

专题长文未逐字复制所有 v2.6 新设计；涉及 Capability、RAG、Knowledge Graph、Prompt/Model Runtime、患者证据 UI、版本链和旧设计资产继承时，以冻结基线、对应详细设计和执行文档为准。

---

## 4. 文档优先级

```text
架构冻结基线
→ v2.6 跨文档一致性补充
→ 原设计资产保留、改造与目标架构映射
→ 总架构与模块设计
→ Capability / RAG / Model / Patient Evidence UI 详细设计
→ Inventory / Migration / Coverage / Roadmap / Engineering
→ 四份专题长文
→ Future Extension Design
→ docs/AI医生/项目文档 等历史设计
```

Future Extension Design 只沉淀当前冻结架构之上的未来扩展点，默认状态为 `FUTURE_EXTENSION_DESIGN / NOT_AUTHORIZED`，不得反向覆盖当前 Frozen/Freeze Candidate 架构、Shared Contracts v1 或实施 Scope。

旧文档中的“已完成”描述不能代替代码、测试和运行证据。

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
+ Versioned Clinical Scenario Package
= New Supported Clinical Scenario
```

首个场景为 `adult_respiratory_v1`。它用于验证通用平台，不是永久限制到呼吸道。

A6 已建立 `adult_respiratory_v1` 的 `DRAFT` 结构骨架（`PARTIALLY_VALIDATED`）：可解析、可校验、可评审，但临床内容仍为 `REQUIRES_CLINICAL_REVIEW`，runtime 未接入，生产资格为 `BLOCKED`。详见 [A6 Evidence](./evidence/phase-a/a6/capability-package-skeleton-report.md)。A6.5 已完成 A、B-Core、非临床 C 与 D01 等合并验证工作。Enterprise 当前真值：PR #38 `MERGED_AND_VERIFIED`（`6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda`）；Owner `LEGACY_CLINICAL_ASSETS_WILL_NOT_BE_MIGRATED`（`A6.5-NONADOPT-001`，`MERGED_AND_VERIFIED`）；B04 = `SUPERSEDED_BY_LEGACY_CLINICAL_NON_ADOPTION` / Executed NO；C02 = `NOT_REQUIRED_FOR_REJECTED_ASSETS` / Executed NO；A6.5 = `LEGACY_GOVERNANCE_CLOSED`；A6.5 Exit = `PASS_LEGACY_GOVERNANCE_ONLY`（≠ A6 COMPLETE / A7-CL READY）。PR #39 E01 与 PR #40 E02 均为 `MERGED_AND_VERIFIED`。不得写成 A6/A7/Frozen Baseline/NC-CLOSE-01 COMPLETE。详见 [Exit Review](./evidence/phase-a/a6-5/a6-5-exit-review-2026-08-14.md) 与 [non-adoption strategy](./plans/phase-a/a6-5/a6-5-legacy-clinical-non-adoption-strategy.md)。A7 overall 为 `NOT_COMPLETE`；[A7-NC 路线修订规划](./plans/phase-a/a7-non-clinical-roadmap-amendment.md)为历史 Model Runtime parallel-lane authority（PR #29），A7-NC 现为 `COMPLETE` 且 Exit `PASSED`（≠ A7 COMPLETE）。当前控制真值见 [2026-08-14 post-PR40 reconciliation](./plans/phase-a/phase-a-current-state-reconciliation-2026-08-14-post-pr40.md)。Phase A Non-Clinical Closure 路线修订已随 PR #38 合并（`PHASE_A_NC_CLOSURE` implementation / A8–A11 仍 `NOT_AUTHORIZED`）。A5 三语言 binding 仍为 A11 前置缺口。

新增场景必须考虑 Scope、Terminology、Observation、Safety、Question、Hypothesis、Knowledge、Prompt、Model Route、Tool/Skill、Delivery 和 Eval，不允许只更换 Prompt。

Capability Package 是跨模块的临床场景配置与发布单元，不是第十二个一级模块。

---

## 7. 患者证据展示原则

```text
经验证的患者事实
+ 确定性安全依据
+ Claim-Level 医学 Citation
+ 适用性、冲突与局限
+ 医生审核状态
→ PatientDelivery
```

患者端展示患者友好的结论、来源机构、日期、适用限制和不确定性；不得展示完整 Prompt、模型原始响应、未批准候选或私有推理。

紧急 Safety/Triage 不依赖 Medical RAG 在线成功。确定性安全依据与医学文献 Citation 必须分开展示。

---

## 8. 原设计资产继承原则

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

Patient RAG 与 Medical RAG 必须隔离。Knowledge Graph 只有在来源治理和净收益评估通过后才能进入生产，并且不能替代 Citation。

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

Capability、Prompt/Model Runtime、Knowledge Release、Patient Evidence UI 和 Legacy Asset Mapping 是现有模块内部能力，不新增一级模块。

---

## 12. 实施路线一览

### Phase A：真实基线、资产继承与冻结

- Java/Python/Frontend 编译、启动和测试；
- 全量 Service/API/Table/Prompt/Model/Knowledge/Rule Inventory；
- Legacy Design Asset Inventory 与代码证据映射；
- Shared Contracts 和 UI Contract Schema；
- Capability/Prompt/Model/Knowledge Schema；
- ADR、CI 和固定 Workflow 基线；
- Freeze Review。

### Phase B：临床状态和 Safety

- EncounterCDP、Observation、State Committer；
- Legacy CDP Adapter；
- `adult_respiratory_v1` Safety Pack；
- v2 Business API 基础。

### Phase C：Agent Runtime 和 Model Runtime

- FastAPI + LangGraph；
- ContextEnvelope、QuestionDecision；
- Model Runtime；
- Checkpoint、Interrupt/Resume；
- OTel v1、AOP 迁移和 Crash Matrix。

### Phase D：有限推理和 RAG

- DiagnosticHypothesis；
- Patient History/Memory；
- Medical RAG、EvidencePack 和 Citation；
- Knowledge Graph 对照实验；
- OCR/Artifact。

### Phase E：医生审核和业务闭环

- ReviewTask、Clinician Resume；
- 三类 Delivery；
- 患者证据卡与 Citation UI；
- Care Navigation、Follow-up；
- 医生端和患者端闭环。

### Phase F：生产治理和下线

- 完整治理、Durable Hardening、Eval 和 Replay；
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
→ A11 Frozen Baseline Review
```

A1–A5 已形成基线与 Shared Contracts v1 结构包；A6 Capability Package 骨架已建立但未临床批准、未接入 runtime、未升级 Frozen Baseline。历史执行顺序继续保留。历史 snapshot 见 [Phase A Current-State Addendum](./plans/phase-a/phase-a-current-state-addendum.md)；**当前控制真值**见 [2026-08-14 post-PR40 reconciliation](./plans/phase-a/phase-a-current-state-reconciliation-2026-08-14-post-pr40.md)：PR #38 / #39 / #40 已合并；E01/E02 为 `MERGED_AND_VERIFIED`；A6.5 为 `LEGACY_GOVERNANCE_CLOSED`；Exit 为 `PASS_LEGACY_GOVERNANCE_ONLY`。A7-NC 为 `COMPLETE` / Exit `PASSED`；A7-CL 为 `BLOCKED`；A7 overall 为 `NOT_COMPLETE`；`PHASE_A_NC_CLOSURE` implementation 与 A8–A11 仍 `NOT_AUTHORIZED`；Clinical Runtime 为 `NOT_ENABLED`；Production 为 `BLOCKED`。不得跳过新临床治理门禁直接填充阈值、规则、Prompt 或 route eligibility。FB-11 / FB-20 / FB-21 保留。

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

旧资产不能因为新目录已创建就下线，必须满足替代实现、契约兼容、回归、Shadow、数据迁移或归档、Fallback、Rollback、全仓无引用、Owner 批准和回滚窗口结束。

---

## 15. 当前下一步

`NC-CLOSE-01A` ADR decisions 现为 **`PROPOSED_DECIDED_PENDING_REVIEW`**。决策正文见 [NC-CLOSE-01A Runtime & State Boundaries](./plans/phase-a/nc-close-01a/nc-close-01a-runtime-state-boundaries.md)。本 PR 审查期间下一步仅为 **Combined Independent Review + Merge Review** of that Draft PR Head。`NC-CLOSE-01A` implementation 仍为 `NOT_AUTHORIZED`。`NC-CLOSE-01` umbrella implementation 仍为 `NOT_AUTHORIZED`；`01B` / `01C` 仍为 `NOT_AUTHORIZED`。不得将本指针写成 `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`。内部排序与决策深度见 [NC-CLOSE-01 ADR scope refinement](./plans/phase-a/nc-close-01-adr-scope-refinement.md)；lane 范围仍以 [NC Closure scope register](./plans/phase-a/phase-a-non-clinical-closure-scope-register.csv) 为准。A6.5 legacy governance 已关闭；不得因此写成 A6/A7 COMPLETE，不得启动 A7-CL，不得执行 B04 提取或 C02 拒收资产映射，也不得把未来新临床内容塞回 A6.5。历史 Phase A 证据、历史 addendum、PR #29 / P7 与 dated reconciliations 分别维护。

---

## 16. Future Extension Design

当前重构之外的未来平台扩展设计统一进入 [extensions/](./extensions/README.md)。

当前已沉淀：

1. [外部健康数据与设备连接扩展方案](./extensions/外部健康数据与设备连接扩展方案.md)  
   External Health Source、Connector Registry、Device Adapter，以及未来数字脉诊、舌象、Wearable、EHR 的统一接入边界。

2. [结构化医疗知识与专业 Tool 扩展方案](./extensions/结构化医疗知识与专业Tool扩展方案.md)  
   明确 Medical RAG、Knowledge Graph 与 Structured Medical Provider 的职责边界，并预设计 Drug / Regulatory / Device Intelligence Tool。

3. [vNext 临床 Capability 扩展设计](./extensions/vNext临床Capability扩展设计.md)  
   预设计 Temporal、Special Population、Chronic 和 Multimodal Capability，并记录 `tcm_four_diagnosis_v1` 等未来候选。

4. [外部产品参考与架构抽象矩阵](./extensions/外部产品参考与架构抽象矩阵.md)  
   维护 Health、freddy、Fitness AI Connector、DrugBank、Nyquist AI、Rhizome AI、Pregnancy Progress 等外部参考与 AIdoctor 平台抽象的映射。

这些文档均为 `FUTURE_EXTENSION_DESIGN / NOT_AUTHORIZED`。它们用于避免未来再次从零设计，但不改变当前 Phase、Runtime、Contracts、Capability 或生产资格。
