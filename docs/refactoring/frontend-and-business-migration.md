# AIdoctor 前端与业务迁移方案

> 文档状态：Draft v2.5  
> 更新时间：2026-07-29  
> 范围：患者端、医生端、管理端、Business API、实时通信、旧接口兼容和业务闭环。

---

## 1. 当前状态

当前前端技术栈：

- React 18；
- TypeScript；
- Vite；
- Ant Design；
- Zustand；
- Axios；
- React Flow；
- SockJS + STOMP。

当前业务入口主要由 `diagnosis-service` 提供：

- 启动诊断；
- 继续诊断；
- 查询状态；
- 查询结果；
- 旧回答接口兼容；
- 健康筛查启动。

当前 dialog-service 另提供 WebSocket 对话接口，Trace 管理页面读取自定义执行追踪。

---

## 2. 目标交互模型

前端不直接调用内部 Safety、Clinical Intelligence、Tool 或模型服务。

```text
Patient UI / Clinician Console / Admin Console
                    ↓
          Business & Care Delivery API
                    ↓
             Agent Runtime Gateway
                    ↓
         Runtime / State / Review / Tool
```

前端只理解业务对象：

- Encounter；
- AgentTurn；
- Interrupt；
- ReviewTask；
- DeliveryPackage；
- CarePath；
- FollowUpPlan；
- ExternalActionStatus；
- Timeline View。

---

## 3. API 版本策略

### 3.1 新 API

建议：

```text
POST   /api/v2/encounters
GET    /api/v2/encounters/{encounter_id}
POST   /api/v2/encounters/{encounter_id}/messages
POST   /api/v2/encounters/{encounter_id}/artifacts
GET    /api/v2/encounters/{encounter_id}/turns
GET    /api/v2/encounters/{encounter_id}/delivery
POST   /api/v2/threads/{thread_id}/resume
POST   /api/v2/review-tasks/{review_id}/decision
GET    /api/v2/review-tasks
GET    /api/v2/follow-up-tasks
```

### 3.2 旧 API 兼容

```text
/api/v1/diagnosis/start
/api/v1/diagnosis/continue
/api/v1/diagnosis/{id}/status
/api/v1/diagnosis/{id}/result
/api/v1/diagnosis/{id}/answer
```

旧接口通过 Compatibility Controller 转换：

```text
Legacy Request
→ v2 Command
→ New Runtime or Fixed Workflow Adapter
→ v2 Result
→ Legacy Response Mapper
```

### 3.3 兼容期限

旧接口必须记录：

- 调用方；
- 调用量；
- 错误率；
- 返回差异；
- deprecation header；
- 计划下线日期。

无调用并经过回滚期后才能删除。

---

## 4. 患者端状态机

患者端不应只依赖自由文本消息，必须显示 Thread 状态。

```text
CREATING
RUNNING
AWAITING_USER
AWAITING_ARTIFACT
AWAITING_CLINICIAN
RETRYING
DEGRADED
COMPLETED
CANCELLED
EXPIRED
FAILED
```

### 4.1 患者端主要页面

1. Encounter 创建；
2. 问诊对话；
3. 文件/报告上传；
4. 等待医生审核；
5. 患者版结果；
6. 就医导航；
7. 随访任务；
8. 历史 Encounter；
9. Consent 和数据管理。

### 4.2 问诊页面

显示：

- 当前问题；
- 为什么需要该信息的简短解释；
- 输入类型；
- 允许跳过/不知道；
- 高风险提示；
- 上传入口；
- 连接/恢复状态；
- 最近提交状态；
- 当前能力边界。

不显示：

- 模型 Chain of Thought；
- 内部完整疾病候选；
- 系统 Prompt；
- 未审核的治疗建议；
- 内部错误堆栈。

---

## 5. 实时通信

### 5.1 通信模型

推荐：

