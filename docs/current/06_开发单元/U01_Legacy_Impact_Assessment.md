# U01 Legacy Impact Assessment

> Unit：U01 Consultation 建立、对象识别与问题框定
>
> 阶段：Unit 前置 Legacy Impact Assessment
>
> 分支：`impl/u01-legacy-impact`
>
> 基线：Foundation-0 `IMPLEMENTED / COMPONENT_VERIFIED / NOT_BUSINESS_WIRED`
>
> 本文件只处理 U01 会触碰的旧资产，不构成 U01 业务实现、U02+ 授权、Clinical Runtime Enablement、Merge Authorization 或旧链物理删除授权。

---

## 1. U01 目标边界

U01 的目标业务状态转换固定为：

```text
START_CONSULTATION
→ Subject Resolution
→ Problem Framing
→ Scope Adjudication
→ Consultation Lifecycle Decision
→ governed state commit
```

U01 不是：

- 旧 `5-step` Workflow 的 Step 1 重命名；
- Risk / Safety Gate / Readiness 的提前实现；
- completeness 阈值判断；
- 主动问诊 U06；
- DDx / Workup / Treatment；
- wellness flow；
- 将旧 `clinical_mode_collecting` 直接等价为新的 Consultation Lifecycle。

---

## 2. 当前真实旧链

当前 `/api/v1/diagnosis/start` 的真实入口仍为：

```text
DiagnosisController.startDiagnosis
→ DiagnosisOrchestrationService.startDiagnosis
→ CDPManager.createCDP
→ HealthStateAssessmentClient.assessHealthState
→ CDPManager.updateCDP
→ legacy response semantics
```

其中 `DiagnosisOrchestrationService.startDiagnosis()` 当前同时承担：

1. Session ID 生成；
2. CDP 创建；
3. 健康状态判定调用；
4. 健康状态服务失败时默认进入 `clinical_mode`；
5. 直接向 `patientState` 写入 userInput/basicInfo/symptoms/vitalSigns；
6. 直接向 CDP 写 `healthStateAssessment` 与 `cdpStatus`；
7. 直接生成 `currentStep=step1_identify_problem`、`completeness`、`nextAction` 等旧流程语义。

继续诊断时，旧链为：

```text
DiagnosisController.continueDiagnosis
→ DiagnosisOrchestrationService.continueDiagnosis
→ CDPManager.updateCDP(userAnswers)
→ DiagnosisWorkflowOrchestrator.step1IdentifyProblem
→ completeness >= 60%
   ├─ true  → executeRemainingSteps
   └─ false → 继续旧 collecting/question 语义
```

`DiagnosisWorkflowOrchestrator` 仍拥有固定 5-step 编排，并在各步骤直接调用 `CDPManager.updateCDP()`。

---

## 3. Legacy Asset Matrix

