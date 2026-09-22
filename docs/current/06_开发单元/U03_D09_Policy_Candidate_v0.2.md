# U03 D09 Policy Candidate v0.2

> 对象：D / D09 deterministic Clinical Risk Disposition Policy 的独立 candidate release identity。  
> Candidate Ref：`PR-U03-D09-001@0.2.0-candidate`  
> 状态：`CANDIDATE_FROZEN / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 本文件只定义独立 candidate identity；**不修改、不重命名、不覆盖** `PR-U03-D09-001@0.2.0-draft`。

---

## 1. Candidate Identity

```text
policy_release_id = PR-U03-D09-001
policy_set_id = U03-D09-CLINICAL-RISK-DISPOSITION
policy_version = 0.2.0-candidate
status = CANDIDATE_FROZEN
contract_version = U03_D09_POLICY_SCHEMA_V1
source_draft_ref = PR-U03-D09-001@0.2.0-draft
candidate_created_from = U03_D09_Clinical_Policy_Content_Draft_v0.2.md
candidate_freeze_record_ref = U03_D09_Policy_Candidate_Freeze_Record_v0.2.md
```

必须保持：

```text
PR-U03-D09-001@0.2.0-draft
!=
PR-U03-D09-001@0.2.0-candidate
```

不得把 draft 就地改名为 candidate。

---

## 2. Bound Governed Dependencies

```text
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_pair_freeze_ref = PF-U03-C-POLICY-001
coverage_contract_ref = U03_D09_COVERAGE_V0_2
coverage_contract_freeze_ref = U03_D09_Coverage_Contract_Freeze_Record_v0.2.md
pre_freeze_eval_minimum_ref = U03_D09_PreFreeze_Evaluation_Minimum_v0.1.md
pre_freeze_fixture_pack_ref = U03_D09_PreFreeze_Evaluation_Fixtures_v0.1.md
pre_freeze_eval_review_ref = U03_D09_PreFreeze_Eval_Review_Record_v0.1.md
```

绑定状态：

```text
RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN
KR-U03-SOURCE-001@0.1.0-candidate = CANDIDATE_FROZEN
PF-U03-C-POLICY-001 = CANDIDATE_FROZEN
U03_D09_COVERAGE_V0_2 = CANDIDATE_FROZEN
D Pre-Freeze Eval PASS = YES
```

---

## 3. Candidate Content Inheritance

本 candidate 不复制第二份 D09 临床策略真值，只固定 v0.2 已批准内容及其依赖。

```text
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
BF-D-01 = CLOSED
BF-D-02 = CLOSED
6 / 6 branches Medical = APPROVE
6 / 6 branches Technical = APPROVE
coverage contract Medical = APPROVE
coverage contract Technical = APPROVE
```

权威内容仍来自：

```text
U03_D09_Clinical_Policy_Content_Draft_v0.2.md
U03_D09_Coverage_Contract_v0.2.md
```

本 candidate 只形成独立 release identity 和固定 dependency set；不重新发明 branch、precedence 或 disposition。

---

## 4. Frozen Output Vocabulary

本 candidate 冻结后只允许正式 D09 结果：

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

## 5. Frozen Precedence

```text
P0 INTEGRITY_OR_OVERALL_SCOPE_FAILURE
> P1 HIGH_RISK_SIGNAL
> P2 APPLICABLE_INPUT_INSUFFICIENT
> P3 CAUTION_SIGNAL
> P4 NO_HIGH_RISK_SIGNAL
> P5 NO_VALID_DECISION
```

不得依赖 file order / first-hit-wins / LLM synthesis。

---

## 6. Scope

```text
population_scope = Gate-A / C-candidate source-locked adult scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
pediatrics = EXCLUDED
pregnancy/recent-pregnancy sepsis policy = NOT_INCLUDED
china_localized_production_policy = NOT_INCLUDED
```

整体 scope 不成立时仍属于：

```text
D09-P-001
→ FAILED
→ OVERALL_POLICY_SCOPE_MISMATCH
```

specialized-family `RULE_SIGNAL_SCOPE_MISMATCH` 仍只表示 `NOT_APPLICABLE`。

---

## 7. Evaluation Binding

minimum pre-freeze eval 已完成审核：

```text
fixture_count = 48
asset_groups = 8 / 8 APPROVE
D-EVAL-01..10 = APPROVE / APPROVE
blocking_eval_finding = 0
D Pre-Freeze Eval PASS = YES
```

这只支持 D policy candidate freeze：

```text
D Pre-Freeze Eval PASS
!= Gate C PASS
!= CD-06 REVIEW_READY
!= Production Validation
```

---

## 8. Lifecycle Boundary

当前 candidate：

```text
RESOLVABLE_CANDIDATE
CANDIDATE_FROZEN
FROZEN_D09_MAPPING_FOR_C_D_E_CONSISTENCY_REVIEW
```

不等于：

```text
PUBLISHED
ACTIVE_FOR_RUNTIME
ACTIVE_FOR_PRODUCTION
GATE_B_PASS
GATE_C_PASS
CD-07_AUTHORIZED
PRODUCTION_AUTHORIZED
```

任何后续修改必须形成新的 D09 policy candidate version，不得原地改写 `0.2.0-candidate`。

---

## 9. Current Status

```text
PR-U03-D09-001@0.2.0-draft = PRESERVED / NOT_RENAMED
PR-U03-D09-001@0.2.0-candidate = RESOLVABLE / CANDIDATE_FROZEN
Candidate Freeze = COMPLETE
BLOCKER-FZ-D-03 = CLOSED
BLOCKER-FZ-D-04 = CLOSED
CD-05 = DECISION_PENDING
Gate B = NOT_PASSED
Gate C = NOT_PASSED
Runtime = BLOCKED
Production = BLOCKED
```

下一步只允许执行独立 CD-05 decision；不得把 candidate freeze 解释为 Gate B/C 或 runtime authorization。