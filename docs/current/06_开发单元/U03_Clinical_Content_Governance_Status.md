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
U03_Knowledge_Dependency_Applicability_Decision_v0.1.md
U03_Knowledge_Release_Content_Draft_v0.1.md
U03_KR_Source_Metadata_Verification_v0.1.md
U03_KR_Governance_Owner_Assignment_v0.1.md
U03_KR_Formal_Review_Record_v0.1.md
U03_KR_Freeze_Readiness_v0.1.md
U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.1.md
U03_C_Rule_Pack_Formal_Review_Record_v0.1.md
U03_C_Medical_Owner_Review_Record_v0.1.md
U03_C_Revision_Task_v0.1.md
U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md
U03_C_Medical_Owner_ReReview_Record_v0.2.md
U03_C_Revision_Task_v0.2.md
U03_C_BF_C_04_Closure_Amendment_v0.2.md
U03_C_Missingness_Policy_v0.2.md
U03_Sepsis_Shared_Scope_Policy_v0.2.md
U03_C_Evaluation_Refs_Manifest_v0.2.md
U03_C_Package_Reconfirmation_Record_v0.2.md
U03_C_Freeze_Readiness_Assessment_v0.2.md
U03_C_Freeze_Policy_Review_Record_v0.2.md
U03_C_PreFreeze_Evaluation_Minimum_v0.2.md
U03_C_Missingness_Policy_Revision_Task_v0.2.md
U03_C_Missingness_M2_ReReview_Record_v0.2.md
U03_C_Policy_Pair_Freeze_Record_v0.2.md
U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
U03_C_PreFreeze_Eval_Review_Record_v0.2.md
U03_D09_Clinical_Policy_Content_Draft_v0.1.md
U03_D09_Clinical_Policy_Review_Record_v0.1.md
U03_D09_Clinical_Policy_Revision_Task_v0.1.md
U03_D09_Clinical_Policy_Content_Draft_v0.2.md
U03_D09_Coverage_Contract_v0.2.md
U03_D09_Clinical_Policy_ReReview_Record_v0.2.md
U03_D09_Coverage_Contract_Freeze_Record_v0.2.md
U03_D09_Freeze_Readiness_Assessment_v0.1.md
U03_D09_PreFreeze_Evaluation_Minimum_v0.1.md
U03_D09_PreFreeze_Evaluation_Fixtures_v0.1.md
U03_D09_PreFreeze_Eval_Review_Record_v0.1.md
U03_CDE_Cross_Consistency_Review_v0.1.md
U03_CDE_Cross_Consistency_Revision_Task_v0.1.md
U03_Gate_B_Decision_v0.1.md
U03_C_Rule_Release_Scope_Revision_Draft_v0.2.1.md
U03_D09_Policy_Scope_Revision_Draft_v0.2.1.md
U03_D09_Coverage_Contract_Revision_Draft_v0.2.1.md
U03_CDE_Scope_Alignment_Targeted_Review_Record_v0.2.1.md
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
C Historical Candidate = RR-U03-RISK-001@0.2.0-candidate_CANDIDATE_FROZEN
C Scope Revision 0.2.1 = APPROVED_FOR_CONTENT / NOT_FROZEN
D Historical Candidate = PR-U03-D09-001@0.2.0-candidate_CANDIDATE_FROZEN
D Scope Revision 0.2.1 = APPROVED_FOR_CONTENT / NOT_FROZEN
D Coverage 0.2.0 = U03_D09_COVERAGE_V0_2 / CANDIDATE_FROZEN
D Coverage 0.2.1 = APPROVED_FOR_CONTENT / NOT_FROZEN
BF-CDE-01 = CLOSED_FOR_CONTENT
Gate B = NOT_PASSED / BLOCKED_BY_CDE_SCOPE_INCONSISTENCY
D D09 Production Clinical Policy = NOT_FROZEN / NOT_PUBLISHED
E Applicability = APPROVED
E KR REVIEW_READY = YES
E KR Freeze = CANDIDATE_FROZEN
E KR ref = KR-U03-SOURCE-001@0.1.0-candidate
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

- 不把 `KR-U03-SOURCE-001@0.1.0-candidate` 或 `PR-U03-D09-001@0.2.0-draft` 当作 PUBLISHED；F 仍未开始；
- 不打开中国生产本地化；
- 不打开儿科或孕产 source pack；
- 不把 NICE/NHS 直接视为中国最终生产规则；
- 不把 Gate A PASS 或 D content approval 解释为 Merge Authorization、D freeze 或 Production Authorization。
