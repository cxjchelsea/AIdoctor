# U03 Evidence Catalog Schema

> Clinical Input Package：B / Evidence Catalog  
> 状态：STRUCTURAL_SCHEMA_FROZEN / CLINICAL_CONTENT_PENDING / NOT_APPROVED  
> 本文件只定义治理结构，不包含任何具体医学条目、阈值、规则或风险结论。

## 1. 目标

Evidence Catalog 用于登记后续医学 Owner 审核后的风险相关证据定义，使每一条证据都具备稳定身份、版本、来源、适用范围、审核状态和生命周期。

必须保持：

```text
Evidence Definition != Runtime Observation
Evidence Candidate != Accepted Evidence
Accepted Evidence != Clinical Risk Disposition
Evidence Catalog != Rule Pack
```

## 2. Catalog Category

结构层保留以下分类标识：

```text
RED_FLAG
MUST_NOT_MISS
VITAL_SIGN_SAFETY_SIGNAL
RISK_FACTOR
COMBINATION_SIGNAL
```

这些只是分类名称，不代表任何具体医学条目已经批准。

## 3. Evidence Definition Schema

每条 definition 至少包含：

```text
evidence_id
evidence_version
canonical_name
category
clinical_definition
required_input_fields[]
accepted_source_types[]
minimum_evidence_requirement
value_semantics_allowed[]
uncertainty_handling
ambiguity_handling
conflict_handling
missingness_handling
population_scope
region_scope
language_scope
channel_scope
source_reference_ids[]
knowledge_release_refs[]
owner
review_status
reviewed_at
effective_from
effective_until
supersedes_ref
```

## 4. 审核状态

统一使用：

```text
DRAFT
MEDICAL_REVIEW
APPROVED
DEPRECATED
WITHDRAWN
RETIRED
```

必须保持：

```text
DRAFT != APPROVED
MEDICAL_REVIEW != APPROVED
WITHDRAWN != ACTIVE
RETIRED != DELETED
```

## 5. Source Type

沿用 K03：

```text
PATIENT_REPORTED
EXTERNAL_MEASUREMENT
OCR_EXTRACTED
MODEL_INFERRED
RULE_DERIVED
CLINICIAN_CONFIRMED
```

每条 definition 必须显式声明 `accepted_source_types[]`。

禁止通过 Catalog 把一种来源静默重标记为另一种来源。

## 6. Value 与 Lifecycle 分离

沿用 K03：

Fact value：

```text
YES
NO
UNKNOWN
UNMEASURED
NOT_ASKED
NOT_APPLICABLE
```

Observation lifecycle：

```text
EXTRACTED
NORMALIZED
CONFIRMED
UNCERTAIN
CONTRADICTED
INVALIDATED
```

两者不得共用一个字段。

## 7. Runtime Evidence 状态

结构上允许：

```text
CANDIDATE
ACCEPTED
REJECTED
AMBIGUOUS
CONFLICTING
STALE
INVALIDATED
```

这些状态仅描述证据在 U03 证据处理链中的治理状态，不代表正式 Risk Disposition。

## 8. Version / Currentness

运行时 evidence 至少关联：

```text
consultation_id
clinical_state_version
source_fact_refs[]
capability_binding_ref
evidence_definition_id
evidence_definition_version
rule_release_ref (if applicable)
knowledge_release_ref (if applicable)
```

旧 Clinical State Version 产生的 evidence 不得在未重新评估时自动成为新版本 current evidence。

## 9. 与 Rule Pack / D09 的边界

Evidence Catalog 负责：

```text
证据身份
定义
输入要求
来源要求
scope
版本
provenance
审核状态
```

Rule Pack 负责后续组合/规则语义。

D09 负责最终 Clinical Risk Disposition。

因此 Catalog entry 本身不得直接承担最终业务裁决所有权。

## 10. Entry Template

```yaml
evidence_id: TBD
evidence_version: TBD
canonical_name: TBD
category: TBD
clinical_definition: TBD_BY_MEDICAL_OWNER
required_input_fields: []
accepted_source_types: []
minimum_evidence_requirement: TBD_BY_MEDICAL_OWNER
value_semantics_allowed: []
uncertainty_handling: TBD_BY_MEDICAL_OWNER
ambiguity_handling: TBD_BY_MEDICAL_OWNER
conflict_handling: TBD_BY_MEDICAL_OWNER
missingness_handling: TBD_BY_MEDICAL_OWNER
population_scope: TBD
region_scope: TBD
language_scope: TBD
channel_scope: TBD
source_reference_ids: []
knowledge_release_refs: []
owner: TBD_MEDICAL_OWNER
review_status: DRAFT
reviewed_at: null
effective_from: null
effective_until: null
supersedes_ref: null
```

## 11. Catalog Release

完整 Catalog 必须以版本化 release 存在：

```text
evidence_catalog_release_id
release_version
status
entry_refs[]
scope
source_reference_ids[]
review_owner
review_status
effective_from
effective_until
supersedes
rollback_ref
```

只有经过正式审核的 release 才能成为真实 C02 / Rule Pack 的正式依赖。

## 12. 当前缺口

```text
Concrete Evidence Entries = NOT_AVAILABLE
Medical Source Mapping = NOT_AVAILABLE
Medical Review = NOT_STARTED
Catalog Approval = NOT_APPROVED
```

因此当前状态为：

```text
B Evidence Catalog
= STRUCTURAL_SCHEMA_FROZEN
/ CLINICAL_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```
