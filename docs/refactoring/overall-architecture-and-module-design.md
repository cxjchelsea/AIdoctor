# AIdoctor 总体架构与模块设计

> 文档状态：Draft v2.4 Integration Blueprint  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`  
> 适用仓库：`cxjchelsea/AIdoctor`

---

## 1. 文档定位

本文档是 AIdoctor 重构设计的**开发总入口**。

此前四份专题方案分别回答：

1. 临床业务和安全流程如何设计；
2. 患者数据、临床事实和医学证据如何治理；
3. Context、Memory、RAG、Skill、Model 等 Agent 基础能力如何运行；
4. Checkpoint、Resume、幂等、Trace、Log 和 Audit 如何保证系统可靠。

这些专题方案描述的是同一个系统的不同关注面，但不能直接作为代码模块的开发顺序。

本文档把它们统一整理为：

```text
一条完整主流程
+
一组职责清晰的模块
+
一套共享数据契约
+
明确的状态所有权和依赖方向
+
从完整系统切出的分阶段纵向路线
```

本文档负责说明“整体如何协作”。具体临床规则、RAG、Memory、Durable Execution 等细节仍以对应专题文档为准。

---

## 2. 系统形态

### 2.1 当前实现形态

当前代码整体仍以固定 Workflow 为主：

```text
病例输入
→ 信息提取
→ 主动问诊
→ 诊断分析
→ 检查或风险建议
→ 结果生成
```

部分代码已经体现 Agent Loop、工具调用和动态步骤思想，但尚未形成统一的持久化 Agent Runtime、状态契约和安全治理闭环。

因此当前实现可定义为：

> 固定诊断 Workflow + 实验性 Agent 骨架。

### 2.2 重构目标形态

重构目标不是自由自治 Agent，也不是继续维护完全固定的 Workflow，而是：

> **Constrained Agentic Workflow：以确定性 Workflow 作为安全和生命周期骨架，以 Agent 在受约束范围内动态选择下一步。**

固定且不可绕过的部分：

- Capability 范围检查；
- 输入质量检查；
- 每轮 Mandatory Safety Check；
- 临床状态只能通过 State Committer 修改；
- 高风险动作必须经过规则和医生审核；
- 外部不可逆动作必须具备 Checkpoint、幂等和审计；
- 输出必须经过安全和适用范围检查；
- 固定 Workflow 始终保留为降级路径。

允许动态决策的部分：

- 是否继续提问；
- 下一个问题是什么；
- 是否检索患者历史；
- 是否检索医学证据；
- 是否调用工具；
- 是否切换工具或进入降级；
- 是否请求医生审核；
- 是否停止并生成结果；
- 在已批准范围内选择 Skill 和模型。

### 2.3 核心职责分工

```text
LangGraph / Agent Runtime
负责执行、路由、暂停、恢复、重试和降级

Clinical Intelligence
负责临床概念、诊断候选、信息缺口和问题价值

Safety & Policy Engine
负责不可绕过的红旗、分诊、安全和权限判断

State Committer
负责临床状态的唯一受控写入

Evidence Intelligence
负责可追溯医学证据、引用、适用性和冲突

Clinician
负责高风险、受监管和不确定场景的最终决定
```

---

## 3. 系统目标与边界

### 3.1 首要目标

AIdoctor 首个完整能力目标是：

> 对成人常见呼吸道症状进行风险分层、结构化信息采集、有限鉴别分析、白名单循证依据展示、医生交接和就医导航。

### 3.2 系统需要做到

- 患者输入可以转换为可追溯的结构化临床观察；
- 临床事实、模型推断、医生确认和外部证据能够区分；
- 红旗和分诊安全不可被 Agent Planner 绕过；
- 系统可以根据状态动态决定下一步；
- 每个模型节点只看到完成任务所需的授权上下文；
- 患者历史和医学知识使用不同检索路径；
- 用户退出、服务重启和医生审核后可以安全恢复；
- 重复提交和重放不会重复预约、通知或正式写入；
- 技术链路、Agent 决策、临床依据和合规行为分别可追踪；
- 高风险和超出范围场景能够拒答、升级或交给医生。

### 3.3 首个版本不追求

- 通用医学自主诊断；
- 覆盖全部疾病和人群；
- 自由自治多 Agent；
- 无限长期记忆；
- 全互联网医学搜索；
- Agent 自动修改生产 Prompt、Skill 或临床规则；
- 无医生审核的治疗和处方变更；
- 一开始拆分十一个微服务；
- 一次性实现四份专题方案的所有能力。

---

## 4. 完整目标架构

```text
┌─────────────────────────────────────────────────────────────┐
│ Patient UI / Clinician Console / Admin Console              │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ 1. Business & Care Delivery                                 │
│ 用户、患者、医生、机构、权限、Consent、Encounter、Review、 │
│ Delivery、Care Navigation、Follow-up、外部业务动作          │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│ 7. Agent Runtime                                             │
│ LangGraph、节点、路由、Interrupt、Resume、Retry、Fallback   │
└──────────────┬────────────────────┬─────────────────────────┘
               │                    │
               │                    ├───────────────┐
               ▼                    ▼               ▼
┌───────────────────────┐ ┌──────────────────┐ ┌───────────────────┐
│ 4. Safety & Policy    │ │ 5. Clinical      │ │ 6. Evidence      │
│ Engine                │ │ Intelligence     │ │ Intelligence/RAG │
└───────────┬───────────┘ └─────────┬────────┘ └─────────┬─────────┘
            │                       │                    │
            └───────────┬───────────┴───────────┬────────┘
                        │                       │
              ┌─────────▼──────────┐  ┌────────▼─────────────┐
              │ 8. Context &       │  │ 9. Tool / Skill /   │
              │ Memory             │  │ Model Governance    │
              └─────────┬──────────┘  └────────┬─────────────┘
                        │                       │
                        └───────────┬───────────┘
                                    │
                       ┌────────────▼────────────┐
                       │ 3. Clinical State &     │
                       │ Data Foundation         │
                       │ State Committer / CDP / │
                       │ Ledger / Longitudinal   │
                       └────────────┬────────────┘
                                    │
                       ┌────────────▼────────────┐
                       │ 2. Clinical Domain      │
                       │ Shared Contracts        │
                       └─────────────────────────┘

横向基础能力：

10. Durable Execution
Thread / Run / Checkpoint / Lease / Inbox / Outbox / Idempotency

11. Observability, Audit & Evaluation
OTel / Log / AgentEvent / ClinicalDecisionRecord / Audit / Replay / Evals
```

### 4.1 架构解释

- 业务入口通过 Business & Care Delivery 接收请求；
- Agent Runtime 只负责编排，不直接声明临床真值；
- Safety、Clinical Intelligence 和 Evidence Intelligence 提供结构化决策；
- Context & Memory 决定节点本次可以看到的信息；
- Tool、Skill、Model Governance 决定允许使用的能力和模型；
- 所有临床状态修改统一经过 State Committer；
- Durable Execution 横向保证中断可恢复和动作不重复；
- Observability、Audit 与 Evaluation 横向记录、解释和评估全过程；
- Clinical Domain Contracts 是 Java、Python、数据库和前端共同的数据语言。

---

# Part A：完整主流程

## 5. 正常问诊主流程

```text
1. 创建 Encounter
   ↓
