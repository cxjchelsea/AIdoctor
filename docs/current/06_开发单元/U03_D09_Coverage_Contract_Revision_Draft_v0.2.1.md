# U03 D09 Coverage Contract Revision Draft v0.2.1

> 目标对象：`U03_D09_COVERAGE_V0_2_1_DRAFT`  
> 来源：`U03_D09_COVERAGE_V0_2`（保持 frozen / immutable）。  
> 状态：`TARGETED_SCOPE_REVISION_DRAFT / REVIEW_COMPLETE / APPROVED_FOR_CONTENT / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 再审记录：`U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md`  
> 依据：`BF-CDE-01`。

---

## 1. Revision Boundary

本版本只修 coverage contract 的 **overall-policy entry condition**，不修改 denominator 内部规则集合或 C signal 映射。

保持不变：

```text
ALWAYS_APPLICABLE = 5 baseline rules
NHS_DYSPNOEA_FAMILY = unchanged
NG253_SEPSIS_FAMILY = unchanged

C MATCHED / NO_MATCH
→ APPLICABLE_EVALUATED

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE

C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
```

---

## 2. Corrected Overall-scope Entry Condition

coverage contract 只有在当前 U03 whole-policy scope 成立时才进入 denominator resolution。

```text
overall_policy_scope_satisfied requires:
  adult source-locked current U03 slice
  region = INTERNATIONAL_REFERENCE_ONLY
  channel = remote/community initial consultation where source-supported
  pediatrics = FALSE
  pregnancy_or_puerperium = FALSE
```

若：

```text
pregnancy_or_puerperium = TRUE
```

则：

```text
coverage_contract = NOT_APPLICABLE_AT_POLICY_LEVEL
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
```

此时不得进入：

```text
ALWAYS_APPLICABLE baseline denominator
CONDITIONALLY_APPLICABLE family resolution
P-020 / P-040 denominator calculation
```

---

## 3. Family-level Scope Remains Separate

只有 overall policy scope 已成立后，才允许使用：

```text
RULE_SIGNAL_SCOPE_MISMATCH
→ specialized family NOT_APPLICABLE
```

因此：

```text
pregnancy/puerperium whole-policy exclusion
!= family-level RULE_SIGNAL_SCOPE_MISMATCH
```

---

## 4. No Denominator Change

```text
5 baseline rule identities = unchanged
2 conditional family identities = unchanged
sepsis 8 member rules = unchanged
dyspnoea 2 member rules = unchanged
P-020 semantics = unchanged
P-040 semantics = unchanged
```

现有 `U03_D09_COVERAGE_V0_2` freeze 历史不变。

---

## 5. Review Required

```text
COV-SCOPE-R1
Does the contract gate denominator construction on whole-policy pregnancy/puerperium exclusion?

COV-SCOPE-R2
Does it prevent the baseline five rules from entering denominator when whole-policy scope fails?

COV-SCOPE-R3
Are internal denominator membership and C-signal mappings unchanged?
```

正式裁决见 `U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md`。

当前：

```text
Medical Review = COMPLETE / APPROVE
Technical Review = COMPLETE / APPROVE
Freeze = NOT_COMPLETE
```
