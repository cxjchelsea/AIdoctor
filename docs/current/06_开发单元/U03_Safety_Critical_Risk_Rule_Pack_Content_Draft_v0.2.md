# U03 Safety-critical Risk Rule Pack — Content Draft v0.2

> 对象：Clinical Input Package C / Safety-critical Risk Rule Pack 修订临床内容草案。  
> 权威输入：`U03_C_Medical_Owner_Review_Record_v0.1.md`、`U03_C_Revision_Task_v0.1.md`。  
> 状态：`REVISION_DRAFT_AVAILABLE / MEDICAL_TECHNICAL_RE_REVIEW_REQUIRED / NOT_APPROVED / NOT_FROZEN / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 前置：Gate A = PASS；`KR-U03-SOURCE-001@0.1.0-candidate` = frozen resolvable candidate for C drafting。  
> 本文件只定义 C 层 executable rule candidate；不形成 D09 disposition，不形成 U04 Safety Gate，不构成 Implementation Authorization。

---

## 1. Release Identity

```text
rule_release_id = RR-U03-RISK-001
rule_set_id = U03-SAFETY-CRITICAL-RISK
release_version = 0.2.0-draft
status = DRAFT
contract_version = U03_RULE_SCHEMA_V1
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
population_scope = Gate-A-approved adult / source-locked scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
```

`0.2.0-draft` 仅用于再审；不得作为 runtime 或 production binding。

---

## 2. Rule-level Signal Vocabulary

允许：

```text
RULE_SIGNAL_CRITICAL_RED_FLAG
RULE_SIGNAL_MUST_NOT_MISS
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

禁止 C 输出：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
FAILED
SAFE
NORMAL
```

必须保持：

```text
Rule Signal != D09 Disposition
Rule Signal != Safety Gate Decision
```

---

## 3. v0.2 通用执行模型

### 3.1 三态执行结果

每一条 active rule 必须产生且只产生以下执行类别之一：

```text
MATCHED
→ matched_signal = 本 rule 定义的 rule-level signal

INPUT_INSUFFICIENT
→ signal = RULE_SIGNAL_INPUT_INSUFFICIENT

SCOPE_MISMATCH
→ signal = RULE_SIGNAL_SCOPE_MISMATCH
```

只有在 scope 已满足、required inputs 均可判定且 predicate 明确为 false 时，才允许：

```text
NO_MATCH
```

`NO_MATCH` 不是 rule signal，更不是低风险 disposition。

### 3.2 Missingness

以下状态不得转为 `NO_MATCH`：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
```

若它们影响 required evidence / measurement / required scope context 的可判定性：

```text
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

### 3.3 Governed scope context

本 draft 使用两类 scope context；它们不是新的 B evidence taxonomy：

```text
scope_context.suspected_sepsis
scope_context.nhs_dyspnoea_emergency_warning_context
```

二者都必须是 execution 前已存在的受治理上下文，携带 `context_ref + provenance_ref`。

#### suspected_sepsis

```text
scope_context.suspected_sepsis == TRUE
```

必须在当前生命体征 rule 执行前由上游临床上下文建立；**不得仅由正在被本 rule 判定的同一 RR / SBP / HR 值反推 suspected_sepsis**。

若该上下文为 UNKNOWN / NOT_ESTABLISHED：

```text
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

若明确为 FALSE：

```text
→ RULE_SIGNAL_SCOPE_MISMATCH
```

#### NHS dyspnoea emergency warning context

```text
scope_context.nhs_dyspnoea_emergency_warning_context == TRUE
```

只表示 NHS Shortness of breath 页的急诊警示上下文，不等于“任意气促主诉”。必须有 `SRC-NHS-DYSPNOEA` 对应的 context/provenance ref。

UNKNOWN / NOT_ESTABLISHED：

