# U03 Knowledge Release Content Draft v0.1

> 对象：KD-U03-01 Source-grounded Clinical Knowledge Release  
> 状态：`CONTENT_DRAFT_VERIFIED_FOR_SOURCE_METADATA / OWNER_ASSIGNMENT_PENDING / NOT_FROZEN / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
> 权威前置：`U03_Knowledge_Dependency_Applicability_Decision_v0.1.md` 已批准 KD-U03-01 = REQUIRED。  
> 本文件只建立最小、可重放的来源 release 对象；不新增医学指南、阈值、组合规则、D09 branch 或生产结论。

## 1. Release Identity

```text
knowledge_release_id = KR-U03-SOURCE-001
knowledge_domain = U03_SOURCE_GROUNDED_CLINICAL_REFERENCES
knowledge_version = 0.1.0-draft
release_status = DRAFT
contract_version = U03_KR_SCHEMA_V1
```

当前 identity 仅用于 draft/reference；在正式 review/freeze 前不得作为 production binding，也不得作为 C 的 frozen knowledge ref。

## 2. Release Role

本 release 只承载：

```text
source identity
source version / last-updated metadata
publication metadata where applicable
retrieval metadata
source type
locked clinical context
scope
provenance
review status
change history
```

明确不承载：

```text
executable threshold
combination logic
rule predicate
rule precedence
NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK mapping
D09 branch
Safety Gate decision
```

因此：

```text
E = source/version/scope/provenance/release
C = executable rule/threshold/combination
D = deterministic disposition
```

## 3. Source Registry Binding

本 draft 只绑定已经进入 A/B v0.1-v0.2 审核链的 source registry，不新增任何来源。

### SRC-NICE-SEPSIS-NG253

```text
source_reference_id = SRC-NICE-SEPSIS-NG253
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG253
canonical_url = https://www.nice.org.uk/guidance/ng253
source_version_anchor = NG253
publication_date = 2025-11-19
last_reviewed = 2025-12-05
retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = age >= 16 + suspected sepsis + source-supported community/custodial context
```

Current-use boundary:
- 仅支撑当前 A/B 已锁定的成人疑似脓毒症语义、证据与后续来源追踪；
- 不得被 C/D 扩写成全局生命体征来源；
- 当前 slice 继续排除妊娠/近期妊娠，并不得复用到儿科。

### SRC-NICE-NEURO-NG127

```text
source_reference_id = SRC-NICE-NEURO-NG127
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG127
canonical_url = https://www.nice.org.uk/guidance/ng127
source_version_anchor = NG127@last_updated_2023-10-02
publication_date = 2019-05-01
last_updated = 2023-10-02
retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = current U03 use limited to adult sudden focal neurological weakness/sensory abnormality and sudden speech/language disturbance already approved in A/B
```

Current-use boundary:
- 不把 NG127 全量内容引入当前 U03；
- 不覆盖儿科；
- 不扩展为全量神经分诊；
- C 只能消费 Gate A 已批准的相应 B evidence refs。

### SRC-NICE-ANAPHYLAXIS-NG258

```text
source_reference_id = SRC-NICE-ANAPHYLAXIS-NG258
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG258
canonical_url = https://www.nice.org.uk/guidance/ng258
source_version_anchor = NG258@2026-05-27
publication_date = 2026-05-27
last_updated = 2026-05-27
retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = rapidly developing suspected anaphylaxis with airway/breathing/circulation compromise; skin/mucosal features are common but not mandatory; current U03 slice remains adult-scoped
```

Current-use boundary:
- 不扩展为一般过敏反应分类；
- 非过敏性 ABC 受损不得自动归入该条；
- 当前 KR 不引入 NG258 的其他观察、治疗或出院规则。

### SRC-NHS-STROKE

```text
source_reference_id = SRC-NHS-STROKE
publisher = NHS
source_type = PATIENT_INFORMATION
canonical_url = https://www.nhs.uk/conditions/stroke/symptoms/
source_version_anchor = page_last_reviewed_2024-09-12
publication_date = NOT_APPLICABLE
publication_date_rationale = NHS page exposes page-last-reviewed metadata rather than a separate stable publication date used by this KR
page_last_reviewed = 2024-09-12
retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = FAST/stroke-warning patient-information support for sudden unilateral/focal weakness or sensory abnormality and sudden speech/language abnormality already approved in A/B
```

Authority boundary:
- 这是 NHS 患者信息页，不是专业卒中路径全文；
- 可支撑当前第一轮安全候选及 provenance；
- 不得单独升级为生产专业卒中规则来源；
- 不得因页面提到其他卒中症状而自动扩大当前 B catalog。

### SRC-NHS-CHEST

该 source registry entry 是复合绑定，正式 metadata 必须同时记录以下两个 NHS 患者信息页：

```text
source_reference_id = SRC-NHS-CHEST
publisher = NHS
source_type = PATIENT_INFORMATION_COMPOSITE_BINDING

