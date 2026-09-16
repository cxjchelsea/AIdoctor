# U03 C BF-C-04 Closure Amendment v0.2

> 对象：`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md` 的 package-level scope-context 独立性补丁。  
> 权威输入：`U03_C_Medical_Owner_ReReview_Record_v0.2.md`、`U03_C_Revision_Task_v0.2.md`。  
> 状态：`REVISION_APPLIED / PACKAGE_RECONFIRMATION_COMPLETE / BF-C-04_CLOSED / PACKAGE_APPROVED_FOR_CONTENT / NOT_FROZEN / D_STILL_BLOCKED`。  
> 本补丁不改写 15 条已 APPROVE active rule 的 predicate / threshold；只补充所有相关 rule 必须共同遵守的 scope-context provenance 约束。

---

## 1. BF-C-04

BF-C-04 的问题不是单条 rule 阈值错误，而是上下文存在家族循环风险：

```text
本 pack 的 rule/evidence
→ 反推 suspected_sepsis 或 NHS dyspnoea emergency context
→ 再用该 context 允许同一 pack 执行
```

这会让 scope gate 失去独立性。

因此，本补丁增加一个全局不变量：

```text
Scope Context
必须先于 RR-U03-RISK-001 本次执行独立成立
并具有独立 provenance
```

---

## 2. Context Independence Invariant

任何被 C Rule Pack 用作 scope precondition 的 context 必须满足：

```text
context.established_before_rule_pack_execution = TRUE
context.provenance_ref != null
context.provenance_source != RR-U03-RISK-001 current execution
context.provenance_source != any current rule result produced by this pack
```

禁止：

```text
current C rule hit
→ create/upgrade scope context
→ authorize current C pack execution
```

也禁止：

```text
current C accepted evidence alone
→ silently synthesize scope context
→ authorize a source-specific rule family
```

如果无法证明 context 在本次 pack 执行前已独立成立：

```text
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

如果已有受治理 context 明确判定不属于该 scope：

```text
→ RULE_SIGNAL_SCOPE_MISMATCH
```

---

## 3. `scope_context.suspected_sepsis`

`suspected_sepsis` 是执行前既存的受治理临床上下文，不属于本 Rule Pack 的输出。

必须满足：

```text
scope_context.suspected_sepsis.status ∈ {TRUE, FALSE, UNKNOWN}
scope_context.suspected_sepsis.context_ref = REQUIRED
scope_context.suspected_sepsis.provenance_ref = REQUIRED
scope_context.suspected_sepsis.established_before_rule_pack_execution = TRUE
```

### 3.1 明确禁止反推

以下任何当前输入、accepted evidence、measurement 或本 pack rule result，均不得单独或组合建立/升级 `suspected_sepsis`：

```text
respiratory_rate_bpm
systolic_bp_mmHg
usual_systolic_bp_mmHg
heart_rate_bpm
accepted_evidence(EV-VS-SEPSIS-001)
accepted_evidence(EV-VS-SEPSIS-002)
accepted_evidence(EV-VS-SEPSIS-003)
accepted_evidence(EV-RF-APPEAR-001)
accepted_evidence(EV-RF-SEPSIS-001)
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
```

特别保持：

```text
sepsis criterion
!= suspected_sepsis context creator
```

因此不仅禁止“同一生命体征自举”，也禁止 RR/SBP/HR/appearance/rash 家族相互交叉自举。

### 3.2 执行结果

```text
suspected_sepsis == TRUE
AND provenance independence validated
→ 允许继续检查其余 shared scope

suspected_sepsis == UNKNOWN / NOT_ESTABLISHED
OR provenance independence not demonstrable
→ RULE_SIGNAL_INPUT_INSUFFICIENT

suspected_sepsis == FALSE
→ RULE_SIGNAL_SCOPE_MISMATCH
```

---

## 4. `scope_context.nhs_dyspnoea_emergency_warning_context`

该 context 只表达 `SRC-NHS-DYSPNOEA` 对应 Shortness of breath 页的急诊警示上下文，不是任意气促主诉，也不是任意外观/意识异常。

必须满足：

```text
scope_context.nhs_dyspnoea_emergency_warning_context.context_ref = REQUIRED
scope_context.nhs_dyspnoea_emergency_warning_context.provenance_ref = REQUIRED
scope_context.nhs_dyspnoea_emergency_warning_context.established_before_rule_pack_execution = TRUE
```

禁止仅依靠：

```text
accepted_evidence(EV-RF-APPEAR-001)
accepted_evidence(EV-RF-NEURO-001)
C-RULE-DYSPNOEA-APPEAR-001 result
C-RULE-DYSPNOEA-CONFUSION-001 result
```

建立、补全或升级该 context。

也禁止：

```text
任意 dyspnoea complaint
→ 自动等同 NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT
```

执行语义：

```text
context == TRUE
AND provenance independence validated
→ 允许执行两个 NHS dyspnoea source-locked rule

context UNKNOWN / NOT_ESTABLISHED
OR provenance independence not demonstrable
→ RULE_SIGNAL_INPUT_INSUFFICIENT

context == FALSE
→ RULE_SIGNAL_SCOPE_MISMATCH
```

---

## 5. 对 v0.2 active rule 的影响

本补丁不改变 15 条 active rule 的：

```text
rule_id
required_evidence_refs
predicate / threshold
matched_signal
knowledge_release_ref
```

只把以下约束作为所有相关 rule 的强制前置：

```text
scope_context_ref must resolve
scope_context provenance must resolve
scope_context must predate current pack execution
scope_context provenance must be independent from current pack criteria/results
```

所以：

```text
15 active-rule medical verdicts = UNCHANGED
BF-C-04 = CLOSED
Package Approval = APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT
```

---

## 6. 与 D09 的边界

本补丁仍不定义：

```text
HIGH_RISK
CAUTION
NO_HIGH_RISK_SIGNAL
FAILED
```

也不定义 scope context 的上游创建算法。

C 只要求：

```text
context 必须作为已存在、可追溯、独立于本 pack 的输入被消费
```

谁建立 `suspected_sepsis` 或 NHS dyspnoea context，属于上游 Clinical State / Business Context governance，不得在当前 C Rule Pack 中反向发明。

---

## 7. 当前状态

```text
BF-C-01 = CLOSED
BF-C-02 = CLOSED
BF-C-03 = CLOSED
BF-C-04 = CLOSED

Active rule APPROVE = 15
Active rule REVISE = 0
C Package Approval = APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT

RR-U03-RISK-001@0.2.0-draft = NOT_FROZEN
D = STILL_BLOCKED
Gate B = NOT_PASSED
```

下一步是独立的 candidate-freeze readiness：审查并冻结 missingness / shared-scope policy，以及确认 evaluation refs 达到项目定义的 freeze 水位。在此之前不开始 D09。