- Command 使用 HTTP；
- 实时进度使用 SSE；
- 必须双向高频通信时再使用 WebSocket；
- Resume 使用明确的 HTTP Command，不依赖 WebSocket 重连自动触发。

### 5.2 事件类型

```text
encounter.created
run.started
turn.processing
question.ready
artifact.required
review.required
delivery.ready
run.degraded
run.failed
thread.expired
```

前端事件不直接暴露内部节点名称和敏感数据。

### 5.3 断线恢复

客户端保存：

- encounter_id；
- thread_id；
- last_event_id；
- current interrupt_id；
- expected_checkpoint_id；
- last submitted resume_request_id。

重连流程：

```text
GET Encounter State
→ compare last_event_id
→ replay safe UI events
→ if Awaiting Input, render Interrupt
→ user explicitly submits ResumeRequest
```

### 5.4 重复提交

按钮提交后：

- 本地立即禁用；
- 使用稳定 `resume_request_id`；
- 网络重试复用同一 ID；
- 服务器返回已有处理结果；
- 不因刷新页面生成新提交。

---

## 6. 医生端

### 6.1 Review Queue

医生列表显示：

- priority；
- reason codes；
- red flags；
-等待时长；
- Capability；
-患者基本信息最小集；
- assigned clinician；
- current cdp version。

### 6.2 Review Workspace

分区：

1. 患者原始资料；
2. 结构化 Observation；
3. 风险和分诊；
4. 诊断候选与支持/反对证据；
5. EvidencePack 和引用；
6. Agent 已执行步骤；
7. 医生修改区；
8. 决策按钮。

操作：

```text
APPROVE
EDIT_AND_APPROVE
REJECT
REQUEST_MORE_INFO
ESCALATE
CANCEL_AUTOMATION
```

### 6.3 并发

- 打开任务时显示版本；
- 提交携带 expected_cdp_version 和 expected_review_version；
- 冲突时不覆盖其他医生修改；
- 已处理任务只读；
- 所有操作写 Audit。

---

## 7. 管理端

### 7.1 Capability 管理

- 当前版本；
-适用人群；
-允许动作；
-Safety Policy；
-Context Policy；
-知识来源；
-模型路由；
-评估报告；
-发布状态；
-紧急停用。

### 7.2 Observability 管理

分开显示：

- Technical Trace；
- AgentEvent Timeline；
- ClinicalDecisionRecord；
- Compliance Audit；
- Recovery/Reconciliation；
- Evaluation Reports。

不能将所有内容合并成一个“Trace”。

### 7.3 旧 Trace 页面迁移

保留：

- ReactFlow 图；
-时间线；
-调用树；
-耗时统计；
-错误统计。

改造：

```text
Technical graph
→ OTel Trace API

Agent route timeline
→ AgentEvent API

Clinical rationale
→ ClinicalDecisionRecord API

Access/change history
→ Audit API
```

---

## 8. 前端数据模型

禁止在页面中继续使用无结构的“大 diagnosis response”覆盖所有状态。

建议 Store：

```text
encounterStore
threadStore
turnStore
interruptStore
reviewStore
deliveryStore
followUpStore
telemetryViewStore
```

核心前端模型必须从 OpenAPI 生成或与 JSON Schema 对齐。

---

## 9. 错误模型

前端区分：

```text
VALIDATION_ERROR
OUT_OF_SCOPE
CONSENT_REQUIRED
PERMISSION_DENIED
VERSION_CONFLICT
REVIEW_REQUIRED
RETRYABLE_FAILURE
SERVICE_DEGRADED
TERMINAL_FAILURE
THREAD_EXPIRED
```

不将所有错误统一显示为“系统异常”。

每个错误包含：

- user_message；
- error_code；
- retryable；
- next_action；
- correlation_id；
- support_reference。

---

## 10. 患者输出迁移

旧结果响应迁为 `PatientDelivery`：

- 已确认信息摘要；
- 风险等级和原因；
- 建议就医时效；
- 下一步行动；
- 局限性和不确定性；
- 何时需要紧急处理；
- 引用的患者可读说明；
- 医生审核状态。

