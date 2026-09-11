# Foundation-1 治理绑定与 Trace 实施记录

> 状态：IMPLEMENTATION_AUTHORIZED / IN_PROGRESS  
> 分支：`impl/foundation-1-governance-binding-trace`  
> 基线：当前 `main`  
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

实现最小 durable capability binding：

- binding identity；
- capability identity / version；
- capability set version；
- scope / contract version；
- population / region / language / channel scope；
- ACTIVE / DISABLED / EXPIRED；
- effective window。

禁止扩展到完整审批后台、完整 capability package lifecycle、自动 rollout/rollback、C02-C06 预注册。

### F1-02 Binding & Release Resolver Core

实现：

- CapabilityBindingRef 解析；
- capability identity 校验；
- ACTIVE / effective window 校验；
- scope / contract / population / region / language / channel compatibility；
- fail-closed；
- Capability Invocation 公共校验边界。

本轮不提前实现完整 KnowledgeRelease / RuleRelease registry；只保留后续扩展边界。

### F1-03 P05 Capability-call / Governance Trace Baseline

统一关联：

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
```

## 5. 必须保持的不变量

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
```

## 6. 完成标准

Foundation-1 只有在以下条件全部满足后才能标记 VERIFIED：

- diagnosis-service compile PASS；
- Foundation-1 focused tests PASS；
- Foundation-0/U01 相关回归 PASS；
- diagnosis-service full regression PASS；
- Independent Review 完成；
- review findings 全部关闭或明确阻塞；
- 不包含 P01/C01-U02/D05/D01/D10 越界实现。

在此之前：

```text
Foundation-1 = IN_PROGRESS
U02 = NOT_AUTHORIZED BY THIS WORK
Merge Authorization = NOT_GRANTED
```
