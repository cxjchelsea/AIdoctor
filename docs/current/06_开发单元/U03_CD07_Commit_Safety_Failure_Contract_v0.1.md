# U03 CD-07 Commit / Safety / Failure Contract v0.1

> 对象：CD-07 runtime 中 candidate/result/decision/proposal/commit 的责任边界，以及 stale/release/dependency/partial failure 的 fail-closed 规则。  
> 状态：`RDP-04_FROZEN / READINESS_INPUT_ONLY / NO_PRODUCTION_COMMIT_AUTHORIZATION / NOT_IMPLEMENTATION_AUTHORIZATION`。

## 1. Mutation authority

Canonical Clinical State 的受控写入边界保持不变：

```text
C02 capability/result
    != mutation authority
D09 decision
    != mutation authority
K09 typed proposal
    != committed state
P05 trace
    != committed state

P01 adapter → StateCommitter
    = controlled canonical mutation path
```

CD-07 不得引入第二条 governed state mutation path，不得通过 `CDPManager.updateCDP` 或同类 legacy convenience path 绕过 StateCommitter。

## 2. Commit preconditions

即使未来获得 CD-07 implementation authorization，任何受控 non-production proposal commit 也必须满足：

```text
C1 proposal type is explicitly supported by the governed P01 boundary
C2 exact source Clinical State Version is bound
C3 expected/current version check passes
C4 exact governed release refs are bound and consistent
C5 upstream capability/decision execution completed without blocking failure
C6 proposal provenance is traceable to Thread / Run / Event identity
C7 no prohibited production activation/traffic boundary is crossed
```

任一前置失败 → no canonical commit。

## 3. Stale/version conflict semantics

```text
source Clinical State Version stale
expected_version mismatch
concurrent canonical update detected
```

必须被表示为 version/commit conflict，并 fail closed。

禁止：

```text
retry with latest state and silently reuse old clinical result
convert stale failure into low-risk/NO_MATCH
force overwrite canonical state
```

如需 retry，必须重新绑定适用的 current state/version，并按未来实现规范重新执行需要重新执行的决策链；不得把旧 decision 当成新 state 的 Clinical Truth。

## 4. Capability and dependency failure semantics

Operational failure is not a clinical negative result.

```text
C02 FAILED != NO_MATCH
D09 dependency failure != SAFE
release resolution failure != LOW_RISK
trace failure != clinical normality
StateCommitter rejection != clinical disposition
```

Failure must remain explicit and auditable.

## 5. Partial failure boundary

CD-07 implementation must not create a partially committed clinical decision chain.

Examples:

```text
C02 success + D09 failure → no D09 proposal commit
D09 proposal created + version conflict → proposal not committed
commit success + trace persistence problem → committed state must not be rewritten from trace; operational reconciliation path required
release mismatch after binding validation failure → no decision/commit
```

The exact implementation mechanism may be designed after authorization, but the observable invariant above is frozen now.

## 6. Idempotency / replay boundary

Where an invocation/event is replayed, implementation must preserve canonical business-event identity and prevent duplicate governed commits. Replay behavior must be demonstrably distinct from recomputing against a new Clinical State Version.

No new idempotency identity may be inferred from free-form model text.

## 7. Safety semantic boundary

U03 risk/disposition output must never be normalized into a generic safety assertion such as:

```text
SAFE
NORMAL
NO_CLINICAL_RISK
```

unless such semantics are independently governed elsewhere; current U03 package does not authorize them.

U04 remains the separate Safety Gate owner. CD-07 must not pre-decide U04 or encode “U04 passed” into U03 commit state.

## 8. Non-production versus production mutation

Readiness may permit future authorization for controlled non-production StateCommitter integration, but:

```text
Production Clinical State mutation = NOT_AUTHORIZED
Real patient traffic = NOT_AUTHORIZED
Clinical Runtime Production = NOT_ENABLED
```

Test/non-production stores and identities must not be confused with production state.

## 9. Trace relationship

P05/runtime trace must record enough provenance to reconstruct:

```text
input state version
release set
C02 execution/result
D09 decision
proposal identity/type
commit attempt/result
failure/retry identity where applicable
```

But trace is evidence, not Clinical State, and trace loss cannot be converted into a clinical conclusion.

## 10. Verification hooks frozen by this contract

R5 must include explicit checks for:

```text
- StateCommitter-only mutation
- stale-version fail closed
- release mismatch fail closed
- dependency failure != clinical negative
- duplicate/replay no duplicate governed commit
- unsupported proposal no commit
- no production mutation
- U04 not executed/authorized by U03
```

## 11. Verdict

```text
RDP-04 = FROZEN / PASS_FOR_READINESS_REVIEW
StateCommitter = CONTROLLED_MUTATION_AUTHORITY
Production Clinical State commit = NOT_AUTHORIZED
CD-07 Implementation Authorization = NOT_GRANTED
```
