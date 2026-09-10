# AIdoctor 临床数据与循证智能扩展方案

> 文档状态：Draft v2.1 Extension  
> 更新时间：2026-07-29  
> 关联主方案：[AIdoctor 企业级临床 Agent 重构方案](./enterprise-agent-refactoring-plan.md)  
> 适用仓库：`cxjchelsea/AIdoctor`

---

## 1. 文档目的

本文档用于把两类此前未被充分体现的能力正式纳入 AIdoctor 重构：

1. **Clinical Data & Research Foundation**：面向纵向患者数据、临床表型、研究数据集和模型验证的数据基础；
2. **Evidence Intelligence Service**：面向指南、系统综述和医学研究的循证检索、来源治理、引用验证和证据冲突处理能力。

这两部分分别受到 UK Biobank 一类纵向生物医学数据平台，以及 OpenEvidence 一类临床循证决策支持产品的启发。

它们不替代主方案中的 Clinical Intelligence、Safety Engine 和 LangGraph Runtime，也不应被实现成两个新的自由规划 Agent。

它们在系统中的定位是：

```text
Clinical Data & Research Foundation
为临床状态、训练、评估和研究提供可信数据基础

Evidence Intelligence Service
为医生和临床推理模块提供可追踪的医学证据
```

---

## 2. 为什么需要增加这两层

主方案已经解决了以下问题：

- 如何管理多轮问诊状态；
- 如何进行安全检查和动态提问；
- 如何控制工具调用和状态写入；
- 如何暂停、恢复和人工审核；
- 如何生成患者版、医生版和系统版结果；
- 如何连接就医导航和随访。

但仍存在两个平台级缺口。

### 2.1 单次问诊与长期患者状态容易混淆

一次问诊中的模型推断、患者自述或临时风险，不应被直接写成患者永久疾病事实。

例如：

```text
本次问诊：症状模式可能与哮喘相关
```

不能直接转化为：

```text
患者确诊疾病：哮喘
```

系统需要区分：

- 本次 Encounter 的临床证据；
- 患者长期稳定事实；
- 模型推断；
- 疑似状态；
- 检查待确认状态；
- 医生确认状态；
- 已排除状态。

### 2.2 有 RAG 不等于有循证能力

普通 RAG 可能只完成：

```text
检索若干文档
→ 将内容交给 LLM
→ 生成一段带引用回答
```

但临床证据服务还必须回答：

- 来源是否权威；
- 来源是否最新；
- 来源是否真的支持对应结论；
- 来源人群是否适用于当前患者；
- 不同指南是否存在冲突；
- 系统是否把研究关联说成因果；
- 单篇研究是否被错误升级为指南推荐；
- 医生能否追踪每一条关键结论的依据。

因此，需要将 Evidence Intelligence 从诊断引擎和解释生成中独立出来。

---

## 3. 设计原则

### 3.1 纵向患者记录与单次问诊分离

长期事实、历史事件和本次问诊状态分别管理。

### 3.2 原始事实、派生状态和模型推断分离

患者原话、设备数据、医生确认和模型推断不能同权，也不能相互静默覆盖。

### 3.3 所有派生表型必须有定义版本

“糖尿病”“疑似哮喘”“高血压控制不佳”等标签必须能够说明如何计算出来。

### 3.4 生产系统与研究系统隔离

生产数据库不是实验沙箱；研究数据必须经过授权、脱敏、版本化和用途约束。

### 3.5 Evidence Service 不直接替代临床决策

Evidence Service 返回证据包、适用性、冲突和限制，不直接批准治疗或处方动作。

### 3.6 引用必须绑定到结论

不是“回答末尾列出几篇论文”，而是每一条关键 Claim 能追踪到支持来源。

### 3.7 无证据和证据冲突是正常状态

系统必须允许：

- `NO_RELIABLE_EVIDENCE`；
- `EVIDENCE_CONFLICT`；
- `POPULATION_MISMATCH`；
- `STALE_GUIDELINE`；
- `CITATION_NOT_SUPPORTING_CLAIM`。

---

## 4. 更新后的完整架构

```text
┌──────────────────────────────────────────────────────────┐
│ Patient UI / Clinician Console                           │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│ Business & Care Delivery                                 │
│ 用户、权限、病历授权、预约、转诊、审核、随访、机构隔离     │
└──────────────────────────┬───────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────┐
│ LangGraph Agent Runtime                                  │
│ GraphState、Checkpoint、Interrupt、工具治理、Retry、Fallback│
└──────────────────┬───────────────────────┬───────────────┘
                   │                       │
┌──────────────────▼────────────────┐  ┌──▼─────────────────────────┐
│ Clinical Intelligence             │  │ Evidence Intelligence      │
│                                   │  │                            │
│ Clinical Terminology              │  │ Clinical Question Parser   │
│ Question Policy                   │  │ PICO Builder               │
│ Diagnostic Inference              │  │ Source Retrieval           │
│ Triage & Safety                   │  │ Evidence Ranking           │
│ Care Navigation                   │  │ Applicability Evaluator    │
│ Follow-up Reasoning               │  │ Citation Validator         │
│                                   │  │ Conflict Detector          │
└──────────────────┬────────────────┘  └──┬─────────────────────────┘
                   │                       │
┌──────────────────▼───────────────────────▼─────────────────────────┐
│ Clinical Data & Research Foundation                               │
│                                                                   │
│ Longitudinal Patient Record                                       │
│ Encounter / CDP / Evidence Ledger                                 │
│ Source Artifact Registry                                         │
│ Phenotype Registry                                                │
│ Dataset & Cohort Registry                                         │
│ Research Workspace                                                │
│ Model / Prompt / Knowledge Registry                               │
└───────────────────────────────────────────────────────────────────┘
```

