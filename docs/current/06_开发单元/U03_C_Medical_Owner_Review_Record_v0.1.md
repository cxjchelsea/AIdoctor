# U03 C Rule Pack Medical / Technical Review Record v0.1

> 角色：对 C / Safety-critical Risk Rule Pack Content Draft v0.1 的本轮 Medical Owner Review 与 Technical Review 记录。  
> 审核对象：`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.1.md`  
> Rule Release：`RR-U03-RISK-001@0.1.0-draft`  
> Knowledge Release：`KR-U03-SOURCE-001@0.1.0-candidate`  
> 对照：Gate A 已批准 B Catalog v0.2、A Semantics v0.2、NICE NG253 Table 1（community/custodial, 16+, not pregnant）、NHS Shortness of breath 急诊警示列表。  
> 状态：`REVIEW_RECORD_COMPLETE / PACKAGE_NOT_APPROVED / FREEZE_NOT_AUTHORIZED / D_STILL_BLOCKED / NOT_FOR_PRODUCTION`  
> 审核角色：`U03_MEDICAL_OWNER_REVIEW` + `U03_TECHNICAL_GOVERNANCE_REVIEW`  
> 审核日期：`2026-09-15`

本记录只裁决当前 C v0.1 草案。`APPROVE` 表示本轮接受该 rule 的临床方向与来源忠实性，不等于 candidate freeze、D09 授权、runtime binding 或生产发布。

---

## 1. 审核规则

每条 rule 只允许：

```text
APPROVE
REVISE
REJECT
NEED_MORE_SOURCE
```

本轮额外保持：

```text
Rule Signal != D09 Disposition
C 不得新增第二套 Evidence taxonomy
UNKNOWN / UNMEASURED / NOT_ASKED != 明确不命中
NICE / NHS != 中国生产规则
```

判定重点：
1. `required_evidence_refs` 是否只引用 Gate A 已批准 B entry；
2. predicate / threshold 是否忠实于绑定 source，且不宽于 B 已批准定义；
3. population / setting / clinical-context 是否被扩大；
4. missing / unknown / ambiguous 是否会被当成 negative；
5. rule signal 是否停留在 rule-level；
6. 是否把专病阈值复制成全局生命体征；
7. 是否用新 operand 绕过 B Catalog。

---

## 2. 整包结论

```text
C Review = COMPLETE
C Medical APPROVE = 13
C Medical REVISE = 3
C Medical REJECT = 0
C Medical NEED_MORE_SOURCE = 0

Rule Signal Vocabulary
= DIRECTION_APPROVED
/ EXECUTION_STATES_NOT_OPERATIONALIZED

NG253 numeric thresholds
= SOURCE_FAITHFUL
/ SCOPE_LOCK_DIRECTION_APPROVED
/ EXECUTION_SEMANTICS_INCOMPLETE

C Package Approval = NOT_COMPLETE
Initial Rule Release Freeze = NOT_AUTHORIZED
CD-03 = NOT_PASSED
D drafting = BLOCKED
Gate B = NOT_PASSED
```

总裁决：C v0.1 可以作为继续修订的合法草案，不能冻结为 `RR-U03-RISK-001@<candidate>`。下一动作是按修订任务单产出 C Content Draft v0.2，而不是开始 D09。

---

## 3. 先确认成立的治理边界

以下方向本轮接受，不要求推倒重来：

```text
drafting 时机合法
KR-U03-SOURCE-001@0.1.0-candidate 绑定正确
16 条 rule 均引用 Gate A 已批准 B refs
未写入 HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL / FAILED
未把 NG253 阈值写入 E 或 B
未打开儿科 / 孕产 / 中国本地化
未开始 D09 或生产代码
SpO2 / urine / temperature / new-onset arrhythmia /
immunosuppression / recent surgery 排除理由成立
```

6 个 rule-level signal 的职责划分成立：

```text
RULE_SIGNAL_CRITICAL_RED_FLAG
RULE_SIGNAL_MUST_NOT_MISS
RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION
RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION
RULE_SIGNAL_INPUT_INSUFFICIENT
RULE_SIGNAL_SCOPE_MISMATCH
```

后两个 signal 必须在 v0.2 变成可执行输出，不能只停留在词表里。

---

## 4. 条目级裁决

