# U03 Clinical Input Package Specification

> 目的：定义医学侧必须提供给 U03 的最小、可审核、可版本化输入包。  
> 本文件定义结构，不提供任何具体医学阈值、红旗规则或风险判定内容。

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

因此真实 C02 / D09 实现前，必须先有一个医学 Owner 可签字确认的输入包。

---

## 2. 必交文件 A：Clinical Risk Semantics

至少定义 Risk Evidence / Red Flag / Must-not-miss / Vital-sign Safety Signal / Risk Factor 以及 UNKNOWN / UNMEASURED / AMBIGUOUS / CONFLICTING / FAILED 的正式语义。

当前结构文件：

```text
U03_Clinical_Risk_Semantics.md
```

当前状态：

```text
STRUCTURAL_SEMANTICS_FROZEN / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED
```

---

## 3. 必交文件 B：Evidence Catalog

Evidence Catalog 必须是受治理、可版本化目录，不直接拥有 Risk Disposition。

当前结构文件：

```text
U03_Evidence_Catalog_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN / CLINICAL_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED
```

---

## 4. 必交文件 C：Safety-critical Risk Rule Pack

每条 rule 必须是结构化、可测试、可版本化对象，而不是自由文本提示词。Rule Pack 必须显式携带 release/version/scope/review/effective time/source/provenance/supersede/rollback 等治理信息。

当前结构文件：

```text
U03_Safety_Critical_Risk_Rule_Pack_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN / CLINICAL_RULE_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED
```

---

## 5. 必交文件 D：D09 Clinical Policy Table

D09 是正式 Clinical Risk Disposition Owner。Policy Table 必须显式定义 branch、preconditions、required evidence/rule refs、priority、precedence、result status、disposition、reason code、failure behavior、release refs、source/provenance 与 review 状态。

当前结构文件：

```text
U03_D09_Clinical_Policy_Table_Schema.md
```

当前状态：

```text
STRUCTURAL_SCHEMA_FROZEN / CLINICAL_POLICY_CONTENT_PENDING / MEDICAL_OWNER_REVIEW_REQUIRED / NOT_APPROVED
```

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

当前状态：

```text
NOT_STARTED / NOT_ADJUDICATED
```

---

## 7. 必交文件 F：Risk EvalSet / Safety Suite

每个 golden case 至少应绑定 case id、Clinical State fixture、输入事实、expected evidence/rule/policy outcome、negative assertions、release refs、rationale/source refs 与 review owner。

当前状态：

```text
NOT_STARTED
```

---

## 8. 完整输入包验收条件

Clinical Input Package 只有同时满足以下条件才可进入 CD-07 Implementation：

```text
A Clinical Risk Semantics = APPROVED
B Evidence Catalog = APPROVED
C Rule Pack Specification / initial release = APPROVED
D D09 Policy Table = APPROVED
E Knowledge Release Manifest = APPROVED or explicitly NOT_REQUIRED
F EvalSet / Safety Suite = REVIEW_READY
```

并且：

- 每个医学判断有 owner；
- 每个关键规则可追溯 source；
- 每个 release 有 version/scope/effective time；
- EvalSet 与实现代码独立；
- 不存在“开发自行补齐”的未审医学规则。

---

## 9. 当前状态

```text
A = STRUCTURAL_SEMANTICS_FROZEN / NOT_APPROVED
B = STRUCTURAL_SCHEMA_FROZEN / CONTENT_PENDING / NOT_APPROVED
C = STRUCTURAL_SCHEMA_FROZEN / CONTENT_PENDING / NOT_APPROVED
D = STRUCTURAL_SCHEMA_FROZEN / CONTENT_PENDING / NOT_APPROVED
E = NOT_STARTED / NOT_ADJUDICATED
F = NOT_STARTED

Clinical Input Package = NOT_COMPLETE
Medical Owner Approval = NOT_COMPLETE
CD-07 Implementation Readiness = BLOCKED
```

下一步应进入 E / Knowledge Release Manifest 的结构与适用性判定。