---

# Part A：Clinical Data & Research Foundation

## 5. 数据域分层

建议将当前 CDP 进一步拆成以下数据域。

### 5.1 Patient Longitudinal Record

保存跨 Encounter 的长期患者状态。

包括：

- 基础人口信息；
- 稳定风险因素；
- 慢性病状态；
- 既往病史和手术史；
- 过敏史；
- 长期用药；
- 历史检查和重要结果；
- 医生确认诊断；
- 历史 Encounter 索引；
- 长期随访趋势；
- 数据授权和用途范围。

不应直接保存：

- 未经确认的模型诊断；
- 单轮对话中的临时猜测；
- 没有来源的结构化标签；
- 已过期但未标记失效的风险状态。

### 5.2 Encounter

一次完整问诊、复诊、随访或报告解读过程。

```python
class Encounter(BaseModel):
    encounter_id: str
    patient_id: str
    encounter_type: Literal[
        "initial_consultation",
        "follow_up",
        "report_review",
        "clinician_review"
    ]

    capability_id: str
    started_at: datetime
    ended_at: datetime | None

    status: str
    consent_scope_id: str
    originating_channel: str

    cdp_id: str
    thread_id: str
```

### 5.3 Encounter CDP

CDP 只表达本次 Encounter 的临床决策状态。

```python
class EncounterCDP(BaseModel):
    cdp_id: str
    encounter_id: str
    version: int

    observations: list[ClinicalObservation]
    hypotheses: list[DiagnosticHypothesis]
    triage: TriageAssessment | None
    uncertainty: UncertaintyState
    workup_plan: WorkupPlan | None
    management_plan: ManagementPlan | None
    care_path: CarePath | None

    status: Literal[
        "collecting_information",
        "reasoning",
        "awaiting_user",
        "awaiting_artifact",
        "awaiting_clinician",
        "completed",
        "cancelled"
    ]
```

### 5.4 Evidence Ledger

Evidence Ledger 是 CDP 的核心事实层。

```python
class ClinicalObservation(BaseModel):
    observation_id: str
    concept_id: str
    concept_system: str
    concept_type: str

    status: Literal["present", "absent", "unknown"]
    value: Any | None
    unit: str | None

    source_type: Literal[
        "patient_initial",
        "patient_answer",
        "caregiver",
        "device",
        "ehr",
        "report",
        "clinician",
        "derived_rule",
        "model_inference"
    ]

    source_artifact_id: str | None
    encounter_id: str

    observed_at: datetime | None
    recorded_at: datetime
    valid_from: datetime | None
    valid_until: datetime | None

    confidence: float
    input_quality: float
    verification_status: Literal[
        "unverified",
        "patient_confirmed",
        "artifact_confirmed",
        "clinician_confirmed"
    ]

    supersedes_observation_id: str | None
    derived_from_observation_ids: list[str]
```

---

## 6. 临床状态不能静默覆盖

### 6.1 状态更新操作

只允许：

- `add`：增加新事实；
- `supersede`：新证据替代旧证据，但保留旧记录；
- `annotate`：增加解释或审核意见；
- `invalidate`：标记数据无效；
- `confirm`：提高确认等级；
- `derive`：基于规则生成派生状态。

### 6.2 禁止操作

- 直接覆盖患者原话；
- 用模型推断替换医生确认；
- 不留版本地修改历史结果；
- 将一次性风险状态永久化；
- 删除冲突证据来使结果“看起来一致”。

### 6.3 冲突示例

```text
Observation A
患者首次回答：无胸痛

Observation B
后续回答：运动时胸口发紧

系统处理
- 保留 A 和 B；
- 标记潜在冲突；
- 生成澄清问题；
- 安全引擎重新评估；
- 不直接选择其中一个覆盖另一个。
```

---

## 7. Source Artifact Registry

ClinicalObservation 应尽可能关联原始材料。

```python
class SourceArtifact(BaseModel):
    artifact_id: str
    patient_id: str
    encounter_id: str | None

    artifact_type: Literal[
        "conversation_message",
        "ehr_document",
        "lab_report",
        "imaging_report",
        "image",
        "device_measurement",
        "clinician_note"
    ]

    storage_uri: str
    checksum: str
    mime_type: str

    collected_at: datetime
    source_organization: str | None
    author_or_device: str | None

    extraction_status: str
    quality_score: float | None
    consent_scope_id: str

    retention_policy_id: str
```

用途：

- 追踪某个字段从哪里来；
- 重新解析时能够复现；
- OCR 或结构化模型升级后重新运行；
- 审计医生和模型看到的原始材料；
- 删除数据时找到所有派生结果。

---

## 8. 来源可靠度模型

证据不能只看一个模型置信度。

建议使用：

```text
EvidenceReliability =
    SourceReliability
  × TemporalRelevance
  × InputQuality
  × VerificationLevel
  × ClinicalRelevance
```

### 8.1 来源初始等级示例

| 来源 | 默认等级 | 说明 |
|---|---:|---|
| 医生确认 | 高 | 仍需保留日期、机构和上下文 |
| 原始检查数据 | 高 | 需要输入质量和身份匹配 |
| 正式报告 | 高 | 需要报告时间和患者归属 |
| 设备测量 | 中高 | 依赖设备质量和测量规范 |
| 患者自述 | 中 | 对主观症状非常重要，但可能存在记忆误差 |
| 家属转述 | 中低 | 需要标记转述关系 |
| 规则派生 | 取决于规则 | 必须关联规则版本 |
| 模型推断 | 低至中 | 不能作为已确认事实 |

