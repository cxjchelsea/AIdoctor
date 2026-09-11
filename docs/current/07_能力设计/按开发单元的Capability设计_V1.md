# AIdoctor V1 按开发单元的 Capability 设计

> 文档状态：DRAFT FOR FREEZE  
> 所属阶段：复杂业务软件开发 SOP — Phase 7 按开发单元识别、调用与建设 Capability  
> 上游权威：`01_需求/需求与系统边界_V1.md`、`02_功能/功能模块划分_V1.md`、`03_状态/系统级状态主干_V1.md`、`03_状态/模块级状态与状态所有权_V1.md`、`05_业务闭环/业务闭环设计_V1.md`、`06_开发单元/可验证开发单元拆分_V1.md`  
> 当前事实依据：`00_现状与治理/Current_State_Baseline_V1.md`  
> 历史参考：`docs/refactoring/capability-package规范.md` 仅作为资产与治理参考，不直接覆盖当前 Phase 1–6 已冻结结论。  
> 本文定义每个 U01–U15 为完成自身业务状态转换所依赖的 Capability、确定性 Policy、State Governance 与 Runtime 能力，并给出现有资产处置与最低质量门槛；不冻结 API、数据库表、具体 StatePatch Schema、Java/Python 类、具体模型供应商、Prompt 正文、RAG 参数或部署拓扑。

---

# 1. Phase 7 目标

Phase 6 已冻结 15 个可验证开发单元。Phase 7 回答：

> 每个 Unit 为完成自己的业务状态转换，究竟需要哪些能力？这些能力中哪些已经存在、哪些可以直接作为基础设施复用、哪些必须重构或替换、哪些确实需要新建？

Phase 7 的核心不是“一个 Unit 建一个 Agent”，而是：

```text
Unit
→ 识别 Business Decision
→ 判断确定性逻辑是否足够
→ 判断是否需要 Clinical Capability
→ 判断是否需要 Governance / Runtime Capability
→ 选择 REUSE / ADAPT / REFACTOR / REPLACE / NEW
→ 为能力定义质量门槛
```

---

# 2. Capability 的定义与边界

## 2.1 本文中的 Capability

Capability 是能够在明确输入边界内产生结构化、可治理结果的可复用能力，例如：

- 临床文本理解；
- 风险评估；
- Gap 识别与问题候选生成；
- DDx / Must-Exclude 分析；
- 线下证据需求评估；
- 患者侧解释生成。

Capability 的结果不是 Clinical Truth。正式临床状态仍必须由对应业务 Owner 解释，并由 G2 合法提交。

```text
Capability Result
!= Clinical State
```

## 2.2 不应伪装成 Capability 的内容

以下内容优先属于确定性业务 Policy / Governance / Runtime，而不是为了“AI 化”强行包装成模型能力：

- Consultation 生命周期合法转移；
- Safety Gate 的最终授权判断；
- Clinical Readiness 的唯一优先级求值；
- StateCommitter 的合法写入；
- 幂等与版本冲突判断；
- Duplicate / Expired Resume 判定；
- Failure 的业务路由；
- Cancel / Expire；
- 已冻结业务不变量校验。

原则：

```text
能由确定性规则可靠表达的业务约束
→ 不交给概率模型做最终裁决
```

## 2.3 Capability 不拥有业务状态

```text
Parsing Capability 不拥有 Clinical Facts
Risk Capability 不拥有 Safety Gate
Question Capability 不拥有 WAITING_USER
DDx Capability 不拥有 Consultation COMPLETED
Delivery Generator 不拥有 Clinical Truth
```

对应业务状态只能由 Phase 4 已冻结 Owner 决定。

---

# 3. Phase 7 处置分类

本文统一使用：

```text
REUSE_FOUNDATION
现有能力/基础设施本身值得保留，但可能尚未接入新主链。

ADAPT
主体能力可复用，需要增加适配层、结构化输出或治理接入。

REFACTOR
业务资产有价值，但 Failure、状态所有权、规则、Schema 或质量体系不足，必须重构后才能进入新链。

REPLACE
现有实现方式不能继续使用，需要迁移到已批准替代能力。

NEW
当前基线不存在满足 Phase 1–6 语义的正式能力，需要新建。

NOT_A_CAPABILITY
该职责本质是确定性业务 Policy / Governance，不建设独立 AI Capability。

DEFER
属于后续产品阶段，不进入 V1。
```

---

# 4. 共享能力与平台依赖总表

