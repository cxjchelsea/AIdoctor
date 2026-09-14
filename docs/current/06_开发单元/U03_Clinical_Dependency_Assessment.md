# U03 Clinical Dependency Assessment

> 阶段：U03 Clinical Dependency Completion  
> 基线：`impl/u03-current-version-risk-assessment@b6913433b72a7156855db048f524efdd5175abd6`  
> 本文件用于拆分 U03 尚未完成的临床依赖，并界定医学定义、规则/知识资产、工程实现与临床评估之间的边界。  
> 不构成 Implementation Authorization、Merge Authorization、Clinical Runtime Enablement 或 Production Authorization。

---

## 1. 当前准确状态

```text
U03 Engineering Governance Path
= IMPLEMENTED / COMPONENT_VERIFIED

U03 Clinical Capability
= NOT_COMPLETE / NOT_CLINICALLY_EVALUATED

U03 Business Loop
= NOT_CLOSED

Production
= BLOCKED
```

U03 当前缺口不是基础 Runtime/Registry/Trace/Committer，而是临床输入与可审核内容。

---

## 2. Clinical Dependency 分层

```text
Layer 1 医学语义定义
↓
Layer 2 Evidence / Rule / Knowledge / Policy 受治理资产
↓
Layer 3 C02 / D09 临床实现
↓
Layer 4 Clinical Eval / Safety Verification
```

禁止从 Layer 3 反推 Layer 1/2 的医学内容。

---

## 3. 工作包

### CD-01 — Clinical Risk Taxonomy & Semantics

目标：冻结 U03 的风险证据与风险裁决基本语义。

当前：

```text
STRUCTURAL_SEMANTICS_FROZEN
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

产物：`U03_Clinical_Risk_Semantics.md`

### CD-02 — Safety-critical Evidence Definition

目标：建立可审核、可版本化 Evidence Catalog。

当前：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_CONTENT_PENDING
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

产物：`U03_Evidence_Catalog_Schema.md`

### CD-03 — Safety-critical Risk Rule Pack Specification

目标：冻结规则对象、predicate、优先级/冲突、scope、release、rollback 与 provenance 的结构。

当前：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_RULE_CONTENT_PENDING
MEDICAL_OWNER_REVIEW_REQUIRED
NOT_APPROVED
```

产物：`U03_Safety_Critical_Risk_Rule_Pack_Schema.md`

### CD-04 — Knowledge / Rule Release Content

目标：形成真实受审的 initial Knowledge/Rule release。

当前：`NOT_STARTED / NOT_ADJUDICATED`

### CD-05 — D09 Clinical Policy Definition

目标：冻结并审核 accepted evidence / rule results 到正式 Risk Disposition 的确定性策略。

当前：`NOT_STARTED`

### CD-06 — Risk EvalSet / Safety Suite

目标：建立独立于实现代码的临床评估资产。

当前：`NOT_STARTED`

### CD-07 — C02 / D09 Clinical Implementation

目标：在 A/B/C/D/E/F 受审后实现真实临床能力。

当前：`BLOCKED`

### CD-08 — Clinical Verification & Release Readiness

目标：完成临床验证、发布准备与生产前门禁。

当前：`NOT_STARTED`

---

## 4. 当前已有与缺失

已有：

```text
U03 governed invocation boundary
candidate-only capability contract
evidence acceptance/version consistency boundary
minimal P04/P06 release binding
D09 owner boundary
typed proposal + P01 commit
release-aware P05 trace
engineering tests/regression
A structural semantics
B Evidence Catalog schema
C Risk Rule Pack schema
```

仍缺：

```text
Medical Owner approval for A/B/C
reviewed concrete Evidence Catalog entries
reviewed concrete Risk Rule Pack content
initial Knowledge/Rule releases
D09 Clinical Policy Table
Risk EvalSet / Safety Suite
real clinical C02/D09 implementation
clinical verification
```

---

## 5. Foundation 判断

```text
New Foundation = NOT_REQUIRED
```

理由：现有 Foundation-1 + U03 minimal P04/P06/P05/P01 增量已经具备承载后续受治理临床内容的最小工程能力。当前 blocker 是内容/审核/评估，不是平台基础设施。

---

## 6. 当前 Gate

```text
Engineering Prerequisites = PASS
Clinical Structural Preparation = PARTIAL / AVAILABLE
Medical Owner Approval = NOT_COMPLETE
Governed Clinical Content = NOT_COMPLETE
Clinical Evaluation Assets = NOT_COMPLETE
CD-07 Implementation Readiness = BLOCKED
U04 Readiness = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

---

## 7. 下一步

按依赖顺序继续：

```text
D D09 Clinical Policy Table structural preparation
↓
E Knowledge Release Manifest / adjudication
↓
F Risk EvalSet / Safety Suite structure
↓
Medical Owner review + content population
↓
重新执行 Clinical Dependency Readiness
```

在 Gate 通过并获得新的 Implementation Authorization 前，不进入真实 C02/D09 临床实现。
