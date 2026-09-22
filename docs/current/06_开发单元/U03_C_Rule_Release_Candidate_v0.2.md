# U03 C Rule Release Candidate v0.2

> 对象：C / Safety-critical Risk Rule Pack 的独立 candidate release identity。  
> Candidate Ref：`RR-U03-RISK-001@0.2.0-candidate`  
> 状态：`CANDIDATE_OBJECT_CREATED / FREEZE_PENDING / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 本文件创建独立 candidate 对象；**不修改、不重命名、不覆盖** `RR-U03-RISK-001@0.2.0-draft`。

---

## 1. Candidate Identity

```text
rule_release_id = RR-U03-RISK-001
rule_set_id = U03-SAFETY-CRITICAL-RISK
release_version = 0.2.0-candidate
status = CANDIDATE_PENDING_FREEZE
contract_version = U03_RULE_SCHEMA_V1
source_draft_ref = RR-U03-RISK-001@0.2.0-draft
candidate_created_from = U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md
```

Candidate 与 draft 是两个治理身份：

```text
RR-U03-RISK-001@0.2.0-draft
!=
RR-U03-RISK-001@0.2.0-candidate
```

不得把 draft 就地改名为 candidate。

---

## 2. Bound Governance Dependencies

```text
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_pair_freeze_ref = PF-U03-C-POLICY-001
missingness_policy_ref = U03_C_MISSINGNESS_V0_2
sepsis_shared_scope_policy_ref = U03_SEPSIS_SHARED_SCOPE_V0_2
evaluation_manifest_ref = U03_C_EVAL_REFS_V0_2
pre_freeze_fixture_pack_ref = U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
pre_freeze_eval_review_ref = U03_C_PreFreeze_Eval_Review_Record_v0.2.md
```

以上依赖均必须保持可解析且与 freeze record 中的版本一致。

---

## 3. Candidate Content Inheritance

本 candidate **不复制第二份临床规则真值**。它冻结并引用 v0.2 已批准的 rule content：

```text
active_rule_count = 15
active_rule_review = APPROVE_15 / REVISE_0
BF-C-01 = CLOSED
BF-C-02 = CLOSED
BF-C-03 = CLOSED
BF-C-04 = CLOSED
C Package Approval = APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT
```

Rule predicate / threshold / signal vocabulary 的权威内容仍来自：

```text
U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md
```

本 candidate 只形成独立 release identity、固定依赖集合和 freeze target，不重新发明 rule 内容。

---

## 4. Frozen Vocabulary Target

若 freeze 通过，本 candidate 冻结以下 C 层 rule-level vocabulary：

```text
RULE_SIGNAL_CRITICAL_RED_FLAG
RULE_SIGNAL_MUST_NOT_MISS
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

明确不包含：

```text
HIGH_RISK
CAUTION
NO_HIGH_RISK_SIGNAL
FAILED
SAFE
NORMAL
```

这些不属于 C Rule Release candidate 的 disposition vocabulary。

---

## 5. Candidate Scope

```text
population_scope = Gate-A-approved adult / source-locked scope only
region_scope = INTERNATIONAL_REFERENCE_ONLY
channel_scope = remote/community initial consultation where source-supported
pediatrics = EXCLUDED
pregnancy_recent_pregnancy = EXCLUDED_FOR_NG253_SEPSIS_RULES
china_localized_production_pathway = NOT_INCLUDED
```

Sepsis shared scope 受冻结对象：

```text
U03_SEPSIS_SHARED_SCOPE_V0_2
```

Missingness 受冻结对象：

```text
U03_C_MISSINGNESS_V0_2
```

二者只能通过：

```text
PF-U03-C-POLICY-001
```

作为同一 policy pair 使用。

---

## 6. Evaluation Binding

Pre-freeze minimum 已建立并审核：

```text
fixture_count = 57
asset_groups = 8 / 8 APPROVE
PF-EVAL-01..08 = APPROVE / APPROVE
blocking_eval_finding = 0
Pre-Freeze Eval PASS = YES
```

这只支持 candidate freeze：

```text
Pre-Freeze Eval PASS
!= Gate C PASS
!= CD-06 REVIEW_READY
!= Production Validation
```

完整 Clinical EvalSet / Safety Suite 仍未完成。

---

## 7. Lifecycle Boundary

candidate 即使被 freeze，也仅表示：

```text
RESOLVABLE_CANDIDATE
FROZEN_RULE_RESULT_VOCABULARY_FOR_DOWNSTREAM_DRAFTING
```

不等于：

```text
PUBLISHED
ACTIVE_FOR_RUNTIME
ACTIVE_FOR_PRODUCTION
PRODUCTION_AUTHORIZED
GATE_C_PASS
D09_APPROVED
```

Runtime / production binding 禁止引用本 candidate，除非未来独立门禁明确授权。

---

## 8. Current Status

```text
RR-U03-RISK-001@0.2.0-draft = PRESERVED / NOT_RENAMED
RR-U03-RISK-001@0.2.0-candidate = CREATED
Candidate Freeze = PENDING_FREEZE_RECORD
BLOCKER-FZ-C-04 = OPEN
CD-03 = NOT_PASSED
D = BLOCKED
Gate C = NOT_PASSED
Production = BLOCKED
```