```text
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

明确不处于该上下文：

```text
→ RULE_SIGNAL_SCOPE_MISMATCH
```

### 3.4 Sepsis shared scope precondition

每一条 sepsis rule 自身均重复并绑定：

```text
age >= 16
AND pregnancy_recent_pregnancy == FALSE
AND scope_context.suspected_sepsis == TRUE
AND setting in {SOURCE_SUPPORTED_COMMUNITY, SOURCE_SUPPORTED_CUSTODIAL}
```

执行语义：

```text
age UNKNOWN
or pregnancy status UNKNOWN
or setting UNKNOWN
or suspected_sepsis UNKNOWN
→ RULE_SIGNAL_INPUT_INSUFFICIENT

age < 16
or pregnancy/recent-pregnancy == TRUE
or suspected_sepsis == FALSE
or setting outside source-supported community/custodial
→ RULE_SIGNAL_SCOPE_MISMATCH
```

不得复制为一般人群全局生命体征规则。

---

## 4. 非数值高安全信号规则

以下 5 条规则只消费 Gate A 已批准 B evidence。

### C-RULE-RESP-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-RESP-001]
source_reference_ids = [SRC-NHS-DYSPNOEA]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = adult Gate-A source-locked remote/community context
predicate = accepted_evidence(EV-RF-RESP-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

required evidence 为 UNKNOWN / NOT_ASKED / AMBIGUOUS / CONFLICTING / REMOTE_NOT_OBSERVED 时输出 `INPUT_INSUFFICIENT`；明确不属于 Gate A 成人 scope 时输出 `SCOPE_MISMATCH`。

### C-RULE-NEURO-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-MNM-NEURO-001]
source_reference_ids = [SRC-NICE-NEURO-NG127, SRC-NHS-STROKE]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = adult sudden focal/unilateral neurological context
predicate = accepted_evidence(EV-MNM-NEURO-001) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

慢性、双侧或非局灶感觉异常不因“麻木”字样进入本 rule；若 onset/side/focality/temporality 无法判定，按 B required-field 语义进入 `INPUT_INSUFFICIENT`。

### C-RULE-NEURO-002

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-MNM-NEURO-002]
source_reference_ids = [SRC-NICE-NEURO-NG127, SRC-NHS-STROKE]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = adult sudden speech/language abnormality context
predicate = accepted_evidence(EV-MNM-NEURO-002) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

短暂缓解仍保留历史阳性，不能转为 negative。

### C-RULE-CARD-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-MNM-CARD-001]
source_reference_ids = [SRC-NHS-CHEST]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = Gate-A-approved adult acute high-risk chest-pain-like presentation
predicate = accepted_evidence(EV-MNM-CARD-001) == PRESENT
matched_signal = RULE_SIGNAL_MUST_NOT_MISS
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

只消费现象层 evidence；不等于已确诊 ACS / MI。

### C-RULE-ALLERGY-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-ALLERGY-001]
source_reference_ids = [SRC-NICE-ANAPHYLAXIS-NG258]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = rapid systemic allergic reaction with airway/breathing/circulation involvement
predicate = accepted_evidence(EV-RF-ALLERGY-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

皮肤表现不是必需；一般轻型过敏不进入本 rule。

---

## 5. NHS dyspnoea source-locked rules

### C-RULE-DYSPNOEA-APPEAR-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-APPEAR-001]
source_reference_ids = [SRC-NHS-DYSPNOEA]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = scope_context.nhs_dyspnoea_emergency_warning_context == TRUE
predicate = accepted_evidence(EV-RF-APPEAR-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

不得脱离 NHS 严重呼吸困难急诊警示上下文升格为跨病种全局 red flag。

### C-RULE-DYSPNOEA-CONFUSION-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-NEURO-001]
source_reference_ids = [SRC-NHS-DYSPNOEA]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = scope_context.nhs_dyspnoea_emergency_warning_context == TRUE
predicate = accepted_evidence(EV-RF-NEURO-001) == PRESENT
matched_signal = RULE_SIGNAL_CRITICAL_RED_FLAG
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

不得与 NG253 拼接推导为“任何急症 / 全局 RED_FLAG”。

---

## 6. NG253 成人疑似脓毒症数值规则

以下 6 条 active numeric rule 每条都显式应用 §3.4 shared scope precondition。

### C-RULE-SEPSIS-RR-HIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-001]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = respiratory_rate_bpm
predicate = respiratory_rate_bpm >= 25
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

RR UNKNOWN / UNMEASURED / invalid measurement → `INPUT_INSUFFICIENT`。

### C-RULE-SEPSIS-RR-MODHIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-001]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = respiratory_rate_bpm
predicate = 21 <= respiratory_rate_bpm <= 24
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

### C-RULE-SEPSIS-SBP-HIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-002]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = systolic_bp_mmHg
optional_comparison_input = usual_systolic_bp_mmHg WITH provenance
absolute_branch = systolic_bp_mmHg <= 90
relative_drop_branch = usual_systolic_bp_mmHg - systolic_bp_mmHg > 40
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

执行顺序：

```text
SBP UNKNOWN / UNMEASURED / invalid
→ INPUT_INSUFFICIENT