### 8.2 可靠度不能直接等于临床重要性

患者自述可能可靠度不如检查报告，但“出现胸痛”仍然可能具有极高的安全价值。

因此 Safety Engine 应独立读取：

- 严重度；
- 红旗组合；
- 不确定性；
- 来源；
- 输入质量；
- 是否需要保守升级。

---

## 9. Phenotype Registry

### 9.1 为什么需要表型定义

临床系统中的标签可能来自：

- ICD 编码；
- 医生诊断；
- 检查结果；
- 用药记录；
- 患者自述；
- 多条规则组合；
- 模型预测。

系统必须明确某个“疾病状态”是如何得出的。

### 9.2 PhenotypeDefinition

```python
class PhenotypeDefinition(BaseModel):
    phenotype_id: str
    name: str
    version: str

    status: Literal["draft", "validated", "deprecated"]

    inclusion_rules: list[ClinicalRule]
    exclusion_rules: list[ClinicalRule]

    required_sources: list[str]
    supporting_sources: list[str]

    output_state: Literal[
        "possible",
        "suspected",
        "probable",
        "confirmed",
        "ruled_out"
    ]

    population_scope: PopulationDescriptor
    valid_from: datetime
    valid_until: datetime | None

    clinical_reviewer: str
    evidence_source_ids: list[str]
```

### 9.3 示例

```text
suspected_asthma_v1
- 反复喘息或咳嗽模式；
- 存在触发因素；
- 尚未完成肺功能确认；
- Agent 可以生成 suspected；
- 不能写入 confirmed。

confirmed_asthma_v1
- 医生确认；
或
- 满足机构认可的检查标准并经医生审核；
- Agent 不能自动提交。
```

### 9.4 表型版本影响

表型定义变化时：

- 不静默重写旧数据；
- 新旧结果分别保留；
- 评估报告记录使用的表型版本；
- 训练标签可追溯；
- 生产规则变更必须经过回归测试。

---

## 10. Longitudinal State Promotion

本次 Encounter 的状态进入长期患者记录，必须经过 Promotion Policy。

```python
class PromotionDecision(BaseModel):
    observation_id: str
    target_field: str

    decision: Literal[
        "do_not_promote",
        "promote_as_unverified",
        "promote_as_suspected",
        "promote_as_confirmed"
    ]

    reason: str
    requires_clinician_review: bool
    reviewer_id: str | None
```

### 10.1 可以自动提升的示例

- 患者确认的基础人口信息；
- 明确的过敏史，但仍需标记来源；
- 可验证的设备测量趋势；
- 用户明确授权保存的长期风险因素。

### 10.2 不允许自动提升的示例

- 模型产生的疾病候选；
- 未经医生确认的治疗反应解释；
- 单次问诊推测的慢性病；
- 与已有长期事实冲突的信息；
- 超出 Capability 的推断。

---

## 11. Dataset Registry

生产问诊数据不能被直接当作训练集。

```python
class DatasetVersion(BaseModel):
    dataset_id: str
    version: str
    purpose: Literal[
        "evaluation",
        "model_training",
        "prompt_evaluation",
        "safety_regression",
        "research"
    ]

    cohort_definition_id: str
    phenotype_versions: list[str]
    time_range: DateRange

    deidentification_policy_id: str
    consent_requirements: list[str]
    exclusion_rules: list[str]

    created_at: datetime
    created_by: str
    checksum: str

    status: Literal["draft", "approved", "frozen", "deprecated"]
```

### 11.1 每个数据集必须回答

- 数据来自哪里；
- 采集时间；
- 包含哪些人群；
- 排除了哪些人群；
- 标签如何产生；
- 使用哪个表型版本；
- 是否存在选择偏差；
- 是否允许训练；
- 是否只允许评估；
- 是否能导出；
- 何时应失效。

---

## 12. Cohort Builder

```python
class CohortDefinition(BaseModel):
    cohort_id: str
    name: str
    version: str

    inclusion_criteria: list[FilterRule]
    exclusion_criteria: list[FilterRule]

    index_event: str | None
    observation_window: TimeWindow
    outcome_window: TimeWindow | None

    required_data_domains: list[str]
    phenotype_versions: list[str]
```

首批用途：

- 成人呼吸道主诉病例；
- 高危红旗病例；
- 工具失败病例；
- 人工审核病例；
- 随访加重病例；
- 模型与医生不一致病例；
- 特定年龄和合并症子组。

---

## 13. Research Workspace

生产和研究环境必须逻辑隔离。

```text
Production Clinical System
        │
        │ 授权 + 脱敏 + 数据集冻结
        ▼
Dataset Export Pipeline
        │
        ▼
Research Workspace
- 只访问批准的数据集版本；
- 不能直接回写生产 CDP；
- 训练和评估任务有审计；
- 输出模型先进入 Registry；
- 经过门禁才能进入影子模式。
```

### 13.1 研究工作区最小能力

- 数据字典；
- 队列预览；
- 数据质量统计；
- 缺失分布；
- 人群分布；
- 时间切分；
- 标签分布；
- 子组评估；
- 训练任务追踪；
- 模型评估报告；
- 数据和代码版本绑定。

### 13.2 禁止

- 研究脚本直接连接生产数据库；
- 未记录数据版本的实验；
- 将测试集反复用于调参；
- 未经审核将研究模型发布到患者流量；
- 将模型输出直接升级为长期患者事实。

---

## 14. 数据授权与用途治理

建议定义：

