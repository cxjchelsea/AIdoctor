# U03 C Evaluation Refs Manifest v0.2

> 对象：`RR-U03-RISK-001@0.2.0-draft` 的 evaluation reference 绑定清单。  
> 状态：`REFERENCE_STRUCTURE_AVAILABLE / CLINICAL_EVAL_CONTENT_NOT_COMPLETE / NOT_REVIEW_READY / NOT_FOR_PRODUCTION`。  
> 目的：让 C candidate-freeze 的 `evaluation_refs[]` 有可解析治理对象；本文件不伪装成 EvalSet 已完成。

## 1. Manifest Identity

```text
manifest_id = U03_C_EVAL_REFS_V0_2
rule_release_ref = RR-U03-RISK-001@0.2.0-draft
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
```

## 2. Required Evaluation References

当前 C freeze 至少需要下列 evaluation 资产存在并完成审核：

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

这些 refs 当前只是治理身份，不表示对应 golden cases 已经编写或通过。

## 3. Minimum Coverage Contract

### Positive fixtures
验证 15 条 active rule 的合法 MATCHED 路径。

### Negative fixtures
验证 scope 满足、输入完整、predicate 明确 false 时才形成 `NO_MATCH`。

### Missing / unknown fixtures
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

并验证不会静默变成 NO_MATCH。

### Scope mismatch fixtures
至少覆盖：

```text
age < 16
pregnancy/recent-pregnancy == TRUE
suspected_sepsis == FALSE
setting outside community/custodial
NHS dyspnoea emergency context == FALSE
```

### Context independence fixtures
必须验证 BF-C-04：

```text
RR/SBP/HR/appearance/rash
不得建立 suspected_sepsis

EV-RF-APPEAR-001 / EV-RF-NEURO-001
不得单独建立 NHS dyspnoea emergency context

current rule result
不得反向创建 current execution scope context
```

### SBP branch fixtures
至少覆盖：

```text
SBP <= 90, usual unknown → HIGH MATCHED
SBP 91..100, usual unknown → MODHIGH MATCHED + HIGH-drop INPUT_INSUFFICIENT
SBP >100, usual unknown → HIGH-drop INPUT_INSUFFICIENT + MODHIGH NO_MATCH
usual known and drop >40 → HIGH MATCHED
```

### Multi-rule coexistence
验证不同 rule 的 MATCHED / INPUT_INSUFFICIENT 可以并存，且 C 不做 D09 disposition precedence。

## 4. Current Status

```text
evaluation_refs structure = AVAILABLE
clinical golden-case content = NOT_STARTED
Medical/Eval Owner Review = NOT_COMPLETE
Independent Evaluation = NOT_READY
Gate C = NOT_PASSED
```

因此：

```text
Evaluation refs resolvable identity
!=
Evaluation content complete
!=
Candidate freeze approved
```

本 manifest 只能解除“evaluation_refs 无任何可解析治理身份”的结构缺口，不能解除 clinical evaluation blocker。