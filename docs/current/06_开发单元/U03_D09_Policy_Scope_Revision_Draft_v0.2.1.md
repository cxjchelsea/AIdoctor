# U03 D09 Policy Scope Revision Draft v0.2.1

> 目标版本：`PR-U03-D09-001@0.2.1-draft`  
> 来源：`PR-U03-D09-001@0.2.0-candidate`（保持 frozen / immutable）。  
> 状态：`TARGETED_SCOPE_REVISION_DRAFT / REVIEW_REQUIRED / NOT_FROZEN / NOT_FOR_PRODUCTION`  
> 依据：`BF-CDE-01`、A v0.2 whole-slice scope、`KR-U03-SOURCE-001@0.1.0-candidate` release scope。

---

## 1. Revision Boundary

本版本只修 D overall policy scope metadata，不修改任何 D09 branch 或 disposition 逻辑。

保持不变：

```text
6 branch identities = UNCHANGED
P0 > P1 > P2 > P3 > P4 > P5 = UNCHANGED
formal output vocabulary = UNCHANGED
reason-code semantics = UNCHANGED
HIGH / CAUTION / NO_HIGH_RISK_SIGNAL mapping = UNCHANGED
```

---

## 2. Corrected Overall Policy Scope

```text
population_scope = Gate-A / E-source-locked adult scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported

pediatrics = EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
pregnancy_puerperium = EXCLUDED_FROM_CURRENT_U03_WHOLE_SLICE
china_localized_production_policy = NOT_INCLUDED
```

因此 pregnancy / puerperium consultation 在当前 U03 slice 中属于：

```text
outside overall D policy scope
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
```

不得解释为：

```text
baseline 5 rules remain ALWAYS_APPLICABLE
while only sepsis family is excluded
```

---

## 3. Specialized-family Scope Remains Distinct

在 otherwise eligible current U03 whole-policy scope 内，specialized-family scope 仍保持：

```text
C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ not whole-policy failure
```

因此：

```text
whole-policy pregnancy/puerperium exclusion
!= specialized-family RULE_SIGNAL_SCOPE_MISMATCH
```

前者由 P0 overall scope 处理；后者仅用于条件 family applicability。

---

## 4. No Branch Change

明确：

```text
D09-P-001..P-090 = unchanged
P0 overall-scope responsibility = unchanged
P1 HIGH precedence = unchanged
P2 insufficiency fail-closed = unchanged
P3 CAUTION = unchanged
P4 coverage-complete NO_HIGH_RISK_SIGNAL = unchanged
P5 conflict fail-closed = unchanged
48 pre-freeze fixtures = historical evidence for 0.2.0 candidate, not automatically re-certified for 0.2.1
```

---

## 5. Versioning Boundary

```text
PR-U03-D09-001@0.2.0-candidate
= historical frozen candidate / immutable

PR-U03-D09-001@0.2.1-draft
= new targeted scope-alignment draft
```

不得就地修改 `0.2.0-candidate`。

---

## 6. Review Required

```text
D-SCOPE-R1
Does 0.2.1 faithfully express pregnancy/puerperium as whole-policy exclusion?

D-SCOPE-R2
Does pregnancy/puerperium route to P0 OVERALL_POLICY_SCOPE_MISMATCH rather than specialized-family NOT_APPLICABLE?

D-SCOPE-R3
Are all six branches / precedence / disposition mappings unchanged?
```

当前：

```text
Medical Review = PENDING
Technical Review = PENDING
Candidate identity = NOT_CREATED
Freeze = NOT_COMPLETE
```
