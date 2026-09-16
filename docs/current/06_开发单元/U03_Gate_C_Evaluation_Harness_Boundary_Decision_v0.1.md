# U03 Gate C Evaluation Harness Boundary Decision v0.1

> 对象：`BF-CD06-EXEC-01 = NO_EXECUTABLE_GOVERNED_EVALUATION_PATH` 的边界裁决。  
> 状态：`DECISION_COMPLETE / HARNESS_INFRA_ALLOWED / CLINICAL_EXECUTABLE_SEMANTICS_REQUIRES_IMPLEMENTATION_AUTHORIZATION / NOT_GATE_C / NOT_FOR_PRODUCTION`。  
> 前置：`CD-06 = REVIEW_READY`；`ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION`。  
> 本文件不授予 Implementation Authorization。

---

## 1. Decision Question

需要判断：

```text
为 Gate C 建设 governed evaluation harness
是否只是评估基础设施，
还是已经等价于 C02 / D09 clinical implementation？
```

答案必须按责任层拆分，不能把所有“测试代码”统一视为非临床实现。

---

## 2. Current Repository Fact

当前已有：

```text
U03GovernedCandidateGateway
U03EvidenceAcceptanceService
U03DecisionService
U03StateProposalFactory
U03CommitService
U03RiskAssessmentApplicationService
```

这些只形成工程治理/编排边界。

当前缺失：

```text
real executable C02 evidence/rule evaluator
for RR-U03-RISK-001@0.2.1-candidate

real executable D09 deterministic policy evaluator
for PR-U03-D09-001@0.2.1-candidate

runner capable of executing
GC-001..GC-031 + SS-001..SS-020
against those exact frozen semantics
```

因此：

```text
existing engineering tests
!= governed clinical evaluation execution
```

---

## 3. Layer A — Evaluation Harness Infrastructure

以下内容属于 **evaluation infrastructure**，本身不拥有或创造 Clinical Truth：

```text
EvalSet loader
Fixture registry loader
exact-release ref validator
case dispatcher
execution sandbox / in-memory isolation
expected-vs-actual comparator
must_not_output assertion engine
must_not_commit assertion engine
critical-blocking aggregation
stale/release-mismatch assertion capture
idempotency observation
result bundle writer
trace/provenance capture
human-readable report generation
```

必须满足：

```text
no clinical thresholds encoded in harness
no clinical rule predicates authored in harness
no D09 branch conditions reimplemented in harness
no production patient state commit
no runtime release publication / activation
no mutable latest alias
```

因此：

```text
Layer A Harness Infrastructure
= ALLOWED_AS_GATE_C_EVALUATION_TOOLING
```

但只有 Layer A 时，当前仍无法执行 51 个 governed cases，因为没有真实 clinical evaluator target。

---

## 4. Layer B — Executable Clinical Semantics

以下行为属于 **clinical implementation**，即使代码只被 offline harness 调用也不改变其性质：

```text
implement evidence interpretation required by C rules
implement the 15 frozen C rule predicates / thresholds
implement C missingness/scope executable logic
implement D09 P0..P5 branch predicates
implement D09 precedence resolution
implement disposition/reason-code mapping
implement coverage denominator semantics
implement scope-entry TRUE/FALSE/UNKNOWN handling
```

原因：这些代码会把已批准临床内容转化为机器可执行 Clinical Decision Logic。

因此：

```text
"offline only"
!= "not clinical implementation"

"test-only code"
!= authorization exemption
```

结论：

```text
Layer B Executable Clinical Semantics
= REQUIRES_EXPLICIT_IMPLEMENTATION_AUTHORIZATION
```

---

## 5. Separation from CD-07 Runtime Authorization

Gate C 需要 executable evaluator 才能完成独立评估，但这不意味着必须直接授权 production/runtime wiring。

允许治理上定义一个更窄的授权范围：

```text
Evaluation-Only Clinical Implementation Authorization
```

