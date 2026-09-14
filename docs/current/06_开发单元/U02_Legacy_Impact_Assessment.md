# U02 Legacy Impact Assessment

> Unit：U02 临床事实形成与版本提交  
> 阶段：Unit 前置 Legacy Impact Assessment  
> 原前置分支：`prep/u02-clinical-fact-formation` / PR #82  
> 当前承载分支：`impl/u02-clinical-fact-formation`  
> 基线：`main@e0b9d776fec9b9510b4d6b68425f87f8f5fbc87c`（Foundation-1 + C01-U01 已合并）  
> 本文件记录 U02 会触碰的旧资产、允许复用边界和退役条件；不构成 Clinical Runtime Enablement、Merge Authorization 或物理删除授权。

---

## 1. U02 冻结业务目标

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

## 2. 当前相关 Legacy 链

旧 `/continue` 路径仍包含：

```text
DiagnosisController.continueDiagnosis
→ DiagnosisOrchestrationService.continueDiagnosis
→ CDPManager.updateCDP(userAnswers)
→ DiagnosisWorkflowOrchestrator.step1IdentifyProblem
→ completeness >= 60%
```

旧链没有 K03 Observation Candidate / Fact lifecycle 分层，没有 Candidate→Decision→Proposal→Commit，仍存在 Map/direct-write 语义，也没有正式版本提交和 D05 依赖失效语义，因此不能成为新的 U02 实现入口。

---

## 3. Legacy / Existing Asset Matrix

| Asset | U02 处置 | 新依赖允许？ | U02 方向 | 删除条件 |
|---|---|---|---|---|
| `DiagnosisOrchestrationService.continueDiagnosis()` | DEPRECATE + BLOCK_NEW_USE | NO | U07 后续负责 Resume；U02 接收已接受 Clinical Input Event | U07/U02 新链 WIRED+VERIFIED、旧路由迁移后 |
| `DiagnosisWorkflowOrchestrator.step1IdentifyProblem()` | DEPRECATE + BLOCK_NEW_USE | NO | C01-U02 + U05/U06 分拆 | U02/U05/U06 完成替代后 |
| `DiagnosisWorkflowOrchestrator` fixed 5-step | LEGACY_ONLY + BLOCK_NEW_USE | NO | Phase 9 Unit-driven Runtime | 全目标 Unit 替代并迁移后 |
| `CDPManager.updateCDP()` | BLOCK_NEW_USE | NO | K09→P01/G2/StateCommitter→Clinical CDP Adapter | governed path 全迁移后 |
| patientState/userAnswers Map direct write | DEPRECATE + BLOCK_NEW_USE | NO | typed K03 / governed facts | U02/U07 替代后 |
| legacy Clinical Parsing/NLU low-level assets | ADAPT / REFACTOR | YES，仅 C01 boundary 以下 | C01-U02 candidate-only output | 新 C01 覆盖后逐资产处理 |
| legacy `suggested_writes` / tool direct patch | REPLACE / BLOCK_NEW_USE | NO | Proposal 只能由 Business Owner 产生 | 新 C01/P01 稳定后 |
| `StateCommitter` | KEEP + ADAPT | YES | typed Clinical Fact proposal/permission/idempotency | 无删除计划 |
| `StatePatchBoundaryValidator` | KEEP + ADAPT | YES | U02 typed operation/path validation | 无删除计划 |
| `ClinicalCdpStateRepositoryAdapter` | KEEP + ADAPT | YES | 新 Clinical State Version 写入 | 无删除计划 |
| CDP persistence/version assets | KEEP / ADAPT | YES through P01 | 禁止业务代码绕过 P01 | 无删除计划 |
| Foundation-1 P06 binding governance | KEEP / REUSE | YES | C01-U02 valid binding | 无删除计划 |
| Foundation-1 Resolver / Invocation Guard | KEEP / REUSE | YES | C01-U02 必经 | 无删除计划 |
| Foundation-1 P05 capability-call trace | KEEP / REUSE + U02 INCREMENT | YES | result→decision→proposal→commit-result/version refs | 无删除计划 |
| C01-U01 PR #80 | MERGED PREDECESSOR / INCREMENTAL BASELINE | YES | 不得平行重建 C01 foundation | N/A |

---

## 4. U02 新链禁止事项

```text
直接调用 legacy continueDiagnosis / fixed workflow
直接通过 CDPManager.updateCDP 写新 Clinical Facts
把 legacy userAnswers Map 当正式 Clinical Facts
把 C01 输出直接作为 committed Clinical State
由 C01 直接生成正式 K09 Proposal
UNKNOWN → NO
UNMEASURED → NORMAL
MODEL_INFERRED → PATIENT_REPORTED
无 base Clinical State Version 校验直接提交
同一 NEW_CLINICAL_INPUT 产生重复 Clinical effect
Correction 静默覆盖历史版本
为 U02 提前实现 U03/U06/U08
```

---

## 5. Strangler / 接线边界

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
route downstream unit
```

旧 fixed workflow 继续只服务 legacy-bound consultation，直到迁移门槛满足。

---

## 6. 物理删除仍不授权

当前不删除 `DiagnosisOrchestrationService`、`DiagnosisWorkflowOrchestrator`、`step1IdentifyProblem()`、`CDPManager.updateCDP()`、legacy patientState/userAnswers storage 或 legacy Parsing/Dialog assets。

物理删除至少等待目标新路径 `WIRED + VERIFIED`、旧入口对目标 Consultation 不可达、回归通过、回滚窗口结束。

---

## 7. 结论

```text
U02 Legacy Impact Assessment = COMPLETE
Legacy fixed workflow = LEGACY_ONLY / BLOCK_NEW_USE FOR U02
Legacy direct CDP write = BLOCK_NEW_USE
Legacy Parsing/NLU low-level assets = ADAPT / REFACTOR BELOW C01 BOUNDARY
StateCommitter / Clinical CDP Adapter = KEEP + ADAPT
Foundation-1 governance/trace = REUSE
C01-U01 Mainline Predecessor = SATISFIED / MERGED AS PR #80
Physical Legacy Removal = NOT AUTHORIZED
U02 Implementation Authorization = GRANTED SEPARATELY BY USER
Merge Authorization = NOT_GRANTED
```