component_1_name = Chest pain
component_1_url = https://www.nhs.uk/symptoms/chest-pain/
component_1_version_anchor = page_last_reviewed_2023-08-08
component_1_publication_date = NOT_APPLICABLE
component_1_publication_date_rationale = NHS page exposes page-last-reviewed metadata rather than a separate stable publication date used by this KR
component_1_page_last_reviewed = 2023-08-08

component_2_name = Heart attack
component_2_url = https://www.nhs.uk/conditions/heart-attack/
component_2_version_anchor = page_last_reviewed_2026-03-31
component_2_publication_date = NOT_APPLICABLE
component_2_publication_date_rationale = NHS page exposes page-last-reviewed metadata rather than a separate stable publication date used by this KR
component_2_page_last_reviewed = 2026-03-31

retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = high-risk chest-pain pattern already approved in A/B; source names remain patient-information provenance and do not establish a complete professional ACS pathway
```

Authority boundary:
- 两页共同构成当前 `SRC-NHS-CHEST` provenance；
- 不得只绑定其中一页却继续声称复合来源完整；
- 不得把该复合患者页绑定视为中国生产 ACS pathway；
- C 若形成 executable rule，必须继续受 Gate A 的中性胸痛语义和后续专业/本地化门禁约束。

### SRC-NHS-DYSPNOEA

```text
source_reference_id = SRC-NHS-DYSPNOEA
publisher = NHS
source_type = PATIENT_INFORMATION
canonical_url = https://www.nhs.uk/symptoms/shortness-of-breath/
source_version_anchor = page_last_reviewed_2024-01-30
publication_date = NOT_APPLICABLE
publication_date_rationale = NHS page exposes page-last-reviewed metadata rather than a separate stable publication date used by this KR
page_last_reviewed = 2024-01-30
retrieved_at = 2026-09-15
language_as_published = English
locked_clinical_context = severe-dyspnoea emergency-warning context only; appearance and sudden confusion may be used only within the source-supported severe-breathlessness emergency-warning context already locked in A/B
```

Authority boundary:
- 不能把 pale/blue/grey appearance 或 sudden confusion 从该页抽离后升级成跨病种全局 RED_FLAG 来源；
- 不能把 NHS 患者页当成专业呼吸/急诊完整路径；
- 远程未观察到这些表现仍不得作为充分排除依据。

## 4. Source-type Authority Boundary

本 KR 明确区分：

```text
NICE NG253 / NG127 / NG258 = CLINICAL_GUIDELINE
NHS Stroke / Chest / Heart attack / Shortness of breath = PATIENT_INFORMATION
```

必须保持：

```text
source exists
!= same authority class
```

NHS 患者信息页在当前 slice 中可作为来源锁定的安全候选/provenance 支撑，但不得单独承担生产专业临床规则的最终依据。后续 C/D review 必须保留 `source_type`，禁止把 patient-information 与 clinical-guideline 当成无差别同级来源。

## 5. Release Scope

```text
population_scope:
  current U03 adult slice only
  sepsis-specific content: age >= 16 where NG253 governs
  source-specific adult locks follow Gate A definitions

region_scope:
  INTERNATIONAL_REFERENCE_ONLY
  NOT_CHINA_PRODUCTION_LOCALIZED

language_scope:
  source language as published = English
  system-facing semantic normalization governed separately

channel_scope:
  remote/community initial consultation context only where explicitly supported by the locked source use

clinical_context_scope:
  limited to Gate A approved A/B v0.2 source-locked semantics

exclusions:
  pediatrics
  pregnancy / puerperium
  psychiatric emergency full coverage
  trauma full triage
  poisoning full triage
  unreviewed disease-specific red flags
  China production localization
