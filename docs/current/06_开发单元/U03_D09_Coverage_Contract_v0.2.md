# U03 D09 Coverage Contract v0.2

> Contract ID：`U03_D09_COVERAGE_V0_2`  
> 对象：`PR-U03-D09-001@0.2.0-draft` 的 deterministic coverage denominator。  
> 状态：`APPROVED_FOR_CONTENT / RESOLVABLE / NOT_FROZEN / NOT_FOR_PRODUCTION`。  
> 权威输入：`RR-U03-RISK-001@0.2.0-candidate`、`PF-U03-C-POLICY-001`、`U03_D09_Clinical_Policy_Revision_Task_v0.1.md`。  
> 本文件只定义 D09 如何确定“哪些 C rule 属于本次 disposition completeness 分母”；不新增医学 rule、阈值、evidence 或 disposition。

---

## 1. Overall D Policy Scope

本 contract 仅在当前 D policy release 的整体 scope 成立时适用：

```text
population_scope = Gate-A / C-candidate source-locked adult scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
```

若 consultation 明确不属于上述整体 scope：

```text
coverage_contract = NOT_APPLICABLE_AT_POLICY_LEVEL
→ D09-P-001
→ FAILED
→ reason_code = OVERALL_POLICY_SCOPE_MISMATCH
```

这与 C rule family 的 `RULE_SIGNAL_SCOPE_MISMATCH` 完全不同。

---

## 2. ALWAYS_APPLICABLE Rules

当整体 D policy scope 成立时，以下 5 条 baseline rule 始终属于本次 coverage denominator：

```text
C-RULE-RESP-001
C-RULE-NEURO-001
C-RULE-NEURO-002
C-RULE-CARD-001
C-RULE-ALLERGY-001
```

其执行结果必须被 D09 completeness 统计消费。

允许状态：

```text
MATCHED
NO_MATCH
INPUT_INSUFFICIENT
```

这些 baseline rule 在整体 D scope 成立时不得通过 D 层任意解释被移出 denominator。

若其 required evidence/input 不可判定并由 C 输出：

```text
RULE_SIGNAL_INPUT_INSUFFICIENT
```

则：

```text
coverage_state = INSUFFICIENT_APPLICABLE
→ counts for D09-P-020
→ blocks D09-P-030
→ blocks D09-P-040
```

---

## 3. CONDITIONALLY_APPLICABLE — NHS_DYSPNOEA_FAMILY

Family ID：

```text
NHS_DYSPNOEA_FAMILY
```

Members：

```text
C-RULE-DYSPNOEA-APPEAR-001
C-RULE-DYSPNOEA-CONFUSION-001
```

D09 只消费冻结 C 执行结果：

```text
RULE_SIGNAL_SCOPE_MISMATCH
→ family coverage_state = NOT_APPLICABLE
→ exclude both family members from P-020 / P-040 denominator

RULE_SIGNAL_INPUT_INSUFFICIENT
→ family coverage_state = INSUFFICIENT_APPLICABLE
→ include family in P-020 completeness failure
→ blocks P-030 / P-040

MATCHED / NO_MATCH
→ family coverage_state = APPLICABLE_EVALUATED
→ include members in denominator
```

D09 不得自行重建 `nhs_dyspnoea_emergency_warning_context`。

---

## 4. CONDITIONALLY_APPLICABLE — NG253_SEPSIS_FAMILY

Family ID：

```text
NG253_SEPSIS_FAMILY
```

Members：

```text
C-RULE-SEPSIS-RR-HIGH-001
C-RULE-SEPSIS-RR-MODHIGH-001
C-RULE-SEPSIS-SBP-HIGH-001
C-RULE-SEPSIS-SBP-MODHIGH-001
C-RULE-SEPSIS-HR-HIGH-001
C-RULE-SEPSIS-HR-MODHIGH-001
C-RULE-SEPSIS-APPEAR-HIGH-001
C-RULE-SEPSIS-RASH-HIGH-001
```

该 family 的 scope 由冻结对象：

```text
U03_SEPSIS_SHARED_SCOPE_V0_2
```

控制。D09 不重新判断 age/pregnancy/setting/suspected_sepsis 临床语义，只消费 C 的冻结执行结果。

```text
RULE_SIGNAL_SCOPE_MISMATCH
→ family coverage_state = NOT_APPLICABLE
→ exclude 8 条 sepsis rule from P-020 / P-040 denominator

RULE_SIGNAL_INPUT_INSUFFICIENT
→ family coverage_state = INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 / P-040

MATCHED / NO_MATCH
→ family coverage_state = APPLICABLE_EVALUATED
→ include applicable results in denominator
```

若同一 family 内因 SBP branch coexistence 等原因同时存在：

```text
MATCHED + INPUT_INSUFFICIENT
```

则 coverage 仍记录 insufficiency；最终 disposition 按 D09 precedence 处理：P1 HIGH 可高于 P2，但 P3/P4 不得绕过 P2。

---

## 5. Coverage State Vocabulary

D09 coverage contract 只允许：

```text
APPLICABLE_EVALUATED
INSUFFICIENT_APPLICABLE
NOT_APPLICABLE
```

禁止引入新的 clinical disposition。

映射来源只能是冻结 C candidate 的执行结果：

```text
C MATCHED / NO_MATCH
→ APPLICABLE_EVALUATED

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE

C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
```

但：

```text
OVERALL_POLICY_SCOPE_MISMATCH
```

不是 C signal，也不进入 coverage denominator；它直接属于 D09-P-001。

---

## 6. P-020 Completeness Rule

`D09-P-020` 必须引用：

```text
coverage_contract_ref = U03_D09_COVERAGE_V0_2
```

并且仅当：

```text
no P0
AND no P1
AND exists coverage_state == INSUFFICIENT_APPLICABLE
```

时形成：

```text
FAILED + INSUFFICIENT_INFORMATION
```

`NOT_APPLICABLE` 不计入 P-020 insufficiency。

---

## 7. P-040 Denominator Rule

`D09-P-040` 必须引用：

```text
coverage_contract_ref = U03_D09_COVERAGE_V0_2
```

只有同时满足：

```text
overall D policy scope = satisfied
ALL ALWAYS_APPLICABLE rules = APPLICABLE_EVALUATED
ALL conditionally applicable families = either:
  APPLICABLE_EVALUATED
  OR NOT_APPLICABLE
NO coverage_state = INSUFFICIENT_APPLICABLE
NO HIGH-class signal
NO CAUTION-class signal
all denominator-included rule results = NO_MATCH
```

才允许：

```text
VALID + NO_HIGH_RISK_SIGNAL
```

因此：

```text
suspected_sepsis UNKNOWN
→ cannot become NOT_APPLICABLE
→ C INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ blocks P-040
```

同理，NHS dyspnoea context UNKNOWN / provenance independence 不可验证也阻断 P-040。

---

## 8. Frozen Dependency Boundary

本 contract 不修改：

```text
RR-U03-RISK-001@0.2.0-candidate
PF-U03-C-POLICY-001
U03_C_MISSINGNESS_V0_2
U03_SEPSIS_SHARED_SCOPE_V0_2
```

若 coverage contract 后续经审核冻结，任何变更必须新建 contract version，不得原地改变 denominator。

---

## 9. Current Status

```text
contract_ref = U03_D09_COVERAGE_V0_2
Resolvable Object = YES
Medical Review = COMPLETE / APPROVE
Technical Review = COMPLETE / APPROVE
Frozen = NO
Production Eligible = NO
```

内容再审已通过；这不等于 coverage contract 已冻结，也不等于 D policy candidate freeze / CD-05 / Gate B。
