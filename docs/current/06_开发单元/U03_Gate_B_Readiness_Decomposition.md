# U03 Gate B Readiness Decomposition

> 阶段：U03 Clinical Dependency Completion / Gate B readiness decomposition  
> 前置：Gate A = PASS；A/B v0.2 来源锁定语义已冻结。  
> 目的：拆解 CD-03～CD-05 在进入真实临床内容前仍缺失的输入、Owner、依赖顺序与通过条件。  
> 本文件不提供具体医学阈值、规则分支、风险 disposition 或生产知识内容；不构成 Implementation Authorization、Merge Authorization 或 Production Authorization。

## 1. 当前 Gate A 后状态

```text
Gate A = PASS
A Clinical Risk Semantics v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A
B Evidence Catalog v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A

E Applicability Decision v0.1 = APPROVED
E Applicability Approval = COMPLETE_FOR_ROLE_APPLICABILITY
KD-U03-01 = REQUIRED
KD-U03-01 Knowledge Release Content = NOT_STARTED

Medical Owner Approval for whole Clinical Input Package = NOT_COMPLETE
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

Gate A PASS 与 E applicability approval 均不表示 Rule Pack、D09 Policy、Knowledge Release publication 或整个 Clinical Input Package 已批准。

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
E applicability adjudication = COMPLETE
↓
KD-U03-01 minimum Knowledge Release object
↓
C initial Rule Pack clinical content
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

并且：

```text
即使未来工程合同支持空 knowledge ref
KD-U03-01 在本 slice 仍为临床 REQUIRED
```

### E 下一步

只允许起草最小 KD-U03-01 Knowledge Release 内容对象，且：

- 只能绑定已进入 A/B 审核链的 source registry；
- 不新增指南或临床结论；
- 不保存 executable threshold / combination；
- 不保存 D09 disposition mapping；
- 必须显式保留 version/scope/provenance/review/effective-time 字段；
- 未知的 source version/publication/retrieval metadata 不得编造，必须显式标为待核验。

在最小 KR 对象尚不可解析前：

```text
CD-04 = NOT_PASSED
C real rule content = BLOCKED
D real policy content = BLOCKED
```

## 4. C / Rule Pack 进入真实内容前需要什么

结构 Schema 已冻结，但真实 Rule Pack 仍缺：

```text
真实 rule entries
predicate / operator / threshold content
required / optional evidence refs
population / region / language / channel scope
missingness / uncertainty handling
priority / conflict group
source refs / provenance
knowledge release refs
initial rule release identity/version/effective time
clinical review
technical review
evaluation refs
```

C 只能消费 Gate A 已冻结的 B Evidence refs，并必须引用受治理 KD-U03-01 Knowledge Release 作为来源/provenance。

禁止：

```text
C 自己新增第二套 Evidence taxonomy
开发者凭常识补阈值
Knowledge Release 与 Rule Pack 各维护一份阈值
rule hit 直接等同 HIGH_RISK
```

### C readiness 必须先满足

```text
Gate A = PASS
E applicability = APPROVED
KD-U03-01 Knowledge Release object = RESOLVABLE
Rule content owner = ASSIGNED
Source refs = AVAILABLE
Rule release scope/version scheme = FROZEN
```

## 5. D / D09 Policy 进入真实内容前需要什么

D09 是唯一正式 Risk Disposition Owner，真实 policy 不能先于 C 的 rule/result vocabulary 稳定。

至少缺：

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
KD-U03-01 minimum Knowledge Release content
release identity/version/scope/effective time
source/provenance
supersede/rollback
review/approval status
P06 binding compatibility
```

当前 CD-04 已完成 applicability 角色裁定，但最小 Knowledge Release 内容仍未完成。

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
C Clinical Content = NOT_STARTED

D Structural Schema = PASS
D Clinical Content = NOT_STARTED

E Structural Schema = PASS
E Applicability Adjudication = APPROVED
E Applicability Approval = COMPLETE_FOR_ROLE_APPLICABILITY
KD-U03-01 Knowledge Release Content = NOT_STARTED

Gate B Readiness = NOT_READY_FOR_C_OR_D_CONTENT
Primary Blocker = KD-U03-01_MINIMUM_KNOWLEDGE_RELEASE_NOT_AVAILABLE
Secondary Blockers = C/D_REAL_CONTENT_NOT_AVAILABLE
```

当前唯一合理的下一步：

```text
Draft minimum KD-U03-01 Knowledge Release object
using only already-reviewed A/B source registry
```

不得直接进入 C Rule Pack 真实医学规则内容。