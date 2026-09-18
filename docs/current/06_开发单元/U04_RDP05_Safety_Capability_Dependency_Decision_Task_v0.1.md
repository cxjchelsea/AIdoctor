# U04 RDP-05 Safety Capability / Dependency Owner Decision Package v0.3

> 目标：给出一套最小依赖、最小权限的 U04 V1 dependency policy proposal，供 Safety/Product/Medical Owner 直接审阅签署。  
> 当前状态：`APPROVED_AS_PROPOSED / FROZEN / PASS_FOR_READINESS`。  
> 本文件冻结当前 U04 V1 / NON_PRODUCTION_ONLY dependency policy；不授权 future dependency、fallback、production wiring 或真实患者流量。

## 1. 已冻结约束

```text
required Safety Capability unavailable / failed
→ 不得解释成 ALLOW

undefined dependency absence != safe
capability failure != clinical negative
fallback exists != fallback authorized
```

## 2. D2-01 当前 V1 dependency strategy

**建议选择 A：当前 U04 V1 不新增 U03 之外的 required Safety Capability。**

建议冻结：

```text
NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE
```

当前 U04 V1 的 Gate decision 只消费：

```text
1. 已验证的 U03 typed handoff
2. 当前已冻结/已批准的 U04 Safety Gate Policy
3. 当前适用 Scope / authorization context
4. G2 / StateCommitter 等已有治理基础设施
```

不新增：

```text
外部急诊 API
额外 LLM safety judge
第二套 risk model
自由 Agent safety tool
未治理规则服务
自动互联网检索
隐藏 fallback
```

作为当前 U04 Gate 的 required dependency。

理由：

- U03 已经完成当前版本 Risk Assessment；
- U04 的职责是解释“当前普通临床是否获准继续”，不是重新做第二次医学风险评估；
- 新增 required Safety Capability 会引入新的 clinical/safety truth source 和新的 availability semantics；
- 当前没有对应 capability package / release / eval / authorization；
- 最小实现可以避免 U03 与 U04 形成重复风险判断 Owner。

## 3. 当前 dependency inventory 建议

| Dependency | Classification | Version identity | Role | Failure consequence |
|---|---|---|---|---|
| Governed U03 handoff | REQUIRED INPUT | exact U03 current-version refs | U04 primary business input | admission failure / no Gate commit |
| U04 Safety Gate Policy | REQUIRED GOVERNED POLICY | exact approved policy ref | unique Gate decision | unavailable policy -> no Gate commit / fail closed |
| Scope / authorization context | REQUIRED GOVERNANCE INPUT | exact current governed context | determine applicability/permission | unavailable/not established -> UNAVAILABLE |
| G2 / StateCommitter | REQUIRED INFRASTRUCTURE FOR MUTATION | existing governed implementation/version | commit canonical Gate state | commit failure != Gate result; fail closed |
| Trace/Audit | REQUIRED GOVERNANCE INFRASTRUCTURE where commit/execution requires it | exact runtime build | durable evidence | failure handled by runtime/governance policy, never ALLOW by default |
| Any new external/model/tool Safety Capability | PROHIBITED UNTIL SEPARATELY GOVERNED | none | none | invocation rejected |

注意：

```text
REQUIRED INPUT / POLICY / GOVERNANCE INFRASTRUCTURE
!= 新增 clinical Safety Capability
```

## 4. D2-02 / D2-03 / D2-04

由于建议 D2-01=A：

```text
当前 U04 V1
不存在额外 required Safety Capability
```

因此：

```text
D2-02 additional capability unavailable
= NOT_APPLICABLE_IN_CURRENT_SLICE

D2-03 additional capability timeout
= NOT_APPLICABLE_IN_CURRENT_SLICE

D2-04 additional capability execution failure
= NOT_APPLICABLE_IN_CURRENT_SLICE
```

但已有必需治理输入/基础设施仍然 fail closed：

```text
U03 handoff invalid/stale
→ no Gate commit

Safety Gate Policy unavailable
→ no Gate commit / fail closed

Scope unavailable/not established
→ Gate = UNAVAILABLE（若 RDP-02 proposal 获批）

StateCommitter failure
→ commit failure
→ 不得伪装为 Gate result
```

