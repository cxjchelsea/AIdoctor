# AIdoctor Current State Baseline V1

> 状态：FROZEN / V1
> 所属阶段：SOP Phase 0 — 现状与治理
> 基线分支：`main`
> 基线 HEAD：`617f7e45bafffdabdd16301d8e46b811006dc43a`
> 用途：后续重构的唯一现状基线与第一版资产处置依据。
> 实证来源：`docs/current/00_现状与治理/evidence/`
> 处置决策：`docs/current/00_现状与治理/重构决策_V1.md`

本文件只冻结当前 `main` 中可被代码、配置、测试、CI 或运行结构证实的系统状态，以及第一版 KEEP / REFACTOR / REPLACE / REMOVE / MISSING / UNKNOWN 处置。重构总原则、目标架构和建议实施路径不在本文件升格为施工令。

---

# 1. 基线声明

本文件描述的是：

> 当前 `main` 分支中真实存在并能够由代码、配置、测试、CI 或运行结构证实的系统状态。

本文件不把以下内容直接视为当前实现事实：

- 历史设计文档；
- Enterprise 目标架构文档；
- 已存在但没有进入当前业务主链的代码；
- 仅有测试但没有业务接入的基础设施；
- 仅有接口定义但无实际调用的功能；
- 仅有 Demo、Smoke Test、硬编码样例的能力；
- “理论上可以工作”的推断。

证据优先级固定为：

```text
当前 main 代码
>
当前配置 / CI / 测试 / 实际调用关系
>
当前文档
>
历史文档
>
推断
```

所有后续重构判断必须基于本文件，而不是重新从旧文档推测系统当前状态。

---

# 2. 当前系统总体定性

当前 AIdoctor 不是：

```text
一个完整的新 Enterprise Agent Runtime
```

也不是：

```text
一个只剩历史代码、应该推倒重写的旧系统
```

更准确的描述是：

> 一个已经具有较多真实医疗业务能力的旧业务系统，与一套正在建设中的 Enterprise Runtime / Contract / State / Model 基础设施并存，但两者尚未完成主链收敛。

当前仓库同时存在三代架构：

```text
A. 当前真实业务主链
Frontend
→ DiagnosisController
→ DiagnosisOrchestrationService
→ CDPManager
→ DiagnosisWorkflowOrchestrator
→ 各 Python / Java 临床服务

B. 历史 Agent 化实现
AgentLoop
AgentStateManager
ToolCaller
EvidenceFusion
ClinicalAgentBrain
...

C. Enterprise 基础设施
contracts/
capabilities/
packages/model_runtime/
packages/python_runtime/
StateCommitter
engineering/
docs/refactoring/
```

其中：

```text
A = 当前主要业务事实
B = 历史资产，部分可复用
C = 未来接管主链的基础设施，但目前尚未完成临床接管
```

因此重构策略必须是：

> 保留现有有效业务能力，逐步迁移到新的运行时、状态、契约和安全边界中，而不是重新开发全部功能。

---

# 3. 当前真实业务主链

目前已确认的真实 HTTP 入口是：

```text
Frontend
    ↓
DiagnosisController
/api/v1/diagnosis/*
    ↓
DiagnosisOrchestrationService
    ↓
CDPManager
+
DiagnosisWorkflowOrchestrator
    ↓
Clinical Parsing
Dialog
Diagnosis Engine
Workup Planner
Treatment
Risk
Explanation
Health State Assessment
...
```

`DiagnosisController` 当前直接依赖 `DiagnosisOrchestrationService`，没有使用新的 `AgentLoop`、`StateCommitter`、`Python Runtime Executor` 或 Model Runtime 作为临床主控。

因此当前系统的实际运行范式仍然是：

```text
Java 固定 Workflow
+
旧微服务能力
+
CDP 状态
```

而不是：

```text
Enterprise Runtime
→ Capability
→ StatePatch
→ StateCommitter
→ deterministic lifecycle
```

---