SBP known <= 90
→ MATCHED / SEPSIS_HIGH_RISK_CRITERION
  （不要求 usual SBP）

SBP known > 90 AND usual_systolic_bp known + traceable
→ evaluate relative_drop_branch

SBP known > 90 AND usual_systolic_bp UNKNOWN / untraceable
→ relative_drop_branch = INPUT_INSUFFICIENT
→ 不得把 HIGH 判成明确 NO_MATCH
```

`usual_systolic_bp` 只有在有可追溯来源时才能参与计算。

### C-RULE-SEPSIS-SBP-MODHIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-002]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = systolic_bp_mmHg
predicate = 91 <= systolic_bp_mmHg <= 100
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

当 SBP 在 91..100 且 usual SBP 未知时：

```text
MODHIGH rule 可独立 MATCHED
同时 HIGH relative-drop branch 可为 INPUT_INSUFFICIENT
两者允许并存；MODHIGH 不抑制 HIGH branch 的 insufficient 状态
```

### C-RULE-SEPSIS-HR-HIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-003]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = heart_rate_bpm
predicate = heart_rate_bpm > 130
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

`heart_rate_bpm == 130` 不命中 HIGH。

### C-RULE-SEPSIS-HR-MODHIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-VS-SEPSIS-003]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
required_measurement = heart_rate_bpm
predicate = 91 <= heart_rate_bpm <= 130
matched_signal = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

本 slice 排除 pregnancy/recent-pregnancy，不使用孕期 100..130 分层。

---

## 7. NG253 非数值 sepsis rules

### C-RULE-SEPSIS-APPEAR-HIGH-001

v0.1 新增了 B 未批准的 `mottled_or_ashen_or_cyanotic_appearance` operand；v0.2 删除该 operand，改为只消费 B evidence：

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-APPEAR-001]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
predicate = accepted_evidence(EV-RF-APPEAR-001) == PRESENT
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

该 rule 不新增 `mottled` taxonomy；只按 Gate A 已批准 appearance evidence 运行。

### C-RULE-SEPSIS-RASH-HIGH-001

```text
rule_version = 0.2.0-draft
required_evidence_refs = [EV-RF-SEPSIS-001]
source_reference_ids = [SRC-NICE-SEPSIS-NG253]
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
scope_precondition = U03_SEPSIS_SHARED_SCOPE_V0_2
predicate = accepted_evidence(EV-RF-SEPSIS-001) == PRESENT
matched_signal = RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
insufficient_signal = RULE_SIGNAL_INPUT_INSUFFICIENT
scope_mismatch_signal = RULE_SIGNAL_SCOPE_MISMATCH
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
```

---

## 8. v0.1 sepsis mental HIGH rule 的处置

`C-RULE-SEPSIS-MENTAL-HIGH-001` **不进入 v0.2 active rule set**。

原因：

```text
Gate A B Catalog 只有 EV-RF-NEURO-001
= 新发意识/行为/认知异常

