# U03 Clinical Input Package Specification

> 目的：定义医学侧必须提供给 U03 的最小、可审核、可版本化输入包。  
> 本文件定义结构与当前门禁状态，不提供任何具体医学阈值、红旗规则或风险判定内容。

---

## 1. 为什么需要独立 Clinical Input Package

U03 当前工程链已经可以安全承载临床能力，但工程侧不能从代码结构反推出医学规则。若缺少正式输入包，最容易出现：

```text
开发者自行补医学常识
→ 规则散落在代码 / Prompt
→ 无法追溯来源
→ 无法审核版本
→ 无法证明 Eval 覆盖
→ 生产变更无法治理
```

因此真实 C02 / D09 实现前，必须先有一个医学 Owner 可审核、可版本化、可追溯的输入包。

---

## 2. 必交文件 A：Clinical Risk Semantics

至少定义 Risk Evidence / Red Flag / Must-not-miss / Vital-sign Safety Signal / Risk Factor 以及 UNKNOWN / UNMEASURED / AMBIGUOUS / CONFLICTING / FAILED 的正式语义。

当前结构文件：

```text
U03_Clinical_Risk_Semantics.md
```

当前内容与审核状态：

```text
A Content Draft v0.2 = AVAILABLE
A item-level verdict = APPROVE 7 / REVISE 0
A source-locked semantics = FROZEN_FOR_GATE_A
Gate A contribution = PASS
```

这不等于整个 Clinical Input Package 已批准，也不等于生产授权。

---

## 3. 必交文件 B：Evidence Catalog

Evidence Catalog 必须是受治理、可版本化目录，不直接拥有 Risk Disposition。

当前结构文件：

```text
U03_Evidence_Catalog_Schema.md
```

当前内容与审核状态：

```text
B Content Draft v0.2 = AVAILABLE
B item-level verdict = APPROVE 11 / REVISE 0
B source-locked evidence semantics = FROZEN_FOR_GATE_A
Gate A contribution = PASS
```

当前 B 条目只允许作为后续 C 的受治理 evidence 输入；不得直接解释为 Rule、D09 Decision、HIGH_RISK 或 Safety Gate。

---

## 4. 必交文件 C：Safety-critical Risk Rule Pack

每条 rule 必须是结构化、可测试、可版本化对象，而不是自由文本提示词。Rule Pack 必须显式携带 release/version/scope/review/effective time/source/provenance/supersede/rollback 等治理信息。

当前结构文件：

```text
U03_Safety_Critical_Risk_Rule_Pack_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_RULE_CONTENT = NOT_STARTED
MEDICAL_OWNER_REVIEW = NOT_COMPLETE
INITIAL_RULE_RELEASE = NOT_AVAILABLE
CD-03 = NOT_PASSED
```

C 的真实内容不得在 E applicability 尚未裁定前直接开始。

---

## 5. 必交文件 D：D09 Clinical Policy Table

D09 是正式 Clinical Risk Disposition Owner。Policy Table 必须显式定义 branch、preconditions、required evidence/rule refs、priority、precedence、result status、disposition、reason code、failure behavior、release refs、source/provenance 与 review 状态。

当前结构文件：

```text
U03_D09_Clinical_Policy_Table_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN
CLINICAL_POLICY_CONTENT = NOT_STARTED
MEDICAL_PRIORITY_PRECEDENCE = NOT_AVAILABLE
MEDICAL_OWNER_REVIEW = NOT_COMPLETE
POLICY_RELEASE = NOT_AVAILABLE
CD-05 = NOT_PASSED
```

D 的真实 policy 必须基于稳定的 governed evidence/rule refs，不能先于 C 的实际 rule/result vocabulary 成型。

---

## 6. 必交文件 E：Knowledge Release Manifest

若 Risk Engine / Rule Pack / D09 需要医学知识内容，每个 Knowledge Release 至少应提供：

```text
knowledge_release_id
version
content_scope
source_reference_ids
source_versions / publication dates where applicable
curation_method
review_owner
review_status
applicable scope
effective_from / effective_to
supersedes
rollback_ref
```

