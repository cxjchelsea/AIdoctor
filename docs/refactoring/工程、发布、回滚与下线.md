# AIdoctor 工程、发布、回滚与下线方案

> 文档状态：Draft v2.6  
> 更新时间：2026-07-29  
> 目标：把总体架构、Capability、RAG、Model Runtime 和旧设计资产继承转化为可构建、可验证、可发布、可回滚和可下线的工程过程。

---

## 1. 工程原则

1. 每个阶段必须产生可运行纵向闭环；
2. 代码、Schema、Capability、Prompt、Model Route、Knowledge、Safety 和旧资产迁移记录独立版本化；
3. 任何生产发布必须可重现当时的完整版本快照；
4. 高风险能力无安全 Eval 和医生审核不得发布；
5. Prompt、Model、知识或图谱变化不能绕过发布流程；
6. 重构采用资产继承式迁移，不默认推倒重写；
7. 旧服务只有在替代路径、流量、数据、旧资产验证和回滚证据齐备后下线；
8. 文档“已完成”不等于构建、运行和测试通过；
9. 新目录或新类存在不等于旧能力已被正确替代；
10. 删除旧实现必须使用独立 PR。

---

## 2. 版本链

一次生产运行至少关联：

```text
application_version
graph_runtime_version
contract_version
schema_version
capability_release_id
safety_policy_version
prompt_release_ids
model_route_release_ids
knowledge_release_id
embedding_version
reranker_version
graph_release_id（可选）
tool_release_ids
skill_release_ids
eval_suite_version
legacy_adapter_version（迁移期）
```

历史运行永远引用当时快照，不因新版本发布被重新解释。

旧资产迁移还必须关联：

```text
legacy_asset_id
retention_decision
source_document_path
source_code_path
migration_artifact
validation_report
rollback_target
decommission_register_entry
```

---

## 3. 构建基线

### 3.1 Java

Phase A 必须确认：

- JDK 和 Maven 版本；
- 所有 Java Module 可编译；
- Flyway Migration 可执行；
- 单元和集成测试结果；
- Nacos、数据库、Redis 和远程服务依赖；
- 旧 Workflow 最小 E2E；
- AOP/Trace 实际切点和故障行为；
- 依赖漏洞和 Secret 扫描。

### 3.2 Python

- 统一 Python 版本和锁文件；
- 统一 Pydantic/FastAPI/LangGraph 版本；
- 禁止各服务漂移依赖；
- lint、type check、pytest；
- provider SDK 只能存在于 Provider Adapter；
- Prompt 文件通过 Schema 校验；
- 旧服务 Adapter 有 Contract 和 timeout/fallback 测试。

### 3.3 Frontend

- Node 和包管理器版本锁定；
- build、lint、typecheck；
- Unit/Component/E2E；
- Thread reload/resume；
- 角色与权限；
- Accessibility；
- Release 管理页面安全测试；
- ReactFlow/Timeline 旧资产迁移验证。

### 3.4 Legacy Design Asset Baseline

A6.5 必须生成：

```text
legacy-design-asset-inventory.csv
legacy-design-code-evidence.csv
legacy-contract-extraction.md
legacy-clinical-policy-extraction.md
legacy-eval-asset-inventory.csv
legacy-observability-migration.md
legacy-asset-decommission-register.csv
```

P0/P1 旧资产未进入上述交付物时，不允许删除或替换其实现。

---

## 4. CI Pipeline

### 4.1 基础 Pipeline

```text
checkout
→ secret scan
→ dependency scan
→ contract/schema validation
→ lint/typecheck
→ unit tests
→ build
→ integration tests
→ architecture rules
→ artifact/SBOM/signature
```

### 4.2 跨语言契约一致性

Shared Contracts 应从同一 Schema 源生成或验证：

- Java DTO；
- Python Pydantic；
- TypeScript types；
- OpenAPI；
- Event Schema；
- Database enum mapping。

CI 必须阻止字段、枚举、required 属性和版本策略漂移。

### 4.3 架构规则

CI 或 ArchUnit/静态检查至少阻止：

- 业务模块直接调用 Provider SDK；
- Tool、LLM 或旧服务直接写临床状态；
- Patient RAG 和 Medical RAG 共用无隔离索引；
- 未注册 Prompt/Model/Knowledge/Capability 被生产代码引用；
- AOP 隐式执行 Red Flag、Triage、State Commit 或 Human Review；
- Technical Trace 保存完整 PHI；
- 旧资产在 Decommission Gate 前被删除。

---

## 5. Prompt Release Gate