```python
class ConsentScope(BaseModel):
    consent_scope_id: str
    patient_id: str

    allowed_purposes: list[Literal[
        "care_delivery",
        "care_coordination",
        "quality_improvement",
        "evaluation",
        "research",
        "model_training"
    ]]

    allowed_data_domains: list[str]
    allowed_organizations: list[str]

    effective_at: datetime
    expires_at: datetime | None
    revoked_at: datetime | None
```

所有 SourceArtifact、Encounter、DatasetVersion 都应关联授权范围。

---

# Part B：Evidence Intelligence Service

## 15. 服务定位

Evidence Intelligence Service 是一个受治理的循证医学服务，目标不是自动替医生做治疗决策，而是：

- 为 Clinical Intelligence 提供医学依据；
- 为医生审核提供快速证据检索；
- 为关键结论提供可验证引用；
- 识别来源、人群和地区不匹配；
- 显示指南与研究之间的冲突；
- 在证据不足时明确拒绝生成确定性结论。

---

## 16. Evidence Service 与其他模块的边界

### 16.1 Diagnostic Inference Engine

负责：

- 疾病候选；
- 支持与反对证据；
- 问题价值；
- 不确定性；
- 是否继续问诊。

不负责：

- 从互联网任意搜索；
- 判断论文版权许可；
- 组织完整指南证据包；
- 承担患者版自然语言表达。

### 16.2 Evidence Intelligence Service

负责：

- 临床问题结构化；
- 检索白名单来源；
- 来源分级；
- 证据摘要；
- 人群适配；
- 引用校验；
- 冲突识别；
- 证据更新。

不负责：

- 直接写入最终诊断；
- 自动批准治疗动作；
- 根据单篇研究修改分诊等级；
- 绕过 Safety Engine。

### 16.3 Response Composer

负责：

- 将结构化证据转成医生可读摘要；
- 将批准的结果转成患者表达；
- 保留限制、不确定性和来源。

---

## 17. 支持的临床问题类型

```python
class ClinicalQuestion(BaseModel):
    question_id: str
    question_type: Literal[
        "diagnosis",
        "differential",
        "triage",
        "test_selection",
        "treatment",
        "prognosis",
        "follow_up",
        "patient_education"
    ]

    raw_question: str
    normalized_question: str

    patient_context: PatientContext
    pico: PICO | None
    requested_by: Literal["agent", "clinician"]

    risk_level: str
    allowed_source_tiers: list[str]
```

---

## 18. PICO 结构化

```python
class PICO(BaseModel):
    population: PopulationDescriptor
    intervention: str | None
    comparator: str | None
    outcomes: list[str]

    setting: str | None
    timeframe: str | None
    region: str | None
```

PICO 不适用于所有问题。

例如：

- 分诊红旗问题更适合规则和指南检索；
- 疾病定义可使用 Condition/Population 结构；
- 治疗和检查选择更适合 PICO；
- 患者教育可能只需要经审阅的权威内容。

因此 Question Parser 必须先决定问题类型，再选择检索模板。

---

## 19. 医学来源模型

```python
class MedicalSource(BaseModel):
    source_id: str
    source_type: Literal[
        "guideline",
        "regulatory_document",
        "systematic_review",
        "meta_analysis",
        "randomized_trial",
        "cohort_study",
        "expert_consensus",
        "clinical_reference",
        "patient_education"
    ]

    title: str
    publisher: str
    authors: list[str]

    publication_date: date
    updated_at: date | None
    guideline_version: str | None

    region: str | None
    language: str

    population: PopulationDescriptor | None
    intervention: str | None
    comparator: str | None
    outcomes: list[str]

    evidence_level: str
    source_tier: str

    access_type: Literal[
        "public_full_text",
        "licensed_full_text",
        "abstract_only",
        "metadata_only"
    ]

    valid_from: date | None
    valid_until: date | None

    ingestion_version: str
    checksum: str
```

---

## 20. 来源分级

### Tier 1：优先临床依据

- 适用地区的官方临床指南；
- 国家级卫生机构；
- WHO、NICE、CDC 等权威机构；
- 监管机构发布的安全信息；
- 项目临床委员会批准的指南。

### Tier 2：高等级研究证据

- 系统综述；
- Meta 分析；
- 高质量随机对照试验；
- 权威同行评审期刊中的关键证据。

### Tier 3：补充证据

- 队列研究；
- 病例对照研究；
- 专家共识；
- 经过审核的临床参考内容。

### Tier 4：仅用于提示研究方向

- 单篇观察研究；
- 病例报告；
- 预印本；
- 尚未验证的新研究结论。

### 禁止来源

- 无作者健康博客；
- SEO 医疗页面；
- 无法确认版本的转载；
- 未经审阅的社交媒体内容；
- 来源不明的模型生成文本；
- 无授权的受限全文抓取内容。

---

## 21. Evidence Retrieval Pipeline

```text
Clinical Question
      │
      ▼
Question Classification
      │
      ▼
PICO / Query Plan
      │
      ▼
Capability & Source Policy
      │
      ▼
Whitelist Retrieval
      │
      ▼
Deduplication & Version Resolution
      │
      ▼
Source Tier Ranking
      │
      ▼
Population / Region / Time Matching
      │
      ▼
Claim Extraction
      │
      ▼
Citation Validation
      │
      ▼
Conflict Detection
      │
      ▼
EvidencePack
```

---

## 22. EvidencePack

```python
class EvidencePack(BaseModel):
    evidence_pack_id: str
    question_id: str

    claims: list[EvidenceClaim]
    sources: list[MedicalSource]

    source_coverage: float
    citation_precision: float
    population_match: float
    region_match: float
    freshness_score: float

    conflicts: list[EvidenceConflict]
    limitations: list[str]

    status: Literal[
        "supported",
        "partially_supported",
        "conflicting",
        "insufficient_evidence",
        "population_mismatch",
        "out_of_scope"
    ]

    generated_at: datetime
    knowledge_version: str
```