| ID | 能力/平台职责 | 当前资产 | Phase 7 处置 | 主要使用 Unit |
|---|---|---|---|---|
| P01 | Clinical State Governance / State Commit | `StateCommitter` + CDP versioning | REUSE_FOUNDATION + ADAPT | U02/U03/U05/U08/U10/U12/U13 |
| P02 | Clinical Workflow Durable Execution | Python Runtime checkpoint 基础 | REUSE_FOUNDATION + NEW clinical orchestration/resume layer | U06/U07/U14/U15 |
| P03 | Model Runtime / Prompt Registry | Enterprise Model Runtime 已存在 | REUSE_FOUNDATION + ADAPT | 所有需要模型的 Capability |
| P04 | Evidence / Knowledge Governance | KG + 历史 Evidence 资产 + G6 设计 | REFACTOR + ADAPT | U03/U08/U10/U11/U12 |
| P05 | Trace / Audit | Execution Trace 已实现但默认未开启 | REUSE_FOUNDATION + ADAPT | U01–U15 |
| P06 | Capability Scope / Version Binding | 历史 Capability Package 思路，当前临床主链未正式接管 | NEW current V1 binding layer | U01/U03/U05/U08/U10/U11/U12 |

平台依赖不等于业务 Capability。特别是 P01/P02/P05 不允许被某个临床服务私有化。

---

# 5. U01 — Consultation 建立与对象/问题框定

## 5.1 Unit 需要的能力

U01 需要：

```text
A. Consultation lifecycle creation policy
B. Subject Resolution
C. Problem Framing / intent clarification need detection
D. Clinical Scope Recognition
E. early safety-signal passthrough
```

## 5.2 能力分类

### A. Consultation lifecycle creation

```text
类型：NOT_A_CAPABILITY
处置：NEW deterministic business policy
```

负责创建 Consultation、校验终止态/新建语义，不交给模型。

### B. Subject Resolution / Problem Framing

当前 Clinical Parsing、Dialog NLU 与 Health State Assessment 中已有部分语义资产，但没有一个当前正式 Capability 完整拥有“问诊对象 + 当前问题 framing”。

```text
处置：ADAPT + REFACTOR
资产来源：Clinical Parsing / Dialog
```

能力输出只应是结构化候选，例如：主体候选、主诉候选、歧义点、是否需澄清，不直接改 Consultation。

### C. Clinical Scope Recognition

当前历史 Capability Package 有 ScopePolicy 设计，但临床主链未形成当前 V1 的正式版本绑定与范围判定层。

```text
处置：NEW
```

它必须基于已批准 Scope，而不是“模型知道就算支持”。

## 5.3 最低质量门槛

- 本人/代他人主体识别必须有专门测试集；
- 混合诉求不得掩盖临床不适；
- OUT_OF_SCOPE 必须可解释到当前 Scope Version；
- 低置信/歧义必须形成 clarification need，不允许静默猜测；
- 风险线索不得因 framing 不完整被丢弃。

---

# 6. U02 — 临床事实形成与版本提交

## 6.1 Unit 需要的能力

```text
Clinical Parsing / Normalization
Source Attribution
Ambiguity / Contradiction Detection
State Governance
Dependency Invalidation Proposal
```

## 6.2 当前资产与处置

Clinical Parsing 已有 Concept Recognition、Normalization、症状、疾病、药物、检查、过敏和歧义检测，属于最强的可复用业务资产之一；Signs 尚缺失，Schema governance 与 Eval 也不足。

```text
Clinical Parsing core = REFACTOR
Signs parsing = NEW
Clinical Parsing Eval = NEW
```

G2 StateCommitter 已具有授权、字段权限、来源验证、幂等、base version conflict、atomic commit 等机制：

```text
StateCommitter = REUSE_FOUNDATION + ADAPT
Clinical CDP adapter = NEW
```

## 6.3 Capability 输出边界

Parsing Capability 输出必须保留：

```text
fact candidate
value semantics
source
confidence / uncertainty
negation
normalization
ambiguity / contradiction
provenance
```

不得把：

```text
UNKNOWN → NO
UNMEASURED → NORMAL
MODEL_INFERRED → PATIENT_REPORTED
```

## 6.4 最低质量门槛

- versioned extraction EvalSet；
- 否定、时间、程度、主体、单位、药物/检查等关键子集评估；
- UNKNOWN/UNMEASURED 语义保真测试；
- 同一业务事件幂等提交测试；
- base-version conflict / concurrent write 测试；
- Capability success 但 StateCommitter reject 的场景必须被视为 Unit 未成功。

---

# 7. U03 — 当前版本风险评估

## 7.1 Unit 需要的能力

