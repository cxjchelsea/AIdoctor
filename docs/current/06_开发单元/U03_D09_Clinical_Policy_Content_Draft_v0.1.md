# U03 D09 Clinical Policy Table — Content Draft v0.1

> 对象：Clinical Input Package D / D09 deterministic Clinical Risk Disposition Policy 初始内容草案。  
> 状态：`CLINICAL_POLICY_DRAFT / MEDICAL_REVIEW_COMPLETE / TECHNICAL_REVIEW_COMPLETE / REVISE_REQUIRED / NOT_APPROVED / NOT_FROZEN / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 前置：`RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN`；`KR-U03-SOURCE-001@0.1.0-candidate = CANDIDATE_FROZEN / RESOLVABLE`。  
> 审核记录：`U03_D09_Clinical_Policy_Review_Record_v0.1.md`  
> 修订任务：`U03_D09_Clinical_Policy_Revision_Task_v0.1.md`  
> 本文件只定义 D09 的确定性 disposition draft；不修改 C rule、不定义 U04 Safety Gate、不构成 Implementation Authorization。

---

## 1. Policy Release Identity

```text
policy_release_id = PR-U03-D09-001
policy_set_id = U03-D09-CLINICAL-RISK-DISPOSITION
policy_version = 0.1.0-draft
status = DRAFT
contract_version = U03_D09_POLICY_SCHEMA_V1
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_pair_freeze_ref = PF-U03-C-POLICY-001
region_scope = INTERNATIONAL_REFERENCE_ONLY
population_scope = Gate-A / C-candidate source-locked adult scope only
channel_scope = remote/community initial consultation where source-supported
```

本 draft 不得作为 runtime / production binding。

---

## 2. D09 Formal Output Vocabulary

D09 只允许：

```text
VALID + HIGH_RISK
VALID + CAUTION
VALID + NO_HIGH_RISK_SIGNAL
FAILED + disposition = NONE
```

必须保持：

```text
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
NO_HIGH_RISK_SIGNAL != NORMAL
Rule Signal != D09 Disposition
D09 Decision != U04 Safety Gate Decision
```

`NO_HIGH_RISK_SIGNAL` 仅表示：**在本 policy release 所覆盖、实际适用且完成充分评估的 governed C rule set 中，没有形成 HIGH/CAUTION disposition 所需的 rule signal。** 它不是“患者安全”或“无疾病”的声明。

---

## 3. Input Contract

D09 每次执行必须绑定：

```text
consultation_id
clinical_state_version
capability_binding_ref
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_release_ref = PR-U03-D09-001@0.1.0-draft
C rule execution results[]
accepted_evidence_refs[]
source/provenance refs
```

输入只允许消费冻结 C candidate 的 rule identities / execution semantics / rule-level signals：

```text
RULE_SIGNAL_CRITICAL_RED_FLAG
RULE_SIGNAL_MUST_NOT_MISS
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

D09 不得重新计算 RR/SBP/HR 阈值，不得重新解释 B evidence，也不得从自由文本、模型常识、实时网页或 mutable RAG 生成新的 clinical policy condition。

---

## 4. Rule-family Applicability Model

D09 必须区分：

```text
APPLICABLE_RULE
NOT_APPLICABLE_RULE
INSUFFICIENT_APPLICABLE_RULE
```

而不能把所有 `SCOPE_MISMATCH` 一律升级为整次 Risk Assessment failure。

### 4.1 Baseline source-locked rules

```text
C-RULE-RESP-001
C-RULE-NEURO-001
C-RULE-NEURO-002
C-RULE-CARD-001
C-RULE-ALLERGY-001
```

这些规则仍受各自 C candidate scope 限制。

### 4.2 NHS dyspnoea source-locked family

```text
C-RULE-DYSPNOEA-APPEAR-001
C-RULE-DYSPNOEA-CONFUSION-001
```

只有 `scope_context.nhs_dyspnoea_emergency_warning_context` 合法成立时进入适用集合。

若该 context 明确为 FALSE / outside scope，C 的 `SCOPE_MISMATCH` 表示该 family 对当前输入 `NOT_APPLICABLE`，不能单独制造 D09 `FAILED`。

若 context 本应可判定但为 UNKNOWN / provenance independence 不可验证，C 形成 `INPUT_INSUFFICIENT`；此时该 family 属于 `INSUFFICIENT_APPLICABLE_RULE`。

### 4.3 NG253 sepsis source-locked family

绑定 `U03_SEPSIS_SHARED_SCOPE_V0_2` 的 8 条 active sepsis rule：

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

`suspected_sepsis == FALSE`、age<16、pregnancy/recent-pregnancy 或 setting outside source-supported community/custodial 时，该 family 对当前 input 为 `NOT_APPLICABLE`；不得仅因这些专用规则 `SCOPE_MISMATCH` 把普通非 sepsis consultation 判为 D09 `FAILED`。

若 shared-scope input 本应评估但 UNKNOWN / NOT_ESTABLISHED / provenance independence 不可验证，则 C 的 `INPUT_INSUFFICIENT` 进入 D09 fail-closed completeness 判断。

---

