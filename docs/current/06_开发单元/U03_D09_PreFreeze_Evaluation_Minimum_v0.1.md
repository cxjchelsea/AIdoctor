# U03 D09 Pre-Freeze Evaluation Minimum v0.1

> 对象：`PR-U03-D09-001` candidate freeze 前的最低 evaluation 要求。  
> 状态：`MINIMUM_DEFINED / EVAL_CONTENT_REVIEWED / PRE_FREEZE_EVAL_PASS / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> 本文件只定义 D09 candidate freeze 前最低需要验证什么；不等于完整 Gate C / CD-06 Clinical EvalSet。

---

## 1. Boundary

本 minimum 只回答：

```text
D09 deterministic policy candidate
在冻结正式 Risk Disposition mapping 前
最低需要哪些可审核 evaluation evidence？
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
D Pre-Freeze Eval PASS
!= Gate C PASS
```

---

## 2. Required Evaluation Asset Groups

至少需要以下 8 组：

```text
EVAL-U03-D-P0-INTEGRITY-SCOPE-FIXTURES
EVAL-U03-D-P1-HIGH-PRECEDENCE-FIXTURES
EVAL-U03-D-P2-INSUFFICIENCY-FIXTURES
EVAL-U03-D-P3-CAUTION-FIXTURES
EVAL-U03-D-P4-NO-HIGH-COVERAGE-FIXTURES
EVAL-U03-D-P5-CONFLICT-FIXTURES
EVAL-U03-D-COVERAGE-CONTRACT-FIXTURES
EVAL-U03-D-RELEASE-VERSION-MISMATCH-FIXTURES
```

每组至少需要：

```text
fixture_id
bound_policy_draft_ref = PR-U03-D09-001@0.2.0-draft
bound_rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
bound_knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
bound_coverage_contract_ref = U03_D09_COVERAGE_V0_2 where applicable
input C execution results / coverage states
expected D result_status
expected disposition or NONE
expected reason_code
expected matched/insufficient/not-applicable refs retention
negative assertions
review status
```

---

## 3. Minimum Coverage

### P0 — Integrity / Overall Scope

至少验证：

```text
stale clinical_state_version
wrong/unresolvable rule release
wrong/unresolvable knowledge release
wrong/unresolvable coverage contract
invalid accepted evidence binding
dependency failure
overall population scope mismatch
overall region scope mismatch
overall channel scope mismatch
```

expected：

```text
FAILED / disposition NONE
```

且 overall scope mismatch 必须使用：

```text
OVERALL_POLICY_SCOPE_MISMATCH
```

不得混用 C `RULE_SIGNAL_SCOPE_MISMATCH`。

### P1 — HIGH Precedence

至少验证：

```text
CRITICAL_RED_FLAG → HIGH_RISK
MUST_NOT_MISS → HIGH_RISK
SEPSIS_HIGH_RISK_CRITERION → HIGH_RISK
HIGH + CAUTION → HIGH_RISK
HIGH + INSUFFICIENT_APPLICABLE → HIGH_RISK + retain insufficient refs
```

前提：无 P0 integrity/scope failure。

### P2 — Insufficiency Fail-Closed

至少验证：

```text
no HIGH + baseline INPUT_INSUFFICIENT
no HIGH + dyspnoea family INSUFFICIENT_APPLICABLE
no HIGH + sepsis family INSUFFICIENT_APPLICABLE
CAUTION + INSUFFICIENT_APPLICABLE
```

expected：

```text
FAILED / NONE / INSUFFICIENT_INFORMATION
```

不得输出 CAUTION 或 NO_HIGH_RISK_SIGNAL。

### P3 — CAUTION

至少验证：

```text
SEPSIS_MODERATE_HIGH_RISK_CRITERION
+ no P0
+ no HIGH
+ no P2 insufficiency
→ VALID + CAUTION
```

并验证全部 contributing `matched_rule_refs[]` 被保留。

### P4 — NO_HIGH_RISK_SIGNAL

至少验证：

```text
overall D scope satisfied
5 ALWAYS_APPLICABLE rules = APPLICABLE_EVALUATED / NO_MATCH
NHS_DYSPNOEA_FAMILY = APPLICABLE_EVALUATED or NOT_APPLICABLE
NG253_SEPSIS_FAMILY = APPLICABLE_EVALUATED or NOT_APPLICABLE
no INSUFFICIENT_APPLICABLE
no HIGH
no CAUTION
all denominator-included rules = NO_MATCH
→ VALID + NO_HIGH_RISK_SIGNAL
```

并验证以下反例均阻断 P4：

```text
suspected_sepsis UNKNOWN
NHS dyspnoea context UNKNOWN
baseline rule INPUT_INSUFFICIENT
conditional family INPUT_INSUFFICIENT
```

### P5 — No Unique Legal Decision

至少验证：

```text
coverage/family state internally inconsistent
precedence resolution cannot determine unique legal branch
```

expected：

```text
FAILED / NONE / UNRESOLVABLE_CONFLICT
```

### Coverage Contract

必须验证：

```text
5 baseline rules cannot be removed from denominator when overall D scope holds
C RULE_SIGNAL_SCOPE_MISMATCH → NOT_APPLICABLE
C RULE_SIGNAL_INPUT_INSUFFICIENT → INSUFFICIENT_APPLICABLE
specialized-family NOT_APPLICABLE != overall D failure
condition family mixed incompatible states fail closed
```

### Version / Release Mismatch

至少验证错误或不可解析的：

```text
rule_release_ref
knowledge_release_ref
coverage_contract_ref
policy release/candidate ref where applicable
clinical_state_version currentness
```

不得继续形成合法临床 disposition。

---

## 4. Cross-Cutting Assertions

必须验证：

```text
P0 > P1 > P2 > P3 > P4 > P5
FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
Rule Signal != D09 Disposition
D09 Decision != U04 Safety Gate Decision
HIGH 不被另一条 insufficiency 降级
CAUTION + insufficiency fail closed
P4 只有 coverage complete 才允许
```

---

## 5. Pre-Freeze Review Requirement

candidate freeze 前最低要求：

```text
all 8 asset groups = CONTENT_AVAILABLE
all required fixtures = REVIEWED
Medical review = APPROVE
Technical/Eval review = APPROVE
blocking eval finding = 0
```

这仍不等于 Gate C。

---

## 6. Current Status

```text
D Pre-Freeze Evaluation Minimum = DEFINED
Evaluation Asset Identities = DEFINED
Fixture Content = REVIEWED / 48
Medical/Eval Review = COMPLETE / APPROVE
D Pre-Freeze Eval PASS = YES
Gate C = NOT_PASSED
```

因此当前：

```text
BLOCKER-FZ-D-02 = CLOSED
BLOCKER-FZ-D-03 = OPEN
D policy candidate freeze = BLOCKED
CD-05 = NOT_PASSED
```
