# U03 Gate B Readiness Decomposition

> 阶段：U03 Clinical Dependency Completion / Gate B readiness decomposition  
> 前置：Gate A = PASS；A/B v0.2 来源锁定语义已冻结。  
> 目的：拆解 CD-03～CD-05 在进入真实临床内容前仍缺失的输入、Owner、依赖顺序与通过条件。  
> 本文件不构成 Implementation Authorization、Merge Authorization 或 Production Authorization。

## 1. 当前 Gate A 后状态

```text
Gate A = PASS
A Clinical Risk Semantics v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A
B Evidence Catalog v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A

E Applicability Decision v0.1 = APPROVED
KD-U03-01 Knowledge Release
= KR-U03-SOURCE-001@0.1.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED

C Rule Release
= RR-U03-RISK-001@0.2.0-candidate
/ CANDIDATE_FROZEN
/ NOT_PUBLISHED

D Clinical Policy Content = NOT_STARTED
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

## 2. Gate B 的三个受治理对象

```text
C / Safety-critical Risk Rule Pack
D / D09 Clinical Policy Table
E / Knowledge Release applicability + governed release content
```

对应：

```text
CD-03 = Rule Pack specification / initial clinical rule release
CD-04 = Knowledge / Rule Release governance content
CD-05 = D09 deterministic clinical policy
```

当前依赖关系：

```text
Gate A PASS
↓
E applicability adjudication
↓
KD-U03-01 Knowledge Release candidate
↓
C initial Rule Pack candidate
↓
D D09 clinical policy content
↓
C/D/E cross-release consistency review
↓
Gate B decision
```

## 3. E 当前状态

当前工程实现中的 `U03ReleaseBinding` 强制要求：

```text
ruleReleaseId = required
ruleReleaseVersion = required
knowledgeReleaseId = required
knowledgeReleaseVersion = required
```

Medical/Governance review 已明确：

```text
KD-U03-01 Source-grounded Clinical Knowledge Release = REQUIRED
KD-U03-02 Executable Threshold / Combination Logic = owner C / independent KR NOT_REQUIRED
KD-U03-03 Final Risk Disposition Mapping = owner D09 / independent KR NOT_REQUIRED
KD-U03-04 Runtime Free-form External Retrieval = PROHIBITED_AS_IMPLICIT_DEPENDENCY
KD-U03-05 Explanation-only Knowledge = OPTIONAL
```

当前：

```text
CD-04 knowledge candidate
= KR-U03-SOURCE-001@0.1.0-candidate
/ REVIEWED
/ RESOLVABLE
/ CANDIDATE_FROZEN
/ NOT_PRODUCTION
```

## 4. C / Rule Pack 当前状态

C 已完成：

```text
15 active rule review = APPROVE_15 / REVISE_0
BF-C-01..04 = CLOSED
Package Approval = APPROVED_FOR_CONTENT_AND_SCOPE_INVARIANT
PF-U03-C-POLICY-001 = CANDIDATE_FROZEN
Pre-Freeze Eval PASS = YES
RR-U03-RISK-001@0.2.0-candidate = CANDIDATE_FROZEN
```

因此：

```text
CD-03 = PASSED_FOR_INITIAL_CANDIDATE
```

这只表示 initial governed candidate 可用于后续 D drafting，不表示 production rule release 已发布。

## 5. D / D09 Policy 进入真实内容前需要什么

D09 是唯一正式 Risk Disposition Owner，真实 policy 不能先于 C 的 rule/result vocabulary 稳定。

该前置现在已经满足：

```text
rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
C rule/result vocabulary = FROZEN_FOR_CANDIDATE
```

D drafting readiness 已单独判断：

```text
U03_D_Drafting_Readiness_Assessment_v0.1.md
D Drafting Readiness = PASS_FOR_DRAFTING
```

D 初稿至少需要定义：

```text
policy branches
branch preconditions
required evidence refs
required rule refs
priority / precedence
multi-hit conflict behavior
VALID vs FAILED behavior
NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK mapping
reason codes
scope
rule release refs
knowledge release refs where applicable
source/provenance
review + evaluation refs
```

必须保持：

```text
Rule Hit != D09 Decision
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
D09 Decision != U04 Safety Gate Decision
```

## 6. CD-04 的实际角色

CD-04 负责把 C/D 真正依赖的 release 资产变成可治理、可解析、可追溯版本对象，至少覆盖：

```text
Rule Release governance
Knowledge Release applicability decision
KD-U03-01 Knowledge Release content
release identity/version/scope/effective time
source/provenance
supersede/rollback
review/approval status
P06 binding compatibility
```

当前：

```text
Knowledge Release candidate = FROZEN / RESOLVABLE
Rule Release candidate = FROZEN / RESOLVABLE
Production release = NOT_AUTHORIZED
```

## 7. Gate B Cross-Consistency Check

Gate B 最终必须验证：

```text
B Evidence refs
→ C Rule refs
→ D Policy refs

C Rule Release refs
→ D Policy Release

KD-U03-01 Knowledge Release
→ C / D source/provenance refs

scope/version/effective-time
→ mutually compatible
```

并确保：无 dangling refs、无 scope 扩大、无隐式知识依赖、无 mutable `latest` alias、无 `FAILED -> NO_HIGH_RISK_SIGNAL`。

## 8. Gate B 最小通过定义

```text
Gate A = PASS
CD-03 = APPROVED_FOR_GATE_B
CD-04 = INITIAL_RELEASE_GOVERNANCE_READY
CD-05 = APPROVED_FOR_GATE_B
E Knowledge Applicability = APPROVED
KD-U03-01 Knowledge Release = RESOLVABLE / REVIEWED
C/D/E Cross-Consistency = PASS
```

即使 Gate B PASS，Gate C / CD-06、CD-07 Implementation Authorization、U04 与 Production 仍是独立后续门禁。

## 9. 当前 Readiness 判定

```text
Gate A = PASS

C Structural Schema = PASS
C Clinical Content = APPROVED_FOR_CONTENT_AND_SCOPE
C Rule Release Candidate = RR-U03-RISK-001@0.2.0-candidate / CANDIDATE_FROZEN
CD-03 = PASSED_FOR_INITIAL_CANDIDATE

D Structural Schema = PASS
D Drafting Readiness = PASS_FOR_DRAFTING
D Clinical Content = NOT_STARTED
CD-05 = NOT_PASSED

E Structural Schema = PASS
E Applicability = APPROVED
KD-U03-01 Knowledge Release = KR-U03-SOURCE-001@0.1.0-candidate / CANDIDATE_FROZEN

Gate B Readiness = READY_FOR_D_DRAFTING
Primary Blocker = D_POLICY_CONTENT_NOT_STARTED
Secondary Blocker = C_D_E_CROSS_CONSISTENCY_NOT_STARTED
```

当前唯一合理的下一步：

```text
Draft D / D09 Clinical Policy Table content
using frozen C + E candidate refs
```

只允许形成 review draft；不得把 drafting readiness 解释为 CD-05 approval、Implementation Authorization、runtime activation 或 production authorization。
