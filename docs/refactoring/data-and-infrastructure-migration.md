# AIdoctor 数据与基础设施迁移方案

> 文档状态：Draft v2.6  
> 更新时间：2026-07-29  
> 目标：在不中断现有固定 Workflow 的前提下，将临床状态、Durable Execution、Capability、RAG、Model Runtime 和可观测性迁移到目标架构。  
> 关联设计：[呼吸道 RAG V1](./adult-respiratory-medical-rag-v1-design.md) · [Prompt 与 Model Runtime](./prompt-and-model-runtime-design.md)

---

## 1. 原则

1. 旧数据先读、后映射、再迁移，不直接原地改写；
2. 临床状态、运行状态、知识资产、模型资产、审计资产分域；
3. 所有生产资产必须版本化并可回滚；
4. Patient RAG 与 Medical Knowledge RAG 物理或逻辑强隔离；
5. Prompt、Model、Knowledge、Capability 不能只存在于代码内；
6. Trace、Checkpoint、Memory 和 Knowledge 均不得成为临床事实源；
7. Neo4j 只有在来源治理和净收益评估通过后才能进入生产路径；
8. PHI 不写入普通模型调用日志、向量元数据或通用遥测。

---

## 2. 目标数据域

推荐 PostgreSQL 按 Schema 隔离：

```text
clinical
├── encounter
├── encounter_cdp
├── clinical_observation
├── observation_source_link
├── diagnostic_hypothesis
├── hypothesis_evidence_link
├── triage_assessment
├── information_gap
├── review_task
├── delivery_package
└── patient_longitudinal_record

runtime
├── thread
├── run
├── checkpoint
├── interrupt
├── resume_inbox
├── thread_lease
└── reconciliation_finding

governance
├── capability_package
├── capability_release
├── capability_asset_binding
├── prompt_spec
├── prompt_release
├── model_spec
├── model_route_policy
├── model_route_release
├── tool_spec
├── skill_spec
└── eval_suite

knowledge
├── knowledge_source
├── knowledge_document
├── knowledge_chunk
├── knowledge_release
├── knowledge_release_item
├── knowledge_ingestion_run
├── knowledge_embedding
├── retrieval_audit
├── evidence_pack
├── evidence_claim
└── citation_validation

integration
├── outbox_event
├── inbox_record
├── external_action_record
└── idempotency_record

audit
├── agent_event
├── clinical_decision_record
├── compliance_audit
└── release_audit
```

Checkpoint 可与业务数据库同 PostgreSQL 集群、不同 Schema，也可独立实例；最终通过 ADR 确认。

---

## 3. 旧 MySQL / Oracle 迁移

### 3.1 阶段

```text
现状表和数据盘点
→ 旧库只读 Adapter
→ 新 PostgreSQL Schema
→ 旧 CDP 解析和映射
→ Shadow Migration
→ 数据对账
→ 新 Encounter 分流
→ 旧写路径停止
→ 旧库只读窗口
→ 归档或下线
```

### 3.2 旧 CDP 处理

旧 CDP 原始记录必须保留：

- source database/table/PK；
- 原始 JSON/CLOB；
- checksum；
- old schema version；
- migration version；
- parse status；
- unmapped fields；
- error reason；
- migrated object IDs。

不得把旧 `evidence_graph` 自动升级为新的循证 `EvidenceClaim`；只有能追踪来源、版本和支持段落的内容才可进入 EvidencePack。

### 3.3 双写

首版原则上避免应用层长期双写。推荐：

- 旧路径继续写旧库；
- 新路径只写新库；
- 兼容层读取并映射；
- Shadow 期间复制和对账；
- 切流后停止旧写。

必须双写时，应使用 Transactional Outbox，而不是跨数据库分布式事务。

---

## 4. PostgreSQL 临床状态

### 4.1 版本与并发

- `EncounterCDP.version` 使用乐观锁；
- 所有写入携带 `expected_cdp_version`；
- State Committer 是唯一临床写入入口；
- Observation 不静默覆盖，使用 add/supersede/confirm/invalidate；
- 每次提交产生 CommitResult 和事件。

### 4.2 SourceArtifact

原始患者文本、报告、OCR、设备数据和外部病历通过 SourceArtifact 管理：

- 对象存储 URI；
- checksum；
- MIME；
- 患者和机构范围；
- Consent；
- 质量状态；
- 解析版本；
- 保留和删除策略。

模型不能直接读取原始对象存储路径，只能通过 Artifact Service 获取受控内容。

---

## 5. Capability 数据模型

### 5.1 `capability_package`

建议字段：

```text
capability_id
name
clinical_domain
major_version
status
owner
clinical_reviewer
supported_population
supported_complaints
excluded_population
created_at
```

### 5.2 `capability_release`

