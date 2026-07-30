# AIdoctor 模型调用与路由矩阵

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29

## 1. 目的

本文明确：

- 哪些任务允许调用大模型；
- 哪些任务必须使用确定性代码；
- 每类模型调用的输入、输出、风险和失败策略；
- 如何通过 Model Registry、Model Router 和 Prompt Registry 统一治理；
- 首个 `adult_respiratory_v1` Capability 的模型调用边界。

本文不固定商业模型名称。具体模型通过 Registry 和评估报告发布，业务代码只引用稳定的 `route_id`。

## 2. 总原则

```text
LLM is a governed component,
not the owner of clinical truth or workflow safety.
```

所有模型调用必须满足：

- 通过 Model Gateway；
- 使用版本化 Prompt Release；
- 使用明确 ContextEnvelope；
- 声明输出 Schema；
- 经过 Structured Output Validator；
- 经过 Capability 和 Safety 校验；
- 记录 model_id、prompt_version、context_hash、token、cost 和 latency；
- 不直接写数据库；
- 不直接执行外部动作；
- 不修改 Capability、Prompt、Skill 或长期记忆。

## 3. 任务分类

### 3.1 确定性任务

原则上不调用 LLM：

- 身份、权限和 Consent；
- State Committer；
- CDP 版本检查；
- Checkpoint、Interrupt、Resume；
- 幂等、Outbox/Inbox；
- 红旗规则最终执行；
- 分诊阈值最终执行；
- Tool 权限；
- 数据库查询；
- Audit；
- Trace；
- Knowledge Release 过滤；
- Citation 来源存在性检查。

### 3.2 模型辅助任务

允许模型参与，但结果是 Candidate：

- 自然语言结构化抽取；
- 术语归一化辅助；
- 对话摘要；
- 问题自然语言表达；
- 有限候选证据解释；
- PICO 或检索问题构建；
- Claim 提取；
- 患者语言改写；
- 医生摘要草稿。

### 3.3 高风险模型辅助任务

模型只能在确定性安全路径、Clinical Engine 和医生审核下辅助：

- 高风险临床推理；
- must-not-miss 证据解释；
- 高风险医生交接摘要；
- 治疗、药物和检查建议的未来扩展。

无已验证模型时，必须降级到固定安全路径或人工审核，不能自动选择未验证模型。

## 4. Route ID 规范

```text
<domain>.<task>.<risk-tier>.<major-version>
```

例如：

```text
clinical.extract_observations.medium.v1
clinical.question_wording.low.v1
clinical.summarize_encounter.medium.v1
evidence.classify_question.medium.v1
evidence.extract_claims.medium.v1
delivery.patient_rewrite.low.v1
review.clinician_summary.high.v1
```

业务模块只能使用 Capability Allowlist 中的 `route_id`。

## 5. 模型调用矩阵