# 4. 当前问诊流程

## 4.1 Start

当前启动逻辑：

```text
创建 CDP
↓
调用 Health State Assessment
↓
判断 clinical / wellness
↓
写入 patientState / healthStateAssessment / cdpStatus
```

如果 Health State Assessment 服务异常，当前后端会：

```text
workMode = clinical_mode
needsClinicalMode = true
riskLevel = L4
↓
继续临床流程
```

即关键入口能力失败时仍允许进入临床模式。

前端又存在另一层 Fail-Open：

```text
健康状态判断请求异常
↓
前端直接设为 clinical_mode
healthAssessmentDone = true
```

因此安全入口目前并非不可绕过。

---

# 5. 当前 Continue 模型

当前 `/continue` / `/answer` 本质是：

```text
用户回答
↓
写 patientState.userAnswers
↓
Step 1
↓
计算 completeness
↓
completeness < 60
    → 继续提问

completeness >= 60
    → Step 2
    → Step 3
    → Step 4
    → Step 5
    → completed
```



因此当前核心业务状态机实际上主要依赖：

```text
completeness threshold
+
固定 5 Step Workflow
```

而不是一套严格定义：

```text
WAITING_FOR_USER
WAITING_FOR_TOOL
WAITING_FOR_DOCTOR
RUNNING
SUSPENDED
FAILED
COMPLETED
...
```

的业务生命周期状态机。

---

# 6. 当前 Diagnosis Workflow

当前 Orchestrator 五步：

```text
Step 1
问题识别 / 临床解析 / 主动问诊

Step 2
DDx + 风险分层

Step 3
候选组织 / Routing

Step 4
关键证据 / Workup

Step 5
终点结论
```

其价值在于：

> 它已经形成了真实的业务流程骨架。

但当前异常语义不一致：

```text
Clinical Parsing 失败
→ 使用旧 patientState 继续

Dialog 失败
→ 使用默认问题继续

Diagnosis Engine 失败
→ ddx = []

Risk 失败
→ 记录日志继续

部分后续步骤
→ 抛异常
```



因此流程骨架值得保留，但 Failure Policy 必须重构。

---

# 7. CDP 当前定位

CDP 已包含：

```text
patientState
ddx
evidenceGraph
workupPlan
managementPlan
uncertainty
audit
healthStateAssessment
triage
status
version
...
```

CDPManager 已经具有：

```text
version
版本快照
悲观锁
并发更新
版本递增
```



这是当前系统最重要的可保留资产之一。

但是当前更新方式仍大量依赖：

```text
Map<String, Object>
+
业务服务直接修改字段
```

并没有统一通过 StateCommitter。

因此：

```text
CDP aggregate concept = KEEP
CDP uncontrolled write model = REFACTOR
```

---

# 8. 当前状态体系

目前至少存在以下状态源：

```text
1. CDP
   Java / DB
   当前主要临床业务状态

2. AgentState
   历史 AgentLoop 状态

3. Dialog Redis state

4. Dialog memory fallback

5. Frontend Zustand state

6. StateCommitter repository state

7. Python Runtime Checkpoint

8. Execution Trace
```

问题不是“系统没有状态管理”，而是：

> 系统拥有过多状态源，而没有清晰的业务事实源与同步边界。

后续必须明确：

```text
Clinical Business Truth = CDP
Execution Truth = Runtime
Observation Truth = Trace
UI Local State = Frontend
```

其他状态只能作为派生状态或缓存。

---

# 9. AgentLoop 当前状态

历史 `AgentLoop` 已实现：

```text
Observe
→ Plan
→ Act
→ Update
→ Evaluate
```

并具有：

```text
ToolCaller
EvidenceFusion
AuditTrail
AgentState
CDP field writer
Stop / escalation / refusal
```

但若干关键医疗判断仍直接抛：

```text
ClinicalSemanticBlockerException
```

原因是：

```text
Red Flag
Conflict
Insufficient Evidence
```

尚无完整确定性语义实现。

