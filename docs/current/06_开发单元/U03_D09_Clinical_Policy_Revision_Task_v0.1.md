# U03 D09 Clinical Policy Revision Task v0.1

> 权威输入：`U03_D09_Clinical_Policy_Review_Record_v0.1.md`  
> 目标对象：`U03_D09_Clinical_Policy_Content_Draft_v0.1.md`  
> 目标版本：`PR-U03-D09-001@0.2.0-draft`  
> 状态：`REVISION_APPLIED / RE_REVIEW_COMPLETE / BF-D-01_CLOSED / BF-D-02_CLOSED / NOT_FROZEN`

本任务只关闭 BF-D-01 / BF-D-02，使 6 个 branch 与三组边界可执行。不改 C 阈值，不发明新 evidence，不冻结 D，不开始 runtime。

---

## 1. 必须修订

### BF-D-01 整包 D scope 并入 P0

把第 8 节“整体 D policy scope 不成立 → FAILED / SCOPE_MISMATCH”写入 `D09-P-001`：

```text
preconditions += consultation outside this policy release
  population_scope / region_scope / channel_scope

reason_code += OVERALL_POLICY_SCOPE_MISMATCH
```

必须显式区分：

```text
OVERALL_POLICY_SCOPE_MISMATCH
= D-level P0 failure

RULE_SIGNAL_SCOPE_MISMATCH
= C-level specialized-family non-applicability
= NOT_APPLICABLE
!= whole D09 FAILED
```

不要新增第七个医学 disposition branch。

### BF-D-02 固化 coverage contract

把第 4 节升级为 P-020 / P-040 共同引用的冻结表，删除“本应可判定”：

```text
ALWAYS_APPLICABLE  when overall D scope holds:
  C-RULE-RESP-001
  C-RULE-NEURO-001
  C-RULE-NEURO-002
  C-RULE-CARD-001
  C-RULE-ALLERGY-001

CONDITIONALLY_APPLICABLE:
  NHS_DYSPNOEA_FAMILY
    C-RULE-DYSPNOEA-APPEAR-001
    C-RULE-DYSPNOEA-CONFUSION-001

  NG253_SEPSIS_FAMILY
    8 active sepsis rules bound to U03_SEPSIS_SHARED_SCOPE_V0_2
```

判定只允许消费 C 已冻结信号：

```text
C RULE_SIGNAL_SCOPE_MISMATCH
→ NOT_APPLICABLE
→ exclude from P-020 and P-040 denominators

C RULE_SIGNAL_INPUT_INSUFFICIENT
→ INSUFFICIENT_APPLICABLE
→ counts for P-020
→ blocks P-030 and P-040
```

P-040 precondition 必须改为引用该表，而不是“actually applicable governed rules/families required by this policy execution”。

---

## 2. 建议一并修订，但不构成第三条 blocker

```text
P-030 与 P-010 一样保留全部 matched_rule_refs
HIGH / CAUTION / NO_HIGH_RISK_SIGNAL / FAILED 词表不变
P0 > P1 > P2 > P3 > P4 > P5 不变
```

---

## 3. 明确不要做的事

```text
不要改 RR-U03-RISK-001@0.2.0-candidate
不要改 KR-U03-SOURCE-001@0.1.0-candidate
不要重算 C 阈值或新增 evidence taxonomy
不要把 NO_HIGH_RISK_SIGNAL 解释成 SAFE
不要把 D09 写成 U04 Safety Gate
不要打开儿科 / 孕产 / 中国生产本地化
不要冻结 D policy
不要开始 D runtime / CD-07 / U04
```

---

## 4. 完成定义

```text
D09-P-001 含 OVERALL_POLICY_SCOPE_MISMATCH
coverage contract 可执行并被 P-020 / P-040 引用
6 / 6 branches Technical = APPROVE
D-RV-06 / D-RV-07 = APPROVE
BF-D-01 / BF-D-02 = CLOSED
```

之后才能再评估 D content approval；仍不自动等于 candidate freeze / CD-05 / Gate B。
