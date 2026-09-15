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
/ v0.2_RE_REVIEW_COMPLETE
/ SOURCE_DRAFT = PR-U03-D09-001@0.2.0-draft_PRESERVED
/ POLICY_CANDIDATE = PR-U03-D09-001@0.2.0-candidate_CANDIDATE_FROZEN
/ BF-D-01_CLOSED
/ BF-D-02_CLOSED
/ COVERAGE_CONTRACT = U03_D09_COVERAGE_V0_2_CANDIDATE_FROZEN
/ CONTENT_APPROVAL = APPROVED_FOR_CONTENT_AND_COVERAGE
/ PRE_FREEZE_FIXTURE_CONTENT_REVIEWED_48
/ PRE_FREEZE_EVAL_PASS
/ BLOCKER-FZ-D-01..04_CLOSED
/ CD-05_PASSED_FOR_INITIAL_CANDIDATE

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

当前：`NOT_PASSED / BLOCKED_BY_CDE_SCOPE_INCONSISTENCY`

C / D / E 各自 initial candidate gate 已满足：

```text
CD-03 = PASSED_FOR_INITIAL_CANDIDATE
CD-04 = CANDIDATE_READY / NOT_PRODUCTION
CD-05 = PASSED_FOR_INITIAL_CANDIDATE

E = KR-U03-SOURCE-001@0.1.0-candidate / CANDIDATE_FROZEN
C = RR-U03-RISK-001@0.2.0-candidate / CANDIDATE_FROZEN
D = PR-U03-D09-001@0.2.0-candidate / CANDIDATE_FROZEN
```

Cross-consistency review 已完成：

```text
U03_CDE_Cross_Consistency_Review_v0.1.md
PASS items = 11
REVISE items = 1
blocking finding = 1
BF-CDE-01 = OPEN
C/D/E Cross-Consistency = REVISE_REQUIRED
```

阻塞项：

```text
BF-CDE-01
= pregnancy / puerperium scope inconsistency
```

当前上游 authority：

```text
A v0.2 overall scope
→ pregnancy / puerperium NOT COVERED

E KR-U03-SOURCE-001@0.1.0-candidate
→ pregnancy / puerperium in whole-release exclusions
```

但冻结的 C/D candidate metadata 只显式表达：

```text
C
→ pregnancy_recent_pregnancy = EXCLUDED_FOR_NG253_SEPSIS_RULES

D
→ pregnancy/recent-pregnancy sepsis policy = NOT_INCLUDED
```

因此 whole-policy scope 与 D coverage denominator 存在可执行歧义。

Gate B 决策已记录：

```text
U03_Gate_B_Decision_v0.1.md
Gate B = NOT_PASSED
reason = BLOCKED_BY_CDE_SCOPE_INCONSISTENCY
```

修订任务：

```text
U03_CDE_Cross_Consistency_Revision_Task_v0.1.md
```

不得原地修改已冻结的 C/D/E/coverage 对象；必须形成新的受影响 candidate version 后 targeted review / freeze / re-review。

### Gate C — Independent Evaluation Ready

```text
Gate C = NOT_PASSED
CD-06 = NOT_REVIEW_READY
Clinical Golden Cases = NOT_STARTED
```

C/D pre-freeze evaluation 均只服务 candidate freeze，不等于 Gate C PASS。

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

D09 Coverage Contract
= U03_D09_COVERAGE_V0_2
/ CANDIDATE_FROZEN
/ NOT_FOR_PRODUCTION

D09 Policy Source Draft
= PR-U03-D09-001@0.2.0-draft
/ PRESERVED
/ NOT_FROZEN

D09 Policy Candidate
= PR-U03-D09-001@0.2.0-candidate
/ RESOLVABLE
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED
/ NOT_FOR_RUNTIME
/ NOT_FOR_PRODUCTION
```

## 5. 当前 Readiness 判定

```text
Engineering Prerequisites = PASS
Gate A = PASS

CD-03 = PASSED_FOR_INITIAL_CANDIDATE
CD-04 = CANDIDATE_READY / NOT_PRODUCTION
CD-05 = PASSED_FOR_INITIAL_CANDIDATE

C/D/E Cross-Consistency Review = COMPLETE
C/D/E Cross-Consistency = REVISE_REQUIRED
BF-CDE-01 = OPEN
Gate B = NOT_PASSED / BLOCKED_BY_CDE_SCOPE_INCONSISTENCY

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
execute U03_CDE_Cross_Consistency_Revision_Task_v0.1.md
↓
resolve BF-CDE-01 with new affected candidate versions
↓
targeted Medical / Technical review
↓
freeze new affected candidates
↓
C/D/E cross-consistency re-review
↓
only if PASS + blocking finding = 0
→ reconsider Gate B
```

默认修订路径必须以当前 A/E 已批准 authority 为准：

```text
pregnancy / puerperium
= OUTSIDE_CURRENT_U03_WHOLE_POLICY_SLICE
```

若要改成“仅 sepsis family 排除”，则必须先重新打开 A/E 的 Medical/source scope review；不能作为纯技术 metadata 修正处理。

## 7. 当前禁止事项

- 不原地修改已冻结的 `RR-U03-RISK-001@0.2.0-candidate`；
- 不原地修改已冻结的 `PR-U03-D09-001@0.2.0-candidate`；
- 不原地修改已冻结的 `U03_D09_COVERAGE_V0_2`；
- 不把 C/D/E individually frozen 当成 Gate B PASS；
- 不把 `CD-05 = PASSED_FOR_INITIAL_CANDIDATE` 当成 Gate B PASS；
- 不把 D/C Pre-Freeze Eval PASS 误当成 Gate C PASS；
- 不把 `NO_HIGH_RISK_SIGNAL` 解释为 SAFE / NORMAL；
- 不开始 D09 runtime implementation、C02 clinical implementation 或 U04；
- 不新增未经过 A/B 审核链的医学来源或 evidence；
- 不把 NICE/NHS 直接视为中国生产规则；
- 不打开儿科或孕产 source pack。