同时当前 DiagnosisController 并不调用 AgentLoop。

因此：

```text
AgentLoop = PARTIAL + BYPASSED
```

不应直接把旧 AgentLoop 恢复成新主控。

---

# 10. StateCommitter 当前状态

StateCommitter 已实现大量重要机制：

```text
Capability Authorization
Consent
Field Permission
Source Validation
Idempotency
Base Version Conflict
ADD / REPLACE / REMOVE
Atomic Commit
Audit / Evidence
```



这是 Enterprise 重构中非常重要的真实资产。

但其当前 COMMITTED 只表示：

```text
机械 Repository 版本推进
```

而不是：

```text
真实 Clinical CDP 已写入
```

并且尚未接管当前 Diagnosis Workflow 的临床写入。

因此：

```text
StateCommitter implementation = KEEP
Current adapter/integration = MISSING
Clinical mainline ownership = MISSING
```

---

# 11. Python Runtime 当前状态

新的 Python Runtime 是：

```text
deterministic
engine-neutral
single Tool invocation runtime
```

明确不是：

```text
LangGraph
AgentLoop
Clinical Workflow
```

也明确：

```text
无自主循环
无隐式重试
```



当前 checkpoint 主要记录：

```text
accepted
completed
status
run_id
```

因此：

```text
Python Runtime = KEEP AS FOUNDATION
```

但不能把它误认为：

```text
Clinical Workflow Runtime 已完成
```

---

# 12. Workflow Checkpoint / Resume

当前存在 Runtime Checkpoint 基础结构，但没有证明存在：

```text
临床业务状态持久化
WAITING 原因
Pending Event
Resume Token
Expired Event
Duplicate Event
Doctor Review Resume
Tool Resume
User Resume
```

的完整业务恢复闭环。

Execution Trace 也不能替代 Checkpoint。

因此：

```text
Durable Clinical Workflow Checkpoint = MISSING
Clinical Resume = MISSING
```

---

# 13. Execution Trace

Execution Trace Service 是真实实现，能够记录：

```text
cdpId
traceId
eventType
service
module
method
step
status
duration
inputData
outputData
error
```



Diagnosis Service 的 AOP 能记录：

```text
SERVICE_CALL_START
SERVICE_CALL_END
SERVICE_CALL_ERROR
```



值得保留的是：

当前切面不会直接记录完整临床参数，而主要记录：

```text
参数数量
参数类型
返回类型
```

减少 PHI 泄漏风险。

但 Trace 默认配置并未开启，因此：

```text
Trace implementation = KEEP
Repo default runtime = OFF
Production state = UNKNOWN
```

---

# 14. Health State Assessment

真实实现存在，并且具有：

```text
health state assess
wellness screening
ToolContext → ToolResult
```

等接口。

但当前 ToolResult 中：

```text
confidence = 0.9
completeness = 0.85
accuracy = 0.88
```

属于固定值，而不是模型/能力 Eval 结果。

当前 `test_basic.py` 主要属于 Smoke/Demo，不是版本化临床评估。

因此：

```text
Health State capability = KEEP + REFACTOR
Quality metadata = REPLACE
Clinical Eval = MISSING
```

---

# 15. Clinical Parsing

已经真实实现：

```text
Concept Recognition
Normalization
Symptoms
Diseases
Medication
Examination
Allergy
Ambiguity Detection
```

但：

```python
signs = []
```

仍明确未实现。

因此：

```text
Clinical Parsing core = KEEP
Signs parsing = MISSING
Schema governance = REFACTOR
Clinical Eval = MISSING
```

---

# 16. Dialog

Dialog 已包含：

```text
Context
NLU / NLG
Gap Identification
Completeness
Adaptive Questioning
Redis
Memory fallback
```

属于真实业务资产。

但是其代码尝试调用：

```text
POST /api/v1/diagnosis/cdp/{cdp_id}/update
```

而当前 DiagnosisController 并不存在该接口。

