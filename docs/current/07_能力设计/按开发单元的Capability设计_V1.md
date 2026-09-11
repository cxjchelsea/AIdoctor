# AIdoctor V1 按开发单元的 Capability 设计

> 文档状态：FROZEN / V1  
> 所属阶段：复杂业务软件开发 SOP — Phase 7 按开发单元识别、调用与建设 Capability  
> 上游权威：`01_需求/需求与系统边界_V1.md`、`02_功能/功能模块划分_V1.md`、`03_状态/系统级状态主干_V1.md`、`03_状态/模块级状态与状态所有权_V1.md`、`05_业务闭环/业务闭环设计_V1.md`、`06_开发单元/可验证开发单元拆分_V1.md`  
> 当前事实依据：`00_现状与治理/Current_State_Baseline_V1.md`  
> 历史参考：`docs/refactoring/capability-package规范.md` 仅作为资产与治理参考，不覆盖 Phase 1–6 已冻结结论。  
> 本文定义 U01–U15 完成自身业务状态转换所依赖的 Clinical Capability、确定性 Policy、State Governance 与 Runtime 能力，并给出现有资产处置与最低质量门槛；不冻结 API、数据库表、具体 StatePatch Schema、Java/Python 类、具体模型供应商、Prompt 正文、RAG 参数或部署拓扑。

---

# 1. Phase 7 目标

Phase 6 已冻结 15 个可验证开发单元。Phase 7 回答：

> 每个 Unit 为完成自己的业务状态转换，究竟需要哪些能力？哪些能力已经存在、哪些可以复用、哪些必须改造或替换、哪些确实需要新建？

核心不是“一个 Unit 建一个 Agent”，而是：

```text
Unit
→ 识别 Business Decision
→ 优先判断确定性逻辑是否足够
→ 判断是否需要 Clinical Capability
→ 判断是否需要 Governance / Runtime
→ REUSE / ADAPT / REFACTOR / REPLACE / NEW
→ 定义能力边界与最低质量门槛
```

---

# 2. Capability 与业务真值的边界

## 2.1 Capability 的定义

Capability 是在明确输入边界内产生结构化、可治理结果的可复用能力，例如：

- 临床文本理解；
- 红旗/风险证据识别；
- Gap 与问题候选生成；
- DDx / Must-Exclude 分析；
- 线下证据需求评估；
- 患者侧语言渲染。

```text
Capability Result != Clinical Truth
```

Capability 只能提供候选、证据、建议或结构化结果。最终业务状态仍由 Phase 4 已冻结的业务 Owner 解释，并由 G2 在属于 Clinical State/CDP 的情况下合法提交。

## 2.2 不应 AI 化的职责

以下优先属于确定性业务 Policy / Governance / Runtime：

- Consultation 生命周期合法转移；
- Scope 最终裁决；
- Clinical Risk Disposition 最终裁决；
- Safety Gate 最终授权；
- Clinical Readiness 唯一求值；
- StateCommitter 合法写入；
- 幂等、版本冲突、Duplicate / Expired Resume；
- Question stopping；
- Correction / Dependency Invalidation；
- Delivery Validation；
- Failure Routing；
- Cancel / Expire；
- 已冻结业务不变量校验。

原则：

```text
能由确定性规则可靠表达的最终业务约束
→ 不交给概率模型做最终裁决
```

## 2.3 单一 Owner 约束

```text
Parsing Capability 不拥有 Clinical Facts
Risk Capability 不拥有 Clinical Risk Disposition
Risk Capability 不拥有 Safety Gate
Scope semantic extraction 不拥有 OUT_OF_SCOPE 最终结论
Question Capability 不拥有 WAITING_USER
DDx Capability 不拥有 COMPLETED
Delivery Validator 不拥有 Delivery Readiness
Rendering Capability 不拥有 Clinical Truth
Dependency Invalidation Engine 不成为跨模块状态 Owner
```

---

# 3. Phase 7 处置分类

```text
REUSE_FOUNDATION
现有基础设施值得保留，但可能尚未进入临床主链。

ADAPT
主体能力可复用，需要适配层、结构化输出或治理接入。

REFACTOR
业务资产有价值，但 Failure、状态所有权、规则、Schema 或质量体系不足。

REPLACE
现有实现方式不能继续使用，需要迁移到批准的替代路径。

NEW
当前基线不存在满足 Phase 1–6 语义的正式能力。

NOT_A_CAPABILITY
本质是确定性业务 Policy / Governance，不建设独立 AI Capability。

DEFER
属于后续产品阶段，不进入 V1。
```