| Rule ID | Evidence | Medical | Technical | 原因 |
|---|---|---|---|---|
| C-RULE-RESP-001 | EV-RF-RESP-001 | APPROVE | APPROVE | NHS 999 列表支持 gasping / choking / 无法说话；只消费已批准 B；未升格为诊断。 |
| C-RULE-NEURO-001 | EV-MNM-NEURO-001 | APPROVE | APPROVE | 突发局灶/单侧边界与历史阳性语义与 Gate A 一致。 |
| C-RULE-NEURO-002 | EV-MNM-NEURO-002 | APPROVE | APPROVE | NG127 / NHS FAST 言语异常方向正确；短暂缓解不得转阴。 |
| C-RULE-CARD-001 | EV-MNM-CARD-001 | APPROVE | APPROVE | 只消费现象层胸痛 evidence；未写成已确诊 ACS/MI。 |
| C-RULE-ALLERGY-001 | EV-RF-ALLERGY-001 | APPROVE | APPROVE | 锁快速 ABC；皮肤非必需；未扩到轻型过敏。 |
| C-RULE-DYSPNOEA-APPEAR-001 | EV-RF-APPEAR-001 | APPROVE | APPROVE | 外观未做成全局 red flag；锁在 NHS 严重呼吸困难急诊警示上下文。 |
| C-RULE-DYSPNOEA-CONFUSION-001 | EV-RF-NEURO-001 | APPROVE | APPROVE | 与 NG253 拆开正确；禁止拼接成任意急症全局 RED_FLAG。 |
| C-RULE-SEPSIS-RR-HIGH-001 | EV-VS-SEPSIS-001 | APPROVE | APPROVE | NG253 Table 1：RR `>= 25`；须继续锁 16+ / suspected sepsis / community-custodial。 |
| C-RULE-SEPSIS-RR-MODHIGH-001 | EV-VS-SEPSIS-001 | APPROVE | APPROVE | NG253 Table 1：RR `21..24`；与 HIGH 无重叠。 |
| C-RULE-SEPSIS-SBP-HIGH-001 | EV-VS-SEPSIS-002 | REVISE | REVISE | 阈值 `<=90 OR >40 below usual` 来源正确，但 unknown baseline 的 OR 分支尚未变成可执行 insufficient 状态。 |
| C-RULE-SEPSIS-SBP-MODHIGH-001 | EV-VS-SEPSIS-002 | APPROVE | APPROVE | NG253 Table 1：SBP `91..100`。与 HIGH drop 分支的并存关系须在 v0.2 写清，但不否决本条阈值。 |
| C-RULE-SEPSIS-HR-HIGH-001 | EV-VS-SEPSIS-003 | APPROVE | APPROVE | NG253 Table 1：HR `> 130`；`=130` 不得升 HIGH。 |
| C-RULE-SEPSIS-HR-MODHIGH-001 | EV-VS-SEPSIS-003 | APPROVE | APPROVE | NG253 Table 1：HR `91..130`；孕期 `100..130` 已正确排除。 |
| C-RULE-SEPSIS-MENTAL-HIGH-001 | EV-RF-NEURO-001 | REVISE | REVISE | 发明了 B 中不存在的 `objective_altered_mental_state`；且 B 已批准定义宽于 NG253 HIGH-only “objective” 标准。 |
| C-RULE-SEPSIS-APPEAR-HIGH-001 | EV-RF-APPEAR-001 | REVISE | REVISE | 发明了 `mottled_or_ashen_or_cyanotic_appearance`；mottled 未进入 Gate A 已批准 B 定义。 |
| C-RULE-SEPSIS-RASH-HIGH-001 | EV-RF-SEPSIS-001 | APPROVE | APPROVE | 与已批准不褪色瘀点/紫癜定义对齐；v0.2 建议改回 `accepted_evidence(...)` 以保持统一。 |

没有条目被 REJECT。没有条目需要为本 slice 新开来源。

---

## 5. NG253 数值阈值核对

对照 NICE NG253 community/custodial Table 1（16 岁及以上、非妊娠/非近期妊娠）：

| C predicate | NG253 Table 1 | 本轮 |
|---|---|---|
| RR `>= 25` | High-risk：25 breaths/min or more | 忠实 |
| RR `21..24` | Moderate- to high-risk：21 to 24 | 忠实 |
| SBP `<= 90` OR `>40 below usual` | High-risk：90 mmHg or less, or more than 40 mmHg below normal | 忠实 |
| SBP `91..100` | Moderate- to high-risk：91 to 100 mmHg | 忠实 |
| HR `> 130` | High-risk：more than 130 beats/min | 忠实 |
| HR `91..130` | Moderate- to high-risk：91 to 130；妊娠为 100 to 130 | 忠实，且未套用孕期带 |

这些值本身不是 blocker。blocker 是：阈值只在 `matched` 分支写清，没有把未测/未知/超范围写成 `RULE_SIGNAL_INPUT_INSUFFICIENT` 或 `RULE_SIGNAL_SCOPE_MISMATCH`。

---

## 6. Dyspnoea / Sepsis scope

### 成立

