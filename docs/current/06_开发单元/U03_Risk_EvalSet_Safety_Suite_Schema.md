# U03 Risk EvalSet / Safety Suite Schema

> 目的：冻结 U03 风险能力的独立评估结构、版本绑定、覆盖维度与发布门禁。  
> 本文件不提供真实临床病例、医学阈值、生产规则或诊断结论；所有真实 golden case 与 expected outcome 均须由医学 Owner 审核。

---

## 1. 评估定位

Risk EvalSet / Safety Suite 用于验证：

```text
Clinical State Version
→ C02 evidence candidate
→ evidence acceptance
→ Rule Pack evaluation
→ D09 policy decision
→ typed Risk proposal
→ formal commit
```

必须保持：

```text
Engineering Test != Clinical Eval
Synthetic Case != Clinical Golden Case
Eval Expected Outcome != Runtime Inference
EvalSet != Production Rule Source
```

EvalSet 只验证系统行为，不拥有 Clinical Truth。

---

## 2. EvalSet 与 Safety Suite 分层

### 2.1 EvalSet

回答：系统在受控输入下是否产生符合已批准 clinical policy 的预期结果。

最小覆盖：

- evidence extraction / acceptance；
- rule hit / no-hit；
- D09 result status / disposition；
- version / release binding；
- currentness；
- failure semantics；
- idempotency / no duplicate effect。

### 2.2 Safety Suite

回答：系统是否避免产生被明确禁止的危险语义或治理违规。

至少覆盖：

- unknown 不得被当成 negative；
- unmeasured 不得被当成 normal；
- capability / dependency failure 不得被当成低风险结果；
- stale result 不得冒充 current；
- release mismatch 不得静默接受；
- 未批准 rule / knowledge / policy 不得用于正式判断；
- candidate 不得绕过 D09/P01 直接 commit。

---

## 3. Case Schema

每个 case 至少包含：

```text
case_id
case_version
case_type
review_status
review_owner

clinical_state_fixture_ref
clinical_state_version
input_fact_refs[]
derived_assertion_refs[]

capability_binding_ref
rule_release_ref
knowledge_release_ref
policy_release_ref
contract_version

expected_business_status
expected_evidence_refs[]
expected_evidence_categories[]
expected_rule_refs[]
expected_policy_ref
expected_result_status
expected_disposition
expected_reason_code

must_not_output[]
must_not_commit[]
expected_failure_code

source_refs[]
rationale_ref
provenance_refs[]
created_at
updated_at
```

字段为结构要求，真实 expected 值由医学 Owner / policy owner 审核后填写。

---

## 4. Case Type

建议至少支持：

```text
POSITIVE_EXPECTED
NEGATIVE_ASSERTION
BOUNDARY
AMBIGUITY
CONFLICT
MISSING_DATA
VERSIONING
RELEASE_GOVERNANCE
FAILURE_PATH
IDEMPOTENCY
REGRESSION
```

case type 不代表医学风险等级，仅表示评估目的。

---

## 5. Expected 与 Forbidden 分离

必须同时支持正向期望和负向断言。

正向：

```text
expected_result_status
expected_disposition
expected_reason_code
```

负向：

```text
must_not_output[]
must_not_commit[]
```

重要原则：

```text
只验证“应该出现什么”是不够的
还必须验证“绝对不能出现什么”
```

---

## 6. 版本与 Release 绑定

每个临床 golden case 必须显式绑定：

```text
clinical_state_version
capability_binding_ref
rule_release_ref
knowledge_release_ref（适用时）
policy_release_ref
contract_version
```

禁止：

```text
同一 case 在不同 release 下运行
→ 仍宣称同一个 expected outcome 永久有效
```

当 rule / knowledge / policy release 发生变化时：

```text
case 需要 review
↓
expected outcome 需要重新确认
↓
形成新的 case_version 或明确 unchanged approval
```

---

## 7. Review Lifecycle

建议：

```text
DRAFT
MEDICAL_REVIEW
POLICY_REVIEW
READY_FOR_EVAL
APPROVED
DEPRECATED
RETIRED
```

必须保持：

```text
DRAFT != APPROVED
READY_FOR_EVAL != PRODUCTION_AUTHORIZED
DEPRECATED != DELETED
```

---

## 8. EvalSet Release

EvalSet 自身必须可版本化：

```text
evalset_release_id
evalset_version
status
case_refs[]
coverage_manifest_ref
source_refs[]
review_owner
review_status
approved_at
effective_from
effective_until
supersedes_refs[]
rollback_ref
```

建议状态：

```text
DRAFT
REVIEW
READY
ACTIVE_FOR_EVALUATION
DEPRECATED
RETIRED
```

EvalSet release 不是 production clinical release。

---

## 9. Coverage Manifest

至少从以下维度声明覆盖情况：

```text
evidence_category_coverage
rule_family_coverage
policy_branch_coverage
population_scope_coverage
region_scope_coverage
language_channel_coverage
missingness_coverage
ambiguity_coverage
conflict_coverage
failure_coverage
versioning_coverage
release_mismatch_coverage
idempotency_coverage
```

Coverage 必须是可审查清单，不允许只写“已充分覆盖”。

---

## 10. Gate Metrics

本结构层只定义指标类别，不冻结临床阈值。

至少包括：

```text
critical_case_pass_rate
safety_negative_assertion_pass_rate
rule_binding_accuracy
policy_binding_accuracy
version_binding_accuracy
forbidden_output_rate
unexpected_commit_rate
failure_semantics_accuracy
stale_acceptance_rate
release_mismatch_acceptance_rate
```

具体 release gate 阈值：

```text
TBD_BY_MEDICAL_OWNER / EVALUATION_OWNER
```

工程侧不得自行宣称“95% 即可上线”等门槛。

---

## 11. Independent Ownership

必须将：

```text
implementation owner
!=
eval expected outcome owner
```

推荐至少区分：

- engineering owner：执行 harness / reporting；
- medical owner：病例医学正确性与 expected clinical outcome；
- policy owner：D09 branch / reason code 一致性；
- evaluation owner：coverage、dataset version 与 release gate 管理。

开发实现者不能单独修改 production expected outcome 来让测试通过。

---

## 12. Regression Policy

任何以下变化都必须触发重新评估：

```text
Capability version change
Rule Release change
Knowledge Release change
D09 Policy Release change
Clinical contract change
Evidence Catalog release change
critical bug fix affecting risk semantics
```

至少执行：

```text
affected-case regression
+
critical safety suite
+
negative assertion suite
```

生产发布前不得只跑普通单元测试代替 Clinical Eval。

---

## 13. Failure 与 Eval Failure 分离

必须区分：

```text
Runtime / Capability Failure
!=
Eval Harness Failure
!=
Expected Outcome Mismatch
!=
Clinical Review Disagreement
```

四类问题的 owner 与处理流程不同，不允许全部折叠为 `TEST_FAILED`。

---

## 14. 当前结构状态

```text
F Risk EvalSet / Safety Suite
= STRUCTURAL_SCHEMA_FROZEN
/ GOLDEN_CASE_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ EVALUATION_OWNER_REVIEW_REQUIRED
/ NOT_REVIEW_READY
```

尚未包含：

```text
真实 clinical golden cases
真实 expected clinical outcomes
批准后的 negative assertions
正式 coverage manifest
正式 gate threshold
医学 / policy / evaluation owner 审核
```

因此该文件完成的是评估结构冻结，不代表 U03 已完成临床验证。
