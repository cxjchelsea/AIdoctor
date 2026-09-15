# U03 Clinical Dependency Readiness

> 阶段：U03 Clinical Dependency Completion / Readiness  
> 基线：`main@765fb9ca1178c47a6ecfc660bd650edb5bffaf8b`（包含 PR #86 + #87）  
> 本文件判断是否可以进入真实 C02/D09 临床依赖实现；不构成 Implementation Authorization。

## 1. 已满足工程前置

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
New Foundation = NOT_REQUIRED
```

## 2. Clinical Input Package 当前进度

```text
A Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ CONTENT_DRAFT_v0.2_AVAILABLE
/ MEDICAL_OWNER_REVIEW_COMPLETE
/ A_APPROVE_7_REVISE_0
/ SOURCE_LOCKED_SEMANTICS_FROZEN

B Evidence Catalog
= STRUCTURAL_SCHEMA_FROZEN
/ CONTENT_DRAFT_v0.2_AVAILABLE
/ MEDICAL_OWNER_REVIEW_COMPLETE
/ B_APPROVE_11_REVISE_0
/ SOURCE_LOCKED_SEMANTICS_FROZEN

C Safety-critical Risk Rule Pack
= STRUCTURAL_SCHEMA_FROZEN
/ CONTENT_DRAFT_v0.2_AVAILABLE
/ ACTIVE_RULE_CANDIDATES_15
/ ACTIVE_RULE_APPROVE_15_REVISE_0
/ BF-C-01..04_CLOSED
/ PACKAGE_APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT
/ POLICY_PAIR_CANDIDATE_FROZEN
/ PRE_FREEZE_EVAL_PASS
/ RR-U03-RISK-001@0.2.0-candidate_CANDIDATE_FROZEN
/ CD-03_PASSED_FOR_INITIAL_CANDIDATE

D D09 Clinical Policy Table
= STRUCTURAL_SCHEMA_FROZEN
/ DRAFTING_READINESS_PASS
/ v0.1_REVIEW_COMPLETE_REVISE_REQUIRED
/ v0.2_RE_REVIEW_COMPLETE
/ POLICY_RELEASE = PR-U03-D09-001@0.2.0-draft
/ BF-D-01_CLOSED
/ BF-D-02_CLOSED
/ COVERAGE_CONTRACT = U03_D09_COVERAGE_V0_2_APPROVED_FOR_CONTENT
/ MEDICAL_RE_REVIEW_COMPLETE
/ TECHNICAL_RE_REVIEW_COMPLETE
/ CONTENT_APPROVAL = APPROVED_FOR_CONTENT_AND_COVERAGE
/ POLICY_CANDIDATE_FREEZE_NOT_COMPLETE
/ CD-05_NOT_PASSED

E Knowledge Release Manifest
= STRUCTURAL_SCHEMA_FROZEN
/ APPLICABILITY_DECISION_v0.1_APPROVED
/ KD-U03-01_REQUIRED
/ KNOWLEDGE_RELEASE_REF = KR-U03-SOURCE-001@0.1.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED

F Risk EvalSet / Safety Suite
= STRUCTURAL_SCHEMA_FROZEN / GOLDEN_CASE_CONTENT_NOT_STARTED / NOT_REVIEW_READY
```

## 3. Gate 状态

### Gate A — v0.2 Source-locked Clinical Semantics

```text
Gate A = PASS
A APPROVE = 7 / REVISE = 0
B APPROVE = 11 / REVISE = 0
```

### Gate B — Governed Content Ready

当前：`NOT_PASSED`

C 已完成 initial candidate freeze：

```text
RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN
CD-03 = PASSED_FOR_INITIAL_CANDIDATE
```

D v0.1 review 结论：

```text
Medical = 6 / 6 APPROVE
Technical = 4 APPROVE / 2 REVISE
D Content Approval = REVISE_REQUIRED
BF-D-01 = OPEN at v0.1 review
BF-D-02 = OPEN at v0.1 review
```

v0.2 re-review 已完成：

```text
PR-U03-D09-001@0.2.0-draft = REVIEWED
BF-D-01 = CLOSED
BF-D-02 = CLOSED
U03_D09_COVERAGE_V0_2 = APPROVED_FOR_CONTENT / NOT_FROZEN
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
```

v0.2 保持：

```text
P0 > P1 > P2 > P3 > P4 > P5
```

并新增/固化：

```text
OVERALL_POLICY_SCOPE_MISMATCH
= D09-P-001 / P0 failure
!= C RULE_SIGNAL_SCOPE_MISMATCH

