# U03 Gate C Governed Evaluation Re-Execution Evidence Freeze v0.1

> Run: `35077669669`  
> Executed SHA: `66a10209b9e98d49d49eae1f472d15110bddc4df`  
> Artifact ID: `10439131250`  
> Artifact: `u03-gatec-governed-evidence-35077669669-attempt-1`  
> Artifact SHA-256: `c0649153d40610e685bc80940c25b75a2616698e439e913d6da5d0ebaf56e4f3`  
> Status: `EVIDENCE_FROZEN / CASE_LEVEL_BUNDLE_VERIFIED / NOT_RUNTIME_AUTHORIZATION / NOT_PRODUCTION_AUTHORIZATION`

## 1. Execution identity

```text
execution_kind = GITHUB_ACTIONS_WORKFLOW_DISPATCH
run_id = 35077669669
run_attempt = 1
repository = cxjchelsea/AIdoctor
ref = refs/heads/impl/u03-gatec-eval-only
ref_name = impl/u03-gatec-eval-only
commit_sha = 66a10209b9e98d49d49eae1f472d15110bddc4df
event_name = workflow_dispatch
workflow = U03 Gate C Governed Evaluation
job = governed-offline-evaluation
review_confirmation = REVIEW_COMPLETE
gate_c_decision_embedded = false
```

The GitHub Actions job completed successfully. Self-tests, governed evaluation execution, provenance binding, bundle verification, artifact upload, and durable evidence identity reporting all completed with `success`.

## 2. Durable artifact identity

```text
artifact_id = 10439131250
artifact_name = u03-gatec-governed-evidence-35077669669-attempt-1
artifact_size = 5877 bytes
artifact_expired = false
artifact_digest = sha256:c0649153d40610e685bc80940c25b75a2616698e439e913d6da5d0ebaf56e4f3
artifact_workflow_run = 35077669669
artifact_head_sha = 66a10209b9e98d49d49eae1f472d15110bddc4df
```

The downloaded ZIP SHA-256 was independently recomputed and equals the GitHub artifact digest exactly:

```text
c0649153d40610e685bc80940c25b75a2616698e439e913d6da5d0ebaf56e4f3
```

The archive contains exactly one governed evidence file:

```text
result-bundle.json
```

## 3. Bound governed set

```text
bound_evalset_release_ref = ER-U03-RISK-001@0.1.0-candidate
knowledge_release_ref = KR-U03-SOURCE-001@0.1.0-candidate
rule_release_ref = RR-U03-RISK-001@0.2.1-candidate
coverage_contract_ref = U03_D09_COVERAGE_V0_2_1_CANDIDATE
policy_release_ref = PR-U03-D09-001@0.2.1-candidate
policy_pair_ref = PF-U03-C-POLICY-001
harness_version = u03-gatec-eval-harness-0.1.1-ir-remediation
```

Every executable Golden result is bound to the same EvalSet and governed release refs.

## 4. Case-level execution evidence

```text
approved Golden identities = 31
executable Golden cases = 30
golden PASS = 30
golden FAIL = 0
excluded Golden = GC-026

approved critical Safety identities = 20
executable critical Safety cases = 19
critical Safety PASS = 19
critical Safety FAIL = 0
excluded Safety = SS-012

failed_golden = []
failed_critical_safety = []
failed_non_case_checks = []
gate_c_execution_blocked_by_critical_failure = false
```

The bundle contains full `golden_case_results` and `safety_case_results`, not only summary counters. Golden records include case identity/version, fixture ref, expected result, actual result, rule results/evidence refs, forbidden-output assertions, replay evidence where applicable, and errors. Safety records include case identity, critical-blocking classification, scenario, executable evidence, status, and errors.

Excluded identities remain explicit and non-executable:

```text
GC-026 = UNPRODUCIBLE_UNDER_SHARED_SCOPE / executable_counted=false
SS-012 = UNPRODUCIBLE_UNDER_SHARED_SCOPE / executable_counted=false
```

## 5. Non-case and isolation evidence

```text
shared_scope_invariant_evidence.status = PASS
d09_p5_defensive_contract_boundary.status = PASS
d09_p5_defensive_contract_boundary.counts_as_governed_c_execution = false
production_state_mutation_capability = false
network_access_required = false
evidence_capture_status = CASE_LEVEL_BUNDLE_READY_FOR_ARTIFACT_UPLOAD
```

The durable bundle does not contain the stale harness-owned summary keys:

```text
governed_evaluation_execution_status
gate_c_status
```

and explicitly records:

```text
gate_c_decision_embedded = false
```

## 6. Residual note

The harness-owned top-level field remains:

```text
execution_run_id = LOCAL-DETERMINISTIC-RUN
```

This was reviewed before re-execution as non-blocking `N-ECR-02`. It is not used as the authoritative governed execution identity. The authoritative durable execution identity is `execution_provenance.run_id = 35077669669`, cross-verified against GitHub Actions run metadata and artifact metadata.

## 7. Evidence decision

```text
Governed Evaluation Re-Execution = COMPLETED
Execution Summary = PASS
Case-Level Bundle = DURABLY_CAPTURED / VERIFIED
Artifact Digest = VERIFIED
Execution Provenance = VERIFIED
Clinical Eval Executable Cases = PASS
Critical Safety Executable Cases = PASS
BF-GATEC-EVIDENCE-01 = CLOSED
Evidence Freeze = COMPLETE
```

This evidence freeze does not itself authorize Runtime, U04, CD-07, merge, release activation, real patient traffic, or production use. Gate C remains subject to a separate Gate C Re-Decision record.
