# AIdoctor 前端与业务迁移方案

> 文档状态：Draft v2.6  
> 更新时间：2026-07-29  
> 范围：患者端、医生端、管理端、Business API、实时通信、Capability/RAG/Model 发布展示、旧接口兼容和业务闭环。

---

## 1. 迁移目标

前端不再围绕“当前执行到第几个工具”组织，而围绕以下业务状态：

```text
Encounter
Thread / Run
Interrupt / Resume
ReviewTask
DeliveryPackage
FollowUpTask
Capability Release
Evidence / Citation
```

前端只消费经过授权的业务 DTO，不直接读取 LangGraph State、原始 Prompt、Provider 请求或完整内部推理过程。

---

## 2. 业务 API 分层

### 2.1 v1 兼容 API

保留旧 `/diagnosis/start`、`/continue`、`/status`、`/result` 语义，由 Compatibility Controller 转换到新 Business Command。

兼容 API 必须：

- 返回 Deprecation Header；
- 记录旧调用方；
- 映射新 correlation ID；
- 不暴露新内部状态；
- 支持明确下线日期。

### 2.2 v2 API

建议资源：

```text
POST   /api/v2/encounters
GET    /api/v2/encounters/{id}
POST   /api/v2/threads/{id}/messages
GET    /api/v2/threads/{id}
POST   /api/v2/threads/{id}/resume
POST   /api/v2/encounters/{id}/artifacts
GET    /api/v2/encounters/{id}/delivery
GET    /api/v2/reviews
POST   /api/v2/reviews/{id}/decision
GET    /api/v2/follow-ups/{id}
```

所有写操作必须携带 authentication、expected version 和 idempotency key。

---

## 3. 患者端状态模型

```text
creating
collecting_information
awaiting_user
awaiting_artifact
processing
awaiting_clinician
completed
cancelled
expired
failed_recoverable
```

患者端必须支持：

- 页面刷新后恢复 Thread；
- Interrupt 问题和上传请求；
- 重复点击不重复提交；
- 网络断开后的状态查询；
- 等待医生审核提示；
- 明确的错误和降级状态；
- PatientDelivery 和后续随访。

不得显示：

- 内部 Prompt；
- 供应商模型名称；
- 未批准的诊断候选；
- 内部 Chain-of-Thought；
- 其他患者或租户信息。

---

## 4. Evidence 与 Citation 展示

患者端：

- 只展示经批准的患者友好结论；
- 显示来源机构、发布时间和适用限制；
- 明确“不确定”“证据不足”“不同来源存在差异”；
- 不提供超出许可范围的全文。

医生端：

- Claim 与 SourceSpan 对应；
- Source Tier、版本、地区、人群和有效期；
- 支持、反对和冲突证据；
- Citation Validation 结果；
- `knowledge_release_id`；
- 检索为何选择该来源。

管理端：

- Source Registry；
- Ingestion 状态；
- Knowledge Release；
- Retrieval/Citation Eval；
- 过期、撤回和回滚。

---

## 5. 医生端

### 5.1 Review Queue

显示：

- 优先级和 SLA；
- 高风险原因；
- Encounter 和 Capability；
- 等待时间；
- 分配状态；
- 版本冲突提示。

### 5.2 Review Workspace

至少包含：

- 患者原始来源事实；
- Observation 和确认状态；
- Triage 与红旗；
- 有限 Hypothesis；
- EvidencePack；
- AgentEvent 时间线；
- Tool/Model 失败摘要；
- 医生编辑、批准、拒绝、补充信息和升级。

医生修改必须生成新的 StatePatch 和 ClinicalDecisionRecord，而不是只修改展示文本。

### 5.3 版本可见性

医生可查看：

- Capability Release；
- Knowledge Release；
- 参与的 Prompt/Model Route 版本摘要；
- Safety Policy；
- CDP 和 Checkpoint 版本。

默认不展示完整 Prompt 和供应商原始响应。

---

## 6. 管理端

### 6.1 Capability 管理

- Package/Release 列表；
- 支持和排除范围；
- Safety、Knowledge、Prompt、Model 和 Eval 绑定；
- Shadow/Canary 状态；
- Emergency Disable；
- 回滚。

### 6.2 Prompt 管理

- PromptSpec 和 Release；
- diff、Owner、Reviewer；
- 输入/输出 Schema；
- 允许 Route；
- Eval 报告；
- 发布、弃用和回滚。

权限上，普通运营人员不得查看包含敏感系统策略的完整 Prompt。

