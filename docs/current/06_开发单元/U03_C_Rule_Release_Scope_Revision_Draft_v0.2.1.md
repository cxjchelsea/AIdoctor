# U03 C Rule Release Scope Revision Draft v0.2.1

> 目标版本：`RR-U03-RISK-001@0.2.1-draft`  
> 来源：`RR-U03-RISK-001@0.2.0-candidate`（保持 frozen / immutable）。  
> 状态：`TARGETED_SCOPE_REVISION_DRAFT / REVIEW_COMPLETE / APPROVED_FOR_CONTENT / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 再审记录：`U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md`  
> 依据：`BF-CDE-01`、A v0.2 whole-slice scope、`KR-U03-SOURCE-001@0.1.0-candidate` release scope。

---

## 1. Revision Boundary

本版本只修 C release scope metadata，不修改任何 active rule content。

保持不变：

```text
active_rule_count = 15
rule predicates = UNCHANGED
thresholds = UNCHANGED
rule-level signals = UNCHANGED
missingness policy = UNCHANGED
sepsis shared-scope policy = UNCHANGED
knowledge release ref = KR-U03-SOURCE-001@0.1.0-candidate
```

---

## 2. Corrected Candidate Scope

```text
population_scope = Gate-A-approved adult / source-locked scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported

pediatrics = EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
pregnancy_puerperium = EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
china_localized_production_pathway = NOT_INCLUDED
```

NG253 sepsis-specific rule family 继续额外受：

```text
U03_SEPSIS_SHARED_SCOPE_V0_2
```

约束，但该 family-level scope 不能被解释为对 whole-slice pregnancy/puerperium exclusion 的替代。

因此必须保持两层：

```text
whole C release scope:
  pregnancy / puerperium = OUTSIDE_CURRENT_U03_SLICE

inside otherwise eligible current slice:
  NG253 sepsis family still applies its frozen shared-scope contract
```

---

## 3. No Rule Change

明确：

```text
15 active rule identities = unchanged
15 active rule predicates = unchanged
RR/SBP/HR thresholds = unchanged
RULE_SIGNAL_* vocabulary = unchanged
BF-C-01..04 closures = unchanged
57 pre-freeze fixtures = historical evidence for 0.2.0 candidate, not automatically re-certified for 0.2.1
```

本修订没有新增 evidence/source/clinical conclusion。

---

## 4. Versioning Boundary

```text
RR-U03-RISK-001@0.2.0-candidate
= historical frozen candidate / immutable

RR-U03-RISK-001@0.2.1-draft
= new targeted scope-alignment draft
```

不得就地修改 `0.2.0-candidate`。

---

## 5. Review Required

仅需定向确认：

```text
C-SCOPE-R1
Does 0.2.1 metadata faithfully inherit A/E whole-slice pregnancy/puerperium exclusion?

C-SCOPE-R2
Does it preserve NG253 sepsis shared-scope as an additional family-level constraint, not the whole-slice owner?

C-SCOPE-R3
Are all 15 rule predicates/thresholds/signals unchanged?
```

正式裁决见 `U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md`。

当前：

```text
Medical Review = COMPLETE / APPROVE
Technical Review = COMPLETE / APPROVE
Candidate identity = NOT_CREATED
Freeze = NOT_COMPLETE
```