2. 创建 Thread / Run
   ↓
3. 加载 Capability、Consent 和身份权限
   ↓
4. 接收患者消息或上传资料
   ↓
5. 输入质量和范围检查
   ↓
6. Context Assembly 构建当前节点 ContextEnvelope
   ↓
7. Clinical Understanding 提取 ClinicalObservationCandidate
   ↓
8. State Committer 验证并写入 Evidence Ledger / Encounter CDP
   ↓
9. Mandatory Safety Check
   ↓
10. 更新 DiagnosticHypothesis、InformationGap 和 TriageAssessment
   ↓
11. Agent Runtime 决定下一动作
    ├── ask_user
    ├── request_artifact
    ├── retrieve_patient_history
    ├── retrieve_medical_evidence
    ├── call_tool
    ├── require_clinician_review
    └── prepare_delivery
   ↓
12. 动作执行并校验结构化结果
   ↓
13. State Committer 提交允许的 StatePatch
   ↓
14. 保存 Checkpoint
   ↓
15. 需要外部输入时创建 Interrupt 并暂停
   ↓
16. 用户或医生提交 ResumeRequest
   ↓
17. 校验权限、Checkpoint、CDP 版本和幂等
   ↓
18. 恢复到下一轮执行
   ↓
19. 达到停止条件后生成 DeliveryPackage
   ↓
20. 输出患者版、医生版和系统版结果
   ↓
21. 生成 CarePath / FollowUpPlan
   ↓
22. 完成或等待随访
```

## 6. 单轮执行细化

每轮执行保持固定安全骨架：

```text
load_runtime_context
→ validate_input
→ understand_input
→ propose_observations
→ commit_observations
→ mandatory_safety_check
→ update_clinical_state
→ evaluate_progress
→ choose_next_action
→ execute_action
→ validate_action_result
→ commit_state_patch
→ checkpoint
→ interrupt_or_continue
```

其中只有 `choose_next_action` 在允许范围内动态选择路径。

## 7. 下一动作类型

```python
class NextActionType(str, Enum):
    ASK_USER = "ask_user"
    REQUEST_ARTIFACT = "request_artifact"
    RETRIEVE_PATIENT_HISTORY = "retrieve_patient_history"
    RETRIEVE_MEDICAL_EVIDENCE = "retrieve_medical_evidence"
    CALL_TOOL = "call_tool"
    EXECUTE_SKILL = "execute_skill"
    REQUIRE_CLINICIAN_REVIEW = "require_clinician_review"
    PREPARE_DELIVERY = "prepare_delivery"
    FALLBACK_WORKFLOW = "fallback_workflow"
    STOP_OUT_OF_SCOPE = "stop_out_of_scope"
    STOP_UNSAFE = "stop_unsafe"
```

Agent 不能生成任意动作字符串。所有动作必须属于 Capability 允许的枚举和注册表。

---

## 8. 高风险升级流程

```text
ClinicalObservation 写入
→ Mandatory Safety Check
→ 命中 Red Flag 或高风险组合
→ 生成 TriageAssessment
→ State Committer 提交分诊结果
→ 阻止普通诊断 Planner 覆盖
→ 创建 ReviewTask 或 Emergency Guidance
→ 保存 Checkpoint
→ 输出明确就医建议
→ 等待医生审核或结束当前自动路径
```

高风险状态下：

- 不继续为了“提高诊断准确率”而延迟就医建议；
- 不允许普通 Planner 降低分诊级别；
- 不允许未经审核执行治疗和处方动作；
- 可以继续收集不延误处置的必要信息，但必须受 Safety Policy 限制。

---

## 9. 输入不足和低质量资料流程

```text
输入或文件到达
→ Quality Gate
→ 质量不足
→ 生成 InputQualityAssessment
→ 选择：澄清 / 重新上传 / 人工查看 / 使用有限信息继续
→ 明确记录缺失和不确定性
```

低质量 OCR、模糊报告和缺失关键字段不能被模型静默补全为事实。

---

## 10. 工具失败流程

```text
Agent Runtime 选择 Tool
→ Tool Policy 验证
→ 执行 Tool
→ ToolResult = failed
→ Structured Evaluation
   ├── retry_same_tool
   ├── switch_tool
   ├── fallback
   ├── ask_user
   ├── require_human_review
   └── stop
