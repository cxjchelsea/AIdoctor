# U03 D09 Clinical Policy Table — Content Draft v0.2

> 对象：Clinical Input Package D / D09 deterministic Clinical Risk Disposition Policy 修订草案。  
> 状态：`REVISION_DRAFT / RE_REVIEW_COMPLETE / APPROVED_FOR_CONTENT_AND_COVERAGE / NOT_FROZEN / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 前置：`RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN`；`KR-U03-SOURCE-001@0.1.0-candidate = CANDIDATE_FROZEN / RESOLVABLE`。  
> 修订依据：`U03_D09_Clinical_Policy_Revision_Task_v0.1.md`。  
> 再审记录：`U03_D09_Clinical_Policy_ReReview_Record_v0.2.md`。  
> Coverage contract：`U03_D09_COVERAGE_V0_2`（当前 `APPROVED_FOR_CONTENT / NOT_FROZEN`）。  
> 本文件只定义 D09 review draft；不修改 C、不定义 U04、不构成 Implementation Authorization。

---

## 1. Policy Release Identity

```text
policy_release_id = PR-U03-D09-001
policy_set_id = U03-D09-CLINICAL-RISK-DISPOSITION
policy_version = 0.2.0-draft
status = DRAFT
contract_version = U03_D09_POLICY_SCHEMA_V1
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_pair_freeze_ref = PF-U03-C-POLICY-001
coverage_contract_ref = U03_D09_COVERAGE_V0_2
region_scope = INTERNATIONAL_REFERENCE_ONLY
population_scope = Gate-A / C-candidate source-locked adult scope only
channel_scope = remote/community initial consultation where source-supported
```

`0.2.0-draft` 不得作为 runtime / production binding。

---

## 2. Formal Output Vocabulary

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

---

## 3. Input Contract

每次执行至少绑定：

```text
consultation_id
clinical_state_version
capability_binding_ref
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_release_ref = PR-U03-D09-001@0.2.0-draft
coverage_contract_ref = U03_D09_COVERAGE_V0_2
C rule execution results[]
accepted_evidence_refs[]
source/provenance refs
```

D09 只消费冻结 C candidate 的 execution/result vocabulary，不重新计算阈值、不新增 evidence taxonomy、不从模型常识或 mutable knowledge 生成政策条件。

---

## 4. Frozen Coverage Contract Dependency

P-020 / P-040 的 coverage denominator 必须由：

```text
U03_D09_COVERAGE_V0_2
```

唯一确定。

当前 review draft 中的 contract 语义为：

### ALWAYS_APPLICABLE

整体 D policy scope 成立时，以下 5 条始终纳入 denominator：

```text
C-RULE-RESP-001
C-RULE-NEURO-001
C-RULE-NEURO-002
C-RULE-CARD-001
C-RULE-ALLERGY-001
```

### CONDITIONALLY_APPLICABLE

```text
NHS_DYSPNOEA_FAMILY
  C-RULE-DYSPNOEA-APPEAR-001
  C-RULE-DYSPNOEA-CONFUSION-001

NG253_SEPSIS_FAMILY
  C-RULE-SEPSIS-RR-HIGH-001
  C-RULE-SEPSIS-RR-MODHIGH-001
  C-RULE-SEPSIS-SBP-HIGH-001
  C-RULE-SEPSIS-SBP-MODHIGH-001
  C-RULE-SEPSIS-HR-HIGH-001
  C-RULE-SEPSIS-HR-MODHIGH-001
  C-RULE-SEPSIS-APPEAR-HIGH-001
  C-RULE-SEPSIS-RASH-HIGH-001
```

D09 只允许消费 C 已冻结执行结果映射：

```text
RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ exclude from P-020 / P-040 denominator

RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 / P-040

MATCHED / NO_MATCH
→ APPLICABLE_EVALUATED
```

必须保持：

```text
OVERALL_POLICY_SCOPE_MISMATCH
!= RULE_SIGNAL_SCOPE_MISMATCH
```

前者属于 D-level P0 failure；后者只表达 C specialized-family non-applicability。

---

## 5. Deterministic Precedence

```text
P0 INTEGRITY_OR_OVERALL_SCOPE_FAILURE
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

含义不变：