```

本 scope 不得被 C/D 扩大。若后续任何 Rule/Policy 希望扩大 source use 或 population/context scope，必须重新进入 source review / Medical Owner review。

## 6. Provenance Binding

本 release 的 provenance 至少引用：

```text
U03_Clinical_Risk_Semantics_Content_Draft_v0.2.md
U03_Evidence_Catalog_Content_Draft_v0.2.md
U03_AB_Medical_Owner_Review_Record_v0.1.md
U03_AB_Medical_Owner_Review_Record_v0.2.md
U03_AB_Source_Review_v0.1.md
U03_Knowledge_Dependency_Applicability_Decision_v0.1.md
```

这些文档证明：
- 当前 source registry 是如何进入 A/B 审核链的；
- 哪些语义被 source lock；
- 哪些 scope 被明确禁止扩大；
- 为什么 KD-U03-01 在本 slice 中为 REQUIRED。

## 7. Source Metadata Verification Status

```text
6 source_reference_ids = CONFIRMED
canonical official URL(s) = VERIFIED
publisher = VERIFIED
source_type = VERIFIED
locked_clinical_context = REVIEW-DERIVED_AND_APPLIED
version/last-updated anchor = VERIFIED
publication_date = VERIFIED where exposed; otherwise NOT_APPLICABLE_WITH_RATIONALE
retrieved_at = 2026-09-15 / ACTUAL_OFFICIAL_SOURCE_VERIFICATION_DATE
language_as_published = English / VERIFIED
```

本轮 source-metadata 核验没有新增医学来源或新临床结论。

## 8. Curation / Review

```text
curation_method = bind_only_already_reviewed_A_B_source_registry
curation_owner = PENDING_ASSIGNMENT
clinical_review_owner = PENDING_ASSIGNMENT
technical_review_owner = PENDING_ASSIGNMENT
clinical_review_status = PENDING_KR_CONTENT_REVIEW
technical_review_status = PENDING_KR_CONTENT_REVIEW
release_approval_status = NOT_APPROVED
```

由于 owner 仍未分配：

```text
KR Review Ready = NO
KR Frozen Candidate = NO
```

当前不允许通过本 draft：
- 加入新的 guideline/source；
- 将来源网页中的新结论带入 A/B；
- 把 source text 直接转成 executable rule；
- 补写 C 阈值或 D09 branch。

## 9. Effective Time / Lifecycle

```text
effective_from = NOT_SET
effective_until = NOT_SET
supersedes_refs = []
rollback_target_ref = NOT_SET
published_at = NOT_SET
```

正式 frozen candidate 前至少必须冻结：

```text
knowledge_release_id
knowledge_version
source metadata
locked source context
scope
effective_from
curation owner
clinical review owner
technical review owner
review status
approval refs
```

禁止使用 `latest` 等 mutable alias 作为正式 clinical binding。

## 10. Binding Intent

未来若本 KR 通过 review/freeze，C Rule Release 才允许显式引用：

```text
knowledge_release_ref = KR-U03-SOURCE-001@<frozen_version>
```

D09 Policy 可直接或通过 C 间接引用该 release 作为 provenance。

但当前：

```text
KR-U03-SOURCE-001@0.1.0-draft
= NOT_FROZEN
= NOT_REVIEW_READY
= NOT_VALID_FOR_C_FROZEN_REF
= NOT_VALID_FOR_PRODUCTION_BINDING
```

## 11. 完成条件

本 KR draft 要达到 `REVIEW_READY`，至少需要：

```text
6 source identities confirmed = COMPLETE
source metadata verification = COMPLETE
locked source context = COMPLETE_FOR_CURRENT_SLICE
scope review = CONTENT_AVAILABLE / FORMAL_REVIEW_PENDING
curation owner assigned = PENDING
clinical review owner assigned = PENDING
technical review owner assigned = PENDING
provenance refs validated = CONTENT_AVAILABLE / FORMAL_REVIEW_PENDING
no new clinical content introduced = PASS
```

因此当前仍不能标记 `REVIEW_READY`。

达到 `REVIEW_READY` 仍不等于 `FROZEN`、`PUBLISHED` 或 production-authorized。

## 12. 当前状态

```text
KD-U03-01 Applicability = REQUIRED / APPROVED
KD-U03-01 Knowledge Release Content Draft = AVAILABLE
Source Registry Bound = 6 EXISTING A/B SOURCES ONLY
Source Metadata Verification = COMPLETE_FOR_CURRENT_6_SOURCES
Locked Clinical Context = APPLIED
Knowledge Release Owner Assignment = NOT_COMPLETE
Knowledge Release Review = NOT_COMPLETE
Knowledge Release Freeze = NOT_COMPLETE
Knowledge Release Publication = NOT_COMPLETE
CD-04 = NOT_PASSED
C real clinical rule content = BLOCKED
D real clinical policy content = BLOCKED
Gate B = NOT_PASSED
```

下一步只允许完成 owner assignment 与 KR content/governance review，之后再判断能否冻结 `KR-U03-SOURCE-001` candidate。不得开始 C 的真实医学阈值、组合规则或 D09 branch。
