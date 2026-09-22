# U03 Safety-critical Risk Rule Pack Schema

> 阶段：U03 Clinical Dependency Completion / C 规则包结构冻结  
> 目的：冻结 Safety-critical Risk Rule Pack 的对象结构、版本治理、作用域、优先级、冲突处理与发布边界。  
> 本文件不提供任何具体医学阈值、红旗内容、诊断规则或生产判定逻辑。

---

## 1. 角色边界

必须保持：

```text
Evidence Catalog != Rule Pack
Rule Pack != D09 Clinical Policy
Rule Hit != Clinical Risk Disposition
Rule Pack != Clinical Truth Owner
```

Rule Pack 的职责是：

```text
结构化规则定义
+
可版本化发布
+
可追溯来源
+
可测试输入/输出
+
显式适用范围
+
显式优先级/冲突关系
```

Rule Pack 不直接提交 Clinical State，也不拥有 U04 Safety Gate。

---

## 2. Rule Definition 最小结构

每一条 rule 至少包含：

```text
rule_id
rule_version
rule_set_id
canonical_name
description
status

input_schema_ref
predicate_schema_ref
required_evidence_refs[]
optional_evidence_refs[]

priority
conflict_group
precedence_refs[]
mutual_exclusion_refs[]

population_scope
region_scope
language_scope
channel_scope

source_reference_ids[]
knowledge_release_refs[]
provenance_refs[]
review_owner
review_status

effective_from
effective_until
supersedes_refs[]
rollback_target_ref
```

其中任何字段都不得由 application service 隐式补全生产医学含义。

---

## 3. Predicate 结构

Rule predicate 必须为结构化、可测试对象，不得仅以自由文本 Prompt 作为唯一正式定义。

结构层至少支持：

```text
predicate_id
operand_refs[]
operator_ref
comparison_value_ref
missingness_policy_ref
uncertainty_policy_ref
conflict_policy_ref
```

具体 operator、阈值和医学语义必须由医学 Owner 与正式来源提供。

---

## 4. Rule Outcome 结构

规则输出只表达受治理的 rule-level signal，不直接表达最终 Clinical Risk Disposition。

结构至少包含：

```text
rule_result_id
rule_ref
matched
business_status
reason_code
supporting_evidence_refs[]
limitation_refs[]
uncertainty_refs[]
clinical_state_version
rule_release_ref
knowledge_release_refs[]
```

必须保持：

```text
Rule matched
!=
HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL
```

最终 disposition 仍由 D09 决定。

---

## 5. Rule Set / Release 结构

Safety-critical Risk Rule Pack 作为发布单元至少包含：

```text
rule_release_id
rule_set_id
release_version
status
contract_version
scope_version

rule_refs[]
knowledge_release_refs[]
evaluation_refs[]
source_reference_ids[]
provenance_refs[]

population_scope
region_scope
language_scope
channel_scope

effective_from
effective_until
supersedes_refs[]
rollback_target_ref

clinical_review_owner
technical_review_owner
release_approval_status
```

禁止 runtime 自动选择“最新规则”；必须通过正式 binding/release ref 使用明确版本。

---

## 6. 发布生命周期

推荐结构状态：

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
DEPRECATED != WITHDRAWN
RETIRED != DELETED
```

只有满足项目治理要求、处于允许状态且当前 effective/scope 命中的 release，才能进入正式受治理调用链。

---

## 7. 优先级与冲突结构

本文件只冻结“必须显式表达”，不定义任何医学优先级。

至少要求：

```text
priority
conflict_group
precedence_refs[]
mutual_exclusion_refs[]
conflict_resolution_policy_ref
```

具体哪条规则优先、哪些组合互斥、冲突如何医学裁决，必须由后续 D09 Clinical Policy / Medical Owner 明确定义。

不得：

```text
代码顺序 = 医学优先级
文件顺序 = 冲突裁决
首次命中 = 默认最终结论
```

---

## 8. Missing / Unknown / Conflict 边界

Rule Pack 必须引用显式的：

```text
missingness_policy_ref
uncertainty_policy_ref
conflict_policy_ref
```

并继承 Clinical Risk Semantics 的结构不变量：

```text
UNKNOWN != NO
UNMEASURED != NORMAL
AMBIGUOUS != NEGATIVE
CONFLICTING != NEGATIVE
FAILED != NO_HIGH_RISK_SIGNAL
```

具体医学处理仍由受审 policy 定义。

---

## 9. Currentness 与版本绑定

任何正式 rule execution 至少绑定：

```text
consultation_id
clinical_state_version
rule_release_ref
knowledge_release_refs[]
capability_binding_ref
contract_version
scope_version
```

禁止：

```text
在旧 Clinical State Version 上得到的 rule result
→ 无校验继续作为新版本 current input
```

上游事实或依赖版本变化后，旧结果必须通过既有失效/currentness 机制被重新判断。

---

## 10. Evidence Catalog 依赖

Rule 只能引用已经进入受治理 Evidence Catalog 的 evidence definition。

```text
rule.required_evidence_refs[]
→ Evidence Catalog release 中存在
→ version/scope/currentness 可解析
→ 才能进入正式执行
```

Rule Pack 不得自己偷偷定义第二套 evidence taxonomy。

---

## 11. Eval 依赖

每个待发布 rule / release 至少关联：

```text
evaluation_refs[]
```

这些引用必须指向独立于实现代码的测试/评估资产。

结构上至少需要覆盖：

```text
positive fixtures
negative fixtures
missing/unknown fixtures
conflict fixtures
scope mismatch fixtures
version mismatch fixtures
release unavailable fixtures
```

具体临床案例与期望值不在本文件定义。

---

## 12. 当前状态

```text
C / Safety-critical Risk Rule Pack
= STRUCTURAL_SCHEMA_FROZEN
/ CLINICAL_RULE_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```

尚未提供：

```text
真实医学 rule entries
真实 thresholds
真实 precedence / conflict resolution content
生产 release
Medical Owner approval
Clinical Eval evidence
```

因此本文件不能单独使 CD-03 或 U03 Clinical Dependency Readiness 通过。
