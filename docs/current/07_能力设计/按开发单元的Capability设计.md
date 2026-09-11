# AIdoctor V1 能力、平台与确定性规则设计（补齐重组版）

> 文档性质：Phase 7 能力设计重组稿  
> 目标：不改变既有 Phase 7 冻结业务语义，只重组表达方式，降低施工时的查找和理解成本。  
> 上游权威：Phase 1–6 已冻结文档。  
> 下游约束：Phase 8 Contract、Phase 9 Runtime 不得反向改变本文件冻结的业务语义。  
> 说明：本文件不记录具体实施进度。`CODE EXISTS / IMPLEMENTED / WIRED / VERIFIED / MERGED` 等状态应由实施与验证记录维护，而不是写入能力设计。

---

# 1. 本文件回答什么

Phase 7 只回答四件事：

1. 系统需要哪些能力？
2. 每项能力负责什么、不负责什么？
3. 哪些 Unit 会消费这些能力？
4. 这些能力应在什么时候建设，以及最低达到什么质量门槛？

不在本阶段冻结：

- API 路径；
- 数据库表；
- Java / Python 类名；
- 具体 StatePatch Schema；
- 模型供应商；
- Prompt 正文；
- RAG 参数；
- 部署拓扑；
- 当前代码是否已经实现。

核心推导链：

```text
Business Unit
↓
识别 Business Decision
↓
优先判断是否可由确定性 Policy 完成
↓
需要概率性/检索/模型能力时定义 Clinical Capability
↓
识别 Platform / Governance / Runtime 依赖
↓
确定 REUSE / ADAPT / REFACTOR / REPLACE / NEW
↓
冻结能力边界与最低质量门槛
```

---

# 2. 统一编号与角色

```text
Uxx = Business Unit
Cxx = Clinical Capability
Pxx = Platform Capability
Dxx = Deterministic Policy
Kxx = Contract Family（Phase 8）
BL-xx = Business Loop
```

| 类型 | 回答的问题 | 是否拥有业务真值 |
|---|---|---|
| U — Business Unit | 一次业务状态转换如何完成 | Unit 组织业务过程，但不绕过 Owner |
| C — Clinical Capability | 能生成哪些候选、证据、建议、结构化结果 | 否 |
| P — Platform Capability | 状态、执行、版本、Trace 等基础能力如何提供 | 否 |
| D — Deterministic Policy | 最终业务规则如何确定性裁决 | 在所属 Business Owner / Governance 边界内参与正式裁决 |
| K — Contract | 模块间交换什么数据、语义是什么 | 否 |

## 2.1 中文术语约定

本文件优先使用中文表达，编号保留英文首字母仅用于稳定引用：

```text
C = 临床能力
P = 平台能力
D = 确定性规则
U = 业务开发单元
K = 契约族
BL = 业务闭环
```

后续施工、审查和提交记录应优先写成：

```text
C01 临床理解能力
P04 医学知识与证据治理
D10 范围裁决规则
```

统一原则：

```text
Capability Result != Clinical Truth
Decision != Proposal
Proposal != Commit
Runtime State != Clinical State
Trace != Clinical State
```

---

# 3. 三类能力的建设节奏

统一使用以下建设时机：

```text
FOUNDATION_PREREQUISITE
= 第一个业务 Unit 开工前必须具备最小版本

FIRST_CONSUMER_UNIT
= 第一个真实消费者 Unit 开始时建设

INCREMENTAL_EXTENSION
= 后续 Unit 出现新需求时增量扩展

DEFERRED
= 当前 Slice 尚不需要，不提前建设
```

## 3.1 建设原则

```text
第一个 Unit
↓
展开 C / P / D 依赖
↓
判断已有能力是否满足 Contract + Quality
├─ 满足 → REUSE / ADAPT
└─ 不满足 → 按当前 Unit 所需最小范围建设
```

禁止：

```text
因为 P01-P06 已经被定义
→ 所以先把 P01-P06 全部做完
```

也禁止：

```text
因为 C01 会被多个 Unit 使用
→ 所以第一次建设时一次性实现所有未来场景
```

正确做法：

```text
首个消费者需要什么
→ 做到什么
↓
后续 Unit 再增量扩展
```

---

# 4. 处置分类

| 分类 | 含义 |
|---|---|
| REUSE_FOUNDATION | 现有基础设施值得保留，但可能尚未进入临床主链 |
| ADAPT | 主体能力可复用，需要适配层、结构化输出或治理接入 |
| REFACTOR | 业务资产有价值，但 Failure、状态所有权、Schema 或质量体系不足 |
| REPLACE | 现有实现方式不能继续作为正式路径，需要迁移 |
| NEW | 当前基线不存在满足 V1 语义的正式能力 |
| NOT_A_CAPABILITY | 本质是确定性 Policy / Governance，不建设独立 AI Capability |
| DEFER | 当前 V1 / 当前 Slice 不建设 |

