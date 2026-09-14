# U03 D09 Clinical Policy Table Schema

> 目的：冻结 U03 中 D09 临床风险等级裁决规则的结构、版本、优先级、失败语义、审核和发布边界。  
> 本文件不提供任何具体医学裁决分支、症状阈值、生命体征阈值、疾病规则或生产医学策略。

---

## 1. D09 的定位

D09 是 U03 中唯一正式 Clinical Risk Disposition Owner。

控制链必须保持：

```text
C02 / Evidence Catalog / Rule Pack
→ accepted evidence + governed rule hits
→ D09 deterministic policy
→ formal Risk Decision
→ typed StateChangeProposal
→ P01 / StateCommitter
```

必须保持：

```text
Evidence != Decision
Rule Hit != Decision
Capability Result != Clinical Truth
D09 Decision != Commit
Risk Disposition != Safety Gate
```

U04 仍是 Safety Gate Owner；D09 不得越权形成 Safety Gate 结果。

---

## 2. D09 输出词表

D09 只允许形成以下正式结果：

```text
VALID + NO_HIGH_RISK_SIGNAL
VALID + CAUTION
VALID + HIGH_RISK
FAILED
```

禁止新增模糊词表，例如：

```text
LOW_RISK
SAFE
NORMAL
NO_PROBLEM
```

除非未来由正式业务设计重新冻结。

特别约束：

```text
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
NO_DDX != LOW_RISK
```

---

## 3. Policy Table 顶层对象

每个 D09 Policy Release 至少包含：

```text
policy_release_id
policy_set_id
policy_version
status
scope_ref
population_scope
region_scope
language_scope
channel_scope
rule_release_refs[]
knowledge_release_refs[]
source_refs[]
provenance_refs[]
evaluation_refs[]
review_owner
review_status
effective_from
effective_until
supersedes_refs[]
rollback_target_ref
created_at
published_at
withdrawn_at
```

推荐 release 状态：

```text
DRAFT
CLINICAL_REVIEW
TECHNICAL_REVIEW
EVALUATION
READY_TO_PUBLISH
PUBLISHED
DEPRECATED
WITHDRAWN
EXPIRED
RETIRED
```

必须保持：

```text
DRAFT != PUBLISHED
EVALUATION != PUBLISHED
WITHDRAWN != ACTIVE
EXPIRED != ACTIVE
```

---

## 4. Policy Branch 最小结构

每个确定性 branch 至少包含：

```text
policy_id
policy_version
description
preconditions
required_evidence_refs[]
required_rule_refs[]
optional_evidence_refs[]
priority
precedence_group
result_status
disposition
reason_code
failure_behavior
scope_ref
source_refs[]
provenance_refs[]
review_owner
review_status
effective_from
effective_until
```

其中：

```text
result_status ∈ {VALID, FAILED}
```

仅当：

```text
result_status = VALID
```

时，`disposition` 才可取：

```text
NO_HIGH_RISK_SIGNAL
CAUTION
HIGH_RISK
```

当：

```text
result_status = FAILED
```

时不得伪造任何低风险 disposition。

---

## 5. Preconditions 结构

`preconditions` 只描述确定性输入条件结构，不在本文件中填医学内容。

允许引用：

```text
clinical_state_version
accepted_evidence_refs[]
rule_hit_refs[]
rule_release_ref
knowledge_release_ref
capability_binding_ref
scope_ref
population_ref
region/language/channel context
business_status
```

禁止：

```text
直接读取未治理模型输出作为最终条件
未绑定版本的知识文本
未发布规则
stale Clinical State Version
```

---

## 6. Priority 与 Precedence

D09 必须显式版本化优先级与冲突裁决，禁止依赖：

```text
代码 if/else 顺序
文件顺序
数据库自然顺序
首次命中
模型自由选择
```

每个 branch 至少应提供：

```text
priority
precedence_group
conflict_behavior
```

具体哪个医学 branch 优先于哪个 branch：

```text
TBD_BY_MEDICAL_OWNER
```

工程层只负责执行已经发布的确定性 precedence。

---

## 7. 多 Branch 命中语义

当多个 policy branch 同时满足时，系统必须依据正式 release 中的 deterministic precedence 解析。

不得：

```text
随机选择
按返回顺序选择
由 LLM 自由综合成新结论
```

若 release 无法确定唯一合法结果，应显式进入：

```text
FAILED
```

具体冲突医学处理策略仍需医学 Owner 冻结。

