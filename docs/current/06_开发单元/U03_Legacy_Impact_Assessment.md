# U03 Legacy Impact Assessment

> Unit：U03 当前版本风险评估  
> 阶段：Unit 前置 Legacy Impact Assessment  
> 分支：`prep/u03-current-version-risk-assessment`  
> 基线：`main@df1ee8dab58156ec9594d505c06d31622633072b`（Foundation-1 + C01-U01 + U02 已合并）  
> 本文件只冻结 U03 会触碰的旧资产、复用边界与退役条件；不构成 Implementation Authorization、Merge Authorization、Clinical Runtime Enablement 或物理删除授权。

---

## 1. U03 冻结业务目标

```text
Current Clinical State Version exists
+
Risk Assessment = NOT_ASSESSED / STALE
+
CLINICAL_STATE_CHANGED or RISK_REEVALUATION_REQUIRED
↓
C02 structured risk evidence / candidate assessment
↓
D09 deterministic Clinical Risk Disposition
↓
Risk Assessment = VALID + exactly one disposition
  NO_HIGH_RISK_SIGNAL / CAUTION / HIGH_RISK
or
Risk Assessment = FAILED
↓
K09 governed proposal
↓
P01 / G2 governed commit
↓
Risk result bound to exact Clinical State Version
↓
U04 Safety Gate
```

必须保持：

```text
Risk FAILED != NO_HIGH_RISK_SIGNAL
NO_HIGH_RISK_SIGNAL != SAFE
NO_DDX != LOW_RISK
Capability Result != Clinical Truth
C02 evidence != Clinical Risk Disposition
Candidate != Decision != Proposal != Commit
Risk result must bind exact Clinical State Version
newer Clinical State Version invalidates prior Risk currentness
high-risk capability / rule / knowledge failure must fail closed
```

U03 不拥有 Safety Gate，也不负责 U04 之后的 Failure Routing、Readiness、Question、DDx 或 Delivery。

---

## 2. 当前相关 Legacy / Existing 状态

当前主线已经具备：

```text
U02 governed Clinical Fact / Derived Assertion formation
→ versioned Clinical State
→ minimum D05 downstream invalidation markers

Foundation-1
→ P06 Capability Binding Registry
→ BindingReleaseResolver core
→ CapabilityInvocationGuard
→ P05 capability-call trace baseline

P01 / G2 / StateCommitter
→ governed proposal / commit / version mechanics
```

但当前主线尚不存在可作为 U03 正式链路直接复用的完整 governed Risk 链：

```text
C02 governed risk-evidence contract = NOT_IMPLEMENTED
D09 deterministic Risk Disposition = NOT_IMPLEMENTED
versioned Safety-critical Risk Rule Pack = NOT_IMPLEMENTED
P04 minimal Knowledge/Rule Release governance = NOT_IMPLEMENTED
P06 Risk Rule / Knowledge release binding = NOT_IMPLEMENTED
P05 Risk release refs trace increment = NOT_IMPLEMENTED
```

冻结 Capability 设计将既有 `Risk Engine` 定义为 `REFACTOR` 候选，而不是正式 Risk Owner；新 U03 不得直接继承旧 Risk Engine 的最终裁决语义。

---

## 3. Legacy / Existing Asset Matrix

| Asset | U03 处置 | 新依赖允许？ | U03 方向 | 删除条件 |
|---|---|---|---|---|
| legacy `Risk Engine` / risk heuristics | REFACTOR + EVIDENCE_ONLY | YES，仅 C02 boundary 以下 | 只允许贡献 RedFlag/RiskFactor/Vital/Must-not-miss evidence；不得拥有正式 disposition | C02 Eval + governed D09 稳定后逐资产处理 |
| legacy fixed workflow / `DiagnosisWorkflowOrchestrator` | LEGACY_ONLY + BLOCK_NEW_U03_USE | NO | Unit-driven U03 + U04 | U03/U04 与后续 Unit 迁移完成后 |
| legacy orchestration direct risk branching | REPLACE + BLOCK_NEW_USE | NO | U03 先形成 Risk Assessment，U04 唯一求 Safety Gate | U03/U04 新链 WIRED+VERIFIED 后 |
| legacy implicit `low risk / safe` default | REPLACE + BLOCK_NEW_USE | NO | explicit `VALID/FAILED` + D09 disposition | governed U03 完全替代后 |
| `CDPManager.updateCDP()` direct write | BLOCK_NEW_U03_USE | NO | K09→P01/G2/StateCommitter | governed path 全迁移后 |
| U02 versioned Clinical State / Clinical Facts | KEEP / REUSE | YES | C02 必须读取 exact current Clinical State Version | 无删除计划 |
| U02 minimum D05 invalidation hook | KEEP / REUSE | YES | newer facts make prior Risk STALE/currentness invalid | full D05 后再扩展 |
| `StateCommitter` / P01 mechanics | KEEP + ADAPT | YES | typed Risk Assessment proposal/commit | 无删除计划 |
| `StatePatchBoundaryValidator` | KEEP + ADAPT | YES | Risk typed path/value validation | 无删除计划 |
| `ClinicalCdpStateRepositoryAdapter` / version persistence | KEEP + ADAPT | YES through P01 | Risk formal write must not bypass P01 | 无删除计划 |
| Foundation-1 P06 capability binding | KEEP / REUSE + U03 INCREMENT | YES | C02 binding + release binding refs | 无删除计划 |
| `BindingReleaseResolver` / `CapabilityInvocationGuard` | KEEP / REUSE + U03 INCREMENT | YES | governed C02 invocation + exact release resolution | 无删除计划 |
| Foundation-1 P05 trace | KEEP / REUSE + U03 INCREMENT | YES | evidence→D09 decision→proposal→commit + release refs | 无删除计划 |
| P04 Knowledge/Rule governance | NEW MINIMAL IN U03 | YES | immutable/versioned safety-critical rule/knowledge releases | 后续消费者按需扩展 |
| D09 Clinical Risk Disposition | NEW | YES | sole deterministic owner of final disposition | 无删除计划 |

