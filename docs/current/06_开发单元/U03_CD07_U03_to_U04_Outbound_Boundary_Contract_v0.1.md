# U03 CD-07 U03→U04 Outbound Boundary Contract v0.1

> 对象：CD-07 为未来 U04 Safety Gate 提供的 outbound contract；只冻结接口边界，不授权 U04 实现或路由。  
> 状态：`RDP-06_FROZEN / INTERFACE_BOUNDARY_ONLY / NOT_U04_AUTHORIZATION / NOT_IMPLEMENTATION_AUTHORIZATION`。

## 1. Ownership boundary

```text
U03 = current-version risk/disposition producer within its governed scope
U04 = separate Safety Gate owner
```

CD-07 may prepare a typed outbound boundary that U04 could later consume. It must not execute U04, emulate U04, pre-compute U04 PASS, or encode U04 safety truth into U03 state.

```text
U03 result != U04 safety decision
handoff contract exists != U04 routing authorized
```

## 2. Outbound source of truth

The outbound boundary must be derived from the governed U03 execution/committed state boundary, not from free-form model text or an uncommitted convenience result.

Where a downstream handoff requires canonical-state facts, the payload must bind the exact committed/current Clinical State Version relevant to that handoff.

## 3. Minimum outbound contract

The producer must make available, as applicable to the existing governed types, structured references for:

```text
O1 source/current Clinical State Version ref
O2 Thread / Run / Event correlation refs
O3 U03 execution/result identity
O4 U03 governed disposition/risk-related structured result or committed-state ref
O5 exact knowledge/rule/coverage/policy/policy-pair refs
O6 evidence/provenance refs required to audit the U03 result
O7 result/execution status
O8 insufficiency/scope/failure information required to avoid false downstream safety assumptions
O9 proposal/commit identity where canonical mutation occurred
O10 trace correlation ref
```

This document intentionally does not invent new implementation class names or clinical fields beyond governed concepts already present in U03.

## 4. Forbidden outbound shortcuts

CD-07 must not reduce U03 output to an ambiguous convenience flag such as:

```text
safe = true
normal = true
risk = none
u04_passed = true
continue_without_safety_gate = true
```

unless a future separately governed contract explicitly defines such semantics. Current U03 does not.

Likewise, free-form generated explanation cannot replace typed/provenance-bound handoff data.

## 5. Failure / insufficient / scope behavior

The outbound boundary must preserve distinctions relevant to downstream safety handling:

```text
FAILED
INPUT_INSUFFICIENT
SCOPE_MISMATCH
NO_MATCH
MATCHED
```

or their already-governed runtime equivalents. A technical/dependency failure must not be flattened into a low-risk or safe handoff.

If the required outbound contract cannot be constructed because version/release/provenance bindings are missing or stale, handoff must fail closed rather than silently route around U04.

## 6. Version and release consistency

An outbound payload must identify the exact U03 state/result and release set from which it was produced.

Forbidden:

```text
- handoff from stale U03 state while claiming current
- replacing exact refs with latest aliases
- mixing U03 results from different Clinical State Versions
- mixing incompatible governed release sets
```

## 7. Routing boundary

CD-07 readiness covers only the producer/interface boundary needed to keep U03 compatible with a future U04.

Not authorized here:

```text
- U04 implementation
- U04 policy/rules
- U04 Safety Gate decision
- production U03→U04 routing
- U14 routing
- fallback around U04
- real patient traffic
```

A later U04 readiness package must independently define the consumer-side contract and safety-owner behavior.

## 8. Commit relationship

If the handoff references canonical Clinical State, it must reference state established through the governed P01/StateCommitter mutation path. An uncommitted K09 proposal must not be mislabeled as committed clinical state.

```text
proposal ref != committed state ref
```

## 9. Verification obligations

R5 runtime verification must later demonstrate:

```text
- outbound payload binds exact state/result/version/release refs
- failure/insufficient/scope distinctions are preserved
- no generic safe/normal truth is invented
- no U04 owner logic executes
- no unauthorized U04/U14 routing occurs
- stale/malformed outbound handoff fails closed
```

## 10. Verdict

```text
RDP-06 = FROZEN / PASS_FOR_READINESS_REVIEW
U03→U04 Outbound Contract = INTERFACE_BOUNDARY_DEFINED
U04 Implementation Readiness = NOT_READY
U04 Implementation Authorization = NOT_GRANTED
CD-07 Implementation Authorization = NOT_GRANTED
```
