# 成人呼吸道 Medical RAG V1 详细设计

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29  
> Capability：`adult_respiratory_v1`

## 1. 目标

本文将已有的 RAG 架构原则转化为首个 Capability 可直接实施的知识库设计。

V1 目标不是构建全医学搜索引擎，而是为成人常见呼吸道症状场景提供：

- 白名单来源检索；
- 红旗与分诊依据展示；
- 有限鉴别所需知识；
- 检查和就医层级依据；
- 患者教育内容；
- Claim-Level Citation；
- 适用性、冲突、时效性和无结果表达。

## 2. 非目标

V1 不包括：

- 开放互联网实时搜索；
- 自动处方和药物调整；
- 自动生成正式诊断；
- 依赖单篇研究改变分诊；
- 未经许可抓取受限全文；
- 将患者资料与公共医学知识混入同一索引；
- 使用知识图谱替代指南和引用；
- 以向量相似度直接决定临床结论。

## 3. 总体架构

```text
Approved Sources
→ Source Registry
→ License / Access / Region Review
→ Parse / Normalize / Extract
→ Clinical Metadata Enrichment
→ KnowledgeChunk
→ BM25 + pgvector + optional Graph Index
→ Retrieval Plan
→ ACL / Metadata Filter
→ Hybrid Retrieval
→ Rerank
→ Claim Extraction
→ Citation Validation
→ Applicability / Conflict
→ EvidencePack
```

## 4. 两类 RAG 隔离

### 4.1 Patient RAG

检索患者自身：

- 既往 Encounter；
- 医生确认病史；
- 过敏和长期用药；
- 相关检查；
- 既往相同风险事件；
- 随访。

原则：

- 结构化查询优先；
- patient_id、tenant_id、consent_scope 强过滤；
- 不允许跨患者召回；
- 不将完整历史直接放入 Prompt；
- 返回最小必要字段和召回理由。

### 4.2 Medical Knowledge RAG

检索公共或授权医学知识：

- 官方指南；
- 监管与安全文件；
- 系统综述和高等级研究；
- 经临床审核的参考；
- 患者教育材料；
- 可选医学知识图谱。

Patient RAG 与 Medical Knowledge RAG 使用不同 Schema、不同索引、不同 ACL 和不同 Cache Namespace。

## 5. V1 知识域

### 5.1 安全与分诊

必须覆盖：

- 严重呼吸困难；
- 低氧相关表现；
- 胸痛；
- 咯血；
- 意识改变；
- 循环不稳定表现；
- 快速恶化；
- 高风险基础疾病；
- 特殊人群；
- 急诊、尽快就医和常规就诊的边界。

其中明确阈值、组合规则和 Capability 排除项进入结构化 Safety Rule Store。RAG 只用于展示来源、限制和解释，不作为唯一安全执行路径。

### 5.2 信息采集

覆盖以下主诉需要采集的维度：

- 咳嗽；
- 咳痰；
- 发热；
- 喘息；
- 胸闷；
- 呼吸困难；
- 呼吸相关胸痛；
- 咯血。

知识内容包括：

- 起病和病程；
- 严重度和变化趋势；
- 诱因和缓解因素；
- 伴随症状；
- 暴露史；
- 既往呼吸系统疾病；
- 吸烟或职业暴露；
- 用药、过敏和免疫状态；
- 既往检查和治疗反应。

必须采集项同时进入 Question Policy，不依赖运行时临时 RAG 才能得知。

### 5.3 有限鉴别

知识库不维护无限疾病清单。候选仅来自 Capability 批准范围。

每个候选知识单元至少包含：

```python
class RespiratoryConditionProfile(BaseModel):
    concept_id: str
    display_name: str
    category: Literal["common", "alternative", "must_not_miss"]
    typical_features: list[str]
    opposing_features: list[str]
    discriminators: list[str]
    red_flags: list[str]
    applicable_populations: list[str]
    exclusion_conditions: list[str]
    recommended_evidence_queries: list[str]
    source_claim_ids: list[str]
```

### 5.4 检查与就医依据

覆盖：

- 何时需要生命体征和血氧信息；
- 何时建议线下评估；
- 何时考虑基础实验室检查；
- 何时考虑影像或肺功能类检查；
- 检查前置条件、限制和特殊人群；
- 就医层级、紧急度和科室建议。

不得将“常见做法”表达为强制推荐，所有关键 Claim 必须有来源和适用性。

