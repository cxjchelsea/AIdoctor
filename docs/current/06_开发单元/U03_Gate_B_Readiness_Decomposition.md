# U03 Gate B Readiness Decomposition

> 阶段：U03 Clinical Dependency Completion / Gate B readiness decomposition  
> 前置：Gate A = PASS；A/B v0.2 来源锁定语义已冻结。  
> 目的：拆解 CD-03～CD-05 在进入真实临床内容前仍缺失的输入、Owner、依赖顺序与通过条件。  
> 本文件不提供具体医学阈值、规则分支、风险 disposition 或生产知识内容；不构成 Implementation Authorization、Merge Authorization 或 Production Authorization。

---

## 1. 当前 Gate A 后状态

```text
Gate A = PASS
A Clinical Risk Semantics v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A
B Evidence Catalog v0.2 = SOURCE-LOCKED / FROZEN_FOR_GATE_A

Medical Owner Approval for whole Clinical Input Package = NOT_COMPLETE
Gate B = NOT_PASSED
Gate C = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

Gate A PASS 只意味着：

```text
A/B 当前候选语义与 Evidence 定义
已完成本 slice 的来源锁定与条目级审核
```

不意味着：

```text
Rule Pack 已批准
D09 Policy 已批准
Knowledge Release 已裁定/发布
EvalSet 已就绪
Clinical Input Package 已整体批准
```

---

## 2. Gate B 的三个受治理对象

Gate B 由以下三类临床依赖共同构成：

```text
C / Safety-critical Risk Rule Pack
D / D09 Clinical Policy Table
E / Knowledge Release applicability + governed release content
```

对应工作包：

```text
CD-03 = Rule Pack specification / initial clinical rule release
CD-04 = Knowledge / Rule Release governance content
CD-05 = D09 deterministic clinical policy
```

三者不是可随意并行的独立文档。

依赖关系至少为：

```text
Gate A PASS
↓
E applicability adjudication
↓
C initial Rule Pack clinical content
↓
D D09 clinical policy content
↓
C/D/E cross-release consistency review
↓
Gate B decision
```

其中 C 与 D 可在结构层并行准备，但真实医学内容必须遵守上游依赖。

---

## 3. E 必须先做 applicability adjudication

现有工程实现中的 `U03ReleaseBinding` 当前强制要求：

```text
ruleReleaseId = required
ruleReleaseVersion = required
knowledgeReleaseId = required
knowledgeReleaseVersion = required
```

因此当前工程合同不能把“没有 Knowledge Release”静默解释为 `NOT_REQUIRED`。

Gate B 前必须先做一个显式 E 决策：

```text
E-DECISION-01
U03 当前 slice 是否依赖独立 Knowledge Release？
```

允许的治理结论：

```text
REQUIRED
OPTIONAL（但若当前运行绑定则仍需正式 release）
NOT_REQUIRED（仅在工程合同也支持该一等语义后才可进入正式链）
PROHIBITED_AS_IMPLICIT_DEPENDENCY
```

当前技术约束下：

```text
NOT_REQUIRED
!=
留空 knowledgeReleaseId
```

如果 Medical/Product/Governance 最终裁定 E = NOT_REQUIRED，则必须单独评估是否需要后续工程变更，使 release binding 能合法表达该状态。该工程变化不属于本 #88 docs-only scope，也不得由文档隐式修改。

### E readiness 输入

至少需要：

- A/B v0.2 已通过 Gate A；
- 列出 C02、Rule Pack、D09 在本 slice 中可能依赖的知识资产类型；
- 对每类依赖给出 `REQUIRED / OPTIONAL / NOT_REQUIRED / PROHIBITED_AS_IMPLICIT_DEPENDENCY`；
- 每个 adjudication 有 owner、rationale、scope；
- 若 REQUIRED，定义最小 source/provenance/release/version/effective-time 要求；
- 若 NOT_REQUIRED，记录工程合同兼容性结论。

### E readiness 输出

```text
U03_Knowledge_Dependency_Applicability_Decision_v0.1.md
```

在该文件完成 Medical/Governance review 前：

```text
CD-04 = NOT_READY
C real release = SHOULD_NOT_START
D real policy = SHOULD_NOT_START
```

---

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
knowledge release refs where applicable
initial rule release identity/version/effective time
clinical review
technical review
evaluation refs
```

### C 的医学输入边界

C 只能消费 Gate A 已冻结的 B Evidence refs：

```text
Rule.required_evidence_refs[]
→ approved/frozen B evidence definitions
```

禁止：