## 5. Deterministic Precedence

D09 v0.1 draft 使用显式 precedence group，禁止 file-order / first-hit-wins / LLM synthesis。

```text
P0 INTEGRITY_FAILURE
P1 HIGH_RISK_SIGNAL
P2 APPLICABLE_INPUT_INSUFFICIENT
P3 CAUTION_SIGNAL
P4 NO_HIGH_RISK_SIGNAL
P5 NO_VALID_DECISION
```

优先级：

```text
P0 > P1 > P2 > P3 > P4 > P5
```

特别说明：

- release/currentness/integrity failure 高于任何 clinical rule hit，因为此时无法证明输入来自正确冻结 release；
- 在 release/currentness 完整的前提下，只要存在有效 HIGH-class rule signal，允许形成 `VALID + HIGH_RISK`，即使另一条适用 rule 同时为 `INPUT_INSUFFICIENT`；缺失信息不得把已经存在的高风险阳性降级；
- 若无 HIGH-class hit，但任何实际适用 rule 为 `INPUT_INSUFFICIENT`，不得形成 CAUTION 或 NO_HIGH_RISK_SIGNAL，必须 fail closed；
- `CAUTION` 只有在无 HIGH、无 applicable insufficiency 时才能形成；
- `NO_HIGH_RISK_SIGNAL` 只有在 coverage-complete 条件满足时才能形成。

---

## 6. Policy Branches

### D09-P-001 — Integrity / Release / Currentness Failure

```text
policy_id = D09-P-001
priority = 1000
precedence_group = P0_INTEGRITY_FAILURE
preconditions = any of:
  stale clinical_state_version
  wrong/unresolvable rule_release_ref
  wrong/unresolvable knowledge_release_ref
  wrong/unresolvable policy/policy-pair ref
  invalid accepted evidence binding
  dependency execution failure preventing trustworthy C input
result_status = FAILED
disposition = NONE
reason_code ∈ {
  STALE_INPUT,
  RELEASE_MISMATCH,
  INVALID_INPUT,
  DEPENDENCY_FAILURE
}
failure_behavior = FAIL_CLOSED
```

该 branch 禁止伪造 `NO_HIGH_RISK_SIGNAL`。

### D09-P-010 — High-risk Rule Signal Present

```text
policy_id = D09-P-010
priority = 900
precedence_group = P1_HIGH_RISK_SIGNAL
required_rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
precondition = at least one valid applicable C result contains any of:
  RULE_SIGNAL_CRITICAL_RED_FLAG
  RULE_SIGNAL_MUST_NOT_MISS
  RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
result_status = VALID
disposition = HIGH_RISK
reason_code = HIGH_RISK_RULE_SIGNAL_PRESENT
```

多条 HIGH-class signal 可以并存；D09 必须保留全部 matched_rule_refs，而不是 first-hit-wins。

如果另一条适用 rule 同时 `INPUT_INSUFFICIENT`：

```text
HIGH_RISK remains HIGH_RISK
+ insufficient refs retained in provenance/reason detail
```

不得因为信息缺失而把已存在的明确高风险阳性降级。

### D09-P-020 — Applicable Information Insufficient, No High Signal

```text
policy_id = D09-P-020
priority = 800
precedence_group = P2_APPLICABLE_INPUT_INSUFFICIENT
preconditions =
  no P0 integrity failure
  AND no P1 high-risk signal
  AND at least one actually applicable C rule result is INPUT_INSUFFICIENT
result_status = FAILED
disposition = NONE
reason_code = INSUFFICIENT_INFORMATION
failure_behavior = FAIL_CLOSED
```

包括但不限于：

```text
RULE_SIGNAL_INPUT_INSUFFICIENT
missing/unknown required measurement
unresolved required evidence
required scope-context provenance independence cannot be validated
```

不得把 insufficiency 当成 `NO_MATCH`。

### D09-P-030 — Moderate/High Sepsis Criterion, Complete Enough for Caution

```text
policy_id = D09-P-030
priority = 700
precedence_group = P3_CAUTION_SIGNAL
preconditions =
  no P0 integrity failure
  AND no P1 high-risk signal
  AND no P2 applicable insufficiency
  AND at least one applicable C result contains:
    RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
result_status = VALID
disposition = CAUTION
reason_code = MODERATE_HIGH_RULE_SIGNAL_PRESENT
```

本 branch 不得把 `CAUTION` 解释成 U04 Safety Gate 决策。

### D09-P-040 — No High-risk Signal in Fully Evaluated Applicable Set

```text
policy_id = D09-P-040
priority = 500
precedence_group = P4_NO_HIGH_RISK_SIGNAL
preconditions =
  no P0 integrity failure
  AND no P1 high-risk signal
  AND no P2 applicable insufficiency
  AND no P3 caution signal
  AND all actually applicable governed rules/families required by this policy execution are deterministically evaluated
  AND each applicable rule is NO_MATCH
result_status = VALID
disposition = NO_HIGH_RISK_SIGNAL
reason_code = APPLICABLE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL
```

`SCOPE_MISMATCH` 的专用 rule family 只有在明确 `NOT_APPLICABLE` 后才可从 completeness denominator 中排除。