→ 记录 AgentEvent
→ 必要时保存 Checkpoint
```

工具失败不应直接变成自然语言异常后继续推理。

---

## 11. 医生审核流程

```text
触发 Human Review
→ 创建 ReviewTask
→ 保存关联 checkpoint_id / cdp_version / reason_codes
→ Interrupt: AWAITING_CLINICIAN
→ 医生 approve / edit / reject / request_more_info
→ 创建 ResumeRequest
→ 校验医生权限和版本
→ State Committer 提交医生修改
→ Agent Runtime Resume
```

医生审核必须影响真实运行状态，而不是仅作为旁路备注。

---

## 12. 中断恢复流程

```text
执行到可持久化点
→ 保存 Checkpoint
→ 创建 InterruptRecord
→ Thread 进入等待状态
→ 用户离开或服务停止
→ 外部输入到达
→ ResumeRequest
→ Inbox 去重
→ 获取 Thread Lease
→ 校验 Checkpoint / CDP / Capability / Consent 版本
→ 兼容则恢复
→ 不兼容则迁移、固定 Workflow 或人工接管
```

恢复可能从节点入口重新执行，因此节点必须可重入，副作用必须独立并幂等。

---

## 13. 超范围和拒答流程

```text
Capability Check
→ 当前请求超出支持范围
→ 返回 OUT_OF_SCOPE
→ 给出可理解的限制说明
→ 提供安全的就医或转人工路径
→ 不在未验证范围内继续自主推理
```

---

# Part B：十一个一级模块

## 14. 模块总览

| 编号 | 模块 | 核心问题 |
|---|---|---|
| 1 | Business & Care Delivery | 谁在使用系统，如何创建服务、审核和交付 |
| 2 | Clinical Domain Contracts | 全系统用什么数据语言 |
| 3 | Clinical State & Data Foundation | 临床事实和状态保存在哪里、谁能写 |
| 4 | Safety & Policy Engine | 什么绝对不能被 Agent 绕过 |
| 5 | Clinical Intelligence | 临床上理解什么、缺什么、下一步问什么 |
| 6 | Evidence Intelligence & RAG | 患者历史和医学证据如何检索、引用和适配 |
| 7 | Agent Runtime | 流程如何执行、路由、暂停和恢复 |
| 8 | Context & Memory | 当前节点看到什么、系统记住什么 |
| 9 | Tool, Skill & Model Governance | Agent 能调用什么能力和模型 |
| 10 | Durable Execution | 中断后如何继续，动作如何不重复 |
| 11 | Observability, Audit & Evaluation | 如何排障、解释、追责和验证 |

---

## 15. 模块一：Business & Care Delivery

### 15.1 定位

负责面向患者、医生、运营人员和外部业务系统的服务入口与业务闭环。

### 15.2 负责

- 用户、患者、医生和机构身份；
- 登录、角色、权限和 Consent；
- Encounter 创建、查询和关闭；
- 患者消息和文件提交；
- ReviewTask 创建与处理；
- 患者、医生和系统三类 DeliveryPackage；
- Care Navigation；
- 预约、转诊、通知；
- Follow-up 计划和任务；
- 面向前端和外部系统的 API；
- 业务级 Audit 触发。

### 15.3 不负责

- 不进行临床推理；
- 不自行选择下一问题；
- 不拼接 Prompt；
- 不直接修改 Evidence Ledger；
- 不自行恢复 LangGraph；
- 不将外部业务结果直接当作临床事实。

### 15.4 主要输入

- `CreateEncounterRequest`；
- `SubmitPatientMessageRequest`；
- `SubmitArtifactRequest`；
- `SubmitReviewDecisionRequest`；
- `ResumeEncounterRequest`；
- `CreateExternalActionRequest`。

### 15.5 主要输出

- `EncounterView`；
- `AgentTurnView`；
- `ReviewTaskView`；
- `DeliveryPackage`；
- `CarePath`；
- `FollowUpPlan`；
- `ExternalActionStatus`。

### 15.6 初始部署位置

Spring Boot 应用。

---

## 16. 模块二：Clinical Domain Contracts

### 16.1 定位

定义 Java、Python、数据库、事件和前端共享的核心领域语言。

### 16.2 负责

- 核心 Schema；
- 枚举和状态机；
- 字段语义；
- 基础结构校验；
- 版本和兼容策略；
- Java/Python 代码生成或一致性测试；
- OpenAPI / JSON Schema / Event Schema。

### 16.3 不负责

- 不包含 LangGraph 节点；
- 不包含数据库 Repository；
- 不调用模型；
- 不执行临床规则；
- 不绑定具体框架基类。

### 16.4 第一批契约

- `Encounter`；
- `EncounterCDP`；
- `ClinicalObservation`；
- `SourceArtifact`；
- `StatePatch`；
- `CommitResult`；
- `DiagnosticHypothesis`；
- `InformationGap`；
- `TriageAssessment`；
- `QuestionDecision`；
- `CapabilityEnvelope`；
- `ContextEnvelope`；
- `ToolResult`；
- `EvidencePack`；
- `ReviewTask`；
- `DeliveryPackage`；
- `ThreadStatus`；
- `CheckpointMetadata`；
- `InterruptRecord`；
- `ResumeRequest`；
- `AgentEvent`；
- `ClinicalDecisionRecord`；
- `ExternalActionRecord`。

### 16.5 初始部署位置

独立 `contracts/` 包，由 Java 与 Python 共同消费，不作为独立服务。

---

## 17. 模块三：Clinical State & Data Foundation

### 17.1 定位

保存和治理临床事实、单次 Encounter 状态和长期患者状态。

### 17.2 负责

- Encounter CDP；
- Evidence Ledger；
- ClinicalObservation 版本；
- SourceArtifact；
- State Committer；
- CDP Repository；
- Patient Longitudinal Record；
- Promotion Candidate 和 Promotion Policy；
- 临床事实的来源、置信度和确认状态；
- Consent 与保留策略；
- Phenotype、Dataset 和 Research Workspace 的后续扩展。

### 17.3 不负责

- 不决定下一个问题；
- 不把模型输出自动视为事实；
- 不负责 Graph 路由；
- 不保存技术 Trace；
- 不使用 Checkpoint 代替临床状态。

### 17.4 对外接口

```python
def get_encounter_cdp(encounter_id: str) -> EncounterCDP: ...

def propose_state_patch(patch: StatePatch) -> CommitResult: ...

def get_longitudinal_record(patient_id: str, scope: ConsentScope) -> PatientLongitudinalRecord: ...

def create_promotion_candidate(candidate: PromotionCandidate) -> PromotionDecision: ...
```

### 17.5 状态所有权

该模块拥有：

- `EncounterCDP`；
- `EvidenceLedger`；
- `ClinicalObservation`；
- `PatientLongitudinalRecord`。

所有临床写入必须通过 State Committer。

### 17.6 初始部署位置

PostgreSQL + Spring/Python 领域服务；首版可以保持在现有应用和 Agent Runtime 内的受控 package 中。

---

## 18. 模块四：Safety & Policy Engine

### 18.1 定位

提供不可被 Planner、Tool、Skill 或模型绕过的安全和权限决策。

### 18.2 负责

- Capability 范围；
- 红旗和紧急程度；
- 特殊人群；
- 输入安全和质量门；
- 输出安全；
- Tool、Skill、Model 和字段访问权限；
- Policy Enforcement Point；
- Human Review 触发；
- 高风险动作许可；
- 拒答、降级和保守升级。

### 18.3 不负责

- 不负责自然语言表达；
- 不进行开放式疾病生成；
- 不执行外部动作；
- 不直接修改 CDP；
- 不由 LLM 最终决定红旗是否成立。

### 18.4 对外接口

```python
def evaluate_input_quality(request: InputQualityRequest) -> InputQualityAssessment: ...

def evaluate_safety(request: SafetyEvaluationRequest) -> TriageAssessment: ...

def authorize_action(request: PolicyAuthorizationRequest) -> PolicyDecision: ...

def validate_output(request: OutputSafetyRequest) -> OutputSafetyDecision: ...
```

### 18.5 第一版

- 成人呼吸道红旗规则；
- 过敏、妊娠、儿童、老年等基础特殊人群标记；
- Capability 白名单；
- Tool 和 StatePatch 字段权限；
- 医生审核触发规则。

---

## 19. 模块五：Clinical Intelligence

### 19.1 定位

负责从临床状态中形成结构化理解、候选、信息缺口和下一问题建议。

### 19.2 子模块

```text
Clinical Understanding
Diagnostic Inference
Information Gap Analysis
Question Policy
Stop / Abstention Decision
Clinical Explanation
Care Navigation Reasoning
Follow-up Reasoning
```

### 19.3 负责

- 从患者表达生成 Observation Candidate；
- 标准化医学概念；
- 维护鉴别诊断候选；
- 关联支持和反对证据；
- 识别信息缺口；
- 评估问题价值；
- 判断继续、停止或交给医生；
- 生成结构化临床解释材料。

### 19.4 不负责

- 不直接写 CDP；
- 不决定高风险安全底线；
- 不管理 Checkpoint；
- 不负责检索实现；
- 不执行预约和通知；
- 不直接向患者输出未经 Delivery 和 Safety 验证的结论。

### 19.5 对外接口

```python
def understand_input(request: UnderstandingRequest) -> UnderstandingResult: ...

def update_hypotheses(request: InferenceRequest) -> InferenceResult: ...

def identify_information_gaps(request: GapAnalysisRequest) -> GapAnalysisResult: ...

def select_next_question(request: QuestionPolicyRequest) -> QuestionDecision: ...

