# AIdoctor 目标能力覆盖矩阵

> 文档状态：Draft v2.5 Coverage Matrix  
> 更新时间：2026-07-29  
> 目的：证明总体路线没有遗漏目标能力，并为每项能力指定模块、阶段、契约、实现和测试。

---

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
- `DESIGNED`：有稳定设计；
- `IMPLEMENTING`：正在实现；
- `VALIDATED`：通过阶段验收；
- `DEFERRED`：明确不属于当前范围；
- `BLOCKED`：缺少决策或外部条件。

任何专题方案新增能力都必须先进入本矩阵，不能只写在长文中。

---

## 2. 核心流程覆盖

| 能力 | Owner | Phase | 核心契约 | 实现位置 | 必测场景 | 状态 |
|---|---|---|---|---|---|---|
| 创建 Encounter | Business | A/B | Encounter | Java Business Service | 正常、重复、权限失败 | DESIGNED |
| 创建 Thread/Run | Durable/Runtime | C | ThreadRecord/RunRecord | Python Runtime | 创建、重复、取消 | DESIGNED |
| Capability 选择 | Safety/Governance | B/C | CapabilityEnvelope | Capability Registry | 支持、超范围、停用 | DESIGNED |
| Consent 校验 | Business/Safety | A/B | ConsentScope | Java Policy Adapter | 有效、过期、撤销 | PLANNED |
| 输入质量检查 | Safety/Evidence | B/D | InputQualityAssessment | Safety/Input Gate | 空输入、模糊文件、缺字段 | DESIGNED |
| Context Assembly | Context | C/D | ContextEnvelope | Python package | token 超限、关键事实 pin | DESIGNED |
| 临床概念抽取 | Clinical Intelligence | B/C | ObservationCandidate | Parsing Adapter | 正常、否定、时间、歧义 | DESIGNED |
| 临床状态提交 | Clinical State | B | StatePatch/CommitResult | State Committer | 成功、部分、拒绝、冲突 | DESIGNED |
| Mandatory Safety | Safety | B/C | TriageAssessment | Safety Engine | 正常、红旗、输入不足 | DESIGNED |
| 诊断候选更新 | Clinical Intelligence | D | DiagnosticHypothesis | Inference Engine | 支持/反对/不确定 | DESIGNED |
| 信息缺口更新 | Clinical Intelligence | C/D | InformationGap | Question Policy | 缺口排序、重复问题 | DESIGNED |
| 下一动作选择 | Runtime/Intelligence | C | NextAction/QuestionDecision | LangGraph Router | ask/tool/review/stop | DESIGNED |
| Tool 执行 | Governance/Runtime | C/D | ToolExecutionRequest/ToolResult | Tool Runtime | 超时、失败、切换、降级 | DESIGNED |
| Skill 执行 | Governance/Runtime | F | ClinicalSkill/SkillRun | Skill Runtime | 版本、权限、中断 | PLANNED |
| 保存 Checkpoint | Durable | C | CheckpointMetadata | PostgreSQL Checkpointer | 正常、失败、重启 | DESIGNED |
| 创建 Interrupt | Durable | C/E | InterruptRecord | Runtime | 用户、文件、医生 | DESIGNED |
| Resume | Durable | C/E | ResumeRequest/ResumeDecision | Runtime | 重复、并发、过期 | DESIGNED |
| 停止条件 | Runtime/Intelligence | C/D | StopDecision | Graph Node | 已足够、超范围、不安全 | DESIGNED |
| Delivery 生成 | Business/Delivery | C/E | DeliveryPackage | Delivery Builder | 患者/医生/系统一致 | DESIGNED |
| Care Navigation | Business/Intelligence | E | CarePath | Care Delivery | 时效、地点不足、升级 | PLANNED |
| Follow-up | Business | E | FollowUpPlan | Follow-up Worker | 到期、取消、升级 | PLANNED |

---

