# AIdoctor 工程、发布、回滚与下线方案

> 文档状态：Draft v2.6  
> 更新时间：2026-07-29  
> 目标：把总体架构、Capability、RAG 和 Model Runtime 转化为可构建、可验证、可发布、可回滚和可下线的工程过程。

---

## 1. 工程原则

1. 每个阶段必须产生可运行纵向闭环；
2. 代码、Schema、Capability、Prompt、Model Route、Knowledge 和 Safety 独立版本化；
3. 任何生产发布必须可重现当时的完整版本快照；
4. 高风险能力无安全 Eval 和医生审核不得发布；
5. Prompt、Model 或知识变化不能绕过发布流程；
6. 旧服务只有在替代路径、流量、数据和回滚证据齐备后下线；
7. 文档“已完成”不等于构建、运行和测试通过。

---

## 2. 版本链

一次生产运行至少关联：

```text
application_version
graph_version
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
```

历史运行永远引用当时快照，不因新版本发布被重新解释。

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
- 依赖漏洞和 Secret 扫描。

### 3.2 Python

- 统一 Python 版本；
- 使用锁文件；
- 统一 Pydantic/FastAPI/LangGraph 版本；
- 禁止服务各自漂移依赖；
- lint、type check、pytest；
- provider SDK 只能存在于 Provider Adapter；
- Prompt 文件通过 Schema 校验。

### 3.3 Frontend