### 6.3 Model 管理

- ModelSpec；
- Route Policy；
- Provider health；
- 数据驻留和 PHI 策略；
- 延迟、成本和 Eval；
- Fallback；
- Emergency Disable。

### 6.4 Knowledge 管理

- Source Registry；
- License/Access；
- Ingestion Run；
- Chunk 抽检；
- Knowledge Release；
- Embedding/BM25/Reranker 版本；
- Graph Release 和净收益评估；
- 撤回、过期和回滚。

---

## 7. Trace 与审计页面拆分

不得继续使用一个页面混合所有信息。

| 视图 | 内容 | 主要用户 |
|---|---|---|
| Technical Trace | Span、延迟、错误、服务调用 | 工程人员 |
| AgentEvent | Node、Route、Retry、Fallback、Resume | 工程/产品 |
| ClinicalDecisionRecord | 规则、证据、医生修改和版本链 | 医生/审核 |
| Compliance Audit | 访问、修改、批准、导出 | 合规人员 |

所有视图遵守字段级权限和 PHI 最小化。

---

## 8. 实时通信

推荐：

- SSE：状态更新、AgentEvent 摘要、Delivery 就绪；
- WebSocket：确有双向实时需求的场景；
- REST：命令、查询、Resume 和上传。

实时连接不是状态源。断线后必须通过 Thread API 恢复，前端不能依赖内存中的消息顺序判断临床状态。

事件统一包含：

```text
event_id
event_type
encounter_id
thread_id
run_id
sequence
occurred_at
correlation_id
payload_version
```

---

## 9. Frontend Store

建议分域：

```text
identityStore
encounterStore
threadStore
interruptStore
reviewStore
deliveryStore
evidenceStore
releaseAdminStore
telemetryStore
```

禁止将完整患者资料、Prompt 或 Provider 原始响应长期保存在浏览器 Local Storage。

---

## 10. 错误模型

统一错误结构：

```text
code
message
correlation_id
retryable
user_action
current_status
expected_version
actual_version
```

重点状态：

- `STALE_VERSION`；
- `DUPLICATE_REQUEST`；
- `AWAITING_REVIEW`；
- `CAPABILITY_OUT_OF_SCOPE`；
- `KNOWLEDGE_INSUFFICIENT`；
- `MODEL_DEGRADED`；
- `SERVICE_RECOVERING`；
- `CONSENT_REQUIRED`。

患者端错误文本不得暴露堆栈、Prompt、内部 Policy 或供应商信息。

---

## 11. Feature Flag 与放量

Flag 层级：

- tenant；
- user cohort；
- capability release；
- frontend feature；
- model route；
- knowledge release；
- graph expansion；
- clinician review requirement。

放量顺序：

```text
internal
→ shadow
→ clinician assist
→ restricted patient cohort
→ canary
→ broader rollout
```

前端必须支持随时切回固定 Workflow 或人工支持页面。

---

## 12. 测试

### 患者端

- 创建 Encounter；
- 多轮提问；
- 刷新恢复；
- 上传失败；
- 红旗升级；
- 等待医生；
- 重复提交；
- RAG 无结果和冲突展示；
- 降级和取消。

### 医生端

- Review 分配；
- approve/edit/reject；
- 多医生并发；
- stale version；
- Evidence/Citation；
- 医生修改进入 CDP；
- Audit。

### 管理端

- Prompt/Model/Knowledge/Capability 发布权限；
- Eval 不达标禁止发布；
- Emergency Disable；
- 回滚；
- 敏感字段隐藏；
- Graph Release 门禁。

---

## 13. 迁移顺序

```text
F1 建立 v2 API Client 和统一错误
→ F2 Thread/Interrupt/Resume 患者主链路
→ F3 Delivery 与 Evidence 展示
→ F4 医生 Review Queue/Workspace
→ F5 Trace/AgentEvent/Decision/Audit 分视图
→ F6 Capability/Prompt/Model/Knowledge 管理端
→ F7 Shadow/Canary 与旧页面下线
```

旧页面下线前必须确认无流量、无功能缺口、可回滚且用户支持流程已更新。

---

## 14. 当前结论

前端迁移不是简单改 API 地址，而是从“工具步骤页面”升级为“受版本治理的 Encounter/Thread/Review/Delivery 系统”。患者、医生和管理端看到的内容不同；Knowledge、Prompt、Model 和 Capability 的管理能力必须进入后台，但内部 Prompt、模型原始响应和私有推理不得暴露给普通用户。