---

# 5. 全局控制边界

## 5.1 Capability 只产生候选、证据与建议

例如：

- 临床文本理解；
- 红旗 / 风险证据识别；
- Gap 与问题候选；
- DDx / Must-Exclude 候选；
- 线下证据需求；
- 患者侧语言渲染。

Capability 不直接拥有正式业务状态。

## 5.2 下列职责优先属于确定性 Policy / Governance / Runtime

- Consultation 生命周期合法转移；
- Scope 最终裁决；
- Clinical Risk Disposition 最终裁决；
- Safety Gate 最终授权；
- Clinical Readiness 唯一求值；
- State Commit 合法性；
- Duplicate / Expired / Resume 合法性；
- Question stopping；
- Correction / Invalidation；
- Delivery Validation；
- Failure Routing；
- Cancel / Expire；
- 已冻结业务不变量校验。

## 5.3 单一 Owner 约束

```text
Parsing Capability 不拥有 Clinical Facts
Risk Capability 不拥有 Clinical Risk Disposition
Risk Capability 不拥有 Safety Gate
Scope semantic extraction 不拥有 OUT_OF_SCOPE
Question Capability 不拥有 WAITING_USER
DDx Capability 不拥有 COMPLETED
交付校验规则 不拥有 Delivery Readiness
Rendering Capability 不拥有 Clinical Truth
Dependency Invalidation Engine 不成为跨模块状态 Owner
```

核心控制链：

```text
Clinical Intelligence proposes
↓
Business Owner / Deterministic Policy interprets
↓
State Governance commits
↓
Runtime executes / resumes
↓
Trace observes
```

---

# 6. 临床能力目录

## C01 — 临床理解能力

### 目标
负责用户输入到结构化临床语义候选的理解。

### 核心能力
- Subject / Problem semantic extraction；
- Clinical Parsing；
- Normalization；
- Source Attribution；
- Ambiguity / Contradiction Detection；
- negation / temporality / severity / unit 等语义；
- Scope-related semantic extraction。

### 消费 Unit
`U01 / U02 / U13`

### 不负责
```text
不拥有 Clinical Facts 最终真值
不拥有 Scope 最终裁决
不决定 Correction 是否可修改历史
```

### 现有资产与处置
```text
Clinical Parsing + Dialog NLU + Health State Assessment
→ ADAPT + REFACTOR

Signs parsing = NEW
Clinical Parsing Eval = NEW
```

### 建设时机
```text
首次消费者：U01
U01：Subject / Problem / Scope semantic extraction 最小版本
U02：扩展 Clinical Fact parsing / normalization / provenance
U13：扩展 correction 后重新解释
```

### 最低门槛
- 本人 / 代他人识别专项测试；
- 混合诉求不掩盖临床不适；
- 歧义不得静默猜测；
- 否定、时间、程度、主体、单位、药物、检查有专项评估；
- `UNKNOWN != NO`；
- `UNMEASURED != NORMAL`；
- `MODEL_INFERRED != PATIENT_REPORTED`；
- 输出保留 source / uncertainty / provenance。

---

## C02 — 临床安全识别能力

### 目标
生成风险相关证据与候选评估。

### 核心能力
- Red Flag Evidence；
- Risk Factor / Combination Rule Hits；
- Vital-sign Safety Signals；
- Must-not-miss Signals；
- Proposed Risk Assessment。

### 消费 Unit
`U03`，并向 `U04` 提供输入。

### 不负责
```text
不拥有 Clinical Risk Disposition
不拥有 Safety Gate
```

### 正式链路
```text
C02 structured risk evidence
+
versioned Safety-critical Risk Rule Pack
↓
D09 临床风险等级裁决规则 / F4
↓
NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK
```

### 现有资产与处置
```text
Risk Engine = REFACTOR
Safety-critical Risk Rule Pack = NEW / versioned
Risk EvalSet = NEW
Risk failure semantics = explicit contract
```

### 建设时机
`FIRST_CONSUMER_UNIT = U03`

### 最低门槛
- red-flag recall 优先；
- Risk failure 不得映射为 `NO_HIGH_RISK_SIGNAL`；
- `NO_DDX != LOW_RISK`；
- Risk 结果绑定 Clinical State Version + Rule / Knowledge / Capability Version；
- 特殊人群、生命体征、must-not-miss 有独立 safety suite。

---

## C03 — 问题与信息缺口能力

### 目标
识别信息缺口并生成有决策价值的问题候选。

### 核心能力
- Gap Detection；
- Question Candidate Generation；
- Question Value / Priority Assessment；
- Duplicate / Already-answered Filtering；
- Question Rendering；
- DDx 后 Gap Re-evaluation；
- Decision Impact Estimation。