```text
capability_release_id
capability_id
version
manifest_checksum
safety_pack_version
terminology_pack_version
question_pack_version
hypothesis_pack_version
knowledge_release_id
prompt_release_ids
model_route_release_ids
tool_release_ids
eval_suite_id
release_status
approved_by
released_at
rollback_to_release_id
```

### 5.3 `capability_asset_binding`

用于绑定 Capability 与具体版本资产，禁止运行时自动选择“最新版本”。

首个记录为 `adult_respiratory_v1`，但其发布必须显式绑定 Safety、Prompt、Model、Knowledge 和 Eval。

---

## 6. Prompt 与 Model Runtime 数据模型

### 6.1 Prompt

`prompt_spec` 保存稳定定义，`prompt_release` 保存可发布版本。

关键字段：

```text
prompt_id
version
task_type
capability_ids
language
risk_level
input_schema_id
output_schema_id
template_location
template_checksum
allowed_route_ids
owner
reviewer
eval_suite_id
status
released_at
deprecated_at
rollback_to_version
```

首版模板可保存在 Git/YAML；数据库保存发布元数据和 checksum。生产运行只加载明确发布版本。

### 6.2 Model

`model_spec`：

```text
model_id
provider
provider_model_name
provider_model_version
capabilities
context_window
structured_output_support
phi_policy
allowed_regions
data_retention_policy
cost_profile
latency_profile
health_status
```

`model_route_policy`：

```text
route_id
version
task_type
risk_level
capability_ids
required_features
input_data_classification
primary_model_ids
fallback_model_ids
latency_budget
cost_budget
minimum_eval_suite_id
minimum_eval_thresholds
```

`model_route_release` 绑定某一策略版本和批准的模型集合。

### 6.3 Model Invocation Record

仅保存最小、可审计元数据：

- trace/thread/encounter；
- route、prompt、model、schema、capability 版本；
- context hash；
- token、成本、延迟；
- output validation status；
- retry/fallback；
- error code；
- redaction manifest reference。

禁止默认保存完整 Prompt、完整患者输入和完整模型输出到普通日志。需要临床留存的结构化结果进入相应业务表并保留来源。

---

## 7. Medical Knowledge RAG 数据模型

### 7.1 Source Registry

`knowledge_source` 至少保存：

```text
source_id
source_type
title
publisher
license
access_type
region
language
population
publication_date
updated_at
valid_from
valid_until
source_tier
clinical_review_status
checksum
```

### 7.2 文档和 Chunk

`knowledge_document` 保存原始版本和解析状态；`knowledge_chunk` 保存：

- section_path；
- chunk_type；
- recommendation_strength；
- evidence_level；
- population/region/setting；
- validity；
- parent/adjacent links；
- text checksum；
- parser/chunker version。

### 7.3 Knowledge Release

```text
knowledge_release_id
capability_ids
source_manifest_checksum
chunker_version
embedding_model_id
embedding_version
bm25_index_version
vector_index_version
reranker_model_id
reranker_version
graph_release_id
clinical_reviewer
release_status
released_at
rollback_to_release_id
```

所有检索、EvidencePack 和临床决策记录必须记录 `knowledge_release_id`。

---

## 8. Patient RAG 隔离

Patient RAG 以结构化查询优先，向量召回仅用于获批场景。

必须隔离：

- tenant；
- patient；
- consent scope；
- purpose；
- valid time；
- deleted/revoked data。

患者向量不得与公共医学知识共用无权限隔离的 collection、namespace 或索引。删除患者数据时必须同步清理主存储、向量索引、缓存和派生映射。

---

## 9. pgvector、BM25 与 Reranker

### 9.1 pgvector

首版用于 Medical Knowledge RAG；索引必须绑定：

- embedding model/version；
- Knowledge Release；
- language；
- dimension；
- distance metric；
- index type 和参数。

Embedding 变化必须新建索引版本，禁止原地覆盖。

### 9.2 BM25

需要 ADR 选择：

1. PostgreSQL Full Text Search；
2. OpenSearch/Elasticsearch；
3. 独立轻量 BM25 服务。

Phase D 可先使用 PostgreSQL FTS 建立最小闭环；若中文医学分词、规模或排序效果不足，再迁 OpenSearch。

### 9.3 Reranker

Reranker 通过 Model Registry 管理，但不经过通用对话 Prompt。必须记录：

- model/version；
- input item count；
- score；
- latency；
- fallback；
- Eval 结果。

---

## 10. Neo4j 与知识图谱

### 10.1 首版定位

Neo4j 仅用于：术语关系、Query Expansion、症状—疾病—检查多跳关系和 must-not-miss 扩展。

不得用于：

- 替代指南全文；
- 替代证据等级；
- 直接生成最终诊断；
- 自动批准治疗；
- 省略 Citation。

### 10.2 Graph Release

若启用生产图谱，需要：

```text
graph_release_id
schema_version
source_manifest
node_count
edge_count
provenance_coverage
build_version
eval_report_id
released_at
rollback_to_release_id
```

