# AIdoctor 目标能力覆盖矩阵

> 文档状态：Draft v2.6 Coverage Matrix  
> 更新时间：2026-07-29  
> 目的：证明目标系统、场景扩展、RAG、模型运行时、迁移和生产治理均有唯一归属。

## 1. 使用规则

每项能力必须具有：

```text
Capability
→ Owner Module
→ Delivery Phase
→ Contract
→ Implementation Artifact
→ Test / Evidence
→ Completion Gate
```

状态：

- `PLANNED`：已进入路线；
- `DESIGNED`：已有稳定设计；
- `IMPLEMENTING`：正在实现；
- `VALIDATED`：通过阶段门禁；
- `DEFERRED`：明确不属于当前范围；
- `BLOCKED`：缺少 ADR、数据或外部条件。

任何新能力必须先进入本矩阵，不能只写在专题长文中。

## 2. Capability 与场景扩展

| 能力 | Owner | Phase | 核心契约/产物 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| Capability Registry | Safety/Governance | A/B | CapabilityManifest | 注册、停用、版本、回滚 | DESIGNED |
| Capability Package | Governance/Clinical | A-F | Capability Package 目录 | 缺少资产拒绝发布 | DESIGNED |
| 人群与范围策略 | Safety | A/B | PopulationPolicy/ScopePolicy | 支持、排除、超范围 | DESIGNED |
| Terminology Pack | Intelligence | A-C | Concept/Synonym Mapping | 同义词、缩写、歧义 | DESIGNED |
| Observation Profile | Clinical State | A/B | ObservationFieldSpec | 类型、来源、冲突 | DESIGNED |
| Safety Pack | Safety | A/B | SafetyPolicy/SafetyRule | 红旗、特殊人群、输入不足 | DESIGNED |
| Question Pack | Intelligence | A-C | QuestionPolicy | 必问、区分、重复、停止 | DESIGNED |
| Hypothesis Pack | Intelligence | A/D | HypothesisSpec | 批准候选、must-not-miss | DESIGNED |
| Knowledge Pack | Evidence | A/D | KnowledgePolicy/Release | 非批准来源拒绝 | DESIGNED |
| Runtime Pack | Runtime/Governance | A-C | Tool/Skill/Prompt/Route Allowlist | 越权调用拒绝 | DESIGNED |
| Capability Eval Pack | Evaluation | A-F | EvalSuite/ReleaseReport | 安全、RAG、模型、E2E | DESIGNED |
| Capability 发布生命周期 | Governance | F | CapabilityRelease | Shadow、Assist、Restricted、Active | DESIGNED |
| Capability 版本绑定 | Business/Runtime | B/C | CapabilitySnapshot | 运行中不静默升级 | DESIGNED |
| adult_respiratory_v1 | All | A-F | 首个 Capability Package | 咳嗽+喘、红旗、Resume、Delivery | DESIGNED |
| 后续场景扩展 | Governance | F+ | 新 Capability Package | 不修改通用安全骨架 | PLANNED |

## 3. 核心流程

| 能力 | Owner | Phase | 核心契约 | 实现位置 | 必测场景 | 状态 |
|---|---|---|---|---|---|---|
| 创建 Encounter | Business | A/B | Encounter | Java Business | 正常、重复、权限失败 | DESIGNED |
| 创建 Thread/Run | Durable/Runtime | C | ThreadRecord/RunRecord | Python Runtime | 创建、取消、重启 | DESIGNED |
| Consent 校验 | Business/Safety | A/B | ConsentScope | Java Policy Adapter | 有效、过期、撤销 | PLANNED |
| 输入质量检查 | Safety/Evidence | B/D | InputQualityAssessment | Input Gate | 空输入、模糊资料 | DESIGNED |
| Context Assembly | Context | C/D | ContextEnvelope | Python package | token、PHI、Critical Pin | DESIGNED |
| 临床概念抽取 | Intelligence | B/C | ObservationCandidate | Parsing Adapter/Model Runtime | 否定、时间、歧义 | DESIGNED |
| 临床状态提交 | Clinical State | B | StatePatch/CommitResult | State Committer | 冲突、权限、版本 | DESIGNED |
| Mandatory Safety | Safety | B/C | TriageAssessment | Safety Engine | 正常、红旗、输入不足 | DESIGNED |
| 诊断候选更新 | Intelligence | D | DiagnosticHypothesis | Clinical Engine | 支持、反对、must-not-miss | DESIGNED |
| 信息缺口更新 | Intelligence | C/D | InformationGap | Question Policy | 排序、重复问题 | DESIGNED |
| 下一动作选择 | Runtime/Intelligence | C | NextAction/QuestionDecision | LangGraph Router | ask/tool/review/stop | DESIGNED |
| Tool 执行 | Governance/Runtime | C/D | ToolExecutionRequest/ToolResult | Tool Runtime | 超时、失败、降级 | DESIGNED |
| Skill 执行 | Governance/Runtime | F | ClinicalSkill/SkillRun | Skill Runtime | 版本、权限、中断 | PLANNED |
| Checkpoint | Durable | C | CheckpointMetadata | PostgreSQL Checkpointer | 保存失败、重启 | DESIGNED |
| Interrupt/Resume | Durable | C/E | InterruptRecord/ResumeRequest | Runtime | 重复、并发、过期 | DESIGNED |
| 停止条件 | Runtime/Intelligence | C/D | StopDecision | Graph Node | 足够、无收益、风险 | DESIGNED |
| Delivery | Business | C/E | DeliveryPackage | Delivery Builder | 三类输出一致 | DESIGNED |
| Care Navigation | Business/Intelligence | E | CarePath | Care Delivery | 紧急度、地点不足 | PLANNED |
| Follow-up | Business | E | FollowUpPlan | Follow-up Worker | 到期、取消、恶化 | PLANNED |