- P0 输入/release/整体 policy scope 不可信时，禁止任何临床 disposition；
- P1 已存在 HIGH-class 阳性时，不得因另一条适用规则 insufficient 而降级；
- P2 在无 HIGH 时 fail closed；
- P3 只有无 HIGH、无 applicable insufficiency 时允许 CAUTION；
- P4 只有 frozen coverage contract 判定 completeness 完成后允许；
- P5 作为无唯一合法结果的守底失败。

---

## 6. Policy Branches

### D09-P-001 — Integrity / Release / Currentness / Overall Policy Scope Failure

```text
policy_id = D09-P-001
priority = 1000
precedence_group = P0_INTEGRITY_OR_OVERALL_SCOPE_FAILURE
preconditions = any of:
  stale clinical_state_version
  wrong/unresolvable rule_release_ref
  wrong/unresolvable knowledge_release_ref
  wrong/unresolvable policy/policy-pair/coverage-contract ref
  invalid accepted evidence binding
  dependency execution failure preventing trustworthy C input
  consultation outside this D policy release population_scope
  consultation outside this D policy release region_scope
  consultation outside this D policy release channel_scope
result_status = FAILED
disposition = NONE
reason_code ∈ {
  STALE_INPUT,
  RELEASE_MISMATCH,
  INVALID_INPUT,
  DEPENDENCY_FAILURE,
  OVERALL_POLICY_SCOPE_MISMATCH
}
failure_behavior = FAIL_CLOSED
```

明确：

```text
OVERALL_POLICY_SCOPE_MISMATCH
= whole-policy D failure

RULE_SIGNAL_SCOPE_MISMATCH
= C specialized-family non-applicability
= NOT_APPLICABLE
!= D09-P-001 by itself
```

不得新增第七个医学 branch。

### D09-P-010 — High-risk Rule Signal Present

```text
policy_id = D09-P-010
priority = 900
precedence_group = P1_HIGH_RISK_SIGNAL
preconditions =
  no P0
  AND at least one valid applicable C result contains any of:
    RULE_SIGNAL_CRITICAL_RED_FLAG
    RULE_SIGNAL_MUST_NOT_MISS
    RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
result_status = VALID
disposition = HIGH_RISK
reason_code = HIGH_RISK_RULE_SIGNAL_PRESENT
```

要求保留全部 contributing `matched_rule_refs[]`。

若另一条 applicable rule 同时 `INPUT_INSUFFICIENT`：

```text
HIGH_RISK remains HIGH_RISK
+ insufficient_rule_refs[] retained
```

### D09-P-020 — Coverage Contract Incomplete, No High Signal

```text
policy_id = D09-P-020
priority = 800
precedence_group = P2_APPLICABLE_INPUT_INSUFFICIENT
coverage_contract_ref = U03_D09_COVERAGE_V0_2
preconditions =
  no P0
  AND no P1
  AND exists coverage_state == INSUFFICIENT_APPLICABLE
result_status = FAILED
disposition = NONE
reason_code = INSUFFICIENT_INFORMATION
failure_behavior = FAIL_CLOSED
```

`NOT_APPLICABLE` 不计入 insufficiency；`INSUFFICIENT_APPLICABLE` 必须计入 P2，并阻断 P3/P4。

### D09-P-030 — Moderate/High Sepsis Criterion, Complete Enough for Caution

```text
policy_id = D09-P-030
priority = 700
precedence_group = P3_CAUTION_SIGNAL
coverage_contract_ref = U03_D09_COVERAGE_V0_2
preconditions =
  no P0
  AND no P1
  AND no P2
  AND at least one applicable C result contains:
    RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
result_status = VALID
disposition = CAUTION
reason_code = MODERATE_HIGH_RULE_SIGNAL_PRESENT
```

必须保留所有 contributing `matched_rule_refs[]`；不得 first-hit-wins。

### D09-P-040 — No High-risk Signal in Coverage-complete Denominator

```text
policy_id = D09-P-040
priority = 500
precedence_group = P4_NO_HIGH_RISK_SIGNAL
coverage_contract_ref = U03_D09_COVERAGE_V0_2
preconditions =
  no P0
  AND no P1
  AND no P2
  AND no P3
  AND overall D policy scope == satisfied
  AND all ALWAYS_APPLICABLE rules == APPLICABLE_EVALUATED
  AND every CONDITIONALLY_APPLICABLE family == APPLICABLE_EVALUATED OR NOT_APPLICABLE
  AND no coverage_state == INSUFFICIENT_APPLICABLE
  AND every denominator-included C rule result == NO_MATCH
result_status = VALID
disposition = NO_HIGH_RISK_SIGNAL
reason_code = COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL
```

