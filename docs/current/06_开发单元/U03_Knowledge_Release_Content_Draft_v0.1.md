# U03 Knowledge Release Content Draft v0.1

> 对象：KD-U03-01 Source-grounded Clinical Knowledge Release  
> 状态：`CONTENT_DRAFT_AVAILABLE / SOURCE_METADATA_VERIFICATION_PENDING / NOT_PUBLISHED / NOT_FOR_PRODUCTION`  
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

当前 identity 仅用于 draft/reference；在正式 review/freeze 前不得作为 production binding。

## 2. Release Role

本 release 只承载：

```text
source identity
source version metadata where verified
publication metadata where verified
retrieval metadata where verified
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

| source_reference_id | source identity | current use | source version | publication date | retrieved_at | verification |
|---|---|---|---|---|---|---|
| SRC-NICE-SEPSIS-NG253 | NICE NG253 — Suspected sepsis in people aged 16 or over | suspected-sepsis source-locked semantics / evidence provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |
| SRC-NICE-NEURO-NG127 | NICE NG127 — Suspected neurological conditions | focal neurological / speech-language evidence provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |
| SRC-NICE-ANAPHYLAXIS-NG258 | NICE NG258 — Anaphylaxis | allergy/ABC evidence provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |
| SRC-NHS-STROKE | NHS — Symptoms of a stroke | FAST / stroke-warning provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |
| SRC-NHS-CHEST | NHS — Chest pain / Heart attack | high-risk chest-pain provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |
| SRC-NHS-DYSPNOEA | NHS — Shortness of breath | severe dyspnoea / appearance / confusion provenance | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | `PENDING_VERIFICATION` | REQUIRED |

`PENDING_VERIFICATION` 不得在正式 release 中保留为已完成状态。正式 publish 前必须由 source review 补齐可重放 metadata，或明确记录某字段对该 source 不适用及其理由。

## 4. Scope

```text
population_scope:
  adult / age constraints follow each bound A/B source-locked item
  sepsis-specific content: age >= 16 where NG253 is the governing source

region_scope:
  INTERNATIONAL_REFERENCE_ONLY
  NOT_CHINA_PRODUCTION_LOCALIZED

language_scope:
  source language as published
  system-facing semantic normalization governed separately

channel_scope:
  remote/community initial consultation context where explicitly supported

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

本 scope 不得被 C/D 扩大。若后续任何 Rule/Policy 希望扩大 scope，必须重新进入 source review / Medical Owner review。

## 5. Provenance Binding

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

## 6. Curation / Review

```text
curation_method = bind_only_already_reviewed_A_B_source_registry
curation_owner = PENDING_ASSIGNMENT
clinical_review_status = PENDING_KR_CONTENT_REVIEW
technical_review_status = PENDING_KR_CONTENT_REVIEW
release_approval_status = NOT_APPROVED
```

当前不允许通过本 draft：
- 加入新的 guideline/source；
- 将来源网页中的新结论带入 A/B；
- 把 source text 直接转成 executable rule；
- 补写 C 阈值或 D09 branch。

## 7. Effective Time / Lifecycle

```text
effective_from = NOT_SET
 effective_until = NOT_SET
supersedes_refs = []
rollback_target_ref = NOT_SET
published_at = NOT_SET
```

正式 release 前必须冻结：

```text
knowledge_release_id
knowledge_version
source metadata
scope
effective_from
review status
approval refs
```

禁止使用 `latest` 等 mutable alias 作为正式 clinical binding。

## 8. Binding Intent

未来若本 KR 通过 review/freeze，C Rule Release 必须显式引用：

```text
knowledge_release_ref = KR-U03-SOURCE-001@<frozen_version>
```

D09 Policy 可直接或通过 C 间接引用该 release 作为 provenance。

但当前：

```text
KR-U03-SOURCE-001@0.1.0-draft
= NOT_VALID_FOR_PRODUCTION_BINDING
```

## 9. 完成条件

本 KR draft 要达到 `REVIEW_READY`，至少需要：

```text
6 source identities confirmed
source version metadata verified where applicable
publication metadata verified where applicable
retrieved_at recorded
scope reviewed
curation owner assigned
clinical review owner assigned
technical review owner assigned
provenance refs validated
no new clinical content introduced
```

达到 `REVIEW_READY` 仍不等于 `PUBLISHED`。

## 10. 当前状态

```text
KD-U03-01 Applicability = REQUIRED / APPROVED
KD-U03-01 Knowledge Release Content Draft = AVAILABLE
Source Registry Bound = 6 EXISTING A/B SOURCES ONLY
Source Metadata Verification = NOT_COMPLETE
Knowledge Release Review = NOT_COMPLETE
Knowledge Release Publication = NOT_COMPLETE
CD-04 = NOT_PASSED
C real clinical rule content = BLOCKED
D real clinical policy content = BLOCKED
Gate B = NOT_PASSED
```

下一步只能对本最小 KR 对象做 source-metadata / scope / governance review，并补齐可重放 metadata。不得开始 C 的真实医学阈值、组合规则或 D09 branch。