def evaluate_stop_condition(request: StopEvaluationRequest) -> StopDecision: ...
```

### 19.6 第一版

- 症状、持续时间、严重程度和伴随症状抽取；
- 少量呼吸道候选；
- 规则与结构化模型结合的信息缺口；
- 单问题 Question Policy；
- 明确的 `INSUFFICIENT_INFORMATION` 和 `OUT_OF_SCOPE`。

---

## 20. 模块六：Evidence Intelligence & RAG

### 20.1 定位

分别检索患者自己的历史信息和外部医学知识，并形成可引用、可评估适用性的 EvidencePack。

### 20.2 子模块

```text
Patient History Retrieval
Medical Knowledge Retrieval
Source Registry
Knowledge Ingestion
Clinical Chunking
Hybrid Retrieval
Medical Concept Expansion
Reranking
Citation Validation
Applicability Assessment
Conflict Detection
EvidencePack Builder
```

### 20.3 负责

- Patient RAG 与 Medical RAG 隔离；
- 白名单医学来源摄取；
- 元数据、许可、版本和有效期；
- BM25 + Vector + 可选 Graph；
- ACL 和 Consent 过滤；
- 结论级引用；
- 人群、地区和场景适配；
- 冲突和无答案状态；
- RAG Injection 污染标记。

### 20.4 不负责

- 不把检索片段直接视为临床结论；
- 不把不同患者数据放入共享无 ACL 索引；
- 不自行修改 Safety Policy；
- 不允许检索文档中的指令控制 Agent；
- 不直接写入长期患者事实。

### 20.5 对外接口

```python
def retrieve_patient_history(request: PatientRetrievalRequest) -> PatientRetrievalResult: ...

def retrieve_medical_evidence(request: MedicalRetrievalRequest) -> RetrievalResult: ...

def build_evidence_pack(request: EvidencePackRequest) -> EvidencePack: ...

def validate_citations(pack: EvidencePack) -> CitationValidationResult: ...
```

### 20.6 第一版

- 结构化患者相关病史召回；
- 成人呼吸道白名单指南；
- Metadata Filter + BM25 + Vector；
- 基础 Rerank；
- Claim-Level Citation；
- 无答案和来源冲突状态。

---

## 21. 模块七：Agent Runtime

### 21.1 定位

作为唯一 Agent 执行运行时，负责编排结构化组件和外部输入，不负责声明临床真值。

### 21.2 负责

- LangGraph Graph；
- Node 和 Edge；
- GraphState；
- 动态路由；
- Interrupt / Resume 调用；
- Tool 和 Skill 调度；
- Retry、Switch、Fallback；
- 循环和预算控制；
- 调用 Context、Safety、Clinical Intelligence 和 Evidence Intelligence；
- 创建 AgentEvent；
- 触发 Checkpoint。

### 21.3 不负责

- 不直接读取任意数据库表；
- 不自行拼接完整患者对象；
- 不直接写临床数据库；
- 不覆盖 Safety 决策；
- 不允许任意工具调用；
- 不把 Trace 当作状态。

### 21.4 建议节点

```text
initialize_run
load_capability
assemble_context
validate_input
understand_input
commit_observations
mandatory_safety_check
update_hypotheses
identify_information_gaps
evaluate_progress
choose_next_action
retrieve_patient_history
retrieve_medical_evidence
execute_tool
execute_skill
validate_result
commit_state_patch
prepare_review
prepare_delivery
checkpoint_and_interrupt
complete_run
```

### 21.5 第一版

只实现首个 Capability 所需节点，不建立通用任意规划器。

---

## 22. 模块八：Context & Memory

### 22.1 定位

控制每个节点本次可以看到什么，以及跨轮、跨 Encounter 的信息如何被记住和召回。

### 22.2 子模块

```text
Context Assembly
Context Policy
Token Budget
Critical Context Pin
Conversation Summary
Redaction
Working Memory
Episodic Memory
Semantic Memory
Operational Memory
Memory Write Gate
Memory Recall
Retention / Expiry / Delete
```

### 22.3 负责

- 生成 `ContextEnvelope`；
- 节点级字段选择；
- token 预算；
- 最近消息窗口；
- 结构化摘要；
- 红旗、过敏等不可压缩事实；
- PHI 脱敏；
- Memory Candidate；
- 写入审批；
- Recall 原因；
- 过期、冲突、更正和删除。

### 22.4 不负责

- Context 不是新的临床事实源；
- Summary 不能覆盖原始来源；
- 模型推断不能自动进入 Semantic Memory；
- Operational Memory 不能保存患者诊断；
- Memory Service 不执行临床确认。

### 22.5 对外接口

```python
def assemble_context(request: ContextAssemblyRequest) -> ContextEnvelope: ...

def create_summary(request: SummaryRequest) -> ConversationSummary: ...

def propose_memory_write(candidate: MemoryCandidate) -> MemoryWriteDecision: ...

def recall_memory(request: MemoryRecallRequest) -> MemoryRecallResult: ...
```

### 22.6 第一版

- 最近 6～10 轮消息；
- 当前 Encounter 摘要；
- 红旗、过敏和关键状态固定保留；
- 经确认相关长期病史；
- `context_hash`；
- 仅白名单低风险 Memory Write Candidate。

---

## 23. 模块九：Tool, Skill & Model Governance

### 23.1 定位

统一注册和治理 Agent 可使用的原子工具、任务技能、Prompt 和模型。

### 23.2 子模块

```text
Tool Registry
Tool Runtime Adapter
Tool Policy
Skill Registry
Skill Selection
Prompt Registry
Model Registry
Model Router
Release Management
Compatibility Matrix
```

### 23.3 负责

- ToolSpec；
- ToolContext；
- ToolResult；
- 工具身份、权限、超时、重试、幂等和 Circuit Breaker；
- ClinicalSkill 和 SkillStep；
- Skill 版本、Owner、审核和测试；
- Prompt 版本；
- ModelRoutePolicy；
- 模型隐私、数据驻留、质量、延迟和成本；
- 高风险模型降级限制。

### 23.4 不负责

- Tool 不能直接写临床状态；
- Skill 不能绕过 Capability 和 Safety；
- Model Router 不能选择未验证高风险模型；
- Prompt 不得携带未授权完整患者数据；
- MCP 不能替代 Tool Registry 和 Policy。

### 23.5 对外接口

```python
def resolve_tool(request: ToolResolutionRequest) -> ToolSpec: ...

def execute_tool(request: ToolExecutionRequest) -> ToolResult: ...

def select_skill(request: SkillSelectionRequest) -> SkillSelectionDecision: ...

def route_model(request: ModelRouteRequest) -> ModelRouteDecision: ...
```

### 23.6 第一版

- 少量显式 ToolSpec；
- ToolResult 统一结构；
- 四个呼吸道 Skill 定义，但可先只实现两个；
- 抽取、表达、总结和引用校验四类模型路由；
- 高风险任务无模型时进入固定安全路径。

---

## 24. 模块十：Durable Execution

### 24.1 定位

保证执行中断后能够安全继续，并保证重复投递不会重复产生业务副作用。

### 24.2 负责

- Thread / Run 生命周期；
- PostgreSQL Checkpointer；
- CheckpointMetadata；
- InterruptRecord；
- ResumeRequest；
- Resume 鉴权和幂等；
- Thread Lease；
- CDP 乐观锁；
- Resume Inbox；
- Transactional Outbox；
- ExternalActionRecord；
- Checkpoint Migration；
- Reconciliation；
- 取消、过期和清理。

### 24.3 不负责

- Checkpoint 不保存临床真值；
- Trace 和 Log 不用于恢复；
- Durable Execution 不决定诊断和分诊；
- 不假设外部动作天然恰好一次；
- 不允许 Resume 绕过当前 Consent 和 Policy。

### 24.4 对外接口

```python
def create_thread(request: CreateThreadRequest) -> ThreadRecord: ...