系统只绑定正式 release ref；不得把“最新知识”作为未版本化隐式依赖。

当前结构文件：

```text
U03_Knowledge_Release_Manifest_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN
APPLICABILITY_ADJUDICATION = NOT_STARTED
CLINICAL_RELEASE_CONTENT = NOT_STARTED
MEDICAL_OWNER_REVIEW = NOT_COMPLETE
CD-04 = NOT_READY
```

当前工程事实：`U03ReleaseBinding` 要求 `knowledgeReleaseId` 与 `knowledgeReleaseVersion` 非空。因此：

```text
NOT_REQUIRED
!=
留空 knowledge release ref
```

必须先完成 E applicability adjudication。若未来裁定 `NOT_REQUIRED`，还必须确认工程合同是否需要显式支持该一等状态；该实现变化不属于当前 #88 docs-only scope。

---

## 7. 必交文件 F：Risk EvalSet / Safety Suite

每个 golden case 至少应绑定 case id、Clinical State fixture、输入事实、expected evidence/rule/policy outcome、negative assertions、release refs、rationale/source refs 与 review owner。

当前结构文件：

```text
U03_Risk_EvalSet_Safety_Suite_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN
GOLDEN_CASE_CONTENT = NOT_STARTED
MEDICAL_OWNER_REVIEW = NOT_COMPLETE
EVALUATION_OWNER_REVIEW = NOT_COMPLETE
CD-06 = NOT_REVIEW_READY
Gate C = NOT_PASSED
```

---

## 8. 完整输入包验收条件

Clinical Input Package 只有同时满足以下条件才可进入 CD-07 Implementation：

```text
A Clinical Risk Semantics = PASSED_FOR_GATE_A
B Evidence Catalog = PASSED_FOR_GATE_A
C Rule Pack Specification / initial release = APPROVED
D D09 Policy Table = APPROVED
E Knowledge Release Manifest = APPROVED or explicitly NOT_REQUIRED with compatible governed contract
F EvalSet / Safety Suite = REVIEW_READY
```

并且：

- 每个医学判断有 owner；
- 每个关键规则可追溯 source；
- 每个 release 有 version/scope/effective time；
- EvalSet 与实现代码独立；
- 不存在“开发自行补齐”的未审医学规则；
- C/D/E 的 evidence/rule/knowledge refs 可相互解析；
- 不存在 scope 超出 Gate A 的 source lock。

---

## 9. Gate B 顺序

当前不得把 Gate A PASS 解释为可以直接开始 C。

推荐顺序：

```text
Gate A = PASS
↓
E Knowledge Dependency Applicability Adjudication
↓
C Initial Rule Pack Clinical Content
↓
D D09 Clinical Policy Content
↓
C/D/E Cross-release Consistency Review
↓
Gate B Decision
```

详细拆解见：

```text
U03_Gate_B_Readiness_Decomposition.md
```

---

## 10. 当前状态

```text
A = PASS_FOR_GATE_A
B = PASS_FOR_GATE_A
Gate A = PASS

C = STRUCTURAL_SCHEMA_FROZEN / CLINICAL_CONTENT_NOT_STARTED
D = STRUCTURAL_SCHEMA_FROZEN / CLINICAL_CONTENT_NOT_STARTED
E = STRUCTURAL_SCHEMA_FROZEN / APPLICABILITY_NOT_ADJUDICATED
F = STRUCTURAL_SCHEMA_FROZEN / GOLDEN_CASE_CONTENT_NOT_STARTED

Gate B = NOT_PASSED
Gate C = NOT_PASSED
Clinical Input Package = NOT_COMPLETE
Medical Owner Approval = NOT_COMPLETE
CD-07 Implementation Readiness = BLOCKED
U04 = BLOCKED_BY_U03_CLINICAL_DEPENDENCY
```

当前唯一合理的下一步：

```text
E Knowledge Dependency Applicability Adjudication
```

不得直接进入 C Rule Pack 真实医学内容。
