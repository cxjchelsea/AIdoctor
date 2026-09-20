# U03 C Rule Pack Medical / Technical Re-review Record v0.2

> 审核对象：`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md`  
> Rule Release：`RR-U03-RISK-001@0.2.0-draft`  
> Knowledge Release：`KR-U03-SOURCE-001@0.1.0-candidate`  
> 对照：`U03_C_Medical_Owner_Review_Record_v0.1.md`、`U03_C_Revision_Task_v0.1.md`、Gate A 已批准 B Catalog v0.2、NICE NG253 Table 1。  
> 状态：`RE_REVIEW_COMPLETE / V0.1_BLOCKERS_CLOSED / PACKAGE_SCOPE_CONTEXT_REVISE / FREEZE_NOT_AUTHORIZED / D_STILL_BLOCKED / NOT_FOR_PRODUCTION`  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-15`

本记录只裁决 C v0.2。条目级 `APPROVE` 不自动授权 candidate freeze、D09 或生产 binding。

---

## 1. v0.2 Re-review Scope

已确认：

```text
active executable rule candidates = 15
v0.1 C-RULE-SEPSIS-MENTAL-HIGH-001 = WITHHELD_FROM_ACTIVE_SET
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
new source introduced = NO
new B evidence introduced = NO
D09 disposition introduced = NO
invented v0.1 operands retained = NO
```

---

## 2. Blocking Findings from v0.1

### BF-C-01 — SBP HIGH unknown usual baseline

```text
SBP unknown/unmeasured → INPUT_INSUFFICIENT
SBP <= 90 → HIGH independently MATCHED
SBP > 90 + usual known/traceable → evaluate drop > 40
SBP > 90 + usual unknown/untraceable → HIGH drop branch INPUT_INSUFFICIENT
91..100 MODHIGH may coexist with HIGH-drop insufficient
```

Verdict：`CLOSED`

### BF-C-02 — invented objective altered mental state operand

```text
C-RULE-SEPSIS-MENTAL-HIGH-001 removed from active set
objective_altered_mental_state not used
NG253 objective HIGH mental + MODHIGH history/functional branches explicitly excluded
future inclusion requires B revision + review
```

Verdict：`CLOSED`（采用修订任务单方案 A）

### BF-C-03 — invented appearance taxonomy

```text
mottled_or_ashen_or_cyanotic_appearance removed
sepsis appearance rule consumes only EV-RF-APPEAR-001
mottled cannot enter without B revision
```

Verdict：`CLOSED`

---

## 3. 本轮新发现

### BF-C-04 — scope context 仍可能被本 pack 准则循环建立

v0.2 已禁止“用本条正在判定的同一生命体征反推 `suspected_sepsis`”，这只关闭了单规则循环，没有关闭家族循环。

仍须写成：

```text
scope_context.suspected_sepsis
不得由本 pack 正在评估的任何
RR / SBP / HR / appearance / rash
criterion 建立或反推

