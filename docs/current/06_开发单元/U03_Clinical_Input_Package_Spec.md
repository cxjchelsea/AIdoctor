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

至少定义：

| 项 | 必须回答的问题 |
|---|---|
| Risk Evidence | 什么可以被 C02 作为风险证据，什么不能 |
| Red Flag | 定义、证据最低要求、适用范围 |
| Must-not-miss | 定义及与 Red Flag 的关系 |
| Vital-sign Safety Signal | 哪些字段属于该类、缺失/未测如何表达 |
| Risk Factor | 单因素与组合因素的边界 |
| UNKNOWN | 未知不得如何解释 |
| UNMEASURED | 未测不得如何解释 |
| AMBIGUOUS | 歧义时能否进入 D09 |
| CONFLICTING | 冲突证据如何处理 |
| FAILED | 哪些情况必须失败闭合 |

禁止：

```text
UNKNOWN -> NEGATIVE
UNMEASURED -> NORMAL
FAILED -> NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL -> SAFE
```

---

## 3. 必交文件 B：Evidence Catalog

每一条受治理 evidence definition 至少包含：

```text
evidence_id
canonical_name
category
clinical_definition
required_input_fields
accepted_source_types
minimum evidence requirement
uncertainty handling
conflict handling
population scope
region scope
language/channel constraints if any
source_reference_ids
owner
review_status
```

category 只能来自冻结 taxonomy，例如：

```text
RED_FLAG
MUST_NOT_MISS
VITAL_SIGN_SAFETY_SIGNAL
RISK_FACTOR
COMBINATION_SIGNAL
```

最终 taxonomy 由医学 Owner 冻结。

---

## 4. 必交文件 C：Safety-critical Risk Rule Pack

每条 rule 必须是结构化、可测试对象，而不是自由文本提示词。

最小字段：

```text
rule_id
rule_version
description
inputs / predicates
required evidence refs
priority
conflict precedence
applicable population / scope
output signal
failure condition
source_reference_ids
review_owner
review_status
```

Rule Pack 自身还必须有：

```text
release_id
release_version
status
effective_from / effective_to
scope
provenance
supersedes
rollback_ref
```

禁止把具体医学规则永久写死在 application service、controller 或 prompt 中作为唯一来源。

---

## 5. 必交文件 D：D09 Clinical Policy Table

D09 是 final Clinical Risk Disposition Owner。医学侧必须提供从 accepted evidence 到 disposition 的确定性决策表/策略定义。

允许输出：

```text
VALID + NO_HIGH_RISK_SIGNAL
VALID + CAUTION
VALID + HIGH_RISK
FAILED
```

每个 policy branch 至少包含：

```text
policy_id
preconditions
required evidence state
rule refs
priority
result status
disposition
reason_code
failure behavior
source/provenance refs
```

必须显式定义：

- 多个规则同时命中时的优先级；
- 证据不足时的处理；
- 冲突证据时的处理；
- rule/knowledge unavailable 时的处理；
- stale Clinical State Version 时的处理；
- 哪些情况必须 FAILED。

---

## 6. 必交文件 E：Knowledge Release Manifest

若 Risk Engine 需要医学知识内容，每个 Knowledge Release 至少提供：

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

系统只绑定 release ref；不得把“最新知识”作为未版本化隐式依赖。

---

## 7. 必交文件 F：Risk EvalSet / Safety Suite

每个 golden case 至少包含：

```text
case_id
clinical_state_version fixture
input facts / assertions
expected evidence refs or expected evidence categories
expected D09 outcome
must_not_output
applicable rule release
applicable knowledge release
rationale/source refs
review_owner
```

至少覆盖：

- 明确高风险；
- 明确 caution；
- 无高风险信号但不得表述为 SAFE；
- unknown / unmeasured；
- evidence conflict；
- evidence ambiguity；
- special population；
- vital-sign missing/abnormal；
- must-not-miss；
- capability failure；
- rule/knowledge unavailable；
- stale version；
- release mismatch。

必须有 negative assertions，例如：

```text
must_not_output = SAFE
must_not_output = NO_HIGH_RISK_SIGNAL when status=FAILED
must_not_convert UNKNOWN to NO
must_not_convert UNMEASURED to NORMAL
```

---

## 8. 完整输入包的验收条件

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
Clinical Input Package = NOT_AVAILABLE
Medical Owner Approval = NOT_AVAILABLE
Rule Content Release = NOT_AVAILABLE
D09 Clinical Policy Release = NOT_AVAILABLE
Risk EvalSet / Safety Suite = NOT_AVAILABLE
```

因此当前只能冻结输入规范，不能宣称真实临床 U03 implementation-ready。