---

# 4. 共享平台与确定性 Policy

## 4.1 Platform Capability

| ID | 职责 | 当前资产 | Phase 7 处置 |
|---|---|---|---|
| P01 | Clinical State Governance / State Commit | `StateCommitter` + CDP versioning | REUSE_FOUNDATION + ADAPT；Clinical CDP adapter NEW |
| P02 | Clinical Workflow Durable Execution | Python Runtime checkpoint 基础 | REUSE_FOUNDATION + NEW clinical orchestration/resume layer |
| P03 | Model Runtime / Prompt Registry | Enterprise Model Runtime | REUSE_FOUNDATION + ADAPT |
| P04 | Evidence / Knowledge Governance | KG + Evidence 资产 + G6 | REFACTOR + ADAPT |
| P05 | Trace / Audit | Execution Trace | REUSE_FOUNDATION + ADAPT |
| P06 | Capability Scope / Version Binding | 历史 Capability Package 思路 | NEW current V1 binding layer |

P01 的适用原则不是封闭 Unit 列表，而是：

```text
任何 Unit
只要要把模块状态写入正式 Versioned Clinical State / CDP
→ 必须经过 P01 / G2
```

纯 Consultation lifecycle 状态由生命周期治理负责；纯 Thread/Run/Checkpoint 状态由 Runtime 负责。

## 4.2 Deterministic Policy

```text
D01 Consultation Lifecycle Policy
D02 Safety Gate Resolver
D03 Clinical Readiness Resolver
D04 Question Stopping Policy
D05 Correction / Dependency Invalidation Policy
D06 Delivery Validator
D07 Failure Router / Fallback Policy
D08 Cancel / Expire Policy
D09 Clinical Risk Disposition Resolver
D10 Scope Adjudication Policy
```

核心控制链：

```text
Clinical Intelligence proposes
↓
Business Owner / Deterministic Policy interprets
↓
State Governance commits when Clinical State changes
↓
Runtime executes / resumes
↓
Trace observes
```

---

# 5. U01 — Consultation 建立与对象/问题框定

## 5.1 所需能力

```text
Consultation lifecycle policy
Subject Resolution
Problem Framing / intent clarification detection
Scope-related semantic extraction
early safety-signal passthrough
Scope Adjudication
```

## 5.2 当前资产与处置

Subject / Problem 语义可从 Clinical Parsing、Dialog NLU、Health State Assessment 中吸收：

```text
Subject / Problem semantic extraction = ADAPT + REFACTOR
```

能力只返回主体候选、主诉/意图候选、歧义和需澄清信号。

Scope 必须拆成两层：

```text
C01 semantic extraction
→ population / complaint / intent / concept candidates

P06 Capability Scope + Version Binding
+
D10 Scope Adjudication Policy
→ IN_SCOPE / OUT_OF_SCOPE / NEEDS_CLARIFICATION
```

最终 `OUT_OF_SCOPE` 不由模型自由决定。

## 5.3 最低门槛

- 本人/代他人识别有专项测试；
- 混合诉求不掩盖临床不适；
- OUT_OF_SCOPE 可追溯到绑定的 Scope Version；
- 歧义形成 clarification need，不静默猜测；
- 风险线索不因 framing 不完整被丢弃。

---

# 6. U02 — 临床事实形成与版本提交

## 6.1 所需能力

```text
Clinical Parsing / Normalization
Source Attribution
Ambiguity / Contradiction Detection
State Governance
Dependency Invalidation Proposal
```

## 6.2 当前资产与处置

```text
Clinical Parsing core = REFACTOR
Signs parsing = NEW
Clinical Parsing Eval = NEW
StateCommitter = REUSE_FOUNDATION + ADAPT
Clinical CDP adapter = NEW
```

Parsing 输出必须保留：fact candidate、value semantics、source、confidence/uncertainty、negation、normalization、ambiguity/contradiction、provenance。

禁止：

```text
UNKNOWN → NO
UNMEASURED → NORMAL
MODEL_INFERRED → PATIENT_REPORTED
```

## 6.3 最低门槛

- versioned extraction EvalSet；
- 否定/时间/程度/主体/单位/药物/检查等专项评估；
- UNKNOWN/UNMEASURED 语义保真；
- 幂等提交、base-version conflict、并发写测试；
- Capability success 但 G2 reject 时 Unit 仍视为未成功。

