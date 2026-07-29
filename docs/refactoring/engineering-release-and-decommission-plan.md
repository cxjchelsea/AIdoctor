# AIdoctor 工程、发布、回滚与下线方案

> 文档状态：Draft v2.5  
> 更新时间：2026-07-29  
> 目标：把架构路线转化为可构建、可验证、可发布、可回滚和可下线的工程过程。

---

## 1. 原则

1. 每个 Phase 都必须产生可运行 E2E；
2. 文档完成不等于代码完成；
3. README 完成度不作为验收依据；
4. 所有临床状态和安全变更必须有自动化回归；
5. 数据库 Migration 必须支持演练和回滚；
6. 旧系统在新路径达到门禁前保留；
7. 发布与模型、Prompt、Skill、Capability 版本绑定；
8. 生产问题必须能按 Encounter/Thread/Release 追踪；
9. 不在同一发布中引入过多不可逆变化；
10. 所有下线均独立 PR、独立审批。

---

## 2. 仓库工程基线

### 2.1 Java

Phase A 必须建立：

```bash
mvn -B clean verify
```

要求：

- 编译无错误；
- Unit/Integration tests；
- Checkstyle/SpotBugs 或等价工具；
- dependency vulnerability scan；
- Flyway validate；
- OpenAPI generation/validation；
- Java 8 技术债清单；
- 不允许跳过测试发布。

### 2.2 Python

要求：

- 统一 Python 版本；
- 统一依赖管理和 lock file；
- Ruff/Black 或等价规范；
- mypy/pyright 覆盖 Contracts 和核心模块；
- pytest；
- dependency scan；
- Pydantic 统一版本；
- 不再依赖各服务手动 `pip install -r` 的不可复现环境。

推荐结构：

```text
pyproject.toml
uv.lock or poetry.lock
packages/*
apps/agent-runtime
```

### 2.3 Frontend

要求：

```bash
npm ci
npm run lint
npm run test
npm run build
```

当前没有明确 test script，Phase A/C 必须补充：

- Vitest；
- React Testing Library；
- Playwright/Cypress；
- API mock；
- accessibility scan。

### 2.4 Contracts

统一 Pipeline：

```text
JSON Schema / OpenAPI Source
→ validate
→ generate Python models
→ generate Java models
→ generate TypeScript models
→ contract tests
→ compatibility check
```

禁止 Java、Python、TypeScript 各自维护同名类型。

---

## 3. CI Pipeline

### PR Pipeline

1. secret scan；
2. changed-file classification；
3. contracts validate/generate/diff；
4. Java build/test；
5. Python lint/type/test；
6. frontend lint/test/build；
7. database migration validate；
8. container build；
9. SAST/dependency scan；
10. targeted integration tests；
11. documentation link and schema checks。

### Main Pipeline

- 全量 build；
- integration environment；
- first vertical slice E2E；
- red flag regression；
- resume/crash regression；
- artifact publication；
- SBOM；
- image signing；
- staging deployment。

### Capability Release Pipeline

- capability manifest validation；
- safety rules tests；
- prompt/model/skill compatibility；
- knowledge release validation；
- clinical evaluation；
- security evaluation；
- release report；
- manual approval。

---

## 4. 测试金字塔

### 4.1 Contract Tests

覆盖：

- Java/Python/TS schema 一致；
- enum；
- required fields；
- version compatibility；
- unknown field；
- PHI classification；
- error model。

### 4.2 Unit Tests

重点：

- State Committer；
- Safety Rules；
- Observation mapper；
- Question Policy；
- Context Policy；
- Resume validation；
- idempotency；
- migration mapper。

### 4.3 Integration Tests

- PostgreSQL；
- Redis；
- Object Storage；
- Neo4j/pgvector；
- legacy service adapter；
- OTel propagation；
- Outbox/Inbox；
- ReviewTask。

### 4.4 E2E

每阶段至少：

1. 正常问诊；
2. 红旗升级；
3. 输入不足；
4. 重复提交；
5. 服务重启 Resume；
6. Tool 失败；
7. 超范围；
8. 权限/Consent；
9. 医生审核（Phase E 起）；
10. 回滚路径。

### 4.5 Clinical Regression

病例集必须版本化，包含：

- 典型病例；
- 非典型病例；
- 必须排除；
- 低风险；
- 高风险；
- 信息缺失；
- 矛盾信息；
- 对抗输入；
- 人群边界。

---

## 5. 质量门禁

### Phase A

- 所有服务编译/依赖状态有报告；
- 现有固定 Workflow 至少一条可运行，或明确阻塞原因；
- Contracts 可生成；
- 无已提交真实密钥；
- Inventory/Migration Matrix 完成。

### Phase B

- State Committer 覆盖率达标；
- 红旗病例漏检为零（在批准的测试集内）；
- Candidate 不自动确认；
- version conflict 测试通过。

### Phase C

- Runtime restart Resume；
- 重复 Resume 不重复写入；
- 每轮 Safety；
- Trace/Log/AgentEvent 关联；
- p95 延迟基线。

### Phase D

- Patient RAG 隔离；
- Citation 支持 Claim；
- 无证据时明确返回；
- Context 关键事实不丢失。

### Phase E

- 医生审核真实影响 CDP；
- 高风险动作无法绕过审核；
- 三类输出一致；
- 模拟外部动作幂等。

### Phase F

- Replay 无真实副作用；
- PHI 遥测扫描通过；
- Capability Release Report；
- Shadow/Canary 指标达标；
- 备份恢复演练。

---

## 6. 环境

```text
local
integration
staging
pre-production
production
```

要求：