| Legacy Asset | 当前真实角色 | 当前可达性 | U01 处理状态 | U01 新依赖允许？ | 替代/适配方向 | 删除条件 |
|---|---|---|---|---|---|---|
| `DiagnosisController` | `/start`、`/continue`、status/result 等 HTTP compatibility ingress | ACTIVE / REACHABLE | **ADAPT** | 允许继续作为兼容入口；禁止继续拥有新业务判断 | Controller 只做 transport/validation/routing；按 Runtime Binding 分流到 legacy 或 new U01 application path | 新 API/compatibility 策略稳定且旧 controller 路由不再需要时再评估 |
| `DiagnosisOrchestrationService.startDiagnosis()` | 旧 Consultation 创建 + 健康状态判定 + 直接 CDP 写入 + legacy response | ACTIVE / REACHABLE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | 新 U01 application service / Unit execution path | 新 U01 已 WIRED/VERIFIED；新绑定 Consultation 不再进入该方法；回滚窗口结束 |
| `DiagnosisOrchestrationService.continueDiagnosis()` | 旧回答写入 + Step1 + completeness 路由 | ACTIVE / REACHABLE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | 后续 U07 Resume + U06/U05 路由 | U07 等替代链完成前不得删除 |
| `DiagnosisWorkflowOrchestrator` | 固定 5-step 旧主控 | ACTIVE / REACHABLE | **DEPRECATE + LEGACY_ONLY + BLOCK_NEW_USE** | **NO** | Phase 9 state-driven Unit scheduler / Runtime | 所有目标 Consultation 已迁移；旧入口不可达；无回滚职责 |
| `step1IdentifyProblem()` | Parsing + Dialog + completeness + nextQuestion 混合职责 | ACTIVE / REACHABLE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | U02/C01 + U05/U06/C03 分拆 | U02/U05/U06 各自 WIRED/VERIFIED 后再删除 |
| `executeRemainingSteps()` | completeness 达阈值后固定执行 Step2-5 | ACTIVE / REACHABLE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | U08-U12 等后续 Unit state-driven routing | 对应后续 Units 全部替代并完成迁移后 |
| `HealthStateAssessmentClient` 在 start 链中的使用 | 旧工作态判定；失败时默认 clinical mode | ACTIVE / REACHABLE | **LEGACY_ONLY + BLOCK_NEW_USE** | **NO** | U01 的 D10 Scope Adjudication + 后续 U03/U04 风险/安全语义分别承担 | 新 U01/U03/U04 链替代且兼容入口迁移完成 |
| `CDPManager.createCDP()` | 创建 authoritative CDP 实体与初始版本 | ACTIVE / REACHABLE | **KEEP + ADAPT** | 有条件允许 | 可复用 persistence 创建能力，但新业务状态语义需通过治理链形成 | 除非未来 CDP persistence 被正式替换，否则不删除 |
| `CDPManager.updateCDP()` | 旧万能 Map 更新入口，直接改 Clinical State | ACTIVE / REACHABLE | **BLOCK_NEW_USE** | **NO**（对任何新 governed clinical semantics） | `StateCommitter → StateRepositoryPort → ClinicalCdpStateRepositoryAdapter` | 新链全部迁移且旧链无调用后才可删除/收窄 |
| `CDPRepository` / `CDPVersionService` | authoritative CDP persistence / version history | ACTIVE | **KEEP / ADAPT** | 允许通过治理 Adapter 使用 | P01 Clinical CDP Adapter | 当前无删除计划 |
| `clinical_mode_collecting` / `step1_identify_problem` | 旧流程状态/步骤字符串 | ACTIVE | **LEGACY_ONLY + BLOCK_NEW_USE** | **NO** 作为新 Runtime/Clinical truth | Phase 3 Consultation Lifecycle + Clinical Readiness | Compatibility 输出迁移完成后移除 |
| `completeness >= 60%` | 旧进入后续固定流程的门槛 | ACTIVE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | D03 Clinical Readiness Resolver + D04 Question Stopping Policy | U05/U06 通过且旧链退役后 |
| `patientState.userAnswers` 直接 Map 写入 | 旧回答存储/状态推进 | ACTIVE | **DEPRECATE + BLOCK_NEW_USE** | **NO** | U02 governed Fact + U07 accepted resume event | U02/U07 替代后 |
| Legacy Agent/Dialog private state | 历史 Agent/对话运行状态 | 部分资产存在，非 U01 authority | **BLOCK_NEW_USE** | **NO** 作为 Consultation/Clinical truth | Runtime Thread/Run + governed Clinical State | 后续逐资产验证不可达后处理 |

---

## 4. U01 新链禁止事项（BLOCK_NEW_USE）

任何 U01 / new Runtime 实现禁止：

```text
直接调用 DiagnosisOrchestrationService
直接调用 DiagnosisWorkflowOrchestrator
直接调用 step1IdentifyProblem / executeRemainingSteps
直接通过 CDPManager.updateCDP 写新的 governed clinical semantics
用 HealthStateAssessmentClient 的旧 workMode 结果直接形成新的 Scope/Risk/Safety truth
使用 completeness >= 60% 决定新的 Clinical Readiness
使用 step1_identify_problem 作为新 Runtime Unit identity
使用 clinical_mode_collecting 作为新的 Consultation Lifecycle truth
创建新的私有 Session/Agent State 作为 Consultation 或 Clinical State 第二真源
```