---

# 7. U03 — 当前版本风险评估

## 7.1 所需能力

C02 Clinical Safety Intelligence 只负责提供风险证据与候选评估：

```text
Red Flag Evidence
Risk Factor / Combination Rule Hits
Vital-sign Safety Signals
High-risk / Must-not-miss Signals
Proposed Risk Assessment
```

不得由 C02 最终拥有 `Clinical Risk Disposition`。

最终链：

```text
C02 structured risk evidence/result
+
versioned Safety-critical Risk Rule Pack
↓
D09 Clinical Risk Disposition Resolver / F4
↓
NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK
```

## 7.2 当前资产与处置

```text
Risk Engine = REFACTOR
Safety-critical Risk Rule Pack = NEW / versioned
D09 Risk Disposition Resolver = NEW deterministic policy
Risk EvalSet = NEW
Risk failure semantics = explicit contract
```

现有 `DDx empty → mild` 语义必须删除。

## 7.3 最低门槛

- red-flag recall 优先；
- Risk failure 100% 不得投射为 `NO_HIGH_RISK_SIGNAL`；
- `NO_DDX != LOW_RISK`；
- Risk 结果绑定 Clinical State Version + Rule/Knowledge/Capability Version；
- 特殊人群、生命体征和 must-not-miss 有独立 safety suite。

---

# 8. U04 — Safety Gate 决策

U04 不建设 AI Capability。

```text
Clinical Risk
+ capability availability
+ Scope / Consent / Authorization
+ mandatory safety rules
→ D02 Safety Gate Resolver
→ ALLOW / RESTRICTED / BLOCKED / UNAVAILABLE
```

```text
D02 = NEW + NOT_A_CAPABILITY
Enterprise safety/governance foundations = REUSE_FOUNDATION
```

最低门槛：HIGH_RISK 不得普通 ALLOW；Risk/Safety Capability failure fail-closed；Gate decision table 可穷举测试；结果可重放和解释。

---

# 9. U05 — Clinical Readiness 唯一求值

U05 不建设自由 Agent。

```text
F1/F3/F5/F6 readiness inputs
+ stage applicability
+ blocking semantics
→ D03 Clinical Readiness Resolver
```

```text
D03 = NEW + NOT_A_CAPABILITY
```

最低门槛：同一版本同一输入得到同一结果；`absent-by-design != failure != negative`；冲突有确定性优先级；Safety Gate 非普通允许时不得绕过。

---

# 10. U06 — 关键问题选择与 WAITING_USER

## 10.1 所需能力

```text
Question Candidate Generation
Question Value / Priority Assessment
Duplicate / Already-answered Filtering
Question Rendering
D04 Stopping Policy
```

来源包括 F1 Clarification Requirement 与 F3 Information Gap。

## 10.2 当前资产与处置

```text
Dialog question/gap assets = REFACTOR
Dialog direct CDP write = REMOVE
Dialog private truth state = REPLACE BY governed state inputs
Question Planner = ADAPT / REFACTOR
Question Renderer = ADAPT / REFACTOR
D04 Stopping Policy = NEW deterministic policy
```

最终选择/是否进入 WAITING_USER 仍由业务单元与确定性规则决定。

## 10.3 最低门槛

- question purpose 可追溯；
- duplicate question rate 有 Eval；
- 已回答/invalidated Gap 不重复问；
- USER_UNKNOWN/UNMEASURED 不机械重复问；
- 无决策价值则停止；
- 问题表达无诊断暗示和治疗越界。

---

# 11. U07 — 用户回答 Resume 与幂等恢复

U07 主要依赖 Runtime / Governance：

```text
Pending Question identity
Resume validation
Duplicate detection
Expiry
Checkpoint compatibility
Clinical State Version compatibility
Idempotent apply
```

```text
Python Runtime = REUSE_FOUNDATION
Clinical Durable Resume Layer = NEW
```

最低门槛：Duplicate 至多一次临床效果；expired/wrong-version 0 写入；process restart/page refresh 后合法恢复；stale checkpoint 不得成为 Clinical Truth；支持 crash/replay。

---

# 12. U08 — DDx / Must-Exclude 评估

## 12.1 所需能力

```text
Candidate Retrieval / Generation within approved scope
KG Reasoning
Evidence Matching
Candidate Ranking / Tiering
Must-Exclude Identification
Supporting / Opposing / Unknown Evidence
DDx Assessment Proposal
```

