# AIdoctor 数据与基础设施迁移方案

> 文档状态：Draft v2.5  
> 更新时间：2026-07-29  
> 目标：在不中断现有固定 Workflow 的前提下，将临床状态、Checkpoint、RAG、可观测性和运行基础设施迁移到目标架构。

---

## 1. 当前状态

当前仓库明确使用或声明：

- MySQL 8：本地开发和 Docker Compose；
- Oracle：Java 生产适配；
- Redis 7：缓存、对话上下文；
- Neo4j 5：知识图谱；
- Milvus：diagnosis engine 环境变量声明，但 Compose 未定义；
- Nacos：服务发现；
- Spring Cloud OpenFeign：Java 调用 Python 服务；
- 自定义执行 Trace；
- Prometheus Client / Micrometer；
- 本地文件和未统一的报告存储方式。

目标架构提出：

- PostgreSQL：新临床状态与生产 Checkpoint；
- pgvector：首版向量检索；
- Redis：Lease、短缓存和协调；
- Neo4j：仅在明确收益下保留；
- Object Storage：SourceArtifact；
- OpenTelemetry Collector；
- Tempo/Jaeger、Prometheus、Loki/OpenSearch、Grafana。

---

## 2. 核心原则

1. 不在同一个发布中同时更换数据库、重写临床状态和切换 Agent Runtime；
2. 旧数据库在验证完成前保持可回滚；
3. 新旧系统之间不进行无版本的双向写；
4. 临床事实迁移必须保留原始记录、来源和迁移错误；
5. Checkpoint 与临床状态分责，即使物理上位于同一个 PostgreSQL 实例；
6. Redis 不作为临床事实主存储；
7. Neo4j/Milvus 不是第一条主链路前置条件；
8. Trace 后端故障不能阻塞普通请求；
9. Checkpoint 或 State Commit 失败必须阻止不可逆动作；
10. 所有迁移支持 dry-run、对账和回滚。

---

## 3. 数据库目标决策

### 3.1 推荐方向

推荐目标：

```text
PostgreSQL Cluster
├── clinical schema
│   ├── encounter
│   ├── encounter_cdp
│   ├── clinical_observation
│   ├── source_artifact_metadata
│   ├── diagnostic_hypothesis
│   ├── triage_assessment
│   ├── information_gap
│   └── delivery metadata
│
├── runtime schema
│   ├── thread
│   ├── run
│   ├── checkpoint
│   ├── interrupt
│   ├── resume_inbox
│   └── thread_lease metadata
│
├── integration schema
│   ├── outbox
│   ├── inbox
│   └── external_action_record
│
├── governance schema
│   ├── capability_release
│   ├── prompt_release
│   ├── skill_release
│   └── model_route_release
│
└── vector schema / pgvector
```

Audit 和原始 SourceArtifact 内容根据合规要求可独立实例或独立存储。

### 3.2 必须提交 ADR

`ADR-051: Clinical Production Database Target`

ADR 必须比较：

| 方案 | 优点 | 风险 |
|---|---|---|
| 保留 Oracle/MySQL | 改动小 | Checkpointer、JSON、vector 和开发一致性较差 |
| 全量迁移 PostgreSQL | 目标统一 | 一次性迁移风险高 |
| 新能力 PostgreSQL，旧系统渐进迁移 | 风险可控 | 过渡期多库和对账复杂 |

推荐第三种。

---

## 4. 分阶段数据库迁移

### Stage 0：只读盘点

- 导出当前表、字段、索引、约束；
- 统计 CDP 数量、版本数量、JSON 大小和坏数据；
- 识别 patient ID/session ID 的稳定性；
- 识别 Oracle/MySQL 差异；
- 备份数据库；
- 建立脱敏样本数据集。

禁止：修改生产表结构。

### Stage 1：新契约独立 Schema

在 PostgreSQL 建立新 Contract Schema，仅服务第一条纵向切片。

- 新 Encounter；
- 新 EncounterCDP；
- Observation；
- StatePatch/Commit log；
- Thread/Checkpoint；
- AgentEvent correlation metadata。

旧系统继续运行，第一条新路径使用独立测试/影子患者数据。

### Stage 2：Legacy CDP Adapter

建立：

```text
LegacyCdpReader
LegacyCdpToContractMapper
ContractToLegacyResponseMapper
```

原则：

- 旧 CDP 初期只读；
- 新 Agent 不直接修改旧 CLOB；
- 必须写旧系统时，仅通过受控兼容层；
- 每次转换记录 mapper version 和 unmapped fields。

### Stage 3：Shadow Migration

- 周期性将旧 CDP 转换到新 Schema；
- 不切生产读流量；
- 比较患者状态、分诊、候选和输出；
- 生成差异报告；
- 修复映射规则。

### Stage 4：分流写入

只让选定 Capability 的新 Encounter 写 PostgreSQL。

- 老 Encounter 仍走旧系统；
- 新 Encounter 不回写旧 CDP，除非兼容 API 明确要求；
- 前端根据 encounter runtime version 路由。

