# U03 Clinical Input Package Specification

> 目的：定义医学侧必须提供给 U03 的最小、可审核、可版本化输入包。  
> 本文件定义结构与当前门禁状态，不提供任何具体医学阈值、红旗规则或风险判定内容。

## 1. 为什么需要独立 Clinical Input Package

U03 当前工程链已经可以安全承载临床能力，但工程侧不能从代码结构反推出医学规则。真实 C02 / D09 实现前，必须先有医学 Owner 可审核、可版本化、可追溯的输入包。

## 2. A：Clinical Risk Semantics

```text
A Content Draft v0.2 = AVAILABLE
A item-level verdict = APPROVE 7 / REVISE 0
A source-locked semantics = FROZEN_FOR_GATE_A
Gate A contribution = PASS
```

## 3. B：Evidence Catalog

```text
B Content Draft v0.2 = AVAILABLE
B item-level verdict = APPROVE 11 / REVISE 0
B source-locked evidence semantics = FROZEN_FOR_GATE_A
Gate A contribution = PASS
```

B 条目只允许作为后续 C 的受治理 evidence 输入；不得直接解释为 Rule、D09 Decision、HIGH_RISK 或 Safety Gate。

## 4. C：Safety-critical Risk Rule Pack

结构文件：`U03_Safety_Critical_Risk_Rule_Pack_Schema.md`

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_RULE_CONTENT = DRAFT_v0.2_AVAILABLE
INITIAL_RULE_RELEASE = RR-U03-RISK-001@0.2.0-draft
MEDICAL_OWNER_REVIEW = COMPLETE
TECHNICAL_REVIEW = COMPLETE
ACTIVE_RULE_APPROVE_15_REVISE_0
BF-C-04_CLOSED
PACKAGE_APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT
CD-03 = NOT_PASSED
```

C package 内容与 scope invariant 已再确认通过。下一动作是独立的 candidate-freeze readiness；D 仍须等 freeze。

## 5. D：D09 Clinical Policy Table

结构文件：`U03_D09_Clinical_Policy_Table_Schema.md`

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_POLICY_CONTENT_DRAFT_v0.1 = REVIEWED
MEDICAL_PRIORITY_PRECEDENCE = REVIEWED_P0_TO_P5
MEDICAL_OWNER_REVIEW = COMPLETE
TECHNICAL_REVIEW = COMPLETE
CONTENT_APPROVAL = REVISE_REQUIRED
POLICY_RELEASE = PR-U03-D09-001@0.1.0-draft
CD-05 = NOT_PASSED
```

D v0.1 的 6 个 branch 与 `P0 > P1 > P2 > P3 > P4 > P5` 方向已审过；BF-D-01 / BF-D-02 关闭前不得进入 content approval 或 candidate freeze。

## 6. E：Knowledge Release Manifest

结构文件：`U03_Knowledge_Release_Manifest_Schema.md`

Applicability decision：`U03_Knowledge_Dependency_Applicability_Decision_v0.1.md`

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN
APPLICABILITY_ADJUDICATION = APPROVED
APPLICABILITY_APPROVAL = COMPLETE_FOR_ROLE_APPLICABILITY

KD-U03-01 = REQUIRED
KD-U03-02 = NOT_REQUIRED_AS_INDEPENDENT_KR / OWNER_C
KD-U03-03 = NOT_REQUIRED_AS_INDEPENDENT_KR / OWNER_D09
KD-U03-04 = PROHIBITED_AS_IMPLICIT_DEPENDENCY
KD-U03-05 = OPTIONAL

KD-U03-01 KNOWLEDGE_RELEASE_CONTENT = NOT_STARTED
KNOWLEDGE_RELEASE_PUBLICATION = NOT_COMPLETE
CD-04 = NOT_PASSED
```

当前工程事实：`U03ReleaseBinding` 要求 `knowledgeReleaseId` 与 `knowledgeReleaseVersion` 非空。但 KD-U03-01 的 REQUIRED 也是临床治理要求：即使未来工程合同允许空 ref，本 slice 仍必须保留可重放来源 release。

下一步只允许起草最小 KD-U03-01 Knowledge Release 对象，并且只能绑定已经进入 A/B 审核链的 source registry。不得新增指南、阈值、临床结论或 D09 branch。

## 7. F：Risk EvalSet / Safety Suite

结构文件：`U03_Risk_EvalSet_Safety_Suite_Schema.md`

```text
STRUCTURAL_SCHEMA_FROZEN
GOLDEN_CASE_CONTENT = NOT_STARTED
MEDICAL_OWNER_REVIEW = NOT_COMPLETE
EVALUATION_OWNER_REVIEW = NOT_COMPLETE
CD-06 = NOT_REVIEW_READY
Gate C = NOT_PASSED
```

## 8. 完整输入包验收条件

Clinical Input Package 只有同时满足以下条件才可进入 CD-07 Implementation：

```text
A = PASSED_FOR_GATE_A
B = PASSED_FOR_GATE_A
C Rule Pack initial release = APPROVED
D D09 Policy Table = APPROVED
E KD-U03-01 Knowledge Release = REVIEWED / RESOLVABLE
F EvalSet / Safety Suite = REVIEW_READY
```

并且每个医学判断有 owner、每个关键规则可追溯 source、每个 release 有 version/scope/effective time、EvalSet 与实现代码独立、C/D/E refs 可相互解析、不存在 scope 超出 Gate A source lock。

## 9. Gate B 顺序

```text
Gate A = PASS
↓
E Applicability Adjudication = APPROVED
↓
KD-U03-01 Minimum Knowledge Release Object
↓
C Initial Rule Pack Clinical Content
↓
D D09 Clinical Policy Content
↓
C/D/E Cross-release Consistency Review
↓
Gate B Decision
```

详细拆解见 `U03_Gate_B_Readiness_Decomposition.md`。

## 10. 当前状态

```text
A = PASS_FOR_GATE_A
B = PASS_FOR_GATE_A
Gate A = PASS

C = STRUCTURAL_SCHEMA_FROZEN / PACKAGE_APPROVED_FOR_CONTENT / NOT_FROZEN
D = STRUCTURAL_SCHEMA_FROZEN / CLINICAL_CONTENT_NOT_STARTED / BLOCKED_UNTIL_C_CANDIDATE_FREEZE
E = STRUCTURAL_SCHEMA_FROZEN / APPLICABILITY_APPROVED / KD-U03-01_CANDIDATE_FROZEN
F = STRUCTURAL_SCHEMA_FROZEN / GOLDEN_CASE_CONTENT_NOT_STARTED

Gate B = NOT_PASSED
Gate C = NOT_PASSED
Clinical Input Package = NOT_COMPLETE
Medical Owner Approval = NOT_COMPLETE
CD-07 Implementation Readiness = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

当前唯一合理的下一步：对 missingness / shared-scope / evaluation refs 做独立 candidate-freeze readiness。D 不得先于 C freeze 开始。