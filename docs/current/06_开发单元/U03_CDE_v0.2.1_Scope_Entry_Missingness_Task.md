# U03 C / D / E v0.2.1 Scope-entry Missingness Task

> 对象：0.2.1 whole-policy pregnancy/puerperium entry gate 的 missingness formalization。  
> 状态：`TARGETED_FREEZE_TASK / DECISION_REQUIRED / NOT_A_CONTENT_REOPEN / NOT_FOR_PRODUCTION`  
> 来源：`U03_CDE_v0.2.1_Evaluation_Reuse_Assessment.md`。  
> 本任务不重开 `BF-CDE-01`；只补 candidate freeze 前不可由实现者猜测的 formal handling。

---

## 1. 已冻结/已批准的不变量

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

已批准：

```text
pregnancy_or_puerperium = TRUE
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
→ coverage NOT_APPLICABLE_AT_POLICY_LEVEL
→ denominator NOT_CONSTRUCTED
```

并且：

```text
pregnancy_or_puerperium = FALSE
+ other overall-scope predicates satisfied
→ may enter coverage denominator construction
```

---

## 2. Missing Formal States

以下状态不得被解释为 FALSE：

```text
UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
```

必须保持：

```text
UNKNOWN != FALSE
NOT_ASKED != FALSE
NOT_ESTABLISHED != FALSE
```

因此这些状态：

```text
must NOT enter baseline denominator
must NOT resolve conditional families
must NOT produce P3 CAUTION
must NOT produce P4 NO_HIGH_RISK_SIGNAL
```

---

## 3. Decision Needed

需要 Medical + Technical 明确唯一 formal handling。

最低决策必须回答：

```text
MISSING-SCOPE-R1
UNKNOWN / NOT_ASKED / NOT_ESTABLISHED 是否都在 whole-policy entry gate fail closed？

MISSING-SCOPE-R2
其 D09 branch 是否保持 P0，而不是 P2？

MISSING-SCOPE-R3
formal reason_code 使用哪个既有/新增代码？

MISSING-SCOPE-R4
是否明确与 TRUE 的 OVERALL_POLICY_SCOPE_MISMATCH 区分？
```

建议的技术边界：

```text
P2 = only after overall policy scope is established and an applicable governed rule/family is insufficient
```

因此 scope applicability 尚未建立时不应进入 P2 denominator semantics。

这只是 Technical recommendation，不构成 Medical approval，也不自动决定 reason_code。

---

## 4. Forbidden Shortcuts

不得：

```text
UNKNOWN → FALSE
NOT_ASKED → FALSE
NOT_ESTABLISHED → FALSE

or

UNKNOWN/NOT_ASKED/NOT_ESTABLISHED
→ specialized-family NOT_APPLICABLE
```

也不得为了避免新增 formal handling 而直接让 baseline 5 条进入 denominator。

---

## 5. Exit Criteria

本 task 关闭至少需要：

```text
MISSING-SCOPE-R1 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R2 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R3 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R4 = Medical APPROVE / Technical APPROVE
blocking finding = 0
```

之后才能生成完整 targeted scope fixtures，至少覆盖：

```text
TRUE
FALSE
UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
```

---

## 6. Current Status

```text
BF-CDE-01 = CLOSED_FOR_CONTENT
BLOCKER-FZ-CDE-021-01 = OPEN
Scope-entry Missingness Decision = NOT_COMPLETE
Targeted Scope Fixtures = NOT_COMPLETE
new 0.2.1 candidates = NOT_CREATED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
```