```text
Red Flag Detection
Risk Factor / Combination Rule Evaluation
Vital-sign Safety Evaluation
High-risk Disease / Must-not-miss Signals
Clinical Risk Synthesis
```

## 7.2 当前资产与处置

现有 Risk Engine 已包含症状组合、生命体征、高风险疾病、严重程度和分诊逻辑，但规则仍有原型性，并存在 `DDx empty → mild` 的危险语义。

```text
Risk Engine = REFACTOR
```

不得 AS-IS 复用。

Risk Capability 可以由规则、知识、模型或组合能力提供候选风险证据，但最终 `Clinical Risk Disposition` 仍由 F4 业务 Owner 形成。

## 7.3 必须新增的治理

```text
Safety-critical Risk Rule Pack = NEW / versioned
Risk EvalSet = NEW
Risk failure semantics = NEW explicit contract
```

关键安全规则不能只有概率模型单点决定。

## 7.4 最低质量门槛

- 红旗召回优先于普通准确率；
- Risk failure 100% 不得投射为 `NO_HIGH_RISK_SIGNAL`；
- `NO_DDX` 不得影响为低风险默认值；
- 每个 Risk 结果必须绑定 Clinical State Version、规则/知识/Capability Version；
- 特殊人群与关键生命体征场景需要独立 safety suite。

---

# 8. U04 — Safety Gate 决策

## 8.1 Unit 需要的能力

U04 的核心不是一个 AI Capability，而是确定性 Safety Policy：

```text
Clinical Risk
+ capability availability
+ scope / consent / authorization
+ mandatory safety rules
→ Safety Gate
```

## 8.2 处置

```text
Safety Gate Resolver = NEW + NOT_A_CAPABILITY
Enterprise safety/governance foundations = REUSE_FOUNDATION
```

最终 Gate：

```text
ALLOW / RESTRICTED / BLOCKED / UNAVAILABLE
```

必须由确定性 Policy 决定，模型只能提供输入证据。

## 8.3 最低质量门槛

- HIGH_RISK 永远不能得到普通 ALLOW；
- Risk/Safety Capability FAILED 必须导致普通流程 fail-closed；
- 权限/Consent/Scope 不满足时不能被 Agent 覆盖；
- Gate decision table 必须可穷举测试；
- 所有分支必须可重放并解释“为什么允许/阻断”。

---

# 9. U05 — Clinical Readiness 唯一求值

## 9.1 Unit 需要的能力

```text
Readiness input collection
Deterministic priority resolution
blocking-gap interpretation
stage applicability interpretation
```

## 9.2 处置

```text
Clinical Readiness Resolver = NEW + NOT_A_CAPABILITY
```

它不应使用自由 Agent 直接决定下一步。F1/F3/F5/F6 提供输入，Resolver 按 Phase 5 冻结优先级计算唯一状态，G2 提交。

## 9.3 最低质量门槛

- 同一状态版本同一输入必须得到同一 Readiness；
- `absent-by-design != failure != negative`；
- OUT_OF_SCOPE / NEEDS_OFFLINE_EVIDENCE / NEEDS_CLARIFICATION / CAN_ASK_MORE 等冲突必须有确定性判定；
- Safety Gate 非普通允许时 Resolver 不得绕过 Safety；
- 需要决策表与属性测试覆盖所有优先级组合。

---

# 10. U06 — 关键问题选择与进入 WAITING_USER

## 10.1 Unit 需要的能力

U06 有两类来源：

```text
F1 Clarification Requirement
F3 Information Gap
```

需要：

```text
Question Candidate Generation
Question Value / Priority Assessment
Duplicate / Already-answered Filtering
Question Rendering
Stopping Policy
```

## 10.2 当前资产与处置

Dialog 已包含 Gap Identification、Adaptive Questioning、NLU/NLG、Completeness、Redis/Memory，但其直接 CDP callback 和自有状态所有权必须移除。

```text
Dialog question/gap assets = REFACTOR
Dialog direct CDP write = REMOVE
Dialog private truth state = REPLACE BY governed state inputs
```

固定 completeness 百分比不能继续作为决定性推进机制。

## 10.3 能力拆分建议

```text
Question Planner Capability
→ 产生问题候选、目的、关联 Gap、预期决策价值

Question Renderer Capability
→ 将已选问题表达给患者
```

最终“选哪个问题”和“是否进入 WAITING_USER”仍属于业务单元规则，不由 NLG 模型拥有。

## 10.4 最低质量门槛