## 12.2 当前资产与处置

```text
Diagnosis Engine domain assets = REFACTOR
KG retrieval/reasoning = REFACTOR + ADAPT
Legacy LLM path = REPLACE
Clinical model calls = Model Runtime
Prompt ownership = Prompt Registry
DDx Eval = NEW
```

Candidate 只能来自批准 Scope/Hypothesis space；KG path 不自动等于 Citation；`NO_RELIABLE_DIRECTION != FAILED`；Must-Exclude 未排除不得隐藏。

最低门槛包括 candidate 分层评估、must-not-miss recall、证据正确性、unsupported candidate rate、failure/no-result 语义测试及版本绑定。

---

# 13. U09 — DDx 后 Gap 重评与循环路由

```text
Gap Re-evaluation
Decision Impact Estimation
Question Value Estimation
Stale Gap Invalidation Proposal
Stopping Recommendation
```

当前资产与处置：

```text
Dialog Gap assets = REFACTOR
Diagnosis Evidence Analyzer = ADAPT as input
Question value capability = ADAPT/REFACTOR
D04 Stopping Policy = NEW deterministic
```

Capability 只回答“缺什么、影响什么、哪些问题值得候选”；是否继续循环由 U09 + U05 的确定性规则决定。

最低门槛：candidate change 能触发相关 Gap；candidate invalidation 能撤销旧 Gap；无价值 Gap 不进入下一轮；不用固定 completeness 百分比替代决策价值。

---

# 14. U10 — 线下证据需求与检查建议

```text
Offline Evidence Need Assessment
Examination Direction Recommendation
Specific Examination Suggestion when formally supported
Evidence-gap-to-exam mapping
```

当前资产：

```text
Workup capability = REFACTOR
Examination planning logic = REMOVE / MERGE INTO WORKUP
Examination record/report/OCR = REUSE_FOUNDATION but Phase 2 product loop DEFER
Workup Eval = NEW
```

边界：`Suggestion != Medical Order`；`Assessment FAILED != NOT_NEEDED`；具体检查必须来自批准规则/知识；V1 不建设跨天检查结果自动 Resume。

---

# 15. U11 — Safe Exit 结果组装与交付

## 15.1 所需能力

```text
Safe Exit Package Assembly
Reason-specific Content Selection
Patient-safe Rendering
D06 Delivery Validation
```

当前资产：

```text
Deterministic Explanation Builders = REUSE / ADAPT
Legacy LLM Explanation = REPLACE
Rendering model call = Model Runtime + Prompt Registry
D06 Delivery Validator = NEW / REFACTOR deterministic gate
```

关键边界：

```text
D06 Validation Result != Delivery Readiness
```

D06 只返回 validation result / violations。最终：

```text
F7
→ 根据结构化 Package + Validation Result
→ 解释并拥有 Delivery Readiness
```

渲染模型不得新增疾病、风险、检查或行动；Delivery failure 不得把 Consultation 写成 SAFE_EXIT。

---

# 16. U12 — 正常结果组装与 COMPLETED

与 U11 共享 C06 Delivery & Explanation 能力族，但使用正常完成策略，不复制第二套解释系统。

```text
Structured Delivery Assembly = ADAPT / NEW orchestration
Deterministic Evidence/Conclusion builders = REUSE/ADAPT
Natural-language Rendering = Model Runtime
D06 Delivery Validator = shared deterministic gate
F7 = Delivery Readiness 唯一业务 Owner
```

最低门槛：只引用当前有效版本；blocking Must-Exclude 未解决不得通过；UNKNOWN/UNMEASURED 保留；疾病方向不表达为确诊；禁止自动处方/治疗；Delivery validation failure 不得 COMPLETED。

---

# 17. U13 — 用户更正事件验证与失效触发

U13 主要依赖确定性 Event / State Governance：

```text
Correction Event Validation
Target Fact Resolution
Expiry / Applicability Validation
Dependency Invalidation Rules
```

处置：

```text
Correction Event Policy = NEW + NOT_A_CAPABILITY
D05 Dependency Invalidation Rules = NEW deterministic governance
StateCommitter = REUSE_FOUNDATION + ADAPT
```

D05 明确归属于 G2 临床状态治理机制，不成为新的跨模块状态 Owner：