### 消费 Unit
`U06 / U09`

### 不负责
```text
不拥有 WAITING_USER
不决定最终是否继续循环
```

### 现有资产与处置
```text
Dialog question/gap assets = REFACTOR
Question Planner = ADAPT / REFACTOR
Question Renderer = ADAPT / REFACTOR
Dialog direct CDP write = REMOVE
Dialog private truth state = REPLACE BY governed state inputs
```

### 建设时机
```text
FIRST_CONSUMER_UNIT = U06
U09 时扩展 DDx 后 Gap 重评和 Question Value
```

### 最低门槛
- question purpose 可追溯；
- duplicate question rate 有 Eval；
- 已回答 / invalidated Gap 不重复问；
- USER_UNKNOWN / UNMEASURED 不机械重复问；
- 没有决策价值时停止；
- 问题表达不得产生诊断暗示或治疗越界。

---

## C04 — 鉴别方向与证据能力

### 目标
形成 DDx / Must-Exclude 候选与证据关系。

### 核心能力
- Candidate Retrieval / Generation；
- KG Reasoning；
- Evidence Matching；
- Candidate Ranking / Tiering；
- Must-Exclude Identification；
- Supporting / Opposing / Unknown Evidence；
- DDx Assessment Proposal。

### 消费 Unit
`U08 / U09`

### 不负责
```text
不拥有 COMPLETED
不把 KG path 自动等同 Citation
不把 NO_RELIABLE_DIRECTION 当成 FAILED
```

### 现有资产与处置
```text
Diagnosis Engine = REFACTOR
KG retrieval/reasoning = REFACTOR + ADAPT
Legacy LLM path = REPLACE
DDx Eval = NEW
```

### 建设时机
```text
FIRST_CONSUMER_UNIT = U08
U09 增量扩展 Gap / decision impact
```

### 最低门槛
- candidate 必须在批准 Scope / Hypothesis Space 内；
- must-not-miss recall；
- evidence relation 正确；
- unsupported candidate rate 可评估；
- failure / no-result 语义清晰；
- Must-Exclude 未排除不得隐藏；
- 所有正式结果必须版本绑定。

---

## C05 — 线下证据与检查建议能力

### 目标
判断是否需要线下证据以及哪些检查方向有正式依据。

### 核心能力
- Offline Evidence Need Assessment；
- Examination Direction Recommendation；
- Specific Examination Suggestion（有正式依据时）；
- Evidence-gap-to-exam mapping。

### 消费 Unit
`U10`

### 现有资产与处置
```text
Workup capability = REFACTOR
Examination planning duplicate logic = REMOVE / MERGE INTO WORKUP
Examination record/report/OCR = REUSE_FOUNDATION but DEFER for V1 loop
Workup Eval = NEW
```

### 建设时机
`FIRST_CONSUMER_UNIT = U10`

### 最低门槛
```text
Suggestion != Medical Order
Assessment FAILED != NOT_NEEDED
```

具体检查必须来自批准规则 / 知识。V1 不建设跨天检查结果自动 Resume。

---

## C06 — 临床交付与解释能力

### 目标
把已形成的结构化业务结果安全地组装和渲染给患者。

### 核心能力
- Safe Exit Package Assembly；
- Structured Delivery Assembly；
- Reason-specific Content Selection；
- Patient-safe Rendering；
- Clinical Summary / Explanation。

### 消费 Unit
`U11 / U12`

### 不负责
```text
不拥有 Delivery Readiness
不得创造新的疾病、风险、检查或治疗断言
```

### 现有资产与处置
```text
Deterministic Explanation Builders = REUSE / ADAPT
Legacy LLM Explanation = REPLACE
Natural-language Rendering = Model Runtime + Prompt Registry
```

### 建设时机
```text
FIRST_CONSUMER_UNIT = U11
U12 复用同一能力族
```

### 最低门槛
- 只引用当前有效版本；
- blocking Must-Exclude 未解决不得正常完成；
- UNKNOWN / UNMEASURED 保留；
- 疾病方向不得表达为确诊；
- 禁止自动处方 / 治疗；
- rendering 不产生新 Clinical Truth；
- Delivery validation failure 不得 COMPLETED。

---

# 7. 平台能力目录

## P01 — 临床状态治理与提交

### 职责
任何正式写入 Versioned Clinical State / CDP 的状态改变都必须经过 P01 / G2。

### 当前资产与处置
```text
StateCommitter + CDP versioning
= REUSE_FOUNDATION + ADAPT

Clinical CDP Adapter
= NEW
```

### 建设时机
```text
FOUNDATION_PREREQUISITE
+
INCREMENTAL_EXTENSION
```

首个 Unit 前必须有最小正式 Commit 路径；后续随新的 Clinical State 类型扩展。