## 5. OPTIONAL dependency

**建议当前 V1：NONE。**

```text
OPTIONAL_SAFETY_CAPABILITY_SET = EMPTY
```

这样可以避免“可有可无”的隐藏能力逐渐改变 Gate 语义。

未来新增 optional dependency 必须单独说明：

- 它是否改变 Gate；
- 是否只提供解释/证据；
- unavailable 时是否影响业务权限；
- 是否需要独立 eval。

## 6. PROHIBITED dependency

建议当前 V1 明确禁止以下未经治理的 Gate dependency：

```text
- free-form LLM safety judge
- autonomous Agent-selected safety tool
- unversioned external API
- mutable latest/current rule service
- hidden fallback model/tool
- frontend-computed safety permission
- any capability without exact governed ID/version/scope/eval
```

这些 capability 即使代码可调用，也不得成为 U04 Safety Gate 输入。

## 7. Fallback policy

**建议选择 A：当前 U04 V1 不允许 Safety Capability fallback。**

冻结建议：

```text
U04_V1_SAFETY_CAPABILITY_FALLBACK
= NONE
```

原因：

- 当前不存在额外 required Safety Capability；
- 因此没有必要引入“primary failed -> another model/tool decides safety”的新路径；
- 这可以避免 fallback 重新创造另一套未治理 Safety truth。

未来若新增 fallback，必须重新做：

```text
capability governance
+ exact version binding
+ safety policy review
+ eval
+ implementation authorization
```

## 8. Least-privilege 建议

当前 U04 V1 建议只允许读取：

```text
- U03 typed status/disposition/failure
- Clinical State Version identity
- exact governed release refs
- required provenance refs
- policy/scope identity
- execution correlation refs
```

默认禁止：

```text
- 自由读取完整患者历史
- 自由网络访问
- 自由工具发现
- 任意知识库查询
- 任意 LLM 推理作为 Gate truth
- 未声明 PHI
```

如实现确实需要新增字段，应回到 contract review，而不是扩大通用 Context。

## 9. 建议 verification cases

若 Owner 批准“无额外 Safety Capability”策略：

| Case | Expected |
|---|---|
| U04-DEP-001 no-additional-dependency baseline | Gate 仅依赖 governed U03 + policy/scope |
| U04-DEP-002 unauthorized dependency introduction | rejected / fail closed |
| U04-DEP-003 hidden fallback attempt | rejected |
| U04-DEP-004 future dependency without governance | rejected |
| U04-DEP-005 mutable/latest dependency ref | rejected |
| U04-DEP-006 free-form LLM safety judge attempt | cannot own Gate |
| U04-DEP-007 frontend safety override attempt | rejected |
| U04-DEP-008 undeclared network tool attempt | rejected |

## 10. Future expansion rule

任何未来新增 Safety Capability：

```text
CODE EXISTS
!= U04 DEPENDENCY AUTHORIZED
```

必须重新建立：

```text
capability ID
exact version
REQUIRED/OPTIONAL/PROHIBITED
scope
failure semantics
fallback semantics
verification cases
owner approval
implementation authorization
```

否则不得进入 U04 Gate。

## 11. Owner sign-off

以下内容已完成 Owner 决策记录：

```text
Owner authorization source:
Explicit repository-owner approval in project governance conversation

Dependency policy version:
U04-SAFETY-DEPENDENCY-V0.1-FROZEN

Effective scope:
U04 V1 / NON_PRODUCTION_ONLY

Decision:
APPROVE AS PROPOSED

Approval date:
2026-09-18
```

## 12. 当前 Verdict

```text
U04-RDP-05
= APPROVED_AS_PROPOSED
= FROZEN / PASS_FOR_READINESS

Proposed current-slice strategy:
NO_ADDITIONAL_REQUIRED_SAFETY_CAPABILITY_FOR_CURRENT_U04_V1_SLICE
NO_OPTIONAL_SAFETY_CAPABILITY
NO_SAFETY_CAPABILITY_FALLBACK

U04 Implementation Readiness
= READY_FOR_REVIEW
```
