# U03 C / D / E v0.2.1 Scope-entry Missingness Task

> 对象：0.2.1 whole-policy pregnancy/puerperium entry gate 的 missingness formalization。  
> 状态：`TARGETED_FREEZE_TASK_COMPLETE / MEDICAL_APPROVED / TECHNICAL_APPROVED / BLOCKER_CLOSED / NOT_FOR_PRODUCTION`  
> 来源：`U03_CDE_v0.2.1_Evaluation_Reuse_Assessment.md`。  
> 本任务不重开 `BF-CDE-01`；只补 candidate freeze 前不可由实现者猜测的 formal handling。

---

## 1. Approved Invariants

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

Confirmed TRUE:

```text
pregnancy_or_puerperium = TRUE
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
→ coverage NOT_APPLICABLE_AT_POLICY_LEVEL
→ denominator NOT_CONSTRUCTED
```

Confirmed FALSE:

```text
pregnancy_or_puerperium = FALSE
+ other overall-scope predicates satisfied
→ may enter coverage denominator construction
```

Missing/unestablished states:

```text
pregnancy_or_puerperium ∈ {UNKNOWN, NOT_ASKED, NOT_ESTABLISHED}
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
→ denominator NOT_CONSTRUCTED
```

---

## 2. Formal Distinctions

```text
OVERALL_POLICY_SCOPE_MISMATCH
= confirmed outside whole-policy scope

OVERALL_POLICY_SCOPE_NOT_ESTABLISHED
= whole-policy scope cannot be established because required scope fact is unavailable/not asked/not established

RULE_SIGNAL_SCOPE_MISMATCH
= specialized-family non-applicability only after overall policy scope is established
```

Therefore:

```text
UNKNOWN != FALSE
NOT_ASKED != FALSE
NOT_ESTABLISHED != FALSE
```

and these states must not enter denominator/P2/P3/P4 semantics.

---

## 3. Review Results

| ID | Decision | Medical | Technical |
|---|---|---|---|
| MISSING-SCOPE-R1 | UNKNOWN / NOT_ASKED / NOT_ESTABLISHED fail closed at whole-policy entry | APPROVE | APPROVE |
| MISSING-SCOPE-R2 | remain P0 rather than P2 | APPROVE | APPROVE |
| MISSING-SCOPE-R3 | use `OVERALL_POLICY_SCOPE_NOT_ESTABLISHED` | APPROVE | APPROVE |
| MISSING-SCOPE-R4 | distinguish from TRUE → `OVERALL_POLICY_SCOPE_MISMATCH` | APPROVE | APPROVE |

Review record:

```text
U03_CDE_v0.2.1_Scope_Entry_Missingness_Review_Record.md
```

---

## 4. Targeted Evaluation

```text
fixture_pack = U03_CDE_v0.2.1_Targeted_Scope_Evaluation_Fixtures_Draft.md
fixture_count = 6
Medical = APPROVE
Technical/Eval = APPROVE
blocking finding = 0
Targeted Eval PASS = YES
```

Historical evidence reuse remains:

```text
C57 = reusable for unchanged C semantics
D48 = reusable for unchanged D semantics
full rebuild = NOT_REQUIRED
```

---

## 5. Closure

Exit criteria are fully satisfied:

```text
MISSING-SCOPE-R1..R4 = APPROVE / APPROVE
TGT-CDE-01..06 = APPROVE / APPROVE
blocking finding = 0
```

Therefore:

```text
BF-CDE-01 = CLOSED_FOR_CONTENT
BLOCKER-FZ-CDE-021-01 = CLOSED
Scope-entry Missingness Decision = COMPLETE
Targeted Scope Eval = PASS
```

---

## 6. Current Status

```text
new 0.2.1 candidate identities = NOT_CREATED
new 0.2.1 freeze = NOT_COMPLETE
C/D/E cross-consistency re-review = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
```

Next allowed step is independent 0.2.1 candidate identity creation. Candidate creation is not freeze and does not change Gate B.