允许复用：

```text
HTTP compatibility route
请求 DTO 中仍有效的 transport 字段
CDP entity / repository persistence
CDPVersionService
纯工具函数
不拥有业务真值的 trace/helper
Foundation-0 Runtime Binding / Canonical Event / Run / Version Binding / Trace correlation
```

---

## 5. U01 的 Strangler 分流点

U01 应把“新旧主控分流”固定在 Consultation 建立入口，而不是在旧 5-step 内部插入条件：

```text
POST /api/v1/diagnosis/start
↓
transport validation
↓
建立/解析 Consultation identity + CDP
↓
RuntimeBinding
├─ LEGACY
│  → 现有 DiagnosisOrchestrationService.startDiagnosis
│
└─ CLINICAL_RUNTIME_V1
   → U01 new application path
      → Canonical START_CONSULTATION event
      → C01 subject/problem candidate
      → D10 Scope Adjudication
      → D01 Lifecycle Decision
      → K09 Proposal
      → G2 / StateCommitter / P01
      → committed Clinical State Version
```

严格禁止：

```text
CLINICAL_RUNTIME_V1
→ 新 U01
→ 再回调旧 DiagnosisOrchestrationService / fixed Workflow
```

否则会形成名义新链、实际旧主控。

---

## 6. 旧链当前不得删除的原因

目前 Foundation-0 仅为：

```text
IMPLEMENTED / COMPONENT_VERIFIED / NOT_BUSINESS_WIRED
```

旧 `/start` 与 `/continue` 仍是真实可达业务路径，因此：

```text
旧链 = 仍需承担 legacy-bound Consultation 与回滚职责
```

在 U01 新链未达到 `WIRED + VERIFIED` 前，不允许物理删除：

- `DiagnosisOrchestrationService`；
- `DiagnosisWorkflowOrchestrator`；
- 旧 `/start` compatibility behavior；
- `CDPManager.updateCDP()` 的 legacy callers。

---

## 7. U01 PASS 后的 Legacy Retirement Update 预期

U01 完成并通过 Unit Verification 后，只允许重新评估与 U01 直接相关资产：

```text
DiagnosisController
ADAPT → 继续 compatibility ingress

DiagnosisOrchestrationService.startDiagnosis
DEPRECATE/BLOCK_NEW_USE → 对 CLINICAL_RUNTIME_V1 不可达

DiagnosisWorkflowOrchestrator
LEGACY_ONLY → 对 CLINICAL_RUNTIME_V1 不可达

CDPManager.updateCDP
BLOCK_NEW_USE → U01 新链零调用

legacy status/currentStep/completeness routing
LEGACY_ONLY → U01 new path 零依赖
```

此时仍不自动进入 `REMOVED`。

物理删除必须等到：

1. 新实现 `WIRED`；
2. 新实现 `VERIFIED`；
3. 对应业务路径迁移；
4. 旧入口对目标 Consultation 不可达；
5. 无新代码继续依赖；
6. 旧测试已迁移/替代；
7. 回滚窗口结束或已有替代回滚机制；
8. 删除后 Regression PASS。

---

## 8. 当前结论

```text
U01 Legacy Impact Assessment = COMPLETE

DiagnosisController
= ADAPT

DiagnosisOrchestrationService
= DEPRECATE / BLOCK_NEW_USE

DiagnosisWorkflowOrchestrator
= DEPRECATE / LEGACY_ONLY / BLOCK_NEW_USE

CDPManager.createCDP
= KEEP / ADAPT

CDPManager.updateCDP
= BLOCK_NEW_USE FOR NEW GOVERNED CLINICAL SEMANTICS

CDPRepository / CDPVersionService
= KEEP / ADAPT

Legacy completeness/currentStep/workMode routing
= DEPRECATE / BLOCK_NEW_USE

Physical Legacy Removal
= NOT AUTHORIZED

U01 Business Implementation
= NOT STARTED BY THIS CHANGE

Merge Authorization
= NOT GRANTED
```