```text
F3-F7
→ 提供 dependency / invalidation requirement

D05 / G2
→ 计算受影响范围

G2
→ 最终验证并提交 STALE / INVALIDATED / SUPERSEDED
```

F2 Parsing 可在 Correction 被接受后重新解释新事实，但不决定能否修改正式历史。

最低门槛：rejected correction 0 Clinical State effect；accepted correction 只产生一次新版本；旧 Delivery/history 不静默覆盖；只失效受影响派生状态；Safety 可重新评估。

---

# 18. U14 — Capability / Runtime Failure 路由

U14 是确定性 Failure Policy + Runtime control，不是 LLM Agent：

```text
Failure Classification
Retry/Repair Eligibility
Approved Fallback Eligibility
Safety Impact
Safe Exit Feasibility
Terminal Failure Decision
```

```text
D07 Business Failure Router = NEW + NOT_A_CAPABILITY
Runtime retry/repair primitives = REUSE_FOUNDATION + ADAPT
Fallback Registry / Policy = NEW governance
```

最低门槛：Failure 显式分类；failure 不变成 empty normal object；safety-sensitive failure 先形成 Gate 后果；retry/fallback 幂等且不绕版本；能 Safe Exit 时不机械 FAILED_TERMINAL。

---

# 19. U15 — Consultation 取消与过期终止

```text
Lifecycle Transition Policy
Cancellation Validation
Waiting Expiry Policy
Late Event Rejection
Runtime Cancellation / Suspension
```

```text
D08 Lifecycle termination policy = NEW + NOT_A_CAPABILITY
Runtime cancellation primitives = REUSE_FOUNDATION + ADAPT
```

不需要 AI Capability。

最低门槛：CANCELLED/EXPIRED 后普通 Resume 拒绝；迟到异步结果不写状态；Expiry 只用于合法 WAITING_USER；不修改历史 Clinical Truth；继续咨询默认新 Consultation。

---

# 20. U01–U15 Capability 处置矩阵

| Unit | 核心 Clinical Capability | Governance / Runtime / Policy | 当前处置 |
|---|---|---|---|
| U01 | Subject/Problem semantic extraction | D01 + P06 + D10 Scope Adjudication | Parsing/Dialog ADAPT/REFACTOR；Scope binding/adjudication NEW |
| U02 | Clinical Parsing/Normalization | P01 StateCommitter | Parsing REFACTOR；Clinical adapter NEW |
| U03 | Red Flag/Risk Evidence | D09 Risk Disposition + risk rule governance | Risk REFACTOR；Rule Pack/Resolver/Eval NEW |
| U04 | 无独立 AI Capability | D02 Safety Gate Resolver | NEW deterministic |
| U05 | 无独立 AI Capability | D03 Readiness Resolver | NEW deterministic |
| U06 | Gap/Question Planner/Renderer | D04 stopping + wait transition | Dialog REFACTOR |
| U07 | 无临床 AI | P02 Durable Resume / Idempotency | Runtime foundation REUSE + clinical resume NEW |
| U08 | DDx/KG/Evidence/Must-Exclude | P03/P04/P06 | Diagnosis/KG REFACTOR；Legacy LLM REPLACE |
| U09 | Gap Re-evaluation/Question Value | D04 Stopping | Dialog/Evidence assets REFACTOR/ADAPT |
| U10 | Offline Evidence/Workup | P04 knowledge/rule governance | Workup REFACTOR；duplicate planning REMOVE |
| U11 | Patient-safe Explanation | D06 Validator + F7 ownership | builders ADAPT；LLM REPLACE；validator NEW |
| U12 | Clinical Summary/Explanation | D06 Validator + F7 ownership | 与 U11 共享能力族 |
| U13 | Parsing after correction | D05/G2 Correction+Invalidation+P01 | governance NEW + StateCommitter ADAPT |
| U14 | 无临床 AI | D07 Failure Router/Fallback/P02 | NEW deterministic governance |
| U15 | 无临床 AI | D08 lifecycle/expiry/cancel/P02 | NEW deterministic + Runtime ADAPT |

---

# 21. V1 Clinical Capability Family

## C01 Clinical Understanding

覆盖 U01/U02/U13：Subject/Problem semantic extraction、Clinical Parsing、Normalization、Ambiguity/Contradiction、Source Attribution。

主体资产：Clinical Parsing + Dialog NLU，REFACTOR。

注意：C01 只负责 Scope 相关语义抽取，不拥有 Scope 最终裁决。

