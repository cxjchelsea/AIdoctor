# U03 Evaluation-Only Clinical Implementation Authorization Readiness v0.1

> 对象：为关闭 `BF-CD06-EXEC-01` 所需的最小、隔离式 clinical evaluator implementation 授权前置。  
> 状态：`READINESS_COMPLETE / AUTHORIZATION_REQUIRED / NOT_AUTHORIZED / NOT_RUNTIME / NOT_PRODUCTION`。  
> 依据：`U03_Gate_C_Evaluation_Harness_Boundary_Decision_v0.1.md`。  
> 本文件不是 Implementation Authorization；只有用户显式授权后才可实施。

---

## 1. Why This Authorization Exists

Gate C 当前已满足：

```text
Gate A = PASS
Gate B = PASS
CD-06 = REVIEW_READY
ER-U03-RISK-001@0.1.0-candidate = READY_FOR_EVALUATION
```

但：

```text
BF-CD06-EXEC-01 = OPEN
reason = NO_EXECUTABLE_GOVERNED_EVALUATION_PATH
```

当前不存在能够真实执行：

```text
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
```

的 C02 / D09 executable evaluator。

因此必须实现最小 evaluation-only clinical semantics，才能产生 Gate C execution evidence。

---

## 2. Proposed Authorization Name

```text
Evaluation-Only Clinical Implementation Authorization
```

建议授权对象：

```text
AUTH-U03-GATEC-EVAL-IMPL-001
```

目的仅为：

```text
enable governed offline execution
of ER-U03-RISK-001@0.1.0-candidate
```

---

## 3. Authorized Scope — Proposed

### 3.1 Layer A — Evaluation Infrastructure

允许实现：

```text
EvalSet loader
Fixture resolver
exact release pinning / validation
case dispatcher
sandbox / in-memory execution context
expected-vs-actual comparator
must_not_output checker
must_not_commit checker
critical-blocking aggregator
result bundle writer
trace/provenance recorder
```

### 3.2 Layer B — Evaluation-only Clinical Executable Semantics

允许把当前冻结 candidate **机械实现**为 evaluation-only code：

```text
15 C rule predicates / thresholds
C missingness semantics
C specialized-family scope semantics
whole-policy scope-entry semantics
coverage 5+2 denominator semantics
D09 P0..P5 branch predicates
P0 > P1 > P2 > P3 > P4 > P5 precedence
D09 disposition / reason-code mapping
```

唯一 clinical authority 来源：

```text
KR-U03-SOURCE-001@0.1.0-candidate
RR-U03-RISK-001@0.2.1-candidate
U03_D09_COVERAGE_V0_2_1_CANDIDATE
PR-U03-D09-001@0.2.1-candidate
PF-U03-C-POLICY-001
```

禁止开发者新增、修改、解释扩展 clinical semantics。

---

## 4. Explicit Exclusions

本授权若未来被授予，也**不得**覆盖：

```text
production/runtime C02 adapter wiring
production/runtime D09 wiring
real patient traffic
production database writes
Clinical State production commit
U04 Safety Gate routing
U14 routing
external API exposure
release publication / activation
P06 production rollout
legacy physical deletion
China localized policy
pediatrics
pregnancy/puerperium clinical pathway
```

必须保持：

```text
Evaluation-only implementation
!= runtime implementation
!= production implementation
```

---

## 5. Required Isolation Controls

实现必须满足：

```text
no production DB credentials
no production message bus
no external patient request entry
no production release registry activation
no real patient identifiers
no mutable latest release lookup
no network clinical retrieval as implicit dependency
```

允许：

```text
in-memory state
fixture-only synthetic evaluation state
read-only frozen candidate refs
local / CI offline execution
```

---

## 6. Required Engineering Contracts

建议新增 evaluation-only contracts：

```text
U03EvaluationCase
U03EvaluationFixture
U03EvaluationExecutionContext
U03EvaluationClinicalEvaluator
U03EvaluationCaseResult
U03EvaluationRunResult
```

Clinical evaluator interface 只能：

```text
fixture + exact governed refs
→ rule evaluation result
→ D09 decision result
```

不得：

```text
commit production state
invoke U04
publish clinical release
change expected outcome
```

---

## 7. Implementation Acceptance Criteria

授权后的实现至少需要独立验证：

```text
A1 exact candidate refs pinned
A2 15 C rules mechanically traceable to frozen rule IDs
A3 C missingness/scope behavior traceable
A4 D09 P0..P5 mechanically traceable to frozen policy IDs
A5 precedence deterministic and file-order independent
A6 no SAFE/NORMAL invented vocabulary
A7 no production state mutation path
A8 no mutable latest alias
A9 result bundle captures actual/expected/assertions
A10 critical safety failure blocks Gate C
```

必须有：

```text
implementation review
+
independent evaluation review
```

开发者不得自行调整 expected case 以匹配实现。

---

## 8. Legacy / Runtime Impact Assessment

```text
production runtime wiring impact = NONE
legacy path retirement = NONE
existing frozen candidate mutation = NONE
DB migration = NONE
public API change = NONE
```

因此这是可隔离的 narrow implementation slice。

但因为它实现真实 clinical decision semantics：

```text
Implementation Authorization = REQUIRED
```

---

## 9. Authorization State

当前：

```text
AUTH-U03-GATEC-EVAL-IMPL-001
= PROPOSED
= READINESS_COMPLETE
= NOT_AUTHORIZED
```

在用户显式授权前：

```text
no clinical evaluator code
no 15-rule implementation
no D09 policy executable implementation
no governed execution start
```

允许继续做：

```text
interface/spec design
test-plan design
review checklist
non-clinical harness scaffolding design
```

---

## 10. Exact Authorization Effect

若用户明确给出对本对象的 `Implementation Authorization`，授权效果仅为：

```text
AUTH-U03-GATEC-EVAL-IMPL-001 = AUTHORIZED
```

随后允许：

```text
implement isolated evaluation harness
implement evaluation-only executable C/D semantics
independent implementation review
execute ER-U03-RISK-001@0.1.0-candidate
produce Gate C execution evidence
```

仍然禁止：

```text
runtime/production wiring
CD-07 runtime implementation
U04 implementation
production activation
```

---

## 11. Current Verdict

```text
Evaluation-only Implementation Readiness = PASS
Implementation Authorization = REQUIRED / NOT_GRANTED
BF-CD06-EXEC-01 = OPEN
Evaluation Execution = BLOCKED_BEFORE_START
Gate C = NOT_PASSED
```