## 4. 临床数据与事实治理

| 能力 | Owner | Phase | 契约 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| Encounter CDP | Clinical State | B | EncounterCDP | 版本、快照、读取 | DESIGNED |
| Observation 来源 | Clinical State | B | ClinicalObservation/SourceArtifact | 无来源拒绝 | DESIGNED |
| Candidate 与 Fact 分离 | State Committer | B | VerificationStatus | 模型不自动确认 | DESIGNED |
| 患者原话保存 | Clinical State | B | SourceArtifact | checksum、Consent | PLANNED |
| 医生确认 | State/Business | E | ReviewDecision | approve/edit/reject | DESIGNED |
| 冲突事实 | Clinical State | D | ObservationConflict | 不静默覆盖 | PLANNED |
| 更正与失效 | Clinical State | D/F | Supersession/Invalidation | 历史可追溯 | PLANNED |
| 长期患者记录 | Clinical State | D | PatientLongitudinalRecord | 跨 Encounter、撤销 | DESIGNED |
| Promotion Policy | Clinical State | D | PromotionDecision | 推断不自动晋升 | DESIGNED |
| Evidence Ledger | Clinical State | B/D | EvidenceEntry | 来源、方向、强度 | DESIGNED |
| Tenant/Patient 隔离 | Business/State/RAG | A-F | TenantContext/PatientScope | 跨租户/患者为零 | DESIGNED |
| 数据保留与删除 | State/Compliance | F | RetentionPolicy | 删除、legal hold | PLANNED |

## 5. Safety 与 Policy

| 能力 | Owner | Phase | 契约/规则 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| 呼吸道红旗 | Safety | B | SafetyRule/TriageAssessment | 红旗病例集 | DESIGNED |
| 特殊人群 | Safety | B/D | PatientRiskProfile | 排除和升级 | PLANNED |
| 输入安全 | Safety | B/C | InputSafetyDecision | 注入、恶意内容 | DESIGNED |
| 输出安全 | Safety/Delivery | C/E | OutputSafetyDecision | 虚假确定性、越权建议 | DESIGNED |
| Capability 范围 | Safety | B | CapabilityEnvelope | out_of_scope | DESIGNED |
| Tool/Skill 权限 | Governance/Safety | C/F | PolicyDecision | 未批准拒绝 | DESIGNED |
| Model 权限 | Governance/Safety | C/F | ModelRouteDecision | PHI、地区、风险 | DESIGNED |
| Human Review | Safety/Business | E | ReviewRequirement | 高风险强制审核 | DESIGNED |
| Emergency Guidance | Safety/Delivery | B/E | EmergencyGuidance | 不延迟就医 | DESIGNED |
| Planner 不可覆盖 Safety | Safety | B/C | SafetyOverride | 绕过尝试 | DESIGNED |
| 安全策略版本 | Safety | F | SafetyPolicyVersion | Resume 版本变化 | PLANNED |

## 6. Clinical Intelligence