def save_checkpoint(request: SaveCheckpointRequest) -> CheckpointMetadata: ...

def create_interrupt(request: CreateInterruptRequest) -> InterruptRecord: ...

def resume_thread(request: ResumeRequest) -> ResumeDecision: ...

def acquire_thread_lease(thread_id: str, worker_id: str) -> ThreadLease: ...

def execute_idempotent_action(request: ExternalActionRequest) -> ExternalActionRecord: ...
```

### 24.5 第一版

```text
提问
→ 保存 PostgreSQL Checkpoint
→ 创建 AWAITING_USER Interrupt
→ 重启 Agent Runtime
→ 使用相同 thread_id Resume
→ 不丢 Observation
→ 不重复提问
→ 重复 ResumeRequest 只处理一次
```

---

## 25. 模块十一：Observability, Audit & Evaluation

### 25.1 定位

分别解决技术排障、Agent 行为解释、临床决策解释、合规追责和质量验证。

### 25.2 子模块

```text
OpenTelemetry Instrumentation
Trace / Metric / Structured Log
Agent Event Store
Clinical Decision Record
Compliance Audit Store
State Resume / Simulation / Forensic Replay
Clinical Evals
Agent Infrastructure Evals
Patient Simulator / OSCE
Operational Dashboards
```

### 25.3 负责

- OTel Trace Context 传播；
- Java、Python、LangGraph 和 Tool Span；
- Metric 和结构化 Log；
- AgentEvent；
- ClinicalDecisionRecord；
- Compliance Audit；
- PHI 过滤；
- Replay Sandbox；
- Context、Memory、RAG、Routing、Resume 和安全测试；
- 运营 Dashboard 和告警。

### 25.4 不负责

- Trace 不作为临床事实；
- Log 不作为恢复状态；
- AgentEvent 不替代 ClinicalDecisionRecord；
- ClinicalDecisionRecord 不替代 Audit；
- 可观测后端故障不能阻塞普通临床请求；
- Checkpoint 写入失败时不能继续不可逆动作。

### 25.5 推荐开源栈

```text
OpenTelemetry SDK / Collector
Trace: Tempo 或 Jaeger
Metric: Prometheus
Log: Loki 或 OpenSearch
Visualization: Grafana
Agent/RAG 调试：可选 Phoenix
```

### 25.6 第一版

- 统一 `trace_id / span_id / thread_id / run_id / encounter_id`；
- Agent 节点 Span；
- State Commit、Safety、模型和 Tool Span；
- AgentEvent；
- PHI 字段过滤；
- 一条服务重启 Resume 的端到端 Trace。

---

# Part C：共享数据契约

## 26. 契约设计原则

1. Clinical Domain Contracts 是唯一共享定义源；
2. Java 与 Python 使用同一 JSON Schema 或自动生成模型；
3. 所有可持久化对象必须有版本；
4. 所有临床事实必须有来源；
5. 模型输出默认是 Candidate，不是 Confirmed Fact；
6. 写操作必须携带期望版本；
7. 失败状态应结构化，不只抛异常；
8. 数据对象不绑定 LangGraph、ORM 或具体模型供应商；
9. PHI、Consent 和 Tenant 字段必须显式；
10. 关键对象必须有稳定 ID 和审计时间。

---

## 27. 核心身份与关联字段

跨模块对象按需携带：

```text
tenant_id
organization_id
patient_id
user_id
encounter_id
thread_id
run_id
checkpoint_id
capability_id
consent_scope_id
correlation_id
trace_id
schema_version
created_at
updated_at
```

不是所有对象都包含全部字段，但不能依赖从自然语言或日志推断身份关系。

---

## 28. 第一批核心契约

### 28.1 Encounter

```python
class Encounter(BaseModel):
    encounter_id: str
    patient_id: str
    organization_id: str
    capability_id: str
    status: EncounterStatus
    mode: EncounterMode
    consent_scope_id: str
    created_at: datetime
    closed_at: datetime | None
```

### 28.2 EncounterCDP

```python
class EncounterCDP(BaseModel):
    encounter_id: str
    version: int
    observations: list[ClinicalObservation]
    hypotheses: list[DiagnosticHypothesis]
    information_gaps: list[InformationGap]
    triage: TriageAssessment | None
    active_plan: ClinicalPlan | None
    delivery_status: DeliveryStatus
    updated_at: datetime
```

### 28.3 ClinicalObservation

```python
class ClinicalObservation(BaseModel):
    observation_id: str
    encounter_id: str
    concept_code: str
    value: Any
    value_type: str
    status: ObservationStatus
    source_ids: list[str]
    confidence: float | None
    asserted_by: AssertionActor
    observed_at: datetime | None
    valid_from: datetime | None
    valid_until: datetime | None
    version: int
```

### 28.4 SourceArtifact

```python
class SourceArtifact(BaseModel):
    source_id: str
    source_type: SourceType
    owner_scope: str
    storage_reference: str | None
    text_reference: str | None
    checksum: str
    quality_status: str
    consent_scope_id: str
    created_at: datetime
```

### 28.5 StatePatch

```python
class StatePatch(BaseModel):
    patch_id: str
    encounter_id: str
    expected_cdp_version: int
    proposed_writes: list[ProposedWrite]
    proposed_by: str
    source_ids: list[str]
    reason_codes: list[str]
    idempotency_key: str
```

### 28.6 CommitResult

```python
class CommitResult(BaseModel):
    status: Literal[
        "committed",
        "partially_committed",
        "rejected",
        "version_conflict",
        "requires_review"
    ]
    new_cdp_version: int | None
    accepted_write_ids: list[str]
    rejected_writes: dict[str, list[str]]
    audit_record_id: str
```

### 28.7 DiagnosticHypothesis

```python
class DiagnosticHypothesis(BaseModel):
    hypothesis_id: str
    condition_code: str
    status: HypothesisStatus
    supporting_observation_ids: list[str]
    opposing_observation_ids: list[str]
    missing_discriminators: list[str]
    confidence_band: str
    generated_by: str
    version: int
```

### 28.8 InformationGap

```python
class InformationGap(BaseModel):
    gap_id: str
    concept_code: str
    importance: str
    target_hypothesis_ids: list[str]
    resolution_methods: list[str]
    status: str
```

### 28.9 TriageAssessment

```python
class TriageAssessment(BaseModel):
    assessment_id: str
    level: TriageLevel
    red_flags: list[RedFlagFinding]
    reason_codes: list[str]
    required_actions: list[str]
    requires_clinician_review: bool
    policy_version: str
```

### 28.10 QuestionDecision

```python
class QuestionDecision(BaseModel):
    question_id: str
    target_concepts: list[str]
    question_text: str
    reason_codes: list[str]
    expected_information_gain: float | None
    alternative_question_ids: list[str]
    stop_if_unanswerable: bool
    policy_version: str
```

### 28.11 CapabilityEnvelope

```python
class CapabilityEnvelope(BaseModel):
    capability_id: str
    version: str
    supported_populations: list[str]
    supported_conditions: list[str]
    excluded_conditions: list[str]
    allowed_tools: list[str]
    allowed_skills: list[str]
    allowed_actions: list[str]
    required_safety_policies: list[str]
    output_constraints: list[str]
