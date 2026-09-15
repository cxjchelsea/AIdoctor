# U03 Clinical Content Governance Status

> 角色：PR #88 内容治理索引。  
> 本文件只声明文档成熟度与权威边界，不包含临床规则、阈值或医学决策。  
> 基线：U03 engineering governance slice 已合并至 `main`；Clinical Dependency Completion 尚未完成。

## 1. 总体原则

`docs/current` 表示当前项目正在采用的设计与治理资料，但并不自动把其中标记为 Draft / Review Pending 的医学内容提升为已批准 Clinical Truth。

必须区分：

```text
STRUCTURAL / GOVERNANCE AUTHORITY
!=
CLINICAL CONTENT APPROVAL
```

任何文件只有在其自身状态明确达到相应 review / approval gate 后，才可承担对应层级的权威性。

## 2. 可作为当前结构/治理基线的文件

以下文件用于定义治理结构、schema、边界、readiness 与 review gate；它们不等于真实临床内容已批准：

```text
U03_Clinical_Dependency_Assessment.md
U03_Clinical_Dependency_Readiness.md
U03_Clinical_Input_Package_Spec.md
U03_Clinical_Risk_Semantics.md
U03_Evidence_Catalog_Schema.md
U03_Safety_Critical_Risk_Rule_Pack_Schema.md
U03_D09_Clinical_Policy_Table_Schema.md
U03_Knowledge_Release_Manifest_Schema.md
U03_Risk_EvalSet_Safety_Suite_Schema.md
```

治理状态：

```text
STRUCTURAL / GOVERNANCE REFERENCE = CURRENT
CLINICAL CONTENT APPROVAL = NOT_IMPLIED
PRODUCTION AUTHORIZATION = NOT_IMPLIED
```

## 3. 非权威临床内容 / 审核工作材料

以下文件是 source-grounded draft、历史审稿意见、本轮 review record、修订任务或状态快照，只用于 A/B 审核与 v0.2 修订；不得被实现、测试或 Runtime 当成已批准生产临床内容：

```text
U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md
U03_Evidence_Catalog_Content_Draft_v0.1.md
U03_AB_Source_Review_v0.1.md
U03_AB_Medical_Review_Recommendation_v0.1.md
U03_AB_Medical_Owner_Review_Record_v0.1.md
U03_AB_Revision_Task_v0.2.md
U03_AB_Medical_Review_Status.md
U03_AB_Revision_Status_v0.2.md
```

统一权威状态：

```text
REVIEW_WORKING_MATERIAL = YES
CLINICAL_AUTHORITY = NO
MEDICAL_OWNER_APPROVAL = NOT_COMPLETE
PRODUCTION_ELIGIBILITY = NO
RUNTIME_CONSUMPTION = PROHIBITED
RULE_PACK_PROMOTION = BLOCKED_UNTIL_APPROVAL
```

## 4. 当前 Gate

```text
A/B Medical Owner Review Record v0.1 = COMPLETE
A/B Package Approval = NOT_COMPLETE
A/B Content Draft v0.2 Revision = REQUIRED
C Rule Pack Clinical Content = BLOCKED
D D09 Production Clinical Policy = BLOCKED
E Knowledge Applicability / Content Approval = PENDING
F Clinical Eval Content = PENDING
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

## 5. 合并语义

即使 PR #88 未来被授权合并，也只能表示：

```text
Clinical dependency governance package / review workspace
= merged as project documentation
```

不能表示：

```text
Medical Owner Approval = COMPLETE
Clinical Input Package = APPROVED
Clinical Rule Pack = READY
Clinical Evaluation = PASSED
Clinical Runtime = ENABLED
Production Authorization = GRANTED
```

## 6. 下一步

下一动作是按 `U03_AB_Revision_Task_v0.2.md` 产出 A/B Content Draft v0.2 并再审。只有再审后明确批准的 B entries 才允许进入 C。本轮不进入 C/D/E/F 真实临床内容。

A/B 条目计数以 `U03_AB_Medical_Owner_Review_Record_v0.1.md` 为准；Recommendation v0.1 只保留为历史审稿意见。
