# U03 Clinical Input Package Specification

> 目的：定义医学侧必须提供给 U03 的最小、可审核、可版本化输入包。  
> 本文件定义结构，不提供任何具体医学阈值、红旗规则或风险判定内容。

---

## 1. Clinical Input Package 总体组成

```text
A Clinical Risk Semantics
B Evidence Catalog
C Safety-critical Risk Rule Pack
D D09 Clinical Policy Table
E Knowledge Release Manifest
F Risk EvalSet / Safety Suite
```

当前 A/B/C 已完成结构层准备，但均未获得医学 Owner 批准，也没有形成 production clinical content。

---

## 2. A — Clinical Risk Semantics

状态：

```text
STRUCTURAL_SEMANTICS_FROZEN
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

文件：`U03_Clinical_Risk_Semantics.md`

负责冻结风险证据、状态语义、owner/currentness/failure 等基础语义；具体医学内容仍需审核。

---

## 3. B — Evidence Catalog

状态：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_CONTENT_PENDING
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

文件：`U03_Evidence_Catalog_Schema.md`

每个正式 evidence definition 至少需要：

```text
evidence_id
evidence_version
canonical_name
category
clinical_definition
required_input_fields
accepted_source_types
minimum_evidence_requirement
uncertainty/ambiguity/conflict/missingness policy refs
population/region/language/channel scope
source_reference_ids
provenance_refs
knowledge_release_refs
owner
review_status
effective_from/effective_until
```

具体 evidence entries 必须后续由可追溯医学来源和 Medical Owner 提供/审核。

---

## 4. C — Safety-critical Risk Rule Pack

状态：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_RULE_CONTENT_PENDING
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

文件：`U03_Safety_Critical_Risk_Rule_Pack_Schema.md`

每条正式 rule 至少需要：

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
scope fields
source_reference_ids[]
knowledge_release_refs[]
provenance_refs[]
review_owner
review_status
effective_from/effective_until
supersedes_refs[]
rollback_target_ref
```

Rule Pack release 至少需要：

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
source/provenance refs
scope
effective window
supersedes/rollback refs
clinical_review_owner
technical_review_owner
release_approval_status
```

具体医学 rule entries、thresholds、precedence 与冲突裁决内容不得由开发侧自行补齐。

---

## 5. D — D09 Clinical Policy Table

待建设。

目标：定义 accepted evidence / governed rule results 到正式 Clinical Risk Disposition 的确定性 policy 结构。

允许 outcome vocabulary 保持：

```text
VALID + NO_HIGH_RISK_SIGNAL
VALID + CAUTION
VALID + HIGH_RISK
FAILED
```

具体 policy branch / priority / precedence / failure handling 需要受审内容。

---

## 6. E — Knowledge Release Manifest

待建设/裁定是否需要具体独立 release。

至少应支持：

```text
knowledge_release_id
version
status
content_scope
source_reference_ids
provenance_refs
review_owner
review_status
scope
effective_from/effective_until
supersedes
rollback_ref
```

---

## 7. F — Risk EvalSet / Safety Suite

待建设。

至少需要独立于实现代码的 fixtures/cases，并绑定：

```text
case_id
clinical_state_version fixture
input facts/assertions
expected evidence/rule/policy outcomes
must_not_output assertions
rule release
knowledge release
source/rationale refs
review_owner
```

---

## 8. 完整输入包验收条件

进入 CD-07 Implementation 之前至少满足：

```text
A = APPROVED
B = APPROVED
C = APPROVED + initial release READY
D = APPROVED
E = APPROVED or explicitly NOT_REQUIRED
F = REVIEW_READY
```

并且：

- 每个医学判断有 owner；
- 每个安全关键内容有可追溯来源；
- release 有 version/scope/effective time；
- EvalSet 与实现代码独立；
- 不存在开发自行补齐的未审医学规则。

---

## 9. 当前状态

```text
A = STRUCTURAL_SEMANTICS_FROZEN / NOT_APPROVED
B = STRUCTURAL_SCHEMA_FROZEN / CONTENT_PENDING / NOT_APPROVED
C = STRUCTURAL_SCHEMA_FROZEN / CONTENT_PENDING / NOT_APPROVED
D = NOT_STARTED
E = NOT_STARTED / NOT_ADJUDICATED
F = NOT_STARTED

Clinical Input Package = NOT_COMPLETE
Medical Owner Approval = NOT_COMPLETE
CD-07 Implementation Readiness = BLOCKED
```
