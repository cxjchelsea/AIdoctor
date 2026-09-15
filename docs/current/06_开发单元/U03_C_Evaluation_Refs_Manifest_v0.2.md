# U03 C Evaluation Refs Manifest v0.2

> 对象：`RR-U03-RISK-001@0.2.0-draft` 的 evaluation reference 绑定清单。  
> 状态：`REFERENCE_STRUCTURE_AVAILABLE / MINIMUM_PRE_FREEZE_FIXTURE_CONTENT_AVAILABLE / REVIEW_PENDING / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 目的：让 C candidate-freeze 的 `evaluation_refs[]` 有可解析治理对象并绑定最低 pre-freeze fixture content；本文件不伪装成完整 EvalSet 已完成。

## 1. Manifest Identity

```text
manifest_id = U03_C_EVAL_REFS_V0_2
rule_release_ref = RR-U03-RISK-001@0.2.0-draft
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
policy_pair_ref = PF-U03-C-POLICY-001
fixture_pack_ref = U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
review_record_ref = U03_C_PreFreeze_Eval_Review_Record_v0.2.md
```

## 2. Required Evaluation References

当前 C freeze 至少需要下列 8 组 evaluation 资产：

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

当前状态：

```text
8 / 8 asset identities = RESOLVABLE
8 / 8 minimum pre-freeze fixture groups = CONTENT_AVAILABLE
Medical review = NOT_STARTED
Technical/Eval review = NOT_STARTED
```

这些内容仅满足“minimum pre-freeze fixture 已构建”，不表示对应 clinical golden set / full Safety Suite 已经完成或通过。

## 3. Minimum Coverage Contract

### Positive fixtures
- 验证 15 条 active rule 的合法 MATCHED 路径；
- 覆盖 RR 21/24/25、SBP 90/91/100、HR 91/130/131 与 SBP relative-drop >40。

### Negative fixtures
验证 scope 满足、输入完整、predicate 明确 false 时才形成 `NO_MATCH`。

### Missing / unknown fixtures
覆盖：

```text
UNKNOWN
UNMEASURED
NOT_ASKED
AMBIGUOUS
CONFLICTING
REMOTE_NOT_OBSERVED
INVALID
```

并验证不会静默变成 NO_MATCH。

### Scope mismatch fixtures
覆盖：

```text
age < 16
pregnancy/recent-pregnancy == TRUE
suspected_sepsis == FALSE
setting outside community/custodial
NHS dyspnoea emergency context == FALSE
```

### Context independence fixtures
验证 BF-C-04：

```text
RR / SBP / HR / appearance / rash
单独或组合
不得建立 suspected_sepsis

EV-RF-APPEAR-001 / EV-RF-NEURO-001
单独或组合
不得由当前 pack 创建 NHS dyspnoea emergency context

current rule result
不得反向创建 current execution scope context
```

### SBP branch fixtures
覆盖：

```text
SBP <= 90, usual unknown → HIGH MATCHED
SBP 91..100, usual unknown → MODHIGH MATCHED + HIGH-drop INPUT_INSUFFICIENT
SBP >100, usual unknown → HIGH-drop INPUT_INSUFFICIENT + MODHIGH NO_MATCH
usual known and drop >40 → HIGH MATCHED
```

### Multi-rule coexistence
验证不同 rule 的 MATCHED / INPUT_INSUFFICIENT 可以并存，且 C 不做 D09 disposition precedence。

### Version / release mismatch
验证错误 knowledge release / policy pair / unresolvable ref / stale clinical state 不得静默生成合法 C result。

## 4. Bound Fixture Content

最低 pre-freeze fixture pack：

```text
U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md
```

覆盖统计：

```text
Positive = 20
Negative = 8
Missing/unknown = 7
Scope mismatch = 5
Context independence = 6
SBP branch = 4
Multi-rule coexistence = 3
Version/release mismatch = 4
Total = 57
```

该 pack 当前：

```text
Fixture Content = AVAILABLE
Medical Review = NOT_STARTED
Technical/Eval Review = NOT_STARTED
Pre-Freeze Eval PASS = NO
```

## 5. Full Gate C Boundary

必须保持：

```text
Minimum pre-freeze fixture content available
!=
Pre-Freeze Eval reviewed/passed
!=
Clinical EvalSet complete
!=
Safety Suite complete
!=
Gate C PASS
```

完整 CD-06 / Gate C 后续仍可要求更大的独立 golden set、回归集、扰动集和生产前安全评估。

## 6. Current Status

```text
evaluation_refs structure = AVAILABLE
minimum pre-freeze fixture content = AVAILABLE
Medical/Eval Owner Review = NOT_COMPLETE
Pre-Freeze Eval PASS = NO
Independent Evaluation = NOT_READY
Gate C = NOT_PASSED

BLOCKER-FZ-C-03 = OPEN / CONTENT_BUILT_REVIEW_PENDING
BLOCKER-FZ-C-04 = OPEN / MUST_REMAIN_LAST
RR-U03-RISK-001@0.2.0-draft = NOT_FROZEN
D = STILL_BLOCKED
```

下一步只允许对 `U03_C_PreFreeze_Evaluation_Fixtures_v0.2.md` 做 Medical + Technical/Eval review。