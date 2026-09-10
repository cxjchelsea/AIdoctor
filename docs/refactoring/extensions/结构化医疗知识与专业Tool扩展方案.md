# AIdoctor 结构化医疗知识与专业 Tool 扩展方案

> 文档状态：`FUTURE_EXTENSION_DESIGN`  
> 设计来源：DrugBank / Nyquist AI / Rhizome AI 等外部产品模式观察  
> 当前 Runtime 影响：`NONE`  
> Shared Contracts v1 影响：`NONE`  
> 当前 Capability 影响：`NONE`  
> 实现授权：`NOT_AUTHORIZED`

---

## 1. 目的

本方案用于补充 AIdoctor 当前 Medical RAG + Knowledge Graph 之外的第三类知识访问方式：

> **Structured Medical Provider / Professional Tool**

当前重构已经明确：

- Medical RAG 负责白名单医学证据检索；
- Knowledge Graph 只作为关系、术语和 Query Expansion 的可选增强；
- EvidencePack 负责 Claim-Level 来源、冲突和局限；
- Tool 调用受 Capability、Permission 和 Tool Governance 控制。

未来接入药物数据库、FDA/监管数据库、器械数据库时，不应把所有结构化数据先转成文档再做向量检索，也不应把所有数据全量复制进 Neo4j。

---

## 2. 三条医学知识访问路径

```text
Medical Knowledge / Evidence
│
├── A. Medical RAG
│   ├── Guideline
│   ├── Systematic Review
│   ├── Research
│   └── Patient Education
│
├── B. Knowledge Graph
│   ├── Terminology
│   ├── Concept Relation
│   ├── Query Expansion
│   └── Controlled Multi-hop
│
└── C. Structured Medical Provider
    ├── Drug Intelligence
    ├── Regulatory Intelligence
    ├── Device Intelligence
    └── Other Authoritative APIs / Databases
```

三者互补，不互相替代。

---

## 3. 哪类知识进入哪条路径

### 3.1 Medical RAG

适合：

- 指南正文；
- 推荐条款；
- 证据等级；
- 系统综述；
- 研究摘要/授权全文；
- 患者教育；
- 需要保留原始语境的长文本。

核心问题：

> “来源原文对这个 Claim 说了什么？”

### 3.2 Knowledge Graph

适合：

- 标准术语；
- 同义词；
- 症状—疾病关系；
- 疾病—检查关系；
- 概念代码映射；
- must-not-miss 扩展；
- Query Expansion；
- 经过来源治理的有限多跳。

核心问题：

> “这些医学概念之间是什么关系？”

### 3.3 Structured Medical Provider

适合：

- 药物实体解析；
- 药物相互作用；
- 药品属性；
- 监管状态；
- 医疗器械注册/批准/召回；
- 不良事件；
- 结构化 Guidance / Warning / Regulatory Record；
- 权威数据库中天然结构化、需要实时或准实时读取的信息。

核心问题：

> “权威结构化数据库当前返回什么事实？”

---

## 4. 为什么不能把 Structured Provider 全部做成 RAG

错误模式：

```text
结构化数据库
→ 导出文本
→ Chunk
→ Embedding
→ Vector Search
→ LLM 重新解析
```

问题包括：

- 丢失结构化字段和枚举；
- 复杂过滤变得不确定；
- 更新时间延迟；
- 版本难追踪；
- 精确查询被相似度检索替代；
- 相互作用、状态等本可确定查询的问题重新交给 LLM 猜测。

因此，未来应优先采用：

```text
Agent / Clinical Module
↓
Bounded Tool Call
↓
Structured Provider Adapter
↓
Validated Structured Result
↓
Evidence Normalization
↓
EvidencePack / Candidate
```

---

## 5. Provider 抽象

以下为 vNext Candidate，不修改当前 Contract。

### 5.1 MedicalProviderSpec

```python
class MedicalProviderSpec(BaseModel):
    provider_id: str
    provider_version: str
    provider_type: Literal[
        "drug_intelligence",
        "regulatory_intelligence",
        "device_intelligence",
        "reference_data",
    ]
    authority_level: str
    supported_regions: list[str]
    supported_operations: list[str]
    auth_mode: str
    freshness_policy_id: str
    license_policy_id: str
    release_status: str
```

