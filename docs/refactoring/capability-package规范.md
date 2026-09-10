# AIdoctor Capability Package 规范

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> 适用分支：`agent/enterprise-agent-refactoring-plan`

## 1. 目的

本文定义 AIdoctor 如何在不修改通用平台骨架的前提下扩展新的临床场景。

AIdoctor 的扩展单位不是一个 Prompt、一个疾病列表或一个独立 Agent，而是一个经过治理、可版本化、可评估、可回滚的 **Capability Package**。

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

首个 Capability 为 `adult_respiratory_v1`。它用于验证通用平台，不将系统永久限制在呼吸道领域。

## 2. Capability 与平台的边界

### 2.1 平台负责

- Encounter、Thread、Checkpoint、Interrupt、Resume；
- State Committer 和 ClinicalObservation 写入规则；
- 通用 Context、Memory、RAG Runtime；
- Model Router、Prompt Registry、Tool Registry；
- 权限、Consent、审计、Trace、评估框架；
- 医生审核、Delivery、Follow-up 基础设施；
- 固定 Workflow 降级路径。

### 2.2 Capability 负责

- 支持的主诉和临床场景；
- 适用人群与排除人群；
- 领域概念、字段和术语映射；
- 必须采集的信息；
- 红旗和分诊规则；
- 有限候选与 must-not-miss 范围；
- Question Policy 配置；
- 允许调用的 Tool、Skill、Prompt 和模型路由；
- 白名单知识来源与 Knowledge Release；
- 输出边界与医生审核条件；
- 专项评估集、阈值和上线策略。

### 2.3 不属于 Capability 的内容

- 重新定义 State Committer；
- 允许模型直接写临床真值；
- 绕过 Mandatory Safety Check；
- 自行创建未经注册的 Tool 或模型；
- 自行改变 Thread/Checkpoint 协议；
- 自行决定 PHI 是否可发送到外部供应商。

## 3. Capability 生命周期

```text
DRAFT
→ CLINICAL_REVIEW
→ TECHNICAL_REVIEW
→ EVALUATION
→ SHADOW
→ CLINICIAN_ASSIST
→ RESTRICTED_PATIENT
→ ACTIVE
→ DEPRECATED
→ RETIRED
```

任何 Capability 从 `DRAFT` 进入 `ACTIVE` 前必须具备：

- 明确 Owner；
- 临床 Reviewer；
- 版本化配置；
- Knowledge Release；
- Prompt Release；
- Model Route Policy；
- Tool/Skill Allowlist；
- Safety Test Suite；
- E2E Test Suite；
- Rollback Plan；
- 评估报告。

## 4. CapabilityManifest

```python
class CapabilityManifest(BaseModel):
    capability_id: str
    version: str
    display_name: str
    status: Literal[
        "draft",
        "clinical_review",
        "technical_review",
        "evaluation",
        "shadow",
        "clinician_assist",
        "restricted_patient",
        "active",
        "deprecated",
        "retired",
    ]

    owner_team: str
    clinical_reviewers: list[str]
    technical_reviewers: list[str]

    supported_regions: list[str]
    supported_languages: list[str]
    supported_channels: list[str]

    population_policy: PopulationPolicy
    scope_policy: ScopePolicy
    observation_schema_version: str

    safety_policy_id: str
    question_policy_id: str
    hypothesis_policy_id: str
    stopping_policy_id: str
    review_policy_id: str
    delivery_policy_id: str

    allowed_tool_ids: list[str]
    allowed_skill_ids: list[str]
    allowed_prompt_release_ids: list[str]
    allowed_model_route_policy_ids: list[str]
    knowledge_release_ids: list[str]

    eval_suite_ids: list[str]
    minimum_eval_thresholds: dict[str, float]

    effective_from: datetime | None
    effective_until: datetime | None
    rollback_capability_version: str | None
```

## 5. 人群与范围

```python
class PopulationPolicy(BaseModel):
    minimum_age: int | None
    maximum_age: int | None
    included_populations: list[str]
    excluded_populations: list[str]
    special_population_rules: list[str]

class ScopePolicy(BaseModel):
    supported_chief_complaints: list[str]
    supported_symptom_concepts: list[str]
    supported_encounter_types: list[str]
    supported_outputs: list[str]
    unsupported_requests: list[str]
    mandatory_escalation_conditions: list[str]
```

首版 Capability 必须明确：

- 哪些输入属于支持范围；
- 哪些输入属于可安全分流但不做临床推理；
- 哪些输入必须交给医生或急救路径；
- 哪些输入只能提供通用说明；
- 哪些输入必须拒绝自动处理。

## 6. 领域数据包

每个 Capability 必须提供以下数据资产。

### 6.1 Terminology Pack

- 主诉和症状同义词；
- 常用医学概念；
- ICD、SNOMED、LOINC 等映射；
- 否定、程度、频率、时间表达；
- 易混淆概念；
- 用户俗称与标准术语映射。

### 6.2 Observation Profile

```python
class ObservationFieldSpec(BaseModel):
    concept_id: str
    display_name: str
    value_type: str
    required_level: Literal["critical", "important", "optional"]
    allowed_sources: list[str]
    validation_rules: list[str]
    conflict_policy: str
    sensitivity_level: str
```

### 6.3 Question Pack

- 必问问题；
- 红旗问题；
- 区分问题；
- 澄清问题；
- 报告补充问题；
- 停止询问条件；
- 用户负担和重复问题限制。

### 6.4 Safety Pack

- 单项红旗；
- 组合红旗；
- 生命体征阈值；
- 特殊人群规则；
- 输入不足时的保守策略；
- 高风险升级路径；
- 输出禁止项。

### 6.5 Hypothesis Pack