| 能力 | Owner | Phase | 契约 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| 症状/体征抽取 | Intelligence | B/C | ObservationCandidate | F1、人工评审 | DESIGNED |
| 否定/不确定/时间/程度 | Intelligence | B/C | Assertion/Temporal/Severity | 多种表达 | PLANNED |
| 概念归一化 | Intelligence | B/C | ConceptReference | 编码准确率 | DESIGNED |
| 诊断候选 | Intelligence | D | DiagnosticHypothesis | 批准范围、must-not-miss | DESIGNED |
| 支持/反对证据 | Intelligence | D | HypothesisEvidenceLink | 证据方向 | DESIGNED |
| 信息缺口 | Intelligence | C/D | InformationGap | 必须/重要/可选 | DESIGNED |
| 问题价值和表达 | Intelligence | C/D | QuestionDecision/Wording | 语义保持、无新增事实 | DESIGNED |
| 重复问题避免 | Intelligence/Context | C | AskedQuestionRecord | 多轮回归 | DESIGNED |
| 停止判断 | Intelligence | C/D | StopDecision | 足够、超范围、不安全 | DESIGNED |
| Care/Follow-up Reasoning | Intelligence | E | CarePathDecision/FollowUpDecision | 分诊一致、恶化升级 | PLANNED |

## 7. Evidence Intelligence 与 RAG

| 能力 | Owner | Phase | 契约/产物 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| Patient History Retrieval | Evidence/State | D | PatientHistoryQuery/Result | 患者隔离 | DESIGNED |
| Medical Knowledge Retrieval | Evidence | D | RetrievalPlan/Result | 无结果、低质量 | DESIGNED |
| 两类 RAG 隔离 | Evidence | D | RetrievalScope | 交叉污染为零 | DESIGNED |
| Source Registry | Evidence | A/D | KnowledgeSourceRegistration | 许可、版本、地区 | DESIGNED |
| 白名单来源 | Evidence | D | Source Policy | 非白名单拒绝 | DESIGNED |
| Knowledge Ingestion | Evidence | D/F | IngestionRun | 失败、增量、回滚 | DESIGNED |
| KnowledgeChunk | Evidence | D | RespiratoryKnowledgeChunk | 推荐、表格、限制不丢失 | DESIGNED |
| Knowledge Release | Evidence | D | KnowledgeRelease | 绑定、回滚、撤回 | DESIGNED |
| PostgreSQL Metadata | Evidence/Data | D | knowledge schema | 版本和一致性 | DESIGNED |
| pgvector | Evidence | D | Embedding Index | recall、版本隔离 | DESIGNED |
| BM25 | Evidence | D | Lexical Index Adapter | 术语和缩写 | DESIGNED |
| Hybrid Retrieval | Evidence | D | RetrievalResult | recall@k、no-result | DESIGNED |
| Reranking | Evidence | D/F | RerankerSpec | nDCG、延迟 | DESIGNED |
| Query Planning/PICO | Evidence | D | RetrievalPlan | 模板、范围、过滤 | DESIGNED |
| Claim-Level Citation | Evidence | D | EvidenceClaim/SourceSpan | Claim 对齐 | DESIGNED |
| Citation Validation | Evidence | D | CitationValidation | 错引、夸大、断链 | DESIGNED |
| Applicability/Freshness | Evidence | D/F | Applicability/Freshness | 人群、地区、过期 | DESIGNED |
| Evidence Conflict | Evidence | D/F | EvidenceConflict | 冲突不静默裁决 | DESIGNED |
| EvidencePack | Evidence | D | EvidencePack | 可复现版本链 | DESIGNED |
| OCR/Artifact | Evidence/Tool | D | SourceArtifact/InputQuality | 模糊、缺页、身份不符 | PLANNED |
| Knowledge Graph Schema | Evidence | A/D | Graph Schema/Release | 来源链、关系有效期 | DESIGNED |
| Knowledge Graph 生产启用 | Evidence/Evaluation | D/F | Graph Benefit Report | 净收益、错误扩展 | BLOCKED |

## 8. Context 与 Memory

| 能力 | Owner | Phase | 契约 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| ContextEnvelope | Context | C | ContextEnvelope | 字段授权、可复现 | DESIGNED |
| Context Policy/Token Budget | Context | C/F | ContextPolicy/TokenBudget | 越权、截断、降级 | DESIGNED |
| Recent Window/Summary | Context | C/D | ConversationWindow/Summary | 顺序、事实保真 | DESIGNED |
| Critical Context Pin | Context | C/D | CriticalContextItem | 红旗不丢失 | DESIGNED |
| PHI Redaction | Context/Security | C/F | RedactionDecision | 泄露扫描 | DESIGNED |
| Working/Episodic/Semantic Memory | Context | C-F | MemoryItem | 来源、过期、污染 | PLANNED |
| Memory Write Gate | Context | D | MemoryWriteDecision | 不安全候选拒绝 | DESIGNED |
| Memory Recall | Context | D | MemoryRecallResult | reason、patient scope | DESIGNED |
| 更正/过期/删除 | Context | F | MemoryLifecycle | 撤销和冲突 | PLANNED |