每个 Prompt Release 必须满足：

- PromptSpec Schema 通过；
- 输入和输出 Schema 存在且兼容；
- `prompt_id` 和 version 唯一；
- Capability、语言和风险范围明确；
- 允许的 `route_id` 存在；
- 模板 checksum 固定；
- Prompt Injection 对抗测试通过；
- 离线 Eval 达标；
- Owner 和 Reviewer 批准；
- 生产发布和回滚版本明确。

禁止：生产自动加载“最新 Prompt”、直接修改线上 Prompt、Prompt 改变 Tool/Safety 权限、无版本 Prompt 进入决策链。

旧 Prompt 被替换前必须登记来源、调用方、测试样例、目标 PromptSpec 和差异评估。

---

## 6. Model Release Gate

`ModelSpec` 和 `ModelRoutePolicy` 必须经过：

- Provider 和模型版本确认；
- 数据保留和训练政策审查；
- PHI、地区和数据驻留检查；
- 结构化输出可靠性测试；
- 任务级质量 Eval；
- 延迟、成本和容量测试；
- Fallback 验证；
- 模型不可用演练；
- 高风险静默降级为零；
- Emergency Disable 测试。

业务模块只能发布 `route_id`，不能发布具体供应商调用代码。

---

## 7. Knowledge Release Gate

Medical Knowledge Release 必须满足：

1. Source Registry 完整；
2. 许可和访问范围确认；
3. 来源版本、地区、人群和有效期确认；
4. 文件安全和 checksum 通过；
5. 结构解析和 Chunk 质量抽检；
6. Embedding、BM25 和 Reranker 版本固定；
7. Retrieval Eval 达标；
8. Claim-Level Citation Eval 达标；
9. 冲突、无结果、过期和人群不匹配可表达；
10. 临床 Reviewer 批准；
11. Knowledge Release 可回滚。

禁止未授权全文、来源不明文档、模型生成知识或未审核图关系进入生产索引。

旧 Knowledge Publish Gate 的 Sandbox/Staging/Production、provenance、回归和 rollback 原则必须作为迁移资产保留。

---

## 8. Knowledge Graph Release Gate

Neo4j 或其他图数据库进入生产 RetrievalPlan 前必须：

- Schema 和 provenance 覆盖达标；
- 每条临床关系有来源或明确术语映射标记；
- 许可、版本、有效期和回滚明确；
- 与无图基线进行对照；
- must-not-miss recall 有净提升；
- precision、延迟和成本可接受；
- 不替代 Citation；
- Graph Release 可回滚；
- 临床 Reviewer 和架构 Owner 批准。

不达标时仅作为实验能力或历史资产保留。路径注入模型失败时不得将路径直接当作诊断结果。

---

## 9. Capability Release Gate

一个 Capability Release 是以下版本集合：

```text
Capability Manifest
+ Terminology Pack
+ Observation Profile
+ Safety Pack
+ Question Pack
+ Hypothesis Pack
+ Tool/Skill Allowlist
+ Prompt Releases
+ Model Route Releases
+ Knowledge Release
+ Delivery Policy
+ Eval Suite
```

发布前必须确认：

- 支持和排除人群；
- 支持主诉和超范围行为；
- 红旗和分诊病例通过；
- 模型失败仍有安全路径；
- 知识无结果和冲突处理；
- 医生审核门禁；
- 患者端表达安全；
- Shadow 对比；
- 回滚到上一 Capability Release；
- 继承的旧临床规则、问诊模板和评估集已有迁移记录。

首个 Release 为 `adult_respiratory_v1`。

---

## 10. Legacy Asset Retention Gate

### 10.1 KEEP / ADAPT

必须具备：

- 文档与代码证据；
- Valuable Principle 和 Current Problem；
- Target Module/Contract；
- 新旧行为比较；
- 测试或评估计划；
- 数据、PHI 和依赖检查；
- rollback/decommission 条件。

### 10.2 EVALUATE

必须具备：

- 明确基线；
- 对照组；
- 质量、风险、成本和延迟指标；
- 失败不影响主链路；
- 结果进入 ADR 或 Release Gate。

### 10.3 ARCHIVE / REMOVE

必须具备：

- 无有效调用或已完成替代；
- 数据、规则、Prompt、评估集和文档已归档；
- 全仓引用为空；
- 回滚价值评估完成；
- Decommission Register 有 Owner 批准。

---

## 11. AOP / Trace Migration Gate

AOP 作为技术横切机制可以保留，但迁移前后必须验证：