### 不负责
- Consultation lifecycle 真值；
- Runtime Thread / Run / Checkpoint 真值。

---

## P02 — 临床流程持久执行

### 职责
- Clinical orchestration；
- checkpoint；
- durable resume；
- retry / repair；
- crash / replay；
- runtime cancellation / suspension。

### 当前资产与处置
```text
Python Runtime checkpoint foundation = REUSE_FOUNDATION
Clinical orchestration / resume layer = NEW
```

### 建设时机
```text
最小 Run / Thread 基础可能属于 Foundation
完整 Durable Resume FIRST_CONSUMER_UNIT = U07
U14 / U15 增量扩展 failure / cancel / expire
```

### 不负责
Runtime Checkpoint 不得成为 Clinical Truth。

---

## P03 — 模型运行时与提示词注册

### 职责
正式模型调用的统一受治理入口。

### 当前资产与处置
```text
Enterprise Model Runtime = REUSE_FOUNDATION + ADAPT
Prompt Registry = REUSE_FOUNDATION + ADAPT
Legacy common LLM runtime = REMOVE after migration
```

### 建设时机
第一个真实需要模型调用的 Capability 成为消费者时建设 / 适配。

正式调用链：

```text
Unit
→ approved Capability
→ Model Runtime
→ versioned Prompt Registry
→ approved model route
→ structured Capability Result
→ Business Owner / Policy
→ G2 commit if needed
```

---

## P04 — 医学知识与证据治理

### 职责

P04 负责治理所有可能影响正式临床判断的医学知识、规则、知识图谱路径、检索证据和引用来源。

它不是单纯的“知识库访问工具”，而是统一的：

```text
知识发布
版本管理
生效控制
来源追溯
冲突处理
回滚
线上读取治理
```

### 管理对象

至少包括：

```text
医学知识条目
临床规则
红旗规则
特殊人群规则
知识图谱路径
检索语料
循证来源
知识版本
规则版本
知识发布包
证据来源与出处
```

必须区分：

```text
患者事实证据
临床规则证据
知识图谱推理路径
医学知识引用
模型推断
```

这些对象不得统一压缩成一个含糊的“证据”。

### 知识生命周期

P04 至少应支持：

```text
草稿 / 候选
↓
验证中
↓
待发布
↓
已发布
↓
已废弃
↓
已退役
```

必须保持：

```text
候选 != 已发布
已发布 != 永久有效
已废弃 != 已删除
回滚 != 新知识生成
```

### 知识区域与写权限

吸收历史设计中的三层治理思想：

```text
实验区
→ 抽取、构建、试验，不影响线上临床判断

候选验证区
→ 冲突检查、回归评估、影子评估、构建发布候选

生产发布区
→ 只允许正式发布版本被线上读取
```

生产知识默认线上只读。

任何 Agent、模型、知识抽取服务或临床能力都不得直接修改生产知识。

### 发布门禁

知识或规则进入生产使用前，至少经过：

```text
1. 结构合法性
2. 来源与出处完整
3. 冲突检查
4. 版本兼容检查
5. 回归评估
6. 安全关键场景评估
7. 发布审批
8. 回滚信息完整
```

安全关键知识与规则不得仅依赖模型自动判断完成发布。

### 知识发布包

正式知识发布包至少应绑定：

```text
knowledge_release_id
version
effective_from
effective_until
source_refs
rule_refs
scope
population
region
language
evaluation_report
rollback_target
status
```

正式临床能力使用知识时，至少应可追踪：

```text
使用了哪个知识发布包
使用了哪个规则版本
使用了哪些来源
当前是否仍处于有效期
```

### 冲突、失效与替代

P04 必须支持：

```text
新旧知识冲突
来源冲突
地区差异
人群差异
规则替代
来源撤回
版本过期
知识废弃
```

不得静默覆盖。

应形成可追踪的“替代、冲突、撤回、过期、废弃”等状态或关系。

### 回滚

任何影响正式临床判断的知识发布都必须有明确回滚路径：

```text
新版本发布
↓
发现质量或安全问题
↓
停止新版本生效
↓
恢复上一批准版本
↓
保留完整审计记录
```

回滚不得删除历史证据。

### 当前资产与处置

```text
知识图谱 + Evidence 资产 + G6
= REFACTOR + ADAPT

历史 Knowledge Evolution 中：
- 实验区 / 候选验证区 / 生产区
- 发布门禁
- 知识发布包
- 来源追溯
- 冲突处理
- 影子评估
- 回滚
= ABSORB
```

### 建设时机

```text
首次形成强依赖：
通常从 U03 风险规则
或 U08 鉴别方向 / Evidence
开始

后续：
U10 检查建议
U11/U12 解释与引用
继续扩展
```

P04 不要求在 U01 前完整建设。

### 最低质量门槛