## 3. 临床数据与事实治理

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| Encounter CDP | Clinical State | B | EncounterCDP | 版本、读取、快照 | DESIGNED |
| Observation 来源 | Clinical State | B | ClinicalObservation/SourceArtifact | 无来源拒绝 | DESIGNED |
| Candidate 与 Fact 分离 | State Committer | B | ObservationStatus | 模型输出不自动确认 | DESIGNED |
| 患者原话保存 | Clinical State | B | SourceArtifact | checksum、Consent | PLANNED |
| 医生确认 | State/Business | E | AssertionActor/ReviewDecision | approve/edit/reject | DESIGNED |
| 冲突事实 | Clinical State | D | ObservationConflict | 冲突不静默覆盖 | PLANNED |
| 更正与失效 | Clinical State | D/F | Correction/Supersession | 历史可追溯 | PLANNED |
| 长期患者记录 | Clinical State | D | PatientLongitudinalRecord | 跨 Encounter、撤销 | DESIGNED |
| Promotion Policy | Clinical State | D | PromotionDecision | 推断不得自动晋升 | DESIGNED |
| Evidence Ledger | Clinical State | B/D | EvidenceEntry | 来源、方向、强度 | DESIGNED |
| 数据保留与删除 | State/Business | F | RetentionPolicy | 删除、legal hold | PLANNED |
| Tenant 隔离 | Business/State | A-F | TenantContext | 跨租户为零 | PLANNED |
| 患者隔离 | State/RAG | D-F | PatientScope | 跨患者为零 | DESIGNED |

---

## 4. Safety 与 Policy

| 能力 | Owner | Phase | 契约/规则 | 测试 | 状态 |
|---|---|---|---|---|---|
| 呼吸道红旗 | Safety | B | SafetyRule/TriageAssessment | 红旗病例集 | DESIGNED |
| 特殊人群 | Safety | B/D | PatientRiskProfile | 孕产、儿童、老人等 | PLANNED |
| 输入安全 | Safety | B/C | InputSafetyDecision | 注入、恶意内容 | DESIGNED |
| 输出安全 | Safety/Delivery | C/E | OutputSafetyDecision | 越权建议、虚假确定性 | DESIGNED |
| Capability 范围 | Safety | B | CapabilityEnvelope | out of scope | DESIGNED |
| Tool 权限 | Governance/Safety | C | ToolPolicyDecision | 未批准 Tool 拒绝 | DESIGNED |
| Model 权限 | Governance/Safety | C/F | ModelRouteDecision | PHI/驻留/风险 | DESIGNED |
| Skill 权限 | Governance/Safety | F | SkillPolicyDecision | 版本、Owner、范围 | PLANNED |
| Human Review 触发 | Safety/Business | E | ReviewRequirement | 高风险强制审核 | DESIGNED |
| Emergency Guidance | Safety/Delivery | B/E | EmergencyGuidance | 不延迟就医 | DESIGNED |
| Planner 不可降级风险 | Safety | B/C | SafetyOverride | 高风险不可被覆盖 | DESIGNED |
| 安全策略版本 | Safety | F | SafetyPolicyVersion | 恢复时版本变化 | PLANNED |

---

## 5. Clinical Intelligence

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| 症状抽取 | Intelligence | B/C | ObservationCandidate | F1/人工评审 | DESIGNED |
| 否定/不确定识别 | Intelligence | B/C | AssertionStatus | 否定、疑似、家族史 | PLANNED |
| 时间和严重度 | Intelligence | B/C | TemporalValue/Severity | 时间表达 | PLANNED |
| 概念归一化 | Intelligence | B/C | ConceptReference | 编码准确率 | DESIGNED |
| 诊断候选 | Intelligence | D | DiagnosticHypothesis | top-k、必须排除 | DESIGNED |
| 支持/反对证据 | Intelligence | D | HypothesisEvidenceLink | 证据方向 | DESIGNED |
| 信息缺口 | Intelligence | C/D | InformationGap | 必须/重要/可选 | DESIGNED |
| 问题价值 | Intelligence | C/D | QuestionDecision | 信息增益/负担 | DESIGNED |
| 重复问题避免 | Intelligence/Context | C | AskedQuestionRecord | 多轮回归 | DESIGNED |
| 停止判断 | Intelligence | C/D | StopDecision | 足够/无收益/风险 | DESIGNED |
| 解释结构 | Intelligence/Delivery | D/E | ClinicalExplanation | 不暴露 CoT | DESIGNED |
| Care Path 推理 | Intelligence | E | CarePathDecision | 分诊一致 | PLANNED |
| Follow-up 推理 | Intelligence | E | FollowUpDecision | 恶化升级 | PLANNED |

---

