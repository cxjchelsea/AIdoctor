# AIdoctor Prompt 与模型运行时设计

> 文档状态：Draft v2.6 Freeze Candidate  
> 更新时间：2026-07-29

## 1. 目的

本文定义统一的大模型基础设施，避免 Prompt、模型客户端、重试、结构化输出和供应商配置散落在各业务模块中。

统一组件命名为：

> **Model Runtime**

Model Runtime 是平台基础设施，不是临床 Agent，也不是普通 Tool。

## 2. 目标

- 所有模型调用使用统一 Gateway；
- 所有 Prompt 有注册、版本、审核、发布和回滚；
- 所有模型有 Registry 和评估状态；
- 所有调用经过 Router、Policy、Context 和 Output Validator；
- 业务模块不感知具体供应商 SDK；
- Prompt、模型和 Capability 兼容关系可追踪；
- 模型超时、限流和故障有统一错误语义；
- PHI、数据驻留、成本和遥测得到统一治理。

## 3. 非目标

- 让 Model Runtime 决定临床下一步；
- 让 Model Runtime 直接读任意患者数据库；
- 让 Prompt 代替 Safety Rule；
- 让模型直接写 CDP；
- 建设可由模型自行修改的 Prompt 平台；
- 在首版建设独立复杂微服务集群。

首版可以作为 Python Agent Runtime 内部 package，接口稳定后再决定是否独立部署。

## 4. 架构

```text
Graph Node / Clinical Module / Evidence Module
                    │
                    ▼
             ModelRuntimeClient
                    │
          Policy Enforcement Point
                    │
        ┌───────────┴────────────┐
        │                        │
 Prompt Registry          Model Registry
 Prompt Loader             Model Router
 Prompt Builder            Route Policy
        │                        │
        └───────────┬────────────┘
                    ▼
              Model Gateway
                    │
        Provider Adapter Interface
       ┌────────────┼────────────┐
       │            │            │
  Provider A   Provider B   Local Provider
       │            │            │
       └────────────┴────────────┘
                    ▼
        Structured Output Validator
                    ▼
       Safety / Capability Validation
                    ▼
 Candidate / Decision / Draft / Error
```

## 5. 模块划分

### 5.1 Model Runtime Client

对业务模块提供唯一入口：

```python
class ModelRuntimeClient(Protocol):
    async def invoke(self, request: ModelInvocationRequest) -> ModelInvocationResult:
        ...

    async def embed(self, request: EmbeddingRequest) -> EmbeddingResult:
        ...

    async def rerank(self, request: RerankRequest) -> RerankResult:
        ...
```

业务模块不得直接导入供应商 SDK。

### 5.2 Prompt Registry

负责 Prompt 元数据和版本状态。

### 5.3 Prompt Loader

根据明确 ID 和版本加载 Prompt，不自动选择 latest。

### 5.4 Prompt Builder

将 ContextEnvelope、任务输入、模板和输出 Schema 渲染成模型请求。

### 5.5 Model Registry

保存模型能力、数据政策、版本、评估和健康状态。

### 5.6 Model Router

依据 RoutePolicy 选择批准模型或确定性 Fallback。

### 5.7 Model Gateway

统一处理调用、流式、超时、重试、限流、熔断、Token、成本和 Trace。

### 5.8 Provider Adapter

隔离不同供应商协议。

### 5.9 Structured Output Validator

解析和校验输出，生成统一状态。

### 5.10 Prompt/Model Evaluation

管理离线测试、回归、Shadow 对比和发布门禁。

## 6. Prompt Registry 数据模型

```python
class PromptSpec(BaseModel):
    prompt_id: str
    version: str
    task_type: str
    capability_ids: list[str]
    languages: list[str]
    risk_level: Literal["low", "medium", "high"]

    template_format: Literal["yaml", "jinja2", "structured_messages"]
    template_uri: str
    template_checksum: str

    input_schema_id: str
    output_schema_id: str
    context_policy_ids: list[str]
    allowed_route_ids: list[str]

    owner: str
    clinical_reviewer_ids: list[str]
    technical_reviewer_ids: list[str]

    release_status: Literal[
        "draft",
        "review",
        "evaluation",
        "shadow",
        "approved",
        "deprecated",
        "retired",
    ]
    eval_suite_id: str
    eval_report_id: str | None

    effective_from: datetime | None
    effective_until: datetime | None
    rollback_version: str | None
```

## 7. Prompt 文件格式

首版使用 Git 管理 YAML，便于 Review、Diff 和回滚。