```text
C 自己新增第二套 Evidence taxonomy
开发者凭常识补阈值
把 v0.2 Evidence 名称直接当成 executable rule
把 rule hit 直接等同 HIGH_RISK
```

### C readiness 必须先满足

```text
Gate A = PASS
E applicability = ADJUDICATED
Knowledge release refs = RESOLVABLE where required
Rule content owner = ASSIGNED
Source refs = AVAILABLE
Rule release scope/version scheme = FROZEN
```

### CD-03 通过条件

至少：

```text
Initial Rule Release = CLINICALLY_REVIEWED
All executable thresholds / combinations = source-grounded
All rules reference governed B evidence
Missing/unknown behavior = explicit
Scope = frozen
Version = frozen
Knowledge refs = valid where applicable
No rule directly owns Risk Disposition
```

这仍不等于生产发布；是否需要达到 `PUBLISHED` 由后续 release gate 决定。

---

## 5. D / D09 Policy 进入真实内容前需要什么

D09 是唯一正式 Risk Disposition Owner，因此真实 policy 不能先于 C 的 rule/result vocabulary 稳定。

真实 D09 至少缺：

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

### D 的硬边界

必须保持：

```text
Rule Hit != D09 Decision
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
D09 Decision != U04 Safety Gate Decision
```

D09 不得：

- 重新发明 C 中不存在的医学规则；
- 使用未发布 rule/knowledge release；
- 将 UNKNOWN / dependency failure 静默映射为低风险；
- 用代码 if/else 顺序代替医学 precedence。

### CD-05 通过条件

至少：

```text
Concrete Policy Branches = CLINICALLY_REVIEWED
Every branch binds governed evidence/rule refs
Priority/precedence = deterministic + reviewed
Failure behavior = explicit
Disposition vocabulary = frozen
Scope/version/effective time = frozen
Rule/knowledge refs = valid
Evaluation refs = available
```

---

## 6. CD-04 的实际角色

CD-04 不应被理解成“单独再写一份知识文档”。它负责把 C/D 真正依赖的 release 资产变成可治理、可解析、可追溯的版本对象。

至少覆盖：

```text
Rule Release governance
Knowledge Release applicability decision
Knowledge Release governance where REQUIRED
release identity/version/scope/effective time
source/provenance
supersede/rollback
review/approval status
P06 binding compatibility
```

因此：

```text
CD-03 clinical rule content
+
CD-04 governed release content
+
CD-05 D09 policy content
```

必须在 Gate B 前做交叉一致性审查。

---

## 7. Gate B Cross-Consistency Check

Gate B 最终不只检查三个文件“存在”，而要检查它们相互可解析：

```text
B Evidence refs
→ C Rule refs
→ D Policy refs

C Rule Release refs
→ D Policy Release

E Knowledge refs where applicable
→ C / D release manifests

scope/version/effective-time
→ mutually compatible
```

必须验证：

- 不存在 dangling evidence/rule/knowledge refs；
- 不存在 scope widened beyond Gate A source lock；
- 不存在 rule hit 直接提升为 disposition；
- 不存在 D09 branch 绕过 Rule Release；
- 不存在 knowledge implicit dependency；
- 不存在 `FAILED -> NO_HIGH_RISK_SIGNAL`；
- 不存在“latest” mutable alias 进入正式 binding。

---

## 8. Gate B 最小通过定义

```text
Gate A = PASS

CD-03
= APPROVED_FOR_GATE_B

CD-04
= INITIAL_RELEASE_GOVERNANCE_READY

CD-05
= APPROVED_FOR_GATE_B

E Knowledge Applicability
= ADJUDICATED

C/D/E Cross-Consistency
= PASS
```

只有达到以上状态，才允许：

```text
Gate B = PASS
```

即使 Gate B PASS：

```text
Gate C / CD-06 EvalSet = still required
CD-07 Implementation Authorization = still required
U04 = still blocked until U03 clinical dependency completion
Production = still blocked
```

---

## 9. 当前 Readiness 判定

```text
Gate A = PASS

C Structural Schema = PASS
C Clinical Content = NOT_STARTED

D Structural Schema = PASS
D Clinical Content = NOT_STARTED

E Structural Schema = PASS
E Applicability Adjudication = NOT_STARTED
E Clinical Release Content = NOT_STARTED

Gate B Readiness = NOT_READY_FOR_CLINICAL_CONTENT_BUILD
Primary Blocker = E_APPLICABILITY_NOT_ADJUDICATED
Secondary Blockers = C/D_REAL_CONTENT_NOT_AVAILABLE
```

当前唯一合理的下一步：

```text
E Knowledge Dependency Applicability Adjudication
```

不得直接进入 C Rule Pack 真实医学规则内容。
