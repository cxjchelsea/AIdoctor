# U03 Safety-critical Risk Rule Pack — Content Draft v0.1

> 对象：Clinical Input Package C / Safety-critical Risk Rule Pack 初始临床内容草案。  
> 状态：`CLINICAL_CONTENT_DRAFT / REVIEW_COMPLETE / REVISION_REQUIRED / NOT_APPROVED / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 前置：Gate A = PASS；`KR-U03-SOURCE-001@0.1.0-candidate` = frozen resolvable candidate for C drafting。  
> 本文件只定义 C 层可执行 rule candidate；不形成 D09 disposition，不形成 U04 Safety Gate，不构成 Implementation Authorization。

---

## 1. Release Identity

```text
rule_release_id = RR-U03-RISK-001
rule_set_id = U03-SAFETY-CRITICAL-RISK
release_version = 0.1.0-draft
status = DRAFT
contract_version = U03_RULE_SCHEMA_V1
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
population_scope = Gate-A-approved adult / source-locked scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
```

当前 release 仅用于 clinical drafting / review；不得作为 production binding。

---

## 2. C 层输出词表

Rule Pack 只允许输出受治理的 rule-level signal，不直接输出：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
FAILED
```

初始 signal vocabulary：

```text
RULE_SIGNAL_CRITICAL_RED_FLAG
RULE_SIGNAL_MUST_NOT_MISS
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

其中任一 signal 均：

```text
Rule Signal != D09 Disposition
Rule Signal != Safety Gate Decision
```

---

## 3. 通用执行语义

所有 rule candidate 必须遵守：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
NOT_ASKED != NO
AMBIGUOUS != NEGATIVE
CONFLICTING != NEGATIVE
REMOTE_NOT_OBSERVED != EXCLUDED
```

如果 required input 为 `UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS / CONFLICTING`，不得把 rule 判成“明确不命中”；必须形成显式 insufficient/uncertain execution state，供 D09 后续确定性处理。

所有 rule 必须绑定：

```text
clinical_state_version
required_evidence_refs[]
source_reference_ids[]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
population / region / channel / clinical-context scope
```

---

## 4. 非数值高安全信号规则

### C-RULE-RESP-001 — 严重呼吸困难伴言语受限

```text
required_evidence_refs = [EV-RF-RESP-001]
predicate = accepted_evidence(EV-RF-RESP-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
source_reference_ids = [SRC-NHS-DYSPNOEA]
```

scope：仅使用 Gate A 已批准的成人一般急性远程/社区语义；远程未观察到严重表现不得作为阴性排除。

### C-RULE-NEURO-001 — 突发局灶无力或单侧/局灶感觉异常

```text
required_evidence_refs = [EV-MNM-NEURO-001]
predicate = accepted_evidence(EV-MNM-NEURO-001) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
source_reference_ids = [SRC-NICE-NEURO-NG127, SRC-NHS-STROKE]
```

scope：成人突发局灶神经异常；不得把慢性、双侧、非局灶感觉异常仅因“麻木”字样纳入。

### C-RULE-NEURO-002 — 突发言语或语言异常

```text
required_evidence_refs = [EV-MNM-NEURO-002]
predicate = accepted_evidence(EV-MNM-NEURO-002) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
source_reference_ids = [SRC-NICE-NEURO-NG127, SRC-NHS-STROKE]
```

症状短暂缓解仍保留历史阳性语义，不得自动转阴性。

### C-RULE-CARD-001 — 急性高危胸痛样表现

```text
required_evidence_refs = [EV-MNM-CARD-001]
predicate = accepted_evidence(EV-MNM-CARD-001) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
source_reference_ids = [SRC-NHS-CHEST]
```

本规则只消费 B 已冻结的现象层 evidence；不把“高危胸痛样表现”解释成已确诊 ACS/MI。

### C-RULE-ALLERGY-001 — 快速过敏反应伴 ABC 受损

```text
required_evidence_refs = [EV-RF-ALLERGY-001]
predicate = accepted_evidence(EV-RF-ALLERGY-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
source_reference_ids = [SRC-NICE-ANAPHYLAXIS-NG258]
```

仅限 Gate A 已批准的快速进展、涉及 airway / breathing / circulation 的严重全身过敏反应语义；皮肤表现不是绝对必需条件；不得扩展为一般轻型过敏。

---

## 5. Source-locked appearance / mental-state rules

### C-RULE-DYSPNOEA-APPEAR-001 — 严重呼吸困难警示上下文中的异常外观

```text
required_evidence_refs = [EV-RF-APPEAR-001]
precondition = clinical_context == NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT
predicate = accepted_evidence(EV-RF-APPEAR-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
source_reference_ids = [SRC-NHS-DYSPNOEA]
```

禁止脱离严重呼吸困难急诊警示上下文，把该来源单独泛化为跨病种全局 red flag。

### C-RULE-DYSPNOEA-CONFUSION-001 — 严重呼吸困难警示上下文中的突然意识混乱