### 5.2 Provider Result 原则

结果必须区分：

```text
provider fact
provider interpretation
AIdoctor derived interpretation
```

外部 Provider 返回的内容不能被系统静默包装成“医生结论”。

---

## 6. DrugBank 类模式：Drug Intelligence Tool

DrugBank 所代表的关键思想是：药物知识中大量问题天然适合结构化 Tool，而不是自由文本 RAG。

### 6.1 推荐能力拆分

未来可评估：

```text
resolve_medication
get_drug_profile
check_drug_interactions
get_drug_indications
get_drug_contraindications
get_drug_adverse_effects
get_drug_regulatory_status
```

不建议：

```text
ask_drugbank(question: string)
```

因为万能问答接口难以做 Capability Allowlist、参数校验、权限、测试和审计。

### 6.2 药物交互结果

结构化结果应尽量保留：

```text
drug identifiers
interaction pair
effect / description
severity
evidence level
management / action context
source / reference
provider version
retrieved_at
```

AIdoctor 在此基础上生成患者相关解释，但不得把 Provider 数据直接当处方动作授权。

### 6.3 与治疗能力的边界

即使未来 Drug Tool 可用：

```text
Drug Tool Available
≠
Automatic Prescription Enabled
```

是否允许治疗/处方相关动作仍由 Capability、Safety、Clinician Review 和业务法规边界控制。

---

## 7. Nyquist AI 类模式：Regulatory / Device Intelligence Tool

该类产品代表的关键思想是：

```text
多个监管数据库
→ 小而明确的只读 Tool
→ 带来源的结构化结果
```

### 7.1 推荐能力

未来可评估：

```text
search_medical_device
search_device_recall
search_device_adverse_event
search_regulatory_guidance
search_warning_letter
search_device_registration
search_clinical_trial
```

### 7.2 AIdoctor 中的主要位置

不建议默认放在每次患者问诊 hot path。

更适合：

```text
Knowledge Management
Admin / Research
Device Product Research
Regulatory Evidence
Clinician Assist（特定场景）
```

对于数字脉诊和舌诊设备项目，可用于未来：

- 同类器械研究；
- 注册/审批信息研究；
- 召回和不良事件研究；
- Guidance 检索；
- 产品边界与风险分析。

---

## 8. Rhizome AI 类模式：多权威数据源统一治理

此类产品的主要借鉴点不是搜索 UI，而是：

```text
多个异构 Authority Sources
→ Source Adapter
→ Unified Metadata
→ Search / Filter
→ Provenance
→ Evidence
```

这与当前 AIdoctor 的 Source Registry / Knowledge Release 思路兼容。

未来如果结构化 Provider 数量增加，应避免每个 Provider 都产生一套独立证据格式。

建议统一：

```text
Provider Adapter
↓
ProviderResult
↓
EvidenceNormalizer
↓
EvidencePack
```

---

## 9. Structured Provider 与 EvidencePack 的关系

Structured Provider 返回结果后，仍需要进入统一证据治理。

```text
Provider Result
├── provider_id
├── record_id
├── record_version
├── source authority
├── retrieved_at
├── jurisdiction
├── validity / status
└── structured payload
        │
        ▼
Evidence Normalizer
        │
        ▼
EvidencePack
├── Claim
├── SourceSpan / SourceRef
├── Applicability
├── Conflict
└── Limitation
```

当 Provider 不提供“文档原文 span”时，不能伪造文档 Citation，应使用结构化记录引用并明确来源类型。

---

## 10. Structured Provider 与 Knowledge Graph 的关系

知识图谱不应成为外部专业数据库的无条件镜像。

推荐：

```text
Knowledge Graph
├── canonical concept
├── normalized relation
└── external provider identifier
        │
        ▼
Structured Tool
        │
        ▼
current authoritative fact
```

例如药物：

```text
(:Drug {concept_id, external_drug_id})
```

KG 可负责实体链接和关系导航，Drug Provider 负责相互作用、最新安全状态等权威结构化事实。

只有在许可、版本、更新机制和净收益明确时，才考虑把某些 Provider 数据物化进图谱。