---

## 23. 结论级引用

```python
class EvidenceClaim(BaseModel):
    claim_id: str
    claim_text: str

    claim_type: Literal[
        "recommendation",
        "risk_statement",
        "diagnostic_statement",
        "test_statement",
        "treatment_statement",
        "limitation"
    ]

    source_spans: list[SourceSpan]
    support_level: Literal[
        "direct",
        "indirect",
        "mixed",
        "unsupported"
    ]

    applicability: ApplicabilityAssessment
    confidence: float
    requires_clinician_review: bool
```

医生点击结论时应能看到：

- 支持该结论的来源；
- 来源中的对应段落或推荐条目；
- 来源适用人群；
- 系统做出的推断；
- 是否存在反对证据；
- 是否超出了来源原意。

---

## 24. Citation Validator

引用校验至少包含：

### 24.1 存在性

- 来源是否真实存在；
- 版本是否可确认；
- 文档是否已撤回或废弃。

### 24.2 支持性

- 来源是否直接支持 Claim；
- 是否只是讨论了相邻主题；
- 是否将“可能”改写成“推荐”；
- 是否将相关性改写成因果性。

### 24.3 位置准确性

- 引用条目、页码或章节是否正确；
- 是否断章取义；
- 是否遗漏关键限制。

### 24.4 适用性

- 患者年龄；
- 性别；
- 妊娠状态；
- 合并症；
- 疾病阶段；
- 地区；
- 医疗环境；
- 干预剂量或检查条件。

---

## 25. Applicability Evaluator

```python
class ApplicabilityAssessment(BaseModel):
    population_match: float
    region_match: float
    setting_match: float
    intervention_match: float
    outcome_match: float

    mismatches: list[str]
    excluded_population_flags: list[str]

    decision: Literal[
        "applicable",
        "partially_applicable",
        "not_applicable",
        "unknown"
    ]
```

一篇研究即使质量很高，也可能不适用于当前患者。

例如：

- 研究只包含成年人，患者为儿童；
- 研究排除了孕妇；
- 研究在住院重症患者中进行；
- 研究剂量与当前地区临床规范不同；
- 研究结局与当前问题无关。

---

## 26. Evidence Conflict

```python
class EvidenceConflict(BaseModel):
    conflict_id: str
    topic: str

    source_ids: list[str]
    conflict_type: Literal[
        "guideline_disagreement",
        "version_conflict",
        "population_difference",
        "region_difference",
        "evidence_level_difference",
        "recommendation_strength_difference"
    ]

    explanation: str
    resolution_policy: str
    automatically_resolved: bool
    requires_clinician_review: bool
```

### 26.1 默认冲突处理原则

1. 优先当前适用地区的正式指南；
2. 优先有效的新版本；
3. 优先更匹配患者人群的来源；
4. 优先更高证据等级；
5. 不隐藏无法解决的冲突；
6. 治疗和高风险建议存在冲突时进入医生审核；
7. 不让 LLM 通过语言润色把冲突伪装成一致。

---

## 27. Evidence Service 的安全边界

### 27.1 可以自动执行

- 在白名单指南中检索；
- 提取推荐条目；
- 检查版本和生效时间；
- 计算人群匹配度；
- 标记证据冲突；
- 为医生生成证据摘要；
- 为已有患者结论附加来源。

### 27.2 需要医生审核

- 治疗建议；
- 药物选择；
- 剂量或停药；
- 高风险检查或处置；
- 指南冲突；
- 人群适配不明确；
- 仅有低等级研究支持；
- 证据结论将改变原有医疗计划。

### 27.3 禁止

- 将单篇研究自动升级为标准治疗；
- 使用未经授权全文；
- 编造引用；
- 没有来源时生成确定性医学事实；
- 绕过 Capability 和 Safety Policy；
- 将研究数据直接写为患者事实。

---

## 28. 医生 Evidence Copilot

医生审核台新增 `Ask Evidence`。

### 28.1 支持的问题

- 当前 DDx 是否遗漏重要方向；
- 某条建议有哪些指南依据；
- 某项检查是否适合当前患者；
- 当前人群是否被相关指南覆盖；
- 支持和反对某个候选诊断的证据是什么；
- 不同指南为什么存在差异；
- 某条来源是否已经过期；
- 当前结论有哪些限制。

### 28.2 工作台展示

- 临床问题；
- PICO；
- EvidencePack 状态；
- 关键 Claim；
- 来源等级；
- 引用原文位置；
- 人群适配；
- 指南冲突；
- 更新日期；
- 医生评价：有用、无用、错误、过期、不可适用。

### 28.3 医生反馈用途

医生反馈可以进入：

- 检索回归集；
- Citation Validator 评估集；
- 来源排序训练数据；
- 错误分析；
- 知识更新任务。

反馈不能未经审核直接修改生产医学知识。

---

## 29. 面向患者的证据表达

患者版不应显示复杂论文列表，而应：

- 使用通俗语言；
- 只展示已批准结论；
- 区分“指南建议”“研究提示”和“当前信息不足”；
- 不把研究概率直接转化为个人风险；
- 不使用单篇研究生成治疗方案；
- 明确需要医生判断的部分。

示例：

```text
根据当前收集的信息和适用的临床指南，出现持续呼吸困难时需要尽快线下评估。
目前信息不足以确定具体原因，建议不要仅依靠在线问诊继续观察。
```