- Node 和包管理器版本锁定；
- build、lint、typecheck；
- Unit/Component/E2E；
- Thread reload/resume；
- 角色与权限；
- Accessibility；
- Release 管理页面安全测试。

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
→ artifact/SBOM/signature
```

### 4.2 Java/Python 契约一致性

Shared Contracts 应从同一 Schema 源生成或验证：

- Java DTO；
- Python Pydantic；
- TypeScript types；
- OpenAPI；
- Event Schema；
- Database enum mapping。

CI 必须阻止跨语言字段、枚举和 required 属性漂移。

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

禁止：

- 生产自动加载“最新 Prompt”；
- 直接修改线上 Prompt；
- Prompt 自行改变 Capability、Tool 权限或 Safety；
- 无版本 Prompt 进入 Trace 或决策链。

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

禁止：未授权全文、来源不明文档、模型生成知识、未审核图关系进入生产索引。

---

## 8. Knowledge Graph Release Gate

Neo4j 或其他图数据库进入生产 RetrievalPlan 前必须：

- Schema 和 provenance 覆盖达标；
- 每条临床关系有来源或明确术语映射标记；
- 与无图基线进行对照；
- must-not-miss recall 有净提升；
- precision、延迟和成本可接受；
- 不替代 Citation；
- Graph Release 可回滚；
- 临床 Reviewer 和架构 Owner 批准。

不达标时仅作为实验能力保留。

---

## 9. Capability Release Gate

一个 Capability Release 不是单个配置文件，而是以下版本集合：

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
- 回滚到上一 Capability Release。

首个 Release 为 `adult_respiratory_v1`。

---

## 10. 测试体系

### 10.1 Unit

覆盖：State Committer、Safety Rules、Question Policy、Context Policy、Prompt Loader、Model Router、Validator、Retriever、Citation Validator、Version Resolver。

### 10.2 Integration

覆盖：

- Java ↔ Python；
- PostgreSQL/Redis/Object Storage；
- Provider Adapter；
- pgvector/BM25/Reranker；
- OTel；
- Outbox/Inbox；
- Knowledge Release 切换；
- Prompt/Model Route 切换。

### 10.3 E2E

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
- Fallback Workflow。

### 10.4 Clinical Eval

- 红旗漏检；
- 分诊一致性；
- Observation 抽取 F1；
- 否定和时间表达；
- 问题价值和重复率；
- Hypothesis 支持/反对证据；
- 患者表达安全性。

### 10.5 RAG Eval

- recall@k；
- nDCG/MRR；
- source-tier 命中；
- citation precision；
- claim support；
- population/region match；
- stale source rejection；
- injection resistance；
- graph net benefit。

### 10.6 Model/Prompt Eval

每个 `route_id` 独立评估：

- schema validity；
- task accuracy；
- hallucination/unsupported rate；
- consistency；
- safety；
- latency/cost；
- fallback behavior；
- capability-specific cases。

---

## 11. 环境

```text
local
integration
staging
shadow
production
```

环境必须隔离数据库、Redis、对象存储、Provider Key、Knowledge Release 和患者数据。非生产环境默认使用合成或脱敏数据。

---

## 12. 发布流程

### 12.1 应用发布

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

### 12.2 资产发布

Prompt、Model Route、Knowledge 和 Capability 使用独立发布流程，但必须生成统一 Release Manifest。

### 12.3 Release Manifest

至少包含：

- Git commit；
- container digest；
- Schema version；
- Graph version；
- Capability Release；
- Prompt Releases；
- Model Route Releases；
- Knowledge Release；
- Eval Report；
- approvals；
- rollback targets。

---

## 13. 放量策略

```text
离线评估
→ 合成病例
→ Shadow（不影响用户）
→ Clinician Assist
→ 内部/受限患者
→ 小流量 Canary
→ 分阶段扩大
```

高风险动作在任何阶段都保留医生最终控制。放量必须按 Capability Release，而不是仅按应用版本。

---

## 14. 回滚

### 14.1 类型

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

### 14.2 原则

- 不重写历史执行；
- 中断中的 Thread 按版本兼容策略恢复；
- 高风险异常优先切固定 Workflow/人工；
- Prompt/Model/Knowledge 回滚必须保留 Audit；
- 数据 Migration 未验证前不能以应用回滚代替数据修复。

---

## 15. 可观测性与 SLO

必须监控：

- API、Graph Node、Tool、Model、RAG、Checkpoint 延迟；
- Error、Retry、Fallback、Validation Failure；
- 红旗升级和 Human Review；
- Resume 失败和重复提交；
- Prompt/Model/Knowledge/Capability 版本分布；
- Citation failure；
- Provider cost/token；
- Queue 和 Outbox backlog；
- 数据迁移和 Reconciliation。

报警必须带 correlation ID，不包含完整 PHI。

---

## 16. Incident 与 Emergency Disable

支持独立禁用：

- 某 Capability Release；
- 某 Model Route 或具体模型；
- 某 Prompt Release；
- 某 Knowledge Release；
- Graph Expansion；
- 某 Tool/Skill；
- 外部动作。

禁用后采用：确定性安全路径、固定 Workflow、人工审核或明确服务降级，不得使用未验证替代品。

---

## 17. 备份与恢复演练

定期演练：

- PostgreSQL PITR；
- Checkpoint 恢复；
- Object Storage 恢复；
- Knowledge Index 重建；
- Prompt/Model/Capability Registry 恢复；
- Neo4j 恢复；
- Audit Store 恢复；
- Secret 轮换。

每次演练记录 RPO、RTO、数据差异和后续整改。

---

## 18. 旧服务下线门禁

旧服务下线前必须：

- 新能力覆盖所有有效调用；
- 新旧双跑差异通过；
- 生产流量为零；
- 数据和事件已迁移；
- Prompt、模型和知识资产已抽出；
- 无直接 SDK、数据库和配置引用；
- 回滚窗口结束；
- Runbook 和 Archive 完成；
- Owner 批准。

删除应使用独立 PR，不能与新功能开发混合。

---

## 19. Phase 门禁

### Phase A

- 三端构建基线；
- 全量 Inventory；
- Contracts 和 Registry Schema；
- ADR；
- CI 和 Secret 扫描。

### Phase B

- State/Safety E2E；
- Legacy Adapter；
- 呼吸道 Safety Eval。

### Phase C

- LangGraph/Resume；
- Prompt Loader/Model Gateway；
- 首批 Route Eval；
- 模型失败 Crash Matrix。

### Phase D

- Knowledge Release；
- RAG/Citation Eval；
- Graph 对照实验；
- Patient RAG 隔离。

### Phase E

- Clinician Review；
- Delivery；
- Outbox/Inbox；
- 业务闭环。

### Phase F

- 完整 Governance；
- Shadow/Canary；
- Backup/DR；
- Decommission。

---

## 20. 当前结论

应用代码发布只是系统发布的一部分。AIdoctor 的生产单元必须升级为“应用 + Capability + Safety + Prompt + Model Route + Knowledge + Eval”的受控版本集合；只有这样才能回放、比较、回滚并安全扩展临床场景。