### Stage 5：历史迁移

- 迁移仍有业务价值的历史 Encounter；
- 旧原始 JSON 进入 `legacy_payload_archive`；
- 无法映射字段进入 `unmapped_payload`；
- 对账通过后旧库变只读。

### Stage 6：下线旧写路径

在回滚期结束后：

- 禁止旧系统新建目标 Capability Encounter；
- 保留历史查询；
- 导出最终快照；
- 完成审计和备份；
- 按 Decommission Plan 归档。

---

## 5. 旧 CDP 迁移设计

### 5.1 原始保留

每条旧 CDP 必须保存：

```text
legacy_record_id
legacy_database
legacy_table
legacy_version
legacy_payload
legacy_payload_checksum
extracted_at
migration_run_id
migration_status
migration_errors
unmapped_fields
```

### 5.2 字段拆分

旧大 JSON 不直接复制到新 CDP。

```text
patient_state
→ ClinicalObservation[]
→ SourceArtifact links
→ unresolved legacy attributes

ddx
→ DiagnosticHypothesis[]

evidence_graph
→ EvidenceEntry[] / legacy reasoning archive

triage
→ TriageAssessment

execution_trace
→ LegacyAgentEventArchive

audit_info
→ LegacyAuditArchive
```

### 5.3 迁移状态

```text
DISCOVERED
PARSED
PARTIALLY_MAPPED
MAPPED
VALIDATED
FAILED_RETRYABLE
FAILED_MANUAL_REVIEW
SKIPPED_OUT_OF_SCOPE
```

### 5.4 对账

至少对账：

- record count；
- patient/encounter identity；
- version chain；
- status；
- symptoms and critical facts；
- triage；
- conclusion/delivery；
- unmapped field rate；
- checksum。

---

## 6. PostgreSQL Checkpoint

### 6.1 与临床状态分责

即使同实例，也采用独立 schema、表和 repository。

Checkpoint 保存：

- GraphState；
- node cursor；
- interrupt；
- runtime metadata；
- version references。

不保存为临床真值：

- 未提交的模型推断；
- 完整长期患者记录；
- Audit 主记录；
- 原始报告内容。

### 6.2 事务边界

```text
Graph Node computes Candidate
→ State Commit transaction
→ CommitResult
→ Checkpoint transaction records new cdp_version
→ Interrupt / Continue
```

不可逆动作前：

```text
Checkpoint Before
→ Outbox / ExternalActionRecord
→ Action Worker
→ Checkpoint After
```

---

## 7. Redis 迁移

### 7.1 保留用途

- Thread Lease；
- 短期运行锁；
- 限流；
- 非关键缓存；
- 临时流式连接状态；
- Circuit Breaker 状态。

### 7.2 禁止用途

- 最终临床事实；
- 唯一患者历史；
- 唯一 Checkpoint；
- 唯一 ReviewTask；
- 永久 Memory；
- 无过期策略的完整对话。

### 7.3 Key 规范

```text
{tenant}:{env}:thread:{thread_id}:lease
{tenant}:{env}:rate:{actor_id}:{operation}
{tenant}:{env}:cache:{namespace}:{version}:{key}
```

要求：

- TTL；
- tenant/env 隔离；
- 无 PHI 明文 key；
- 监控 hit/miss/eviction；
- Redis 故障降级策略。

---

## 8. Neo4j 与 Milvus

### 8.1 Neo4j

保留为候选组件，但不作为首条链路前置。

保留条件：

- KG 路径对诊断候选或解释有可量化收益；
- 数据来源、版本和许可明确；
- 路径结果可转换为 Evidence/ReasoningPath；
- 运行成本可接受；
- 无 Neo4j 时存在 fallback。

### 8.2 Milvus

当前仅见配置声明，开发 Compose 未定义。

首版建议：

- 不新增 Milvus 依赖；
- 使用 pgvector 完成白名单小规模 RAG；
- 数据量和召回性能达到明确阈值后再比较 Milvus；
- 若无显著收益，移除死配置。

### 8.3 Benchmark

比较：

- 数据规模；
- ingest 时间；
- recall@k；
- latency p95；
- 运维复杂度；
- 备份恢复；
- 多租户隔离；
- 成本。

---

## 9. Nacos 与服务发现

### 9.1 过渡期

旧 Java → 多 Python 服务仍可保留 Nacos/Feign 或静态 URL。

### 9.2 目标期

当 Python 能力收拢到一个 Agent Runtime 后：

- 不再需要为每个 Python package 注册服务；
- Java 只调用 Agent Runtime 和少数独立 Tool Service；
- Kubernetes/DNS 或部署平台服务发现可替代 Nacos；
- 是否保留 Nacos 由部署环境 ADR 决定。

### 9.3 下线门禁

- 无客户端依赖；
- 配置迁移完成；
- 健康检查和路由替代；
- 回滚验证；
- Nacos 数据导出。

---

## 10. Object Storage 与 SourceArtifact