scope_context.nhs_dyspnoea_emergency_warning_context
不得仅由 EV-RF-APPEAR-001 或 EV-RF-NEURO-001
单独建立
否则 appearance / confusion 会再次被升成全局 red flag
```

理由：NG253 Table 1 只能在“已经存在的 suspected sepsis”之后执行；NHS dyspnoea 的外观/意识混乱只能在该页急诊警示上下文中执行。若上游用同一组准则反推上下文，v0.2 的 scope lock 会被绕过。

Verdict：`OPEN / PACKAGE_REVISE`

这不否决 15 条 active rule 的条目方向，但阻塞 package approval 与 candidate freeze。

---

## 4. Package-level Execution Semantics

| Check | Medical | Technical |
|---|---|---|
| all active rules define matched / insufficient / scope mismatch | APPROVE | APPROVE |
| UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS / CONFLICTING are not NO_MATCH | APPROVE | APPROVE |
| suspected_sepsis is pre-existing governed context | APPROVE | APPROVE |
| suspected_sepsis is not circularly inferred from any pack sepsis criterion | REVISE | REVISE |
| NHS dyspnoea emergency warning context is source-specific and not arbitrary dyspnoea | APPROVE | APPROVE |
| NHS dyspnoea context is not inferred solely from appearance/confusion | REVISE | REVISE |
| each sepsis numeric/non-numeric rule carries shared scope precondition | APPROVE | APPROVE |
| Rule Signal remains below D09 disposition | APPROVE | APPROVE |
| no new source/evidence taxonomy is introduced | APPROVE | APPROVE |

`NO_MATCH` 已正确保持为非 signal、非低风险 disposition。v0.2 正文写“三态”后又允许 `NO_MATCH`，freeze 时应正式冻结为四类执行态：`MATCHED / INPUT_INSUFFICIENT / SCOPE_MISMATCH / NO_MATCH`。这不是本轮条目否决。

---

## 5. Active Rule Re-review Table

| Rule ID | Medical | Technical | Notes |
|---|---|---|---|
| C-RULE-RESP-001 | APPROVE | APPROVE | 仍只消费 EV-RF-RESP-001；未升格为诊断。 |
| C-RULE-NEURO-001 | APPROVE | APPROVE | 局灶/单侧边界与 required-field insufficient 成立。 |
| C-RULE-NEURO-002 | APPROVE | APPROVE | 短暂缓解仍保留历史阳性。 |
| C-RULE-CARD-001 | APPROVE | APPROVE | 现象层胸痛；未写成已确诊 ACS/MI。 |
| C-RULE-ALLERGY-001 | APPROVE | APPROVE | 锁快速 ABC；皮肤非必需。 |
| C-RULE-DYSPNOEA-APPEAR-001 | APPROVE | APPROVE | 条目方向正确；上下文独立性见 BF-C-04。 |
| C-RULE-DYSPNOEA-CONFUSION-001 | APPROVE | APPROVE | 未与 NG253 拼接成全局 RED_FLAG。 |
| C-RULE-SEPSIS-RR-HIGH-001 | APPROVE | APPROVE | `>= 25`；自带 shared scope。 |
| C-RULE-SEPSIS-RR-MODHIGH-001 | APPROVE | APPROVE | `21..24`；与 HIGH 无重叠。 |
| C-RULE-SEPSIS-SBP-HIGH-001 | APPROVE | APPROVE | v0.1 BF-C-01 已关闭。 |
| C-RULE-SEPSIS-SBP-MODHIGH-001 | APPROVE | APPROVE | 与 HIGH-drop insufficient 可并存。 |
| C-RULE-SEPSIS-HR-HIGH-001 | APPROVE | APPROVE | `> 130`；`=130` 不升 HIGH。 |
| C-RULE-SEPSIS-HR-MODHIGH-001 | APPROVE | APPROVE | `91..130`；未套用孕期带。 |
| C-RULE-SEPSIS-APPEAR-HIGH-001 | APPROVE | APPROVE | 只消费 EV-RF-APPEAR-001。 |
| C-RULE-SEPSIS-RASH-HIGH-001 | APPROVE | APPROVE | 已改回 `accepted_evidence(EV-RF-SEPSIS-001)`。 |

```text
Active-rule Medical APPROVE = 15
Active-rule Medical REVISE = 0
Active-rule REJECT = 0
Active-rule NEED_MORE_SOURCE = 0
Package-level REVISE = 1 (BF-C-04)
```

Withheld item：

```text
C-RULE-SEPSIS-MENTAL-HIGH-001
= NOT_IN_ACTIVE_SET
= BF-C-02 CLOSED
= requires B split/review before future inclusion
```

D09 将来不得把“本 slice 没有 sepsis mental rule”解释成“精神状态已排除”或 `NO_HIGH_RISK_SIGNAL`。

---

## 6. 整包结论

```text
v0.1 blocking findings = CLOSED
C v0.2 rule-level review = COMPLETE
C v0.2 package approval = NOT_COMPLETE
C v0.2 remaining blocker = BF-C-04
RR-U03-RISK-001@0.2.0-draft = NOT_FROZEN
CD-03 = NOT_PASSED
D drafting = BLOCKED
Gate B = NOT_PASSED
```

v0.1 修订任务已按条目完成，不需要推倒 15 条 rule。下一动作是补一段 scope-context 独立性约束，而不是重写阈值或重开 B。

---

## 7. Freeze Gate

当前仍不满足：

```text
Medical REVISE = 0                    PASS at rule level
Technical REVISE = 0                  PASS at rule level
BF-C-01 = CLOSED                      PASS
BF-C-02 = CLOSED                      PASS
BF-C-03 = CLOSED                      PASS
BF-C-04 = OPEN                        FAIL
package execution semantics = REVISE  FAIL
scope_context contracts = NOT_FROZEN
missingness_policy_ref = NAMED_NOT_EXTRACTED
evaluation_refs readiness = NOT_SATISFIED
```

因此：

```text
RR-U03-RISK-001 candidate freeze = NOT_AUTHORIZED
CD-03 = NOT_PASSED
D = BLOCKED
Gate B = NOT_PASSED
```

关闭 BF-C-04 后，仍须单独确认 `U03_C_MISSINGNESS_V0_2` / `U03_SEPSIS_SHARED_SCOPE_V0_2` 可解析，以及 candidate freeze 所需 `evaluation_refs`。这些是 freeze 卫生条件，不是本轮条目否决。