它可以仅允许：

```text
implement frozen C/D semantics for isolated offline evaluation
bind exact Gate-B-qualified candidate refs
run only approved EvalSet fixtures
produce evaluation evidence bundle
```

并明确禁止：

```text
external runtime wiring
real patient traffic
production database / Clinical State mutation
release activation
U04 routing
production publication
legacy retirement
```

因此：

```text
Evaluation-Only Implementation Authorization
!= CD-07 Runtime Implementation Authorization
!= Production Authorization
```

但无论范围多窄，仍必须由用户显式授权，不能由 Gate C readiness 自动推导。

---

## 6. Recommended Architecture Boundary

推荐最小结构：

```text
Gate C Harness
  ├─ EvalSetLoader
  ├─ FixtureResolver
  ├─ ExactReleaseBindingValidator
  ├─ EvaluationExecutionAdapter
  ├─ AssertionEngine
  └─ ResultBundleWriter

EvaluationExecutionAdapter
  ↓
Evaluation-only C02 executable adapter
  ↓
Frozen C 0.2.1 semantics
  ↓
Evaluation-only D09 executable policy
  ↓
Frozen D 0.2.1 + Coverage 0.2.1 semantics
```

关键约束：

```text
Harness owns orchestration / comparison only.
Clinical evaluator owns executable frozen semantics only.
Neither may change expected outcomes.
Neither may invent clinical rules.
```

---

## 7. Authorization Matrix

| Work item | Current status | Authorization needed? |
|---|---|---|
| Harness data model / interfaces | ALLOWED | No new clinical implementation auth |
| Loader / comparator / reporter design | ALLOWED | No new clinical implementation auth |
| Documentation / test-plan generation | ALLOWED | No |
| Stub evaluator for plumbing only | ALLOWED only if explicitly non-evidentiary | No, but cannot produce Gate C evidence |
| Implement 15 C rules | BLOCKED | Yes |
| Implement C missingness/scope semantics | BLOCKED | Yes |
| Implement D09 P0..P5 / precedence / disposition | BLOCKED | Yes |
| Execute 51 cases against real clinical semantics | BLOCKED until above exists | Yes, indirectly depends on authorized implementation |
| Runtime wiring / production activation | BLOCKED | Separate later authorization |

---

## 8. Decision on BF-CD06-EXEC-01

当前：

```text
BF-CD06-EXEC-01 = OPEN
```

不能仅通过建设 Layer A harness 关闭，因为：

```text
harness without executable clinical semantics
= runnable shell without valid evaluation target
```

关闭条件至少需要：

```text
Layer A harness = runnable / isolated / exact-release-bound
AND
Layer B clinical evaluator = implemented under explicit authorization
AND
clinical evaluator semantics = independently reviewed against frozen C/D/Coverage refs
AND
no production/runtime wiring
```

---

## 9. Current Authorization Decision

当前没有新的 Implementation Authorization。

因此允许继续：

```text
harness interface / architecture / execution contract design
implementation-readiness package preparation
independent review planning
```

当前禁止：

```text
writing executable C02 clinical rules
writing executable D09 clinical policy
claiming evaluation execution has started
producing fabricated result bundles
```

---

## 10. Final Verdict

```text
Evaluation Harness Boundary
= SPLIT

Layer A evaluation infrastructure
= MAY_PROCEED_AS_DESIGN / NON-CLINICAL TOOLING

Layer B executable clinical semantics
= CLINICAL IMPLEMENTATION
= EXPLICIT IMPLEMENTATION AUTHORIZATION REQUIRED

BF-CD06-EXEC-01
= OPEN
Gate C
= NOT_PASSED
```

下一步应准备最小 `Evaluation-Only Clinical Implementation Authorization` 范围与 LIA/readiness 包；只有用户显式授权后，才允许实现 Layer B，并随后执行 Gate C EvalSet。
