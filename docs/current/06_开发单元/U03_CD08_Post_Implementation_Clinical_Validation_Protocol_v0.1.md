# U03 CD-08 Post-Implementation Clinical Validation Protocol v0.1

> 对象：真实 CD-07 non-production runtime 的 post-implementation clinical validation protocol。  
> 状态：`PROTOCOL_FROZEN_FOR_READINESS / NOT_EXECUTION_AUTHORIZATION / NOT_PRODUCTION_AUTHORIZATION`。

## 1. Validation question

CD-08 的唯一核心问题：

> 在完全相同的 governed clinical release set 与 approved EvalSet 下，真实 CD-07 runtime 是否产生与 Gate C 冻结语义一致、可追踪、无安全退化的临床结果？

## 2. Required execution modes

### P8-01 Golden runtime validation

对全部 30 个 executable Golden cases：

```text
Gate-C fixture / governed input
→ actual CD-07 non-production runtime
→ C02 result
→ D09 decision
→ K09 proposal
→ P01 / StateCommitter result where applicable
→ P05 trace / finalization
```

逐 case 比较：

```text
expected governed result
vs
actual runtime clinical result
```

不得只比较 workflow success / HTTP status / schema validity。

### P8-02 Critical Safety runtime validation

对全部 19 个 executable critical Safety cases，逐 case 证明：

```text
- critical expected behavior preserved
- prohibited false-negative/safe-normal interpretation absent
- required evidence/provenance preserved
- technical failure does not become low-risk clinical output
- no unauthorized U04 decision is synthesized
```

任一 critical Safety case mismatch 默认阻断 CD-08 PASS，除非经独立临床治理重新裁定；实现者不能自行豁免。

### P8-03 Governed state/result validation

对存在 canonical state commit 的场景，必须验证：

```text
proposal semantic content
→ P01 admission
→ committed version
→ committed U03 clinical semantic state
```

同时证明：

```text
uncommitted proposal != committed Clinical State
trace != Clinical State
```

### P8-04 Failure / missingness / scope validation

至少覆盖：

```text
FAILED
INPUT_INSUFFICIENT
SCOPE_MISMATCH
NO_MATCH
MATCHED
```

或真实实现中的等价 governed vocabulary，且禁止把 dependency/runtime failure 解释成阴性、低风险或 safe/normal。

### P8-05 Replay / identity stability

对于 Gate-C package 中已有 replay/identity expectation 的 case，实际 runtime 必须保持：

```text
same governed event identity
→ no duplicate governed clinical commit
→ stable clinically relevant outcome
```

## 3. Traceability matrix

每个 executable case 至少记录：

```text
case_id / case_version
fixture_ref
expected governed outcome
actual runtime outcome
source Clinical State Version
resulting Clinical State Version if committed
Thread / Run / Event refs
C02 invocation/result ref
accepted evidence refs
D09 decision ref
K09 proposal ref
P01/commit result ref
P05 trace ref
exact release refs
comparison verdict
errors/findings
```

## 4. Acceptance criteria

Minimum acceptance candidate:

```text
Golden runtime validation = 30 / 30 PASS
Critical Safety runtime validation = 19 / 19 PASS
GC-026 = remains explicit excluded / not counted
SS-012 = remains explicit excluded / not counted
release-set identity = exact match
state-version identity = verified
provenance completeness = verified
critical forbidden-output assertions = PASS
unauthorized U04 execution = absent
production mutation / real-patient traffic = absent
blocking clinical findings = 0
```

These numerical criteria do not self-authorize PASS. A formal independent clinical/governance review must review the frozen evidence package.

## 5. Discrepancy classification

Every mismatch must be classified before remediation:

```text
D8-A implementation/wiring defect
D8-B evidence/provenance defect
D8-C state/commit defect
D8-D runtime identity/release-binding defect
D8-E evaluation adapter defect
D8-F possible clinical-semantic discrepancy
D8-G known governed exclusion
```

For `D8-F`, developers must stop semantic remediation until Medical/Clinical Owner review determines whether the runtime is wrong, the fixture is wrong, or a separately governed clinical revision is required.

## 6. No self-healing of expected truth

Forbidden during CD-08 execution:

```text
- editing Gate-C expected results to match runtime
- weakening critical Safety assertions
- dropping failed cases from denominator
- changing exact releases to a more convenient version
- substituting latest/current aliases
- inventing SAFE/NORMAL shortcuts
- marking technical errors as NO_MATCH or low risk
```

## 7. Evidence durability

The formal run must generate a durable machine-readable case-level bundle plus human-reviewable summary. Ephemeral CI logs alone are insufficient.

## 8. Review sequence

```text
CD-08 readiness PASS
→ separate CD-08 Execution Authorization
→ isolated validation implementation/execution
→ evidence freeze
→ independent clinical/governance review
→ discrepancy remediation if needed
→ re-execution if needed
→ CD-08 formal decision
→ only after PASS: U03 Clinical Dependency Closure review
```

## 9. Verdict

```text
CD-08 Validation Protocol = DEFINED / FROZEN_FOR_READINESS
CD-08 Execution = NOT_STARTED
CD-08 Decision = NOT_MADE
U03 Clinical Dependency Closure = NOT_COMPLETE
U04 Readiness = DEFERRED
```
