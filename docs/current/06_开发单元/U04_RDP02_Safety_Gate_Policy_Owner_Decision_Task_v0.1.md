# U04 RDP-02 Safety Gate Policy / Owner Decision Package v0.3

> 目标：给出一套可直接由 Safety/Product/Medical Owner 审阅并签署的 U04 Safety Gate policy proposal。  
> 当前状态：`APPROVED_AS_PROPOSED / FROZEN / PASS_FOR_READINESS`。  
> 本方案已由仓库所有者在项目治理会话中显式批准为当前 U04 V1 / NON_PRODUCTION_ONLY 的 Owner Decision。  
> 本文件不授权 U04 实现、owner execution、routing、production 或真实患者流量。

## 1. 已冻结、不可修改的上游约束

Safety Gate 唯一业务状态：

```text
ALLOW
RESTRICTED
BLOCKED
UNAVAILABLE
```

已冻结：

```text
HIGH_RISK -> 不得保持普通 ALLOW
Risk Assessment FAILED -> 不得 ALLOW
required Safety Capability unavailable -> Gate != ALLOW
普通 Planner / Model / Agent 不得覆盖 Safety Gate
U04 只决定“当前普通临床是否获准继续”
U14 只决定“失败后如何恢复 / 降级 / Safe Exit / FAILED_TERMINAL”
U04 与 U14 不得竞争同一个最终业务决策

NO_HIGH_RISK_SIGNAL != SAFE
NO_HIGH_RISK_SIGNAL != NORMAL
Risk FAILED != NO_HIGH_RISK_SIGNAL
```

## 2. 建议冻结的 V1 Gate Policy

### D1-01 NO_HIGH_RISK_SIGNAL

**建议：C. 条件式 ALLOW。**

```text
当且仅当：
- U03 Risk Assessment = VALID
- disposition = NO_HIGH_RISK_SIGNAL
- U03 handoff 已通过 U04 RDP-01 admission
- Clinical State Version 为当前版本
- scope 已建立且适用
- 不存在 required Safety Capability unavailable/failure
- 不存在更高优先级 safety conflict

→ Safety Gate = ALLOW
```

这里的 `ALLOW` 只表示：

```text
ordinary clinical continuation permitted
```

明确不表示：

```text
patient is safe
patient is normal
no disease
no further risk
```

建议：

```text
ordinary_u05_allowed = true
u11_eligibility = false
u14_eligibility = false
reason_code = U04_NO_HIGH_RISK_SIGNAL_ALLOWED
```

### D1-02 CAUTION

**建议：B. RESTRICTED。**

理由：

- CAUTION 已不是普通低约束状态；
- 但现有冻结文档也没有把 CAUTION 定义成 HIGH_RISK；
- 因此 V1 最小权限方案是允许继续，但必须进入受限普通路径，而不是普通 ALLOW。

建议冻结：

```text
Risk Assessment = VALID
disposition = CAUTION
且无更高优先级 blocker

→ Safety Gate = RESTRICTED
```

RESTRICTED 的 V1 含义建议限定为：

```text
- 可以暴露 U05 eligibility
- 但下游必须知道当前 Gate = RESTRICTED
- 后续不得把 RESTRICTED 投射成 SAFE/NORMAL
- 不允许跳过后续安全约束
- 不自动触发 SAFETY_ESCALATED，除非同时存在 HIGH_RISK 或其他更高优先级安全条件
```

建议：

```text
ordinary_u05_allowed = true
但 restricted context 必须保留
u11_eligibility = false
u14_eligibility = false
reason_code = U04_CAUTION_RESTRICTED
```

### D1-03 HIGH_RISK

**建议：A. 所有 HIGH_RISK 在当前 V1 一律 BLOCKED。**

理由：

- 现有业务闭环已经要求 HIGH_RISK 抢占普通 DDx；
- V1 没有冻结“哪些 HIGH_RISK 允许 restricted ordinary continuation”的更细粒度例外；
- 因而为了避免实现层自己解释 `RESTRICTED`，建议当前 V1 不引入 HIGH_RISK 的 RESTRICTED 子类。

建议冻结：

```text
Risk Assessment = VALID
disposition = HIGH_RISK

→ Safety Gate = BLOCKED
→ ordinary U05 prohibited
→ Consultation SAFETY_ESCALATED eligibility
→ U11 safe-exit/high-risk delivery eligibility
```

建议：

```text
ordinary_u05_allowed = false
u11_eligibility = true
u14_eligibility = false
reason_code = U04_HIGH_RISK_BLOCKED
```

注意：

```text
BLOCKED
!= system failure
!= U14 failure route
```

这是已知安全风险导致的业务阻断。

### D1-04 U03 Risk Assessment FAILED

**建议：A. UNAVAILABLE。**

理由：

```text
FAILED 表示风险能力没有形成可靠业务结论
!= 已知 HIGH_RISK
!= 已知 NO_HIGH_RISK_SIGNAL
```

因此建议：

```text
U03 status = FAILED
→ Safety Gate = UNAVAILABLE
→ ordinary U05 prohibited
→ U14 failure-handling eligibility = true
```

建议：

```text
ordinary_u05_allowed = false
u11_eligibility = false
u14_eligibility = true
reason_code = U04_RISK_ASSESSMENT_UNAVAILABLE
```

### D1-05 Scope unavailable / not established

**建议：A. UNAVAILABLE。**