- 线上知识必须可定位到正式发布版本；
- 安全关键规则必须显式版本化；
- 无来源知识不得成为正式医学依据；
- 来源撤回后可阻止继续生效；
- 冲突不得静默覆盖；
- 发布前必须执行回归与安全关键评估；
- 必须支持回滚；
- 生产知识不得被普通能力直接写入；
- `Knowledge Query Success != Clinical Truth`；
- `KG Path != Medical Citation`。


---

## P05 — 追踪与审计

### 职责
串联：
```text
consultation
event
run
unit
capability result
decision
proposal
commit
delivery
```

### 当前资产与处置
`Execution Trace = REUSE_FOUNDATION + ADAPT`

### 建设时机
```text
FOUNDATION_PREREQUISITE
+
INCREMENTAL_EXTENSION
```

---

## P06 — 能力范围、能力包与版本治理

### 职责

P06 不只是“版本绑定”，而是负责回答：

```text
当前咨询允许使用哪些能力？
能力适用于什么人群和范围？
当前使用的是哪个能力版本？
允许调用哪些工具、Skill、提示词、模型和知识？
能力当前是草稿、评估中、已启用还是已退役？
出现问题时应回滚到哪个版本？
```

因此 P06 的正式定位是：

> 能力范围 + 能力包 + 版本 + 激活状态 + 白名单 + 回滚治理

### 能力包

吸收历史 `Capability Package` 的有效设计。

一个正式能力包至少应包含：

```text
能力标识
能力版本
显示名称
负责人
临床审查人
技术审查人

适用人群
排除人群
适用地区
适用语言
适用渠道

支持的主诉 / 临床场景
不支持的请求
必须升级的场景

术语与概念包
观察字段定义
安全规则包
问题包
候选方向包
知识发布包

允许使用的工具
允许使用的 Skill
允许使用的提示词版本
允许使用的模型路由
允许使用的知识发布版本

交付规则
专项评估集
最低质量门槛
生效时间
失效时间
回滚目标版本
当前状态
```

### 能力生命周期

P06 应显式支持：

```text
草稿
→ 临床审查
→ 技术审查
→ 评估
→ 影子运行
→ 受限启用
→ 正式启用
→ 已废弃
→ 已退役
```

状态名可以调整，但必须保持：

```text
草稿 != 已启用
评估通过 != 已启用
已废弃 != 已退役
已退役 != 已删除
```

代码存在也不代表能力已经正式启用。

### 能力激活条件

正式咨询使用某能力前，至少检查：

```text
能力版本有效
适用范围命中
人群合法
地区 / 语言 / 渠道合法
知识版本有效
规则版本有效
模型 / 提示词 / 工具在白名单
评估门槛通过
安全要求满足
未过期
未被废弃或退役
```

### 范围与人群治理

P06 必须治理：

```text
支持人群
排除人群
特殊人群
支持主诉
支持症状概念
支持咨询类型
支持输出
不支持请求
强制升级条件
```

最终范围裁决仍由：

```text
C01 语义抽取
+
P06 范围与版本绑定
+
D10 范围裁决规则
```

共同完成。

P06 自身不拥有 `OUT_OF_SCOPE` 最终结论。

### 白名单治理

至少包括：

```text
允许工具
允许 Skill
允许提示词版本
允许模型路由
允许知识发布版本
允许规则版本
```

禁止：

```text
能力自行创建未注册工具
能力自行切换未经批准模型
能力绕过 Prompt Registry
能力使用未发布知识
能力修改 State Commit 规则
```

### 版本绑定

正式能力执行至少应追踪：

```text
capability_id
capability_version
scope_version
knowledge_release_version
rule_version
prompt_version
model_route_version
contract_version
```

适用时还应绑定：

```text
region
language
population
channel
```

### 回滚

每个正式能力版本都应具备回滚目标版本或等价机制。

```text
停止新版本激活
↓
恢复上一批准版本
↓
保留原运行记录与审计
```

不得用“直接改当前配置”替代正式回滚。

### 当前资产与处置

```text
历史 Capability Package 设计
= ABSORB

当前 V1 binding layer
= NEW / ADAPT

模型运行时 / Prompt Registry / Tool Registry / Knowledge Release
= P06 负责允许范围绑定
但内部实现仍分别属于 P03 / P04 / Runtime 等职责
```

### 建设时机

```text
基础前置：
U01 前需要最小版本
至少支持：
- Scope Version
- Capability Version
- Consultation binding

增量扩展：
U03 增加 Rule / Safety Pack binding
U06 增加 Question Policy binding
U08 增加 Hypothesis / Knowledge / Model / Prompt binding
U11/U12 增加 Delivery Policy binding
```

### 最低质量门槛