- `@TraceExecution` 或替代注解的切点范围；
- `ExecutionTraceAspect` 不同步依赖远程 Trace 服务；
- Trace 失败不阻止 `joinPoint.proceed()`；
- OTel Exporter 失败不阻塞临床主事务；
- OTel Context/MDC 在异步、线程池和跨服务传播；
- Feign 使用 W3C `traceparent`/`tracestate`；
- 不记录完整患者输入、Prompt 或模型原始响应；
- Technical Span、AgentEvent、ClinicalDecisionRecord 和 ComplianceAudit 分离；
- 旧 Trace UI/ReactFlow 功能已迁移或归档。

旧 `execution-trace-service` 只有在 OTel Backend 和 AgentEvent Store/UI 均完成替代后才能下线。

---

## 12. 测试体系

### 12.1 Unit

覆盖：State Committer、Safety Rules、Question Policy、Context Policy、Prompt Loader、Model Router、Validator、Retriever、Citation Validator、Version Resolver、Legacy Adapter 和 AOP 边界。

### 12.2 Integration

覆盖：

- Java ↔ Python；
- PostgreSQL/Redis/Object Storage；
- Provider Adapter；
- pgvector/BM25/Reranker；
- OTel；
- Outbox/Inbox；
- Knowledge Release 切换；
- Prompt/Model Route 切换；
- 旧服务 Adapter；
- AOP/Trace 跨服务传播。

### 12.3 E2E

每个阶段至少包括：

- 正常呼吸道场景；
- 红旗场景；
- 输入不足；
- 重复请求；
- 服务重启；
- 模型超时和非法 JSON；
- RAG 无结果、冲突、过期和人群不匹配；
- 医生审核；
- 前端刷新恢复；
- Fallback Workflow；
- Trace Backend 不可用；
- 旧/新链路双跑。

### 12.4 Clinical Eval

- 红旗漏检；
- 分诊一致性；
- Observation 抽取 F1；
- 否定和时间表达；
- 问题价值和重复率；
- Hypothesis 支持/反对证据；
- 患者表达安全性；
- 旧静态、交互和轨迹评估集迁移。

### 12.5 RAG Eval

- recall@k；
- nDCG/MRR；
- source-tier 命中；
- citation precision；
- claim support；
- population/region match；
- stale source rejection；
- injection resistance；
- graph net benefit。

### 12.6 Model/Prompt Eval

每个 `route_id` 独立评估：schema validity、task accuracy、unsupported rate、consistency、safety、latency/cost、fallback 和 capability-specific cases。

### 12.7 Durable / Replay Eval

- Checkpoint 前后 crash；
- State Commit 前后 crash；
- 重复 Resume；
- 并发 Lease；
- 原 capability/prompt/model/knowledge 版本回放；
- AgentState 和旧轨迹评估集映射。

---

## 13. 环境与发布流程

环境：

```text
local
integration
staging
shadow
production
```

环境必须隔离数据库、Redis、对象存储、Provider Key、Knowledge Release 和患者数据。非生产环境默认使用合成或脱敏数据。

应用发布：

```text
Build + SBOM
→ Unit/Contract
→ Integration
→ E2E
→ Security
→ Staging
→ Shadow
→ Canary
→ Production
```

Prompt、Model Route、Knowledge 和 Capability 使用独立发布流程，但必须生成统一 Release Manifest。

Release Manifest 至少包含：Git commit、container digest、Schema、Graph Runtime、Capability、Prompt、Model Route、Knowledge、Eval Report、approvals、rollback targets 和迁移期 Legacy Adapter version。

---

## 14. 放量策略

```text
离线评估
→ 合成病例
→ Shadow（不影响用户）
→ Clinician Assist
→ 内部/受限患者
→ 小流量 Canary
→ 分阶段扩大
```

高风险动作始终保留医生最终控制。放量按 Capability Release，不只按应用版本。

迁移期必须保留新旧双跑差异报告，不能以“新链路返回成功”替代临床、状态和证据一致性比较。

---

## 15. 回滚

| 类型 | 回滚对象 |
|---|---|
| Application | 镜像和部署配置 |
| Schema | Expand/Contract 或向前修复 |
| Graph Runtime | Graph/State Schema |
| Capability | `capability_release_id` |
| Prompt | `prompt_release_id` |
| Model | `model_route_release_id` |
| Knowledge | `knowledge_release_id` |
| Graph Knowledge | `graph_release_id` |
| Safety | `safety_policy_version` |
| Legacy Migration | Adapter、旧 Workflow、旧数据只读路径 |

