# U03 C Missingness M2 Re-review Record v0.2

> 对象：`U03_C_MISSINGNESS_V0_2` 的 M2 定向再审。  
> 前置：`U03_C_Freeze_Policy_Review_Record_v0.2.md` 已给出 Medical APPROVE / Technical REVISE(M2)。  
> 修订输入：`U03_C_Missingness_Policy_Revision_Task_v0.2.md`。  
> 状态：`M2_RE_REVIEW_COMPLETE / MEDICAL_APPROVE / TECHNICAL_APPROVE / POLICY_PAIR_FREEZE_AUTHORIZED`。  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-15`  
> 本记录只审核 M2 映射完整性，不重审 15 条 active rule，不重审 Shared Scope，不开始 Eval 或 D09。

## 1. Re-review Scope

只检查以下三点：

```text
M2-1 Required Evidence
UNMEASURED 是否显式映射到 INPUT_INSUFFICIENT

M2-2 Required Measurement
NOT_ASKED / AMBIGUOUS / CONFLICTING
是否与 UNKNOWN / UNMEASURED / INVALID 一样显式映射到 INPUT_INSUFFICIENT

M2-3 Scope Context
INPUT_INSUFFICIENT 分支是否显式携带
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

对照基线：`65047f3d7cc82f775181c1370792c758952197ee`。

## 2. Revision Applied

当前 `U03_C_MISSINGNESS_V0_2` 已写入：

```text
required evidence:
UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS /
CONFLICTING / REMOTE_NOT_OBSERVED / INVALID
→ INPUT_INSUFFICIENT
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

```text
required measurement:
UNKNOWN / UNMEASURED / NOT_ASKED /
AMBIGUOUS / CONFLICTING / INVALID
→ INPUT_INSUFFICIENT
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

```text
required scope context:
UNKNOWN / NOT_ESTABLISHED
OR provenance independence cannot be validated
→ INPUT_INSUFFICIENT
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

SBP optional comparison branch 同步保持：

```text
current SBP <= 90
→ absolute HIGH branch 可独立 MATCHED

current SBP > 90
AND usual SBP 不可判定
→ HIGH relative-drop branch INPUT_INSUFFICIENT

独立 MODHIGH rule 不被抑制
```

usual SBP 不可判定态已与测量词表对齐为：

```text
UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS / CONFLICTING / INVALID
```

这是 M2 一致性补强，不是新的临床阈值。

## 3. Re-review Questions

| # | Question | Medical | Technical |
|---|---|---|---|
| M2-R1 | Required Evidence 是否显式包含 UNMEASURED → INPUT_INSUFFICIENT | APPROVE | APPROVE |
| M2-R2 | RR/SBP/HR 的 NOT_ASKED / AMBIGUOUS / CONFLICTING 是否显式 → INPUT_INSUFFICIENT | APPROVE | APPROVE |
| M2-R3 | INPUT_INSUFFICIENT 是否显式携带 RULE_SIGNAL_INPUT_INSUFFICIENT | APPROVE | APPROVE |
| M2-R4 | 修订是否未改变 15 条 active rule predicate / threshold | APPROVE | APPROVE |
| M2-R5 | 修订是否未改变 Shared Scope、未开始 Eval、未引入 D09 disposition | APPROVE | APPROVE |

```text
M2 Technical finding = CLOSED
Missingness Medical = APPROVE
Missingness Technical = APPROVE
```

第 3 / 4 / 5 / 6 节现在都把不可判定态写成可执行映射，而不再只靠第 2 节散文。`NO_MATCH` 仍只能在 scope 满足、输入可判定、predicate 明确为 false 时出现。

## 4. Freeze Rule

本记录得到：

```text
Medical = APPROVE
Technical = APPROVE
```

且既有 Shared Scope review 保持：

```text
Medical = APPROVE
Technical = APPROVE
```

因此两个 policy **一起**进入 freeze decision 已被授权。本记录不冻结 Rule Release，不开始 pre-freeze Eval。

冻结对象与边界见：

```text
U03_C_Policy_Pair_Freeze_Record_v0.2.md
```

## 5. Current Status

```text
Missingness M2 Revision = APPLIED
Missingness M2 Re-review = COMPLETE_APPROVE
Missingness Policy Freeze = AUTHORIZED_WITH_SHARED_SCOPE
Shared Scope Policy Freeze = AUTHORIZED_WITH_MISSINGNESS
Pre-Freeze Eval Content = NOT_STARTED
RR-U03-RISK-001@0.2.0-draft = NOT_FROZEN
D = STILL_BLOCKED
```