- 每个正式能力有稳定 `capability_id + version`；
- 每次临床执行可追踪当前激活版本；
- Scope / Population 可测试；
- 白名单可验证；
- 未批准模型 / 提示词 / 知识不得被调用；
- 能力版本与契约版本不兼容时必须阻断；
- 过期能力不得继续新建运行；
- 已废弃能力不得继续新增绑定；
- 已退役能力仅保留历史追踪；
- 支持安全回滚；
- `Capability Exists != Capability Active`；
- `Capability Eval PASS != Capability Authorized for Production Use`。


---

# 8. 确定性规则目录

| ID | 名称 | 首次消费者 | 核心职责 |
|---|---|---|---|
| D01 | 咨询生命周期规则 | U01 | Consultation 生命周期合法转移 |
| D02 | 安全门裁决规则 | U04 | ALLOW / RESTRICTED / BLOCKED / UNAVAILABLE |
| D03 | 临床准备度裁决规则 | U05 | Clinical Readiness 唯一求值 |
| D04 | 提问停止规则 | U06 | 是否继续问、何时停止 |
| D05 | 更正与依赖失效规则 | U13 | 更正后的依赖失效传播 |
| D06 | 交付校验规则 | U11 | Delivery Package 合法性验证 |
| D07 | 失败路由与降级规则 | U14 | retry / repair / fallback / safe exit / terminal |
| D08 | 取消与过期规则 | U15 | cancel / expiry / late-event rejection |
| D09 | 临床风险等级裁决规则 | U03 | 正式 Clinical Risk Disposition |
| D10 | 范围裁决规则 | U01 | IN_SCOPE / OUT_OF_SCOPE / NEEDS_CLARIFICATION |

关键边界：

```text
D06 Validation Result != Delivery Readiness
D05 属于 G2 治理，不成为跨模块 Owner
C02 Result != D09 Clinical Risk Disposition
C01 Scope semantics != D10 Scope Decision
```

---

# 9. 三类能力的建设方式

C、P、D 三类虽然都出现在依赖矩阵里，但建设方式不同：

```text
C 类临床能力
= 通常在第一个真实消费它的 Unit 开始时建设

D 类确定性规则
= 在第一个依赖它的 Unit 实施前准备到可用

P 类平台能力
= 跨 Unit 共享
= 首个 Unit 前只建设最小公共基础
= 后续随着 Unit 需求持续扩展
```

其中：

```text
P01 / P05 / P06
偏基础前置

P02
在 U07 Resume 场景形成完整需求

P03
在真正出现模型型能力时形成强依赖

P04
在风险规则、DDx、Evidence、Workup 等场景形成强依赖
```

---

# 10. 业务开发单元 → C / P / D 依赖矩阵


| Unit | Clinical Capability | Platform | Deterministic Policy | 建设说明 |
|---|---|---|---|---|
| U01 | C01 | P05、P06；正式 Clinical State 写入时经 P01 | D01、D10 | C01/P06/D01/D10 首次消费者；Foundation 提供最小治理与 Trace |
| U02 | C01 | P01、P05、P06 | D05 相关失效规则输入 | 扩展 C01 到 fact/normalization/provenance；扩展 P01 typed commit |
| U03 | C02 | P01、P04、P05、P06 | D09 | 首次建设 Risk Rule Pack、Risk Eval、Risk Resolver |
| U04 | 无独立 AI Capability | P01、P05 | D02 | 纯确定性安全裁决 |
| U05 | 无独立 AI Capability | P01、P05 | D03 | 纯确定性 readiness resolver |
| U06 | C03 | P01、P05、P06 | D04 | 首次建设 Question / Gap 能力 |
| U07 | 无临床 AI | P01、P02、P05、P06 | Resume validation / lifecycle rules | P02 Durable Resume 首次成为核心依赖 |
| U08 | C04 | P01、P03、P04、P05、P06 | 相关 stopping / safety constraints | 首次建设 DDx / Evidence Intelligence |
| U09 | C03、C04 | P01、P04、P05、P06 | D04 | 扩展 Gap Re-evaluation / Question Value |
| U10 | C05 | P01、P04、P05、P06 | 规则治理 | 首次建设 Workup Intelligence |
| U11 | C06 | P03（如需模型）、P05、P06 | D06 | C06 首次消费者 |
| U12 | C06 | P03（如需模型）、P05、P06 | D06 | 与 U11 共享能力族 |
| U13 | C01 | P01、P05、P06 | D05 | 扩展 correction 后 parsing / invalidation |
| U14 | 无临床 AI | P02、P05 | D07 | Failure Router / Fallback |
| U15 | 无临床 AI | P02、P05 | D01、D08 | cancel / expire / runtime cancellation |

说明：

1. `P01` 不是每个 Unit 都“新建一次”，而是所有正式 Clinical State 写入复用同一治理路径。
2. `P05` 是共享 Trace，随 Unit 增量补充关联。
3. `P06` 是共享版本 / 范围绑定，随 Capability / Rule / Knowledge 类型增量扩展。
4. “无独立 AI Capability”不等于“没有逻辑”，只是核心逻辑属于 D / Governance。