---

## 11. Provider Governance

每个专业 Provider 必须进入 Registry，并声明：

```text
owner
provider type
jurisdiction
license
allowed use
credential policy
source authority
update frequency
freshness SLA
rate limit
PHI policy
allowed capabilities
allowed operations
fallback behavior
citation strategy
eval suite
```

禁止业务代码直接调用第三方 SDK/API。

未来结构应遵循：

```text
Clinical Module / Agent
↓
Tool Registry
↓
Provider Adapter
↓
External Provider
```

这与当前统一 Model Runtime 中“业务模块不得直接调用模型供应商 SDK”的治理思想保持一致。

---

## 12. Capability Allowlist

专业 Tool 不能全局自动开放。

例如：

```yaml
allowed_tool_ids:
  - drug-resolver
  - drug-interaction-check
```

而某个不涉及药物的 Capability 可以完全不允许 Drug Provider。

监管查询 Tool 同理。

Agent 不根据自然语言自行发现并启用未批准 Provider。

---

## 13. Failure / Uncertainty Semantics

Provider 结果必须允许：

```text
NO_RESULT
AMBIGUOUS_ENTITY
STALE_RESULT
REGION_MISMATCH
LICENSE_BLOCKED
POLICY_BLOCKED
DEPENDENCY_FAILURE
TIMED_OUT
CONFLICTING_RECORDS
```

禁止：

```text
Provider 查不到
→ LLM 根据记忆补一个“应该是”
```

如果该事实必须来自权威 Provider，失败时应显式降级或停止相应结论。

---

## 14. 与 Medical RAG 的组合

推荐组合方式：

### 药物示例

```text
Drug Tool
→ 精确相互作用 / 药物事实

Medical RAG
→ 指南中该人群如何管理、何时需要医生评估

EvidencePack
→ 合并但区分两类证据
```

### 器械示例

```text
Regulatory Tool
→ 当前器械注册 / recall / adverse event

Medical RAG
→ 临床指南中的使用场景和限制
```

不能因为接入 Structured Provider 就绕过 Medical RAG 的适用性和 Citation 机制。

---

## 15. 安全与合规

未来实现至少需要：

- License / Terms Review；
- Region / Jurisdiction Filter；
- Provider Credential Secret Management；
- Rate Limit；
- Audit；
- No silent fallback；
- Source freshness；
- Record version；
- Tool input validation；
- PHI 最小化；
- Provider 是否允许发送 Patient Context 的明确策略。

默认原则：

> 能用非患者化结构化查询解决的问题，不向外部 Provider 发送患者 PHI。

---

## 16. 未来评估指标

### Drug Tool

- entity resolution accuracy；
- interaction pair precision / recall；
- severity mapping correctness；
- unsupported claim rate；
- no-result correctness；
- stale data rejection。

### Regulatory Tool

- record retrieval precision；
- jurisdiction match；
- current status accuracy；
- recall/adverse-event disambiguation；
- citation/source traceability。

### Tool Governance

- unauthorized provider call = 0；
- direct third-party SDK call outside Adapter = 0；
- evidence provenance completeness；
- policy-block correctness；
- dependency failure safe degradation。

---

## 17. 推荐未来实施顺序

当前不实施。未来正式进入 Roadmap 时建议：

```text
1. ADR：Structured Medical Provider 进入正式架构
2. Provider Registry / ProviderSpec
3. FakeDrugProvider + synthetic fixtures
4. Evidence Normalizer
5. 一个只读、低风险专业 Tool
6. Tool Allowlist / Capability Integration
7. Offline Eval
8. Shadow / Clinician Assist
9. 再评估 Patient-facing use
```

不建议一开始就把多个外部数据库全部接入。

---

## 18. Exit 条件（未来）

某 Structured Provider 进入正式 Capability 前必须满足：

- Provider Registry 完整；
- License / Region / Allowed Use 明确；
- Tool Surface 足够窄且可测试；
- Structured Result Schema 稳定；
- EvidencePack 可追溯；
- Failure Semantics 明确；
- 不存在业务代码直接调用 Provider SDK/API；
- Capability Allowlist 生效；
- Eval 达标；
- Rollback / Disable 可执行。
