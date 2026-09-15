# U03 C Missingness Policy v0.2

> Policy ID：`U03_C_MISSINGNESS_V0_2`  
> 状态：`RESOLVABLE_DRAFT / REVIEW_COMPLETE / TECHNICAL_REVISE / NOT_FROZEN / NOT_FOR_PRODUCTION`  
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
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
```

且该状态使 predicate 无法确定：

```text
execution = INPUT_INSUFFICIENT
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

若 evidence 明确、合法且 predicate false：

```text
execution = NO_MATCH
```

## 4. Required Measurement Handling

测量型输入如 RR / SBP / HR：

```text
UNMEASURED / UNKNOWN / INVALID
→ INPUT_INSUFFICIENT
```

不得把“未测”视为未命中阈值。

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
AND usual SBP UNKNOWN / untraceable
→ HIGH relative-drop branch = INPUT_INSUFFICIENT
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

## 7. Current Status

```text
policy_ref = U03_C_MISSINGNESS_V0_2
Resolvable Object = YES
Medical Review = COMPLETE_APPROVE
Technical Review = COMPLETE_REVISE
Frozen = NO
Production Eligible = NO
```

该对象存在只解决“ref 可解析”问题，不自动使 C package 或 candidate freeze 通过。