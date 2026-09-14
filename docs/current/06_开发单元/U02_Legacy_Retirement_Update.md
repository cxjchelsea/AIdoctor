# U02 Legacy Retirement Update

> Unit：U02 临床事实形成与版本提交  
> 分支：`impl/u02-clinical-fact-formation`  
> PR：#85  
> 目的：按 Brownfield SOP 在 U02 组件验证通过后，更新 legacy 资产处置状态。  
> 本文件不授权任何物理删除、Phase10 cutover、Production Authorization 或 Merge。

---

## 1. 总体结论

U02 已建立新的内部 component path：

```text
C01-U02 candidate
→ U02 Business Owner
→ typed K09 Proposal
→ P01 StateCommitter
→ Clinical State Version
→ minimum D05 hook
```

这意味着部分旧语义已经可以在**新 U02 路径中禁止继续使用**，但 legacy external route / fixed workflow 仍然存在且尚未完成迁移。因此：

```text
Legacy semantic replacement for new U02 path = PARTIAL / ENFORCED
Legacy physical retirement = NOT_READY
Physical deletion = NOT_AUTHORIZED
```

---

## 2. Asset-by-asset Update

| Legacy / Existing Asset | U02 后状态 | 新 U02 是否依赖 | 现在能否删除 | 后续 retirement 条件 |
|---|---|---:|---:|---|
| `DiagnosisOrchestrationService.continueDiagnosis()` | LEGACY_ONLY / BLOCK_NEW_U02_USE | NO | NO | U07 + Phase10 new route WIRED/VERIFIED，legacy-bound consultation 完成迁移 |
| `DiagnosisWorkflowOrchestrator.step1IdentifyProblem()` | LEGACY_ONLY / PARTIALLY_REPLACED | NO | NO | U02 parsing 已替代一部分，但 U05/U06 等旧混合职责仍需替代 |
| `DiagnosisWorkflowOrchestrator` fixed workflow | LEGACY_ONLY | NO | NO | 目标 Units 全部迁移并完成 external cutover |
| `CDPManager.updateCDP()` direct Map write | BLOCK_NEW_U02_USE | NO | NO | 全部相关 formal writes 迁移到 G2/P01，旧调用不可达后 |
| `patientState.userAnswers` / legacy Map facts | LEGACY_STORAGE_ONLY | NO as formal U02 fact source | NO | U02/U07/Phase10 migration 完成并验证历史兼容 |
| legacy `suggested_writes` / tool direct patch semantics | BLOCK_NEW_U02_USE | NO | 尚不物理删 | 新 capability consumers 全部禁止 direct-write 后统一清理 |
| legacy low-level vocabulary / concept recognition assets | RETAIN / ADAPTED BELOW C01 | YES, bounded reuse | NO | 无删除目标；按能力质量演进处理 |
| `StateCommitter` | RETAIN / EXTENDED | YES | NO | 正式 P01 foundation |
| `StatePatchBoundaryValidator` | RETAIN / U02-SOURCE-EXTENDED | YES | NO | 正式 P01 boundary |
| `ClinicalCdpStateRepositoryAdapter` | RETAIN | YES through P01 | NO | authoritative persistence adapter |
| Foundation-1 P06 / InvocationGuard | RETAIN / REUSED | YES | NO | governance foundation |
| Foundation-1 P05 trace baseline | RETAIN / U02-WIRED | YES | NO | governance/observability foundation |

---

## 3. 已完成的 semantic retirement

对新的 U02 component path，以下行为已经正式禁止，不再作为可接受实现方式：

```text
C01 Capability Result 直接写 Clinical State
C01 直接生成正式 K09 StateChangeProposal
通过 CDPManager.updateCDP 写新的 U02 Clinical Facts
把 legacy userAnswers Map 当正式 current Clinical Facts
UNKNOWN -> NO
UNMEASURED -> NORMAL
MODEL_INFERRED -> PATIENT_REPORTED
MODEL_INFERRED / RULE_DERIVED 冒充 patient fact
未知 D05 artifact type 自动生成 INVALIDATED 效果
绕过 Foundation-1 binding governance 调用 C01-U02
```

这属于 semantic retirement / BLOCK_NEW_USE，不等于旧代码物理删除。

---

## 4. 尚未满足的物理退休条件

以下条件至少仍未满足：

- external `/continue` 尚未正式切换到 Unit-driven Runtime；
- U07 Resume 尚未完成；
- legacy-bound consultation 仍可能经过旧 fixed workflow；
- U05/U06 等职责尚未从旧 `step1IdentifyProblem()` 完整拆出；
- Phase10 migration/cutover 尚未完成；
- rollback window / production observation 尚未建立；
- Production Authorization 未授予。

因此删除旧 Controller/Orchestration/CDP direct-write/storage 资产会破坏现有 legacy 业务路径，当前不允许。

---

## 5. U02 后准确 retirement 状态

```text
DiagnosisOrchestrationService.continueDiagnosis
= RETAIN / LEGACY_ONLY / BLOCK_NEW_U02_USE

DiagnosisWorkflowOrchestrator.step1IdentifyProblem
= RETAIN / LEGACY_ONLY / PARTIALLY_REPLACED

DiagnosisWorkflowOrchestrator fixed workflow
= RETAIN / LEGACY_ONLY

CDPManager.updateCDP
= RETAIN FOR LEGACY / BLOCK_NEW_U02_USE

legacy userAnswers Map formal-fact semantics
= RETIRED FOR NEW U02 PATH
= PHYSICAL STORAGE STILL PRESENT

legacy suggested_writes/direct-write semantics
= RETIRED FOR NEW U02 PATH
= PHYSICAL CODE MAY STILL EXIST FOR LEGACY

low-level parsing assets
= RETAIN / ADAPT BELOW C01 BOUNDARY

Physical Legacy Removal
= NOT_READY / NOT_AUTHORIZED
```

---

## 6. 下一次更新时机

至少在以下事件之一发生后重新更新 retirement 状态：

1. U07 Resume 完成并验证；
2. Phase10 `/continue` cutover 开始；
3. legacy consultation migration 完成；
4. 旧 direct-write 调用点被证明对目标业务不可达；
5. 用户另行授予 legacy physical removal authorization。

在此之前，不得因为 U02 component verification PASS 就删除 legacy 业务链。