代码自身也承认该更新不是关键路径。

因此：

```text
Dialog capability = KEEP
Dialog own state ownership = REFACTOR
Direct CDP callback write = REMOVE
Problem list integration = REFACTOR
```

未来 Dialog 应返回结构化 Capability Result，而不是自己修改 CDP。

---

# 17. Diagnosis Engine

Diagnosis Engine 包含：

```text
Rule Engine
Knowledge Graph Engine
Statistical Engine
LLM Engine
Differential Engine
Fusion Engine
Three Layer Classifier
Evidence Analyzer
Reasoning Organizer
```

因此其业务资产非常丰富。

但是当前 Fusion 策略：

```text
任一 Engine 失败
→ warning
→ 跳过
→ 剩余结果继续融合
```

没有明确：

```text
minimum evidence
minimum engine count
safety dependency
must-have exclusion
```

等门槛。

因此：

```text
Diagnosis Engine business capabilities = KEEP
Fusion policy = REFACTOR
Failure semantics = REFACTOR
Clinical Eval = MISSING
```

---

# 18. 当前 Legacy LLM 严重断层

当前 `common/aidoctor_llm.LangChainLLMClient` 已被 Enterprise 阶段改造成：

```text
FAIL_CLOSED_NO_ENABLE_PATH
```

其构造函数本身直接抛 `LegacyLLMDisabledError`。

但 Diagnosis Engine 的：

```text
get_llm_client()
```

仍直接构造：

```text
LangChainLLMClient(config)
```



并且 FusionEngine 创建时依赖该客户端。

因此当前 `main` 上存在确定性的架构冲突：

```text
Legacy LLM 已被安全冻结
+
旧 Diagnosis Engine 仍依赖 Legacy LLM
```

结论：

```text
Legacy LLM Runtime = REMOVE
Clinical LLM call sites = REPLACE
Replacement target = Model Runtime
```

这是当前重构中的最高优先级集成问题之一。

---

# 19. Prompt 体系

目前存在三套 Prompt 所有权：

```text
A. Legacy common Prompt facade
   已 fail-closed

B. Diagnosis Engine local Prompt
   仍保存 clinical diagnosis/question/explanation templates

C. Enterprise Model Runtime Prompt Registry
```

 

这种状态不能长期存在。

决定：

```text
Service-local executable Clinical Prompt = REPLACE
Legacy Prompt facade = REMOVE AFTER MIGRATION
Prompt Registry = KEEP
```

最终要求：

```text
所有正式模型调用
→ Model Runtime
→ Prompt Registry
→ Versioned Prompt
→ Eval binding
```

---

# 20. Model Runtime

Model Runtime 已经有：

```text
gateway
provider
prompt registry
prompt builder
resource
tests
```

属于实质性实现。

但当前真实 Diagnosis Engine LLM 路径仍绕过它。

因此：

```text
Model Runtime = KEEP
Clinical integration = MISSING
```

---

# 21. Knowledge Graph

知识图谱推理已经真实进入 Diagnosis Engine：

```text
symptom CUI
↓
retrieve disease paths
↓
path score
↓
prompt/path injection
↓
LLM
```

但 Neo4j 初始化或路径检索失败时：

```text
warning
↓
继续 LLM
```

因此：

```text
KG capability = KEEP
KG availability semantics = REFACTOR
KG evidence schema = REFACTOR
KG clinical Eval = MISSING
```

---

# 22. Risk Assessment

Risk Engine 已包含：

```text
症状组合规则
生命体征规则
高风险疾病规则
严重程度判断
分诊
```

但目前大量规则仍属于示例/初始规则。

此外存在一个高风险语义：

```text
DDx 为空
→ severity 可能降为 mild
```

这会把：

```text
“诊断能力失败”
```

误解释为：

```text
“病情较轻”
```

这是不可接受的语义。

因此：

```text
Risk Engine = REFACTOR
```

不能直接 KEEP AS-IS。

需要明确：