## 9. Prompt、Model、Tool 与 Skill Governance

| 能力 | Owner | Phase | 契约/产物 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| Tool Registry/Context/Result | Governance | C/D | ToolSpec/ToolResult | 注册、权限、错误 | DESIGNED |
| Tool Timeout/Circuit/Idempotency | Governance/Durable | C-F | Retry/Health/Idempotency | 超时、连续失败、重复 | DESIGNED |
| Skill Registry/Selection | Governance | F | ClinicalSkill/Selection | 版本、范围、fallback | DESIGNED |
| Prompt Registry | Governance | A/C | PromptSpec | 版本、状态、回滚 | DESIGNED |
| Prompt Loader | Governance | C | PromptLoadRequest/Bundle | 缺失、过期、Capability 不匹配 | DESIGNED |
| Prompt Builder | Governance/Context | C | RenderedModelRequest | 指令/数据隔离、token | DESIGNED |
| Prompt 发布流程 | Governance/Evaluation | C/F | PromptRelease | review、eval、shadow | DESIGNED |
| Model Registry | Governance | A/C | ModelSpec | 能力、PHI、版本、健康 | DESIGNED |
| Model Router | Governance | C/F | ModelRoutePolicy/Decision | 风险、成本、地区、无模型 | DESIGNED |
| Model Gateway | Governance | C | ModelInvocationRequest/Result | timeout、rate limit、provider failure | DESIGNED |
| Provider Adapter | Governance | C | ProviderAdapter | 协议、usage、错误映射 | DESIGNED |
| Structured Output Validator | Governance | C | ValidationResult | 非法 JSON、越界内容 | DESIGNED |
| Prompt/Model Compatibility | Governance | C/F | CompatibilityRecord | 未评估组合拒绝 | DESIGNED |
| 模型 Retry/Fallback | Governance/Safety | C/F | FallbackDecision | 高风险静默降级为零 | DESIGNED |
| Token/Cost/Latency | Governance/Observability | C/F | UsageRecord | 统计、预算、限额 | DESIGNED |
| Prompt Injection | Security | C/F | InjectionDecision | 患者/RAG/Tool 对抗集 | DESIGNED |
| common/aidoctor_llm Adapter | Governance | A/C | LegacyProviderAdapter | 旧调用迁移 | DESIGNED |

## 10. 模型调用路线

| 路线 | Phase | 风险 | 输出 | Fallback | 状态 |
|---|---|---|---|---|---|
| Observation Extraction | C | 中 | ObservationCandidate | 规则/澄清 | DESIGNED |
| Terminology Normalization | C | 中 | NormalizationCandidate | 原文+未归一化 | DESIGNED |
| Red Flag Candidate Extraction | C | 高辅助 | RedFlagCandidate | 确定性 Safety | DESIGNED |
| Information Gap Candidate | C | 中 | InformationGapCandidate | Capability 必问清单 | DESIGNED |
| Question Wording | C | 低 | QuestionWordingResult | 固定模板 | DESIGNED |
| Encounter Summary | C/D | 中 | SummaryCandidate | 最近窗口 | DESIGNED |
| Limited Hypothesis Assistance | D | 高辅助 | HypothesisCandidate | 规则/KG/统计 | DESIGNED |
| Evidence Query/Claim | D | 中 | RetrievalPlan/ClaimCandidate | 固定模板/无 Claim | DESIGNED |
| Citation Validation | D | 高 | CitationValidation | unsupported/review | DESIGNED |
| Patient Rewrite | C/E | 低 | PatientDeliveryDraft | 模板 | DESIGNED |
| Clinician Summary | E | 高 | ClinicianSummaryDraft | 结构化直出+审核 | DESIGNED |
| 自动治疗/处方 | F+ | 高 | N/A | 禁止 | DEFERRED |

## 11. Durable Execution