### 5.5 患者教育

内容必须经过临床审核，覆盖：

- 症状观察；
- 危险变化；
- 就诊前准备；
- 报告和用药清单准备；
- 感染防护类通用注意事项；
- 不应自行采取的高风险行为；
- 何时重新评估。

患者教育知识单独标记 `chunk_type=patient_education`，不参与诊断候选打分。

## 6. Source Registry

```python
class KnowledgeSourceRegistration(BaseModel):
    source_id: str
    title: str
    publisher: str
    source_type: Literal[
        "guideline",
        "regulatory_document",
        "systematic_review",
        "meta_analysis",
        "randomized_trial",
        "cohort_study",
        "expert_consensus",
        "clinical_reference",
        "patient_education",
    ]
    source_tier: Literal["tier_1", "tier_2", "tier_3", "tier_4"]
    language: str
    region: str | None
    population: dict
    publication_date: date
    version: str
    valid_from: date | None
    valid_until: date | None
    access_type: str
    license_type: str
    allowed_uses: list[str]
    clinical_review_status: str
    reviewer_ids: list[str]
    checksum: str
```

没有完成来源、许可、版本、地区、有效期和临床审核登记的文档，不得进入生产 Knowledge Release。

## 7. 首批来源清单的管理方式

本文不写死某个当前版本的外部指南名称，避免设计文档与实际版本脱节。Phase D 前建立 `source-manifest.yaml`，每条来源必须填写：

```yaml
source_id: respiratory-guideline-example
publisher: <official organization>
source_type: guideline
source_tier: tier_1
region: <target region>
language: zh-CN
document_version: <reviewed version>
publication_date: <date>
valid_until: <date or null>
license_type: <license>
access_type: public_full_text
supported_topics:
  - respiratory_red_flags
  - triage
  - limited_differential
clinical_review_status: approved
```

来源策略：

- Tier 1 为首要依据；
- Tier 2 用于补充高等级证据；
- Tier 3 用于背景和补充；
- Tier 4 不用于生成生产级推荐；
- 禁止来源永不进入索引。

## 8. Ingestion Pipeline

### 8.1 流程

```text
register_source
→ verify_license
→ fetch_or_upload
→ malware_scan
→ parse_document_structure
→ identify_version_and_region
→ extract_recommendations_tables_algorithms
→ normalize_terminology
→ enrich_clinical_metadata
→ create_chunks
→ embed_and_index
→ automated_quality_checks
→ clinician_sampling_review
→ publish_knowledge_release
```

### 8.2 解析要求

必须保留：

- 标题层级；
- 章节和页码；
- 推荐原文位置；
- 推荐强度；
- 证据等级；
- 表头和表格行关系；
- 算法前置条件；
- 例外和限制；
- 人群、地区、环境和时间。

PDF、Office 和网页解析结果必须保留 SourceArtifact 和 checksum。

## 9. Chunk 设计

```python
class RespiratoryKnowledgeChunk(BaseModel):
    chunk_id: str
    source_id: str
    source_version: str
    knowledge_release_id: str

    section_path: list[str]
    page_or_location: str | None
    chunk_type: Literal[
        "recommendation",
        "definition",
        "red_flag",
        "triage_rule",
        "algorithm_step",
        "table_row",
        "diagnostic_feature",
        "test_selection",
        "evidence_summary",
        "limitation",
        "patient_education",
    ]

    text: str
    normalized_concepts: list[str]
    supported_question_types: list[str]
    recommendation_strength: str | None
    evidence_level: str | None

    population: dict | None
    region: str | None
    care_setting: str | None
    valid_from: date | None
    valid_until: date | None

    parent_chunk_id: str | None
    adjacent_chunk_ids: list[str]
    table_id: str | None
    checksum: str
```

### 9.1 切分原则

- 推荐条目不与关键限制分开；
- 表格按行切分时保留表头；
- 算法步骤保留前置条件和下一步；
- 红旗条目保持原子化；
- 患者教育与专业推荐分开；
- 最大长度只作为保护阈值，不作为主要切分规则；
- 相邻 Chunk 可通过 parent/adjacent 关系补充上下文。

## 10. 存储与索引

### 10.1 PostgreSQL

建议 Schema：

```text
knowledge.source_registry
knowledge.source_artifact
knowledge.knowledge_release
knowledge.knowledge_chunk
knowledge.chunk_metadata
knowledge.evidence_claim
knowledge.citation_validation
knowledge.retrieval_audit
```

