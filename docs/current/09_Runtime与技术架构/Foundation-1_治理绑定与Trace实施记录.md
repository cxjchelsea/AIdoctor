# Foundation-1 治理绑定与 Trace 实施记录

> 状态：IMPLEMENTED / COMPONENT_VERIFIED / INDEPENDENT_REVIEWED / NOT_MERGED  
> 分支：`impl/foundation-1-governance-binding-trace`  
> PR：#81  
> 基线：`main@31e77d686e43072096fa52ed5c891bc01c17e76a`  
> 验证代码 HEAD：`c740fe09d6042c82fb2299f3f5045465427b2d2c`  
> 依据：Phase 7/8/9 当前 FROZEN / V1 + 《复杂业务软件开发 SOP V3.3》  
> 本记录不构成 Merge Authorization、Clinical Runtime Enablement、U02 Implementation Authorization 或 Production Authorization。

## 1. 建设原因

Foundation-0 已经提供 Runtime Binding、Canonical Event、Run skeleton、P01 CDP Adapter、版本集合绑定和基础 Trace，但最新 Phase 7/8/9 已进一步冻结：

- `Capability Exists != Capability Active`；
- Capability invocation 必须经过有效 `CapabilityBindingRef`；
- Runtime 需要显式 Binding & Release Resolver；
- 后续 Unit 需要统一关联 capability result / decision / proposal / commit 与 Clinical State Version。

这些缺口跨多个后续 Unit / Capability，属于公共施工前置，而不是 U02 独有业务逻辑。

## 2. 授权范围

本轮只建设三个公共前置：

### F1-01 P06 最小 Capability Binding Governance

已实现最小 durable capability binding：

- binding identity；
- capability identity / version；
- capability set version；
- scope / contract version；
- population / region / language / channel scope；
- ACTIVE / DISABLED / EXPIRED；
- effective window；
- `CapabilityBindingRegistry` 受控注册、禁用、过期边界；
- binding identity 注册后不可静默重定向到另一 capability/version/scope；
- minimal lifecycle 不允许静默 reactivation，重新启用需新 binding identity。

未扩展到完整审批后台、完整 capability package lifecycle、自动 rollout/rollback、C02-C06 预注册。

### F1-02 Binding & Release Resolver Core

已实现：

- CapabilityBindingRef 解析；
- capability identity 校验；
- ACTIVE / effective window 校验；
- scope / contract / population / region / language / channel compatibility；
- fail-closed；
- Capability Invocation 公共校验边界。

本轮未提前实现完整 KnowledgeRelease / RuleRelease registry；留给其真实首个消费者按需扩展。

### F1-03 P05 Capability-call / Governance Trace Baseline

已统一关联：

- consultation / thread / run / event / unit；
- capability / binding；
- capability result；
- decision；
- proposal；
- commit；
- clinical state version before / after；
- call status / failure reason。

Trace 只记录“发生了什么”，不拥有 Clinical Truth。

## 3. 明确不在本轮范围

```text
P01-U02 Typed Clinical Fact Commit
C01-U02 Clinical Fact Parsing
D05 Dependency Invalidation
D01 lifecycle common framework
D10 policy formalization
KnowledgeRelease full registry
RuleRelease full registry
完整 P05 平台
完整 P06 发布/审批平台
U02 business implementation
Phase10 external cutover
legacy deletion
production enablement
```

## 4. 当前实现

新增：

```text
runtime/governance/CapabilityBindingRecord
runtime/governance/CapabilityBindingRepository
runtime/governance/CapabilityBindingRegistry
runtime/governance/CapabilityExecutionContext
runtime/governance/BindingReleaseResolver
runtime/governance/CapabilityInvocationGuard
runtime/governance/CapabilityCallTraceRecord
runtime/governance/CapabilityCallTraceRepository
runtime/governance/CapabilityTraceService
```

数据库：

```text
clinical_capability_binding
clinical_capability_call_trace
```

验证：

```text
Foundation1GovernanceTest
Foundation-1 Verification workflow
Foundation-0 Verification regression workflow
```

## 5. Independent Review 与修复

### F1-R01 P06 只有 Repository，没有受控治理写边界

初版可以读取/验证 binding，但 `ACTIVE / DISABLED / EXPIRED` 仅作为数据字段存在；若业务直接操作 Repository，会削弱“Capability Exists != Capability Active”的治理语义。

处理：新增 `CapabilityBindingRegistry`；注册只能在完整定义一致时幂等复用；同一 binding identity 不允许静默改绑 capability/version/scope；状态变化限定为受控的 ACTIVE → DISABLED / EXPIRED。

状态：CLOSED。

### F1-R02 时间来源不一致

初版 entity 在缺失时间时使用本地 `LocalDateTime.now()`，而 Resolver 使用 UTC Clock，存在时区不一致风险。

处理：binding 创建必须显式提供 `effectiveFrom / createdAt`，时间生成由上层受控边界负责；Resolver 继续使用可测试 UTC Clock。

状态：CLOSED。

### F1-R03 Scope Expansion 检查

审查未发现 P01-U02、C01-U02、D05、D01、D10、完整 P04/P05/P06、U02 业务实现等越界内容。

状态：PASS。

## 6. 可执行验证证据

验证代码 HEAD：

```text
c740fe09d6042c82fb2299f3f5045465427b2d2c
```

Foundation-1 Verification：

```text
Run 34576898396
Shared Contracts install = PASS
diagnosis-service compile = PASS
Foundation-1 focused tests = PASS
diagnosis-service full regression = PASS
job java-governance-foundation = SUCCESS
```

Foundation-0 / U01 / StateCommitter 回归：

```text
Run 34576898406
Shared Contracts install = PASS
diagnosis-service compile = PASS
Foundation-0 + U01 + StateCommitter focused tests = PASS
diagnosis-service full regression = PASS
job java-foundation = SUCCESS
```

## 7. 必须保持的不变量

```text
Capability Exists != Capability Active
Capability Result != Clinical Truth
Decision != Proposal
Proposal != Commit
Trace != Clinical State
Runtime cache != authoritative binding registry
invalid binding -> fail closed
expired/disabled binding -> no clinical capability invocation
binding mismatch -> no silent latest-version fallback
binding identity -> no silent semantic repointing
```

## 8. 当前结论

```text
F1-01 P06 Minimal Binding Governance = IMPLEMENTED / COMPONENT_VERIFIED
F1-02 Binding & Release Resolver Core = IMPLEMENTED / COMPONENT_VERIFIED
F1-03 P05 Governance Trace Baseline   = IMPLEMENTED / COMPONENT_VERIFIED
Independent Review                    = COMPLETE
Review Findings                       = CLOSED
Consumer/Business Wiring              = NOT_COMPLETE
Clinical Runtime                      = NOT_ENABLED
U02                                   = NOT_AUTHORIZED BY THIS WORK
PR #81                                = OPEN / DRAFT / NOT_MERGED
Merge Authorization                   = NOT_GRANTED
Production                            = BLOCKED
```

Foundation-1 当前只证明公共治理底座在组件级可用；它不证明 C01 已正式激活、不证明 U02 已实现、不证明任何 Clinical Business Loop 已闭合，也不构成生产授权。