| ID | 任务 | Owner | LLM | 风险 | 输入 | 输出 | 主路线 | 失败策略 |
|---|---|---|---|---|---|---|---|---|
| MC-01 | 患者文本结构化抽取 | Clinical Intelligence | 是 | 中 | 原始消息、最小上下文、术语表 | `ObservationCandidate[]` | `clinical.extract_observations.medium.v1` | 规则抽取/请求澄清，不写事实 |
| MC-02 | 医学术语归一化 | Clinical Intelligence | 可选 | 中 | Candidate、术语候选 | `NormalizationCandidate[]` | `clinical.normalize_terms.medium.v1` | 保留原文并标记未归一化 |
| MC-03 | 否定、时间、程度识别 | Clinical Intelligence | 可选 | 中 | 文本、候选 Span | 结构化修饰信息 | `clinical.extract_modifiers.medium.v1` | 规则或 unknown |
| MC-04 | 红旗线索抽取 | Safety | 辅助 | 高 | 当前消息、关键上下文 | `RedFlagCandidate[]` | `safety.extract_red_flags.high.v1` | 确定性规则继续；不静默忽略 |
| MC-05 | 红旗与分诊最终判断 | Safety | 否 | 高 | Observation、规则、生命体征 | `TriageAssessment` | N/A | 固定安全路径/人工审核 |
| MC-06 | 信息缺口候选 | Clinical Intelligence | 可选 | 中 | Observation、候选、规则 | `InformationGapCandidate[]` | `clinical.identify_gaps.medium.v1` | 使用 Capability 必问清单 |
| MC-07 | 下一问题选择 | Question Policy | 否/可选排序 | 中 | Gap、Safety、已问问题 | `QuestionDecision` | `clinical.rank_questions.medium.v1` | 确定性评分或固定顺序 |
| MC-08 | 下一问题自然语言表达 | Clinical Intelligence | 是 | 低 | QuestionDecision、语言偏好 | `QuestionWordingResult` | `clinical.question_wording.low.v1` | 模板化问题 |
| MC-09 | 长对话结构化摘要 | Context & Memory | 是 | 中 | 历史消息、已确认事实 | `EncounterSummaryCandidate` | `clinical.summarize_encounter.medium.v1` | 保留最近窗口，禁止覆盖事实 |
| MC-10 | Memory Candidate 提取 | Context & Memory | 可选 | 中 | 已完成 Encounter、来源 | `MemoryCandidate[]` | `memory.extract_candidates.medium.v1` | 不写长期记忆 |
| MC-11 | 有限候选辅助生成 | Clinical Intelligence | 可选 | 高 | Observation、批准候选表 | `HypothesisCandidate[]` | `clinical.hypothesis_support.high.v1` | 规则/KG/统计引擎；仅批准候选 |
| MC-12 | 候选支持与反对证据解释 | Clinical Intelligence | 是 | 高 | 候选、Evidence Ledger | 结构化支持/反对项 | `clinical.explain_hypotheses.high.v1` | 输出 unavailable，进入审核 |
| MC-13 | 临床问题分类 | Evidence Intelligence | 是/规则 | 中 | 规范化问题 | `ClinicalQuestionType` | `evidence.classify_question.medium.v1` | 规则模板或 out_of_scope |
| MC-14 | PICO/Query Plan 辅助 | Evidence Intelligence | 是 | 中 | 问题、患者人群 | `RetrievalPlanCandidate` | `evidence.build_query.medium.v1` | 固定查询模板 |
| MC-15 | Claim 提取 | Evidence Intelligence | 是 | 中 | 来源 Chunk | `EvidenceClaimCandidate[]` | `evidence.extract_claims.medium.v1` | 不生成 Claim，保留原文 |
| MC-16 | Citation 支持性判断 | Evidence Intelligence | NLI/交叉编码器+规则 | 高 | Claim、SourceSpan | `CitationValidation` | `evidence.validate_citation.high.v1` | 标记 unsupported/人工审核 |
| MC-17 | 证据冲突摘要 | Evidence Intelligence | 是 | 高 | 多来源 Claim | `EvidenceConflictSummary` | `evidence.summarize_conflict.high.v1` | 原样呈现冲突，不自动裁决 |
| MC-18 | 患者版语言改写 | Delivery | 是 | 低 | 已批准结构化结果 | `PatientDeliveryDraft` | `delivery.patient_rewrite.low.v1` | 模板化输出 |
| MC-19 | 医生交接摘要草稿 | Delivery/Review | 是 | 高 | CDP、EvidencePack、Triage | `ClinicianSummaryDraft` | `review.clinician_summary.high.v1` | 结构化字段直出，必须审核 |
| MC-20 | 随访问题表达 | Delivery | 是 | 低 | FollowUpPlan | `FollowUpMessageDraft` | `delivery.followup_wording.low.v1` | 固定模板 |
| MC-21 | OCR 文本理解 | Evidence/Tool | 可选多模态 | 中 | 已安全解析的 Artifact | `ArtifactObservationCandidate[]` | `artifact.extract_clinical.medium.v1` | 请求人工确认/重新上传 |
| MC-22 | 图像或报告正式诊断 | N/A | 禁止首版 | 高 | N/A | N/A | N/A | 医生处理 |

