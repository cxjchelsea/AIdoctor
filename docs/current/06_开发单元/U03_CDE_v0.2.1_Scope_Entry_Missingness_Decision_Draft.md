# U03 C / D / E v0.2.1 Scope-entry Missingness Decision Draft

> 对象：0.2.1 whole-policy pregnancy/puerperium entry gate 的 missingness formal handling。  
> 状态：`DECISION_DRAFT / TECHNICAL_RECOMMENDATION_COMPLETE / MEDICAL_REVIEW_REQUIRED / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 来源：`U03_CDE_v0.2.1_Scope_Entry_Missingness_Task.md`。  
> 本文件不重开 `BF-CDE-01`，不新增临床 branch；只补充 P0 内 scope-entry missingness 的确定性 reason-code 语义。

---

## 1. Existing Approved Boundary

已批准：

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

并保持：

```text
pregnancy_or_puerperium = TRUE
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
→ coverage NOT_APPLICABLE_AT_POLICY_LEVEL
→ denominator NOT_CONSTRUCTED
```

只有：

```text
pregnancy_or_puerperium = FALSE
```

且其他 overall-scope predicate 均成立时，才允许进入 coverage denominator construction。

---

## 2. Missing-state Formalization

以下状态不得解释为 FALSE：

```text
UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
```

也不得解释为已确认 scope mismatch。

因此建议正式定义：

```text
pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}
→ D09-P-001
→ result_status = FAILED
→ disposition = NONE
→ reason_code = OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
→ failure_behavior = FAIL_CLOSED
```

并且：

```text
coverage_contract = NOT_ENTERED
baseline denominator = NOT_CONSTRUCTED
conditional family resolution = NOT_STARTED
P2 = NOT_ELIGIBLE
P3 = NOT_ELIGIBLE
P4 = NOT_ELIGIBLE
```

---

## 3. Why This Remains P0

P2 的语义保持不变：

```text
P2 = only after overall policy scope is established
     AND an applicable governed rule/family is insufficient
```

因此 scope applicability 本身尚未建立时：

```text
UNKNOWN / NOT_ASKED / NOT_ESTABLISHED
!= INSUFFICIENT_APPLICABLE
!= P2
```

而应在 denominator 建立前 fail closed 于 P0。

---

## 4. Reason-code Separation

必须保持三个不同层级：

```text
OVERALL_POLICY_SCOPE_MISMATCH
= 已明确知道 consultation 不属于 whole-policy scope
= 例如 pregnancy_or_puerperium = TRUE

OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
= whole-policy scope 所需字段无法确定
= UNKNOWN / NOT_ASKED / NOT_ESTABLISHED

RULE_SIGNAL_SCOPE_MISMATCH
= C specialized-family non-applicability
= only after overall policy scope already holds
```

因此：

```text
OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
!= OVERALL_POLICY_SCOPE_MISMATCH
!= RULE_SIGNAL_SCOPE_MISMATCH
```

---

## 5. No Branch / Precedence Change

本修订不新增第七个 branch。

仍保持：

```text
D09-P-001 = P0_INTEGRITY_OR_OVERALL_SCOPE_FAILURE
P0 > P1 > P2 > P3 > P4 > P5
```

只扩展 P0 reason-code vocabulary：

```text
+ OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
```

其余 D branch / disposition / precedence 不变。

---

## 6. Deterministic State Table

| pregnancy_or_puerperium | Overall-scope handling | D09 | Coverage |
|---|---|---|---|
| `TRUE` | confirmed outside scope | `P0 / FAILED / OVERALL_POLICY_SCOPE_MISMATCH` | do not construct denominator |
| `FALSE` | pregnancy/puerperium predicate satisfied | continue checking other overall-scope predicates | denominator allowed only if all other scope predicates pass |
| `UNKNOWN` | scope not established | `P0 / FAILED / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | do not construct denominator |
| `NOT_ASKED` | scope not established | `P0 / FAILED / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | do not construct denominator |
| `NOT_ESTABLISHED` | scope not established | `P0 / FAILED / OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | do not construct denominator |

---

## 7. Review Questions

```text
MISSING-SCOPE-R1
Do UNKNOWN / NOT_ASKED / NOT_ESTABLISHED fail closed at whole-policy entry?

MISSING-SCOPE-R2
Do they remain in P0 rather than P2?

MISSING-SCOPE-R3
Is OVERALL_POLICY_SCOPE_NOT_ESTABLISHED the formal reason code?

MISSING-SCOPE-R4
Is it explicitly distinct from TRUE → OVERALL_POLICY_SCOPE_MISMATCH?
```

Current proposed verdict:

```text
Technical = APPROVE_RECOMMENDATION
Medical = PENDING
```

---

## 8. Boundary

This decision draft does not mean:

```text
BLOCKER-FZ-CDE-021-01 closed
new 0.2.1 candidate created
new candidate frozen
Gate B passed
Gate C passed
runtime authorized
```

Medical + Technical approval is required before freeze use.