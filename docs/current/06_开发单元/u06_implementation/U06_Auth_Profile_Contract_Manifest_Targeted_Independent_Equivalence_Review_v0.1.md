# U06 Auth Profile / Contract Manifest Targeted Independent Equivalence Review v0.1

**Review verdict:** PASS  
**Review type:** Targeted independent authority/equivalence review  
**Reviewed remediation candidate:** `98b7ff7c8e77685490f5db6416878aff49c0fb07`  
**Implementation semantic SHA:** `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
**Authority core digest:** `038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22`  
**Reviewed auth-profile SHA-256:** `b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4`  
**Reviewed contract-manifest Git blob:** `fec6755eab3709472b571f9317c75c8b50b6efd4`

## 1. Review question

This review asks only whether the U06 Auth Profile Authority Consistency remediation is a faithful machine-readable projection of already frozen authorization and whether the contract-manifest change is provenance/asset-binding equivalent rather than a semantic authority expansion.

It does not re-review runtime implementation semantics and does not grant evidence closure or merge authorization.

## 2. Runtime semantic isolation

Compared with the current implementation/evidence branch base `12efdd491e4d0bb14840132be8e3e72ff75ab518`, the remediation candidate changes only:

- `u06-auth-profile.json`;
- `u06-auth-profile-review-gate.json`;
- `u06-contract-manifest.json`;
- `verify_u06.py`;
- remediation documentation.

~~~text
runtime src/main changes
= 0

IMPLEMENTATION_SEMANTIC_PASS_SHA
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
= unchanged
~~~

Therefore the existing implementation semantic freeze remains applicable.

## 3. B-U06-EVRR-01 — Auth Profile contract shape

The candidate migrates the legacy profile to:

~~~text
schema
= U06_VERIFICATION_AUTH_PROFILE_V0_1
~~~

Independent comparison against frozen RDP-06 minimum fields found:

~~~text
required minimum fields
= 18

missing required fields
= 0

required-value mismatches
= 0
~~~

The profile correctly states:

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

unreviewed_shared_runtime_change_count = 0
~~~

Legacy compatibility aliases remain restrictive and do not contradict the frozen minimum fields.

### Verdict

~~~text
B-U06-EVRR-01
= CLOSED_FOR_CANDIDATE / PASS
~~~

## 4. Authorization refs and path allowlist equivalence

Candidate authority refs:

~~~text
AUTH-U06-PROFILEB-IMPL-001
FROZEN_RDP01_TO_RDP06
AGGREGATE_AC01_TO_AC10
CA_IRR01_TO_IRR03
EXACT_SHARED_RUNTIME_ALLOWLIST_ONLY
~~~

Independent source check:

~~~text
refs present in frozen Owner Authorization / Allowed Change authority
= 5 / 5

unauthorized refs
= 0
~~~

Candidate authorized paths contain 18 entries.

Independent source check:

~~~text
paths present in frozen Owner Authorization Decision
or Authorization-Time Allowed Change Manifest
= 18 / 18

paths outside frozen authorization
= 0
~~~

The list includes the complete authorization-time implementation/test/tool/workflow surface rather than only files literally named “shared runtime.” This is accepted because:

1. it is an exact projection of the already frozen owner allowlist;
2. it grants no path absent from owner authorization;
3. RDP-06 requires reviewed allowlist refs/paths to constrain change scope;
4. it must not be interpreted as reclassifying every listed path as a shared-runtime production file.

No permission or production scope is broadened.

## 5. B-U06-EVRR-02 — semantic SHA consistency

Prior stale value:

~~~text
1d427fd09957767c938e34883dcbcda85b7fd992
~~~

Candidate value:

~~~text
66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

This equals the currently refrozen implementation semantic SHA in the contract manifest.

~~~text
auth_profile.implementation_semantic_pass_sha
= manifest.authority_core.implementation_semantic_pass_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

### Verdict

~~~text
B-U06-EVRR-02
= CLOSED_FOR_CANDIDATE / PASS
~~~

## 6. Contract-manifest equivalence

Independent structural diff of the pre-remediation and candidate contract manifests found exactly one changed JSON path:

~~~text
auth_profile.digest
~~~

Change:

~~~text
old
= 38f6afb30a55ea8d4cd54dad52509a629e20fbd31639b9b537e1324205a65f7c

new
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4
~~~

Unchanged:

~~~text
authority_core
= unchanged

authority_core_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

implementation semantic SHA
= unchanged

Oracle path/digest
= unchanged

Fixture path/digest
= unchanged

all 17 frozen authority entries
= unchanged

case counts
= unchanged
~~~

Therefore the contract-manifest change is an exact auth-profile asset rebinding, not a semantic contract/Oracle/Fixture refreeze.

## 7. Auth Profile review-gate design

The candidate introduces:

~~~text
U06_AUTH_PROFILE_REVIEW_GATE_V0_1
~~~

It correctly remains:

~~~text
verdict
= PENDING_INDEPENDENT_REVIEW
~~~

in the remediation candidate.

Its candidate bindings are exact:

~~~text
reviewed_auth_profile_digest
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4

reviewed_contract_manifest_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

reviewed_contract_manifest_blob_sha
= fec6755eab3709472b571f9317c75c8b50b6efd4

reviewed_implementation_semantic_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661
~~~

This review supplies the independent authority needed to transition that gate from PENDING to PASS in a subsequent provenance-binding commit.

New immutable review identity:

~~~text
U06_AUTH_PROFILE_EQUIVALENCE_REVIEW_20260924_01
~~~

## 8. B-U06-EVRR-03 — verifier consistency enforcement

The candidate verifier no longer treats a matching auth-profile file hash as sufficient.

It adds fail-closed checks for:

- auth-profile schema;
- authorization ID;
- environment / execution / delivery profile;
- semantic SHA against both manifest and CLI implementation target;
- every frozen hard-boundary field;
- exact authority refs;
- exact authorization-time path list;
- unreviewed shared-runtime count;
- independent auth-profile gate verdict/digest/core/blob/semantic binding;
- presence of review identity and independent review record;
- exact current contract-manifest Git blob in Oracle and Fixture refrozen review gates.

This is directionally and structurally consistent with RDP-06 §22 authority-status consistency and §23 exact-head binding.

The candidate intentionally cannot PASS before the independent review gate is rebound.

### Verdict

~~~text
B-U06-EVRR-03
= CLOSED_FOR_CANDIDATE / PASS
~~~

## 9. No authorization expansion

Independent comparison found no transition from false/prohibited to true/allowed for any sensitive boundary.

Still prohibited:

- PROFILE-A;
- real patient traffic;
- real PHI;
- real C03/D04;
- external delivery;
- external model/tool/knowledge calls;
- production state/consultation stores;
- direct F1 runtime activation;
- live U07;
- live U14;
- production Scheduler routing.

~~~text
authorization expansion
= 0
~~~

## 10. Review verdict

~~~text
U06 Auth Profile / Contract Manifest
Targeted Independent Equivalence Review
= PASS

review_id
= U06_AUTH_PROFILE_EQUIVALENCE_REVIEW_20260924_01

reviewed_candidate
= 98b7ff7c8e77685490f5db6416878aff49c0fb07

reviewed_auth_profile_digest
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4

reviewed_contract_manifest_blob
= fec6755eab3709472b571f9317c75c8b50b6efd4

authority_core_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

B-U06-EVRR-01
= PASS / ELIGIBLE_TO_CLOSE_AFTER_BINDING

B-U06-EVRR-02
= PASS / ELIGIBLE_TO_CLOSE_AFTER_BINDING

B-U06-EVRR-03
= PASS / ELIGIBLE_TO_CLOSE_AFTER_BINDING
~~~

## 11. Required post-review provenance binding

This review does not mutate the reviewed candidate in place.

Next permitted step is a controlled post-review binding commit that:

1. sets `u06-auth-profile-review-gate.json` to PASS;
2. binds:
   - review ID `U06_AUTH_PROFILE_EQUIVALENCE_REVIEW_20260924_01`;
   - this review-record commit SHA;
3. rebinds the refrozen Oracle and Fixture gates' `reviewed_contract_manifest_blob_sha` from the previous contract-manifest blob to:
   `fec6755eab3709472b571f9317c75c8b50b6efd4`;
4. does not change their reviewed Oracle/Fixture digests or review identities;
5. integrates the reviewed remediation candidate into the implementation/evidence branch;
6. runs a fresh authoritative verification;
7. performs another Independent Evidence-Only Re-Review.

No new implementation semantic re-freeze is required unless runtime implementation code changes.
