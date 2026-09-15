# U03 Clinical Content Governance Status

> 角色：PR #88 内容治理索引。  
> 本文件只声明文档成熟度与权威边界，不包含临床规则、阈值或医学决策。  
> 基线：U03 engineering governance slice 已合并至 `main`；Clinical Dependency Completion 尚未完成。

## 1. 总体原则

`docs/current` 中的结构/治理资料可作为当前项目参考，但 Draft / Review Material 不会因此自动升级为生产 Clinical Truth。

```text
STRUCTURAL / GOVERNANCE AUTHORITY
!=
CLINICAL CONTENT APPROVAL
```

## 2. 当前结构/治理基线

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
U03_AB_Revision_Readiness_v0.2.md
```

## 3. 临床审核工作材料

以下材料用于 A/B 审核、修订与审计记录；不得被 Runtime 当作已发布生产规则：

```text
U03_Clinical_Risk_Semantics_Content_Draft_v0.1.md
U03_Evidence_Catalog_Content_Draft_v0.1.md
U03_Clinical_Risk_Semantics_Content_Draft_v0.2.md
U03_Evidence_Catalog_Content_Draft_v0.2.md
U03_AB_Source_Review_v0.1.md
U03_AB_Medical_Review_Recommendation_v0.1.md
U03_AB_Medical_Owner_Review_Record_v0.1.md
U03_AB_Medical_Owner_Review_Record_v0.2.md
U03_AB_Revision_Task_v0.2.md
U03_AB_Medical_Review_Status.md
U03_AB_Revision_Status_v0.2.md
```

## 4. 当前 Gate

```text
A/B Content Draft v0.2 = AVAILABLE
A/B Medical Owner Review v0.2 = COMPLETE
A final verdict = APPROVE_7 / REVISE_0
B final verdict = APPROVE_11 / REVISE_0
Gate A = PASS
A/B v0.2 Source-locked Semantics = FROZEN

Medical Owner Approval = NOT_COMPLETE
Clinical Input Package = NOT_COMPLETE
C Rule Pack Clinical Content = BLOCKED
D D09 Production Clinical Policy = BLOCKED
E Knowledge Applicability / Content Approval = PENDING
F Clinical Eval Content = PENDING
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

## 5. Gate A 的边界

Gate A PASS 仅表示当前 v0.2 来源锁定语义已冻结。它不表示：

```text
Medical Owner Approval = COMPLETE
Clinical Input Package = APPROVED
Clinical Rule Pack = READY
Clinical Evaluation = PASSED
Clinical Runtime = ENABLED
Production Authorization = GRANTED
```

未来若扩大任何条目的 population / setting / disease scope，必须重新走来源与 Medical Owner 审核。

## 6. 当前禁止事项

- 不开始 C/D/E/F 真实临床内容；
- 不打开中国生产本地化；
- 不打开儿科或孕产 source pack；
- 不把 NICE/NHS 直接视为中国最终生产规则；
- 不把 Gate A PASS 解释为 Merge Authorization 或 Production Authorization。