| 能力 | Owner | Phase | 契约 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| Thread/Run Lifecycle | Durable | C | ThreadStatus/RunRecord | 全状态转换 | DESIGNED |
| PostgreSQL Checkpointer | Durable | C | CheckpointMetadata | 重启恢复 | DESIGNED |
| Interrupt/Resume Auth/Inbox | Durable | C/E | Interrupt/Resume/Inbox | 重复、并发、过期 | DESIGNED |
| Thread Lease | Durable | F | ThreadLease | 接管和心跳 | DESIGNED |
| CDP 乐观锁 | State/Durable | B/C | expected_cdp_version | 冲突 | DESIGNED |
| Outbox/External Action/Idempotency | Durable | E/F | OutboxEvent/ActionRecord | timeout unknown、重复 | DESIGNED |
| Checkpoint Migration/Reconciliation | Durable | F | Migration/Finding | 旧 Graph、卡死 | DESIGNED |
| Replay Sandbox | Durable/Eval | F | ReplayRequest | 无真实副作用 | DESIGNED |

## 12. Observability、Audit 与 Evaluation

| 能力 | Owner | Phase | 契约/工具 | 必测场景 | 状态 |
|---|---|---|---|---|---|
| OTel Context/Trace/Metric/Log | Observability | C/F | OTel/Prometheus | Java-Python、错误关联 | DESIGNED |
| PHI Telemetry Filter | Security | C/F | TelemetryPolicy | 敏感字段 | DESIGNED |
| AgentEvent | Observability | C | AgentEvent | 路由、重试、恢复 | DESIGNED |
| ClinicalDecisionRecord | Clinical/Audit | E/F | DecisionRecord | 证据和规则链 | DESIGNED |
| Compliance Audit | Audit | E/F | AuditEvent | 访问、修改、批准 | DESIGNED |
| Clinical/Safety/RAG/Context/Resume/Security Eval | Evaluation | B-F | EvalReport | 各专项门禁 | DESIGNED |
| Model/Prompt Eval | Evaluation | C-F | RouteEval/PromptEval | 升级回归、schema、安全 | DESIGNED |
| Capability Release Report | Evaluation | F | ReleaseReport | 每版本必带 | DESIGNED |
| Patient Simulator | Evaluation | F | SimulatedPatient | OSCE | PLANNED |

## 13. Business、Frontend、数据与工程

| 能力 | Phase | 验收 | 状态 |
|---|---|---|---|
| Identity/Tenant/Consent | A-E | 角色、租户和授权测试 | PLANNED |
| ReviewTask/Clinician Console | E | approve/edit/reject、多医生并发 | DESIGNED |
| Patient/Clinician/System Delivery | C/E | 同一事实基础、不同披露 | DESIGNED |
| Follow-up/Notification | E | 到期、取消、重复发送 | PLANNED |
| 数据库目标 ADR | A | 决策批准 | BLOCKED |
| 旧 CDP 迁移 | A-E | 对账、双写、回滚 | PLANNED |
| PostgreSQL/Redis/Object Storage | B-F | migration、TTL、加密、恢复 | PLANNED |
| Neo4j/Milvus 去留 | A/D | benchmark/ADR | BLOCKED |
| Java/Python/Frontend Build | A | CI 可复现 | PLANNED |
| Contract Test | A-F | Java/Python/TS parity | DESIGNED |
| Unit/Integration/E2E/Security | A-F | 每阶段门禁 | DESIGNED |
| Shadow/Canary/Rollback | F | 报告和演练 | DESIGNED |
| Decommission | E/F | 无流量、无引用、可恢复 | DESIGNED |

## 14. 明确延期

以下不是遗漏：

- 通用所有疾病自动诊断；
- 儿童、孕产完整自动路径；
- 自动治疗与处方修改；
- 自由自治多 Agent；
- Agent 自动修改 Prompt、Skill 或 Policy；
- 全互联网医学搜索；
- 未经许可的全文抓取；
- 无限制长期记忆；
- 首阶段真实预约和转诊；
- 未通过净收益评估的知识图谱生产依赖。

## 15. 覆盖结论

当前路线已经覆盖：

```text
Clinical Workflow
Clinical State
Safety
Clinical Intelligence
Capability Expansion
Evidence / Patient RAG / Medical RAG
Knowledge Graph Governance
Context / Memory
Prompt / Model Runtime
Tool / Skill Governance
Durable Execution
Observability / Audit / Evaluation
Business / Frontend
Data / Infrastructure
Engineering / Release / Decommission
```

尚未完成但已显式进入路线的主要事项：

- 真实编译、启动和 E2E 基线；
- 数据库与框架 ADR；
- 首批 JSON Schema；
- 白名单来源的具体版本和许可审核；
- Embedding/Reranker 评估选型；
- 知识图谱净收益评估；
- Prompt/Model 真实发布配置；
- 旧服务双跑、迁移和下线；
- 备份、恢复和生产运维。

这些属于计划项，不再属于架构遗漏。