# U03 C / D / E Cross-Consistency Review v0.1

> 对象：U03 Clinical Input Package 的 C Rule Release / D09 Policy / E Knowledge Release frozen candidates。  
> 状态：`REVIEW_COMPLETE / REVISE_REQUIRED / GATE_B_BLOCKED / NOT_FOR_PRODUCTION`  
> 审核类型：`U03_TECHNICAL_GOVERNANCE_CROSS_CONSISTENCY_REVIEW`。  
> 本记录检查冻结候选之间是否相互一致；不重新发明医学内容，不构成 Gate B PASS / Gate C PASS / Implementation Authorization。

---

## 1. Review Inputs

```text
A source-locked scope authority
= U03_Clinical_Risk_Semantics_Content_Draft_v0.2.md

E Knowledge Release
= KR-U03-SOURCE-001@0.1.0-candidate
= CANDIDATE_FROZEN

C Rule Release
= RR-U03-RISK-001@0.2.0-candidate
= CANDIDATE_FROZEN

D09 Policy Release
= PR-U03-D09-001@0.2.0-candidate
= CANDIDATE_FROZEN

C policy pair
= PF-U03-C-POLICY-001
= CANDIDATE_FROZEN

D coverage contract
= U03_D09_COVERAGE_V0_2
= CANDIDATE_FROZEN
```

CD-03 / CD-04 / CD-05 均已达到 initial candidate gate，但 Gate B 仍要求 C/D/E 作为一个闭合集合保持版本、scope、语义和 authority 一致。

---

## 2. Consistency Matrix

| ID | Dimension | Result | Finding |
|---|---|---|---|
| CDE-01 | Release identity / resolvability | PASS | C/D/E 均使用显式 immutable candidate ref；无 `latest` alias。 |
| CDE-02 | E → C binding | PASS | C 显式绑定 `KR-U03-SOURCE-001@0.1.0-candidate`。 |
| CDE-03 | C → D binding | PASS | D 显式绑定 `RR-U03-RISK-001@0.2.0-candidate`。 |
| CDE-04 | C signal → D disposition ownership | PASS | C 只输出 rule-level signals；D 才拥有 `HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL / FAILED`。 |
| CDE-05 | Missingness / scope-signal mapping | PASS | C `INPUT_INSUFFICIENT / SCOPE_MISMATCH` 与 D frozen coverage contract 的 `INSUFFICIENT_APPLICABLE / NOT_APPLICABLE` 映射一致。 |
| CDE-06 | Knowledge authority boundary | PASS | E 仅承载 source/version/scope/provenance；C 承载 executable rule；D 承载 disposition。 |
| CDE-07 | Region scope | PASS | C/D/E 均为 `INTERNATIONAL_REFERENCE_ONLY`，均未声称 China production localization。 |
| CDE-08 | Channel scope | PASS | 均限制于 remote/community initial consultation where source-supported。 |
| CDE-09 | Pediatrics | PASS | A/C/D/E 均明确排除儿科。 |
| CDE-10 | Pregnancy / puerperium scope | **REVISE** | A/E 是 whole-slice exclusion；C/D frozen candidate metadata 只显式写成 sepsis-specific exclusion，范围表达不闭合。 |
| CDE-11 | Gate C boundary | PASS | C/D pre-freeze eval 均未冒充完整 Clinical EvalSet / Gate C。 |
| CDE-12 | Runtime / production boundary | PASS | C/D/E candidates 均 NOT_PUBLISHED / NOT_FOR_PRODUCTION；无 runtime authorization。 |

```text
PASS = 11
REVISE = 1
blocking finding = 1
```

---

## 3. Blocking Finding — BF-CDE-01

### 3.1 Upstream authority

A v0.2 overall scope 明确：

```text
current U03 adult acute-symptom remote/community slice
NOT COVERED:
  pediatrics
  pregnancy / puerperium
  psychiatric emergency full coverage
  trauma full triage
  poisoning full triage
  ...
```

E / KR release scope 同样明确：

