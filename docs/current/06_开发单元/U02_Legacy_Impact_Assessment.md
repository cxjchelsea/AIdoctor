# U02 Legacy Impact Assessment

> Unit：U02 临床事实形成与版本提交  
> 阶段：Unit 前置 Legacy Impact Assessment  
> 分支：`prep/u02-clinical-fact-formation`  
> 基线：`main@e0b9d776fec9b9510b4d6b68425f87f8f5fbc87c`（Foundation-1 + C01-U01 已合并）  
> 本文件只识别 U02 会触碰的旧资产、允许复用边界和退役条件；不构成 U02 Implementation Authorization、Clinical Runtime Enablement、Merge Authorization 或物理删除授权。

---

## 1. U02 冻结业务目标

U02 的目标状态转换来自 Phase 6：

```text
Consultation ACTIVE / 合法 Resume / 合法 Correction Input
+
NEW_CLINICAL_INPUT
↓
C01 Clinical Observation Candidate
↓
Business Owner / deterministic interpretation
↓
K09 StateChangeProposal
↓
P01 / G2 governed commit
↓
Clinical State Version n+1
+
current effective Clinical Facts
+
minimum dependent-state invalidation markers
```

必须保持：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
MODEL_INFERRED != PATIENT_REPORTED
Capability Result != Clinical Truth
Candidate != Decision != Proposal != Commit
```

U02 不负责 U03 Risk、U04 Safety Gate、U05 Readiness、U06 Question、U08 DDx、U11/U12 Delivery。

---

## 2. 当前真实相关旧链

根据现有 U01 Legacy Impact Assessment，旧 `/continue` 路径仍包含：

```text
DiagnosisController.continueDiagnosis
→ DiagnosisOrchestrationService.continueDiagnosis
→ CDPManager.updateCDP(userAnswers)
→ DiagnosisWorkflowOrchestrator.step1IdentifyProblem
→ completeness >= 60%
```

其中 `step1IdentifyProblem()` 混合承担 Parsing、Dialog、completeness、nextQuestion 等职责，并由旧 Workflow 直接调用 `CDPManager.updateCDP()`。

这条旧链不满足 U02：

- 没有 K03 Observation Candidate / Fact lifecycle 分层；
- 没有 Candidate → Decision → Proposal → Commit；
- 用户回答仍可能以 Map 形式直接写入 patientState/CDP；
- `UNKNOWN / UNMEASURED / NOT_ASKED` 等值语义没有正式类型边界；
- 新事实没有强制形成新的 Clinical State Version；
- 更正后的依赖失效没有正式 D05 hook。

---

## 3. Legacy / Existing Asset Matrix

| Asset | 当前角色 | U02 处置 | U02 新依赖允许？ | U02 适配/替代方向 | 删除条件 |
|---|---|---|---|---|---|
| `DiagnosisOrchestrationService.continueDiagnosis()` | 旧回答写入与 fixed workflow 推进 | **DEPRECATE + BLOCK_NEW_USE** | **NO** | U07 后续负责 Resume；U02 只接收已接受的 Clinical Input Event | U07/U02 新链 WIRED+VERIFIED、旧路由迁移后 |
| `DiagnosisWorkflowOrchestrator.step1IdentifyProblem()` | Parsing + Dialog + completeness 混合 | **DEPRECATE + BLOCK_NEW_USE** | **NO** | C01-U02 + U05/U06 分拆 | U02/U05/U06 均完成替代后 |
| `DiagnosisWorkflowOrchestrator` fixed 5-step | 旧主控 | **LEGACY_ONLY + BLOCK_NEW_USE** | **NO** | Phase 9 Unit-driven Runtime | 全目标 Unit 替代并迁移后 |
| `CDPManager.updateCDP()` | 万能 Map 直接写 Clinical State | **BLOCK_NEW_USE** | **NO** | K09 Proposal → P01/G2/StateCommitter → Clinical CDP Adapter | 新 governed path 全部迁移后 |
| `patientState.userAnswers` / symptom Map direct write | 旧回答与事实存储 | **DEPRECATE + BLOCK_NEW_USE** | **NO** | typed K03 Clinical Observation / governed Facts | U02/U07 替代后 |
| legacy Clinical Parsing / Dialog NLU low-level assets | 文本理解底层资产 | **ADAPT / REFACTOR** | **YES, only below C01 boundary** | 由 C01-U02 封装成 typed candidate-only output；不得携带 direct write semantics | 新 C01 覆盖后按逐资产验证处理 |
| legacy `suggested_writes` / tool direct patch semantics | 能力结果夹带写状态建议 | **REPLACE / BLOCK_NEW_USE** | **NO** | capability candidate-only result；Proposal 只能由 Business Owner 产生 | 新 C01/P01 链稳定后 |
| `StateCommitter` | K09/P01 机械 commit 基础 | **KEEP + ADAPT** | **YES** | 扩展 typed Clinical Fact proposal/permission/idempotency path | 当前无删除计划 |
| `StatePatchBoundaryValidator` | patch 边界校验 | **KEEP + ADAPT** | **YES** | 增加 U02 Clinical Fact typed operation / path 权限校验 | 当前无删除计划 |
| `ClinicalCdpStateRepositoryAdapter` | P01 CDP 持久化 adapter | **KEEP + ADAPT** | **YES** | 支撑新 Clinical State Version 写入 | 当前无删除计划 |
| CDP persistence/version assets | authoritative persistence/history | **KEEP / ADAPT** | **YES through P01** | 不允许业务代码绕过 P01 直接写 | 当前无删除计划 |
| Foundation-1 P06 binding governance | Capability 可用性治理 | **KEEP / REUSE** | **YES** | C01-U02 invocation 绑定合法 CapabilityBindingRef | 当前无删除计划 |
| Foundation-1 Resolver / Invocation Guard | 调用前 fail-closed 校验 | **KEEP / REUSE** | **YES** | C01-U02 必经 | 当前无删除计划 |
| Foundation-1 P05 capability-call trace | 调用关联基础 | **KEEP / REUSE + U02 INCREMENT** | **YES** | 绑定 result→decision→proposal→commit→state version | 当前无删除计划 |
| C01-U01 slice（PR #80） | U01 所需 C01 typed candidate/Java gateway/internal wiring | **MERGED PREDECESSOR / INCREMENTAL BASELINE** | **YES** | U02 必须在该基础上增量扩展，不得平行重建 C01 foundation | N/A |

---

## 4. U02 新链禁止事项

```text
直接调用 DiagnosisOrchestrationService.continueDiagnosis
直接调用 DiagnosisWorkflowOrchestrator / step1IdentifyProblem
直接通过 CDPManager.updateCDP 写新的 Clinical Facts
把 legacy userAnswers Map 当作正式 Clinical Facts
把 C01 输出直接作为 committed Clinical State
由 C01 直接生成正式 K09 StateChangeProposal
把 UNKNOWN 写成 NO
把 UNMEASURED 写成 NORMAL
把 MODEL_INFERRED 写成 PATIENT_REPORTED
无 base Clinical State Version 校验直接提交
同一 NEW_CLINICAL_INPUT 产生重复 Clinical effect
Correction 后静默覆盖历史版本
为 U02 提前实现 U03 Risk / U06 Question / U08 DDx
```

---

## 5. U02 Strangler / 接线边界

U02 不应嵌入旧 Step1 内部。目标接线应为：

```text
accepted NEW_CLINICAL_INPUT
↓
Clinical Runtime / Unit Router
↓
U02 Application / Business Owner
↓
CapabilityInvocationGuard
↓
C01-U02 candidate-only Clinical Understanding
↓
Business interpretation / acceptance
↓
K09 typed StateChangeProposal
↓
P01 / G2 / StateCommitter
↓
ClinicalCdpStateRepositoryAdapter
↓
Clinical State Version n+1
↓
minimum D05 invalidation markers
↓
route U03 / U05-U06 / U11-U14
```

旧 fixed workflow 继续只服务 legacy-bound consultation，直到对应迁移门槛满足。

---

## 6. 物理删除仍不授权

本阶段不删除：

- `DiagnosisOrchestrationService`；
- `DiagnosisWorkflowOrchestrator`；
- `step1IdentifyProblem()`；
- `CDPManager.updateCDP()`；
- legacy patientState/userAnswers storage；
- legacy Parsing/Dialog assets。

物理删除至少等待：目标新路径 `WIRED + VERIFIED`、旧入口对目标 Consultation 不可达、回归通过、回滚窗口结束。

---

## 7. 当前结论

```text
U02 Legacy Impact Assessment = COMPLETE

Legacy fixed workflow
= LEGACY_ONLY / BLOCK_NEW_USE FOR U02

Legacy direct CDP write
= BLOCK_NEW_USE

Legacy Parsing/NLU low-level assets
= ADAPT / REFACTOR BELOW C01 BOUNDARY

StateCommitter / Clinical CDP Adapter
= KEEP + ADAPT

Foundation-1 governance/trace
= REUSE

C01-U01 Mainline Predecessor
= SATISFIED / MERGED AS PR #80

Physical Legacy Removal
= NOT AUTHORIZED

U02 Business Implementation
= NOT AUTHORIZED BY THIS DOCUMENT
```
