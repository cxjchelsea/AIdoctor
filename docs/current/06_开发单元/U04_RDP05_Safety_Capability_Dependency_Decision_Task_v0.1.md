# U04 RDP-05 Safety Capability / Dependency Owner Decision Package v0.2

> 目标：把当前 V1 U04 所需 Safety Capability / dependency 语义整理成可直接签署的 Owner Decision Package。  
> 当前状态：`OWNER_DECISION_REQUIRED / OPEN / BLOCKING`。  
> 本文件不授权任何新 Capability 调用、fallback、production wiring 或真实患者流量。

## 1. 已冻结约束

已冻结：

```text
如果某 Safety Capability 被正式定义为 required：
其 unavailable / failed 状态不得被解释成 ALLOW
```

另外：

```text
undefined dependency absence != safe
capability failure != clinical negative
fallback exists != fallback authorized
```

## 2. 当前必须先回答的总问题

### D2-01 当前 U04 V1 是否存在 U03 之外的 required Safety Capability？

Owner 必须选择：

- [ ] A. 否。当前 U04 V1 的 Safety Gate 只消费已验证的 U03 handoff + 当前 Safety Policy/Scope；不新增外部/独立 Safety Capability。
- [ ] B. 是。存在以下 required capabilities：________
- [ ] C. 部分 required、部分 optional，见下表。
- [ ] D. 其他：________

如果选择 A：

```text
RDP-05 可冻结为：
NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE
```

但仍需确认未来新增 dependency 必须重新 governance。

## 3. Dependency inventory

如 D2-01 不是 A，Owner 必须完整填写：

| Dependency / Capability | Exact ID | Version / Release | REQUIRED / OPTIONAL / PROHIBITED | Scope | Failure consequence | Fallback |
|---|---|---|---|---|---|---|
| ______ | ______ | ______ | ______ | ______ | ______ | ______ |
| ______ | ______ | ______ | ______ | ______ | ______ | ______ |

禁止使用：

```text
latest
current
auto
best available
any compatible
```

作为 governed version identity。

## 4. REQUIRED dependency 语义

对每个 REQUIRED dependency，必须冻结：

### D2-02 unavailable

- [ ] Gate = `UNAVAILABLE`
- [ ] Gate = `BLOCKED`
- [ ] 由 dependency 类型决定：________
- [ ] 其他：________

### D2-03 timeout

- [ ] 等价于 unavailable
- [ ] 等价于 dependency failure
- [ ] 可 retry 后再判定，retry policy 由单独 Runtime Policy 冻结
- [ ] 其他：________

### D2-04 execution failure

- [ ] Gate = `UNAVAILABLE`
- [ ] Gate = `BLOCKED`
- [ ] 进入批准 fallback 后重新求值
- [ ] 其他：________

注意：任何路径都不得因为 dependency failure 自动形成 `ALLOW`。

## 5. OPTIONAL dependency 语义

如存在 OPTIONAL dependency，必须冻结：

```text
缺失时是否影响 Gate：
YES / NO / CONDITIONALLY

影响条件：
________________________________

若不影响 Gate，其结果是否只用于解释/附加约束：
________________________________
```

Optional 不得通过实现层临时升级为 Required，也不得反向。

## 6. PROHIBITED dependency

PROHIBITED dependency：

- 不得由 U04 调用；
- 不得影响 Gate；
- 不得作为 hidden fallback；
- 不得通过 Model/Agent 间接调用绕过治理。

Owner 可列出当前明确禁止的 capability：

```text
________________________________
```

## 7. Fallback policy

Owner 必须选择：

- [ ] A. 当前 U04 V1 不允许 Safety Capability fallback。
- [ ] B. 允许，但仅限以下明确批准 fallback：________
- [ ] C. 其他：________

若允许 fallback，必须冻结：

```text
primary capability id/version:
fallback capability id/version:
trigger:
authorization:
expected gate semantics:
evidence requirements:
max attempts / timeout owner:
```

未经签署的 fallback = NOT_AUTHORIZED。

## 8. Authorization and least privilege

每个 required/optional dependency 必须明确：

```text
allowed input fields
forbidden input fields
PHI minimum necessary boundary
network/external call permission
timeout owner
retry owner
audit/trace requirements
```

这些内容可在后续 technical contract 细化，但业务级授权范围必须先冻结。

## 9. Verification cases

Owner 签署后至少需要：

```text
U04-DEP-001 required dependency available
U04-DEP-002 required dependency unavailable
U04-DEP-003 required dependency timeout
U04-DEP-004 required dependency failed
U04-DEP-005 optional dependency unavailable
U04-DEP-006 prohibited dependency invocation attempt
U04-DEP-007 unapproved fallback attempt
U04-DEP-008 wrong dependency version
```

如果 D2-01=A（当前无额外 required Safety Capability），则必须替换为：

```text
U04-DEP-001 no-additional-dependency baseline
U04-DEP-002 unauthorized dependency introduction attempt
U04-DEP-003 hidden fallback attempt
U04-DEP-004 future dependency without governance attempt
```

## 10. Owner sign-off

```text
Safety/Product/Medical Owner:
____________________

Dependency policy version:
____________________

Effective scope:
U04 V1 / NON_PRODUCTION_ONLY

Approval date:
____________________
```

## 11. 当前 Verdict

```text
U04-RDP-05
= OWNER_DECISION_REQUIRED
= OPEN / BLOCKING

Undefined dependency policy
= NOT_IMPLEMENTABLE

U04 Implementation Readiness
= NOT_READY
```