### 10.2 pgvector

`knowledge_chunk` 保存：

- `embedding_model_id`；
- `embedding_version`；
- `embedding_vector`；
- `embedding_checksum`。

向量索引不得替代 metadata filter。

### 10.3 BM25

用于：

- 医学术语精确匹配；
- 缩写；
- 红旗关键词；
- 指南推荐条目；
- 检查名称；
- 概念编码。

可使用 PostgreSQL 全文检索或独立搜索组件，由 ADR 决定。V1 必须提供统一 Retrieval Adapter。

### 10.4 Redis

仅用于：

- 查询计划短期缓存；
- 相同 Knowledge Release 的检索缓存；
- 限流；
- 任务状态。

Cache Key 必须包含：

```text
knowledge_release_id
query_hash
population_filter_hash
region
language
retrieval_policy_version
```

## 11. Embedding 与 Reranker

V1 不在架构文档中硬编码商业模型名称，而是使用 Registry：

```python
class EmbeddingModelSpec(BaseModel):
    model_id: str
    version: str
    languages: list[str]
    medical_eval_suite_id: str
    vector_dimension: int
    max_input_tokens: int
    allowed_data_types: list[str]
    release_status: str

class RerankerSpec(BaseModel):
    model_id: str
    version: str
    supported_languages: list[str]
    max_pair_tokens: int
    eval_suite_id: str
    release_status: str
```

选型门禁：

- 中文和医学术语表现；
- 红旗、指南推荐和检查问题召回；
- 多语言需求；
- 数据驻留；
- 成本和延迟；
- 可复现版本；
- 离线评估结果。

## 12. Query Planning

```python
class RespiratoryRetrievalPlan(BaseModel):
    query_id: str
    encounter_id: str
    capability_id: str
    query_type: Literal[
        "red_flag_evidence",
        "triage_evidence",
        "diagnosis_support",
        "discriminator",
        "test_selection",
        "care_navigation",
        "patient_education",
    ]
    normalized_query: str
    concepts: list[str]
    patient_population: dict
    region: str
    language: str
    required_source_tiers: list[str]
    allowed_chunk_types: list[str]
    retrieval_methods: list[str]
    top_k: int
    rerank_k: int
    knowledge_release_id: str
```

Query Planner 只能从 Capability 配置中的模板和策略选择，不能自由上网。

## 13. Hybrid Retrieval

```text
Capability / Knowledge Release Filter
+ Region / Population / Validity Filter
+ BM25
+ Vector Retrieval
+ Terminology Expansion
+ optional Knowledge Graph Expansion
+ Reranker
+ Source Tier Boost
+ Population / Region / Freshness Rerank
```

V1 默认流程：

1. metadata 过滤；
2. BM25 召回；
3. pgvector 召回；
4. 合并和去重；
5. Rerank；
6. 来源等级和适用性校正；
7. 生成 `RetrievalResult`。

具体 `top_k`、阈值和融合权重必须通过离线评估确定，不能在文档中使用拍脑袋常量。

## 14. 知识图谱设计

### 14.1 V1 定位

知识图谱是可选增强，不是 RAG V1 上线阻塞项。

适合处理：

- 症状、疾病、检查和概念编码关系；
- 同义词与标准术语；
- must-not-miss 扩展；
- 多跳关系查询；
- Query Expansion；
- 可解释关系路径。

不适合替代：

- 指南版本；
- 推荐强度；
- Evidence Level；
- Claim-Level Citation；
- 患者适用性；
- 最新安全通知。

### 14.2 图 Schema

```text
(:Symptom)
(:Condition)
(:RedFlag)
(:Test)
(:Population)
(:ClinicalConcept)
(:Code)

(:Symptom)-[:SUPPORTS]->(:Condition)
(:Symptom)-[:ARGUES_AGAINST]->(:Condition)
(:Condition)-[:MUST_EXCLUDE_WHEN]->(:RedFlag)
(:Condition)-[:MAY_REQUIRE]->(:Test)
(:ClinicalConcept)-[:MAPS_TO]->(:Code)
(:Condition)-[:APPLIES_TO]->(:Population)
```

每条临床关系必须带：

- `source_claim_id`；
- `source_version`；
- `valid_from/valid_until`；
- `review_status`；
- `confidence_type`。

