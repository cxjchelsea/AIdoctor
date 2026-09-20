# U03 C Pre-Freeze Evaluation Fixtures v0.2

> 对象：`RR-U03-RISK-001@0.2.0-draft` candidate freeze 前的 minimum evaluation fixture content。  
> 权威输入：`U03_C_PreFreeze_Evaluation_Minimum_v0.2.md`、`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md`、`PF-U03-C-POLICY-001`。  
> 状态：`FIXTURE_CONTENT_AVAILABLE / REVIEW_COMPLETE_APPROVE / PRE_FREEZE_EVAL_PASS / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 本文件只把已批准 C rule/policy 语义转成可审核 fixture，不新增医学来源、阈值、D09 disposition 或生产结论。

---

## 1. Common Binding

所有 fixture 统一绑定：

```text
bound_rule_release_ref = RR-U03-RISK-001@0.2.0-draft
bound_knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
bound_policy_pair_ref = PF-U03-C-POLICY-001
bound_policy_refs = [U03_C_MISSINGNESS_V0_2, U03_SEPSIS_SHARED_SCOPE_V0_2] where applicable
clinical_state_version = TEST_STATE_V0_2_<fixture_id>
review_status = PRE_FREEZE_REVIEW_APPROVED
```

统一负面断言：

```text
Rule Signal != D09 Disposition
NO_MATCH only when scope satisfied + required inputs resolvable + predicate deterministically false
missing/unknown must not become ABSENT/NORMAL/NEGATIVE
current pack must not create its own required scope context
```

---

## 2. EVAL-U03-C-POSITIVE-FIXTURES

目标：15 条 active rule 每条至少 1 个合法 MATCHED；数值阈值覆盖规定边界。

| fixture_id | rule | fixture input | expected C result | expected signal | negative assertion | rationale refs |
|---|---|---|---|---|---|---|
| C-POS-001 | C-RULE-RESP-001 | adult Gate-A scope; `EV-RF-RESP-001=PRESENT` | MATCHED | CRITICAL_RED_FLAG | 不得输出 D09 disposition | B v0.2 / C v0.2 §4 |
| C-POS-002 | C-RULE-NEURO-001 | adult sudden focal/unilateral context; `EV-MNM-NEURO-001=PRESENT` | MATCHED | MUST_NOT_MISS | 不得扩到慢性/双侧非局灶 | B v0.2 / C v0.2 §4 |
| C-POS-003 | C-RULE-NEURO-002 | adult sudden speech/language context; `EV-MNM-NEURO-002=PRESENT` | MATCHED | MUST_NOT_MISS | 短暂缓解不得转 negative | B v0.2 / C v0.2 §4 |
| C-POS-004 | C-RULE-CARD-001 | adult approved chest-pain-like scope; `EV-MNM-CARD-001=PRESENT` | MATCHED | MUST_NOT_MISS | 不得解释为已确诊 ACS/MI | B v0.2 / C v0.2 §4 |
| C-POS-005 | C-RULE-ALLERGY-001 | rapid systemic allergic ABC scope; `EV-RF-ALLERGY-001=PRESENT` | MATCHED | CRITICAL_RED_FLAG | 不得扩为一般轻型过敏 | B v0.2 / C v0.2 §4 |
| C-POS-006 | C-RULE-DYSPNOEA-APPEAR-001 | independently-established NHS dyspnoea emergency context; `EV-RF-APPEAR-001=PRESENT` | MATCHED | CRITICAL_RED_FLAG | appearance 不得自行创建 context | BF-C-04 / C v0.2 §5 |
| C-POS-007 | C-RULE-DYSPNOEA-CONFUSION-001 | independently-established NHS dyspnoea emergency context; `EV-RF-NEURO-001=PRESENT` | MATCHED | CRITICAL_RED_FLAG | confusion 不得自行创建 context | BF-C-04 / C v0.2 §5 |
| C-POS-008 | C-RULE-SEPSIS-RR-HIGH-001 | shared scope valid; RR=25 | MATCHED | SEPSIS_HIGH_RISK_CRITERION | RR 不得创建 suspected_sepsis | C v0.2 §6 / Shared Scope |
| C-POS-009 | C-RULE-SEPSIS-RR-MODHIGH-001 | shared scope valid; RR=21 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 不得升级 D09 | C v0.2 §6 |
| C-POS-010 | C-RULE-SEPSIS-RR-MODHIGH-001 | shared scope valid; RR=24 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 24 不得升 HIGH | C v0.2 §6 |
| C-POS-011 | C-RULE-SEPSIS-SBP-HIGH-001 | shared scope valid; SBP=90; usual=UNKNOWN | MATCHED | SEPSIS_HIGH_RISK_CRITERION | absolute branch 不依赖 usual | C v0.2 §6 / Missingness |
| C-POS-012 | C-RULE-SEPSIS-SBP-MODHIGH-001 | shared scope valid; SBP=91 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 不得被 HIGH-drop unknown 抑制 | C v0.2 §6 |
| C-POS-013 | C-RULE-SEPSIS-SBP-MODHIGH-001 | shared scope valid; SBP=100 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 100 不得升 absolute HIGH | C v0.2 §6 |
| C-POS-014 | C-RULE-SEPSIS-SBP-HIGH-001 | shared scope valid; current SBP=110; usual SBP=151 with provenance | MATCHED | SEPSIS_HIGH_RISK_CRITERION | drop>40 可命中 relative branch | C v0.2 §6 |
| C-POS-015 | C-RULE-SEPSIS-HR-MODHIGH-001 | shared scope valid; HR=91 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 不得套用孕期分层 | C v0.2 §6 |
| C-POS-016 | C-RULE-SEPSIS-HR-MODHIGH-001 | shared scope valid; HR=130 | MATCHED | SEPSIS_MODERATE_HIGH_RISK_CRITERION | 130 不得升 HIGH | C v0.2 §6 |
| C-POS-017 | C-RULE-SEPSIS-HR-HIGH-001 | shared scope valid; HR=131 | MATCHED | SEPSIS_HIGH_RISK_CRITERION | >130 才 HIGH | C v0.2 §6 |
| C-POS-018 | C-RULE-SEPSIS-APPEAR-HIGH-001 | shared scope valid; `EV-RF-APPEAR-001=PRESENT` | MATCHED | SEPSIS_HIGH_RISK_CRITERION | 不得发明 mottled operand | C v0.2 §7 |
| C-POS-019 | C-RULE-SEPSIS-RASH-HIGH-001 | shared scope valid; `EV-RF-SEPSIS-001=PRESENT` | MATCHED | SEPSIS_HIGH_RISK_CRITERION | 只消费已批准 B evidence | C v0.2 §7 |
| C-POS-020 | C-RULE-SEPSIS-RR-HIGH-001 | shared scope valid; RR=26 | MATCHED | SEPSIS_HIGH_RISK_CRITERION | rule result 不得反建 scope | BF-C-04 / C v0.2 §6 |

Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 3. EVAL-U03-C-NEGATIVE-FIXTURES

目标：只有 scope 满足、输入完整且 predicate 明确 false 才允许 `NO_MATCH`。

| fixture_id | rule | fixture input | expected | negative assertion | rationale refs |
|---|---|---|---|---|---|
| C-NEG-001 | C-RULE-RESP-001 | valid adult scope; `EV-RF-RESP-001=ABSENT` | NO_MATCH | 不得输出 INPUT_INSUFFICIENT | Missingness / C §4 |
| C-NEG-002 | C-RULE-NEURO-001 | valid scope/required fields; `EV-MNM-NEURO-001=ABSENT` | NO_MATCH | 不得把 unknown 当 absent | Missingness / C §4 |
| C-NEG-003 | C-RULE-DYSPNOEA-APPEAR-001 | context independently TRUE; `EV-RF-APPEAR-001=ABSENT` | NO_MATCH | context TRUE 与 evidence ABSENT 分离 | BF-C-04 / C §5 |
| C-NEG-004 | C-RULE-SEPSIS-RR-HIGH-001 | shared scope valid; RR=24 | NO_MATCH | 不影响 MODHIGH 独立 rule | C §6 |
| C-NEG-005 | C-RULE-SEPSIS-RR-MODHIGH-001 | shared scope valid; RR=20 | NO_MATCH | 不是缺失数据 | C §6 |
| C-NEG-006 | C-RULE-SEPSIS-SBP-MODHIGH-001 | shared scope valid; SBP=101 | NO_MATCH | HIGH-drop 另行计算 | C §6 |
| C-NEG-007 | C-RULE-SEPSIS-HR-HIGH-001 | shared scope valid; HR=130 | NO_MATCH | 130 属于 MODHIGH | C §6 |
| C-NEG-008 | C-RULE-SEPSIS-HR-MODHIGH-001 | shared scope valid; HR=90 | NO_MATCH | 不得升级/降级 D09 | C §6 |

Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 4. EVAL-U03-C-MISSING-UNKNOWN-FIXTURES

目标：覆盖 frozen Missingness Policy 的不可判定 vocabulary。

| fixture_id | target | input state | expected | expected signal | negative assertion |
|---|---|---|---|---|---|
| C-MISS-001 | required evidence | UNKNOWN | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != NO_MATCH |
| C-MISS-002 | required measurement RR | UNMEASURED | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != normal RR |
| C-MISS-003 | required measurement HR | NOT_ASKED | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != NO_MATCH |
| C-MISS-004 | required measurement SBP | AMBIGUOUS | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != NO_MATCH |
| C-MISS-005 | required measurement RR | CONFLICTING | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != NO_MATCH |
| C-MISS-006 | required remote evidence | REMOTE_NOT_OBSERVED | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != excluded |
| C-MISS-007 | required measurement HR | INVALID | INPUT_INSUFFICIENT | RULE_SIGNAL_INPUT_INSUFFICIENT | != NO_MATCH |

Rationale refs：`PF-U03-C-POLICY-001 / U03_C_MISSINGNESS_V0_2`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 5. EVAL-U03-C-SCOPE-MISMATCH-FIXTURES

| fixture_id | target | scope input | expected | expected signal | negative assertion |
|---|---|---|---|---|---|
| C-SCOPE-001 | sepsis rule | age=15; remaining scope known-valid | SCOPE_MISMATCH | RULE_SIGNAL_SCOPE_MISMATCH | 不得执行阈值 |
| C-SCOPE-002 | sepsis rule | pregnancy/recent-pregnancy=TRUE | SCOPE_MISMATCH | RULE_SIGNAL_SCOPE_MISMATCH | 不得套用当前成人非孕 scope |
| C-SCOPE-003 | sepsis rule | suspected_sepsis=FALSE with valid independent context ref | SCOPE_MISMATCH | RULE_SIGNAL_SCOPE_MISMATCH | != INPUT_INSUFFICIENT |
| C-SCOPE-004 | sepsis rule | setting=OUTSIDE_SOURCE_SUPPORTED_COMMUNITY_CUSTODIAL | SCOPE_MISMATCH | RULE_SIGNAL_SCOPE_MISMATCH | 不得转全局 vital rule |
| C-SCOPE-005 | dyspnoea source-locked rule | NHS dyspnoea emergency context=FALSE | SCOPE_MISMATCH | RULE_SIGNAL_SCOPE_MISMATCH | appearance/confusion 不得全局化 |

Rationale refs：`U03_SEPSIS_SHARED_SCOPE_V0_2 / BF-C-04 / C v0.2`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 6. EVAL-U03-C-CONTEXT-INDEPENDENCE-FIXTURES

目标：验证 BF-C-04 family-loop prohibition。

| fixture_id | attempted derivation | fixture setup | expected C handling | negative assertion |
|---|---|---|---|---|
| C-CTX-001 | RR → suspected_sepsis | no pre-existing suspected-sepsis context; RR=25 | INPUT_INSUFFICIENT / do not execute sepsis rule as in-scope | RR 不得创建 scope |
| C-CTX-002 | SBP+HR → suspected_sepsis | no pre-existing context; SBP=90, HR=131 | INPUT_INSUFFICIENT | 多个 criteria 组合也不得创建 scope |
| C-CTX-003 | appearance+rash → suspected_sepsis | only `EV-RF-APPEAR-001=PRESENT` + `EV-RF-SEPSIS-001=PRESENT`; no independent context provenance | INPUT_INSUFFICIENT | accepted evidence 组合不得创建 scope |
| C-CTX-004 | appearance/confusion → NHS dyspnoea context | `EV-RF-APPEAR-001=PRESENT` + `EV-RF-NEURO-001=PRESENT`; no pre-existing NHS context | INPUT_INSUFFICIENT | 两 evidence 组合不得反建 dyspnoea context |
| C-CTX-005 | prior current-pack result → current scope | current execution only has a C rule result as proposed provenance | INPUT_INSUFFICIENT | current rule result 不得成为当前 execution scope provenance |
| C-CTX-006 | valid independent suspected-sepsis context | pre-existing context_ref + provenance_ref independent of current pack; remaining shared scope valid | proceed to predicate evaluation | 不得错误拒绝合法独立 context |

Rationale refs：`U03_C_BF_C_04_Closure_Amendment_v0.2.md / PF-U03-C-POLICY-001`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 7. EVAL-U03-C-SBP-BRANCH-FIXTURES

| fixture_id | input | HIGH expected | MODHIGH expected | negative assertion |
|---|---|---|---|---|
| C-SBP-001 | shared scope valid; SBP=90; usual=UNKNOWN | MATCHED / HIGH criterion | NO_MATCH | absolute HIGH 不依赖 usual |
| C-SBP-002 | shared scope valid; SBP=95; usual=UNKNOWN | relative-drop INPUT_INSUFFICIENT | MATCHED / MODHIGH criterion | 两结果允许并存 |
| C-SBP-003 | shared scope valid; SBP=110; usual=UNKNOWN | relative-drop INPUT_INSUFFICIENT | NO_MATCH | 不得把 HIGH 判明确 NO_MATCH |
| C-SBP-004 | shared scope valid; SBP=110; usual=151 with provenance | MATCHED / HIGH criterion | NO_MATCH | drop=41 命中 relative branch |

Rationale refs：`C v0.2 §6 / U03_C_MISSINGNESS_V0_2`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 8. EVAL-U03-C-MULTI-RULE-COEXISTENCE-FIXTURES

| fixture_id | fixture setup | expected result set | negative assertion |
|---|---|---|---|
| C-MULTI-001 | shared scope valid; SBP=95; usual unknown | SBP MODHIGH=MATCHED + SBP HIGH-drop=INPUT_INSUFFICIENT | no first-hit-wins |
| C-MULTI-002 | independent suspected-sepsis context; RR=25; HR=131 | RR HIGH=MATCHED + HR HIGH=MATCHED | C 不做 disposition precedence |
| C-MULTI-003 | independently-established NHS dyspnoea context; appearance=PRESENT; confusion=PRESENT | both dyspnoea rules MATCHED | 不得由命中结果反向创建 context |

Rationale refs：`C v0.2 §10 / BF-C-04 / frozen policies`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 9. EVAL-U03-C-VERSION-RELEASE-MISMATCH-FIXTURES

这些 fixture 不规定下游 D09 disposition；只要求 C evaluation 不得把 binding/version 错配静默当作合法 result。

| fixture_id | mismatch | expected | negative assertion |
|---|---|---|---|
| C-VER-001 | `knowledge_release_ref != KR-U03-SOURCE-001@0.1.0-candidate` | REJECT/FAIL_C_EVALUATION_INPUT; no valid rule result | 不得静默 fallback latest |
| C-VER-002 | missingness policy ref not `PF-U03-C-POLICY-001` frozen pair | REJECT/FAIL_C_EVALUATION_INPUT | 不得混用 draft/frozen policy |
| C-VER-003 | unresolvable shared-scope policy ref | REJECT/FAIL_C_EVALUATION_INPUT | 不得继续执行阈值 |
| C-VER-004 | clinical_state_version stale vs fixture-bound current version | REJECT/FAIL_C_EVALUATION_INPUT | stale state 不得生成 current C result |

说明：`REJECT/FAIL_C_EVALUATION_INPUT` 是 fixture harness 的评估期待，不是新增 C rule signal，也不是 D09 `FAILED` disposition。

Rationale refs：`U03_C_PreFreeze_Evaluation_Minimum_v0.2.md / C release binding invariants`。  
Group status：`CONTENT_AVAILABLE / REVIEW_APPROVED`。

---

## 10. Coverage Summary

```text
Required asset groups = 8 / 8 CONTENT_AVAILABLE
Positive fixtures = 20
Negative fixtures = 8
Missing/unknown fixtures = 7
Scope-mismatch fixtures = 5
Context-independence fixtures = 6
SBP-branch fixtures = 4
Multi-rule coexistence fixtures = 3
Version/release mismatch fixtures = 4
Total fixtures = 57
```

Coverage assertions：

```text
15 active rules each have >=1 positive MATCHED fixture = YES
RR boundaries 21/24/25 = COVERED
SBP boundaries 90/91/100 + relative-drop >40 = COVERED
HR boundaries 91/130/131 = COVERED
all frozen missingness states = COVERED
BF-C-04 independence = COVERED
policy-pair binding = COVERED
Gate C full independent evaluation = NOT_CLAIMED
```

---

## 11. Current Status

```text
Pre-Freeze Fixture Content = AVAILABLE
All 8 Asset Groups = REVIEW_APPROVED
Medical Review = COMPLETE_APPROVE
Technical/Eval Review = COMPLETE_APPROVE
Blocking Eval Finding = 0
Pre-Freeze Eval PASS = YES
BLOCKER-FZ-C-03 = CLOSED
BLOCKER-FZ-C-04 = OPEN / MUST_REMAIN_LAST
RR-U03-RISK-001@0.2.0-draft = NOT_FROZEN
Gate C = NOT_PASSED
D = STILL_BLOCKED
```

下一步：独立处理 `BLOCKER-FZ-C-04`，创建 Rule Release candidate version / freeze record。不得把 `0.2.0-draft` 改名成 candidate，不得开始 D09，不得宣称 Gate C PASS。
