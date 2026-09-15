# U03 C Rule Pack Revision Task v0.2

> 权威输入：`U03_C_Medical_Owner_ReReview_Record_v0.2.md`  
> 目标：在 C v0.2 上关闭 BF-C-04，不必重写 15 条已批准 rule。  
> 状态：`REVISION_TASK_COMPLETE / BF-C-04_CLOSED / FREEZE_STILL_BLOCKED / D_STILL_BLOCKED`

v0.1 的 3 个 blocker 已关闭。本任务只处理再审新发现的 scope-context 独立性。

---

## 1. BF-C-04 必须补上的约束

在通用执行模型中明确写入：

```text
scope_context.suspected_sepsis
= 执行前已存在的受治理上下文
≠ 由本 pack 任何 sepsis criterion 建立

禁止用以下任何一项建立或反推 suspected_sepsis：
- respiratory_rate_bpm
- systolic_bp_mmHg / usual_systolic_bp_mmHg
- heart_rate_bpm
- accepted_evidence(EV-RF-APPEAR-001)
- accepted_evidence(EV-RF-SEPSIS-001)
```

```text
scope_context.nhs_dyspnoea_emergency_warning_context
= NHS Shortness of breath 页的急诊警示上下文
≠ 任意气促主诉
≠ 仅因外观或意识混乱阳性而自动成立

禁止仅用以下证据单独建立该上下文：
- EV-RF-APPEAR-001
- EV-RF-NEURO-001
```

未知上下文继续输出 `RULE_SIGNAL_INPUT_INSUFFICIENT`；明确不在上下文中继续输出 `RULE_SIGNAL_SCOPE_MISMATCH`。

---

## 2. 明确不要做的事

```text
不要重写已 APPROVE 的 15 条 rule predicate / threshold
不要把 C-RULE-SEPSIS-MENTAL-HIGH-001 加回 active set
不要发明 mottled / objective mental operand
不要开始 D09
不要 freeze RR-U03-RISK-001 candidate
不要新增 B evidence 或新来源
```

---

## 3. 完成定义

```text
BF-C-04 = CLOSED
15 条 active rule 仍保持 v0.2 predicate
仍只绑定 KR-U03-SOURCE-001@0.1.0-candidate
仍不输出 D09 disposition
```

关闭后才能重新评估 package approval；package approval 之后再单独评估 candidate freeze（仍需 missingness/scope 对象可解析与 `evaluation_refs`）。