---

## 4. U03 新链禁止事项

```text
让 C02 / model / Risk Engine 直接拥有正式 Clinical Risk Disposition
Risk capability failure → NO_HIGH_RISK_SIGNAL
no red flag found → SAFE
NO_DDX → LOW_RISK
无 exact Clinical State Version 绑定的 Risk 结果
使用旧版本 Risk 结果冒充新 Clinical State Version 当前结果
直接通过 CDPManager.updateCDP 写 Risk Assessment
绕过 P01/G2 提交正式 Risk
未绑定 Rule/Knowledge/Capability Version 即形成 safety-critical Risk Decision
规则或知识 release 失败时静默 fallback 为低风险
U03 FAILED 同时并发进入 U04 和 U14
为 U03 提前实现 U04 Safety Gate 或后续 Unit
```

---

## 5. Strangler / 接线边界

```text
current Clinical State Version
↓
U03 Application / Business Owner
↓
P06 Resolver + CapabilityInvocationGuard
↓
C02 candidate-only structured risk evidence
+
P04 versioned Risk Rule / Knowledge release
↓
D09 deterministic Clinical Risk Disposition
↓
K09 typed Risk Assessment proposal
↓
P01 / G2 / StateCommitter
↓
new Clinical State Version with Risk Assessment bound to evaluated version
↓
U04 input only
```

旧 risk/workflow 逻辑只能在明确适配后作为 C02 evidence-level asset 使用；不得把旧 final-risk semantics 接入新正式状态链。

---

## 6. Foundation-2 判断

```text
Foundation-2 = NOT_REQUIRED
```

理由：

1. Foundation-1 已提供 Registry / Resolver / Invocation Guard / Trace 的共享骨架；
2. Capability 设计明确 C02 `FIRST_CONSUMER_UNIT = U03`；
3. Safety-critical Risk Rule Pack 也是 U03 首次消费者驱动建设；
4. P04/P06/P05 在 U03 只需要最小、可验证的 release governance/binding/trace increment；
5. 将这些增量先抽成独立 Foundation-2 会脱离首个消费者，扩大平台范围，违背 consumer-driven foundation 原则。

因此 U03 应在自身实施包内完成最小 P04/P06/P05 增量，不建设 full P04/P06 release platform。

---

## 7. 物理删除仍不授权

当前不删除 legacy Risk Engine、fixed workflow、旧 risk branching、`CDPManager.updateCDP()` 或旧数据结构。

物理删除至少等待对应新路径 `WIRED + VERIFIED`、目标旧入口不可达、回归通过、迁移与回滚条件满足，并另行授权。

---

## 8. 结论

```text
U03 Legacy Impact Assessment = COMPLETE
Legacy Risk Engine = REFACTOR / EVIDENCE_ONLY
Legacy final-risk semantics = BLOCK_NEW_U03_USE
Legacy fixed workflow/direct risk branching = LEGACY_ONLY / BLOCK_NEW_USE
U02 versioned Clinical State = REUSE
P01 / StateCommitter = KEEP + ADAPT
Foundation-1 P06/Resolver/Guard/P05 = REUSE + U03 INCREMENT
P04 minimal Risk Rule/Knowledge Release Governance = NEW / U03-CONSUMER-DRIVEN
D09 deterministic Risk Disposition = NEW / SOLE FINAL OWNER
Foundation-2 = NOT_REQUIRED
Physical Legacy Removal = NOT_AUTHORIZED
U03 Business Implementation = NOT_STARTED
U03 Implementation Authorization = NOT_GRANTED
Merge Authorization = NOT_GRANTED
```