## 6. Evidence Intelligence 与 RAG

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| Patient History Retrieval | Evidence/State | D | PatientHistoryQuery/Result | 患者隔离 | DESIGNED |
| Medical Knowledge Retrieval | Evidence | D | MedicalEvidenceQuery | 无结果、低质量 | DESIGNED |
| 两类 RAG 隔离 | Evidence | D | RetrievalScope | 交叉污染为零 | DESIGNED |
| 白名单来源 | Evidence | D | KnowledgeSource | 非白名单拒绝 | DESIGNED |
| Ingestion | Evidence | D/F | KnowledgeRelease | 增量、失败、回滚 | PLANNED |
| Hybrid Retrieval | Evidence | D | RetrievalResult | recall@k | DESIGNED |
| Reranking | Evidence | D/F | RankedEvidence | nDCG/人工评审 | PLANNED |
| PICO | Evidence | D/F | ClinicalQuestion/PICO | 字段完整性 | DESIGNED |
| Claim-Level Citation | Evidence | D | EvidenceClaim/Citation | 引用支持 Claim | DESIGNED |
| Citation Validation | Evidence | D | CitationValidation | 断链、错引 | DESIGNED |
| 适用人群 | Evidence | D/F | ApplicabilityAssessment | 人群不匹配 | DESIGNED |
| 时效性 | Evidence | D/F | FreshnessAssessment | 旧指南 | PLANNED |
| 证据冲突 | Evidence | D/F | EvidenceConflict | 冲突展示 | DESIGNED |
| EvidencePack | Evidence | D | EvidencePack | 可重现、版本链 | DESIGNED |
| OCR/报告接入 | Evidence/Tool | D | SourceArtifact/InputQuality | 模糊、缺页 | PLANNED |

---

## 7. Context 与 Memory

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| ContextEnvelope | Context | C | ContextEnvelope | 字段授权 | DESIGNED |
| 节点级 Context Policy | Context | C/F | ContextPolicy | 越权字段为零 | DESIGNED |
| Token Budget | Context | C | TokenBudget | 截断/降级 | DESIGNED |
| 最近消息窗口 | Context | C | ConversationWindow | 顺序和去重 | DESIGNED |
| Context Summary | Context | D | ConversationSummary | 事实保真 | DESIGNED |
| Critical Context Pin | Context | C/D | CriticalContextItem | 红旗不丢失 | DESIGNED |
| PHI Redaction | Context/Security | C/F | RedactionDecision | 泄露扫描 | DESIGNED |
| Working Memory | Context | C | WorkingMemory | Thread 生命周期 | PLANNED |
| Episodic Memory | Context | D/F | EpisodicMemory | 来源和过期 | PLANNED |
| Semantic Memory | Context | D/F | SemanticMemory | 推断污染为零 | DESIGNED |
| Operational Memory | Context | F | OperationalMemory | 不保存临床事实 | PLANNED |
| Memory Write Gate | Context | D | MemoryWriteDecision | 拒绝不安全候选 | DESIGNED |
| Memory Recall | Context | D | MemoryRecallResult | recall reason | DESIGNED |
| 更正/过期/删除 | Context | F | MemoryLifecycle | 撤销和冲突 | PLANNED |

---

## 8. Tool、Skill、Prompt 与 Model Governance

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| Tool Registry | Governance | C/D | ToolSpec | 注册、版本、停用 | DESIGNED |
| Tool Context | Governance | C | ToolExecutionRequest | 最小数据 | DESIGNED |
| Tool Result | Governance | C | ToolResult | Schema/错误 | DESIGNED |
| Tool Timeout | Governance | C | RetryPolicy | 超时和取消 | DESIGNED |
| Circuit Breaker | Governance | F | ToolHealthPolicy | 连续失败 | PLANNED |
| Tool Idempotency | Governance/Durable | E/F | IdempotencyKey | 重复调用 | DESIGNED |
| Skill Registry | Governance | F | ClinicalSkill | Owner、版本、审核 | DESIGNED |
| Skill Selection | Governance/Runtime | F | SkillSelectionDecision | 范围和 fallback | PLANNED |
| Prompt Registry | Governance | C/F | PromptRelease | 版本、回滚 | DESIGNED |
| Model Registry | Governance | C/F | ModelSpec | 可用性和数据策略 | DESIGNED |
| Model Router | Governance | C/F | ModelRouteDecision | 质量、成本、PHI | DESIGNED |
| 结构化输出校验 | Governance | C | StructuredOutputResult | 非法 JSON | DESIGNED |
| 模型降级 | Governance/Safety | C/F | FallbackDecision | 高风险静默降级为零 | DESIGNED |
| Prompt Injection | Security | C/F | InjectionDecision | 对抗集 | DESIGNED |
| RAG Injection | Security | D/F | EvidenceSafetyDecision | 恶意文档 | DESIGNED |
| Tool Injection | Security | C/F | ToolPolicyDecision | 参数污染 | DESIGNED |

