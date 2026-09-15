# U03 C Missingness Policy v0.2

> Policy ID：`U03_C_MISSINGNESS_V0_2`  
> 状态：`RESOLVABLE_DRAFT / M2_REVISION_APPLIED / RE_REVIEW_REQUIRED / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 目的：把 C v0.2 中已经写入各 rule 的 missingness 语义变成可解析对象，供后续 candidate-freeze 审查引用。  
> 本文件不新增医学阈值、rule 或 D09 disposition。

## 1. Policy Vocabulary

输入状态：

```text
PRESENT
ABSENT
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
```

C 执行结果：

```text
MATCHED
NO_MATCH
INPUT_INSUFFICIENT
SCOPE_MISMATCH
```

rule-level signals：

```text
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

## 2. Core Invariants

```text
UNKNOWN != ABSENT
UNMEASURED != NORMAL
NOT_ASKED != ABSENT
AMBIGUOUS != NEGATIVE
CONFLICTING != NEGATIVE
REMOTE_NOT_OBSERVED != EXCLUDED
INVALID != NEGATIVE
```

只有在：

```text
scope satisfied
AND required inputs resolvable
AND predicate deterministically false
```

时，才允许 `NO_MATCH`。

## 3. Required Evidence Handling

若 required evidence 为：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
```

且该状态使 predicate 无法确定，则必须：

```text
execution = INPUT_INSUFFICIENT
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

不得把上述任一不可判定态解释为：

```text
ABSENT
NEGATIVE
NO_MATCH
```

若 evidence 明确、合法且 predicate false：

```text
execution = NO_MATCH
```

## 4. Required Measurement Handling

测量型输入如 RR / SBP / HR，只要 required measurement 为以下任一状态：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
INVALID
```

均必须：

```text
execution = INPUT_INSUFFICIENT
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

不得把以下情况解释成阈值明确未命中：

```text
未测
未问
值存在歧义
多个来源互相冲突
值无效
```

只有 measurement 已明确、有效、可解释，并且 scope 已满足、predicate 可确定为 false 时，才允许对应 rule 进入 `NO_MATCH`。

## 5. Optional Comparison Input

`usual_systolic_bp_mmHg` 只用于 SBP HIGH 的 relative-drop branch。

```text
current SBP <= 90
→ absolute branch MATCHED
→ usual SBP 不再是 required

current SBP > 90
AND usual SBP known + traceable
→ evaluate relative-drop branch

current SBP > 90
AND usual SBP in {UNKNOWN, UNMEASURED, NOT_ASKED, AMBIGUOUS, CONFLICTING, INVALID}
→ HIGH relative-drop branch = INPUT_INSUFFICIENT
→ signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

该 branch 的 insufficient 不得抑制其他独立 rule，例如 SBP 91..100 的 MODHIGH rule。

## 6. Scope Context Handling

若 required scope context：

```text
UNKNOWN / NOT_ESTABLISHED
OR provenance independence cannot be validated
```

则：

```text
execution = INPUT_INSUFFICIENT
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

若 scope context 明确为 FALSE / outside permitted scope：

```text
execution = SCOPE_MISMATCH
signal = RULE_SIGNAL_SCOPE_MISMATCH
```

scope-context provenance independence 详见：

```text
U03_C_BF_C_04_Closure_Amendment_v0.2.md
```

## 7. M2 Revision Summary

本次只关闭 `U03_C_Freeze_Policy_Review_Record_v0.2.md` 中 M2 technical finding：

```text
Required Evidence
+ UNMEASURED 显式进入 INPUT_INSUFFICIENT

Required Measurement
+ NOT_ASKED
+ AMBIGUOUS
+ CONFLICTING
显式进入 INPUT_INSUFFICIENT

Scope Context INPUT_INSUFFICIENT
+ 显式 signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

未修改：

```text
15 条 active rule predicate / threshold
SBP <=90 / drop>40 / MODHIGH coexistence
U03_SEPSIS_SHARED_SCOPE_V0_2
pre-freeze evaluation content
D09
```

## 8. Current Status

```text
policy_ref = U03_C_MISSINGNESS_V0_2
Resolvable Object = YES
Medical Review = COMPLETE_APPROVE
Technical Review v0.2 = REVISE_M2
M2 Revision = APPLIED
M2 Re-review = REQUIRED
Frozen = NO
Production Eligible = NO
```

在 M2 Technical re-review = APPROVE 前：

```text
U03_C_MISSINGNESS_V0_2 = NOT_FROZEN
U03_SEPSIS_SHARED_SCOPE_V0_2 = FREEZE_HELD
Policy Pair Freeze = BLOCKED
RR-U03-RISK-001 Candidate Freeze = BLOCKED
Pre-Freeze Eval Content = NOT_STARTED
D = BLOCKED
```
