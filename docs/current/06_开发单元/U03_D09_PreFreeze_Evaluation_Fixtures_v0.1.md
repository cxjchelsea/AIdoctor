# U03 D09 Pre-Freeze Evaluation Fixtures v0.1

> 对象：`PR-U03-D09-001@0.2.0-draft` candidate freeze 前最低 evaluation fixture pack。  
> 契约：`U03_D09_PreFreeze_Evaluation_Minimum_v0.1.md`。  
> 状态：`FIXTURE_CONTENT_AVAILABLE / REVIEW_REQUIRED / PRE_FREEZE_EVAL_NOT_PASSED / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 本文件只验证已审核 D09 policy/coverage 的确定性映射；不新增 C rule、医学阈值、evidence、来源或 U04 语义。

---

## 1. 固定绑定

```text
bound_policy_draft_ref = PR-U03-D09-001@0.2.0-draft
bound_rule_release_ref = RR-U03-RISK-001@0.2.0-candidate
bound_knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
bound_coverage_contract_ref = U03_D09_COVERAGE_V0_2
coverage_contract_freeze_ref = U03_D09_Coverage_Contract_Freeze_Record_v0.2.md
```

所有 fixture 默认：

```text
no P0 integrity failure
current clinical_state_version
all release refs resolvable and exact
```

除非 fixture 明确覆盖 P0 / mismatch 场景。

D09 正式输出只允许：

```text
VALID + HIGH_RISK
VALID + CAUTION
VALID + NO_HIGH_RISK_SIGNAL
FAILED + disposition = NONE
```

---

## 2. Asset Group A — P0 Integrity / Overall Scope

Asset：`EVAL-U03-D-P0-INTEGRITY-SCOPE-FIXTURES`

| Fixture | Input | Expected | Negative assertion |
|---|---|---|---|
| D-P0-001 | stale `clinical_state_version` | `FAILED / NONE / STALE_INPUT` | 不得被任何 HIGH signal 覆盖 |
| D-P0-002 | wrong rule release ref | `FAILED / NONE / RELEASE_MISMATCH` | 不得继续做 disposition |
| D-P0-003 | unresolvable knowledge release ref | `FAILED / NONE / RELEASE_MISMATCH` | 不得 fallback 到 mutable/latest knowledge |
| D-P0-004 | wrong/unresolvable coverage contract ref | `FAILED / NONE / RELEASE_MISMATCH` | 不得使用未冻结 denominator |
| D-P0-005 | invalid accepted-evidence binding | `FAILED / NONE / INVALID_INPUT` | 不得作为 NO_MATCH 消费 |
| D-P0-006 | dependency failure prevents trustworthy C input | `FAILED / NONE / DEPENDENCY_FAILURE` | 不得解释为无高风险信号 |
| D-P0-007 | outside overall population scope | `FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH` | 不得使用 C `RULE_SIGNAL_SCOPE_MISMATCH` reason |
| D-P0-008 | outside overall region scope | `FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH` | 不得进入 P4 |
| D-P0-009 | outside overall channel scope | `FAILED / NONE / OVERALL_POLICY_SCOPE_MISMATCH` | 不得进入 P4 |
| D-P0-010 | stale state + valid CRITICAL_RED_FLAG result simultaneously present | `FAILED / NONE / STALE_INPUT` | 验证 `P0 > P1` |

Review status：`PENDING`。

---

## 3. Asset Group B — P1 HIGH Precedence

Asset：`EVAL-U03-D-P1-HIGH-PRECEDENCE-FIXTURES`

| Fixture | Governed C result / coverage | Expected D09 | Retention |
|---|---|---|---|
| D-P1-001 | one applicable `RULE_SIGNAL_CRITICAL_RED_FLAG` | `VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT` | contributing `matched_rule_refs[]` |
| D-P1-002 | one applicable `RULE_SIGNAL_MUST_NOT_MISS` | `VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT` | matched ref retained |
| D-P1-003 | one applicable `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION` | `VALID / HIGH_RISK / HIGH_RISK_RULE_SIGNAL_PRESENT` | matched ref retained |
| D-P1-004 | HIGH-class + `RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION` | `VALID / HIGH_RISK` | both matched refs retained; no CAUTION output |
| D-P1-005 | HIGH-class + one `INSUFFICIENT_APPLICABLE` | `VALID / HIGH_RISK` | matched + `insufficient_rule_refs[]` both retained |
| D-P1-006 | two distinct HIGH-class rule hits | single `VALID / HIGH_RISK` decision | all contributing matched refs retained; no first-hit-wins |

Assertions：

```text
P1 > P2/P3/P4
HIGH 不得被另一条 insufficiency 降级
HIGH + CAUTION 不得输出 CAUTION
```

Review status：`PENDING`。

---

## 4. Asset Group C — P2 Insufficiency Fail-Closed

Asset：`EVAL-U03-D-P2-INSUFFICIENCY-FIXTURES`

| Fixture | Coverage input | Expected |
|---|---|---|
| D-P2-001 | no HIGH + baseline rule `INPUT_INSUFFICIENT` | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| D-P2-002 | no HIGH + `NHS_DYSPNOEA_FAMILY = INSUFFICIENT_APPLICABLE` | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| D-P2-003 | no HIGH + `NG253_SEPSIS_FAMILY = INSUFFICIENT_APPLICABLE` | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| D-P2-004 | CAUTION signal + any `INSUFFICIENT_APPLICABLE` | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| D-P2-005 | `suspected_sepsis` context UNKNOWN → C insufficient | `FAILED / NONE / INSUFFICIENT_INFORMATION` |
| D-P2-006 | NHS dyspnoea context UNKNOWN/provenance unresolved → C insufficient | `FAILED / NONE / INSUFFICIENT_INFORMATION` |

必须保留：

```text
insufficient_rule_refs[] / affected family ref
```

Negative assertions：

```text
不得输出 CAUTION
不得输出 NO_HIGH_RISK_SIGNAL
UNKNOWN 不得折叠为 NOT_APPLICABLE
```

Review status：`PENDING`。

---

## 5. Asset Group D — P3 CAUTION

Asset：`EVAL-U03-D-P3-CAUTION-FIXTURES`

### D-P3-001 — Single moderate-high sepsis signal

```text
no P0
no HIGH-class signal
no INSUFFICIENT_APPLICABLE
NG253_SEPSIS_FAMILY = APPLICABLE_EVALUATED
one result = RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
other applicable denominator results = NO_MATCH
```

Expected：

```text
result_status = VALID
disposition = CAUTION
reason_code = MODERATE_HIGH_RULE_SIGNAL_PRESENT
```

### D-P3-002 — Multiple moderate-high matches

两个或多个 `RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION` 同时命中，且无 P0/P1/P2。

Expected：

```text
single VALID + CAUTION decision
all contributing matched_rule_refs[] retained
```

不得 first-hit-wins。

Review status：`PENDING`。

---

## 6. Asset Group E — P4 NO_HIGH_RISK_SIGNAL / Coverage Complete

Asset：`EVAL-U03-D-P4-NO-HIGH-COVERAGE-FIXTURES`

### D-P4-001 — Both conditional families NOT_APPLICABLE

```text
overall D scope = satisfied
5 ALWAYS_APPLICABLE rules = APPLICABLE_EVALUATED / NO_MATCH
NHS_DYSPNOEA_FAMILY = NOT_APPLICABLE
NG253_SEPSIS_FAMILY = NOT_APPLICABLE
no HIGH / CAUTION / insufficiency
```

Expected：

```text
VALID / NO_HIGH_RISK_SIGNAL
reason = COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL
```

### D-P4-002 — Dyspnoea applicable and fully negative

```text
baseline 5 = NO_MATCH
NHS_DYSPNOEA_FAMILY = APPLICABLE_EVALUATED / both NO_MATCH
sepsis family = NOT_APPLICABLE
```

Expected：`VALID / NO_HIGH_RISK_SIGNAL`。

### D-P4-003 — Sepsis applicable and fully negative

```text
baseline 5 = NO_MATCH
NHS dyspnoea = NOT_APPLICABLE
NG253 sepsis = APPLICABLE_EVALUATED / all denominator members NO_MATCH
```

Expected：`VALID / NO_HIGH_RISK_SIGNAL`。

### D-P4-004 — Both conditional families applicable and fully negative

全部 denominator-included results = NO_MATCH，无 insufficiency。

Expected：`VALID / NO_HIGH_RISK_SIGNAL`。

### D-P4-005 — Baseline insufficient blocks P4

一个 baseline rule = `INSUFFICIENT_APPLICABLE`。

Expected：不得进入 P4；应由 P2 处理。

### D-P4-006 — Sepsis context unknown blocks P4

Expected：不得把 family 标成 `NOT_APPLICABLE`；进入 P2。

### D-P4-007 — Dyspnoea context unknown blocks P4

Expected：不得进入 P4；进入 P2。

### D-P4-008 — Specialized scope mismatch is safely excluded

```text
baseline complete NO_MATCH
sepsis family produces governed SCOPE_MISMATCH consistently
→ family NOT_APPLICABLE
other family complete / NOT_APPLICABLE
```

Expected：该 specialized family 可排除 denominator；不制造 whole-policy FAILED。

所有 P4 fixture 均必须带 negative assertion：

```text
NO_HIGH_RISK_SIGNAL != SAFE
NO_HIGH_RISK_SIGNAL != NORMAL
NO_HIGH_RISK_SIGNAL != no disease
```

Review status：`PENDING`。

---

## 7. Asset Group F — P5 No Unique Legal Decision

Asset：`EVAL-U03-D-P5-CONFLICT-FIXTURES`

### D-P5-001 — Mixed incompatible family states

同一 conditional family 的 frozen C execution results 无法形成唯一合法 coverage state，例如同时出现：

```text
SCOPE_MISMATCH
+
MATCHED
```

且该组合违反 frozen shared-scope invariant。

Expected：

```text
FAILED / NONE / UNRESOLVABLE_CONFLICT
```

不得自行发明第二套 family aggregation。

### D-P5-002 — Deterministic resolution yields no unique branch

构造一个通过 P0 input integrity 检查但 policy branch resolution 无唯一合法结果的 harness fixture。

Expected：

```text
FAILED / NONE / UNRESOLVABLE_CONFLICT
```

禁止 LLM synthesis / random branch / file-order branch。

Review status：`PENDING`。

---

## 8. Asset Group G — Frozen Coverage Contract Semantics

Asset：`EVAL-U03-D-COVERAGE-CONTRACT-FIXTURES`

| Fixture | Input | Expected coverage behavior |
|---|---|---|
| D-COV-001 | overall D scope holds | 5 baseline rules 全部必须在 denominator |
| D-COV-002 | baseline C result = INPUT_INSUFFICIENT | `INSUFFICIENT_APPLICABLE`，不得移出 denominator |
| D-COV-003 | dyspnoea family governed SCOPE_MISMATCH | `NOT_APPLICABLE`，排除两条 family members |
| D-COV-004 | sepsis family governed SCOPE_MISMATCH | `NOT_APPLICABLE`，排除 8 条 family members |
| D-COV-005 | conditional family INPUT_INSUFFICIENT | `INSUFFICIENT_APPLICABLE`，P2 eligible，P3/P4 blocked |
| D-COV-006 | specialized NOT_APPLICABLE | 不得产生 `OVERALL_POLICY_SCOPE_MISMATCH` |
| D-COV-007 | overall policy scope mismatch | 直接 P0；不得进入 family denominator |
| D-COV-008 | conditional family mixed incompatible states | fail closed → P5 conflict path |

必须保持 coverage vocabulary 仅：

```text
APPLICABLE_EVALUATED
INSUFFICIENT_APPLICABLE
NOT_APPLICABLE
```

不得在 coverage layer 发明 `HIGH_RISK / CAUTION / SAFE`。

Review status：`PENDING`。

---

## 9. Asset Group H — Release / Version Mismatch

Asset：`EVAL-U03-D-RELEASE-VERSION-MISMATCH-FIXTURES`

| Fixture | Invalid input | Expected |
|---|---|---|
| D-VER-001 | `rule_release_ref != RR-U03-RISK-001@0.2.0-candidate` | P0 `FAILED / RELEASE_MISMATCH` |
| D-VER-002 | wrong knowledge release ref | P0 `FAILED / RELEASE_MISMATCH` |
| D-VER-003 | wrong/unresolvable `U03_D09_COVERAGE_V0_2` ref | P0 `FAILED / RELEASE_MISMATCH` |
| D-VER-004 | wrong policy draft/candidate ref where exact binding required | P0 `FAILED / RELEASE_MISMATCH` |
| D-VER-005 | stale clinical-state version | P0 `FAILED / STALE_INPUT` |
| D-VER-006 | mutable `latest` alias supplied for governed release | reject as invalid/unresolvable governed binding; no clinical disposition |

Negative assertion：任何 mismatch 都不得继续形成 `HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL`。

Review status：`PENDING`。

---

## 10. Cross-Cutting Precedence Matrix

必须在 review 中逐项确认：

| ID | Scenario | Expected |
|---|---|---|
| D-PX-01 | P0 + HIGH | P0 FAILED |
| D-PX-02 | HIGH + insufficiency | HIGH_RISK + retain insufficiency |
| D-PX-03 | HIGH + CAUTION | HIGH_RISK |
| D-PX-04 | CAUTION + insufficiency | P2 FAILED |
| D-PX-05 | CAUTION only, coverage sufficient | CAUTION |
| D-PX-06 | complete denominator all NO_MATCH | NO_HIGH_RISK_SIGNAL |
| D-PX-07 | specialized SCOPE_MISMATCH | NOT_APPLICABLE, not whole D failure |
| D-PX-08 | overall D scope mismatch | P0 FAILED / OVERALL_POLICY_SCOPE_MISMATCH |
| D-PX-09 | unresolved policy conflict | P5 FAILED |

因此必须验证：

```text
P0 > P1 > P2 > P3 > P4 > P5
```

---

## 11. Fixture Inventory

```text
P0 fixtures = 10
P1 fixtures = 6
P2 fixtures = 6
P3 fixtures = 2
P4 fixtures = 8
P5 fixtures = 2
Coverage fixtures = 8
Version/release fixtures = 6

Total fixtures = 48
```

这 48 条仅为 candidate-freeze minimum fixture pack，不是完整 Gate C Clinical EvalSet / Safety Suite。

---

## 12. Current Status

```text
8 required asset groups = CONTENT_AVAILABLE
Fixture Count = 48
Medical Review = NOT_STARTED
Technical/Eval Review = NOT_STARTED
blocking eval finding = UNKNOWN
D Pre-Freeze Eval PASS = NO
BLOCKER-FZ-D-02 = OPEN
BLOCKER-FZ-D-03 = OPEN / MUST_REMAIN_AFTER_D-02
BLOCKER-FZ-D-04 = OPEN / MUST_REMAIN_LAST
Gate C = NOT_PASSED
```

下一步：只审核本 fixture pack；在 Medical + Technical/Eval APPROVE 前不得创建 `PR-U03-D09-001@0.2.0-candidate`。