---

# 11. 各业务开发单元的关键语义

## U01
```text
C01 semantic extraction
→ subject / complaint / intent / ambiguity

P06
→ Scope / Version Binding

D10
→ IN_SCOPE / OUT_OF_SCOPE / NEEDS_CLARIFICATION

D01
→ lifecycle transition
```
约束：OUT_OF_SCOPE 不由模型自由决定；clarification need 不等于 WAITING_USER；风险线索不能因 framing 不完整被丢弃。

## U02
```text
C01
→ fact candidate / normalization / provenance

Business Owner
→ interpretation

P01 / G2
→ governed commit
```
约束：
```text
UNKNOWN != NO
UNMEASURED != NORMAL
MODEL_INFERRED != PATIENT_REPORTED
```
Capability 成功但 G2 reject，U02 仍不成功。

## U03
```text
C02
→ risk evidence

D09
→ Clinical Risk Disposition
```
约束：
```text
Risk failure != NO_HIGH_RISK_SIGNAL
NO_DDX != LOW_RISK
```

## U04
```text
Clinical Risk
+ capability availability
+ authorization
+ mandatory safety rules
→ D02
```
约束：`SAFETY_FAILURE != SAFE`

## U05
readiness inputs → D03 → exactly one Clinical Readiness。不得用固定 completeness 百分比替代业务 readiness。

## U06
```text
C03 question candidates
+ D04 stopping
→ selected question
→ delivery boundary
→ WAITING_USER
```
Question Capability 不拥有 WAITING_USER。

## U07
重点：Pending Question identity、Duplicate detection、Expiry、Clinical State Version compatibility、Checkpoint compatibility、Idempotent apply、crash / replay。

```text
Business Resume validity
!= Runtime Resume compatibility
```

## U08
```text
C04
→ DDx / Must-Exclude candidates
→ evidence relations
```
约束：
```text
NO_RELIABLE_DIRECTION != FAILED
KG path != Citation
```

## U09
Capability 只回答“缺什么、影响什么、哪些问题值得候选”；是否继续循环由业务与 stopping/readiness 决定。

## U10
```text
Suggestion != Medical Order
Assessment FAILED != NOT_NEEDED
```

## U11 / U12
```text
C06 rendering
+ D06 validation
→ F7 interprets Delivery Readiness
```
`D06 Validation Result != Delivery Readiness`

## U13
```text
Correction Event
→ validation
→ D05 dependency invalidation
→ P01 / G2 commit
```
Rejected correction = 0 Clinical State effect。

## U14
failure → classify → retry / repair / fallback / safe exit / terminal。failure 不得包装成 normal empty object。

## U15
CANCELLED / EXPIRED 后：普通 Resume 拒绝；late async result 不写状态；不修改历史 Clinical Truth。

---

# 12. Slice A 能力集合

首个候选纵向 Slice：

```text
U01–U07
+
U11 / U14 / U15 提供横向闭合
```

Clinical Capability：
```text
C01 临床理解能力
C02 临床安全识别能力
C03 问题与信息缺口能力
```

Platform：
```text
P01 State Governance / Clinical CDP Adapter
P02 Durable Clinical Resume
P03 Model Runtime clinical adapter（仅当对应 Capability 真正使用模型）
P05 追踪与审计
P06 Scope / Capability Version Binding
```

Deterministic Policy：
```text
D01 Lifecycle
D02 Safety Gate
D03 Readiness
D04 Stopping
D06 Safe Exit 交付校验规则
D07 Failure Router
D08 Cancel / Expire
D09 Risk Disposition
D10 Scope Adjudication
```

关键解释：

```text
“Slice A 需要这些能力”
!=
“开始 Slice A 前一次性实现全部这些能力”
```

正确方式：
```text
按 Unit 首次消费时建设
+
共享基础能力最小化 Foundation
+
后续 Unit 增量扩展
```

---

# 13. 能力失败统一语义

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

必须保持：

```text
NO_RESULT != DEPENDENCY_FAILURE
INSUFFICIENT_INFORMATION != INVALID_OUTPUT
UNSUPPORTED != TIMEOUT
SAFETY_BLOCKED != SUCCESS
```

Business Unit 不允许通过 `null / [] / {}` 猜测 Failure 语义。

---

# 13. Capability Quality Gate

Capability 进入正式 Unit 前至少需要：

```text
Capability ID / Version
Purpose / Supported Scope
Input semantic boundary
Output semantic boundary
Failure semantics
Knowledge / Rule / Prompt / Model versions（适用时）
EvalSet
Baseline
Metrics
Acceptance threshold
Safety cases
Regression suite
Fallback / unavailable behavior
Owner / Review status
```