---

## 9. Durable Execution

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| Thread Lifecycle | Durable | C/F | ThreadStatus | 全状态转换 | DESIGNED |
| Run Lifecycle | Durable | C | RunRecord | 启停失败 | DESIGNED |
| PostgreSQL Checkpointer | Durable | C | CheckpointMetadata | 重启恢复 | DESIGNED |
| Interrupt | Durable | C/E | InterruptRecord | 用户/医生/文件 | DESIGNED |
| Resume Auth | Durable/Business | C/E | ResumeRequest | 越权、过期 | DESIGNED |
| Resume Inbox | Durable | C | InboxRecord | 重复请求 | DESIGNED |
| Thread Lease | Durable | F | ThreadLease | 并发和接管 | DESIGNED |
| CDP 乐观锁 | State/Durable | B/C | expected_cdp_version | 冲突 | DESIGNED |
| Outbox | Durable/State | E/F | OutboxEvent | 事务后发送 | DESIGNED |
| ExternalActionRecord | Durable | E | ExternalActionRecord | timeout unknown | DESIGNED |
| 动作幂等 | Durable | E | IdempotencyKey | 重复预约模拟 | DESIGNED |
| 补偿动作 | Durable/Business | F | CompensationRecord | 失败补偿 | PLANNED |
| Checkpoint Migration | Durable | F | MigrationDecision | 旧 Graph | DESIGNED |
| Reconciliation | Durable | F | ReconciliationFinding | 卡死/孤儿 | DESIGNED |
| Cancel/Expire | Durable | E/F | CancellationRecord | 用户撤销 | PLANNED |
| Replay Sandbox | Durable/Observability | F | ReplayRequest | 无真实副作用 | DESIGNED |

---

## 10. Observability、Audit 与 Evaluation

| 能力 | Owner | Phase | 契约/工具 | 测试 | 状态 |
|---|---|---|---|---|---|
| OTel Context | Observability | C | trace_id/span_id | Java/Python传播 | DESIGNED |
| Collector | Observability | F | OTLP Pipeline | 断网、重试 | DESIGNED |
| Technical Trace | Observability | C/F | OTel Span | 调用链 | DESIGNED |
| Metric | Observability | C/F | Prometheus | 延迟/错误/队列 | DESIGNED |
| Structured Log | Observability | C | Log Schema | trace 关联 | DESIGNED |
| PHI Filter | Security/Observability | C/F | TelemetryPolicy | 敏感字段 | DESIGNED |
| AgentEvent | Observability | C | AgentEvent | 路由/重试/恢复 | DESIGNED |
| ClinicalDecisionRecord | Observability/Clinical | E/F | DecisionRecord | 证据与规则链 | DESIGNED |
| Compliance Audit | Audit | E/F | AuditEvent | 访问/修改/批准 | DESIGNED |
| Simulation Replay | Evaluation | F | ReplayRequest | 版本对比 | DESIGNED |
| Forensic Replay | Audit | F | ForensicReplay | 只读和授权 | PLANNED |
| Clinical Eval | Evaluation | B-F | EvalCase/Report | 病例集 | DESIGNED |
| Safety Eval | Evaluation | B-F | SafetyEval | 红旗漏检 | DESIGNED |
| RAG Eval | Evaluation | D-F | RetrievalEval | recall/citation | DESIGNED |
| Context Eval | Evaluation | C-F | ContextEval | 丢失/泄露 | DESIGNED |
| Resume Eval | Evaluation | C-F | ResumeEval | crash matrix | DESIGNED |
| Security Eval | Evaluation | C-F | AttackCase | 注入/越权 | DESIGNED |
| Patient Simulator | Evaluation | F | SimulatedPatient | OSCE | PLANNED |
| Release Report | Evaluation | F | CapabilityReleaseReport | 每版必带 | DESIGNED |

---

## 11. Business 与 Care Delivery