而不是：

```text
研究表明你有 73% 的概率患有某疾病。
```

---

## 30. Evidence Intelligence MVP

首个版本不建设全医学搜索引擎。

### 30.1 范围

只覆盖初始 Capability：

> 成人常见呼吸道症状风险分层、信息采集和就医导航。

### 30.2 来源

仅使用：

- 项目临床顾问批准的白名单指南；
- 可合法使用的公开权威来源；
- 版本明确的规则文档；
- 少量经过人工审核的系统综述或研究摘要。

### 30.3 用户

第一版主要面向：

- Agent 内部证据检索；
- 医生审核工作台；
- 开发和测试人员的回归验证。

不直接开放：

- 面向患者的自由医学论文问答；
- 自动治疗推荐；
- 任意互联网搜索；
- 无限制的开放式医学研究总结。

### 30.4 MVP 输出

```python
class EvidenceMVPResult(BaseModel):
    question: str
    status: str
    guideline_claims: list[EvidenceClaim]
    source_ids: list[str]
    applicability: ApplicabilityAssessment
    conflicts: list[EvidenceConflict]
    limitations: list[str]
```

---

# Part C：与现有重构阶段的整合

## 31. Phase 0 增量任务

在主方案 Phase 0 中增加：

1. 盘点当前 CDP 字段中哪些属于长期事实、Encounter 状态和运行状态；
2. 盘点现有知识库和文档来源；
3. 标记无法确认来源、版本和授权状态的知识内容；
4. 禁止 README 或文档宣称当前已经具备完整循证能力；
5. 为首个 Capability 确认临床顾问和白名单指南；
6. 建立首批结论级引用测试样例。

### 验收标准

- 当前所有医学知识来源可列出；
- 不明来源内容不进入生产白名单；
- CDP 字段职责完成分类；
- 首批指南来源、版本和适用人群已确认；
- 至少 20 条 Claim-Citation 测试样例可运行。

---

## 32. Phase 1 增量任务

主方案 Phase 1 扩展为：

### 32.1 Clinical Data Contracts

- PatientLongitudinalRecord；
- Encounter；
- EncounterCDP；
- ClinicalObservation；
- SourceArtifact；
- PromotionDecision；
- PhenotypeDefinition；
- ConsentScope。

### 32.2 Evidence Contracts

- ClinicalQuestion；
- PICO；
- MedicalSource；
- EvidenceClaim；
- SourceSpan；
- ApplicabilityAssessment；
- EvidenceConflict；
- EvidencePack。

### 32.3 数据迁移

- 保留当前 CDP 兼容视图；
- 新增 Encounter 和 Observation 表；
- 旧 JSON 字段逐步映射；
- 不一次性删除旧结构；
- 建立双写和一致性检查。

### 32.4 验收标准

- Java/Python Schema 一致；
- Observation 必须关联 source_type；
- 模型推断不能写入 confirmed；
- SourceArtifact 能关联原始数据；
- EvidenceClaim 必须关联 source span；
- 所有 Schema 具备版本字段。

---

## 33. Phase 2 增量任务

主方案 Phase 2 的 Clinical Intelligence MVP 增加 Evidence Intelligence MVP。

### 33.1 数据核心

- Observation 写入规则；
- Promotion Policy；
- 来源可靠度；
- 表型 Registry 最小实现；
- 首个 Capability 的 PhenotypeDefinitions。

### 33.2 循证核心

- Clinical Question Parser；
- PICO Builder；
- 白名单 Source Registry；
- 指南版本解析；
- 关键词与语义混合检索；
- Claim 提取；
- Citation Validator；
- Applicability Evaluator；
- Conflict Detector。

### 33.3 验收标准

- 相同问题和知识版本产生稳定结构化结果；
- 每条关键 Claim 可定位来源；
- 无来源时返回 insufficient evidence；
- 已过期指南能够被识别；
- 儿童研究不能自动应用于成人以外人群；
- 引用不支持 Claim 时测试失败；
- Evidence Service 不直接修改 CDP 最终诊断。

---

## 34. Phase 3 增量任务

LangGraph Runtime 增加以下节点或工具：

```text
build_clinical_question
retrieve_evidence
validate_citations
evaluate_applicability
detect_evidence_conflict
attach_evidence_pack
```

### 路由原则

- 安全分诊规则不依赖 Evidence Service 在线可用；
- Evidence Service 失败不能阻塞紧急升级；
- 治疗证据冲突进入 Human Review；
- 低风险患者教育可以在证据充分时继续；
- EvidencePack 只能通过 State Committer 关联到 CDP；
- 证据结果必须记录 knowledge_version。

---

## 35. Phase 4 增量任务

医生接管阶段增加 Evidence Copilot。

### 任务

1. 医生提出临床问题；
2. 查看 PICO；
3. 查看 Claim 与来源；
4. 查看人群适配；
5. 查看冲突；
6. 标记引用错误；
7. 将认可证据附加到审核意见；
8. 记录证据是否改变医生决策；
9. 将反馈进入离线评估集。

### 验收标准

- 医生可追踪每条关键结论；
- 不适用来源有明显标记；
- 冲突不会被隐藏；
- Evidence Service 不可用时医生仍能完成审核；
- 所有查询和查看行为可审计。

---

## 36. Phase 5 增量任务

### 36.1 Data & Research Foundation

- Dataset Registry；
- Cohort Builder；
- 脱敏导出；
- 时间切分；
- 子组评估；
- 数据漂移；
- 表型版本回归；
- 研究工作区权限；
- 实验追踪。

### 36.2 Evidence Evaluation