```text
scope unavailable / not established
→ 当前系统没有足够授权或适用性证明形成 ordinary Gate permission
→ Gate = UNAVAILABLE
→ ordinary U05 prohibited
→ U14 / safe-exit failure handling eligibility
```

建议：

```text
ordinary_u05_allowed = false
u11_eligibility = false
u14_eligibility = true
reason_code = U04_SCOPE_UNAVAILABLE
```

这里不把 scope failure 当成患者 HIGH_RISK。

### D1-06 conflicting safety inputs

**建议：不采用简单“数值最严格 Gate”排序，而采用 reason-class precedence。**

原因：

```text
BLOCKED = 已知业务/安全条件明确禁止普通路径
UNAVAILABLE = 无法形成可靠权限判断
```

二者不是一个单轴严重程度。

建议冻结冲突原则：

```text
1. 已知 HIGH_RISK 的正向安全事实不得被技术/依赖 failure 覆盖
   → BLOCKED

2. 没有已知 HIGH_RISK，但 Safety 判断因能力/范围/依赖不可用而无法成立
   → UNAVAILABLE

3. CAUTION 与 NO_HIGH_RISK_SIGNAL 不得同时作为同一版本唯一 U03 disposition；
   若出现这种内部冲突
   → typed conflict failure / no Gate commit
   → 不允许实现层挑一个继续
```

因此：

```text
BLOCKED 与 UNAVAILABLE
= 按原因类型选择
!= 简单严格度排序
```

### D1-07 dependency failure

**建议：required dependency failure -> UNAVAILABLE。**

前提：仅针对 RDP-05 正式定义为 REQUIRED 的 dependency。

```text
required dependency unavailable / timeout exhausted / execution failed
→ Gate = UNAVAILABLE
→ ordinary U05 prohibited
→ U14 eligibility = true
```

如果同时已经存在明确 HIGH_RISK：

```text
known HIGH_RISK
→ BLOCKED 优先保留
```

技术 failure 不得擦除已知 HIGH_RISK。

## 3. 建议冻结的唯一 Policy Precedence

建议 Owner 签署以下顺序：

```text
P0 invalid / stale / untrusted inbound
→ typed admission failure
→ no Gate commit

P1 known VALID HIGH_RISK
→ BLOCKED

P2 U03 Risk Assessment FAILED
→ UNAVAILABLE

P3 required Safety Capability unavailable / failed
→ UNAVAILABLE

P4 scope unavailable / not established
→ UNAVAILABLE

P5 VALID CAUTION
→ RESTRICTED

P6 VALID NO_HIGH_RISK_SIGNAL
→ ALLOW
```

额外规则：

```text
若同一 U03 current-version result 同时出现多个互斥 disposition
→ INTERNAL_SAFETY_INPUT_CONFLICT
→ no Gate commit
→ fail closed
```

## 4. 建议 executable expectations

| Case | Input | Expected Gate | U05 | U11 | U14 | Suggested reason |
|---|---|---|---|---|---|---|
| U04-POL-001 | VALID + NO_HIGH_RISK_SIGNAL | ALLOW | yes | no | no | U04_NO_HIGH_RISK_SIGNAL_ALLOWED |
| U04-POL-002 | VALID + CAUTION | RESTRICTED | yes, restricted | no | no | U04_CAUTION_RESTRICTED |
| U04-POL-003 | VALID + HIGH_RISK | BLOCKED | no | yes | no | U04_HIGH_RISK_BLOCKED |
| U04-POL-004 | U03 FAILED | UNAVAILABLE | no | no | yes | U04_RISK_ASSESSMENT_UNAVAILABLE |
| U04-POL-005 | scope not established | UNAVAILABLE | no | no | yes | U04_SCOPE_UNAVAILABLE |
| U04-POL-006 | mutually conflicting U03 dispositions | no Gate commit | no | no | failure handling only after typed conflict | INTERNAL_SAFETY_INPUT_CONFLICT |
| U04-POL-007 | required dependency unavailable | UNAVAILABLE | no | no | yes | U04_REQUIRED_DEPENDENCY_UNAVAILABLE |
| U04-POL-008 | stale U03 handoff | no Gate commit | no | no | no normal route | STALE_U03_HANDOFF |

这些 reason code 是**建议的技术/治理标识**，不是新的临床诊断语义；Owner 可改名，但不得改变已冻结业务区别。

## 5. 为什么采用这套建议

这套 proposal 的设计目标是：

```text
最少新增语义
+ 最小权限
+ fail closed
+ 不把 technical failure 误报成 clinical high risk
+ 不把 no-high-risk-signal 误报成 patient safe
+ 不让 HIGH_RISK 进入普通 DDx
+ 不让 U04 与 U14 双重拥有失败路由
```

## 6. Owner sign-off

以下内容已完成 Owner 决策记录：

```text
Owner authorization source:
Explicit repository-owner approval in project governance conversation

Decision package version:
U04-SAFETY-GATE-POLICY-V0.1-FROZEN

Effective scope:
U04 V1 / NON_PRODUCTION_ONLY

Decision:
APPROVE AS PROPOSED

Approval date:
2026-09-18
```

## 7. 当前 Verdict

```text
U04-RDP-02
= APPROVED_AS_PROPOSED
= FROZEN / PASS_FOR_READINESS

Developer-selected defaults
= PROHIBITED

U04 Implementation Readiness
= READY_FOR_REVIEW
```
