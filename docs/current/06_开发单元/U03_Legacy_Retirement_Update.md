# U03 Legacy Retirement Update

> Unit：U03 当前版本风险评估  
> 对应前置：`U03_Legacy_Impact_Assessment.md`  
> 当前实施分支：`impl/u03-current-version-risk-assessment`  
> 本文件只更新 U03 工程实现后的 Legacy 状态，不授权物理删除。

---

## 1. 本轮已建立的新边界

当前 U03 工程链已形成：

```text
exact Current Clinical State Version
↓
Foundation-1 CapabilityInvocationGuard
↓
U03 versioned Rule/Knowledge Release Binding
↓
C02 candidate-only provider boundary
↓
Evidence Acceptance
↓
D09 business-owner boundary
↓
K09 typed Risk proposal
↓
P01 / StateCommitter
↓
version-bound current_risk_assessment
↓
P05 release-aware trace
```

但真实 C02 医学 Risk Engine、真实 Safety-critical Rule Pack、临床 EvalSet、U04 Safety Gate 和外部 Unit wiring 仍未完成。

---

## 2. Legacy / Existing Asset 状态更新

| Asset / Pattern | Pre-U03 Assessment | Post-U03 Update | 物理删除 |
|---|---|---|---|
| legacy Risk Engine / risk logic（若后续定位到） | REFACTOR / EVIDENCE_ONLY | 仍只能作为受审查 evidence producer 候选；不得拥有 Clinical Risk Disposition；当前默认分支未定位到可直接接入的 governed implementation | NOT_AUTHORIZED |
| legacy red-flag / direct risk branching | BLOCK_NEW_U03_USE | 新 U03 不依赖这类 direct branching；保持 BLOCK_NEW_U03_USE | NOT_AUTHORIZED |
| fixed `DiagnosisWorkflowOrchestrator` risk routing | LEGACY_ONLY / BLOCK_NEW_USE | 未被新 U03 复用；U03 application 不负责 U04/U14 路由 | NOT_AUTHORIZED |
| direct CDP / Map risk writes | BLOCK_NEW_U03_USE | 新 U03 使用 typed K09 proposal → P01 / StateCommitter | NOT_AUTHORIZED |
| Foundation-1 Capability Binding / Guard | REUSE | 已被 U03 Governed Candidate Gateway 复用 | N/A |
| Foundation-1 P05 trace | REUSE + INCREMENT | 已增量支持 Rule/Knowledge release refs 与 FAILED-with-formal-outcome trace | N/A |
| P01 StateCommitter / boundary validator | KEEP + ADAPT/REUSE | 已被 U03 typed Risk proposal 复用，无需扩大通用 state write boundary | N/A |
| U02 versioned Clinical State semantics | REUSE | U03 command/proposal 明确绑定 exact Clinical State Version | N/A |

---

## 3. Strangler 状态

当前只能声明：

```text
U03 new engineering component path = EXISTS / COMPONENT_VERIFIED
legacy direct risk path = BLOCK_NEW_U03_USE
```

不能声明：

```text
legacy risk runtime = RETIRED
legacy external route = CUT_OVER
U03 = BUSINESS_WIRED
U03 = E2E_VERIFIED
```

因为尚未完成：

- 真实 C02 clinical engine / governed adapter；
- Safety-critical Rule/Knowledge production release；
- D09 production clinical policy；
- U03 Unit Router / external runtime wiring；
- U03 → U04 Safety Gate wiring；
- 临床专项 Eval；
- E2E；
- 回滚窗口与生产迁移。

---

## 4. Retirement 条件仍未满足

任何 legacy risk-related implementation 的物理删除至少需要：

```text
1. 替代 C02 clinical engine = IMPLEMENTED + CLINICALLY_EVALUATED
2. Production Rule / Knowledge Release = APPROVED + VERSIONED + REGISTERED
3. D09 production clinical policy = APPROVED
4. U03 Runtime Wiring = WIRED
5. U03 -> U04 safety path = WIRED + VERIFIED
6. U03 E2E = VERIFIED
7. 目标 Consultation 对 legacy risk path = UNREACHABLE
8. Regression / migration / rollback evidence = PASS
9. Explicit Legacy Retirement / deletion authorization = GRANTED
```

当前上述条件没有全部满足。

---

## 5. 当前结论

```text
U03 Legacy Impact Assessment = COMPLETE
U03 Engineering Replacement Skeleton = IMPLEMENTED / COMPONENT_VERIFIED
Legacy New Dependency = BLOCKED
Legacy Runtime Retirement = NOT_COMPLETE
Legacy Physical Deletion = NOT_AUTHORIZED
External Cutover = NOT_COMPLETE
Clinical Evaluation = NOT_COMPLETE
Production Authorization = BLOCKED
Merge Authorization = NOT_GRANTED
```

本次 Update 的作用是确认：新 U03 工程路径已经不需要继续扩张 legacy direct-risk 语义，但 legacy 资产尚不能删除，也不能声称已经被生产替换。