- 严重呼吸困难、外观、意识混乱没有被做成跨病种全局规则。
- 同一条 B evidence 按来源上下文拆成不同 C rule 和不同 signal，方向正确：

```text
EV-RF-APPEAR-001
→ C-RULE-DYSPNOEA-APPEAR-001 = CRITICAL_RED_FLAG
→ C-RULE-SEPSIS-APPEAR-HIGH-001 = SEPSIS_HIGH_RISK_CRITERION

EV-RF-NEURO-001
→ C-RULE-DYSPNOEA-CONFUSION-001 = CRITICAL_RED_FLAG
→ C-RULE-SEPSIS-MENTAL-HIGH-001 = SEPSIS_HIGH_RISK_CRITERION
```

- NHS Shortness of breath 的 999 列表支持：gasping/choking/无法说话、唇/皮肤明显苍白蓝灰、突然意识混乱。
- 儿童 NHS 段落没有被误用。
- NG253 数值规则正文锁在 `age >= 16 + suspected sepsis + source-supported community/custodial`，没有写成全局生命体征。

### 仍须加严

1. `NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT` 和 `suspected_sepsis` 目前只是 precondition 名字，没有受治理的 accepted-input 定义。
2. `suspected_sepsis` 不得由正在判定的同一条生命体征反推。否则 scope lock 会被循环绕过。
3. 数值规则只在第 6 节前言继承 scope，RR/SBP/HR 规则块自身没有重复 precondition；v0.2 必须写成每条 rule 自带 scope。
4. NG253 moderate-high 的 “history of new onset altered behaviour / mental state” 与 “acute deterioration of functional ability” 没有像 SpO2 一样写入显式排除清单。不能让人误以为 `EV-RF-NEURO-001` 已经覆盖并等于 HIGH。

---

## 7. 整包 Technical Review

结构方向成立，freeze 不成立。

当前 blocker：

```text
1. RULE_SIGNAL_INPUT_INSUFFICIENT / RULE_SIGNAL_SCOPE_MISMATCH
   已进词表，但 16 条 rule 都没有定义何时输出它们。

2. 第 3 节写了 UNKNOWN != 明确不命中，
   但没有 per-rule unmatched / insufficient / scope-mismatch 执行态。

3. suspected_sepsis、age、setting、
   NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT、usual_systolic_bp
   都是执行前置，却没有 required/optional evidence 或 acceptance policy。

4. C-RULE-SEPSIS-MENTAL-HIGH-001 /
   C-RULE-SEPSIS-APPEAR-HIGH-001
   使用了 B 未批准的新 operand，构成第二套 evidence taxonomy。

5. 数值规则的 scope 只写在章节前言，不具备可解析 rule 结构。

6. evaluation_refs / missingness_policy_ref / rule_version
   仍缺；对 draft 可容忍，对 candidate freeze 不可容忍。
```

SBP HIGH 的正确执行语义必须写成：

```text
SBP UNKNOWN / UNMEASURED
→ INPUT_INSUFFICIENT
   不得判 HIGH 不命中，也不得判 MODHIGH 不命中

SBP known <= 90
→ HIGH 可独立命中

SBP known > 90 AND usual_systolic_bp UNKNOWN
→ HIGH 的 drop 分支 = INPUT_INSUFFICIENT
→ 不得把 drop 分支当成明确不命中
→ 若 SBP 落在 91..100，MODHIGH 仍可独立命中

SBP known > 100 AND usual UNKNOWN
→ HIGH drop 分支仍是 INPUT_INSUFFICIENT
→ MODHIGH 不命中
```

---

## 8. 明确不在本轮打开的内容

继续排除，且不构成本轮否决：

```text
SpO2 / new oxygen requirement
urine output
temperature < 36°C
new-onset arrhythmia
immunosuppression
recent surgery / invasive procedure
pediatric sepsis
pregnancy / recent-pregnancy
China-localized thresholds
NEWS2 / acute-hospital NG253 pathway
```

这些内容若要进入 C，必须先回到 A/B 增补并重新审核，不能由 C 直接发明。

---

## 9. 后续门禁

```text
C v0.1 Review = COMPLETE
C v0.2 Revision = REQUIRED
RR-U03-RISK-001@0.1.0-draft = NOT_FROZEN
RR-U03-RISK-001 candidate freeze = BLOCKED

D D09 Clinical Policy Content = BLOCKED
Gate B = NOT_PASSED
CD-07 = BLOCKED
U04 = BLOCKED
Production = BLOCKED
```

只有 v0.2 消除全部 blocking REVISE，且 Medical / Technical 再审均为 APPROVE 后，才能重新评估 candidate freeze。在此之前不得开始真实 D09 branch。
