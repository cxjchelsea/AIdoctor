# U03 C Pre-Freeze Evaluation Minimum v0.2

> 对象：`RR-U03-RISK-001` candidate freeze 前的最低 evaluation 要求。  
> 状态：`MINIMUM_DEFINED / EVAL_CONTENT_NOT_BUILT / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 目的：明确“candidate freeze 前至少要验证什么”，并与完整 Gate C / CD-06 Clinical EvalSet 分离。

---

## 1. Boundary

本 minimum 只回答：

```text
一个 C Rule Release candidate
在冻结 rule/result vocabulary 前
最低需要哪些可审查 evaluation evidence？
```

它不等于：

```text
Clinical EvalSet complete
Safety Suite complete
Gate C PASS
Production validation
```

因此：

```text
Pre-Freeze Eval PASS
!= Gate C PASS
```

---

## 2. Required Asset Groups

必须绑定 `U03_C_EVAL_REFS_V0_2` 中至少以下 8 组 asset identity：

```text
EVAL-U03-C-POSITIVE-FIXTURES
EVAL-U03-C-NEGATIVE-FIXTURES
EVAL-U03-C-MISSING-UNKNOWN-FIXTURES
EVAL-U03-C-SCOPE-MISMATCH-FIXTURES
EVAL-U03-C-CONTEXT-INDEPENDENCE-FIXTURES
EVAL-U03-C-SBP-BRANCH-FIXTURES
EVAL-U03-C-MULTI-RULE-COEXISTENCE-FIXTURES
EVAL-U03-C-VERSION-RELEASE-MISMATCH-FIXTURES
```

每组必须至少有：

```text
fixture_id
bound_rule_release_ref
bound_knowledge_release_ref
bound_policy_refs[] where applicable
clinical_state_fixture / accepted evidence fixture
expected C execution result
expected rule-level signal where applicable
negative assertions
rationale / source refs
review status
```

---

## 3. Minimum Coverage

### Positive
- 15 条 active rule 每条至少 1 个合法 MATCHED fixture；
- numeric threshold 边界至少覆盖等号边界：RR 25、RR 21/24、SBP 90/91/100、HR 130/131。

### Negative
- scope 满足、required input 完整且 predicate 明确 false 时才能 NO_MATCH；
- 不得通过缺失数据制造 NO_MATCH。

### Missing / Unknown
至少覆盖：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
```

expected：不得静默转 negative；应按 policy 形成 INPUT_INSUFFICIENT。

### Scope Mismatch
至少覆盖：

```text
age < 16
pregnancy/recent-pregnancy == TRUE
suspected_sepsis == FALSE
setting outside source-supported community/custodial
NHS dyspnoea emergency context == FALSE
```

expected：SCOPE_MISMATCH，而不是 NO_MATCH。

### BF-C-04 Context Independence
必须有正反 fixture 证明：

```text
RR / SBP / HR / appearance / rash
单独或组合
不得创建/升级 suspected_sepsis

EV-RF-APPEAR-001 + EV-RF-NEURO-001
单独或组合
不得由当前 pack 创建 NHS dyspnoea emergency context

current rule result
不得反向创建 current execution scope context
```

若 context provenance independence 无法证明：

```text
expected = INPUT_INSUFFICIENT
```

### SBP Branch
至少覆盖：

```text
SBP <= 90, usual unknown
→ HIGH MATCHED

SBP 91..100, usual unknown
→ MODHIGH MATCHED
+ HIGH relative-drop INPUT_INSUFFICIENT

SBP >100, usual unknown
→ MODHIGH NO_MATCH
+ HIGH relative-drop INPUT_INSUFFICIENT

usual known, drop >40
→ HIGH MATCHED
```

### Multi-rule Coexistence
验证不同 rule 的 MATCHED / INPUT_INSUFFICIENT 可共存；C 不做 first-hit-wins 或 disposition precedence。

### Version / Release Mismatch
至少验证：

```text
wrong knowledge_release_ref
wrong rule/policy version
unresolvable policy ref
stale clinical_state_version where applicable
```

不得静默继续执行成合法 C result。

---

## 4. Pre-Freeze Review Requirement

candidate freeze 前最低要求：

```text
all 8 required asset groups = CONTENT_AVAILABLE
all required fixtures = REVIEWED
clinical review = APPROVE
technical/eval review = APPROVE
blocking eval finding = 0
```

这里的 clinical/technical/eval review 可以是 pre-freeze review；完整 CD-06 / Gate C 仍可要求更大的独立 golden set、回归集、扰动集和生产前安全评估。

---

## 5. Current Status

```text
Minimum Evaluation Contract = DEFINED
Evaluation Asset Identities = AVAILABLE
Fixture Content = NOT_STARTED
Pre-Freeze Eval Review = NOT_STARTED
Pre-Freeze Eval PASS = NO
Gate C = NOT_PASSED
```

因此当前仍：

```text
RR-U03-RISK-001 candidate freeze = BLOCKED
D drafting = BLOCKED
```
