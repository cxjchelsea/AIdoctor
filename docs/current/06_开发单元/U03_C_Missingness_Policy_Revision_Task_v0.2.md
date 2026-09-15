# U03 C Missingness Policy Revision Task v0.2

> 权威输入：`U03_C_Freeze_Policy_Review_Record_v0.2.md`  
> 目标对象：`U03_C_MISSINGNESS_V0_2`  
> 状态：`REVISION_REQUIRED / POLICY_FREEZE_BLOCKED`

本任务只补齐测量/evidence 不可判定态的显式执行映射。不重写 15 条 rule，不改阈值，不开始 Eval 或 D09。

---

## 1. 必须修订

### 第 4 节 Required Measurement Handling

对 RR / SBP / HR，以下状态必须与 UNKNOWN / UNMEASURED / INVALID 一样明确输出：

```text
NOT_ASKED
AMBIGUOUS
CONFLICTING
→ INPUT_INSUFFICIENT
→ RULE_SIGNAL_INPUT_INSUFFICIENT
```

不得把这些状态写成 NO_MATCH，也不得只依赖第 2 节散文。

### 第 3 节 Required Evidence Handling

建议同步补：

```text
UNMEASURED
→ INPUT_INSUFFICIENT
```

使第 1 节 vocabulary 与第 3 / 4 节映射完整对齐。

### 第 6 节信号字段

`INPUT_INSUFFICIENT` 分支应与 `SCOPE_MISMATCH` 一样显式写出：

```text
signal = RULE_SIGNAL_INPUT_INSUFFICIENT
```

---

## 2. 明确不要做的事

```text
不要改 15 条 active rule predicate / threshold
不要改 SBP <=90 / drop>40 / MODHIGH 共存方向
不要冻结 U03_C_MISSINGNESS_V0_2 或 U03_SEPSIS_SHARED_SCOPE_V0_2
不要开始 pre-freeze Eval fixture 内容
不要创建 RR-U03-RISK-001 candidate version
不要开始 D09
```

---

## 3. 完成定义

```text
第 3 / 4 节覆盖 vocabulary 中全部不可判定态
均明确输出 INPUT_INSUFFICIENT
再审 M2 = APPROVE
```

之后才能把两个 policy 一并评估为 candidate-freeze 可冻结对象。