## C02 Clinical Safety Intelligence

覆盖 U03，向 D09/F4 与 U04 提供输入。

包括：Red Flag Evidence、Risk Rule Hits、Vital-sign Signals、Must-not-miss Signals、Proposed Risk Assessment。

主体资产：Risk Engine，REFACTOR；关键 Risk Rule Pack 与 Eval NEW。

注意：C02 不拥有最终 Clinical Risk Disposition。

## C03 Question & Gap Intelligence

覆盖 U06/U09：Gap、Question Candidate、Decision Value、Question Rendering。

主体资产：Dialog，REFACTOR。Stopping Policy 不属于概率能力的最终权限。

## C04 Differential & Evidence Intelligence

覆盖 U08/U09：candidate、KG reasoning、evidence relation、ranking、Must-Exclude。

主体资产：Diagnosis Engine + KG，REFACTOR；Legacy LLM REPLACE。

## C05 Offline Evidence / Workup Intelligence

覆盖 U10。主体资产：Workup Planner，REFACTOR；Examination 中重复规划逻辑移除。

## C06 Clinical Delivery & Explanation

覆盖 U11/U12。结构化 builder 优先 deterministic；语言渲染可经 Model Runtime，但不得产生新 Clinical Truth。主体资产：Explanation，ADAPT/REFACTOR；Legacy LLM REPLACE。

---

# 22. Model / Prompt / KG 统一处置

## 22.1 Legacy LLM

```text
Legacy common LLM runtime = REMOVE after migration
旧 clinical call sites = REPLACE
```

禁止为了恢复旧功能重新启用已 fail-closed Legacy LLM。

## 22.2 Model Runtime

正式模型调用逐步统一为：

```text
Unit
→ approved Capability
→ Model Runtime
→ versioned Prompt Registry
→ approved model route
→ structured Capability Result
→ Business Owner / Deterministic Policy interpretation
→ G2 commit when formal Clinical State changes
```

## 22.3 KG / RAG / Evidence

KG、RAG、规则、文献是不同 evidence/capability source，不是互斥架构选型。必须区分：patient evidence、clinical rule evidence、KG reasoning path、medical knowledge citation、model inference。

---

# 23. Capability Failure 统一语义

概率性或外部依赖 Capability 至少表达：

```text
SUCCESS
NO_RESULT
INSUFFICIENT_INFORMATION
NOT_APPLICABLE
UNSUPPORTED
DEPENDENCY_FAILURE
TIMEOUT
INVALID_OUTPUT
SAFETY_BLOCKED
```

```text
NO_RESULT != DEPENDENCY_FAILURE
INSUFFICIENT_INFORMATION != INVALID_OUTPUT
UNSUPPORTED != TIMEOUT
SAFETY_BLOCKED != SUCCESS
```

业务 Unit 不允许通过 `null / [] / {}` 猜测语义。

---

# 24. Capability 质量门槛模型

Capability 进入正式 Unit 前至少需要：

```text
Capability ID / Version
Purpose / Supported Scope
Input semantic boundary
Output semantic boundary
Failure semantics
Knowledge / Rule / Prompt / Model versions where applicable
EvalSet
Baseline
Metrics
Acceptance threshold
Safety cases
Regression suite
Fallback / unavailable behavior
Owner / Review status
```

不同 Capability 使用不同重点指标：Risk 重 red-flag recall/fail-closed；Parsing 重 fact/value/source；Question 重 decision value/重复率/负担；DDx 重 supported candidate/must-not-miss/evidence；Workup 重必要性/依据；Delivery 重 faithfulness/uncertainty/no-new-claims。

Phase 7 冻结质量维度，不冻结具体阈值数值。

---

# 25. Slice A 最小能力集合

Phase 6 已冻结首个候选纵向切片 U01–U07，并要求 U11/U14/U15 提供可达横向闭合。

Slice A 最小集合：

```text
C01 Clinical Understanding
C02 Clinical Safety Intelligence
C03 Question & Gap Intelligence

P01 State Governance / Clinical CDP Adapter
P02 Durable Clinical Resume
P03 Model Runtime clinical adapter（仅当对应 Capability 使用模型）
P05 Trace / Audit
P06 Scope / Capability Version Binding

D01 Lifecycle Policy
D02 Safety Gate Resolver
D03 Clinical Readiness Resolver
D04 Stopping Policy
D06 Slice-A Safe Exit Delivery Validator
D07 Failure Router
D08 Cancel / Expire Policy
D09 Clinical Risk Disposition Resolver
D10 Scope Adjudication Policy
```