- question purpose 必须可追溯；
- duplicate question rate 需要专项 Eval；
- 已回答/invalidated gap 不得再问；
- USER_UNKNOWN / UNMEASURED 不得触发机械重复问；
- 没有决策价值的问题不得生成；
- 问题表达不得产生诊断暗示、治疗越界或诱导性假设。

---

# 11. U07 — 用户回答 Resume 与幂等恢复

## 11.1 Unit 需要的能力

U07 主要依赖 Runtime / Governance，而不是临床 AI：

```text
Pending Question identity
Resume event validation
Duplicate detection
Expiry validation
Checkpoint compatibility
Clinical State Version compatibility
Idempotent apply
```

## 11.2 当前资产与处置

Python Runtime 已有 deterministic single-tool runtime 与基础 checkpoint，但当前基线缺少完整临床 WAITING / Pending Event / Resume / Expired / Duplicate 业务恢复协议。

```text
Python Runtime = REUSE_FOUNDATION
Clinical Durable Resume Layer = NEW
```

## 11.3 最低质量门槛

- Duplicate event at most one clinical effect；
- expired answer 0 次 Clinical State 写入；
- wrong question/version token 0 次普通恢复；
- process restart / page refresh 后合法事件可恢复；
- stale checkpoint 不可提升为当前 Clinical Truth；
- Resume Protocol 必须支持 crash/replay 测试。

---

# 12. U08 — DDx / Must-Exclude 评估

## 12.1 Unit 需要的能力

```text
Candidate Retrieval / Generation within approved scope
Knowledge Graph Reasoning
Evidence Matching
Candidate Ranking / Tiering
Must-Exclude Identification
Supporting / Opposing / Unknown Evidence
DDx Assessment Synthesis
```

## 12.2 当前资产与处置

Diagnosis Engine 已有 Rule/KG/Statistical/LLM/Differential/Fusion/Three-layer/Evidence 等丰富资产；KG 也已经真实参与疾病路径检索。

但现有 Fusion 在 engine failure 时跳过继续，缺少 minimum evidence / safety dependency / must-have exclusion 门槛；Legacy LLM 又已 fail-closed，旧 Diagnosis Engine 仍依赖它。

因此：

```text
Diagnosis Engine domain assets = REFACTOR
KG retrieval/reasoning = REFACTOR + ADAPT
Legacy LLM path = REPLACE
Clinical model calls = REPLACE BY Model Runtime
Prompt ownership = REPLACE BY Prompt Registry
DDx Eval = NEW
```

## 12.3 关键能力边界

- Candidate 只能来自当前批准 Scope / Hypothesis space；
- 模型不得自由扩展正式疾病集合；
- KG path 不是自动等价于医学 Citation；
- `NO_RELIABLE_DIRECTION` 是合法结果；
- `FAILED` 必须与 `NO_RELIABLE_DIRECTION` 分离；
- Must-Exclude 未排除不得被隐藏。

## 12.4 最低质量门槛

- candidate recall/precision 分层评估；
- must-not-miss recall 单独设门槛；
- support/opposition evidence correctness；
- unsupported candidate rate；
- failure/no-result semantic tests；
- Knowledge/Model/Prompt/Capability version binding；
- 每个正式方向必须能追溯到批准证据基础。

---

# 13. U09 — DDx 后 Gap 重评与循环路由

## 13.1 Unit 需要的能力

```text
Gap Re-evaluation
Decision Impact Estimation
Question Value Estimation
Stale Gap Invalidation
Stopping Recommendation
```

## 13.2 当前资产与处置

Dialog 的 Gap Identification / Adaptive Questioning 可以复用，Diagnosis 的 Evidence Analyzer 可以作为输入资产，但目前缺少 Phase 5 已冻结的“决策价值驱动停止”正式语义。

```text
Gap analysis assets = REFACTOR
Stopping Policy = NEW deterministic policy
Question value capability = ADAPT/REFACTOR
```

## 13.3 关键边界

Capability 可以回答：

```text
还缺什么？
这个缺口可能影响什么？
哪些问题值得候选？
```

但是否继续循环最终由 U09 + U05 的确定性业务规则决定。

## 13.4 最低质量门槛

- candidate change 能正确触发相关 Gap；
- candidate invalidation 能取消旧 Gap；
- no-value gap 不会形成下一轮问题；
- stopping decision 有解释依据；
- 不使用固定 completeness 百分比替代决策价值。

---

# 14. U10 — 线下证据需求与检查建议

## 14.1 Unit 需要的能力

```text
Offline Evidence Need Assessment
Examination Direction Recommendation
Specific Examination Suggestion when formally supported
Evidence-gap-to-exam mapping
```