```

### 28.12 ContextEnvelope

```python
class ContextEnvelope(BaseModel):
    context_id: str
    node_id: str
    encounter_id: str
    thread_id: str
    cdp_version: int
    capability_version: str
    safety_snapshot: dict
    patient_summary: dict
    encounter_summary: dict
    recent_messages: list[Message]
    selected_observations: list[ClinicalObservation]
    active_hypotheses: list[DiagnosticHypothesis]
    unresolved_conflicts: list[ConflictRecord]
    retrieved_evidence: list[EvidenceClaim]
    token_budget: TokenBudget
    redaction_manifest: RedactionManifest
    context_hash: str
```

### 28.13 ToolResult

```python
class ToolResult(BaseModel):
    tool_call_id: str
    tool_id: str
    tool_version: str
    status: ToolResultStatus
    output: dict | None
    proposed_writes: list[ProposedWrite]
    source_artifacts: list[SourceArtifact]
    warnings: list[str]
    error: StructuredError | None
    idempotency_key: str | None
```

### 28.14 EvidencePack

```python
class EvidencePack(BaseModel):
    evidence_pack_id: str
    clinical_question_id: str
    claims: list[EvidenceClaim]
    sources: list[MedicalSource]
    applicability: list[ApplicabilityAssessment]
    conflicts: list[EvidenceConflict]
    limitations: list[str]
    knowledge_release_id: str
```

### 28.15 ReviewTask

```python
class ReviewTask(BaseModel):
    review_task_id: str
    encounter_id: str
    thread_id: str
    checkpoint_id: str
    expected_cdp_version: int
    review_type: str
    reason_codes: list[str]
    status: ReviewStatus
    assigned_to: str | None
    due_at: datetime | None
```

### 28.16 DeliveryPackage

```python
class DeliveryPackage(BaseModel):
    delivery_id: str
    encounter_id: str
    patient_view: PatientDelivery
    clinician_view: ClinicianDelivery
    system_view: SystemDelivery
    triage: TriageAssessment
    evidence_pack_ids: list[str]
    limitations: list[str]
    requires_review: bool
    version: int
```

### 28.17 AgentState

```python
class AgentState(BaseModel):
    encounter_id: str
    thread_id: str
    run_id: str
    current_node: str
    active_action: NextAction | None
    retry_count: int
    replan_count: int
    token_budget_remaining: int
    cost_budget_remaining: float
    pending_interrupt_id: str | None
    last_cdp_version: int
```

### 28.18 CheckpointMetadata

```python
class CheckpointMetadata(BaseModel):
    checkpoint_id: str
    thread_id: str
    run_id: str
    graph_version: str
    state_schema_version: str
    capability_version: str
    context_policy_version: str
    skill_release_id: str | None
    model_route_version: str
    safety_policy_version: str
    knowledge_release_id: str | None
    cdp_version: int
    created_at: datetime
```

### 28.19 InterruptRecord

```python
class InterruptRecord(BaseModel):
    interrupt_id: str
    thread_id: str
    checkpoint_id: str
    interrupt_type: str
    required_actor_type: str
    expected_input_schema_id: str
    status: InterruptStatus
    expires_at: datetime | None
```

### 28.20 ResumeRequest

```python
class ResumeRequest(BaseModel):
    resume_request_id: str
    thread_id: str
    interrupt_id: str
    expected_checkpoint_id: str
    expected_cdp_version: int
    actor_id: str
    actor_role: str
    payload: dict
    idempotency_key: str
```

### 28.21 AgentEvent

```python
class AgentEvent(BaseModel):
    event_id: str
    encounter_id: str
    thread_id: str
    run_id: str
    checkpoint_id: str | None
    event_type: str
    node_id: str | None
    reason_codes: list[str]
    related_object_ids: list[str]
    trace_id: str | None
    occurred_at: datetime
```

### 28.22 ClinicalDecisionRecord

```python
class ClinicalDecisionRecord(BaseModel):
    decision_record_id: str
    encounter_id: str
    decision_type: str
    input_observation_ids: list[str]
    evidence_claim_ids: list[str]
    applied_rule_ids: list[str]
    selected_action: str
    rejected_actions: dict[str, list[str]]
    clinician_modification: dict | None
    version_chain: dict
    created_at: datetime
```

### 28.23 ExternalActionRecord

```python
class ExternalActionRecord(BaseModel):
    action_record_id: str
    encounter_id: str
    action_type: str
    status: ExternalActionStatus
    idempotency_key: str
    external_reference_id: str | None
    request_hash: str
    submitted_at: datetime | None
    completed_at: datetime | None
    compensation_status: str | None
```

---

# Part D：状态所有权与写入规则

## 29. 状态所有权矩阵

| 状态或记录 | 唯一所有者 | 其他模块如何参与 |
|---|---|---|
| Encounter | Business & Care Delivery | Runtime 读取关联信息 |
| EncounterCDP | Clinical State & Data Foundation | 其他模块提交 StatePatch |
| ClinicalObservation | State Committer | Understanding/Tool 提出 Candidate |
| Evidence Ledger | Clinical State & Data Foundation | Safety/Inference 读取 |
| Patient Longitudinal Record | Clinical State & Data Foundation | Memory/RAG 受控召回 |
| DiagnosticHypothesis | State Committer | Clinical Intelligence 提议 |
| TriageAssessment | State Committer | Safety Engine 生成 |
| EvidencePack | Evidence Intelligence | Runtime 和 Delivery 引用 |
| ContextEnvelope | Context & Memory | 每次节点临时生成 |
| MemoryItem | Context & Memory | 通过 Write Gate 写入 |
| AgentState | Agent Runtime | Checkpointer 持久化快照 |
| Checkpoint | Durable Execution | Runtime 触发保存 |
| ReviewTask | Business & Care Delivery | Runtime 创建请求 |
| ToolSpec / Skill / ModelRoutePolicy | Tool, Skill & Model Governance | Runtime 读取并执行 |
| ExternalActionRecord | Durable Execution / Business Action Worker | Runtime 只请求动作 |
| AgentEvent | Observability Event Store | 各模块发送事件 |
| ClinicalDecisionRecord | Clinical Decision Store | Safety/Inference/Doctor 提供输入 |
| Compliance Audit | Audit Store | 所有受保护行为写入 |

## 30. 临床写入统一规则

```text
任意模块产生 Candidate 或 ProposedWrite
→ StatePatch
→ State Committer
→ Schema 校验
→ Capability 和字段权限
→ 来源和 Consent 校验
→ expected_cdp_version 校验
→ 冲突处理
→ CommitResult
→ 新 CDP 版本
→ Audit / AgentEvent
```

禁止：

- LLM 直接写数据库；
- Tool 直接确认诊断；
- Memory Service 直接修改 CDP；
- Context Summary 覆盖原始事实；
- LangGraph Checkpoint 成为临床事实源；
- Trace 或 Log 被读取为恢复状态。

---

# Part E：依赖方向

## 31. 允许的依赖方向

```text
Business & Care Delivery
          ↓
Agent Runtime
          ↓
Context & Memory / Durable Execution / Governance
          ↓
