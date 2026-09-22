# U03 C / D / E Cross-Consistency Revision Task v0.1

> 权威输入：`U03_CDE_Cross_Consistency_Review_v0.1.md`  
> 目标：关闭 `BF-CDE-01`。  
> 状态：`REVISION_REQUIRED / GATE_B_BLOCKED / FROZEN_OBJECTS_IMMUTABLE / NOT_FOR_PRODUCTION`

---

## 1. Blocking Finding

```text
BF-CDE-01
= pregnancy / puerperium scope is inconsistent across frozen A/E authority and C/D candidate metadata
```

当前 authority：

```text
A v0.2 overall scope
→ pregnancy / puerperium NOT COVERED

E KR-U03-SOURCE-001@0.1.0-candidate
→ pregnancy / puerperium in release exclusions
```

当前 C/D metadata：

```text
C RR-U03-RISK-001@0.2.0-candidate
→ pregnancy_recent_pregnancy = EXCLUDED_FOR_NG253_SEPSIS_RULES

D PR-U03-D09-001@0.2.0-candidate
→ pregnancy/recent-pregnancy sepsis policy = NOT_INCLUDED
```

这不能唯一证明 whole-slice exclusion。

---

## 2. Default Resolution Path Under Current Approved Authority

如果不改变 A/E 已批准医学 scope，则目标语义必须统一为：

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

这意味着：

```text
D overall policy scope mismatch
→ D09-P-001
→ FAILED / NONE
→ OVERALL_POLICY_SCOPE_MISMATCH
```

而不是：

```text
baseline 5 rules remain ALWAYS_APPLICABLE for pregnancy
```

当前 C/D/coverage 的新版本 metadata 必须使该边界无歧义。

---

## 3. Required Versioning

禁止原地修改已冻结对象：

```text
RR-U03-RISK-001@0.2.0-candidate
PR-U03-D09-001@0.2.0-candidate
U03_D09_COVERAGE_V0_2
KR-U03-SOURCE-001@0.1.0-candidate
```

若沿用现有 A/E authority，至少需要形成新的：

```text
C rule release candidate version
D09 policy candidate version
D09 coverage contract version where denominator/scope contract representation changes
```

版本号由后续 versioning step 按仓库既有 release convention 确定；不得覆盖历史 frozen identity。

E 不需要因为“把下游纠正为与 E 一致”而新建版本；只有 E 自身医学 scope 被改变时才需要新版本。

---

## 4. Required Content Change Boundary

本修订只允许解决 scope consistency：

```text
whole-policy pregnancy / puerperium exclusion
C candidate scope metadata
D candidate overall policy scope metadata
coverage contract overall-scope entry condition
```

不得修改：

```text
15 C active rule predicates / thresholds
C rule-level signal vocabulary
P0 > P1 > P2 > P3 > P4 > P5
D branch clinical responsibilities
HIGH / CAUTION / NO_HIGH_RISK_SIGNAL mappings
E source registry
A/B evidence taxonomy
```

不得新增来源或医学结论。

---

## 5. Required Re-review

新版本至少需要 targeted review：

```text
C scope metadata review
D overall policy scope review
coverage denominator entry-condition review
C/D/E cross-consistency re-review
```

若只做上述 scope-alignment，已有 C 57 fixtures / D 48 fixtures 是否可以部分复用，必须在新版本 freeze readiness 中显式判断；不得自动继承 PASS。

---

## 6. Alternative Medical Change Path

如果 Medical Owner 明确希望：

```text
pregnancy / puerperium
= allowed for baseline RESP/NEURO/CARD/ALLERGY
and excluded only from NG253 sepsis family
```

则这与当前 A/E whole-slice exclusion 不一致，必须先回到：

```text
A scope medical review
E knowledge-release scope review
```

形成新的 approved upstream authority 后，才能再生成新的 C/D candidates。

不能把该选择当成“技术 metadata 修订”。

---

## 7. Completion Definition

```text
BF-CDE-01 CLOSED only when:

A/E/C/D/coverage scope expression = UNIQUE_AND_CONSISTENT
new affected candidate identities = independently versioned
required targeted Medical/Technical reviews = APPROVE
required freeze records = COMPLETE
C/D/E cross-consistency re-review = PASS
blocking finding = 0
```

在此之前：

```text
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
Runtime = BLOCKED
```