## 14.2 当前资产与处置

Workup Planner 已有真实检查推荐逻辑，但规则仍有原型内容；Examination Service 的检查记录/报告/OCR/历史可保留，而其临床检查规划逻辑应并入 Workup。

```text
Workup capability = REFACTOR
Examination planning logic = REMOVE / MERGE INTO WORKUP
Examination record/report/OCR = REUSE_FOUNDATION but Phase 2 product loop DEFER
Workup Eval = NEW
```

## 14.3 关键边界

```text
Suggestion != Medical Order
Assessment FAILED != NOT_NEEDED
```

V1 只回答“还需要什么线下证据以及为什么”，不建设跨天检查结果自动 Resume。

## 14.4 最低质量门槛

- 每项建议必须绑定 Gap/DDx/Must-Exclude/Risk rationale；
- 具体检查项目必须来自批准规则/知识；
- 禁止自由模型凭常识生成医嘱；
- unnecessary-test rate / unsupported-test rate 需要评估；
- failure 不能降级为“不需要检查”。

---

# 15. U11 — Safe Exit 结果组装与交付

## 15.1 Unit 需要的能力

```text
Safe Exit Package Assembly
Reason-specific content selection
Patient-safe Rendering
Delivery Validation
```

原因包括 OUT_OF_SCOPE、NO_RELIABLE_DIRECTION、NEEDS_OFFLINE_EVIDENCE、HIGH_RISK/Safety blocked、Safety unavailable、可安全收束的 Failure。

## 15.2 当前资产与处置

Explanation Service 已有 deterministic Conclusion Builder / Evidence Chain Builder / Reasoning Path；其自然语言 LLM 仍依赖 Legacy LLM。

```text
Deterministic explanation builders = ADAPT / REUSE
Legacy LLM explanation = REPLACE
Patient rendering model call = Model Runtime + Prompt Registry
Delivery Validator = NEW / REFACTOR as deterministic gate
```

## 15.3 关键边界

- Safe Exit 的结构化核心应先确定，再进行患者语言渲染；
- 渲染模型不能新增疾病、风险、检查或行动；
- OUT_OF_SCOPE 不得自由补全疾病方向；
- Safety unavailable 不得渲染为“低风险”；
- Delivery failure 不得直接把 Consultation 写成 SAFE_EXIT。

## 15.4 最低质量门槛

- semantic faithfulness：渲染与结构化 package 一致；
- no invented clinical claims；
- high-risk action visibility；
- uncertainty preservation；
- product boundary / prohibited content tests；
- version-consistency tests。

---

# 16. U12 — 正常结果组装与 COMPLETED

## 16.1 Unit 需要的能力

```text
Clinical Summary Assembly
DDx / Must-Exclude Explanation
Evidence Presentation
Uncertainty Presentation
Patient-safe Rendering
Delivery Validation
```

## 16.2 处置

复用 U11 的 Delivery/Explanation 能力族，但使用不同的 normal-completion policy/template，而不是复制一个第二套解释系统。

```text
Structured Delivery Assembly = ADAPT / NEW orchestration
Deterministic Evidence/Conclusion builders = REUSE/ADAPT
Natural-language rendering = REPLACE legacy LLM with Model Runtime
Delivery Validator = shared NEW deterministic gate
```

## 16.3 最低质量门槛

- 结果只引用当前有效 Clinical State Version；
- 未解决 blocking Must-Exclude 不得通过；
- UNKNOWN/UNMEASURED 必须保留；
- disease direction 不得表达为确诊；
- 自动处方/治疗计划禁止；
- rendering faithfulness 与 citation/evidence consistency 必须评估；
- Delivery validation failure 不能进入 COMPLETED。

---

# 17. U13 — 用户更正事件验证与失效触发

## 17.1 Unit 需要的能力

U13 主要是确定性 Event / State Governance：

```text
Correction Event Validation
Target Fact Resolution
Expiry / applicability validation
Dependency Invalidation Rules
```

## 17.2 处置

```text
Correction Event Policy = NEW + NOT_A_CAPABILITY
Dependency Invalidation Engine/Rules = NEW deterministic governance
StateCommitter = REUSE_FOUNDATION + ADAPT
```

F2 Parsing Capability 可在更正被接受后重新解释新的事实内容，但不负责判断能否修改正式历史。

## 17.3 最低质量门槛

