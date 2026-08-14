# AIdoctor vNext 临床 Capability 扩展设计

> 文档状态：`FUTURE_EXTENSION_DESIGN`  
> 设计来源：Pregnancy Progress 等垂直健康产品模式观察，以及当前 Capability Package 的未来演进需求  
> 当前 Runtime 影响：`NONE`  
> Shared Contracts v1 影响：`NONE`  
> 当前 Capability 影响：`NONE`  
> 实现授权：`NOT_AUTHORIZED`

---

## 1. 目的

本设计用于回答一个未来问题：

> 当 AIdoctor 从 `adult_respiratory_v1` 扩展到妊娠、慢病、中医四诊、多模态设备等新的临床/健康场景时，应该如何扩展，而不是重新创建一套 Agent、状态模型和运行时？

当前答案保持不变：

```text
Stable Platform
+ Versioned Capability Package
= New Supported Clinical Scenario
```

Future Extension 的目标不是改变这一原则，而是补充不同类型 Capability 的设计模板。

---

## 2. 外部产品带来的核心启发

Pregnancy Progress 一类产品揭示了一个重要产品结构：

```text
医疗健康能力
不一定按“疾病”组织
也可以按：
人群 + 时间阶段 + 当前状态 + 特定风险边界
```

因此，未来 Capability 不应只支持：

```text
disease_or_symptom_capability
```

还应能表达：

```text
special_population_capability
temporal_stage_capability
chronic_management_capability
multimodal_assessment_capability
```

这些都是 Capability 模板变化，而不是新的 Agent Runtime。

---

## 3. Capability 类型扩展

以下分类为设计层概念，不要求立即写入当前 `CapabilityManifest`。

### 3.1 Symptom / Domain Capability

当前 `adult_respiratory_v1` 属于此类。

```text
主诉 / 症状域
→ 风险分层
→ 信息采集
→ 有限候选
→ Evidence
→ Delivery
```

### 3.2 Special Population Capability

例如未来：

```text
pregnancy_health_v1
older_adult_health_v1
pediatric_xxx_v1
```

特点：

- 人群本身改变适用规则；
- Safety 阈值和升级条件不同；
- 可用药物、检查和建议边界不同；
- Provider / Tool / Knowledge 都需要按人群过滤。

### 3.3 Temporal / Stage-based Capability

例如妊娠、术后恢复、康复、长期治疗周期。

```text
Patient State
+ Stage / Time Window
+ Stage-specific Policy
= Current Capability Behavior
```

需要支持：

- stage definition；
- stage transition；
- time-relative observations；
- stage-specific safety；
- stage-specific knowledge；
- follow-up schedule；
- expired recommendation handling。

### 3.4 Chronic Management Capability

例如未来慢病管理：

```text
hypertension_followup_v1
diabetes_management_v1
```

重点不同于一次性问诊：

- Longitudinal Record 优先；
- trend / adherence / follow-up；
- repeated measurements；
- care plan version；
- escalation based on trend；
- clinician collaboration。

### 3.5 Multimodal Assessment Capability

例如未来：

```text
tcm_four_diagnosis_v1
```

可能组合：

```text
问诊文本
+ 舌象
+ 脉象
+ 其他结构化测量
```

但仍必须遵守：

```text
Multimodal Input
→ SourceArtifact / ToolResult
→ ObservationCandidate
→ State Committer
→ Clinical State
```

而不是多模态模型直接生成正式 Clinical State。

---

## 4. Capability 不应演化成“一个场景一个 Agent”

禁止未来形成：

```text
RespiratoryAgent
PregnancyAgent
PulseAgent
TongueAgent
DrugAgent
...
```

如果每新增场景都创建独立 Planner、独立 Memory、独立 State 和独立 Tool 调用路径，最终会重新回到不可治理的多 Agent 系统。

推荐：

```text
One Stable Runtime
+ Capability-specific Policies / Knowledge / Tools / Evals
```

只有在未来出现真正不同的执行语义且无法通过现有 Runtime 表达时，才通过 ADR 评估是否新增 Runtime 类型。

---