Safety / Clinical Intelligence / Evidence Intelligence
          ↓
Clinical State & Data Foundation
          ↓
Clinical Domain Contracts
```

横向：

```text
Observability / Audit / Evaluation
接收各模块事件，但不能反向控制临床真值
```

## 32. 禁止的依赖

- Clinical Domain Contracts 依赖 LangGraph；
- Clinical Intelligence 依赖具体前端；
- Safety Engine 依赖自由文本 Prompt 才能工作；
- State Committer 调用 Planner 决定是否写入；
- Business API 直接调用模型；
- Context Assembly 直接修改数据库；
- Tool Runtime 直接绕过 Policy；
- Checkpointer 读取 Trace 恢复状态；
- Audit Store 成为业务查询主库；
- Python 和 Java 各自定义同名不同义的临床类型。

---

## 33. 模块调用示例

### 33.1 患者消息

```text
Business API
→ Agent Runtime
→ Context Assembly
→ Clinical Understanding
→ State Committer
→ Safety Engine
→ Clinical Intelligence
→ Agent Runtime
→ Checkpoint / Interrupt
→ Business API 返回问题
```

### 33.2 指南检索

```text
Agent Runtime
→ Policy Engine
→ Evidence Intelligence
→ Medical RAG
→ EvidencePack Builder
→ Citation Validation
→ Agent Runtime
→ State Committer 只提交允许的引用关联
```

### 33.3 医生审核

```text
Agent Runtime
→ Business & Care Delivery 创建 ReviewTask
→ Durable Execution 保存 Interrupt
→ Clinician Console
→ Business API 提交 Review Decision
→ State Committer 提交医生修改
→ Durable Execution Resume
→ Agent Runtime 继续
```

---

# Part F：部署与代码组织

## 34. 模块不等于微服务

十一个模块是领域和职责边界，不要求第一阶段拆成十一个服务。

建议初始物理部署：

```text
Spring Boot
├── business-care-delivery
├── identity-consent
├── review-delivery
├── external-actions
└── audit-api

Python FastAPI + LangGraph
├── agent-runtime
├── safety-policy
├── clinical-intelligence
├── evidence-intelligence
├── context-memory
├── tool-skill-model-governance
├── durable-execution-adapter
└── observability-instrumentation

Shared
├── contracts
├── capability-packages
├── evals
└── test-fixtures

Storage
├── PostgreSQL
├── Redis
├── pgvector
├── Neo4j（按需）
├── Object Storage
└── OTel Backends
```

### 34.1 首版部署原则

- Spring Boot 保留业务、身份、审核和外部业务动作；
- Python 是唯一 Agent Runtime；
- Clinical Intelligence、Safety、Context 等首版作为 Python package，不急于拆服务；
- PostgreSQL 是临床状态和生产 Checkpoint 的主存储；
- Redis 只用于短期缓存、Lease 和运行时协调；
- Neo4j 和独立 Research Platform 不作为第一条主链路前置条件；
- OTel Collector 与 Trace 后端不进入临床事务。

---

## 35. 推荐仓库结构

```text
AIdoctor/
├── apps/
│   ├── business-service/
│   ├── agent-runtime/
│   ├── clinician-console/
│   └── patient-ui/
│
├── packages/
│   ├── clinical-domain/
│   ├── clinical-state/
│   ├── safety-policy/
│   ├── clinical-intelligence/
│   ├── evidence-intelligence/
│   ├── context-memory/
│   ├── tool-skill-model-governance/
│   ├── durable-execution/
│   └── observability/
│
├── contracts/
│   ├── json-schema/
│   ├── openapi/
│   ├── events/
│   ├── generated-java/
│   └── generated-python/
│
├── capabilities/
│   └── adult-respiratory-v1/
│       ├── capability.yaml
│       ├── safety-rules.yaml
│       ├── context-policy.yaml
│       ├── question-policy.yaml
│       ├── skill-manifest.yaml
│       ├── model-routes.yaml
│       ├── knowledge-sources.yaml
│       └── eval-manifest.yaml
│
├── evals/
│   ├── clinical/
│   ├── safety/
│   ├── context/
│   ├── rag/
│   ├── resume/
│   └── security/
│
└── docs/
    ├── refactoring/
    ├── modules/
    ├── contracts/
    ├── capabilities/
    └── adr/