- rejected correction = 0 Clinical State effect；
- accepted correction 只产生一次新版本；
- old Delivery/history 不被静默覆盖；
- 受影响 Risk/DDx/Gap/Workup/Delivery 必须可 stale/invalidate；
- 未受影响结果不要求无差别全量清空；
- correction 后 Safety 获得重新评估机会。

---

# 18. U14 — Capability / Runtime Failure 路由

## 18.1 Unit 需要的能力

U14 是确定性 Failure Policy + Runtime control，而不是一个 LLM Agent：

```text
Failure Classification
Retry/Repair Eligibility
Approved Fallback Eligibility
Safety Impact Evaluation
Safe Exit Feasibility
Terminal Failure Decision
```

## 18.2 处置

```text
Business Failure Router = NEW + NOT_A_CAPABILITY
Runtime retry/repair primitives = REUSE_FOUNDATION + ADAPT
Fallback Registry/Policy = NEW governance
```

具体 retry 次数、timeout、alternate model 等仍属于后续 Runtime / Contract 设计。

## 18.3 最低质量门槛

- 每类 Failure 都有显式分类；
- failure 不得变为 empty normal object；
- safety-sensitive failure 先形成 Gate 后果；
- retry/fallback 必须幂等且不能绕过版本验证；
- Safe Exit 可形成时不机械升级 FAILED_TERMINAL；
- 无最低安全交付能力时才允许业务终止失败。

---

# 19. U15 — Consultation 取消与过期终止

## 19.1 Unit 需要的能力

```text
Lifecycle Transition Policy
Cancellation Validation
Waiting Expiry Policy
Late Event Rejection
Runtime Cancellation / Suspension
```

## 19.2 处置

```text
Lifecycle termination policy = NEW + NOT_A_CAPABILITY
Runtime cancellation primitives = REUSE_FOUNDATION + ADAPT
```

不需要新 AI Capability。

## 19.3 最低质量门槛

- CANCELLED/EXPIRED 后普通 Resume 100% 拒绝；
- late async result 不得写临床状态；
- Expiry 只在合法 WAITING_USER 场景生效；
- cancel/expire 不修改历史 Clinical Truth；
- 继续咨询默认走新 Consultation。

---

# 20. U01–U15 Capability 处置矩阵

| Unit | 核心 Clinical Capability | Governance / Runtime | 当前处置摘要 |
|---|---|---|---|
| U01 | Subject/Problem Framing、Scope Recognition | lifecycle policy | Parsing/Dialog ADAPT/REFACTOR；Scope NEW |
| U02 | Clinical Parsing/Normalization | StateCommitter | Parsing REFACTOR；State adapter NEW |
| U03 | Risk / Red Flag | safety rule governance | Risk REFACTOR；Safety Rule Pack/Eval NEW |
| U04 | 无独立 AI Capability | Safety Gate Resolver | NEW deterministic |
| U05 | 无独立 AI Capability | Readiness Resolver | NEW deterministic |
| U06 | Gap/Question Planner、Renderer | stopping/wait transition | Dialog REFACTOR |
| U07 | 无临床 AI | Durable Resume/Idempotency | Runtime foundation REUSE + clinical resume NEW |
| U08 | DDx/KG/Evidence/Must-Exclude | Model/Prompt/Knowledge governance | Diagnosis/KG REFACTOR；Legacy LLM REPLACE |
| U09 | Gap Re-evaluation/Question Value | Stopping Policy | Dialog assets REFACTOR；stopping NEW |
| U10 | Offline Evidence/Workup | knowledge/rule governance | Workup REFACTOR；duplicate planning REMOVE |
| U11 | Patient-safe Explanation | Delivery Validator | deterministic builders ADAPT；LLM REPLACE；validator NEW |
| U12 | Clinical Summary/Explanation | Delivery Validator | 与 U11 共享能力族 |
| U13 | Parsing（更正后） | Correction/Invalidation/StateCommit | governance NEW + StateCommitter ADAPT |
| U14 | 无临床 AI | Failure Router/Fallback/Runtime | NEW deterministic governance |
| U15 | 无临床 AI | lifecycle/expiry/cancel | NEW deterministic + Runtime ADAPT |

---

# 21. 需要真正建设/重构的 Capability Family

从 15 个 Unit 反推，V1 并不需要 15 个独立智能体，而更适合形成以下能力族。

## C01 Clinical Understanding

覆盖：U01/U02/U13。

包括：

```text
Subject/Problem Framing
Clinical Parsing
Normalization
Ambiguity/Contradiction
Source Attribution
```

主体资产：Clinical Parsing + Dialog NLU，REFACTOR。

## C02 Clinical Safety Intelligence

覆盖：U03，向 U04 提供输入。