## 5. vNext Capability 结构候选

当前 Capability Package 已包含 Population、Scope、Observation、Safety、Question、Hypothesis、Knowledge、Runtime、Delivery 和 Eval。

未来可以在保持兼容的前提下考虑增加以下可选概念。

### 5.1 TemporalPolicy

```python
class TemporalPolicy(BaseModel):
    stage_model_id: str
    stage_source: str
    allowed_stages: list[str]
    transition_rules: list[str]
    stage_expiry_rules: list[str]
    followup_policy_id: str | None
```

### 5.2 DataSourcePolicy

定义该 Capability 允许读取哪些外部患者数据和设备来源。

```python
class DataSourcePolicy(BaseModel):
    allowed_connector_ids: list[str]
    allowed_artifact_types: list[str]
    required_quality_policies: list[str]
    maximum_data_age: dict[str, str]
    consent_requirements: list[str]
```

### 5.3 StructuredProviderPolicy

```python
class StructuredProviderPolicy(BaseModel):
    allowed_provider_ids: list[str]
    allowed_operations: list[str]
    jurisdiction_policy_id: str | None
    freshness_policy_id: str | None
```

### 5.4 MultimodalPolicy

```python
class MultimodalPolicy(BaseModel):
    required_modalities: list[str]
    optional_modalities: list[str]
    minimum_quality_by_modality: dict[str, float]
    fusion_policy_id: str
    missing_modality_behavior: str
```

上述类型都是未来 Candidate，不应在当前 Contracts v1 中提前落地。

---

## 6. Pregnancy 类 Capability 设计示例

本节只作为产品结构示例，不授权妊娠诊疗实现。

```text
pregnancy_health_v1
├── population
│   └── pregnancy-specific inclusion / exclusion
├── temporal
│   ├── gestational age
│   └── trimester / stage
├── observations
│   ├── current symptoms
│   ├── pregnancy history
│   └── relevant measurements
├── safety
│   └── stage-specific red flags
├── questions
│   └── stage-specific collection
├── knowledge
│   └── pregnancy-specific approved sources
├── tools
│   └── only approved low-risk tools
├── delivery
│   └── conservative patient-facing output
└── evals
    ├── stage transition
    ├── special population safety
    └── out-of-scope escalation
```

重点是：

> “阶段”成为 Capability Context 的一部分，而不是写进 Prompt 后由 LLM 自行理解。

---

## 7. `tcm_four_diagnosis_v1` 未来候选

结合未来可能接入的数字脉诊和舌象分析，可形成一个独立的研究/临床辅助 Capability Candidate。

### 7.1 初始定位

不建议一开始定义成“自动中医诊断”。

更合理的首版定位：

```text
Multimodal TCM Observation Collection
+ Structured Feature Presentation
+ Evidence-supported Interpretation
+ Clinician Assist
```

### 7.2 输入

```text
望：舌象等图像特征
闻：未来语音/声音等候选数据
问：结构化问诊
切：脉搏波与脉象候选特征
```

每个模态都必须先变成独立、有来源、有质量状态的 ObservationCandidate。

### 7.3 不能做的事

首版不应：

- 因缺失某个模态而让模型自动补齐；
- 用一张舌图直接推导完整证型；
- 用脉搏波算法输出直接覆盖问诊事实；
- 把未经临床验证的模型输出写入长期确诊记录；
- 用知识图谱路径替代来源证据。

### 7.4 推荐成熟路径

```text
RESEARCH
→ OFFLINE_EVALUATION
→ SHADOW
→ CLINICIAN_ASSIST
→ RESTRICTED_PATIENT（仅在证据充分时）
```

是否进入患者自动化路径应由真实评估和临床 Reviewer 决定，而不是项目完成度决定。

---

## 8. Capability 与外部 Connector 的关系

Connector 是平台能力，Capability 决定是否允许使用。

```text
Connector Registry
提供：系统当前有哪些数据源能力

Capability
决定：当前场景允许调用哪些 Connector / Tool
```

例如：

```yaml
allowed_connector_ids:
  - pulse-device-v1
  - tongue-imaging-v1
```