必须保持：

```text
NO_HIGH_RISK_SIGNAL
!= SAFE
!= NORMAL
!= no disease
!= no future deterioration
```

### D09-P-090 — No Unique Valid Decision

```text
policy_id = D09-P-090
priority = 100
precedence_group = P5_NO_VALID_DECISION
precondition = policy execution reaches no unique legal branch after deterministic applicability/precedence resolution
result_status = FAILED
disposition = NONE
reason_code = UNRESOLVABLE_CONFLICT
failure_behavior = FAIL_CLOSED
```

该 branch 是守底失败分支，不允许由 LLM 自由补结论。

---

## 7. Multi-hit / Conflict Semantics

确定性行为：

```text
multiple HIGH-class hits
→ one VALID + HIGH_RISK decision
→ preserve all contributing matched_rule_refs

HIGH-class hit + CAUTION-class hit
→ HIGH_RISK

HIGH-class hit + applicable INPUT_INSUFFICIENT
→ HIGH_RISK
→ preserve insufficient evidence/rule refs as supplementary trace

CAUTION-class hit + applicable INPUT_INSUFFICIENT
→ FAILED / INSUFFICIENT_INFORMATION

only CAUTION-class hits, no insufficiency
→ CAUTION

only NO_MATCH across complete applicable set
→ NO_HIGH_RISK_SIGNAL

specialized family SCOPE_MISMATCH
→ NOT_APPLICABLE for that family
→ does not itself create FAILED
```

禁止：

```text
first-hit-wins
file-order precedence
random selection
LLM synthesis of a fourth disposition
```

---

## 8. Overall Scope Failure vs Specialized-family Non-applicability

必须区分：

```text
整体 D policy scope 不成立
vs
某个专用 C rule family 不适用
```

若 consultation 本身明确不属于本 D policy release 的 population/region/channel scope：

```text
result_status = FAILED
disposition = NONE
reason_code = SCOPE_MISMATCH
```

但若 consultation 在 D 总体 scope 内，只是：

```text
suspected_sepsis == FALSE
或
NHS dyspnoea emergency warning context == FALSE
```

则仅对应专用 rule family `NOT_APPLICABLE`，不应把整个 D09 decision 判失败。

---

## 9. Decision Object Requirements

每个 D09 Decision 至少记录：

```text
decision_id
consultation_id
clinical_state_version
result_status
disposition | NONE
reason_code
accepted_evidence_refs[]
matched_rule_refs[]
insufficient_rule_refs[] where applicable
not_applicable_rule_family_refs[] where applicable
capability_binding_ref
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_release_ref = PR-U03-D09-001@0.1.0-draft
source_refs[]
provenance_refs[]
created_at
```

D09 Decision 不直接写 Clinical State；后续仍由 proposal factory → typed K09 proposal → P01 commit。

---

## 10. Explicit Non-goals / Exclusions

本 v0.1 不定义：

```text
U04 Safety Gate output
U14 failure routing
China-localized production disposition policy
pediatric policy
pregnancy/recent-pregnancy sepsis policy
new C thresholds or evidence taxonomy
D09 runtime code
production publication/effective time
```

也不把 C candidate / KR candidate 当成 production release。

---

## 11. Review Questions

Medical Owner + Technical review 至少确认：

1. HIGH-class 三类 C signal → HIGH_RISK 是否成立；
2. SEPSIS_MODERATE_HIGH → CAUTION 的条件是否成立；
3. applicable INPUT_INSUFFICIENT 在无 HIGH 时 → FAILED 是否足够 fail-closed；
4. HIGH + insufficiency → HIGH_RISK 是否保留明确阳性且不被缺失信息降级；
5. specialized-family `SCOPE_MISMATCH` → NOT_APPLICABLE 而非整次 FAILED 是否正确；
6. `NO_HIGH_RISK_SIGNAL` 的 completeness 条件是否足够严格；
7. P0>P1>P2>P3>P4>P5 precedence 是否医学/技术可接受；
8. reason codes / decision provenance 是否足够支持 tracing、review 和后续 U04 输入；
9. 是否存在任何 D09 越权成 U04 Safety Gate 的语义；
10. 是否允许进入 D v0.2 / policy candidate readiness。

允许 verdict：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

正式裁决见 `U03_D09_Clinical_Policy_Review_Record_v0.1.md`。本 v0.1 不得进入 candidate freeze。

---

## 12. Current Status

```text
D Structural Schema = FROZEN
D Drafting Readiness = PASS_FOR_DRAFTING
D Clinical Policy Content Draft v0.1 = REVIEWED
Policy Release = PR-U03-D09-001@0.1.0-draft
Rule Release Ref = RR-U03-RISK-001@0.2.0-candidate
Knowledge Release Ref = KR-U03-SOURCE-001@0.1.0-candidate
Medical Owner Review = COMPLETE
Technical Review = COMPLETE
D Content Approval = REVISE_REQUIRED
blocking findings = BF-D-01, BF-D-02
D Policy Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED
C/D/E Cross-Consistency = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
Production = BLOCKED
```