---

## 8. Missing / Ambiguous / Conflicting / Failed 输入

D09 必须能够区分：

```text
NO_MATCH
INSUFFICIENT_INFORMATION
AMBIGUOUS_INPUT
CONFLICTING_INPUT
DEPENDENCY_FAILURE
STALE_INPUT
RELEASE_MISMATCH
INVALID_INPUT
```

这些 reason_code 不等于 disposition。

不得将：

```text
UNKNOWN
UNMEASURED
AMBIGUOUS
CONFLICTING
DEPENDENCY_FAILURE
```

静默解释成：

```text
NO_HIGH_RISK_SIGNAL
```

具体哪些条件允许继续形成 VALID，哪些必须 FAILED，由后续医学 Policy Release 冻结。

---

## 9. Version / Currentness 约束

每次 D09 决策至少绑定：

```text
consultation_id
clinical_state_version
capability_binding_ref
rule_release_ref
knowledge_release_ref
policy_release_ref
contract_version
```

必须保证：

```text
Decision.input_clinical_state_version
=
当前被评估的 Clinical State Version
```

若 Clinical State 已更新，旧 Decision 不得继续作为 current Risk。

D09 不负责修改上游 Clinical Facts，只消费当前治理状态。

---

## 10. Decision 对象边界

D09 输出的正式 Decision 至少应包含：

```text
decision_id
consultation_id
clinical_state_version
result_status
disposition
reason_code
accepted_evidence_refs[]
matched_rule_refs[]
capability_binding_ref
rule_release_ref
knowledge_release_ref
policy_release_ref
source_refs[]
provenance_refs[]
created_at
```

必须保持：

```text
D09 Decision
!=
StateChangeProposal
```

只有 Business Owner / proposal factory 才能把 Decision 转成 typed K09 Proposal。

---

## 11. Failure Behavior

D09 failure 必须是一等正式结果。

失败语义至少能表达：

```text
POLICY_NOT_AVAILABLE
RULE_RELEASE_NOT_AVAILABLE
KNOWLEDGE_RELEASE_NOT_AVAILABLE
RELEASE_MISMATCH
STALE_CLINICAL_STATE
INVALID_ACCEPTED_EVIDENCE
UNRESOLVABLE_CONFLICT
POLICY_EXECUTION_FAILURE
```

失败后：

```text
Risk Assessment = FAILED
```

并由后续 U04 作为 Safety Gate 输入处理。

本文件不实现 U04，也不定义 U14 路由。

---

## 12. D09 与 Rule Pack 的边界

```text
Rule Pack
= 判定结构化规则是否命中、生成 governed rule hits

D09 Policy
= 对 accepted evidence + rule hits 进行最终确定性 Risk Disposition 裁决
```

禁止：

```text
Rule hit 直接写 Risk State
Rule Engine 自己成为 final Risk Owner
D09 重新发明医学规则而不引用正式 Rule Release
```

---

## 13. D09 与 U04 的边界

```text
D09
→ Clinical Risk Disposition

U04
→ Safety Gate Decision
```

因此：

```text
HIGH_RISK != Safety Gate result
FAILED != Safety Gate result
NO_HIGH_RISK_SIGNAL != Safety Gate authorization
```

U03 只提交 Risk Assessment，后续由 U04 解释其 Safety Gate 含义。

---

## 14. Review / Approval Gate

任何 D09 Policy Release 在进入真实 U03 Clinical Implementation 前，至少需要：

```text
Medical Owner Review = APPROVED
Technical Review = APPROVED
Rule Release refs = VALID
Knowledge Release refs = VALID or explicitly NOT_REQUIRED
Evaluation refs = AVAILABLE
Scope = FROZEN
Version = FROZEN
```

开发者不得以 synthetic policy 替代生产医学 policy。

---

## 15. 当前状态

```text
D09 Policy Structural Schema = FROZEN
Concrete Clinical Policy Branches = NOT_AVAILABLE
Medical Priority / Precedence = NOT_AVAILABLE
Medical Owner Review = NOT_COMPLETE
Policy Release = NOT_PUBLISHED
Clinical Evaluation = NOT_COMPLETE
```

因此当前状态为：

```text
D / D09 Clinical Policy Table
= STRUCTURAL_SCHEMA_FROZEN
/ CLINICAL_POLICY_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```

这只代表结构准备完成，不代表 D09 临床策略已经可用于真实 Risk Decision。