不能因为患者连接了设备就自动把所有设备数据送入每个问诊流程。

---

## 9. Capability 与 Structured Medical Provider 的关系

同理：

```text
Provider Registry
提供：系统有哪些 Drug / Regulatory / Device Provider

Capability
决定：允许哪些 Provider Operation
```

例如呼吸道首版不需要药物治疗能力时，可以完全不允许药物相互作用 Tool。

未来药物相关 Capability 即使允许 Drug Provider，也不表示允许自动处方。

---

## 10. Capability 与 RAG / KG 的关系

每个 Capability 继续拥有明确的 `Knowledge Release`。

未来即使增加 Connector 和 Structured Provider，也不改变：

```text
Capability
├── Knowledge Release
├── Tool Allowlist
├── Provider Policy
├── Connector Policy
└── Eval Suite
```

Medical RAG / KG / Structured Provider 的使用范围应由 Capability 显式控制。

---

## 11. Capability 生命周期增强

现有生命周期继续有效：

```text
DRAFT
→ CLINICAL_REVIEW
→ TECHNICAL_REVIEW
→ EVALUATION
→ SHADOW
→ CLINICIAN_ASSIST
→ RESTRICTED_PATIENT
→ ACTIVE
```

Future Extension 建议进一步强调：

- 新设备 Tool 可以独立处于 Shadow；
- 新 Provider 可以独立处于 Technical Review；
- Capability 只有在依赖版本均满足 Gate 时才能升级；
- Tool/Provider/Knowledge 的版本变化不自动升级 Capability。

---

## 12. Dependency Lock 候选

未来 Capability Release 应能锁定：

```text
capability_version
knowledge_release_id
prompt_release_ids
model_route_policy_ids
tool_versions
connector_versions
provider_versions
algorithm_versions
eval_suite_versions
```

这样一次 Encounter 可以明确回答：

> 当时到底使用了哪一版知识、工具、设备算法和模型。

---

## 13. 评估维度扩展

不同 Capability 类型需要不同专项 Eval。

### Temporal Capability

- stage calculation correctness；
- stage transition correctness；
- stale recommendation rejection；
- time-window boundary cases。

### Chronic Capability

- longitudinal trend correctness；
- missing data handling；
- follow-up escalation；
- repeated measurement deduplication。

### Multimodal Capability

- modality quality gate；
- missing modality behavior；
- cross-modal conflict handling；
- single-modality overclaim rate；
- provenance completeness。

### Special Population

- population matching；
- exclusion correctness；
- special safety recall；
- out-of-scope escalation。

---

## 14. 新 Capability 的标准决策流程

未来看到新的医疗插件、产品或场景时，不直接新增 Agent。

```text
1. 识别它解决的临床/健康场景
2. 判断是否已有对应 Capability Type
3. 判断当前 Stable Platform 是否能表达
4. 如能表达：新增 Capability Package
5. 如不能表达：记录 Platform Extension Candidate
6. 通过 ADR 决定是否扩展平台
7. 建立专项 Eval
8. Shadow / Clinician Assist
9. 再决定是否进入患者路径
```

---

## 15. 当前建议

当前阶段只保留以下未来 Candidate：

```text
pregnancy_health_v1
    作为 Temporal + Special Population Capability 设计参考

tcm_four_diagnosis_v1
    作为 Multimodal Capability 设计参考

chronic_disease_management_v1
    作为 Longitudinal / Chronic Capability 模板占位
```

它们均不进入当前实施路线，不创建实际 Capability 目录，不创建临床 Prompt，不创建规则资产。

---

## 16. 未来进入正式 Roadmap 的门禁

某 vNext Capability Candidate 进入实施前至少需要：

- 明确临床/健康目标；
- Population / Scope 确定；
- 临床 Owner 与 Reviewer；
- Source / Tool / Connector / Provider 依赖清单；
- Contract Impact Assessment；
- Safety Design；
- Eval Design；
- Data / Privacy Review；
- Implementation Authorization。

在这些条件满足前，本文件只作为未来设计输入。