ALWAYS_APPLICABLE baseline denominator
= RESP / NEURO-001 / NEURO-002 / CARD / ALLERGY

CONDITIONALLY_APPLICABLE families
= NHS_DYSPNOEA_FAMILY
= NG253_SEPSIS_FAMILY

C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ excluded from denominator

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 / P-040
```

Gate B 仍缺：

```text
coverage contract freeze decision
D policy candidate freeze
CD-05 approval
C/D/E cross-consistency review
Gate B final decision
```

### Gate C — Independent Evaluation Ready

```text
Gate C = NOT_PASSED
CD-06 = NOT_REVIEW_READY
Clinical Golden Cases = NOT_STARTED
```

C Pre-Freeze Eval PASS 仅支持 C candidate freeze，不等于 Gate C PASS。

### Gate D — Authorization

上一轮 U03 工程实现授权不自动覆盖 CD-01～CD-08。后续实现仍需要独立、明确的 Implementation Authorization。

## 4. Current Governed Release References

```text
Knowledge Release
= KR-U03-SOURCE-001@0.1.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED

Rule Release
= RR-U03-RISK-001@0.2.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED

D09 Policy Release
= PR-U03-D09-001@0.2.0-draft
/ APPROVED_FOR_CONTENT_AND_COVERAGE
/ NOT_FROZEN
/ NOT_PUBLISHED

D09 Coverage Contract
= U03_D09_COVERAGE_V0_2
/ RESOLVABLE
/ APPROVED_FOR_CONTENT
/ NOT_FROZEN
```

## 5. 当前 Readiness 判定

```text
Engineering Prerequisites = PASS
Gate A = PASS

CD-03 = PASSED_FOR_INITIAL_CANDIDATE
CD-04 = CANDIDATE_READY / NOT_PRODUCTION

D Drafting Readiness = PASS_FOR_DRAFTING
D v0.1 Review = COMPLETE / REVISE_REQUIRED
D v0.2 Re-review = COMPLETE_APPROVE
BF-D-01 = CLOSED
BF-D-02 = CLOSED
D Coverage Contract = APPROVED_FOR_CONTENT / NOT_FROZEN
D Content Approval = APPROVED_FOR_CONTENT_AND_COVERAGE
D Policy Candidate Freeze = NOT_COMPLETE
CD-05 = NOT_PASSED

C/D/E Cross-Consistency = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
Medical Owner Approval for whole Clinical Input Package = NOT_COMPLETE
Governed Clinical Content = NOT_COMPLETE
Clinical Evaluation Content = NOT_COMPLETE
Independent Evaluation Readiness = NOT_READY
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
```

总判定保持：

```text
U03 Clinical Dependency Readiness
= BLOCKED_BY_CLINICAL_INPUT_PACKAGE
```

## 6. 当前唯一下一步

```text
Assess
coverage-contract freeze
+
D policy candidate freeze
+
CD-05 readiness
↓
do not treat content approval as freeze
↓
C/D/E cross-consistency
↓
Gate B decision
```

## 7. 当前禁止事项

- 不把 `PR-U03-D09-001@0.2.0-draft` 的 content approval 当成 frozen/published policy；
- 不把 `U03_D09_COVERAGE_V0_2` 的 content approval 误当成已冻结；
- 不修改或重命名 `RR-U03-RISK-001@0.2.0-candidate`；后续变更必须新建版本；
- D 不得重新解释 C 阈值或新增 C evidence taxonomy；
- `NO_HIGH_RISK_SIGNAL` 不得解释为 SAFE / NORMAL；
- D09 不得形成 U04 Safety Gate Decision；
- 不把 C Pre-Freeze Eval PASS 当成 Gate C PASS；
- 不开始 D09 runtime implementation、C02 clinical implementation 或 U04；
- 不新增未经过 A/B 审核链的医学来源或 evidence；
- 不把 NICE/NHS 直接视为中国生产规则；
- 不打开儿科或孕产 source pack。
