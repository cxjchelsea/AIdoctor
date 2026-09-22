# U03 CD-08 Execution Result v0.1

> Authorization: AUTH-U03-CD08-CLINICAL-VALIDATION-EXEC-001
> Scope: NON_PRODUCTION_POST_IMPLEMENTATION_CLINICAL_VALIDATION_ONLY
> This record freezes the authorized execution result. It does not authorize remediation, U04, production, or real-patient traffic.

## 1. Exact execution identity

Implementation SHA:

6d32d8d131c61561c704dc8ace643d766c08b583

Required aggregate runtime base:

9071ea14b310c0e300299b2569c4919be2b669db

GitHub Actions run:

35310181093

Retained artifact:

artifact_id = 10533321494
name = u03-cd08-clinical-validation-35310181093-attempt-1
digest = sha256:374999caf04a9017c7a90d92d728b9a9716d51034db166f203d343a2139cfdda

## 2. Frozen population

Golden executable = 30
Critical Safety executable = 19

Excluded governed identities remain:

GC-026 = UNPRODUCIBLE_UNDER_SHARED_SCOPE
SS-012 = UNPRODUCIBLE_UNDER_SHARED_SCOPE

Frozen Gate-C baseline remained unchanged and passed:

Golden 30 / 30 PASS
Critical Safety 19 / 19 PASS
failed_non_case_checks = []

## 3. Runtime execution result

CD-08 runtime result:

Golden = 28 / 30 PASS
Critical Safety = 19 / 19 PASS
Total = 47 / 49 PASS

Failed IDs:

GC-024
GC-025

Engineering checks:

compile = PASS
structural / fail-closed focused regression = PASS
full diagnosis-service regression = PASS

Hard execution boundaries:

production mutation = false
real-patient traffic = false
release activation = false
U04 execution = false
U14 execution = false

## 4. Blocking finding BF-CD08-03

Finding ID:

BF-CD08-03 = STALE_INPUT_TYPED_FAILURE_PARITY_MISMATCH

Frozen expected behavior for GC-024:

result_status = FAILED
disposition = NONE
reason_code = STALE_INPUT

Observed real runtime behavior:

boundary = EXECUTION_CONTEXT_VERSION_GUARD
runtime exception = IllegalStateException
message = accepted evidence is not bound to the execution Clinical State Version
typed result_status = not produced
typed disposition = not produced
observed harness reason = RUNTIME_VERSION_GUARD_REJECTION

Interpretation:

The runtime correctly fails closed before C02 for stale evidence/state identity, so the safety property is preserved. However, the frozen Gate-C contract expects a typed governed failure result with reason STALE_INPUT. The current runtime rejects structurally instead of materializing that frozen typed outcome.

Verdict:

BF-CD08-03 = OPEN / BLOCKING

This mismatch must not be closed by changing the frozen expected result.

## 5. Blocking finding BF-CD08-04

Finding ID:

BF-CD08-04 = RELEASE_MISMATCH_TYPED_FAILURE_PARITY_MISMATCH

Frozen expected behavior for GC-025:

result_status = FAILED
disposition = NONE
reason_code = RELEASE_MISMATCH

Observed real runtime behavior:

boundary = EXACT_RELEASE_GUARD
runtime exception = IllegalStateException
message = ruleReleaseRef is not authorized for CD-07 non-production runtime
typed result_status = not produced
typed disposition = not produced
observed harness reason = RUNTIME_RELEASE_GUARD_REJECTION

Interpretation:

The runtime correctly rejects the non-frozen release before clinical execution, so wrong-release fail-closed behavior is preserved. However, the frozen Gate-C contract expects a typed governed failure result with reason RELEASE_MISMATCH. The current runtime rejects structurally instead of materializing that frozen typed outcome.

Verdict:

BF-CD08-04 = OPEN / BLOCKING

This mismatch must not be closed by weakening exact-release guards or changing the frozen expected result.

## 6. Evidence checksums

From the retained artifact:

evidence.json
sha256 = 73e6b3c4e65bb243fc5e3008d8cbb4c36126573765794501f391dec9eaa677ae

frozen-cases.json
sha256 = c2ef36bf550296d0f4056d231584d77b5f7ffafe5efb16da6140a2d0264a54dd

workflow-provenance.txt
sha256 = cf065fb447a1c16037bbaeb24a0e4ca3f3eba6e08330e085add919ea5cb86f9a

java-runtime-results.json
sha256 = cd4f0f8309f6ff63120562c560da4a840a44861a05c93757ed30e35685ec6183

Gate-C result-bundle.json
sha256 = 77090410602fad6014248de3db926d700cea317909ebe17b1240352ed413c764

## 7. Governance verdict

CD-08 Execution Authorization = AUTHORIZED / CONSUMED_BY_EXECUTION

CD-08 Execution = COMPLETE

CD-08 Clinical Validation = FAIL / BLOCKED

BF-CD08-03 = OPEN / BLOCKING
BF-CD08-04 = OPEN / BLOCKING

U03 Clinical Dependency Closure = NOT_COMPLETE

U04 Readiness Re-review = BLOCKED_PENDING_CD08

U04 Implementation Authorization = NOT_GRANTED

Clinical Runtime Production = NOT_ENABLED

Production Authorization = BLOCKED

Real-patient traffic = NOT_AUTHORIZED

## 8. Next permitted work

The next work is a separately governed remediation review for BF-CD08-03 and BF-CD08-04.

A remediation may normalize the already-detected stale/release guard failures into the frozen typed governed failure contract, but it may not:

- weaken stale-state or exact-release guards;
- move those failures later merely to make tests pass;
- change STALE_INPUT or RELEASE_MISMATCH expected semantics;
- introduce new clinical truth;
- authorize U04 or production.

After separately authorized remediation and focused verification, CD-08 must be re-executed on the same frozen 30 + 19 population. The previous 47 passing cases do not waive re-execution.