NG253 HIGH objective altered mental state
与 MODHIGH history/new altered behaviour/functional deterioration
并未在 B 中拆成可执行、可区分 evidence
```

因此 C 不得使用：

```text
objective_altered_mental_state
```

也不得把：

```text
accepted_evidence(EV-RF-NEURO-001) == PRESENT
```

直接等同 `SEPSIS_HIGH_RISK_CRITERION`。

本 slice 明确排除：

```text
NG253 high-risk objective altered mental state rule
NG253 moderate-high history of new altered behaviour / mental state
NG253 moderate-high acute deterioration of functional ability
```

未来若要加入，必须先回 B 拆分/审核 evidence，再重新进入 C。

---

## 9. Explicitly Excluded Content

本 v0.2 继续不加入：

```text
new oxygen requirement / SpO2
urine output
temperature < 36°C
new-onset arrhythmia
immunosuppression
recent surgery / invasive procedure
NG253 objective mental HIGH rule
NG253 moderate-high altered behaviour / functional decline
pediatric sepsis
pregnancy / recent-pregnancy sepsis
China-localized emergency thresholds
NEWS2 / acute-hospital NG253 pathway
any rule not already supported by Gate A approved B evidence definitions
```

这些内容若要进入 C，必须先回 A/B 增补并审核。

---

## 10. Priority / Conflict Semantics

C 只定义 rule execution，不定义 D09 disposition precedence。

```text
multiple MATCHED rule results may coexist
MATCHED and INPUT_INSUFFICIENT from different rules/branches may coexist
no first-hit-wins
no file-order priority
no rule suppresses another rule by default
```

特别地：

```text
SBP 91..100
+
usual SBP unknown

→ MODHIGH MATCHED
+
HIGH relative-drop branch INPUT_INSUFFICIENT
```

最终如何映射为 Risk Disposition 属于 D09，当前仍禁止开始。

---

## 11. Candidate Freeze 尚缺字段

本 v0.2 为 re-review draft，不宣称 candidate-ready。freeze 前仍至少需要在再审后确认/冻结：

```text
rule_version / release_version candidate value
missingness_policy_ref = validated
scope_context contract refs = validated
evaluation_refs[] = available
clinical review = APPROVE
technical review = APPROVE
all blocking REVISE = 0
```

---

## 12. v0.1 → v0.2 修订摘要

```text
C-RULE-SEPSIS-SBP-HIGH-001
= explicit absolute/drop branch execution semantics added

C-RULE-SEPSIS-MENTAL-HIGH-001
= removed from active rule set; explicit exclusion added

C-RULE-SEPSIS-APPEAR-HIGH-001
= removed invented operand; now consumes EV-RF-APPEAR-001 only

all active rules
= matched / insufficient / scope-mismatch semantics added

all sepsis numeric rules
= per-rule U03_SEPSIS_SHARED_SCOPE_V0_2 precondition added

suspected_sepsis
= must pre-exist execution; cannot be inferred solely from same vital being evaluated

NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT
= governed source-specific context; not arbitrary dyspnoea complaint
```

---

## 13. 当前状态

```text
C Structural Schema = FROZEN
C Content Draft v0.1 Review = COMPLETE
C Content Draft v0.2 = AVAILABLE
Active executable rule candidates = 15
v0.1 mental HIGH rule = WITHHELD_FROM_ACTIVE_SET
Knowledge Release Ref = KR-U03-SOURCE-001@0.1.0-candidate
Medical Re-review = REQUIRED
Technical Re-review = REQUIRED
Initial Rule Release Freeze = NOT_COMPLETE
CD-03 = NOT_PASSED

D D09 Clinical Policy Content = BLOCKED
Gate B = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
Production = BLOCKED
```

下一步只能对 C v0.2 做 Medical / Technical re-review。全部 blocking findings 清零前，不冻结 `RR-U03-RISK-001` candidate，不开始 D09。