因此：

```text
suspected_sepsis UNKNOWN / NOT_ESTABLISHED
→ C INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ P2
→ P4 blocked
```

NHS dyspnoea context UNKNOWN / provenance independence 不可验证同理。

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
precondition = no unique legal branch after deterministic applicability / precedence resolution
result_status = FAILED
disposition = NONE
reason_code = UNRESOLVABLE_CONFLICT
failure_behavior = FAIL_CLOSED
```

---

## 7. Multi-hit / Conflict Semantics

```text
multiple HIGH-class hits
→ HIGH_RISK
→ preserve all matched_rule_refs

HIGH + CAUTION
→ HIGH_RISK

HIGH + INSUFFICIENT_APPLICABLE
→ HIGH_RISK
→ preserve insufficient refs

CAUTION + INSUFFICIENT_APPLICABLE
→ FAILED / INSUFFICIENT_INFORMATION

CAUTION only + coverage complete enough
→ CAUTION

all denominator-included rules NO_MATCH + coverage complete
→ NO_HIGH_RISK_SIGNAL

specialized-family SCOPE_MISMATCH
→ NOT_APPLICABLE
→ excluded from denominator

overall D policy scope mismatch
→ D09-P-001 / FAILED / OVERALL_POLICY_SCOPE_MISMATCH
```

禁止 file-order precedence、first-hit-wins、LLM synthesis。

---

## 8. Decision Object Requirements

每个 Decision 至少记录：

```text
decision_id
consultation_id
clinical_state_version
result_status
disposition | NONE
reason_code
coverage_contract_ref = U03_D09_COVERAGE_V0_2
coverage_state_by_rule_or_family[]
accepted_evidence_refs[]
matched_rule_refs[]
insufficient_rule_refs[] where applicable
not_applicable_rule_family_refs[] where applicable
capability_binding_ref
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_release_ref = PR-U03-D09-001@0.2.0-draft
source_refs[]
provenance_refs[]
created_at
```

D09 Decision != StateChangeProposal；后续仍需 proposal factory → K09 → P01。

---

## 9. Explicit Non-goals

本 v0.2 不定义：

```text
U04 Safety Gate
U14 failure routing
China production-localized policy
pediatric policy
pregnancy/recent-pregnancy sepsis policy
new C threshold/evidence
D09 runtime code
production publication/effective time
```

---

## 10. v0.1 → v0.2 Revision Summary

```text
BF-D-01
= APPLIED
D09-P-001 now includes OVERALL_POLICY_SCOPE_MISMATCH
C family-level RULE_SIGNAL_SCOPE_MISMATCH remains NOT_APPLICABLE

BF-D-02
= APPLIED
U03_D09_COVERAGE_V0_2 created as explicit denominator object
P-020 / P-040 explicitly reference coverage_contract_ref
baseline 5 rules = ALWAYS_APPLICABLE
NHS dyspnoea + NG253 sepsis = CONDITIONALLY_APPLICABLE
INPUT_INSUFFICIENT = INSUFFICIENT_APPLICABLE
SCOPE_MISMATCH = NOT_APPLICABLE

P-030
= retains all matched_rule_refs

precedence
= unchanged: P0 > P1 > P2 > P3 > P4 > P5
```

---

## 11. Current Status

```text
PR-U03-D09-001@0.1.0-draft Review = COMPLETE / REVISE_REQUIRED
PR-U03-D09-001@0.2.0-draft = REVIEWED
BF-D-01 = CLOSED
BF-D-02 = CLOSED
Coverage Contract = U03_D09_COVERAGE_V0_2 / APPROVED_FOR_CONTENT / NOT_FROZEN
Medical Re-review = COMPLETE
Technical Re-review = COMPLETE
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
D Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED
C/D/E Cross-Consistency = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
Production = BLOCKED
```

下一步只评估 coverage-contract freeze / D policy candidate freeze / CD-05 readiness。不冻结 D、不开始 runtime/CD-07/U04。
