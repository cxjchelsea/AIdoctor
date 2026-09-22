# U03 Gate C Governed Evaluation Execution Readiness v0.1

> 对象：`ER-U03-RISK-001@0.1.0-candidate` 的 governed evaluation execution 前置判断。  
> 状态：`ASSESSMENT_COMPLETE / EXECUTION_BLOCKED / HARNESS_OR_EXECUTABLE_CLINICAL_EVALUATOR_MISSING / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> 前置：`CD-06 = REVIEW_READY`；`ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION`。  
> 本文件不构成 Clinical Runtime Implementation Authorization。

---

## 1. 已满足的执行输入

```text
Gate A = PASS
Gate B = PASS / GOVERNED_CONTENT_READY
CD-06 = REVIEW_READY
ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION

Coverage Manifest v0.2 = APPROVED_FOR_EVALUATION
Golden Cases v0.2 = 31 APPROVED_FOR_EVALUATION_CONTENT
Fixture Registry v0.2 = APPROVED_FOR_EVALUATION_INPUT
Safety Suite v0.1 = 20 CONTENT_APPROVED / CRITICAL_BLOCKING_APPROVED
```

受测 governed set：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

因此评估**内容输入已准备完成**。

---

## 2. Governed Evaluation Execution 必须产生的证据

根据 EvalSet schema / release candidate，真正的 governed execution 至少必须产生：

```text
execution_run_id
bound_evalset_release_ref = ER-U03-RISK-001@0.1.0-candidate
bound governed C/D/E/Coverage refs
case-by-case execution result
actual evidence refs / rule refs / policy branch
actual result_status / disposition / reason_code
expected-vs-actual comparison
must_not_output assertion result
must_not_commit assertion result
stale acceptance result
release mismatch acceptance result
idempotency / duplicate effect result
critical safety case result
execution environment / harness version
trace / provenance refs
```

不得仅依据 expected table 人工写 `PASS`。

---

## 3. 当前工程能力核验

当前 `main` 已存在 U03 engineering governance path，包括：

```text
U03GovernedCandidateGateway
U03EvidenceAcceptanceService
U03DecisionService
U03StateProposalFactory
U03CommitService
U03RiskAssessmentApplicationService
```

但这些是治理/编排边界，不等于真实 clinical evaluator。

### 3.1 C02

当前：

```text
U03CandidateProvider
= interface / port
= expects governed C02 adapter implementation
```

未发现可执行当前冻结 `RR-U03-RISK-001@0.2.1-candidate` 的真实 C02 clinical rule/evidence engine 实现。

### 3.2 D09

现有 `U03DecisionService` 通过 `U03DecisionPort` 消费外部 decision implementation。

现有工程测试使用 lambda/stub 构造 decision，并使用 `SYNTHETIC_*` candidate / release values。

这只能证明：

```text
engineering governance boundary behavior
```

不能证明：

```text
31 Golden Cases clinical expected outcomes
20 Safety Suite governed clinical behavior
current C 0.2.1 / D 0.2.1 executable semantics
```

### 3.3 Evaluation harness

当前未发现能够：

```text
load GC-001..GC-031 + SS-001..SS-020
resolve FX-GC-* fixtures
bind exact current frozen releases
execute real C rule semantics
execute real D09 policy semantics
capture forbidden-output / commit assertions
produce case-level evidence bundle
```

的独立 governed evaluation harness。

---

## 4. Blocking Finding

```text
BF-CD06-EXEC-01
= NO_EXECUTABLE_GOVERNED_EVALUATION_PATH
```

含义：

```text
Eval content = READY
Execution target / harness = NOT_AVAILABLE
```

这不是 Clinical Content blocker，也不回退：

```text
Gate B
CD-06 REVIEW_READY
BF-CD06-01
BF-CD06-02
```

它只阻塞：

```text
Evaluation Execution START
Gate C decision
```

---

## 5. 禁止的替代做法

不得把以下内容当作 governed evaluation execution：

```text
人工阅读 expected table 后直接记录 PASS
C57 / D48 pre-freeze fixtures
6 条 targeted Gate-B fixtures
U03RiskAssessmentFlowTest 的 synthetic/stub assertions
mock / lambda 直接返回期望 D09 outcome
未绑定 current frozen release 的 unit tests
```

也不得为了让 case 通过而由 implementation owner 改写 expected outcome。

---

## 6. 所需执行能力边界

要关闭 `BF-CD06-EXEC-01`，至少需要一个独立、可重复、可审计的 evaluation execution path，满足：

```text
Input owner = approved EvalSet / fixture registry
Expected owner = Medical + Policy/Eval reviewed artifacts
Execution owner = evaluation engineering

Execution must consume exact current governed refs
Execution must not use mutable latest aliases
Execution must not publish/activate runtime clinical release
Execution must not commit real patient Clinical State
Execution output must be isolated evaluation evidence
```

允许的目标形态可以是：

```text
offline evaluation harness
or
sandboxed governed clinical evaluator
```

但其 clinical rule/policy execution semantics 必须来自当前冻结 C/D/E objects，不能由开发者自行重新解释。

---

## 7. Authorization Boundary

当前已授权的是：

```text
Gate C governed evaluation execution
```

尚未授权：

```text
CD-07 clinical runtime implementation
C02 production/runtime implementation
D09 production/runtime implementation
U04 implementation
production activation
```

如果关闭 `BF-CD06-EXEC-01` 需要**新增实现 C02 rule engine 或 D09 executable clinical policy code**，则这已经超出纯 evaluation execution tooling，不能借 Gate C 名义绕过后续 `Implementation Authorization`。

如果已有可执行 clinical evaluator 只是仓库定位尚未发现，可直接绑定该现有实现并继续 execution；否则必须先单独裁决 evaluation harness 与 clinical implementation 的边界。

---

## 8. Current Decision

```text
CD-06 = REVIEW_READY
ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION
Evaluation Content = READY

Evaluation Execution = BLOCKED_BEFORE_START
BF-CD06-EXEC-01 = OPEN
reason = NO_EXECUTABLE_GOVERNED_EVALUATION_PATH

Gate C = NOT_PASSED
CD-07 = BLOCKED
Implementation Authorization = NOT_GRANTED
```

---

## 9. Next Allowed Step

```text
identify existing executable evaluator/harness
OR
perform explicit Evaluation Harness Boundary Decision
↓
prove no clinical-runtime authorization bypass
↓
provide runnable, isolated, exact-release-bound evaluation path
↓
close BF-CD06-EXEC-01
↓
then start ER-U03-RISK-001@0.1.0-candidate governed execution
```

在 `BF-CD06-EXEC-01` 关闭前，不得伪造 execution result bundle 或 Gate C PASS。