```text
NO_EVIDENCE
≠
LOW_RISK
```

---

# 23. Workup Planner

Workup Planner 已实现真实检查推荐逻辑，但仍包含大量原型规则。

因此：

```text
Workup capability = KEEP
Clinical rule content = REFACTOR
Output contract = REFACTOR
Eval = MISSING
```

---

# 24. Examination Service

Examination Service 已真实实现：

```text
检查计划
检查记录
报告上传
本地文件保存
OCR
结构化报告结果
检查历史
```



这些属于可保留业务资产。

但其“检查推荐”又单独包含：

```text
胸痛 → ECG / 心肌酶谱
发热 → X 光
所有情况 → 血常规
```

并明确标记为简化实现。

因此当前出现：

```text
Workup Planner
+
Examination Service
```

同时决定检查方案的问题。

决定：

```text
Examination record/report/OCR = KEEP
Examination clinical planning logic = REMOVE / MERGE INTO WORKUP
```

---

# 25. Examination Result Feedback Loop

当前检查报告主要落入：

```text
ExaminationRecord
↓
OCR
↓
reportStructuredData
```

尚未证明存在：

```text
report result
→ CDP Evidence
→ State Committer
→ Diagnosis re-evaluation
→ Risk re-evaluation
→ Workflow Resume
```

因此：

```text
Examination → Diagnosis closed loop = MISSING
```

---

# 26. Treatment / Management

Treatment 当前具有真实业务实现，但该领域属于高风险医疗输出。

当前没有充分证据证明：

```text
药物推荐
剂量
禁忌
交互作用
特殊人群
不确定性
人工审核
Clinical Eval
```

形成了可靠闭环。

因此：

```text
Treatment service = REFACTOR
Medication autonomous output = BLOCK UNTIL VERIFIED
Management planning capability = KEEP AS ASSET
```

禁止因为“代码已经存在”就视为可生产能力。

---

# 27. Explanation Service

Explanation Service 具有：

```text
Conclusion Builder
Evidence Chain Builder
Reasoning Path
Natural Language Explanation
```

真实实现。

但其 LLM 仍依赖已经被禁用的 Legacy LLM。

初始化失败后：

```text
llm_client = None
↓
自然语言解释直接跳过
```



因此：

```text
Deterministic conclusion/evidence builders = KEEP
Legacy LLM explanation = REPLACE
Presentation fallback policy = REFACTOR
```

---

# 28. Frontend

Frontend 已具备真实问诊 UI 和状态管理。

但存在两个关键问题。

第一：

```text
第一次用户消息
↓
performHealthAssessment()
→ /diagnosis/start
↓
随后 diagnosisId 已存在
↓
同一 content 又进入 answerQuestion()
```

因此同一首次输入存在被重复消费的风险。

第二：

`convertDiagnosisResult()` 会在后端信息缺失时自己生成：

```text
待诊断
需要进一步检查
默认 confidence
默认 action
默认 followup
```

这些属于临床语义，而不是纯 UI 展示。

决定：

```text
Frontend UI = KEEP
Frontend clinical inference = REMOVE
Frontend state flow = REFACTOR
First-message flow = REFACTOR
```

原则：

> 前端只展示后端明确提供的状态，不创造临床结论。

---

# 29. CI / Test 当前事实

目前 Enterprise CI 能有效验证：

```text
Contracts
StateCommitter
Python Runtime
Model Runtime
NC Safety Invariants
Frontend build
Java foundation tests
```

这是有效资产。

但是它没有充分验证：

```text
Frontend
→ Diagnosis Service
→ Clinical Parsing
→ Dialog
→ Diagnosis Engine
→ Risk
→ Workup
→ Treatment
→ Explanation
```

这条真实业务闭环。

一个非常明确的例子：

```text
Legacy LLM 被禁用
```

对于 Enterprise Freeze Test 是正确行为；

但旧 Diagnosis Engine 又依赖它。

因此：

```text
Enterprise CI PASS
```

和：

