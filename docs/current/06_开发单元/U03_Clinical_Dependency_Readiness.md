# U03 Clinical Dependency Readiness

> 阶段：U03 Clinical Dependency Completion / Readiness  
> 基线：`impl/u03-current-version-risk-assessment@b6913433b72a7156855db048f524efdd5175abd6`  
> 本文件判断是否可以进入真实 C02/D09 临床依赖实现；不构成 Implementation Authorization。

---

## 1. 已满足的工程前置

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
```

这些前置足以承载后续真实临床内容，无需新增 Foundation。

---

## 2. 临床前置当前状态

A / Clinical Risk Semantics 已形成第一版结构语义文件：

```text
U03_Clinical_Risk_Semantics.md
```

当前状态：

```text
CD-01 Clinical Risk Semantics
= STRUCTURAL_SEMANTICS_FROZEN
/ MEDICAL_CONTENT_PENDING
/ MEDICAL_OWNER_REVIEW_REQUIRED
/ NOT_APPROVED
```

它已经冻结：

- Risk Evidence != Clinical Risk Disposition；
- `UNKNOWN != NO`；
- `UNMEASURED != NORMAL`；
- `FAILED != NO_HIGH_RISK_SIGNAL`；
- `NO_HIGH_RISK_SIGNAL != SAFE`；
- `Candidate != Decision != Proposal != Commit`；
- C02 / D09 / U04 Owner 边界；
- currentness / Clinical State Version 绑定；
- AMBIGUOUS / CONFLICTING 不得静默解释为 negative；
- VALID / FAILED 与 disposition 分层。

但以下内容仍必须由医学 Owner 审核/提供：

```text
具体 Red Flag / Must-not-miss 定义
生命体征字段与阈值
Risk Factor Catalog
Combination Rules
source type 的医学证据强度
关键歧义/冲突的医学处理原则
D09 具体 branch / priority / precedence
population / region / scope 差异
```

其余 Clinical Input Package 当前仍缺失：

```text
Evidence Catalog = MISSING
Safety-critical Risk Rule Pack content = MISSING
Knowledge Release clinical content = MISSING / NOT_ADJUDICATED
D09 Clinical Policy Table = MISSING
Risk EvalSet / Safety Suite = MISSING
Medical Owner Approval = MISSING
```

因此真实 C02 Risk Evidence Engine / D09 policy 仍不能由工程侧自行推导。

---

## 3. Readiness Gate

进入 CD-07 `C02 / D09 Clinical Implementation` 之前必须满足：

### Gate A — Clinical Semantics Frozen

```text
CD-01 APPROVED
CD-02 APPROVED
```

当前：

```text
CD-01 = REVIEW_REQUIRED / NOT_APPROVED
CD-02 = NOT_STARTED
Gate A = NOT_PASSED
```

### Gate B — Governed Content Ready

```text
CD-03 APPROVED
CD-04 initial release READY
CD-05 APPROVED
```

当前：`NOT_PASSED`

### Gate C — Independent Evaluation Ready

```text
CD-06 REVIEW_READY
```

当前：`NOT_PASSED`

### Gate D — Authorization

在 A/B/C 通过后仍需新的、明确的：

```text
Implementation Authorization
```

上一轮针对 U03-01～U03-11 的授权不自动覆盖 CD-01～CD-08。

---

## 4. 当前 Readiness 判定

```text
Engineering Prerequisites = PASS
Clinical Risk Structural Semantics = AVAILABLE / REVIEW_REQUIRED
Clinical Definition Prerequisites = PARTIAL / NOT_APPROVED
Governed Clinical Content = FAIL / MISSING
Clinical Evaluation Assets = FAIL / MISSING
New Foundation = NOT_REQUIRED
CD-07 Implementation Readiness = BLOCKED
U04 Implementation Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

总判定保持：

```text
U03 Clinical Dependency Readiness
= BLOCKED_BY_CLINICAL_INPUT_PACKAGE
```

这不是代码 blocker，也不是 CI blocker；是受治理临床输入尚未完成并获批。

---

## 5. 现在允许做什么

在没有新的临床输入和授权前，可以继续：

- 医学审核 `U03_Clinical_Risk_Semantics.md`；
- 建立 B / Evidence Catalog 的结构与待审内容；
- 收集医学 Owner 提供的定义和来源；
- 对来源做版本、scope、provenance 整理；
- 建立 EvalSet 的数据结构和案例编写规范；
- 做非临床 schema/validator 设计评审；
- 独立审查当前 U03 工程边界。

不应继续：

- 开发者自行写真实医学 red-flag 阈值；
- 从通用常识直接生成 production Rule Pack；
- 把未审核 Prompt 当 D09 policy；
- 用 synthetic test cases 冒充 Clinical EvalSet；
- 开始 U04 并假定 U03 已临床可用。

---

## 6. 下一道门

Clinical Input Package 最终至少应包含：

```text
A Clinical Risk Semantics
B Evidence Catalog
C Safety-critical Risk Rule Pack
D D09 Clinical Policy Table
E Knowledge Release Manifest（或明确 NOT_REQUIRED）
F Risk EvalSet / Safety Suite
```

当前进展：

```text
A = STRUCTURAL_SEMANTICS_FROZEN / REVIEW_REQUIRED
B = NOT_STARTED
C = NOT_STARTED
D = NOT_STARTED
E = NOT_STARTED / NOT_ADJUDICATED
F = NOT_STARTED
```

下一步应进入：

```text
CD-02 / B Evidence Catalog preparation
```

但具体 evidence 内容仍必须保持 `TBD_BY_MEDICAL_OWNER`，直到有可追溯医学来源与 Owner 审核。