| Capability | 质量重点 |
|---|---|
| C01 | fact / value / source / negation / temporality / provenance |
| C02 | red-flag recall / fail-closed / safety suite |
| C03 | decision value / duplicate rate / user burden |
| C04 | supported candidate / must-not-miss / evidence correctness |
| C05 | necessity / formal basis / no medical-order overreach |
| C06 | faithfulness / uncertainty / no-new-claims |

```text
Capability Eval PASS
!= Business Unit PASS
```

---

# 15. 模型、提示词与知识图谱统一规则

## 14.1 Legacy LLM
```text
Legacy common LLM runtime = REMOVE after migration
旧 clinical call sites = REPLACE
```
禁止为了恢复旧功能重新启用 fail-closed Legacy LLM。

## 14.2 Model Runtime
```text
Unit
→ approved Capability
→ Model Runtime
→ versioned Prompt Registry
→ approved model route
→ structured Capability Result
→ Business Owner / Policy
→ G2 commit when formal Clinical State changes
```

## 14.3 KG / RAG / Evidence
必须区分：
```text
Patient Evidence
Clinical Rule Evidence
KG Reasoning Path
Medical Knowledge Citation
Model Inference
```

---

# 16. Phase 7 全局不变量

```text
CAP-INV-01 Capability Result != Clinical Truth
CAP-INV-02 Capability 不拥有 Consultation / Clinical Risk / Safety Gate / Readiness / Delivery 等业务真值
CAP-INV-03 能由确定性 Policy 完成的最终裁决不得交给自由概率模型
CAP-INV-04 凡正式写入 Versioned Clinical State / CDP 的状态改变必须经过 G2/P01
CAP-INV-05 Safety-critical Capability failure 必须 fail-closed
CAP-INV-06 NO_RESULT != FAILURE
CAP-INV-07 UNKNOWN / UNMEASURED 不得由 Capability 改写为阴性 / 正常
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
CAP-INV-18 交付校验规则 Result != Delivery Readiness；Delivery Readiness 仅由 F7 解释
CAP-INV-19 Dependency Invalidation Rules 属于 G2 治理机制，不成为跨模块状态 Owner
```

---

# 17. 独立审查闭环

```text
P7-R01
C02 Risk Synthesis 与 F4 Owner 冲突
→ CLOSED：C02 仅提供风险证据 / Proposed Assessment；D09 / F4 形成最终 Clinical Risk Disposition。

P7-R02
Scope Recognition 混合语义理解与最终范围裁决
→ CLOSED：C01 负责语义抽取；P06 + D10 负责最终 Scope adjudication。

P7-R03
P01/G2 使用范围枚举过窄
→ CLOSED：任何正式 Versioned Clinical State / CDP 写入均必须经过 G2/P01。

P7-R04
交付校验规则 可能侵入 Delivery Readiness ownership
→ CLOSED：D06 只产生 Validation Result；Delivery Readiness 仍由 F7 解释。

P7-R05
Dependency Invalidation Engine 可能形成新的跨模块 Owner
→ CLOSED：D05 属于 G2；模块只提供 invalidation requirement。
```

---

# 18. Phase 7 完成定义

Phase 7 应能直接回答：

1. V1 有哪些 Clinical Capability？
2. 有哪些共享 Platform Capability？
3. 有哪些 Deterministic Policy？
4. 每个 Unit 依赖哪些 C / P / D？
5. 每个能力的首次消费者是谁？
6. 哪些属于 Foundation prerequisite？
7. 哪些应按 Unit 增量扩展？
8. 哪些现有资产 REUSE / ADAPT / REFACTOR / REPLACE / NEW？
9. 每项能力最低质量门槛是什么？
10. 哪些能力明确不拥有业务真值？

冻结语义：
```text
6 个 Clinical Capability Family
6 个 Platform Capability
10 个 Deterministic Policy
U01–U15 依赖矩阵
统一建设时机模型
统一 Failure 语义
统一 Capability Quality Gate
19 条 Phase 7 全局不变量
```

```text
SOP Phase 7 — Capability Design
= FROZEN / V1 semantic baseline
```

---

# 19. 施工时如何使用本文件

实施任一 Unit 时：

```text
1. 在 Unit Dependency Matrix 找到当前 Uxx
2. 列出其依赖 C / P / D
3. 对每项依赖判断：
   - 已满足？
   - 需要 ADAPT？
   - 需要增量扩展？
   - 还是 MISSING？
4. 检查首次消费者 / 建设时机
5. 只建设当前 Unit 真正需要的最小范围
6. Capability 达到 Quality Gate 后接回 Unit
7. Unit Verification PASS 后再推进下一个 Unit
```

始终保持：

```text
Capability exists
!= Capability verified
!= Unit verified

Phase 7 Design
!= Implementation Status
```
