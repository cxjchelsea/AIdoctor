# U03 Sepsis Shared Scope Policy v0.2

> Policy ID：`U03_SEPSIS_SHARED_SCOPE_V0_2`  
> 状态：`RESOLVABLE_DRAFT / REVIEW_REQUIRED / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 目的：把 C v0.2 所有 NG253 sepsis rule 共用的 scope precondition 固化为可解析对象。  
> 本文件不新增阈值，不定义 D09 disposition，也不创建 suspected sepsis。

## 1. Required Scope Inputs

```text
age
pregnancy_recent_pregnancy
setting
scope_context.suspected_sepsis
```

其中：

```text
scope_context.suspected_sepsis.context_ref = REQUIRED
scope_context.suspected_sepsis.provenance_ref = REQUIRED
scope_context.suspected_sepsis.established_before_rule_pack_execution = TRUE
```

## 2. Allowed Scope

只有同时满足：

```text
age >= 16
pregnancy_recent_pregnancy == FALSE
scope_context.suspected_sepsis == TRUE
setting in {SOURCE_SUPPORTED_COMMUNITY, SOURCE_SUPPORTED_CUSTODIAL}
suspected_sepsis provenance independence = VALID
```

才允许执行绑定本 policy 的 NG253 sepsis rule。

## 3. Independence Rule

`suspected_sepsis` 必须先于 `RR-U03-RISK-001` 当前执行存在，并且不得由当前 pack 的准则建立或升级。

禁止将以下任一项或组合用作当前 pack 内的 suspected-sepsis creator：

```text
respiratory_rate_bpm
systolic_bp_mmHg
usual_systolic_bp_mmHg
heart_rate_bpm
EV-VS-SEPSIS-001
EV-VS-SEPSIS-002
EV-VS-SEPSIS-003
EV-RF-APPEAR-001
EV-RF-SEPSIS-001
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
```

详细治理约束引用：

```text
U03_C_BF_C_04_Closure_Amendment_v0.2.md
```

## 4. Execution Outcomes

```text
age UNKNOWN
OR pregnancy status UNKNOWN
OR setting UNKNOWN
OR suspected_sepsis UNKNOWN / NOT_ESTABLISHED
OR suspected_sepsis provenance independence cannot be validated
→ RULE_SIGNAL_INPUT_INSUFFICIENT

age < 16
OR pregnancy/recent-pregnancy == TRUE
OR suspected_sepsis == FALSE
OR setting outside source-supported community/custodial
→ RULE_SIGNAL_SCOPE_MISMATCH

all scope inputs valid and within scope
→ evaluate rule predicate
```

## 5. Explicit Exclusions

```text
pediatrics
pregnancy / recent-pregnancy
acute-hospital NG253 pathway / NEWS2
China-localized production pathway
any scope expansion not re-reviewed through A/B/C governance
```

## 6. Current Status

```text
policy_ref = U03_SEPSIS_SHARED_SCOPE_V0_2
Resolvable Object = YES
Medical Review = NOT_COMPLETE
Technical Review = NOT_COMPLETE
Frozen = NO
Production Eligible = NO
```

该对象只解决 shared-scope ref 的可解析性，不代表 C package 或 Rule Release candidate 已冻结。