```text
Clinical Runtime broken
```

可以同时成立。

决定：

```text
Existing foundation tests = KEEP
Mainline integration tests = MISSING
Clinical capability Eval = MISSING
E2E business tests = MISSING
```

---

# 30. CI Branch 配置

当前 canonical branch 已是：

```text
main
```

但 CI workflow 仍主要绑定旧 Enterprise branch。

因此：

```text
CI branch trigger = REFACTOR
```

必须先保证：

```text
main PR
main push
```

均进入正确 CI。

---

# 31. Deployment

当前 `docker-compose.yml` 没有覆盖所有实际业务依赖服务。

实际主链需要的若干服务并不在 Compose 中。

因此：

```text
docker-compose = PARTIAL
Full reproducible deployment = UNKNOWN
```

决定：

```text
Deployment topology = REFACTOR
```

---

# 32. 文档真实性分类

## 32.1 当前代码

```text
CURRENT TRUTH
```

作为最高优先级事实源。

## 32.2 docs/refactoring

作用：

```text
TARGET ARCHITECTURE / GOVERNANCE
```

不是：

```text
CURRENT RUNTIME DESCRIPTION
```

## 32.3 docs/AI医生/项目文档

作用：

```text
HISTORICAL DESIGN + DOMAIN INTENT
```

其中很多业务思想仍有价值，但必须逐项由代码验证。

## 32.4 当前系统资产盘点

作用：

```text
PARTIALLY VALID HISTORICAL INVENTORY
```

由于后续 Enterprise 改造与 main 合并，其部分结论已经过时。

---

# 33. 当前核心风险登记

| ID | 风险 | 等级 |
|---|---|---|
| CSA-INTEGRATION-001 | Legacy LLM 已禁用但 Diagnosis Engine 仍依赖 | CRITICAL |
| CSA-SAFETY-001 | Health State / Safety Entry 失败后仍进入临床流程 | HIGH |
| CSA-SAFETY-002 | Empty DDx / Missing Evidence 可能降为 mild | CRITICAL 候选 |
| CSA-FLOW-001 | 首条用户输入可能被 start + answer 重复消费 | HIGH |
| CSA-FLOW-002 | Examination Result 无完整诊断回流 | HIGH |
| CSA-STATE-001 | 多状态源并存 | HIGH |
| CSA-STATE-002 | 当前临床写入绕过 StateCommitter | HIGH |
| CSA-DATA-001 | Clinical Parsing 与 Risk symptom schema 不一致 | HIGH |
| CSA-DIALOG-001 | Dialog 调用不存在的 CDP update API | MEDIUM/HIGH |
| CSA-DIALOG-002 | problem_list 生成但无可靠主状态持久化 | MEDIUM |
| CSA-MODEL-001 | 当前临床路径绕过 Model Runtime | HIGH |
| CSA-PROMPT-001 | 三套 Prompt 所有权并存 | HIGH |
| CSA-AI-001 | Tool quality 使用固定 confidence/accuracy | HIGH |
| CSA-AI-002 | 缺少版本化 Clinical Eval | HIGH |
| CSA-ARCH-001 | Frontend 构造临床 fallback 结论 | HIGH |
| CSA-TEST-001 | CI 与当前真实临床主链验证错位 | HIGH |
| CSA-CI-001 | main 已成为主分支但 CI 仍绑定旧 branch | HIGH |
| CSA-DEPLOY-001 | Compose 不覆盖完整真实依赖 | HIGH |
| CSA-RUNTIME-001 | 无 Durable Clinical Workflow Checkpoint/Resume | HIGH |
| CSA-CAPABILITY-001 | Workup 与 Examination 重复决定检查 | MEDIUM/HIGH |
| CSA-TRACE-001 | Trace summary 回写接口未证明存在 | MEDIUM |

---

# 34. 第一版资产处置矩阵

## KEEP

保留其核心思想和主体实现：

