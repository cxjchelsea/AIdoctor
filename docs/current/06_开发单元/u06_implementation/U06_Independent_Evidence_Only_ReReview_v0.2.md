# U06 Independent Evidence-Only Re-Review v0.2

**Review verdict:** REVISE_REQUIRED  
**Review type:** Independent evidence-only re-review  
**Implementation semantics are not re-reviewed here.**  
**Reviewed fresh run:** `35961775811`  
**Reviewed authoritative job:** `107511923027`  
**Reviewed implementation semantic SHA:** `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
**Reviewed PR merge-ref / verifier SHA:** `d4407493bf144424b99c0cab9d4e5e470b202913`  
**Reviewed implementation/evidence branch head:** `12efdd491e4d0bb14840132be8e3e72ff75ab518`  
**Authority core digest:** `038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22`

Primary evidence artifact:

~~~text
artifact_id
= 10792431755

artifact_name
= u06-rdp06-authoritative-d4407493bf144424b99c0cab9d4e5e470b202913

GitHub SHA-256
= 90505430e6873346a27003e8ba1e4098b7283db7809bce209c26c66ecda844c8

retention
= 90 days

expires_at
= 2026-12-23
~~~

Upload receipt artifact:

~~~text
artifact_id
= 10791919153

GitHub SHA-256
= 49dd5d31feb787c9a7dc3b933177028a35abdc9944a3e794c408e0f962cead93

retention
= 90 days
~~~

---

## 1. Review boundary

This re-review evaluates only whether the remediated durable evidence package can now be independently accepted under frozen U06-RDP-06.

It does not:

- reopen the exact-head implementation review at `66fa3c0...`;
- change the implementation semantic freeze;
- grant Combined Implementation / Evidence Review;
- grant Implementation Verification Closure;
- grant Merge Authorization;
- authorize PROFILE-A, production Clinical Runtime, real PHI, real C03/D04, external delivery, live U07, production Scheduler routing, release activation, or real-patient traffic.

---

# 2. Closure of prior evidence-review findings

## 2.1 B-U06-EVR-01 — Oracle / Fixture exact-byte independent review provenance

**Disposition: CLOSED / PASS**

The prior review ID `5298650596` has been restored to its historical pre-refreeze bindings.

New immutable provenance-only independent reviews were created before the fresh run:

~~~text
review PR
= #247

review record commit
= 0a6cbc7d7c6f587d41da64fad969e421fa574e78

review record created
= 2026-09-24T05:45:39Z

fresh authoritative run started
= 2026-09-24T05:51:10Z
~~~

New Oracle review identity:

~~~text
U06_ORACLE_REFREEZE_PROVENANCE_REVIEW_20260924_01
~~~

New Fixture review identity:

~~~text
U06_FIXTURE_REFREEZE_PROVENANCE_REVIEW_20260924_01
~~~

The independent review record proves the refrozen Oracle / Fixture bytes are byte-for-byte equal to the prior independently reviewed assets after only the authority-core provenance substitution is normalized.

The authoritative verifier now checks:

~~~text
oracle_gate_review_id
= PASS

fixture_gate_review_id
= PASS

oracle_gate_review_record
= PASS

fixture_gate_review_record
= PASS
~~~

The current artifact retains both the new refrozen gates and the historical gates.

---

## 2.2 B-U06-EVR-02 — complete checksum coverage

**Disposition: CLOSED / PASS**

Independent recomputation of the downloaded primary artifact:

~~~text
outer ZIP SHA-256
= 90505430e6873346a27003e8ba1e4098b7283db7809bce209c26c66ecda844c8
= exact GitHub metadata digest
~~~

Primary bundle:

~~~text
total retained files
= 21

SHA256SUMS-covered files
= 20

SHA256SUMS verification
= 20 / 20 OK
~~~

The sole non-self-hashed file is `SHA256SUMS` itself.

Previously omitted files are now covered:

~~~text
environment-input.json
= HASH COVERED / PASS

external-call-spy.json
= HASH COVERED / PASS
~~~

The receipt artifact also recomputes exactly and its own checksum file passes.

---

## 2.3 B-U06-EVR-03 — durable provenance source artifacts missing from bundle

**Disposition: CLOSED / PASS**

The primary artifact is now self-contained for the RDP-06 required evidence semantics.

It retains exact bytes for:

- contract manifest;
- verification Oracle;
- Fixture manifest;
- auth profile;
- refrozen Oracle review gate;
- refrozen Fixture review gate;
- historical Oracle gate;
- historical Fixture gate;
- authorized shared-runtime observed-change manifest;
- workflow snapshot;
- verifier source snapshot;
- runtime SUT observations;
- raw external-call spy;
- environment input/evidence;
- regression summary;
- case evidence;
- verification summary;
- run/toolchain metadata;
- artifact manifest;
- SHA256SUMS.

Independent Git blob checks prove the retained authority/provenance files are exact bytes from the `d440...` verification checkout.

---

## 2.4 RF-U06-EVR-01 — durable run / job / artifact / toolchain metadata

**Disposition: CLOSED / PASS**

The primary artifact retains:

~~~text
workflow_run_id
= 35961775811

job_id
= 107511923027

job_name
= u06-rdp06-authoritative

workflow_ref
= refs/pull/243/merge

github_sha / verifier_sha
= d4407493bf144424b99c0cab9d4e5e470b202913

implementation_semantic_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661

runner_os
= Linux

runner_arch
= X64

image_os
= ubuntu24

Java
= Temurin 1.8.0_504

Maven
= 3.9.16

Python
= 3.12.3

Docker
= 28.0.4

authoritative container
= maven:3.9.9-eclipse-temurin-8
~~~

The separate receipt artifact independently binds:

~~~text
workflow_run_id
= 35961775811

primary artifact_id
= 10792431755

primary artifact_name
= u06-rdp06-authoritative-d4407493bf144424b99c0cab9d4e5e470b202913

primary artifact_digest
= 90505430e6873346a27003e8ba1e4098b7283db7809bce209c26c66ecda844c8
~~~

Those fields exactly match GitHub artifact metadata.

---

## 2.5 RF-U06-EVR-02 — retention

**Disposition: CLOSED / PASS**

Both the primary artifact and receipt artifact are retained for 90 days:

~~~text
created
= 2026-09-24

expires
= 2026-12-23
~~~

This satisfies the frozen RDP-06 minimum policy.

The accepted compact repository snapshot requirement is not yet due because independent evidence acceptance has not yet passed.

---

# 3. Positive authoritative evidence findings

## 3.1 Artifact manifest integrity

~~~text
artifact-manifest declared file_count
= 19

actual primary files excluding artifact-manifest and SHA256SUMS
= 19

missing manifest entries
= 0

extra manifest entries
= 0

hash/size mismatches
= 0
~~~

`SHA256SUMS` additionally covers the artifact manifest itself.

## 3.2 Exact authority binding

All 17 authority entries in `u06-contract-manifest.json` were independently checked against the `d440...` checkout.

~~~text
authority entries checked
= 17

Git blob mismatches
= 0
~~~

This includes Unit Spec, RDP-01..06, Aggregate, IRR physical designs, readiness, authorization records, authorization allowlist, observed-change manifest, and shared contract version.

## 3.3 Exact implementation / verifier separation

The fresh run uses PR merge-ref:

~~~text
verifier_sha
= d4407493bf144424b99c0cab9d4e5e470b202913
~~~

Its parents are:

~~~text
authorization/base
= f78bd9192d0603cfa3cc878644088f908dcb2fb8

evidence branch
= 12efdd491e4d0bb14840132be8e3e72ff75ab518
~~~

The merge-ref tree equals the evidence branch tree.

The implementation semantic target remains:

~~~text
66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

Independent diff review from `66fa3c0...` to `12efdd4...` found:

~~~text
runtime src/main changes
= 0
~~~

Only provenance/test-resource/verifier/workflow/review documentation changed after the implementation semantic freeze.

## 3.4 Required case inventory

~~~text
U06-EV
= 106 / 106 PASS

U06-CW
= 9 / 9 PASS

U06-HG
= 3 / 3 PASS

U06-VG
= 10 / 10 PASS

U06-AGG-V
= 12 / 12 PASS

IRR01
= 16 / 16 PASS

IRR02
= 16 / 16 PASS

IRR03
= 17 / 17 PASS

TOTAL
= 189 / 189 PASS

FAIL
= 0

NOT_EXECUTED
= 0

duplicate case IDs
= 0

PASS cases with mismatch payload
= 0

cases without evidence refs
= 0
~~~

Critical late EVs are all PASS:

~~~text
U06-EV-101..103
= PASS

U06-EV-104..106
= PASS
~~~

## 3.5 Runtime observation identity

~~~text
runtime observations
= 115

unique runtime observation IDs
= 115

FIXTURE_DRIVEN_SUT_OBSERVATION cases
= 115

missing observation IDs
= 0

extra observation IDs
= 0
~~~

All 115 observations record:

~~~text
observed_external_transport_count
= 0
~~~

## 3.6 Hard-boundary / isolation evidence

~~~text
network_egress_policy
= DOCKER_NETWORK_NONE

allowed_endpoints
= []

production_secret_count
= 0

real_recipient_endpoint_count
= 0

real_phi_count
= 0

production_store_write_count
= 0

real_external_delivery_count
= 0

real_model_call_count
= 0

real_tool_call_count
= 0

real_knowledge_call_count
= 0

unreviewed_shared_runtime_change_count
= 0

external_call_spy_enabled
= true

external_call_spy_observed_count
= 0
~~~

## 3.7 Regression

~~~text
tests
= 434

failures
= 0

errors
= 0

skips
= 1

unexpected_skips
= 0
~~~

The single skip is the enumerated historical U03 frozen-clinical-validation skip.

## 3.8 Required boundary cases

The following frozen coverage points are PASS:

~~~text
PROFILE-A blocked / no fallback
→ U06-EV-097, U06-VG-007, U06-AGG-V-011

synthetic scope guard
→ U06-HG-003

exact replay / replay conflict / replay identity
→ U06-EV-015, 016, 046, 047, 054, 059, 064, 065, 084, 090, 105
  + physical replay cases

no duplicate delivery/effect
→ U06-EV-028, 029, 054
  + U06-CW-01..09

cross-store wait choreography
→ U06-AGG-V-008

U07 eligibility only after AWAITING_USER
→ U06-EV-088, U06-CW-08, U06-CW-09, IRR03-V13

expectation-gap detector
→ U06-HG-001

hard-boundary detector
→ U06-HG-002

full regression gate
→ U06-VG-010
~~~

---

# 4. New blocking finding B-U06-EVRR-01 — Auth Profile contract shape does not satisfy frozen RDP-06

## 4.1 Frozen requirement

U06-RDP-06 requires a machine-readable:

~~~text
U06_VERIFICATION_AUTH_PROFILE_V0_1
~~~

with minimum fields including:

~~~text
environment = NON_PRODUCTION
execution_profile = SYNTHETIC_STRUCTURAL_NONPROD
synthetic_patient_data_only = true
real_patient_traffic = false

profile_a_real_c03_enabled = false
profile_a_real_delivery_enabled = false

external_delivery_side_effects = false
external_model_calls = false
external_tool_calls = false
external_knowledge_calls = false

production_state_store = false
production_consultation_store = false

live_u07_execution = false
live_u14_final_routing = false

synthetic_delivery_scope_required = true

authorized_shared_runtime_change_refs[]
authorized_shared_runtime_paths[]
unreviewed_shared_runtime_change_count = 0
~~~

## 4.2 Current retained auth profile

The exact retained file is:

~~~text
schema
= U06_AUTH_PROFILE_V0_1
~~~

It does contain several equivalent negative boundary flags, but it does **not** provide the frozen minimum machine-readable contract shape.

Missing required fields include at least:

~~~text
environment
synthetic_patient_data_only
real_patient_traffic
profile_a_real_c03_enabled
profile_a_real_delivery_enabled
external_delivery_side_effects
external_model_calls
external_tool_calls
external_knowledge_calls
production_state_store
production_consultation_store
live_u07_execution
live_u14_final_routing
synthetic_delivery_scope_required
authorized_shared_runtime_change_refs
authorized_shared_runtime_paths
unreviewed_shared_runtime_change_count
~~~

The observed-change manifest and environment evidence prove many of these facts elsewhere, but RDP-06 explicitly requires them in the auth profile authority artifact.

## 4.3 Why this is blocking

The contract manifest explicitly treats the auth profile as a required authority artifact.

RDP-06 requires the static Oracle / Fixtures / Auth Profile to be built/reviewed before authoritative verification.

Therefore a digest-valid but schema-incomplete auth profile cannot be accepted as satisfying the frozen authority package.

**Disposition: OPEN / BLOCKING.**

---

# 5. New blocking finding B-U06-EVRR-02 — Auth Profile contains stale implementation semantic metadata

The retained auth profile contains:

~~~text
implementation_semantic_pass_sha
= 1d427fd09957767c938e34883dcbcda85b7fd992
~~~

The exact current semantic target is:

~~~text
implementation_semantic_pass_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

This is not a digest mismatch: the contract manifest correctly hashes the stale auth-profile bytes.

It is an **authority-status consistency** mismatch.

Frozen RDP-06 §22 requires the verifier to confirm authority metadata is not stale or internally contradictory.

The old semantic SHA was valid for the prior implementation review but was superseded by the exact-head re-review and semantic re-freeze.

No contract text defines this field as an immutable “authorization-time semantic SHA”; its explicit name is `implementation_semantic_pass_sha`.

Therefore:

~~~text
auth profile semantic SHA
!= current implementation semantic SHA

→ stale authority metadata
→ evidence cannot be accepted
~~~

**Disposition: OPEN / BLOCKING.**

---

# 6. New blocking finding B-U06-EVRR-03 — Verifier does not enforce Auth Profile authority-status consistency

The current authoritative verifier validates:

~~~text
auth_profile_sha256
== manifest.auth_profile.digest
~~~

but does not validate the auth-profile contract body against the frozen minimum fields.

It also does not validate:

~~~text
auth_profile.implementation_semantic_pass_sha
== manifest.authority_core.implementation_semantic_pass_sha
== --implementation-sha
~~~

This explains why the fresh authoritative run could produce:

~~~text
PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
~~~

while carrying stale/incomplete auth-profile authority metadata.

Frozen RDP-06 §22 requires this authority-status consistency gate to be enforced by the verifier, not deferred solely to human review.

**Disposition: OPEN / BLOCKING.**

---

# 7. Required remediation

The next remediation must remain evidence/authority/verifier-only unless a separate implementation defect is found.

At minimum:

1. Replace / migrate `u06-auth-profile.json` to the frozen machine-readable `U06_VERIFICATION_AUTH_PROFILE_V0_1` shape.
2. Populate all minimum RDP-06 fields from the already authorized PROFILE-B non-production boundary.
3. Bind `implementation_semantic_pass_sha` to:
   `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`.
4. Include exact authorized shared-runtime refs / paths and:
   `unreviewed_shared_runtime_change_count = 0`.
5. Update `manifest.auth_profile.digest` to the new exact bytes.
6. Because the contract-manifest blob changes, rebind provenance-only gate metadata that explicitly stores `reviewed_contract_manifest_blob_sha`.
7. Perform a targeted independent auth-profile / manifest equivalence review before the fresh authoritative run.
8. Add verifier fail-closed checks for:
   - auth-profile schema;
   - every required hard-boundary field;
   - semantic SHA consistency;
   - exact authorization ID/profile;
   - exact shared-runtime allowlist refs/paths;
   - unreviewed shared-runtime count.
9. Execute a fresh authoritative run.
10. Perform a new Independent Evidence-Only Re-Review.

If no runtime implementation code changes, the implementation semantic freeze remains:

~~~text
66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

No new implementation semantic re-freeze is required solely for this evidence-authority remediation.

---

# 8. Re-review verdict

Prior findings:

~~~text
B-U06-EVR-01
= CLOSED / PASS

B-U06-EVR-02
= CLOSED / PASS

B-U06-EVR-03
= CLOSED / PASS

RF-U06-EVR-01
= CLOSED / PASS

RF-U06-EVR-02
= CLOSED / PASS
~~~

New findings:

~~~text
B-U06-EVRR-01
AUTH_PROFILE_CONTRACT_SHAPE_INCOMPLETE
= OPEN / BLOCKING

B-U06-EVRR-02
AUTH_PROFILE_STALE_SEMANTIC_BINDING
= OPEN / BLOCKING

B-U06-EVRR-03
AUTH_PROFILE_CONSISTENCY_GATE_NOT_ENFORCED
= OPEN / BLOCKING
~~~

Therefore:

~~~text
U06 Independent Evidence-Only Re-Review
= REVISE_REQUIRED

Fresh Authoritative Behavioral Evidence
= PASSING

Durable Bundle Remediation
= PASS

Independent Evidence Acceptance
= NOT_PASSED

Combined Implementation / Evidence Review
= NOT_PERMITTED YET

Implementation Verification Closure
= NOT_PASSED

Merge Authorization
= NOT_GRANTED
~~~

---

# 9. Next permitted step

> **U06 Auth Profile Authority Consistency Targeted Remediation**

This remediation should update only authority/provenance/verifier assets, preserve implementation semantic head `66fa3c0...`, undergo targeted independent equivalence review, and then produce a fresh authoritative evidence artifact for another evidence-only re-review.
