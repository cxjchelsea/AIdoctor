# U04 RDP-02 Safety Gate Policy / Owner Decision Package v0.2

> 目标：把当前仍未冻结的 U04 Safety Gate 业务语义整理成可直接签署的 Owner Decision Package。  
> 当前状态：`OWNER_DECISION_REQUIRED / OPEN / BLOCKING`。  
> 本文件不授权 U04 实现，不授权 U04 owner execution，不授权 routing，不授权 production 或真实患者流量。

## 1. 已冻结、不可再由实现层修改的内容

Safety Gate 唯一业务状态：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

已冻结强约束：

```text
HIGH_RISK -> 不得保持普通 ALLOW
Risk Assessment FAILED -> 不得 ALLOW
required Safety Capability unavailable -> Gate != ALLOW
普通 Planner / Model / Agent 不得覆盖 Safety Gate
U04 只决定“当前普通临床是否获准继续”
U14 只决定“失败之后如何恢复 / 降级 / Safe Exit / FAILED_TERMINAL”
U04 与 U14 不得竞争同一个最终业务决策
```

另外：

```text
NO_HIGH_RISK_SIGNAL != SAFE
NO_HIGH_RISK_SIGNAL != NORMAL
Risk FAILED != NO_HIGH_RISK_SIGNAL
```

这些内容已经是冻结约束，不属于本次 Owner 选择项。

## 2. Owner 必须签署的决策表

### D1-01 NO_HIGH_RISK_SIGNAL

已知：

```text
U03 = VALID / NO_HIGH_RISK_SIGNAL
!= SAFE / NORMAL
```

Owner 必须选择 U04 Gate 行为：

- [ ] A. `ALLOW`
- [ ] B. `RESTRICTED`
- [ ] C. 条件式：满足 ______ 时 `ALLOW`，否则 `RESTRICTED`
- [ ] D. 其他：________

必须补充：

```text
允许进入 U05 的附加前提（如有）：
________________________________
```

### D1-02 CAUTION

当前没有冻结的唯一 Gate 映射。

Owner 必须选择：

- [ ] A. `ALLOW`
- [ ] B. `RESTRICTED`
- [ ] C. `BLOCKED`
- [ ] D. 条件式映射：________
- [ ] E. 其他：________

必须补充：

```text
CAUTION 下仍可继续普通临床路径的条件：
________________________________

CAUTION 下必须停止普通路径的条件：
________________________________
```

### D1-03 HIGH_RISK

已知：

```text
HIGH_RISK -> ordinary ALLOW 禁止
HIGH_RISK + RESTRICTED/BLOCKED
-> Consultation SAFETY_ESCALATED
-> U11
```

Owner 必须冻结 `RESTRICTED` 与 `BLOCKED` 的选择规则：

- [ ] A. 所有 HIGH_RISK 一律 `BLOCKED`
- [ ] B. 所有 HIGH_RISK 一律 `RESTRICTED`
- [ ] C. 条件式：
  - `RESTRICTED` 条件：________
  - `BLOCKED` 条件：________
- [ ] D. 其他：________

注意：不得选择 ordinary `ALLOW`。

### D1-04 U03 Risk Assessment FAILED

已知：

```text
FAILED -> 不得 ALLOW
FAILED -> 普通临床继续必须先被禁止
之后才允许 U14 判断恢复/降级
```

Owner 必须选择 Gate：

- [ ] A. `UNAVAILABLE`
- [ ] B. `BLOCKED`
- [ ] C. 条件式：________
- [ ] D. 其他更严格的已定义 Gate：________

### D1-05 Scope unavailable / not established

Owner 必须选择：

- [ ] A. `UNAVAILABLE`
- [ ] B. `BLOCKED`
- [ ] C. `RESTRICTED`
- [ ] D. 条件式：________
- [ ] E. 其他：________

必须明确：

```text
是否允许进入 U05：
YES / NO / CONDITIONALLY

条件：
________________________________
```

### D1-06 conflicting safety inputs

需要冻结冲突优先级。

Owner 必须选择冲突原则：

- [ ] A. 最严格 Gate 优先
- [ ] B. 指定 Safety Policy 优先级：________
- [ ] C. 冲突直接 `UNAVAILABLE`
- [ ] D. 其他：________

若选择“最严格优先”，还必须确认 Gate 严格度顺序：

```text
建议待签署顺序（非默认真值）：
ALLOW < RESTRICTED < BLOCKED / UNAVAILABLE
```

其中 `BLOCKED` 与 `UNAVAILABLE` 是否有严格度顺序必须由 Owner 决定：

- [ ] BLOCKED > UNAVAILABLE
- [ ] UNAVAILABLE > BLOCKED
- [ ] 二者不可比较，按原因类型决定
- [ ] 其他：________

### D1-07 dependency failure

此项必须与 RDP-05 一致。

Owner 必须选择：

- [ ] A. required dependency failure -> `UNAVAILABLE`
- [ ] B. required dependency failure -> `BLOCKED`
- [ ] C. 由 dependency class 决定：________
- [ ] D. 其他：________

## 3. 决策优先级必须唯一

Owner 还必须冻结 U04 policy evaluation precedence，避免多个输入同时命中时出现双结果。

待签署：

```text
P0 invalid/stale/untrusted input
→ typed admission failure / no Gate commit

P1 Risk Assessment FAILED
→ [待签署 Gate]

P2 required Safety Capability unavailable/failure
→ [待签署 Gate]

P3 scope unavailable/not established
→ [待签署 Gate]

P4 HIGH_RISK
→ [待签署 RESTRICTED/BLOCKED policy]

P5 CAUTION
→ [待签署 Gate]

P6 NO_HIGH_RISK_SIGNAL
→ [待签署 Gate]
```

如果 Owner 采用不同优先级，请完整替换并冻结。

## 4. 必须提供的 executable expectations

Owner 签署后，至少需要给出以下可执行期望案例：

```text
U04-POL-001 NO_HIGH_RISK_SIGNAL
U04-POL-002 CAUTION
U04-POL-003 HIGH_RISK
U04-POL-004 U03 FAILED
U04-POL-005 scope unavailable
U04-POL-006 conflicting safety inputs
U04-POL-007 required dependency unavailable
U04-POL-008 stale U03 handoff
```

每例必须明确：

```text
input
expected_gate
ordinary_u05_allowed
u11_eligibility
u14_eligibility
reason_code
```

## 5. Owner sign-off

```text
Safety/Product/Medical Owner:
____________________

Decision package version:
____________________

Effective scope:
U04 V1 / NON_PRODUCTION_ONLY

Approval date:
____________________
```

## 6. 当前 Verdict

```text
U04-RDP-02
= OWNER_DECISION_REQUIRED
= OPEN / BLOCKING

Developer-selected defaults
= PROHIBITED

U04 Implementation Readiness
= NOT_READY
```