- Retrieval Recall@k；
- 指南覆盖率；
- 来源 Tier 分布；
- Citation Precision；
- Citation Support Rate；
- Population Match；
- Freshness；
- Conflict Recall；
- 关键限制遗漏率；
- 医生有用性评价。

### 36.3 验收标准

- 每次知识更新生成回归报告；
- 引用支持率低于门槛不能发布；
- 过期指南不能作为唯一高风险依据；
- 训练、验证和测试数据集版本明确；
- 子组指标可比较；
- 生产数据不会被研究代码直接访问。

---

## 37. Phase 6 增量任务

### Stage 0：离线

- 只使用白名单指南；
- 临床专家逐条审核 EvidencePack；
- 建立 Claim-Citation 金标准。

### Stage 1：影子模式

- 在真实医生问题旁路运行；
- 不向医生展示；
- 比较实际使用来源和系统检索结果。

### Stage 2：医生辅助

- Evidence Copilot 面向医生开放；
- 医生承担全部决策；
- 记录采用、修改和拒绝率。

### Stage 3：受限患者表达

- 只展示经过批准的指南级结论；
- 不开放自由治疗问答；
- 高风险结论仍由医生审核。

### Stage 4：逐项扩大

扩大前需要：

- 新来源授权；
- 新 Capability；
- 新人群评估；
- 新地区适配；
- 新 Citation 回归集；
- 临床委员会批准。

---

# Part D：仓库结构调整

## 38. 推荐新增目录

```text
AIdoctor/
├── apps/
│   ├── business-api/
│   ├── agent-runtime/
│   ├── clinician-console/
│   └── research-console/
│
├── packages/
│   ├── clinical-domain/
│   ├── evidence-ledger/
│   ├── phenotype-registry/
│   ├── dataset-registry/
│   ├── consent-policy/
│   ├── clinical-intelligence/
│   ├── evidence-intelligence/
│   │   ├── question-parser/
│   │   ├── pico-builder/
│   │   ├── source-registry/
│   │   ├── retrieval/
│   │   ├── citation-validator/
│   │   ├── applicability/
│   │   └── conflict-detector/
│   ├── safety-engine/
│   ├── tool-sdk/
│   └── observability/
│
├── knowledge/
│   ├── source-manifests/
│   ├── guideline-registry/
│   ├── capability-policies/
│   └── phenotype-definitions/
│
├── datasets/
│   ├── manifests/
│   ├── cohort-definitions/
│   └── data-dictionaries/
│
├── evals/
│   ├── clinical-cases/
│   ├── claim-citation/
│   ├── retrieval/
│   ├── applicability/
│   ├── conflict-cases/
│   ├── subgroup-analysis/
│   └── longitudinal-cases/
│
└── docs/
    ├── refactoring/
    ├── data-governance/
    ├── evidence-governance/
    └── adr/
```

初期可以在同一个 FastAPI 服务内按 package 模块化实现，不需要立即拆成新的微服务。

---

## 39. 数据库建议

### 39.1 业务和临床状态

推荐 PostgreSQL 或现有生产主库。

核心表：

- patient；
- patient_longitudinal_state；
- encounter；
- cdp；
- clinical_observation；
- source_artifact；
- diagnostic_hypothesis；
- triage_assessment；
- phenotype_definition；
- phenotype_instance；
- review_task；
- follow_up_task。

### 39.2 循证知识

核心表：

- medical_source；
- source_version；
- source_section；
- evidence_claim；
- source_span；
- evidence_pack；
- evidence_conflict；
- knowledge_release。

向量索引可先使用 pgvector；只有规模和吞吐证明需要时再引入独立向量数据库。

### 39.3 研究治理

- dataset_version；
- cohort_definition；
- data_export_job；
- evaluation_run；
- model_registry；
- prompt_registry；
- knowledge_registry。

---

## 40. API 建议

### 40.1 Evidence API

```text
POST /api/v1/evidence/questions
POST /api/v1/evidence/search
GET  /api/v1/evidence/packs/{evidencePackId}
POST /api/v1/evidence/validate-citations
POST /api/v1/evidence/evaluate-applicability
GET  /api/v1/evidence/sources/{sourceId}
GET  /api/v1/evidence/sources/{sourceId}/versions
POST /api/v1/evidence/feedback
```

### 40.2 Data Foundation API

```text
POST /api/v1/encounters
GET  /api/v1/encounters/{encounterId}
GET  /api/v1/patients/{patientId}/longitudinal-state
POST /api/v1/observations
POST /api/v1/observations/{observationId}/confirm
POST /api/v1/observations/{observationId}/supersede
GET  /api/v1/phenotypes/{phenotypeId}/versions
POST /api/v1/datasets
POST /api/v1/cohorts/preview
```

所有接口都要执行：

- 身份认证；
- 机构和患者权限；
- consent scope；
- 审计；
- 版本检查；
- 幂等控制。

---

# Part E：评估体系

## 41. 数据评估指标

- 字段缺失率；
- SourceArtifact 关联率；
- Observation 来源覆盖率；
- 冲突证据识别率；
- 长期状态错误提升率；
- 表型一致性；
- 标签噪声；
- 人群分布；
- 时间漂移；
- 机构分布；
- Consent 覆盖率。

---

## 42. Evidence Retrieval 指标

- Recall@k；
- Precision@k；
- 指南 Top-k 命中率；
- 来源 Tier 1 覆盖率；
- 最新有效版本命中率；
- 重复来源率；
- 失效来源召回率；
- 平均检索延迟。

---

## 43. Citation 指标

- Citation Existence Rate；
- Citation Support Rate；
- Source Span Accuracy；
- Claim Overstatement Rate；
- Limitation Omission Rate；
- Unsupported Claim Rate；
- 引用版本正确率。