无来源关系只可用于实验，不可用于生产解释。

### 14.3 启用门禁

只有当离线评估证明图扩展对以下指标有净收益时才进入生产：

- must-not-miss recall；
- terminology normalization；
- retrieval recall；
- query precision；
- latency；
- false expansion rate。

## 15. RetrievalResult 与 EvidencePack

```python
class RespiratoryRetrievalResult(BaseModel):
    query_id: str
    knowledge_release_id: str
    items: list[dict]
    filters_applied: dict
    rejected_items: list[dict]
    status: Literal[
        "success",
        "partial",
        "no_result",
        "insufficient_authorized_sources",
        "population_mismatch",
        "out_of_scope",
        "failure",
    ]
    latency_ms: int
    degraded_mode: bool
```

EvidencePack 必须包含：

- Claims；
- SourceSpan；
- 来源 Tier；
- Citation 支持级别；
- Population/Region/Freshness；
- Conflicts；
- Limitations；
- Knowledge Release；
- 是否需要医生审核。

## 16. Citation Validation

采用组合验证：

```text
source existence/version check
+ exact span check
+ rule-based wording check
+ NLI or Cross-Encoder support check
+ population applicability check
+ clinician sampling review
```

禁止：

- 引用不存在来源；
- 用相邻主题支持 Claim；
- 把“可能”改成“推荐”；
- 把相关性改成因果；
- 忽略适用人群；
- 只在回答末尾列文献而不绑定 Claim。

## 17. 安全

所有文档内容视为不可信数据。

必须：

- 数据与系统指令隔离；
- 清理隐藏文本和恶意标记；
- 禁止文档触发 Tool；
- 文件病毒和类型检查；
- URL Allowlist；
- 权限和 License Filter；
- Prompt Injection 测试；
- PHI 不进入公共知识索引；
- 日志不保存完整文档和患者上下文。

## 18. Knowledge Release

```python
class KnowledgeRelease(BaseModel):
    knowledge_release_id: str
    capability_id: str
    version: str
    source_versions: list[str]
    chunk_schema_version: str
    embedding_model_id: str
    reranker_model_id: str | None
    retrieval_policy_version: str
    graph_release_id: str | None
    clinical_review_status: str
    eval_report_id: str
    released_at: datetime
    rollback_release_id: str | None
```

每个 Encounter 绑定明确 Knowledge Release，不使用“自动最新版本”。

## 19. 更新、过期与撤回

- 定期检查来源版本和有效期；
- 新版本先并行摄取和评估；
- 被撤回来源立即停止新检索；
- 已生成的 EvidencePack 保留当时版本链；
- 紧急安全撤回触发 Capability/Knowledge Release 召回；
- 缓存按 Release 隔离并失效；
- 图关系同步撤回。

## 20. 评估集

至少建设：

- 红旗证据检索；
- 分诊依据检索；
- 有限候选支持/反对证据；
- 检查选择；
- 患者教育；
- 同义词和缩写；
- 人群不匹配；
- 地区不匹配；
- 过期来源；
- 冲突指南；
- 无结果；
- Citation 不支持 Claim；
- Prompt Injection 文档；
- 图谱错误扩展。

指标包括：

- Recall@k；
- Precision@k；
- nDCG；
- must-not-miss recall；
- citation precision；
- population match accuracy；
- no-result correctness；
- stale-source rejection；
- cross-patient leakage=0；
- latency；
- cost。

## 21. V1 实施顺序

```text
1. Source Registry 和 KnowledgeRelease Schema
2. 选定并审核首批白名单来源
3. 结构解析和 Chunk Pipeline
4. PostgreSQL 元数据与 pgvector
5. BM25 + Vector Retrieval
6. Reranker Adapter
7. Query Plan 与 RetrievalResult
8. EvidencePack 和 Citation Validator
9. 前端 Citation 展示
10. 离线评估和 Shadow
11. 可选知识图谱对照实验
```

## 22. Exit Gate

- 所有来源完成许可、版本和临床审核；
- Patient 与 Medical RAG 物理和逻辑隔离；
- 红旗不依赖 RAG 才能执行；
- 每个关键 Claim 有 SourceSpan；
- 无结果、冲突和人群不匹配可显式返回；
- Knowledge Release 可回滚；
- 引用评估达标；
- 未授权来源无法进入索引；
- 知识图谱若启用，具备来源链和净收益报告。
