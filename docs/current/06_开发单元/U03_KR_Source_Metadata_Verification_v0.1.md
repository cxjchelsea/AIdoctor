# U03 KR Source Metadata Verification v0.1

> 对象：`KR-U03-SOURCE-001@0.1.0-draft` 的 6 个既有 A/B source registry 条目。  
> 状态：`SOURCE_METADATA_VERIFICATION_COMPLETE_FOR_CURRENT_6_SOURCES / NO_NEW_CLINICAL_CONTENT / NOT_A_KR_APPROVAL`  
> 核验日期：`2026-09-15`。  
> 本文件只记录官方来源 metadata 与既有 Medical Owner scope lock 的一致性，不新增指南、不新增医学结论、不批准 C/D 内容。

## 1. 核验规则

每个 source 至少核验：

```text
canonical_url
publisher
source_type
locked_clinical_context
source_version or last_updated anchor
publication_date where exposed
retrieved_at
language_as_published
```

若官方患者信息页未提供独立 publication date，则允许：

```text
publication_date = NOT_APPLICABLE
+
page_last_reviewed as replay/version anchor
+
rationale recorded
```

## 2. NICE sources

### SRC-NICE-SEPSIS-NG253

```text
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG253
canonical_url = https://www.nice.org.uk/guidance/ng253
publication_date = 2025-11-19
last_reviewed = 2025-12-05
retrieved_at = 2026-09-15
language_as_published = English
locked_context = age >= 16 + suspected sepsis + source-supported community/custodial context; not a global vital-sign source
```

### SRC-NICE-NEURO-NG127

```text
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG127
canonical_url = https://www.nice.org.uk/guidance/ng127
publication_date = 2019-05-01
last_updated = 2023-10-02
retrieved_at = 2026-09-15
language_as_published = English
locked_context = current U03 use limited to adult sudden focal neurological weakness/sensory abnormality and sudden speech/language disturbance already approved in A/B
```

### SRC-NICE-ANAPHYLAXIS-NG258

```text
publisher = NICE
source_type = CLINICAL_GUIDELINE
guideline_id = NG258
canonical_url = https://www.nice.org.uk/guidance/ng258
publication_date = 2026-05-27
last_reviewed = 2026-06-04
last_updated = 2026-06-04
retrieved_at = 2026-09-15
language_as_published = English
locked_context = rapidly developing suspected anaphylaxis with airway/breathing/circulation compromise; skin/mucosal features common but not mandatory; current U03 slice remains adult-scoped
```

## 3. NHS patient-information sources

### SRC-NHS-STROKE

```text
publisher = NHS
source_type = PATIENT_INFORMATION
canonical_url = https://www.nhs.uk/conditions/stroke/symptoms/
publication_date = NOT_APPLICABLE
publication_date_rationale = official page exposes page-last-reviewed metadata used as the replay anchor
page_last_reviewed = 2024-09-12
retrieved_at = 2026-09-15
language_as_published = English
locked_context = FAST/stroke-warning support for the already approved sudden unilateral/focal weakness/sensory and speech/language evidence only
```

### SRC-NHS-CHEST

Current source registry identity is a composite binding:

```text
publisher = NHS
source_type = PATIENT_INFORMATION_COMPOSITE_BINDING

component_1 = Chest pain
component_1_url = https://www.nhs.uk/symptoms/chest-pain/
component_1_publication_date = NOT_APPLICABLE
component_1_page_last_reviewed = 2023-08-08

component_2 = Heart attack
component_2_url = https://www.nhs.uk/conditions/heart-attack/
component_2_publication_date = NOT_APPLICABLE
component_2_page_last_reviewed = 2026-03-31

retrieved_at = 2026-09-15
language_as_published = English
locked_context = high-risk chest-pain pattern already approved in A/B; patient-information provenance only, not a complete professional ACS pathway
```

### SRC-NHS-DYSPNOEA

```text
publisher = NHS
source_type = PATIENT_INFORMATION
canonical_url = https://www.nhs.uk/symptoms/shortness-of-breath/
publication_date = NOT_APPLICABLE
publication_date_rationale = official page exposes page-last-reviewed metadata used as the replay anchor
page_last_reviewed = 2024-01-30
retrieved_at = 2026-09-15
language_as_published = English
locked_context = severe-dyspnoea emergency-warning context only; appearance and sudden confusion cannot be detached and promoted into global red-flag provenance
```

## 4. Authority-class boundary

```text
NICE guideline != NHS patient information
```

Current KR must preserve source type. NHS patient-information pages may support current safety-candidate provenance but cannot by themselves become the final professional production-rule authority.

## 5. Result

```text
6 source_reference_ids = CONFIRMED
canonical URL(s) = VERIFIED
publisher = VERIFIED
source_type = VERIFIED
version/last-updated anchor = VERIFIED
publication_date = VERIFIED_OR_NOT_APPLICABLE_WITH_RATIONALE
retrieved_at = RECORDED
locked_clinical_context = APPLIED_FROM_MEDICAL_OWNER_REVIEW
new source added = NO
new clinical conclusion added = NO
```

This verification does not assign KR owners, does not approve/freeze `KR-U03-SOURCE-001`, does not authorize C/D content, and does not make the release production-valid.