```yaml
prompt_id: clinical.question_wording
version: 1.0.0
task_type: question_wording
capability_ids:
  - adult_respiratory_v1
languages:
  - zh-CN
risk_level: low
input_schema_id: QuestionWordingInput.v1
output_schema_id: QuestionWordingResult.v1
allowed_route_ids:
  - clinical.question_wording.low.v1
context_policy_ids:
  - question_wording_context.v1
owner: clinical-intelligence-team
release_status: approved
messages:
  - role: system
    content: |
      你负责将已经批准的 QuestionDecision 改写为患者易理解的问题。
      不得增加新的临床事实，不得改变问题目标，不得给出诊断或治疗结论。
  - role: developer
    content: |
      Capability: {{ capability.display_name }}
      Language: {{ language }}
      Output must match {{ output_schema_id }}.
  - role: user
    content: |
      <structured_question_decision>
      {{ question_decision_json }}
      </structured_question_decision>
```

模板中的患者、RAG 和 Tool 内容必须放在明确数据边界中，不能拼接为系统指令。

## 8. Prompt Loader

```python
class PromptLoadRequest(BaseModel):
    prompt_id: str
    prompt_version: str
    capability_id: str
    language: str
    environment: str

class PromptBundle(BaseModel):
    spec: PromptSpec
    messages_template: list[dict]
    checksum: str
```

Loader 校验：

- Prompt 是否存在；
- 状态是否允许当前环境；
- Capability 是否允许；
- Route 是否兼容；
- 输入输出 Schema 是否存在；
- checksum 是否匹配；
- 是否过期或被撤回。

禁止：

- `load_latest(prompt_id)` 用于生产；
- 从用户输入指定 Prompt；
- 节点读取任意本地文件；
- 未经发布使用 Draft Prompt。

## 9. Prompt Builder

```python
class PromptBuildRequest(BaseModel):
    prompt_bundle: PromptBundle
    context_envelope: ContextEnvelope
    task_input: dict
    output_schema: dict
    model_capabilities: dict

class RenderedModelRequest(BaseModel):
    prompt_release_id: str
    messages: list[dict]
    output_schema: dict
    context_hash: str
    prompt_hash: str
    redaction_manifest: dict
    token_estimate: int
```

Builder 负责：

- System Policy 固定置顶；
- Capability 和禁止项注入；
- 只选择该 Prompt 需要的 Context；
- Patient/RAG/Tool 数据标记为 untrusted；
- 脱敏；
- token 预算；
- 输出 Schema；
- Prompt Injection 边界；
- 生成可复现 hash。

## 10. ModelInvocationRequest

```python
class ModelInvocationRequest(BaseModel):
    invocation_id: str
    trace_id: str
    thread_id: str | None
    encounter_id: str | None

    capability_id: str
    capability_version: str
    task_type: str
    route_id: str
    route_policy_version: str

    prompt_id: str
    prompt_version: str
    context_envelope: ContextEnvelope
    task_input: dict

    output_schema_id: str
    risk_level: str
    contains_phi: bool
    idempotency_key: str | None

    timeout_ms: int | None
    metadata: dict
```

调用方不提交具体供应商模型名。

## 11. Model Router

Router 输入：

- RoutePolicy；
- Capability；
- 风险；
- PHI；
- 数据驻留；
- 上下文长度；
- 输出 Schema；
- 模型健康；
- 成本与延迟；
- 评估结果。

输出：

```python
class ModelRouteDecision(BaseModel):
    route_id: str
    route_policy_version: str
    selected_model_id: str | None
    fallback_model_ids: list[str]
    rejected_models: dict[str, list[str]]
    reason_codes: list[str]
    requires_human_review: bool
    deterministic_fallback_id: str | None
```

高风险 Route 不允许模型健康异常时自动放宽评估阈值。

## 12. Provider Adapter

```python
class ProviderAdapter(Protocol):
    provider_id: str

    async def generate(
        self,
        model: ModelSpec,
        request: RenderedModelRequest,
        runtime_options: RuntimeOptions,
    ) -> RawProviderResult:
        ...

    async def embed(self, request: ProviderEmbeddingRequest) -> RawEmbeddingResult:
        ...

    async def rerank(self, request: ProviderRerankRequest) -> RawRerankResult:
        ...
```

Adapter 只做协议转换，不做临床判断。

统一转换：

- authentication；
- request schema；
- streaming；
- usage；
- finish reason；
- provider errors；
- safety filters；
- rate limits；
- request id。

## 13. Model Gateway

```python
class ModelGateway:
    async def invoke(self, request: ModelInvocationRequest) -> ModelInvocationResult:
        policy = await self.policy_engine.evaluate(request)
        route = await self.router.route(request, policy)
        prompt = await self.prompt_loader.load(...)
        rendered = await self.prompt_builder.build(...)
        raw = await self.provider_executor.execute(route, rendered)
        validated = await self.output_validator.validate(...)
        return await self.result_builder.build(...)
```

Gateway 统一负责：