包括：Red Flag、Risk Rules、Risk Evidence、Risk Synthesis。

主体资产：Risk Engine，REFACTOR；关键 rule pack 与 Eval NEW。

## C03 Question & Gap Intelligence

覆盖：U06/U09。

包括：Gap、Question Candidate、Decision Value、Question Rendering。

主体资产：Dialog，REFACTOR；Stopping Policy 不属于该概率能力的最终权限。

## C04 Differential & Evidence Intelligence

覆盖：U08/U09。

包括：candidate、KG reasoning、evidence relation、ranking、Must-Exclude。

主体资产：Diagnosis Engine + KG，REFACTOR；Legacy LLM path REPLACE。

## C05 Offline Evidence / Workup Intelligence

覆盖：U10。

主体资产：Workup Planner，REFACTOR；Examination 中重复规划逻辑移除。

## C06 Clinical Delivery & Explanation

覆盖：U11/U12。

结构化 builder 优先 deterministic；语言渲染可经 Model Runtime，但不得产生新 Clinical Truth。

主体资产：Explanation，ADAPT/REFACTOR；Legacy LLM REPLACE。

---

# 22. 必须作为平台/确定性 Policy 建设的能力

以下不建设成自由 Agent：

```text
P01 State Governance / Clinical CDP Adapter
P02 Durable Clinical Resume
P03 Model Runtime / Prompt Registry clinical adapter
P04 Evidence / Knowledge Governance
P05 Trace / Audit integration
P06 Scope / Capability Version Binding
D01 Consultation Lifecycle Policy
D02 Safety Gate Resolver
D03 Clinical Readiness Resolver
D04 Question Stopping Policy
D05 Correction / Dependency Invalidation Policy
D06 Delivery Validator
D07 Failure Router / Fallback Policy
D08 Cancel / Expire Policy
```

核心原则：

```text
Clinical Intelligence proposes
Deterministic policy decides legal transition
State Governance commits
Runtime executes/resumes
Trace observes
```

---

# 23. Model / Prompt / KG 的统一处置原则

## 23.1 Legacy LLM

```text
Legacy common LLM runtime = REMOVE after migration
旧 clinical call sites = REPLACE
```

禁止任何 Unit 为了快速恢复旧功能而重新启用已 fail-closed 的 Legacy LLM。

## 23.2 Model Runtime

所有正式临床模型调用必须逐步迁移为：

```text
Unit
→ approved Capability
→ Model Runtime
→ versioned Prompt Registry
→ approved model route
→ structured Capability Result
→ business interpretation
→ G2 commit if state change is legal
```

## 23.3 Knowledge Graph / RAG / Evidence

KG、RAG、规则、文献不是彼此互斥的“架构选型”，而是不同 evidence/capability source。

必须区分：

```text
patient evidence
clinical rule evidence
KG reasoning path
medical knowledge citation
model inference
```

模型不能把任一来源伪装成另一来源。

---

# 24. Capability Failure 统一语义

所有概率性或外部依赖 Capability 至少必须能够表达：

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

要求：

```text
NO_RESULT != DEPENDENCY_FAILURE
INSUFFICIENT_INFORMATION != INVALID_OUTPUT
UNSUPPORTED != TIMEOUT
SAFETY_BLOCKED != SUCCESS
```

业务 Unit 不允许通过 `null / [] / {}` 猜测是哪一种语义。

---

# 25. Capability 质量门槛模型

一个 Capability 进入正式 Unit 前，至少需要：

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

不同 Capability 的指标不能机械统一。例如：

- Risk 更重视 red-flag recall 与 fail-closed；
- Parsing 更重视 fact/value/source semantic correctness；
- Question 更重视 decision value、重复率、负担与安全；
- DDx 更重视 supported candidate、must-not-miss 与 evidence correctness；
- Workup 更重视必要性、依据与避免不支持检查；
- Delivery 更重视 faithfulness、uncertainty preservation 与禁止新增临床断言。

Phase 7 冻结这些质量维度，不冻结具体数值阈值；具体 EvalSet、指标值和 PASS threshold 在后续验证/评估阶段形成并经过临床治理。

---

# 26. Slice A 的 Capability 最小集合

Phase 6 已冻结首个候选纵向切片：U01–U07，并要求 U11/U14/U15 提供可达的横向闭合能力。

因此 Slice A 所需最小能力不是全部 C01–C06，而是：