```

不要求立即重排现有仓库。目录调整应在主链路和契约稳定后逐步完成。

---

# Part G：第一条纵向切片

## 36. 第一阶段场景

```text
成人患者输入：
“咳嗽三天，有点喘”
```

系统完成：

```text
创建 Encounter / Thread
→ 抽取咳嗽、持续时间和呼吸不适
→ 写入 ClinicalObservation
→ 执行红旗检查
→ 选择一个最有价值的问题
→ 保存 Checkpoint 并等待用户
→ 服务重启
→ 用户回答后 Resume
→ 更新 Observation 和分诊
→ 生成简单就医建议
→ 完成 DeliveryPackage
```

## 37. 第一条切片涉及的模块

| 模块 | 第一阶段只实现 |
|---|---|
| Business & Care Delivery | 创建 Encounter、提交消息、返回问题和结果 |
| Clinical Domain | 约 15 个核心契约 |
| Clinical State | CDP、Observation、StatePatch、State Committer |
| Safety | 呼吸道红旗和基本分诊 |
| Clinical Intelligence | 抽取、信息缺口、单问题策略 |
| Evidence Intelligence | 可暂不进入主链路，或只返回白名单固定证据 |
| Agent Runtime | 最小 LangGraph 主链路 |
| Context & Memory | 最近消息、关键状态和 context hash |
| Tool/Skill/Model | 一个抽取模型路由、一个表达模型路由 |
| Durable Execution | PostgreSQL Checkpoint、Interrupt、Resume 去重 |
| Observability | OTel 基线、AgentEvent、结构化 Log |

## 38. 第一阶段不实现

- 完整长期记忆；
- 复杂多疾病推理；
- 动态 Skill 市场；
- 全量医学文献摄取；
- 复杂模型成本优化；
- 真实预约和转诊；
- Neo4j 全量知识图谱；
- Research Workspace；
- 完整医生控制台；
- Checkpoint 多版本迁移平台；
- 全量 Replay Console。

## 39. 第一阶段完成标准

- 患者输入被保存为有来源的 Observation；
- 模型推断没有被自动当作确认事实；
- 每轮都执行 Safety Check；
- 下一问题来自结构化 QuestionDecision；
- 用户退出和服务重启后可以继续；
- 重复 Resume 不重复写入；
- CDP 使用乐观版本；
- 每个临床写入经过 State Committer；
- Trace、Log 和 AgentEvent 可以关联；
- 输出包含限制和就医建议；
- 至少覆盖正常、红旗、输入不足、重复提交和恢复五类 E2E 测试。

---

# Part H：分阶段实施路线

## 40. 路线制定原则

阶段路线从完整主流程中切纵向闭环，不按“先把某个模块全部做完”推进。

每个阶段都必须：

- 有可运行端到端场景；
- 有明确输入输出；
- 有测试和验收；
- 不引入无法被当前场景使用的大型基础设施；
- 保留固定 Workflow 降级路径。

---

## 41. Phase A：现状盘点与总体契约

### 目标

让现有代码、目标模块和共享数据语言对应起来。

### 任务

- 修复当前编译和接口问题；
- 画出现有 Workflow；
- 建立现有代码到 11 个模块的映射；
- 盘点数据表、Prompt、工具和状态；
- 定义第一批 JSON Schema；
- 建立 Java/Python 契约一致性测试；
- 明确首个 Capability。

### 验收

- 当前主链路可运行；
- 每个现有核心类有目标归属；
- 第一批契约无重复定义；
- 系统边界和非目标得到确认。

---

## 42. Phase B：最小临床状态与安全核心

### 目标

建立可信临床状态和不可绕过的安全底座。

### 任务

- EncounterCDP；
- ClinicalObservation；
- SourceArtifact；
- StatePatch / CommitResult；
- State Committer；
- Evidence Ledger；
- 呼吸道 Safety Rules；
- TriageAssessment；
- 基础 Capability Check。

### 验收

- 所有临床写入有来源和版本；
- 模型 Candidate 不自动成为事实；
- 红旗测试通过；
- Planner 无法绕过 Safety。

---

## 43. Phase C：最小 Agentic Workflow 与跨轮恢复

### 目标

跑通第一条真正的受约束 Agent 主链路。

### 任务

- 最小 LangGraph；
- Clinical Understanding；
- Information Gap；
- QuestionDecision；
- PostgreSQL Checkpointer；
- Interrupt / Resume；
- Resume Inbox 去重；
- ContextEnvelope 第一版；
- AgentEvent 和基础 OTel。

### 验收

- 可以动态选择问题；
- 服务重启后恢复；
- 不重复提问和写状态；
- 每轮有 Safety；
- 一条 E2E 完整通过。

---

## 44. Phase D：临床推理、患者历史和白名单证据

### 目标

从“可跨轮问诊”扩展到“有有限临床推理和可引用证据”。

### 任务

- DiagnosticHypothesis；
- 结构化信息缺口；
- Patient History Retrieval；
- Memory Candidate / Write Gate；
- 白名单 Knowledge Ingestion；
- Hybrid Retrieval；
- EvidencePack；
- Citation Validation；
- Context Summary 和 Critical Pin。

### 验收

- 候选有支持和反对证据；
- Patient RAG 不跨患者；
- RAG 无结果可明确返回；
- 红旗不会在摘要中丢失；
- 引用能够支持对应 Claim。

---

## 45. Phase E：医生审核和业务交付闭环

### 目标

让系统从 Agent 演示进入临床辅助业务闭环。

### 任务

- ReviewTask；
- 医生 Interrupt / Resume；
- ClinicianDelivery；
- PatientDelivery；
- CarePath；
- FollowUpPlan；
- 幂等模拟外部动作；
- Outbox / ExternalActionRecord。

### 验收

- 医生修改进入真实 CDP；
- 高风险必须人工决定；
- 三类输出一致；
- 重复恢复不重复动作；
- 患者输出不暴露内部假设和系统 Prompt。

---

## 46. Phase F：治理、评估和分阶段放量

### 目标

建立企业级运行和持续验证能力。

### 任务

- Thread Lease；
- Checkpoint Migration；
- Reconciliation；
- Model Router 完整策略；
- Skill Registry；
- OTel Collector 和 Dashboard；
- ClinicalDecisionRecord；
- Compliance Audit；
- Replay Sandbox；
- Patient Simulator；
- Context、Memory、RAG、Resume、安全和临床评估；
- Shadow / Clinician Assist / Restricted Patient Rollout。

### 验收

- 跨患者污染为零；
- 高风险静默模型降级为零；
- 重放不产生真实副作用；
- 版本链可还原；
- PHI 遥测门禁通过；
- 每次 Capability 发布附带评估报告。

---

# Part I：模块详细设计方法

## 47. 后续每个模块的设计模板

总体方案确认后，每个模块使用相同模板继续细化。

```text
1. 模块定位
2. 负责什么
3. 不负责什么
4. 调用方与依赖方
5. 输入 Schema
6. 输出 Schema
7. 公开接口
8. 内部子模块
9. 状态所有权与写入规则
10. 错误和合法终止状态
11. 权限、PHI 和安全要求
12. Trace、AgentEvent、Audit
13. 幂等和恢复要求
14. 测试用例
15. 第一版范围
16. 后续扩展
```

## 48. 函数设计原则

应提前定义：

- 模块公开接口；
- 输入输出类型；
- 状态修改方式；
- 错误和终止状态；
- 幂等和版本参数；
- 安全和权限边界。

不应过早固定：

- 所有内部私有函数；
- 最终类继承结构；
- 所有数据库索引；
- 所有模型和 Prompt；
- 所有疾病通用抽象；
- 所有模块的微服务拆分。

## 49. 公开接口风格

优先使用：

```text
结构化 Request
→ 一个明确操作
→ 结构化 Result
```

例如：

```python
def evaluate_safety(request: SafetyEvaluationRequest) -> TriageAssessment: ...

def select_next_question(request: QuestionPolicyRequest) -> QuestionDecision: ...

def assemble_context(request: ContextAssemblyRequest) -> ContextEnvelope: ...

def commit_state_patch(patch: StatePatch) -> CommitResult: ...
```

避免：

```python
def process(data): ...
def handle_agent(context): ...
def run_everything(payload): ...
```

---

# Part J：设计完成与开工条件

## 50. 总体方案确认清单

在开始大规模开发前，需要确认：

- [ ] 系统定位为受约束 Agentic Workflow；
- [ ] 首个 Capability 已明确；
- [ ] 正常、高风险、工具失败、医生审核和恢复流程已画出；
- [ ] 11 个模块职责无明显重叠；
- [ ] 状态所有权矩阵已确认；
- [ ] 第一批核心契约已确认；
- [ ] Java/Python 契约同步方式已选定；
- [ ] 依赖方向已确认；
- [ ] 第一条纵向切片已确定；
- [ ] 第一阶段非目标已明确；
- [ ] 固定 Workflow 降级路径保留；
- [ ] Phase A～F 均有端到端验收。

## 51. 第一批模块详细设计顺序

总体方案确认后，建议依次输出：

1. Clinical Domain Contracts；
2. Clinical State & State Committer；
3. Safety & Policy Engine；
4. Agent Runtime 最小 Graph；
5. Durable Execution Resume MVP；
6. Clinical Intelligence；
7. Context Assembly；
8. Evidence Intelligence；
9. Business Review 与 Delivery；
10. Governance 和 Observability。

该顺序不是让前一个模块全部开发完成后再设计下一个，而是先稳定对后续影响最大的接口。

---

## 52. 最终结论

AIdoctor 的最终系统由四份专题方案共同描述，但实际开发统一落在本文档定义的十一项模块中。

```text
四份专题方案
定义系统各个关注面的完整边界

总体架构与模块设计
定义各部分如何联结和如何组织代码

共享数据契约
定义 Java、Python、数据库、事件和前端如何协作

分阶段纵向路线
定义当前应该实现哪些最小闭环
```

当前最重要的开发原则是：

> 先统一总体流程、模块职责、共享契约和状态所有权，再用纵向切片逐步实现，而不是按四份方案或十一个模块分别进行“大而全”的开发。