- Policy；
- Route；
- Prompt Load/Build；
- Timeout；
- Retry；
- Circuit Breaker；
- Rate Limit；
- Concurrency；
- Token/Cost；
- Trace；
- Output Validation；
- Fallback；
- Error Mapping。

## 14. 输出模型

```python
class ModelInvocationResult(BaseModel):
    invocation_id: str
    status: Literal[
        "success",
        "partial",
        "policy_denied",
        "invalid_input",
        "invalid_output",
        "low_confidence",
        "timeout",
        "rate_limited",
        "provider_failure",
        "unsafe_output",
        "no_approved_model",
        "requires_human_review",
    ]

    route_decision: ModelRouteDecision
    model_id: str | None
    prompt_id: str
    prompt_version: str
    output_schema_id: str

    structured_output: dict | None
    validation_errors: list[dict]
    fallback_used: str | None

    input_tokens: int | None
    output_tokens: int | None
    latency_ms: int
    estimated_cost: float | None

    context_hash: str
    prompt_hash: str
    provider_request_id: str | None
```

不向普通业务调用方返回隐藏 CoT。

## 15. Structured Output Validator

分层验证：

```text
JSON Parse
→ Schema Validation
→ Type / Enum / Required Fields
→ Domain Terminology
→ Capability Boundary
→ Safety Prohibited Content
→ Citation / Source Validation
→ StatePatch Permission
→ Confidence / Empty Result
```

Validator 不自动“修正”临床内容。允许一次仅针对格式的 Repair Call，但 Repair Prompt 不得改变语义。

## 16. 错误模型

```python
class ModelRuntimeError(BaseModel):
    error_code: str
    category: Literal[
        "policy",
        "routing",
        "prompt",
        "provider",
        "timeout",
        "rate_limit",
        "validation",
        "safety",
        "configuration",
    ]
    retryable: bool
    message: str
    provider_code: str | None
    correlation_id: str
```

统一错误码示例：

```text
MODEL_POLICY_DENIED
MODEL_NO_APPROVED_ROUTE
PROMPT_NOT_APPROVED
PROMPT_CAPABILITY_MISMATCH
PROVIDER_TIMEOUT
PROVIDER_RATE_LIMITED
OUTPUT_SCHEMA_INVALID
OUTPUT_UNSAFE
OUTPUT_OUT_OF_SCOPE
```

## 17. Retry、Fallback 与熔断

### 17.1 Retry

- 仅瞬时错误；
- 指数退避和 jitter；
- 总时限受节点预算约束；
- Retry 记录在 AgentEvent；
- 结构化格式修复最多一次。

### 17.2 Fallback

Fallback 由 RoutePolicy 预先声明，模型不能自行选择。

```text
Primary Approved Model
→ Approved Equivalent Model
→ Deterministic Template/Engine
→ Fixed Workflow
→ Human Review
```

### 17.3 Circuit Breaker

按 provider/model/region 维度维护：

- failure rate；
- timeout rate；
- schema failure rate；
- safety filter rate；
- latency。

## 18. PHI 与安全

在 Model Route 前执行：

- Provider 是否允许 PHI；
- 地区和数据驻留；
- retention 和 training policy；
- 字段脱敏；
- 最小必要数据；
- Tenant、Patient、Consent；
- 外部模型禁用场景。

Prompt Builder 明确分隔：

```text
SYSTEM_POLICY
TASK_INSTRUCTIONS
STRUCTURED_TRUSTED_CONTEXT
UNTRUSTED_PATIENT_TEXT
UNTRUSTED_RETRIEVED_CONTENT
UNTRUSTED_TOOL_OUTPUT
OUTPUT_SCHEMA
```

不可信内容不能修改：

- Capability；
- Safety；
- Tool Allowlist；
- Model Route；
- Prompt；
- Output Schema；
- 权限。

## 19. Prompt 发布流程

```text
Draft
→ Unit Test
→ Clinical Review
→ Security Review
→ Offline Eval
→ Shadow
→ Approved Release
```

Prompt 变更必须生成：

- diff；
- 变更原因；
- 影响任务；
- 兼容模型；
- 评估结果；
- 回滚版本。

仅文字修饰也需要回归测试，因为可能改变临床确定性和用户理解。

## 20. 模型发布流程

```text
Register Model
→ Data Policy Review
→ Capability Eval
→ Route Candidate
→ Shadow Compare
→ Approved Route
```

供应商将模型名指向新版本时，不得自动继承批准状态。生产使用可复现的版本标识或内部快照 ID。

## 21. Prompt 与模型兼容矩阵

```python
class PromptModelCompatibility(BaseModel):
    prompt_id: str
    prompt_version: str
    route_id: str
    model_id: str
    output_schema_id: str
    compatibility_status: str
    eval_report_id: str
    approved_at: datetime
```