- 数据隔离；
-密钥隔离；
-模型账户隔离；
-对象存储隔离；
-Tenant 隔离；
-不得把生产患者数据复制到开发环境；
-预发使用合成或脱敏数据。

---

## 7. 发布单元

生产发布不是只有代码版本，还包括：

```text
application_release
contract_release
capability_release
safety_policy_release
prompt_release
model_route_release
skill_release
knowledge_release
migration_release
frontend_release
```

每个 Run 和 ClinicalDecisionRecord 必须关联实际使用的版本。

---

## 8. 发布策略

### 8.1 Internal

仅内部测试账号。

### 8.2 Shadow

新 Runtime 不返回患者结果，只与旧 Workflow 比较。

### 8.3 Clinician Assist

仅医生可见新结果。

### 8.4 Restricted Patient

限定：

- 低风险；
- 明确 Capability；
- 明确机构；
- 有医生兜底；
- 有实时监控。

### 8.5 Gradual Rollout

按 tenant/cohort/percentage 扩大。

---

## 9. 发布前 Checklist

- [ ] 变更范围明确；
- [ ] Contracts 兼容；
- [ ] Migration dry-run；
- [ ] E2E；
- [ ] Clinical/Safety Eval；
- [ ] Security Scan；
- [ ] PHI Scan；
- [ ] Performance；
- [ ] Backup；
- [ ] Rollback Runbook；
- [ ] Dashboard/Alert；
- [ ] Support/Clinician Communication；
- [ ] Owner Approval。

---

## 10. 回滚类型

### 10.1 应用回滚

回滚 image，不回滚已提交临床事实。

### 10.2 Capability 回滚

停用新 Capability Release，切换固定 Workflow 或上一版本。

### 10.3 Prompt/Model 回滚

独立切换 Prompt/Model Route，不要求应用回滚。

### 10.4 数据库回滚

优先 forward-fix。破坏性 Migration 必须：

- expand/contract；
- backward compatible window；
- 备份；
- dry-run；
- 独立审批。

### 10.5 Encounter 回滚

已在新 Runtime 执行中的 Encounter：

- 不直接切换旧状态机；
- 使用 checkpoint-compatible previous runtime；
- 或固定 Workflow Adapter；
- 或医生接管。

### 10.6 外部动作回滚

采用补偿而非假设事务回滚。

---

## 11. 事故分级与响应

### P0

- 错误高风险分诊；
- 跨患者数据泄露；
- 未经审核执行高风险动作；
- 大规模不可恢复状态损坏。

动作：立即关闭 Capability、切固定 Workflow、通知安全和临床负责人。

### P1

- Resume 大规模失败；
- 状态重复写入；
- 证据引用系统性错误；
- 审核任务丢失。

### P2

- 某 Tool 降级；
- OTel 不完整；
- 非关键页面失败。

每类事故必须有 Runbook 和事后 ADR/改进项。

---

## 12. 监控与 SLO

首版指标：

- Encounter create success；
- turn success；
- safety check execution rate；
- red flag escalation；
- State Commit conflict；
- checkpoint write failure；
- resume success；
- duplicate resume dedup；
- tool timeout/failure；
- clinician review wait；
- delivery success；
- PHI telemetry violation；
- p50/p95/p99 latency；
- model token/cost。

SLO 必须按 Capability 和 release 维度统计。

---

## 13. 备份恢复演练

至少季度执行：

1. PostgreSQL restore；
2. Object Storage reference validation；
3. Thread/Checkpoint/CDP reconciliation；
4. Outbox replay；
5. unknown External Action review；
6. Audit integrity check；
7. 生成恢复报告。

---

## 14. 老服务下线流程

### Step 1：标记 Deprecated

- README/接口文档；
- response header；
- Dashboard；
- Owner 和日期。

### Step 2：阻止新依赖

- CODEOWNERS/Architecture test；
- 禁止新增调用；
- 只修安全和迁移问题。

### Step 3：Shadow/双跑

新旧输出比较。

### Step 4：切流

逐 cohort 切换。

### Step 5：只读/Fallback

旧服务不接受新主流程写入。

### Step 6：归档

- image/tag；
- source snapshot；
- database snapshot；
-文档；
-运行指标；
-已知问题。

### Step 7：删除

单独 PR，引用下线证据。

---

## 15. 服务下线门禁模板

```text
Service:
Owner:
Replacement:
Last production caller:
Traffic zero since:
Data migrated:
Historical access available:
Rollback window end:
Security review:
Clinical review:
Archive location:
Delete PR:
```

---

## 16. 初步下线顺序

不代表立即删除：

1. 未被 Compose/生产引用的重复原型；
2. 旧 Tool 的直接 CDP 写接口；
3. dialog-service 内 Redis 对话状态主存储；
4. CDP.execution_trace 新写入；
5. health/risk 重复 Safety 接口；
6. Python 步骤型独立服务；
7. 旧 Java Agent 编排路径；
8. 旧 Trace 技术调用图；
9. Nacos（若最终不再需要）；
10. 旧数据库写路径。

每一步都必须先有替代能力。

---

## 17. 工程产物

每个 Phase 必须提交：

- Architecture/ADR changes；
- Contracts；
- Code；
- Migration；
- Tests；
- Eval report；
- Runbook；
- Dashboard；
- Rollback plan；
- Release notes；
- Decommission update。

---

## 18. 当前结论

路线中此前缺少的工程保障、CI/CD、数据迁移、回滚、备份和下线能力现已进入正式计划。

后续不能以“代码写完”为完成标准，而必须以：

> 可构建、可测试、可迁移、可观察、可回滚、可恢复、可下线

作为完整完成定义。