```text
required_evidence_refs = [EV-RF-NEURO-001]
precondition = clinical_context == NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT
predicate = accepted_evidence(EV-RF-NEURO-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
source_reference_ids = [SRC-NHS-DYSPNOEA]
```

禁止与 NG253 拼接后推导为“任何急症 / 全局 RED_FLAG”。

---

## 6. NG253 成人疑似脓毒症数值规则

以下规则仅允许在：

```text
age >= 16
AND suspected_sepsis == TRUE
AND setting in source-supported community / custodial context
AND pregnancy/recent-pregnancy excluded from this slice
```

时执行。不得复制成一般人群全局生命体征阈值。

### C-RULE-SEPSIS-RR-HIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-001]
predicate = respiratory_rate_bpm >= 25
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-RR-MODHIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-001]
predicate = 21 <= respiratory_rate_bpm <= 24
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-SBP-HIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-002]
predicate = systolic_bp_mmHg <= 90 OR systolic_bp_drop_from_usual_mmHg > 40
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

相对基线下降只有在 `usual_systolic_bp` 可信、可追溯时才允许计算；未知 baseline 不得视为该分支不命中。

### C-RULE-SEPSIS-SBP-MODHIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-002]
predicate = 91 <= systolic_bp_mmHg <= 100
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-HR-HIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-003]
predicate = heart_rate_bpm > 130
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-HR-MODHIGH-001

```text
required_evidence_refs = [EV-VS-SEPSIS-003]
predicate = 91 <= heart_rate_bpm <= 130
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

本 slice 已排除 pregnancy/recent-pregnancy，不得套用孕期心率分层。

### C-RULE-SEPSIS-MENTAL-HIGH-001

```text
required_evidence_refs = [EV-RF-NEURO-001]
precondition = suspected_sepsis == TRUE AND age >= 16 AND source_supported_setting == TRUE
predicate = objective_altered_mental_state == PRESENT
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-APPEAR-HIGH-001

```text
required_evidence_refs = [EV-RF-APPEAR-001]
precondition = suspected_sepsis == TRUE AND age >= 16 AND source_supported_setting == TRUE
predicate = mottled_or_ashen_or_cyanotic_appearance == PRESENT
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

### C-RULE-SEPSIS-RASH-HIGH-001

```text
required_evidence_refs = [EV-RF-SEPSIS-001]
precondition = suspected_sepsis == TRUE AND age >= 16 AND source_supported_setting == TRUE
predicate = non_blanching_petechial_or_purpuric_rash == PRESENT
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
```

---

## 7. Explicitly excluded rule content

本 v0.1 不加入：

```text
new oxygen requirement / SpO2 rule
urine output rule
temperature rule
new-onset arrhythmia rule
immunosuppression / recent surgery risk-factor rules
pediatric sepsis rules
pregnancy/recent-pregnancy sepsis rules
China-localized emergency thresholds
any rule not already supported by Gate A approved B evidence definitions
```

原因：这些内容尚未进入当前 B Evidence Catalog 的已批准条目集合。C 不得自行新增第二套 evidence taxonomy。

---

## 8. Priority / conflict draft

本轮只冻结候选结构，不提前定义 D09 disposition precedence。

初始原则：

```text
multiple rule hits may coexist
no first-hit-wins
no file-order priority
no rule may suppress another rule by default
conflicting / insufficient inputs remain explicit
```

具体 rule-to-disposition priority / precedence 属于 D09，不在本 C draft 中定义。

---

## 9. Review checklist

Medical Owner review 至少需要逐条确认：

1. rule 是否只引用 Gate A 已批准 B evidence；
2. predicate / threshold 是否忠实于绑定 source；
3. population / setting / clinical-context scope 是否未被扩大；
4. missingness / unknown 是否未被错误转为 negative；
5. signal vocabulary 是否仍停留在 rule-level，不越权成 D09 disposition；
6. 是否存在应拆分、合并或删除的 rule；
7. 是否允许形成 initial candidate rule release。

允许 verdict：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

---

## 10. 当前状态

```text
C Structural Schema = FROZEN
C Clinical Rule Content Draft v0.1 = AVAILABLE
Initial Rule Release = RR-U03-RISK-001@0.1.0-draft
Knowledge Release Ref = KR-U03-SOURCE-001@0.1.0-candidate
Medical Owner Review = COMPLETE
Technical Review = COMPLETE
Medical APPROVE = 13 / REVISE = 3
Initial Rule Release Freeze = NOT_COMPLETE
CD-03 = NOT_PASSED

D D09 Clinical Policy Content = BLOCKED_UNTIL_C_RULE_RESULT_VOCABULARY_REVIEWED
Gate B = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
```

下一步：按 `U03_C_Revision_Task_v0.1.md` 产出 C Content Draft v0.2。在全部 blocking REVISE 消除并再审通过前，不进入 D09 真实 branch，不把 `RR-U03-RISK-001@0.1.0-draft` 用于 runtime 或 production binding。