候选只用于有限鉴别，不用于自动确诊。

```python
class HypothesisSpec(BaseModel):
    concept_id: str
    display_name: str
    category: Literal["common", "alternative", "must_not_miss"]
    supporting_discriminators: list[str]
    opposing_discriminators: list[str]
    required_exclusions: list[str]
    applicable_populations: list[str]
    prohibited_auto_actions: list[str]
```

### 6.6 Knowledge Pack

- `knowledge_release_id`；
- 允许的来源 Tier；
- 地区和人群过滤；
- 支持的问题类型；
- 无结果和冲突策略；
- 患者教育内容；
- 来源撤回和过期策略。

### 6.7 Runtime Pack

- 允许的 Graph Node；
- Tool/Skill Allowlist；
- Prompt Release Allowlist；
- Model Route Policy；
- Context Policy；
- Token Budget；
- Retry/Fallback；
- Human Review 条件。

## 7. Capability Package 目录结构

```text
capabilities/
└── adult_respiratory_v1/
    ├── manifest.yaml
    ├── terminology/
    │   ├── concepts.yaml
    │   ├── synonyms.yaml
    │   └── code_mappings.yaml
    ├── observations/
    │   └── observation_profile.yaml
    ├── safety/
    │   ├── red_flags.yaml
    │   ├── triage_rules.yaml
    │   └── special_populations.yaml
    ├── questions/
    │   ├── mandatory.yaml
    │   ├── discriminators.yaml
    │   └── stopping_rules.yaml
    ├── hypotheses/
    │   └── limited_hypotheses.yaml
    ├── knowledge/
    │   └── knowledge_policy.yaml
    ├── runtime/
    │   ├── tool_allowlist.yaml
    │   ├── skill_allowlist.yaml
    │   ├── prompt_allowlist.yaml
    │   ├── model_routes.yaml
    │   └── context_policy.yaml
    ├── delivery/
    │   └── delivery_policy.yaml
    └── evals/
        ├── safety_cases.jsonl
        ├── extraction_cases.jsonl
        ├── question_cases.jsonl
        ├── rag_cases.jsonl
        └── e2e_cases.jsonl
```

## 8. 首个 Capability：adult_respiratory_v1

### 8.1 目标

成人常见呼吸道相关主诉的：

- 风险分层；
- 结构化信息采集；
- 有限候选；
- must-not-miss 提醒；
- 白名单循证展示；
- 医生交接；
- 就医导航。

### 8.2 首版支持输入

- 咳嗽；
- 咳痰；
- 发热伴呼吸道症状；
- 喘息；
- 胸闷；
- 呼吸困难；
- 呼吸相关胸痛；
- 咯血；
- 呼吸道检查或报告解读的受控入口。

### 8.3 首版不自动处理

- 儿童和新生儿；
- 妊娠期复杂诊疗；
- 自动处方或用药调整；
- 重症监护决策；
- 影像自动确诊；
- 对全部呼吸系统疾病做开放式诊断；
- 无医生审核的治疗建议。

### 8.4 最小候选边界

候选范围由临床团队评审后形成版本化清单。技术实现只允许加载已批准候选，不允许模型自由生成无限疾病集合。

### 8.5 必须验证的首条场景

```text
成人患者：“咳嗽三天，有点喘”
→ 结构化 Observation
→ Mandatory Safety
→ 选择下一问题
→ Checkpoint
→ Restart / Resume
→ 更新 Triage 与有限候选
→ 输出有限就医建议
```

## 9. 扩展新场景的标准步骤

```text
1. 提交 Capability Proposal
2. 定义人群、主诉、排除项和安全边界
3. 创建 Terminology / Observation / Safety / Question Pack
4. 创建 Knowledge Release
5. 创建 Prompt 和 Model Route Release
6. 配置 Tool/Skill Allowlist
7. 建立专项 Eval Suite
8. Shadow
9. Clinician Assist
10. Restricted Patient
11. Active
```

新增场景不应修改：

- State Committer 规则；
- Durable Execution 协议；
- 通用 Audit 和 Trace；
- 通用 Prompt/Model Gateway；
- 通用 Tool Registry。

## 10. 版本和兼容性

Capability 版本遵循语义化版本：

- Major：临床边界、人群、安全或状态契约不兼容；
- Minor：新增候选、问题、来源或受控能力；
- Patch：文案、阈值修正和不改变边界的修复。

每个 Encounter 在创建时绑定明确的 Capability 版本。运行中不得静默切换到新版本。

## 11. 回滚

Capability 回滚必须同时处理：

- Manifest；
- Safety Policy；
- Prompt Release；
- Model Route Policy；
- Knowledge Release；
- Tool/Skill Allowlist；
- 前端 Feature Flag。

历史 Encounter 继续使用其绑定版本，除非存在安全召回并经过受控迁移。

## 12. 评估门禁

至少包含：

- 范围识别；
- 红旗召回；
- 分诊一致性；
- Observation 抽取；
- 重复问题率；
- 信息缺口选择；
- 有限候选质量；
- must-not-miss 召回；
- RAG 检索与引用；
- 患者隔离；
- Prompt Injection；
- Resume 和幂等；
- 医生审核闭环；
- 患者表达安全。

## 13. 完成定义

一个 Capability 只有在以下条件全部满足时才可声明完成：

- 范围和非目标已批准；
- 所有资产有版本和 Owner；
- Safety 不依赖模型单点判断；
- 模型只使用批准的 Prompt/Route；
- 知识只来自批准的 Knowledge Release；
- 所有写入经过 State Committer；
- 高风险进入医生或固定安全路径；
- E2E、Crash、Security 和 Clinical Eval 达标；
- 具备回滚和停用能力。