## 6. adult_respiratory_v1 首版启用路线

首个纵向切片只启用：

```text
MC-01 Observation Extraction
MC-04 Red Flag Candidate Extraction（可选辅助）
MC-06 Information Gap Candidate
MC-08 Question Wording
MC-09 Encounter Summary（跨轮较长后）
MC-18 Patient Wording
```

Phase D 增加：

```text
MC-11 / MC-12 Limited Hypothesis Assistance
MC-13 / MC-14 Retrieval Planning
MC-15 Claim Extraction
MC-16 Citation Validation
MC-17 Conflict Summary
```

Phase E 增加：

```text
MC-19 Clinician Summary
MC-20 Follow-up Wording
```

首版不得启用自动治疗、处方或影像确诊路线。

## 7. ModelRoutePolicy

```python
class ModelRoutePolicy(BaseModel):
    route_id: str
    version: str
    task_type: str
    risk_level: Literal["low", "medium", "high"]
    capability_ids: list[str]

    required_features: list[str]
    required_structured_output: bool
    output_schema_id: str
    minimum_context_window: int

    contains_phi: bool
    allowed_data_regions: list[str]
    allowed_provider_ids: list[str]
    requires_local_processing: bool

    primary_model_ids: list[str]
    fallback_model_ids: list[str]

    timeout_ms: int
    max_retries: int
    max_input_tokens: int
    max_output_tokens: int
    max_cost_per_call: float | None

    eval_suite_id: str
    minimum_eval_thresholds: dict[str, float]
    no_model_action: str
```

## 8. Model Registry

```python
class ModelSpec(BaseModel):
    model_id: str
    provider_id: str
    provider_model_name: str
    model_version: str
    model_type: Literal[
        "chat",
        "structured_generation",
        "embedding",
        "reranker",
        "nli",
        "multimodal",
    ]
    context_window: int
    supported_languages: list[str]
    supports_json_schema: bool
    supports_tool_calling: bool
    supports_streaming: bool
    phi_policy: str
    data_regions: list[str]
    retention_policy: str
    latency_class: str
    cost_metadata: dict
    health_status: str
    release_status: str
    eval_report_ids: list[str]
```

模型只有在 `release_status=approved` 且满足当前 Capability 的评估阈值时可被 Router 选择。

## 9. Prompt 与模型绑定

每次调用必须绑定：

```text
capability_version
route_policy_version
model_id/model_version
prompt_release_id/prompt_version
output_schema_version
context_policy_version
knowledge_release_id（如适用）
```

不得在节点中写死模型名或 Prompt 文件路径。

## 10. 输入治理

模型输入由 Context Assembly 和 Prompt Builder 产生，必须包含：

- 最小必要临床上下文；
- 任务目标；
- Capability 边界；
- 明确的不可信患者文本；
- 明确的不可信 RAG/Tool 内容；
- 输出 Schema；
- 禁止项；
- 当前风险和审核要求。

外部供应商调用前必须执行：

- PHI Policy；
- 数据驻留；
- 字段脱敏；
- Tenant/Patient Scope；
- 内容长度和 token 预算；
- Prompt Injection 标记。

## 11. 输出治理

```text
Raw Model Response
→ Parse
→ JSON Schema / Pydantic
→ Required Field Check
→ Terminology Validation
→ Citation Validation（如适用）
→ Capability Validation
→ Safety Validation
→ Confidence / Empty Handling
→ Candidate / Decision / Draft
```

任何失败不得直接写状态。

合法结果状态：

```text
success
partial
invalid_schema
empty
low_confidence
out_of_scope
policy_denied
timeout
provider_failure
unsafe_output
requires_human_review
```

## 12. Retry 与 Fallback