---

## 44. Applicability 指标

- 人群匹配准确率；
- 特殊人群排除召回率；
- 地区不匹配识别率；
- 干预剂量不匹配识别率；
- 临床环境不匹配识别率；
- 医生对适用性判断的一致率。

---

## 45. Conflict 指标

- 指南冲突召回率；
- 版本冲突识别率；
- 错误自动消解率；
- 需要审核但未升级率；
- 冲突解释清晰度。

---

## 46. 医生工作流指标

- Evidence Copilot 使用率；
- 平均检索时间；
- 医生采用率；
- 修改率；
- 拒绝率；
- 引用错误反馈率；
- 是否改变临床计划；
- 是否减少查阅时间；
- 医生主观有用性。

---

# Part F：首批实施任务

## 47. PR-A：数据职责盘点和 Schema 草案

### 范围

- 盘点当前 CDP；
- 定义 PatientLongitudinalRecord；
- 定义 Encounter；
- 定义 ClinicalObservation；
- 定义 SourceArtifact；
- 定义 PromotionDecision；
- 输出 JSON Schema 草案。

### 不包含

- 数据库正式迁移；
- LangGraph；
- 文献检索；
- 前端改造。

### 验收

- 当前 CDP 字段全部映射；
- 长期状态和 Encounter 状态无重叠职责；
- 模型推断不能进入 confirmed；
- 示例数据可序列化。

---

## 48. PR-B：Evidence Source Registry

### 范围

- MedicalSource Schema；
- Source Version；
- 白名单 Manifest；
- 来源 Tier；
- 生效和失效时间；
- Capability 关联；
- 首批呼吸道指南 Manifest。

### 验收

- 所有来源有版本；
- 所有来源有许可状态；
- 所有来源有适用地区或明确 unknown；
- 失效来源不被默认使用。

---

## 49. PR-C：Claim-Citation 验证原型

### 范围

- EvidenceClaim；
- SourceSpan；
- Citation Validator；
- 20～50 条金标准测试；
- unsupported claim 状态。

### 验收

- 引用不存在时失败；
- 来源不支持 Claim 时失败；
- 关键限制遗漏时能够标记；
- 测试结果可生成报告。

---

## 50. PR-D：Evidence Intelligence MVP

### 范围

- Question Parser；
- 简化 PICO；
- 白名单检索；
- Source Ranking；
- EvidencePack；
- 医生端 API。

### 验收

- 只检索白名单来源；
- 所有 Claim 有引用；
- 能返回 insufficient evidence；
- 能标记人群不匹配；
- 不直接修改 CDP。

---

## 51. PR-E：Longitudinal Promotion Policy

### 范围

- Encounter 结束时生成 PromotionDecision；
- 自动提升白名单；
- 医生审核路径；
- 长期状态版本；
- 冲突拦截。

### 验收

- 疾病候选不会自动变为确诊；
- 医生确认可产生新长期状态版本；
- 撤销和修订可追踪；
- 所有提升操作有审计。

---

# Part G：ADR 增量

## 52. 新增 ADR

1. ADR-014：Longitudinal Patient Record 与 Encounter CDP 分离；
2. ADR-015：ClinicalObservation 采用追加与 supersede，而不是覆盖更新；
3. ADR-016：模型推断不得自动提升为确认事实；
4. ADR-017：PhenotypeDefinition 必须版本化；
5. ADR-018：生产数据与 Research Workspace 隔离；
6. ADR-019：Evidence Intelligence 独立于 Diagnostic Inference；
7. ADR-020：引用采用 Claim-Level Binding；
8. ADR-021：仅允许白名单和授权来源进入生产；
9. ADR-022：治疗证据冲突必须人工审核；
10. ADR-023：首版 Evidence MVP 只服务单一 Capability。

---

# Part H：明确不做

## 53. 当前阶段不建设

- 通用医学知识搜索引擎；
- 覆盖所有疾病和人群的表型库；
- 基因和多组学诊断；
- 自建大规模医学基础模型；
- 未经授权的医学全文抓取；
- 将预印本作为患者治疗依据；
- 自动处方和剂量修改；
- 研究队列风险模型直接在线分诊；
- 允许模型自行决定数据可否用于训练；
- 允许 Evidence Service 绕过医生和安全引擎。

---

## 54. 实施警告

这部分设计很容易造成新的过度工程化。

需要坚持：

1. 先完成首个 Capability；
2. 先使用少量白名单指南；
3. 先建立结构化 EvidencePack；
4. 先服务医生审核；
5. 先做 20～50 条高质量引用测试；
6. 不先建设全量文献平台；
7. 不先建设复杂数据湖；
8. 不先引入基因、影像和多组学；
9. 不因为 UK Biobank 数据规模大，就模仿其数据规模；
10. 不因为 OpenEvidence 使用体验好，就忽略内容授权和临床验证。

---

## 55. 最终目标

加入本扩展后，AIdoctor 的完整目标不再只是：

> 一个能够多轮问诊、调用工具并生成诊断建议的 Agent。

而是：

> 一个以纵向患者记录和临床证据账本为数据基础，以结构化临床推理和不可绕过的安全分诊为决策核心，以循证医学服务为知识支持，以 LangGraph 为可恢复执行运行时，以医生审核为高风险决策边界，并能够连接就医导航、随访、研究评估和分阶段验证的受约束临床决策支持平台。

该目标仍然必须通过窄 Capability、清晰数据边界、可验证证据、人工审核和逐阶段放量逐步实现，而不是一次性建设全部平台能力。
