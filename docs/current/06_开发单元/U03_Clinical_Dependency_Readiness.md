# U03 Clinical Dependency Readiness

> 阶段：U03 Clinical Dependency Completion / Readiness  
> 基线：`impl/u03-current-version-risk-assessment@b6913433b72a7156855db048f524efdd5175abd6`  
> 本文件判断是否可以进入真实 C02/D09 临床依赖实现；不构成 Implementation Authorization。

---

## 1. 已满足的工程前置

```text
U03 Engineering Governance Path = IMPLEMENTED / COMPONENT_VERIFIED
Foundation-1 capability authorization = AVAILABLE
minimal P04/P06 release binding = AVAILABLE
candidate-only C02 boundary = AVAILABLE
evidence acceptance boundary = AVAILABLE
D09 owner boundary = AVAILABLE
typed K09 proposal + P01 commit = AVAILABLE
P05 release-aware trace = AVAILABLE
failure semantics = AVAILABLE
```

这些前置足以承载后续真实临床内容，无需新增 Foundation。

---

## 2. Clinical Input Package 当前进度

```text
A Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ SOURCE_GROUNDED_CONTENT_DRAFT_v0.1_AVAILABLE
/ SOURCE_REVIEW_PASS_FOR_MEDICAL_OWNER_REVIEW
/ MEDICAL_OWNER_REVIEW_RECORD_v0.1_COMPLETE
/ A_APPROVE_2_REVISE_4
/ CONTENT_DRAFT_v0.2_REVISION_REQUIRED
/ NOT_APPROVED

B Evidence Catalog
= STRUCTURAL_SCHEMA_FROZEN
/ SOURCE_GROUNDED_CONTENT_DRAFT_v0.1_AVAILABLE
/ 11 CANDIDATE ENTRIES
/ SOURCE_REVIEW_PASS_FOR_MEDICAL_OWNER_REVIEW
/ MEDICAL_OWNER_REVIEW_RECORD_v0.1_COMPLETE
/ B_APPROVE_4_REVISE_6_NEED_MORE_SOURCE_1
/ CONTENT_DRAFT_v0.2_REVISION_REQUIRED
/ NOT_APPROVED

C Safety-critical Risk Rule Pack
= STRUCTURAL_SCHEMA_FROZEN / CLINICAL_RULE_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED

D D09 Clinical Policy Table
= STRUCTURAL_SCHEMA_FROZEN / CLINICAL_POLICY_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED

E Knowledge Release Manifest
= STRUCTURAL_SCHEMA_FROZEN / APPLICABILITY_ADJUDICATION_PENDING / CLINICAL_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED

F Risk EvalSet / Safety Suite
= STRUCTURAL_SCHEMA_FROZEN / GOLDEN_CASE_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / EVALUATION_OWNER_REVIEW_REQUIRED / NOT_REVIEW_READY
```

A/B v0.1 已完成 Medical Owner Review，但不能整包批准；下一动作是 A/B Content Draft v0.2 修订。C/D/E/F 仍未完成真实内容，本轮不授权进入 C。

---

## 3. A/B review 结果

当前审核工作材料：

```text
U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md
U03_Evidence_Catalog_Content_Draft_v0.1.md
U03_AB_Source_Review_v0.1.md
U03_AB_Medical_Review_Recommendation_v0.1.md
U03_AB_Medical_Owner_Review_Record_v0.1.md
U03_AB_Revision_Task_v0.2.md
```

条目计数以 Review Record 为准，不再使用 Recommendation v0.1 的 3/7 APPROVE 计数：

```text
A: APPROVE 2 / REVISE 4 / REJECT 0 / NEED_MORE_SOURCE 0
B: APPROVE 4 / REVISE 6 / REJECT 0 / NEED_MORE_SOURCE 1
Source-supported for review: A 6/6, B 11/11
Medical Owner Approval = NOT_COMPLETE
Production Eligibility = NO
```

主要待决问题：
- v0.1 正文仍保留病因命名与过宽 infection-context，必须先完成 v0.2 修订；
- 通用 new altered mental state 若要扩大为跨病种全局 red flag，须补更直接的通用急性病来源，否则收窄 scope；
- 成人来源不能扩展到儿科、妊娠/近期妊娠；
- 生命体征具体阈值留在 C Rule Pack，不由 B Catalog 承担；
- NICE/NHS 初始来源最终仍需根据目标地区做 localization adjudication。

---

## 4. Readiness Gate

### Gate A — Clinical Semantics Frozen

```text
CD-01 APPROVED
CD-02 APPROVED
```

当前：`NOT_PASSED`。A/B v0.1 review 已完成，但因 REVISE / NEED_MORE_SOURCE 未清零，仍未达到 semantics frozen。

### Gate B — Governed Content Ready

```text
CD-03 APPROVED
CD-04 initial release READY
CD-05 APPROVED
```

当前：`NOT_PASSED`

### Gate C — Independent Evaluation Ready

```text
CD-06 REVIEW_READY
```

当前：`NOT_PASSED`

### Gate D — Authorization

A/B/C 通过后仍需新的明确：

```text
Implementation Authorization
```

上一轮 U03-01～U03-11 的授权不自动覆盖 CD-01～CD-08。

---

## 5. 当前 Readiness 判定

```text
Engineering Prerequisites = PASS
Clinical Structural Definitions = COMPLETE_FOR_SCHEMA_LAYER
A/B Source-grounded Content = AVAILABLE / REVIEWED_FOR_SOURCE_CONSISTENCY
A/B Medical Owner Review Record v0.1 = COMPLETE
A/B Content Draft v0.2 Revision = REQUIRED / NOT_STARTED
Medical Owner Approval = NOT_COMPLETE
Governed Clinical Content = NOT_COMPLETE
Clinical Evaluation Content = NOT_COMPLETE
Independent Evaluation Readiness = NOT_READY
New Foundation = NOT_REQUIRED
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

总判定保持：

```text
U03 Clinical Dependency Readiness
= BLOCKED_BY_CLINICAL_INPUT_PACKAGE
```

---

## 6. 下一步

当前最合理的下一步是：

```text
Execute U03_AB_Revision_Task_v0.2.md
↓
produce A/B Content Draft v0.2
↓
re-review revised entries
↓
only approved B entries may enter C Rule Pack content design
```

不得进入 C/D/E/F 真实临床内容。不得把 Review Record 中的 APPROVE 条目直接提升为 Rule Pack 或生产规则。不得由工程侧自行把 A/B 标记为 APPROVED。
