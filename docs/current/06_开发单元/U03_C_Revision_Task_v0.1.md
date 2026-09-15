# U03 C Rule Pack Revision Task v0.1

> 权威输入：`U03_C_Medical_Owner_Review_Record_v0.1.md`  
> 目标稿：`U03_Safety_Critical_Risk_Rule_Pack_Content_Draft_v0.2.md`  
> 状态：`REVISION_REQUIRED / FREEZE_BLOCKED / D_STILL_BLOCKED`

本任务单只处理本轮 blocking findings。不授权新来源、不开始 D09、不写入生产阈值发布。

---

## 1. 必须修订的 3 条 rule

### C-RULE-SEPSIS-SBP-HIGH-001 = REVISE

保留：

```text
predicate values
= SBP <= 90
OR SBP drop from usual > 40
```

必须补：

```text
usual_systolic_bp 的可信/可追溯条件
OR 分支的独立执行态
SBP 未测 / usual 未知时输出 RULE_SIGNAL_INPUT_INSUFFICIENT
不得把 drop 分支未知写成 HIGH 明确不命中
```

### C-RULE-SEPSIS-MENTAL-HIGH-001 = REVISE

禁止继续使用：

```text
objective_altered_mental_state
```

原因：该 operand 不在 Gate A 已批准 B 中。`EV-RF-NEURO-001` 的已批准定义是“新出现的意识、行为或认知异常”，宽于 NG253 HIGH-only 的 “Objective evidence of new altered mental state”。

允许的修法，只能选其一：

```text
A. 本 slice 不写 sepsis mental HIGH rule；
   并把 NG253 HIGH objective mental
   以及 MODHIGH history-of-altered-behaviour / functional decline
   写入显式排除清单。

B. 先回到 B，把 objective vs history/concern 拆成两条已审核 evidence，
   再让 C 分别引用。不得由 C 私自拆条。
```

不得把 `accepted_evidence(EV-RF-NEURO-001) == PRESENT` 直接标成 `RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION`。

### C-RULE-SEPSIS-APPEAR-HIGH-001 = REVISE

禁止继续使用：

```text
mottled_or_ashen_or_cyanotic_appearance
```

必须改回消费：

```text
accepted_evidence(EV-RF-APPEAR-001) == PRESENT
+
suspected_sepsis / age>=16 / community-custodial precondition
```

`mottled` 若要单独进入可执行 predicate，必须先修订 B 定义并重新走 Gate A 条目审核。C 不得补第二套外观 taxonomy。

---

## 2. 整包必须补齐的执行语义

所有 16 条 rule 在 v0.2 都要显式写出：

```text
matched → 已定义 matched_signal
insufficient → RULE_SIGNAL_INPUT_INSUFFICIENT
scope mismatch → RULE_SIGNAL_SCOPE_MISMATCH
```

至少覆盖：

```text
UNKNOWN / UNMEASURED / NOT_ASKED / AMBIGUOUS / CONFLICTING
REMOTE_NOT_OBSERVED
age < 16
pregnancy / recent-pregnancy
suspected_sepsis != TRUE
setting 不在 source-supported community/custodial
dyspnoea 规则缺少 NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT
```

并增加：

```text
suspected_sepsis
不得由正在判定的同一条生命体征反推
```

`NHS_DYSPNOEA_EMERGENCY_WARNING_CONTEXT` 必须定义成“NHS Shortness of breath 页的急诊警示上下文”，不得退化成任意气促主诉。

RR / SBP / HR 规则必须把第 6 节前言中的 scope 写成每条 rule 自己的 precondition，不能只靠章节散文继承。

---

## 3. 建议但不阻塞条目批准的对齐

- `C-RULE-SEPSIS-RASH-HIGH-001` 改回 `accepted_evidence(EV-RF-SEPSIS-001) == PRESENT`。
- 在排除清单中显式写入 NG253 moderate-high mental/history 分支，避免被理解成“已经用 EV-RF-NEURO-001 覆盖”。
- 为 `C-RULE-SEPSIS-SBP-MODHIGH-001` 写清：当 HIGH drop 分支为 insufficient 且当前 SBP 在 91..100 时，两条 rule 可以并存，不互相抑制。

---

## 4. 明确不要做的事

```text
不要开始 D09
不要 freeze RR-U03-RISK-001 candidate
不要把 NICE/NHS 写成中国生产规则
不要打开儿科或孕产
不要把 SpO2 / urine / temperature / arrhythmia
    以“来源里有”为由直接写入 C
不要新增 B 中不存在的 evidence_id
不要把 rule signal 提升为 HIGH_RISK / CAUTION / NO_HIGH_RISK_SIGNAL
```

---

## 5. 完成定义

v0.2 只有同时满足以下条件，才能再进入审核：

```text
3 条 REVISE rule 已按本任务单改完
16 条 rule 都有 insufficient / scope-mismatch 执行态
sepsis 数值规则每条自带 scope precondition
C 不再发明 B 未批准 operand
仍只绑定 KR-U03-SOURCE-001@0.1.0-candidate
仍不输出 D09 disposition
```