### 12.1 允许重试

- 网络瞬时错误；
- Provider 5xx；
- 一次结构化输出修复；
- 明确的速率限制退避。

### 12.2 禁止盲目重试

- Safety Policy 拒绝；
- PHI/Region 不允许；
- 输入超范围；
- 多次 Schema 失败；
- 内容被判定为不安全；
- 高风险任务无批准模型。

### 12.3 Fallback

低风险语言任务可降级到模板或等价已验证模型。

高风险任务只允许：

```text
approved equivalent model
→ deterministic engine
→ fixed workflow
→ clinician review
```

## 13. 缓存

禁止缓存包含完整患者文本的通用模型响应。

允许缓存：

- 非 PHI 的术语标准化；
- 公共知识 Claim 提取结果；
- Prompt 模板；
- Model Route Decision 短期结果；
- Embedding。

缓存必须绑定版本和输入 hash。

## 14. Trace 与审计

每次调用记录：

- trace_id、span_id；
- encounter_id、thread_id 的受控引用；
- node_id、task_type、route_id；
- model_id、provider_id；
- prompt_release_id；
- context_hash；
- input/output token；
- latency；
- retry/fallback；
- validation result；
- cost；
- policy decision；
- error category。

普通 Trace 不记录完整 PHI、完整 Prompt 或完整模型输出。

## 15. 评估矩阵

每个 Route 至少评估：

- Schema 成功率；
- 字段准确率；
- 幻觉率；
- 空结果处理；
- Prompt Injection；
- 跨患者污染；
- 高风险遗漏；
- 语言质量；
- 延迟和成本；
- 模型升级回归。

专项指标：

- Observation extraction：Concept/Status/Modifier F1；
- Red flag extraction：Recall 优先；
- Question wording：语义保持、无新增临床事实；
- Summary：事实忠实、红旗保留、冲突保留；
- Claim extraction：Claim/Span 对齐；
- Citation validation：unsupported 检出；
- Patient rewrite：不增加诊断确定性；
- Clinician summary：关键事实遗漏率。

## 16. 发布和回滚

模型发布链：

```text
ModelSpec
→ Offline Eval
→ Route Candidate
→ Shadow
→ Limited Capability
→ Approved
```

回滚单位可以是：

- Model Route Policy；
- ModelSpec；
- Prompt Release；
- Output Schema；
- Capability Allowlist。

运行中的 Encounter 继续使用绑定版本，紧急安全召回除外。

## 17. 禁止模式

- 在 Graph Node 内直接创建 Provider Client；
- 使用环境变量中的模型名绕过 Registry；
- 自动使用供应商“latest”模型；
- Prompt 更新后不运行 Eval；
- 高风险任务静默切小模型；
- 将模型置信度当作临床概率；
- 让模型自由生成疾病全集；
- 让模型直接决定红旗和分诊；
- 让模型输出直接写 CDP；
- 将模型 CoT 保存或展示给患者。

## 18. 实施顺序

```text
1. 定义 ModelSpec / RoutePolicy / RouteDecision
2. 包装 common/aidoctor_llm 为 Provider Adapter
3. 建立 Model Gateway
4. 建立 Prompt Registry 和 Output Schema Registry
5. 实现 MC-01 与 MC-08
6. 加入 Trace、Token、Cost、Timeout
7. 实现安全 Fallback
8. 建立 Route Eval
9. Phase D 扩展 Evidence 和 Hypothesis 路线
10. Phase E 扩展医生和随访路线
```

## 19. Exit Gate

- 业务代码无 Provider 直接调用；
- 每个模型调用可追踪到 Route、Prompt、Model、Schema 和 Context；
- 高风险任务无静默降级；
- 所有输出经过结构化校验；
- 模型不能直接写状态和执行动作；
- 首个 Capability 的启用路线有评估报告；
- Prompt/Model 可独立回滚；
- Timeout、Schema 失败和 Provider 故障有 E2E 测试。