Router 只选择具有批准兼容记录的组合。

## 22. 目录结构

```text
agent-runtime/
├── model_runtime/
│   ├── api/
│   │   ├── client.py
│   │   └── models.py
│   ├── registry/
│   │   ├── model_registry.py
│   │   ├── prompt_registry.py
│   │   ├── schema_registry.py
│   │   └── compatibility_registry.py
│   ├── prompts/
│   │   ├── loader.py
│   │   ├── builder.py
│   │   ├── renderer.py
│   │   └── sanitizer.py
│   ├── routing/
│   │   ├── router.py
│   │   ├── policies.py
│   │   └── health.py
│   ├── gateway/
│   │   ├── gateway.py
│   │   ├── executor.py
│   │   ├── retry.py
│   │   ├── circuit_breaker.py
│   │   └── rate_limit.py
│   ├── providers/
│   │   ├── base.py
│   │   ├── existing_llm_adapter.py
│   │   └── local_adapter.py
│   ├── validation/
│   │   ├── schema.py
│   │   ├── domain.py
│   │   ├── safety.py
│   │   └── citations.py
│   ├── telemetry/
│   │   ├── tracing.py
│   │   ├── usage.py
│   │   └── cost.py
│   └── evals/
│       ├── runner.py
│       └── reports.py
└── prompts/
    ├── clinical_understanding/
    ├── question_wording/
    ├── encounter_summary/
    ├── evidence_claims/
    ├── clinician_delivery/
    └── patient_delivery/
```

## 23. 配置策略

配置分为：

- 静态安全配置：代码和受控配置库；
- Prompt 模板：Git 版本管理；
- Model Registry：数据库或版本化配置；
- Provider Secret：Secret Manager；
- Runtime Health：动态状态；
- Capability Allowlist：Capability Package。

禁止将 Secret、生产 Prompt 和模型策略混在 `.env` 中。

## 24. 与现有 common/aidoctor_llm 的迁移

处理方式：`ADAPT`。

步骤：

1. 盘点现有 Provider Client 和调用点；
2. 提取统一 ProviderAdapter；
3. 禁止新业务代码直接导入旧 Client；
4. 为旧调用建立 Legacy Adapter；
5. 首先迁移 extraction 和 question wording；
6. 加入统一 usage、timeout、error 和 trace；
7. 逐步删除绕过 Gateway 的调用。

## 25. 测试

### 25.1 Unit

- Prompt Load；
- 变量缺失；
- checksum；
- Router 条件；
- PHI Policy；
- Schema Validator；
- Error Mapping；
- Retry/Fallback。

### 25.2 Integration

- 每个 Provider Adapter；
- Rate Limit；
- Timeout；
- Streaming；
- Usage；
- Circuit Breaker；
- Trace。

### 25.3 Security

- 患者 Prompt Injection；
- RAG Injection；
- Tool Output Injection；
- Prompt 泄露；
- 跨患者 Context；
- 外部 Provider PHI 拒绝；
- 恶意 JSON；
- 超长输入。

### 25.4 E2E

- MC-01 抽取成功/失败；
- MC-08 模型故障降级模板；
- 服务重启；
- 重复 Resume 不重复模型副作用；
- 高风险无模型进入固定路径；
- Prompt 回滚；
- Model Route 回滚。

## 26. 可观测性

Metrics：

- calls、success、failure；
- latency；
- input/output token；
- estimated cost；
- retries；
- fallback；
- schema failure；
- safety rejection；
- route/model/provider 分布；
- prompt version 分布。

Trace 只保存 hash 和最小元数据。完整敏感输入输出进入受控临床记录或专用安全存储，按政策决定是否保存。

## 27. 首版实施范围

Phase C：

- Registry 基础类型；
- Prompt YAML Loader；
- Prompt Builder；
- Provider Adapter；
- Model Gateway；
- extraction/question wording Route；
- Schema Validator；
- Timeout/Retry/Trace；
- 模板 Fallback。

Phase D：

- Embedding/Reranker；
- Claim Extraction；
- Citation Validation；
- Hypothesis 辅助；
- Knowledge Release 绑定。

Phase E/F：

- Clinician Summary；
- 完整 Prompt 发布后台；
- Model Health；
- Cost Policy；
- Shadow Compare；
- Emergency Disable。

## 28. Exit Gate

- 所有模型调用经 Model Runtime；
- 生产 Prompt 全部版本化；
- 生产调用不使用 latest；
- Prompt、模型、Schema 和 Capability 可追溯；
- common/aidoctor_llm 已有统一 Adapter；
- 高风险无批准模型时进入安全路径；
- Prompt Injection 和 PHI 门禁通过；
- Prompt/Model 可独立回滚；
- 关键 Route 有离线评估和 E2E。
