# U03 C / D / E v0.2.1 Scope-entry Missingness Task

> 对象：0.2.1 whole-policy pregnancy/puerperium entry gate 的 missingness formalization。  
> 状态：`TARGETED_FREEZE_TASK / TECHNICAL_RECOMMENDATION_COMPLETE / MEDICAL_DECISION_PENDING / NOT_A_CONTENT_REOPEN / NOT_FOR_PRODUCTION`  
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

## 3. Technical Recommendation

Technical review recommends the following deterministic handling:

```text
pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
→ denominator NOT_CONSTRUCTED
```

Rationale:

```text
P2 = only after overall policy scope is established and an applicable governed rule/family is insufficient.
Scope-entry missingness occurs before denominator construction.
```

Therefore:

```text
UNKNOWN / NOT_ASKED / NOT_ESTABLISHED
!= P2 INSUFFICIENT_INFORMATION
!= OVERALL_POLICY_SCOPE_MISMATCH
!= RULE_SIGNAL_SCOPE_MISMATCH
```

Formal proposal and review records:

```text
U03_CDE_v0.2.1_Scope_Entry_Missingness_Decision_Draft.md
U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md
```

---

## 4. Review Questions

| ID | Decision | Medical | Technical |
|---|---|---|---|
| MISSING-SCOPE-R1 | UNKNOWN / NOT_ASKED / NOT_ESTABLISHED fail closed at whole-policy entry | PENDING | APPROVE |
| MISSING-SCOPE-R2 | remain P0 rather than P2 | PENDING | APPROVE |
| MISSING-SCOPE-R3 | use `OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | PENDING | APPROVE |
| MISSING-SCOPE-R4 | distinguish from TRUE → `OVERALL_POLICY_SCOPE_MISMATCH` | PENDING | APPROVE |

---

## 5. Targeted Evaluation Draft

Targeted fixture content has been prepared:

```text
U03_CDE_v0.2.1_Targeted_Scope_Evaluation_Fixtures_Draft.md
fixture_count = 6
```

Coverage includes:

```text
TRUE
FALSE
UNKNOWN
NOT_ASKED
NOT_ESTABLISHED
P0 scope-entry failure + historical HIGH precedence check
```

Current fixture status:

```text
Technical/Eval = APPROVE recommendation
Medical = PENDING
Targeted Eval PASS = NO
```

---

## 6. Forbidden Shortcuts

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

## 7. Exit Criteria

本 task 关闭至少需要：

```text
MISSING-SCOPE-R1 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R2 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R3 = Medical APPROVE / Technical APPROVE
MISSING-SCOPE-R4 = Medical APPROVE / Technical APPROVE
TGT-CDE-01..06 = Medical APPROVE / Technical-Eval APPROVE
blocking finding = 0
```

之后才能：

```text
close BLOCKER-FZ-CDE-021-01
create independent 0.2.1 candidate identities
freeze affected candidates
re-run C/D/E cross-consistency
```

---

## 8. Current Status

```text
BF-CDE-01 = CLOSED_FOR_CONTENT
BLOCKER-FZ-CDE-021-01 = OPEN
Technical Decision = COMPLETE / APPROVE_RECOMMENDATION
Medical Decision = PENDING
Targeted Scope Fixtures = CONTENT_AVAILABLE / REVIEW_PENDING
new 0.2.1 candidates = NOT_CREATED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
```