```text
exclusions:
  pediatrics
  pregnancy / puerperium
  ...
```

因此，在当前 source-locked slice 中：

```text
pregnancy / puerperium
= whole-slice exclusion
```

而不只是 NG253 sepsis family exclusion。

### 3.2 C frozen candidate metadata

当前 C candidate 写为：

```text
pregnancy_recent_pregnancy
= EXCLUDED_FOR_NG253_SEPSIS_RULES
```

这只明确了 sepsis-family exclusion，没有显式表达 A/E 的 whole-slice exclusion。

### 3.3 D frozen candidate metadata

当前 D candidate 写为：

```text
pregnancy/recent-pregnancy sepsis policy
= NOT_INCLUDED
```

同样只显式限定到 sepsis policy，而 D overall population scope 又以泛化的：

```text
Gate-A / C-candidate source-locked adult scope only
```

表达。

由于 C 本身对 pregnancy 的 metadata 已比 A/E 窄，D 不能依赖这一泛化引用证明 whole-slice exclusion 已唯一确定。

### 3.4 Why This Blocks Gate B

如果不修正，两个实现者可能合法地得出不同解释：

```text
Interpretation A
pregnancy / puerperium entirely outside current U03 D policy slice

Interpretation B
only sepsis family is excluded for pregnancy;
baseline RESP/NEURO/CARD/ALLERGY remain inside current policy denominator
```

这会直接影响：

```text
overall D policy scope
ALWAYS_APPLICABLE denominator
P-020 insufficiency behavior
P-040 NO_HIGH_RISK_SIGNAL eligibility
```

因此不是文案问题，而是 executable coverage scope ambiguity。

```text
BF-CDE-01 = OPEN / BLOCKING
```

---

## 4. Required Resolution Boundary

不得原地修改：

```text
RR-U03-RISK-001@0.2.0-candidate
PR-U03-D09-001@0.2.0-candidate
KR-U03-SOURCE-001@0.1.0-candidate
U03_D09_COVERAGE_V0_2
```

因为这些对象已经冻结。

修订必须：

1. 以 A/E 已批准 whole-slice scope 为 authority；
2. 不新增任何临床来源、阈值或 disposition；
3. 明确 pregnancy / puerperium 在当前 U03 slice 是 whole-policy exclusion 还是仅 sepsis-family exclusion；
4. 若维持 A/E 当前 authority，则创建新的 C/D（以及需要时 coverage contract）candidate version，使 metadata 和 denominator 明确继承 whole-slice exclusion；
5. 重新做受影响的 targeted review / freeze / cross-consistency；
6. 不得通过修改冻结 candidate 原文“修补历史”。

若 Medical Owner 希望改成“仅 sepsis-family exclusion”，则不是 metadata 修正，而是对 A/E 已批准 scope 的实质变更，必须回到上游 source/medical review 后再形成新版本。

---

## 5. Non-blocking Confirmed Invariants

以下保持一致：

```text
E != executable rule owner
C != disposition owner
D != U04 Safety Gate owner

RULE_SIGNAL_INPUT_INSUFFICIENT
!= NO_MATCH

RULE_SIGNAL_SCOPE_MISMATCH
= specialized-family NOT_APPLICABLE where governed by frozen coverage
!= OVERALL_POLICY_SCOPE_MISMATCH

FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE / NORMAL / no disease

P0 > P1 > P2 > P3 > P4 > P5
```

Release chain 也闭合：

```text
KR-U03-SOURCE-001@0.1.0-candidate
→ RR-U03-RISK-001@0.2.0-candidate
→ PR-U03-D09-001@0.2.0-candidate
```

---

## 6. Verdict

```text
C/D/E Cross-Consistency Review = COMPLETE
C/D/E Cross-Consistency = REVISE_REQUIRED
blocking finding = 1
BF-CDE-01 = OPEN

Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
Runtime = BLOCKED
Production = BLOCKED
```

下一步只能先关闭 BF-CDE-01，再重新做 C/D/E cross-consistency review；在此之前不得宣称 Gate B PASS。
