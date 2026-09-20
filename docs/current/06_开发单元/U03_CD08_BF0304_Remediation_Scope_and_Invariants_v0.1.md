# U03 CD-08 BF0304 Remediation Scope and Invariants v0.1

> Findings:
> BF-CD08-03 = STALE_INPUT_TYPED_FAILURE_PARITY_MISMATCH
> BF-CD08-04 = RELEASE_MISMATCH_TYPED_FAILURE_PARITY_MISMATCH
>
> This document defines remediation scope only. It does not authorize implementation, merge, U04, production, or real-patient traffic.

## 1. Frozen problem statement

CD-08 execution run 35310181093 validated implementation SHA:

6d32d8d131c61561c704dc8ace643d766c08b583

and retained artifact:

10533321494
sha256:374999caf04a9017c7a90d92d728b9a9716d51034db166f203d343a2139cfdda

The final real-runtime result was:

Golden = 28 / 30 PASS
Critical Safety = 19 / 19 PASS
Total = 47 / 49 PASS

Only:

GC-024
GC-025

remain blocking.

## 2. BF-CD08-03 exact mismatch

Frozen expected:

status = FAILED
disposition = NONE
reason_code = STALE_INPUT

Current runtime:

U03NonProductionExecutionContext constructor / accepted-evidence version guard
-> throws IllegalStateException
-> no typed governed failure result.

Safety behavior is already fail-closed. The remediation target is typed failure-contract parity only.

## 3. BF-CD08-04 exact mismatch

Frozen expected:

status = FAILED
disposition = NONE
reason_code = RELEASE_MISMATCH

Current runtime:

U03ExplicitNonProductionReleaseRefs / U03NonProductionExecutionContext /
U03ExactNonProductionReleaseResolver exact-release guard
-> throws IllegalStateException or IllegalArgumentException
-> no typed governed failure result.

Safety behavior is already fail-closed. The remediation target is typed failure-contract parity only.

## 4. Owner boundary

These findings are P0 / pre-C02 admission findings.

They are NOT:

- C02 clinical rule defects;
- D09 policy defects;
- K09/P01 defects;
- U04 defects;
- production release defects.

Correct owner boundary:

NON_PRODUCTION_EXECUTION_ADMISSION / EARLY_GOVERNANCE_GUARD_NORMALIZATION

The remediation must preserve:

early rejection
before C02
before D09
before K09
before P01
before StateCommitter
before S14
before any U04 surface.

## 5. Authorized design shape if separately approved

A later implementation MAY introduce a narrow typed admission outcome or equivalent typed guard-failure carrier.

Minimum result fields:

status = FAILED
disposition = NONE
reason_code = STALE_INPUT | RELEASE_MISMATCH
boundary = PRE_C02_ADMISSION
clinical_state_version / execution identity where valid to retain
attempted exact release refs where safe to retain
no normal clinical disposition
no invented policy decision
no clinical mutation.

The implementation MAY use narrowly typed exception classes internally if useful, but only if they preserve exact cause classification.

## 6. Defense-in-depth invariants

Existing constructor / exact-release guard behavior must remain fail-closed when bypassing the new admission boundary.

Therefore remediation MUST NOT:

- remove U03NonProductionExecutionContext version validation;
- weaken requireAcceptedEvidenceBinding();
- weaken U03ExplicitNonProductionReleaseRefs exact-ref validation;
- weaken requireGateCFrozenSet();
- allow latest/current/newest aliases;
- allow draft or wrong release refs;
- defer stale/release rejection until C02 or D09;
- convert unrelated IllegalStateException / IllegalArgumentException into STALE_INPUT or RELEASE_MISMATCH;
- suppress unexpected runtime exceptions;
- produce a normal VALID disposition for either finding.

## 7. Exact classification rules

STALE_INPUT may be emitted only when the accepted evidence Clinical State Version is not the exact execution Clinical State Version, or an equivalent explicitly versioned stale-state condition already defined by the frozen contract.

RELEASE_MISMATCH may be emitted only when the attempted governed release tuple is not the exact frozen authorized tuple, including wrong-version / unapproved exact refs. Alias rejection remains a hard guard and may be represented as RELEASE_MISMATCH only if the implementation keeps alias rejection equally strict and records the rejected attempted ref.

No other failure class may be silently collapsed into these two reason codes.

## 8. No clinical semantics change

The remediation must not change:

- any of the 15 C02 rules;
- any threshold;
- any evidence-state semantics;
- D09 P1-P4 ordering;
- any frozen Golden/Safety expected outcome;
- the frozen governed release tuple;
- population/scope semantics;
- pediatric/pregnancy/regional production behavior.

This is a runtime contract-parity remediation, not a clinical-content change.

## 9. Required verification

Focused verification must prove at minimum:

1. GC-024 produces typed FAILED / NONE / STALE_INPUT.
2. GC-024 does not invoke C02, D09, K09, P01, StateCommitter, S14, U04.
3. GC-025 produces typed FAILED / NONE / RELEASE_MISMATCH.
4. GC-025 does not invoke C02, D09, K09, P01, StateCommitter, S14, U04.
5. the original low-level guards still throw/fail closed when directly bypassed.
6. wrong release / stale input cannot become VALID through the new boundary.
7. unrelated exceptions are not misclassified.
8. existing N1-N13 fail-closed tests remain PASS.
9. CD-07R regression remains PASS.
10. full CD-08 30 + 19 re-execution is performed after remediation.

## 10. Governance boundary

This scope definition does not grant remediation implementation authorization.

Current state:

BF-CD08-03 = OPEN / BLOCKING
BF-CD08-04 = OPEN / BLOCKING
CD-08 Clinical Validation = FAIL / BLOCKED
U03 Clinical Dependency Closure = NOT_COMPLETE
U04 Implementation Authorization = NOT_GRANTED
Clinical Runtime Production = NOT_ENABLED
Production Authorization = BLOCKED
