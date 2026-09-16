# U03 CD-07 Runtime Input / Output Contract v0.1

> 对象：CD-07 non-production runtime implementation 的输入、输出与责任边界。  
> 状态：`RDP-02_FROZEN / READINESS_INPUT_ONLY / NOT_IMPLEMENTATION_AUTHORIZATION`。

## 1. Contract chain

```text
Committed Clinical State Version
+ Thread / Run / Event identity
+ accepted evidence binding
+ exact governed release refs
+ scope context / dependency status
        ↓
C02 governed candidate execution
        ↓
C02 candidate/result
        ↓
D09 owner decision
        ↓
typed K09 proposal
        ↓
P01 adapter
        ↓
StateCommitter
```

No upstream capability output is Clinical Truth merely because it exists.

## 2. Runtime input contract

A CD-07 runtime invocation must bind:

```text
I1 exact clinical_state_version
I2 Thread identity
I3 Run identity
I4 canonical Event/business-event identity where applicable
I5 accepted-evidence binding and provenance
I6 exact knowledge/rule/coverage/policy/policy-pair refs
I7 scope context required by the frozen rule/policy package
I8 dependency execution status
I9 runtime/governance correlation refs required for traceability
```

Required input missing, stale, ambiguous, unresolvable or inconsistent must fail closed; runtime must not synthesize a clinically meaningful default.

## 3. C02 result contract

C02 output is a governed capability/candidate result, not canonical Clinical Truth.

It must preserve at least:

```text
- execution/result status
- exact input Clinical State Version
- evidence/provenance refs
- release refs used
- rule/result identities required by D09
- scope / insufficiency / mismatch semantics from the frozen contract
- explicit FAILED semantics
- Thread / Run / Event correlation
```

Forbidden transformations:

```text
FAILED → NO_MATCH
UNKNOWN → NO
UNMEASURED → NORMAL
model inference → patient-reported fact
candidate result → direct state mutation
```

## 4. D09 input and output contract

D09 consumes only an accepted, internally consistent C02 result set plus exact governed policy bindings. D09 must not reinterpret medical evidence or recalculate clinical thresholds owned by the frozen C rule layer.

D09 output must remain within the frozen disposition/reason-code contract and may produce a typed proposal suitable for the existing K09/P01 boundary.

```text
C02 result != D09 decision
D09 decision != K09 proposal
K09 proposal != committed Clinical State
```

## 5. Commit-boundary output

The runtime unit may emit a typed proposal toward P01, but canonical mutation remains controlled by StateCommitter.

CD-07 code must not add a second mutation authority or bypass expected-version checks.

## 6. Failure contract

The following are operational/governance failures, not negative clinical findings:

```text
- stale Clinical State Version
- release mismatch
- unresolved required release
- invalid accepted-evidence binding
- dependency execution failure
- malformed candidate/result payload
- unsupported proposal type
- StateCommitter conflict/version rejection
```

They must remain distinguishable in trace/evidence from genuine frozen-rule `NO_MATCH`, `INPUT_INSUFFICIENT`, or `SCOPE_MISMATCH` outcomes.

## 7. Trace boundary

P05/runtime trace records execution facts and provenance. It cannot create or override Clinical Truth.

```text
Trace failure != clinical negative result
Trace content != canonical Clinical State
```

## 8. U04 boundary

This contract may produce the structured outbound material defined by R6, but it does not authorize U04 owner execution or routing activation.

## 9. Verdict

```text
RDP-02 = FROZEN / PASS_FOR_READINESS_REVIEW
CD-07 Implementation Authorization = NOT_GRANTED
Production Clinical State mutation = NOT_AUTHORIZED
```
