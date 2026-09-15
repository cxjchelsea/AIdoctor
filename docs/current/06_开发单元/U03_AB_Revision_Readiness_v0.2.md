# U03 A/B Revision Readiness v0.2

> 角色：A/B Content Draft v0.2 修订前就绪检查。  
> 基线：`main@765fb9ca1178c47a6ecfc660bd650edb5bffaf8b`，工作分支 `prep/u03-clinical-dependency-completion`。  
> 状态：`REVISION_READY / MEDICAL_APPROVAL_NOT_COMPLETE / C_CONTENT_BLOCKED / NOT_FOR_PRODUCTION`

## 1. 输入检查

```text
A/B v0.1 Content Drafts = AVAILABLE
Source Review v0.1 = AVAILABLE
Medical Review Recommendation v0.1 = AVAILABLE_AS_HISTORICAL_REVIEW_INPUT
Medical Owner Review Record v0.1 = COMPLETE / AUTHORITATIVE_FOR_CURRENT_VERDICTS
Revision Task v0.2 = COMPLETE
Clinical Governance Status = CURRENT
Clinical Dependency Readiness = CURRENT
```

结论：`PASS`。

## 2. 当前有效裁决

```text
A: APPROVE 2 / REVISE 4 / REJECT 0 / NEED_MORE_SOURCE 0
B: APPROVE 4 / REVISE 6 / REJECT 0 / NEED_MORE_SOURCE 1
```

条目计数只以 `U03_AB_Medical_Owner_Review_Record_v0.1.md` 为准。
`U03_AB_Medical_Review_Recommendation_v0.1.md` 与 `U03_AB_Source_Review_v0.1.md` 只作为历史审稿与来源证据记录，不得覆盖 Medical Owner verdict。

结论：`PASS`。

## 3. v0.2 修订范围

允许：
- 仅产出 A/B Content Draft v0.2；
- 执行 Medical Owner 明确要求的病因中性化、scope 收窄、拆条与字段硬化；
- 对 APPROVE 条目补齐 UNKNOWN / UNMEASURED / NOT_ASKED / 远程未观察等缺失语义，不扩大临床 scope；
- 对 `EV-RF-NEURO-001` 保持 `NEED_MORE_SOURCE`，或在未补足通用来源前主动收窄适用上下文；
- 更新 revision/readiness/governance 状态为“v0.2 修订完成待再审”。

禁止：
- 新增 C Rule Pack 真实阈值、组合规则或风险分层；
- 新增 D09 生产 clinical policy；
- 新增 E 临床知识适用性批准；
- 新增 F 金标准病例或临床评价结论；
- 将任何条目标为 Medical Owner APPROVED / Production Eligible；
- 将成人范围扩展到儿科或孕产；
- 将 NICE/NHS 直接定义为中国生产规则；
- Runtime 消费任何 v0.2 draft。

结论：`PASS`。

## 4. 关键验收边界

v0.2 至少必须满足：

```text
A-RS-02 = split + mechanism-neutral
A-RS-03 = focal/unilateral sensory scope hardened
A-RS-04 = ischemic causation removed
A-RS-06 = suspected-sepsis/source scope locked; no A-layer COMBINATION_SIGNAL
EV-RF-CIRC-001 = mechanism-neutral + observation limitations hardened
EV-MNM-NEURO-001 = sudden focal/unilateral sensory semantics hardened
EV-MNM-CARD-001 = ischemic causation removed
EV-VS-SEPSIS-001/002/003 = suspected-sepsis scope only; no executable thresholds
EV-RF-SEPSIS-001 = naming aligned to suspected-sepsis scope
EV-RF-NEURO-001 = scope explicit; global promotion prohibited without additional source
UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS / CONFLICTING != negative/safe
```

结论：`PASS / ACCEPTANCE_DEFINED`。

## 5. Gate 判定

```text
Engineering Prerequisites = PASS
Revision Inputs = PASS
Medical Owner Verdicts = AVAILABLE
Revision Scope = FROZEN
Revision Acceptance Criteria = DEFINED
New Foundation = NOT_REQUIRED
A/B v0.2 Revision Readiness = PASS
```

因此：

```text
A/B Content Draft v0.2 Revision = ALLOWED
```

但同时保持：

```text
Medical Owner Approval = NOT_COMPLETE
Gate A = NOT_PASSED
C Rule Pack Clinical Content = BLOCKED
CD-07 Implementation Readiness = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

## 6. 下一步

只允许执行：

```text
U03_AB_Revision_Task_v0.2
↓
produce A/B Content Draft v0.2
↓
revision self-check against this readiness record
↓
Medical Owner re-review
```

本文件不构成新的 `Implementation Authorization`，也不构成 `Merge Authorization`。