U08 DDx、U09 DDx 后 Gap、U10 Workup、完整 U12 normal delivery 后续接入，不为 Slice A 提前强耦合。

---

# 26. Phase 7 全局不变量

```text
CAP-INV-01 Capability Result != Clinical Truth
CAP-INV-02 Capability 不拥有 Consultation / Clinical Risk / Safety Gate / Readiness / Delivery 等业务真值
CAP-INV-03 能由确定性 Policy 完成的最终裁决不得交给自由概率模型
CAP-INV-04 凡正式写入 Versioned Clinical State / CDP 的状态改变必须经过 G2/P01
CAP-INV-05 Safety-critical Capability failure 必须 fail-closed
CAP-INV-06 NO_RESULT != FAILURE
CAP-INV-07 UNKNOWN / UNMEASURED 不得由 Capability 改写为阴性/正常
CAP-INV-08 正式模型调用逐步统一经过 Model Runtime + Prompt Registry
CAP-INV-09 禁止恢复 Legacy LLM 作为迁移捷径
CAP-INV-10 正式医学方向必须来自批准 Scope / Knowledge / Rule / Evidence 边界
CAP-INV-11 Question Capability 不拥有 WAITING_USER
CAP-INV-12 DDx Capability 不拥有 COMPLETED
CAP-INV-13 Rendering Capability 不得创造新的疾病、风险、检查或治疗断言
CAP-INV-14 StateCommitter / Runtime / Trace 是共享平台职责，不由业务 Capability 私有化
CAP-INV-15 Capability Eval PASS != Unit PASS
CAP-INV-16 C02 Risk Capability Result != Clinical Risk Disposition；后者仅由 F4/D09 形成
CAP-INV-17 Scope semantic extraction != OUT_OF_SCOPE；最终 Scope 由 P06 + D10 裁决
CAP-INV-18 Delivery Validator Result != Delivery Readiness；Delivery Readiness 仅由 F7 解释
CAP-INV-19 Dependency Invalidation Rules 属于 G2 治理机制，不成为跨模块状态 Owner
```

---

# 27. 独立审查闭环

```text
P7-R01 C02 Risk Synthesis 与 F4 Owner 冲突
→ CLOSED：C02 仅提供风险证据/Proposed Assessment；新增 D09，由 F4 形成最终 Clinical Risk Disposition。

P7-R02 Scope Recognition 混合语义理解与最终范围裁决
→ CLOSED：C01 负责语义抽取；P06 + D10 负责最终 IN_SCOPE / OUT_OF_SCOPE / NEEDS_CLARIFICATION。

P7-R03 P01/G2 使用范围枚举过窄
→ CLOSED：改为原则性约束——任何正式 Versioned Clinical State / CDP 写入均必须经过 G2/P01。

P7-R04 Delivery Validator 可能侵入 Delivery Readiness ownership
→ CLOSED：D06 仅产生 Validation Result；Delivery Readiness 始终由 F7 解释。

P7-R05 Dependency Invalidation Engine 可能形成新的跨模块 Owner
→ CLOSED：D05 明确归属于 G2；模块提供失效需求，G2 计算、验证并提交失效状态。
```

---

# 28. Phase 7 完成状态

Phase 7 已回答：

> 对 U01–U15，每个 Unit 依赖哪些 Clinical Capability、确定性 Policy、State Governance 与 Runtime；现有资产如何处置；能力至少达到什么语义和质量标准后才能进入后续 Contract / Runtime / Implementation 设计。

当前形成：

- 6 个主要 Clinical Capability Family；
- 6 类共享 Platform Capability；
- 10 类确定性业务/治理 Policy；
- U01–U15 的逐单元能力映射；
- Slice A 最小能力集合；
- 统一 Failure 与质量门槛模型；
- Risk、Scope、Delivery、Invalidation 最终业务 Owner 边界已闭合。

```text
SOP Phase 7 — Capability Design
= FROZEN / V1
```

下一正式 SOP 阶段：

```text
Phase 8 — Contract & Data Design
```

Phase 8 才开始把当前已冻结的 Unit/Capability/Policy 边界落为正式输入输出契约、状态变更契约、数据语义与版本关系；不得反向改变 Phase 1–7 已冻结业务语义。