```text
CDP aggregate
CDP version history
CDP pessimistic concurrency

Clinical Parsing core
Dialog questioning/gap capability
Diagnosis Engine domain sub-engines
KG reasoning capability
Workup domain capability
Examination record/report/OCR capability
Explanation deterministic builders

StateCommitter
Contracts
Capability package structure
Model Runtime
Python Runtime
Execution Trace
Enterprise governance / safety foundation

Frontend UI structure
```

KEEP 不代表无需修改。

---

# 35. REFACTOR

保持功能目标，但重新定义边界或实现：

```text
Diagnosis Workflow Orchestrator
CDP write model
Health State Assessment failure policy
Clinical Parsing schema
Dialog state ownership
Diagnosis Fusion policy
Risk Assessment
Workup rules
Treatment / Management
Explanation failure semantics
Frontend diagnosis flow
Frontend result rendering
Trace integration
Deployment topology
CI branch configuration
```

---

# 36. REPLACE

不应继续沿用当前实现路径：

```text
Legacy clinical LLM calls
→ Model Runtime

Service-local executable Prompt
→ Prompt Registry

Direct clinical Map mutation
→ typed StatePatch + StateCommitter

completeness-only workflow progression
→ explicit state transition rules

clinical failure → empty object
→ explicit Failure / Uncertainty state
```

---

# 37. REMOVE

在迁移完成后应删除或彻底退出业务路径：

```text
Legacy aidoctor_llm executable runtime
Legacy Prompt executable facade
Dialog direct CDP callback write
Trace direct summary callback if superseded
Frontend fabricated clinical conclusions
Duplicate Examination planning logic
Obsolete AgentState ownership
Unused old architecture adapters
```

注意：

REMOVE 应在替代路径完成后执行。

禁止提前删除导致业务能力丢失。

---

# 38. MISSING

当前需要新建设的真正缺口：

```text
Clinical Workflow Lifecycle State Machine

Durable Workflow Checkpoint

Resume protocol

WAITING_FOR_USER
WAITING_FOR_TOOL
WAITING_FOR_DOCTOR

Event idempotency

Expired / duplicate event handling

Doctor Review closed loop

Clinical StateCommitter adapter

Model Runtime clinical adapter

Versioned Clinical Prompt

Versioned Clinical EvalSet

Safety Eval

Diagnosis Eval

Risk Eval

Questioning Eval

Workup Eval

Treatment Eval

Examination-result feedback loop

Full clinical E2E tests

Production-grade deployment topology
```

---

# 39. UNKNOWN

目前不应假装已经确认：

```text
当前线上环境是否启用了 Execution Trace

Neo4j 当前真实数据质量

真实 Clinical Knowledge 数据版本

真实模型 Provider / 模型版本

实际线上部署拓扑

生产环境 Nacos 覆盖配置

所有旧 Python 服务当前是否仍独立部署

真实历史患者数据兼容情况

PHI 在所有日志中的完整传播情况

数据库生产 Schema 与仓库实体完全一致性

Doctor Review 是否存在仓库外系统

现有 Clinical Eval 是否存在仓库之外
```

这些问题后续应随着对应业务单元重构按需关闭，而不是继续无边界盘点。

---

# 40. 文件边界

本文件到此冻结。后续重构判断必须基于本文件，而不是重新从旧文档或过程盘点稿推测系统当前状态。

配套文件：

- 重构总原则与建议实施路径：`docs/current/00_现状与治理/重构决策_V1.md`
- 本轮考古/实证过程稿：`docs/current/00_现状与治理/evidence/`
- 产品边界：`docs/current/01_需求/需求与系统边界_V1.md`
- 功能模块：`docs/current/02_功能/功能模块划分_V1.md`

```text
Repository Archaeology = COMPLETE
Current State Assessment = COMPLETE / FROZEN V1
Baseline V1 = FROZEN
Implementation = NOT_STARTED
Clinical Runtime Migration = NOT_STARTED
Production Clinical Enablement = NOT_AUTHORIZED
```

