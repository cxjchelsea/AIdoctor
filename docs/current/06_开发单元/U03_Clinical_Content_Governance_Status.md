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
```

## 3. 当前 Governed Content Set

```text
A/B source-locked semantics = FROZEN / Gate A PASS

E = KR-U03-SOURCE-001@0.1.0-candidate
  / CANDIDATE_FROZEN / RESOLVABLE / REVIEWED / NOT_PUBLISHED

C = RR-U03-RISK-001@0.2.1-candidate
  / CANDIDATE_FROZEN / CD-03_APPROVED_FOR_GATE_B

Coverage = U03_D09_COVERAGE_V0_2_1_CANDIDATE
  / CANDIDATE_FROZEN / RESOLVABLE

D = PR-U03-D09-001@0.2.1-candidate
  / CANDIDATE_FROZEN / CD-05_APPROVED_FOR_GATE_B

C/D/E Cross-Consistency v0.2.1 = PASS
BF-CDE-01 = CLOSED
```

Current binding chain:

```text
KR-U03-SOURCE-001@0.1.0-candidate
→ RR-U03-RISK-001@0.2.1-candidate
→ U03_D09_COVERAGE_V0_2_1_CANDIDATE
→ PR-U03-D09-001@0.2.1-candidate
```

历史 `0.2.0` C/D/Coverage candidates 继续冻结并保持 immutable，不属于当前 Gate B set。

## 4. 当前 Gate

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
Gate B decision = U03_Gate_B_Decision_v0.2.1.md

CD-03 = APPROVED_FOR_GATE_B
CD-04 = INITIAL_RELEASE_GOVERNANCE_READY
CD-05 = APPROVED_FOR_GATE_B

Gate C = NOT_PASSED
CD-06 = REVIEW_READY
F Clinical Eval Content = APPROVED_FOR_EVALUATION
BF-CD06-01 = CLOSED
BF-CD06-02 = CLOSED
Evaluation Execution = NOT_STARTED

Evaluation-only Implementation Authorization = GRANTED / AUTH-U03-GATEC-EVAL-IMPL-001
Semantic Mapping Review = COMPLETE / NO_BLOCKING_GAP
Evaluation-only Implementation = NOT_STARTED / AUTHORIZED_TO_START

CD-07 Implementation Readiness = BLOCKED
Runtime Implementation Authorization = NOT_GRANTED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

Gate B PASS 只表示当前 E/C/Coverage/D governed content set 已满足 Gate B 的版本、scope、authority、binding 与 cross-consistency 要求。

它不表示：

```text
Gate C PASS
Clinical Evaluation PASS
CD-07 Implementation Authorization
Runtime Active
Production Authorized
```

## 5. Gate B 后的当前边界

下一步必须进入 governed evaluation execution：

```text
execute ER-U03-RISK-001@0.1.0-candidate
  GC-001..GC-031
  SS-001..SS-020
↓
record execution evidence
↓
Gate C decision
```

C57 / D48 historical fixture reuse + 6 targeted delta fixtures 仅用于 candidate governance / Gate B，不替代完整 Clinical EvalSet / Safety Suite。

## 6. 当前禁止事项

- 不把 current frozen candidates 当作 PUBLISHED / ACTIVE_FOR_RUNTIME / ACTIVE_FOR_PRODUCTION；
- 不把 Gate B PASS 解释为 Gate C PASS；
- 不开始 CD-07 / C02 clinical runtime / D09 runtime / U04；
- 不打开中国生产本地化；
- 不打开儿科或孕产 source pack；
- 不把 NICE/NHS 直接视为中国最终生产规则；
- 不把 Gate B PASS 解释为 Merge Authorization 或 Production Authorization。