原始文件、报告、图像和大文本不存入普通 Trace 或 CDP JSON。

建议：

```text
Object Storage
├── encrypted original artifact
├── normalized derivative
├── OCR text
└── redacted preview

PostgreSQL SourceArtifact Metadata
├── source_id
├── patient/encounter scope
├── checksum
├── content type
├── quality status
├── consent scope
├── storage reference
├── encryption key reference
└── retention policy
```

要求：

- 上传幂等；
- 病毒/恶意文件扫描；
- MIME 校验；
- 文件大小限制；
- 访问审计；
- signed URL；
- 生命周期和删除。

---

## 11. 可观测基础设施

### 11.1 开发环境

```text
OpenTelemetry Collector
Jaeger
Prometheus
Grafana（可选）
```

### 11.2 目标环境

```text
OTel Collector
├── Trace → Tempo
├── Metric → Prometheus
└── Log → Loki or OpenSearch

Grafana
```

### 11.3 迁移步骤

1. Java/Python 统一 trace propagation；
2. 保留旧 Trace 页面读取 AgentEvent；
3. OTel 与旧 Trace 双写有限时期；
4. 技术调用图切到 OTel；
5. Agent 决策继续使用 AgentEvent；
6. 删除 CDP.execution_trace 新写入；
7. 历史 execution trace 只读归档。

### 11.4 PHI

Collector 必须：

- 删除禁止字段；
- patient ID hash/tokenize；
- 限制 attribute 长度；
- 禁止完整 Prompt/Response；
- 采样；
- 输出后端 allowlist。

---

## 12. Secret 与配置管理

当前 Compose 默认密码只允许本地示例。

目标：

- `.env.example` 不含真实密钥；
- 密钥不进入 Git；
- 环境变量只传 secret reference 或由 secret manager 注入；
- 数据库、Neo4j、LLM、对象存储密钥独立；
- 定期轮换；
- secret scanning；
- 开发、测试、预发、生产隔离。

必须扫描历史提交中的密钥风险。

---

## 13. 备份与恢复

### 13.1 PostgreSQL

- PITR；
- 每日快照；
- 跨可用区备份；
- 定期 restore drill；
- clinical/runtime schema 一致恢复点记录。

### 13.2 Object Storage

- versioning；
- encryption；
- lifecycle；
- delete marker；
- checksum validation。

### 13.3 Neo4j

仅在生产保留时建立备份和恢复 Runbook。

### 13.4 Redis

Redis 数据默认可重建；仅 Lease/缓存不要求与临床库一致恢复。

### 13.5 恢复验收

- RPO/RTO 明确；
- 恢复后 Thread/Checkpoint 与 CDP 版本一致性扫描；
- Outbox/Reconciliation 执行；
- 未知 External Action 人工处理。

---

## 14. 环境与 Compose 重构

建议拆分：

```text
compose.base.yml
compose.legacy.yml
compose.new-runtime.yml
compose.observability.yml
compose.dev.yml
```

每个服务增加：

- healthcheck；
- readiness；
- resource limits；
- non-root user；
- read-only filesystem where possible；
- secret injection；
- structured log；
- trace propagation；
- dependency condition。

开发环境必须能一条命令启动第一条纵向切片，不要求同时启动所有旧服务。

---

## 15. 数据迁移测试

### 15.1 单元测试

- 每个旧字段 mapper；
- 无效 JSON；
- 缺字段；
- 类型变化；
- 未知枚举；
- 大 CLOB。

### 15.2 集成测试

- Oracle/MySQL 导出；
- PostgreSQL 导入；
- 版本链；
- rollback；
- retry；
- partial failure。

### 15.3 E2E

- 旧 Encounter 历史查看；
- 新 Encounter 问诊；
- 旧 API 兼容；
- 服务重启 Resume；
- 数据库恢复；
- Shadow compare。

### 15.4 对账阈值

进入切流前：

- 关键身份映射 100%；
- 关键红旗和分诊字段 100%；
- 未映射关键字段 0；
- 非关键未映射字段有清单和处理策略；
- 版本链无断裂；
- checksum 一致；
- 临床抽样评审通过。

---

## 16. 当前建议决策

| 组件 | 建议 |
|---|---|
| MySQL/Oracle | 过渡期保留，旧系统只读/兼容 |
| PostgreSQL | 新合同、临床状态和 Checkpoint 目标主库 |
| Redis | 保留，收敛为短期协调和缓存 |
| Neo4j | 有证据收益时保留，不作首版前置 |
| Milvus | 首版不启用，先用 pgvector |
| Nacos | 旧微服务过渡期保留，收拢后评估下线 |
| Feign | Java→Runtime/Tool 的过渡调用可保留 |
| 自定义 Trace | AgentEvent/UI 可复用，技术 Trace 迁至 OTel |
| Object Storage | 新增，为 SourceArtifact 提供原始内容存储 |
| OTel Stack | 新增并作为统一技术遥测主干 |

最终数据库和 Nacos 决策必须通过 ADR，不以本文档单方面替代架构评审。