禁止：

- 将候选诊断写成确定诊断；
- 展示模型内部推理；
- 输出未经审核的处方和用药调整；
- 用“置信度百分比”制造临床确定性。

---

## 11. 前端迁移阶段

### UI-A：基线

- 前端 build/lint；
- 现有路由和 API 清单；
- 页面截图和行为基线；
- 当前 Store 清单；
- 自定义 Trace 页面数据源。

### UI-B：v2 API Client

- OpenAPI 生成；
- 新 Error Model；
- Encounter/Thread Store；
- 旧 API Adapter；
- feature flag。

### UI-C：第一条 Agent 主链路

- 新建 Encounter；
- 提交消息；
- 显示问题；
- Interrupt/Resume；
- 服务重启恢复；
- 简单 PatientDelivery。

### UI-D：文件与证据

- SourceArtifact 上传；
- 质量状态；
- Evidence 展示；
- 引用跳转。

### UI-E：医生审核

- Review Queue；
- Review Workspace；
- 冲突处理；
- 医生修改；
- Delivery 发布。

### UI-F：管理与可观测

- Capability；
- AgentEvent；
- OTel Trace；
- Audit；
- Eval；
- Recovery Dashboard。

---

## 12. 测试

### 12.1 Unit

- Store reducer；
-状态映射；
-错误映射；
-重复提交；
-权限 UI。

### 12.2 Component

- Question Card；
- Artifact Upload；
- Risk Banner；
- Review Panel；
- Delivery View；
- Timeline。

### 12.3 E2E

- 正常跨轮；
- 红旗；
- 输入不足；
- 重复提交；
- 刷新恢复；
- 服务重启；
- 医生审核；
- 权限拒绝；
- 过期 Thread；
- 旧 API 兼容。

### 12.4 Accessibility

- 键盘操作；
- screen reader；
- 高风险提示不只依赖颜色；
- 表单错误关联；
- 对话流焦点管理。

---

## 13. 业务迁移和流量策略

### 13.1 Feature Flag

按：

- tenant；
- organization；
- clinician；
- patient cohort；
- Capability；
- percentage；
- internal user。

### 13.2 Shadow

旧 Workflow 正常返回，新 Agent 后台执行但不影响患者。

比较：

- 结构化 Observation；
-红旗；
-问题；
-分诊；
-输出；
-耗时；
-失败。

### 13.3 Clinician Assist

新 Agent 结果只给医生，不直接给患者。

### 13.4 Restricted Patient

经过医生和安全评估后，只对低风险、明确范围人群开放。

### 13.5 Rollback

按 encounter runtime version 回退旧 Workflow；已进入新 Runtime 的 Encounter 不应在中途无迁移地切回旧状态机。

---

## 14. 下线门禁

旧患者问诊路径下线前：

- [ ] v2 E2E 通过；
- [ ] Shadow 差异达标；
- [ ] 回滚演练；
- [ ] 旧 API 调用量为零或有豁免；
- [ ] 历史 Encounter 可查看；
- [ ] 医生和客服培训；
- [ ] 监控和告警；
- [ ] 数据迁移对账；
- [ ] 安全评审；
- [ ] 发布审批。

旧 Trace 页面下线前：

- [ ] OTel 调用图可用；
- [ ] AgentEvent 时间线可用；
- [ ] 历史 Trace 归档；
- [ ] PHI 清理；
- [ ] 运维 Runbook 更新。

---

## 15. 当前结论

前端不需要重写全部 React 组件，但必须重写：

- API 和错误契约；
- Encounter/Thread 状态模型；
- Interrupt/Resume；
- 医生 ReviewTask；
- Trace 数据源；
- 测试体系。

业务端则采用 v1 Compatibility + v2 Business API 的渐进迁移，而不是让前端直接连接 Python Agent Runtime。