原则：不重写历史执行；中断 Thread 按原版本恢复；高风险异常优先切固定 Workflow/人工；Prompt/Model/Knowledge 回滚保留 Audit；数据 Migration 未验证前不能用应用回滚代替数据修复。

---

## 16. 可观测性、SLO 与 Emergency Disable

必须监控：

- API、Graph Node、Tool、Model、RAG、Checkpoint 延迟；
- Error、Retry、Fallback、Validation Failure；
- 红旗升级和 Human Review；
- Resume 失败和重复提交；
- Prompt/Model/Knowledge/Capability 版本分布；
- Citation failure；
- Provider cost/token；
- Queue 和 Outbox backlog；
- 数据迁移和 Reconciliation；
- Legacy Adapter 调用和新旧差异；
- Trace Exporter 失败。

报警必须带 correlation ID，不包含完整 PHI。

支持独立禁用：Capability、Model Route、Prompt、Knowledge Release、Graph Expansion、Tool/Skill、外部动作和 Legacy Adapter。

禁用后采用确定性安全路径、固定 Workflow、人工审核或明确服务降级，不得使用未验证替代品。

---

## 17. 备份与恢复演练

定期演练：

- PostgreSQL PITR；
- Checkpoint 恢复；
- Object Storage；
- Knowledge Index 重建；
- Prompt/Model/Capability Registry；
- Neo4j；
- Audit Store；
- Secret 轮换；
- Legacy CDP 原始 blob 和 Migration Register 恢复。

每次演练记录 RPO、RTO、数据差异和后续整改。

---

## 18. 旧服务与旧资产下线门禁

### 18.1 服务级门禁

旧服务下线前必须：

- 新能力覆盖所有有效调用；
- 新旧双跑差异通过；
- 生产流量为零；
- 数据和事件已迁移；
- Prompt、模型、知识和规则资产已抽出；
- 无直接 SDK、数据库和配置引用；
- 回滚窗口结束；
- Runbook 和 Archive 完成；
- Owner 批准。

### 18.2 资产级门禁

每项 P0/P1 旧资产下线或替换前必须：

- `legacy_design_asset_id` 存在；
- Evidence Level 已确认；
- Retention Decision 已批准；
- Target Contract/Implementation 已完成；
- Validation Suite 通过；
- 数据、Prompt、规则、评估集和文档已迁移或归档；
- 全仓引用检查通过；
- 回滚目标存在；
- `legacy-asset-decommission-register.csv` 状态批准。

### 18.3 特殊门禁

- 固定 Workflow：新链路 Shadow/Canary 和 fallback 演练完成后再下线；
- AOP/Trace：OTel、AgentEvent UI 和故障隔离完成后再下线；
- CDP：迁移对账、原始 blob 归档和回滚完成后再停止旧写入；
- Neo4j/Milvus：来源、使用方、数据价值和重建策略确认后再清理；
- 旧评估集：已迁入 Eval Registry 或明确归档后再删除。

删除必须使用独立 PR，不能与新功能开发混合。

---

## 19. Phase 门禁

### Phase A

- 三端构建基线；
- 全量技术、数据、Prompt、模型、知识和测试 Inventory；
- A6.5 Legacy Design Asset Validation；
- Contracts 和 Registry Schema；
- ADR；
- CI 和 Secret 扫描；
- 固定 Workflow 与 AOP/Trace 基线；
- Frozen Baseline Review。

### Phase B

- State/Safety E2E；
- Legacy CDP Adapter；
- 呼吸道 Safety Eval；
- 无 Tool/LLM 直写。

### Phase C

- LangGraph/Resume；
- Prompt Loader/Model Gateway；
- 首批 Route Eval；
- 模型失败 Crash Matrix；
- OTel/AOP 迁移验证。

### Phase D

- Knowledge Release；
- RAG/Citation Eval；
- Graph 对照实验；
- Patient RAG 隔离；
- 旧三类评估集扩展。

### Phase E

- Clinician Review；
- Delivery；
- Outbox/Inbox；
- 业务闭环；
- 新旧交付差异通过。

### Phase F

- 完整 Governance；
- Shadow/Canary；
- Backup/DR；
- 服务和资产 Decommission。

---

## 20. 当前结论

应用代码发布只是系统发布的一部分。AIdoctor 的生产单元必须是“应用 + Capability + Safety + Prompt + Model Route + Knowledge + Eval”的受控版本集合；重构单元还必须包含“Legacy Asset Decision + Migration Evidence + Validation + Rollback + Decommission Gate”。只有这样才能在不丢失原设计价值的前提下完成回放、比较、回滚和安全扩展。