每条边必须可追溯到知识来源或明确标记为术语映射。无来源关系不得进入临床证据链。

### 10.3 启用门禁

- 与 BM25+vector 基线对照；
- must-not-miss recall 提升；
- 无明显 precision 降低；
- 延迟和成本可接受；
- provenance coverage 达标；
- 临床 Reviewer 批准。

未达标时保留 Neo4j 用于实验，不进入生产 RetrievalPlan。

---

## 11. Redis

允许用途：

- Thread Lease；
- 短期缓存；
- Rate Limit；
- 临时事件和连接状态；
- 分布式锁辅助；
- Provider health cache。

禁止作为：

- 临床事实主存储；
- 唯一 Checkpoint；
- 唯一 Prompt/Model/Knowledge Registry；
- 长期患者记忆。

Key 必须包含 tenant、environment、purpose 和版本；所有临时 Key 有 TTL。

---

## 12. Object Storage

用于：原始报告、上传文件、知识源原文、解析产物和受控导出。

要求：

- 服务端加密；
- 租户隔离；
- 短期签名 URL；
- checksum；
- Malware scan；
- Retention 和 Legal Hold；
- 删除传播；
- 原始文件与解析结果分离。

受版权限制的知识源不得因进入对象存储而改变访问范围。

---

## 13. OpenTelemetry 与版本链

所有关键 Span/Event 必须关联：

```text
trace_id / span_id
encounter_id / thread_id / run_id
capability_release_id
prompt_release_id
model_route_release_id
selected_model_id
knowledge_release_id
embedding_version
reranker_version
graph_release_id
context_hash
checkpoint_id
cdp_version
```

普通 OTel Backend 不保存完整 PHI。ClinicalDecisionRecord 保存影响临床输出的结构化依据，Compliance Audit 保存访问、修改、批准和导出行为。

---

## 14. Secret 与配置管理

必须从代码和 Compose 中移除：数据库密码、Provider API Key、对象存储 Secret、Nacos 凭据和签名密钥。

配置分层：

```text
代码默认值
→ 环境非敏感配置
→ Secret Manager
→ Release Snapshot
```

模型和知识选择通过发布 ID，而不是临时环境变量中的“最新模型”或“当前索引”。

---

## 15. 备份、恢复与回滚

必须分别覆盖：

- Clinical DB；
- Runtime Checkpoint；
- Governance Registry；
- Knowledge Metadata 和向量索引；
- Object Storage；
- Neo4j Graph；
- Audit Store。

回滚类型：

| 类型 | 方法 |
|---|---|
| 应用回滚 | 部署前一镜像 |
| Schema 回滚 | Expand/Contract 或向前修复 |
| Capability 回滚 | 切换 `capability_release_id` |
| Prompt 回滚 | 切换 `prompt_release_id` |
| Model 回滚 | 切换 `model_route_release_id` |
| Knowledge 回滚 | 切换 `knowledge_release_id` |
| Graph 回滚 | 切换 `graph_release_id` |

回滚不得修改已经生成的历史记录；历史执行继续引用当时版本快照。

---

## 16. 迁移对账

对账维度：

- Encounter 和患者数量；
- CDP 版本链；
- Observation 和来源；
- 迁移错误和未映射字段；
- Checkpoint 可恢复性；
- Knowledge Source/Chunk/Release 数量；
- 向量索引覆盖；
- Prompt/Model/Capability 绑定完整性；
- Audit 和 Event 链路。

任何临床迁移不一致都不得通过“覆盖旧值”修复，必须产生 ReconciliationFinding。

---

## 17. ADR 清单

最终冻结前必须完成：

1. PostgreSQL 是否为最终临床主库；
2. Checkpoint 与业务库的实例边界；
3. BM25 使用 PostgreSQL FTS 还是 OpenSearch；
4. Milvus 是否下线；
5. Neo4j 是否仅实验或进入生产；
6. Nacos 的保留范围；
7. Prompt Registry 首版 Git/YAML 与数据库元数据关系；
8. Model Provider 与 PHI/数据驻留策略；
9. OTel 后端组合；
10. Object Storage 和知识源版权访问策略。

---

## 18. 阶段出口

### Phase A

- 真实数据库和基础设施盘点；
- 数据库、RAG、Graph、Prompt/Model ADR；
- 新 Schema 草案和 Migration 骨架；
- Secret 扫描。

### Phase B/C

- Clinical/Runtime Schema；
- State Committer；
- Checkpoint/Resume；
- Prompt/Model Registry 最小表或配置；
- ProviderAdapter 和 OTel v1。

### Phase D

- Source Registry；
- Knowledge Release；
- pgvector、BM25、Reranker；
- Patient RAG 隔离；
- Graph 对照实验。

### Phase E/F

- Capability 完整发布链；
- Review/Delivery/Outbox；
- Release Audit；
- Backup/Restore 演练；
- 旧库、旧索引和旧服务下线。