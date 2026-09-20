# U03 CD-07 Runtime Verification & Evidence Plan v0.1

> 对象：CD-07 获得未来实现授权后，runtime implementation 必须如何验证、留证、独立审查。  
> 状态：`RDP-05_FROZEN / VERIFICATION_PLAN_ONLY / NOT_IMPLEMENTATION_AUTHORIZATION / NOT_PRODUCTION_AUTHORIZATION`。

## 1. Purpose

Gate C 已证明冻结 clinical semantics 在 authorized evaluation-only harness 下成立，但它不是 runtime implementation evidence。

```text
Gate C PASS
!= runtime wiring verified
!= runtime E2E PASS
!= production authorized
```

本计划预先冻结“实现完成后什么才算 verified”，防止实现者在代码完成后自行降低验收标准。

## 2. Required verification layers

### V1 Contract/unit verification

至少覆盖：

```text
V1.1 exact Clinical State Version binding
V1.2 Thread / Run / Event identity binding
V1.3 accepted-evidence/provenance preservation
V1.4 exact governed release ref binding
V1.5 C02 result schema/status semantics
V1.6 D09 input/output contract
V1.7 typed proposal boundary
V1.8 unsupported/malformed input fail closed
```

### V2 Governance/release verification

必须证明：

```text
- missing ref fails closed
- unresolved ref fails closed
- incompatible release set fails closed
- policy pair mismatch fails closed
- alias/latest fallback is impossible or rejected
- candidate binding remains explicit non-production only
- no publication or production activation occurs
```

### V3 Mutation authority verification

必须证明：

```text
- C02 cannot directly mutate canonical state
- D09 cannot directly mutate canonical state
- proposal != commit
- P01 → StateCommitter is the governed mutation route
- stale expected version rejects commit
- unsupported proposal rejects commit
- replay does not create duplicate governed commit
- legacy direct-update bypass is not used
```

### V4 Failure semantics verification

Must demonstrate:

```text
FAILED != NO_MATCH
release failure != low risk
stale version != clinical negative
trace failure != Clinical Truth
partial execution does not create partial clinical commit
```

### V5 Trace/provenance verification

Trace must correlate at least:

```text
Clinical State Version
Thread / Run / Event identity
exact release refs
C02 invocation/result
D09 decision
proposal identity/type
commit attempt/result
failure/retry/replay identity where applicable
```

Trace assertions must also prove `Trace != Clinical State`.

### V6 U03→U04 boundary verification

Before any U04 implementation is authorized, CD-07 must at least prove that its outbound producer conforms to R6 and does not execute/pretend to execute U04.

### V7 Gate C semantic regression

Runtime implementation must be regression-checked against the frozen Gate-C semantics/package so wiring changes do not alter clinical meaning.

This regression is a compatibility check, not a replacement for the already-frozen Gate C evidence.

### V8 Non-production runtime integration / E2E

After implementation, a controlled non-production E2E must verify the actual runtime chain authorized for CD-07.

It must be clearly labeled:

```text
NON_PRODUCTION_RUNTIME_E2E
```

and must not use real patient traffic or production state mutation.

## 3. Mandatory negative tests

At minimum include cases for:

```text
N1 stale Clinical State Version
N2 missing release ref
N3 wrong release ref
N4 cross-release incompatible set
N5 dependency failure
N6 malformed C02 result
N7 unsupported proposal type
N8 duplicate/replayed event
N9 StateCommitter version conflict
N10 attempted direct mutation bypass
N11 attempted implicit/latest release selection
N12 attempted production environment/traffic use
N13 attempted U04 owner execution without authorization
```

Every negative test must assert both the failure and the absence of prohibited clinical commit/output.

## 4. Evidence package requirements

Implementation verification evidence must freeze at least:

```text
E1 exact implementation commit SHA
E2 exact test/evaluation code SHA
E3 runtime/environment identity
E4 exact governed release refs
E5 command/workflow invocation provenance
E6 test inventory and results
E7 negative-test results
E8 non-production E2E result
E9 trace/provenance samples or machine-verifiable summaries
E10 confirmation of no production state mutation / real-patient traffic
E11 known exclusions and residual risks
```

Evidence must be durable and reviewable; ephemeral runner output alone is insufficient if it cannot later be independently inspected.

## 5. Independent review sequence

Future CD-07 implementation must follow:

```text
Implementation Authorization
→ implementation
→ implementation verification
→ durable evidence freeze
→ independent implementation/evidence review
→ explicit Merge Authorization review
→ Repository Owner explicit Merge Authorization
→ standard merge commit
→ PMV
```

Passing tests cannot self-authorize merge.

## 6. Acceptance rule

CD-07 implementation may later be described as runtime-verified only if all authorized-scope required verification layers pass and no blocking finding remains.

Forbidden equivalences:

```text
component tests PASS != runtime E2E PASS
runtime E2E PASS != clinical evaluation PASS
clinical evaluation PASS != production authorization
code merged != production enabled
```

## 7. Verdict

```text
RDP-05 = FROZEN / PASS_FOR_READINESS_REVIEW
Runtime Verification Plan = DEFINED
Verification Execution = NOT_STARTED
CD-07 Implementation Authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
```