| 能力 | Owner | Phase | 契约 | 测试 | 状态 |
|---|---|---|---|---|---|
| 用户/患者/医生身份 | Business | A/E | ActorIdentity | 角色权限 | PLANNED |
| Organization/Tenant | Business | A/F | TenantContext | 租户隔离 | PLANNED |
| Consent | Business | A/B/E | ConsentScope | 撤销和过期 | PLANNED |
| ReviewTask | Business | E | ReviewTask | approve/edit/reject | DESIGNED |
| Clinician Console | Frontend/Business | E | ReviewTaskView | 多医生并发 | PLANNED |
| Patient Delivery | Business | C/E | PatientDelivery | 可读性、安全 | DESIGNED |
| Clinician Delivery | Business | E | ClinicianDelivery | 证据和修改 | DESIGNED |
| System Delivery | Business/Observability | E | SystemDelivery | 版本链 | DESIGNED |
| Appointment | Business/Durable | E+ | ExternalActionRecord | 幂等和 unknown | DEFERRED |
| Referral | Business/Durable | E+ | ExternalActionRecord | 医生批准 | DEFERRED |
| Notification | Business/Durable | E | ExternalActionRecord | 重复发送 | PLANNED |
| Follow-up Task | Business | E | FollowUpTask | 到期和升级 | PLANNED |
| Export | Business/Audit | F | ExportRequest | 权限和审计 | PLANNED |

---

## 12. 数据和基础设施

| 能力 | Owner | Phase | 产物 | 验收 | 状态 |
|---|---|---|---|---|---|
| 数据库目标 ADR | Architecture | A | ADR | 决策批准 | BLOCKED |
| 旧 CDP 迁移 | Data | A-E | Migration Job | 对账通过 | PLANNED |
| PostgreSQL Schema | Data | B/C | Flyway/Alembic | 回滚和兼容 | PLANNED |
| Redis Lease/Cache | Platform | C/F | Redis Schema | TTL/隔离 | PLANNED |
| Neo4j 保留评估 | Evidence | A/D | ADR/Benchmark | 有收益才保留 | PLANNED |
| Milvus 去留 | Evidence | A/D | ADR | 与 pgvector 比较 | PLANNED |
| Object Storage | Platform | D | SourceArtifact Store | checksum/加密 | PLANNED |
| Secret Management | Platform | A/F | Secret Policy | 无硬编码 | PLANNED |
| Backup/Restore | Platform | A-F | Runbook | 恢复演练 | PLANNED |
| Data Retention | Data/Compliance | F | Retention Policy | 删除演练 | PLANNED |

---

## 13. 工程和发布

| 能力 | Phase | 验收 | 状态 |
|---|---|---|---|
| Java Compile/Test | A | Maven build | PLANNED |
| Python Dependency Lock | A | reproducible install | PLANNED |
| Frontend Build/Lint/Test | A/C | CI pass | PLANNED |
| Contract Test | A-F | Java/Python parity | DESIGNED |
| Unit Test | A-F | 核心模块阈值 | PLANNED |
| Integration Test | B-F | DB/Redis/Tool | PLANNED |
| E2E Test | A-F | 每阶段闭环 | DESIGNED |
| Security Scan | A-F | secret/SAST/dependency | PLANNED |
| Performance Test | C-F | latency/capacity | PLANNED |
| Migration Rehearsal | A-E | staging dry-run | PLANNED |
| Canary/Shadow | F | compare report | DESIGNED |
| Rollback | A-F | runbook drill | PLANNED |
| Decommission | E-F | no traffic/no refs | DESIGNED |

---

## 14. 明确延期能力

以下能力不是遗漏，而是明确延期：

- 通用所有疾病诊断；
- 儿童、孕产等人群的完整自动路径；
- 自动治疗与处方修改；
- 自由自治多 Agent；
- Agent 自动修改 Prompt/Skill/Policy；
- 全互联网医学搜索；
- 全量医学期刊平台；
- 无限制长期记忆；
- 通用自主浏览器、Shell 或代码执行；
- 真实预约和转诊的第一阶段接入；
- Research Workspace 作为首条主链路前置。

延期能力进入新 Capability 前必须新增独立覆盖项和评估门禁。

---

## 15. 覆盖检查结论

当前 Phase A-F 已覆盖目标系统的主要能力域：

```text
Clinical Workflow
Clinical State
Safety
Clinical Intelligence
Evidence/RAG
Context/Memory
Tool/Skill/Model Governance
Durable Execution
Observability/Audit/Evaluation
Business/Delivery
Data/Infrastructure
Engineering/Release
Frontend Migration
Decommission
```

当前尚未关闭但已经显式进入路线的内容：

- 数据库最终目标；
- 旧 CDP 数据迁移；
- CI/CD 和运行基线；
- 前端与医生端迁移；
- 旧服务双跑和下线；
- 备份恢复和生产运维。

因此后续发现这些问题不属于“遗漏”，而属于本矩阵中尚未完成的计划项。