```text
C01 Clinical Understanding
  - Subject/Problem Framing
  - Clinical Parsing

C02 Clinical Safety Intelligence
  - Red Flag / Risk

C03 Question & Gap Intelligence
  - Clarification / Question planning

P01 State Governance / CDP adapter
P02 Durable Clinical Resume
P03 Model Runtime clinical adapter（仅当上述 Capability 使用模型）
P05 Trace / Audit integration
P06 Scope / Capability Version Binding
D01 Lifecycle Policy
D02 Safety Gate Resolver
D03 Clinical Readiness Resolver
D04 Stopping Policy
D06 Slice-A Safe Exit Delivery Validator
D07 Failure Router
D08 Cancel / Expire Policy
```

U08 DDx、U09 DDx 后 Gap、U10 Workup、完整正常 U12 Delivery 可以后续接入，不应为了做 Slice A 被提前强耦合。

---

# 27. Phase 7 不设计的内容

本文不冻结：

- Capability API URL；
- protobuf/JSON 具体字段；
- DB 表；
- StatePatch Schema；
- Java/Python 包名与类名；
- LangGraph / LangChain / AutoGen 等框架选择；
- 具体模型供应商与模型名；
- Prompt 正文；
- embedding/vector DB 参数；
- KG 查询实现细节；
- retry/timeout 数值；
- Eval threshold 数值；
- 部署拓扑；
- 跨天 Examination Resume；
- Treatment/Medication/Wellness Capability。

这些属于 Phase 8+ 的 Contract/Data、Runtime/Architecture、Verification/Eval 或未来产品范围。

---

# 28. Phase 7 全局不变量

```text
CAP-INV-01 Capability Result != Clinical Truth
CAP-INV-02 Capability 不拥有 Consultation / Risk / Safety Gate / Readiness / Delivery 等业务真值
CAP-INV-03 能由确定性 Policy 完成的最终授权判断不得交给自由概率模型
CAP-INV-04 所有正式 Clinical State 写入必须经过 G2
CAP-INV-05 Safety-critical capability failure 必须 fail-closed
CAP-INV-06 NO_RESULT != FAILURE
CAP-INV-07 UNKNOWN / UNMEASURED 不得由 Capability 改写为阴性/正常
CAP-INV-08 正式模型调用逐步统一经过 Model Runtime + Prompt Registry
CAP-INV-09 禁止恢复 Legacy LLM 作为迁移捷径
CAP-INV-10 正式医学方向必须来自批准 Scope / Knowledge / Rule / Evidence 边界
CAP-INV-11 Question capability 不拥有 WAITING_USER
CAP-INV-12 DDx capability 不拥有 COMPLETED
CAP-INV-13 Rendering capability 不得创造新的疾病、风险、检查或治疗断言
CAP-INV-14 StateCommitter / Runtime / Trace 是共享平台职责，不由业务 Capability 私有化
CAP-INV-15 Capability 独立 Eval PASS != Unit PASS；必须继续验证业务状态接入
```

---

# 29. Phase 7 完成标准

Phase 7 应能够回答：

> 对 U01–U15，每个 Unit 依赖哪些 Clinical Capability、确定性 Policy、State Governance 和 Runtime 能力；现有资产如何处置；能力至少达到什么语义与质量标准后，才值得进入后续 Contract / Runtime / Implementation 设计？

当前形成：

- 6 个主要 Clinical Capability Family；
- 6 类共享 Platform Capability；
- 8 类确定性业务 Policy；
- U01–U15 的逐单元能力映射；
- Slice A 的最小 Capability 集合；
- 统一 Failure 与质量门槛模型。

当前状态：

```text
SOP Phase 7 — Capability Design
= DRAFT COMPLETE / NOT FROZEN
```

冻结前应独立审查：

1. 是否把本应是业务 Policy 的职责错误做成 AI Capability；
2. 是否遗漏 U01–U15 任一 Unit 的必要能力；
3. 是否存在两个 Capability 同时拥有同一业务真值；
4. REUSE / REFACTOR / REPLACE / NEW 是否与 Current State Baseline 一致；
5. Clinical Parsing / Risk / Dialog / Diagnosis / KG / Workup / Explanation 的处置是否合理；
6. StateCommitter / Runtime / Model Runtime / Trace 的平台边界是否清楚；
7. Legacy LLM 是否仍存在任何隐式恢复路径；
8. Capability Failure 语义是否足以阻断 Failure→Normal；
9. Slice A 所需能力是否完整但没有把 U08–U12 全部提前强耦合；
10. 是否错误提前冻结 Phase 8+ 的 API、Schema、模型或部署实现。

通过独立审查后，Phase 7 才可标记 `FROZEN / V1`。
